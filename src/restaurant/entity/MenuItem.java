package restaurant.entity;

/** Anything a customer can order. Order, Chef and Cashier only ever talk to MenuItem. */
public abstract class MenuItem {
    public abstract String getName();

    public abstract int getPriceCents();

    public abstract int getPrepUnits();
}
