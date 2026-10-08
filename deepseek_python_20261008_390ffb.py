#!/usr/bin/env python3
"""
fixer.py - Make the decompiled BedFight source compile, and apply the
requested feature set:

  1. Countdown ticking sound each second (players + spectators)
  2. /bedfight spec <player>
  3. Custom spectator (no block phasing, no default spectator menu)
  4. Scoreboard refresh fix (queue / match / spectate transitions)

Rewrites Pos.java and Match.java completely (decompiler output is not
syntactically valid). Patches the remaining files in place.

Handles files that still contain JD-Core /*    */ line prefixes.
"""

import os
import re
import shutil
import sys
from datetime import datetime
from pathlib import Path

# ============================================================ helpers

def strip_jd(text: str) -> str:
    """Remove JD-Core /*...*/ line prefixes (no-op if already clean)."""
    out = []
    for line in text.split('\n'):
        m = re.match(r'^[ \t]*/\*[^*]*\*/[ \t]?', line)
        if m:
            line = line[m.end():]
        out.append(line)
    return '\n'.join(out)


def read(path: Path) -> str:
    with open(path, 'r', encoding='utf-8') as f:
        return strip_jd(f.read())


def write(path: Path, text: str) -> None:
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)


def backup(path: Path) -> None:
    if not path.exists():
        return
    stamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    shutil.copy2(path, path.with_name(path.name + f'.bak_{stamp}'))


def replace_must(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise RuntimeError(f'[{label}] anchor not found:\n{old[:300]}')
    return text.replace(old, new, 1)


def insert_before(text: str, anchor: str, ins: str, label: str) -> str:
    i = text.find(anchor)
    if i == -1:
        raise RuntimeError(f'[{label}] anchor not found:\n{anchor[:300]}')
    return text[:i] + ins + text[i:]


def insert_after(text: str, anchor: str, ins: str, label: str) -> str:
    i = text.find(anchor)
    if i == -1:
        raise RuntimeError(f'[{label}] anchor not found:\n{anchor[:300]}')
    return text[:i + len(anchor)] + ins + text[i + len(anchor):]


def ensure_import(text: str, imp: str) -> str:
    if imp in text:
        return text
    m = re.search(r'^(import\s+[^\n]+;\n)', text, re.MULTILINE)
    if m:
        return text[:m.end(1)] + imp + '\n' + text[m.end(1):]
    return text


# ======================================================= clean Pos.java

POS_JAVA = r'''package com.example.bedfight.arena;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;

public final class Pos {

    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public Pos(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }

    public static Pos of(Location l) {
        return new Pos(l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    public static Pos ofBlock(Block b) {
        return new Pos(b.getX(), b.getY(), b.getZ(), 0.0F, 0.0F);
    }

    public Location toLocation(World w) {
        return new Location(w, x, y, z, yaw, pitch);
    }

    public int blockX() { return (int) Math.floor(x); }
    public int blockY() { return (int) Math.floor(y); }
    public int blockZ() { return (int) Math.floor(z); }

    public void write(ConfigurationSection s) {
        s.set("x", (Double) x);
        s.set("y", (Double) y);
        s.set("z", (Double) z);
        s.set("yaw", (Double) (double) yaw);
        s.set("pitch", (Double) (double) pitch);
    }

    public static Pos read(ConfigurationSection s) {
        if (s == null) return null;
        return new Pos(
            s.getDouble("x"),
            s.getDouble("y"),
            s.getDouble("z"),
            (float) s.getDouble("yaw"),
            (float) s.getDouble("pitch")
        );
    }

    @Override
    public String toString() {
        return "Pos{" + x + "," + y + "," + z + "," + yaw + "," + pitch + "}";
    }

    @Override
    public int hashCode() {
        long h = Double.doubleToLongBits(x);
        h = h * 31 + Double.doubleToLongBits(y);
        h = h * 31 + Double.doubleToLongBits(z);
        h = h * 31 + Float.floatToIntBits(yaw);
        h = h * 31 + Float.floatToIntBits(pitch);
        return (int) (h ^ (h >>> 32));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pos)) return false;
        Pos p = (Pos) o;
        return Double.compare(p.x, x) == 0
            && Double.compare(p.y, y) == 0
            && Double.compare(p.z, z) == 0
            && Float.compare(p.yaw, yaw) == 0
            && Float.compare(p.pitch, pitch) == 0;
    }
}
'''

# ===================================================== clean Match.java

MATCH_JAVA = r'''package com.example.bedfight.game;

import com.example.bedfight.BedFightPlugin;
import com.example.bedfight.arena.Arena;
import com.example.bedfight.arena.Pos;
import com.example.bedfight.arena.TeamSpec;
import com.example.bedfight.arena.Teams;
import com.example.bedfight.util.Ping;
import com.example.bedfight.util.Titles;
import com.example.bedfight.util.WorldFiles;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class Match {

    public enum State { PREPARING, COUNTDOWN, RUNNING, ENDING; }

    private static final AtomicInteger IDS = new AtomicInteger();

    private final BedFightPlugin plugin;
    private final GameManager games;
    private final Arena arena;
    private final Mode mode;
    private final String worldName;

    private static final class TeamState {
        final DyeColor color;
        final Pos spawn;
        final Pos bed;
        final Set<UUID> members = new LinkedHashSet<UUID>();
        final Set<UUID> alive = new HashSet<UUID>();
        boolean bedAlive = true;
        TeamState(DyeColor color, Pos spawn, Pos bed) {
            this.color = color;
            this.spawn = spawn;
            this.bed = bed;
        }
    }

    private static final class Damage {
        private final UUID by;
        private final long time;
        private Damage(UUID by, long time) { this.by = by; this.time = time; }
        public UUID by() { return by; }
        public long time() { return time; }
    }

    private final Map<DyeColor, TeamState> teams = new LinkedHashMap<DyeColor, TeamState>();
    private final Map<UUID, TeamState> byPlayer = new HashMap<UUID, TeamState>();
    private final Set<UUID> respawning = new HashSet<UUID>();
    private final Map<UUID, Damage> lastDamage = new HashMap<UUID, Damage>();
    private final Set<Long> placed = new HashSet<Long>();
    private final Set<UUID> spectators = new HashSet<UUID>();

    private State state = State.PREPARING;
    private boolean finished;
    private World world;
    private BukkitTask countdownTask;
    private int countdownLeft;
    private long startMillis;
    private final Map<UUID, Integer> kills = new HashMap<UUID, Integer>();
    private final Map<UUID, Integer> bedBreaks = new HashMap<UUID, Integer>();

    public Match(BedFightPlugin plugin, GameManager games, Arena arena, Mode mode, List<List<Player>> split) {
        this.plugin = plugin;
        this.games = games;
        this.arena = arena;
        this.mode = mode;
        this.worldName = "bf_" + arena.getWorldName() + "_" + IDS.incrementAndGet();

        List<DyeColor> colors = new ArrayList<DyeColor>(arena.getTeams().keySet());
        Collections.shuffle(colors);
        for (int i = 0; i < split.size(); i++) {
            TeamSpec spec = arena.getTeams().get(colors.get(i));
            TeamState ts = new TeamState(spec.getColor(), spec.getSpawn(), spec.getBed());
            for (Player p : split.get(i)) {
                ts.members.add(p.getUniqueId());
                ts.alive.add(p.getUniqueId());
                this.byPlayer.put(p.getUniqueId(), ts);
            }
            this.teams.put(ts.color, ts);
        }
    }

    public static final class TeamView {
        private final DyeColor color;
        private final boolean bedAlive;
        private final int alive;
        private final boolean mine;
        public TeamView(DyeColor color, boolean bedAlive, int alive, boolean mine) {
            this.color = color;
            this.bedAlive = bedAlive;
            this.alive = alive;
            this.mine = mine;
        }
        public DyeColor color() { return color; }
        public boolean bedAlive() { return bedAlive; }
        public int alive() { return alive; }
        public boolean mine() { return mine; }
    }

    public State getState() { return state; }
    public String getWorldName() { return worldName; }
    public World getWorld() { return world; }
    public Set<UUID> getPlayerIds() { return byPlayer.keySet(); }

    /* ==================== Spectator API ==================== */

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
        if (!this.spectators.add(p.getUniqueId())) {
            Pos sp0 = this.arena.getSpawn();
            if (sp0 != null) p.teleport(sp0.toLocation(this.world));
            return;
        }
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

    /* ======================================================== */

    public void prepare() {
        broadcast(ChatColor.GRAY + "Preparing your arena...");
        final File src = new File(Bukkit.getWorldContainer(), this.arena.getWorldName());
        final File dst = new File(Bukkit.getWorldContainer(), this.worldName);
        World template = Bukkit.getWorld(this.arena.getWorldName());
        if (template != null) template.save();

        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
            public void run() {
                try {
                    WorldFiles.copy(src.toPath(), dst.toPath());
                    Bukkit.getScheduler().runTask(Match.this.plugin, new Runnable() {
                        public void run() { begin(); }
                    });
                } catch (final IOException ex) {
                    Match.this.plugin.getLogger().log(Level.SEVERE,
                            "Could not copy arena world " + src, ex);
                    Bukkit.getScheduler().runTask(Match.this.plugin, new Runnable() {
                        public void run() { abort("Could not copy the arena world."); }
                    });
                }
            }
        });
    }

    private void begin() {
        final Path dst = new File(Bukkit.getWorldContainer(), this.worldName).toPath();
        if (this.state != State.PREPARING) {
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                public void run() { WorldFiles.delete(dst); }
            });
            return;
        }
        for (UUID id : this.byPlayer.keySet()) {
            if (Bukkit.getPlayer(id) == null) {
                abort("A player left before the match started.");
                return;
            }
        }
        this.world = Bukkit.createWorld(new WorldCreator(this.worldName));
        if (this.world == null) { abort("Could not load the arena."); return; }
        this.world.setAutoSave(false);
        this.world.setKeepSpawnInMemory(false);
        this.world.setPVP(true);
        this.world.setDifficulty(Difficulty.NORMAL);
        this.world.setStorm(false);
        this.world.setTime(6000L);
        this.world.setGameRuleValue("doDaylightCycle", "false");
        this.world.setGameRuleValue("doMobSpawning", "false");
        Pos sp = this.arena.getSpawn();
        this.world.setSpawnLocation(sp.blockX() + 100000, 64, sp.blockZ() + 100000);

        for (TeamSpec spec : this.arena.getTeams().values()) {
            if (!this.teams.containsKey(spec.getColor())) {
                removeBed(this.world.getBlockAt(
                        spec.getBed().blockX(),
                        spec.getBed().blockY(),
                        spec.getBed().blockZ()));
            }
        }

        this.state = State.COUNTDOWN;
        for (TeamState t : this.teams.values()) {
            for (UUID id : t.members) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    equip(p, t);
                    p.teleport(t.spawn.toLocation(this.world));
                    p.sendMessage(BedFightPlugin.PREFIX + "You are on team "
                            + Teams.display(t.color)
                            + ". Protect your bed, destroy the enemy bed!");
                }
            }
        }

        final int seconds = Math.max(1, this.plugin.getConfig().getInt("countdown-seconds", 5));
        this.countdownTask = new BukkitRunnable() {
            int left = seconds;
            public void run() {
                if (Match.this.state != State.COUNTDOWN) { cancel(); return; }
                if (this.left <= 0) { cancel(); Match.this.startGame(); return; }
                Match.this.countdownLeft = this.left;
                ChatColor c = (this.left > 3) ? ChatColor.GREEN
                            : (this.left == 3) ? ChatColor.YELLOW
                                               : ChatColor.RED;
                for (UUID id : Match.this.byPlayer.keySet()) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) {
                        Titles.send(p,
                                c.toString() + ChatColor.BOLD + this.left,
                                ChatColor.GRAY + "Get ready to fight!",
                                0, 25, 5);
                        p.playSound(p.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                    }
                }
                for (UUID sid : Match.this.spectators) {
                    Player sp2 = Bukkit.getPlayer(sid);
                    if (sp2 != null) {
                        sp2.playSound(sp2.getLocation(), Sound.CLICK, 1.0F, 1.0F);
                    }
                }
                this.left--;
            }
        }.runTaskTimer(this.plugin, 0L, 20L);
    }

    private void startGame() {
        if (this.state != State.COUNTDOWN) return;
        this.state = State.RUNNING;
        this.startMillis = System.currentTimeMillis();
        for (TeamState t : this.teams.values()) {
            for (UUID id : t.members) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    Titles.send(p,
                            ChatColor.GREEN + "FIGHT!",
                            ChatColor.GRAY + "Team " + Teams.display(t.color)
                                    + " - destroy the enemy bed!",
                            0, 30, 10);
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1.0F, 2.0F);
                }
            }
        }
    }

    private void abort(String reason) {
        if (this.state == State.ENDING) return;
        this.state = State.ENDING;
        broadcast(ChatColor.RED + reason);
        finish(true);
    }

    private void end(TeamState winner) {
        if (this.state == State.ENDING) return;
        this.state = State.ENDING;
        if (winner == null) broadcast(ChatColor.YELLOW + "The match ended in a draw.");
        else broadcast(Teams.display(winner.color) + " team wins the match!");

        for (TeamState t : this.teams.values()) {
            boolean won = (t == winner);
            for (UUID id : t.members) {
                Player p = Bukkit.getPlayer(id);
                if (p == null) continue;
                if (winner == null) {
                    Titles.send(p, ChatColor.YELLOW + "DRAW", "", 0, 60, 10);
                } else if (won) {
                    Titles.send(p, ChatColor.GOLD + "VICTORY!",
                            ChatColor.GRAY + "You won the BedFight!", 0, 80, 10);
                    p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F);
                } else {
                    Titles.send(p, ChatColor.RED + "DEFEAT",
                            ChatColor.GRAY + "Better luck next time!", 0, 80, 10);
                }
            }
        }
        long delay = this.plugin.getConfig().getLong("end-delay-seconds", 8L) * 20L;
        Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {
            public void run() { finish(true); }
        }, delay);
    }

    public void forceEnd() {
        this.state = State.ENDING;
        finish(false);
    }

    private void finish(boolean async) {
        if (this.finished) return;
        this.finished = true;
        if (this.countdownTask != null) this.countdownTask.cancel();

        for (UUID id : this.byPlayer.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && this.world != null && p.getWorld().equals(this.world)) {
                reset(p);
                p.teleport(this.plugin.getLobby());
            }
        }
        for (UUID sid : new ArrayList<UUID>(this.spectators)) {
            Player sp = Bukkit.getPlayer(sid);
            if (sp != null) removeSpectator(sp, true);
        }
        this.spectators.clear();

        this.games.unregister(this);
        if (this.world != null) {
            Bukkit.unloadWorld(this.world, false);
            final Path dir = new File(Bukkit.getWorldContainer(), this.worldName).toPath();
            if (async) {
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                    public void run() { WorldFiles.delete(dir); }
                });
            } else {
                WorldFiles.delete(dir);
            }
        }
    }

    public void onQuit(Player p) {
        if (isSpectator(p)) { removeSpectator(p, false); return; }
        leave(p, p.getName() + " disconnected.");
    }

    public void onLeave(Player p) {
        if (isSpectator(p)) { removeSpectator(p, true); return; }
        leave(p, p.getName() + " left the match.");
    }

    private void leave(Player p, String message) {
        if (this.state == State.PREPARING || this.state == State.COUNTDOWN) {
            abort(p.getName() + " left before the match started.");
            return;
        }
        if (this.state == State.RUNNING) {
            this.games.detach(p);
            reset(p);
            p.teleport(this.plugin.getLobby());
            eliminate(p, message, false);
        } else if (this.state == State.ENDING
                && this.world != null
                && p.getWorld().equals(this.world)) {
            this.games.detach(p);
            reset(p);
            p.teleport(this.plugin.getLobby());
        }
    }

    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (isSpectator(p)) { e.setCancelled(true); return; }
        Block b = e.getBlock();
        if (!isActive(p)) { e.setCancelled(true); return; }
        if (!this.arena.inBuildArea(b.getX(), b.getY(), b.getZ())) {
            e.setCancelled(true);
            this.plugin.msg(p, ChatColor.RED + "You can't build there!");
            return;
        }
        e.setCancelled(false);
        e.setBuild(true);
        this.placed.add(Long.valueOf(key(b)));
    }

    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (isSpectator(p)) { e.setCancelled(true); return; }
        Block b = e.getBlock();
        if (!isActive(p)) { e.setCancelled(true); return; }
        if (b.getType() == Material.BED_BLOCK) {
            e.setCancelled(true);
            breakBed(p, b);
            return;
        }
        if (this.placed.remove(Long.valueOf(key(b)))
                || this.arena.inAnyDefense(b.getX(), b.getY(), b.getZ())) {
            e.setCancelled(false);
        } else {
            e.setCancelled(true);
            this.plugin.msg(p, ChatColor.RED
                    + "You can only break blocks placed by players or the bed defense!");
        }
    }

    public void onExplode(EntityExplodeEvent e) {
        e.setYield(0.0F);
        e.blockList().removeIf(b ->
                !this.placed.contains(Long.valueOf(key(b)))
                && !this.arena.inAnyDefense(b.getX(), b.getY(), b.getZ()));
        for (Block b : e.blockList()) {
            this.placed.remove(Long.valueOf(key(b)));
        }
    }

    public void onDamage(EntityDamageEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof Player)) return;
        Player p = (Player) entity;
        if (isSpectator(p)) { e.setCancelled(true); return; }
        if (this.state != State.RUNNING || !isActive(p)) { e.setCancelled(true); return; }
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true);
            return;
        }
        if (e instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent be = (EntityDamageByEntityEvent) e;
            Player attacker = attacker(be.getDamager());
            if (attacker != null) {
                if (!isActive(attacker)
                        || this.byPlayer.get(attacker.getUniqueId())
                            == this.byPlayer.get(p.getUniqueId())) {
                    e.setCancelled(true);
                    return;
                }
                this.lastDamage.put(p.getUniqueId(),
                        new Damage(attacker.getUniqueId(), System.currentTimeMillis()));
            }
        }
        if (e.getCause() == EntityDamageEvent.DamageCause.VOID
                || p.getHealth() - e.getFinalDamage() <= 0.0D) {
            e.setCancelled(true);
            handleDeath(p);
            return;
        }
        e.setCancelled(false);
    }

    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (isSpectator(p)) return;
        if (this.state == State.COUNTDOWN) {
            if (this.byPlayer.containsKey(p.getUniqueId())) {
                Location from = e.getFrom();
                Location to = e.getTo();
                if (from.getX() != to.getX() || from.getZ() != to.getZ()
                        || to.getY() > from.getY()) {
                    Location fixed = from.clone();
                    fixed.setYaw(to.getYaw());
                    fixed.setPitch(to.getPitch());
                    e.setTo(fixed);
                }
            }
            return;
        }
        if (this.state != State.RUNNING || e.getTo().getY() >= this.arena.getMinY()) return;
        if (isActive(p)) handleDeath(p);
    }

    private boolean isActive(Player p) {
        TeamState t = this.byPlayer.get(p.getUniqueId());
        return this.state == State.RUNNING && t != null
                && t.alive.contains(p.getUniqueId())
                && !this.respawning.contains(p.getUniqueId());
    }

    private void breakBed(Player p, Block b) {
        TeamState owner = bedOwner(b);
        if (owner == null) return;
        TeamState mine = this.byPlayer.get(p.getUniqueId());
        if (owner == mine) {
            this.plugin.msg(p, ChatColor.RED + "You can't break your own bed!");
            return;
        }
        if (!owner.bedAlive) return;
        owner.bedAlive = false;
        this.bedBreaks.merge(p.getUniqueId(), Integer.valueOf(1), Integer::sum);
        removeBed(b);
        broadcast(ChatColor.GRAY + "The bed of team "
                + Teams.display(owner.color)
                + " was destroyed by " + Teams.chat(mine.color)
                + p.getName() + "!");
        for (UUID id : owner.members) {
            Player victim = Bukkit.getPlayer(id);
            if (victim != null) {
                Titles.send(victim,
                        ChatColor.RED + "BED DESTROYED!",
                        ChatColor.GRAY + "You will no longer respawn!",
                        0, 50, 10);
                victim.playSound(victim.getLocation(), Sound.ENDERDRAGON_GROWL, 1.0F, 1.0F);
            }
        }
    }

    private TeamState bedOwner(Block b) {
        for (TeamState t : this.teams.values()) {
            int dx = Math.abs(b.getX() - t.bed.blockX());
            int dz = Math.abs(b.getZ() - t.bed.blockZ());
            if (b.getY() == t.bed.blockY() && dx + dz <= 1) return t;
        }
        return null;
    }

    private static void removeBed(Block b) {
        BlockFace[] faces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
        for (BlockFace f : faces) {
            Block n = b.getRelative(f);
            if (n.getType() == Material.BED_BLOCK) n.setType(Material.AIR);
        }
        b.setType(Material.AIR);
    }

    private void handleDeath(Player p) {
        if (!isActive(p)) return;
        TeamState t = this.byPlayer.get(p.getUniqueId());
        Damage d = this.lastDamage.remove(p.getUniqueId());
        Player killer = (d != null && System.currentTimeMillis() - d.time() < 10000L)
                ? Bukkit.getPlayer(d.by()) : null;
        if (killer != null) this.kills.merge(killer.getUniqueId(), Integer.valueOf(1), Integer::sum);

        String who = Teams.chat(t.color) + p.getName();
        String how = (killer != null) ? (" was killed by " + killer.getName()) : " died";
        reset(p);
        if (t.bedAlive) {
            broadcast(who + ".");
            respawn(p, t);
        } else {
            broadcast(who + how + ". ELIMINATED!");
            Titles.send(p, ChatColor.RED + "ELIMINATED",
                    ChatColor.GRAY + "You can't respawn.", 0, 50, 10);
            eliminate(p, null, true);
        }
    }

    private void respawn(Player p, final TeamState t) {
        final UUID id = p.getUniqueId();
        this.respawning.add(id);
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(this.arena.getSpawn().toLocation(this.world));
        final int total = Math.max(1, this.plugin.getConfig().getInt("respawn-delay-seconds", 5));
        new BukkitRunnable() {
            int left = total;
            public void run() {
                Player pl = Bukkit.getPlayer(id);
                if (Match.this.state != State.RUNNING
                        || !Match.this.respawning.contains(id)
                        || pl == null || !t.alive.contains(id)) {
                    Match.this.respawning.remove(id);
                    cancel();
                    return;
                }
                if (this.left <= 0) {
                    cancel();
                    Match.this.respawning.remove(id);
                    Match.this.equip(pl, t);
                    pl.teleport(t.spawn.toLocation(Match.this.world));
                    Titles.send(pl, ChatColor.GREEN + "RESPAWNED", "", 0, 20, 10);
                    return;
                }
                Titles.send(pl,
                        ChatColor.RED + "YOU DIED!",
                        ChatColor.YELLOW + "Respawning in " + this.left + "s",
                        0, 25, 5);
                this.left--;
            }
        }.runTaskTimer(this.plugin, 0L, 20L);
    }

    private void eliminate(Player p, String message, boolean spectate) {
        TeamState t = this.byPlayer.get(p.getUniqueId());
        if (t == null || !t.alive.remove(p.getUniqueId())) return;
        this.respawning.remove(p.getUniqueId());
        if (message != null) broadcast(ChatColor.GRAY + message);
        if (spectate && p.isOnline() && this.world != null) {
            p.setGameMode(GameMode.SPECTATOR);
            p.teleport(this.arena.getSpawn().toLocation(this.world));
        }
        checkWin();
    }

    private void checkWin() {
        if (this.state != State.RUNNING) return;
        List<TeamState> left = new ArrayList<TeamState>();
        for (TeamState t : this.teams.values()) if (!t.alive.isEmpty()) left.add(t);
        if (left.size() <= 1) end(left.isEmpty() ? null : left.get(0));
    }

    private static Player attacker(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player) return (Player) src;
        }
        return null;
    }

    private void broadcast(String message) {
        for (UUID id : this.byPlayer.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(BedFightPlugin.PREFIX + message);
        }
        for (UUID sid : this.spectators) {
            Player sp = Bukkit.getPlayer(sid);
            if (sp != null) sp.sendMessage(BedFightPlugin.PREFIX + message);
        }
    }

    private static long key(Block b) {
        return ((b.getX() & 0x3FFFFFFL) << 38L)
             | ((b.getZ() & 0x3FFFFFFL) << 12L)
             | (b.getY() & 0xFFFL);
    }

    private static void reset(Player p) {
        p.setGameMode(GameMode.SURVIVAL);
        p.setAllowFlight(false);
        p.setFlying(false);
        p.getInventory().clear();
        p.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20.0F);
        p.setFireTicks(0);
        p.setFallDistance(0.0F);
        for (PotionEffect pe : new ArrayList<PotionEffect>(p.getActivePotionEffects())) {
            p.removePotionEffect(pe.getType());
        }
    }

    private void equip(Player p, TeamState t) {
        reset(p);
        this.plugin.getKitManager().apply(p, t.color);
        p.updateInventory();
    }

    public List<TeamView> teamViews(Player viewer) {
        TeamState mine = this.byPlayer.get(viewer.getUniqueId());
        List<TeamView> out = new ArrayList<TeamView>();
        for (TeamState t : this.teams.values()) {
            out.add(new TeamView(t.color, t.bedAlive, t.alive.size(), t == mine));
        }
        return out;
    }

    public Map<String, String> placeholders(Player viewer) {
        Map<String, String> m = new HashMap<String, String>();
        TeamState mine = this.byPlayer.get(viewer.getUniqueId());
        List<String> enemyNames = new ArrayList<String>();
        int pingSum = 0, pingCount = 0;
        for (TeamState t : this.teams.values()) {
            if (t == mine) continue;
            for (UUID id : t.members) {
                Player pl = Bukkit.getPlayer(id);
                enemyNames.add(pl != null ? pl.getName() : "?");
                if (pl != null) { pingSum += Ping.get(pl); pingCount++; }
            }
        }
        long elapsed = (this.state == State.RUNNING || this.state == State.ENDING)
                ? Math.max(0L, (System.currentTimeMillis() - this.startMillis) / 1000L)
                : 0L;
        String status;
        switch (this.state) {
            case PREPARING: status = "Preparing"; break;
            case COUNTDOWN: status = "Starting in " + this.countdownLeft + "s"; break;
            case RUNNING:   status = "Fighting"; break;
            default:        status = "Ending"; break;
        }
        m.put("%player%", viewer.getName());
        m.put("%mode%", this.mode.label());
        m.put("%map%", this.arena.getName());
        m.put("%team%", mine != null ? Teams.display(mine.color) : "-");
        m.put("%opponent%", String.join(", ", enemyNames));
        m.put("%ping%", String.valueOf(Ping.get(viewer)));
        m.put("%opponent_ping%", String.valueOf(pingCount == 0 ? 0 : pingSum / pingCount));
        m.put("%status%", status);
        m.put("%countdown%", String.valueOf(this.countdownLeft));
        m.put("%time%", String.format("%d:%02d", elapsed / 60L, elapsed % 60L));
        m.put("%kills%", String.valueOf(
                this.kills.getOrDefault(viewer.getUniqueId(), Integer.valueOf(0))));
        m.put("%bed_breaks%", String.valueOf(
                this.bedBreaks.getOrDefault(viewer.getUniqueId(), Integer.valueOf(0))));
        return m;
    }
}
'''

# ==================================================== small patches

def patch_bedfight_plugin(text: str) -> str:
    text = ensure_import(text, 'import org.bukkit.Location;')
    text = ensure_import(text, 'import org.bukkit.plugin.java.JavaPlugin;')

    # Fix the decompiler's broken msg() — it duplicated PREFIX and ignored
    # the actual message argument.
    old_variants = [
        'public void msg(CommandSender to, String message) {\n'
        '    to.sendMessage(PREFIX + PREFIX);\n'
        '  }',
        'public void msg(CommandSender to, String message) {\n'
        '    to.sendMessage(PREFIX);\n'
        '  }',
        'public void msg(CommandSender to, String message) {\n'
        '    to.sendMessage(PREFIX + message);\n'
        '  }',
    ]
    new_body = ('public void msg(CommandSender to, String message) {\n'
                '    to.sendMessage(PREFIX + message);\n'
                '  }')
    for old in old_variants:
        if old in text:
            if old == new_body:
                return text
            return text.replace(old, new_body, 1)

    # Fallback: regex the method body regardless of whitespace.
    pat = re.compile(
        r'public\s+void\s+msg\s*\(\s*CommandSender\s+to\s*,\s*String\s+message\s*\)\s*\{'
        r'.*?'
        r'\}',
        re.DOTALL,
    )
    def _sub(m):
        return ('public void msg(CommandSender to, String message) {\n'
                '    to.sendMessage(PREFIX + message);\n'
                '  }')
    new_text, n = pat.subn(_sub, text, count=1)
    if n == 0:
        # No msg() at all — append one just before the final closing brace.
        last = text.rstrip()
        if last.endswith('}'):
            return last[:-1] + ('  public void msg(CommandSender to, String message) {\n'
                                '    to.sendMessage(PREFIX + message);\n'
                                '  }\n}') + '\n'
        return text
    return new_text


def patch_game_manager(text: str) -> str:
    if 'private final Map<UUID, Match> bySpectator' not in text:
        text = replace_must(
            text,
            'private final Map<String, Match> byWorld = new HashMap<>();',
            'private final Map<String, Match> byWorld = new HashMap<>();\n'
            '  private final Map<UUID, Match> bySpectator = new HashMap<>();',
            'GameManager.bySpectator field',
        )

    if 'public Match getSpectating(Player p)' not in text:
        text = insert_before(
            text,
            'public Match getMatch(Player p) {',
            'public Match getSpectating(Player p) {\n'
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
            'GameManager.spectator API',
        )

    if 's.addAll(this.bySpectator.keySet());' not in text:
        text = replace_must(
            text,
            's.addAll(this.queued.keySet());\n    return s;',
            's.addAll(this.queued.keySet());\n    s.addAll(this.bySpectator.keySet());\n    return s;',
            'GameManager.activePlayers',
        )

    if 'this.bySpectator.containsKey(p.getUniqueId())' not in text:
        text = replace_must(
            text,
            'return (this.byPlayer.containsKey(p.getUniqueId()) || this.queued.containsKey(p.getUniqueId()));',
            'return (this.byPlayer.containsKey(p.getUniqueId())\n'
            '        || this.queued.containsKey(p.getUniqueId())\n'
            '        || this.bySpectator.containsKey(p.getUniqueId()));',
            'GameManager.isBusy',
        )

    if 'Match spec = this.bySpectator.remove' not in text:
        text = replace_must(
            text,
            'Mode m = this.queued.remove(p.getUniqueId());\n'
            '    this.queuedAt.remove(p.getUniqueId());\n'
            '    if (m == null) {\n'
            '      return false;\n'
            '    }\n'
            '    ((LinkedHashSet)this.queues.get(m)).remove(p.getUniqueId());\n'
            '    return true;',
            'Mode m = this.queued.remove(p.getUniqueId());\n'
            '    this.queuedAt.remove(p.getUniqueId());\n'
            '    Match spec = this.bySpectator.remove(p.getUniqueId());\n'
            '    if (spec != null) {\n'
            '      spec.removeSpectator(p, true);\n'
            '    }\n'
            '    if (m == null) {\n'
            '      return spec != null;\n'
            '    }\n'
            '    ((LinkedHashSet)this.queues.get(m)).remove(p.getUniqueId());\n'
            '    return true;',
            'GameManager.leaveQueue',
        )

    if 'this.bySpectator.values().removeIf' not in text:
        text = replace_must(
            text,
            'for (UUID id : m.getPlayerIds()) {\n      this.byPlayer.remove(id, m);\n    }',
            'for (UUID id : m.getPlayerIds()) {\n'
            '      this.byPlayer.remove(id, m);\n'
            '    }\n'
            '    this.bySpectator.values().removeIf(mm -> mm == m);',
            'GameManager.unregister',
        )

    return text


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


def patch_command(text: str) -> str:
    if 'if (sub.equals("spec")) {' not in text:
        text = insert_before(
            text,
            'if (!ADMIN_SUBS.contains(sub)) {',
            SPEC_HANDLER,
            'BedFightCommand.spec handler',
        )

    if 'out.addAll(List.of("1v1", "2v2", "3v3", "spec", "leave"));' not in text:
        text = replace_must(
            text,
            'out.addAll(List.of("1v1", "2v2", "3v3", "leave"));',
            'out.addAll(List.of("1v1", "2v2", "3v3", "spec", "leave"));',
            'BedFightCommand.tab top',
        )

    if 'args[0].equalsIgnoreCase("spec")' not in text:
        text = insert_before(
            text,
            '} else if (args.length == 2 && p.hasPermission("bedfight.admin")) {',
            '} else if (args.length == 2 && args[0].equalsIgnoreCase("spec")) {\n'
            '      for (Player online : Bukkit.getOnlinePlayers()) {\n'
            '        if (this.plugin.getGameManager().getMatch(online) != null) {\n'
            '          out.add(online.getName());\n'
            '        }\n'
            '      }\n'
            '    ',
            'BedFightCommand.spec tab',
        )

    return text


ARENA_NEW = '''  @EventHandler(priority = EventPriority.HIGHEST)
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


def patch_arena_listener(text: str) -> str:
    text = ensure_import(text, 'import org.bukkit.Location;')
    text = ensure_import(text, 'import org.bukkit.block.Block;')
    text = ensure_import(text, 'import org.bukkit.entity.Player;')
    text = ensure_import(text, 'import org.bukkit.event.inventory.InventoryOpenEvent;')
    text = ensure_import(text, 'import org.bukkit.event.player.PlayerInteractEvent;')

    if 'public void onSpecMove(PlayerMoveEvent e)' not in text:
        text = insert_before(
            text,
            '  @EventHandler\n  public void onQuit(PlayerQuitEvent e) {',
            ARENA_NEW,
            'ArenaListener spectator handlers',
        )
    return text


SB_METHODS = '''  public void refresh(Player p) {
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


def patch_scoreboard(text: str) -> str:
    if 'Match sm = games.getSpectating(p);' not in text:
        text = insert_after(
            text,
            'Match m = games.getMatch(p);\n',
            '      Match sm = games.getSpectating(p);\n',
            'ScoreboardManager.getSpectating',
        )
    if 'lines = spectatorLines(p, sm);' not in text:
        text = replace_must(
            text,
            '} else if (games.getQueuedMode(p) != null) {',
            '} else if (sm != null) {\n'
            '        title = this.cfg.getString("spectator.title", this.cfg.getString("match.title", "&b&lBedFight"));\n'
            '        lines = spectatorLines(p, sm);\n'
            '      } else if (games.getQueuedMode(p) != null) {',
            'ScoreboardManager.spectator branch',
        )
    if 'public void refresh(Player p)' not in text:
        text = insert_before(
            text,
            'private List<String> matchLines(Player p, Match m) {',
            SB_METHODS,
            'ScoreboardManager.spectator methods',
        )
    return text


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
        return
    text = read(yml)
    if 'spectator:' in text:
        return
    backup(yml)
    write(yml, text.rstrip() + '\n\n' + SPECTATOR_YAML + '\n')
    print('[ok]   scoreboard.yml')


# =========================================================== main

def main() -> int:
    root = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path.cwd()
    if not (root / 'com').is_dir():
        print(f'!! {root} does not contain "com". Aborting.')
        return 1
    print(f'Patching in: {root}\n')

    # 1) Rewrite completely broken files (decompiler output is not
    #    syntactically valid Java for these).
    for rel, content in [
        ('com/example/bedfight/arena/Pos.java', POS_JAVA),
        ('com/example/bedfight/game/Match.java', MATCH_JAVA),
    ]:
        p = root / rel
        if not p.exists():
            print(f'[skip] {rel} (not found)')
            continue
        backup(p)
        write(p, content)
        print(f'[rewrite] {rel}')

    # 2) Patch the rest in place.
    patches = [
        ('com/example/bedfight/BedFightPlugin.java', patch_bedfight_plugin),
        ('com/example/bedfight/game/GameManager.java', patch_game_manager),
        ('com/example/bedfight/command/BedFightCommand.java', patch_command),
        ('com/example/bedfight/listener/ArenaListener.java', patch_arena_listener),
        ('com/example/bedfight/scoreboard/ScoreboardManager.java', patch_scoreboard),
    ]
    for rel, fn in patches:
        p = root / rel
        if not p.exists():
            print(f'[skip] {rel} (not found)')
            continue
        try:
            original = read(p)
            patched = fn(original)
        except RuntimeError as ex:
            print(f'[ERR ] {rel}:')
            print(str(ex))
            continue
        except Exception as ex:
            print(f'[ERR ] {rel}: {type(ex).__name__}: {ex}')
            continue

        # If nothing changed AND we didn't rewrite the file, say so.
        if patched == original:
            # still need to persist the stripped version if the on-disk
            # file had JD-Core prefixes.
            on_disk_raw = open(p, 'r', encoding='utf-8').read()
            if on_disk_raw != patched:
                backup(p)
                write(p, patched)
                print(f'[ok]   {rel} (stripped JD-Core comments)')
            else:
                print(f'[skip] {rel} (already patched)')
            continue

        backup(p)
        write(p, patched)
        print(f'[ok]   {rel}')

    patch_scoreboard_yml(root)
    print('\nDone.')
    return 0


if __name__ == '__main__':
    sys.exit(main())