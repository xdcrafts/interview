package forex.services.metrics.reporter

import java.util.concurrent.TimeUnit

import cats.Eval
import com.readytalk.metrics.StatsDReporter
import forex.config.{ ApplicationConfig, StatsDMetricsReporterConfig }
import org.xdcrafts.metrics.MetricsInstrumented
import org.zalando.grafter.{ Start, StartResult, Stop, StopResult }
import org.zalando.grafter.macros.readerOf

@readerOf[ApplicationConfig]
case class StatsDMetricsReporter(
    config: StatsDMetricsReporterConfig
) extends MetricsInstrumented
    with Start
    with Stop {

  @volatile private final var reporter: Option[StatsDReporter] = None

  override def start: Eval[StartResult] = StartResult.eval("StatsDMetricsReporter") {
    reporter = Some {
      StatsDReporter
        .forRegistry(metricRegistry)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build(config.host, config.port)
    }
    reporter.foreach(_.start(config.reportRate.length, config.reportRate.unit))
  }

  override def stop: Eval[StopResult] = StopResult.eval("StatsDMetricsReporter") {
    reporter.foreach(_.stop())
  }
}