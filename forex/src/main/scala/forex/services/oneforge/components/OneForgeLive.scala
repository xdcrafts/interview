package forex.services.oneforge.components

import java.util.concurrent.TimeUnit

import cats.Eval
import cats.data.Validated.{ Invalid, Valid }
import cats.data.{ Validated, ValidatedNel }
import com.typesafe.scalalogging.LazyLogging
import forex.config.{ ApplicationConfig, OneForgeConfig }
import forex.main._
import forex.services.oneforge.algebra.Error.{ ApiError, System }
import forex.services.oneforge.algebra.{ Interpreters, Live }
import forex.services.oneforge.client.OneForgeClient
import monix.eval.Task
import monix.execution.{ Cancelable, Scheduler }
import org.zalando.grafter._
import org.zalando.grafter.macros.readerOf

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

@readerOf[ApplicationConfig]
case class OneForgeLive(
    config: OneForgeConfig,
    client: OneForgeClient,
    executors: Executors
) extends OneForgeComponent
    with Start
    with Stop
    with LazyLogging {

  import OneForgeLive._
  import executors._

  override final lazy val oneForge: Live[AppStack] =
    Interpreters.live[AppStack](config.cacheInvalidateAfter)

  private final lazy val effect: Task[Unit] =
    client.getOneForgeRates.map(c ⇒ oneForge.swap(Task.now(c)))

  @volatile private final var scheduledTask: Option[Cancelable] = None

  override def start: Eval[StartResult] = StartResult.eval("AsyncOneForge") {

    validateConfig(config, client) match {
      case Invalid(errors) ⇒ throw new IllegalArgumentException(errors.toList.mkString("{", " | ", "}"))
      case Valid(_)        ⇒ ()
    }

    logger.info(s"Will update rates each ${config.cacheRefreshRate}")

    scheduledTask = Some(
      default.scheduleAtFixedRate(FiniteDuration(0, TimeUnit.SECONDS), config.cacheRefreshRate) {
        effect.runOnComplete {
          case Success(_) ⇒ logger.debug("OneForge rates updated")
          case Failure(err) ⇒
            err match {
              case ApiError(msg) ⇒ logger.warn(s"OneForge rates update failed. OneForge API responded with error: $msg")
              case System(thr)   ⇒ logger.warn(s"OneForge rates update failed:", thr)
              case _             ⇒ logger.warn(s"OneForge rates update failed:", err)
            }
        }
      }
    )
  }

  override def stop: Eval[StopResult] = StopResult.eval("AsyncOneForge") {
    scheduledTask.foreach(_.cancel())
    scheduledTask = None
  }
}

object OneForgeLive {
  import cats.implicits._

  type ValidatedConf = ValidatedNel[String, Unit]

  val unsupportedTimeUnits = Set(TimeUnit.MILLISECONDS, TimeUnit.MICROSECONDS, TimeUnit.NANOSECONDS)

  def validateConfig(
      config: OneForgeConfig,
      oneForgeClient: OneForgeClient
  )(
      implicit sc: Scheduler
  ): ValidatedConf =
    validateSupportedTimeUnits("invalidate after duration", config.cacheInvalidateAfter)
      .combine(validateSupportedTimeUnits("refresh rate", config.cacheRefreshRate))
      .combine(validateRates(config.cacheRefreshRate, config.cacheInvalidateAfter))
      .andThen(_ ⇒ validateQuotas(config, oneForgeClient))

  def validateQuotas(
      config: OneForgeConfig,
      oneForgeClient: OneForgeClient
  )(
      implicit sc: Scheduler
  ): ValidatedConf =
    if (!config.validateDailyQuota && !config.validateRemainingQuota) {
      Validated.valid(())
    } else {
      val quota = Await.result(oneForgeClient.getQuota.runAsync, config.validateQuotaTimeout)
      val dailyQuotaValidator =
        () ⇒ validateQuota(config.cacheRefreshRate, "total", quota.quotaLimit, 24)
      val remainingQuotaValidator =
        () ⇒ validateQuota(config.cacheRefreshRate, "remaining", quota.quotaRemaining, quota.hoursUntilReset)
      (config.validateDailyQuota, config.validateRemainingQuota) match {
        case (true, true)  ⇒ dailyQuotaValidator().combine(remainingQuotaValidator())
        case (false, true) ⇒ remainingQuotaValidator()
        case (true, false) ⇒ dailyQuotaValidator()
        case _             ⇒ Validated.valid(())
      }
    }

  def validateQuota(refreshRate: FiniteDuration, quotaType: String, quota: Int, hours: Int): ValidatedConf = {
    val period = refreshRate.toSeconds
    val refreshesUntilReset = hours * 60 * 60 / period
    if (refreshesUntilReset <= quota) {
      Validated.valid(())
    } else {
      Validated.invalidNel(
        s"the $quotaType quota of $quota requests per $hours hours unable to support refresh every $refreshRate."
      )
    }
  }

  def validateSupportedTimeUnits(field: String, duration: FiniteDuration): ValidatedConf =
    if (unsupportedTimeUnits.contains(duration.unit)) {
      Validated.invalidNel(s"$field does not support time unit ${duration.unit}")
    } else {
      Validated.valid(())
    }

  def validateRates(refreshRate: FiniteDuration, invalidateRate: FiniteDuration): ValidatedConf =
    if (refreshRate.compare(invalidateRate) > 0) {
      Validated.invalidNel(s"refresh rate can not be greater than cache invalidation duration")
    } else {
      Validated.valid(())
    }
}
