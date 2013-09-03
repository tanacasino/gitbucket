package servlet

import javax.servlet._
import org.slf4j.LoggerFactory
import javax.servlet.http.HttpServletRequest
import util.Directory

/**
 * Controls the transaction with the open session in view pattern.
 */
class TransactionFilter extends Filter {
  
  private val logger = LoggerFactory.getLogger(classOf[TransactionFilter])
  
  def init(config: FilterConfig) = {}
  
  def destroy(): Unit = {}
  
  def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain): Unit = {
    if(req.asInstanceOf[HttpServletRequest].getRequestURI().startsWith("/assets/")){
      // assets don't need transaction
      chain.doFilter(req, res)
    } else {
      Database(req.getServletContext) withTransaction {
        logger.debug("TODO begin transaction")
        chain.doFilter(req, res)
        logger.debug("TODO end transaction")
      }
    }
  }

}

object Database {
  def apply(context: ServletContext): scala.slick.session.Database =
    scala.slick.session.Database.forURL(s"jdbc:h2:${Directory.DatabaseHome}",
        context.getInitParameter("db.user"),
        context.getInitParameter("db.password"))
}
