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

package uk.gov.nationalarchives.omega.api.common

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.util.UUID

class Version1UUIDSpec extends AnyFreeSpec with Matchers {

  "A Version1UUID when" - {
    "constructed with an UUID of version" - {
      "1" in {

        val uuid = UUID.fromString("c035b074-cd50-11ed-afa1-0242ac120002")

        val version1UUID = Version1UUID(uuid)

        version1UUID.value mustBe uuid

      }
      "4" in {

        val uuid = UUID.fromString("f25c9285-d654-484a-8aa4-f94cbc5690a9")

        val thrownException = intercept[IllegalArgumentException] {
          Version1UUID(uuid)
        }

        thrownException.getMessage mustBe "Provided UUID must be version 1; this one is version [4]"

      }

    }
    "generated" in {

      val version1UUID = Version1UUID.generate()

      version1UUID.value.version() mustBe 1

    }
    "displayed as a string" in {

      val uuid = UUID.fromString("c035b074-cd50-11ed-afa1-0242ac120002")

      val version1UUID = Version1UUID(uuid)

      version1UUID.toString mustBe "c035b074-cd50-11ed-afa1-0242ac120002"

    }
  }

}
