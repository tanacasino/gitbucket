package servlet

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.slf4j.LoggerFactory
import service.api.AccessTokenService
import util.Keys


class AccessTokenAuthenticationFilter extends Filter with AccessTokenService {
  private val tokenHeaderPrefix = "token "

  private val logger = LoggerFactory.getLogger(classOf[BasicAuthenticationFilter])

  override def init(filterConfig: FilterConfig): Unit = {}

  override def destroy(): Unit = {}

  override def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain): Unit = {
    implicit val request = req.asInstanceOf[HttpServletRequest]
    implicit val session = req.getAttribute(Keys.Request.DBSession).asInstanceOf[slick.jdbc.JdbcBackend#Session]

    val response = res.asInstanceOf[HttpServletResponse]
    val authorization = Option(request.getHeader("Authorization"))

    logger.error("Authorization : " + authorization.toString)
    val accessToken = authorization.filter(_.startsWith(tokenHeaderPrefix)).map(_.stripPrefix(tokenHeaderPrefix))
    accessToken match {
      case Some(token) =>
        authenticate(token) match {
          case Some(account) =>
            request.getSession.setAttribute(Keys.Session.LoginAccount, account)
            chain.doFilter(req, res)
          case None => response.sendError(HttpServletResponse.SC_UNAUTHORIZED)

        }
      case None => response.sendError(HttpServletResponse.SC_UNAUTHORIZED)

    }
  }

}
