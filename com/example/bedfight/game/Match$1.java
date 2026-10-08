package com.example.bedfight.game;

import com.example.bedfight.util.Titles;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;































































































































































































class null
  extends BukkitRunnable
{
  int left = seconds;

  
  public void run() {
    if (Match.this.state != Match.State.COUNTDOWN) {
      cancel();
      return;
    } 
    if (this.left <= 0) {
      cancel();
      Match.this.startGame();
      return;
    } 
    Match.this.countdownLeft = this.left;
    ChatColor c = (this.left > 3) ? ChatColor.GREEN : ((this.left == 3) ? ChatColor.YELLOW : ChatColor.RED);
    for (UUID id : Match.this.byPlayer.keySet()) {
      Player p = Bukkit.getPlayer(id);
      if (p != null) {
        Titles.send(p,
            String.valueOf(c) + String.valueOf(ChatColor.BOLD) + this.left,
            String.valueOf(ChatColor.GRAY) + "Get ready to fight!", 0, 25, 5);
        p.playSound(p.getLocation(), Sound.CLICK, 1.0F, 1.0F);
      } 
    } 
    for (UUID sid : Match.this.getSpectators()) {
      Player sp = Bukkit.getPlayer(sid);
      if (sp != null) sp.playSound(sp.getLocation(), Sound.CLICK, 1.0F, 1.0F);
    }
    this.left--;
  }
}


/* Location:              C:\Users\nasle javan\Desktop\BedFight-1.0.0.jar!\com\example\bedfight\game\Match$1.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */