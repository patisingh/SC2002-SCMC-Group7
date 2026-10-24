package restaurant.entity;

/** One customer's food: what it is, how far it is cooked, and its locked-in bill. */
public class Order {
    private final Customer customer;
    private final MenuItem item;
    private final int billCents;     // worked out once, when the order is taken; never changes
    private int unitsDone = 0;
    private OrderStatus status = OrderStatus.PLACED;
    private int servedTurn = -1;

    public Order(Customer customer, MenuItem item, int billCents) {
        this.customer = customer;
        this.item = item;
        this.billCents = billCents;
    }

    public boolean canBeCooked() {
        return status == OrderStatus.PLACED || status == OrderStatus.IN_PREPARATION;
    }

    public void prepareOneUnit() {
        unitsDone++;
        status = (unitsDone >= item.getPrepUnits()) ? OrderStatus.READY : OrderStatus.IN_PREPARATION;
    }

    public boolean isReady() {
        return status == OrderStatus.READY;
    }

    public void markServed(int turn) {
        status = OrderStatus.SERVED;
        servedTurn = turn;
    }

    public void markPaid() {
        status = OrderStatus.PAID;
    }

    public void cancel() {
        status = OrderStatus.CANCELLED;
    }

    public String getProgress() {
        return unitsDone + "/" + item.getPrepUnits();
    }

    public Customer getCustomer() { return customer; }
    public MenuItem getItem() { return item; }
    public int getBillCents() { return billCents; }
    public OrderStatus getStatus() { return status; }
    public int getServedTurn() { return servedTurn; }
}
