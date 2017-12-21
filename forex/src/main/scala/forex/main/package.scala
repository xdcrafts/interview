package forex

import forex.services.oneforge.components.OneForgeLive
import monix.eval.Task
import org.atnos.eff._
import org.xdcrafts.eff.addon.metrics.Metrics
import org.zalando.grafter._

package object main {

  type OneForgeImpl = OneForgeLive
  type AppStack = Fx.fx2[Task, Metrics]
  type AppEffect[R] = Eff[AppStack, R]

  def toStartErrorString(results: List[StartResult]): String =
    results
      .collect {
        case StartError(message, ex) ⇒ s"$message: ${ex.getMessage}"
        case StartFailure(message)   ⇒ message
      }
      .mkString("Application startup failed. Modules: [", ", ", "]")

  def toStartSuccessString(results: List[StartResult]): String =
    results
      .collect {
        case StartOk(message) ⇒ message
      }
      .mkString("Application startup successful. Modules: [", ", ", "]")

  def toStopString(results: List[StopResult]): String = {
    val okString = results
      .filter(_.success)
      .collect {
        case StopOk(message) ⇒ message
      }
      .mkString("- Successfully stopped modules: [", ", ", "]")
    val failedString = results
      .filter(!_.success)
      .collect {
        case StopError(message, ex) ⇒ s"$message: ${ex.getMessage}"
        case StopFailure(message)   ⇒ message
      }
      .mkString("- Failed to stop modules: [", ", ", "]")
    s"Application stopped.\n$okString\n$failedString"
  }
}
