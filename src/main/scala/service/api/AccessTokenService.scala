package service.api

import model.Account
import model.Profile._
import profile.simple._

import org.slf4j.LoggerFactory


trait AccessTokenService {

  private val logger = LoggerFactory.getLogger(classOf[AccessTokenService])

  def authenticate(token: String)(implicit s: Session): Option[Account] = {
    logger.error(s"token : $token")
    // TODO Implements
    val userName = "root"
    Accounts filter (_.userName === userName.bind) firstOption
  }

}
