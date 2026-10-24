package restaurant.control;

import java.util.ArrayList;
import java.util.List;

import restaurant.boundary.GameCLI;
import restaurant.entity.ActionResult;
import restaurant.entity.Customer;
import restaurant.entity.Menu;
import restaurant.entity.Order;
import restaurant.entity.Restaurant;

public class GameController {

    public static final int MAX_TURNS = 18;
    public static final int TABLES = 2;
    public static final int TARGET_PAID = 4;
    public static final int TARGET_REVENUE_CENTS = 4500;
    public static final double TARGET_AVG_SATISFACTION = 60.0;

    private Restaurant restaurant;
    private GameCLI cli;
    private ArrivalSchedule schedule;
    private int turn;

    public GameController(GameCLI cli) {
        this.cli = cli;
        restaurant = new Restaurant(Menu.standard(), TABLES);
        schedule = new ArrivalSchedule();
    }

    public void play() {
        cli.showWelcome(MAX_TURNS);

        for (turn = 1; turn <= MAX_TURNS; turn++) {
            runTurn();
        }

        cli.showResult(restaurant, isVictory());
    }

    private void runTurn() {
        restaurant.startTurn();

        cli.showTurnHeader(turn, MAX_TURNS);
        processArrivals();
        cli.showState(restaurant);

        cli.showSection("Your decisions");

        askWaiter();
        askChef();

        cli.showSection("End of turn " + turn);

        processEatingAndPayment();
        applyWaiting();

        cli.showTurnSummary(restaurant, turn, MAX_TURNS);
    }

    private void processArrivals() {
        Customer customer = schedule.customerArriving(turn);

        if (customer != null) {
            restaurant.addToQueue(customer);
            cli.showMessage(customer.getLabel() + " arrives and joins the queue.");
        }
    }

    private void askWaiter() {

        while (true) {

            List<String> tasks = new ArrayList<>();

            tasks.add("Seat customer [" + restaurant.getQueue().size()
                    + " waiting, " + restaurant.countFreeTables() + " free tables]");

            tasks.add("Take order [" + restaurant.getCustomersWaitingToOrder().size()
                    + " customers]");

            tasks.add("Serve food [" + restaurant.getReadyOrders().size()
                    + " ready orders]");

            int choice = cli.askChoice("WAITER - choose one task:", tasks, "Wait");

            if (choice == 0) {
                cli.showMessage("The Waiter waits this turn.");
                return;
            }

            ActionResult result;

            if (choice == 1) {
                Customer customer = pickCustomer(
                        "Seat which customer?", restaurant.getQueue());

                result = restaurant.getWaiter().seat(restaurant, customer);

            } else if (choice == 2) {
                Customer customer = pickCustomer(
                        "Take whose order?", restaurant.getCustomersWaitingToOrder());

                result = restaurant.getWaiter().takeOrder(restaurant, customer);

            } else {
                Order order = pickOrder(
                        "Serve which order?", restaurant.getReadyOrders());

                result = restaurant.getWaiter().serve(restaurant, order, turn);
            }

            if (result.isSuccess()) {
                cli.showMessage(result.getMessage());
                return;
            } else {
                cli.showError(result.getMessage() + " Choose again.");
            }
        }
    }

    private void askChef() {

        if (restaurant.getCookableOrders().isEmpty()) {
            cli.showMessage("Chef: No orders to cook.");
            return;
        }

        while (true) {

            List<String> tasks = new ArrayList<>();

            tasks.add("Cook one order ["
                    + restaurant.getCookableOrders().size() + " orders]");

            int choice = cli.askChoice("CHEF - choose one task:", tasks, "Wait");

            if (choice == 0) {
                cli.showMessage("The Chef waits this turn.");
                return;
            }

            Order order = pickOrder(
                    "Cook which order?", restaurant.getCookableOrders());

            ActionResult result = restaurant.getChef().prepare(order);

            if (result.isSuccess()) {
                cli.showMessage(result.getMessage());
                return;
            } else {
                cli.showError(result.getMessage() + " Choose again.");
            }
        }
    }

    private Customer pickCustomer(String prompt, List<Customer> customers) {

        if (customers.isEmpty()) {
            return null;
        }

        if (customers.size() == 1) {
            return customers.get(0);
        }

        List<String> choices = new ArrayList<>();

        for (Customer customer : customers) {
            choices.add(customer.getLabel()
                    + ", satisfaction " + customer.getSatisfaction());
        }

        int choice = cli.askPick(prompt, choices);

        return customers.get(choice);
    }

    private Order pickOrder(String prompt, List<Order> orders) {

        if (orders.isEmpty()) {
            return null;
        }

        if (orders.size() == 1) {
            return orders.get(0);
        }

        List<String> choices = new ArrayList<>();

        for (Order order : orders) {
            String details = order.getCustomer().getLabel()
                    + "'s " + order.getItem().getName()
                    + " (" + order.getProgress() + ")";

            choices.add(details);
        }

        int choice = cli.askPick(prompt, choices);

        return orders.get(choice);
    }

    private void processEatingAndPayment() {

        ActionResult payment = restaurant.getCashier().collectPayment(restaurant);

        if (payment != null) {
            cli.showMessage(payment.getMessage());
        }

        for (Customer customer : restaurant.finishEating(turn)) {
            cli.showMessage(customer.getLabel()
                    + " finishes eating and is READY_TO_PAY.");
        }
    }

    private void applyWaiting() {

        for (Customer customer : restaurant.applyWaiting()) {
            cli.showMessage(customer.getLabel()
                    + " reached 0 satisfaction and LEFT without paying.");
        }
    }

    private boolean isVictory() {

        Double average = restaurant.averageSatisfaction();

        if (average == null) {
            return false;
        }

        return restaurant.paidCount() >= TARGET_PAID
                && restaurant.getRevenueCents() >= TARGET_REVENUE_CENTS
                && average >= TARGET_AVG_SATISFACTION;
    }
}
