package com.pamu_nagarjuna.add2cal

import java.util.Properties
import javax.mail.event.{MessageCountEvent, MessageCountAdapter}
import javax.mail.{Folder, Session, Message}


import akka.actor.{Props, ActorSystem}
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPFolder.ProtocolCommand
import com.sun.mail.imap.protocol.IMAPProtocol

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by pnagarjuna on 05/08/15.
 */
object Main {
  def main(args: Array[String]): Unit = {
    /*
    getNewEmails("imap", "imap.gmail.com", "993", "******", "*****") match {
      case Success(emails) => emails.foreach(email => println(s"${email.getSubject}"))
      case Failure(th) => th.printStackTrace()
    }*/
    //notifyOnNewEmail("imap", "imap.gmail.com", "993", "*****", "*****")
    val system = ActorSystem("MainSnifferSystem")
    val demo = system.actorOf(Props[Demo], "Demo")
    demo ! Demo.Start
    Thread.sleep(Long.MaxValue)
  }

  def getServerProps(protocol: String, host: String, port: String): Properties = {
    val properties = new Properties()
    properties put(String.format("mail.%s.host", protocol), host)
    properties put(String.format("mail.%s.port", protocol), port)
    properties put(String.format("mail.%s.socketFactory.class", protocol), "javax.net.ssl.SSLSocketFactory")
    properties put(String.format("mail.%s.socketFactory.fallback", protocol), "false")
    properties put(String.format("mail.%s.socketFactory.port", protocol), String.valueOf(port))
    properties
  }

  def getIMAPFolder(protocol: String, host: String, port: String, username: String, password: String, box: String): Future[IMAPFolder] = {
    Future {
      scala.concurrent.blocking {
        val properties = getServerProps(protocol, host, port)
        val session = Session.getDefaultInstance(properties)
        val store = session.getStore(protocol)
        store.connect(username, password)
        val inbox = store.getFolder("INBOX")
        inbox.open(Folder.READ_WRITE)
        inbox.asInstanceOf[IMAPFolder]
      }
    }
  }



  def getNewEmails(protocol: String, host: String, port: String, username: String, password: String): Try[List[Message]] = {
    Try {
      val properties = getServerProps(protocol, host, port)
      val session = Session.getDefaultInstance(properties)
      val store = session.getStore(protocol)
      store.connect(username, password)
      val inbox = store.getFolder("INBOX")
      inbox.open(Folder.READ_WRITE)
      inbox.getMessages(1, inbox.getMessageCount).toList
    }
  }

  def notifyOnNewEmail(protocol: String, host: String, port: String, username: String, password: String): Unit = {
    Try {
      val properties = getServerProps(protocol, host, port)
      val session = Session.getDefaultInstance(properties)
      val store = session.getStore(protocol)
      store.connect(username, password)
      val inbox = store.getFolder("INBOX")
      inbox.open(Folder.READ_WRITE)
      //inbox.getMessages(1, inbox.getMessageCount).toList
      inbox.addMessageCountListener(new MessageCountAdapter {
        override def messagesAdded(e: MessageCountEvent): Unit = {
          super.messagesAdded(e)
          println("New Message")
          e.getMessages.foreach(msg => println(msg.getSubject))
        }
      })
      val imapFolder = inbox.asInstanceOf[IMAPFolder]
      imapFolder.idle()
      imapFolder.doCommand(new ProtocolCommand {
        override def doCommand(imapProtocol: IMAPProtocol): AnyRef = {
          imapProtocol.simpleCommand("NOOP", null);
          return null;
        }
      })
    }
  }
}
