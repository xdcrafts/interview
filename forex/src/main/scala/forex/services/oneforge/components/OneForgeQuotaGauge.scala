package forex.services.oneforge.components

import cats.Eval
import com.typesafe.scalalogging.LazyLogging
import forex.config.{ ApplicationConfig, OneForgeConfig }
import forex.main.Executors
import forex.services.metrics._
import forex.services.oneforge.client.OneForgeClient
import org.xdcrafts.metrics.MetricsInstrumented
import org.zalando.grafter.{ Start, StartResult }
import org.zalando.grafter.macros.readerOf

import scala.concurrent.Await

@readerOf[ApplicationConfig]
case class OneForgeQuotaGauge(
    config: OneForgeConfig,
    client: OneForgeClient,
    executors: Executors
) extends MetricsInstrumented
    with LazyLogging
    with Start {

  import executors._

  override def start: Eval[StartResult] = StartResult.eval("OneForgeQuotaGauge") {
    metrics.cachedGauge(s"$biz.one-forge-live.quota.${config.apiKey}", config.cacheRefreshRate) {
      Await.result(client.getQuota.runAsync, config.quotaRequestTimeout) match {
        case Left(error) ⇒
          logger.warn("OneForge quota update failed: ", error)
          metrics.meter(
            s"$biz.one-forge-live.quota.${config.apiKey}.${error.getClass.getSimpleName.toLowerCase}"
          )
        case Right(quota) ⇒ quota.quotaRemaining
      }
    }
  }
}
