/*
 * Copyright (c) 2023 The National Archives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.gov.nationalarchives.omega.api.business.records

import cats.data.Validated.{ Invalid, Valid }
import uk.gov.nationalarchives.omega.api.messages.request.RequestByIdentifier
import uk.gov.nationalarchives.omega.api.repository.TestRepository
import uk.gov.nationalarchives.omega.api.repository.vocabulary.Cat
import uk.gov.nationalarchives.omega.api.support.UnitTest

class GetRecordServiceSpec extends UnitTest {

  private val testRepository = new TestRepository
  private val getRecordService = new GetRecordService(testRepository)

  "The GetRecordFullService" - {
    "process function returns" - {
      "a full record when given a valid concept URI" in {
        val recordRequest = RequestByIdentifier(s"${Cat.NS}COAL.2022.N373.P")
        val result = getRecordService.process(recordRequest)
        result mustBe
          Right(GetRecordReply(getExpectedRecord))
      }
    }
    "validateRequest function returns" - {
      "a valid result when the concept URI is valid" in {
        val message = getValidatedLocalMessage(s"""{
                                                  |    "identifier" : "${Cat.NS}COAL.2022.N373.P"
                                                  |}""".stripMargin)
        val result = getRecordService.validateRequest(message)
        result mustBe Valid(RequestByIdentifier(s"${Cat.NS}COAL.2022.N373.P"))
      }
      "an invalid result when the concept URI is not valid" in {
        val message = getValidatedLocalMessage(s"""{
                                                  |    "identifier" : "COAL.2022.N373.P"
                                                  |}""".stripMargin)
        val result = getRecordService.validateRequest(message)
        result mustBe a[Invalid[_]]
      }
    }
  }

  private def getExpectedRecord: String =
    s"""{
       |  "identifier" : "${Cat.NS}COAL.2022.N373.P",
       |  "type" : "Physical",
       |  "creator" : [
       |    "${Cat.NS}agent.24"
       |  ],
       |  "current-description" : "${Cat.NS}COAL.2022.N373.P.1",
       |  "description" : [
       |    {
       |      "identifier" : "${Cat.NS}COAL.2022.N373.P.2",
       |      "secondary-identifier" : [
       |        {
       |          "identifier" : "COAL 80/2052/9",
       |          "type" : "${Cat.NS}classicCatalogueReference"
       |        }
       |      ],
       |      "label" : "<scopecontent><p>Coal News albums 1963. Collection of contact prints of photographs taken by Deryk Wills. Photographs depicting: Banwen, Glamorgan. Banwen Miners Hunt. </p></scopecontent>",
       |      "abstract" : "<scopecontent><p>Coal News albums 1963. Collection of contact prints of photographs taken by Deryk Wills. Photographs depicting: Banwen, Glamorgan. Banwen Miners Hunt. </p></scopecontent>",
       |      "access-rights" : [
       |        "${Cat.NS}policy.Open_Description",
       |        "${Cat.NS}policy.Normal_Closure_before_FOI_Act_30_years_from_1963-12-31"
       |      ],
       |      "is-part-of" : [
       |        "${Cat.NS}recordset.COAL.2022.2834"
       |      ],
       |      "previous-sibling" : "${Cat.NS}COAL.2022.N3HQ.P.1",
       |      "version-timestamp" : "2023-08-30T12:10:00.000Z",
       |      "previous-description" : "${Cat.NS}COAL.2022.N373.P.1",
       |      "asset-legal-status" : {
       |        "identifier" : "${Cat.NS}public-record",
       |        "label" : "Public Record"
       |      },
       |      "legacy-tna-cs13-record-type" : "Item",
       |      "designation-of-edition" : "<unittitle type=\\\"Map Designation\\\">GSGS 2321</unittitle>",
       |      "created" : {
       |        "description" : "1963",
       |        "temporal" : {
       |          "date-from" : "1963-01-01Z",
       |          "date-to" : "1963-12-31Z"
       |        }
       |      },
       |      "archivists-note" : "[Grid reference: N/A]",
       |      "source-of-acquisition" : "${Cat.NS}agent.24",
       |      "custodial-history" : "Retained until 2006",
       |      "administrative-biographical-background" : "<bioghist><p>The board met periodically until 1935 when it was allowed to lapse.</p></bioghist>",
       |      "accumulation" : {
       |        "description" : "1963",
       |        "temporal" : {
       |          "date-from" : "1963-01-01Z",
       |          "date-to" : "1963-12-31Z"
       |        }
       |      },
       |      "appraisal" : "Files selected in accordance with Operational Selection Policy OSP 25",
       |      "accrual-policy" : "${Cat.NS}policy.Series_is_accruing",
       |      "layout" : "Photographs in an envelope",
       |      "publication-note" : "Some of the photographs in this series appeared in The Times newspaper.",
       |      "referenced-by" : [
       |        {
       |          "identifier" : "${Cat.NS}res.JN31",
       |          "label" : "Coal Board Minutes 1963"
       |        }
       |      ],
       |      "related-to" : [
       |        {
       |          "identifier" : "${Cat.NS}COAL.2022.S144",
       |          "label" : "Index of colliery photographs March 1963"
       |        }
       |      ],
       |      "separated-from" : [
       |        {
       |          "identifier" : "${Cat.NS}CAB.2022.L744",
       |          "label" : "NCB records 1963"
       |        }
       |      ],
       |      "subject" : [
       |        "${Cat.NS}agent.4N6",
       |        "${Cat.NS}agent.S7",
       |        {
       |          "identifier" : "${Cat.NS}agent.24",
       |          "label" : "from 1965"
       |        }
       |      ]
       |    },
       |    {
       |      "identifier" : "${Cat.NS}COAL.2022.N373.P.1",
       |      "secondary-identifier" : [
       |        {
       |          "identifier" : "COAL 80/2052/9",
       |          "type" : "${Cat.NS}classicCatalogueReference"
       |        }
       |      ],
       |      "label" : "<scopecontent><p>Coal News albums 1963. Collection of contact prints of photographs taken by Derick Wills. Photographs depicting: Banwen, Glamorgan. Banwen Miners Hunt. </p></scopecontent>",
       |      "abstract" : "<scopecontent><p>Coal News albums 1963. Collection of contact prints of photographs taken by Derick Wills. Photographs depicting: Banwen, Glamorgan. Banwen Miners Hunt. </p></scopecontent>",
       |      "access-rights" : [
       |        "${Cat.NS}policy.Open_Description",
       |        "${Cat.NS}policy.Normal_Closure_before_FOI_Act_30_years_from_1963-12-31"
       |      ],
       |      "is-part-of" : [
       |        "${Cat.NS}recordset.COAL.2022.2834"
       |      ],
       |      "previous-sibling" : "${Cat.NS}COAL.2022.N3HQ.P.1",
       |      "version-timestamp" : "2023-08-30T12:10:00.000Z",
       |      "previous-description" : "${Cat.NS}COAL.2022.N373.P.1",
       |      "asset-legal-status" : {
       |        "identifier" : "${Cat.NS}public-record",
       |        "label" : "Public Record"
       |      },
       |      "legacy-tna-cs13-record-type" : "Item",
       |      "created" : {
       |        "description" : "1963",
       |        "temporal" : {
       |          "instant" : "1963-01-01Z"
       |        }
       |      },
       |      "archivists-note" : "[Grid reference: NX 509 582]",
       |      "source-of-acquisition" : "${Cat.NS}agent.25",
       |      "custodial-history" : "Retained until 2001",
       |      "administrative-biographical-background" : "<bioghist><p>The board met periodically until 1936 when it was allowed to lapse.</p></bioghist>",
       |      "accumulation" : {
       |        "description" : "1963",
       |        "temporal" : {
       |          "instant" : "1963-01-01Z"
       |        }
       |      },
       |      "appraisal" : "Files selected in accordance with Operational Selection Policy OSP 26",
       |      "accrual-policy" : "${Cat.NS}policy.No_future_accruals_expected",
       |      "layout" : "Photographs in a box",
       |      "publication-note" : "Some of the photographs in this series appeared in The Manchester Guardian newspaper.",
       |      "referenced-by" : [
       |        {
       |          "identifier" : "${Cat.NS}res.4JJF",
       |          "label" : "Coal Board Minutes 1962"
       |        }
       |      ],
       |      "related-to" : [
       |        {
       |          "identifier" : "${Cat.NS}COAL.2022.G221",
       |          "label" : "Index of colliery photographs September 1963"
       |        }
       |      ],
       |      "separated-from" : [
       |        {
       |          "identifier" : "${Cat.NS}CAB.2022.N901",
       |          "label" : "Cabinet records 1963"
       |        }
       |      ]
       |    }
       |  ]
       |}""".stripMargin
}
