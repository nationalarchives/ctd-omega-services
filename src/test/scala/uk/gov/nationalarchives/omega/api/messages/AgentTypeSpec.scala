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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.nationalarchives.omega.api.messages.AgentType.{ CollectiveAgent, CorporateBody, Family, HardwareAgent, Person }

class AgentTypeSpec extends AnyFreeSpec with Matchers {

  "AgentType" - {
    "CorporateBody" - {
      "must decode" in {
        CorporateBody.entryName mustBe "Corporate Body"
      }
      "must encode" in {
        AgentType.withName("Corporate Body") mustBe CorporateBody
      }
    }
    "Person" - {
      "must decode" in {
        Person.entryName mustBe "Person"
      }
      "must encode" in {
        AgentType.withName("Person") mustBe Person
      }
    }
    "CollectiveAgent" - {
      "must decode" in {
        CollectiveAgent.entryName mustBe "Collective Agent"
      }
      "must encode" in {
        AgentType.withName("Collective Agent") mustBe CollectiveAgent
      }
    }
    "Family" - {
      "must decode" in {
        Family.entryName mustBe "Family"
      }
      "must encode" in {
        AgentType.withName("Family") mustBe Family
      }
    }
    "HardwareAgent" - {
      "must decode" in {
        HardwareAgent.entryName mustBe "Hardware Agent"
      }
      "must encode" in {
        AgentType.withName("Hardware Agent") mustBe HardwareAgent
      }
    }
  }

}
