/*    */ package com.example.bedfight.scoreboard;
/*    */ 
/*    */ import java.util.List;
/*    */ import org.bukkit.Bukkit;
/*    */ import org.bukkit.ChatColor;
/*    */ import org.bukkit.entity.Player;
/*    */ import org.bukkit.scoreboard.DisplaySlot;
/*    */ import org.bukkit.scoreboard.Objective;
/*    */ import org.bukkit.scoreboard.Scoreboard;
/*    */ import org.bukkit.scoreboard.Team;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ final class PlayerBoard
/*    */ {
/*    */   private static final char SECTION = '§';
/*    */   private final Scoreboard board;
/*    */   private final Objective objective;
/*    */   private int shown;
/*    */   
/*    */   PlayerBoard(Player p) {
/* 26 */     this.board = Bukkit.getScoreboardManager().getNewScoreboard();
/* 27 */     this.objective = this.board.registerNewObjective("bedfight", "dummy");
/* 28 */     this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
/* 29 */     p.setScoreboard(this.board);
/*    */   }
/*    */   
/*    */   void render(Player p, String title, List<String> lines) {
/* 33 */     if (p.getScoreboard() != this.board) {
/* 34 */       p.setScoreboard(this.board);
/*    */     }
/* 36 */     this.objective.setDisplayName(cut(title, 32));
/* 37 */     int n = Math.min(lines.size(), 15); int i;
/* 38 */     for (i = 0; i < n; i++) {
/* 39 */       String entry = entry(i);
/* 40 */       Team team = this.board.getTeam("l" + i);
/* 41 */       if (team == null) {
/* 42 */         team = this.board.registerNewTeam("l" + i);
/*    */       }
/* 44 */       if (!team.hasEntry(entry)) {
/* 45 */         team.addEntry(entry);
/*    */       }
/* 47 */       String[] parts = split(lines.get(i));
/* 48 */       if (!parts[0].equals(team.getPrefix())) {
/* 49 */         team.setPrefix(parts[0]);
/*    */       }
/* 51 */       if (!parts[1].equals(team.getSuffix())) {
/* 52 */         team.setSuffix(parts[1]);
/*    */       }
/* 54 */       this.objective.getScore(entry).setScore(n - i);
/*    */     } 
/* 56 */     for (i = n; i < this.shown; i++) {
/* 57 */       this.board.resetScores(entry(i));
/*    */     }
/* 59 */     this.shown = n;
/*    */   }
/*    */   
/*    */   private static String entry(int i) {
/* 63 */     return "§" + Integer.toHexString(i) + "§r";
/*    */   }
/*    */   
/*    */   private static String cut(String s, int max) {
/* 67 */     return (s.length() <= max) ? s : s.substring(0, max);
/*    */   }
/*    */   
/*    */   private static String[] split(String text) {
/* 71 */     text = cut(text, 32);
/* 72 */     if (text.length() <= 16) {
/* 73 */       return new String[] { text, "" };
/*    */     }
/* 75 */     String prefix = text.substring(0, 16);
/* 76 */     String rest = text.substring(16);
/* 77 */     if (prefix.charAt(15) == '§') {
/* 78 */       prefix = prefix.substring(0, 15);
/* 79 */       rest = "§" + rest;
/*    */     } 
/* 81 */     String suffix = cut(ChatColor.getLastColors(prefix) + ChatColor.getLastColors(prefix), 16);
/* 82 */     return new String[] { prefix, suffix };
/*    */   }
/*    */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\scoreboard\PlayerBoard.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */