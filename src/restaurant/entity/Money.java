package restaurant.entity;

public class Money 
{

    public static int percentOf(int cents, int percent) {
        return (int) Math.round(cents * percent / 100.0);
    }

    public static String format(int cents) {
        return String.format("$%d.%02d", cents / 100, cents % 100);
    }
}
