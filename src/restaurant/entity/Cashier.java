
package restaurant.entity;

import java.util.List;

public class Cashier extends Staff {

    public ActionResult collectPayment(Restaurant restaurant) {
        if (!canAct()) {
            return null;
        }

        List<Customer> customers = restaurant.getReadyToPayCustomers();

        if (customers.isEmpty()) {
            return null;
        }

        Customer customer = customers.get(0);
        Order order = customer.getOrder();

        order.markPaid();
        customer.markPaid();

        int table = restaurant.recordPayment(customer);
        finishTask();

        String message = "Cashier: " + customer.getLabel()
                + " pays " + Money.format(order.getBillCents())
                + " (satisfaction " + customer.getSatisfaction()
                + " recorded). Table " + table + " is free.";

        return ActionResult.ok(message);
    }

    @Override
    public String getRoleName() {
        return "Cashier";
    }
}

