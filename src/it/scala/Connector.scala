import javax.jms.Connection

trait Connector {

  /** Returns the JMS Connection */
  def getConnection: Connection

}
