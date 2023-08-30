package uk.gov.nationalarchives.omega.api.business.records

import uk.gov.nationalarchives.omega.api.business.BusinessServiceError
import uk.gov.nationalarchives.omega.api.common.ErrorCode

case class GetRecordFullError(message: String) extends BusinessServiceError {

  override def code: ErrorCode = ErrorCode.PROC002

  override def description: String = "There was an error"
}
