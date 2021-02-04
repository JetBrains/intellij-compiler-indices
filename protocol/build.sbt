name := "scala-compiler-indices-protocol"

organization := "org.jetbrains.scala"

scalaVersion := "2.13.2"

crossScalaVersions := Seq("2.10.7", "2.12.11", "2.13.2")

libraryDependencies += "io.spray" %% "spray-json" % "1.3.5"

homepage := Some(url("https://github.com/JetBrains/intellij-scala-indices-protocol"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/JetBrains/intellij-scala-indices-protocol"),
    "https://github.com/JetBrains/intellij-scala-indices-protocol.git"
  )
)

developers := List(
  Developer(
    id    = "sugakandrey",
    name  = "Andrey Sugak",
    email = "andrey.sugak@jetbrains.com",
    url   = url("https://github.com/sugakandrey")
  )
)

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))
publishArtifact in Test := false
sonatypeProfileName := "org.jetbrains"

val publishAllCommand =
  "; clean ; compile ; + test ; + publishLocal ; ci-release"

addCommandAlias("publishAll", publishAllCommand)
