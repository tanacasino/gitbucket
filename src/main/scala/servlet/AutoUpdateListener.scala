package servlet

import java.io.{InputStream, File}
import java.sql.Connection
import org.apache.commons.io.FileUtils
import javax.servlet._
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import util.Directory
import javax.servlet.descriptor.JspConfigDescriptor
import java.util.EventListener
import java.util
import javax.servlet.FilterRegistration.Dynamic
import java.net.URL

object AutoUpdate {
  
  /**
   * Version of GitBucket
   * 
   * @param majorVersion the major version
   * @param minorVersion the minor version
   */
  case class Version(majorVersion: Int, minorVersion: Int){
    
    private val logger = LoggerFactory.getLogger(classOf[servlet.AutoUpdate.Version])
    
    /**
     * Execute update/MAJOR_MINOR.sql to update schema to this version.
     * If corresponding SQL file does not exist, this method do nothing.
     */
    def update(conn: Connection): Unit = {
      val sqlPath = s"update/${majorVersion}_${minorVersion}.sql"
      val in = Thread.currentThread.getContextClassLoader.getResourceAsStream(sqlPath)
      if(in != null){
        val sql = IOUtils.toString(in, "UTF-8")
        val stmt = conn.createStatement()
        try {
          logger.debug(sqlPath + "=" + sql)
          stmt.executeUpdate(sql)
        } finally {
          stmt.close()
        }
      }
    }
    
    /**
     * MAJOR.MINOR
     */
    val versionString = s"${majorVersion}.${minorVersion}"
  }

  /**
   * The history of versions. A head of this sequence is the current BitBucket version.
   */
  val versions = Seq(
    Version(1, 5),
    Version(1, 4),
    new Version(1, 3){
      override def update(conn: Connection): Unit = {
        super.update(conn)
        // Fix wiki repository configuration
        val rs = conn.createStatement.executeQuery("SELECT USER_NAME, REPOSITORY_NAME FROM REPOSITORY")
        while(rs.next){
          val wikidir = Directory.getWikiRepositoryDir(rs.getString("USER_NAME"), rs.getString("REPOSITORY_NAME"))
          val repository = org.eclipse.jgit.api.Git.open(wikidir).getRepository
          val config = repository.getConfig
          if(!config.getBoolean("http", "receivepack", false)){
            config.setBoolean("http", null, "receivepack", true)
            config.save
          }
          repository.close
        }
      }
    },
    Version(1, 2),
    Version(1, 1),
    Version(1, 0)
  )
  
  /**
   * The head version of BitBucket.
   */
  val headVersion = versions.head
  
  /**
   * The version file (GITBUCKET_HOME/version).
   */
  val versionFile = new File(Directory.GitBucketHome, "version")
  
  /**
   * Returns the current version from the version file.
   */
  def getCurrentVersion(): Version = {
    if(versionFile.exists){
      FileUtils.readFileToString(versionFile, "UTF-8").split("\\.") match {
        case Array(majorVersion, minorVersion) => {
          versions.find { v => 
            v.majorVersion == majorVersion.toInt && v.minorVersion == minorVersion.toInt
          }.getOrElse(Version(0, 0))
        }
        case _ => Version(0, 0)
      }
    } else {
      Version(0, 0)
    }
    
  }  
  
}

/**
 * Start H2 database and update schema automatically.
 */
class AutoUpdateListener extends org.h2.server.web.DbStarter {
  import AutoUpdate._
  private val logger = LoggerFactory.getLogger(classOf[AutoUpdateListener])
  
  override def contextInitialized(event: ServletContextEvent): Unit = {
    super.contextInitialized(createServletContextEventWrapper(event.getServletContext))
    logger.debug("H2 started")
    
    logger.debug("Start schema update")
    val conn = getConnection()
    try {
      val currentVersion = getCurrentVersion()
      if(currentVersion == headVersion){
        logger.debug("No update")
      } else {
        versions.takeWhile(_ != currentVersion).reverse.foreach(_.update(conn))
        FileUtils.writeStringToFile(versionFile, headVersion.versionString, "UTF-8")
        conn.commit()
        logger.debug("Updated from " + currentVersion.versionString + " to " + headVersion.versionString)
      }
    } catch {
      case ex: Throwable => {
        logger.error("Failed to schema update", ex)
        ex.printStackTrace()
        conn.rollback()
      }
    }
    logger.debug("End schema update")
  }

  private def createServletContextEventWrapper(servletContext: ServletContext): ServletContextEvent = {
    new ServletContextEvent(new ServletContext() {
      override def getInitParameter(name: String) = name match {
        case "db.url" => s"jdbc:h2:${Directory.DatabaseHome}"
        case _ => servletContext.getInitParameter(name)
      }

      def getContextPath: String = servletContext.getContextPath

      def getContext(uripath: String): ServletContext = servletContext.getContext(uripath)

      def getMajorVersion: Int = servletContext.getMajorVersion

      def getMinorVersion: Int = servletContext.getMinorVersion

      def getEffectiveMajorVersion: Int = servletContext.getEffectiveMajorVersion

      def getEffectiveMinorVersion: Int = servletContext.getEffectiveMinorVersion

      def getMimeType(file: String): String = servletContext.getMimeType(file)

      def getResourcePaths(path: String): util.Set[String] = servletContext.getResourcePaths(path)

      def getResource(path: String): URL = servletContext.getResource(path)

      def getResourceAsStream(path: String): InputStream = servletContext.getResourceAsStream(path)

      def getRequestDispatcher(path: String): RequestDispatcher = servletContext.getRequestDispatcher(path)

      def getNamedDispatcher(name: String): RequestDispatcher = servletContext.getNamedDispatcher(name)

      def getServlet(name: String): Servlet = servletContext.getServlet(name)

      def getServlets: util.Enumeration[Servlet] = servletContext.getServlets

      def getServletNames: util.Enumeration[String] = servletContext.getServletNames

      def log(msg: String) = servletContext.log(msg)

      def log(exception: Exception, msg: String) = servletContext.log(exception, msg)

      def log(message: String, throwable: Throwable) = servletContext.log(message, throwable)

      def getRealPath(path: String): String = servletContext.getRealPath(path)

      def getServerInfo: String = servletContext.getServerInfo

      def getInitParameterNames: util.Enumeration[String] = servletContext.getInitParameterNames

      def setInitParameter(name: String, value: String): Boolean = servletContext.setInitParameter(name, value)

      def getAttribute(name: String): AnyRef = servletContext.getAttribute(name)

      def getAttributeNames: util.Enumeration[String] = servletContext.getAttributeNames

      def setAttribute(name: String, `object`: scala.Any) = servletContext.setAttribute(name, `object`)

      def removeAttribute(name: String) = servletContext.removeAttribute(name)

      def getServletContextName: String = servletContext.getServletContextName

      def addServlet(servletName: String, className: String): ServletRegistration.Dynamic = servletContext.addServlet(servletName, className)

      def addServlet(servletName: String, servlet: Servlet): ServletRegistration.Dynamic = servletContext.addServlet(servletName, servlet)

      def addServlet(servletName: String, servletClass: Class[_ <: Servlet]): ServletRegistration.Dynamic = servletContext.addServlet(servletName, servletClass)

      def createServlet[T <: Servlet](clazz: Class[T]): T = servletContext.createServlet(clazz)

      def getServletRegistration(servletName: String): ServletRegistration = servletContext.getServletRegistration(servletName)

      def getServletRegistrations: util.Map[String, _ <: ServletRegistration] = servletContext.getServletRegistrations

      def addFilter(filterName: String, className: String): Dynamic = servletContext.addFilter(filterName, className)

      def addFilter(filterName: String, filter: Filter): Dynamic = servletContext.addFilter(filterName, filter)

      def addFilter(filterName: String, filterClass: Class[_ <: Filter]): Dynamic = servletContext.addFilter(filterName, filterClass)

      def createFilter[T <: Filter](clazz: Class[T]): T = servletContext.createFilter(clazz)

      def getFilterRegistration(filterName: String): FilterRegistration = servletContext.getFilterRegistration(filterName)

      def getFilterRegistrations: util.Map[String, _ <: FilterRegistration] = servletContext.getFilterRegistrations

      def getSessionCookieConfig: SessionCookieConfig = servletContext.getSessionCookieConfig

      def setSessionTrackingModes(sessionTrackingModes: util.Set[SessionTrackingMode]) = servletContext.setSessionTrackingModes(sessionTrackingModes)

      def getDefaultSessionTrackingModes: util.Set[SessionTrackingMode] = servletContext.getDefaultSessionTrackingModes

      def getEffectiveSessionTrackingModes: util.Set[SessionTrackingMode] = servletContext.getEffectiveSessionTrackingModes

      def addListener(className: String) = servletContext.addListener(className)

      def addListener[T <: EventListener](t: T) = servletContext.addListener(t)

      def addListener(listenerClass: Class[_ <: EventListener]) = servletContext.addListener(listenerClass)

      def createListener[T <: EventListener](clazz: Class[T]): T = servletContext.createListener(clazz)

      def getJspConfigDescriptor: JspConfigDescriptor = servletContext.getJspConfigDescriptor

      def getClassLoader: ClassLoader = servletContext.getClassLoader

      def declareRoles(roleNames: String*): Unit = servletContext.declareRoles(roleNames: _*)
    })
  }
}