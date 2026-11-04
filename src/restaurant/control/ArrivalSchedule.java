package restaurant.control;

import java.util.HashMap;
import java.util.Map;
import restaurant.entity.CriticCustomer;
import restaurant.entity.Customer;
import restaurant.entity.RegularCustomer;
import restaurant.entity.VIPCustomer;

public class ArrivalSchedule {
    private static final int[] ARRIVAL_TURNS = {1, 4, 7, 10, 13, 16};

    private final Map<Integer, Customer> arrivals = new HashMap<>();

    public ArrivalSchedule() {
        for (int i = 0; i < ARRIVAL_TURNS.length; i++) {
            arrivals.put(ARRIVAL_TURNS[i], createCustomer(i + 1));
        }
    }

    public Customer customerArriving(int turn) {
        return arrivals.get(turn);
    }

    private Customer createCustomer(int id) {
        return switch (id) {
            case 2 -> new VIPCustomer(id);
            case 4 -> new CriticCustomer(id);
            default -> new RegularCustomer(id);
        };
    }
}
