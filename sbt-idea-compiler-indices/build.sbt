import sbtdynver.GitDirtySuffix

lazy val root = (project in file("."))
  .settings(
    crossSbtVersions   := Seq("1.3.10", "0.13.18"),
    crossScalaVersions := Nil,
    sbtPlugin          := true,
    publishMavenStyle  := false,
    organization       := "org.jetbrains.scala",
    name               := "sbt-idea-compiler-indices",
    description        := "sbt plugin for writing IntelliJ bytecode indices",
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
    ),

    libraryDependencies += "org.jetbrains.scala" %% "scala-compiler-indices-protocol" % {
      // depend on the latest non-dirty version of protocol. needs to be published (locally) to depend on
      val describe = dynverGitDescribeOutput.value
      val unsullied = describe.map(_.copy(dirtySuffix = GitDirtySuffix("")))
      if (describe.isVersionStable) version.value else unsullied.map(_.sonatypeVersion).getOrElse(version.value)
    },

    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("public"),
    sonatypeProfileName := "org.jetbrains",

    sbtVersion in pluginCrossBuild := {
      // keep this as low as possible to avoid running into binary incompatibility such as https://github.com/sbt/sbt/issues/5049
      scalaBinaryVersion.value match {
        case "2.10" => "0.13.18"
        case "2.12" => "1.3.12"
      }
    },

    scripted := {}, // temporarily disable scripted tests, because of buildserver issues
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
    scriptedBufferLog := false,


    scriptedSbt := {
      // first releases that can build 2.13 (as they bring a Zinc version with a compiler-bridge published for 2.13)
      scalaBinaryVersion.value match {
        case "2.10" => "0.13.18"
        case "2.12" => "1.3.12"
      }
    }
  )
  .enablePlugins(SbtPlugin)
