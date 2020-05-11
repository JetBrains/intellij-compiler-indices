import complete.DefaultParsers._
import sbt.Def

lazy val check = inputKey[Unit]("123")

lazy val settings: Seq[Def.Setting[_]] = Seq(
  check := {
    val compilationInfoCount = (Space ~> NatBasic).parsed
    val config               = configuration.value
    val infoDirBase          = file("project") / "target" / ".sbt-compilation-infos" / s"root-$config"

    val condition = infoDirBase.exists() && {
      val infoFiles = infoDirBase.listFiles(_.getName.startsWith("compilation-info"))
      infoFiles.size == compilationInfoCount
    }

    if (!condition) sys.error("Plugin check failed.")
  }
)

lazy val root = project.in(file("."))
  .settings(inConfig(Compile)(settings) ++ inConfig(Test)(settings))