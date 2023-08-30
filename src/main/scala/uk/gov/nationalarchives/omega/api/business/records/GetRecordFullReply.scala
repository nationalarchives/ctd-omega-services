package uk.gov.nationalarchives.omega.api.business.records

import uk.gov.nationalarchives.omega.api.business.BusinessServiceReply

case class GetRecordFullReply(override val content: String) extends BusinessServiceReply
