package restaurant.control;

import java.util.HashMap;
import java.util.Map;

import restaurant.entity.Customer;
import restaurant.entity.RegularCustomer;

/** Knows which customer walks in on which turn. Base setting: turns 1, 4, 7, 10, 13 and 16. */
public class ArrivalSchedule {
    private static final int[] ARRIVAL_TURNS = {1, 4, 7, 10, 13, 16};
    private final Map<Integer, Customer> arrivals = new HashMap<>();

    public ArrivalSchedule() {
        for (int i = 0; i < ARRIVAL_TURNS.length; i++) {
            int id = i + 1;
            int turn = ARRIVAL_TURNS[i];
            arrivals.put(turn, new RegularCustomer(id, turn));   // Stage 1: everyone is a Regular
        }
    }

    /** The customer arriving at the start of this turn, or null if nobody arrives. */
    public Customer customerArriving(int turn) {
        return arrivals.get(turn);
    }
}
