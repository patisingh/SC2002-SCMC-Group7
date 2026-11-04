package restaurant.entity;

/** Once-per-game pricing policy. The controller advances it once at turn end. */
public class HappyHour {
    private boolean used;
    private int remainingTurns;

    public ActionResult activate() {
        if (used) return ActionResult.fail("Happy Hour has already been activated this game.");
        used = true;
        remainingTurns = 2;
        return ActionResult.ok("Happy Hour activated for this turn and the next.");
    }

    public boolean canActivate() { return !used; }

    public boolean isActive() { return remainingTurns > 0; }

    public int getRemainingTurns() { return remainingTurns; }

    public int billFor(Customer customer, MenuItem item) {
        int customerBill = customer.billFor(item);
        return isActive() ? Money.percentOf(customerBill, 80) : customerBill;
    }

    public void endTurn() {
        if (isActive()) remainingTurns--;
    }
}
