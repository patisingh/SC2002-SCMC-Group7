package restaurant.entity;

/** What a staff action hands back: did it work, and a message saying what happened or why not. */
public class ActionResult {
    private final boolean success;
    private final String message;

    private ActionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ActionResult ok(String message) {
        return new ActionResult(true, message);
    }

    public static ActionResult fail(String reason) {
        return new ActionResult(false, reason);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
