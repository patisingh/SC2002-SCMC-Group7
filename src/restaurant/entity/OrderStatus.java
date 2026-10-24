package restaurant.entity;

/** Where an order is. */
public enum OrderStatus {
    PLACED,          // taken by the Waiter, not cooked yet
    IN_PREPARATION,  // some preparation units done
    READY,           // all units done, can be served
    SERVED,          // given to the customer
    PAID,            // the customer has paid for it
    CANCELLED        // the customer left before it was finished
}
