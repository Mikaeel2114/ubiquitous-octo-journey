/*     */ package com.example.bedfight.arena;
/*     */ 
/*     */ import com.example.bedfight.BedFightPlugin;
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.util.Collection;
/*     */ import java.util.HashMap;
/*     */ import java.util.HashSet;
/*     */ import java.util.LinkedHashMap;
/*     */ import java.util.Map;
/*     */ import java.util.Set;
/*     */ import java.util.UUID;
/*     */ import java.util.logging.Level;
/*     */ import org.bukkit.Bukkit;
/*     */ import org.bukkit.DyeColor;
/*     */ import org.bukkit.World;
/*     */ import org.bukkit.WorldCreator;
/*     */ import org.bukkit.configuration.file.YamlConfiguration;
/*     */ import org.bukkit.entity.Player;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class ArenaManager
/*     */ {
/*     */   private final BedFightPlugin plugin;
/*     */   private final File dir;
/*  30 */   private final Map<String, Arena> arenas = new LinkedHashMap<>();
/*  31 */   private final Map<UUID, Arena> sessions = new HashMap<>();
/*  32 */   private final Map<UUID, DyeColor> selectedTeam = new HashMap<>();
/*  33 */   private final Set<UUID> buildMode = new HashSet<>();
/*     */   
/*     */   public ArenaManager(BedFightPlugin plugin) {
/*  36 */     this.plugin = plugin;
/*  37 */     this.dir = new File(plugin.getDataFolder(), "arenas");
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public void loadAll() {
/*  43 */     this.arenas.clear();
/*  44 */     this.dir.mkdirs();
/*  45 */     File[] files = this.dir.listFiles((d, n) -> n.toLowerCase().endsWith(".yml"));
/*  46 */     if (files == null) {
/*     */       return;
/*     */     }
/*  49 */     for (File f : files) {
/*  50 */       String name = f.getName().substring(0, f.getName().length() - 4);
/*  51 */       Arena a = Arena.read(name, YamlConfiguration.loadConfiguration(f));
/*  52 */       String problem = a.validate();
/*  53 */       if (problem == null) {
/*  54 */         String geo = a.checkGeometry();
/*  55 */         if (geo != null) {
/*  56 */           this.plugin.getLogger().warning("Arena '" + name + "' may be misconfigured: " + geo);
/*     */         }
/*  58 */         this.arenas.put(name.toLowerCase(), a);
/*     */       } else {
/*  60 */         this.plugin.getLogger().warning("Skipping incomplete arena '" + name + "': " + problem);
/*     */       } 
/*     */     } 
/*     */   }
/*     */   public Collection<Arena> all() {
/*  65 */     return this.arenas.values();
/*     */   } public Arena get(String name) {
/*  67 */     return this.arenas.get(name.toLowerCase());
/*     */   }
/*     */   
/*     */   public String save(Player p) {
/*  71 */     Arena a = this.sessions.get(p.getUniqueId());
/*  72 */     if (a == null) {
/*  73 */       return "No arena setup in progress. Use /bedfight setuparena <world_name>.";
/*     */     }
/*  75 */     String err = a.validate();
/*  76 */     if (err == null) {
/*  77 */       err = a.checkGeometry();
/*     */     }
/*  79 */     if (err != null) {
/*  80 */       return err;
/*     */     }
/*  82 */     YamlConfiguration y = new YamlConfiguration();
/*  83 */     a.write(y);
/*  84 */     File f = new File(this.dir, a.getName() + ".yml");
/*     */     try {
/*  86 */       this.dir.mkdirs();
/*  87 */       y.save(f);
/*  88 */     } catch (IOException ex) {
/*  89 */       this.plugin.getLogger().log(Level.SEVERE, "Could not save arena " + a.getName(), ex);
/*  90 */       return "Could not write " + f.getName() + ": " + ex.getMessage();
/*     */     } 
/*     */     
/*  93 */     this.arenas.put(a.getName().toLowerCase(), Arena.read(a.getName(), y));
/*  94 */     World w = Bukkit.getWorld(a.getWorldName());
/*  95 */     if (w != null) {
/*  96 */       w.save();
/*     */     }
/*  98 */     return null;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public String setupArena(Player p, String worldName) {
/* 105 */     if (!worldName.matches("[A-Za-z0-9_.\\-]+") || worldName.contains("..")) {
/* 106 */       return "Invalid world name.";
/*     */     }
/* 108 */     File container = Bukkit.getWorldContainer();
/* 109 */     File folder = new File(container, worldName);
/* 110 */     if (!folder.isDirectory() || !(new File(folder, "level.dat")).isFile()) {
/* 111 */       return "World folder '" + worldName + "' (with level.dat) not found in " + container
/* 112 */         .getAbsolutePath();
/*     */     }
/* 114 */     World world = Bukkit.getWorld(worldName);
/* 115 */     if (world == null) {
/* 116 */       world = Bukkit.createWorld(new WorldCreator(worldName));
/*     */     }
/* 118 */     if (world == null) {
/* 119 */       return "Failed to load world '" + worldName + "'.";
/*     */     }
/*     */     
/* 122 */     File existing = new File(this.dir, worldName + ".yml");
/*     */ 
/*     */     
/* 125 */     Arena arena = existing.isFile() ? Arena.read(worldName, YamlConfiguration.loadConfiguration(existing)) : new Arena(worldName, worldName);
/* 126 */     this.sessions.put(p.getUniqueId(), arena);
/* 127 */     this.selectedTeam.remove(p.getUniqueId());
/* 128 */     p.teleport(world.getSpawnLocation());
/* 129 */     return null;
/*     */   }
/*     */   public Arena session(Player p) {
/* 132 */     return this.sessions.get(p.getUniqueId());
/*     */   } public void select(Player p, DyeColor c) {
/* 134 */     this.selectedTeam.put(p.getUniqueId(), c);
/*     */   } public DyeColor selected(Player p) {
/* 136 */     return this.selectedTeam.get(p.getUniqueId());
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean toggleBuildMode(Player p) {
/* 142 */     if (!this.buildMode.remove(p.getUniqueId())) {
/* 143 */       this.buildMode.add(p.getUniqueId());
/* 144 */       return true;
/*     */     } 
/* 146 */     return false;
/*     */   }
/*     */   public boolean isBuildMode(Player p) {
/* 149 */     return this.buildMode.contains(p.getUniqueId());
/*     */   } public void onQuit(Player p) {
/* 151 */     this.buildMode.remove(p.getUniqueId());
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean isTemplateWorld(World w) {
/* 157 */     String n = w.getName();
/* 158 */     for (Arena a : this.arenas.values()) {
/* 159 */       if (a.getWorldName().equalsIgnoreCase(n)) {
/* 160 */         return true;
/*     */       }
/*     */     } 
/* 163 */     for (Arena a : this.sessions.values()) {
/* 164 */       if (a.getWorldName().equalsIgnoreCase(n)) {
/* 165 */         return true;
/*     */       }
/*     */     } 
/* 168 */     return false;
/*     */   }
/*     */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\arena\ArenaManager.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */