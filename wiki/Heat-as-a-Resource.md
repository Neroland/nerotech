# Heat as a Resource

Before 0.8.0, heat was only a problem: machines throttle when hot and the Fusion Reactor can melt down (see
[Thermal System](Thermal-System.md)). Now heat is also something you move and use. A reactor's or
generator's waste heat can run a Blast Foundry instead of a wall of ice.

Heat never becomes energy in NeroTech. Pipes and exchangers move it; they do not generate power from it.

## Heat Pipe

A Heat Pipe carries heat along a run of connected pipes, from hotter machines to cooler ones and into
Foundry Heat Ports.

- **Endpoints** are any NeroTech machine, any Foundry Heat Port and any block from another mod that accepts
  heat through NeroTech's heat interface, touching a pipe in the run.
- **Each exchange** (`heatPipeExchangeIntervalTicks`, every 10 ticks) moves `heatPipeConductivityPermille`
  (a quarter) of the difference between each endpoint and the run, hottest endpoint first, so heat goes hot
  machine → pipe → cold machine in one step.
- **Long runs lose more.** Each pipe sheds `heatPipeLossPermille` of its heat above ambient per exchange, and a
  longer run has more mass to warm.
- **A run is at most `heatPipeMaxRun` (64) pipes.** Pipes past the cap form their own run.
- **Hot pipes burn.** At heat level 2 or 3 a pipe hurts anything standing on it (`heatPipeContactDamage`,
  `heatPipeContactDamageAmount`), like magma. Sneaking avoids it.
- Heat is never created on the way: what comes out of a run is at most what went in.

Pipes have no block entity. A run is worked out once, cached, and ticks once per exchange from one pipe. It is
rebuilt only when a pipe's connections change, so a long pipe network costs about the same as one machine.

## Heat Exchanger

The Heat Exchanger moves heat between the machine (or Foundry Heat Port) on its **front** and the [Process Tank](Chemistry-and-Cryogenics.md#process-tank)
on its **back**. What is in the tank decides the direction:

| Tank holds | Effect |
| --- | --- |
| A coolant (`heatExchangerCoolants`: cryo coolant, nitrogen) | Soaks up the machine's heat above ambient and is used up. |
| A hot medium (`heatExchangerHotMedia`: lava) | Heats the machine, up to `heatExchangerHotTemperature`, and is used up. |
| Anything else (water, for example) | Stores heat: takes it from a hot machine, gives it back to a cold one. |

Each exchange moves at most `heatExchangerMaxPerOp` heat (more with Speed modules or Overdrive). For plain
cooling, a Coolant Pump with Radiators needs no tank.

The exchanger says when it is stalled and why: no Process Tank, no machine or heat port opposite the tank,
an empty tank, or nothing left to move because both sides are at their limit.

## Building a heat loop

The gallery shows this one (`/nerotech gallery`, bay "Heat loop"):

1. A hot source: a Fusion Reactor, a generator working hard, or a Heat Exchanger heating a machine from a
   lava tank.
2. A run of Heat Pipes from the source to the Foundry Heat Ports in a Blast Foundry shell.
3. The foundry runs once its furnace passes the recipe's heat threshold, and the reactor runs cooler for it.

Keep the run short for the best transfer. The
[Thermal Goggles](Tools.md#thermal-goggles) show the heat of machines and pipes in view.

## See also

- [Thermal System](Thermal-System.md)
- [Metallurgy](Metallurgy.md#blast-foundry)
- [Fusion Reactor](Fusion-Reactor.md)
- [Config](Config.md)
- [Home](Home.md)
