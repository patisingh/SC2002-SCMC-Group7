package restaurant.entity;

public class Order {
    private final Customer customer;
    private final MenuItem item;
    private final int billCents;
    private int unitsDone;
    private OrderStatus status = OrderStatus.PLACED;
    private int servedTurn = -1;

    Order(Customer customer, MenuItem item, int billCents) {
        this.customer = customer;
        this.item = item;
        this.billCents = billCents;
    }

    public boolean canBeCooked() {
        return status == OrderStatus.PLACED || status == OrderStatus.IN_PREPARATION;
    }

    public boolean isReady() {
        return status == OrderStatus.READY;
    }

    void prepareOneUnit() {
        if (!canBeCooked()) throw new IllegalStateException("Order is already " + status);
        unitsDone++;
        status = unitsDone >= item.getPrepUnits() ? OrderStatus.READY : OrderStatus.IN_PREPARATION;
    }

    void markServed(int turn) {
        if (status != OrderStatus.READY) throw new IllegalStateException("Cannot serve an order that is " + status);
        status = OrderStatus.SERVED;
        servedTurn = turn;
    }

    void markPaid() {
        if (status != OrderStatus.SERVED) throw new IllegalStateException("Cannot charge for an order that is " + status);
        status = OrderStatus.PAID;
    }

    void cancel() {
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
