/*    */ package com.example.bedfight.game;
/*    */ 
/*    */ import com.example.bedfight.arena.Pos;
/*    */ import java.util.HashSet;
/*    */ import java.util.LinkedHashSet;
/*    */ import java.util.Set;
/*    */ import java.util.UUID;
/*    */ import org.bukkit.DyeColor;
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
/*    */ final class TeamState
/*    */ {
/*    */   final DyeColor color;
/*    */   final Pos spawn;
/*    */   final Pos bed;
/* 66 */   final Set<UUID> members = new LinkedHashSet<>();
/* 67 */   final Set<UUID> alive = new HashSet<>();
/*    */   boolean bedAlive = true;
/*    */   
/*    */   TeamState(DyeColor color, Pos spawn, Pos bed) {
/* 71 */     this.color = color;
/* 72 */     this.spawn = spawn;
/* 73 */     this.bed = bed;
/*    */   }
/*    */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\game\Match$TeamState.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */