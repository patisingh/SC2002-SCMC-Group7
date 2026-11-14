package restaurant.boundary;

import java.util.List;
import java.util.OptionalDouble;
import restaurant.control.GameController;
import restaurant.entity.Customer;
import restaurant.entity.HappyHour;
import restaurant.entity.Money;
import restaurant.entity.Order;

/** Wording shared by the text and window views, so both describe the game the same way. */
final class DisplayText {

    private DisplayText() {
    }

    static List<String> welcomeLines(int maxTurns) {
        return List.of(
                "You are the manager. Each turn you tell the Waiter",
                "Host and the Chef what to do. The Cashier works by itself.",
                "",
                maxTurns + " turns | " + GameController.TABLES + " tables | 1 Host | 1 Waiter | 1 Chef | 1 Cashier",
                "Regular: loses 8 satisfaction per waiting turn.",
                "VIP: loses 5 per waiting turn and gets 10% off.",
                "Critic: loses 12 per waiting turn, pays full price,",
                "and orders the most expensive menu item.",
                "Host seats before the Waiter; each gets one task.",
                "Salad + Burger Combo: $21.60, 2 prep units.",
                "Happy Hour: activate once for 2 turns; waiting customers",
                "recover 15 at turn start; new orders get 20% off after VIP.",
                "To win, by the end of turn " + maxTurns + " you need:",
                "  - at least " + GameController.TARGET_PAID + " paid customers",
                "  - revenue of at least " + Money.format(GameController.TARGET_REVENUE_CENTS),
                "  - average satisfaction of at least " + (int) GameController.TARGET_AVG_SATISFACTION);
    }

    static String happyHourStatus(HappyHour happyHour) {
        if (happyHour.isActive()) {
            return happyHour.getRemainingTurns() + " turn(s) remaining (including this turn)";
        }
        return happyHour.canActivate() ? "available" : "used / expired";
    }

    static String shortName(Customer c) {
        return "C" + c.getId() + " " + c.getTypeName();
    }

    static String describe(Customer c) {
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

    static String formatAverage(OptionalDouble avg) {
        return avg.isPresent() ? String.format("%.1f", avg.getAsDouble()) : "N/A";
    }
}
