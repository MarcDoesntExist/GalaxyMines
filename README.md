# GalaxyMines

A mine plugin for Paper. You select an area with WorldEdit, tell the plugin what blocks go in it, and it takes care of resets, drops, selling and stats. Menus, holograms, boss bars and PlaceholderAPI placeholders are all configurable.

## Requirements

- Paper 1.21.x (built against the 1.21.11 API)
- Java 21
- WorldEdit 7.x

Everything below is optional:

- Vault, for `/sell` and auto-sell
- PlaceholderAPI, for the `%galaxymines_...%` placeholders
- WorldGuard, to generate protection regions from your mine bounds
- ExcellentEnchants 5.x with NightCore, for area-mining enchant compatibility
- Nexo, ItemsAdder or Oraxen, for custom item prices

## Features

**Mines**

- Cuboid, polygon and circular mines from a WorldEdit selection
- Resets on a timer, at a mined percentage, by hand, or as an animated wave
- Weighted block compositions, with the reset state saved per mine
- Per-mine access and break permissions, composition-only protection, optional WorldGuard regions

**Drops and selling**

- Auto-pickup, auto-smelt, auto-block, auto-sell and drop/sell multipliers, all set per mine
- Vault selling with a `prices.yml` (materials and custom item IDs)
- Personal timed boosters
- Lucky blocks with item, command and message rewards

**Players**

- Daily, weekly and all-time stats with leaderboards
- Sale history, a daily login reward with a streak, daily and weekly quests
- Battle pass, bombs and an auto-miner

**Server**

- Temporary events (double drops, sell bonuses, rare drops)
- GUIs for managing mines, TextDisplay/ItemDisplay holograms, boss bars
- YAML storage by default, SQLite or MySQL if you have a JDBC driver
- TNT and creeper explosions count towards mine progress
- A small public API

## Installation

1. Stop the server.
2. Drop the GalaxyMines jar and WorldEdit into `plugins/`.
3. Start the server. The plugin creates `plugins/GalaxyMines/` with `config.yml`, `Menu.yml`, `Messages.yml`, `prices.yml`, `events.yml`, `luckyblocks.yml` and the `mines/` and `storage/` folders.
4. Edit what you need, then run `/mine reload` or restart.

Upgrading is safe: old mine files, storage, `config.yml`, `Menu.yml` and `Messages.yml` still load, and missing keys fall back to defaults. Legacy colour codes in `Messages.yml` are converted once, and the original is kept as `Messages.yml.bak-<timestamp>`. Back up the plugin folder before a big upgrade anyway.

## Quick start

```
/mine wand
```

Select the area with the wand (a normal WorldEdit selection works too), then:

```
/mine create coal
/mine edit coal ...
/mine reset coal
```

`/mine edit` is where you add materials to the composition and change the alias, reset delay and broadcasts. `/mine info coal` shows what the mine looks like now, and `/mine gui` does all of this from a menu.

## Commands

The main command is `/mine`, also available as `/gmine`, `/gmines`, `/galaxymine`, `/galaxymines` and `/mines`. Mine names are tab-completed. `/mine help` in game lists what your version supports.

| Command | What it does |
|---|---|
| `/mine create <name> [resetDelay] [broadcast] [nearby] [radius] [message]` | Create a mine from your selection |
| `/mine delete <name>` | Delete a mine, its file and its blocks |
| `/mine rename <name> <newName>` | Rename a mine and its file |
| `/mine clone <name> <newName>` | Copy a mine definition |
| `/mine edit <name> ...` | Add or remove materials, change alias, delay, broadcasts |
| `/mine info <name>` | Show a mine's settings and composition |
| `/mine list` | List loaded mines |
| `/mine reset <name\|all>` | Reset one mine or all of them |
| `/mine resetall confirm` | Confirm a reset of every mine |
| `/mine pause\|resume <name>` | Stop or restart automatic resets |
| `/mine setspawn <name>` | Use your position as the mine spawn |
| `/mine tp <name>` | Teleport to a mine |
| `/mine wand` | Get the selection wand |
| `/mine gui` | Open the mine management menu |
| `/mine stats <name>` | Mine statistics |
| `/mine mystats [player]` | Player statistics |
| `/mine top [daily\|weekly\|all]` | Leaderboard (GUI for players, text for console) |
| `/mine leaderboard [daily\|weekly\|all]` | Same as `/mine top` |
| `/mine history [player]` | Last 10 sales, yours or someone else's (admin) |
| `/mine daily` | Claim the daily reward |
| `/mine daily reset <player>` | Clear a player's claim state and streak |
| `/mine quests [player]` | Daily and weekly quests with progress |
| `/mine quests reset <player>` | Reset a player's quests for the current period |
| `/mine milestones [player]` | Lifetime blocks, paid thresholds and the next one |
| `/mine pass`, `/mine pass claim` | List battle pass tiers, claim reached ones |
| `/mine pass addxp\|reset <player>` | Admin helpers for the battle pass |
| `/mine bomb give <player> [radius]` | Give a bomb item |
| `/mine automine [on\|off]` | Toggle the auto-miner |
| `/mine booster give <player> <sell\|drop> <multiplier> <duration>` | Give a timed booster (`90`, `30m`, `2h`, `7d`). Repeating a type stacks, up to the cap |
| `/mine booster list [player]` | Active boosters and time left |
| `/mine booster clear <player> [sell\|drop]` | Remove one type, or all |
| `/mine event list\|start\|stop <id>\|reload` | Manage temporary events |
| `/mine luckyblocks reload\|validate` | Reload or check `luckyblocks.yml` |
| `/mine prices` | List price keys and flag provider IDs that don't resolve |
| `/mine prices gui` | In-game price editor |
| `/mine prices set <key> <price>` | Add or change a price |
| `/mine prices remove <key>` | Remove a price |
| `/mine providers` | Show custom item adapters and unresolved IDs |
| `/mine debug` | Version, caches, reset timing, teleports and other diagnostics |
| `/mine reload` | Reload config, prices, events, integrations and mines |
| `/sell`, `/sellall`, `/sellhand` | Sell priced items from your inventory or hand |

Most admin subcommands need `galaxymines.admin`. Player commands check their own node.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `galaxymines.admin` | op | Admin commands and mine check bypass |
| `galaxymines.bypass` | op | Skip access, break and protection checks |
| `galaxymines.prices.edit` | op | Open the price editor |
| `galaxymines.wand` | op | Get the selection wand |
| `galaxymines.events` | op | Start and stop events |
| `galaxymines.mine.*` | op | Access every mine (when per-mine permissions are on) |
| `galaxymines.mine.*.break` | op | Break in every mine (when break permissions are on) |
| `galaxymines.mine.<name>` | false | Access one mine, registered when the mine loads |
| `galaxymines.mine.<name>.break` | false | Break blocks in one mine |
| `galaxymines.tp` | true | Teleport to mines you can access |
| `galaxymines.sell` | true | `/sell`, `/sellall`, `/sellhand` |
| `galaxymines.stats` | true | Statistics and leaderboards |
| `galaxymines.daily` | true | Daily reward |
| `galaxymines.sell.vip` | false | Sell multiplier from `config.yml` |
| `galaxymines.sell.mvp` | false | Sell multiplier from `config.yml` |
| `galaxymines.bomb` | see `plugin.yml` | Use bombs |
| `galaxymines.autominer` | see `plugin.yml` | Use the auto-miner |

## PlaceholderAPI

The expansion identifier is `galaxymines`. The `mine_*` placeholders need an online player.

| Placeholder | Value |
|---|---|
| `%galaxymines_mine%` | Mine the player is in, or empty |
| `%galaxymines_mine_alias%` | Alias of that mine |
| `%galaxymines_mine_remaining%` | Blocks left in that mine |
| `%galaxymines_mine_percent%` | Percentage left |
| `%galaxymines_mine_reset%` | Time to next reset, `mm:ss` |
| `%galaxymines_mines%` | Number of loaded mines |
| `%galaxymines_events%` | Active event IDs, comma separated |
| `%galaxymines_events_names%` | Active event names, comma separated |
| `%galaxymines_events_remaining%` | `mm:ss` until the first active event ends, `00:00` if none |
| `%galaxymines_event_<id>_name%` | Display name of one event |
| `%galaxymines_event_<id>_remaining%` | `mm:ss` until that event ends, `00:00` if inactive |
| `%galaxymines_economy%` | `yes` or `no` |
| `%galaxymines_booster_sell%` | Active sell booster, like `2.00` (or `1.00`) |
| `%galaxymines_booster_drop%` | Active drop booster, like `2.00` (or `1.00`) |
| `%galaxymines_booster_sell_remaining%` | `mm:ss` until the sell booster ends |
| `%galaxymines_booster_drop_remaining%` | `mm:ss` until the drop booster ends |
| `%galaxymines_total_blocks%` | Blocks mined, all time |
| `%galaxymines_daily_blocks%` | Blocks mined today |
| `%galaxymines_weekly_blocks%` | Blocks mined this week |
| `%galaxymines_total_sold%` | Items sold |
| `%galaxymines_total_earned%` | Money earned, all time |
| `%galaxymines_daily_earned%` | Money earned today |
| `%galaxymines_weekly_earned%` | Money earned this week |

## API

`GalaxyMineAPI` lets other plugins look up mines by name, player or location, list loaded mines, create, delete, rename and reset them, read statistics, and translate messages. Anything that changes a mine has to run on the main thread.

