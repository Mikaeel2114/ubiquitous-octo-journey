package com.example.bedfight.command;

import com.example.bedfight.BedFightPlugin;
import com.example.bedfight.arena.Arena;
import com.example.bedfight.arena.ArenaManager;
import com.example.bedfight.arena.Pos;
import com.example.bedfight.arena.TeamSpec;
import com.example.bedfight.arena.Teams;
import com.example.bedfight.game.Match;
import com.example.bedfight.game.Mode;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class BedFightCommand
  implements CommandExecutor, TabCompleter
{
  private static final String ADMIN = "bedfight.admin";
  private static final List<String> ADMIN_SUBS = List.of("setuparena", "po1", "po2", "setspawn", "createteam", "setteam", "setbed", "buildmode", "save", "reload");
  
  private final BedFightPlugin plugin;

  
  public BedFightCommand(BedFightPlugin plugin) {
    this.plugin = plugin;
  }
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    Player p;
    String err;
    if (sender instanceof Player) { p = (Player)sender; }
    else { sender.sendMessage("Players only.");
      return true; }
    
    ArenaManager arenas = this.plugin.getArenaManager();

    
    if (cmd.getName().equalsIgnoreCase("leave")) {
      return leave(p);
    }
    if (cmd.getName().equalsIgnoreCase("adminkiteditor")) {
      return kitEditor(p, args);
    }
    if (cmd.getName().equalsIgnoreCase("beddefend")) {
      return bedDefend(p, args);
    }

    
    if (args.length == 0) {
      return queue(p, Mode.parse(this.plugin.getConfig().getString("default-mode", "1v1")));
    }
    String sub = args[0].toLowerCase(Locale.ROOT);
    
    Mode asMode = Mode.parse(sub);
    if (asMode != null) {
      return queue(p, asMode);
    }
    if (sub.equals("leave")) {
      return leave(p);
    }
    if (sub.equals("private")) {
      if (!p.hasPermission("bedfight.rank.mvpplusplus")) {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Only MVP++ players can create private matches.");
        return true;
      } 
      if (this.plugin.getGameManager().isBusy(p)) {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "You are already in a queue or match.");
        return true;
      } 
      this.plugin.getPrivateGui().open(p);
      return true;
    } 

    
    if (sub.equals("spec")) {
      if (args.length < 2) {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Usage: /bedfight spec <player>");
        return true;
      }
      Player target = Bukkit.getPlayerExact(args[1]);
      if (target == null) {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Player '" + args[1] + "' is not online.");
        return true;
      }
      if (target.equals(p)) {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "You can't spectate yourself.");
        return true;
      }
      Match targetMatch = this.plugin.getGameManager().getMatch(target);
      if (targetMatch == null || targetMatch.getWorld() == null) {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + target.getName() + " is not in a running match.");
        return true;
      }
      Match currentMatch = this.plugin.getGameManager().getMatch(p);
      if (currentMatch != null) {
        currentMatch.onLeave(p);
      }
      this.plugin.getGameManager().leaveQueue(p);
      targetMatch.addSpectator(p);
      this.plugin.getScoreboards().refresh(p);
      return true;
    }
    
    if (!ADMIN_SUBS.contains(sub)) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Unknown sub-command. Usage: /bedfight [1v1|2v2|3v3|private|leave]");
      return true;
    } 
    if (!p.hasPermission("bedfight.admin")) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "You don't have permission to do that.");
      return true;
    } 
    
    if (sub.equals("setuparena")) {
      if (args.length < 2) {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Usage: /bedfight setuparena <world_name>");
        return true;
      } 
      String str = arenas.setupArena(p, args[1]);
      if (str != null) {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.RED));
      } else {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.GREEN) + "World '" + String.valueOf(ChatColor.GREEN) + "' loaded. You are now setting up arena '" + args[1] + "'. Next: /bedfight po1, po2, setspawn, createteam, setteam, setbed, save.");
      } 
      
      return true;
    } 
    
    if (sub.equals("buildmode")) {
      if (arenas.toggleBuildMode(p)) {
        p.sendMessage("you currently buildmode");
      } else {
        this.plugin.msg((CommandSender)p, "Build mode disabled. The map is protected again.");
      } 
      return true;
    } 
    
    if (sub.equals("reload")) {
      this.plugin.reloadConfig();
      arenas.loadAll();
      this.plugin.getKitManager().load();
      this.plugin.getScoreboards().reload();
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.GREEN) + "Reloaded config.yml, scoreboard.yml, kit.yml and arenas.");
      return true;
    } 
    
    Arena arena = arenas.session(p);
    if (arena == null) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Start with /bedfight setuparena <world_name> first.");
      return true;
    } 
    
    switch (sub) {
      case "po1":
      case "po2":
        setBound(p, arena, sub.equals("po1"));
        break;
      case "setspawn":
        arena.setSpawn(Pos.of(p.getLocation()));
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.GREEN) + "Game spawn set.");
        break;
      case "createteam":
        createTeam(p, arena, args);
        break;
      case "setteam":
        setTeamSpawn(p, arena, args);
        break;
      case "setbed":
        setBed(p, arena, args);
        break;
      case "save":
        err = arenas.save(p);
        this.plugin.msg((CommandSender)p, (err == null) ? (
            String.valueOf(ChatColor.GREEN) + "Arena '" + String.valueOf(ChatColor.GREEN) + "' saved to arenas/" + arena.getName() + ".yml") : (
            String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.RED)));
        break;
    } 


    
    return true;
  }



  
  private boolean leave(Player p) {
    if (this.plugin.getGameManager().leaveQueue(p)) {
      this.plugin.msg((CommandSender)p, "You left the queue.");
      return true;
    } 
    Match m = this.plugin.getGameManager().getMatch(p);
    if (m != null) {
      m.onLeave(p);
      this.plugin.msg((CommandSender)p, "You left the BedFight match.");
      return true;
    } 
    this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "You are not in a BedFight queue or match.");
    return true;
  }
  
  private boolean queue(Player p, Mode mode) {
    if (!p.hasPermission("bedfight.play")) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "You don't have permission to play BedFight.");
      return true;
    } 
    this.plugin.getGameManager().joinQueue(p, (mode == null) ? Mode.SOLO : mode);
    return true;
  }



  
  private boolean kitEditor(Player p, String[] args) {
    if (!p.hasPermission("bedfight.admin")) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "You don't have permission to do that.");
      return true;
    } 
    if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
      this.plugin.getKitManager().resetToDefault();
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.GREEN) + "Kit reset to the default (iron sword, 64 wool, shears, golden apple, leather armor).");
      return true;
    } 
    this.plugin.getKitEditor().open(p);
    return true;
  }




  
  private boolean bedDefend(Player p, String[] args) {
    if (!p.hasPermission("bedfight.admin")) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "You don't have permission to do that.");
      return true;
    } 
    Arena arena = this.plugin.getArenaManager().session(p);
    if (arena == null) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Start with /bedfight setuparena <world_name> first.");
      return true;
    } 
    String op = (args.length == 0) ? "" : args[0].toLowerCase(Locale.ROOT);
    if (!op.equals("po1") && !op.equals("po2") && !op.equals("clear")) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Usage: /beddefend <po1|po2|clear> [color]");
      return true;
    } 
    DyeColor c = resolveTeam(p, arena, args);
    if (c == null) {
      return true;
    }
    TeamSpec t = arena.team(c, true);
    if (op.equals("clear")) {
      t.setDefense1(null);
      t.setDefense2(null);
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.GREEN) + "Bed defense region of team " + String.valueOf(ChatColor.GREEN) + Teams.display(c) + " cleared.");
      return true;
    } 
    Pos pos = Pos.ofBlock(p.getLocation().getBlock());
    if (op.equals("po1")) {
      t.setDefense1(pos);
    } else {
      t.setDefense2(pos);
    } 
    
    String msg = String.valueOf(ChatColor.GREEN) + "Bed defense " + String.valueOf(ChatColor.GREEN) + " of team " + op + Teams.display(c) + " set to " + String.valueOf(ChatColor.GREEN) + ", " + pos.blockX() + ", " + pos.blockY() + ".";
    if (t.hasDefense()) {
      int dx = Math.abs(t.getDefense1().blockX() - t.getDefense2().blockX()) + 1;
      int dy = Math.abs(t.getDefense1().blockY() - t.getDefense2().blockY()) + 1;
      int dz = Math.abs(t.getDefense1().blockZ() - t.getDefense2().blockZ()) + 1;
      msg = msg + msg + " Region " + String.valueOf(ChatColor.GRAY) + "x" + dx + "x" + dy + ": blocks inside can be broken during matches. Don't forget /bedfight save.";
    } else {
      
      msg = msg + msg + " Now set the opposite corner with /beddefend " + String.valueOf(ChatColor.GRAY) + ".";
    } 
    this.plugin.msg((CommandSender)p, msg);
    return true;
  }
  
  private void setBound(Player p, Arena arena, boolean first) {
    Pos pos = Pos.ofBlock(p.getLocation().getBlock());
    if (first) {
      arena.setPos1(pos);
    } else {
      arena.setPos2(pos);
    } 
    
    String msg = String.valueOf(ChatColor.GREEN) + String.valueOf(ChatColor.GREEN) + " set to " + (first ? "po1" : "po2") + ", " + pos.blockX() + ", " + pos.blockY() + ".";
    if (arena.hasBounds())
    {
      msg = msg + msg + " Void level (min Y) = " + String.valueOf(ChatColor.GRAY) + String.valueOf(ChatColor.WHITE) + arena.getMinY() + ", max build height (max Y) = " + String.valueOf(ChatColor.GRAY) + String.valueOf(ChatColor.WHITE) + ".";
    }
    this.plugin.msg((CommandSender)p, msg);
    if (arena.hasBounds() && arena.getMaxY() - arena.getMinY() < 8) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.YELLOW) + "Warning: only " + String.valueOf(ChatColor.YELLOW) + " blocks between void level and build limit. Players can't build above Y " + arena.getMaxY() - arena.getMinY() + " - set one point at the lowest void level and one high above the islands.");
    }
  }


  
  private void createTeam(Player p, Arena arena, String[] args) {
    if (args.length < 2) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Usage: /bedfight createteam <color>");
      return;
    } 
    DyeColor c = Teams.parse(args[1]);
    if (c == null) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Unknown color. Valid: " + String.valueOf(ChatColor.RED));
      return;
    } 
    boolean existed = arena.getTeams().containsKey(c);
    arena.team(c, true);
    this.plugin.getArenaManager().select(p, c);
    this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.GREEN) + "Team " + String.valueOf(ChatColor.GREEN) + Teams.display(c) + String.valueOf(ChatColor.GREEN) + " Now use /bedfight setteam and /bedfight setbed.");
  }


  
  private void setTeamSpawn(Player p, Arena arena, String[] args) {
    DyeColor c = resolveTeam(p, arena, args);
    if (c == null) {
      return;
    }
    Location loc = p.getLocation();
    Block under = loc.getBlock().getRelative(0, -1, 0);
    if (under.getType() == Material.AIR) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Stand on a solid block - the block under your feet is the spawn.");
      
      return;
    } 
    arena.team(c, true).setSpawn(new Pos(under.getX() + 0.5D, (under.getY() + 1), under.getZ() + 0.5D, loc
          .getYaw(), 0.0F));
    this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.GREEN) + "Spawn of team " + String.valueOf(ChatColor.GREEN) + Teams.display(c) + " set to block " + String.valueOf(ChatColor.GREEN) + ", " + under
        .getX() + ", " + under.getY() + ".");
  }
  
  private void setBed(Player p, Arena arena, String[] args) {
    DyeColor c = resolveTeam(p, arena, args);
    if (c == null) {
      return;
    }
    Block bed = p.getTargetBlock((HashSet)null, 6);
    if (bed == null || bed.getType() != Material.BED_BLOCK) {
      Block feet = p.getLocation().getBlock();
      bed = (feet.getType() == Material.BED_BLOCK) ? feet : null;
    } 
    if (bed == null) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "Look at the team's bed (within 6 blocks) and run the command again.");
      return;
    } 
    arena.team(c, true).setBed(Pos.ofBlock(bed));
    this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.GREEN) + "Bed of team " + String.valueOf(ChatColor.GREEN) + Teams.display(c) + " registered at " + String.valueOf(ChatColor.GREEN) + ", " + bed
        .getX() + ", " + bed.getY() + ".");
  }

  
  private DyeColor resolveTeam(Player p, Arena arena, String[] args) {
    if (args.length >= 2) {
      DyeColor dyeColor = Teams.parse(args[1]);
      if (dyeColor == null || !arena.getTeams().containsKey(dyeColor)) {
        this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "That team doesn't exist. Use /bedfight createteam <color> first.");
        return null;
      } 
      this.plugin.getArenaManager().select(p, dyeColor);
      return dyeColor;
    } 
    DyeColor c = this.plugin.getArenaManager().selected(p);
    if (c == null || !arena.getTeams().containsKey(c)) {
      this.plugin.msg((CommandSender)p, String.valueOf(ChatColor.RED) + "No team selected. Use /bedfight createteam <color> or pass the color.");
      return null;
    } 
    return c;
  }
  
  private static String colorList() {
    List<String> l = new ArrayList<>();
    for (DyeColor c : DyeColor.values()) {
      l.add(c.name().toLowerCase(Locale.ROOT));
    }
    return String.join(", ", (Iterable)l);
  }


  
  public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
    Player p;
    List<String> out = new ArrayList<>();
    if (sender instanceof Player) { p = (Player)sender; }
    else { return out; }
    
    String cn = cmd.getName().toLowerCase(Locale.ROOT);
    if (cn.equals("leave")) {
      return out;
    }
    if (cn.equals("adminkiteditor")) {
      if (args.length == 1) {
        out.add("reset");
      }
      return filter(out, args);
    } 
    if (cn.equals("beddefend")) {
      if (args.length == 1) {
        out.addAll(List.of("po1", "po2", "clear"));
      } else if (args.length == 2) {
        Arena a = this.plugin.getArenaManager().session(p);
        if (a != null) {
          for (DyeColor c : a.getTeams().keySet()) {
            out.add(c.name().toLowerCase(Locale.ROOT));
          }
        }
      } 
      return filter(out, args);
    } 
    if (args.length == 1) {
      out.addAll(List.of("1v1", "2v2", "3v3", "spec", "leave"));
      if (p.hasPermission("bedfight.rank.mvpplusplus")) {
        out.add("private");
      }
      if (p.hasPermission("bedfight.admin")) {
        out.addAll(ADMIN_SUBS);
      }
    } else if (args.length == 2 && args[0].equalsIgnoreCase("spec")) {
      for (Player online : Bukkit.getOnlinePlayers()) {
        if (this.plugin.getGameManager().getMatch(online) != null) {
          out.add(online.getName());
        }
      }
    } else if (args.length == 2 && p.hasPermission("bedfight.admin")) {
      String sub = args[0].toLowerCase(Locale.ROOT);
      if (sub.equals("createteam")) {
        for (DyeColor c : DyeColor.values()) {
          out.add(c.name().toLowerCase(Locale.ROOT));
        }
      } else if (sub.equals("setteam") || sub.equals("setbed")) {
        Arena a = this.plugin.getArenaManager().session(p);
        if (a != null) {
          for (DyeColor c : a.getTeams().keySet()) {
            out.add(c.name().toLowerCase(Locale.ROOT));
          }
        }
      } else if (sub.equals("setuparena")) {
        HashSet<String> names = new HashSet<>();
        for (World w : Bukkit.getWorlds()) {
          names.add(w.getName());
        }
        File[] dirs = Bukkit.getWorldContainer().listFiles(f -> 
            (f.isDirectory() && (new File(f, "level.dat")).isFile()));
        if (dirs != null) {
          for (File f : dirs) {
            names.add(f.getName());
          }
        }
        out.addAll(names);
      } 
    } 
    return filter(out, args);
  }
  
  private static List<String> filter(List<String> out, String[] args) {
    String prefix = (args.length == 0) ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
    out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(prefix));
    return out;
  }
}


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\command\BedFightCommand.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */