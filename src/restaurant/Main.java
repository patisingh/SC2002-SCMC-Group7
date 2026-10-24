package restaurant;

import restaurant.boundary.GameCLI;
import restaurant.control.GameController;

public class Main {
    public static void main(String[] args) {
        GameCLI cli = new GameCLI();
        GameController game = new GameController(cli);
        game.play();
    }
}
