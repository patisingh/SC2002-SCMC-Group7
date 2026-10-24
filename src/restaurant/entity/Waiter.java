package restaurant.entity;

/** Seats a queued customer, takes an order, or serves a READY order. One per turn. */
public class Waiter extends Staff {

    public ActionResult seat(Restaurant restaurant, Customer customer) {
        if (!canAct()) return ActionResult.fail("The Waiter has already done a task this turn.");
        if (customer == null) return ActionResult.fail("Nobody is waiting in the queue.");
        if (customer.getStatus() != CustomerStatus.WAITING)
            return ActionResult.fail(customer.getLabel() + " is not waiting in the queue.");
        Table table = restaurant.findFreeTable();
        if (table == null)
            return ActionResult.fail("Both tables are taken. A table frees up when its customer pays or leaves.");

        restaurant.seatFromQueue(customer, table);
        finishTask();
        return ActionResult.ok("Waiter seats " + customer.getLabel() + " at Table " + table.getNumber() + ".");
    }

    public ActionResult takeOrder(Restaurant restaurant, Customer customer) {
        if (!canAct()) return ActionResult.fail("The Waiter has already done a task this turn.");
        if (customer == null) return ActionResult.fail("No seated customer is waiting to order.");
        if (customer.getStatus() != CustomerStatus.SEATED)
            return ActionResult.fail(customer.getLabel() + " is not waiting to order.");

        MenuItem item = customer.chooseItem(restaurant.getMenu());   // the customer chooses, never the manager
        int bill = customer.billFor(item);                            // price locked now
        Order order = new Order(customer, item, bill);
        customer.placeOrder(order);
        finishTask();
        return ActionResult.ok(customer.getLabel() + " orders " + item.getName() + " ("
                + Money.format(bill) + ", " + item.getPrepUnits() + " prep unit"
                + (item.getPrepUnits() == 1 ? "" : "s") + ").");
    }

    public ActionResult serve(Restaurant restaurant, Order order, int turn) {
        if (!canAct()) return ActionResult.fail("The Waiter has already done a task this turn.");
        if (order == null)
            return ActionResult.fail("No order is READY yet. A dish the Chef finishes this turn can be served next turn.");
        if (!order.isReady()) return ActionResult.fail("That order is not READY yet.");

        order.markServed(turn);
        order.getCustomer().startEating();
        restaurant.recordServed();
        finishTask();
        return ActionResult.ok("Waiter serves " + order.getItem().getName() + " to "
                + order.getCustomer().getLabel() + ". Their satisfaction stops dropping.");
    }

    @Override
    public String getRoleName() { return "Waiter"; }
}
