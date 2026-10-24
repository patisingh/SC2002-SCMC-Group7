package restaurant.entity;

/** A single dish. Salad, Burger and Pasta are three Dish objects, not three subclasses. */
public class Dish extends MenuItem {
    private final String name;
    private final int priceCents;
    private final int prepUnits;

    public Dish(String name, int priceCents, int prepUnits) {
        this.name = name;
        this.priceCents = priceCents;
        this.prepUnits = prepUnits;
    }

    @Override public String getName() { return name; }
    @Override public int getPriceCents() { return priceCents; }
    @Override public int getPrepUnits() { return prepUnits; }
}
