package restaurant.control;

import java.util.ArrayList;
import java.util.List;

import restaurant.boundary.GameCLI;
import restaurant.entity.ActionResult;
import restaurant.entity.Customer;
import restaurant.entity.Menu;
import restaurant.entity.Order;
import restaurant.entity.OrderStatus;
import restaurant.entity.Restaurant;

/** Runs the game: the six steps of every turn, then the win check. */
public class GameController {
    public static final int MAX_TURNS = 18;
    public static final int TABLES = 2;
    public static final int TARGET_PAID = 4;
    public static final int TARGET_REVENUE_CENTS = 4500;
    public static final double TARGET_AVG_SATISFACTION = 60.0;

    private final Restaurant restaurant;
    private final GameCLI cli;
    private final ArrivalSchedule schedule;
    private int turn;

    public GameController(GameCLI cli) {
        this.cli = cli;
        this.restaurant = new Restaurant(Menu.standard(), TABLES);
        this.schedule = new ArrivalSchedule();
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
        processArrivals();                        // step 1
        cli.showState(restaurant);
        cli.showSection("Your decisions");
        askWaiter();                              // steps 2-3: the Waiter acts before the Chef
        askChef();
        cli.showSection("End of turn " + turn);
        processEatingAndPayment();                // step 4
        applyWaiting();                           // step 5
        cli.showTurnSummary(restaurant, turn, MAX_TURNS);
    }

    private void processArrivals() {
        Customer c = schedule.customerArriving(turn);
        if (c != null) {
            restaurant.addToQueue(c);
            cli.showMessage(c.getLabel() + " arrives and joins the queue.");
        }
    }

    // ---------- manager choices ----------

    private void askWaiter() {
        while (true) {
            // Each option shows how many things it could act on right now, so the player can see what is possible.
            int inQueue = restaurant.getQueue().size();
            int freeTables = restaurant.countFreeTables();
            int toOrder = restaurant.getCustomersWaitingToOrder().size();
            int ready = restaurant.getReadyOrders().size();
            List<String> tasks = List.of(
                    "Seat a customer from the queue   [" + inQueue + " in queue, " + freeTables + " table"
                            + (freeTables == 1 ? "" : "s") + " free]",
                    "Take a seated customer's order   [" + toOrder + " waiting to order]",
                    "Serve a READY order              [" + ready + " READY]");
            int choice = cli.askChoice("WAITER - choose one task:", tasks, "Wait");
            ActionResult result;
            switch (choice) {
                case 0:
                    cli.showMessage("The Waiter waits this turn.");
                    return;
                case 1:
                    result = restaurant.getWaiter().seat(restaurant,
                            pickCustomer("Seat which customer?", restaurant.getQueue()));
                    break;
                case 2:
                    result = restaurant.getWaiter().takeOrder(restaurant,
                            pickCustomer("Take whose order?", restaurant.getCustomersWaitingToOrder()));
                    break;
                default:
                    result = restaurant.getWaiter().serve(restaurant,
                            pickOrder("Serve which order?", restaurant.getReadyOrders()), turn);
                    break;
            }
            if (result.isSuccess()) {
                cli.showMessage(result.getMessage());
                return;
            }
            cli.showError(result.getMessage() + " Choose again; the Waiter's task has not been used.");
        }
    }

    private void askChef() {
        // Only ask the player when there is something to cook; otherwise the Chef waits automatically.
        if (restaurant.getCookableOrders().isEmpty()) {
            cli.showMessage("Chef: no orders to cook, so the Chef waits this turn.");
            return;
        }
        // If the only thing to cook is a dish the Chef has already started (e.g. Pasta 1/2),
        // there is no real decision to make, so he carries on automatically.
        // If another order is also waiting, the player still chooses: that is the kitchen-priority decision.
        List<Order> cookable = restaurant.getCookableOrders();
        if (cookable.size() == 1 && cookable.get(0).getStatus() == OrderStatus.IN_PREPARATION) {
            Order inProgress = cookable.get(0);
            cli.showMessage("Chef is still cooking " + inProgress.getCustomer().getLabel() + "'s "
                    + inProgress.getItem().getName() + ", so he carries on automatically this turn.");
            cli.showMessage(restaurant.getChef().prepare(inProgress).getMessage());
            return;
        }
        while (true) {
            List<String> tasks = List.of("Cook one unit of an order          ["
                    + restaurant.getCookableOrders().size() + " to cook]");
            int choice = cli.askChoice("CHEF - choose one task:", tasks, "Wait");
            if (choice == 0) {
                cli.showMessage("The Chef waits this turn.");
                return;
            }
            ActionResult result = restaurant.getChef().prepare(
                    pickOrder("Cook which order?", restaurant.getCookableOrders()));
            if (result.isSuccess()) {
                cli.showMessage(result.getMessage());
                return;
            }
            cli.showError(result.getMessage() + " Choose again; the Chef's task has not been used.");
        }
    }

    /** Lets the player pick when there is more than one candidate. Returns null if there are none. */
    private Customer pickCustomer(String prompt, List<Customer> candidates) {
        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);
        List<String> labels = new ArrayList<>();
        for (Customer c : candidates) labels.add(c.getLabel() + ", satisfaction " + c.getSatisfaction());
        return candidates.get(cli.askPick(prompt, labels));
    }

    private Order pickOrder(String prompt, List<Order> candidates) {
        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);
        List<String> labels = new ArrayList<>();
        for (Order o : candidates) {
            labels.add(o.getCustomer().getLabel() + "'s " + o.getItem().getName() + " (" + o.getProgress() + ")");
        }
        return candidates.get(cli.askPick(prompt, labels));
    }

    // ---------- automatic steps ----------

    private void processEatingAndPayment() {
        // The Cashier goes first, so someone who finishes eating this turn pays next turn at the earliest.
        ActionResult payment = restaurant.getCashier().collectPayment(restaurant);
        if (payment != null) cli.showMessage(payment.getMessage());
        for (Customer c : restaurant.finishEating(turn)) {
            cli.showMessage(c.getLabel() + " finishes eating and is READY_TO_PAY (pays next turn at the earliest).");
        }
    }

    private void applyWaiting() {
        for (Customer c : restaurant.applyWaiting()) {
            cli.showMessage(c.getLabel() + " reached 0 satisfaction and LEFT without paying.");
        }
    }

    private boolean isVictory() {
        Double avg = restaurant.averageSatisfaction();
        return restaurant.paidCount() >= TARGET_PAID
                && restaurant.getRevenueCents() >= TARGET_REVENUE_CENTS
                && avg != null && avg >= TARGET_AVG_SATISFACTION;
    }
}
