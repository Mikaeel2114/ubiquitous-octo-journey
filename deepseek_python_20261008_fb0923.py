#!/usr/bin/env python3
"""
fixer.py — BedFight plugin feature patcher.

Applies:
  1. Countdown ticking sound each second (players + spectators)
  2. /bedfight spec <player> command
  3. Custom spectator mode (no block phasing, no default spectator menu)
  4. Scoreboard refresh fix (queue / match / spectate transitions)

Usage:
    python3 fixer.py [source_root]

If source_root is omitted, the current working directory is used.
Files touched:
    com/example/bedfight/game/Match.java
    com/example/bedfight/game/Match$1.java
    com/example/bedfight/game/GameManager.java
    com/example/bedfight/command/BedFightCommand.java
    com/example/bedfight/listener/ArenaListener.java
    com/example/bedfight/scoreboard/ScoreboardManager.java
    scoreboard.yml
"""

import os
import re
import shutil
import sys
from pathlib import Path
from datetime import datetime

# --------------------------------------------------------------------- utils

JD_PREFIX_RE = re.compile(r'^/\*[^*]*\*/[ ]?', re.MULTILINE)


def maybe_strip_jd_prefix(text: str) -> str:
    """If most non-empty lines start with a JD-Core /*..*/ prefix, strip it."""
    lines = text.split('\n')
    total = 0
    prefixed = 0
    for line in lines:
        if line.strip():
            total += 1
            if re.match(r'^/\*[^*]*\*/', line):
                prefixed += 1
    if total and prefixed / total > 0.7:
        text = JD_PREFIX_RE.sub('', text)
    return text


def read_source(path: Path) -> str:
    with open(path, 'r', encoding='utf-8') as f:
        return maybe_strip_jd_prefix(f.read())


def write_source(path: Path, text: str) -> None:
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)


def backup(path: Path) -> None:
    if not path.exists():
        return
    stamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    bak = path.with_name(path.name + f'.bak_{stamp}')
    shutil.copy2(path, bak)


def must_replace(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f'[{label}] anchor not found:\n---\n{old[:400]}\n---')
    return text.replace(old, new, 1)


def must_insert_before(text: str, anchor: str, insertion: str, label: str) -> str:
    idx = text.find(anchor)
    if idx == -1:
        raise RuntimeError(f'[{label}] anchor not found:\n---\n{anchor[:400]}\n---')
    return text[:idx] + insertion + text[idx:]


def must_insert_after(text: str, anchor: str, insertion: str, label: str) -> str:
    idx = text.find(anchor)
    if idx == -1:
        raise RuntimeError(f'[{label}] anchor not found:\n---\n{anchor[:400]}\n---')
    end = idx + len(anchor)
    return text[:end] + insertion + text[end:]


# =================================================================== MATCH.JAVA

MATCH_SPECTATOR_API = '''  /* ================= Spectator API ================= */
  public boolean isSpectator(Player p) {
    return this.spectators.contains(p.getUniqueId());
  }

  public Set<UUID> getSpectators() {
    return Collections.unmodifiableSet(this.spectators);
  }

  public void addSpectator(Player p) {
    if (this.state == State.ENDING || this.state == State.PREPARING) {
      this.plugin.msg(p, ChatColor.RED + "The match is not running.");
      return;
    }
    if (this.world == null) {
      this.plugin.msg(p, ChatColor.RED + "The match world is not ready yet.");
      return;
    }
    if (this.byPlayer.containsKey(p.getUniqueId())) {
      this.plugin.msg(p, ChatColor.RED + "You are already playing in this match.");
      return;
    }
    if (this.spectators.contains(p.getUniqueId())) {
      Pos sp0 = this.arena.getSpawn();
      if (sp0 != null) p.teleport(sp0.toLocation(this.world));
      return;
    }
    this.spectators.add(p.getUniqueId());
    this.games.attachSpectator(this, p);

    // Custom spectator: ADVENTURE + flight => cannot phase through blocks,
    // and the default "Teleport to player" / swap-hands menu never opens.
    p.setGameMode(GameMode.ADVENTURE);
    p.setAllowFlight(true);
    p.setFlying(true);
    p.setFireTicks(0);
    p.setFallDistance(0.0F);
    p.setHealth(p.getMaxHealth());
    p.setFoodLevel(20);
    p.setSaturation(20.0F);
    for (PotionEffect pe : new ArrayList<PotionEffect>(p.getActivePotionEffects())) {
      p.removePotionEffect(pe.getType());
    }
    p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
        Integer.MAX_VALUE, 0, false, false));
    for (Player o : Bukkit.getOnlinePlayers()) {
      if (!o.equals(p)) o.hidePlayer(p);
      if (!o.equals(p) && !isSpectator(o)) p.hidePlayer(o);
    }

    Pos sp = this.arena.getSpawn();
    if (sp != null) p.teleport(sp.toLocation(this.world));

    this.plugin.msg(p, ChatColor.GREEN + "You are now spectating this BedFight match.");
    this.plugin.msg(p, ChatColor.GRAY + "Use /bedfight leave to stop spectating.");
  }

  public void removeSpectator(Player p, boolean sendToLobby) {
    if (!this.spectators.remove(p.getUniqueId())) return;
    this.games.detachSpectator(p);
    for (Player o : Bukkit.getOnlinePlayers()) {
      if (!o.equals(p)) {
        o.showPlayer(p);
        p.showPlayer(o);
      }
    }
    reset(p);
    if (sendToLobby) p.teleport(this.plugin.getLobby());
  }
  /* ================================================== */

'''


def patch_match_java(text: str) -> str:
    # 1. import PotionEffectType
    if 'import org.bukkit.potion.PotionEffectType;' not in text:
        text = must_replace(
            text,
            'import org.bukkit.potion.PotionEffect;\n',
            'import org.bukkit.potion.PotionEffect;\n'
            'import org.bukkit.potion.PotionEffectType;\n',
            'Match.java: import PotionEffectType',
        )

    # 2. spectators field
    if 'private final Set<UUID> spectators' not in text:
        text = must_replace(
            text,
            'private final Set<Long> placed = new HashSet<>();\n',
            'private final Set<Long> placed = new HashSet<>();\n'
            '  private final Set<UUID> spectators = new HashSet<>();\n',
            'Match.java: spectators field',
        )

    # 3. spectator API methods
    if 'public boolean isSpectator(Player p)' not in text:
        text = must_insert_before(
            text,
            '  public State getState() {',
            MATCH_SPECTATOR_API,
            'Match.java: spectator API',
        )

    # 4. finish() — clean up spectators before unregister
    if 'this.spectators.clear();' not in text:
        text = must_insert_before(
            text,
            '    this.games.unregister(this);',
            '    for (UUID sid : new ArrayList<UUID>(this.spectators)) {\n'
            '      Player sp = Bukkit.getPlayer(sid);\n'
            '      if (sp != null) removeSpectator(sp, true);\n'
            '    }\n'
            '    this.spectators.clear();\n',
            'Match.java: finish() spectator cleanup',
        )

    # 5. onQuit / onLeave guards
    if 'removeSpectator(p, false); return;' not in text:
        text = must_replace(
            text,
            'leave(p, p.getName() + " disconnected.");',
            'if (isSpectator(p)) { removeSpectator(p, false); return; }\n'
            '    leave(p, p.getName() + " disconnected.");',
            'Match.java: onQuit guard',
        )
    if 'removeSpectator(p, true); return;' not in text or 'left the match' not in text:
        text = must_replace(
            text,
            'leave(p, p.getName() + " left the match.");',
            'if (isSpectator(p)) { removeSpectator(p, true); return; }\n'
            '    leave(p, p.getName() + " left the match.");',
            'Match.java: onLeave guard',
        )

    # 6. interactive guards
    if 'public void onPlace(BlockPlaceEvent e) {\n    Player p = e.getPlayer();\n    if (isSpectator(p))' not in text:
        text = must_insert_after(
            text,
            'public void onPlace(BlockPlaceEvent e) {\n    Player p = e.getPlayer();\n',
            '    if (isSpectator(p)) { e.setCancelled(true); return; }\n',
            'Match.java: onPlace guard',
        )
    if 'public void onBreak(BlockBreakEvent e) {\n    Player p = e.getPlayer();\n    if (isSpectator(p))' not in text:
        text = must_insert_after(
            text,
            'public void onBreak(BlockBreakEvent e) {\n    Player p = e.getPlayer();\n',
            '    if (isSpectator(p)) { e.setCancelled(true); return; }\n',
            'Match.java: onBreak guard',
        )
    if 'isSpectator((Player)e.getEntity())' not in text:
        text = must_insert_after(
            text,
            'public void onDamage(EntityDamageEvent e) {\n',
            '    if (e.getEntity() instanceof Player && isSpectator((Player)e.getEntity())) {\n'
            '      e.setCancelled(true);\n'
            '      return;\n'
            '    }\n',
            'Match.java: onDamage guard',
        )
    if 'public void onMove(PlayerMoveEvent e) {\n    Player p = e.getPlayer();\n    if (isSpectator(p)) return;' not in text:
        text = must_insert_after(
            text,
            'public void onMove(PlayerMoveEvent e) {\n    Player p = e.getPlayer();\n',
            '    if (isSpectator(p)) return;\n',
            'Match.java: onMove guard',
        )

    return text


# ================================================================= MATCH$1.JAVA

def patch_match_1_java(text: str) -> str:
    # 1. Ticking sound (CLICK) instead of the old drum hit.
    text = text.replace(
        'p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F);',
        'p.playSound(p.getLocation(), Sound.CLICK, 1.0F, 1.0F);',
    )

    # 2. Show the actual number in the title.
    old_title = ('Titles.send(p, String.valueOf(c) + String.valueOf(c) + '
                 'String.valueOf(ChatColor.BOLD), '
                 'String.valueOf(ChatColor.GRAY) + "Get ready to fight!", 0, 25, 5);')
    new_title = ('Titles.send(p,\n'
                 '            String.valueOf(c) + String.valueOf(ChatColor.BOLD) + this.left,\n'
                 '            String.valueOf(ChatColor.GRAY) + "Get ready to fight!", 0, 25, 5);')
    if old_title in text:
        text = text.replace(old_title, new_title, 1)

    # 3. Tick sound for spectators too.
    if 'Match.this.getSpectators()' not in text:
        text = must_insert_before(
            text,
            '    this.left--;',
            '    for (UUID sid : Match.this.getSpectators()) {\n'
            '      Player sp = Bukkit.getPlayer(sid);\n'
            '      if (sp != null) sp.playSound(sp.getLocation(), Sound.CLICK, 1.0F, 1.0F);\n'
            '    }\n',
            'Match$1.java: spectator tick sound',
        )

    return text


# ============================================================= GAMEMANAGER.JAVA

def patch_game_manager_java(text: str) -> str:
    # 1. bySpectator field
    if 'private final Map<UUID, Match> bySpectator' not in text:
        text = must_replace(
            text,
            '  private final Map<String, Match> byWorld = new HashMap<>();',
            '  private final Map<String, Match> byWorld = new HashMap<>();\n'
            '  private final Map<UUID, Match> bySpectator = new HashMap<>();',
            'GameManager.java: bySpectator field',
        )

    # 2. spectator API methods
    if 'public Match getSpectating(Player p)' not in text:
        text = must_insert_before(
            text,
            '  public Match getMatch(Player p) {',
            '  public Match getSpectating(Player p) {\n'
            '    return this.bySpectator.get(p.getUniqueId());\n'
            '  }\n'
            '  public Set<UUID> getSpectatorIds() {\n'
            '    return new HashSet<>(this.bySpectator.keySet());\n'
            '  }\n'
            '  void attachSpectator(Match m, Player p) {\n'
            '    this.bySpectator.put(p.getUniqueId(), m);\n'
            '  }\n'
            '  void detachSpectator(Player p) {\n'
            '    this.bySpectator.remove(p.getUniqueId());\n'
            '  }\n',
            'GameManager.java: spectator API',
        )

    # 3. activePlayers includes spectators
    if 's.addAll(this.bySpectator.keySet());' not in text:
        text = must_replace(
            text,
            '    s.addAll(this.queued.keySet());\n'
            '    return s;',
            '    s.addAll(this.queued.keySet());\n'
            '    s.addAll(this.bySpectator.keySet());\n'
            '    return s;',
            'GameManager.java: activePlayers',
        )

    # 4. isBusy includes spectators
    if 'this.bySpectator.containsKey(p.getUniqueId())' not in text:
        text = must_replace(
            text,
            'return (this.byPlayer.containsKey(p.getUniqueId()) || '
            'this.queued.containsKey(p.getUniqueId()));',
            'return (this.byPlayer.containsKey(p.getUniqueId()) || '
            'this.queued.containsKey(p.getUniqueId()) || '
            'this.bySpectator.containsKey(p.getUniqueId()));',
            'GameManager.java: isBusy',
        )

    # 5. leaveQueue cleans up spectators too
    old_lq = ('    Mode m = this.queued.remove(p.getUniqueId());\n'
              '    this.queuedAt.remove(p.getUniqueId());\n'
              '    if (m == null) {\n'
              '      return false;\n'
              '    }\n'
              '    ((LinkedHashSet)this.queues.get(m)).remove(p.getUniqueId());\n'
              '    return true;')
    new_lq = ('    Mode m = this.queued.remove(p.getUniqueId());\n'
              '    this.queuedAt.remove(p.getUniqueId());\n'
              '    Match spec = this.bySpectator.remove(p.getUniqueId());\n'
              '    if (spec != null) {\n'
              '      spec.removeSpectator(p, true);\n'
              '    }\n'
              '    if (m == null) {\n'
              '      return spec != null;\n'
              '    }\n'
              '    ((LinkedHashSet)this.queues.get(m)).remove(p.getUniqueId());\n'
              '    return true;')
    if 'Match spec = this.bySpectator.remove' not in text:
        text = must_replace(text, old_lq, new_lq, 'GameManager.java: leaveQueue')

    # 6. unregister drops spectator entries pointing at this match
    if 'this.bySpectator.values().removeIf' not in text:
        text = must_replace(
            text,
            '    for (UUID id : m.getPlayerIds()) {\n'
            '      this.byPlayer.remove(id, m);\n'
            '    }',
            '    for (UUID id : m.getPlayerIds()) {\n'
            '      this.byPlayer.remove(id, m);\n'
            '    }\n'
            '    this.bySpectator.values().removeIf(mm -> mm == m);',
            'GameManager.java: unregister spectator cleanup',
        )

    return text


# =========================================================== BEDFIGHTCOMMAND.JAVA

SPEC_HANDLER = '''    if (sub.equals("spec")) {
      if (args.length < 2) {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Usage: /bedfight spec <player>");
        return true;
      }
      Player target = Bukkit.getPlayerExact(args[1]);
      if (target == null) {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Player '" + args[1] + "' is not online.");
        return true;
      }
      if (target.equals(p)) {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "You can't spectate yourself.");
        return true;
      }
      Match targetMatch = this.plugin.getGameManager().getMatch(target);
      if (targetMatch == null || targetMatch.getWorld() == null) {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + target.getName() + " is not in a running match.");
        return true;
      }
      Match currentMatch = this.plugin.getGameManager().getMatch(p);
      if (currentMatch != null) {
        currentMatch.onLeave(p);
      }
      this.plugin.getGameManager().leaveQueue(p);
      targetMatch.addSpectator(p);
      this.plugin.getScoreboards().refresh(p);
      return true;
    }
    
'''


def patch_command_java(text: str) -> str:
    # 1. Insert the spec handler before the ADMIN_SUBS gate.
    if 'if (sub.equals("spec")) {' not in text:
        text = must_insert_before(
            text,
            '    if (!ADMIN_SUBS.contains(sub)) {',
            SPEC_HANDLER,
            'BedFightCommand.java: spec handler',
        )

    # 2. Tab completion.
    old_tab1 = 'out.addAll(List.of("1v1", "2v2", "3v3", "leave"));'
    new_tab1 = 'out.addAll(List.of("1v1", "2v2", "3v3", "spec", "leave"));'
    if old_tab1 in text:
        text = text.replace(old_tab1, new_tab1, 1)

    old_tab2 = '    } else if (args.length == 2 && p.hasPermission("bedfight.admin")) {'
    new_tab2 = ('    } else if (args.length == 2 && args[0].equalsIgnoreCase("spec")) {\n'
                '      for (Player online : Bukkit.getOnlinePlayers()) {\n'
                '        if (this.plugin.getGameManager().getMatch(online) != null) {\n'
                '          out.add(online.getName());\n'
                '        }\n'
                '      }\n'
                '    } else if (args.length == 2 && p.hasPermission("bedfight.admin")) {')
    if 'args[0].equalsIgnoreCase("spec")' not in text:
        text = must_replace(text, old_tab2, new_tab2, 'BedFightCommand.java: spec tab-complete')

    return text


# =========================================================== ARENALISTENER.JAVA

ARENA_NEW_HANDLERS = '''  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInventoryOpen(InventoryOpenEvent e) {
    if (!(e.getPlayer() instanceof Player)) return;
    Player p = (Player)e.getPlayer();
    Match m = this.plugin.getGameManager().getSpectating(p);
    if (m != null) {
      e.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInteract(PlayerInteractEvent e) {
    Player p = e.getPlayer();
    if (this.plugin.getGameManager().getSpectating(p) != null) {
      e.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onSpecMove(PlayerMoveEvent e) {
    Player p = e.getPlayer();
    Match m = this.plugin.getGameManager().getSpectating(p);
    if (m == null || m.getWorld() == null) return;
    if (!p.getWorld().equals(m.getWorld())) return;
    Block to = e.getTo().getBlock();
    Block from = e.getFrom().getBlock();
    if (to.getType().isSolid() && !from.getType().isSolid()) {
      Location fixed = e.getFrom().clone();
      fixed.setYaw(e.getTo().getYaw());
      fixed.setPitch(e.getTo().getPitch());
      e.setTo(fixed);
    }
  }

'''


def patch_arena_listener_java(text: str) -> str:
    # 1. Imports
    if 'import org.bukkit.Location;' not in text:
        text = must_replace(
            text,
            'import org.bukkit.World;\n',
            'import org.bukkit.Location;\n'
            'import org.bukkit.World;\n'
            'import org.bukkit.block.Block;\n'
            'import org.bukkit.entity.Player;\n',
            'ArenaListener.java: imports (Location/Block/Player)',
        )
    if 'import org.bukkit.event.inventory.InventoryOpenEvent;' not in text:
        text = must_replace(
            text,
            'import org.bukkit.event.entity.ItemSpawnEvent;\n',
            'import org.bukkit.event.entity.ItemSpawnEvent;\n'
            'import org.bukkit.event.inventory.InventoryOpenEvent;\n',
            'ArenaListener.java: InventoryOpenEvent import',
        )
    if 'import org.bukkit.event.player.PlayerInteractEvent;' not in text:
        text = must_replace(
            text,
            'import org.bukkit.event.player.PlayerMoveEvent;\n',
            'import org.bukkit.event.player.PlayerInteractEvent;\n'
            'import org.bukkit.event.player.PlayerMoveEvent;\n',
            'ArenaListener.java: PlayerInteractEvent import',
        )

    # 2. New handlers
    if 'public void onSpecMove(PlayerMoveEvent e)' not in text:
        text = must_insert_before(
            text,
            '  @EventHandler\n  public void onQuit(PlayerQuitEvent e) {',
            ARENA_NEW_HANDLERS,
            'ArenaListener.java: spectator handlers',
        )

    return text


# ====================================================== SCOREBOARDMANAGER.JAVA

SCOREBOARD_NEW_METHODS = '''  public void refresh(Player p) {
    if (p == null) return;
    PlayerBoard old = this.boards.remove(p.getUniqueId());
    if (old != null) {
      p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
    tick();
  }

  private List<String> spectatorLines(Player p, Match m) {
    Map<String, String> ph = m.placeholders(p);
    ph.put("%ip%", this.cfg.getString("ip", "minemen.club"));
    ph.put("%status%", "Spectating");
    List<String> out = new ArrayList<String>();
    List<String> template = this.cfg.getStringList("spectator.lines");
    if (template == null || template.isEmpty()) {
      template = new ArrayList<String>();
      template.add("&7&m------------");
      template.add("&fMode: &b%mode%");
      template.add("&fMap: &b%map%");
      template.add("");
      template.add("&fStatus: &eSpectating");
      template.add("&fTime: &b%time%");
      template.add("");
      template.add("&fYour Ping: &a%ping%ms");
      template.add("");
      template.add("&b%ip%");
    }
    for (String raw : template) {
      out.add(color(replace(raw, ph)));
    }
    return out;
  }

'''


def patch_scoreboard_manager_java(text: str) -> str:
    # 1. Look up the spectator match alongside the playing match.
    if 'Match sm = games.getSpectating(p);' not in text:
        text = must_insert_after(
            text,
            '      Match m = games.getMatch(p);\n',
            '      Match sm = games.getSpectating(p);\n',
            'ScoreboardManager.java: spectator lookup',
        )

    # 2. Insert the spectator branch before the queue branch.
    if 'lines = spectatorLines(p, sm);' not in text:
        text = must_replace(
            text,
            '} else if (games.getQueuedMode(p) != null) {',
            '} else if (sm != null) {\n'
            '        title = this.cfg.getString("spectator.title", this.cfg.getString("match.title", "&b&lBedFight"));\n'
            '        lines = spectatorLines(p, sm);\n'
            '      } else if (games.getQueuedMode(p) != null) {',
            'ScoreboardManager.java: spectator branch',
        )

    # 3. Add refresh() and spectatorLines() before matchLines().
    if 'public void refresh(Player p)' not in text:
        text = must_insert_before(
            text,
            '  private List<String> matchLines(Player p, Match m) {',
            SCOREBOARD_NEW_METHODS,
            'ScoreboardManager.java: spectator methods',
        )

    return text


# ================================================================= SCOREBOARD.YML

SPECTATOR_YAML = '''spectator:
  title: "&b&lMinemen Club"
  lines:
    - "&7&m------------"
    - "&fMode: &b%mode%"
    - "&fMap: &b%map%"
    - ""
    - "&fStatus: &eSpectating"
    - "&fTime: &b%time%"
    - ""
    - "&fYour Ping: &a%ping%ms"
    - ""
    - "&b%ip%"
'''


def patch_scoreboard_yml(root: Path) -> None:
    yml = root / 'scoreboard.yml'
    if not yml.exists():
        print('[skip] scoreboard.yml not found')
        return
    with open(yml, 'r', encoding='utf-8') as f:
        text = f.read()
    if 'spectator:' in text:
        print('[skip] scoreboard.yml already has spectator section')
        return
    backup(yml)
    text = text.rstrip() + '\n\n' + SPECTATOR_YAML + '\n'
    with open(yml, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)
    print('[ok]   scoreboard.yml')


# ========================================================================= main

TARGETS = [
    ('com/example/bedfight/game/Match.java',              patch_match_java),
    ('com/example/bedfight/game/Match$1.java',            patch_match_1_java),
    ('com/example/bedfight/game/GameManager.java',        patch_game_manager_java),
    ('com/example/bedfight/command/BedFightCommand.java', patch_command_java),
    ('com/example/bedfight/listener/ArenaListener.java',  patch_arena_listener_java),
    ('com/example/bedfight/scoreboard/ScoreboardManager.java', patch_scoreboard_manager_java),
]


def main() -> int:
    root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path.cwd()
    if not (root / 'com').is_dir():
        print(f'!! {root} does not contain a "com" folder. Aborting.')
        return 1

    print(f'Patching in: {root}\n')

    for rel, patcher in TARGETS:
        path = root / rel
        if not path.exists():
            print(f'[skip] {rel} not found')
            continue
        try:
            original = read_source(path)
        except Exception as ex:
            print(f'[ERR ] {rel}: cannot read -> {ex}')
            continue

        try:
            patched = patcher(original)
        except RuntimeError as ex:
            print(f'[ERR ] {rel}:')
            print(str(ex))
            continue
        except Exception as ex:
            print(f'[ERR ] {rel}: {type(ex).__name__}: {ex}')
            continue

        if patched == original:
            print(f'[skip] {rel} (already patched)')
            continue

        backup(path)
        write_source(path, patched)
        print(f'[ok]   {rel}')

    patch_scoreboard_yml(root)

    print('\nDone. Backups have .bak_<timestamp> suffix.')
    print('Recompile with:  mvn clean package   (or your usual build)')
    return 0


if __name__ == '__main__':
    sys.exit(main())