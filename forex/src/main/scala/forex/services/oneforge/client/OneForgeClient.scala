package forex.services.oneforge.client

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.http.scaladsl.unmarshalling.Unmarshal
import forex.config.{ ApplicationConfig, OneForgeConfig }
import forex.main.ActorSystems
import forex.services.oneforge._
import forex.services.oneforge.algebra.Error.ApiError
import forex.services.oneforge.client.OneForgeClientUtils.OneForgeQuotaResponse
import io.circe.Decoder
import monix.eval.Task
import org.zalando.grafter.macros.{ defaultReader, readerOf }

@defaultReader[AkkaOneForgeClient]
trait OneForgeClient {
  def getOneForgeRates: Task[OneForgeCache]
  def getQuota: Task[OneForgeQuotaResponse]
}

@readerOf[ApplicationConfig]
case class AkkaOneForgeClient(
    config: OneForgeConfig,
    actorSystems: ActorSystems
) extends OneForgeClient {
  import OneForgeClientUtils._
  import actorSystems._

  private lazy val oneForgeQuotaRequestString =
    s"https://forex.1forge.com/1.0.2/quota?api_key=${config.apiKey}"

  private lazy val oneForgeRatesRequestString =
    s"https://forex.1forge.com/1.0.2/quotes?" +
      s"pairs=${supportedPairs.map(pairToCode).mkString(",")}&" +
      s"api_key=${config.apiKey}"

  private def oneForgeRequestTask[Response: Decoder](uri: String): Task[Response] =
    Task
      .deferFuture(Http().singleRequest(HttpRequest(uri = uri)))
      .flatMap {
        case r @ HttpResponse(StatusCodes.OK, _, _, _) ⇒
          Task
            .deferFuture(Unmarshal(r.entity).to[OneForgeErrorResponse Either Response])
            .flatMap {
              case Left(error)  ⇒ Task.raiseError(ApiError(error.message))
              case Right(value) ⇒ Task.now(value)
            }
        case _ ⇒
          Task.raiseError(ApiError("OneForge API returned non 200 response"))
      }

  override def getOneForgeRates: Task[OneForgeCache] =
    oneForgeRequestTask[OneForgeRatesResponse](oneForgeRatesRequestString).map {
      _.values
        .map(oneForgeRateToRate)
        .map(v ⇒ v.pair → v)
        .toMap
    }

  override def getQuota: Task[OneForgeQuotaResponse] =
    oneForgeRequestTask[OneForgeQuotaResponse](oneForgeQuotaRequestString)
}
