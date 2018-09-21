package org.jetbrains.sbt.indices

import java.util.UUID

import org.jetbrains.plugins.scala.indices.protocol.sbt.Locking._
import org.jetbrains.sbt.indices.SbtCompilationBackCompat._
import sbt.Keys._
import sbt.plugins.{CorePlugin, JvmPlugin}
import sbt.{AutoPlugin, Def, _}

object SbtIntellijIndicesPlugin extends AutoPlugin {
  import org.jetbrains.sbt.indices.IntellijIndexer._

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = CorePlugin && JvmPlugin

  object autoImport {
    lazy val incrementalityType = settingKey[IncrementalityType]("internal use only: Configures index incrementality type")
    lazy val ideaPort           = settingKey[Int]("Port to talk to IDEA indexer")
  }
  import autoImport._

  private[this] def perConfig: Seq[Def.Setting[_]] = Seq(
    incOptions ~= { opt => opt.withClassfileManager(IndexingClassfileManager(opt)) },
    compile    := Def.taskDyn {
      val previousValue = compile.taskValue
      val buildBaseDir  = (baseDirectory in ThisBuild).value

      if (!isIdeaProject(buildBaseDir)) Def.task(previousValue.value)
      else {
        val log             = streams.value.log
        val projectId       = thisProject.value.id
        val configurationId = configuration.value
        val compilationId   = UUID.randomUUID()
        val itype           = incrementalityType.value
        val version         = scalaBinaryVersion.value
        val infoDir         = compilationInfoDir(buildBaseDir, s"$projectId-$configurationId")
        val port            = ideaPort.value

        infoDir.lock(log = log.info(_))
        val compilationStartTimestamp = System.currentTimeMillis()

        val socket =
          try   notifyIdeaStart(port, buildBaseDir.getPath, compilationId)
          catch { case e: Throwable => infoDir.unlock(log = log.info(_)); throw e }

        Def.taskDyn {
          val previousResult = itype match {
            case IncrementalityType.Incremental    => previousCompile.value
            case IncrementalityType.NonIncremental => PreviousResult.empty()
          }

          Def.task {
            val oldTaskValue = previousValue.value

            //@TODO: what if info dumping fails while IDEA is not listening for connections
            val compilationInfoFile = dumpCompilationInfo(
              oldTaskValue,
              previousResult,
              projectId,
              version,
              itype,
              infoDir,
              compilationStartTimestamp,
              compilationId
            )

            val result = CompilationResult(
              successful = true,
              compilationStartTimestamp,
              Option(compilationInfoFile)
            )

            socket.foreach(notifyFinish(_, result))
            oldTaskValue
          }
        }.andFinally {
          socket.foreach(_.close())
          infoDir.unlock(log = log.info(_))
        }
      }
    }.value
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(perConfig) ++ inConfig(Test)(perConfig) ++ Seq(
      cleanFiles += {
        val base = (baseDirectory in ThisBuild).value
        compilationInfoDir(base, thisProject.value.id)
      }
    )

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    ideaPort           := 65337,
    incrementalityType := IncrementalityType.Incremental
  )
}
