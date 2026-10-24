package restaurant.control;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import restaurant.boundary.GameCLI;
import restaurant.entity.ActionResult;
import restaurant.entity.Chef;
import restaurant.entity.Customer;
import restaurant.entity.Menu;
import restaurant.entity.Order;
import restaurant.entity.Restaurant;
import restaurant.entity.Staff;
import restaurant.entity.Waiter;

public class GameController {
    public static final int MAX_TURNS = 18;
    public static final int TABLES = 2;
    public static final int TARGET_PAID = 4;
    public static final int TARGET_REVENUE_CENTS = 4500;
    public static final double TARGET_AVG_SATISFACTION = 60.0;

    private final GameCLI cli;
    private final Restaurant restaurant = new Restaurant(Menu.standard(), TABLES);
    private final ArrivalSchedule schedule = new ArrivalSchedule();
    private int turn;

    public GameController(GameCLI cli) {
        this.cli = cli;
    }

    public void play() {
        cli.showWelcome(MAX_TURNS);
        for (turn = 1; turn <= MAX_TURNS; turn++) {
            runTurn();
        }
        boolean paidMet = restaurant.paidCount() >= TARGET_PAID;
        boolean revenueMet = restaurant.getRevenueCents() >= TARGET_REVENUE_CENTS;
        boolean satisfactionMet = restaurant.averageSatisfaction().orElse(0) >= TARGET_AVG_SATISFACTION;
        cli.showResult(restaurant, paidMet && revenueMet && satisfactionMet, paidMet, revenueMet, satisfactionMet);
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
        Customer c = schedule.customerArriving(turn);
        if (c != null) {
            restaurant.addToQueue(c);
            cli.showMessage(c.getLabel() + " arrives and joins the queue.");
        }
    }


    private void askWaiter() {
        Waiter waiter = restaurant.getWaiter();
        List<Customer> queue = restaurant.getQueue();
        List<Customer> toOrder = restaurant.getCustomersWaitingToOrder();
        List<Order> ready = restaurant.getReadyOrders();
        int freeTables = restaurant.countFreeTables();
        List<String> tasks = List.of(
                "Seat a customer from the queue   [" + queue.size() + " in queue, " + freeTables + " table"
                        + (freeTables == 1 ? "" : "s") + " free]",
                "Take a seated customer's order   [" + toOrder.size() + " waiting to order]",
                "Serve a READY order              [" + ready.size() + " READY]");

        while (true) {
            int choice = cli.askChoice("WAITER - choose one task:", tasks, "Wait");
            if (choice == 0) {
                cli.showMessage("The Waiter waits this turn.");
                return;
            }
            ActionResult result = switch (choice) {
                case 1 -> waiter.seat(restaurant, pick("Seat which customer?", queue, this::customerLine));
                case 2 -> waiter.takeOrder(pick("Take whose order?", toOrder, this::customerLine), restaurant.getMenu());
                case 3 -> waiter.serve(restaurant, pick("Serve which order?", ready, this::orderLine), turn);
                default -> throw new IllegalStateException("Unexpected choice " + choice);
            };
            if (report(result, waiter)) {
                return;
            }
        }
    }

    private void askChef() {
        List<Order> cookable = restaurant.getCookableOrders();
        if (cookable.isEmpty()) {
            cli.showMessage("Chef: no orders to cook, so the Chef waits this turn.");
            return;
        }
        Chef chef = restaurant.getChef();
        List<String> tasks = List.of("Cook one unit of an order        [" + cookable.size() + " to cook]");

        if (cli.askChoice("CHEF - choose one task:", tasks, "Wait") == 0) {
            cli.showMessage("The Chef waits this turn.");
        } else {
            report(chef.prepare(pick("Cook which order?", cookable, this::orderLine)), chef);
        }
    }

    private boolean report(ActionResult result, Staff staff) {
        if (result.success()) {
            cli.showMessage(result.message());
            return true;
        }
        cli.showError(result.message() + " Choose again; the " + staff.getRole() + "'s task has not been used.");
        return false;
    }

    private <T> T pick(String prompt, List<T> options, Function<T, String> describe) {
        if (options.isEmpty()) return null;
        if (options.size() == 1) return options.get(0);
        List<String> labels = new ArrayList<>();
        for (T option : options) {
            labels.add(describe.apply(option));
        }
        return options.get(cli.askPick(prompt, labels));
    }

    private String customerLine(Customer c) {
        return c.getLabel() + ", satisfaction " + c.getSatisfaction();
    }

    private String orderLine(Order o) {
        return o.getCustomer().getLabel() + "'s " + o.getItem().getName() + " (" + o.getProgress() + ")";
    }


    private void processEatingAndPayment() {
        ActionResult payment = restaurant.getCashier().collectPayment(restaurant);
        if (payment.success()) {
            cli.showMessage(payment.message());
        }
        for (Customer c : restaurant.finishEating(turn)) {
            cli.showMessage(c.getLabel() + " finishes eating and is READY_TO_PAY (pays next turn at the earliest).");
        }
    }

    private void applyWaiting() {
        for (Customer c : restaurant.applyWaiting()) {
            cli.showMessage(c.getLabel() + " reached 0 satisfaction and LEFT without paying.");
        }
    }
}
