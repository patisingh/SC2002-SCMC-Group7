package restaurant.entity;

/** Offline rule checks; throws on failure without requiring JUnit. */
public class Stage3RulesTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Menu menu = Menu.standard();
        ComboMeal combo = new ComboMeal(menu.getItems().get(0), menu.getItems().get(2));
        check(combo.getPriceCents() == 2430 && combo.getPrepUnits() == 3, "Salad/Pasta combo");
        ComboMeal fractional = new ComboMeal(new Dish("A", 901, 1), new Dish("B", 904, 1));
        check(fractional.getPriceCents() == 1625, "Half-cent rounds up");
        Menu extended = Menu.stageThree();
        check(new RegularCustomer(3).chooseItem(extended) instanceof ComboMeal, "Regular combo rule");
        check(new CriticCustomer(4).chooseItem(extended) instanceof ComboMeal, "Critic highest price rule");

        Restaurant r = new Restaurant(extended, 2);
        Customer vip = new VIPCustomer(2);
        Customer regular = new RegularCustomer(1);
        r.addToQueue(vip);
        r.addToQueue(regular);
        r.startTurn();
        check(!r.getHost().seat(r, new RegularCustomer(9)).success(), "Reject customer outside queue");
        check(r.getHost().canAct(), "Failed seating preserves task");
        check(r.getHost().seat(r, vip).success(), "Host seats VIP");
        check(!r.getHost().seat(r, regular).success(), "Host gets one task");
        check(r.getWaiter().seat(r, regular).success(), "Waiter can also seat independently");
        r.startTurn();
        r.getWaiter().takeOrder(regular, menu, r.getHappyHour());
        int locked = regular.getOrder().getBillCents();
        r.applyWaiting();
        check(vip.getSatisfaction() == 95, "VIP waiting loss");
        check(r.getHappyHour().activate().success(), "First activation");
        r.applyHappyHourRecovery();
        check(vip.getSatisfaction() == 100, "Recovery capped at 100");
        check(regular.getOrder().getBillCents() == locked, "No retroactive discount");
        check(r.getHappyHour().billFor(vip, menu.getItems().get(1)) == 1080, "VIP then HH discount");
        r.startTurn();
        check(r.getWaiter().takeOrder(vip, menu, r.getHappyHour()).success(), "Order on active turn");
        check(vip.getOrder().getBillCents() == 1296, "VIP Pasta locked at 12.96");
        check(r.getChef().prepare(vip.getOrder()).success(), "Cook same turn as order");
        check(!r.getWaiter().serve(r, vip.getOrder(), 1).success(), "Waiter allowance enforced");
        r.getHappyHour().endTurn();
        check(r.getHappyHour().getRemainingTurns() == 1, "Second active turn");
        r.getHappyHour().endTurn();
        check(!r.getHappyHour().isActive(), "Expires after two turns");
        check(!r.getHappyHour().activate().success(), "Cannot reactivate");
        check(vip.getOrder().getBillCents() == 1296, "Discount persists after expiry");
        check(r.getHappyHour().billFor(vip, menu.getItems().get(1)) == 1350, "New bills after expiry");

        // Recovery excludes eating, ready-to-pay, paid and departed customers.
        r.startTurn();
        r.getChef().prepare(vip.getOrder());
        r.applyWaiting();
        r.getWaiter().serve(r, vip.getOrder(), 2);
        int servedSatisfaction = vip.getSatisfaction();
        vip.recoverWaitingSatisfaction(15);
        check(vip.getSatisfaction() == servedSatisfaction, "No recovery after service");
        r.finishEating(3);
        check(r.getCashier().collectPayment(r).success(), "Collect locked bill");
        check(r.getRevenueCents() == 1296, "Revenue uses order bill");
        Customer left = new RegularCustomer(99);
        for (int i = 0; i < 13; i++) left.loseWaitingSatisfaction();
        left.leave();
        left.recoverWaitingSatisfaction(15);
        check(left.getSatisfaction() == 0, "No recovery after departure");
        System.out.println("Stage 3 rule checks passed.");
    }
}
