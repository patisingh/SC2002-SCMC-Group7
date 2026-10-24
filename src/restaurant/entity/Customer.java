package restaurant.entity;

import java.util.List;

public abstract class Customer {

    public static final int MAX_SATISFACTION = 100;

    private final int id;
    private final int arrivalTurn;
    private int satisfaction = 100;
    private CustomerStatus status = CustomerStatus.WAITING;
    private Order order;
    private int readyToPayTurn = -1;

    protected Customer(int id, int arrivalTurn) {
        this.id = id;
        this.arrivalTurn = arrivalTurn;
    }

    protected abstract int waitingPenalty();

    public abstract String getTypeName();

    public MenuItem chooseItem(Menu menu) {
        List<MenuItem> items = menu.getItems();
        int index = id % items.size();
        return items.get(index);
    }

    public int billFor(MenuItem item) {
        return item.getPriceCents();
    }

    public boolean isWaitingForFood() {
        return status == CustomerStatus.WAITING
                || status == CustomerStatus.SEATED
                || status == CustomerStatus.ORDERED;
    }

    public void loseWaitingSatisfaction() {
        if (isWaitingForFood()) {
            satisfaction -= waitingPenalty();

            if (satisfaction < 0) {
                satisfaction = 0;
            }
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

        if (order != null) {
            if (order.getStatus() != OrderStatus.SERVED
                    && order.getStatus() != OrderStatus.PAID) {
                order.cancel();
            }
        }
    }

    public String getLabel() {
        return "C" + id + " (" + getTypeName() + ")";
    }

    public int getId() {
        return id;
    }

    public int getArrivalTurn() {
        return arrivalTurn;
    }

    public int getSatisfaction() {
        return satisfaction;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public Order getOrder() {
        return order;
    }

    public int getReadyToPayTurn() {
        return readyToPayTurn;
    }
}
