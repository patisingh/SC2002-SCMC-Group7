# Restaurant Rush

**SC2002 Group 7:** Hari, Sachin, Abhi, Dylan, Yukesh

[How to run](#how-to-run) · [How to play](#how-to-play) · [Project structure](#project-structure) ·
[Tests](#tests) · [Stage snapshots](#stage-snapshots) · [AI History](#ai-history) · [Video](#video)

A turn-based restaurant game built in Java for SC2002. You are the manager and choose what the staff do
each turn. Win by the end of turn 18 with at least 4 paid customers, $45 revenue and average satisfaction
of 60. The game runs in the command line or through an optional Java Swing GUI.

The current version includes all three stages: the core restaurant, VIP and Critic customers, then the
Host, combo meal and Happy Hour. The original versions are kept under the stage tags below.

## How to run

JDK 17 or newer. No external Java libraries or build tools are needed. Run the commands below from the
repository root.

### Command line

On macOS or Linux:

```sh
javac -d out $(find src -name "*.java")
java -cp out restaurant.Main
```

On Windows PowerShell, compile and run with:

```powershell
$sources = (Get-ChildItem -Recurse src -Filter *.java).FullName
javac -d out $sources
java -cp out restaurant.Main
```

In Eclipse, run `src/restaurant/Main.java` as a Java Application.

### Swing GUI

The optional GUI uses the same game logic as the command-line version. After compiling, start it with:

```sh
java -cp out restaurant.MainGUI
```

In Eclipse, run `src/restaurant/MainGUI.java` as a Java Application.

Click the action buttons or press their number keys. The window shows the restaurant, queue, tables,
order progress, Happy Hour status and game log. At the end, use **Play again** to start a new game.

## How to play

There are two tables and six customers, arriving on turns 1, 4, 7, 10, 13 and 16. Each staff member can
do one task per turn, so deciding what to leave until the next turn is part of the game.

All three targets must be met at the end of turn 18: **4 or more paid customers**, **at least $45 revenue**
and **average paid-customer satisfaction of at least 60**.

### Staff and turn order

| Staff | Task |
|---|---|
| Host | Seat one queued customer at a free table, before the Waiter acts |
| Waiter | Seat a customer, take an order, serve a READY dish, or wait |
| Chef | Cook one preparation unit of an order, or wait |
| Cashier | Automatically collect one payment from a customer who is ready to pay |

The Host can seat a customer and the Waiter can take that customer's order in the same turn. The Chef
acts after the Waiter, so a dish finished this turn can only be served from the next turn. Customers
finish eating at the end of the turn after they are served; payment can happen from the following turn.
A table becomes free when its customer pays or leaves.

### Customers and satisfaction

Customers start with 100 satisfaction. While queued, waiting to order or waiting for food, they lose
satisfaction at the end of each turn. At 0 they leave without paying. Satisfaction stops dropping once
food is served. Only customers who have paid count towards revenue and average satisfaction.

| Customer | Satisfaction lost per waiting turn | Food and pricing |
|---|---|---|
| Regular | 8 | Chooses by customer ID; pays the menu price |
| VIP | 5 | Same food selection as Regular; gets 10% off |
| Critic | 12 | Chooses the most expensive menu item; pays full price before any Happy Hour discount |

C2 is the VIP and C4 is the Critic; the other four are Regular. Food choices and arrivals are fixed,
so the same decisions give the same result. Invalid inputs and rejected actions do not use a staff task.

### Menu and Happy Hour

| Menu item | Price | Preparation units |
|---|---|---|
| Salad | $9.00 | 1 |
| Burger | $15.00 | 1 |
| Pasta | $18.00 | 2 |
| Salad + Burger Combo | $21.60 | 2 |

Happy Hour can be activated once, at the start of a turn. It lasts for that turn and the next. Waiting
customers recover 15 satisfaction at turn start, capped at 100, and orders taken while it is active get
20% off after any VIP discount. The bill is fixed when the order is taken. For example, VIP Pasta costs
$18.00 → $16.20 → $12.96 during Happy Hour, even if the customer pays after it ends.

## Project structure

| Location | Contains |
|---|---|
| src/restaurant/Main.java | Command-line entry point |
| src/restaurant/MainGUI.java | Swing GUI entry point |
| src/restaurant/boundary/ | CLI and GUI views, shared display text and the animated restaurant scene |
| src/restaurant/control/ | GameController turn flow and the fixed ArrivalSchedule |
| src/restaurant/entity/ | Customers, staff, tables, orders, menu, money and game rules |
| tests/ | Victory, defeat and invalid-input replays for each stage; Stage 3 rule checks |
| docs/decisions.md | Rule choices and the reasons behind them |
| docs/test-notes.md | Expected replay results and test instructions |

The code follows Boundary–Control–Entity. GameController handles the turn order and talks to a GameView;
GameCLI and GameGUI implement that interface. The entity classes own the game rules. Customer subclasses
override patience, pricing and food selection, while Dish and ComboMeal share the MenuItem interface.
Host and Waiter reuse the seating checks in SeatingStaff.

## Tests

With Python 3 and JDK 17 or newer installed, run:

```sh
python3 tests/stage3/verify.py
```

The script compiles the source with `--release 17`, runs 27 rule checks and replays the three Stage 3
input files. It uses only the Python standard library.

| Replay | Expected result |
|---|---|
| tc07_stage3_victory.txt | 6 served, 5 paid, $86.16 revenue, 87.8 average satisfaction, no departures; VICTORY |
| tc08_stage3_defeat.txt | All staff wait, no Happy Hour; 0 paid, $0.00 revenue, 2 departures; DEFEAT |
| tc09_stage3_invalid_input.txt | Rejects invalid choices without spending tasks, then reaches the same result as TC-07 |

To replay a single test after compiling (macOS, Linux or Windows Command Prompt):

```sh
java -Decho=true -cp out restaurant.Main < tests/stage3/tc07_stage3_victory.txt
```

Stage 1 and Stage 2 replays need their matching stage tags, since the prompts and rules change between
stages. Full results and action plans are in [docs/test-notes.md](docs/test-notes.md); rule choices are in
[docs/decisions.md](docs/decisions.md).

## Stage snapshots

| Tag | Commit | Adds |
|---|---|---|
| [`stage-1`](https://github.com/patisingh/SC2002-SCMC-Group7/tree/stage-1) | ae473d6 | Regular customers, Waiter, Chef, Cashier, two tables, queue, victory and defeat |
| [`stage-2`](https://github.com/patisingh/SC2002-SCMC-Group7/tree/stage-2) | ab7dbff | VIP customers (lose 5 per turn, 10% off) and Critic customers (lose 12, most expensive item, full price) |
| [`stage-3`](https://github.com/patisingh/SC2002-SCMC-Group7/tree/stage-3) | 2f1ca2b | Host, Salad + Burger Combo ($21.60, 2 prep units) and Happy Hour |

### Stage 1 to Stage 2

[Compare stage-1 → stage-2](https://github.com/patisingh/SC2002-SCMC-Group7/compare/stage-1...stage-2)

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

### Stage 2 to Stage 3

[Compare stage-2 → stage-3](https://github.com/patisingh/SC2002-SCMC-Group7/compare/stage-2...stage-3)

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

### After Stage 3: Optional GUI

After Stage 3, we added the Swing GUI and animated restaurant scene. Both views use the same
`GameController` through `GameView`, so the GUI keeps the existing game rules.

## AI History

Generative AI was used to assist in building the optional Java Swing GUI after Stage 3. The team used
a single shared AI chat for the GUI. No AI assistance was used for the Stage 1–3 game code.

GUI AI history: included in the zip submission (Team_GUI_AI_History.pdf)

## Video

| # | Segment | Time |
|---|---|---|
| 1 | Game overview and the three stages | 00:00 |
| 2 | The game launching from the submitted project | 01:40 |
| 3 | Meaningful manager choices, including a decision where staff are constrained | 01:48 |
| 4 | A dish going READY → SERVED → payment | 02:06 |
| 5 | VIP behaviour (02:15) and Critic behaviour (03:29) | 02:15 |
| 6 | Stage 3 features: Happy Hour (02:24) and the combo (03:03) | 02:24 |
| 7 | One victory (04:06) and one defeat (04:54) | 04:06 |
| 8 | Class diagrams (05:07) and stage tag comparisons (07:05) | 05:07 |
| 9 | Optional GUI and the shared GameView interface | 09:49 |
| 10 | Stage 3 rule checks and victory replay | 10:19 |
| 11 | Selected use case and sequence diagram | 10:37 |
| 12 | Individual contributions and key learning | 13:11 |
