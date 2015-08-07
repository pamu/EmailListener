package com.pamu_nagarjuna.add2cal

import javax.mail.Message
import javax.mail.event.{MessageCountEvent, MessageCountAdapter}

import akka.actor.{Status, ActorLogging, Actor}
import akka.pattern.pipe
import com.pamu_nagarjuna.add2cal.Main.{NoFolder, FolderNotOpen, NOOP}
import com.sun.mail.imap.IMAPFolder

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by pnagarjuna on 06/08/15.
 */
object MailSniffer {
  case object Connection
  case object ImapFolder
  case object Idle
  case object IdleOff
  case object PushMails
  case class Msgs(msgs: List[Message])
}

class MailSniffer(protocol: String, host: String, port: String, username: String, password: String) extends Actor with ActorLogging {
  import MailSniffer._

  def receive = {
    case Connection =>
      context become connection(protocol, host, port, username, password)
      self ! ImapFolder
    case x => log.info("{}", x, x getClass)
  }

  def connection(protocol: String, host: String, port: String, username: String, password: String): Receive = {
    case ImapFolder =>
      log info "Got a ImapFolder request"
      Main.getIMAPFolder(protocol, host, port, username, password, "inbox") pipeTo self
    case folder: IMAPFolder =>
      log.info("Got Inbox")
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
      self ! Idle
    case Msgs(msgs) =>
      println("Messages")
      msgs.foreach(msg => println(msg.getSubject))
    case Idle =>
      log.info("Got Idle Message")
      Main.keepAlive(folder) pipeTo self
    case unit: Unit =>
      Main.keepAlive(folder) pipeTo self
    case Status.Failure(throwable) =>
      throwable match {
        case NoFolder(_) =>
          context become connection(protocol, host, port, username, password)
        case FolderNotOpen(_) =>
          context become connection(protocol, host, port, username, password)
        case x => log.info("failure {} cause {}", x.getMessage, x.getCause)
      }
    case IdleOff =>
      Main.idleOff(folder) pipeTo self
    case NOOP =>
      context stop self
    case x =>
      log.info("Unknown message in Connection state {} of type {}", x, x.getClass)
  }
}
