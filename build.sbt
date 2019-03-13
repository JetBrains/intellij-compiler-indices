lazy val root = (project in file("."))
  .settings(
    bintrayOrganization := Option("jetbrains"),
    bintrayRepository   := "sbt-plugins",
    bintrayVcsUrl       := Option("https://github.com/JetBrains/sbt-idea-compiler-indices")
  )
  .settings(
    crossSbtVersions  := Seq("1.2.8", "0.13.18"),
    publishMavenStyle := false,
    organization      := "org.jetbrains",
    name              := "sbt-idea-compiler-indices",
    scalaVersion      := "2.12.6",
    version           := "0.1.2",
    description       := "sbt plugin for writing IntelliJ bytecode indices",
    scalacOptions     := Seq(
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
    libraryDependencies += "io.github.sugakandrey" %% "scala-compiler-indices-protocol" % "0.1.1",
  )
  .enablePlugins(SbtPlugin)
  .settings(
    scripted := {}, // temporarily disable scripted tests, because of buildserver issues
    scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
    scriptedBufferLog := false
  )