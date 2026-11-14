package restaurant;

import restaurant.boundary.GameGUI;
import restaurant.control.GameController;

/**
 * Starts the window version of the game. The text version is still started by Main.
 * The same GameController runs both; only the view passed to it is different.
 */
public class MainGUI {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        GameGUI gui = GameGUI.create();
        gui.setRestartAction(() -> startGame(gui));
        gui.show();
        startGame(gui);
    }

    /** Runs one game on its own thread so the window stays responsive while it waits for clicks. */
    private static void startGame(GameGUI gui) {
        Thread game = new Thread(() -> new GameController(gui).play(), "game-loop");
        game.setDaemon(true);
        game.start();
    }
}
