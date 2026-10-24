# Decisions our group made (the brief leaves these to us)

| Decision | Our choice | Why | Where in the code |
|---|---|---|---|
| How customers choose food | Customer number n picks menu item (n mod menu size). With Salad, Burger, Pasta: C1 Burger, C2 Pasta, C3 Salad, C4 Burger, C5 Pasta, C6 Salad | Deterministic, so every run can be reproduced; still gives a mix of dishes | `Customer.chooseItem()` |
| Which table a customer gets | The lowest-numbered free table | Simple and predictable | `Restaurant.findFreeTable()` |
| When the Chef is asked | Only when there is something to cook | No pointless questions | `GameController.askChef()` |
| Half-finished dish | If a started dish (e.g. Pasta 1/2) is the only thing to cook, the Chef finishes it automatically. If another order is waiting, the player chooses | No real choice in the first case; in the second the brief gives the player kitchen priority | `GameController.askChef()` |
| Order of the eating and payment step | The Cashier collects before eaters become READY_TO_PAY | Gives "pays next turn at the earliest" with no extra code | `GameController.processEatingAndPayment()` |
| Rounding money | Stored as whole cents; rounded to the nearest cent | Required by the brief for fractional prices | `Money` |
