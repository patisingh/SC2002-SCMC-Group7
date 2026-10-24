package restaurant.entity;

/** Money is stored as whole cents (int) and only turned into dollars for display. */
public final class Money {
    private Money() { }

    public static String format(int cents) {
        return String.format("$%d.%02d", cents / 100, cents % 100);
    }
}
