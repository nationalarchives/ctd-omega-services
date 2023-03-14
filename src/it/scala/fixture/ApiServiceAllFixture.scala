package fixture

import org.scalatest.{ BeforeAndAfterAll, Suite }
import uk.gov.nationalarchives.omega.api.common.ServiceArgs
import uk.gov.nationalarchives.omega.api.conf.ServiceConfig

import java.nio.file.Path

/** API Service Fixture which starts beforeAll tests and stops afterAll tests.
  */
trait ApiServiceAllFixture extends BeforeAndAfterAll { this: Suite =>

  /** Get the path to the directory to use as the Message Directory for the API Service.
    *
    * @return
    *   the path to a directory that will be used for storing messages
    */
  protected def getMessageDir(): Path

//  private val apiService: uk.gov.nationalarchives.omega.api.services.ApiService =
//    new uk.gov.nationalarchives.omega.api.services.ApiService(ServiceConfig("temp",1,1,1,1,"request-general","ctd-omega-editorial-web-application-instance-1"))

//  override protected def beforeAll(): Unit = {
//    this.apiService.start                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          //(ServiceArgs(Some(getMessageDir().toString), Option.empty))
//    super.beforeAll() // NOTE(AR) To be stackable, we must call super.beforeAll
//  }

//  override protected def afterAll(): Unit =
//    try
//      super.afterAll() // NOTE(AR) To be stackable, we must call super.afterAll
//    finally
//      this.apiService.stop()

  /** Get the API Service.
    *
    * @return
    *   the API Service
    */
  // def getApiService() = this.apiService
}
