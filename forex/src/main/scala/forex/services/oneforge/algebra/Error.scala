package forex.services.oneforge.algebra

import scala.util.control.NoStackTrace

sealed abstract class Error(msg: String) extends Throwable(msg) with NoStackTrace
object Error {
  final case object Generic extends Error("Generic error")
  final case object NotFound extends Error("Not found")
  final case class ApiError(message: String) extends Error(message)
  final case class System(underlying: Throwable) extends Error(underlying.getMessage)
}
