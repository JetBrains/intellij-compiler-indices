import sbtdynver.GitDirtySuffix

enablePlugins(ScriptedPlugin)

crossSbtVersions   := Nil // handled by explicitly setting sbtVersion via scalaVersion
crossScalaVersions := Seq("2.12.17", "2.10.7")
sbtPlugin          := true
scalaVersion       := "2.12.17"
organization       := "org.jetbrains.scala"
name               := "sbt-idea-compiler-indices"
description        := "sbt plugin for writing IntelliJ bytecode indices"
scalacOptions      := Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Xfuture",
  "-Xexperimental"
)

libraryDependencies += "org.jetbrains.scala" %% "scala-compiler-indices-protocol" % {
  // depend on the latest non-dirty version of protocol. needs to be published (locally) to depend on
  val describe = dynverGitDescribeOutput.value
  val unsullied = describe.map(_.copy(dirtySuffix = GitDirtySuffix("")))
  if (describe.isVersionStable) version.value else unsullied.map(_.sonatypeVersion).getOrElse(version.value)
}

resolvers ++= Resolver.sonatypeOssRepos("snapshots")
resolvers ++= Resolver.sonatypeOssRepos("public")
sonatypeProfileName := "org.jetbrains"


sbtVersion in pluginCrossBuild := {
  // keep this as low as possible to avoid running into binary incompatibility such as https://github.com/sbt/sbt/issues/5049
  scalaBinaryVersion.value match {
    case "2.10" => "0.13.17"
    case "2.12" => "1.2.1"
  }
}

scripted := {} // temporarily disable scripted tests, because of buildserver issues
scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
scriptedBufferLog := false

scriptedSbt := {
  // first releases that can build 2.13 (as they bring a Zinc version with a compiler-bridge published for 2.13)
  scalaBinaryVersion.value match {
    case "2.10" => "0.13.18"
    case "2.12" => "1.2.7"
  }
}

// Project metadata

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
