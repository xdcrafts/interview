package forex.config

import org.zalando.grafter.macros._

import scala.concurrent.duration.FiniteDuration

@readers
case class ApplicationConfig(
    akka: AkkaConfig,
    api: ApiConfig,
    oneForge: OneForgeConfig,
    executors: ExecutorsConfig,
    consoleMetricsReporter: ConsoleMetricsReporterConfig,
    statsDMetricsReporter: StatsDMetricsReporterConfig
)

case class AkkaConfig(
    name: String,
    exitJvmTimeout: Option[FiniteDuration]
)

case class ApiConfig(
    interface: String,
    port: Int
)

case class ExecutorsConfig(
    default: String
)

case class OneForgeConfig(
    apiKey: String,
    cacheInvalidateAfter: FiniteDuration,
    cacheRefreshRate: FiniteDuration,
    quotaRequestTimeout: FiniteDuration,
    validateDailyQuota: Boolean,
    validateRemainingQuota: Boolean
)

case class ConsoleMetricsReporterConfig(
    reportRate: FiniteDuration
)

case class StatsDMetricsReporterConfig(
    reportRate: FiniteDuration,
    host: String,
    port: Int
)