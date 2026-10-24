package restaurant.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The list of things customers can choose from. */
public class Menu {
    private final List<MenuItem> items = new ArrayList<>();

    public void add(MenuItem item) {
        items.add(item);
    }

    public List<MenuItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /** The base-setting menu from the brief. */
    public static Menu standard() {
        Menu menu = new Menu();
        menu.add(new Dish("Salad", 900, 1));
        menu.add(new Dish("Burger", 1500, 1));
        menu.add(new Dish("Pasta", 1800, 2));
        return menu;
    }
}
