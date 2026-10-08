/*    */ package com.example.bedfight.util;
/*    */ 
/*    */ import org.bukkit.entity.Player;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public final class Ping
/*    */ {
/*    */   public static int get(Player p) {
/*    */     try {
/* 12 */       Object handle = p.getClass().getMethod("getHandle", new Class[0]).invoke(p, new Object[0]);
/* 13 */       return Math.max(0, handle.getClass().getField("ping").getInt(handle));
/* 14 */     } catch (Exception ex) {
/* 15 */       return 0;
/*    */     } 
/*    */   }
/*    */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfigh\\util\Ping.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */