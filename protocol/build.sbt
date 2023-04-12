name := "scala-compiler-indices-protocol"

organization := "org.jetbrains.scala"

scalaVersion := "2.13.10"

crossScalaVersions := Seq("2.10.7", "2.12.17", "2.13.10")

libraryDependencies += "io.spray" %% "spray-json" % "1.3.6"

homepage := Some(url("https://github.com/JetBrains/intellij-compiler-indices"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/JetBrains/intellij-compiler-indices"),
    "scm:git:git@github.com:JetBrains/intellij-compiler-indices.git"
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
Test / publishArtifact := false
sonatypeProfileName := "org.jetbrains"

val publishAllCommand =
  "; clean ; compile ; + test ; + publishLocal ; ci-release"

addCommandAlias("publishAll", publishAllCommand)
