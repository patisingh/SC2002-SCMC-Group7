# Test notes

How to replay: `java -Decho=true -cp out restaurant.Main < tests/stage1/tc01_victory.txt`

## Stage 1 (run on tag `stage-1`)

| ID | File | What it checks | Expected result |
|---|---|---|---|
| TC-01 | tests/stage1/tc01_victory.txt | A full game with good choices | 4 paid, $57.00, average 78.0, VICTORY |
| TC-02 | tests/stage1/tc02_defeat.txt | Waiter waits every turn | C1 leaves on turn 13, C2 on turn 16; 0 paid, $0.00, N/A, DEFEAT |
| TC-03 | tests/stage1/tc03_invalid_input.txt | `abc`, `9`, then serving with nothing READY | Each is explained; the player chooses again; the Waiter's task is not used |
