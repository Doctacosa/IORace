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

	private IORace plugin;
	private String filePath = "plugins/IORace/positions.yml";
	private Map< UUID, Integer > posPlayers;
	private Map< UUID, Integer > posDeaths;
	private boolean announceDeaths = false;
	private int updateInterval = 500;
	private int announceInterval = 5000;
	private boolean useIOChatBridge = false;
	
	
	public PlayerWatcher(IORace plugin) {
		this.posPlayers = new HashMap< UUID, Integer >();
		this.posDeaths = new HashMap< UUID, Integer >();
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
	public void setPosPlayer(Player p, int x) {
		this.posPlayers.put(p.getUniqueId(), x);
		this.savePositions();
	}
	public void recordDeath(Player p, int x) {
		if (!this.posDeaths.containsKey(p.getUniqueId()) || x > this.posDeaths.get(p.getUniqueId())) {
			this.posDeaths.put(p.getUniqueId(), x);
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
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective objective = board.getObjective("position");
		if (objective != null) {
			Score myScore = objective.getScore(player.getDisplayName());
			
			int update = posPlayers.get(player.getUniqueId());
			if (!death)
				update = (update / updateInterval) * updateInterval;
			myScore.setScore(update);
		} else {
			Bukkit.getLogger().severe("No objective found!!");
		}
	}
	
	
	//Load the initial display of the scoreboard
	public void loadScores() {
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
				
				//If the player's top score is his death, display it as-is
				//Else, display the rounded best score
				int display = 0;
				if (posDeaths.containsKey(key) && posDeaths.get(key) >= posPlayers.get(key))
					display = posDeaths.get(key);
				else
					display = (posPlayers.get(key) / updateInterval) * updateInterval;

				myScore.setScore(display);
			}
		}
	}
}
