package com.example.bedfight.scoreboard;

import com.example.bedfight.BedFightPlugin;
import com.example.bedfight.arena.Teams;
import com.example.bedfight.game.GameManager;
import com.example.bedfight.game.Match;
import com.example.bedfight.game.Mode;
import com.example.bedfight.util.Ping;
import com.example.bedfight.util.YamlFiles;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;


public final class ScoreboardManager
{
  private final BedFightPlugin plugin;
  private final File file;
  private YamlConfiguration cfg;
  private final Map<UUID, PlayerBoard> boards = new HashMap<>();
  private BukkitTask task;
  
  public ScoreboardManager(BedFightPlugin plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "scoreboard.yml");
    if (!this.file.isFile()) {
      plugin.saveResource("scoreboard.yml", false);
    }
    reload();
  }
  
  public void reload() {
    stop();
    this.cfg = YamlFiles.load(this.file);
    if (this.cfg.getBoolean("enabled", true)) {
      long period = Math.max(2L, this.cfg.getLong("update-ticks", 20L));
      this.task = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::tick, 20L, period);
    } 
  }

  
  public void stop() {
    if (this.task != null) {
      this.task.cancel();
      this.task = null;
    } 
    for (UUID id : new ArrayList(this.boards.keySet())) {
      Player p = Bukkit.getPlayer(id);
      if (p != null) {
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
      }
    } 
    this.boards.clear();
  }
  
  private void tick() {
    GameManager games = this.plugin.getGameManager();
    Set<UUID> active = games.activePlayers();
    for (UUID id : active) {
      String title; List<String> lines; Player p = Bukkit.getPlayer(id);
      if (p == null) {
        continue;
      }
      Match m = games.getMatch(p);
      Match sm = games.getSpectating(p);

      
      if (m != null) {
        title = this.cfg.getString("match.title", "&b&lBedFight");
        lines = matchLines(p, m);
      } else if (sm != null) {
        title = this.cfg.getString("spectator.title", this.cfg.getString("match.title", "&b&lBedFight"));
        lines = spectatorLines(p, sm);
      } else if (games.getQueuedMode(p) != null) {
        title = this.cfg.getString("queue.title", "&b&lBedFight");
        lines = queueLines(p, games);
      } else {
        continue;
      } 
      PlayerBoard board = this.boards.computeIfAbsent(id, k -> new PlayerBoard(p));
      board.render(p, color(title), lines);
    } 
    for (UUID id : new ArrayList(this.boards.keySet())) {
      if (!active.contains(id)) {
        Player p = Bukkit.getPlayer(id);
        if (p != null) {
          p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        this.boards.remove(id);
      } 
    } 
  }


  
  public void refresh(Player p) {
    if (p == null) return;
    PlayerBoard old = this.boards.remove(p.getUniqueId());
    if (old != null) {
      p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
    tick();
  }

  private List<String> spectatorLines(Player p, Match m) {
    Map<String, String> ph = m.placeholders(p);
    ph.put("%ip%", this.cfg.getString("ip", "minemen.club"));
    ph.put("%status%", "Spectating");
    List<String> out = new ArrayList<String>();
    List<String> template = this.cfg.getStringList("spectator.lines");
    if (template == null || template.isEmpty()) {
      template = new ArrayList<String>();
      template.add("&7&m------------");
      template.add("&fMode: &b%mode%");
      template.add("&fMap: &b%map%");
      template.add("");
      template.add("&fStatus: &eSpectating");
      template.add("&fTime: &b%time%");
      template.add("");
      template.add("&fYour Ping: &a%ping%ms");
      template.add("");
      template.add("&b%ip%");
    }
    for (String raw : template) {
      out.add(color(replace(raw, ph)));
    }
    return out;
  }

  private List<String> matchLines(Player p, Match m) {
    Map<String, String> ph = m.placeholders(p);
    ph.put("%ip%", this.cfg.getString("ip", "minemen.club"));
    List<String> out = new ArrayList<>();
    for (String raw : this.cfg.getStringList("match.lines")) {
      if (raw.contains("%teams%")) {
        for (Match.TeamView tv : m.teamViews(p))
          out.add(color(teamLine(tv))); 
        continue;
      } 
      out.add(color(replace(raw, ph)));
    } 
    
    return out;
  }
  
  private String teamLine(Match.TeamView tv) {
    String status, name = tv.color().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    name = "" + Character.toUpperCase(name.charAt(0)) + Character.toUpperCase(name.charAt(0));
    
    if (tv.bedAlive()) {
      status = this.cfg.getString("match.bed-alive", "&a✔");
    } else if (tv.alive() > 0) {
      status = this.cfg.getString("match.bed-dead", "&a%alive%").replace("%alive%", String.valueOf(tv.alive()));
    } else {
      status = this.cfg.getString("match.eliminated", "&c✘");
    } 
    Map<String, String> ph = new HashMap<>();
    ph.put("%color%", Teams.chat(tv.color()).toString());
    ph.put("%letter%", name.substring(0, 1));
    ph.put("%name%", name);
    ph.put("%team_status%", status);
    ph.put("%you%", tv.mine() ? this.cfg.getString("match.you", " &7YOU") : "");
    return replace(this.cfg.getString("match.team-line", "%color%%letter% &f%name%&7: %team_status%%you%"), ph);
  }
  
  private List<String> queueLines(Player p, GameManager games) {
    Mode mode = games.getQueuedMode(p);
    long secs = Math.max(0L, (System.currentTimeMillis() - games.getQueuedSince(p)) / 1000L);
    Map<String, String> ph = new HashMap<>();
    ph.put("%player%", p.getName());
    ph.put("%mode%", mode.label());
    ph.put("%queued%", String.valueOf(games.getQueueSize(mode)));
    ph.put("%needed%", String.valueOf(mode.totalPlayers()));
    ph.put("%queue_time%", String.format("%d:%02d", new Object[] { Long.valueOf(secs / 60L), Long.valueOf(secs % 60L) }));
    ph.put("%ping%", String.valueOf(Ping.get(p)));
    ph.put("%ip%", this.cfg.getString("ip", "minemen.club"));
    List<String> out = new ArrayList<>();
    for (String raw : this.cfg.getStringList("queue.lines")) {
      out.add(color(replace(raw, ph)));
    }
    return out;
  }
  
  private static String replace(String s, Map<String, String> ph) {
    for (Map.Entry<String, String> e : ph.entrySet()) {
      s = s.replace(e.getKey(), e.getValue());
    }
    return s;
  }
  
  private static String color(String s) {
    return ChatColor.translateAlternateColorCodes('&', s);
  }
}


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\scoreboard\ScoreboardManager.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */