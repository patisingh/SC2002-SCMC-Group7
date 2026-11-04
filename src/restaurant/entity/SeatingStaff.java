package restaurant.entity;

import java.util.Optional;

public abstract class SeatingStaff extends Staff {
    protected SeatingStaff(String role) {
        super(role);
    }

    public ActionResult seat(Restaurant restaurant, Customer customer) {
        if (!canAct()) return alreadyActed();
        if (customer == null) return ActionResult.fail("Nobody is waiting in the queue.");
        if (!restaurant.getQueue().contains(customer) || customer.getStatus() != CustomerStatus.WAITING) {
            return ActionResult.fail(customer.getLabel() + " is not waiting in the queue.");
        }
        Optional<Table> table = restaurant.findFreeTable();
        if (table.isEmpty()) {
            return ActionResult.fail("All tables are taken. A table frees up when its customer pays or leaves.");
        }

        restaurant.seat(customer, table.get());
        finishTask();
        return ActionResult.ok(getRole() + " seats " + customer.getLabel() + " at Table " + table.get().getNumber() + ".");
    }

}
