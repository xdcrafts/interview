package forex.services.oneforge.components

import java.util.concurrent.TimeUnit

import forex.config.{ ApplicationConfig, OneForgeConfig }
import forex.domain.{ Price, Rate, Timestamp }
import forex.main.ApplicationSpec
import forex.services.oneforge.algebra.Error
import forex.services.oneforge.client.OneForgeClient
import forex.services.oneforge.client.OneForgeClientUtils.OneForgeQuotaResponse
import monix.eval.Task
import org.scalatest.exceptions.TestFailedException
import org.zalando.grafter.macros.readerOf

import scala.concurrent.duration.FiniteDuration

@readerOf[ApplicationConfig]
case class LiveApplication(
    applicationConfig: ApplicationConfig,
    oneForge: OneForgeLive
)

class LiveApplicationSpec extends ApplicationSpec {
  import forex.domain.Currency._
  import org.zalando.grafter.syntax.rewriter._

  behavior of "LiveApplication"

  it should "fail if configuration is not valid" in assertThrows[TestFailedException] {
    val invalidOneForgeConfig = OneForgeConfig(
      apiKey = "",
      cacheInvalidateAfter = FiniteDuration(5, TimeUnit.MILLISECONDS),
      cacheRefreshRate = FiniteDuration(50, TimeUnit.MILLISECONDS),
      validateQuotaTimeout = FiniteDuration(5, TimeUnit.MILLISECONDS),
      validateDailyQuota = true,
      validateRemainingQuota = true
    )
    val invalidConfig = config
      .replace[OneForgeConfig](invalidOneForgeConfig)
    val asyncApplication = application[LiveApplication](invalidConfig)
    withApplication(asyncApplication)(_ ⇒ succeed)
  }

  it should "fail if configuration does not fit quota" in assertThrows[TestFailedException] {
    val mockOneForgeClient = mock[OneForgeClient]
    val asyncApplication = application[LiveApplication](config)
      .replace[OneForgeClient](mockOneForgeClient)

    val quota = Task.now(OneForgeQuotaResponse(0, 100, 100, 24))
    (mockOneForgeClient.getQuota _).expects().returning(quota)
    withApplication(asyncApplication)(_ ⇒ succeed)
  }

  it should "start if configuration fit quota" in {
    val mockOneForgeClient = mock[OneForgeClient]
    val asyncApplication = application[LiveApplication](config)
      .replace[OneForgeClient](mockOneForgeClient)

    val quota = Task.now(OneForgeQuotaResponse(0, 100000, 100000, 24))
    (mockOneForgeClient.getQuota _).expects().returning(quota)
    (mockOneForgeClient.getOneForgeRates _).expects().returning(Task.now(Map()))

    withApplication(asyncApplication)(_ ⇒ succeed)
  }

  it should "return a not found error if conversion rate not found" in {
    val mockOneForgeClient = mock[OneForgeClient]
    val asyncApplication = application[LiveApplication](config)
      .replace[OneForgeClient](mockOneForgeClient)

    val quota = Task.now(OneForgeQuotaResponse(0, 100000, 100000, 24))
    (mockOneForgeClient.getQuota _).expects().returning(quota)
    (mockOneForgeClient.getOneForgeRates _).expects().returning(Task.now(Map()))

    withApplication(asyncApplication) { _ ⇒
      eff(asyncApplication.oneForge.oneForge.get(Rate.Pair(USD, EUR))) { r ⇒
        assert(r.left.get == Error.NotFound)
      }
    }
  }

  it should "refresh cache after configured amount of time" in {
    val mockOneForgeClient = mock[OneForgeClient]
    val asyncApplication = application[LiveApplication](config)
      .replace[OneForgeClient](mockOneForgeClient)

    val quota = Task.now(OneForgeQuotaResponse(0, 100000, 100000, 24))
    (mockOneForgeClient.getQuota _).expects().returning(quota)

    val pair = Rate.Pair(USD, EUR)
    val cacheTask = Task.eval(Map(pair → Rate(pair, Price(100.0), Timestamp.now)))
    (mockOneForgeClient.getOneForgeRates _).stubs().returning(cacheTask)

    val oneForge = asyncApplication.oneForge.oneForge
    def getRateTimestamp = eff(oneForge.get(pair))(_.right.value.timestamp)

    withApplication(asyncApplication) { _ ⇒
      Thread.sleep(asyncApplication.applicationConfig.oneForge.cacheRefreshRate.toMillis)
      val firstTs = getRateTimestamp

      Thread.sleep(asyncApplication.applicationConfig.oneForge.cacheRefreshRate.toMillis * 2)
      val thirdTs = getRateTimestamp

      assert(thirdTs.value.isAfter(firstTs.value))
    }
  }
}
