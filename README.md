# MAS Polymorphic Simulation (Concept Objet M2)

Elves + Humans (GOOD) against Orcs + Goblins (BAD) on a 16 x 12 map. Agents collect
messages and bring them to their Master. The team whose Master holds the most distinct
messages wins.

## Run

| Where | How |
|---|---|
| Windows | `.\build.bat` once, then `.\run.bat` opens the game window |
| Mac / Linux | `sh build.sh` then `sh run.sh` |
| NetBeans | File > Open Project > select this folder (Maven project), then Run |
| Self-checks | `java -cp out mas.SelfTest` |

**Game window**: a title screen, then the board with drawn characters (Elf, Human knight, Orc,
Goblin; Masters wear a crown). Each step is replayed slowly in the engine's shuffled turn order
(shown in the bar at the top): agents walk tile by tile, envelopes fly between agents when they
share messages and to the Master on delivery, fights show both rock-paper-scissors picks and
the winner taking the envelopes, energy gifts travel as purple orbs, meteorites fall and explode.
The caption at the bottom says what is happening. Controls: Play / Pause, Step (one step),
Restart (same seed), New random game, seed box, speed (Slow, Normal, Fast, Very fast, Turbo,
Instant). Hover a character for its details.

The engine still computes each step instantly and in order (brief rules). The window only
replays the recorded turns (`EventLog.Turn`) with animations, so the display never changes
the simulation.

**Terminal (ASCII) version**: `run.bat 42 --console`. Options: `[seed|random] [maxSteps] [delayMs]
[--fast] [--quiet] [--no-color]`. `run.bat 7 --quiet` prints only the result.
The same seed always replays the same simulation.

## Code map

| Package | Contents |
|---|---|
| `mas` | `Main`, `Config` (every tunable number), `SelfTest` |
| `mas.map` | `Map` (neighbours 3/5/8, direction to SafeZone, obstacles, BFS path home), `Tile`, `SafeZone`, `Position`, `Direction` |
| `mas.model` | `Being` > `Agent` > `GoodAgent`/`BadAgent` > `Elf`, `Human`, `Orc`, `Goblin`; `Being` > `Master` > 4 Singleton Masters; enums |
| `mas.movement` | Strategy pattern: `MovementStrategy` > `LineMove` > `QueenMove`, `BishopMove`, `RookMove`; `KnightMove` |
| `mas.sim` | `Simulation` (main loop), `RandomGen` (single seeded generator, Singleton), `EventLog`, `EventManager` (meteorites) |
| `mas.view` | `SimulationView` interface, implemented by `ConsoleRenderer` (ASCII) and `gui.SimulationWindow` (Swing) |
| `mas.view.gui` | `SimulationWindow`, `MapPanel` (board + animations), `Sprites` (characters), `Playback` (step replay), `DisplayState`, `ScorePanel`, `AgentTableModel`, `Palette` |

Masters are Singletons, so they live as long as the program. `Master.resetForNewGame()` and
`InstanceCounters.resetForNewGame()` make Restart / New game start clean.

## Design patterns
- **Singleton**: `MasterElf`, `MasterHuman`, `MasterOrc`, `MasterGoblin` (required). `RandomGen` is also a Singleton, so the whole run shares one seeded stream.
- **Strategy** (bonus): each population's movement is a `MovementStrategy`.

## Rules we adapted (to justify in the report)
1. **Movement**: Elf = Queen, Human = Bishop, Orc = Rook (1-4 tiles), Goblin = Knight walked as an L **tile by tile**, so it can meet agents on its path.
2. **Return home** (EP < 20 %): BFS shortest path around obstacles (and around other agents when possible, so tired agents don't jam), up to 3 tiles per step (bonus). An agent rests at home until it reaches 90 % EP.
3. **Masters start empty**. Agents start with 3 unique messages each (48 in total). Delivery happens on entering the own SafeZone and is a **copy**: the agent keeps its messages and can keep sharing them.
4. **Blocked by an obstacle, the map edge, an enemy SafeZone or an enemy exhausted agent**: stop in front and lose the EP of the tiles not travelled (brief rule). Blocked by an active agent: meet it and pay only for the tiles walked.
5. **Revival**: an agent of the same alliance that bumps into an exhausted agent donates 30 % of max EP (never going below its own 20 %). The revived agent goes straight home. Exhausted agents still lose all their messages. Enemies cannot revive.
6. **Confrontation**: rock-paper-scissors, ties replayed. The winner takes 1-3 messages it lacks, and the loser loses them. Orcs favour ROCK and Goblins favour SCISSORS (polymorphic `chooseRps()`). Good agents play uniformly at random, so every fight stays 50/50.
7. **Meteorites** (bonus): 4 % chance per step. A meteorite creates an obstacle, or exhausts the agent it hits. SafeZones are protected.

## Balance observed (60 seeds)
Games last about 170 steps on average and usually end when one Master collects all 48 messages.
Wins: Elves 21, Orcs 14, Humans 12, Goblins 9, draws 4.
Two tuning lessons for the report: (1) giving every population a rock-paper-scissors favourite made
fights unfair (Orcs won only 3 out of 60 games), so Good agents now play at random; (2) a shortest path
home that ignored other agents created traffic jams with hundreds of repeated fights, so it now goes
around them when it can.

## CI
`.github/workflows/ci.yml` runs on every push and pull request. It checks that the code compiles with warnings treated as errors (Java 21 and 25), runs the self-checks, and checks that 10 seeds each end with a winner or draw. It also checks that the same seed replays identically, that the step limit is respected, that the Maven build and jar run, and that the Windows `.bat` scripts work.
