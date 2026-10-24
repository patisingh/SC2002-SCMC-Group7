package restaurant.entity;

import java.util.List;

/**
 * What every customer has in common. Subclasses only change what differs
 * (in Stage 1 that is just the satisfaction lost per waiting turn).
 */
public abstract class Customer {
    public static final int MAX_SATISFACTION = 100;

    private final int id;
    private final int arrivalTurn;
    private int satisfaction = MAX_SATISFACTION;   // private: only changed through methods below
    private CustomerStatus status = CustomerStatus.WAITING;
    private Order order;
    private int readyToPayTurn = -1;

    protected Customer(int id, int arrivalTurn) {
        this.id = id;
        this.arrivalTurn = arrivalTurn;
    }

    /** How much satisfaction this kind of customer loses per waiting turn. */
    protected abstract int waitingPenalty();

    /** Short name of the kind of customer, e.g. "Regular". */
    public abstract String getTypeName();

    /**
     * Food choice rule (deterministic, so runs are reproducible):
     * customer n picks menu item number (n mod menu size).
     * With Salad, Burger, Pasta: C1 Burger, C2 Pasta, C3 Salad, C4 Burger, C5 Pasta, C6 Salad.
     */
    public MenuItem chooseItem(Menu menu) {
        List<MenuItem> items = menu.getItems();
        return items.get(id % items.size());
    }

    /** The bill for an item. Regular customers pay full price. */
    public int billFor(MenuItem item) {
        return item.getPriceCents();
    }

    /** Waiting in the queue, or seated but not yet served. */
    public boolean isWaitingForFood() {
        return status == CustomerStatus.WAITING
                || status == CustomerStatus.SEATED
                || status == CustomerStatus.ORDERED;
    }

    public void loseWaitingSatisfaction() {
        if (isWaitingForFood()) {
            satisfaction = Math.max(0, satisfaction - waitingPenalty());
        }
    }

    public void markSeated() {
        status = CustomerStatus.SEATED;
    }

    public void placeOrder(Order newOrder) {
        order = newOrder;
        status = CustomerStatus.ORDERED;
    }

    public void startEating() {
        status = CustomerStatus.EATING;
    }

    public void finishEating(int turn) {
        status = CustomerStatus.READY_TO_PAY;
        readyToPayTurn = turn;
    }

    public void markPaid() {
        status = CustomerStatus.PAID;
    }

    public void leave() {
        status = CustomerStatus.LEFT;
        if (order != null && order.getStatus() != OrderStatus.SERVED && order.getStatus() != OrderStatus.PAID) {
            order.cancel();
        }
    }

    public String getLabel() {
        return "C" + id + " (" + getTypeName() + ")";
    }

    public int getId() { return id; }
    public int getArrivalTurn() { return arrivalTurn; }
    public int getSatisfaction() { return satisfaction; }
    public CustomerStatus getStatus() { return status; }
    public Order getOrder() { return order; }
    public int getReadyToPayTurn() { return readyToPayTurn; }
}
