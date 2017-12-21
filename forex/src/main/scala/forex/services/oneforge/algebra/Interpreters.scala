package forex.services.oneforge.algebra

import forex.domain._
import forex.services.metrics._
import forex.services.oneforge.OneForgeCache
import monix.eval.Task
import org.atnos.eff._
import org.atnos.eff.addon.monix.task._
import org.xdcrafts.eff.addon.metrics.MetricsEffect._

import scala.concurrent.duration.FiniteDuration

object Interpreters {
  def dummy[R: _task: _metrics]: Algebra[Eff[R, ?]] = new DummyInterpreter[R]
  def live[R: _task: _metrics](
      invalidateAfter: FiniteDuration
  ): Live[R] = new LiveInterpreter[R](invalidateAfter)
}

final class DummyInterpreter[R: _task: _metrics] private[oneforge] extends Algebra[Eff[R, ?]] {
  override def get(pair: Rate.Pair): Eff[R, Error Either Rate] =
    for {
      result ← fromTask(Task.now(Rate(pair, Price(BigDecimal(100)), Timestamp.now)))
      _ ← meter(s"$biz.one-forge-dummy.get")
    } yield Right(result)
}

trait Live[R] extends Algebra[Eff[R, ?]] {
  def swap(updatedCache: Task[OneForgeCache]): Eff[R, Unit]
}

class LiveInterpreter[R: _task: _metrics] private[oneforge] (invalidateAfter: FiniteDuration) extends Live[R] {
  import scala.language.postfixOps

  @volatile private var cache: Task[OneForgeCache] = Task.now {
    Map[Rate.Pair, Rate]()
  }

  override def swap(updatedCache: Task[OneForgeCache]): Eff[R, Unit] =
    for {
      _ ← fromTask(Task.eval(this.cache = updatedCache))
      _ ← meter(s"$biz.one-forge-live.cache-update")
    } yield ()

  override def get(pair: Rate.Pair): Eff[R, Error Either Rate] = {
    val getPairTask: Task[Error Either Rate] = cache.attempt.map {
      _.left.map(Error.fromThrowable).flatMap {
        _.get(pair)
          .filter(!_.timestamp.olderThan(invalidateAfter))
          .toRight(Error.NotFound)
      }
    }
    for {
      pairOrError ← fromTask(getPairTask)
      _ ← pairOrError match {
        case Left(error) ⇒
          meter(s"$biz.one-forge-live.get") *>
            meter(s"$biz.one-forge-live.get.${pair.from}.${pair.to}") *>
            meter(s"$biz.one-forge-live.get.${error.getClass.getSimpleName.toLowerCase}") *>
            meter(s"$biz.one-forge-live.get.${pair.from}.${pair.to}.${error.getClass.getSimpleName.toLowerCase}")
        case Right(_) ⇒
          meter(s"$biz.one-forge-live.get") *>
            meter(s"$biz.one-forge-live.get.${pair.from}.${pair.to}")
      }
    } yield pairOrError
  }
}
