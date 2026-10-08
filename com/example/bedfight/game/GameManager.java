package com.example.bedfight.game;

import com.example.bedfight.BedFightPlugin;
import com.example.bedfight.arena.Arena;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GameManager
{
  private final BedFightPlugin plugin;
  private final Random random = new Random();
  private final Map<Mode, LinkedHashSet<UUID>> queues = new EnumMap<>(Mode.class);
  private final Map<UUID, Mode> queued = new HashMap<>();
  private final Map<UUID, Long> queuedAt = new HashMap<>();
  private final Set<Match> matches = new LinkedHashSet<>();
  private final Map<UUID, Match> byPlayer = new HashMap<>();
  private final Map<String, Match> byWorld = new HashMap<>();
  private final Map<UUID, Match> bySpectator = new HashMap<>();
  
  public GameManager(BedFightPlugin plugin) {
    this.plugin = plugin;
    for (Mode m : Mode.values()) {
      this.queues.put(m, new LinkedHashSet<>());
    }
  }

  
  public Match getSpectating(Player p) {
    return this.bySpectator.get(p.getUniqueId());
  }
  public Set<UUID> getSpectatorIds() {
    return new HashSet<>(this.bySpectator.keySet());
  }
  void attachSpectator(Match m, Player p) {
    this.bySpectator.put(p.getUniqueId(), m);
  }
  void detachSpectator(Player p) {
    this.bySpectator.remove(p.getUniqueId());
  }
  public Match getMatch(Player p) {
    return this.byPlayer.get(p.getUniqueId());
  } public Match getMatchByWorld(World w) {
    return this.byWorld.get(w.getName());
  } public Mode getQueuedMode(Player p) {
    return this.queued.get(p.getUniqueId());
  } public int getQueueSize(Mode m) {
    return ((LinkedHashSet)this.queues.get(m)).size();
  }
  public long getQueuedSince(Player p) {
    Long t = this.queuedAt.get(p.getUniqueId());
    return (t == null) ? System.currentTimeMillis() : t.longValue();
  }

  
  public Set<UUID> activePlayers() {
    Set<UUID> s = new HashSet<>(this.byPlayer.keySet());
    s.addAll(this.queued.keySet());
    s.addAll(this.bySpectator.keySet());
    return s;
  }
  
  public boolean isBusy(Player p) {
    return (this.byPlayer.containsKey(p.getUniqueId()) || this.queued.containsKey(p.getUniqueId()) || this.bySpectator.containsKey(p.getUniqueId()));
  }


  
  public void joinQueue(Player p, Mode mode) {
    List<Player> group = this.plugin.getPartyHook().getPartyMembers(p);
    if (group.size() > 1 && !this.plugin.getPartyHook().isLeader(p)) {
      this.plugin.msg((CommandSender)p, "Only your party leader can queue the party.");
      return;
    } 
    if (group.size() > mode.totalPlayers()) {
      this.plugin.msg((CommandSender)p, "Your party is too large for " + mode.label() + ".");
      return;
    } 
    for (Player member : group) {
      if (isBusy(member)) {
        this.plugin.msg((CommandSender)p, member.getName() + " is already in a queue or match.");
        return;
      } 
    } 
    if (this.plugin.getArenaManager().all().isEmpty()) {
      this.plugin.msg((CommandSender)p, "No arenas are available right now.");
      return;
    } 
    LinkedHashSet<UUID> q = this.queues.get(mode);
    for (Player member : group) {
      q.add(member.getUniqueId());
      this.queued.put(member.getUniqueId(), mode);
      this.queuedAt.put(member.getUniqueId(), Long.valueOf(System.currentTimeMillis()));
      this.plugin.msg((CommandSender)member, "Joined the " + mode.label() + " queue (" + q.size() + "/" + mode
          .totalPlayers() + "). Use /bedfight leave to leave.");
    } 
    matchmake(mode);
  }
  
  public boolean leaveQueue(Player p) {
    Mode m = this.queued.remove(p.getUniqueId());
    this.queuedAt.remove(p.getUniqueId());
    Match spec = this.bySpectator.remove(p.getUniqueId());
    if (spec != null) {
      spec.removeSpectator(p, true);
    }
    if (m == null) {
      return spec != null;
    }
    ((LinkedHashSet)this.queues.get(m)).remove(p.getUniqueId());
    return true;
  }
  
  private void matchmake(Mode mode) {
    LinkedHashSet<UUID> q = this.queues.get(mode);
    while (q.size() >= mode.totalPlayers()) {
      List<Arena> arenas = new ArrayList<>(this.plugin.getArenaManager().all());
      if (arenas.isEmpty()) {
        return;
      }
      List<Player> players = new ArrayList<>();
      Iterator<UUID> it = q.iterator();
      while (it.hasNext() && players.size() < mode.totalPlayers()) {
        UUID id = it.next();
        it.remove();
        this.queued.remove(id);
        this.queuedAt.remove(id);
        Player pl = Bukkit.getPlayer(id);
        if (pl != null && pl.isOnline()) {
          players.add(pl);
        }
      } 
      if (players.size() < mode.totalPlayers()) {
        
        for (Player pl : players) {
          q.add(pl.getUniqueId());
          this.queued.put(pl.getUniqueId(), mode);
          this.queuedAt.put(pl.getUniqueId(), Long.valueOf(System.currentTimeMillis()));
        } 
        continue;
      } 
      startMatch(arenas.get(this.random.nextInt(arenas.size())), mode, players);
    } 
  }



  
  public String startPrivateMatch(Player host, Arena arena, Mode mode) {
    if (!this.plugin.getPartyHook().isAvailable()) {
      return "The party system (minestormparty) is not available.";
    }
    if (!this.plugin.getPartyHook().isLeader(host)) {
      return "Only the party leader can start a private match.";
    }
    List<Player> members = this.plugin.getPartyHook().getPartyMembers(host);
    if (members.size() != mode.totalPlayers()) {
      return "A " + mode.label() + " needs exactly " + mode.totalPlayers() + " players in your party (you have " + members
        .size() + ").";
    }
    for (Player m : members) {
      if (isBusy(m)) {
        return m.getName() + " is already in a queue or match.";
      }
    } 
    startMatch(arena, mode, members);
    return null;
  }
  
  public Match startMatch(Arena arena, Mode mode, List<Player> players) {
    for (Player p : players) {
      leaveQueue(p);
    }
    List<List<Player>> teams = this.plugin.getPartyHook().splitIntoTeams(players, mode.teamSize(), 2);
    Match match = new Match(this.plugin, this, arena, mode, teams);
    register(match);
    match.prepare();
    return match;
  }
  
  private void register(Match m) {
    this.matches.add(m);
    this.byWorld.put(m.getWorldName(), m);
    for (UUID id : m.getPlayerIds()) {
      this.byPlayer.put(id, m);
    }
  }

  
  void detach(Player p) {
    this.byPlayer.remove(p.getUniqueId());
  }
  
  void unregister(Match m) {
    this.matches.remove(m);
    this.byWorld.remove(m.getWorldName());
    for (UUID id : m.getPlayerIds()) {
      this.byPlayer.remove(id, m);
    }
    this.bySpectator.values().removeIf(mm -> mm == m);
  }
  
  public void shutdown() {
    for (Match m : new ArrayList(this.matches))
      m.forceEnd(); 
  }
}


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\game\GameManager.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */