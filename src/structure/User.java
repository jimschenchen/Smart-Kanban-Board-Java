package structure;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;

import com.google.gson.*;

/**
 * The class {@code User} instance represents an Account which stores the users informations and
 * provides needed methods to interact.
 *
 * @author Cloudy Young, Jimschenchen
 * @since 1.0
 * @version 4.0
 */
public class User {

  /** The current signed-in user instance. */
  private static User current;

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
  private boolean existing = false;

  /** A list of int to storage the available hours which user plans to strive per day */
  private ArrayList<Integer> availability;

  /** The current theme name */
  private String theme;

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
  private void setUsername(String username) {
    this.username = username;
  }

  /**
   * Sets the password of the account.
   *
   * @param password the password to be set
   */
  private void setPassword(String password) {
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
   * Returns the current theme of user.
   *
   * @return the theme String
   */
  private String getTheme() {
    return this.theme;
  }

  /**
   * Sets the current theme of user.
   *
   * @param theme current theme of user.
   */
  private void setTheme(String theme) {
    this.theme = theme;
  }

  /**
   * Returns Available house of users. Note: The index of 0 is Sunday
   *
   * @return the arraylist contains the available houses from Mon to Sun
   */
  public ArrayList<Integer> getAvailability() {
    return new ArrayList<Integer>(this.availability);
  }

  /**
   * Returns Available house of users on current day. Note: The index of 1 is Sunday 2 is Monday
   *
   * @return Available house of users on current day
   */
  public Integer getTodayAvailability() {

    Calendar c = Calendar.getInstance();
    Integer dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
    return this.getAvailability().get(dayOfWeek - 1);
  }

  /**
   * Sets the current theme of user.
   *
   * @param availability the current theme of user.
   */
  private void setAvailability(ArrayList<Integer> availability) {
    this.availability = availability;
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
  private void setId(Integer id) {
    this.id = id;
  }

  /**
   * Sets the session id of the account.
   *
   * @param sessionId the session id to be set
   */
  private void setSessionId(String sessionId) {
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
   * @param username the username of the account
   * @param password the pssword of the account
   * @return the {@code Result} instance of this action
   * @since 1.0
   * @version 4.0
   */
  public Result signInRequest(String username, String password) {
    Result res = new Result();

    this.setUsername(username);
    this.setPassword(password);

    // Request server to sign in
    HttpBody param = new HttpBody();
    param.put("username", this.getUsername());
    param.put("password", this.getPassword());

    HttpRequest req = new HttpRequest();
    req.setRequestUrl("/users/authentication");
    req.setRequestMethod("PUT");
    req.setRequestBody(param);
    req.send();

    res.add(req);

    if (req.isSucceeded()) {
      HttpBody response = req.getResponseBody();
      HttpBody cookie = req.getResponseCookie();

      response.put("password", this.getPassword());
      writeLocalFile(response);

      this.setId(response.getInt("id"));
      this.setSessionId(cookie.getString("PHPSESSID"));

      ArrayList<Integer> availability = new ArrayList<Integer>();
      Collection<Object> c = response.getList("availability").values();
      for (Object i : c) {
        Integer a = ((Double) i).intValue();
        availability.add(a);
      }
      this.setAvailability(availability);
      this.setTheme(response.getString("theme"));
      this.existing = true;

      // Sign in locally
      this.authenticated = true;
      User.current = this;
    }

    return res;
  }

  /**
   * Authenticates the user account with the local file.
   *
   * @version 4.0
   * @return the {@code Result} instance of this action
   */
  public Result signInLocalRequest() {

    HttpBody body = User.readLocalFile();

    if (body == null) {
      Result res = new Result();
      StructureRequest req = new StructureRequest(false, true, false, this);
      res.add(req);
      return res;
    }

    String username = body.getString("username");
    String password = body.getString("password");

    return this.signInRequest(username, password);
  }

  /**
   * Returns the existing status of the instance account.
   *
   * @return the boolean to represents the account existing status
   * @since 2.0
   * @version 2.1
   */
  public boolean isExisting() {
    return this.existing;
  }

  /**
   * Signs out the user account.
   *
   * @version 4.0
   * @return the {@code Result} instance of this action
   */
  public Result signOut() {
    this.authenticated = false;
    User.current = null;

    StructureRequest req = new StructureRequest(true, false, false, this);
    Result res = new Result();
    res.add(req);
    File myObj = new File("temp.meonc");
    myObj.delete();
    return res;
  }

  /**
   * Writes the given {@code HttpBody} object into a file.
   *
   * @version 4.0
   * @param body the {@code HttpBody} to write
   */
  private static void writeLocalFile(Map<?, ?> body) {
    try {
      FileWriter myWriter = new FileWriter("temp.meonc");
      String str = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().toJson(body);
      myWriter.write(EncrytionUtils.encrypt(str, "secret"));
      myWriter.close();
    } catch (IOException e) {
      // e.printStackTrace();
      // Fail silently
    }
  }

  /**
   * Reads the given {@code HttpBody} object from a file.
   *
   * @version 4.0
   * @return the {@code HttpBody} body read
   */
  private static HttpBody readLocalFile() {
    int ch;
    FileReader fr = null;
    String encryptStr = "";
    try {
      fr = new FileReader("temp.meonc");

      // read from FileReader till the end of file
      while ((ch = fr.read()) != -1) encryptStr += (char) ch;

      // close the file
      fr.close();

      String str = EncrytionUtils.decrypt(encryptStr, "secret");
      return new HttpBody(new Gson().fromJson(str, Map.class));
    } catch (FileNotFoundException e) {
      // Fail silently
    } catch (IOException e) {
      // Fail silently
    }
    return null;
  }

  /**
   * Registers the instance account in the server.
   *
   * @param username the username of the account
   * @param password the password of the account
   * @param secQues the security question of the account
   * @param secAns the security answer of the the account
   * @return the result object of this action
   * @since 2.0
   * @version 3.0
   */
  public Result signUpRequest(String username, String password, String secQues, String secAns) {
    Result res = new Result();

    this.setUsername(username);
    this.setPassword(password);

    HttpBody param = new HttpBody();
    param.put("username", this.getUsername());
    param.put("password", this.getPassword());
    param.put("security_question", secQues);
    param.put("security_answer", secAns);

    HttpRequest req = new HttpRequest();
    req.setRequestUrl("/users/");
    req.setRequestMethod("POST");
    req.setRequestBody(param);
    req.send();

    res.add(req);

    if (req.isSucceeded()) {
      this.signInRequest(username, password);
    }

    return res;
  }

  /**
   * Authenticates the user account with the server.
   *
   * @version 3.0
   * @param username the user username
   * @param password the user password
   * @return the {@code Result} instance of this action
   */
  public static Result authenticationRequest(String username, String password) {
    User user = new User();
    Result res = user.signInRequest(username, password);
    if (res.isSucceeded()) {
      User.current = user;
    }
    return res;
  }

  /**
   * Authenticates the user account in local file.
   *
   * @version 3.0
   * @return the {@code Result} instance of this action
   */
  public static Result authenticationLocalRequest() {
    User user = new User();
    Result res = user.signInLocalRequest();
    if (res.isSucceeded()) {
      User.current = user;
    }
    return res;
  }

  /**
   * Authenticates the user account with the server.
   *
   * @version 3.0
   * @param username the user username
   * @param password the user password
   * @param secQues the user security question
   * @param secAns the user security answer
   * @return the {@code Result} instance of this action
   */
  public static Result registrationRequest(
      String username, String password, String secQues, String secAns) {
    User user = new User();
    return user.signUpRequest(username, password, secQues, secAns);
  }

  /**
   * Returns the current signed in {@code User} instance.
   *
   * @version 4.0
   * @return the current signed in {@code User} instance.
   */
  public static User getCurrent() {
    return User.current;
  }
}
