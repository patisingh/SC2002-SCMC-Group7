package restaurant.entity;

import java.util.Comparator;
import java.util.Optional;

public class Cashier extends Staff {

    private static final Comparator<Customer> OLDEST_FIRST =
            Comparator.comparingInt(Customer::getReadyToPayTurn).thenComparingInt(Customer::getId);

    public Cashier() {
        super("Cashier");
    }

    public ActionResult collectPayment(Restaurant restaurant) {
        if (!canAct()) return alreadyActed();
        Optional<Customer> next = restaurant.getReadyToPayCustomers().stream().min(OLDEST_FIRST);
        if (next.isEmpty()) return ActionResult.fail("Nobody is ready to pay.");

        Customer customer = next.get();
        Table table = restaurant.checkOut(customer);
        finishTask();
        return ActionResult.ok("Cashier: " + customer.getLabel() + " pays "
                + Money.format(customer.getOrder().getBillCents()) + " (satisfaction "
                + customer.getSatisfaction() + " recorded). Table " + table.getNumber() + " is free.");
    }
}
