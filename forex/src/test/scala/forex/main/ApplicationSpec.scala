package forex.main

import java.util.concurrent.TimeUnit

import forex.config.{ configure, ApplicationConfig, ApplicationConfigReader }
import monix.execution.Scheduler
import org.scalamock.scalatest.MockFactory
import org.scalatest._

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

abstract class ApplicationSpec extends FlatSpec with Matchers with EitherValues with BeforeAndAfter with MockFactory {

  import org.atnos.eff.syntax.addon.monix.task._
  import org.xdcrafts.eff.addon.syntax.metrics._
  import org.zalando.grafter.syntax.rewriter._

  implicit val sc: Scheduler = Scheduler(scala.concurrent.ExecutionContext.global)

  def config: ApplicationConfig =
    pureconfig
      .loadConfig[ApplicationConfig]("app")
      .right
      .get

  def application[Application: ApplicationConfigReader](
      config: ApplicationConfig
  ): Application =
    configure[Application](config).singletons

  def start[Application](application: Application): Unit = {
    val withFailures = application.startAll.value
      .filter(!_.success)
    if (withFailures.nonEmpty) {
      fail(toStartErrorString(withFailures))
    }
  }

  def stop[Application](application: Application): Unit = {
    val withFailures = application.stopAll.value
      .filter(!_.success)
    if (withFailures.nonEmpty) {
      fail(toStopString(withFailures))
    }
  }

  def withApplication[Application](
      application: Application
  )(
      callback: Application ⇒ Assertion
  ): Assertion =
    try {
      start(application)
      callback(application)
    } finally {
      stop(application)
    }

  def eff[R, T](eff: AppEffect[R])(callback: R ⇒ T): T =
    Await.result(
      eff.runMetrics.runAsync.runAsync.map(callback),
      FiniteDuration(1, TimeUnit.SECONDS)
    )

}
