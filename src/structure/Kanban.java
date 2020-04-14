package structure;

import java.util.ArrayList;

/**
 * The {@code Kanban} class, extends from {@code Node}.
 *
 * <p>
 * The instance should contains {@code Board} object as children nodes.
 *
 * @since 1.0
 * @version 2.1
 */
public class Kanban extends Node {

  /** The current Kanban object */
  private static Kanban current;

  private Board today;
  private Column todayToDo;
  private Column todayInProgress;
  private Column todayDone;

  /**
   * Constructor of {@code Kanban}
   *
   * @param obj the {@code HttpBody} object to map
   */
  protected Kanban(HttpBody obj) {
    super(obj);

    this.today = new Board("Today",
        TimeUtils.currentMonthName() + " " + TimeUtils.currentDay() + ", " + TimeUtils.currentYear(), "#fd79a8", this);

    this.todayToDo = new Column("To Do", "jimjimsjimshtodo", 0, today);
    this.todayInProgress = new Column("In Progress", "", 1, today);
    this.todayDone = new Column("Done", "", 2, today);

    this.today.setSpecialized(true);
    this.todayToDo.setSpecialized(true);
    this.todayInProgress.setSpecialized(true);
    this.todayDone.setSpecialized(true);
  }

  /**
   * Check out the {@code Kanban} data of current {@code User} from the server.
   *
   * <p>
   * This is an <i>action</i> for controllers.
   *
   * @return the result object of this action in {@code Result}
   * @since 2.0
   * @version 2.0
   */
  public static Result checkout() {
    Result res = new Result();
    if (User.getCurrent() == null) {
      StructureRequest req2 = new StructureRequest(false, true);
      req2.setErrorMessage("User not authenticated");
      res.add(req2);
    } else {
      HttpBody cookie = new HttpBody();
      cookie.put("PHPSESSID", User.getCurrent().getSessionId());

      HttpRequest req2 = new HttpRequest();
      req2.setRequestUrl("/kanban");
      req2.setRequestMethod("GET");
      req2.setRequestCookie(cookie);
      req2.send();

      if (req2.isSucceeded()) {
        Kanban.current = new Kanban(req2.getResponseBody());
      }
      res.add(req2);
    }
    return res;
  }

  /*
   * Today
   */
  public void generateToday() {

    this.todayToDo.clearNodes();
    this.todayInProgress.clearNodes();
    this.todayDone.clearNodes();

    Column candicates = new Column("temp", "temp", 0, null); // candidates column, store tasks which may be added into today as new
    candicates.setSpecialized(true);

    // Adding all the generated events
    for (Node board : this.getNodes()) {
      if (board.isSpecialized()) {
        continue;
      }

      for (Node column_node : board.getNodes()) {
        for (Node event_node : column_node.getNodes()) {

          Event event = (Event) event_node;
          Column column = (Column) column_node;

          switch(column.getPreset()){

            case Column.TO_DO: // For To Do list: include the past and present task and store the rest as candicates
              if(event.isOnGeneratedToday() || event.isBeforeGeneratedToday()){
                event.setParent(this.todayToDo);
              }else{
                event.setParent(candicates);
              }

            case Column.IN_PROGRESS: // For in Progress list: include the past and present tasks only
              if(event.isOnGeneratedToday() || event.isBeforeGeneratedToday()){
                event.setParent(this.todayInProgress);
              }
              break;

            case Column.DONE: // For Done list: include only the present tasks
              if(event.isOnGeneratedToday()){
                event.setParent(this.todayDone);
              }

          }
        }
      }
    }

    // second loop to add new available events that has not generated today
    for (Node node : candicates.getNodes(Node.SORT_BY_PRIORITY, Node.ORDER_BY_ASC)) {
      Event event = (Event) node;
      if (this.hasEnoughTime((Event) event)) {
        // is has enough time add it
        event.setParent(this.todayToDo);
        // update its lastGeneratedDate to current time
        event.setLastGeneratedDateRequest();
      }
    }
  }

  public boolean hasEnoughTime(Event eventNext) {
    Long totalTime = Long.valueOf(User.getCurrent().getTodayAvailability()) * 3600;
    Long timeAccumulator = 0l;
    for (Node node : this.todayToDo.getNodes()) {
      final Event event = (Event) node;
      timeAccumulator += event.getDurationValue();
    }
    for (Node node : this.todayInProgress.getNodes()) {
      final Event event = (Event) node;
      timeAccumulator += event.getDurationValue();
    }
    for (Node node : this.todayDone.getNodes()) {
      final Event event = (Event) node;
      timeAccumulator += event.getDurationValue();
    }
    return (eventNext.getDurationValue() + timeAccumulator) <= totalTime ? true : false;
  }

  /*
   * Overview
   */
  public void generateOverview() {

    this.todayToDo.clearNodes();
    this.todayInProgress.clearNodes();
    this.todayDone.clearNodes();
    Column tempColumn = new Column("temp", "temp", 0, null);
    tempColumn.setSpecialized(true);

    // All todo
    for (Node board : this.getNodes()) {
      if (!board.isSpecialized()) {
        for (Node node : board.getNodes()) {
          Column column = (Column) node;
          for (Node event : column.getNodes()) {
            System.out.println("---- TODAY ----");
            if (column.getPreset() == Column.TO_DO) {
              // this.todayToDo.addNode(event);
              event.setParent(tempColumn);
            } else if (column.getPreset() == Column.IN_PROGRESS) {
              // this.todayInProgress.addNode(event);
              event.setParent(this.todayInProgress);
            } else {
              if (!((Event) event).isOverdue()) {
                event.setParent(this.todayDone);
              }
              // this.todayDone.addNode(event);
            }
          }
        }
      }
    }

    for (Node node : tempColumn.getNodes(Node.SORT_BY_PRIORITY, Node.ORDER_BY_ASC)) {
      Event event = (Event) node;
      System.out.println(event.getTitle() + ": " + event.getPriority());
      if (todayToDo.hasEnoughTime((Event) event)) {
        event.setParent(this.todayToDo);
      }
    }
  }

  public static Kanban getCurrent() {
    return Kanban.current;
  }

  /*
   * Search
   *
   */
  public ArrayList<Node> search(String name) {
    ArrayList<Node> ret = new ArrayList<Node>();
    Kanban kanban = Kanban.current;

    for (Node board : kanban.getNodes()) {
      // excluded Today
      if (!board.isSpecialized()) {
        for (Node column : board.getNodes()) {
          for (Node nodeEvent : column.getNodes()) {
            Event event = (Event) nodeEvent;
            if (event.getTitle().toLowerCase().indexOf(name.toLowerCase()) != -1
                || event.getNote().toLowerCase().indexOf(name.toLowerCase()) != -1
                || Integer.toString(event.getId()).indexOf(name) != -1) {
              ret.add(event);
            }
          }
        }
      }
    }
    return ret;
  }
}
