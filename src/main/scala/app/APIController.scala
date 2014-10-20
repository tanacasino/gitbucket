package app

import org.scalatra.{BadRequest, Ok}
import org.slf4j.LoggerFactory
import service.AccountService


class APIController extends APIControllerBase

trait APIControllerBase extends ControllerBase with AccountService {

  private val logger = LoggerFactory.getLogger(classOf[APIControllerBase])

  /*
   * Users
   */
  /**
   * Get the authenticated user
   * @see https://developer.github.com/v3/users/#get-the-authenticated-user
   */
  get("/api/v3/user") {
    case class User(
      login: String,
      id: Long,
      avatar_url: String,
      gravatar_id: String,
      url: String,
      `type`: String,
      name: String,
      email: String)

    context.loginAccount.map { account =>
      val user = User(id = 0,
           login = account.userName,
           avatar_url = "avatar_url_dummy",
           gravatar_id = "gravatar_id_dummy",
           url = "url_dummy_string",
           `type` = "User",
           name = account.fullName,
           email = account.mailAddress
      )
      contentType = formats("json")
      Ok(org.json4s.jackson.Serialization.write(user))
    } getOrElse(BadRequest())
  }


  /*
   * Miscellaneous
   * https://developer.github.com/v3/misc/
   */
  /**
   * Get your current rate limit status
   * @see https://developer.github.com/v3/rate_limit/#get-your-current-rate-limit-status
   */
  get("/api/v3/rate_limit") {
    contentType = formats("json")
    val limit = Limit(5000, 4999, (System.currentTimeMillis / 1000))
    val rateLimit = RateLimit(Resources(limit, limit), limit)
    response.setHeader("X-RateLimit-Limit", "5000")
    response.setHeader("X-RateLimit-Remaining", "4999")
    response.setHeader("X-RateLimit-Reset", (System.currentTimeMillis / 1000).toString)
    Ok(org.json4s.jackson.Serialization.write(rateLimit))
  }


  /*
   * Repositories
   * https://developer.github.com/v3/repos/
   */
  /**
   * Get repository information
   * @see https://developer.github.com/v3/repos/#get
   */
  get("/api/v3/repos/:owner/:repo") {
    Ok("/api/v3/repos/:owner/:repo")
  }


  /*
   * Pull Requests
   * https://developer.github.com/v3/pulls/
   */
  /**
   * List pull requests
   * @see https://developer.github.com/v3/pulls/#list-pull-requests
   *
   */
  get("/api/v3/repos/:owner/:repo/pulls") {
    Ok("/api/v3/repos/:owner/:repo/pulls")
  }

  /**
   * Get a single pull request
   * @see https://developer.github.com/v3/pulls/#get-a-single-pull-request
   */
  get("/api/v3/repos/:owner/:repo/pulls/:number") {
    Ok("/api/v3/repos/:owner/:repo/pulls/:number")
  }


  /*
   * Statuses
   * https://developer.github.com/v3/repos/statuses
   */
  /**
   *
   * Create a Status
   * @see https://developer.github.com/v3/repos/statuses/#create-a-status
   */
  post("/api/v3/repos/:owner/:repo/statuses/:sha") {
    Ok("/api/v3/repos/:owner/:repo/statuses/:sha")
  }


  /*
   * Comments
   * https://developer.github.com/v3/issues/comments/
   */
  post("/api/v3/repos/:owner/:repo/issues/:number/comments") {
    Ok("/api/v3/repos/:owner/:repo/issues/:number/comments")
  }

}



// TODO Move other file
case class Limit(limit: Int, remaining: Int, reset: Long)
case class Resources(core: Limit, search: Limit)
case class RateLimit(resources: Resources, rate: Limit)
