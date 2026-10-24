package restaurant.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Everything in the restaurant right now, plus the running totals used for the win check. */
public class Restaurant {
    private final List<Table> tables = new ArrayList<>();
    private final List<Customer> queue = new ArrayList<>();
    private final Menu menu;
    private final Waiter waiter = new Waiter();
    private final Chef chef = new Chef();
    private final Cashier cashier = new Cashier();

    private int revenueCents = 0;
    private final List<Integer> paidSatisfaction = new ArrayList<>();
    private int servedCount = 0;
    private int departures = 0;

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

    // ---------- queue and tables ----------

    public void addToQueue(Customer c) {
        queue.add(c);
    }

    public Table findFreeTable() {
        for (Table t : tables) {
            if (t.isFree()) return t;
        }
        return null;
    }

    public int countFreeTables() {
        int free = 0;
        for (Table t : tables) {
            if (t.isFree()) free++;
        }
        return free;
    }

    public void seatFromQueue(Customer c, Table t) {
        queue.remove(c);
        t.seat(c);
        c.markSeated();
    }

    public List<Customer> getSeatedCustomers() {
        List<Customer> list = new ArrayList<>();
        for (Table t : tables) {
            if (!t.isFree()) list.add(t.getCustomer());
        }
        return list;
    }

    public List<Customer> getCustomersWaitingToOrder() {
        List<Customer> list = new ArrayList<>();
        for (Customer c : getSeatedCustomers()) {
            if (c.getStatus() == CustomerStatus.SEATED) list.add(c);
        }
        return list;
    }

    public List<Order> getReadyOrders() {
        List<Order> list = new ArrayList<>();
        for (Customer c : getSeatedCustomers()) {
            if (c.getOrder() != null && c.getOrder().isReady()) list.add(c.getOrder());
        }
        return list;
    }

    public List<Order> getCookableOrders() {
        List<Order> list = new ArrayList<>();
        for (Customer c : getSeatedCustomers()) {
            if (c.getOrder() != null && c.getOrder().canBeCooked()) list.add(c.getOrder());
        }
        return list;
    }

    public List<Customer> getReadyToPayCustomers() {
        List<Customer> list = new ArrayList<>();
        for (Customer c : getSeatedCustomers()) {
            if (c.getStatus() == CustomerStatus.READY_TO_PAY) list.add(c);
        }
        list.sort(Comparator.comparingInt(Customer::getReadyToPayTurn).thenComparingInt(Customer::getId));
        return list;
    }

    // ---------- turn steps that act on many customers ----------

    /** Step 4: customers served on an earlier turn finish eating. Returns who finished. */
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

    /** Step 5: everyone not yet served loses satisfaction; anyone at 0 leaves. Returns who left. */
    public List<Customer> applyWaiting() {
        List<Customer> everyone = new ArrayList<>(queue);
        everyone.addAll(getSeatedCustomers());
        List<Customer> left = new ArrayList<>();
        for (Customer c : everyone) {
            if (!c.isWaitingForFood()) continue;
            c.loseWaitingSatisfaction();
            if (c.getSatisfaction() == 0) {
                c.leave();
                queue.remove(c);
                Table t = tableOf(c);
                if (t != null) t.clear();
                departures++;
                left.add(c);
            }
        }
        return left;
    }

    // ---------- money and score ----------

    /** Records a payment and frees the customer's table. Returns the table number. */
    public int recordPayment(Customer c) {
        revenueCents += c.getOrder().getBillCents();
        paidSatisfaction.add(c.getSatisfaction());
        Table t = tableOf(c);
        int number = t.getNumber();
        t.clear();
        return number;
    }

    public void recordServed() {
        servedCount++;
    }

    /** Average satisfaction of paid customers, or null if nobody has paid yet. */
    public Double averageSatisfaction() {
        if (paidSatisfaction.isEmpty()) return null;
        double sum = 0;
        for (int s : paidSatisfaction) sum += s;
        return sum / paidSatisfaction.size();
    }

    private Table tableOf(Customer c) {
        for (Table t : tables) {
            if (t.getCustomer() == c) return t;
        }
        return null;
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
