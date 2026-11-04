package restaurant.entity;

public class Waiter extends SeatingStaff {

    public Waiter() {
        super("Waiter");
    }

    public ActionResult takeOrder(Customer customer, Menu menu) {
        return takeOrder(customer, menu, new HappyHour());
    }

    public ActionResult takeOrder(Customer customer, Menu menu, HappyHour pricing) {
        if (!canAct()) return alreadyActed();
        if (customer == null) return ActionResult.fail("No seated customer is waiting to order.");
        if (customer.getStatus() != CustomerStatus.SEATED) {
            return ActionResult.fail(customer.getLabel() + " is not waiting to order.");
        }

        MenuItem item = customer.chooseItem(menu);
        Order order = new Order(customer, item, pricing.billFor(customer, item));
        customer.placeOrder(order);
        finishTask();
        int units = item.getPrepUnits();
        return ActionResult.ok(customer.getLabel() + " orders " + item.getName() + " ("
                + Money.format(order.getBillCents()) + ", " + units + " prep unit" + (units == 1 ? "" : "s") + ").");
    }

    public ActionResult serve(Restaurant restaurant, Order order, int turn) {
        if (!canAct()) return alreadyActed();
        if (order == null) {
            return ActionResult.fail("No order is READY yet. A dish the Chef finishes this turn can be served next turn.");
        }
        if (!order.isReady()) return ActionResult.fail("That order is not READY yet.");

        order.getCustomer().receiveFood(turn);
        restaurant.recordServed();
        finishTask();
        return ActionResult.ok("Waiter serves " + order.getItem().getName() + " to "
                + order.getCustomer().getLabel() + ". Their satisfaction stops dropping.");
    }
}
