package restaurant.entity;

/** Seats one queued customer per turn, independently of the Waiter. */
public class Host extends SeatingStaff {
    public Host() {
        super("Host");
    }
}
