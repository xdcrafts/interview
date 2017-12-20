package forex.services.oneforge.client

import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport
import forex.domain.Rate.Pair
import forex.domain.{ Currency, Price, Rate, Timestamp }
import io.circe._
import io.circe.generic.extras.wrapped._
import io.circe.generic.semiauto._

object OneForgeClientUtils extends ErrorAccumulatingCirceSupport {

  final case class OneForgeErrorResponse(error: Boolean, message: String)
  final case class OneForgeRate(symbol: String, price: Price, timestamp: Long)
  final case class OneForgeRatesResponse(values: List[OneForgeRate]) extends AnyVal
  final case class OneForgeQuotaResponse(quotaUsed: Int, quotaLimit: Int, quotaRemaining: Int, hoursUntilReset: Int)

  implicit val _oneForgeRateDecoder: Decoder[OneForgeRate] = deriveDecoder

  implicit val _oneForgeOkResponseDecoder: Decoder[OneForgeRatesResponse] = deriveUnwrappedDecoder

  implicit val _oneForgeErrorResponseDecoder: Decoder[OneForgeErrorResponse] = deriveDecoder

  implicit val _oneForgeQuotaResponse: Decoder[OneForgeQuotaResponse] = Decoder
    .forProduct4(
      "quota_used",
      "quota_limit",
      "quota_remaining",
      "hours_until_reset"
    )(OneForgeQuotaResponse)

  implicit def _eitherDecoder[A, B](implicit a: Decoder[A], b: Decoder[B]): Decoder[Either[A, B]] = {
    val l: Decoder[Either[A, B]] = a.map(Left.apply)
    val r: Decoder[Either[A, B]] = b.map(Right.apply)
    l or r
  }

  def pairToCode(pair: Pair): String = pair.from.toString + pair.to.toString

  def pairFromCode(stringCode: String): Pair = {
    if (stringCode.length != 6) {
      throw new IllegalArgumentException(
        "Expected code that contains two three-character currency identifiers, like 'usdeur'."
      )
    }
    Pair(Currency.fromString(stringCode.substring(0, 3)), Currency.fromString(stringCode.substring(3, 6)))
  }

  val supportedPairs: List[Rate.Pair] =
    for {
      x ← Currency.values
      y ← Currency.values
      if y != x
    } yield Rate.Pair(x, y)

  def oneForgeRateToRate(rate: OneForgeRate) =
    Rate(pairFromCode(rate.symbol), rate.price, Timestamp.fromSeconds(rate.timestamp))
}
