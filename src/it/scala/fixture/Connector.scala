package fixture

import javax.jms.Connection

trait Connector {

  /** Returns the JMS Connection */
  def getConnection: Connection

  /** Returns true if the named queue exists */
  def queueExists(queueName: String): Boolean

}
