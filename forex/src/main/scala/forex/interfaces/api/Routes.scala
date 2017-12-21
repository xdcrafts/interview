package forex.interfaces.api

import akka.http.scaladsl._
import backline.http.metrics.{ StatusCodeCounterDirectives, TimerDirectives }
import forex.config._
import forex.interfaces.api.utils._
import forex.services.metrics.tech
import org.xdcrafts.metrics.MetricsInstrumented
import org.zalando.grafter.macros._

@readerOf[ApplicationConfig]
case class Routes(
    ratesRoutes: rates.Routes
) extends MetricsInstrumented
    with TimerDirectives
    with StatusCodeCounterDirectives {
  import server.Directives._

  lazy val route: server.Route =
    withStatusCodeCounterNamed(s"$tech.routes") {
      withTimerNamed(s"$tech.routes") {
        handleExceptions(ApiExceptionHandler()) {
          handleRejections(ApiRejectionHandler()) {
            ratesRoutes.route
          }
        }
      }
    }
}
