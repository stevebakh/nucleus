package nl.gideondk.nucleus

import akka.actor.ActorSystem
import nl.gideondk.nucleus.protocol.NucleusMessaging._
import akka.io._
import scala.annotation.tailrec
import akka.util.ByteString
import java.nio.ByteOrder
import nl.gideondk.nucleus.protocol.{ NucleusMessage, NucleusMessageStage }
import nl.gideondk.nucleus.protocol.ETFProtocol

class Server(description: String, router: Router)(implicit system: ActorSystem) {
  var serverRef: Option[nl.gideondk.sentinel.Server[NucleusMessage, NucleusMessage]] = None

  def ctx = new HasByteOrder {
    def byteOrder = java.nio.ByteOrder.BIG_ENDIAN
  }

  val stages = new NucleusMessageStage() >> new LengthFieldFrame(1024 * 1024 * 50, lengthIncludesHeader = false) // Max 50MB messages

  def stop {
    if (serverRef.isDefined) {
      system stop serverRef.get.actor
      serverRef = None
    }
  }

  def run(port: Int) = {
    if (serverRef.isEmpty) serverRef = Some(nl.gideondk.sentinel.Server(port, Processor(router), description, stages, 1024, 1024 * 1024, 1024 * 1024 * 10))
  }
}

object Server {
  def apply(description: String, modules: NucleusModules, etfProtocol: ETFProtocol = ETFProtocol())(implicit system: ActorSystem) = {
    new Server(description, new Router(modules, etfProtocol))
  }
}
