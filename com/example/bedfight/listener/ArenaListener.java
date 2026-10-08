package com.example.bedfight.listener;

import com.example.bedfight.BedFightPlugin;
import com.example.bedfight.arena.ArenaManager;
import com.example.bedfight.game.Match;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public final class ArenaListener
  implements Listener
{
  private static final String PROTECTED_MSG = "This arena is protected. Use /bedfight buildmode to edit it.";
  private final BedFightPlugin plugin;
  
  public ArenaListener(BedFightPlugin plugin) {
    this.plugin = plugin;
  }
  private Match match(World w) {
    return this.plugin.getGameManager().getMatchByWorld(w);
  }
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onBreak(BlockBreakEvent e) {
    Match m = match(e.getBlock().getWorld());
    if (m != null) {
      m.onBreak(e);
      return;
    } 
    ArenaManager am = this.plugin.getArenaManager();
    if (am.isTemplateWorld(e.getBlock().getWorld()) && !am.isBuildMode(e.getPlayer())) {
      e.setCancelled(true);
      this.plugin.msg((CommandSender)e.getPlayer(), "This arena is protected. Use /bedfight buildmode to edit it.");
    } 
  }
  
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlace(BlockPlaceEvent e) {
    Match m = match(e.getBlock().getWorld());
    if (m != null) {
      m.onPlace(e);
      return;
    } 
    ArenaManager am = this.plugin.getArenaManager();
    if (am.isTemplateWorld(e.getBlock().getWorld()) && !am.isBuildMode(e.getPlayer())) {
      e.setCancelled(true);
      this.plugin.msg((CommandSender)e.getPlayer(), "This arena is protected. Use /bedfight buildmode to edit it.");
    } 
  }
  
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onExplode(EntityExplodeEvent e) {
    World w = e.getEntity().getWorld();
    Match m = match(w);
    if (m != null) {
      m.onExplode(e);
    } else if (this.plugin.getArenaManager().isTemplateWorld(w)) {
      e.blockList().clear();
    } 
  }
  
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onDamage(EntityDamageEvent e) {
    Match m = match(e.getEntity().getWorld());
    if (m != null) {
      m.onDamage(e);
    }
  }
  
  @EventHandler(ignoreCancelled = true)
  public void onMove(PlayerMoveEvent e) {
    Match m = match(e.getPlayer().getWorld());
    if (m != null) {
      m.onMove(e);
    }
  }

  
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onDrop(PlayerDropItemEvent e) {
    if (match(e.getPlayer().getWorld()) != null) {
      e.setCancelled(true);
    }
  }
  
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPickup(PlayerPickupItemEvent e) {
    if (match(e.getPlayer().getWorld()) != null) {
      e.setCancelled(true);
    }
  }

  
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onItemSpawn(ItemSpawnEvent e) {
    if (match(e.getLocation().getWorld()) != null) {
      e.setCancelled(true);
    }
  }
  
  @EventHandler
  public void onFood(FoodLevelChangeEvent e) {
    if (match(e.getEntity().getWorld()) != null) {
      e.setCancelled(true);
    }
  }
  
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInventoryOpen(InventoryOpenEvent e) {
    if (!(e.getPlayer() instanceof Player)) return;
    Player p = (Player)e.getPlayer();
    Match m = this.plugin.getGameManager().getSpectating(p);
    if (m != null) {
      e.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInteract(PlayerInteractEvent e) {
    Player p = e.getPlayer();
    if (this.plugin.getGameManager().getSpectating(p) != null) {
      e.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled = true)
  public void onSpecMove(PlayerMoveEvent e) {
    Player p = e.getPlayer();
    Match m = this.plugin.getGameManager().getSpectating(p);
    if (m == null || m.getWorld() == null) return;
    if (!p.getWorld().equals(m.getWorld())) return;
    Block to = e.getTo().getBlock();
    Block from = e.getFrom().getBlock();
    if (to.getType().isSolid() && !from.getType().isSolid()) {
      Location fixed = e.getFrom().clone();
      fixed.setYaw(e.getTo().getYaw());
      fixed.setPitch(e.getTo().getPitch());
      e.setTo(fixed);
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    this.plugin.getGameManager().leaveQueue(e.getPlayer());
    this.plugin.getArenaManager().onQuit(e.getPlayer());
    Match m = this.plugin.getGameManager().getMatch(e.getPlayer());
    if (m != null)
      m.onQuit(e.getPlayer()); 
  }
}


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\listener\ArenaListener.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */