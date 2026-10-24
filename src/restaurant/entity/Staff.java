package restaurant.entity;

public abstract class Staff {
    private final String role;
    private boolean busy;

    protected Staff(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public boolean canAct() {
        return !busy;
    }

    void startTurn() {
        busy = false;
    }

    protected void finishTask() {
        busy = true;
    }

    protected ActionResult alreadyActed() {
        return ActionResult.fail("The " + role + " has already done a task this turn.");
    }
}
