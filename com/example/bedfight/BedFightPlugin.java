package com.example.bedfight;
import com.example.bedfight.arena.ArenaManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Location;
import com.example.bedfight.command.BedFightCommand;
import com.example.bedfight.game.GameManager;
import com.example.bedfight.gui.PrivateMatchGui;
import com.example.bedfight.hook.PartyHook;
import com.example.bedfight.kit.KitEditorGui;
import com.example.bedfight.kit.KitManager;
import com.example.bedfight.listener.ArenaListener;
import com.example.bedfight.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class BedFightPlugin extends JavaPlugin {
  public static final String PREFIX = String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.RED) + "BedFight " + String.valueOf(ChatColor.BOLD) + "» " + String.valueOf(ChatColor.GRAY);
  
  private PartyHook partyHook;
  
  private ArenaManager arenaManager;
  
  private GameManager gameManager;
  private PrivateMatchGui privateGui;
  private KitManager kitManager;
  private KitEditorGui kitEditor;
  private ScoreboardManager scoreboards;
  
  public void onEnable() {
    saveDefaultConfig();
    
    this.partyHook = new PartyHook(this);
    this.arenaManager = new ArenaManager(this);
    this.arenaManager.loadAll();
    this.gameManager = new GameManager(this);
    this.privateGui = new PrivateMatchGui(this);
    this.kitManager = new KitManager(this);
    this.kitEditor = new KitEditorGui(this);
    this.scoreboards = new ScoreboardManager(this);
    
    getServer().getPluginManager().registerEvents((Listener)new ArenaListener(this), (Plugin)this);
    getServer().getPluginManager().registerEvents((Listener)this.privateGui, (Plugin)this);
    getServer().getPluginManager().registerEvents((Listener)this.kitEditor, (Plugin)this);
    
    BedFightCommand command = new BedFightCommand(this);
    PluginCommand pc = getCommand("bedfight");
    pc.setExecutor((CommandExecutor)command);
    pc.setTabCompleter((TabCompleter)command);
    PluginCommand leave = getCommand("leave");
    leave.setExecutor((CommandExecutor)command);
    getCommand("adminkiteditor").setExecutor((CommandExecutor)command);
    getCommand("adminkiteditor").setTabCompleter((TabCompleter)command);
    getCommand("beddefend").setExecutor((CommandExecutor)command);
    getCommand("beddefend").setTabCompleter((TabCompleter)command);
    
    getLogger().info("Loaded " + this.arenaManager.all().size() + " arena(s). Party hook: " + (
        this.partyHook.isAvailable() ? "OK" : "NOT FOUND"));
  }

  
  public void onDisable() {
    if (this.scoreboards != null) {
      this.scoreboards.stop();
    }
    if (this.gameManager != null) {
      this.gameManager.shutdown();
    }
  }
  
  public void msg(CommandSender to, String message) {
    to.sendMessage(PREFIX + message);
  }

  
  public Location getLobby() {
    World w = Bukkit.getWorld(getConfig().getString("lobby-world", ""));
    if (w == null) {
      w = Bukkit.getWorlds().get(0);
    }
    return w.getSpawnLocation();
  }
  
  public PartyHook getPartyHook() { return this.partyHook; }
  public ArenaManager getArenaManager() { return this.arenaManager; }
  public GameManager getGameManager() { return this.gameManager; }
  public PrivateMatchGui getPrivateGui() { return this.privateGui; }
  public KitManager getKitManager() { return this.kitManager; }
  public KitEditorGui getKitEditor() { return this.kitEditor; } public ScoreboardManager getScoreboards() {
    return this.scoreboards;
  }
}


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\BedFightPlugin.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */