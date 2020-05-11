package org.jetbrains.sbt.indices

import java.util.Optional

object SbtCompilationBackCompat {
  type Analysis         = sbt.internal.inc.Analysis
  type Relations        = sbt.internal.inc.Relations
  type CompileResult    = xsbti.compile.CompileResult
  type CompileAnalysis  = xsbti.compile.CompileAnalysis
  type PreviousResult   = xsbti.compile.PreviousResult
  type ClassFileManager = xsbti.compile.ClassFileManager
  type IncOptions       = xsbti.compile.IncOptions

  val Analysis = sbt.internal.inc.Analysis

  implicit class CompileResultExt(val result: PreviousResult) extends AnyVal {
    def getAnalysis: Optional[CompileAnalysis] = result.analysis()
  }

  def patchIncOptions(options: IncOptions): IncOptions = {
    val newExternalHooks = options.externalHooks().withExternalClassFileManager(IndexingClassfileManager)
    options.withExternalHooks(newExternalHooks)
  }

  object PreviousResult {
    def empty(): PreviousResult =
      xsbti.compile.PreviousResult.create(Optional.empty(), Optional.empty())
  }
}
