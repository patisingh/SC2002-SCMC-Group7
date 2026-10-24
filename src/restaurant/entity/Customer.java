package restaurant.entity;

import java.util.List;

/**
 * What every customer has in common. A subclass only says what is different about it
 * (in Stage 1, just how fast it loses satisfaction).
 */
public abstract class Customer {
    private static final int MAX_SATISFACTION = 100;

    private final int id;
    private int satisfaction = MAX_SATISFACTION;
    private CustomerStatus status = CustomerStatus.WAITING;
    private Order order;
    private int readyToPayTurn = -1;

    protected Customer(int id) {
        this.id = id;
    }

    /** Satisfaction lost for every turn spent waiting. */
    protected abstract int waitingPenalty();

    public abstract String getTypeName();

    /**
     * Customer n picks menu item number (n mod menu size), so every run is the same.
     * With Salad, Burger, Pasta: C1 Burger, C2 Pasta, C3 Salad, C4 Burger, C5 Pasta, C6 Salad.
     */
    public MenuItem chooseItem(Menu menu) {
        List<MenuItem> items = menu.getItems();
        return items.get(id % items.size());
    }

    public int billFor(MenuItem item) {
        return item.getPriceCents();
    }

    /** In the queue, or at a table but not served yet. */
    public boolean isWaitingForFood() {
        return status == CustomerStatus.WAITING
                || status == CustomerStatus.SEATED
                || status == CustomerStatus.ORDERED;
    }

    void loseWaitingSatisfaction() {
        if (isWaitingForFood()) {
            satisfaction = Math.max(0, satisfaction - waitingPenalty());
        }
    }

    // The visit can only move forward one step at a time, so for example nobody can
    // eat before being served or pay before they are READY_TO_PAY.

    void sitDown() {
        moveTo(CustomerStatus.WAITING, CustomerStatus.SEATED);
    }

    void placeOrder(Order newOrder) {
        moveTo(CustomerStatus.SEATED, CustomerStatus.ORDERED);
        order = newOrder;
    }

    void receiveFood(int turn) {
        moveTo(CustomerStatus.ORDERED, CustomerStatus.EATING);
        order.markServed(turn);
    }

    void finishEating(int turn) {
        moveTo(CustomerStatus.EATING, CustomerStatus.READY_TO_PAY);
        readyToPayTurn = turn;
    }

    void pay() {
        moveTo(CustomerStatus.READY_TO_PAY, CustomerStatus.PAID);
        order.markPaid();
    }

    void leave() {
        if (!isWaitingForFood()) {
            throw new IllegalStateException(getLabel() + " has already been served");
        }
        status = CustomerStatus.LEFT;
        if (order != null) {
            order.cancel();
        }
    }

    private void moveTo(CustomerStatus expected, CustomerStatus next) {
        if (status != expected) {
            throw new IllegalStateException(getLabel() + " is " + status + ", expected " + expected);
        }
        status = next;
    }

    public String getLabel() {
        return "C" + id + " (" + getTypeName() + ")";
    }

    public int getId() { return id; }

    public int getSatisfaction() { return satisfaction; }

    public CustomerStatus getStatus() { return status; }

    public Order getOrder() { return order; }

    int getReadyToPayTurn() { return readyToPayTurn; }
}
