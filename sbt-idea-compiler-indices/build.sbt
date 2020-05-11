lazy val root = (project in file("."))
  .settings(
    bintrayOrganization := Option("jetbrains"),
    bintrayRepository   := "sbt-plugins",
    bintrayVcsUrl       := Option("https://github.com/JetBrains/sbt-idea-compiler-indices")
  )
  .settings(
    crossSbtVersions  := Seq("1.3.10", "0.13.18"),
    sbtPlugin         := true,
    publishMavenStyle := false,
    organization      := "org.jetbrains",
    name              := "sbt-idea-compiler-indices",
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