/*     */ package com.example.bedfight.kit;
/*     */ 
/*     */ import com.example.bedfight.BedFightPlugin;
/*     */ import java.util.Arrays;
/*     */ import java.util.Iterator;
/*     */ import java.util.Map;
/*     */ import java.util.TreeMap;
/*     */ import org.bukkit.Bukkit;
/*     */ import org.bukkit.ChatColor;
/*     */ import org.bukkit.Material;
/*     */ import org.bukkit.command.CommandSender;
/*     */ import org.bukkit.entity.HumanEntity;
/*     */ import org.bukkit.entity.Player;
/*     */ import org.bukkit.event.EventHandler;
/*     */ import org.bukkit.event.Listener;
/*     */ import org.bukkit.event.inventory.InventoryClickEvent;
/*     */ import org.bukkit.event.inventory.InventoryCloseEvent;
/*     */ import org.bukkit.event.inventory.InventoryDragEvent;
/*     */ import org.bukkit.inventory.Inventory;
/*     */ import org.bukkit.inventory.InventoryHolder;
/*     */ import org.bukkit.inventory.ItemStack;
/*     */ import org.bukkit.inventory.meta.ItemMeta;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class KitEditorGui
/*     */   implements Listener
/*     */ {
/*  32 */   private static final String TITLE = String.valueOf(ChatColor.DARK_GRAY) + "BedFight kit editor";
/*     */   private static final int LOCKED_FROM = 40;
/*     */   private final BedFightPlugin plugin;
/*     */   
/*     */   private static final class Holder implements InventoryHolder { Inventory inventory;
/*     */     
/*     */     public Inventory getInventory() {
/*  39 */       return this.inventory;
/*     */     } }
/*     */ 
/*     */ 
/*     */   
/*     */   public KitEditorGui(BedFightPlugin plugin) {
/*  45 */     this.plugin = plugin;
/*     */   }
/*     */   
/*     */   public void open(Player p) {
/*  49 */     Holder holder = new Holder();
/*  50 */     Inventory inv = Bukkit.createInventory(holder, 54, TITLE);
/*  51 */     holder.inventory = inv;
/*     */     
/*  53 */     KitManager kit = this.plugin.getKitManager();
/*  54 */     for (Map.Entry<Integer, ItemStack> e : kit.getItems().entrySet()) {
/*  55 */       int slot = ((Integer)e.getKey()).intValue();
/*  56 */       inv.setItem((slot >= 9) ? (slot - 9) : (27 + slot), ((ItemStack)e.getValue()).clone());
/*     */     } 
/*  58 */     ItemStack[] armor = kit.getArmor();
/*  59 */     for (int i = 0; i < 4; i++) {
/*  60 */       if (armor[i] != null) {
/*  61 */         inv.setItem(36 + i, armor[i].clone());
/*     */       }
/*     */     } 
/*     */     
/*  65 */     ItemStack filler = named(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short)7), " ", new String[0]);
/*  66 */     for (int j = 40; j < 54; j++) {
/*  67 */       inv.setItem(j, filler);
/*     */     }
/*  69 */     inv.setItem(49, named(new ItemStack(Material.PAPER), String.valueOf(ChatColor.YELLOW) + "How it works", new String[] { String.valueOf(ChatColor.GRAY) + "Rows 1-3: inventory, row 4: hotbar", String.valueOf(ChatColor.GRAY) + "Row 5, first 4 slots: armor", String.valueOf(ChatColor.GRAY) + "Close the window to save the kit.", String.valueOf(ChatColor.GRAY) + "Wool, stained clay/glass and leather", String.valueOf(ChatColor.GRAY) + "armor take the team color." }));
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*  75 */     p.openInventory(inv);
/*  76 */     this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.GRAY) + "Arrange the kit and close the window to save it.");
/*     */   }
/*     */   
/*     */   @EventHandler
/*     */   public void onClick(InventoryClickEvent e) {
/*  81 */     if (e.getInventory().getHolder() instanceof Holder) {
/*  82 */       int raw = e.getRawSlot();
/*  83 */       if (raw >= 40 && raw < 54) {
/*  84 */         e.setCancelled(true);
/*     */       }
/*     */     } 
/*     */   }
/*     */   
/*     */   @EventHandler
/*     */   public void onDrag(InventoryDragEvent e) {
/*  91 */     if (e.getInventory().getHolder() instanceof Holder)
/*  92 */       for (Iterator<Integer> iterator = e.getRawSlots().iterator(); iterator.hasNext(); ) { int raw = ((Integer)iterator.next()).intValue();
/*  93 */         if (raw >= 40 && raw < 54) {
/*  94 */           e.setCancelled(true);
/*     */           return;
/*     */         }  }
/*     */        
/*     */   }
/*     */   
/*     */   @EventHandler
/*     */   public void onClose(InventoryCloseEvent e) {
/*     */     Player p;
/* 103 */     if (e.getInventory().getHolder() instanceof Holder) { HumanEntity humanEntity = e.getPlayer(); if (humanEntity instanceof Player) { p = (Player)humanEntity; } else { return; }
/*     */        }
/*     */     else { return; }
/* 106 */      Inventory inv = e.getInventory();
/* 107 */     Map<Integer, ItemStack> items = new TreeMap<>();
/* 108 */     for (int i = 0; i < 36; i++) {
/* 109 */       ItemStack it = inv.getItem(i);
/* 110 */       if (it != null && it.getType() != Material.AIR) {
/* 111 */         items.put(Integer.valueOf((i < 27) ? (i + 9) : (i - 27)), it.clone());
/*     */       }
/*     */     } 
/* 114 */     ItemStack[] armor = new ItemStack[4];
/* 115 */     for (int j = 0; j < 4; j++) {
/* 116 */       ItemStack it = inv.getItem(36 + j);
/* 117 */       armor[j] = (it == null || it.getType() == Material.AIR) ? null : it.clone();
/*     */     } 
/* 119 */     this.plugin.getKitManager().set(items, armor);
/* 120 */     this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.GREEN) + "Kit saved (" + String.valueOf(ChatColor.GREEN) + " item stacks). It is given at match start and on respawn.");
/*     */   }
/*     */   
/*     */   private static ItemStack named(ItemStack it, String name, String... lore) {
/* 124 */     ItemMeta meta = it.getItemMeta();
/* 125 */     meta.setDisplayName(name);
/* 126 */     if (lore.length > 0) {
/* 127 */       meta.setLore(Arrays.asList(lore));
/*     */     }
/* 129 */     it.setItemMeta(meta);
/* 130 */     return it;
/*     */   }
/*     */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\kit\KitEditorGui.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */