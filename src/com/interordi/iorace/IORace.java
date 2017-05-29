package com.interordi.iorace;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.interordi.iorace.DeathListener;
import com.interordi.iorace.PlayerWatcher;


public class IORace extends JavaPlugin {

	DeathListener thisDeathListener;
	PlayerWatcher thisPlayerWatcher;
	
	
	public void onEnable() {
		thisDeathListener = new DeathListener(this);
		thisPlayerWatcher = new PlayerWatcher(this);
		
		//Always ensure we've got a copy of the config in place (does not overwrite existing)
		this.saveDefaultConfig();
		
		//Configuration file use (config.yml): http://wiki.bukkit.org/Configuration_API_Reference
		boolean announceDeaths = this.getConfig().getBoolean("announce-deaths");
		thisDeathListener.setAnnounceDeaths(announceDeaths);
		thisPlayerWatcher.setAnnounceDeaths(announceDeaths);
		
		//Check every second for updated positions
		getServer().getScheduler().scheduleSyncRepeatingTask(this, thisPlayerWatcher, 10*20L, 10*20L);
		
		getLogger().info("IORace enabled");
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
}
