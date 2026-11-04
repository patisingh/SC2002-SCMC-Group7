package restaurant.boundary;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Scanner;
import restaurant.control.GameController;
import restaurant.entity.Customer;
import restaurant.entity.Money;
import restaurant.entity.Order;
import restaurant.entity.Restaurant;
import restaurant.entity.Table;

public class GameCLI {
    private static final int WIDTH = 62;   
    private static final String HEAVY = "=".repeat(WIDTH);
    private static final String TABLE_LINE = "  +-------+------------+----------------+------------------------+";

    private final Scanner in = new Scanner(System.in);
    private final boolean echo = Boolean.getBoolean("echo");  

    public void showWelcome(int maxTurns) {
        System.out.println(HEAVY);
        System.out.println(center("RESTAURANT RUSH  -  Stage 2"));
        System.out.println(HEAVY);
        System.out.println("  You are the manager. Each turn you tell the Waiter");
        System.out.println("  and the Chef what to do. The Cashier works by itself.");
        System.out.println();
        System.out.println("  " + maxTurns + " turns | " + GameController.TABLES + " tables | 1 Waiter | 1 Chef | 1 Cashier");
        System.out.println("  Regular: loses 8 satisfaction per waiting turn.");
        System.out.println("  VIP: loses 5 per waiting turn and gets 10% off.");
        System.out.println("  Critic: loses 12 per waiting turn, pays full price,");
        System.out.println("  and orders the most expensive menu item.");
        System.out.println("  To win, by the end of turn " + maxTurns + " you need:");
        System.out.println("    - at least " + GameController.TARGET_PAID + " paid customers");
        System.out.println("    - revenue of at least " + Money.format(GameController.TARGET_REVENUE_CENTS));
        System.out.println("    - average satisfaction of at least " + (int) GameController.TARGET_AVG_SATISFACTION);
        System.out.println(HEAVY);
    }

    public void showTurnHeader(int turn, int maxTurns) {
        System.out.println();
        System.out.println();
        System.out.println(HEAVY);
        System.out.println("  TURN " + turn + " of " + maxTurns);
        System.out.println(HEAVY);
    }

    public void showState(Restaurant r) {
        System.out.println("  Revenue " + Money.format(r.getRevenueCents())
                + " | Paid " + r.paidCount()
                + " | Avg satisfaction " + formatAverage(r.averageSatisfaction())
                + " | Left " + r.getDepartures());

        System.out.println();
        System.out.println("  QUEUE (waiting for a table)");
        if (r.getQueue().isEmpty()) {
            System.out.println("    nobody");
        }
        for (Customer c : r.getQueue()) {
            System.out.println("    " + pad(shortName(c), 12) + satisfactionBar(c.getSatisfaction()));
        }

        System.out.println();
        System.out.println(TABLE_LINE);
        System.out.println("  | TABLE | CUSTOMER   | SATISFACTION   | WHAT'S HAPPENING       |");
        System.out.println(TABLE_LINE);
        for (Table t : r.getTables()) {
            Customer c = t.getCustomer();
            String name = c == null ? "-" : shortName(c);
            String bar = c == null ? "" : satisfactionBar(c.getSatisfaction());
            String activity = c == null ? "Free" : describe(c);
            System.out.println("  | " + pad(String.valueOf(t.getNumber()), 5) + " | " + pad(name, 10)
                    + " | " + pad(bar, 14) + " | " + pad(activity, 22) + " |");
        }
        System.out.println(TABLE_LINE);
    }

    public void showSection(String title) {
        System.out.println();
        String text = "-- " + title + " ";
        System.out.println("  " + text + "-".repeat(Math.max(3, WIDTH - 2 - text.length())));
    }

    public void showTurnSummary(Restaurant r, int turn, int maxTurns) {
        System.out.println();
        System.out.println("  Served " + r.getServedCount()
                + " | Left " + r.getDepartures()
                + " | Revenue " + Money.format(r.getRevenueCents())
                + " | Avg " + formatAverage(r.averageSatisfaction())
                + " | Turn " + turn + "/" + maxTurns);
    }

    public void showResult(Restaurant r, boolean victory, boolean paidMet, boolean revenueMet, boolean satisfactionMet) {
        System.out.println();
        System.out.println();
        System.out.println(HEAVY);
        System.out.println(center(victory ? "*** VICTORY ***" : "*** DEFEAT ***"));
        System.out.println(HEAVY);
        System.out.println("  " + pad("Target", 25) + pad("You got", 12) + "Result");
        System.out.println("  " + "-".repeat(47));
        printTarget("Paid customers >= " + GameController.TARGET_PAID, String.valueOf(r.paidCount()), paidMet);
        printTarget("Revenue >= " + Money.format(GameController.TARGET_REVENUE_CENTS),
                Money.format(r.getRevenueCents()), revenueMet);
        printTarget("Avg satisfaction >= " + (int) GameController.TARGET_AVG_SATISFACTION,
                formatAverage(r.averageSatisfaction()), satisfactionMet);
        System.out.println();
        System.out.println("  Customers who left unhappy: " + r.getDepartures());
        System.out.println(HEAVY);
    }

    public void showMessage(String text) {
        printWrapped("  > ", text);
    }

    public void showError(String text) {
        printWrapped("  ! ", text);
    }

    public int askChoice(String title, List<String> options, String zeroLabel) {
        System.out.println();
        System.out.println("  " + title);
        for (int i = 0; i < options.size(); i++) {
            System.out.println("    " + (i + 1) + ") " + options.get(i));
        }
        System.out.println("    0) " + zeroLabel);
        return readNumber(0, options.size());
    }

    public int askPick(String title, List<String> labels) {
        System.out.println("  " + title);
        for (int i = 0; i < labels.size(); i++) {
            System.out.println("    " + (i + 1) + ") " + labels.get(i));
        }
        return readNumber(1, labels.size()) - 1;
    }

    private int readNumber(int min, int max) {
        while (true) {
            System.out.print("  Your choice: ");
            if (!in.hasNextLine()) {
                System.out.println();
                System.out.println("  [Input ended. Game stopped.]");
                System.exit(0);
            }
            String line = in.nextLine().trim();
            if (echo) System.out.println(line);
            try {
                int n = Integer.parseInt(line);
                if (n >= min && n <= max) return n;
                showError("Please type a number from " + min + " to " + max + ".");
            } catch (NumberFormatException e) {
                showError("\"" + line + "\" is not a number. Please type a number from " + min + " to " + max + ".");
            }
        }
    }


    private void printTarget(String target, String achieved, boolean met) {
        System.out.println("  " + pad(target, 25) + pad(achieved, 12) + (met ? "MET" : "MISSED"));
    }

    private String shortName(Customer c) {
        return "C" + c.getId() + " " + c.getTypeName();
    }

    private String describe(Customer c) {
        Order o = c.getOrder();
        return switch (c.getStatus()) {
            case SEATED -> "Waiting to order";
            case ORDERED -> o.isReady()
                    ? o.getItem().getName() + " READY to serve"
                    : o.getItem().getName() + " cooking " + o.getProgress();
            case EATING -> "Eating " + o.getItem().getName();
            case READY_TO_PAY -> "Done, ready to pay";
            default -> c.getStatus().toString();
        };
    }

    private String satisfactionBar(int satisfaction) {
        int filled = (int) Math.round(satisfaction / 10.0);
        return "#".repeat(filled) + "-".repeat(10 - filled) + String.format(" %3d", satisfaction);
    }

    private String formatAverage(OptionalDouble avg) {
        return avg.isPresent() ? String.format("%.1f", avg.getAsDouble()) : "N/A";
    }

    private void printWrapped(String prefix, String text) {
        int max = WIDTH - prefix.length();
        String indent = " ".repeat(prefix.length());
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() > 0 && current.length() + 1 + word.length() > max) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) current.append(' ');
            current.append(word);
        }
        lines.add(current.toString());
        for (int i = 0; i < lines.size(); i++) {
            System.out.println((i == 0 ? prefix : indent) + lines.get(i));
        }
    }

    private String pad(String s, int width) {
        return s.length() >= width ? s.substring(0, width) : s + " ".repeat(width - s.length());
    }

    private String center(String s) {
        return " ".repeat(Math.max(0, (WIDTH - s.length()) / 2)) + s;
    }
}
