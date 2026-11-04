# Test notes

How to replay: `java -Decho=true -cp out restaurant.Main < tests/stage1/tc01_victory.txt`

## Stage 1 (run on the preserved Stage 1 version; existing tag `Stage-1`)

| ID | File | What it checks | Expected result |
|---|---|---|---|
| TC-01 | tests/stage1/tc01_victory.txt | A full game with good choices | 4 paid, $57.00, average 78.0, VICTORY |
| TC-02 | tests/stage1/tc02_defeat.txt | Waiter waits every turn | C1 leaves on turn 13, C2 on turn 16; 0 paid, $0.00, N/A, DEFEAT |
| TC-03 | tests/stage1/tc03_invalid_input.txt | `abc`, `9`, then serving with nothing READY | Each is explained; the player chooses again; the Waiter's task is not used |

## Stage 2

Verified with Java 17. Compile the game first, then run each input file from the project root.
In PowerShell:

```powershell
cmd /c "java -Decho=true -cp out restaurant.Main < tests\stage2\tc04_stage2_victory.txt"
cmd /c "java -Decho=true -cp out restaurant.Main < tests\stage2\tc05_stage2_defeat.txt"
cmd /c "java -Decho=true -cp out restaurant.Main < tests\stage2\tc06_stage2_invalid_input.txt"
```

| ID | File | Expected and verified result |
|---|---|---|
| TC-04 | `tests/stage2/tc04_stage2_victory.txt` | All 18 turns; 4 paid; $58.20; average satisfaction 74.3 (exactly 74.25); 0 departures; VICTORY. VIP C2 orders Pasta for $16.20; Critic C4 chooses Pasta for $18.00. |
| TC-05 | `tests/stage2/tc05_stage2_defeat.txt` | Wait every turn; all 18 turns; 0 paid; $0.00; average N/A; C1 leaves on turn 13 and Critic C4 on turn 18; VIP C2 remains; DEFEAT. |
| TC-06 | `tests/stage2/tc06_stage2_invalid_input.txt` | Text input, out-of-range choice and serving without a READY order are rejected; then the same victory as TC-04. Invalid inputs do not spend a turn or a staff task. |

### TC-04 action plan

The Chef is explicitly asked for every preparation unit in this implementation.
Cashier actions are automatic.

| Turn | Waiter | Chef | Payment |
|---|---|---|---|
| 1 | Seat C1 | None | - |
| 2 | Take C1 order | Burger 1/1 | - |
| 3 | Serve C1 | None | - |
| 4 | Seat C2 (VIP) | None | - |
| 5 | Take C2 order | Pasta 1/2 | C1: $15.00, satisfaction 84 |
| 6 | Wait | Pasta 2/2 | - |
| 7 | Serve C2 | None | - |
| 8 | Seat C3 | None | - |
| 9 | Take C3 order | Salad 1/1 | C2: $16.20, satisfaction 85 |
| 10 | Serve C3 | None | - |
| 11 | Seat C4 (Critic) | None | - |
| 12 | Take C4 order | Pasta 1/2 | C3: $9.00, satisfaction 76 |
| 13 | Seat C5 | C4 Pasta 2/2 | - |
| 14 | Serve C4 | None | - |
| 15 | Take C5 order | C5 Pasta 1/2 | - |
| 16 | Wait | C5 Pasta 2/2 | C4: $18.00, satisfaction 52 |
| 17 | Serve C5 | None | - |
| 18 | Seat C6 | None | - |

Revenue: 15 + 16.20 + 9 + 18 = $58.20.
Paid-customer average: (84 + 85 + 76 + 52) / 4 = 74.25, displayed as 74.3.
C5 is served but has not paid by turn 18, so is excluded from revenue and the average.

Before the extension, the uploaded Stage 1 source also compiled and its original
victory ($57.00, 4 paid, 78.0 average) and defeat replays matched their expected results.
The Stage 1 input files are preserved byte-for-byte. They are not Stage 2 test cases.
