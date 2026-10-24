package restaurant.entity;

public class RegularCustomer extends Customer {

    public RegularCustomer(int id) {
        super(id);
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
