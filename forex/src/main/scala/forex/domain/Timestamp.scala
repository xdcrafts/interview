package forex.domain

import java.time.{ Duration, Instant, OffsetDateTime, ZoneId }

import io.circe._
import io.circe.generic.extras.wrapped._
import io.circe.java8.time._

import scala.concurrent.duration.FiniteDuration

case class Timestamp(value: OffsetDateTime) extends AnyVal {
  def olderThan(duration: FiniteDuration): Boolean = Timestamp.olderThan(this, duration)
}

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  def fromSeconds(timestamp: Long) = Timestamp(
    OffsetDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
  )

  def olderThan(timestamp: Timestamp, duration: FiniteDuration): Boolean =
    timestamp.value.plus(Duration.ofNanos(duration.toNanos)).isBefore(OffsetDateTime.now())

  implicit val encoder: Encoder[Timestamp] =
    deriveUnwrappedEncoder[Timestamp]

  implicit val decoder: Decoder[Timestamp] =
    deriveUnwrappedDecoder[Timestamp]
}
