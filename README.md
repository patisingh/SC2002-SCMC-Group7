# Restaurant Rush (Stage 2)

Regular, VIP and Critic customers, the menu, Waiter, Chef, Cashier, the full turn sequence, victory and defeat.

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

    java -Decho=true -cp out restaurant.Main < tests/stage2/tc04_stage2_victory.txt

In PowerShell, use `cmd /c` for file redirection:

```powershell
cmd /c "java -Decho=true -cp out restaurant.Main < tests\stage2\tc04_stage2_victory.txt"
```

In Eclipse: Run > Run Configurations > Main > Arguments tab, VM arguments `-Decho=true`;
Common tab, tick Input File and choose the test file.

Stage 1 replay inputs remain unchanged for the preserved Stage 1 version. Use the Stage 2
inputs on this version: the Critic's food choice changes the cooking schedule.
See `docs/test-notes.md` for the replay plan and expected results.

## Stage snapshots

The working code in this package is Stage 2. The existing repository tag is `Stage-1`
(case-sensitive). Stage 2 must be committed, tested and tagged after the group accepts it;
this ZIP does not create commits, tags or pushes.

The assignment specifies lowercase tags `stage-1`, `stage-2` and `stage-3`. Before submission,
confirm which commit is the completed Stage 1 version and add the required lowercase
`stage-1` tag to that commit. Keep the existing `Stage-1` tag intact. Do not tag the new Stage 2
code as Stage 1. Tag completed Stage 2 as `stage-2`; Stage 3 is future work.

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
                                   Order, Menu, MenuItem,
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
