package servlet

import javax.servlet._
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import service.api.AccessTokenService
import util.Keys


class AccessTokenAuthenticationFilter extends Filter with AccessTokenService {
  private val tokenHeaderPrefix = "token "

  override def init(filterConfig: FilterConfig): Unit = {}

  override def destroy(): Unit = {}

  override def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain): Unit = {
    implicit val request = req.asInstanceOf[HttpServletRequest]
    implicit val session = req.getAttribute(Keys.Request.DBSession).asInstanceOf[slick.jdbc.JdbcBackend#Session]
    val response = res.asInstanceOf[HttpServletResponse]
    // TODO Basic Authentication Support
    val authorization = Option(request.getHeader("Authorization"))
    authorization.filter(_.startsWith(tokenHeaderPrefix)).map(_.stripPrefix(tokenHeaderPrefix)) match {
      case Some(token) =>
        authenticate(token) match {
          case Some(account) =>
            // For private api (require auth)
          request.getSession.setAttribute(Keys.Session.LoginAccount, account)
            chain.doFilter(req, res)
          case None =>
            // Authentication Error
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
        }
      case None =>
        // For public api
        chain.doFilter(req, res)
    }
  }

}
