package restaurant.entity;

import java.util.List;

/** Automatic: takes at most one payment per turn, from the customer who has been ready longest. */
public class Cashier extends Staff {

    /** Returns null when there is nobody to charge this turn. */
    public ActionResult collectPayment(Restaurant restaurant) {
        if (!canAct()) return null;
        List<Customer> ready = restaurant.getReadyToPayCustomers();
        if (ready.isEmpty()) return null;

        Customer customer = ready.get(0);   // oldest first
        Order order = customer.getOrder();
        order.markPaid();
        customer.markPaid();
        int tableNumber = restaurant.recordPayment(customer);
        finishTask();
        return ActionResult.ok("Cashier: " + customer.getLabel() + " pays " + Money.format(order.getBillCents())
                + " (satisfaction " + customer.getSatisfaction() + " recorded). Table " + tableNumber + " is free.");
    }

    @Override
    public String getRoleName() { return "Cashier"; }
}
