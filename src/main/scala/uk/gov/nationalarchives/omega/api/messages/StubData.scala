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

package uk.gov.nationalarchives.omega.api.messages

import org.apache.jena.ext.xerces.util.URI
import uk.gov.nationalarchives.omega.api.messages.reply.{ AgentDescription, AgentSummary }
import uk.gov.nationalarchives.omega.api.repository.model.{ AgentConceptEntity, AgentDescriptionEntity, LegalStatusEntity }

import java.time.ZonedDateTime

class StubDataImpl extends StubData

trait StubData {

  def getLegalStatusEntities: Seq[LegalStatusEntity] = Seq(
    LegalStatusEntity(new URI("http://cat.nationalarchives.gov.uk/public-record"), "Public Record"),
    LegalStatusEntity(new URI("http://cat.nationalarchives.gov.uk/non-public-record"), "Non-Public Record"),
    LegalStatusEntity(
      new URI("http://cat.nationalarchives.gov.uk/public-record-unless-otherwise-stated"),
      "Public Record (unless otherwise stated)"
    ),
    LegalStatusEntity(new URI("http://cat.nationalarchives.gov.uk/welsh-public-record"), "Welsh Public Record"),
    LegalStatusEntity(new URI("http://cat.nationalarchives.gov.uk/non-record-material"), "Non-Record Material")
  )

  def getAgentSummaries: Seq[AgentSummary] = Seq(
    AgentSummary(
      AgentType.Person,
      "3RX",
      "current description",
      List(
        AgentDescription(
          "3RX",
          "Abbot, Charles",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1798"),
          Some("1867")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "48N",
      "current description",
      List(
        AgentDescription(
          "48N",
          "Baden-Powell, Lady Olave St Clair",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1889"),
          Some("1977")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "39K",
      "current description",
      List(
        AgentDescription(
          "39K",
          "Cannon, John Francis Michael",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1930"),
          None
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "3FH",
      "current description",
      List(
        AgentDescription(
          "3FH",
          "Dainton, Sir Frederick Sydney",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1914"),
          Some("1997")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "54J",
      "current description",
      List(
        AgentDescription(
          "54J",
          "Edward, ",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1330"),
          Some("1376")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "2QX",
      "current description",
      List(
        AgentDescription(
          "2QX",
          "Edward VII",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1841"),
          Some("1910")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "561",
      "current description",
      List(
        AgentDescription(
          "561",
          "Fanshawe, Baron, of Richmond, ",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "46F",
      "current description",
      List(
        AgentDescription(
          "46F",
          "Fawkes, Guy",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1570"),
          Some("1606")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "2JN",
      "current description",
      List(
        AgentDescription(
          "2JN",
          "George, David Lloyd",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1863"),
          Some("1945")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "34X",
      "current description",
      List(
        AgentDescription(
          "34X",
          "Halley, Edmund",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1656"),
          Some("1742")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "2TK",
      "current description",
      List(
        AgentDescription(
          "2TK",
          "Halifax, ",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "39T",
      "current description",
      List(
        AgentDescription(
          "39T",
          "Irvine, Linda Mary",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1928"),
          None
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "4",
      "current description",
      List(
        AgentDescription(
          "4",
          "Jack the Ripper, ",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1888"),
          Some("1888")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "4FF",
      "current description",
      List(
        AgentDescription(
          "4FF",
          "Keay, Sir Lancelot Herman",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1883"),
          Some("1974")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "ST",
      "current description",
      List(
        AgentDescription(
          "ST",
          "Lawson, Nigel",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1932"),
          None
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "51X",
      "current description",
      List(
        AgentDescription(
          "51X",
          "Macpherson, Sir William (Alan)",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1926"),
          None
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "515",
      "current description",
      List(
        AgentDescription(
          "515",
          "Newcastle, 1st Duke of, ",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "4VF",
      "current description",
      List(
        AgentDescription(
          "4VF",
          "Old Pretender, The",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "4H3",
      "current description",
      List(
        AgentDescription(
          "4H3",
          "Oliphant, Sir Mark Marcus Laurence Elwin",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1901"),
          Some("2000")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "46W",
      "current description",
      List(
        AgentDescription(
          "46W",
          "Paine, Thomas",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1737"),
          Some("1809")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "3SH",
      "current description",
      List(
        AgentDescription(
          "3SH",
          "Reade, Hubert Granville Revell",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1859"),
          Some("1938")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "2TF",
      "current description",
      List(
        AgentDescription(
          "2TF",
          "Reading, ",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "53T",
      "current description",
      List(
        AgentDescription(
          "53T",
          "Salisbury, Sir Edward James",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1886"),
          Some("1978")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "3QL",
      "current description",
      List(
        AgentDescription(
          "3QL",
          "Tate, Sir Henry",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1819"),
          Some("1899")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "37K",
      "current description",
      List(
        AgentDescription(
          "37K",
          "Uvarov, Sir Boris Petrovitch",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1889"),
          Some("1970")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "2T1",
      "current description",
      List(
        AgentDescription(
          "2T1",
          "Vane-Tempest-Stewart, Charles Stewart",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1852"),
          Some("1915")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "4RW",
      "current description",
      List(
        AgentDescription(
          "4RW",
          "Victor Amadeus, ",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1666"),
          Some("1732")
        )
      )
    ),
    AgentSummary(
      AgentType.Person,
      "3GY",
      "current description",
      List(
        AgentDescription(
          "3GY",
          "Victoria, ",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1819"),
          Some("1901")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "RR6",
      "current description",
      List(
        AgentDescription(
          "RR6",
          "100th (Gordon Highlanders) Regiment of Foot",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1794"),
          Some("1794")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "S34",
      "current description",
      List(
        AgentDescription(
          "S34",
          "1st Regiment of Foot or Royal Scots",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1812"),
          Some("1812")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "87K",
      "current description",
      List(
        AgentDescription(
          "87K",
          "Abbotsbury Railway Company",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1877"),
          Some("1877")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VWG",
      "current description",
      List(
        AgentDescription(
          "VWG",
          "Accountant General in the Court of Chancery",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1726"),
          Some("1726")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "LWY",
      "current description",
      List(
        AgentDescription(
          "LWY",
          "Admiralty Administrative Whitley Council, General Purposes Committee",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1942"),
          Some("1942")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VS6",
      "current description",
      List(
        AgentDescription(
          "VS6",
          "Advisory Committee on Animal Feedingstuffs",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1999"),
          Some("1999")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "CC",
      "current description",
      List(
        AgentDescription(
          "CC",
          "Bank of England",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1694"),
          Some("1694")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "N9S",
      "current description",
      List(
        AgentDescription(
          "N9S",
          "Bank on Tickets of the Million Adventure",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1695"),
          Some("1695")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "JS8",
      "current description",
      List(
        AgentDescription(
          "JS8",
          "BBC",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "8WG",
      "current description",
      List(
        AgentDescription(
          "8WG",
          "Bee Husbandry Committee",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1959"),
          Some("1959")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "6VQ",
      "current description",
      List(
        AgentDescription(
          "6VQ",
          "Cabinet",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1919"),
          Some("1919")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "SV",
      "current description",
      List(
        AgentDescription(
          "SV",
          "Cabinet",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1945"),
          Some("1945")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "5V4",
      "current description",
      List(
        AgentDescription(
          "5V4",
          "Cabinet, Committee for Control of Official Histories",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1946"),
          Some("1946")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "GW5",
      "current description",
      List(
        AgentDescription(
          "GW5",
          "Cattle Emergency Committee",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1934"),
          Some("1934")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "934",
      "current description",
      List(
        AgentDescription(
          "934",
          "Dairy Crest",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1981"),
          Some("1981")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "9HC",
      "current description",
      List(
        AgentDescription(
          "9HC",
          "Dean of the Chapel Royal",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "WGL",
      "current description",
      List(
        AgentDescription(
          "WGL",
          "Department for Environment, Food and Rural Affairs, Water Quality Division",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("2002"),
          Some("2002")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "WJ4",
      "current description",
      List(
        AgentDescription(
          "WJ4",
          "Department for Exiting the European Union",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("2016"),
          Some("2016")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "9YJ",
      "current description",
      List(
        AgentDescription(
          "9YJ",
          "East Grinstead, Groombridge and Tunbridge Wells Railway Company",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1862"),
          Some("1862")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "HF4",
      "current description",
      List(
        AgentDescription(
          "HF4",
          "East India Company",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1600"),
          Some("1600")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "WN3",
      "current description",
      List(
        AgentDescription(
          "WN3",
          "Education and Skills Funding Agency",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("2017"),
          Some("2017")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "WNL",
      "current description",
      List(
        AgentDescription(
          "WNL",
          "Education and Skills Funding Agency",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("2017"),
          Some("2017")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "Q1R",
      "current description",
      List(
        AgentDescription(
          "Q1R",
          "Falkland Islands Company",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1899"),
          Some("1899")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "SQ9",
      "current description",
      List(
        AgentDescription(
          "SQ9",
          "Fish's Corps of Foot",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1782"),
          Some("1782")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "R6R",
      "current description",
      List(
        AgentDescription(
          "R6R",
          "Foreign and Commonwealth Office, Consulate, Dusseldorf, West Germany",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1968"),
          Some("1968")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "HKL",
      "current description",
      List(
        AgentDescription(
          "HKL",
          "Foreign Office, Consulate, Angora and Konieh, Ottoman Empire",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1895"),
          Some("1895")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "KSC",
      "current description",
      List(
        AgentDescription(
          "KSC",
          "Gaming Board for Great Britain",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1968"),
          Some("1968")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "73R",
      "current description",
      List(
        AgentDescription(
          "73R",
          "GCHQ",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VR1",
      "current description",
      List(
        AgentDescription(
          "VR1",
          "Geffrye Museum",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1914"),
          Some("1914")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "QX5",
      "current description",
      List(
        AgentDescription(
          "QX5",
          "General Nursing Council for England and Wales, Registration and Enrolment Committee",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1970"),
          Some("1970")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "C1Y",
      "current description",
      List(
        AgentDescription(
          "C1Y",
          "Halifax High Level and North and South Junction Railway Company",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1884"),
          Some("1884")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "W2T",
      "current description",
      List(
        AgentDescription(
          "W2T",
          "Hansard Society",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1944"),
          Some("1944")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "F18",
      "current description",
      List(
        AgentDescription(
          "F18",
          "Health and Safety Commission, Health and Safety Executive, Employment Medical Advisory Service",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1975"),
          Some("1975")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "8JK",
      "current description",
      List(
        AgentDescription(
          "8JK",
          "Her Majesty's Stationery Office",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1986"),
          Some("1986")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "9FV",
      "current description",
      List(
        AgentDescription(
          "9FV",
          "Ideal Benefit Society",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1912"),
          Some("1912")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "5YX",
      "current description",
      List(
        AgentDescription(
          "5YX",
          "Imperial War Museum: Churchill Museum and Cabinet War Rooms",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1939"),
          Some("1939")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "W1Q",
      "current description",
      List(
        AgentDescription(
          "W1Q",
          "Independent Expert Group on Mobile Phones",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1999"),
          Some("1999")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "QLY",
      "current description",
      List(
        AgentDescription(
          "QLY",
          "Independent Expert Group on Mobile Phones",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1999"),
          Some("1999")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "LS5",
      "current description",
      List(
        AgentDescription(
          "LS5",
          "Jodrell Bank Observatory, Cheshire",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1955"),
          Some("1955")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "92W",
      "current description",
      List(
        AgentDescription(
          "92W",
          "Joint Milk Quality Committee",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1948"),
          Some("1948")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "L3W",
      "current description",
      List(
        AgentDescription(
          "L3W",
          "Justices in Eyre, of Assize, of Gaol Delivery, of Oyer and Terminer, of the Peace, and of Nisi Prius",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "N8X",
      "current description",
      List(
        AgentDescription(
          "N8X",
          "Justices of the Forest",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1166"),
          Some("1166")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "THY",
      "current description",
      List(
        AgentDescription(
          "THY",
          "Kew Gardens Archive",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "SGX",
      "current description",
      List(
        AgentDescription(
          "SGX",
          "King's Own Dragoons, 1751-1818",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "CCR",
      "current description",
      List(
        AgentDescription(
          "CCR",
          "Knitting, Lace and Net Industry Training Board",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1966"),
          Some("1966")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TTT",
      "current description",
      List(
        AgentDescription(
          "TTT",
          "King's Volunteers Regiment of Foot, 1761-1763",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VR7",
      "current description",
      List(
        AgentDescription(
          "VR7",
          "Lady Lever Art Gallery",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1922"),
          Some("1922")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "XQ",
      "current description",
      List(
        AgentDescription(
          "XQ",
          "Law Society",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1825"),
          Some("1825")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "91W",
      "current description",
      List(
        AgentDescription(
          "91W",
          "League of Mercy",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1898"),
          Some("1898")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VX",
      "current description",
      List(
        AgentDescription(
          "VX",
          "Legal Aid Board",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1989"),
          Some("1989")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TXG",
      "current description",
      List(
        AgentDescription(
          "TXG",
          "Legal Aid Board, 1988-1989",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "6LL",
      "current description",
      List(
        AgentDescription(
          "6LL",
          "Machinery of Government Committee",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1917"),
          Some("1917")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "G6N",
      "current description",
      List(
        AgentDescription(
          "G6N",
          "Magnetic Department",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1839"),
          Some("1839")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "71K",
      "current description",
      List(
        AgentDescription(
          "71K",
          "Manpower Distribution Board",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1916"),
          Some("1916")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "KN1",
      "current description",
      List(
        AgentDescription(
          "KN1",
          "Master of the Rolls Archives Committee",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1925"),
          Some("1925")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "J6X",
      "current description",
      List(
        AgentDescription(
          "J6X",
          "National Agricultural Advisory Service, Great House Experimental Husbandry Farm",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1951"),
          Some("1951")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "K7N",
      "current description",
      List(
        AgentDescription(
          "K7N",
          "National Air Traffic Control Services, Director General Projects and Engineering, Directorate of Projects",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1963"),
          Some("1963")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TSL",
      "current description",
      List(
        AgentDescription(
          "TSL",
          "National Archives, The",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "LSN",
      "current description",
      List(
        AgentDescription(
          "LSN",
          "Navy Board, Transport Branch, Prisoner of War Department",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1817"),
          Some("1817")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "W1S",
      "current description",
      List(
        AgentDescription(
          "W1S",
          "Office for Budget Responsibility",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("2010"),
          Some("2010")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "N4W",
      "current description",
      List(
        AgentDescription(
          "N4W",
          "Office of Population Censuses and Surveys, Computer Division",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1972"),
          Some("1972")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "QQC",
      "current description",
      List(
        AgentDescription(
          "QQC",
          "Office of Works, Directorate of Works, Maintenance Surveyors Division, Sanitary Engineers Section",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1928"),
          Some("1928")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "QFY",
      "current description",
      List(
        AgentDescription(
          "QFY",
          "Office of the President of Social Security Appeal Tribunals, Medical Appeal Tribunals and Vaccine Damage Tribunals",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1984"),
          Some("1984")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VYJ",
      "current description",
      List(
        AgentDescription(
          "VYJ",
          "Ordnance Survey of Great Britain, Directorate of Data Collection and Management",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("2003"),
          Some("2003")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "8FX",
      "current description",
      List(
        AgentDescription(
          "8FX",
          "Overseas Development Administration, Information Department",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1970"),
          Some("1970")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "3C",
      "current description",
      List(
        AgentDescription(
          "3C",
          "Overseas Finance, International Finance, IF1 International Financial Institutions and Debt Division",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1990"),
          Some("1990")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "988",
      "current description",
      List(
        AgentDescription(
          "988",
          "Oxford University Archives",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1634"),
          Some("1634")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TWX",
      "current description",
      List(
        AgentDescription(
          "TWX",
          "Oxford University Press",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1633"),
          Some("1633")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "79L",
      "current description",
      List(
        AgentDescription(
          "79L",
          "Palace Court",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1660"),
          Some("1660")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TX6",
      "current description",
      List(
        AgentDescription(
          "TX6",
          "Parker Inquiry",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VY4",
      "current description",
      List(
        AgentDescription(
          "VY4",
          "Paymaster General of the Court of Chancery, Supreme Court Pay Office",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1884"),
          Some("1884")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VX3",
      "current description",
      List(
        AgentDescription(
          "VX3",
          "Persona Associates Ltd",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1989"),
          Some("1989")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "V36",
      "current description",
      List(
        AgentDescription(
          "V36",
          "Petty Bag Office",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "8R6",
      "current description",
      List(
        AgentDescription(
          "8R6",
          "Queen Anne's Bounty",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "SH2",
      "current description",
      List(
        AgentDescription(
          "SH2",
          "Queen's Own Dragoons, 1788-1818",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "79X",
      "current description",
      List(
        AgentDescription(
          "79X",
          "Queens Prison",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1842"),
          Some("1842")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "W91",
      "current description",
      List(
        AgentDescription(
          "W91",
          "Queen's Printer for Scotland",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1999"),
          Some("1999")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "F11",
      "current description",
      List(
        AgentDescription(
          "F11",
          "Radioactive Substances Advisory Committee",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1948"),
          Some("1948")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "CYY",
      "current description",
      List(
        AgentDescription(
          "CYY",
          "Railway Executive",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1947"),
          Some("1947")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "CXY",
      "current description",
      List(
        AgentDescription(
          "CXY",
          "Railway Executive",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1914"),
          Some("1914")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "CY1",
      "current description",
      List(
        AgentDescription(
          "CY1",
          "Railway Executive",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1939"),
          Some("1939")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TXH",
      "current description",
      List(
        AgentDescription(
          "TXH",
          "SaBRE",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("2002"),
          Some("2002")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "739",
      "current description",
      List(
        AgentDescription(
          "739",
          "Scaccarium Superius",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "NWN",
      "current description",
      List(
        AgentDescription(
          "NWN",
          "School of Anti-Aircraft Artillery",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1942"),
          Some("1942")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "SGS",
      "current description",
      List(
        AgentDescription(
          "SGS",
          "Scots Greys, 1877-1921",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VXR",
      "current description",
      List(
        AgentDescription(
          "VXR",
          "Takeover Panel",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "QQR",
      "current description",
      List(
        AgentDescription(
          "QQR",
          "Tate Gallery",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1897"),
          Some("1897")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "63K",
      "current description",
      List(
        AgentDescription(
          "63K",
          "Tate Gallery Archive",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1970"),
          Some("1970")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "G91",
      "current description",
      List(
        AgentDescription(
          "G91",
          "Thalidomide Y List Inquiry",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1978"),
          Some("1978")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "FKS",
      "current description",
      List(
        AgentDescription(
          "FKS",
          "The Buying Agency",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1991"),
          Some("1991")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "JLC",
      "current description",
      List(
        AgentDescription(
          "JLC",
          "The Crown Estate, Other Urban Estates, Foreshore and Seabed Branches",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1973"),
          Some("1973")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "SYL",
      "current description",
      List(
        AgentDescription(
          "SYL",
          "Uhlans Britanniques de Sainte-Domingue (Charmilly's), 1794-1795",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TXK",
      "current description",
      List(
        AgentDescription(
          "TXK",
          "UK Passport Service",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1991"),
          Some("1991")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "V3H",
      "current description",
      List(
        AgentDescription(
          "V3H",
          "UK Web Archiving Consortium",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "CCX",
      "current description",
      List(
        AgentDescription(
          "CCX",
          "United Kingdom Atomic Energy Authority, Atomic Weapons Research Establishment, Directors Office",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1954"),
          Some("1954")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VTY",
      "current description",
      List(
        AgentDescription(
          "VTY",
          "Valuation Office Agency",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1991"),
          Some("19910")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "9HJ",
      "current description",
      List(
        AgentDescription(
          "9HJ",
          "Venetian Republic",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("727"),
          Some("727")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "QYF",
      "current description",
      List(
        AgentDescription(
          "QYF",
          "Victoria and Albert Museum",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1857"),
          Some("1857")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "61H",
      "current description",
      List(
        AgentDescription(
          "61H",
          "Victoria & Albert Museum, Archive of Art and Design",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1992"),
          Some("1992")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "W9K",
      "current description",
      List(
        AgentDescription(
          "W9K",
          "Wales Tourist Board",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1969"),
          Some("1969")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VRG",
      "current description",
      List(
        AgentDescription(
          "VRG",
          "Walker Art Gallery",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1873"),
          Some("1873")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "61J",
      "current description",
      List(
        AgentDescription(
          "61J",
          "Wallace Collection",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1897"),
          Some("1897")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "HXV",
      "current description",
      List(
        AgentDescription(
          "HXV",
          "War and Colonial Department, Commissioners for liquidating the Danish and Dutch loans for St Thomas and St John",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1808"),
          Some("1808")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "V2R",
      "current description",
      List(
        AgentDescription(
          "V2R",
          "Zahid Mubarek Inquiry",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("2004"),
          Some("2004")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "763",
      "current description",
      List(
        AgentDescription(
          "763",
          "Zambia Department, Commonwealth Office",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1967"),
          Some("1967")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "765",
      "current description",
      List(
        AgentDescription(
          "765",
          "Zambia, Malawi and Southern Africa Department",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1968"),
          Some("1968")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "G2Y",
      "current description",
      List(
        AgentDescription(
          "G2Y",
          "Zuckerman Working Party",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          None,
          None
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "63F",
      "current description",
      List(
        AgentDescription(
          "63F",
          "British Museum Central Archive",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("2001"),
          Some("2001")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "614",
      "current description",
      List(
        AgentDescription(
          "614",
          "British Library, Sound Archive",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("1983"),
          Some("1983")
        )
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "S2",
      "current description",
      List(
        AgentDescription(
          "S2",
          "The National Archives",
          ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
          Some(false),
          Some(false),
          Some("2003"),
          None
        )
      )
    )
  )

  def getAgentSummaryEntities: List[AgentConceptEntity] =
    List(
      AgentConceptEntity(
        new URI("http://cat.nationalarchives.gov.uk/8R6"),
        new URI("http://cat.nationalarchives.gov.uk/corporate-body-concept"),
        new URI("http://cat.nationalarchives.gov.uk/agent.8R6.1")
      )
    )

  def getAgentDescriptionEntities: List[AgentDescriptionEntity] =
    List(
      AgentDescriptionEntity(
        new URI("http://cat.nationalarchives.gov.uk/agent.8R6.1"),
        "Queen Anne's Bounty",
        ZonedDateTime.parse("2022-06-22T02:00:00-05:00"),
        None,
        None,
        Some(false),
        None
      )
    )
}
