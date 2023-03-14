import org.scalatest.matchers.must.Matchers

import java.nio.file.{ Files, Paths }
import javax.jms.{ Message, MessageListener, TextMessage }
import scala.util.{ Failure, Success, Using }

case class TestListener(resultFile: String) extends MessageListener with Matchers {

  def onMessage(message: Message): Unit = {
    message.acknowledge()
    message match {
      case m: TextMessage => readTextMessage(m)
      case _              => fail("Unknown message type")
    }
  }

  private def readTextMessage(textMessage: TextMessage): Unit = {
    val messageJson = textMessage.getText
    val file = Paths.get(resultFile)
    Using(Files.newBufferedWriter(file)) { writer =>
      writer.write(messageJson)
    } match {
      case Success(_) => ()
      case Failure(e) => fail(s"Unable to write message file due to ${e.getMessage}")
    }
  }

}
