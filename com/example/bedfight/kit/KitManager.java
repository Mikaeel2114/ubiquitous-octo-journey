/*     */ package com.example.bedfight.kit;
/*     */ 
/*     */ import com.example.bedfight.BedFightPlugin;
/*     */ import com.example.bedfight.util.YamlFiles;
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.util.Map;
/*     */ import java.util.TreeMap;
/*     */ import java.util.logging.Level;
/*     */ import org.bukkit.DyeColor;
/*     */ import org.bukkit.Material;
/*     */ import org.bukkit.configuration.ConfigurationSection;
/*     */ import org.bukkit.configuration.file.YamlConfiguration;
/*     */ import org.bukkit.entity.Player;
/*     */ import org.bukkit.inventory.ItemStack;
/*     */ import org.bukkit.inventory.PlayerInventory;
/*     */ import org.bukkit.inventory.meta.ItemMeta;
/*     */ import org.bukkit.inventory.meta.LeatherArmorMeta;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public final class KitManager
/*     */ {
/*  28 */   private static final String[] ARMOR_KEYS = new String[] { "helmet", "chestplate", "leggings", "boots" };
/*     */   
/*     */   private final BedFightPlugin plugin;
/*     */   
/*     */   private final File file;
/*  33 */   private final Map<Integer, ItemStack> items = new TreeMap<>();
/*     */   
/*  35 */   private final ItemStack[] armor = new ItemStack[4];
/*     */   
/*     */   public KitManager(BedFightPlugin plugin) {
/*  38 */     this.plugin = plugin;
/*  39 */     this.file = new File(plugin.getDataFolder(), "kit.yml");
/*  40 */     load();
/*     */   }
/*     */   
/*     */   public void load() {
/*  44 */     if (!this.file.isFile()) {
/*  45 */       resetToDefault();
/*     */       return;
/*     */     } 
/*  48 */     this.items.clear();
/*  49 */     for (int i = 0; i < 4; i++) {
/*  50 */       this.armor[i] = null;
/*     */     }
/*  52 */     YamlConfiguration y = YamlFiles.load(this.file);
/*  53 */     ConfigurationSection slots = y.getConfigurationSection("slots");
/*  54 */     if (slots != null) {
/*  55 */       for (String key : slots.getKeys(false)) {
/*     */         try {
/*  57 */           int slot = Integer.parseInt(key);
/*  58 */           ItemStack it = slots.getItemStack(key);
/*  59 */           if (it != null && slot >= 0 && slot < 36) {
/*  60 */             this.items.put(Integer.valueOf(slot), it);
/*     */           }
/*  62 */         } catch (NumberFormatException numberFormatException) {}
/*     */       } 
/*     */     }
/*     */ 
/*     */     
/*  67 */     for (int j = 0; j < 4; j++) {
/*  68 */       this.armor[j] = y.getItemStack("armor." + ARMOR_KEYS[j]);
/*     */     }
/*     */   }
/*     */ 
/*     */   
/*     */   public void resetToDefault() {
/*  74 */     this.items.clear();
/*  75 */     this.items.put(Integer.valueOf(0), new ItemStack(Material.IRON_SWORD));
/*  76 */     this.items.put(Integer.valueOf(1), new ItemStack(Material.WOOL, 64));
/*  77 */     this.items.put(Integer.valueOf(2), new ItemStack(Material.SHEARS));
/*  78 */     this.items.put(Integer.valueOf(3), new ItemStack(Material.GOLDEN_APPLE, 1));
/*  79 */     this.armor[0] = new ItemStack(Material.LEATHER_HELMET);
/*  80 */     this.armor[1] = new ItemStack(Material.LEATHER_CHESTPLATE);
/*  81 */     this.armor[2] = new ItemStack(Material.LEATHER_LEGGINGS);
/*  82 */     this.armor[3] = new ItemStack(Material.LEATHER_BOOTS);
/*  83 */     save();
/*     */   }
/*     */   public Map<Integer, ItemStack> getItems() {
/*  86 */     return this.items;
/*     */   } public ItemStack[] getArmor() {
/*  88 */     return this.armor;
/*     */   }
/*     */   
/*     */   public void set(Map<Integer, ItemStack> newItems, ItemStack[] newArmor) {
/*  92 */     this.items.clear();
/*  93 */     for (Map.Entry<Integer, ItemStack> e : newItems.entrySet()) {
/*  94 */       this.items.put(e.getKey(), ((ItemStack)e.getValue()).clone());
/*     */     }
/*  96 */     for (int i = 0; i < 4; i++) {
/*  97 */       this.armor[i] = (newArmor[i] == null) ? null : newArmor[i].clone();
/*     */     }
/*  99 */     save();
/*     */   }
/*     */   
/*     */   private void save() {
/* 103 */     YamlConfiguration y = new YamlConfiguration();
/* 104 */     for (Map.Entry<Integer, ItemStack> e : this.items.entrySet()) {
/* 105 */       y.set("slots." + String.valueOf(e.getKey()), e.getValue());
/*     */     }
/* 107 */     for (int i = 0; i < 4; i++) {
/* 108 */       if (this.armor[i] != null) {
/* 109 */         y.set("armor." + ARMOR_KEYS[i], this.armor[i]);
/*     */       }
/*     */     } 
/*     */     try {
/* 113 */       YamlFiles.save(y, this.file);
/* 114 */     } catch (IOException ex) {
/* 115 */       this.plugin.getLogger().log(Level.SEVERE, "Could not save kit.yml", ex);
/*     */     } 
/*     */   }
/*     */ 
/*     */   
/*     */   public void apply(Player p, DyeColor color) {
/* 121 */     PlayerInventory inv = p.getInventory();
/* 122 */     inv.clear();
/* 123 */     inv.setArmorContents(new ItemStack[4]);
/* 124 */     for (Map.Entry<Integer, ItemStack> e : this.items.entrySet()) {
/* 125 */       inv.setItem(((Integer)e.getKey()).intValue(), tint(e.getValue(), color));
/*     */     }
/* 127 */     inv.setHelmet((this.armor[0] == null) ? null : tint(this.armor[0], color));
/* 128 */     inv.setChestplate((this.armor[1] == null) ? null : tint(this.armor[1], color));
/* 129 */     inv.setLeggings((this.armor[2] == null) ? null : tint(this.armor[2], color));
/* 130 */     inv.setBoots((this.armor[3] == null) ? null : tint(this.armor[3], color));
/* 131 */     inv.setHeldItemSlot(0);
/*     */   }
/*     */   
/*     */   private static ItemStack tint(ItemStack src, DyeColor color) {
/* 135 */     ItemStack it = src.clone();
/* 136 */     Material m = it.getType();
/* 137 */     if (m == Material.WOOL || m == Material.STAINED_CLAY || m == Material.STAINED_GLASS || m == Material.STAINED_GLASS_PANE || m == Material.CARPET) {
/*     */       
/* 139 */       it.setDurability((short)color.getWoolData());
/* 140 */     } else if (m.name().startsWith("LEATHER_")) {
/* 141 */       ItemMeta meta = it.getItemMeta();
/* 142 */       if (meta instanceof LeatherArmorMeta) { LeatherArmorMeta lm = (LeatherArmorMeta)meta;
/* 143 */         lm.setColor(color.getColor());
/* 144 */         it.setItemMeta((ItemMeta)lm); }
/*     */     
/*     */     } 
/* 147 */     return it;
/*     */   }
/*     */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\kit\KitManager.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */