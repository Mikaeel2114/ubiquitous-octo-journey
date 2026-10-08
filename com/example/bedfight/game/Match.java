/*     */ package com.example.bedfight.game;
/*     */ 
/*     */ import com.example.bedfight.BedFightPlugin;
/*     */ import com.example.bedfight.arena.Arena;
/*     */ import com.example.bedfight.arena.Pos;
/*     */ import com.example.bedfight.arena.TeamSpec;
/*     */ import com.example.bedfight.arena.Teams;
/*     */ import com.example.bedfight.util.Ping;
/*     */ import com.example.bedfight.util.Titles;
/*     */ import com.example.bedfight.util.WorldFiles;
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.nio.file.Path;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collections;
/*     */ import java.util.HashMap;
/*     */ import java.util.HashSet;
/*     */ import java.util.LinkedHashMap;
/*     */ import java.util.LinkedHashSet;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Set;
/*     */ import java.util.UUID;
/*     */ import java.util.concurrent.atomic.AtomicInteger;
/*     */ import java.util.logging.Level;
/*     */ import org.bukkit.Bukkit;
/*     */ import org.bukkit.ChatColor;
/*     */ import org.bukkit.Difficulty;
/*     */ import org.bukkit.DyeColor;
/*     */ import org.bukkit.GameMode;
/*     */ import org.bukkit.Location;
/*     */ import org.bukkit.Material;
/*     */ import org.bukkit.Sound;
/*     */ import org.bukkit.World;
/*     */ import org.bukkit.WorldCreator;
/*     */ import org.bukkit.block.Block;
/*     */ import org.bukkit.block.BlockFace;
/*     */ import org.bukkit.command.CommandSender;
/*     */ import org.bukkit.entity.Entity;
/*     */ import org.bukkit.entity.Player;
/*     */ import org.bukkit.entity.Projectile;
/*     */ import org.bukkit.event.block.BlockBreakEvent;
/*     */ import org.bukkit.event.block.BlockPlaceEvent;
/*     */ import org.bukkit.event.entity.EntityDamageByEntityEvent;
/*     */ import org.bukkit.event.entity.EntityDamageEvent;
/*     */ import org.bukkit.event.entity.EntityExplodeEvent;
/*     */ import org.bukkit.event.player.PlayerMoveEvent;
/*     */ import org.bukkit.plugin.Plugin;
/*     */ import org.bukkit.potion.PotionEffect;
/*     */ import org.bukkit.projectiles.ProjectileSource;
/*     */ import org.bukkit.scheduler.BukkitRunnable;
/*     */ import org.bukkit.scheduler.BukkitTask;
/*     */ 
/*     */ public final class Match
/*     */ {
/*     */   public enum State
/*     */   {
/*  58 */     PREPARING, COUNTDOWN, RUNNING, ENDING;
/*     */   }
/*  60 */   private static final AtomicInteger IDS = new AtomicInteger(); private final BedFightPlugin plugin; private final GameManager games; private final Arena arena; private final Mode mode;
/*     */   private final String worldName;
/*     */   
/*     */   private static final class TeamState { final DyeColor color;
/*     */     final Pos spawn;
/*     */     final Pos bed;
/*  66 */     final Set<UUID> members = new LinkedHashSet<>();
/*  67 */     final Set<UUID> alive = new HashSet<>();
/*     */     boolean bedAlive = true;
/*     */     
/*     */     TeamState(DyeColor color, Pos spawn, Pos bed) {
/*  71 */       this.color = color;
/*  72 */       this.spawn = spawn;
/*  73 */       this.bed = bed;
/*     */     } }
/*     */   
/*     */   private static final class Damage extends Record { private final UUID by; private final long time;
/*  77 */     private Damage(UUID by, long time) { this.by = by; this.time = time; } public final String toString() { // Byte code:
/*     */       //   0: aload_0
/*     */       //   1: <illegal opcode> toString : (Lcom/example/bedfight/game/Match$Damage;)Ljava/lang/String;
/*     */       //   6: areturn
/*     */       // Line number table:
/*     */       //   Java source line number -> byte code offset
/*     */       //   #77	-> 0
/*     */       // Local variable table:
/*     */       //   start	length	slot	name	descriptor
/*     */       //   0	7	0	this	Lcom/example/bedfight/game/Match$Damage; } public final int hashCode() { // Byte code:
/*     */       //   0: aload_0
/*     */       //   1: <illegal opcode> hashCode : (Lcom/example/bedfight/game/Match$Damage;)I
/*     */       //   6: ireturn
/*     */       // Line number table:
/*     */       //   Java source line number -> byte code offset
/*     */       //   #77	-> 0
/*     */       // Local variable table:
/*     */       //   start	length	slot	name	descriptor
/*     */       //   0	7	0	this	Lcom/example/bedfight/game/Match$Damage; } public final boolean equals(Object o) { // Byte code:
/*     */       //   0: aload_0
/*     */       //   1: aload_1
/*     */       //   2: <illegal opcode> equals : (Lcom/example/bedfight/game/Match$Damage;Ljava/lang/Object;)Z
/*     */       //   7: ireturn
/*     */       // Line number table:
/*     */       //   Java source line number -> byte code offset
/*     */       //   #77	-> 0
/*     */       // Local variable table:
/*     */       //   start	length	slot	name	descriptor
/*     */       //   0	8	0	this	Lcom/example/bedfight/game/Match$Damage;
/*  77 */       //   0	8	1	o	Ljava/lang/Object; } public UUID by() { return this.by; } public long time() { return this.time; }
/*     */      }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*  84 */   private final Map<DyeColor, TeamState> teams = new LinkedHashMap<>();
/*  85 */   private final Map<UUID, TeamState> byPlayer = new HashMap<>();
/*  86 */   private final Set<UUID> respawning = new HashSet<>();
/*  87 */   private final Map<UUID, Damage> lastDamage = new HashMap<>();
/*  88 */   private final Set<Long> placed = new HashSet<>();
/*     */   
/*  90 */   private State state = State.PREPARING;
/*     */   private boolean finished;
/*     */   private World world;
/*     */   private BukkitTask countdownTask;
/*     */   private int countdownLeft;
/*     */   private long startMillis;
/*  96 */   private final Map<UUID, Integer> kills = new HashMap<>();
/*  97 */   private final Map<UUID, Integer> bedBreaks = new HashMap<>();
/*     */   
/*     */   public Match(BedFightPlugin plugin, GameManager games, Arena arena, Mode mode, List<List<Player>> split) {
/* 100 */     this.plugin = plugin;
/* 101 */     this.games = games;
/* 102 */     this.arena = arena;
/* 103 */     this.mode = mode;
/* 104 */     this.worldName = "bf_" + arena.getWorldName() + "_" + IDS.incrementAndGet();
/*     */     
/* 106 */     List<DyeColor> colors = new ArrayList<>(arena.getTeams().keySet());
/* 107 */     Collections.shuffle(colors);
/* 108 */     for (int i = 0; i < split.size(); i++) {
/* 109 */       TeamSpec spec = (TeamSpec)arena.getTeams().get(colors.get(i));
/* 110 */       TeamState ts = new TeamState(spec.getColor(), spec.getSpawn(), spec.getBed());
/* 111 */       for (Player p : split.get(i)) {
/* 112 */         ts.members.add(p.getUniqueId());
/* 113 */         ts.alive.add(p.getUniqueId());
/* 114 */         this.byPlayer.put(p.getUniqueId(), ts);
/*     */       } 
/* 116 */       this.teams.put(ts.color, ts);
/*     */     } 
/*     */   }
/*     */   public static final class TeamView extends Record {
/*     */     private final DyeColor color; private final boolean bedAlive; private final int alive; private final boolean mine;
/* 121 */     public TeamView(DyeColor color, boolean bedAlive, int alive, boolean mine) { this.color = color; this.bedAlive = bedAlive; this.alive = alive; this.mine = mine; } public final String toString() { // Byte code:
/*     */       //   0: aload_0
/*     */       //   1: <illegal opcode> toString : (Lcom/example/bedfight/game/Match$TeamView;)Ljava/lang/String;
/*     */       //   6: areturn
/*     */       // Line number table:
/*     */       //   Java source line number -> byte code offset
/*     */       //   #121	-> 0
/*     */       // Local variable table:
/*     */       //   start	length	slot	name	descriptor
/*     */       //   0	7	0	this	Lcom/example/bedfight/game/Match$TeamView; } public final int hashCode() { // Byte code:
/*     */       //   0: aload_0
/*     */       //   1: <illegal opcode> hashCode : (Lcom/example/bedfight/game/Match$TeamView;)I
/*     */       //   6: ireturn
/*     */       // Line number table:
/*     */       //   Java source line number -> byte code offset
/*     */       //   #121	-> 0
/*     */       // Local variable table:
/*     */       //   start	length	slot	name	descriptor
/*     */       //   0	7	0	this	Lcom/example/bedfight/game/Match$TeamView; } public final boolean equals(Object o) { // Byte code:
/*     */       //   0: aload_0
/*     */       //   1: aload_1
/*     */       //   2: <illegal opcode> equals : (Lcom/example/bedfight/game/Match$TeamView;Ljava/lang/Object;)Z
/*     */       //   7: ireturn
/*     */       // Line number table:
/*     */       //   Java source line number -> byte code offset
/*     */       //   #121	-> 0
/*     */       // Local variable table:
/*     */       //   start	length	slot	name	descriptor
/*     */       //   0	8	0	this	Lcom/example/bedfight/game/Match$TeamView;
/* 121 */       //   0	8	1	o	Ljava/lang/Object; } public DyeColor color() { return this.color; } public boolean bedAlive() { return this.bedAlive; } public int alive() { return this.alive; } public boolean mine() { return this.mine; }
/*     */      } public State getState() {
/* 123 */     return this.state;
/*     */   }
/* 125 */   public String getWorldName() { return this.worldName; }
/* 126 */   public World getWorld() { return this.world; } public Set<UUID> getPlayerIds() {
/* 127 */     return this.byPlayer.keySet();
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public void prepare() {
/* 133 */     broadcast(String.valueOf(ChatColor.GRAY) + "Preparing your " + String.valueOf(ChatColor.GRAY) + " arena...");
/* 134 */     File src = new File(Bukkit.getWorldContainer(), this.arena.getWorldName());
/* 135 */     File dst = new File(Bukkit.getWorldContainer(), this.worldName);
/*     */     
/* 137 */     World template = Bukkit.getWorld(this.arena.getWorldName());
/* 138 */     if (template != null) {
/* 139 */       template.save();
/*     */     }
/* 141 */     Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
/*     */           try {
/*     */             WorldFiles.copy(src.toPath(), dst.toPath());
/*     */             Bukkit.getScheduler().runTask((Plugin)this.plugin, this::begin);
/* 145 */           } catch (IOException ex) {
/*     */             this.plugin.getLogger().log(Level.SEVERE, "Could not copy arena world " + String.valueOf(src), ex);
/*     */             Bukkit.getScheduler().runTask((Plugin)this.plugin, ());
/*     */           } 
/*     */         });
/*     */   }
/*     */   
/*     */   private void begin() {
/* 153 */     Path dst = (new File(Bukkit.getWorldContainer(), this.worldName)).toPath();
/* 154 */     if (this.state != State.PREPARING) {
/* 155 */       Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> WorldFiles.delete(dst));
/*     */       return;
/*     */     } 
/* 158 */     for (UUID id : this.byPlayer.keySet()) {
/* 159 */       if (Bukkit.getPlayer(id) == null) {
/* 160 */         abort("A player left before the match started.");
/*     */         return;
/*     */       } 
/*     */     } 
/* 164 */     this.world = Bukkit.createWorld(new WorldCreator(this.worldName));
/* 165 */     if (this.world == null) {
/* 166 */       abort("Could not load the arena.");
/*     */       return;
/*     */     } 
/* 169 */     this.world.setAutoSave(false);
/* 170 */     this.world.setKeepSpawnInMemory(false);
/* 171 */     this.world.setPVP(true);
/* 172 */     this.world.setDifficulty(Difficulty.NORMAL);
/* 173 */     this.world.setStorm(false);
/* 174 */     this.world.setTime(6000L);
/* 175 */     this.world.setGameRuleValue("doDaylightCycle", "false");
/* 176 */     this.world.setGameRuleValue("doMobSpawning", "false");
/*     */ 
/*     */     
/* 179 */     Pos sp = this.arena.getSpawn();
/* 180 */     this.world.setSpawnLocation(sp.blockX() + 100000, 64, sp.blockZ() + 100000);
/*     */ 
/*     */     
/* 183 */     for (TeamSpec spec : this.arena.getTeams().values()) {
/* 184 */       if (!this.teams.containsKey(spec.getColor())) {
/* 185 */         removeBed(this.world.getBlockAt(spec.getBed().blockX(), spec.getBed().blockY(), spec.getBed().blockZ()));
/*     */       }
/*     */     } 
/*     */ 
/*     */     
/* 190 */     this.state = State.COUNTDOWN;
/* 191 */     for (TeamState t : this.teams.values()) {
/* 192 */       for (UUID id : t.members) {
/* 193 */         Player p = Bukkit.getPlayer(id);
/* 194 */         if (p != null) {
/* 195 */           equip(p, t);
/* 196 */           p.teleport(t.spawn.toLocation(this.world));
/* 197 */           p.sendMessage(BedFightPlugin.PREFIX + BedFightPlugin.PREFIX + "You are on team " + String.valueOf(ChatColor.GRAY) + Teams.display(t.color) + ". Protect your bed, destroy the enemy bed!");
/*     */         } 
/*     */       } 
/*     */     } 
/*     */     
/* 202 */     final int seconds = Math.max(1, this.plugin.getConfig().getInt("countdown-seconds", 5));
/* 203 */     this
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */       
/* 228 */       .countdownTask = (new BukkitRunnable() { int left = seconds; public void run() { if (Match.this.state != Match.State.COUNTDOWN) { cancel(); return; }  if (this.left <= 0) { cancel(); Match.this.startGame(); return; }  Match.this.countdownLeft = this.left; ChatColor c = (this.left > 3) ? ChatColor.GREEN : ((this.left == 3) ? ChatColor.YELLOW : ChatColor.RED); for (UUID id : Match.this.byPlayer.keySet()) { Player p = Bukkit.getPlayer(id); if (p != null) { Titles.send(p, String.valueOf(c) + String.valueOf(c) + String.valueOf(ChatColor.BOLD), String.valueOf(ChatColor.GRAY) + "Get ready to fight!", 0, 25, 5); p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1.0F, 1.0F); }  }  this.left--; } }).runTaskTimer((Plugin)this.plugin, 0L, 20L);
/*     */   }
/*     */   
/*     */   private void startGame() {
/* 232 */     if (this.state != State.COUNTDOWN) {
/*     */       return;
/*     */     }
/* 235 */     this.state = State.RUNNING;
/* 236 */     this.startMillis = System.currentTimeMillis();
/* 237 */     for (TeamState t : this.teams.values()) {
/* 238 */       for (UUID id : t.members) {
/* 239 */         Player p = Bukkit.getPlayer(id);
/* 240 */         if (p != null) {
/* 241 */           Titles.send(p, String.valueOf(ChatColor.GREEN) + String.valueOf(ChatColor.GREEN) + "FIGHT!", String.valueOf(ChatColor.GRAY) + "Team " + String.valueOf(ChatColor.GRAY) + 
/* 242 */               Teams.display(t.color) + " - destroy the enemy bed!", 0, 30, 10);
/*     */           
/* 244 */           p.playSound(p.getLocation(), Sound.NOTE_PLING, 1.0F, 2.0F);
/*     */         } 
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   private void abort(String reason) {
/* 251 */     if (this.state == State.ENDING) {
/*     */       return;
/*     */     }
/* 254 */     this.state = State.ENDING;
/* 255 */     broadcast(String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.RED));
/* 256 */     finish(true);
/*     */   }
/*     */   
/*     */   private void end(TeamState winner) {
/* 260 */     if (this.state == State.ENDING) {
/*     */       return;
/*     */     }
/* 263 */     this.state = State.ENDING;
/* 264 */     if (winner == null) {
/* 265 */       broadcast(String.valueOf(ChatColor.YELLOW) + "The match ended in a draw.");
/*     */     } else {
/* 267 */       broadcast(Teams.display(winner.color) + Teams.display(winner.color) + " team wins the match!");
/*     */     } 
/* 269 */     for (TeamState t : this.teams.values()) {
/* 270 */       boolean won = (t == winner);
/* 271 */       for (UUID id : t.members) {
/* 272 */         Player p = Bukkit.getPlayer(id);
/* 273 */         if (p == null) {
/*     */           continue;
/*     */         }
/* 276 */         if (winner == null) {
/* 277 */           Titles.send(p, String.valueOf(ChatColor.YELLOW) + String.valueOf(ChatColor.YELLOW) + "DRAW", "", 0, 60, 10); continue;
/* 278 */         }  if (won) {
/* 279 */           Titles.send(p, String.valueOf(ChatColor.GOLD) + String.valueOf(ChatColor.GOLD) + "VICTORY!", String.valueOf(ChatColor.GRAY) + "You won the BedFight!", 0, 80, 10);
/*     */           
/* 281 */           p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F); continue;
/*     */         } 
/* 283 */         Titles.send(p, String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.RED) + "DEFEAT", String.valueOf(ChatColor.GRAY) + "Better luck next time!", 0, 80, 10);
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/* 288 */     long delay = this.plugin.getConfig().getLong("end-delay-seconds", 8L) * 20L;
/* 289 */     Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> finish(true), delay);
/*     */   }
/*     */ 
/*     */   
/*     */   public void forceEnd() {
/* 294 */     this.state = State.ENDING;
/* 295 */     finish(false);
/*     */   }
/*     */   
/*     */   private void finish(boolean async) {
/* 299 */     if (this.finished) {
/*     */       return;
/*     */     }
/* 302 */     this.finished = true;
/* 303 */     if (this.countdownTask != null) {
/* 304 */       this.countdownTask.cancel();
/*     */     }
/* 306 */     for (UUID id : this.byPlayer.keySet()) {
/* 307 */       Player p = Bukkit.getPlayer(id);
/* 308 */       if (p != null && this.world != null && p.getWorld().equals(this.world)) {
/* 309 */         reset(p);
/* 310 */         p.teleport(this.plugin.getLobby());
/*     */       } 
/*     */     } 
/* 313 */     this.games.unregister(this);
/* 314 */     if (this.world != null) {
/* 315 */       Bukkit.unloadWorld(this.world, false);
/* 316 */       Path dir = (new File(Bukkit.getWorldContainer(), this.worldName)).toPath();
/* 317 */       if (async) {
/* 318 */         Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> WorldFiles.delete(dir));
/*     */       } else {
/* 320 */         WorldFiles.delete(dir);
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public void onQuit(Player p) {
/* 329 */     leave(p, p.getName() + " disconnected.");
/*     */   }
/*     */ 
/*     */   
/*     */   public void onLeave(Player p) {
/* 334 */     leave(p, p.getName() + " left the match.");
/*     */   }
/*     */   
/*     */   private void leave(Player p, String message) {
/* 338 */     if (this.state == State.PREPARING || this.state == State.COUNTDOWN) {
/* 339 */       abort(p.getName() + " left before the match started.");
/*     */       return;
/*     */     } 
/* 342 */     if (this.state == State.RUNNING) {
/* 343 */       this.games.detach(p);
/* 344 */       reset(p);
/* 345 */       p.teleport(this.plugin.getLobby());
/* 346 */       eliminate(p, message, false);
/* 347 */     } else if (this.state == State.ENDING && this.world != null && p.getWorld().equals(this.world)) {
/* 348 */       this.games.detach(p);
/* 349 */       reset(p);
/* 350 */       p.teleport(this.plugin.getLobby());
/*     */     } 
/*     */   }
/*     */   
/*     */   public void onPlace(BlockPlaceEvent e) {
/* 355 */     Player p = e.getPlayer();
/* 356 */     Block b = e.getBlock();
/* 357 */     if (!isActive(p)) {
/* 358 */       e.setCancelled(true);
/*     */       return;
/*     */     } 
/* 361 */     if (!this.arena.inBuildArea(b.getX(), b.getY(), b.getZ())) {
/* 362 */       e.setCancelled(true);
/* 363 */       this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "You can't build there! (Y limit: " + String.valueOf(ChatColor.RED) + " - " + this.arena.getMinY() + ")");
/*     */       
/*     */       return;
/*     */     } 
/* 367 */     e.setCancelled(false);
/* 368 */     e.setBuild(true);
/* 369 */     this.placed.add(Long.valueOf(key(b)));
/*     */   }
/*     */   
/*     */   public void onBreak(BlockBreakEvent e) {
/* 373 */     Player p = e.getPlayer();
/* 374 */     Block b = e.getBlock();
/* 375 */     if (!isActive(p)) {
/* 376 */       e.setCancelled(true);
/*     */       return;
/*     */     } 
/* 379 */     if (b.getType() == Material.BED_BLOCK) {
/* 380 */       e.setCancelled(true);
/* 381 */       breakBed(p, b);
/*     */       
/*     */       return;
/*     */     } 
/* 385 */     if (this.placed.remove(Long.valueOf(key(b))) || this.arena.inAnyDefense(b.getX(), b.getY(), b.getZ())) {
/* 386 */       e.setCancelled(false);
/*     */     } else {
/* 388 */       e.setCancelled(true);
/* 389 */       this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "You can only break blocks placed by players or the bed defense!");
/*     */     } 
/*     */   }
/*     */   
/*     */   public void onExplode(EntityExplodeEvent e) {
/* 394 */     e.setYield(0.0F);
/* 395 */     e.blockList().removeIf(b -> (!this.placed.contains(Long.valueOf(key(b))) && !this.arena.inAnyDefense(b.getX(), b.getY(), b.getZ())));
/* 396 */     for (Block b : e.blockList())
/* 397 */       this.placed.remove(Long.valueOf(key(b))); 
/*     */   }
/*     */   
/*     */   public void onDamage(EntityDamageEvent e) {
/*     */     Player p;
/* 402 */     Entity entity = e.getEntity(); if (entity instanceof Player) { p = (Player)entity; }
/*     */     else
/*     */     { return; }
/* 405 */      if (this.state != State.RUNNING || !isActive(p)) {
/* 406 */       e.setCancelled(true);
/*     */       return;
/*     */     } 
/* 409 */     if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
/* 410 */       e.setCancelled(true);
/*     */       return;
/*     */     } 
/* 413 */     if (e instanceof EntityDamageByEntityEvent) { EntityDamageByEntityEvent be = (EntityDamageByEntityEvent)e;
/* 414 */       Player attacker = attacker(be.getDamager());
/* 415 */       if (attacker != null) {
/* 416 */         if (!isActive(attacker) || this.byPlayer.get(attacker.getUniqueId()) == this.byPlayer.get(p.getUniqueId())) {
/* 417 */           e.setCancelled(true);
/*     */           return;
/*     */         } 
/* 420 */         this.lastDamage.put(p.getUniqueId(), new Damage(attacker.getUniqueId(), System.currentTimeMillis()));
/*     */       }  }
/*     */     
/* 423 */     if (e.getCause() == EntityDamageEvent.DamageCause.VOID || p.getHealth() - e.getFinalDamage() <= 0.0D) {
/* 424 */       e.setCancelled(true);
/* 425 */       handleDeath(p);
/*     */       return;
/*     */     } 
/* 428 */     e.setCancelled(false);
/*     */   }
/*     */   
/*     */   public void onMove(PlayerMoveEvent e) {
/* 432 */     Player p = e.getPlayer();
/* 433 */     if (this.state == State.COUNTDOWN) {
/* 434 */       if (this.byPlayer.containsKey(p.getUniqueId())) {
/* 435 */         Location from = e.getFrom();
/* 436 */         Location to = e.getTo();
/* 437 */         if (from.getX() != to.getX() || from.getZ() != to.getZ() || to.getY() > from.getY()) {
/* 438 */           Location fixed = from.clone();
/* 439 */           fixed.setYaw(to.getYaw());
/* 440 */           fixed.setPitch(to.getPitch());
/* 441 */           e.setTo(fixed);
/*     */         } 
/*     */       } 
/*     */       return;
/*     */     } 
/* 446 */     if (this.state != State.RUNNING || e.getTo().getY() >= this.arena.getMinY()) {
/*     */       return;
/*     */     }
/* 449 */     if (isActive(p)) {
/* 450 */       handleDeath(p);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private boolean isActive(Player p) {
/* 457 */     TeamState t = this.byPlayer.get(p.getUniqueId());
/* 458 */     return (this.state == State.RUNNING && t != null && t.alive
/* 459 */       .contains(p.getUniqueId()) && !this.respawning.contains(p.getUniqueId()));
/*     */   }
/*     */   
/*     */   private void breakBed(Player p, Block b) {
/* 463 */     TeamState owner = bedOwner(b);
/* 464 */     if (owner == null) {
/*     */       return;
/*     */     }
/* 467 */     TeamState mine = this.byPlayer.get(p.getUniqueId());
/* 468 */     if (owner == mine) {
/* 469 */       this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "You can't break your own bed!");
/*     */       return;
/*     */     } 
/* 472 */     if (!owner.bedAlive) {
/*     */       return;
/*     */     }
/* 475 */     owner.bedAlive = false;
/* 476 */     this.bedBreaks.merge(p.getUniqueId(), Integer.valueOf(1), Integer::sum);
/* 477 */     removeBed(b);
/* 478 */     broadcast(String.valueOf(ChatColor.GRAY) + "The bed of team " + String.valueOf(ChatColor.GRAY) + Teams.display(owner.color) + " was destroyed by " + String.valueOf(ChatColor.GRAY) + 
/* 479 */         String.valueOf(Teams.chat(mine.color)) + p.getName() + "!");
/* 480 */     for (UUID id : owner.members) {
/* 481 */       Player victim = Bukkit.getPlayer(id);
/* 482 */       if (victim != null) {
/* 483 */         Titles.send(victim, String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.RED) + "BED DESTROYED!", String.valueOf(ChatColor.GRAY) + "You will no longer respawn!", 0, 50, 10);
/*     */         
/* 485 */         victim.playSound(victim.getLocation(), Sound.ENDERDRAGON_GROWL, 1.0F, 1.0F);
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   private TeamState bedOwner(Block b) {
/* 491 */     for (TeamState t : this.teams.values()) {
/* 492 */       int dx = Math.abs(b.getX() - t.bed.blockX());
/* 493 */       int dz = Math.abs(b.getZ() - t.bed.blockZ());
/* 494 */       if (b.getY() == t.bed.blockY() && dx + dz <= 1) {
/* 495 */         return t;
/*     */       }
/*     */     } 
/* 498 */     return null;
/*     */   }
/*     */ 
/*     */   
/*     */   private static void removeBed(Block b) {
/* 503 */     BlockFace[] faces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
/* 504 */     for (BlockFace f : faces) {
/* 505 */       Block n = b.getRelative(f);
/* 506 */       if (n.getType() == Material.BED_BLOCK) {
/* 507 */         n.setType(Material.AIR);
/*     */       }
/*     */     } 
/* 510 */     b.setType(Material.AIR);
/*     */   }
/*     */   
/*     */   private void handleDeath(Player p) {
/* 514 */     if (!isActive(p)) {
/*     */       return;
/*     */     }
/* 517 */     TeamState t = this.byPlayer.get(p.getUniqueId());
/* 518 */     Damage d = this.lastDamage.remove(p.getUniqueId());
/* 519 */     Player killer = (d != null && System.currentTimeMillis() - d.time() < 10000L) ? Bukkit.getPlayer(d.by()) : null;
/* 520 */     if (killer != null) {
/* 521 */       this.kills.merge(killer.getUniqueId(), Integer.valueOf(1), Integer::sum);
/*     */     }
/* 523 */     String who = String.valueOf(Teams.chat(t.color)) + String.valueOf(Teams.chat(t.color)) + p.getName();
/* 524 */     String how = (killer != null) ? (" was killed by " + killer.getName()) : " died";
/*     */     
/* 526 */     reset(p);
/* 527 */     if (t.bedAlive) {
/* 528 */       broadcast(who + who + ".");
/* 529 */       respawn(p, t);
/*     */     } else {
/* 531 */       broadcast(who + who + ". " + how + "ELIMINATED!");
/* 532 */       Titles.send(p, String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.RED) + "ELIMINATED", String.valueOf(ChatColor.GRAY) + "You can't respawn.", 0, 50, 10);
/*     */       
/* 534 */       eliminate(p, null, true);
/*     */     } 
/*     */   }
/*     */   
/*     */   private void respawn(Player p, final TeamState t) {
/* 539 */     final UUID id = p.getUniqueId();
/* 540 */     this.respawning.add(id);
/* 541 */     p.setGameMode(GameMode.SPECTATOR);
/* 542 */     p.teleport(this.arena.getSpawn().toLocation(this.world));
/* 543 */     final int total = Math.max(1, this.plugin.getConfig().getInt("respawn-delay-seconds", 5));
/* 544 */     (new BukkitRunnable() {
/* 545 */         int left = total;
/*     */ 
/*     */         
/*     */         public void run() {
/* 549 */           Player pl = Bukkit.getPlayer(id);
/* 550 */           if (Match.this.state != Match.State.RUNNING || !Match.this.respawning.contains(id) || pl == null || !t.alive.contains(id)) {
/* 551 */             Match.this.respawning.remove(id);
/* 552 */             cancel();
/*     */             return;
/*     */           } 
/* 555 */           if (this.left <= 0) {
/* 556 */             cancel();
/* 557 */             Match.this.respawning.remove(id);
/* 558 */             Match.this.equip(pl, t);
/* 559 */             pl.teleport(t.spawn.toLocation(Match.this.world));
/* 560 */             Titles.send(pl, String.valueOf(ChatColor.GREEN) + String.valueOf(ChatColor.GREEN) + "RESPAWNED", "", 0, 20, 10);
/*     */             return;
/*     */           } 
/* 563 */           Titles.send(pl, String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.RED) + "YOU DIED!", String.valueOf(ChatColor.YELLOW) + "Respawning in " + String.valueOf(ChatColor.YELLOW) + "s", 0, 25, 0);
/*     */           
/* 565 */           this.left--;
/*     */         }
/* 567 */       }).runTaskTimer((Plugin)this.plugin, 0L, 20L);
/*     */   }
/*     */   
/*     */   private void eliminate(Player p, String message, boolean spectate) {
/* 571 */     TeamState t = this.byPlayer.get(p.getUniqueId());
/* 572 */     if (t == null || !t.alive.remove(p.getUniqueId())) {
/*     */       return;
/*     */     }
/* 575 */     this.respawning.remove(p.getUniqueId());
/* 576 */     if (message != null) {
/* 577 */       broadcast(String.valueOf(ChatColor.GRAY) + String.valueOf(ChatColor.GRAY));
/*     */     }
/* 579 */     if (spectate && p.isOnline() && this.world != null) {
/* 580 */       p.setGameMode(GameMode.SPECTATOR);
/* 581 */       p.teleport(this.arena.getSpawn().toLocation(this.world));
/*     */     } 
/* 583 */     checkWin();
/*     */   }
/*     */   
/*     */   private void checkWin() {
/* 587 */     if (this.state != State.RUNNING) {
/*     */       return;
/*     */     }
/* 590 */     List<TeamState> left = new ArrayList<>();
/* 591 */     for (TeamState t : this.teams.values()) {
/* 592 */       if (!t.alive.isEmpty()) {
/* 593 */         left.add(t);
/*     */       }
/*     */     } 
/* 596 */     if (left.size() <= 1) {
/* 597 */       end(left.isEmpty() ? null : left.get(0));
/*     */     }
/*     */   }
/*     */   
/*     */   private static Player attacker(Entity damager) {
/* 602 */     if (damager instanceof Player) { Player p = (Player)damager;
/* 603 */       return p; }
/*     */     
/* 605 */     if (damager instanceof Projectile) { Projectile proj = (Projectile)damager; ProjectileSource projectileSource = proj.getShooter(); if (projectileSource instanceof Player) { Player p = (Player)projectileSource;
/* 606 */         return p; }
/*     */        }
/* 608 */      return null;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private void broadcast(String message) {
/* 614 */     for (UUID id : this.byPlayer.keySet()) {
/* 615 */       Player p = Bukkit.getPlayer(id);
/* 616 */       if (p != null) {
/* 617 */         p.sendMessage(BedFightPlugin.PREFIX + BedFightPlugin.PREFIX);
/*     */       }
/*     */     } 
/*     */   }
/*     */   
/*     */   private static long key(Block b) {
/* 623 */     return (b.getX() & 0x3FFFFFF) << 38L | (b.getZ() & 0x3FFFFFF) << 12L | b.getY() & 0xFFFL;
/*     */   }
/*     */   
/*     */   private static void reset(Player p) {
/* 627 */     p.setGameMode(GameMode.SURVIVAL);
/* 628 */     p.getInventory().clear();
/* 629 */     p.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
/* 630 */     p.setHealth(p.getMaxHealth());
/* 631 */     p.setFoodLevel(20);
/* 632 */     p.setSaturation(20.0F);
/* 633 */     p.setFireTicks(0);
/* 634 */     p.setFallDistance(0.0F);
/* 635 */     for (PotionEffect pe : new ArrayList(p.getActivePotionEffects())) {
/* 636 */       p.removePotionEffect(pe.getType());
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   private void equip(Player p, TeamState t) {
/* 642 */     reset(p);
/* 643 */     this.plugin.getKitManager().apply(p, t.color);
/* 644 */     p.updateInventory();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public List<TeamView> teamViews(Player viewer) {
/* 651 */     TeamState mine = this.byPlayer.get(viewer.getUniqueId());
/* 652 */     List<TeamView> out = new ArrayList<>();
/* 653 */     for (TeamState t : this.teams.values()) {
/* 654 */       out.add(new TeamView(t.color, t.bedAlive, t.alive.size(), (t == mine)));
/*     */     }
/* 656 */     return out;
/*     */   }
/*     */   
/*     */   public Map<String, String> placeholders(Player viewer) {
/*     */     String status;
/* 661 */     Map<String, String> m = new HashMap<>();
/* 662 */     TeamState mine = this.byPlayer.get(viewer.getUniqueId());
/* 663 */     List<String> enemyNames = new ArrayList<>();
/* 664 */     int pingSum = 0;
/* 665 */     int pingCount = 0;
/* 666 */     for (TeamState t : this.teams.values()) {
/* 667 */       if (t == mine) {
/*     */         continue;
/*     */       }
/* 670 */       for (UUID id : t.members) {
/* 671 */         Player pl = Bukkit.getPlayer(id);
/* 672 */         enemyNames.add((pl != null) ? pl.getName() : "?");
/* 673 */         if (pl != null) {
/* 674 */           pingSum += Ping.get(pl);
/* 675 */           pingCount++;
/*     */         } 
/*     */       } 
/*     */     } 
/*     */     
/* 680 */     long elapsed = (this.state == State.RUNNING || this.state == State.ENDING) ? Math.max(0L, (System.currentTimeMillis() - this.startMillis) / 1000L) : 0L;
/*     */     
/* 682 */     switch (this.state) { case PREPARING:
/* 683 */         status = "Preparing"; break;
/* 684 */       case COUNTDOWN: status = "Starting in " + this.countdownLeft + "s"; break;
/* 685 */       case RUNNING: status = "Fighting"; break;
/* 686 */       default: status = "Ending"; break; }
/*     */     
/* 688 */     m.put("%player%", viewer.getName());
/* 689 */     m.put("%mode%", this.mode.label());
/* 690 */     m.put("%map%", this.arena.getName());
/* 691 */     m.put("%team%", (mine != null) ? Teams.display(mine.color) : "-");
/* 692 */     m.put("%opponent%", String.join(", ", (Iterable)enemyNames));
/* 693 */     m.put("%ping%", String.valueOf(Ping.get(viewer)));
/* 694 */     m.put("%opponent_ping%", String.valueOf((pingCount == 0) ? 0 : (pingSum / pingCount)));
/* 695 */     m.put("%status%", status);
/* 696 */     m.put("%countdown%", String.valueOf(this.countdownLeft));
/* 697 */     m.put("%time%", String.format("%d:%02d", new Object[] { Long.valueOf(elapsed / 60L), Long.valueOf(elapsed % 60L) }));
/* 698 */     m.put("%kills%", String.valueOf(this.kills.getOrDefault(viewer.getUniqueId(), Integer.valueOf(0))));
/* 699 */     m.put("%bed_breaks%", String.valueOf(this.bedBreaks.getOrDefault(viewer.getUniqueId(), Integer.valueOf(0))));
/* 700 */     return m;
/*     */   }
/*     */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\game\Match.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */