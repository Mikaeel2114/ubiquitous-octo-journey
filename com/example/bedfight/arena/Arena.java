/*     */ package com.example.bedfight.arena;
/*     */ 
/*     */ import java.util.LinkedHashMap;
/*     */ import java.util.Map;
/*     */ import org.bukkit.DyeColor;
/*     */ import org.bukkit.configuration.ConfigurationSection;
/*     */ import org.bukkit.configuration.file.YamlConfiguration;
/*     */ 
/*     */ 
/*     */ public final class Arena
/*     */ {
/*     */   private final String name;
/*     */   private final String worldName;
/*     */   private Pos pos1;
/*     */   private Pos pos2;
/*     */   private Pos spawn;
/*     */   private int minX;
/*     */   private int maxX;
/*     */   private int minY;
/*     */   private int maxY;
/*     */   private int minZ;
/*     */   private int maxZ;
/*  23 */   private final Map<DyeColor, TeamSpec> teams = new LinkedHashMap<>();
/*     */   
/*     */   public Arena(String name, String worldName) {
/*  26 */     this.name = name;
/*  27 */     this.worldName = worldName;
/*     */   }
/*     */   
/*  30 */   public String getName() { return this.name; }
/*  31 */   public String getWorldName() { return this.worldName; }
/*  32 */   public Pos getPos1() { return this.pos1; }
/*  33 */   public Pos getPos2() { return this.pos2; }
/*  34 */   public Pos getSpawn() { return this.spawn; }
/*  35 */   public void setSpawn(Pos spawn) { this.spawn = spawn; } public Map<DyeColor, TeamSpec> getTeams() {
/*  36 */     return this.teams;
/*     */   }
/*  38 */   public int getMinY() { return this.minY; } public int getMaxY() {
/*  39 */     return this.maxY;
/*     */   }
/*  41 */   public void setPos1(Pos p) { this.pos1 = p; recalc(); }
/*  42 */   public void setPos2(Pos p) { this.pos2 = p; recalc(); } public boolean hasBounds() {
/*  43 */     return (this.pos1 != null && this.pos2 != null);
/*     */   }
/*     */   private void recalc() {
/*  46 */     if (!hasBounds()) {
/*     */       return;
/*     */     }
/*  49 */     this.minX = Math.min(this.pos1.blockX(), this.pos2.blockX());
/*  50 */     this.maxX = Math.max(this.pos1.blockX(), this.pos2.blockX());
/*  51 */     this.minY = Math.min(this.pos1.blockY(), this.pos2.blockY());
/*  52 */     this.maxY = Math.max(this.pos1.blockY(), this.pos2.blockY());
/*  53 */     this.minZ = Math.min(this.pos1.blockZ(), this.pos2.blockZ());
/*  54 */     this.maxZ = Math.max(this.pos1.blockZ(), this.pos2.blockZ());
/*     */   }
/*     */ 
/*     */   
/*     */   public boolean inBuildArea(int x, int y, int z) {
/*  59 */     return (x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ && y >= this.minY && y <= this.maxY);
/*     */   }
/*     */ 
/*     */   
/*     */   public boolean inAnyDefense(int x, int y, int z) {
/*  64 */     for (TeamSpec t : this.teams.values()) {
/*  65 */       if (t.inDefense(x, y, z)) {
/*  66 */         return true;
/*     */       }
/*     */     } 
/*  69 */     return false;
/*     */   }
/*     */   
/*     */   public TeamSpec team(DyeColor color, boolean create) {
/*  73 */     TeamSpec t = this.teams.get(color);
/*  74 */     if (t == null && create) {
/*  75 */       t = new TeamSpec(color);
/*  76 */       this.teams.put(color, t);
/*     */     } 
/*  78 */     return t;
/*     */   }
/*     */ 
/*     */   
/*     */   public String validate() {
/*  83 */     if (!hasBounds()) return "Set both boundaries first (/bedfight po1 and /bedfight po2)."; 
/*  84 */     if (this.spawn == null) return "Set the game spawn first (/bedfight setspawn)."; 
/*  85 */     if (this.teams.size() < 2) return "At least 2 teams are required (/bedfight createteam <color>)."; 
/*  86 */     for (TeamSpec t : this.teams.values()) {
/*  87 */       if (t.getSpawn() == null) return "Team " + String.valueOf(t.getColor()) + " has no spawn (/bedfight setteam)."; 
/*  88 */       if (t.getBed() == null) return "Team " + String.valueOf(t.getColor()) + " has no bed (/bedfight setbed)."; 
/*     */     } 
/*  90 */     return null;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public String checkGeometry() {
/*  99 */     if (!hasBounds()) return null; 
/* 100 */     if (this.spawn != null && this.spawn.blockY() < this.minY) {
/* 101 */       return "The game spawn is below the void level (min Y = " + this.minY + ").";
/*     */     }
/* 103 */     for (TeamSpec t : this.teams.values()) {
/* 104 */       if (t.getSpawn() != null && t.getSpawn().blockY() < this.minY) {
/* 105 */         return "Team " + String.valueOf(t.getColor()) + " spawn (Y " + t.getSpawn().blockY() + ") is below the void level (min Y = " + this.minY + "): players would die instantly.";
/*     */       }
/*     */       
/* 108 */       if (t.getBed() != null) {
/* 109 */         Pos b = t.getBed();
/* 110 */         if (b.blockX() < this.minX || b.blockX() > this.maxX || b.blockZ() < this.minZ || b.blockZ() > this.maxZ || b
/* 111 */           .blockY() < this.minY) {
/* 112 */           return "The bed of team " + String.valueOf(t.getColor()) + " (X " + b.blockX() + ", Y " + b.blockY() + ", Z " + b
/* 113 */             .blockZ() + ") is outside the po1/po2 box (X " + this.minX + " to " + this.maxX + ", Y " + this.minY + " to " + this.maxY + ", Z " + this.minZ + " to " + this.maxZ + "). Re-run /bedfight po1 and po2 at two opposite corners so the box covers BOTH islands.";
/*     */         }
/*     */ 
/*     */         
/* 117 */         if (this.maxY < b.blockY() + 3) {
/* 118 */           return "Max Y (" + this.maxY + ") is too low: it must be at least 3 blocks above the bed of team " + 
/* 119 */             String.valueOf(t.getColor()) + " (Y " + b.blockY() + ") so bed defenses can be built. Set po2 (or po1) higher up.";
/*     */         }
/*     */       } 
/*     */     } 
/*     */     
/* 124 */     return null;
/*     */   }
/*     */   
/*     */   public void write(YamlConfiguration y) {
/* 128 */     y.set("name", this.name);
/* 129 */     y.set("world", this.worldName);
/* 130 */     this.pos1.write(y.createSection("pos1"));
/* 131 */     this.pos2.write(y.createSection("pos2"));
/* 132 */     y.set("bounds.min-x", Integer.valueOf(this.minX));
/* 133 */     y.set("bounds.max-x", Integer.valueOf(this.maxX));
/* 134 */     y.set("bounds.min-y", Integer.valueOf(this.minY));
/* 135 */     y.set("bounds.max-y", Integer.valueOf(this.maxY));
/* 136 */     y.set("bounds.min-z", Integer.valueOf(this.minZ));
/* 137 */     y.set("bounds.max-z", Integer.valueOf(this.maxZ));
/* 138 */     this.spawn.write(y.createSection("spawn"));
/* 139 */     for (TeamSpec t : this.teams.values()) {
/* 140 */       String base = "teams." + t.getColor().name();
/* 141 */       t.getSpawn().write(y.createSection(base + ".spawn"));
/* 142 */       t.getBed().write(y.createSection(base + ".bed"));
/* 143 */       if (t.hasDefense()) {
/* 144 */         t.getDefense1().write(y.createSection(base + ".defense.pos1"));
/* 145 */         t.getDefense2().write(y.createSection(base + ".defense.pos2"));
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   public static Arena read(String name, YamlConfiguration y) {
/* 151 */     Arena a = new Arena(name, y.getString("world", name));
/* 152 */     a.pos1 = Pos.read(y.getConfigurationSection("pos1"));
/* 153 */     a.pos2 = Pos.read(y.getConfigurationSection("pos2"));
/* 154 */     a.recalc();
/* 155 */     a.spawn = Pos.read(y.getConfigurationSection("spawn"));
/* 156 */     ConfigurationSection ts = y.getConfigurationSection("teams");
/* 157 */     if (ts != null) {
/* 158 */       for (String key : ts.getKeys(false)) {
/* 159 */         DyeColor c = Teams.parse(key);
/* 160 */         if (c == null) {
/*     */           continue;
/*     */         }
/* 163 */         TeamSpec t = a.team(c, true);
/* 164 */         t.setSpawn(Pos.read(ts.getConfigurationSection(key + ".spawn")));
/* 165 */         t.setBed(Pos.read(ts.getConfigurationSection(key + ".bed")));
/* 166 */         t.setDefense1(Pos.read(ts.getConfigurationSection(key + ".defense.pos1")));
/* 167 */         t.setDefense2(Pos.read(ts.getConfigurationSection(key + ".defense.pos2")));
/*     */       } 
/*     */     }
/* 170 */     return a;
/*     */   }
/*     */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\arena\Arena.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */