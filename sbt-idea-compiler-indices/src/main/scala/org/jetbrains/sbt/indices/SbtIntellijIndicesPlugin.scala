package org.jetbrains.sbt.indices

import java.util.UUID

import org.jetbrains.plugins.scala.indices.protocol.sbt.Locking._
import org.jetbrains.plugins.scala.indices.protocol.sbt.compilationInfoBaseDir
import org.jetbrains.plugins.scala.indices.protocol.sbt.{Configuration => PConfiguration}
import org.jetbrains.sbt.indices.SbtCompilationBackCompat._
import sbt.Keys._
import sbt.plugins.{CorePlugin, JvmPlugin}
import sbt.{AutoPlugin, Def, _}

object SbtIntellijIndicesPlugin extends AutoPlugin { self =>
  import org.jetbrains.sbt.indices.IntellijIndexer._

  override def trigger: PluginTrigger = allRequirements
  override def requires: Plugins      = CorePlugin && JvmPlugin

  object autoImport {
    lazy val incrementalityType = settingKey[IncrementalityType]("internal use only: Configures index incrementality type")
    lazy val ideaPort           = settingKey[Int]("Port to talk to IDEA indexer")

    lazy val rebuildIndices = Command.command("rebuildIdeaIndices") { state =>
      val patchedState = Command.process(
        """set incrementalityType in Global := _root_.org.jetbrains.sbt.indices.IntellijIndexer.IncrementalityType.NonIncremental""",
        state
      )

      val ext = Project.extract(patchedState)

      val relevantProjects = ext.structure.allProjects.filter(
        _.autoPlugins.exists(_.label == self.getClass.getName.stripSuffix("$"))
      )

      val projectIds   = relevantProjects.map(_.id)
      val buildCommand = projectIds.map(id => s"$id/compile $id/test:compile").mkString("all ", " ", "")

      state.log.info(s"Rebuilding IDEA indices in ${projectIds.mkString(", ")}.")
      Command.process(buildCommand, patchedState)
      state
    }
  }
  import autoImport._

  private[this] def perConfig: Seq[Def.Setting[_]] = Seq(
    incOptions ~= patchIncOptions,
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
        val infoDir         = compilationInfoDir(buildBaseDir, s"$projectId-$configurationId").toPath
        val port            = ideaPort.value

        infoDir.lock(log = log.debug(_))
        val compilationStartTimestamp = System.currentTimeMillis()

        val socket =
          try   notifyIdeaStart(port, buildBaseDir.getPath, compilationId)
          catch { case e: Throwable => infoDir.unlock(log = log.debug(_)); throw e }

        Def.taskDyn {
          val previousResult = itype match {
            case IncrementalityType.Incremental    => previousCompile.value
            case IncrementalityType.NonIncremental => PreviousResult.empty()
          }

          Def.task {
            val oldTaskValue = previousValue.value
            val isOffline    = socket.isEmpty

            val pconfig = configurationId match {
              case Compile => PConfiguration.Compile
              case Test    => PConfiguration.Test
              case conf =>
                sys.error(
                  s"Unsupported configuration $conf. Only Compile and Test configuration are supported by idea-compiler-indices."
                )
            }

            val compilationInfoFile = dumpCompilationInfo(
              isOffline,
              oldTaskValue,
              previousResult,
              projectId,
              version,
              itype,
              infoDir.toFile,
              pconfig,
              compilationStartTimestamp,
              compilationId
            )

            val result = CompilationResult(
              successful = true,
              compilationStartTimestamp,
              compilationInfoFile
            )

            socket.foreach(notifyFinish(_, result))
            oldTaskValue
          }
        }.result.map {
          case Inc(cause) =>
            // compilation failed case
            val failedRes = CompilationResult(
              successful = false,
              compilationStartTimestamp,
              None
            )

            socket.foreach(notifyFinish(_, failedRes))
            throw cause
          case Value(canalysis) => canalysis
        }.andFinally {
          try     infoDir.unlock(log = log.debug(_))
          finally socket.foreach(_.close())
        }
      }
    }.value
  )

  override def projectSettings: Seq[Def.Setting[_]] =
    inConfig(Compile)(perConfig) ++ inConfig(Test)(perConfig) ++ Seq(
      cleanFiles += {
        val base = (baseDirectory in ThisBuild).value
        compilationInfoBaseDir(base.toPath).toFile
      },
      commands += rebuildIndices
    )

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    ideaPort           := 65337,
    incrementalityType := IncrementalityType.Incremental
  )
}
