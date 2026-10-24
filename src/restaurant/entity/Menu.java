package restaurant.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Menu {

    private List<MenuItem> items = new ArrayList<>();

    public void add(MenuItem item) {
        items.add(item);
    }

    public List<MenuItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public static Menu standard() {
        Menu menu = new Menu();

        menu.add(new Dish("Salad", 900, 1));
        menu.add(new Dish("Burger", 1500, 1));
        menu.add(new Dish("Pasta", 1800, 2));

        return menu;
    }
}
