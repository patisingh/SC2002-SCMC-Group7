package restaurant.boundary;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import restaurant.control.GameController;
import restaurant.entity.Customer;
import restaurant.entity.Money;
import restaurant.entity.Restaurant;
import restaurant.entity.Table;

/** Text version of the game: prints to the console and reads typed numbers. */
public class GameCLI implements GameView {
    private static final int WIDTH = 62;   
    private static final String HEAVY = "=".repeat(WIDTH);
    private static final String TABLE_LINE = "  +-------+------------+----------------+-------------------------------------+";

    private final Scanner in = new Scanner(System.in);
    private final boolean echo = Boolean.getBoolean("echo");  

    @Override
    public void showWelcome(int maxTurns) {
        System.out.println(HEAVY);
        System.out.println(center("RESTAURANT RUSH  -  Stage 3"));
        System.out.println(HEAVY);
        for (String line : DisplayText.welcomeLines(maxTurns)) {
            if (line.isEmpty()) System.out.println();
            else System.out.println("  " + line);
        }
        System.out.println(HEAVY);
    }

    @Override
    public void showTurnHeader(int turn, int maxTurns) {
        System.out.println();
        System.out.println();
        System.out.println(HEAVY);
        System.out.println("  TURN " + turn + " of " + maxTurns);
        System.out.println(HEAVY);
    }

    @Override
    public void showState(Restaurant r) {
        System.out.println("  Happy Hour: " + DisplayText.happyHourStatus(r.getHappyHour()));
        System.out.println("  Revenue " + Money.format(r.getRevenueCents())
                + " | Paid " + r.paidCount()
                + " | Avg satisfaction " + DisplayText.formatAverage(r.averageSatisfaction())
                + " | Left " + r.getDepartures());

        System.out.println();
        System.out.println("  QUEUE (waiting for a table)");
        if (r.getQueue().isEmpty()) {
            System.out.println("    nobody");
        }
        for (Customer c : r.getQueue()) {
            System.out.println("    " + pad(DisplayText.shortName(c), 12) + satisfactionBar(c.getSatisfaction()));
        }

        System.out.println();
        System.out.println(TABLE_LINE);
        System.out.println("  | TABLE | CUSTOMER   | SATISFACTION   | WHAT'S HAPPENING                    |");
        System.out.println(TABLE_LINE);
        for (Table t : r.getTables()) {
            Customer c = t.getCustomer();
            String name = c == null ? "-" : DisplayText.shortName(c);
            String bar = c == null ? "" : satisfactionBar(c.getSatisfaction());
            String activity = c == null ? "Free" : DisplayText.describe(c);
            System.out.println("  | " + pad(String.valueOf(t.getNumber()), 5) + " | " + pad(name, 10)
                    + " | " + pad(bar, 14) + " | " + pad(activity, 35) + " |");
        }
        System.out.println(TABLE_LINE);
    }

    @Override
    public void showSection(String title) {
        System.out.println();
        String text = "-- " + title + " ";
        System.out.println("  " + text + "-".repeat(Math.max(3, WIDTH - 2 - text.length())));
    }

    @Override
    public void showTurnSummary(Restaurant r, int turn, int maxTurns) {
        System.out.println();
        System.out.println("  Served " + r.getServedCount()
                + " | Left " + r.getDepartures()
                + " | Revenue " + Money.format(r.getRevenueCents())
                + " | Avg " + DisplayText.formatAverage(r.averageSatisfaction())
                + " | Turn " + turn + "/" + maxTurns);
    }

    @Override
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
                DisplayText.formatAverage(r.averageSatisfaction()), satisfactionMet);
        System.out.println();
        System.out.println("  Customers who left unhappy: " + r.getDepartures());
        System.out.println(HEAVY);
    }

    @Override
    public void showMessage(String text) {
        printWrapped("  > ", text);
    }

    @Override
    public void showError(String text) {
        printWrapped("  ! ", text);
    }

    @Override
    public int askChoice(String title, List<String> options, String zeroLabel) {
        System.out.println();
        System.out.println("  " + title);
        for (int i = 0; i < options.size(); i++) {
            System.out.println("    " + (i + 1) + ") " + options.get(i));
        }
        System.out.println("    0) " + zeroLabel);
        return readNumber(0, options.size());
    }

    @Override
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

    private String satisfactionBar(int satisfaction) {
        int filled = (int) Math.round(satisfaction / 10.0);
        return "#".repeat(filled) + "-".repeat(10 - filled) + String.format(" %3d", satisfaction);
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
