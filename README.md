# Restaurant Rush (Stage 3)

Regular, VIP and Critic customers, the menu, Host, Waiter, Chef, Cashier, the full turn sequence, victory and defeat.

C2 (turn 4) is a VIP: loses 5 satisfaction per waiting turn and receives 10% off.
C4 (turn 10) is a Critic: loses 12 per waiting turn, pays full price and chooses the most expensive item.
All other arrivals remain Regulars (loss of 8). There are still six arrivals and two tables.

A turn-based restaurant game for the command line. You are the manager: each turn you choose what the staff
do. Customers arrive by themselves, choose their own food, and pay automatically. Win by the end of turn 18
with at least 4 paid customers, revenue of at least $45, and average satisfaction of at least 60.

Repository: https://github.com/patisingh/SC2002-SCMC-Group7

## How to run

**Eclipse:** File > New > Java Project, untick "Use default location", browse to this folder, untick
"Create module-info.java" if asked, Finish. Then right-click `src/restaurant/Main.java` > Run As > Java Application,
click inside the Console and type numbers.

**VS Code:** open this folder (with the Extension Pack for Java installed), open `src/restaurant/Main.java`, click Run.

**Terminal:**

    javac -d out $(find src -name "*.java")
    java -cp out restaurant.Main

Windows (PowerShell / VS Code terminal, from the folder containing `src`):

```powershell
$javaFiles = Get-ChildItem .\src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac --release 17 -d out $javaFiles
java -cp out restaurant.Main
```

Windows (Command Prompt):

    dir /s /b src\*.java > sources.txt
    javac -d out @sources.txt
    java -cp out restaurant.Main

No libraries and no network needed. Java 17 or newer.

## Replaying a test run

Each test file is the list of numbers a player types, one per line.

    java -Decho=true -cp out restaurant.Main < tests/stage3/tc07_stage3_victory.txt

In PowerShell, use `cmd /c` for file redirection:

```powershell
cmd /c "java -Decho=true -cp out restaurant.Main < tests\stage3\tc07_stage3_victory.txt"
```

In Eclipse: Run > Run Configurations > Main > Arguments tab, VM arguments `-Decho=true`;
Common tab, tick Input File and choose the test file.

Stage 1 and Stage 2 inputs remain unchanged for their preserved tags. Use Stage 3
inputs on this version: Host and Happy Hour add prompts and combos change cooking.
See `docs/test-notes.md` for the replay plan and expected results.

## Stage snapshots

The working code is Stage 3 (`stage-3`). Stage 2 remains runnable at `stage-2`,
and Stage 1 at `stage-1`. The original `Stage-1` tag is also kept; both names
point to the same Stage 1 snapshot.

## Stage 3 features and choices

- Host acts before Waiter and can seat one queued customer independently. A customer
  seated by Host can order in the same turn; Waiter can still choose to seat instead.
- The menu contains Salad, Burger, Pasta, and Salad + Burger Combo ($21.60, 2 units).
  Regular/VIP selection still uses customer ID modulo menu size: C1 Burger,
  C2 Pasta, C3 Combo, C5 Burger, C6 Pasta. Critic C4 chooses the most expensive
  item, now the Combo. The manager never chooses food.
- Happy Hour is offered at each turn start until activated. It lasts that turn and
  the next. After arrivals and before staff choices, waiting customers recover 15
  satisfaction, capped at 100. Normal waiting penalties still apply at turn end.
- Orders placed during Happy Hour receive 20% off after any VIP discount.
  Bills remain locked through expiry and payment; older orders keep their price.
- Existing timing, six arrivals, two tables, 18 turns, and all victory targets stay
  in force. Each active turn displays Happy Hour's remaining duration.

Stage 3 changes add `Host`, shared `SeatingStaff`, `ComboMeal`, and `HappyHour`.
`Waiter` delegates seating to the shared base and applies the pricing policy at
order creation. `Restaurant` owns Host and Happy Hour and handles recovery;
`Customer` owns satisfaction changes. Controller changes only schedule the new
start-of-turn policy and Host action. CLI changes display the policy and prompts.
`Menu.standard()` remains unchanged; `Menu.stageThree()` registers the combo.

Verify all Stage 3 rules and CLI replays offline (Python 3 plus JDK 17+):

```sh
python3 tests/stage3/verify.py
```

Compare: https://github.com/patisingh/SC2002-SCMC-Group7/compare/stage-2...stage-3

## Stage 1 to Stage 2 changes

| File | Change and reason |
|---|---|
| `src/restaurant/entity/VIPCustomer.java` | New subclass: penalty of 5 and 10% discount through overrides. |
| `src/restaurant/entity/CriticCustomer.java` | New subclass: penalty of 12 and highest-price food preference through overrides. |
| `src/restaurant/control/ArrivalSchedule.java` | Creates VIP C2 and Critic C4; keeps all six arrival turns. |
| `src/restaurant/entity/Money.java` | Adds percentage calculation rounded to the nearest cent. |
| `src/restaurant/boundary/GameCLI.java` | Changes the title and explains the customer types. |
| `README.md` | Adds Stage 2 setup, test and transition information. |
| `docs/decisions.md` | Records customer assignments, Critic preference and discount rounding. |
| `docs/test-notes.md` | Adds reproducible Stage 2 victory, defeat and invalid-input evidence. |
| `tests/stage2/*.txt` | New input replays for Stage 2. |

No files were deleted. `GameController`, `Customer`, `Restaurant`, `Waiter`, `Chef`, `Cashier`,
`Order`, `MenuItem` and the other existing domain classes are unchanged. In particular,
`MenuItem` remains an interface and `ActionResult` remains a record.
The existing Waiter calls `chooseItem()` and `billFor()` through `Customer`, and waiting loss
calls the overridden `waitingPenalty()`. No customer-type checks were added to the turn loop.

After the lowercase tags are created and pushed, the transition comparison will be:
https://github.com/patisingh/SC2002-SCMC-Group7/compare/stage-1...stage-2

## Code layout

    src/restaurant/
      Main.java                    starts the game
      boundary/GameCLI.java        screen and keyboard only, no game rules
      control/GameController.java  the six steps of every turn, and the win check
      control/ArrivalSchedule.java who arrives on which turn
      entity/                      Restaurant, Table, Customer, RegularCustomer, VIPCustomer, CriticCustomer,
                                   Order, Menu, MenuItem, ComboMeal, HappyHour,
                                   Dish, Staff, SeatingStaff, Host, Waiter, Chef, Cashier, ActionResult, Money,
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
