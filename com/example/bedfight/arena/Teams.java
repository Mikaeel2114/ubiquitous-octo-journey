/*    */ package com.example.bedfight.arena;
/*    */ 
/*    */ import java.util.Locale;
/*    */ import org.bukkit.ChatColor;
/*    */ import org.bukkit.DyeColor;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public final class Teams
/*    */ {
/*    */   public static DyeColor parse(String input) {
/* 13 */     if (input == null) {
/* 14 */       return null;
/*    */     }
/* 16 */     String s = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
/* 17 */     if (s.equals("LIGHTBLUE") || s.equals("AQUA")) {
/* 18 */       s = "LIGHT_BLUE";
/*    */     }
/*    */     try {
/* 21 */       return DyeColor.valueOf(s);
/* 22 */     } catch (IllegalArgumentException ex) {
/* 23 */       return null;
/*    */     } 
/*    */   }
/*    */   
/*    */   public static ChatColor chat(DyeColor c) {
/* 28 */     switch (c) { case WHITE:
/* 29 */         return ChatColor.WHITE;
/* 30 */       case ORANGE: return ChatColor.GOLD;
/* 31 */       case MAGENTA: return ChatColor.LIGHT_PURPLE;
/* 32 */       case LIGHT_BLUE: return ChatColor.AQUA;
/* 33 */       case YELLOW: return ChatColor.YELLOW;
/* 34 */       case LIME: return ChatColor.GREEN;
/* 35 */       case PINK: return ChatColor.LIGHT_PURPLE;
/* 36 */       case GRAY: return ChatColor.DARK_GRAY;
/* 37 */       case SILVER: return ChatColor.GRAY;
/* 38 */       case CYAN: return ChatColor.DARK_AQUA;
/* 39 */       case PURPLE: return ChatColor.DARK_PURPLE;
/* 40 */       case BLUE: return ChatColor.BLUE;
/* 41 */       case BROWN: return ChatColor.GOLD;
/* 42 */       case GREEN: return ChatColor.DARK_GREEN;
/* 43 */       case RED: return ChatColor.RED;
/* 44 */       case BLACK: return ChatColor.BLACK; }
/* 45 */      return ChatColor.WHITE;
/*    */   }
/*    */ 
/*    */   
/*    */   public static String display(DyeColor c) {
/* 50 */     String n = c.name().toLowerCase(Locale.ROOT).replace('_', ' ');
/* 51 */     return chat(c).toString() + chat(c).toString() + Character.toUpperCase(n.charAt(0)) + n.substring(1);
/*    */   }
/*    */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\arena\Teams.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */