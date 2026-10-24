package restaurant.entity;

public class Chef extends Staff {

    public Chef() {
        super("Chef");
    }

    public ActionResult prepare(Order order) {
        if (!canAct()) return alreadyActed();
        if (order == null) return ActionResult.fail("There is no order waiting to be cooked.");
        if (!order.canBeCooked()) return ActionResult.fail("That order does not need cooking.");

        order.prepareOneUnit();
        finishTask();
        String text = "Chef cooks " + order.getCustomer().getLabel() + "'s "
                + order.getItem().getName() + " (" + order.getProgress() + ")";
        return ActionResult.ok(order.isReady() ? text + ": READY. It can be served from next turn." : text + ".");
    }
}
