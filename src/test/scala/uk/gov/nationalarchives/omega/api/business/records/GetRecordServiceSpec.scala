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

import uk.gov.nationalarchives.omega.api.messages.request.RequestByIdentifier
import uk.gov.nationalarchives.omega.api.repository.{ BaseURL, TestRepository }
import uk.gov.nationalarchives.omega.api.support.UnitTest

class GetRecordServiceSpec extends UnitTest {

  private val testRepository = new TestRepository
  private val getRecordService = new GetRecordService(testRepository)

  "The GetRecordFullService" - {
    "returns a full record when given" - {
      "a valid concept URI" in {

        val recordRequest = RequestByIdentifier(s"${BaseURL.cat}/COAL.2022.N373.P")
        val result = getRecordService.process(recordRequest)
        result mustBe
          Right(GetRecordReply(getExpectedRecord))
      }
    }
  }

  private def getExpectedRecord: String =
    s"""{
       |  "identifier" : "${BaseURL.cat}/COAL.2022.N373.P",
       |  "type" : "Physical",
       |  "creator" : [
       |    "${BaseURL.cat}/agent.24"
       |  ],
       |  "current-description" : "${BaseURL.cat}/COAL.2022.N373.P.1",
       |  "description" : [
       |    {
       |      "identifier" : "${BaseURL.cat}/COAL.2022.N373.P.2",
       |      "secondary-identifier" : [
       |        {
       |          "identifier" : "COAL 80/2052/9",
       |          "type" : "${BaseURL.cat}/classicCatalogueReference"
       |        }
       |      ],
       |      "label" : "<scopecontent><p>Coal News albums 1963. Collection of contact prints of photographs taken by Deryk Wills. Photographs depicting: Banwen, Glamorgan. Banwen Miners Hunt. </p></scopecontent>",
       |      "abstract" : "<scopecontent><p>Coal News albums 1963. Collection of contact prints of photographs taken by Deryk Wills. Photographs depicting: Banwen, Glamorgan. Banwen Miners Hunt. </p></scopecontent>",
       |      "access-rights" : [
       |        "${BaseURL.cat}/policy.Open_Description",
       |        "${BaseURL.cat}/policy.Normal_Closure_before_FOI_Act_30_years_from_1963-12-31"
       |      ],
       |      "is-part-of" : [
       |        "${BaseURL.cat}/recordset.COAL.2022.2834"
       |      ],
       |      "previous-sibling" : "${BaseURL.cat}/COAL.2022.N3HQ.P.1",
       |      "version-timestamp" : "2023-08-30T12:10:00.000Z",
       |      "previous-description" : "${BaseURL.cat}/COAL.2022.N373.P.1",
       |      "asset-legal-status" : {
       |        "identifier" : "${BaseURL.cat}/public-record",
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
       |      "source-of-acquisition" : "${BaseURL.cat}/agent.24",
       |      "custodial-history" : "Retained until 2006",
       |      "administrative-biographical-background" : "<bioghist><p>The board met periodically until 1935 when it was allowed to lapse.</p></bioghist>"
       |    },
       |    {
       |      "identifier" : "${BaseURL.cat}/COAL.2022.N373.P.1",
       |      "secondary-identifier" : [
       |        {
       |          "identifier" : "COAL 80/2052/9",
       |          "type" : "${BaseURL.cat}/classicCatalogueReference"
       |        }
       |      ],
       |      "label" : "<scopecontent><p>Coal News albums 1963. Collection of contact prints of photographs taken by Derick Wills. Photographs depicting: Banwen, Glamorgan. Banwen Miners Hunt. </p></scopecontent>",
       |      "abstract" : "<scopecontent><p>Coal News albums 1963. Collection of contact prints of photographs taken by Derick Wills. Photographs depicting: Banwen, Glamorgan. Banwen Miners Hunt. </p></scopecontent>",
       |      "access-rights" : [
       |        "${BaseURL.cat}/policy.Open_Description",
       |        "${BaseURL.cat}/policy.Normal_Closure_before_FOI_Act_30_years_from_1963-12-31"
       |      ],
       |      "is-part-of" : [
       |        "${BaseURL.cat}/recordset.COAL.2022.2834"
       |      ],
       |      "previous-sibling" : "${BaseURL.cat}/COAL.2022.N3HQ.P.1",
       |      "version-timestamp" : "2023-08-30T12:10:00.000Z",
       |      "previous-description" : "${BaseURL.cat}/COAL.2022.N373.P.1",
       |      "asset-legal-status" : {
       |        "identifier" : "${BaseURL.cat}/public-record",
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
       |      "source-of-acquisition" : "${BaseURL.cat}/agent.25",
       |      "custodial-history" : "Retained until 2001",
       |      "administrative-biographical-background" : "<bioghist><p>The board met periodically until 1936 when it was allowed to lapse.</p></bioghist>"
       |    }
       |  ]
       |}""".stripMargin
}
