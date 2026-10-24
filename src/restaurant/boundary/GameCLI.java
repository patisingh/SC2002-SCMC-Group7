package restaurant.boundary;

import java.util.List;
import java.util.Scanner;

import restaurant.control.GameController;
import restaurant.entity.Customer;
import restaurant.entity.Money;
import restaurant.entity.Order;
import restaurant.entity.Restaurant;
import restaurant.entity.Table;
import restaurant.entity.CustomerStatus;

public class GameCLI {

    private Scanner scanner = new Scanner(System.in);
    private boolean echo = Boolean.getBoolean("echo");

    public void showWelcome(int maxTurns) {
        System.out.println("========================================");
        System.out.println("       RESTAURANT RUSH - STAGE 1");
        System.out.println("========================================");
        System.out.println("You are the restaurant manager.");
        System.out.println("You control the Waiter and Chef.");
        System.out.println("The Cashier works automatically.");

        System.out.println("\nGame Rules:");
        System.out.println("Turns: " + maxTurns);
        System.out.println("Tables: 2");
        System.out.println("Staff: 1 Waiter, 1 Chef, 1 Cashier");

        System.out.println("\nWinning Conditions:");
        System.out.println("Paid customers: " + GameController.TARGET_PAID);
        System.out.println("Revenue: " + Money.format(GameController.TARGET_REVENUE_CENTS));
        System.out.println("Average satisfaction: "
                + GameController.TARGET_AVG_SATISFACTION);
    }

    public void showTurnHeader(int turn, int maxTurns) {
        System.out.println("\n========================================");
        System.out.println("TURN " + turn + " / " + maxTurns);
        System.out.println("========================================");
    }

    public void showState(Restaurant restaurant) {

        System.out.println("\nRestaurant Status:");
        System.out.println("Revenue: " + Money.format(restaurant.getRevenueCents()));
        System.out.println("Paid Customers: " + restaurant.paidCount());
        System.out.println("Average Satisfaction: "
                + formatAvg(restaurant.averageSatisfaction()));
        System.out.println("Customers Left: " + restaurant.getDepartures());

        System.out.println("\nWaiting Queue:");

        if (restaurant.getQueue().isEmpty()) {
            System.out.println("No customers waiting.");
        } else {
            for (Customer customer : restaurant.getQueue()) {
                System.out.println(customer.getLabel()
                        + " | Satisfaction: " + customer.getSatisfaction()
                        + " | Status: " + customer.getStatus());
            }
        }

        System.out.println("\nTables:");

        for (Table table : restaurant.getTables()) {

            if (table.isFree()) {
                System.out.println("Table " + table.getNumber() + ": Free");
            } else {
                Customer customer = table.getCustomer();

                System.out.println("Table " + table.getNumber()
                        + ": " + customer.getLabel());

                System.out.println("  Satisfaction: " + customer.getSatisfaction());
                System.out.println("  Status: " + describe(customer));
            }
        }
    }

    public void showSection(String title) {
        System.out.println("\n---------- " + title + " ----------");
    }

    public void showTurnSummary(Restaurant restaurant, int turn, int maxTurns) {

        System.out.println("\nTurn Summary:");
        System.out.println("Served: " + restaurant.getServedCount());
        System.out.println("Customers Left: " + restaurant.getDepartures());
        System.out.println("Revenue: " + Money.format(restaurant.getRevenueCents()));
        System.out.println("Average Satisfaction: "
                + formatAvg(restaurant.averageSatisfaction()));
        System.out.println("Turns Used: " + turn + "/" + maxTurns);
    }

    public void showResult(Restaurant restaurant, boolean victory) {

        System.out.println("\n========================================");

        if (victory) {
            System.out.println("VICTORY!");
        } else {
            System.out.println("DEFEAT!");
        }

        System.out.println("========================================");

        int paid = restaurant.paidCount();
        int revenue = restaurant.getRevenueCents();
        Double average = restaurant.averageSatisfaction();

        System.out.println("Paid Customers: " + paid
                + " - " + mark(paid >= GameController.TARGET_PAID));

        System.out.println("Revenue: " + Money.format(revenue)
                + " - " + mark(revenue >= GameController.TARGET_REVENUE_CENTS));

        boolean satisfactionMet = average != null
                && average >= GameController.TARGET_AVG_SATISFACTION;

        System.out.println("Average Satisfaction: " + formatAvg(average)
                + " - " + mark(satisfactionMet));

        System.out.println("Unhappy Departures: " + restaurant.getDepartures());
    }

    public void showMessage(String message) {
        System.out.println("> " + message);
    }

    public void showError(String message) {
        System.out.println("Error: " + message);
    }

    public int askChoice(String title, List<String> options, String zeroLabel) {

        System.out.println("\n" + title);

        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i));
        }

        System.out.println("0. " + zeroLabel);

        return readNumber(0, options.size());
    }

    public int askPick(String title, List<String> options) {

        System.out.println("\n" + title);

        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i));
        }

        int choice = readNumber(1, options.size());

        return choice - 1;
    }

    private int readNumber(int min, int max) {

        while (true) {

            System.out.print("Enter choice: ");

            if (!scanner.hasNextLine()) {
                System.out.println("Input ended. Game stopped.");
                System.exit(0);
            }

            String input = scanner.nextLine().trim();

            if (echo) {
                System.out.println(input);
            }

            try {
                int number = Integer.parseInt(input);

                if (number >= min && number <= max) {
                    return number;
                } else {
                    System.out.println("Please enter a number between "
                            + min + " and " + max);
                }

            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private String describe(Customer customer) {

        CustomerStatus status = customer.getStatus();
        Order order = customer.getOrder();

        if (status == CustomerStatus.SEATED) {
            return "Waiting to order";
        }

        if (status == CustomerStatus.ORDERED) {

            if (order.isReady()) {
                return order.getItem().getName() + " READY to serve";
            } else {
                return order.getItem().getName()
                        + " cooking " + order.getProgress();
            }
        }

        if (status == CustomerStatus.EATING) {
            return "Eating " + order.getItem().getName();
        }

        if (status == CustomerStatus.READY_TO_PAY) {
            return "Ready to pay";
        }

        return status.toString();
    }

    private String formatAvg(Double average) {

        if (average == null) {
            return "N/A";
        }

        return String.format("%.1f", average);
    }

    private String mark(boolean success) {

        if (success) {
            return "MET";
        } else {
            return "MISSED";
        }
    }
}
