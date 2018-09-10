package org.jetbrains.sbt.indices

import java.util.Optional

object SbtCompilationBackCompat {
  type Analysis         = sbt.internal.inc.Analysis
  type Relations        = sbt.internal.inc.Relations
  type CompileResult    = xsbti.compile.CompileResult
  type CompileAnalysis  = xsbti.compile.CompileAnalysis
  type PreviousResult   = xsbti.compile.PreviousResult
  type ClassFileManager = xsbti.compile.ClassFileManager

  val Analysis = sbt.internal.inc.Analysis

  implicit class CompileResultExt(val result: PreviousResult) {
    def getAnalysis: Optional[CompileAnalysis] = result.analysis()
  }

  object PreviousResult {
    def empty(): PreviousResult =
      xsbti.compile.PreviousResult.create(Optional.empty(), Optional.empty())
  }
}
