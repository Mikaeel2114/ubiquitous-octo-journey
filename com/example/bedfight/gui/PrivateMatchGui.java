/*     */ package com.example.bedfight.gui;
/*     */ 
/*     */ import com.example.bedfight.BedFightPlugin;
/*     */ import com.example.bedfight.arena.Arena;
/*     */ import com.example.bedfight.game.Mode;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Arrays;
/*     */ import java.util.List;
/*     */ import org.bukkit.Bukkit;
/*     */ import org.bukkit.ChatColor;
/*     */ import org.bukkit.Material;
/*     */ import org.bukkit.command.CommandSender;
/*     */ import org.bukkit.entity.HumanEntity;
/*     */ import org.bukkit.entity.Player;
/*     */ import org.bukkit.event.EventHandler;
/*     */ import org.bukkit.event.Listener;
/*     */ import org.bukkit.event.inventory.InventoryClickEvent;
/*     */ import org.bukkit.event.inventory.InventoryDragEvent;
/*     */ import org.bukkit.inventory.Inventory;
/*     */ import org.bukkit.inventory.InventoryHolder;
/*     */ import org.bukkit.inventory.ItemStack;
/*     */ import org.bukkit.inventory.meta.ItemMeta;
/*     */ 
/*     */ public final class PrivateMatchGui
/*     */   implements Listener
/*     */ {
/*     */   public static final String PERMISSION = "bedfight.rank.mvpplusplus";
/*  28 */   private static final String ARENA_TITLE = String.valueOf(ChatColor.DARK_GRAY) + "Private match: select map";
/*  29 */   private static final String MODE_TITLE = String.valueOf(ChatColor.DARK_GRAY) + "Private match: select mode"; private static final int BACK_SLOT = 22;
/*  30 */   private static final int[] MODE_SLOTS = new int[] { 11, 13, 15 };
/*     */   private final BedFightPlugin plugin;
/*     */   
/*  33 */   private enum Type { ARENAS, MODES; }
/*     */   
/*     */   private static final class Holder implements InventoryHolder {
/*     */     final PrivateMatchGui.Type type;
/*     */     final List<String> arenas;
/*     */     final String arena;
/*     */     Inventory inventory;
/*     */     
/*     */     Holder(PrivateMatchGui.Type type, List<String> arenas, String arena) {
/*  42 */       this.type = type;
/*  43 */       this.arenas = arenas;
/*  44 */       this.arena = arena;
/*     */     }
/*     */     
/*     */     public Inventory getInventory() {
/*  48 */       return this.inventory;
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   public PrivateMatchGui(BedFightPlugin plugin) {
/*  54 */     this.plugin = plugin;
/*     */   }
/*     */   
/*     */   public void open(Player p) {
/*  58 */     List<String> names = new ArrayList<>();
/*  59 */     for (Arena a : this.plugin.getArenaManager().all()) {
/*  60 */       names.add(a.getName());
/*     */     }
/*  62 */     if (names.isEmpty()) {
/*  63 */       this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "There are no arenas available.");
/*     */       return;
/*     */     } 
/*  66 */     int shown = Math.min(names.size(), 54);
/*  67 */     int rows = Math.max(1, (shown + 8) / 9);
/*  68 */     Holder holder = new Holder(Type.ARENAS, names.subList(0, shown), null);
/*  69 */     Inventory inv = Bukkit.createInventory(holder, rows * 9, ARENA_TITLE);
/*  70 */     holder.inventory = inv;
/*  71 */     for (int i = 0; i < shown; i++) {
/*  72 */       Arena a = this.plugin.getArenaManager().get(names.get(i));
/*  73 */       inv.setItem(i, item(Material.PAPER, String.valueOf(ChatColor.GREEN) + String.valueOf(ChatColor.GREEN), new String[] { String.valueOf(ChatColor.GRAY) + "Teams: " + String.valueOf(ChatColor.GRAY) + String.valueOf(ChatColor.WHITE), 
/*  74 */               "", String.valueOf(ChatColor.YELLOW) + "Click to select" }));
/*     */     } 
/*     */ 
/*     */     
/*  78 */     p.openInventory(inv);
/*     */   }
/*     */   
/*     */   private void openModes(Player p, String arenaName) {
/*  82 */     Holder holder = new Holder(Type.MODES, null, arenaName);
/*  83 */     Inventory inv = Bukkit.createInventory(holder, 27, MODE_TITLE);
/*  84 */     holder.inventory = inv;
/*  85 */     Mode[] modes = Mode.values();
/*  86 */     Material[] icons = { Material.WOOD_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD };
/*  87 */     for (int i = 0; i < modes.length; i++) {
/*  88 */       inv.setItem(MODE_SLOTS[i], item(icons[i], String.valueOf(ChatColor.GREEN) + String.valueOf(ChatColor.GREEN), new String[] { String.valueOf(ChatColor.GRAY) + "Map: " + String.valueOf(ChatColor.GRAY) + String.valueOf(ChatColor.WHITE), String.valueOf(ChatColor.GRAY) + "Players needed in party: " + String.valueOf(ChatColor.GRAY) + String.valueOf(ChatColor.WHITE), 
/*     */               
/*  90 */               "", String.valueOf(ChatColor.YELLOW) + "Click to start" }));
/*     */     } 
/*     */ 
/*     */     
/*  94 */     inv.setItem(22, item(Material.ARROW, String.valueOf(ChatColor.RED) + "Back", new String[0]));
/*  95 */     p.openInventory(inv);
/*     */   } @EventHandler
/*     */   public void onClick(InventoryClickEvent e) {
/*     */     Holder holder;
/*     */     Player p;
/* 100 */     InventoryHolder inventoryHolder = e.getInventory().getHolder(); if (inventoryHolder instanceof Holder) { holder = (Holder)inventoryHolder; }
/*     */     else
/*     */     { return; }
/* 103 */      e.setCancelled(true);
/* 104 */     HumanEntity humanEntity = e.getWhoClicked(); if (humanEntity instanceof Player) { p = (Player)humanEntity; }
/*     */     else
/*     */     { return; }
/* 107 */      int slot = e.getRawSlot();
/* 108 */     if (slot < 0 || slot >= e.getInventory().getSize()) {
/*     */       return;
/*     */     }
/* 111 */     if (!p.hasPermission("bedfight.rank.mvpplusplus")) {
/* 112 */       p.closeInventory();
/* 113 */       this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Only MVP++ can create private matches.");
/*     */       
/*     */       return;
/*     */     } 
/* 117 */     if (holder.type == Type.ARENAS) {
/* 118 */       if (slot < holder.arenas.size()) {
/* 119 */         openModes(p, holder.arenas.get(slot));
/*     */       }
/*     */       
/*     */       return;
/*     */     } 
/* 124 */     if (slot == 22) {
/* 125 */       open(p);
/*     */       return;
/*     */     } 
/* 128 */     Mode[] modes = Mode.values();
/* 129 */     for (int i = 0; i < MODE_SLOTS.length; i++) {
/* 130 */       if (slot == MODE_SLOTS[i]) {
/* 131 */         Arena arena = this.plugin.getArenaManager().get(holder.arena);
/* 132 */         p.closeInventory();
/* 133 */         if (arena == null) {
/* 134 */           this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "That arena no longer exists.");
/*     */           return;
/*     */         } 
/* 137 */         String error = this.plugin.getGameManager().startPrivateMatch(p, arena, modes[i]);
/* 138 */         if (error != null) {
/* 139 */           this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.RED));
/*     */         }
/*     */         return;
/*     */       } 
/*     */     } 
/*     */   }
/*     */   
/*     */   @EventHandler
/*     */   public void onDrag(InventoryDragEvent e) {
/* 148 */     if (e.getInventory().getHolder() instanceof Holder) {
/* 149 */       e.setCancelled(true);
/*     */     }
/*     */   }
/*     */   
/*     */   private static ItemStack item(Material m, String name, String... lore) {
/* 154 */     ItemStack it = new ItemStack(m);
/* 155 */     ItemMeta meta = it.getItemMeta();
/* 156 */     meta.setDisplayName(name);
/* 157 */     if (lore.length > 0) {
/* 158 */       meta.setLore(Arrays.asList(lore));
/*     */     }
/* 160 */     it.setItemMeta(meta);
/* 161 */     return it;
/*     */   }
/*     */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\gui\PrivateMatchGui.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */