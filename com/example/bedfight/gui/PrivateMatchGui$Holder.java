/*    */ package com.example.bedfight.gui;
/*    */ 
/*    */ import java.util.List;
/*    */ import org.bukkit.inventory.Inventory;
/*    */ import org.bukkit.inventory.InventoryHolder;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ final class Holder
/*    */   implements InventoryHolder
/*    */ {
/*    */   final PrivateMatchGui.Type type;
/*    */   final List<String> arenas;
/*    */   final String arena;
/*    */   Inventory inventory;
/*    */   
/*    */   Holder(PrivateMatchGui.Type type, List<String> arenas, String arena) {
/* 42 */     this.type = type;
/* 43 */     this.arenas = arenas;
/* 44 */     this.arena = arena;
/*    */   }
/*    */   
/*    */   public Inventory getInventory() {
/* 48 */     return this.inventory;
/*    */   }
/*    */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\gui\PrivateMatchGui$Holder.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */