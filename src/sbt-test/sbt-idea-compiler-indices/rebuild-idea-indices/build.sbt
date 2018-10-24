import complete.DefaultParsers._
import sbt.Def

lazy val check               = inputKey[Unit]("check")
lazy val checkIncrementality = inputKey[Unit]("checkIncrementality")

lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
  check := {
    val compilationInfoCount = (Space ~> NatBasic).parsed
    val config               = configuration.value
    val id                   = thisProjectRef.value.project
    val infoDirBase          = file(".idea") / ".sbt-compilation-infos" / s"$id-$config"

    val condition = infoDirBase.exists() && {
      val infoFiles = infoDirBase.listFiles(_.getName.startsWith("compilation-info"))
      infoFiles.size == compilationInfoCount
    }

    if (!condition) sys.error("Plugin check failed.")
  },
  checkIncrementality := {
    val incrementality = incrementalityType.value
    if (incrementality != org.jetbrains.sbt.indices.IntellijIndexer.IncrementalityType.Incremental)
      sys.error("Incrementality type should not be affected by rebuildIdeaIndices task")
  }
)

lazy val allEnabled = project
  .in(file("."))
  .aggregate(foo, bar)

lazy val foo = project
  .in(file("foo"))
  .settings(inConfig(Compile)(commonSettings) ++ inConfig(Test)(commonSettings))

lazy val bar = project
  .in(file("bar"))
  .settings(inConfig(Compile)(commonSettings) ++ inConfig(Test)(commonSettings))

lazy val disabled = project
  .in(file("disabled"))
  .disablePlugins(SbtIntellijIndicesPlugin)
  .settings(inConfig(Compile)(commonSettings) ++ inConfig(Test)(commonSettings))
  .settings(
    compile in Compile := {
      sys.error("should fail")
    }
  )
