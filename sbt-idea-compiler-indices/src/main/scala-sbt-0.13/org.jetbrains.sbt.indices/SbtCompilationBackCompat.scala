package org.jetbrains.sbt.indices

import java.util.Optional

object SbtCompilationBackCompat {
  type Analysis         = sbt.inc.Analysis
  type Relations        = sbt.inc.Relations
  type CompileResult    = sbt.Compiler.CompileResult
  type CompileAnalysis  = sbt.inc.Analysis
  type PreviousResult   = sbt.Compiler.PreviousAnalysis
  type ClassFileManager = sbt.inc.ClassfileManager
  type IncOptions       = sbt.inc.IncOptions

  val Analysis = sbt.inc.Analysis

  implicit class CompileResultExt(val result: PreviousResult) extends AnyVal {
    def getAnalysis: Optional[CompileAnalysis] = Optional.of(result.analysis)
  }

  def patchIncOptions(options: IncOptions): IncOptions = {
    val inheritedNewClassfileManager = options.newClassfileManager
    val newClassfileManager          = () => IndexingClassfileManager(inheritedNewClassfileManager())
    options.withNewClassfileManager(newClassfileManager)
  }

  object PreviousResult {
    def empty(): PreviousResult =
      sbt.Compiler.PreviousAnalysis(Analysis.Empty, None)
  }
}
