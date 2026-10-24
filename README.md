# Restaurant Rush (Stage 1)

Regular customers, the menu, Waiter, Chef, Cashier, the full turn sequence, victory and defeat.

A turn-based restaurant game for the command line. You are the manager: each turn you choose what the staff
do. Customers arrive by themselves, choose their own food, and pay automatically. Win by the end of turn 18
with at least 4 paid customers, revenue of at least $45, and average satisfaction of at least 60.

Repository: `<add your GitHub URL here>`

## How to run

**Eclipse:** File > New > Java Project, untick "Use default location", browse to this folder, untick
"Create module-info.java" if asked, Finish. Then right-click `src/restaurant/Main.java` > Run As > Java Application,
click inside the Console and type numbers.

**VS Code:** open this folder (with the Extension Pack for Java installed), open `src/restaurant/Main.java`, click Run.

**Terminal:**

    javac -d out $(find src -name "*.java")
    java -cp out restaurant.Main

Windows (Command Prompt):

    dir /s /b src\*.java > sources.txt
    javac -d out @sources.txt
    java -cp out restaurant.Main

No libraries and no network needed. Java 17 or newer.

## Replaying a test run

Each test file is the list of numbers a player types, one per line.

    java -Decho=true -cp out restaurant.Main < tests/stage1/<file>

In Eclipse: Run > Run Configurations > Main > Arguments tab, VM arguments `-Decho=true`;
Common tab, tick Input File and choose the test file.

Tests in `tests/stageN/` are for the code at tag `stage-N` (later stages add new questions, so older
input files no longer line up). See `docs/test-notes.md` for what each test shows and its expected result.

## Stages

Each stage is a tagged, runnable version in the repository:

    git checkout stage-1    # Regular customers only
    git checkout stage-2    # + VIP and Critic
    git checkout stage-3    # + Host, ComboMeal, Happy Hour (final)
    git checkout main       # back to the latest

## Code layout

    src/restaurant/
      Main.java                    starts the game
      boundary/GameCLI.java        screen and keyboard only, no game rules
      control/GameController.java  the six steps of every turn, and the win check
      control/ArrivalSchedule.java who arrives on which turn
      entity/                      Restaurant, Table, Customer, RegularCustomer, Order, Menu, MenuItem,
                                   Dish, Staff, Waiter, Chef, Cashier, ActionResult, Money,
                                   CustomerStatus, OrderStatus

Our own rule choices are listed in `docs/decisions.md`.

## Video timestamps

| # | Segment | Time |
|---|---|---|
| 1 | Launching the game | 00:00 |
| 2 | Manager choices, incl. a constrained decision | |
| 3 | A dish going READY, SERVED, then paid | |
| 4 | A VIP or Critic behaviour | |
| 5 | A Stage 3 feature | |
| 6 | One victory and one defeat | |
| 7 | Stage tag comparisons and the diagrams | |
