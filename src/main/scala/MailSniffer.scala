import java.io.IOException
import javax.mail.internet.MimeMultipart
import javax.mail.{MessagingException, Address, Message}
import javax.mail.event.{MessageCountAdapter, MessageCountEvent}

import akka.actor.{Actor, ActorLogging, Status}
import akka.pattern.pipe
import Main.{FolderNotOpen, NOOP, NoFolder}
import com.sun.mail.imap.IMAPFolder
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

/**
 * Created by pnagarjuna on 06/08/15.
 */
object MailSniffer {
  case object Connect
  case object ImapFolder
  case object Idle
  case object IdleOff
  case object PushMails
  case class Msgs(msgs: List[Message])
}

class MailSniffer(protocol: String, host: String, port: String, username: String, password: String) extends Actor with ActorLogging {
  import MailSniffer._

  def receive = {
    case Connect =>
      log.info("Got a Connection message")
      log.info("Going to Connection state")
      context become connection(protocol, host, port, username, password)
      log.info("Sent ImapFolder message after switching state to Connection")
      self ! ImapFolder
    case x => log.info("unknown message {} of type {}", x, x getClass)
  }

  def connection(protocol: String, host: String, port: String, username: String, password: String): Receive = {
    case ImapFolder =>
      log info "Got a ImapFolder request"
      log info "trying to connect to imap service"
      Main.getIMAPFolder(protocol, host, port, username, password, "inbox") pipeTo self
    case folder: IMAPFolder =>
      log.info("Got Inbox")
      log info ""
      context become idle(folder)
      self ! PushMails
    case Status.Failure(throwable) =>
      log.info("Connection failed because {} cause {}", throwable.getMessage, throwable.getCause)
      context stop self
    case x =>
      log.info("Unknown message in Connection state {} of type {}", x, x.getClass)
  }

  def idle(folder: IMAPFolder): Receive = {
    case PushMails =>
      log.info("Got Push Emails Message")
      folder.addMessageCountListener(new MessageCountAdapter {
        override def messagesAdded(e: MessageCountEvent): Unit = {
          super.messagesAdded(e)
          self ! Msgs(e.getMessages.toList)
        }
      })
      context.system.scheduler.schedule(0 seconds, 5 minutes, self, IdleOff)
      self ! Idle
    case Msgs(msgs) =>
      println("Messages")
      msgs.foreach(msg => {
        println(s"Message number ${msg.getMessageNumber}")
        println("reply to")
        msg.getReplyTo.foreach{add: Address => println(s"${add}")}
        println(s"${msg.getSubject} from ${msg.getFrom.map({add: Address => add.toString}).mkString(" ")}")
      })
      msgs.foreach {
	      msg => {

		      //msg.isMimeType("text/*")
          /**
          val multipart = msg.getContent.asInstanceOf[MimeMultipart]
          val list = for(i <- 0 until multipart.getCount) yield multipart.getBodyPart(i)
          println(s"body ${list.filter(_.isMimeType("text/plain")).head.getContent}") **/

          def getBody(msg: Message): Option[String] = {
            val body: Try[String] = Try {
              val mimeMultiPart = msg.getContent.asInstanceOf[MimeMultipart]
              val bodyParts = for( i <- 0 until mimeMultiPart.getCount) yield mimeMultiPart.getBodyPart(i)
              val str = bodyParts.filter(_.isMimeType("text/plain")).head.getContent.toString
              str
            }
            body.toOption
          }

          println(s"body: ${getBody(msg).getOrElse("")}")
	      }
      }
    case Idle =>
      log.info("Got Idle Message")
      Main.keepAlive(folder) pipeTo self
    case unit: Unit =>
      log info "Idle exited taking to idle state again"
      Main.keepAlive(folder) pipeTo self
    case Status.Failure(throwable) =>
      log info "failure in idle state"
      throwable match {
        case NoFolder(_) =>
          context become connection(protocol, host, port, username, password)
          self ! Connect
        case FolderNotOpen(_) =>
          context become connection(protocol, host, port, username, password)
          self ! Connect
        case ex: IOException =>
          context become connection(protocol, host, port, username, password)
          self ! Connect
        case msgEx: MessagingException =>
          context become connection(protocol, host, port, username, password)
          self ! Connect
        case x =>
          log.info("failure in idle state {} cause {}", x.getMessage, x.getCause)
      }
    case IdleOff =>
      log info "Idle off message is initiated"
      Main.idleOff(folder) pipeTo self
    case NOOP =>
      log info "Got NOOP message"
    case x =>
      log.info("Unknown message in Connection state {} of type {}", x, x.getClass)
  }
}
