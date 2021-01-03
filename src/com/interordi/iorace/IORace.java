package com.interordi.iorace;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import com.interordi.iorace.DeathListener;
import com.interordi.iorace.PlayerWatcher;


public class IORace extends JavaPlugin implements Runnable {

	DeathListener thisDeathListener;
	PlayerWatcher thisPlayerWatcher;
	
	
	public void onEnable() {
		thisDeathListener = new DeathListener(this);
		thisPlayerWatcher = new PlayerWatcher(this);
		
		//Always ensure we've got a copy of the config in place (does not overwrite existing)
		this.saveDefaultConfig();
		
		//Configuration file use (config.yml): http://wiki.bukkit.org/Configuration_API_Reference
		boolean announceDeaths = this.getConfig().getBoolean("announce-deaths");
		int updateInterval = this.getConfig().getInt("update-interval", 500);
		int announceInterval = this.getConfig().getInt("announce-interval", 5000);
		thisDeathListener.setAnnounceDeaths(announceDeaths);
		thisPlayerWatcher.setUpdates(updateInterval, announceInterval, announceDeaths);
		
		//Check every second for updated positions
		getServer().getScheduler().scheduleSyncRepeatingTask(this, thisPlayerWatcher, 5*20L, 5*20L);
		
		getLogger().info("IORace enabled");
		
		//Run initial required tasks once
		getServer().getScheduler().scheduleSyncDelayedTask(this, this);
	}
	
	
	public void onDisable() {
		getLogger().info("IORace disabled");
	}
	
	
	public String getPosition(Player p) {
		return thisPlayerWatcher.getPosition(p);
	}
	
	
	public String colorize(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}
	
	
	@Override
	public void run() {
		//Add the basic scoreboard when the server is loaded
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective objective = board.getObjective("position");
		
		if (objective != null)
			objective.unregister();
		
		objective = board.registerNewObjective("position", "dummy", "Players");
		board.clearSlot(DisplaySlot.SIDEBAR);
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
	}
}
