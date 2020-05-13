[![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![build](https://github.com/JetBrains/intellij-compiler-indices/workflows/build/badge.svg)](https://github.com/JetBrains/intellij-compiler-indices/actions?query=workflow%3Abuild)

# Compiler Indices for IntelliJ IDEA Scala Plugin

This repository contains two separate sbt projects:

* sbt-idea-compiler-indices: sbt plugin that creates bytecode indices after sbt compilation tasks
* scala-compiler-indices-protocol: protocol used for communication between the sbt plugin and the Scala plugin for IntelliJ IDEA

Due to problems with depending on cross-versioned artifacts from sbt plugins, the protocol and sbt plugin are two separate sbt builds. To use an updated version of the protocol from the sbt plugin, it must be published first (locally)