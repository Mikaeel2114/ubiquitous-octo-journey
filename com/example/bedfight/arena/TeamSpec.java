/*    */ package com.example.bedfight.arena;
/*    */ 
/*    */ import org.bukkit.DyeColor;
/*    */ 
/*    */ public final class TeamSpec {
/*    */   private final DyeColor color;
/*    */   private Pos spawn;
/*    */   
/*    */   public TeamSpec(DyeColor color) {
/* 10 */     this.color = color;
/*    */   } private Pos bed; private Pos defense1; private Pos defense2; public DyeColor getColor() {
/* 12 */     return this.color; }
/* 13 */   public Pos getSpawn() { return this.spawn; }
/* 14 */   public void setSpawn(Pos spawn) { this.spawn = spawn; }
/* 15 */   public Pos getBed() { return this.bed; } public void setBed(Pos bed) {
/* 16 */     this.bed = bed;
/*    */   }
/*    */   
/*    */   public Pos getDefense1()
/*    */   {
/* 21 */     return this.defense1;
/* 22 */   } public void setDefense1(Pos p) { this.defense1 = p; }
/* 23 */   public Pos getDefense2() { return this.defense2; }
/* 24 */   public void setDefense2(Pos p) { this.defense2 = p; } public boolean hasDefense() {
/* 25 */     return (this.defense1 != null && this.defense2 != null);
/*    */   }
/*    */   
/*    */   public boolean inDefense(int x, int y, int z) {
/* 29 */     if (!hasDefense()) {
/* 30 */       return false;
/*    */     }
/* 32 */     return (x >= Math.min(this.defense1.blockX(), this.defense2.blockX()) && x <= Math.max(this.defense1.blockX(), this.defense2.blockX()) && y >= 
/* 33 */       Math.min(this.defense1.blockY(), this.defense2.blockY()) && y <= Math.max(this.defense1.blockY(), this.defense2.blockY()) && z >= 
/* 34 */       Math.min(this.defense1.blockZ(), this.defense2.blockZ()) && z <= Math.max(this.defense1.blockZ(), this.defense2.blockZ()));
/*    */   }
/*    */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\arena\TeamSpec.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */