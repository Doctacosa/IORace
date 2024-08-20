package com.interordi.iorace;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.interordi.iorace.utilities.Scores;


public class IORace extends JavaPlugin {

	public DeathListener thisDeathListener;
	public PlayerWatcher thisPlayerWatcher;
	Scores thisScores;
	
	
	public void onEnable() {
		thisScores = new Scores("Position");

		thisDeathListener = new DeathListener(this);
		thisPlayerWatcher = new PlayerWatcher(this);
		
		//Always ensure we've got a copy of the config in place (does not overwrite existing)
		this.saveDefaultConfig();
		
		//Configuration file use (config.yml): http://wiki.bukkit.org/Configuration_API_Reference
		boolean announceDeaths = this.getConfig().getBoolean("announce-deaths");
		int updateInterval = this.getConfig().getInt("update-interval", 500);
		int announceInterval = this.getConfig().getInt("announce-interval", 5000);
		boolean useIOChatBridge = this.getConfig().getBoolean("use-iochatbridge", false);
		thisDeathListener.setAnnounceDeaths(announceDeaths, useIOChatBridge);
		thisPlayerWatcher.setUpdates(updateInterval, announceInterval, announceDeaths, useIOChatBridge);
		
		//Check every second for updated positions
		getServer().getScheduler().scheduleSyncRepeatingTask(this, thisPlayerWatcher, 5*20L, 5*20L);
		
		getLogger().info("IORace enabled");
	}
	
	
	public void onDisable() {
		getLogger().info("IORace disabled");
	}
	
	
	public String getPosition(Player p) {
		return thisPlayerWatcher.getPosition(p);
	}
	
	
	public Scores getScores() {
		return thisScores;
	}


	public String colorize(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}
}
