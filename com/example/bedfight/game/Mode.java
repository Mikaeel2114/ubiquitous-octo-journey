/*    */ package com.example.bedfight.game;
/*    */ 
/*    */ import java.util.Locale;
/*    */ 
/*    */ public enum Mode {
/*  6 */   SOLO(1, "1v1"),
/*  7 */   DUO(2, "2v2"),
/*  8 */   TRIO(3, "3v3");
/*    */   
/*    */   private final int teamSize;
/*    */   private final String label;
/*    */   
/*    */   Mode(int teamSize, String label) {
/* 14 */     this.teamSize = teamSize;
/* 15 */     this.label = label;
/*    */   }
/*    */   
/* 18 */   public int teamSize() { return this.teamSize; } public String label() {
/* 19 */     return this.label;
/*    */   } public int totalPlayers() {
/* 21 */     return this.teamSize * 2;
/*    */   }
/*    */   public static Mode parse(String s) {
/* 24 */     if (s == null) {
/* 25 */       return null;
/*    */     }
/* 27 */     String l = s.toLowerCase(Locale.ROOT);
/* 28 */     for (Mode m : values()) {
/* 29 */       if (m.label.equals(l)) {
/* 30 */         return m;
/*    */       }
/*    */     } 
/* 33 */     return null;
/*    */   }
/*    */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\game\Mode.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */