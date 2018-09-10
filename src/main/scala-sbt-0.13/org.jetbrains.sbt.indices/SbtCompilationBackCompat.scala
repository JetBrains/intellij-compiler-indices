package org.jetbrains.sbt.indices

import java.util.Optional

object SbtCompilationBackCompat {
  type Analysis         = sbt.inc.Analysis
  type Relations        = sbt.inc.Relations
  type CompileResult    = sbt.Compiler.CompileResult
  type CompileAnalysis  = sbt.inc.Analysis
  type PreviousResult   = sbt.Compiler.PreviousAnalysis
  type ClassFileManager = sbt.inc.ClassfileManager

  val Analysis = sbt.inc.Analysis

  implicit class CompileResultExt(val result: PreviousResult) {
    def getAnalysis: Optional[CompileAnalysis] = Optional.of(result.analysis)
  }

  object PreviousResult {
    def empty(): PreviousResult =
      sbt.Compiler.PreviousAnalysis(Analysis.Empty, None)
  }
}
