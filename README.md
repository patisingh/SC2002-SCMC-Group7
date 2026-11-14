# Restaurant Rush

SC2002 Group 7: Hari, Sachin, Abhi, Dylan, Yukesh
Repository: https://github.com/patisingh/SC2002-SCMC-Group7

A turn-based restaurant game for the command line. You are the manager and choose what the staff do each
turn. Win by the end of turn 18 with at least 4 paid customers, $45 revenue and average satisfaction of 60.

## How to run

Java 17 or newer. In Eclipse, run src/restaurant/Main.java as a Java Application. From a terminal:

    javac -d out $(find src -name "*.java")
    java -cp out restaurant.Main

Replay a test run (the same inputs always give the same result):

    java -Decho=true -cp out restaurant.Main < tests/stage3/tc07_stage3_victory.txt

Each stage's test files replay at that stage's tag. Expected results are in docs/test-notes.md, and our
rule choices are in docs/decisions.md.

## Running the GUI

An optional Java Swing GUI is also available.
In Eclipse, run src/restaurant/MainGUI.java as a Java Application.
The GUI uses the same game logic as the command-line version.

## Stage snapshots

| Tag | Commit | Adds |
|---|---|---|
| [`stage-1`](https://github.com/patisingh/SC2002-SCMC-Group7/tree/stage-1) | ae473d6 | Regular customers, Waiter, Chef, Cashier, two tables, queue, victory and defeat |
| [`stage-2`](https://github.com/patisingh/SC2002-SCMC-Group7/tree/stage-2) | ab7dbff | VIP customers (lose 5 per turn, 10% off) and Critic customers (lose 12, most expensive item, full price) |
| [`stage-3`](https://github.com/patisingh/SC2002-SCMC-Group7/tree/stage-3) | 2f1ca2b | Host, Salad + Burger Combo ($21.60, 2 prep units) and Happy Hour |

## Stage 1 to Stage 2

Compare: https://github.com/patisingh/SC2002-SCMC-Group7/compare/stage-1...stage-2

| File | New / changed | Reason |
|---|---|---|
| entity/VIPCustomer.java | New | Overrides waitingPenalty() (5) and billFor() (10% off) |
| entity/CriticCustomer.java | New | Overrides waitingPenalty() (12) and chooseItem() (most expensive item) |
| control/ArrivalSchedule.java | Changed | New createCustomer(id) makes C2 a VIP and C4 a Critic |
| entity/Money.java | Changed | New percentOf() for the VIP discount, rounded to the cent |
| boundary/GameCLI.java | Changed | Welcome text explains the new customer types |
| tests/stage2/, docs/ | New / changed | Stage 2 test runs, decisions and test notes |

No files deleted. Customer, Restaurant and GameController were not changed: the new types work through
polymorphism, so the Regular customer rules are exactly as in Stage 1.

## Stage 2 to Stage 3

Compare: https://github.com/patisingh/SC2002-SCMC-Group7/compare/stage-2...stage-3

| File | New / changed | Reason |
|---|---|---|
| entity/SeatingStaff.java | New | Shared seating rule for the Host and Waiter |
| entity/Host.java | New | Seats one queued customer per turn, before the Waiter |
| entity/HappyHour.java | New | One activation per game, two turns, discounted bill |
| entity/ComboMeal.java | New | Implements MenuItem; two items at 90% of their total price |
| entity/Waiter.java | Changed | Extends SeatingStaff; applies Happy Hour pricing when taking an order |
| entity/Restaurant.java | Changed | Owns the Host and Happy Hour; applies Happy Hour recovery |
| entity/Customer.java | Changed | New recoverWaitingSatisfaction(), capped at 100 |
| entity/Menu.java | Changed | New stageThree() adds the combo |
| control/GameController.java | Changed | Asks about Happy Hour and the Host each turn |
| boundary/GameCLI.java | Changed | Shows Happy Hour status and the new prompts |
| tests/stage3/, docs/ | New / changed | Stage 3 test runs, 27 rule checks (Stage3RulesTest), decisions and test notes |

No files deleted. VIP, Critic and Regular rules are unchanged, which Stage3RulesTest checks.

## After Stage 3: Optional GUI

After completing Stage 3, we added an optional graphical user interface using Java Swing to improve the visual presentation of Restaurant Rush.
The GUI shares the existing game logic through the GameView interface, allowing both the CLI and GUI versions to use the same GameController without changing the original game rules.
To run the GUI, execute src/restaurant/MainGUI.java as a Java Application in Eclipse.

## AI History

Generative AI was used to assist in building the optional Java Swing GUI for Restaurant Rush. This was done after Stage 3 to improve the visual presentation of the game while preserving its existing functionality.

GUI AI history (Abhi): (link to be added)

## Video

| # | Segment | Time |
|---|---|---|
| – | Game overview and the three stages | 00:00 |
| 1 | The game launching from the submitted project | 01:40 |
| 2 | Meaningful manager choices, including a decision where staff are constrained | 01:48 |
| 3 | A dish going READY → SERVED → payment | 02:05 |
| 4 | VIP behaviour (02:15) and Critic behaviour (03:25) | 02:15 |
| 5 | Stage 3 features: Happy Hour (02:20) and the combo (02:57) | 02:20 |
| 6 | One victory (04:06) and one defeat (04:17) | 04:06 |
| 7 | Class diagrams (05:05) and stage tag comparisons (07:05) | 05:05 |
| – | Selected use case and sequence diagram | 08:53 |
| – | Individual contributions and key learning | 11:26 |
