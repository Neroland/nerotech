# Operating Charges (optional)

A server running **NeroEconomy** (0.3.0 or later) can make NeroTech's processing machines cost credits
to run. It is **off by default**, and NeroTech works exactly the same without NeroEconomy installed.

## What is charged

- **Machines:** the Ore Processor, Fabricator, Advanced Ore Processor, Advanced Fabricator and Chemical
  Processor.
- **Who pays:** the machine's owner — the player who placed it. A machine with no owner is never
  charged. Owners are recorded only while charges (or opt-in pollution attribution) are enabled.
- **How:** in advance, per **batch** of operations — never per tick. When an owned machine starts an
  operation and its paid batch is used up, it charges the owner `machineOperationChargeCost` credits
  for the next `machineOperationChargeBatch` operations (64 by default, one stack).
- **Where the credits go:** the economy's Reserve. A machine never pays anyone; there is no way to earn
  credits from a machine.

## When the charge is refused

If the owner cannot pay (not enough credits, the economy is frozen, too many requests at once, …) the
machine **idles** and its [Analytics](Analytics.md) status line turns to **Unpaid**, showing the
economy's reason. It tries again every 30 seconds and resumes on its own once the charge goes through.

## Server config

In `config/nerotech.properties`:

| Key | Default | Meaning |
| --- | --- | --- |
| `machineOperationChargeCost` | `0` | Credits per batch; `0` turns charges off |
| `machineOperationChargeBatch` | `64` | Operations one payment covers |

## Privacy

The owner's UUID is used only to take the charge, is never logged with an amount and is never sent to
other players' clients. Erasing a player's data removes them as owner of every machine. The full
notice is `PRIVACY.md` in the source repository.
