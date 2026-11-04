package restaurant.entity;

import java.util.Objects;

public class ComboMeal implements MenuItem {
    private final MenuItem first;
    private final MenuItem second;

    public ComboMeal(MenuItem first, MenuItem second) {
        this.first = Objects.requireNonNull(first);
        this.second = Objects.requireNonNull(second);
    }

    @Override
    public String getName() {
        return first.getName() + " + " + second.getName() + " Combo";
    }

    @Override
    public int getPriceCents() {
        return Money.percentOf(first.getPriceCents() + second.getPriceCents(), 90);
    }

    @Override
    public int getPrepUnits() {
        return first.getPrepUnits() + second.getPrepUnits();
    }
}
