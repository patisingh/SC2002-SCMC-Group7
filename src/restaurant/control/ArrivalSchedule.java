package restaurant.control;

import java.util.HashMap;
import java.util.Map;
package restaurant.control;

import java.util.HashMap;
import java.util.Map;

import restaurant.entity.Customer;
import restaurant.entity.RegularCustomer;

public class ArrivalSchedule {
    private static final int[] ARRIVAL_TURNS = {1, 4, 7, 10, 13, 16};

    private final Map<Integer, Customer> arrivals = new HashMap<>();

    public ArrivalSchedule() {
        for (int i = 0; i < ARRIVAL_TURNS.length; i++) {
            arrivals.put(ARRIVAL_TURNS[i], new RegularCustomer(i + 1));
        }
    }

    public Customer customerArriving(int turn) {
        return arrivals.get(turn);
    }
}
import restaurant.entity.Customer;
import restaurant.entity.RegularCustomer;

public class ArrivalSchedule {

    private int[] arrivalTurns = {1, 4, 7, 10, 13, 16};
    private Map<Integer, Customer> arrivals = new HashMap<>();

    public ArrivalSchedule() {

        for (int i = 0; i < arrivalTurns.length; i++) {
            int turn = arrivalTurns[i];
            int customerId = i + 1;

            Customer customer = new RegularCustomer(customerId, turn);
            arrivals.put(turn, customer);
        }
    }

    public Customer customerArriving(int turn) {
        return arrivals.get(turn);
    }
}
