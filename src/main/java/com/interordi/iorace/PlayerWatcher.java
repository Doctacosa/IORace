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
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;


public class PlayerWatcher implements Runnable {

	IORace plugin;
	private String filePath = "plugins/IORace/positions.yml";
	Map< UUID, Integer > posPlayers;
	boolean announceDeaths = false;
	int updateInterval = 500;
	int announceInterval = 5000;
	
	
	public PlayerWatcher(IORace plugin) {
		this.posPlayers = new HashMap< UUID, Integer >();
		this.plugin = plugin;
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
		if (posData == null) {
			plugin.getLogger().info("ERROR: Positions YML section not found");
			return;	//Nothing yet, exit
		}
		Set< String > cs = posData.getKeys(false);
		if (cs != null) {
			//Loop on each player
			for (String temp : cs) {
				UUID uuid = UUID.fromString(temp);
				String pos = posData.getString(temp);
				posPlayers.put(uuid, Integer.parseInt(pos));
			}
		}

		//Show the scoreboard with the loaded data
		refreshScores();
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
			Bukkit.getLogger().severe("Failed to save the player positions.");
			e.printStackTrace();
		}
	}


	//Register the position of a player
	public void setPosPlayer(Player p, int x) {
		this.posPlayers.put(p.getUniqueId(), x);
		this.savePositions();
	}
	
	
	public void setUpdates(int updates, int announces, boolean deaths) {
		this.updateInterval = updates;
		this.announceInterval = announces;
		this.announceDeaths = deaths;
	}
	
	
	//Check and update position if needed
	public boolean checkStatus(Player p) {
		if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE)
			return false;

		Location newLocation = p.getLocation();
		int currentPos = newLocation.getBlockX();
		
		Integer oldPos = posPlayers.get(p.getUniqueId());
		
		if (oldPos == null || currentPos > oldPos) {
			//Null bad. Numbers good!
			if (oldPos == null)	oldPos = 0;
			
			//Record the new position
			posPlayers.put(p.getUniqueId(), currentPos);
			
			updateScore(p, false);
			
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
	
	
	//Update a player's score on the global display
	public void updateScore(Player player, boolean precise) {
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective objective = board.getObjective("position");
		if (objective != null) {
			Score myScore = objective.getScore(player.getDisplayName());
			
			int update = posPlayers.get(player.getUniqueId());
			if (!precise)
				update = (update / updateInterval) * updateInterval;
			myScore.setScore(update);
		} else {
			Bukkit.getLogger().severe("No objective found!!");
		}
	}
	
	
	//Refresh the scores based on the current players
	public void refreshScores() {
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective objective = board.getObjective("position");

		//If the objective doesn't exist yet, define it
		if (objective != null) {
			objective.unregister();
			objective = null;
		}

		//Rebuild the scoreboard from the known data
		objective = board.registerNewObjective("position", "dummy", "Players");
		board.clearSlot(DisplaySlot.SIDEBAR);
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		
		for (UUID key : posPlayers.keySet()) {
			OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(key);
			String playerName = offPlayer.getName();
			if (!playerName.isEmpty()) {
				Score myScore = objective.getScore(playerName);
				
				int update = (posPlayers.get(key) / updateInterval) * updateInterval;
				myScore.setScore(update);
			}
		}
	}
}
