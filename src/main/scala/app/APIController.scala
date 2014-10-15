package app

import org.scalatra.{Ok, InternalServerError}
import org.slf4j.LoggerFactory


class APIController extends APIControllerBase

trait APIControllerBase extends ControllerBase {

  private val logger = LoggerFactory.getLogger(classOf[APIControllerBase])

  /*
   * Users
   */
  /**
   * Get the authenticated user
   * @see https://developer.github.com/v3/users/#get-the-authenticated-user
   */
  get("/api/v3/user") {
    Ok("/api/v3/user")
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
    Ok("/api/v3/rate_limit")
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

