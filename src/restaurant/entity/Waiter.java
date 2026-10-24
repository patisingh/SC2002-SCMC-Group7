package restaurant.entity;

import java.util.Optional;

public class Waiter extends Staff {

    public Waiter() {
        super("Waiter");
    }

    public ActionResult seat(Restaurant restaurant, Customer customer) {
        if (!canAct()) return alreadyActed();
        if (customer == null) return ActionResult.fail("Nobody is waiting in the queue.");
        if (customer.getStatus() != CustomerStatus.WAITING) {
            return ActionResult.fail(customer.getLabel() + " is not waiting in the queue.");
        }
        Optional<Table> table = restaurant.findFreeTable();
        if (table.isEmpty()) {
            return ActionResult.fail("All tables are taken. A table frees up when its customer pays or leaves.");
        }

        restaurant.seat(customer, table.get());
        finishTask();
        return ActionResult.ok("Waiter seats " + customer.getLabel() + " at Table " + table.get().getNumber() + ".");
    }

    public ActionResult takeOrder(Customer customer, Menu menu) {
        if (!canAct()) return alreadyActed();
        if (customer == null) return ActionResult.fail("No seated customer is waiting to order.");
        if (customer.getStatus() != CustomerStatus.SEATED) {
            return ActionResult.fail(customer.getLabel() + " is not waiting to order.");
        }

        MenuItem item = customer.chooseItem(menu);                        // the customer decides, not the manager
        Order order = new Order(customer, item, customer.billFor(item));   // price is locked in here
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
