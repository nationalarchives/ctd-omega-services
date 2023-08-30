package uk.gov.nationalarchives.omega.api.messages.request

import io.circe.Decoder

case class GetRecordFull(identifier: String) extends RequestMessage
object GetRecordFull {
  implicit val decodeGetRecordFull: Decoder[GetRecordFull] = json =>
    for {
      identifier <- json.get[String]("identifier")
    } yield GetRecordFull(identifier)

}
