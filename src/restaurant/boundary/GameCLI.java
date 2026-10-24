package restaurant.boundary;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import restaurant.control.GameController;
import restaurant.entity.Customer;
import restaurant.entity.Money;
import restaurant.entity.Order;
import restaurant.entity.Restaurant;
import restaurant.entity.Table;

/**
 * Shows the game and reads the manager's choices. Contains no game rules.
 * Every line is kept under about 62 characters so it fits a normal console window.
 */
public class GameCLI {
    private static final int WIDTH = 62;
    private static final String HEAVY = "=".repeat(WIDTH);

    private final Scanner in = new Scanner(System.in);
    private final boolean echo = Boolean.getBoolean("echo");   // -Decho=true prints inputs read from a file

    // ===================== screens =====================

    public void showWelcome(int maxTurns) {
        System.out.println(HEAVY);
        System.out.println(center("RESTAURANT RUSH  -  Stage 1"));
        System.out.println(HEAVY);
        System.out.println("  You are the manager. Each turn you tell the Waiter");
        System.out.println("  and the Chef what to do. The Cashier works by itself.");
        System.out.println();
        System.out.println("  " + maxTurns + " turns | 2 tables | 1 Waiter | 1 Chef | 1 Cashier");
        System.out.println("  To win, by the end of turn " + maxTurns + " you need:");
        System.out.println("    - at least " + GameController.TARGET_PAID + " paid customers");
        System.out.println("    - revenue of at least " + Money.format(GameController.TARGET_REVENUE_CENTS));
        System.out.println("    - average satisfaction of at least "
                + (int) GameController.TARGET_AVG_SATISFACTION);
        System.out.println(HEAVY);
    }

    public void showTurnHeader(int turn, int maxTurns) {
        System.out.println();
        System.out.println();
        System.out.println(HEAVY);
        System.out.println("  TURN " + turn + " of " + maxTurns);
        System.out.println(HEAVY);
    }

    /** The queue, the tables and the running score, shown before the manager chooses. */
    public void showState(Restaurant r) {
        System.out.println("  Revenue " + Money.format(r.getRevenueCents())
                + " | Paid " + r.paidCount()
                + " | Avg satisfaction " + formatAvg(r.averageSatisfaction())
                + " | Left " + r.getDepartures());

        System.out.println();
        System.out.println("  QUEUE (waiting for a table)");
        if (r.getQueue().isEmpty()) {
            System.out.println("    nobody");
        }
        for (Customer c : r.getQueue()) {
            System.out.println("    " + pad(shortName(c), 12) + satBar(c.getSatisfaction()));
        }

        System.out.println();
        String line = "  +-------+------------+----------------+------------------------+";
        System.out.println(line);
        System.out.println("  | TABLE | CUSTOMER   | SATISFACTION   | WHAT'S HAPPENING       |");
        System.out.println(line);
        for (Table t : r.getTables()) {
            if (t.isFree()) {
                System.out.println("  | " + pad(String.valueOf(t.getNumber()), 5) + " | " + pad("-", 10)
                        + " | " + pad("", 14) + " | " + pad("Free", 22) + " |");
            } else {
                Customer c = t.getCustomer();
                System.out.println("  | " + pad(String.valueOf(t.getNumber()), 5) + " | " + pad(shortName(c), 10)
                        + " | " + pad(satBar(c.getSatisfaction()), 14) + " | " + pad(describe(c), 22) + " |");
            }
        }
        System.out.println(line);
    }

    /** A heading that separates the end-of-turn events from the manager's choices. */
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
                + " | Avg " + formatAvg(r.averageSatisfaction())
                + " | Turn " + turn + "/" + maxTurns);
    }

    public void showResult(Restaurant r, boolean victory) {
        Double avg = r.averageSatisfaction();
        System.out.println();
        System.out.println();
        System.out.println(HEAVY);
        System.out.println(center(victory ? "*** VICTORY ***" : "*** DEFEAT ***"));
        System.out.println(HEAVY);
        System.out.println("  " + pad("Target", 25) + pad("You got", 12) + "Result");
        System.out.println("  " + "-".repeat(47));
        System.out.println("  " + pad("Paid customers >= " + GameController.TARGET_PAID, 25)
                + pad(String.valueOf(r.paidCount()), 12)
                + mark(r.paidCount() >= GameController.TARGET_PAID));
        System.out.println("  " + pad("Revenue >= " + Money.format(GameController.TARGET_REVENUE_CENTS), 25)
                + pad(Money.format(r.getRevenueCents()), 12)
                + mark(r.getRevenueCents() >= GameController.TARGET_REVENUE_CENTS));
        System.out.println("  " + pad("Avg satisfaction >= " + (int) GameController.TARGET_AVG_SATISFACTION, 25)
                + pad(formatAvg(avg), 12)
                + mark(avg != null && avg >= GameController.TARGET_AVG_SATISFACTION));
        System.out.println();
        System.out.println("  Customers who left unhappy: " + r.getDepartures());
        System.out.println(HEAVY);
    }

    /** Something that happened. Long text is wrapped so nothing runs off the screen. */
    public void showMessage(String text) {
        printWrapped("  > ", text);
    }

    /** A move that was not allowed, with the reason. */
    public void showError(String text) {
        printWrapped("  ! ", text);
    }

    // ===================== input =====================

    /** Numbered options 1..n plus 0 for the zero option. Returns 0..n. Re-asks on bad input. */
    public int askChoice(String title, List<String> options, String zeroLabel) {
        System.out.println();
        System.out.println("  " + title);
        for (int i = 0; i < options.size(); i++) {
            System.out.println("    " + (i + 1) + ") " + options.get(i));
        }
        System.out.println("    0) " + zeroLabel);
        return readNumber(0, options.size());
    }

    /** Numbered options 1..n. Returns a 0-based index. */
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
                showError("\"" + line + "\" is not a number. Please type a number from "
                        + min + " to " + max + ".");
            }
        }
    }

    // ===================== formatting helpers =====================

    private String shortName(Customer c) {
        return "C" + c.getId() + " " + c.getTypeName();
    }

    /** A plain-words description of what is happening at a table. */
    private String describe(Customer c) {
        Order o = c.getOrder();
        switch (c.getStatus()) {
            case SEATED:       return "Waiting to order";
            case ORDERED:      return o.isReady()
                                   ? o.getItem().getName() + " READY to serve"
                                   : o.getItem().getName() + " cooking " + o.getProgress();
            case EATING:       return "Eating " + o.getItem().getName();
            case READY_TO_PAY: return "Done, ready to pay";
            default:           return c.getStatus().toString();
        }
    }

    /** e.g. "#########- 92": one # per 10 points. */
    private String satBar(int sat) {
        int filled = (int) Math.round(sat / 10.0);
        return "#".repeat(filled) + "-".repeat(10 - filled) + String.format(" %3d", sat);
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
        int left = Math.max(0, (WIDTH - s.length()) / 2);
        return " ".repeat(left) + s;
    }

    private String formatAvg(Double avg) {
        return avg == null ? "N/A" : String.format("%.1f", avg);
    }

    private String mark(boolean ok) {
        return ok ? "MET" : "MISSED";
    }
}
