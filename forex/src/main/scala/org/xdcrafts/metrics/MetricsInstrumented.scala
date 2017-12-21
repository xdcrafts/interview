package org.xdcrafts.metrics

import nl.grons.metrics.scala.{ DefaultInstrumented, MetricBuilder, MetricName }

trait MetricsInstrumented extends DefaultInstrumented {
  override lazy val metricBuilder: MetricBuilder = new MetricBuilder(MetricName(""), metricRegistry)
}
