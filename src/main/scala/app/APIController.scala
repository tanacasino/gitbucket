package app

import org.json4s.jackson.Serialization
import org.scalatra.{BadRequest, Ok}
import org.slf4j.LoggerFactory

import model.Account
import service.RepositoryService.RepositoryInfo
import service.{RepositoryService, AccountService}
import util.UsersAuthenticator
import util.Implicits._



class APIController extends APIControllerBase

trait APIControllerBase extends ControllerBase
    with AccountService with UsersAuthenticator with RepositoryService {

  private val logger = LoggerFactory.getLogger(classOf[APIControllerBase])

  /*
   * Users
   */
  /**
   * Get the authenticated user
   * @see https://developer.github.com/v3/users/#get-the-authenticated-user
   */
  get("/api/v3/user")(usersOnly {
    context.loginAccount.map { account =>
      contentType = formats("json")
      Ok(Serialization.write(User(account)))
    } getOrElse(BadRequest())
  })


  /*
   * Miscellaneous
   * https://developer.github.com/v3/misc/
   */
  /**
   * Get your current rate limit status
   * @see https://developer.github.com/v3/rate_limit/#get-your-current-rate-limit-status
   */
  get("/api/v3/rate_limit") {
    val limit = Limit(5000, 4999, System.currentTimeMillis / 1000)
    val rateLimit = RateLimit(Resources(limit, limit), limit)
    response.setHeader("X-RateLimit-Limit", "5000")
    response.setHeader("X-RateLimit-Remaining", "4999")
    response.setHeader("X-RateLimit-Reset", (System.currentTimeMillis / 1000).toString)
    contentType = formats("json")
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
    getRepository(params("owner"), params("repo"), baseUrl) map { repository =>
      logger.warn(s"owner: ${params("owner")}, repo: ${params("repo")}")
      if(!repository.repository.isPrivate) {
        Ok(Serialization.write(Repo(context.loginAccount.get, repository)))  // not correct
      } else {
        context.loginAccount
          .filter(a => a.isAdmin)
          .filter(a => a.userName == repository.owner)
          .filter(a => getCollaborators(repository.owner, repository.name).contains(a.userName))
          .map { account =>
            Ok(Serialization.write(Repo(account, repository)))
          } getOrElse NotFound()
      }
    } getOrElse NotFound()
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
case class User(login: String,
                id: Long,
                avatar_url: String,
                gravatar_id: String,
                url: String,
                `type`: String,
                name: String,
                email: String)
object User {
  def apply(account: Account): User = {
    new User(id = 0,
      login = account.userName,
      avatar_url = "avatar_url_dummy",
      gravatar_id = "gravatar_id_dummy",
      url = "url_dummy_string",
      `type` = "User",
      name = account.fullName,
      email = account.mailAddress
    )
  }
}


case class Limit(limit: Int, remaining: Int, reset: Long)
case class Resources(core: Limit, search: Limit)
case class RateLimit(resources: Resources, rate: Limit)


case class Repo(id: Long,
                owner: User,
                name: String,
                full_name: String,
                description: String,
                `private`: Boolean,
                fork: Boolean,
                url: String
                )
object Repo {
  def apply(account: Account, repo: RepositoryInfo): Repo = {
    new Repo(
      id = 1,
      owner = User(account),
      name = repo.name,
      full_name = account.fullName,
      description = repo.repository.description.getOrElse(""),
      `private` = repo.repository.isPrivate,
      fork = repo.repository.originRepositoryName.isDefined,
      url = "dummy_url"
    )
  }
}
/*
{
  "id": 1296269,
  "owner": {
    "login": "octocat",
    "id": 1,
    "avatar_url": "https://github.com/images/error/octocat_happy.gif",
    "gravatar_id": "somehexcode",
    "url": "https://api.github.com/users/octocat",
    "html_url": "https://github.com/octocat",
    "followers_url": "https://api.github.com/users/octocat/followers",
    "following_url": "https://api.github.com/users/octocat/following{/other_user}",
    "gists_url": "https://api.github.com/users/octocat/gists{/gist_id}",
    "starred_url": "https://api.github.com/users/octocat/starred{/owner}{/repo}",
    "subscriptions_url": "https://api.github.com/users/octocat/subscriptions",
    "organizations_url": "https://api.github.com/users/octocat/orgs",
    "repos_url": "https://api.github.com/users/octocat/repos",
    "events_url": "https://api.github.com/users/octocat/events{/privacy}",
    "received_events_url": "https://api.github.com/users/octocat/received_events",
    "type": "User",
    "site_admin": false
  },
  "name": "Hello-World",
  "full_name": "octocat/Hello-World",
  "description": "This your first repo!",
  "private": false,
  "fork": false,
  "url": "https://api.github.com/repos/octocat/Hello-World",
  "html_url": "https://github.com/octocat/Hello-World",
  "clone_url": "https://github.com/octocat/Hello-World.git",
  "git_url": "git://github.com/octocat/Hello-World.git",
  "ssh_url": "git@github.com:octocat/Hello-World.git",
  "svn_url": "https://svn.github.com/octocat/Hello-World",
  "mirror_url": "git://git.example.com/octocat/Hello-World",
  "homepage": "https://github.com",
  "language": null,
  "forks_count": 9,
  "stargazers_count": 80,
  "watchers_count": 80,
  "size": 108,
  "default_branch": "master",
  "open_issues_count": 0,
  "has_issues": true,
  "has_wiki": true,
  "has_pages": false,
  "has_downloads": true,
  "pushed_at": "2011-01-26T19:06:43Z",
  "created_at": "2011-01-26T19:01:12Z",
  "updated_at": "2011-01-26T19:14:43Z",
  "permissions": {
    "admin": false,
    "push": false,
    "pull": true
  },
  "subscribers_count": 42,
  "organization": {
    "login": "octocat",
    "id": 1,
    "avatar_url": "https://github.com/images/error/octocat_happy.gif",
    "gravatar_id": "somehexcode",
    "url": "https://api.github.com/users/octocat",
    "html_url": "https://github.com/octocat",
    "followers_url": "https://api.github.com/users/octocat/followers",
    "following_url": "https://api.github.com/users/octocat/following{/other_user}",
    "gists_url": "https://api.github.com/users/octocat/gists{/gist_id}",
    "starred_url": "https://api.github.com/users/octocat/starred{/owner}{/repo}",
    "subscriptions_url": "https://api.github.com/users/octocat/subscriptions",
    "organizations_url": "https://api.github.com/users/octocat/orgs",
    "repos_url": "https://api.github.com/users/octocat/repos",
    "events_url": "https://api.github.com/users/octocat/events{/privacy}",
    "received_events_url": "https://api.github.com/users/octocat/received_events",
    "type": "Organization",
    "site_admin": false
  },
  "parent": {
    "id": 1296269,
    "owner": {
      "login": "octocat",
      "id": 1,
      "avatar_url": "https://github.com/images/error/octocat_happy.gif",
      "gravatar_id": "somehexcode",
      "url": "https://api.github.com/users/octocat",
      "html_url": "https://github.com/octocat",
      "followers_url": "https://api.github.com/users/octocat/followers",
      "following_url": "https://api.github.com/users/octocat/following{/other_user}",
      "gists_url": "https://api.github.com/users/octocat/gists{/gist_id}",
      "starred_url": "https://api.github.com/users/octocat/starred{/owner}{/repo}",
      "subscriptions_url": "https://api.github.com/users/octocat/subscriptions",
      "organizations_url": "https://api.github.com/users/octocat/orgs",
      "repos_url": "https://api.github.com/users/octocat/repos",
      "events_url": "https://api.github.com/users/octocat/events{/privacy}",
      "received_events_url": "https://api.github.com/users/octocat/received_events",
      "type": "User",
      "site_admin": false
    },
    "name": "Hello-World",
    "full_name": "octocat/Hello-World",
    "description": "This your first repo!",
    "private": false,
    "fork": true,
    "url": "https://api.github.com/repos/octocat/Hello-World",
    "html_url": "https://github.com/octocat/Hello-World",
    "clone_url": "https://github.com/octocat/Hello-World.git",
    "git_url": "git://github.com/octocat/Hello-World.git",
    "ssh_url": "git@github.com:octocat/Hello-World.git",
    "svn_url": "https://svn.github.com/octocat/Hello-World",
    "mirror_url": "git://git.example.com/octocat/Hello-World",
    "homepage": "https://github.com",
    "language": null,
    "forks_count": 9,
    "stargazers_count": 80,
    "watchers_count": 80,
    "size": 108,
    "default_branch": "master",
    "open_issues_count": 0,
    "has_issues": true,
    "has_wiki": true,
    "has_pages": false,
    "has_downloads": true,
    "pushed_at": "2011-01-26T19:06:43Z",
    "created_at": "2011-01-26T19:01:12Z",
    "updated_at": "2011-01-26T19:14:43Z",
    "permissions": {
      "admin": false,
      "push": false,
      "pull": true
    }
  },
  "source": {
    "id": 1296269,
    "owner": {
      "login": "octocat",
      "id": 1,
      "avatar_url": "https://github.com/images/error/octocat_happy.gif",
      "gravatar_id": "somehexcode",
      "url": "https://api.github.com/users/octocat",
      "html_url": "https://github.com/octocat",
      "followers_url": "https://api.github.com/users/octocat/followers",
      "following_url": "https://api.github.com/users/octocat/following{/other_user}",
      "gists_url": "https://api.github.com/users/octocat/gists{/gist_id}",
      "starred_url": "https://api.github.com/users/octocat/starred{/owner}{/repo}",
      "subscriptions_url": "https://api.github.com/users/octocat/subscriptions",
      "organizations_url": "https://api.github.com/users/octocat/orgs",
      "repos_url": "https://api.github.com/users/octocat/repos",
      "events_url": "https://api.github.com/users/octocat/events{/privacy}",
      "received_events_url": "https://api.github.com/users/octocat/received_events",
      "type": "User",
      "site_admin": false
    },
    "name": "Hello-World",
    "full_name": "octocat/Hello-World",
    "description": "This your first repo!",
    "private": false,
    "fork": true,
    "url": "https://api.github.com/repos/octocat/Hello-World",
    "html_url": "https://github.com/octocat/Hello-World",
    "clone_url": "https://github.com/octocat/Hello-World.git",
    "git_url": "git://github.com/octocat/Hello-World.git",
    "ssh_url": "git@github.com:octocat/Hello-World.git",
    "svn_url": "https://svn.github.com/octocat/Hello-World",
    "mirror_url": "git://git.example.com/octocat/Hello-World",
    "homepage": "https://github.com",
    "language": null,
    "forks_count": 9,
    "stargazers_count": 80,
    "watchers_count": 80,
    "size": 108,
    "default_branch": "master",
    "open_issues_count": 0,
    "has_issues": true,
    "has_wiki": true,
    "has_pages": false,
    "has_downloads": true,
    "pushed_at": "2011-01-26T19:06:43Z",
    "created_at": "2011-01-26T19:01:12Z",
    "updated_at": "2011-01-26T19:14:43Z",
    "permissions": {
      "admin": false,
      "push": false,
      "pull": true
    }
  }
}
 */