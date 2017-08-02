package com.interordi.iorace;

import java.io.File;
import java.io.IOException;
//import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;


public class PlayerWatcher implements Runnable {

	IORace plugin;
	private String filePath = "plugins/IORace/positions.yml";
	Map< UUID, Integer > posPlayers;
	boolean announceDeaths = false;
	int announceInterval = 5000;
	
	
	public PlayerWatcher(IORace plugin) {
		this.posPlayers = new HashMap< UUID, Integer >();
		this.plugin = plugin;
		loadPositions();
	}
	
	
	//Get the current list of positions as stored in the file
	public void loadPositions() {
		
		File statsFile = new File(this.filePath);
		FileConfiguration statsAccess = YamlConfiguration.loadConfiguration(statsFile);
		
		ConfigurationSection posData = statsAccess.getConfigurationSection("positions");
		if (posData == null) {
			plugin.getLogger().info("ERROR: Positions YML section not found");
			return;	//Nothing yet, exit
		}
		Set< String > cs = posData.getKeys(false);
		if (cs == null) {
			plugin.getLogger().info("ERROR: Couldn't get player keys");
			return;	//No players found, exit
		}
		
		if (cs.size() == 0) {
			//Nothing recorded yet, whatev'
			//plugin.getLogger().info("ERROR: Required YML section not found");
		}
		
		
		//Loop on each player
		for (String temp : cs) {
			UUID uuid = UUID.fromString(temp);
			String pos = posData.getString(temp);
			posPlayers.put(uuid, Integer.parseInt(pos));
		}
	}
	
	
	//Update the list to the file
	public void savePositions() {
		File statsFile = new File(this.filePath);
		FileConfiguration statsAccess = YamlConfiguration.loadConfiguration(statsFile);
		
		statsAccess.set("positions", "");
		
		for (Map.Entry< UUID , Integer > entry : this.posPlayers.entrySet()) {
			UUID uuid = entry.getKey();
			int position = entry.getValue();
			
			statsAccess.set("positions." + uuid, position);
		}
		
		try {
			statsAccess.save(statsFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	//Register the position of a player
	public void setPosPlayer(Player p, int x) {
		this.posPlayers.put(p.getUniqueId(), x);
		this.savePositions();
	}
	
	
	public void setAnnounceDeaths(boolean value) {
		this.announceDeaths = value;
	}
	
	
	public void setAnnounceInterval(int value) {
		this.announceInterval = value;
	}
	
	
	//Check and update position if needed
	public boolean checkStatus(Player p) {
		Location newLocation = p.getLocation();
		int currentPos = newLocation.getBlockX();
		
		Integer oldPos = posPlayers.get(p.getUniqueId());
		
		if (oldPos == null || currentPos > oldPos) {
			//Null bad. Numbers good!
			if (oldPos == null)	oldPos = 0;
			
			//Record the new position
			posPlayers.put(p.getUniqueId(), currentPos);
			
			//Check if a new target has been reached
			if (currentPos / announceInterval > oldPos / announceInterval) {
				int announce = (currentPos / announceInterval) * announceInterval;
				plugin.getLogger().info("|IOBC|Player " + p.getName() + " has passed the " + String.format(Locale.US, "%,d", announce) + " metres mark!");
			}
			
			return true;
		}
		
		return false;
	}
	
	
	//Get a player's best position, formatted
	public String getPosition(Player p) {
		
		Integer pos = posPlayers.get(p.getUniqueId());
		if (pos == null)
			pos = 0;
		
		return String.format(Locale.US, "%,d", pos);
	}
	
	
	@Override
	public void run() {
		boolean updates = false;
		
		//Get the position of all players on a regular basis
		for (Player p : Bukkit.getOnlinePlayers()) {
			updates |= checkStatus(p);
		}
		
		//Positions changed, save the file
		if (updates) {
			savePositions();
		}
	}
}
