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
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;


public class PlayerWatcher implements Runnable {

	private IORace plugin;
	private char targetAxis;
	private char targetDirection;

	private String filePath = "plugins/IORace/positions.yml";
	private Map< UUID, Integer > posPlayers;
	private Map< UUID, Integer > posDeaths;
	@SuppressWarnings("unused")
	private boolean announceDeaths = false;
	private int updateInterval = 500;
	private int announceInterval = 5000;
	private boolean useIOChatBridge = false;
	private World targetWorld = null;
	
	
	public PlayerWatcher(IORace plugin, char targetAxis, char targetDirection, String targetWorldName) {
		this.posPlayers = new HashMap< UUID, Integer >();
		this.posDeaths = new HashMap< UUID, Integer >();
		this.plugin = plugin;
		this.targetAxis = targetAxis;
		this.targetDirection = targetDirection;
		if (targetWorldName != null && !targetWorldName.isEmpty())
			this.targetWorld = Bukkit.getWorld(targetWorldName);
		else
			this.targetWorld = Bukkit.getWorlds().get(0);

		loadPositions();
	}
	
	
	//Get the current list of positions as stored in the file
	public void loadPositions() {

		File statsFile = new File(this.filePath);
		try {
			if (!statsFile.exists())
				statsFile.createNewFile();
		} catch (IOException e) {
			Bukkit.getLogger().severe("Failed to create the positions file");
			e.printStackTrace();
			return;
		}

		FileConfiguration statsAccess = YamlConfiguration.loadConfiguration(statsFile);
		
		ConfigurationSection posData = statsAccess.getConfigurationSection("positions");
		if (posData != null) {
			Set< String > cs = posData.getKeys(false);
			if (cs != null) {
				//Loop on each player
				for (String temp : cs) {
					UUID uuid = UUID.fromString(temp);
					String pos = posData.getString(temp);
					posPlayers.put(uuid, Integer.parseInt(pos));
				}
			}
		}

		posData = statsAccess.getConfigurationSection("deaths");
		if (posData != null) {
			Set< String > cs = posData.getKeys(false);
			if (cs != null) {
				//Loop on each player
				for (String temp : cs) {
					UUID uuid = UUID.fromString(temp);
					String pos = posData.getString(temp);
					posDeaths.put(uuid, Integer.parseInt(pos));
				}
			}
		}

		//Show the scoreboard with the loaded data
		loadScores();
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
		
		statsAccess.set("deaths", "");
		
		for (Map.Entry< UUID , Integer > entry : this.posDeaths.entrySet()) {
			UUID uuid = entry.getKey();
			int position = entry.getValue();
			
			statsAccess.set("deaths." + uuid, position);
		}
		
		try {
			statsAccess.save(statsFile);
		} catch (IOException e) {
			Bukkit.getLogger().severe("Failed to save the player positions.");
			e.printStackTrace();
		}
	}


	//Register the position of a player
	public void setPosPlayer(Player p, int coord) {
		this.posPlayers.put(p.getUniqueId(), coord);
		this.savePositions();
	}
	public void recordDeath(Player p, int coord) {
		if (!this.posDeaths.containsKey(p.getUniqueId()) || (
			(targetDirection == '+' && coord > this.posDeaths.get(p.getUniqueId())) ||
			(targetDirection == '-' && coord < this.posDeaths.get(p.getUniqueId()))
		)) {
			this.posDeaths.put(p.getUniqueId(), coord);
			this.checkStatus(p);
			this.savePositions();
		}
	}
	
	
	public void setUpdates(int updates, int announces, boolean deaths, boolean useIOChatBridge) {
		this.updateInterval = updates;
		this.announceInterval = announces;
		this.announceDeaths = deaths;
		this.useIOChatBridge = useIOChatBridge;
	}
	
	
	//Check and update position if needed
	public boolean checkStatus(Player p) {
		if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE)
			return false;

		Location newLocation = p.getLocation();
		int currentPos = 0;
		if (targetAxis == 'x')
			currentPos = newLocation.getBlockX();
		else if (targetAxis == 'y')
			currentPos = newLocation.getBlockY();
		else
			currentPos = newLocation.getBlockZ();
		
		Integer oldPos = posPlayers.get(p.getUniqueId());

		//Only count for the target world
		if (p.getWorld() != this.targetWorld)
			return false;
		
		if (oldPos == null || (
			(targetDirection == '+' && currentPos > oldPos) ||
			(targetDirection == '-' && currentPos < oldPos)
		)) {
			//Null bad. Numbers good!
			if (oldPos == null)	oldPos = 0;
			
			//Record the new position
			posPlayers.put(p.getUniqueId(), currentPos);
			
			Integer deathPos = posDeaths.get(p.getUniqueId());
			if (deathPos == null || (
				(targetDirection == '+' && deathPos < currentPos) ||
				(targetDirection == '-' && deathPos > currentPos)
			))
				updateScore(p, false);
			
			//Check if a new target has been reached
			if ((targetDirection == '+' && currentPos / announceInterval > oldPos / announceInterval) ||
				(targetDirection == '-' && currentPos / announceInterval < oldPos / announceInterval)
			) {
				int announce = (currentPos / announceInterval) * announceInterval;
				String message = "Player " + p.getName() + " has passed the " + String.format(Locale.US, "%,d", announce) + " metres mark!";
				if (useIOChatBridge)
					plugin.getLogger().info("|IOBC|" + message);
				else
					Bukkit.getServer().broadcastMessage(message);
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
	
	
	//Update a player's score on the global display
	public void updateScore(Player player, boolean death) {
		int score = posPlayers.get(player.getUniqueId());
		if (!death)
			score = (score / updateInterval) * updateInterval;
		plugin.getScores().updateScore(player, score);
	}
	
	
	//Load the initial display of the scoreboard
	//These are the values we want to display, not actual top position
	public void loadScores() {
		Map< UUID, Integer > scores = new HashMap< UUID, Integer >();
		for (UUID key : posPlayers.keySet()) {
			int display = 0;
			if (posDeaths.containsKey(key) && (
				(targetDirection == '+' && posDeaths.get(key) >= posPlayers.get(key)) ||
				(targetDirection == '-' && posDeaths.get(key) < posPlayers.get(key))
			))
				display = posDeaths.get(key);
			else
				display = (posPlayers.get(key) / updateInterval) * updateInterval;

			scores.put(key, display);
		}
		plugin.getScores().loadScores(scores);
	}
}
