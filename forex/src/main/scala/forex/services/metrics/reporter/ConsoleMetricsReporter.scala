package forex.services.metrics.reporter

import java.util.concurrent.TimeUnit

import cats.Eval
import com.codahale.metrics.ConsoleReporter
import forex.config.{ ApplicationConfig, ConsoleMetricsReporterConfig }
import org.xdcrafts.metrics.MetricsInstrumented
import org.zalando.grafter.macros.readerOf
import org.zalando.grafter.{ Start, StartResult, Stop, StopResult }

@readerOf[ApplicationConfig]
case class ConsoleMetricsReporter(
    config: ConsoleMetricsReporterConfig
) extends MetricsInstrumented
    with Start
    with Stop {

  @volatile private final var reporter: Option[ConsoleReporter] = None

  override def start: Eval[StartResult] = StartResult.eval("ConsoleMetricsReporter") {
    reporter = Some {
      ConsoleReporter
        .forRegistry(metricRegistry)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build
    }
    reporter.foreach(_.start(config.reportRate.length, config.reportRate.unit))
  }

  override def stop: Eval[StopResult] = StopResult.eval("ConsoleMetricsReporter") {
    reporter.foreach(_.stop())
  }
}
