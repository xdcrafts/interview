package forex.interfaces.api.utils

import akka.http.scaladsl._
import akka.http.scaladsl.model.{ HttpResponse, StatusCode }
import akka.http.scaladsl.server.Route
import forex.processes._

object ApiExceptionHandler {
  import akka.http.scaladsl.model.StatusCodes._

  def completeWithError(statusCode: StatusCode, message: String): Route =
    _.complete(HttpResponse(status = statusCode, entity = message))

  def apply(): server.ExceptionHandler =
    server.ExceptionHandler {
      case RatesError.Generic ⇒
        completeWithError(InternalServerError, "Something went wrong in the rates process")
      case RatesError.NotFound ⇒
        completeWithError(NotFound, "Conversion rate not found")
      case RatesError.ApiError(msg) ⇒
        completeWithError(InternalServerError, s"OneForge API responded with error: $msg")
      case RatesError.System(throwable) ⇒
        completeWithError(InternalServerError, s"Failed to process request: ${throwable.getMessage}")
      case throwable: Throwable ⇒
        completeWithError(InternalServerError, s"Unexpected failure: $throwable")
    }

}
