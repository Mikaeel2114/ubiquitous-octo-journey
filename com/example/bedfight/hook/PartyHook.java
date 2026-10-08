/*     */ package com.example.bedfight.hook;
/*     */ 
/*     */ import com.example.bedfight.BedFightPlugin;
/*     */ import java.lang.reflect.Method;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Collection;
/*     */ import java.util.Collections;
/*     */ import java.util.HashSet;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import java.util.Set;
/*     */ import java.util.UUID;
/*     */ import org.bukkit.Bukkit;
/*     */ import org.bukkit.entity.Player;
/*     */ import org.bukkit.plugin.Plugin;
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
/*     */ public final class PartyHook
/*     */ {
/*     */   private static final String PLUGIN_NAME = "minestormparty";
/*  31 */   private static final String[] API_ACCESSORS = new String[] { "getPartyManager", "getPartyAPI", "getAPI", "getPartyService" };
/*     */   
/*  33 */   private static final String[] GET_PARTY = new String[] { "getParty", "getPartyOf", "getPlayerParty" };
/*     */   
/*  35 */   private static final String[] PARTY_MEMBERS = new String[] { "getMembers", "getPlayers", "getOnlineMembers" };
/*     */   
/*  37 */   private static final String[] PARTY_LEADER = new String[] { "getLeader", "getOwner" };
/*     */   
/*  39 */   private static final String[] SPLIT = new String[] { "splitIntoTeams", "splitTeams", "randomTeams", "createTeams" };
/*     */   
/*     */   private final BedFightPlugin plugin;
/*     */   private Object api;
/*     */   private boolean available;
/*     */   
/*     */   public PartyHook(BedFightPlugin plugin) {
/*  46 */     this.plugin = plugin;
/*  47 */     Plugin party = Bukkit.getPluginManager().getPlugin("minestormparty");
/*  48 */     if (party == null || !party.isEnabled()) {
/*  49 */       plugin.getLogger().warning("minestormparty not found - team modes (2v2/3v3) are limited.");
/*     */       return;
/*     */     } 
/*  52 */     Object resolved = null;
/*  53 */     for (String accessor : API_ACCESSORS) {
/*  54 */       Object o = call(party, new String[] { accessor }, new Object[0]);
/*  55 */       if (o != null) {
/*  56 */         resolved = o;
/*     */         break;
/*     */       } 
/*     */     } 
/*  60 */     this.api = (resolved != null) ? resolved : party;
/*  61 */     this.available = true;
/*  62 */     plugin.getLogger().info("Hooked into minestormparty (" + this.api.getClass().getName() + ").");
/*     */   }
/*     */   public boolean isAvailable() {
/*  65 */     return this.available;
/*     */   }
/*     */   
/*     */   public List<Player> getPartyMembers(Player player) {
/*  69 */     List<Player> out = new ArrayList<>();
/*  70 */     if (this.available) {
/*  71 */       Object party = call(this.api, GET_PARTY, new Object[] { player });
/*  72 */       if (party != null) {
/*  73 */         out.addAll(toPlayers(call(party, PARTY_MEMBERS, new Object[0])));
/*     */       }
/*     */     } 
/*  76 */     out.remove(player);
/*  77 */     out.add(0, player);
/*  78 */     return out;
/*     */   }
/*     */ 
/*     */   
/*     */   public boolean isLeader(Player player) {
/*  83 */     if (!this.available) {
/*  84 */       return true;
/*     */     }
/*  86 */     Object party = call(this.api, GET_PARTY, new Object[] { player });
/*  87 */     if (party == null) {
/*  88 */       return true;
/*     */     }
/*  90 */     Object leader = call(party, PARTY_LEADER, new Object[0]);
/*  91 */     if (leader instanceof Player) { Player p = (Player)leader;
/*  92 */       return p.getUniqueId().equals(player.getUniqueId()); }
/*     */     
/*  94 */     if (leader instanceof UUID) { UUID id = (UUID)leader;
/*  95 */       return id.equals(player.getUniqueId()); }
/*     */     
/*  97 */     return true;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public List<List<Player>> splitIntoTeams(List<Player> players, int teamSize, int teamCount) {
/* 105 */     if (this.available) {
/* 106 */       Object raw = call(this.api, SPLIT, new Object[] { new ArrayList<>(players), Integer.valueOf(teamSize) });
/* 107 */       List<List<Player>> teams = toTeams(raw);
/* 108 */       if (isValidSplit(teams, players, teamSize, teamCount)) {
/* 109 */         return teams;
/*     */       }
/*     */     } 
/* 112 */     List<Player> shuffled = new ArrayList<>(players);
/* 113 */     Collections.shuffle(shuffled);
/* 114 */     List<List<Player>> result = new ArrayList<>();
/* 115 */     for (int i = 0; i < teamCount; i++) {
/* 116 */       int from = i * teamSize;
/* 117 */       int to = Math.min(from + teamSize, shuffled.size());
/* 118 */       result.add(new ArrayList<>(shuffled.subList(from, to)));
/*     */     } 
/* 120 */     return result;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   private static boolean isValidSplit(List<List<Player>> teams, List<Player> players, int size, int count) {
/* 126 */     if (teams.size() != count) {
/* 127 */       return false;
/*     */     }
/* 129 */     Set<UUID> seen = new HashSet<>();
/* 130 */     for (List<Player> t : teams) {
/* 131 */       if (t.size() != size) {
/* 132 */         return false;
/*     */       }
/* 134 */       for (Player p : t) {
/* 135 */         seen.add(p.getUniqueId());
/*     */       }
/*     */     } 
/* 138 */     for (Player p : players) {
/* 139 */       if (!seen.contains(p.getUniqueId())) {
/* 140 */         return false;
/*     */       }
/*     */     } 
/* 143 */     return true;
/*     */   }
/*     */   
/*     */   private static List<Player> toPlayers(Object raw) {
/* 147 */     List<Player> out = new ArrayList<>();
/* 148 */     if (raw instanceof Collection) { Collection<?> c = (Collection)raw;
/* 149 */       for (Object o : c) {
/* 150 */         Player p = null;
/* 151 */         if (o instanceof Player) { Player pl = (Player)o;
/* 152 */           p = pl; }
/* 153 */         else if (o instanceof UUID) { UUID id = (UUID)o;
/* 154 */           p = Bukkit.getPlayer(id); }
/* 155 */         else if (o instanceof String) { String name = (String)o;
/* 156 */           p = Bukkit.getPlayerExact(name); }
/*     */         
/* 158 */         if (p != null && p.isOnline() && !out.contains(p)) {
/* 159 */           out.add(p);
/*     */         }
/*     */       }  }
/*     */     
/* 163 */     return out;
/*     */   }
/*     */   
/*     */   private static List<List<Player>> toTeams(Object raw) {
/* 167 */     List<List<Player>> teams = new ArrayList<>();
/* 168 */     Collection<?> outer = null;
/* 169 */     if (raw instanceof Map) { Map<?, ?> m = (Map<?, ?>)raw;
/* 170 */       outer = m.values(); }
/* 171 */     else if (raw instanceof Collection) { Collection<?> c = (Collection)raw;
/* 172 */       outer = c; }
/*     */     
/* 174 */     if (outer != null) {
/* 175 */       for (Object o : outer) {
/* 176 */         teams.add(toPlayers(o));
/*     */       }
/*     */     }
/* 179 */     return teams;
/*     */   }
/*     */ 
/*     */   
/*     */   private static Object call(Object target, String[] names, Object... args) {
/* 184 */     if (target == null) {
/* 185 */       return null;
/*     */     }
/* 187 */     for (String name : names) {
/* 188 */       for (Method m : target.getClass().getMethods()) {
/* 189 */         if (m.getName().equals(name) && m.getParameterCount() == args.length)
/*     */         {
/*     */           
/* 192 */           if (accepts(m.getParameterTypes(), args)) {
/*     */             
/*     */             try {
/*     */               
/* 196 */               m.setAccessible(true);
/* 197 */               return m.invoke(target, args);
/* 198 */             } catch (ReflectiveOperationException|RuntimeException reflectiveOperationException) {}
/*     */           }
/*     */         }
/*     */       } 
/*     */     } 
/* 203 */     return null;
/*     */   }
/*     */   
/*     */   private static boolean accepts(Class<?>[] types, Object[] args) {
/* 207 */     for (int i = 0; i < types.length; i++) {
/* 208 */       Class<?> t = types[i];
/* 209 */       if (t == int.class) t = Integer.class; 
/* 210 */       if (t == long.class) t = Long.class; 
/* 211 */       if (t == boolean.class) t = Boolean.class; 
/* 212 */       if (args[i] == null || !t.isAssignableFrom(args[i].getClass())) {
/* 213 */         return false;
/*     */       }
/*     */     } 
/* 216 */     return true;
/*     */   }
/*     */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\hook\PartyHook.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */