# CommandCooldown

A lightweight, server-side Fabric mod that adds configurable cooldowns to any command registered through Brigadier — including commands from other mods — without requiring any code changes.

## Features

- **Universal command support** — works with vanilla commands and any mod that uses Brigadier (Cobblemon, HennyEssentials, BlanketRTP, etc.)
- **Fully config-driven** — add or remove cooldowns by editing a single JSON file, no recompilation needed
- **OP bypass** — operators can optionally skip cooldowns
- **Permission bypass** — support for a custom permission node (extensible via `PermissionEvaluator`)
- **Flexible duration format** — supports `30s`, `10m`, `2h`, `1d`, and combinations like `1h30m`, `2m15s`, `1d12h`
- **In-game reload** — apply config changes without restarting the server
- **Server-side only** — does not need to be installed on the client
- **Extremely lightweight** — no external dependencies beyond Fabric API

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | ≥ 0.18.4 |
| Fabric API | 0.116.11+1.21.1 |
| Java | ≥ 21 |

## Installation

1. Download the latest `.jar` from [Releases](https://github.com/fadlanmuzaini1/CommandCooldown/releases).
2. Place it in your server's `mods/` folder.
3. Start the server — `config/commandcooldown.json` will be generated automatically with default entries.
4. Edit the config file to your needs, then run `/commandcooldown reload`.

## Configuration

The config file is located at `config/commandcooldown.json` and is generated automatically on first run.

```json
{
  "opsBypass": true,
  "permissionBypass": "commandcooldown.bypass",
  "cooldownMessage": "§cPlease wait %time% before using /%command% again.",
  "commands": [
    {
      "name": "repair",
      "cooldown": "10m"
    },
    {
      "name": "teach",
      "cooldown": "1h"
    },
    {
      "name": "enchant",
      "cooldown": "30m"
    }
  ]
}
```

### Fields

| Field | Type | Description |
|---|---|---|
| `opsBypass` | boolean | If `true`, players with OP level 4 bypass all cooldowns |
| `permissionBypass` | string | Players with this permission node bypass all cooldowns. Leave empty to disable |
| `cooldownMessage` | string | Message shown when a command is on cooldown. Supports `%time%` and `%command%` placeholders |
| `commands` | array | List of commands to apply cooldowns to |

### Adding a command cooldown

Simply add an entry to the `commands` array:

```json
{
  "name": "heal",
  "cooldown": "5m"
}
```

- `name` — the root command name, with or without `/` (e.g. `"repair"` or `"/repair"`)
- `cooldown` — duration string (see [Duration Format](#duration-format) below)

Then run `/commandcooldown reload` in-game to apply changes immediately.

## Duration Format

Durations are written as one or more `<number><unit>` pairs without separators.

| Unit | Character | Example |
|---|---|---|
| Days | `d` | `1d` |
| Hours | `h` | `2h` |
| Minutes | `m` | `30m` |
| Seconds | `s` | `45s` |

Combinations are supported:

| Input | Meaning |
|---|---|
| `1h30m` | 1 hour 30 minutes |
| `2m15s` | 2 minutes 15 seconds |
| `1d12h` | 1 day 12 hours |
| `1d12h30m10s` | 1 day 12 hours 30 minutes 10 seconds |

## Commands

| Command | Permission | Description |
|---|---|---|
| `/commandcooldown reload` | OP level 2 | Reloads `commandcooldown.json` from disk and reapplies all cooldown wrappers |

## Message Placeholders

The `cooldownMessage` field supports the following placeholders:

| Placeholder | Replaced with |
|---|---|
| `%time%` | Remaining cooldown time (e.g. `9m32s`) |
| `%command%` | Name of the command that is on cooldown |

Example:
```json
"cooldownMessage": "§cYou must wait §e%time% §cbefore using §f/%command% §cagain."
```

## Project Structure

```
io.github.fadlanmuzaini1.commandcooldown
├── CommandCooldownMod.java          Entrypoint and composition root
├── command/
│   ├── CommandWrapperService.java   Wraps Brigadier command nodes with cooldown logic
│   ├── CommandInterceptor.java      Core cooldown decision logic
│   ├── CommandNameNormalizer.java   Normalizes command names consistently
│   ├── InterceptionOutcome.java     Value object representing intercept decision
│   └── ReloadCommand.java           /commandcooldown reload
├── config/
│   ├── CommandConfig.java           Immutable record for a single command entry
│   ├── ModConfig.java               Immutable record for the full config file
│   └── ConfigManager.java           Reads, validates, and reloads config.json
├── cooldown/
│   ├── CooldownRegistry.java        O(1) lookup: command name → CommandConfig
│   └── PlayerCooldownTracker.java   Runtime state: UUID + command → expiry time
├── permission/
│   ├── PermissionEvaluator.java     Interface for permission checking (extensible)
│   ├── VanillaOpPermissionEvaluator.java  Default implementation using OP level
│   └── BypassChecker.java           Evaluates OP bypass and permission bypass
├── time/
│   ├── DurationParser.java          Parses "1h30m" → milliseconds
│   └── DurationFormatException.java Thrown on invalid duration strings
└── util/
    └── MessageFormatter.java        Substitutes %time% and %command% placeholders
```

## How It Works

1. On `SERVER_STARTED`, after all mods have registered their commands, `CommandWrapperService` walks the Brigadier command tree and wraps the executor of each configured command with a `CooldownAwareCommand`.
2. When a player runs a wrapped command, `CooldownAwareCommand` checks the player's cooldown state via `PlayerCooldownTracker`.
3. If the cooldown is active, the command is blocked and the cooldown message is sent to the player.
4. If not, the original command executor runs normally. If it succeeds (returns a positive result), a new cooldown entry is recorded for that player.
5. On `/commandcooldown reload`, existing wrappers are removed, the config is re-read and re-parsed, and wrappers are re-applied with the new configuration — all without a server restart.

## Building from Source

```bash
git clone https://github.com/fadlanmuzaini1/CommandCooldown.git
cd CommandCooldown
./gradlew build
```

The output jar will be at `build/libs/commandcooldown-1.0.0.jar`.

## License

This project is licensed under the [MIT License](LICENSE).
