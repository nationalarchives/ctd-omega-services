# Used to manually populate a local instance of RDF4J with test data
curl -v -X DELETE http://localhost:8080/rdf4j-server/repositories/PACT
curl -v -X PUT -H "Content-Type: text/turtle" --data-binary @src/it/resources/repositoryConfig.ttl http://localhost:8080/rdf4j-server/repositories/PACT
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/test-corporate-body-concepts.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/test-corporate-body-descriptions.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/test-person-concepts.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/test-person-descriptions.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/test-legal-status.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/test-record-concepts.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/test-record-descriptions.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/ontologies/edm.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/ontologies/ODRL22.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/ontologies/premis-3-0-0.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/ontologies/prov.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/ontologies/rdaa.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/ontologies/rdac.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/ontologies/rdat.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/ontologies/rdau.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/ontologies/relationshipSubType.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/ontologies/time.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Content-type: text/turtle" --data-binary @src/it/resources/ontologies/version.ttl http://localhost:8080/rdf4j-server/repositories/PACT/statements
curl -v -H "Connection: close" http://localhost:8080/rdf4j-server
