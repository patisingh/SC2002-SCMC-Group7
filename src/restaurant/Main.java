package restaurant;

import restaurant.boundary.GameCLI;
import restaurant.control.GameController;

/** Starting point. Run this file. */
public class Main {
    public static void main(String[] args) {
        new GameController(new GameCLI()).play();
    }
}
