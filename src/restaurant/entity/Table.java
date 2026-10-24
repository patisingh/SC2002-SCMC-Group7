package restaurant.entity;

public class Table {
    private final int number;
    private Customer customer;

    Table(int number) {
        this.number = number;
    }

    public boolean isFree() {
        return customer == null;
    }

    void seat(Customer c) {
        customer = c;
    }

    void clear() {
        customer = null;
    }

    public int getNumber() { return number; }
    public Customer getCustomer() { return customer; }
}
