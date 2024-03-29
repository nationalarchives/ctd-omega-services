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

package uk.gov.nationalarchives.omega.api.repository

import org.apache.jena.query.Query
import org.apache.jena.rdf.model.ResourceFactory
import org.apache.jena.vocabulary.DCTerms
import org.phenoscape.sparql.SPARQLInterpolation.SPARQLStringContext
import uk.gov.nationalarchives.omega.api.repository.vocabulary.{ Cat, TODO }
import uk.gov.nationalarchives.omega.api.support.AsyncUnitTest

class RepositoryUtilsSpec extends AsyncUnitTest {

  val sparqlPrefix =
    sparql"""
            PREFIX premis:<http://www.loc.gov/premis/rdf/v3/>
            PREFIX rdac: <http://rdaregistry.info/Elements/c/>
            PREFIX ver: <http://purl.org/linked-data/version#>
            PREFIX rdaa: <http://rdaregistry.info/Elements/a/>
            PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
            PREFIX todo: <http://TODO/>
            PREFIX dct: <http://purl.org/dc/terms/>
            PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
            PREFIX cat: <http://cat.nationalarchives.gov.uk/>
            PREFIX time: <http://www.w3.org/2006/time#>
            PREFIX prov: <http://www.w3.org/ns/prov#>
            PREFIX foaf: <http://xmlns.com/foaf/0.1/>
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"""

  val expectedSparqlWithValues: Query =
    (sparqlPrefix + sparql"""
            SELECT DISTINCT ?identifier ?agentType ?currentVersion
            WHERE {
              ?identifier dct:type            ?agentType ;
                          ver:currentVersion  ?currentVersion .
              VALUES (?agentType) {(<http://cat.nationalarchives.gov.uk/person-concept>) (<http://cat.nationalarchives.gov.uk/corporate-body-concept>)}
            }""").toQuery

  val expectedSparqlWithBoolean: Query =
    (sparqlPrefix + sparql"""
        SELECT DISTINCT ?identifier ?agentType ?currentVersion
        WHERE {
          ?identifier     dct:type                  ?agentType ;
                          ver:currentVersion        ?currentVersion .
          ?currentVersion todo:is-place-of-deposit  true .
        }""").toQuery

  val expectedSparqlWithExtension: Query =
    (sparqlPrefix +
      sparql"""
        SELECT DISTINCT ?identifier ?agentType ?currentVersion
        WHERE {
          ?identifier     dct:type                  ?agentType ;
                          ver:currentVersion        ?currentVersion .
          ?currentVersion ?predicateParam           ?objectParam .
        }
        ORDER BY DESC(?identifier)""").toQuery

  val expectedSparqlWithObject: Query =
    (sparqlPrefix +
      sparql"""
        SELECT DISTINCT ?identifier ?agentType ?currentVersion
        WHERE {
          ?identifier     dct:type                  ?agentType ;
                          ver:currentVersion        ?currentVersion .
          ?currentVersion dct:type cat:authority-file .
        }""").toQuery

  val expectedSparqlWithPropertiesAndValues: Query =
    (sparqlPrefix +
      sparql"""
        SELECT DISTINCT ?identifier ?agentType ?currentVersion
        WHERE {
          ?identifier     dct:type                  ?agentType ;
                          ver:currentVersion        ?currentVersion .
          ?currentVersion todo:is-place-of-deposit  true .
          ?currentVersion dct:type                  cat:authority-file .
          VALUES (?agentType) {(<http://cat.nationalarchives.gov.uk/person-concept>) (<http://cat.nationalarchives.gov.uk/corporate-body-concept>)}

        }""").toQuery

  val expectedSparqlWithFilter: Query =
    (sparqlPrefix +
      sparql"""
        SELECT DISTINCT ?identifier ?agentType ?currentVersion
        WHERE {
          ?identifier     dct:type                  ?agentType ;
                          ver:currentVersion        ?currentVersion .
          ?currentVersion prov:generatedAtTime      ?versionTimestamp .
          FILTER(?versionTimestamp >= xsd:dateTime("2023-07-01T17:30:00.000Z"))
        }""").toQuery

  val expectedSparqlWithoutFilter: Query =
    (sparqlPrefix +
      sparql"""
        SELECT DISTINCT ?identifier ?agentType ?currentVersion
        WHERE {
          ?identifier     dct:type                  ?agentType ;
                          ver:currentVersion        ?currentVersion .
          ?currentVersion prov:generatedAtTime      ?versionTimestamp .
        }""").toQuery

  "prepareParameterizedQuery must" - {
    "add values" in {
      val utils = new RepositoryUtils {}
      val queryResource = "/sparql/values.rq"
      val queryParams = SparqlParams(values =
        Map(
          "agentTypeValuesParam" -> List(
            ResourceFactory.createResource(Cat.personConcept),
            ResourceFactory.createResource(Cat.corporateBodyConcept)
          )
        )
      )
      val result = utils.prepareParameterizedQuery(queryResource, queryParams)
      result.asserting(_ mustEqual expectedSparqlWithValues)
    }
    "add a boolean property" in {
      val utils = new RepositoryUtils {}
      val queryResource = "/sparql/properties.rq"
      val queryParams =
        SparqlParams(
          booleans = Map("objectParam" -> true),
          uris = Map("predicateParam" -> TODO.isPlaceOfDeposit)
        )
      val result = utils.prepareParameterizedQuery(queryResource, queryParams)
      result.asserting(_ mustEqual expectedSparqlWithBoolean)
    }
    "add a query extension" in {
      val utils = new RepositoryUtils {}
      val queryResource = "/sparql/properties.rq"
      val queryParams =
        SparqlParams(queryExtension = Some("ORDER BY DESC(?identifier)"))
      val result = utils.prepareParameterizedQuery(queryResource, queryParams, extendQuery = true)
      result.asserting(_ mustEqual expectedSparqlWithExtension)
    }
    "add an object property" in {
      val utils = new RepositoryUtils {}
      val queryResource = "/sparql/properties.rq"
      val queryParams =
        SparqlParams(uris = Map("predicateParam" -> DCTerms.`type`.getURI, "objectParam" -> Cat.authorityFile))
      val result = utils.prepareParameterizedQuery(queryResource, queryParams)
      SparqlParams(uris = Map("property" -> DCTerms.`type`.getURI, "value" -> Cat.authorityFile))
      result.asserting(_ mustEqual expectedSparqlWithObject)
    }
    "add multiple params and values" in {
      val utils = new RepositoryUtils {}
      val queryResource = "/sparql/properties-and-values.rq"
      val queryParams = SparqlParams(
        booleans = Map("objectParam1" -> true),
        uris = Map(
          "predicateParam1" -> TODO.isPlaceOfDeposit,
          "predicateParam2" -> DCTerms.`type`.getURI,
          "objectParam2"    -> Cat.authorityFile
        ),
        values = Map(
          "agentTypeValuesParam" -> List(
            ResourceFactory.createResource(Cat.personConcept),
            ResourceFactory.createResource(Cat.corporateBodyConcept)
          )
        )
      )
      val result = utils.prepareParameterizedQuery(queryResource, queryParams)
      result.asserting(_ mustEqual expectedSparqlWithPropertiesAndValues)
    }
    "add a filter param in" in {
      val utils = new RepositoryUtils {}
      val queryResource = "/sparql/filter.rq"
      val queryParams = SparqlParams(filters =
        Map("filterParam" -> "FILTER(?versionTimestamp >= xsd:dateTime(\"2023-07-01T17:30:00.000Z\"))")
      )
      val result = utils.prepareParameterizedQuery(queryResource, queryParams)
      result.asserting(_ mustEqual expectedSparqlWithFilter)
    }
    "remove a filter param in" in {
      val utils = new RepositoryUtils {}
      val queryResource = "/sparql/filter.rq"
      val queryParams = SparqlParams(filters = Map("filterParam" -> ""))
      val result = utils.prepareParameterizedQuery(queryResource, queryParams)
      result.asserting(_ mustEqual expectedSparqlWithoutFilter)
    }
  }

}
