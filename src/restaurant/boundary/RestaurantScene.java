package restaurant.boundary;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.Timer;
import restaurant.entity.CustomerStatus;
import restaurant.entity.Money;

/**
 * An animated view of the dining room. It never changes the game: it receives a
 * snapshot of the restaurant after each step, compares it with the previous one,
 * and walks the characters to show what happened (seating, taking orders, cooking,
 * serving, paying, leaving).
 *
 * <p>All drawing and animation state lives on the Swing thread.
 */
final class RestaurantScene extends JPanel {

    // ------------------------------------------------------------------ snapshot types

    /** One customer as the scene needs to know them. */
    record Guest(int id, String type, int satisfaction, int queueIndex, int table,
                 CustomerStatus status, String item, boolean ready, int done, int total, int billCents) {
    }

    /** Everything the scene needs from one moment of the game. */
    record Frame(List<Guest> guests, int paid, int revenueCents, boolean happyActive, String staffHint) {
    }

    // ------------------------------------------------------------------ layout (logical pixels)

    private static final double W = 900;
    private static final double H = 460;
    private static final double WALL = 110;
    private static final double DOOR_X = -40;
    private static final double DOOR_Y = 418;
    private static final double QUEUE_HEAD_X = 300;
    private static final double QUEUE_Y = 418;
    private static final double QUEUE_GAP = 58;
    private static final double[][] TABLES = {{400, 268}, {590, 268}};
    private static final double PASS_X = 723;
    private static final double[] PASS_SLOTS = {170, 215, 260, 305};
    private static final double[] HOST_HOME = {372, 404};
    private static final double[] WAITER_HOME = {670, 352};
    private static final double[] CHEF_HOME = {835, 210};
    private static final double[] CASHIER_HOME = {105, 148};
    private static final double[] PAY_SPOT = {105, 206};

    /** A point in front of both tables, so the waiter walks around them instead of through them. */
    private static final double[] LANE = {652, 322};

    private static final double SPEED = 300 * speedFactor();

    // ------------------------------------------------------------------ palette

    private static final Color SKIN_1 = new Color(0xF1C7A3);
    private static final Color SKIN_2 = new Color(0xC98B62);
    private static final Color SKIN_3 = new Color(0x8D5B3E);
    private static final Color OUTLINE = new Color(0x3A2E27);
    private static final Color FLOOR_A = new Color(0xD9B88F);
    private static final Color FLOOR_B = new Color(0xCFAD83);
    private static final Color WALL_C = new Color(0xF3E6CF);
    private static final Color TRIM = new Color(0x8C5A3C);
    private static final Color GOOD = new Color(0x3E8E5E);
    private static final Color WARN = new Color(0xD99A22);
    private static final Color BAD = new Color(0xC2413A);
    private static final Color ACCENT = new Color(0xC8552B);

    // ------------------------------------------------------------------ animation state

    private enum Role { CUSTOMER, HOST, WAITER, CHEF, CASHIER }

    private enum Bubble { NONE, WANTS_ORDER, WAITING_FOOD, PAY, ANGRY, NOTEPAD }

    /** One step of a walk: go to (x, y), or just wait, then optionally run something. */
    private record Step(double x, double y, double waitMs, Runnable onArrive) {
        static Step to(double x, double y) {
            return new Step(x, y, 0, null);
        }

        static Step to(double x, double y, Runnable onArrive) {
            return new Step(x, y, 0, onArrive);
        }

        static Step pause(double ms) {
            return new Step(Double.NaN, Double.NaN, ms, null);
        }

        static Step pause(double ms, Runnable onArrive) {
            return new Step(Double.NaN, Double.NaN, ms, onArrive);
        }
    }

    private static final class Actor {
        final Role role;
        final int id;
        String type = "";
        double x;
        double y;
        final Deque<Step> path = new ArrayDeque<>();
        double waited;
        boolean walking;
        boolean seated;
        boolean facingLeft;
        double phase;
        int satisfaction = 100;
        Bubble bubble = Bubble.NONE;
        String carrying;
        String plate;
        boolean plateEmpty;
        boolean leaving;
        boolean remove;
        double flourish;
        int skin;

        Actor(Role role, int id, double x, double y) {
            this.role = role;
            this.id = id;
            this.x = x;
            this.y = y;
            this.skin = Math.floorMod(id * 7 + role.ordinal() * 3, 3);
        }

        Actor go(Step... steps) {
            for (Step s : steps) {
                path.add(s);
            }
            return this;
        }

        /** Seconds until this actor finishes everything already queued (rough). */
        double busyFor() {
            double t = 0;
            double cx = x;
            double cy = y;
            for (Step s : path) {
                if (Double.isNaN(s.x())) {
                    t += s.waitMs() / 1000.0;
                } else {
                    t += Math.hypot(s.x() - cx, s.y() - cy) / SPEED;
                    cx = s.x();
                    cy = s.y();
                }
            }
            return t;
        }
    }

    private static final class Pop {
        final String text;
        final Color color;
        final double x;
        final double y;
        double age;

        Pop(String text, Color color, double x, double y) {
            this.text = text;
            this.color = color;
            this.x = x;
            this.y = y;
        }
    }

    private final Map<Integer, Actor> guests = new LinkedHashMap<>();
    private final Map<Integer, Guest> lastSeen = new HashMap<>();
    private final Map<Integer, String> passDishes = new LinkedHashMap<>();
    private final List<Pop> pops = new ArrayList<>();
    private final Actor host = new Actor(Role.HOST, 1, HOST_HOME[0], HOST_HOME[1]);
    private final Actor waiter = new Actor(Role.WAITER, 2, WAITER_HOME[0], WAITER_HOME[1]);
    private final Actor chef = new Actor(Role.CHEF, 3, CHEF_HOME[0], CHEF_HOME[1]);
    private final Actor cashier = new Actor(Role.CASHIER, 4, CASHIER_HOME[0], CASHIER_HOME[1]);
    private final List<Guest> cooking = new ArrayList<>();
    private Frame last;
    private boolean happyActive;
    private String banner;
    private double bannerAge = 99;
    private double clock;
    private long lastTick = System.nanoTime();
    private volatile boolean busy;

    RestaurantScene() {
        setOpaque(true);
        setBackground(new Color(0xF4EFE7));
        setPreferredSize(new Dimension(640, 330));
        setName("scene");
        Timer timer = new Timer(16, e -> tick());
        timer.start();
    }

    private static double speedFactor() {
        try {
            return Double.parseDouble(System.getProperty("rush.animSpeed", "1"));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    // ------------------------------------------------------------------ API (Swing thread)

    /** True while someone is still walking somewhere. Safe to read from any thread. */
    boolean isBusy() {
        return busy;
    }

    void reset() {
        guests.clear();
        lastSeen.clear();
        passDishes.clear();
        pops.clear();
        cooking.clear();
        for (Actor a : List.of(host, waiter, chef, cashier)) {
            a.path.clear();
            a.carrying = null;
            a.bubble = Bubble.NONE;
        }
        host.x = HOST_HOME[0];
        host.y = HOST_HOME[1];
        waiter.x = WAITER_HOME[0];
        waiter.y = WAITER_HOME[1];
        chef.x = CHEF_HOME[0];
        chef.y = CHEF_HOME[1];
        last = null;
        happyActive = false;
        busy = false;
        repaint();
    }

    void announce(String text) {
        banner = text;
        bannerAge = 0;
    }

    /** Compares the new moment with the last one and animates the difference. */
    void show(Frame f) {
        happyActive = f.happyActive();
        cooking.clear();
        Map<Integer, Guest> now = new LinkedHashMap<>();
        for (Guest g : f.guests()) {
            now.put(g.id(), g);
            if (g.status() == CustomerStatus.ORDERED && !g.ready()) {
                cooking.add(g);
            }
        }
        Actor seater = "WAITER".equals(f.staffHint()) ? waiter : host;

        // Customers who are no longer in the restaurant.
        int paidNow = last == null ? 0 : f.paid() - last.paid();
        for (Iterator<Map.Entry<Integer, Actor>> it = guests.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Integer, Actor> e = it.next();
            Actor a = e.getValue();
            if (now.containsKey(e.getKey()) || a.leaving) {
                continue;
            }
            Guest before = lastSeen.get(e.getKey());
            boolean paid = before != null && before.status() == CustomerStatus.READY_TO_PAY && paidNow > 0;
            a.leaving = true;
            a.seated = false;
            a.plate = null;
            if (paid) {
                paidNow--;
                int bill = before.billCents();
                a.bubble = Bubble.NONE;
                a.go(Step.to(a.x, a.y + 34),
                        Step.to(PAY_SPOT[0] + 40, PAY_SPOT[1] + 40),
                        Step.to(PAY_SPOT[0], PAY_SPOT[1], () -> pops.add(
                                new Pop("+" + Money.format(bill), GOOD, PAY_SPOT[0] + 70, PAY_SPOT[1] - 70))),
                        Step.pause(350),
                        Step.to(60, DOOR_Y - 30),
                        Step.to(DOOR_X, DOOR_Y, () -> a.remove = true));
            } else {
                a.bubble = Bubble.ANGRY;
                pops.add(new Pop("Hmph!", BAD, a.x, a.y - 92));
                a.go(Step.pause(250), Step.to(a.x, Math.max(a.y + 30, 340)),
                        Step.to(DOOR_X, DOOR_Y, () -> a.remove = true));
            }
        }

        for (Guest g : now.values()) {
            Guest before = lastSeen.get(g.id());
            Actor a = guests.get(g.id());
            if (a == null) {
                a = new Actor(Role.CUSTOMER, g.id(), DOOR_X, DOOR_Y);
                a.type = g.type();
                guests.put(g.id(), a);
                double[] spot = spotFor(g);
                a.go(Step.to(spot[0], spot[1]));
                if (g.table() >= 0) {
                    final Actor seatedActor = a;
                    a.go(Step.pause(1, () -> seatedActor.seated = true));
                }
            } else if (before != null && before.table() < 0 && g.table() >= 0) {
                seatGuest(a, g, seater);
            } else if (before != null && g.table() < 0 && before.queueIndex() != g.queueIndex()) {
                double[] spot = spotFor(g);
                a.go(Step.pause(250), Step.to(spot[0], spot[1]));
            }
            a.satisfaction = g.satisfaction();

            if (before != null) {
                if (before.status() == CustomerStatus.SEATED && g.status() == CustomerStatus.ORDERED) {
                    takeOrder(a, g);
                }
                if (!before.ready() && g.ready()) {
                    chef.flourish = 0.8;
                    passDishes.put(g.id(), g.item());
                    pops.add(new Pop("Order up!", ACCENT, PASS_X, PASS_SLOTS[0] - 60));
                }
                if (before.status() == CustomerStatus.ORDERED && g.status() == CustomerStatus.EATING) {
                    if (!before.ready()) {
                        passDishes.put(g.id(), g.item());
                    }
                    serve(a, g);
                }
                if (before.status() == CustomerStatus.EATING && g.status() == CustomerStatus.READY_TO_PAY) {
                    a.plateEmpty = true;
                }
                if (g.satisfaction() > before.satisfaction() + 5) {
                    pops.add(new Pop("+" + (g.satisfaction() - before.satisfaction()), GOOD, a.x + 18, a.y - 86));
                }
            }
            if (!(before != null && before.status() == CustomerStatus.SEATED && g.status() == CustomerStatus.ORDERED)) {
                a.bubble = bubbleFor(g);
            }
        }

        lastSeen.clear();
        lastSeen.putAll(now);
        last = f;
        busy = anyoneWalking();
        repaint();
    }

    // ------------------------------------------------------------------ choreography

    private void seatGuest(Actor guest, Guest g, Actor seater) {
        double[] seat = seatOf(g.table());
        double[] stand = waiterSpot(g.table());
        double delay = seater.busyFor();
        seater.go(Step.to(guest.x + 34, guest.y - 4),
                Step.to(stand[0], stand[1]),
                Step.pause(150),
                Step.to(seater == host ? HOST_HOME[0] : WAITER_HOME[0], seater == host ? HOST_HOME[1] : WAITER_HOME[1]));
        double reach = delay + Math.hypot(guest.x + 34 - seater.x, guest.y - 4 - seater.y) / SPEED;
        guest.go(Step.pause(reach * 1000 + 150),
                Step.to(seat[0], seat[1] + 40),
                Step.to(seat[0], seat[1], () -> guest.seated = true));
    }

    private void takeOrder(Actor guest, Guest g) {
        double[] stand = waiterSpot(g.table());
        int slot = slotFor(g.id());
        waiter.go(Step.to(stand[0], stand[1], () -> waiter.bubble = Bubble.NOTEPAD),
                Step.pause(500, () -> {
                    waiter.bubble = Bubble.NONE;
                    guest.bubble = Bubble.WAITING_FOOD;
                }),
                Step.to(LANE[0], LANE[1]),
                Step.to(PASS_X - 34, PASS_SLOTS[slot] + 20, () -> pops.add(
                        new Pop("Order in!", ACCENT, PASS_X, PASS_SLOTS[0] - 60))),
                Step.to(WAITER_HOME[0], WAITER_HOME[1]));
    }

    private void serve(Actor guest, Guest g) {
        double[] stand = waiterSpot(g.table());
        int slot = slotFor(g.id());
        waiter.go(Step.to(PASS_X - 34, PASS_SLOTS[slot] + 20, () -> {
                    passDishes.remove(g.id());
                    waiter.carrying = g.item();
                }),
                Step.to(LANE[0], LANE[1]),
                Step.to(stand[0], stand[1], () -> {
                    waiter.carrying = null;
                    guest.plate = g.item();
                    guest.plateEmpty = false;
                }),
                Step.pause(200),
                Step.to(WAITER_HOME[0], WAITER_HOME[1]));
    }

    private int slotFor(int id) {
        int i = 0;
        for (int key : passDishes.keySet()) {
            if (key == id) {
                return Math.min(i, PASS_SLOTS.length - 1);
            }
            i++;
        }
        return Math.min(passDishes.size(), PASS_SLOTS.length - 1);
    }

    private static Bubble bubbleFor(Guest g) {
        return switch (g.status()) {
            case SEATED -> Bubble.WANTS_ORDER;
            case ORDERED -> Bubble.WAITING_FOOD;
            case READY_TO_PAY -> Bubble.PAY;
            default -> Bubble.NONE;
        };
    }

    private static double[] spotFor(Guest g) {
        if (g.table() >= 0) {
            return seatOf(g.table());
        }
        return new double[] {QUEUE_HEAD_X - QUEUE_GAP * g.queueIndex(), QUEUE_Y};
    }

    private static double[] seatOf(int table) {
        double[] t = TABLES[Math.min(table, TABLES.length - 1)];
        return new double[] {t[0], t[1] - 8};
    }

    private static double[] waiterSpot(int table) {
        double[] t = TABLES[Math.min(table, TABLES.length - 1)];
        return new double[] {t[0] + 70, t[1] + 42};
    }

    // ------------------------------------------------------------------ ticking

    private void tick() {
        long nowNs = System.nanoTime();
        double dt = Math.min(0.05, (nowNs - lastTick) / 1e9);
        lastTick = nowNs;
        clock += dt;
        bannerAge += dt;

        List<Actor> all = new ArrayList<>(guests.values());
        all.add(host);
        all.add(waiter);
        all.add(chef);
        all.add(cashier);
        for (Actor a : all) {
            advance(a, dt);
        }
        guests.values().removeIf(a -> a.remove);
        for (Iterator<Pop> it = pops.iterator(); it.hasNext();) {
            Pop p = it.next();
            p.age += dt;
            if (p.age > 1.6) {
                it.remove();
            }
        }
        chef.flourish = Math.max(0, chef.flourish - dt);
        busy = anyoneWalking();
        repaint();
    }

    private void advance(Actor a, double dt) {
        a.walking = false;
        double budget = SPEED * dt;
        while (!a.path.isEmpty()) {
            Step s = a.path.peek();
            if (Double.isNaN(s.x())) {
                a.waited += dt * 1000;
                if (a.waited < s.waitMs()) {
                    return;
                }
                a.waited = 0;
                a.path.poll();
                if (s.onArrive() != null) {
                    s.onArrive().run();
                }
                return;
            }
            double dx = s.x() - a.x;
            double dy = s.y() - a.y;
            double dist = Math.hypot(dx, dy);
            if (dist <= budget) {
                a.x = s.x();
                a.y = s.y();
                budget -= dist;
                a.path.poll();
                if (s.onArrive() != null) {
                    s.onArrive().run();
                }
                if (dist > 0.5) {
                    a.walking = true;
                }
                if (budget <= 0) {
                    return;
                }
            } else {
                a.x += dx / dist * budget;
                a.y += dy / dist * budget;
                if (Math.abs(dx) > 1) {
                    a.facingLeft = dx < 0;
                }
                a.walking = true;
                a.phase += dt * 11;
                return;
            }
        }
    }

    private boolean anyoneWalking() {
        if (!host.path.isEmpty() || !waiter.path.isEmpty()) {
            return true;
        }
        for (Actor a : guests.values()) {
            if (!a.path.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        double scale = Math.min(getWidth() / W, getHeight() / H);
        g.translate((getWidth() - W * scale) / 2, (getHeight() - H * scale) / 2);
        g.scale(scale, scale);
        g.clipRect(0, 0, (int) W, (int) H);

        drawRoom(g);

        // Everything that stands on the floor, drawn back to front.
        List<Object[]> items = new ArrayList<>();
        for (int i = 0; i < TABLES.length; i++) {
            items.add(new Object[] {TABLES[i][1], drawTable(g, i)});
        }
        items.add(new Object[] {170.0, (Runnable) () -> drawCashierCounter(g)});
        List<Actor> people = new ArrayList<>(guests.values());
        people.add(host);
        people.add(waiter);
        people.add(chef);
        people.add(cashier);
        for (Actor a : people) {
            items.add(new Object[] {a.y, (Runnable) () -> drawPerson(g, a)});
        }
        items.sort(Comparator.comparingDouble(o -> (Double) o[0]));
        for (Object[] o : items) {
            ((Runnable) o[1]).run();
        }
        for (Actor a : people) {
            drawOverhead(g, a);
        }
        drawPops(g);
        drawBanner(g);
        g.dispose();
    }

    private void drawRoom(Graphics2D g) {
        // Floor planks.
        for (int row = 0; row * 26 < H - WALL; row++) {
            double y = WALL + row * 26;
            g.setColor(row % 2 == 0 ? FLOOR_A : FLOOR_B);
            g.fill(new java.awt.geom.Rectangle2D.Double(0, y, 700, 26));
            g.setColor(new Color(0, 0, 0, 22));
            for (double x = (row % 2) * 70; x < 700; x += 140) {
                g.fill(new java.awt.geom.Rectangle2D.Double(x, y, 2, 26));
            }
            g.fill(new java.awt.geom.Rectangle2D.Double(0, y + 25, 700, 1));
        }
        // Kitchen tiles.
        for (int r = 0; WALL + r * 30 < H; r++) {
            for (int c = 0; 700 + c * 30 < W; c++) {
                g.setColor((r + c) % 2 == 0 ? new Color(0xEDEDE8) : new Color(0xC9CFD1));
                g.fill(new java.awt.geom.Rectangle2D.Double(700 + c * 30, WALL + r * 30, 30, 30));
            }
        }

        // Back wall.
        g.setPaint(new GradientPaint(0, 0, WALL_C, 0, (float) WALL, new Color(0xEAD8BA)));
        g.fill(new java.awt.geom.Rectangle2D.Double(0, 0, 700, WALL));
        g.setColor(new Color(0xDDE3E4));
        g.fill(new java.awt.geom.Rectangle2D.Double(700, 0, W - 700, WALL));
        g.setColor(new Color(0xC9D1D3));
        for (int x = 700; x < W; x += 22) {
            for (int y = 0; y < WALL; y += 22) {
                g.drawRect(x, y, 22, 22);
            }
        }
        g.setColor(TRIM);
        g.fill(new java.awt.geom.Rectangle2D.Double(0, WALL - 12, 700, 12));
        g.setColor(TRIM.darker());
        g.fill(new java.awt.geom.Rectangle2D.Double(0, WALL - 2, W, 3));

        // Window with sky.
        g.setColor(new Color(0xBFE3F2));
        g.fill(new RoundRectangle2D.Double(200, 18, 110, 62, 6, 6));
        g.setColor(new Color(0xFFFFFF));
        g.fill(new java.awt.geom.Ellipse2D.Double(222 + Math.sin(clock * 0.3) * 8, 34, 34, 14));
        g.setColor(TRIM);
        g.setStroke(new BasicStroke(4));
        g.draw(new RoundRectangle2D.Double(200, 18, 110, 62, 6, 6));
        g.drawLine(255, 18, 255, 80);
        g.drawLine(200, 49, 310, 49);

        // Menu board.
        g.setColor(new Color(0x2F3A33));
        g.fill(new RoundRectangle2D.Double(360, 14, 220, 76, 8, 8));
        g.setColor(TRIM);
        g.setStroke(new BasicStroke(4));
        g.draw(new RoundRectangle2D.Double(360, 14, 220, 76, 8, 8));
        g.setColor(new Color(0xF4EFE7));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.drawString("MENU", 452, 31);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        String[][] menu = {{"Salad", "$9.00"}, {"Burger", "$15.00"}, {"Pasta", "$18.00"}, {"Salad + Burger Combo", "$21.60"}};
        for (int i = 0; i < menu.length; i++) {
            g.drawString(menu[i][0], 372, 47 + i * 12);
            g.drawString(menu[i][1], 568 - g.getFontMetrics().stringWidth(menu[i][1]), 47 + i * 12);
        }

        // Door on the left wall.
        g.setColor(new Color(0x6B4A33));
        g.fill(new java.awt.geom.Rectangle2D.Double(0, 362, 12, 98));
        g.setColor(new Color(0x9B2F2F));
        g.fill(new RoundRectangle2D.Double(6, 392, 46, 60, 8, 8));
        g.setColor(new Color(0xB84646));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
        g.setColor(new Color(0xF4EFE7));
        g.drawString("WELCOME", 7, 425);

        // Queue rope.
        g.setColor(new Color(0x8C7A4B));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D rope = new Path2D.Double();
        rope.moveTo(46, 370);
        for (double x = 46; x <= 326; x += 70) {
            rope.quadTo(x + 35, 382, x + 70, 370);
        }
        g.draw(rope);
        for (double x = 46; x <= 396; x += 70) {
            g.setColor(new Color(0xB8A04A));
            g.fill(new java.awt.geom.Rectangle2D.Double(x - 2, 360, 4, 22));
            g.fill(new java.awt.geom.Ellipse2D.Double(x - 4, 356, 8, 8));
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        g.setColor(new Color(0x7A6650));
        g.drawString("QUEUE", 320, 452);

        // Kitchen: stove, pass counter, ticket rail.
        g.setColor(new Color(0x5C6166));
        g.fill(new RoundRectangle2D.Double(780, 112, 108, 56, 6, 6));
        g.setColor(new Color(0x3B3F43));
        g.fill(new java.awt.geom.Ellipse2D.Double(792, 122, 34, 14));
        g.fill(new java.awt.geom.Ellipse2D.Double(840, 122, 34, 14));
        boolean cookingNow = !cooking.isEmpty();
        if (cookingNow) {
            for (int i = 0; i < 5; i++) {
                double fx = 798 + i * 5;
                double h = 6 + 4 * Math.sin(clock * 18 + i * 1.7);
                g.setColor(i % 2 == 0 ? new Color(0xF28C28) : new Color(0xF6C445));
                g.fill(new java.awt.geom.Ellipse2D.Double(fx, 126 - h, 6, h + 4));
            }
            for (int i = 0; i < 3; i++) {
                double t = (clock * 0.7 + i / 3.0) % 1;
                g.setColor(new Color(255, 255, 255, (int) (160 * (1 - t))));
                double sx = 806 + Math.sin(t * 6 + i) * 6;
                g.fill(new java.awt.geom.Ellipse2D.Double(sx, 104 - t * 60, 12 + t * 10, 12 + t * 10));
            }
        }
        // Pan.
        double toss = chef.flourish > 0 ? Math.sin(chef.flourish * 12) * 5 : 0;
        g.setColor(new Color(0x2A2C2E));
        g.fill(new java.awt.geom.Ellipse2D.Double(796, 112 - toss, 30, 12));
        g.fill(new java.awt.geom.Rectangle2D.Double(824, 115 - toss, 22, 4));

        g.setColor(new Color(0xB9BFC4));
        g.fill(new RoundRectangle2D.Double(PASS_X - 14, 130, 28, 210, 6, 6));
        g.setColor(new Color(0x8E959B));
        g.draw(new RoundRectangle2D.Double(PASS_X - 14, 130, 28, 210, 6, 6));
        int i = 0;
        for (String dish : passDishes.values()) {
            if (i >= PASS_SLOTS.length) {
                break;
            }
            drawPlate(g, PASS_X, PASS_SLOTS[i], dish, false, 0.9);
            i++;
        }
        g.setColor(new Color(0x8E959B));
        g.fill(new java.awt.geom.Rectangle2D.Double(712, 30, 60, 3));
        int t = 0;
        for (Guest c : cooking) {
            double tx = 714 + (t % 2) * 30;
            double ty = 34 + (t / 2) * 34;
            g.setColor(new Color(0xFFFBEA));
            g.fill(new java.awt.geom.Rectangle2D.Double(tx, ty, 26, 30));
            g.setColor(new Color(0x6B6252));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
            g.drawString("C" + c.id(), (float) tx + 4, (float) ty + 11);
            for (int u = 0; u < c.total(); u++) {
                g.setColor(u < c.done() ? ACCENT : new Color(0xD8CFC0));
                g.fill(new java.awt.geom.Ellipse2D.Double(tx + 4 + u * 9, ty + 17, 7, 7));
            }
            t++;
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        g.setColor(new Color(0x6E767C));
        g.drawString("KITCHEN", 815, 452);

        if (happyActive) {
            drawHappyHourDecor(g);
        }
    }

    private void drawHappyHourDecor(Graphics2D g) {
        g.setStroke(new BasicStroke(2));
        g.setColor(new Color(0x6B4A33));
        Path2D wire = new Path2D.Double();
        wire.moveTo(0, 6);
        wire.quadTo(175, 30, 350, 6);
        wire.quadTo(525, 30, 700, 6);
        g.draw(wire);
        Color[] bulbs = {new Color(0xF6C445), new Color(0xE8603C), new Color(0x58B26A), new Color(0x4E8FD6)};
        for (int i = 0; i < 20; i++) {
            double x = 17 + i * 35;
            double seg = (x % 350) / 350.0;
            double y = 6 + 24 * 4 * seg * (1 - seg) * 0.5 + 6;
            boolean on = ((int) (clock * 3) + i) % 3 != 0;
            Color c = bulbs[i % bulbs.length];
            if (on) {
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
                g.fill(new java.awt.geom.Ellipse2D.Double(x - 9, y - 6, 18, 18));
            }
            g.setColor(on ? c : c.darker().darker());
            g.fill(new java.awt.geom.Ellipse2D.Double(x - 4, y - 1, 8, 10));
        }
        g.setColor(ACCENT);
        g.fill(new RoundRectangle2D.Double(30, 30, 140, 46, 10, 10));
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.drawString("HAPPY HOUR", 50, 50);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        g.drawString("20% OFF NEW ORDERS", 39, 67);
    }

    private Runnable drawTable(Graphics2D g, int index) {
        return () -> {
            double x = TABLES[index][0];
            double y = TABLES[index][1];
            g.setColor(new Color(0, 0, 0, 35));
            g.fill(new java.awt.geom.Ellipse2D.Double(x - 58, y + 28, 116, 20));
            g.setColor(new Color(0x7A4B30));
            g.fill(new java.awt.geom.Rectangle2D.Double(x - 4, y + 4, 8, 34));
            g.fill(new java.awt.geom.Rectangle2D.Double(x - 22, y + 34, 44, 6));
            g.setColor(new Color(0xFFFDF8));
            g.fill(new java.awt.geom.Ellipse2D.Double(x - 56, y - 18, 112, 40));
            g.setColor(new Color(0xD9604A));
            for (int k = -2; k <= 2; k++) {
                g.fill(new java.awt.geom.Rectangle2D.Double(x + k * 22 - 5, y + 14, 10, 6));
            }
            g.setColor(new Color(0xD8CDBE));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new java.awt.geom.Ellipse2D.Double(x - 56, y - 18, 112, 40));
            g.setColor(new Color(0x9A8A76));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            g.drawString("TABLE " + (index + 1), (float) x - 22, (float) y + 58);
            for (Actor a : guests.values()) {
                if (a.seated && a.plate != null && Math.abs(a.x - x) < 5) {
                    drawPlate(g, x, y - 2, a.plate, a.plateEmpty, 1);
                }
            }
        };
    }

    private void drawCashierCounter(Graphics2D g) {
        g.setColor(new Color(0x8C5A3C));
        g.fill(new RoundRectangle2D.Double(38, 150, 136, 30, 6, 6));
        g.setColor(new Color(0xA8714E));
        g.fill(new RoundRectangle2D.Double(34, 144, 144, 12, 6, 6));
        g.setColor(new Color(0x4A4F55));
        g.fill(new RoundRectangle2D.Double(128, 124, 34, 22, 4, 4));
        g.setColor(new Color(0x9BE29B));
        g.fill(new java.awt.geom.Rectangle2D.Double(133, 128, 24, 8));
    }

    private void drawPerson(Graphics2D g0, Actor a) {
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(a.x, a.y);
        boolean left = a.facingLeft;
        double bob = a.walking ? Math.abs(Math.sin(a.phase)) * 3 : Math.sin(clock * 2 + a.id) * 0.6;
        if (a.role == Role.CHEF && a.flourish > 0) {
            bob = Math.abs(Math.sin(a.flourish * 14)) * 4;
        }

        // Shadow.
        if (!a.seated) {
            g.setColor(new Color(0, 0, 0, 40));
            g.fill(new java.awt.geom.Ellipse2D.Double(-15, -4, 30, 8));
        }
        g.translate(0, -bob);

        Color shirt = shirtColor(a);
        Color pants = a.role == Role.CUSTOMER ? new Color(0x3F4A5A) : new Color(0x2E2E33);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Legs.
        if (!a.seated) {
            double swing = a.walking ? Math.sin(a.phase) * 6 : 0;
            g.setColor(pants);
            g.fill(new RoundRectangle2D.Double(-8 + swing * 0.5, -16, 7, 16, 4, 4));
            g.fill(new RoundRectangle2D.Double(1 - swing * 0.5, -16, 7, 16, 4, 4));
            g.setColor(new Color(0x2A211C));
            g.fill(new RoundRectangle2D.Double(-9 + swing * 0.5, -3, 9, 4, 3, 3));
            g.fill(new RoundRectangle2D.Double(0 - swing * 0.5, -3, 9, 4, 3, 3));
        } else {
            g.setColor(new Color(0x7A4B30));
            g.fill(new RoundRectangle2D.Double(-16, -44, 32, 30, 8, 8));
        }

        // Body.
        RoundRectangle2D body = new RoundRectangle2D.Double(-12, -40, 24, 27, 10, 10);
        g.setColor(shirt);
        g.fill(body);
        g.setColor(OUTLINE);
        g.draw(body);
        decorateBody(g, a);

        // Arms.
        g.setColor(shirt.darker());
        double armSwing = a.walking ? Math.sin(a.phase) * 4 : 0;
        if (a.carrying != null) {
            g.fill(new RoundRectangle2D.Double(-15, -38, 6, 14, 4, 4));
            g.fill(new RoundRectangle2D.Double(9, -38, 6, 14, 4, 4));
        } else {
            g.fill(new RoundRectangle2D.Double(-16, -37 + armSwing, 6, 16, 4, 4));
            g.fill(new RoundRectangle2D.Double(10, -37 - armSwing, 6, 16, 4, 4));
        }

        // Head.
        Color skin = a.skin == 0 ? SKIN_1 : a.skin == 1 ? SKIN_2 : SKIN_3;
        java.awt.geom.Ellipse2D head = new java.awt.geom.Ellipse2D.Double(-12, -64, 24, 24);
        g.setColor(skin);
        g.fill(head);
        g.setColor(OUTLINE);
        g.draw(head);
        drawHair(g, a);
        drawFace(g, a, left);
        drawHat(g, a);

        if (a.carrying != null) {
            drawPlate(g, 0, -42, a.carrying, false, 0.85);
        }
        g.dispose();
    }

    private void decorateBody(Graphics2D g, Actor a) {
        switch (a.role) {
            case WAITER -> {
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Double(-8, -30, 16, 17, 4, 4));
                g.setColor(new Color(0x1E1E22));
                Path2D bow = new Path2D.Double();
                bow.moveTo(-6, -40);
                bow.lineTo(0, -37);
                bow.lineTo(-6, -34);
                bow.closePath();
                bow.moveTo(6, -40);
                bow.lineTo(0, -37);
                bow.lineTo(6, -34);
                bow.closePath();
                g.fill(bow);
            }
            case CHEF -> {
                g.setColor(new Color(0xD8D8D8));
                g.fill(new java.awt.geom.Ellipse2D.Double(-3, -34, 4, 4));
                g.fill(new java.awt.geom.Ellipse2D.Double(-3, -26, 4, 4));
            }
            case HOST -> {
                g.setColor(new Color(0xF4EFE7));
                g.fill(new java.awt.geom.Rectangle2D.Double(-3, -40, 6, 10));
                g.setColor(new Color(0xC8552B));
                g.fill(new java.awt.geom.Rectangle2D.Double(-1.5, -38, 3, 9));
                g.setColor(new Color(0xF2E1A8));
                g.fill(new RoundRectangle2D.Double(8, -32, 10, 13, 2, 2));
            }
            case CASHIER -> {
                g.setColor(new Color(0xFFFFFF));
                g.fill(new RoundRectangle2D.Double(4, -34, 7, 5, 2, 2));
            }
            case CUSTOMER -> {
                if ("Critic".equals(a.type)) {
                    g.setColor(new Color(0xFFFBEA));
                    g.fill(new java.awt.geom.Rectangle2D.Double(8, -32, 9, 12));
                    g.setColor(new Color(0x7A4FA0));
                    g.drawLine(10, -28, 15, -28);
                    g.drawLine(10, -24, 15, -24);
                }
            }
            default -> { }
        }
    }

    private void drawHair(Graphics2D g, Actor a) {
        if (a.role == Role.CHEF) {
            return;
        }
        Color[] hair = {new Color(0x3B2A1E), new Color(0x1E1A18), new Color(0x7A4B2A), new Color(0xB98A4E)};
        g.setColor(hair[Math.floorMod(a.id * 5 + a.role.ordinal(), hair.length)]);
        g.fill(new Arc2D.Double(-12, -65, 24, 20, 0, 180, Arc2D.CHORD));
    }

    private void drawFace(Graphics2D g, Actor a, boolean left) {
        double shift = left ? -2 : 2;
        g.setColor(OUTLINE);
        g.fill(new java.awt.geom.Ellipse2D.Double(-6 + shift, -54, 3.4, 3.8));
        g.fill(new java.awt.geom.Ellipse2D.Double(3 + shift, -54, 3.4, 3.8));
        int mood = a.role == Role.CUSTOMER ? a.satisfaction : 100;
        if (a.leaving && a.bubble == Bubble.ANGRY) {
            mood = 0;
        }
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (mood >= 60) {
            g.draw(new Arc2D.Double(-4 + shift, -51, 8, 6, 200, 140, Arc2D.OPEN));
        } else if (mood >= 30) {
            g.drawLine((int) (-3 + shift), -46, (int) (3 + shift), -46);
        } else {
            g.draw(new Arc2D.Double(-4 + shift, -47, 8, 6, 20, 140, Arc2D.OPEN));
            g.drawLine((int) (-7 + shift), -58, (int) (-3 + shift), -56);
            g.drawLine((int) (7 + shift), -58, (int) (3 + shift), -56);
        }
        if (a.role == Role.CUSTOMER && mood < 30) {
            g.setColor(new Color(194, 65, 58, 70));
            g.fill(new java.awt.geom.Ellipse2D.Double(-12, -64, 24, 24));
        }
    }

    private void drawHat(Graphics2D g, Actor a) {
        switch (a.role) {
            case CHEF -> {
                g.setColor(Color.WHITE);
                g.fill(new java.awt.geom.Rectangle2D.Double(-9, -72, 18, 10));
                g.fill(new java.awt.geom.Ellipse2D.Double(-12, -84, 12, 14));
                g.fill(new java.awt.geom.Ellipse2D.Double(-5, -88, 12, 16));
                g.fill(new java.awt.geom.Ellipse2D.Double(1, -84, 12, 14));
                g.setColor(new Color(0xBDBDBD));
                g.draw(new java.awt.geom.Rectangle2D.Double(-9, -72, 18, 10));
            }
            case CASHIER -> {
                g.setColor(new Color(0x3F6FB0));
                g.fill(new Arc2D.Double(-12, -67, 24, 14, 0, 180, Arc2D.CHORD));
                g.fill(new RoundRectangle2D.Double(a.facingLeft ? -18 : 2, -62, 16, 4, 3, 3));
            }
            case CUSTOMER -> {
                if ("VIP".equals(a.type)) {
                    g.setColor(new Color(0xF2C230));
                    Path2D crown = new Path2D.Double();
                    crown.moveTo(-8, -64);
                    crown.lineTo(-8, -72);
                    crown.lineTo(-4, -67);
                    crown.lineTo(0, -74);
                    crown.lineTo(4, -67);
                    crown.lineTo(8, -72);
                    crown.lineTo(8, -64);
                    crown.closePath();
                    g.fill(crown);
                    g.setColor(new Color(0xB8860B));
                    g.draw(crown);
                } else if ("Critic".equals(a.type)) {
                    g.setColor(new Color(0x5B3A7A));
                    g.fill(new java.awt.geom.Ellipse2D.Double(-13, -70, 24, 10));
                    g.fill(new java.awt.geom.Ellipse2D.Double(-2, -73, 5, 5));
                }
            }
            default -> { }
        }
    }

    private static Color shirtColor(Actor a) {
        return switch (a.role) {
            case WAITER -> new Color(0x2B2622);
            case CHEF -> new Color(0xF7F7F5);
            case HOST -> new Color(0x2F6B55);
            case CASHIER -> new Color(0xE0A23A);
            case CUSTOMER -> switch (a.type) {
                case "VIP" -> new Color(0xB8860B);
                case "Critic" -> new Color(0x7A4FA0);
                default -> new Color(0x5B87B8);
            };
        };
    }

    private void drawOverhead(Graphics2D g, Actor a) {
        double top = a.y - 64 - (a.role == Role.CHEF ? 26 : 0) - (a.role == Role.CUSTOMER && !"Regular".equals(a.type) ? 10 : 0);
        if (a.role == Role.CUSTOMER) {
            // Name tag with satisfaction bar.
            String name = "C" + a.id;
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
            FontMetrics fm = g.getFontMetrics();
            double w = Math.max(34, fm.stringWidth(name) + 12);
            double tx = a.x - w / 2;
            double ty = top - 22;
            g.setColor(new Color(255, 255, 255, 230));
            g.fill(new RoundRectangle2D.Double(tx, ty, w, 18, 8, 8));
            g.setColor(new Color(0x2B2622));
            g.drawString(name, (float) (a.x - fm.stringWidth(name) / 2.0), (float) (ty + 10));
            g.setColor(new Color(0xE3DBCF));
            g.fill(new RoundRectangle2D.Double(tx + 4, ty + 12, w - 8, 3.5, 3, 3));
            g.setColor(moodColor(a.satisfaction));
            g.fill(new RoundRectangle2D.Double(tx + 4, ty + 12, (w - 8) * a.satisfaction / 100.0, 3.5, 3, 3));
            top = ty;
        } else {
            String role = switch (a.role) {
                case HOST -> "Host";
                case WAITER -> "Waiter";
                case CHEF -> "Chef";
                default -> "Cashier";
            };
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 9));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(new Color(43, 38, 34, 170));
            double w = fm.stringWidth(role) + 10;
            g.fill(new RoundRectangle2D.Double(a.x - w / 2, a.y + 4, w, 13, 7, 7));
            g.setColor(Color.WHITE);
            g.drawString(role, (float) (a.x - fm.stringWidth(role) / 2.0), (float) (a.y + 14));
        }
        if (a.bubble != Bubble.NONE && !(a.leaving && a.bubble != Bubble.ANGRY)) {
            drawBubble(g, a, a.x + 22, top - 4);
        }
    }

    private void drawBubble(Graphics2D g, Actor a, double x, double y) {
        double bob = Math.sin(clock * 3 + a.id) * 1.5;
        y += bob;
        RoundRectangle2D box = new RoundRectangle2D.Double(x - 4, y - 30, 34, 26, 10, 10);
        g.setColor(a.bubble == Bubble.ANGRY ? new Color(0xFBE3E1) : Color.WHITE);
        g.fill(box);
        Path2D tail = new Path2D.Double();
        tail.moveTo(x + 2, y - 6);
        tail.lineTo(x - 6, y + 4);
        tail.lineTo(x + 10, y - 6);
        g.fill(tail);
        g.setColor(new Color(0xBFB3A3));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(box);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        switch (a.bubble) {
            case WANTS_ORDER -> {
                g.setColor(WARN);
                g.drawString("!", (float) x + 10, (float) y - 10);
            }
            case WAITING_FOOD -> {
                Guest gs = lastSeen.get(a.id);
                drawFood(g, x + 13, y - 17, gs == null ? "" : gs.item(), 0.62);
            }
            case PAY -> {
                g.setColor(GOOD);
                g.drawString("$", (float) x + 8, (float) y - 10);
            }
            case ANGRY -> {
                g.setColor(BAD);
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D zig = new Path2D.Double();
                zig.moveTo(x + 2, y - 12);
                zig.lineTo(x + 8, y - 22);
                zig.lineTo(x + 14, y - 12);
                zig.lineTo(x + 20, y - 22);
                zig.lineTo(x + 26, y - 12);
                g.draw(zig);
            }
            case NOTEPAD -> {
                g.setColor(new Color(0xFFFBEA));
                g.fill(new java.awt.geom.Rectangle2D.Double(x + 6, y - 26, 16, 19));
                g.setColor(new Color(0x8A8178));
                for (int i = 0; i < 3; i++) {
                    g.drawLine((int) x + 9, (int) y - 21 + i * 5, (int) x + 19, (int) y - 21 + i * 5);
                }
            }
            default -> { }
        }
    }

    private void drawPlate(Graphics2D g, double x, double y, String item, boolean empty, double scale) {
        g.setColor(new Color(0, 0, 0, 30));
        g.fill(new java.awt.geom.Ellipse2D.Double(x - 15 * scale, y - 3 * scale, 30 * scale, 10 * scale));
        g.setColor(Color.WHITE);
        g.fill(new java.awt.geom.Ellipse2D.Double(x - 15 * scale, y - 6 * scale, 30 * scale, 11 * scale));
        g.setColor(new Color(0xCFC7BA));
        g.draw(new java.awt.geom.Ellipse2D.Double(x - 15 * scale, y - 6 * scale, 30 * scale, 11 * scale));
        if (empty) {
            g.setColor(new Color(0xD9B26A));
            g.fill(new java.awt.geom.Ellipse2D.Double(x - 3 * scale, y - 2 * scale, 3 * scale, 2 * scale));
            g.fill(new java.awt.geom.Ellipse2D.Double(x + 4 * scale, y - 1 * scale, 2 * scale, 2 * scale));
            return;
        }
        drawFood(g, x, y - 6 * scale, item, scale * 0.8);
    }

    /** Small food icons, centred on (x, y). */
    private void drawFood(Graphics2D g, double x, double y, String item, double s) {
        String name = item == null ? "" : item;
        if (name.contains("Combo")) {
            drawFood(g, x - 6 * s, y, "Salad", s * 0.8);
            drawFood(g, x + 7 * s, y, "Burger", s * 0.8);
        } else if (name.contains("Burger")) {
            g.setColor(new Color(0xE0A04A));
            g.fill(new Arc2D.Double(x - 10 * s, y - 10 * s, 20 * s, 14 * s, 0, 180, Arc2D.CHORD));
            g.setColor(new Color(0x5DAA4A));
            g.fill(new java.awt.geom.Rectangle2D.Double(x - 10 * s, y - 3.5 * s, 20 * s, 2.5 * s));
            g.setColor(new Color(0x6B3B22));
            g.fill(new RoundRectangle2D.Double(x - 10 * s, y - 1.5 * s, 20 * s, 4 * s, 3 * s, 3 * s));
            g.setColor(new Color(0xE0A04A));
            g.fill(new RoundRectangle2D.Double(x - 9.5 * s, y + 2.5 * s, 19 * s, 4 * s, 3 * s, 3 * s));
            g.setColor(new Color(0xFFF2C4));
            g.fill(new java.awt.geom.Ellipse2D.Double(x - 4 * s, y - 7 * s, 2 * s, 1.5 * s));
            g.fill(new java.awt.geom.Ellipse2D.Double(x + 2 * s, y - 8 * s, 2 * s, 1.5 * s));
        } else if (name.contains("Salad")) {
            g.setColor(new Color(0x58B26A));
            g.fill(new java.awt.geom.Ellipse2D.Double(x - 9 * s, y - 8 * s, 9 * s, 8 * s));
            g.fill(new java.awt.geom.Ellipse2D.Double(x - 3 * s, y - 10 * s, 10 * s, 9 * s));
            g.setColor(new Color(0x3E8E4A));
            g.fill(new java.awt.geom.Ellipse2D.Double(x + 1 * s, y - 7 * s, 8 * s, 7 * s));
            g.setColor(new Color(0xE8463A));
            g.fill(new java.awt.geom.Ellipse2D.Double(x - 4 * s, y - 6 * s, 4 * s, 4 * s));
            g.setColor(new Color(0xFFFFFF));
            g.fill(new Arc2D.Double(x - 10 * s, y - 5 * s, 20 * s, 12 * s, 180, 180, Arc2D.CHORD));
            g.setColor(new Color(0x9CC9D9));
            g.draw(new Arc2D.Double(x - 10 * s, y - 5 * s, 20 * s, 12 * s, 180, 180, Arc2D.CHORD));
        } else if (name.contains("Pasta")) {
            g.setColor(new Color(0xF2D27A));
            for (int i = 0; i < 4; i++) {
                g.draw(new Arc2D.Double(x - 9 * s + i * 2 * s, y - 7 * s + i * s, 16 * s, 8 * s, 0, 300, Arc2D.OPEN));
            }
            g.setColor(new Color(0xC9412E));
            g.fill(new java.awt.geom.Ellipse2D.Double(x - 4 * s, y - 6 * s, 8 * s, 5 * s));
        } else {
            g.setColor(new Color(0xD9B26A));
            g.fill(new java.awt.geom.Ellipse2D.Double(x - 7 * s, y - 6 * s, 14 * s, 9 * s));
        }
    }

    private void drawPops(Graphics2D g) {
        for (Pop p : pops) {
            double t = p.age / 1.6;
            int alpha = (int) (255 * Math.min(1, (1 - t) * 2));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            FontMetrics fm = g.getFontMetrics();
            float px = (float) (p.x - fm.stringWidth(p.text) / 2.0);
            float py = (float) (p.y - t * 26);
            g.setColor(new Color(255, 255, 255, alpha));
            g.drawString(p.text, px + 1, py + 1);
            g.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), alpha));
            g.drawString(p.text, px, py);
        }
    }

    private void drawBanner(Graphics2D g) {
        if (banner == null || bannerAge > 1.4) {
            return;
        }
        double t = bannerAge / 1.4;
        int alpha = (int) (230 * Math.min(1, Math.min(t * 6, (1 - t) * 3)));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        FontMetrics fm = g.getFontMetrics();
        double w = fm.stringWidth(banner) + 40;
        double x = (W - w) / 2;
        double y = 120 - (1 - Math.min(1, t * 4)) * 10;
        g.setColor(new Color(43, 38, 34, alpha));
        g.fill(new RoundRectangle2D.Double(x, y, w, 46, 14, 14));
        g.setColor(new Color(255, 255, 255, Math.min(255, alpha + 25)));
        g.drawString(banner, (float) (x + 20), (float) (y + 32));
    }

    private static Color moodColor(int v) {
        if (v >= 60) return GOOD;
        if (v >= 30) return WARN;
        return BAD;
    }
}
