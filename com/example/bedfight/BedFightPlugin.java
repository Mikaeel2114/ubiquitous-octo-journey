/*    */ package com.example.bedfight;
/*    */ import com.example.bedfight.arena.ArenaManager;
/*    */ import com.example.bedfight.command.BedFightCommand;
/*    */ import com.example.bedfight.game.GameManager;
/*    */ import com.example.bedfight.gui.PrivateMatchGui;
/*    */ import com.example.bedfight.hook.PartyHook;
/*    */ import com.example.bedfight.kit.KitEditorGui;
/*    */ import com.example.bedfight.kit.KitManager;
/*    */ import com.example.bedfight.listener.ArenaListener;
/*    */ import com.example.bedfight.scoreboard.ScoreboardManager;
/*    */ import org.bukkit.Bukkit;
/*    */ import org.bukkit.ChatColor;
/*    */ import org.bukkit.World;
/*    */ import org.bukkit.command.CommandExecutor;
/*    */ import org.bukkit.command.CommandSender;
/*    */ import org.bukkit.command.PluginCommand;
/*    */ import org.bukkit.command.TabCompleter;
/*    */ import org.bukkit.event.Listener;
/*    */ import org.bukkit.plugin.Plugin;
/*    */ 
/*    */ public final class BedFightPlugin extends JavaPlugin {
/* 22 */   public static final String PREFIX = String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.RED) + "BedFight " + String.valueOf(ChatColor.BOLD) + "» " + String.valueOf(ChatColor.GRAY);
/*    */   
/*    */   private PartyHook partyHook;
/*    */   
/*    */   private ArenaManager arenaManager;
/*    */   
/*    */   private GameManager gameManager;
/*    */   private PrivateMatchGui privateGui;
/*    */   private KitManager kitManager;
/*    */   private KitEditorGui kitEditor;
/*    */   private ScoreboardManager scoreboards;
/*    */   
/*    */   public void onEnable() {
/* 35 */     saveDefaultConfig();
/*    */     
/* 37 */     this.partyHook = new PartyHook(this);
/* 38 */     this.arenaManager = new ArenaManager(this);
/* 39 */     this.arenaManager.loadAll();
/* 40 */     this.gameManager = new GameManager(this);
/* 41 */     this.privateGui = new PrivateMatchGui(this);
/* 42 */     this.kitManager = new KitManager(this);
/* 43 */     this.kitEditor = new KitEditorGui(this);
/* 44 */     this.scoreboards = new ScoreboardManager(this);
/*    */     
/* 46 */     getServer().getPluginManager().registerEvents((Listener)new ArenaListener(this), (Plugin)this);
/* 47 */     getServer().getPluginManager().registerEvents((Listener)this.privateGui, (Plugin)this);
/* 48 */     getServer().getPluginManager().registerEvents((Listener)this.kitEditor, (Plugin)this);
/*    */     
/* 50 */     BedFightCommand command = new BedFightCommand(this);
/* 51 */     PluginCommand pc = getCommand("bedfight");
/* 52 */     pc.setExecutor((CommandExecutor)command);
/* 53 */     pc.setTabCompleter((TabCompleter)command);
/* 54 */     PluginCommand leave = getCommand("leave");
/* 55 */     leave.setExecutor((CommandExecutor)command);
/* 56 */     getCommand("adminkiteditor").setExecutor((CommandExecutor)command);
/* 57 */     getCommand("adminkiteditor").setTabCompleter((TabCompleter)command);
/* 58 */     getCommand("beddefend").setExecutor((CommandExecutor)command);
/* 59 */     getCommand("beddefend").setTabCompleter((TabCompleter)command);
/*    */     
/* 61 */     getLogger().info("Loaded " + this.arenaManager.all().size() + " arena(s). Party hook: " + (
/* 62 */         this.partyHook.isAvailable() ? "OK" : "NOT FOUND"));
/*    */   }
/*    */ 
/*    */   
/*    */   public void onDisable() {
/* 67 */     if (this.scoreboards != null) {
/* 68 */       this.scoreboards.stop();
/*    */     }
/* 70 */     if (this.gameManager != null) {
/* 71 */       this.gameManager.shutdown();
/*    */     }
/*    */   }
/*    */   
/*    */   public void msg(CommandSender to, String message) {
/* 76 */     to.sendMessage(PREFIX + PREFIX);
/*    */   }
/*    */ 
/*    */   
/*    */   public Location getLobby() {
/* 81 */     World w = Bukkit.getWorld(getConfig().getString("lobby-world", ""));
/* 82 */     if (w == null) {
/* 83 */       w = Bukkit.getWorlds().get(0);
/*    */     }
/* 85 */     return w.getSpawnLocation();
/*    */   }
/*    */   
/* 88 */   public PartyHook getPartyHook() { return this.partyHook; }
/* 89 */   public ArenaManager getArenaManager() { return this.arenaManager; }
/* 90 */   public GameManager getGameManager() { return this.gameManager; }
/* 91 */   public PrivateMatchGui getPrivateGui() { return this.privateGui; }
/* 92 */   public KitManager getKitManager() { return this.kitManager; }
/* 93 */   public KitEditorGui getKitEditor() { return this.kitEditor; } public ScoreboardManager getScoreboards() {
/* 94 */     return this.scoreboards;
/*    */   }
/*    */ }


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\BedFightPlugin.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */