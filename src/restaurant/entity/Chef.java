package restaurant.entity;

/** Advances one order by one preparation unit per turn. */
public class Chef extends Staff {

    public ActionResult prepare(Order order) {
        if (!canAct()) return ActionResult.fail("The Chef has already cooked this turn.");
        if (order == null) return ActionResult.fail("There is no order waiting to be cooked.");
        if (!order.canBeCooked()) return ActionResult.fail("That order does not need cooking.");

        order.prepareOneUnit();
        finishTask();
        String msg = "Chef cooks " + order.getCustomer().getLabel() + "'s " + order.getItem().getName()
                + " (" + order.getProgress() + ")";
        return ActionResult.ok(order.isReady() ? msg + ": READY. It can be served from next turn." : msg + ".");
    }

    @Override
    public String getRoleName() { return "Chef"; }
}
