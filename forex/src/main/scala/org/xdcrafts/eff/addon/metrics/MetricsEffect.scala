package org.xdcrafts.eff.addon.metrics

import cats._
import cats.implicits._
import com.codahale.metrics.{ Timer ⇒ DropwizardTimer }
import org.atnos.eff._
import org.atnos.eff.all._
import org.atnos.eff.interpret._
import org.xdcrafts.metrics.MetricsInstrumented

import scala.concurrent.duration.FiniteDuration

sealed trait Metrics[A]
case class Count(name: String) extends Metrics[Unit]
case class Meter(name: String, count: Long) extends Metrics[Unit]
case class Gauge[A](name: String, f: () ⇒ A) extends Metrics[Unit]
case class CachedGauge[A](name: String, timeout: FiniteDuration, f: () ⇒ A) extends Metrics[Unit]
case class Timer[A](name: String, f: () ⇒ A) extends Metrics[A]
case class TimerContext(name: String) extends Metrics[DropwizardTimer.Context]

trait MetricsTypes {
  type _metrics[R] = |=[Metrics, R]
  type _Metrics[R] = <=[Metrics, R]
}

trait MetricsEffect extends MetricsTypes {

  def count[R: _metrics](name: String): Eff[R, Unit] =
    send[Metrics, R, Unit](Count(name))

  def meter[R: _metrics](name: String, count: Long = 1): Eff[R, Unit] =
    send[Metrics, R, Unit](Meter(name, count))

  def gauge[A, R: _metrics](name: String)(f: () ⇒ A): Eff[R, Unit] =
    send[Metrics, R, Unit](Gauge(name, f))

  def cachedGauge[A, R: _metrics](name: String, timeout: FiniteDuration, f: () ⇒ A): Eff[R, Unit] =
    send[Metrics, R, Unit](CachedGauge(name, timeout, f))

  def timer[A, R: _metrics](name: String)(f: () ⇒ A): Eff[R, A] =
    send[Metrics, R, A](Timer(name, f))

  def timerContext[R: _metrics](name: String): Eff[R, DropwizardTimer.Context] =
    send[Metrics, R, DropwizardTimer.Context](TimerContext(name))
}

object MetricsEffect extends MetricsEffect

trait MetricsInterpretation extends MetricsTypes with MetricsInstrumented {

  private def matchEffect[X](me: Metrics[X]): X = me match {
    case Count(name) ⇒
      metrics.counter(name).inc()
      ().asInstanceOf[X]
    case Meter(name, count) ⇒
      metrics.meter(name).mark(count)
      ().asInstanceOf[X]
    case Gauge(name, f) ⇒
      metrics.gauge(name)(f())
      ().asInstanceOf[X]
    case CachedGauge(name, timeout, f) ⇒
      metrics.cachedGauge(name, timeout)(f())
      ().asInstanceOf[X]
    case Timer(name, f) ⇒
      metrics.timer(name).time(f())
    case TimerContext(name) ⇒
      metrics
        .timer(name)
        .timerContext()
        .asInstanceOf[X]
  }

  def runMetrics[R, A](effect: Eff[R, A])(implicit m: _Metrics[R]): Eff[m.Out, A] =
    recurse(effect)(new Recurser[Metrics, m.Out, A, A] {
      override def onPure(a: A): A = a

      override def onEffect[X](me: Metrics[X]): Either[X, Eff[m.Out, A]] =
        Left(matchEffect(me))

      override def onApplicative[X, T[_]: Traverse](ms: T[Metrics[X]]): Either[T[X], Metrics[T[X]]] =
        Left(ms.map(matchEffect))
    })(m)
}

object MetricsInterpretation extends MetricsInterpretation
