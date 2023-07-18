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
import uk.gov.nationalarchives.omega.api.messages.reply.{ AgentDescription, AgentSummary, LegalStatus }
import uk.gov.nationalarchives.omega.api.repository.model.AgentEntity

class StubDataImpl extends StubData

trait StubData {

  def getLegalStatuses(): Seq[LegalStatus] = Seq(
    LegalStatus(new URI("http://catalogue.nationalarchives.gov.uk/public-record"), "Public Record"),
    LegalStatus(new URI("http://catalogue.nationalarchives.gov.uk/non-public-record"), "Non-Public Record"),
    LegalStatus(
      new URI("http://catalogue.nationalarchives.gov.uk/public-record-unless-otherwise-stated"),
      "Public Record (unless otherwise stated)"
    ),
    LegalStatus(new URI("http://catalogue.nationalarchives.gov.uk/welsh-public-record"), "Welsh Public Record"),
    LegalStatus(new URI("http://catalogue.nationalarchives.gov.uk/non-record-material"), "Non-Record Material")
  )

  def getAgentSummaries(): Seq[AgentSummary] = Seq(
    AgentSummary(
      AgentType.Person,
      "3RX",
      "current description",
      AgentDescription(
        "3RX",
        "Abbot, Charles",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1798"),
        Some("1867")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "48N",
      "current description",
      AgentDescription(
        "48N",
        "Baden-Powell, Lady Olave St Clair",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1889"),
        Some("1977")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "39K",
      "current description",
      AgentDescription(
        "39K",
        "Cannon, John Francis Michael",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1930"),
        None
      )
    ),
    AgentSummary(
      AgentType.Person,
      "3FH",
      "current description",
      AgentDescription(
        "3FH",
        "Dainton, Sir Frederick Sydney",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1914"),
        Some("1997")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "54J",
      "current description",
      AgentDescription(
        "54J",
        "Edward, ",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1330"),
        Some("1376")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "2QX",
      "current description",
      AgentDescription(
        "2QX",
        "Edward VII",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1841"),
        Some("1910")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "561",
      "current description",
      AgentDescription(
        "561",
        "Fanshawe, Baron, of Richmond, ",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        None,
        None
      )
    ),
    AgentSummary(
      AgentType.Person,
      "46F",
      "current description",
      AgentDescription(
        "46F",
        "Fawkes, Guy",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1570"),
        Some("1606")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "2JN",
      "current description",
      AgentDescription(
        "2JN",
        "George, David Lloyd",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1863"),
        Some("1945")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "34X",
      "current description",
      AgentDescription(
        "34X",
        "Halley, Edmund",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1656"),
        Some("1742")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "2TK",
      "current description",
      AgentDescription("2TK", "Halifax, ", "2022-06-22T02:00:00-0500", Some(false), Some(false), None, None)
    ),
    AgentSummary(
      AgentType.Person,
      "39T",
      "current description",
      AgentDescription(
        "39T",
        "Irvine, Linda Mary",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1928"),
        None
      )
    ),
    AgentSummary(
      AgentType.Person,
      "4",
      "current description",
      AgentDescription(
        "4",
        "Jack the Ripper, ",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1888"),
        Some("1888")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "4FF",
      "current description",
      AgentDescription(
        "4FF",
        "Keay, Sir Lancelot Herman",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1883"),
        Some("1974")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "ST",
      "current description",
      AgentDescription(
        "ST",
        "Lawson, Nigel",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1932"),
        None
      )
    ),
    AgentSummary(
      AgentType.Person,
      "51X",
      "current description",
      AgentDescription(
        "51X",
        "Macpherson, Sir William (Alan)",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1926"),
        None
      )
    ),
    AgentSummary(
      AgentType.Person,
      "515",
      "current description",
      AgentDescription(
        "515",
        "Newcastle, 1st Duke of, ",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        None,
        None
      )
    ),
    AgentSummary(
      AgentType.Person,
      "4VF",
      "current description",
      AgentDescription("4VF", "Old Pretender, The", "2022-06-22T02:00:00-0500", Some(false), Some(false), None, None)
    ),
    AgentSummary(
      AgentType.Person,
      "4H3",
      "current description",
      AgentDescription(
        "4H3",
        "Oliphant, Sir Mark Marcus Laurence Elwin",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1901"),
        Some("2000")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "46W",
      "current description",
      AgentDescription(
        "46W",
        "Paine, Thomas",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1737"),
        Some("1809")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "3SH",
      "current description",
      AgentDescription(
        "3SH",
        "Reade, Hubert Granville Revell",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1859"),
        Some("1938")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "2TF",
      "current description",
      AgentDescription("2TF", "Reading, ", "2022-06-22T02:00:00-0500", Some(false), Some(false), None, None)
    ),
    AgentSummary(
      AgentType.Person,
      "53T",
      "current description",
      AgentDescription(
        "53T",
        "Salisbury, Sir Edward James",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1886"),
        Some("1978")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "3QL",
      "current description",
      AgentDescription(
        "3QL",
        "Tate, Sir Henry",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1819"),
        Some("1899")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "37K",
      "current description",
      AgentDescription(
        "37K",
        "Uvarov, Sir Boris Petrovitch",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1889"),
        Some("1970")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "2T1",
      "current description",
      AgentDescription(
        "2T1",
        "Vane-Tempest-Stewart, Charles Stewart",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1852"),
        Some("1915")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "4RW",
      "current description",
      AgentDescription(
        "4RW",
        "Victor Amadeus, ",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1666"),
        Some("1732")
      )
    ),
    AgentSummary(
      AgentType.Person,
      "3GY",
      "current description",
      AgentDescription(
        "3GY",
        "Victoria, ",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1819"),
        Some("1901")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "RR6",
      "current description",
      AgentDescription(
        "RR6",
        "100th (Gordon Highlanders) Regiment of Foot",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1794"),
        Some("1794")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "S34",
      "current description",
      AgentDescription(
        "S34",
        "1st Regiment of Foot or Royal Scots",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1812"),
        Some("1812")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "87K",
      "current description",
      AgentDescription(
        "87K",
        "Abbotsbury Railway Company",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1877"),
        Some("1877")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VWG",
      "current description",
      AgentDescription(
        "VWG",
        "Accountant General in the Court of Chancery",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1726"),
        Some("1726")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "LWY",
      "current description",
      AgentDescription(
        "LWY",
        "Admiralty Administrative Whitley Council, General Purposes Committee",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1942"),
        Some("1942")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VS6",
      "current description",
      AgentDescription(
        "VS6",
        "Advisory Committee on Animal Feedingstuffs",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1999"),
        Some("1999")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "CC",
      "current description",
      AgentDescription(
        "CC",
        "Bank of England",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1694"),
        Some("1694")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "N9S",
      "current description",
      AgentDescription(
        "N9S",
        "Bank on Tickets of the Million Adventure",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1695"),
        Some("1695")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "JS8",
      "current description",
      AgentDescription("JS8", "BBC", "2022-06-22T02:00:00-0500", Some(false), Some(false), None, None)
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "8WG",
      "current description",
      AgentDescription(
        "8WG",
        "Bee Husbandry Committee",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1959"),
        Some("1959")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "6VQ",
      "current description",
      AgentDescription(
        "6VQ",
        "Cabinet",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1919"),
        Some("1919")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "SV",
      "current description",
      AgentDescription(
        "SV",
        "Cabinet",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1945"),
        Some("1945")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "5V4",
      "current description",
      AgentDescription(
        "5V4",
        "Cabinet, Committee for Control of Official Histories",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1946"),
        Some("1946")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "GW5",
      "current description",
      AgentDescription(
        "GW5",
        "Cattle Emergency Committee",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1934"),
        Some("1934")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "934",
      "current description",
      AgentDescription(
        "934",
        "Dairy Crest",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1981"),
        Some("1981")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "9HC",
      "current description",
      AgentDescription(
        "9HC",
        "Dean of the Chapel Royal",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        None,
        None
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "WGL",
      "current description",
      AgentDescription(
        "WGL",
        "Department for Environment, Food and Rural Affairs, Water Quality Division",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("2002"),
        Some("2002")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "WJ4",
      "current description",
      AgentDescription(
        "WJ4",
        "Department for Exiting the European Union",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("2016"),
        Some("2016")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "9YJ",
      "current description",
      AgentDescription(
        "9YJ",
        "East Grinstead, Groombridge and Tunbridge Wells Railway Company",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1862"),
        Some("1862")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "HF4",
      "current description",
      AgentDescription(
        "HF4",
        "East India Company",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1600"),
        Some("1600")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "WN3",
      "current description",
      AgentDescription(
        "WN3",
        "Education and Skills Funding Agency",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("2017"),
        Some("2017")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "WNL",
      "current description",
      AgentDescription(
        "WNL",
        "Education and Skills Funding Agency",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("2017"),
        Some("2017")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "Q1R",
      "current description",
      AgentDescription(
        "Q1R",
        "Falkland Islands Company",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1899"),
        Some("1899")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "SQ9",
      "current description",
      AgentDescription(
        "SQ9",
        "Fish's Corps of Foot",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1782"),
        Some("1782")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "R6R",
      "current description",
      AgentDescription(
        "R6R",
        "Foreign and Commonwealth Office, Consulate, Dusseldorf, West Germany",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1968"),
        Some("1968")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "HKL",
      "current description",
      AgentDescription(
        "HKL",
        "Foreign Office, Consulate, Angora and Konieh, Ottoman Empire",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1895"),
        Some("1895")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "KSC",
      "current description",
      AgentDescription(
        "KSC",
        "Gaming Board for Great Britain",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1968"),
        Some("1968")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "73R",
      "current description",
      AgentDescription("73R", "GCHQ", "2022-06-22T02:00:00-0500", Some(false), Some(false), None, None)
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VR1",
      "current description",
      AgentDescription(
        "VR1",
        "Geffrye Museum",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1914"),
        Some("1914")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "QX5",
      "current description",
      AgentDescription(
        "QX5",
        "General Nursing Council for England and Wales, Registration and Enrolment Committee",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1970"),
        Some("1970")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "C1Y",
      "current description",
      AgentDescription(
        "C1Y",
        "Halifax High Level and North and South Junction Railway Company",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1884"),
        Some("1884")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "W2T",
      "current description",
      AgentDescription(
        "W2T",
        "Hansard Society",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1944"),
        Some("1944")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "F18",
      "current description",
      AgentDescription(
        "F18",
        "Health and Safety Commission, Health and Safety Executive, Employment Medical Advisory Service",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1975"),
        Some("1975")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "8JK",
      "current description",
      AgentDescription(
        "8JK",
        "Her Majesty's Stationery Office",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1986"),
        Some("1986")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "9FV",
      "current description",
      AgentDescription(
        "9FV",
        "Ideal Benefit Society",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1912"),
        Some("1912")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "5YX",
      "current description",
      AgentDescription(
        "5YX",
        "Imperial War Museum: Churchill Museum and Cabinet War Rooms",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1939"),
        Some("1939")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "W1Q",
      "current description",
      AgentDescription(
        "W1Q",
        "Independent Expert Group on Mobile Phones",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1999"),
        Some("1999")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "QLY",
      "current description",
      AgentDescription(
        "QLY",
        "Independent Expert Group on Mobile Phones",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1999"),
        Some("1999")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "LS5",
      "current description",
      AgentDescription(
        "LS5",
        "Jodrell Bank Observatory, Cheshire",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1955"),
        Some("1955")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "92W",
      "current description",
      AgentDescription(
        "92W",
        "Joint Milk Quality Committee",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1948"),
        Some("1948")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "L3W",
      "current description",
      AgentDescription(
        "L3W",
        "Justices in Eyre, of Assize, of Gaol Delivery, of Oyer and Terminer, of the Peace, and of Nisi Prius",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        None,
        None
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "N8X",
      "current description",
      AgentDescription(
        "N8X",
        "Justices of the Forest",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1166"),
        Some("1166")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "THY",
      "current description",
      AgentDescription("THY", "Kew Gardens Archive", "2022-06-22T02:00:00-0500", Some(false), Some(false), None, None)
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "SGX",
      "current description",
      AgentDescription(
        "SGX",
        "King's Own Dragoons, 1751-1818",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        None,
        None
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "CCR",
      "current description",
      AgentDescription(
        "CCR",
        "Knitting, Lace and Net Industry Training Board",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1966"),
        Some("1966")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TTT",
      "current description",
      AgentDescription(
        "TTT",
        "King's Volunteers Regiment of Foot, 1761-1763",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        None,
        None
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VR7",
      "current description",
      AgentDescription(
        "VR7",
        "Lady Lever Art Gallery",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1922"),
        Some("1922")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "XQ",
      "current description",
      AgentDescription(
        "XQ",
        "Law Society",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1825"),
        Some("1825")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "91W",
      "current description",
      AgentDescription(
        "91W",
        "League of Mercy",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1898"),
        Some("1898")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VX",
      "current description",
      AgentDescription(
        "VX",
        "Legal Aid Board",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1989"),
        Some("1989")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TXG",
      "current description",
      AgentDescription(
        "TXG",
        "Legal Aid Board, 1988-1989",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        None,
        None
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "6LL",
      "current description",
      AgentDescription(
        "6LL",
        "Machinery of Government Committee",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1917"),
        Some("1917")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "G6N",
      "current description",
      AgentDescription(
        "G6N",
        "Magnetic Department",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1839"),
        Some("1839")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "71K",
      "current description",
      AgentDescription(
        "71K",
        "Manpower Distribution Board",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1916"),
        Some("1916")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "KN1",
      "current description",
      AgentDescription(
        "KN1",
        "Master of the Rolls Archives Committee",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1925"),
        Some("1925")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "J6X",
      "current description",
      AgentDescription(
        "J6X",
        "National Agricultural Advisory Service, Great House Experimental Husbandry Farm",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1951"),
        Some("1951")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "K7N",
      "current description",
      AgentDescription(
        "K7N",
        "National Air Traffic Control Services, Director General Projects and Engineering, Directorate of Projects",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1963"),
        Some("1963")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TSL",
      "current description",
      AgentDescription(
        "TSL",
        "National Archives, The",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        None,
        None
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "LSN",
      "current description",
      AgentDescription(
        "LSN",
        "Navy Board, Transport Branch, Prisoner of War Department",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1817"),
        Some("1817")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "W1S",
      "current description",
      AgentDescription(
        "W1S",
        "Office for Budget Responsibility",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("2010"),
        Some("2010")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "N4W",
      "current description",
      AgentDescription(
        "N4W",
        "Office of Population Censuses and Surveys, Computer Division",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1972"),
        Some("1972")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "QQC",
      "current description",
      AgentDescription(
        "QQC",
        "Office of Works, Directorate of Works, Maintenance Surveyors Division, Sanitary Engineers Section",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1928"),
        Some("1928")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "QFY",
      "current description",
      AgentDescription(
        "QFY",
        "Office of the President of Social Security Appeal Tribunals, Medical Appeal Tribunals and Vaccine Damage Tribunals",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1984"),
        Some("1984")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VYJ",
      "current description",
      AgentDescription(
        "VYJ",
        "Ordnance Survey of Great Britain, Directorate of Data Collection and Management",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("2003"),
        Some("2003")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "8FX",
      "current description",
      AgentDescription(
        "8FX",
        "Overseas Development Administration, Information Department",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1970"),
        Some("1970")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "3C",
      "current description",
      AgentDescription(
        "3C",
        "Overseas Finance, International Finance, IF1 International Financial Institutions and Debt Division",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1990"),
        Some("1990")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "988",
      "current description",
      AgentDescription(
        "988",
        "Oxford University Archives",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1634"),
        Some("1634")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TWX",
      "current description",
      AgentDescription(
        "TWX",
        "Oxford University Press",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1633"),
        Some("1633")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "79L",
      "current description",
      AgentDescription(
        "79L",
        "Palace Court",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1660"),
        Some("1660")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TX6",
      "current description",
      AgentDescription("TX6", "Parker Inquiry", "2022-06-22T02:00:00-0500", Some(false), Some(false), None, None)
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VY4",
      "current description",
      AgentDescription(
        "VY4",
        "Paymaster General of the Court of Chancery, Supreme Court Pay Office",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1884"),
        Some("1884")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VX3",
      "current description",
      AgentDescription(
        "VX3",
        "Persona Associates Ltd",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1989"),
        Some("1989")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "V36",
      "current description",
      AgentDescription("V36", "Petty Bag Office", "2022-06-22T02:00:00-0500", Some(false), Some(false), None, None)
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "8R6",
      "current description",
      AgentDescription("8R6", "Queen Anne's Bounty", "2022-06-22T02:00:00-0500", Some(false), Some(false), None, None)
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "SH2",
      "current description",
      AgentDescription(
        "SH2",
        "Queen's Own Dragoons, 1788-1818",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        None,
        None
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "79X",
      "current description",
      AgentDescription(
        "79X",
        "Queens Prison",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1842"),
        Some("1842")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "W91",
      "current description",
      AgentDescription(
        "W91",
        "Queen's Printer for Scotland",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1999"),
        Some("1999")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "F11",
      "current description",
      AgentDescription(
        "F11",
        "Radioactive Substances Advisory Committee",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1948"),
        Some("1948")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "CYY",
      "current description",
      AgentDescription(
        "CYY",
        "Railway Executive",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1947"),
        Some("1947")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "CXY",
      "current description",
      AgentDescription(
        "CXY",
        "Railway Executive",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1914"),
        Some("1914")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "CY1",
      "current description",
      AgentDescription(
        "CY1",
        "Railway Executive",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1939"),
        Some("1939")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TXH",
      "current description",
      AgentDescription(
        "TXH",
        "SaBRE",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("2002"),
        Some("2002")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "739",
      "current description",
      AgentDescription("739", "Scaccarium Superius", "2022-06-22T02:00:00-0500", Some(false), Some(false), None, None)
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "NWN",
      "current description",
      AgentDescription(
        "NWN",
        "School of Anti-Aircraft Artillery",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1942"),
        Some("1942")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "SGS",
      "current description",
      AgentDescription(
        "SGS",
        "Scots Greys, 1877-1921",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        None,
        None
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VXR",
      "current description",
      AgentDescription("VXR", "Takeover Panel", "2022-06-22T02:00:00-0500", Some(false), Some(false), None, None)
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "QQR",
      "current description",
      AgentDescription(
        "QQR",
        "Tate Gallery",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1897"),
        Some("1897")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "63K",
      "current description",
      AgentDescription(
        "63K",
        "Tate Gallery Archive",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1970"),
        Some("1970")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "G91",
      "current description",
      AgentDescription(
        "G91",
        "Thalidomide Y List Inquiry",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1978"),
        Some("1978")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "FKS",
      "current description",
      AgentDescription(
        "FKS",
        "The Buying Agency",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1991"),
        Some("1991")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "JLC",
      "current description",
      AgentDescription(
        "JLC",
        "The Crown Estate, Other Urban Estates, Foreshore and Seabed Branches",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1973"),
        Some("1973")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "SYL",
      "current description",
      AgentDescription(
        "SYL",
        "Uhlans Britanniques de Sainte-Domingue (Charmilly's), 1794-1795",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        None,
        None
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "TXK",
      "current description",
      AgentDescription(
        "TXK",
        "UK Passport Service",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1991"),
        Some("1991")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "V3H",
      "current description",
      AgentDescription(
        "V3H",
        "UK Web Archiving Consortium",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        None,
        None
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "CCX",
      "current description",
      AgentDescription(
        "CCX",
        "United Kingdom Atomic Energy Authority, Atomic Weapons Research Establishment, Directors Office",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1954"),
        Some("1954")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VTY",
      "current description",
      AgentDescription(
        "VTY",
        "Valuation Office Agency",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1991"),
        Some("19910")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "9HJ",
      "current description",
      AgentDescription(
        "9HJ",
        "Venetian Republic",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("727"),
        Some("727")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "QYF",
      "current description",
      AgentDescription(
        "QYF",
        "Victoria and Albert Museum",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1857"),
        Some("1857")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "61H",
      "current description",
      AgentDescription(
        "61H",
        "Victoria & Albert Museum, Archive of Art and Design",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1992"),
        Some("1992")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "W9K",
      "current description",
      AgentDescription(
        "W9K",
        "Wales Tourist Board",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1969"),
        Some("1969")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "VRG",
      "current description",
      AgentDescription(
        "VRG",
        "Walker Art Gallery",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1873"),
        Some("1873")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "61J",
      "current description",
      AgentDescription(
        "61J",
        "Wallace Collection",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1897"),
        Some("1897")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "HXV",
      "current description",
      AgentDescription(
        "HXV",
        "War and Colonial Department, Commissioners for liquidating the Danish and Dutch loans for St Thomas and St John",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1808"),
        Some("1808")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "V2R",
      "current description",
      AgentDescription(
        "V2R",
        "Zahid Mubarek Inquiry",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("2004"),
        Some("2004")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "763",
      "current description",
      AgentDescription(
        "763",
        "Zambia Department, Commonwealth Office",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1967"),
        Some("1967")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "765",
      "current description",
      AgentDescription(
        "765",
        "Zambia, Malawi and Southern Africa Department",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1968"),
        Some("1968")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "G2Y",
      "current description",
      AgentDescription(
        "G2Y",
        "Zuckerman Working Party",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        None,
        None
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "63F",
      "current description",
      AgentDescription(
        "63F",
        "British Museum Central Archive",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("2001"),
        Some("2001")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "614",
      "current description",
      AgentDescription(
        "614",
        "British Library, Sound Archive",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("1983"),
        Some("1983")
      )
    ),
    AgentSummary(
      AgentType.CorporateBody,
      "S2",
      "current description",
      AgentDescription(
        "S2",
        "The National Archives",
        "2022-06-22T02:00:00-0500",
        Some(false),
        Some(false),
        Some("2003"),
        None
      )
    )
  )

  def getAgentEntities(): List[AgentEntity] =
    List(
      AgentEntity(
        new URI("http://cat.nationalarchives.gov.uk/person-concept"),
        new URI("http://cat.nationalarchives.gov.uk/agent.48N"),
        new URI("http://cat.nationalarchives.gov.uk/agent.48N.1"),
        "Baden-Powell",
        "2022-06-22T02:00:00-0500",
        Some("1889"),
        Some("1977"),
        Some(false)
      ),
      AgentEntity(
        new URI("http://cat.nationalarchives.gov.uk/person-concept"),
        new URI("http://cat.nationalarchives.gov.uk/agent.46F"),
        new URI("http://cat.nationalarchives.gov.uk/agent.46F.1"),
        "Fawkes, Guy",
        "2022-06-22T02:00:00-0500",
        Some("1570"),
        Some("1606"),
        Some(false)
      ),
      AgentEntity(
        new URI("http://cat.nationalarchives.gov.uk/corporate-body-concept"),
        new URI("http://cat.nationalarchives.gov.uk/agent.92W"),
        new URI("http://cat.nationalarchives.gov.uk/agent.92W.1"),
        "Joint Milk Quality Committee",
        "2022-06-22T02:00:00-0500",
        Some("1948"),
        Some("1948"),
        Some(false)
      ),
      AgentEntity(
        new URI("http://cat.nationalarchives.gov.uk/corporate-body-concept"),
        new URI("http://cat.nationalarchives.gov.uk/agent.8R6"),
        new URI("http://cat.nationalarchives.gov.uk/agent.8R6.1"),
        "Queen Anne's Bounty",
        "2022-06-22T02:00:00-0500",
        None,
        None,
        Some(false)
      )
    )

  def getPlaceOfDepositEntities(): List[AgentEntity] =
    List(
      AgentEntity(
        new URI("http://cat.nationalarchives.gov.uk/corporate-body-concept"),
        new URI("http://cat.nationalarchives.gov.uk/8R6"),
        new URI("http://cat.nationalarchives.gov.uk/agent.8R6.1"),
        "Queen Anne's Bounty",
        "2022-06-22T02:00:00-0500",
        None,
        None,
        Some(false)
      )
    )
}
