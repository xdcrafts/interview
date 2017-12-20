package forex.services.oneforge.components

import forex.config.ApplicationConfig
import forex.domain.{ Price, Rate }
import forex.main.{ AppEffect, ApplicationSpec }
import forex.services.OneForge
import org.zalando.grafter.macros.readerOf

@readerOf[ApplicationConfig]
case class DummyApplication(
    applicationConfig: ApplicationConfig,
    oneForge: OneForgeDummy
)

class DummyApplicationSpec extends ApplicationSpec {
  import forex.domain.Currency._

  val dummyApplication: DummyApplication = application[DummyApplication](config)
  val oneForge: OneForge[AppEffect] = dummyApplication.oneForge.oneForge

  before {
    start(dummyApplication)
  }

  after {
    stop(dummyApplication)
  }

  "Dummy one forge implementation" should "always run and return valid stubbed value" in
    eff(oneForge.get(Rate.Pair(USD, EUR))) { response ⇒
      val rate = response.right.get
      assert(rate == Rate(Rate.Pair(USD, EUR), Price(100.0), rate.timestamp))
    }
}
