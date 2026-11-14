# Decisions our group made (the brief leaves these to us)

| Decision | Our choice | Why | Where in the code |
|---|---|---|---|
| How Regular and VIP customers choose food | Customer number n picks menu item (n mod menu size). Stages 1–2 (Salad, Burger, Pasta): C1 Burger, C2 Pasta, C3 Salad, C4 Burger, C5 Pasta, C6 Salad. Stage 3 adds the combo (Salad, Burger, Pasta, Combo): C1 Burger, C2 Pasta, C3 Combo, C5 Burger, C6 Pasta; C4 is the Critic and picks the most expensive item, the Combo | Deterministic, so every run can be reproduced; still gives a mix of dishes | `Customer.chooseItem()`, `CriticCustomer.chooseItem()` |
| Which table a customer gets | The lowest-numbered free table | Simple and predictable | `Restaurant.findFreeTable()` |
| When the Chef is asked | Only when there is something to cook | No pointless questions | `GameController.askChef()` |
| Order of the eating and payment step | The Cashier collects before eaters become READY_TO_PAY | Gives "pays next turn at the earliest" with no extra code | `GameController.processEatingAndPayment()` |
| Rounding money | Stored as whole cents; rounded to the nearest cent | Required by the brief for fractional prices | `Money` |

## Stage 2 decisions

| Decision | Choice | Reason | Code |
|---|---|---|---|
| Customer mix | C2 on turn 4 is VIP; C4 on turn 10 is Critic; the other four are Regular | Includes both new types while retaining exactly six arrivals | `ArrivalSchedule.createCustomer()` |
| Critic preference | Select the most expensive menu item; first item wins a price tie | A distinct, deterministic preference that also works if the menu grows | `CriticCustomer.chooseItem()` |
| VIP food selection | Inherit the original customer-number rule | Only patience and pricing differ for VIPs | `Customer.chooseItem()` |
| Customer-specific behaviour | Override `waitingPenalty()`, `billFor()` and `chooseItem()` as needed | Keeps the common service flow independent of customer type | Customer subclasses |
| VIP discount | Calculate 90% of the price, rounding once to the nearest cent; half cents round up | Matches the 10% discount and integer-cents requirement | `VIPCustomer.billFor()`, `Money.percentOf()` |
| Bill timing | Keep the original price lock when the Waiter takes the order | The Cashier collects the stored bill without recalculating it | Existing `Waiter` and `Order` |

## Stage 3 decisions

| Decision | Choice | Reason | Code |
|---|---|---|---|
| Host timing | Host before Waiter, both before Chef | Host can seat while Waiter takes the same customer's order | `GameController.askHost()` |
| Seating reuse | Host and Waiter extend SeatingStaff | Shares validations without duplicating the rule; separate staff task budgets | `SeatingStaff` |
| Combo registration | Append Salad + Burger to the three dishes | Keeps individual dishes available and demonstrates the existing MenuItem interface | `Menu.stageThree()`, `ComboMeal` |
| Customer selection | Keep ID modulo menu size and Critic maximum-price rule | Extends through existing polymorphism without type checks | `Customer`, `CriticCustomer` unchanged selection methods |
| Happy Hour turn start | Arrivals, activation choice, recovery, displayed state, staff actions | Includes all current waiting customers; arrivals start at the cap anyway | `GameController.runTurn()` |
| Happy Hour lifetime | Activation sets two remaining turns; decrement once at turn end | Activation turn and following turn are active, with no reactivation | `HappyHour` |
| Discount sequence | Combo's rounded price, then customer discount, then Happy Hour; round to cents at each step | VIP Burger becomes $10.80; stored order bill is final | `ComboMeal`, `HappyHour`, `Waiter` |
| Recovery | Only waiting, seated, or ordered customers; cap 100 | Excludes served, paid and departed customers, preserves normal waiting loss | `Customer.recoverWaitingSatisfaction()` |

Stage 2 is preserved at its existing tag. The extension keeps the original order,
cooking, eating, cashier, abandonment and result flow, adding only the new phases.
The shared seating validation also checks actual queue membership before acting.
