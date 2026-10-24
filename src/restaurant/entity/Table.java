package restaurant.entity;

/** One table. Occupied from seating until its customer pays or leaves. */
public class Table {
    private final int number;
    private Customer customer;

    public Table(int number) {
        this.number = number;
    }

    public boolean isFree() {
        return customer == null;
    }

    public void seat(Customer c) {
        customer = c;
    }

    public void clear() {
        customer = null;
    }

    public int getNumber() { return number; }
    public Customer getCustomer() { return customer; }
}
