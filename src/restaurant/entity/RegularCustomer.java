package restaurant.entity;

/** A normal customer: loses 8 satisfaction per waiting turn and pays full price. */
public class RegularCustomer extends Customer {
    public RegularCustomer(int id, int arrivalTurn) {
        super(id, arrivalTurn);
    }

    @Override
    protected int waitingPenalty() {
        return 8;
    }

    @Override
    public String getTypeName() {
        return "Regular";
    }
}
