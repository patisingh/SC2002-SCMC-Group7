package restaurant.entity;

/** The rule every staff member shares: one task per turn. */
public abstract class Staff {
    private boolean actedThisTurn = false;

    public boolean canAct() {
        return !actedThisTurn;
    }

    public void startTurn() {
        actedThisTurn = false;
    }

    protected void finishTask() {
        actedThisTurn = true;
    }

    public abstract String getRoleName();
}
