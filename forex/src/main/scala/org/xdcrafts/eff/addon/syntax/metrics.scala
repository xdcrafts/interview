package org.xdcrafts.eff.addon.syntax

import org.xdcrafts.eff.addon.metrics._
import org.xdcrafts.eff.addon.metrics.MetricsInterpretation._
import org.atnos.eff.Eff

object metrics extends metrics

trait metrics {
  implicit def toMetricsEffectOps[R, A](e: Eff[R, A]): MetricsEffectOps[R, A] = new MetricsEffectOps[R, A](e)
}

final class MetricsEffectOps[R, A](val e: Eff[R, A]) extends AnyVal {
  def runMetrics(implicit member: _Metrics[R]): Eff[member.Out, A] =
    MetricsInterpretation.runMetrics(e)(member.aux)
}
