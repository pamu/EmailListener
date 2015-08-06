package com.pamu_nagarjuna.add2cal

import akka.actor.{ActorLogging, Actor}

/**
 * Created by pnagarjuna on 06/08/15.
 */
object MailSniffer {

}

class MailSniffer extends Actor with ActorLogging {
  def receive = {
    case ex => log.info("")
  }
}
