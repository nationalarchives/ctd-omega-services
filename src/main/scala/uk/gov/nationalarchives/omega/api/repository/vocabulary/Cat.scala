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

package uk.gov.nationalarchives.omega.api.repository.vocabulary

import org.apache.jena.rdf.model.{ ModelFactory, Property, Resource }

object Cat {

  /** The RDF model that holds the vocabulary terms */
  private val model = ModelFactory.createDefaultModel

  /** The namespace of the vocabulary as a string */
  final val NS = "http://cat.nationalarchives.gov.uk/"

  final val piece: String = model.createResource(s"${NS}piece").getURI
  final val item: String = model.createResource(s"${NS}item").getURI
  final val personConcept: String = model.createResource(s"${NS}person-concept").getURI
  final val familyConcept: String = model.createResource(s"${NS}family-concept").getURI
  final val corporateBodyConcept: String = model.createResource(s"${NS}corporate-body-concept").getURI
  final val collectiveAgentConcept: String = model.createResource(s"${NS}collective-agent-concept").getURI
  final val hardwareAgentConcept: String = model.createResource(s"${NS}hardware-agent-concept").getURI
  final val softwareAgentConcept: String = model.createResource(s"${NS}software-agent-concept").getURI
  final val authorityFile: String = model.createResource(s"${NS}authority-file").getURI
  final val separatedMaterial: String = model.createResource(s"${NS}separated-material").getURI
  final val relatedMaterial: String = model.createResource(s"${NS}related-material").getURI
  final val publicRecord: String = model.createResource(s"${NS}public-record").getURI
  final val nonPublicRecord: String = model.createResource(s"${NS}non-public-record").getURI
  final val publicRecordUnlessOtherwiseStated: String =
    model.createResource(s"${NS}public-record-unless-otherwise-stated").getURI
  final val welshPublicRecord: String = model.createResource(s"${NS}welsh-public-record").getURI
  final val nonRecordMaterial: String = model.createResource(s"${NS}non-record-material").getURI

}
