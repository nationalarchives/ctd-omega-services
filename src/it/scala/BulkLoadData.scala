import TestConstants.testRepositoryUrl
import cats.effect.IO

import java.nio.file.{Path, Paths}
import scala.sys.process._

/** This is used to load corporate body test data to ElasticSearch instance in Docker container
  */
object BulkLoadData {

  val repositoryConfig: Path = Paths.get(getClass.getClassLoader.getResource("repositoryConfig.ttl").toURI)
  val corporateBodyConceptData: Path =
    Paths.get(getClass.getClassLoader.getResource("test-corporate-body-concepts.ttl").toURI)
  val corporateBodyDescriptionData: Path =
    Paths.get(getClass.getClassLoader.getResource("test-corporate-body-descriptions.ttl").toURI)
  val personConceptData: Path =
    Paths.get(getClass.getClassLoader.getResource("test-person-concepts.ttl").toURI)
  val personDescriptionData: Path =
    Paths.get(getClass.getClassLoader.getResource("test-person-descriptions.ttl").toURI)
  val legalStatusData: Path =
    Paths.get(getClass.getClassLoader.getResource("test-legal-status.ttl").toURI)
  val recordConceptData: Path =
    Paths.get(getClass.getClassLoader.getResource("test-record-concepts.ttl").toURI)
  val recordDescriptionData: Path =
    Paths.get(getClass.getClassLoader.getResource("test-record-descriptions.ttl").toURI)

  private val deleteRepositoryConfig = Seq(
    "curl",
    "-v",
    "-X",
    "DELETE",
    testRepositoryUrl
  )

  private val createRepositoryConfig = Seq(
    "curl",
    "-v",
    "-X",
    "PUT",
    "-H",
    "Content-Type: text/turtle",
    "--data-binary",
    s"@$repositoryConfig",
    testRepositoryUrl
  )

  private val postCorporateBodyConceptData = Seq(
    "curl",
    "-X",
    "POST",
    "-H",
    "Content-type: text/turtle",
    "--data-binary",
    s"@$corporateBodyConceptData",
    s"$testRepositoryUrl/statements"
  )

  private val postCorporateBodyDescriptionData = Seq(
    "curl",
    "-X",
    "POST",
    "-H",
    "Content-type: text/turtle",
    "--data-binary",
    s"@$corporateBodyDescriptionData",
    s"$testRepositoryUrl/statements"
  )

  private val postPersonConceptData = Seq(
    "curl",
    "-X",
    "POST",
    "-H",
    "Content-type: text/turtle",
    "--data-binary",
    s"@$personConceptData",
    s"$testRepositoryUrl/statements"
  )

  private val postPersonDescriptionData = Seq(
    "curl",
    "-X",
    "POST",
    "-H",
    "Content-type: text/turtle",
    "--data-binary",
    s"@$personDescriptionData",
    s"$testRepositoryUrl/statements"
  )

  private val postLegalStatusData = Seq(
    "curl",
    "-X",
    "POST",
    "-H",
    "Content-type: text/turtle",
    "--data-binary",
    s"@$legalStatusData",
    s"$testRepositoryUrl/statements"
  )

  private val postRecordConceptData = Seq(
    "curl",
    "-X",
    "POST",
    "-H",
    "Content-type: text/turtle",
    "--data-binary",
    s"@$recordConceptData",
    s"$testRepositoryUrl/statements"
  )

  private val postRecordDescriptionData = Seq(
    "curl",
    "-X",
    "POST",
    "-H",
    "Content-type: text/turtle",
    "--data-binary",
    s"@$recordDescriptionData",
    s"$testRepositoryUrl/statements"
  )

  def createRepository(): IO[Unit] =
    IO.blocking(deleteRepositoryConfig.!) *>
      IO.blocking(createRepositoryConfig.!) *>
      IO.blocking(postCorporateBodyConceptData.!) *>
      IO.blocking(postCorporateBodyDescriptionData.!) *>
      IO.blocking(postPersonConceptData.!) *>
      IO.blocking(postPersonDescriptionData.!) *>
      IO.blocking(postLegalStatusData.!) *>
      IO.blocking(postRecordConceptData.!) *>
      IO.blocking(postRecordDescriptionData.!) *> IO.unit

}
