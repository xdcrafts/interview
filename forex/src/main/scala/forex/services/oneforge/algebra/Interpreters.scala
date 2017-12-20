package forex.services.oneforge.algebra

import forex.domain._
import forex.services.oneforge.OneForgeCache
import monix.eval.Task
import org.atnos.eff._
import org.atnos.eff.addon.monix.task._

import scala.concurrent.duration.FiniteDuration

object Interpreters {
  def dummy[R: _task]: Algebra[Eff[R, ?]] = new DummyInterpreter[R]
  def live[R: _task](
      invalidateAfter: FiniteDuration
  ): Live[R] = new LiveInterpreter[R](invalidateAfter)
}

final class DummyInterpreter[R: _task] private[oneforge] extends Algebra[Eff[R, ?]] {
  override def get(pair: Rate.Pair): Eff[R, Error Either Rate] =
    for {
      result ← fromTask(Task.now(Rate(pair, Price(BigDecimal(100)), Timestamp.now)))
    } yield Right(result)
}

trait Live[R] extends Algebra[Eff[R, ?]] {
  def swap(updatedCache: Task[OneForgeCache]): Eff[R, Unit]
}

class LiveInterpreter[R: _task] private[oneforge] (invalidateAfter: FiniteDuration) extends Live[R] {

  @volatile private var cache: Task[OneForgeCache] = Task.now {
    Map[Rate.Pair, Rate]()
  }

  override def swap(updatedCache: Task[OneForgeCache]): Eff[R, Unit] = fromTask {
    Task.now(this.cache = updatedCache)
  }

  override def get(pair: Rate.Pair): Eff[R, Error Either Rate] = fromTask {
    cache.map {
      _.get(pair)
        .filter(!_.timestamp.olderThan(invalidateAfter))
        .toRight(Error.NotFound)
    }
  }
}
