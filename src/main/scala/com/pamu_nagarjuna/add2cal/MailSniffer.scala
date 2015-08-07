package com.pamu_nagarjuna.add2cal

import akka.actor.{Status, ActorLogging, Actor}
import akka.pattern.pipe
import com.pamu_nagarjuna.add2cal.Main.{NoFolder, FolderNotOpen, NOOP}
import com.sun.mail.imap.IMAPFolder

/**
 * Created by pnagarjuna on 06/08/15.
 */
object MailSniffer {
  case object Connection
  case object Idle
  case object IdleOff
}

class MailSniffer(protocol: String, host: String, port: String, username: String, password: String) extends Actor with ActorLogging {
  import MailSniffer._

  def receive = {
    case Connection => context become connection(protocol, host, port, username, password)
    case -> => log.info("{}", ->, -> getClass)
  }

  def connection(protocol: String, host: String, port: String, username: String, password: String): Receive = {
    case Connection =>
      import context.dispatcher
      Main.getIMAPFolder(protocol, host, port, username, password, "inbox") pipeTo self
    case folder: IMAPFolder =>
      context become idle(folder)
    case Status.Failure(throwable) =>
      log.info("Connection failed because {} cause {}", throwable.getMessage, throwable.getCause)
      context stop self
    case -> =>
      log.info("Unknown message in Connection state {} of type {}", ->, ->.getClass)
  }

  def idle(folder: IMAPFolder): Receive = {
    case Idle =>
      import context.dispatcher
      Main.keepAlive(folder) pipeTo self
    case unit: Unit =>
      import context.dispatcher
      Main.keepAlive(folder) pipeTo self
    case Status.Failure(throwable) =>
      throwable match {
        case NoFolder =>
          context become connection(protocol, host, port, username, password)
        case FolderNotOpen =>
          context become connection(protocol, host, port, username, password)
        case -> => log.info("failure {} cause {}", ->.getMessage, ->.getCause)
      }
    case IdleOff =>
      import context.dispatcher
      Main.idleOff(folder) pipeTo self
    case NOOP =>
      context stop self
    case -> =>
      log.info("Unknown message in Connection state {} of type {}", ->, ->.getClass)
  }
}
