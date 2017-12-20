package forex.config

import org.zalando.grafter.macros._

import scala.concurrent.duration.FiniteDuration

@readers
case class ApplicationConfig(
    akka: AkkaConfig,
    api: ApiConfig,
    oneForge: OneForgeConfig,
    executors: ExecutorsConfig
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
    validateQuotaTimeout: FiniteDuration,
    validateDailyQuota: Boolean,
    validateRemainingQuota: Boolean
)
