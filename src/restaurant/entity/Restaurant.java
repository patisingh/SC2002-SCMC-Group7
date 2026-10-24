package restaurant.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Predicate;

public class Restaurant {
    private final Menu menu;
    private final List<Table> tables = new ArrayList<>();
    private final List<Customer> queue = new ArrayList<>();
    private final Waiter waiter = new Waiter();
    private final Chef chef = new Chef();
    private final Cashier cashier = new Cashier();

    private int revenueCents;
    private int servedCount;
    private int departures;
    private final List<Integer> paidSatisfaction = new ArrayList<>();

    public Restaurant(Menu menu, int tableCount) {
        this.menu = menu;
        for (int i = 1; i <= tableCount; i++) {
            tables.add(new Table(i));
        }
    }

    public void startTurn() {
        waiter.startTurn();
        chef.startTurn();
        cashier.startTurn();
    }

    public void addToQueue(Customer c) {
        queue.add(c);
    }

    Optional<Table> findFreeTable() {
        return tables.stream().filter(Table::isFree).findFirst();
    }

    public int countFreeTables() {
        return (int) tables.stream().filter(Table::isFree).count();
    }

    void seat(Customer c, Table t) {
        queue.remove(c);
        t.seat(c);
        c.sitDown();
    }

    private Optional<Table> tableOf(Customer c) {
        return tables.stream().filter(t -> t.getCustomer() == c).findFirst();
    }


    public List<Customer> getSeatedCustomers() {
        return tables.stream().filter(t -> !t.isFree()).map(Table::getCustomer).toList();
    }

    public List<Customer> getCustomersWaitingToOrder() {
        return seatedWhere(c -> c.getStatus() == CustomerStatus.SEATED);
    }

    List<Customer> getReadyToPayCustomers() {
        return seatedWhere(c -> c.getStatus() == CustomerStatus.READY_TO_PAY);
    }

    public List<Order> getReadyOrders() {
        return ordersWhere(Order::isReady);
    }

    public List<Order> getCookableOrders() {
        return ordersWhere(Order::canBeCooked);
    }

    private List<Customer> seatedWhere(Predicate<Customer> test) {
        return getSeatedCustomers().stream().filter(test).toList();
    }

    private List<Order> ordersWhere(Predicate<Order> test) {
        return getSeatedCustomers().stream()
                .map(Customer::getOrder)
                .filter(o -> o != null && test.test(o))
                .toList();
    }

    public List<Customer> finishEating(int turn) {
        List<Customer> finished = new ArrayList<>();
        for (Customer c : getSeatedCustomers()) {
            if (c.getStatus() == CustomerStatus.EATING && c.getOrder().getServedTurn() < turn) {
                c.finishEating(turn);
                finished.add(c);
            }
        }
        return finished;
    }

    public List<Customer> applyWaiting() {
        List<Customer> everyone = new ArrayList<>(queue);
        everyone.addAll(getSeatedCustomers());

        List<Customer> left = new ArrayList<>();
        for (Customer c : everyone) {
            c.loseWaitingSatisfaction();
            if (c.isWaitingForFood() && c.getSatisfaction() == 0) {
                c.leave();
                queue.remove(c);
                tableOf(c).ifPresent(Table::clear);
                departures++;
                left.add(c);
            }
        }
        return left;
    }

    Table checkOut(Customer c) {
        Table table = tableOf(c).orElseThrow();
        c.pay();
        revenueCents += c.getOrder().getBillCents();
        paidSatisfaction.add(c.getSatisfaction());
        table.clear();
        return table;
    }

    void recordServed() {
        servedCount++;
    }

    public OptionalDouble averageSatisfaction() {
        return paidSatisfaction.stream().mapToInt(Integer::intValue).average();
    }

    public int paidCount() { return paidSatisfaction.size(); }

    public int getRevenueCents() { return revenueCents; }

    public int getServedCount() { return servedCount; }

    public int getDepartures() { return departures; }

    public List<Customer> getQueue() { return Collections.unmodifiableList(queue); }

    public List<Table> getTables() { return Collections.unmodifiableList(tables); }

    public Menu getMenu() { return menu; }

    public Waiter getWaiter() { return waiter; }

    public Chef getChef() { return chef; }

    public Cashier getCashier() { return cashier; }
}
