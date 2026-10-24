package restaurant.entity;

/** Where a customer is in their visit. */
public enum CustomerStatus {
    WAITING,       // in the queue, no table yet
    SEATED,        // at a table, order not taken yet
    ORDERED,       // order taken, waiting for food
    EATING,        // food served
    READY_TO_PAY,  // finished eating, waiting for the Cashier
    PAID,          // paid and gone
    LEFT           // satisfaction hit 0, left without paying
}
