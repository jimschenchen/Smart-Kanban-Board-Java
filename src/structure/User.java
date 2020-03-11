package structure;

/**
 * The class {@code User} instance represents an Account which stores the users informations and
 * provides needed methods to interact.
 */
public class User {

  /** The current signed-in user instance. */
  public static User current;

  /** The username of the account. */
  private String username;

  /** The user id of the account. */
  private int id;

  /** The session id of the account. For the server requests to be proceeded properly. */
  private String sessionId;

  /** The password of the account. */
  private String password;

  /**
   * The authentication status of the account. {@code true} represents the instance user is
   * authenticated with the server successfully. {@code false} otherwise.
   */
  private boolean authenticated;

  /** A boolean to indicate if the account is existing on the server. */
  private boolean existing;

  /** Default constructor of {@code User}. */
  public User() {}

  /**
   * Returns the username of the account.
   *
   * @return the username of the account
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * Returns the user id of the account.
   *
   * @return the user id of the account
   */
  public int getId() {
    return this.id;
  }

  /**
   * Returns the session id of the account.
   *
   * @return the session id of the account
   */
  public String getSessionId() {
    return this.sessionId;
  }

  /**
   * Sets the username of the account.
   *
   * @param username the username to be set
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Sets the password of the account.
   *
   * @param password the password to be set
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Returns the password of the account.
   *
   * @return the password of the account
   */
  private String getPassword() {
    return this.password;
  }

  /**
   * Return the authentication status of the account.
   *
   * @return {@code true} if the instance user is authenticated with the server successfully. {@code
   *     false} otherwise.
   */
  public boolean isAuthenticated() {
    return this.authenticated;
  }

  /**
   * Sets the user id of the account.
   *
   * @param id the user id to be set
   */
  public void setId(Integer id) {
    this.id = id;
  }

  /**
   * Sets the session id of the account.
   *
   * @param sessionId the session id to be set
   */
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  /** {@inheritDoc} */
  public String toString() {
    return "User (username: "
        + this.getUsername()
        + ", password: "
        + this.getPassword()
        + ", id: "
        + this.getId()
        + ", sessionId: "
        + this.getSessionId()
        + ")";
  }

  /**
   * Authenticates the user account with the server.
   *
   * <p>This is an <i>action</i> for controllers.
   *
   * @return the result object of this action
   */
  public Result authenticate() {
    return this.authenticate(this.getUsername(), this.getPassword());
  }

  /**
   * Authenticates the user account with the server.
   *
   * <p>This is an <i>action</i> for controllers.
   *
   * @param username the username of the account
   * @param password the pssword of the account
   * @return the result object of this action
   */
  public Result authenticate(String username, String password) {
    this.setUsername(username);
    this.setPassword(password);

    Result res = new Result();
    HttpRequest req = this.authenticateRemote(username, password);
    res.add(req);

    if (req.isSucceeded()) {
      HttpBody response = req.getResponseBody();
      HttpBody cookie = req.getResponseCookie();

      this.setId(response.getInt("id"));
      this.setSessionId(cookie.getString("PHPSESSID"));
      this.existing = true;

      StructureRequest req2 = this.authenticateLocal();
      res.add(req2);
    }

    return res;
  }

  /**
   * Authenticates the instance account in the server.
   *
   * @param username the username of the account
   * @param password the password of the account
   * @return the http requests of the action
   */
  private HttpRequest authenticateRemote(String username, String password) {
    HttpBody param = new HttpBody();
    param.put("username", this.getUsername());
    param.put("password", this.getPassword());

    HttpRequest req = new HttpRequest();
    req.setRequestUrl("/users/authentication");
    req.setRequestMethod("PUT");
    req.setRequestBody(param);
    req.send();

    return req;
  }

  /**
   * Authenticates the instance account in local storage
   *
   * @return the structure request of the action
   */
  private StructureRequest authenticateLocal() {
    this.authenticated = true;
    User.current = this;
    StructureRequest req = new StructureRequest(true, false, this);
    return req;
  }

  /**
   * Registers the instance account in the server.
   *
   * <p>This is an <i>action</i> for controllers.
   *
   * @param username the username of the account
   * @param password the password of the account
   * @param sec_ques the security question of the account
   * @param sec_ans the security answer of the the account
   * @return the result object of this action
   */
  public Result resgister(String username, String password, String sec_ques, String sec_ans) {
    Result res = new Result();

    this.setUsername(username);
    this.setPassword(password);

    HttpBody param = new HttpBody();
    param.put("username", this.getUsername());
    param.put("password", this.getPassword());
    param.put("security_question", sec_ques);
    param.put("security_answer", sec_ans);

    HttpRequest req = new HttpRequest();
    req.setRequestUrl("/users/");
    req.setRequestMethod("POST");
    req.setRequestBody(param);
    req.send();

    res.add(req);

    if (req.isSucceeded()) {
      HttpRequest req2 = this.authenticateRemote(username, password);
      res.add(req2);
    }

    return res;
  }

  public static void main(String[] args) {
    User user = new User();
    user.authenticate("cloudy", "cloudy");
    System.out.println(user);

    Kanban.checkout();
    System.out.println(Kanban.current);
  }
}
