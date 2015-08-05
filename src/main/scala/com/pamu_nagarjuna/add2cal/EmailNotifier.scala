package com.pamu_nagarjuna.add2cal

import javax.mail.event.{MessageCountEvent, MessageCountAdapter}
import javax.mail.{MessagingException, FolderClosedException, Folder, Session}

import com.sun.mail.imap.IMAPFolder

import scala.util.Try

/**
 * Created by pnagarjuna on 04/08/15.
 */
object EmailNotifier {
  def main(args: Array[String]): Unit = {
    val props = System getProperties
    val session = Session getInstance(props)
    val store = session getStore("imap")
    store connect(Constants.HOST, Constants.USER, Constants.PASSWORD)
    val folderOption: Option[Folder] = if (store.getFolder(Constants.BOX) == null) None else Some(store.getFolder(Constants.BOX))
    folderOption.map {
      folder => {

        folder open(Folder.READ_ONLY)

        folder addMessageCountListener(new MessageCountAdapter {
          override def messagesAdded(e: MessageCountEvent): Unit = {
            super.messagesAdded(e)
            val messages = e.getMessages
            messages.foreach(msg => println("Subject: " + msg.getSubject))
          }
        })

        val tryImapFolder = Try (folder.asInstanceOf[IMAPFolder])

        tryImapFolder.map {
          imapFolder => {
            while(true) {
              imapFolder.idle()
              println("System Idle")
            }
          }
        }.recover {
          case ex: Throwable => ex match {
            case FolderClosedException => {
              println("Folder is closed. Helpless exiting ...")
              System.exit(0)
            }
            case MessagingException => {
              while (true) {
                Thread.sleep(Constants.FREQ.toLong)
                folder.getMessageCount
              }
            }
            case _ => println()
          }
        }
      }
    }.getOrElse {
      println("No Imap folder found. Exiting ...")
      System.exit(0)
    }
  }
}
