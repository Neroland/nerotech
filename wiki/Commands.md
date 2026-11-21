# Commands

NeroTech registers one command, `/nerotech`, with the subcommands below. All of them need permission
level 2 (a game master or operator). None of them stores anything about a player.

| Command | Who can run it | What it does |
| --- | --- | --- |
| `/nerotech budget` | Op level 2; players and the server console | Samples how much server tick time NeroTech machines cost for **200 ticks** (10 seconds), then reports it per machine type. |
| `/nerotech gallery` | Op level 2, as a player, in creative mode | Builds the showcase gallery around you: the rotunda of every block and the Offworld Industry plant beside it. |
| `/nerotech gallery clear` | Op level 2, as a player, in creative mode | Removes everything the gallery placed (blocks and label stands) and forgets the gallery site. |
| `/nerotech gallery tour` | Op level 2, as a player, in creative mode, standing inside a gallery | Starts the guided tour at the first bay of the plant. |
| `/nerotech gallery tour next` | as above | Moves you to the next bay. |
| `/nerotech gallery tour stop` | as above | Ends the tour and returns you to the gallery entrance. |

## `/nerotech budget`

Run it with your base loaded and working. After 200 ticks it prints one line per machine type, most
expensive first: mean milliseconds per server tick, total milliseconds over the window, and how many machine
ticks it sampled. A last line gives the total for all NeroTech machines. If nothing ticked, it says so. Only
one sample runs at a time; a second request while one is running is refused.

The report names machine types only. It never names a player or gives a position. Outside a sample the cost
of the measurement is one read per machine tick.

Rough guide to reading it: a whole Minecraft tick has 50 ms. If NeroTech's total is a few milliseconds on a
large base, it is not your lag source.

## `/nerotech gallery`

A creative-mode showcase for players, pack makers and screenshots. It is player-only and creative-only even
for operators, because it places a large number of blocks around you.

What it builds:

- **The rotunda** (from earlier releases): every NeroTech block on a display grid, and every older machine
  running the way it is wired in survival: generators, processing lines, the pollution pair, the Analytics
  Terminal, a Tech Guide pedestal, formed Fusion Reactor shells, the Particle Accelerator ring, the gas chain,
  the coolant loop, the power park and the automation lane.
- **The Offworld Industry plant** east of the rotunda, twelve bays:
  - one bay per chain (Structural, Superconductor, Semiconductor, Optics, Chemistry, Fusion Fuel), running
    from raw input to product;
  - a heat loop carrying reactor heat through Heat Pipes into a formed 3x3x3 Blast Foundry;
  - a Deep Core Drill on the gallery's own demo deposit;
  - a Fusion Reactor fitted with two Fuel Injectors and four Europium Flux Lattices in its shell;
  - Annunciator Panels showing each alarm, a tool rack and an Induction Charger;
  - an item catalogue of everything NeroTech registers, grouped by family;
  - a tank farm of Process Tank columns.

Every machine the gallery places is marked as an exhibit: it never opens a milestone, awards a trophy or
counts towards a statistic. The demo deposit is fixed and never runs out, and drilling it writes nothing to
the world's deposit data.

The gallery remembers where it was built (the dimension and the origin block, nothing about who built it, at
most 16 sites) so that `clear` and `tour` work from anywhere inside it. `gallery clear` forgets the site.

## Related commands from Neroland Core

- `/neroland config reload` reloads `config/nerotech.properties` along with every other Neroland mod's
  config. See [Config](Config.md).

## See also

- [Config](Config.md)
- [Privacy](Privacy.md)
- [Home](Home.md)
