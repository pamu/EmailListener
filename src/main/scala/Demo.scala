import akka.actor.{Actor, ActorLogging, Status}
import akka.pattern.pipe

import scala.concurrent.Future

/**
 * Created by pnagarjuna on 06/08/15.
 */
object Demo {
  case object Start
}

class Demo extends Actor with ActorLogging {
  import Demo._
  def receive = {
    case Start =>
      import context.dispatcher
      Future {
        1
        throw new Exception("Crashed")
      } pipeTo self
    case x: Int => log info("Future succesful {}", x)
    case Status.Failure(th) => log info("future failed {} ", th getMessage)
    case ex => log info("message {} of type {}", ex, ex getClass)
  }
}
