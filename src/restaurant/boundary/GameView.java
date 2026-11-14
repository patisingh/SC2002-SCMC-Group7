package restaurant.boundary;

import java.util.List;
import restaurant.entity.Restaurant;

/**
 * Everything the game controller needs from a user interface.
 * The controller only talks to this interface, so the text version (GameCLI)
 * and the window version (GameGUI) can be swapped without touching any game rule.
 */
public interface GameView {

    /** Shows the rules and win targets before turn 1. */
    void showWelcome(int maxTurns);

    /** Marks the start of a new turn. */
    void showTurnHeader(int turn, int maxTurns);

    /** Shows the queue, tables, Happy Hour status and running totals. */
    void showState(Restaurant restaurant);

    /** Starts a titled part of the turn, such as "Your decisions". */
    void showSection(String title);

    /** Shows the totals at the end of a turn. */
    void showTurnSummary(Restaurant restaurant, int turn, int maxTurns);

    /** Shows victory or defeat and which targets were met. */
    void showResult(Restaurant restaurant, boolean victory, boolean paidMet, boolean revenueMet, boolean satisfactionMet);

    /** Reports something that happened. */
    void showMessage(String text);

    /** Reports a rejected action or invalid input. */
    void showError(String text);

    /**
     * Asks the manager to choose one task.
     * @return 1..options.size() for an option, or 0 for the zero option (usually "Wait")
     */
    int askChoice(String title, List<String> options, String zeroLabel);

    /**
     * Asks the manager to pick one item from a list.
     * @return the 0-based index of the picked label
     */
    int askPick(String title, List<String> labels);
}
