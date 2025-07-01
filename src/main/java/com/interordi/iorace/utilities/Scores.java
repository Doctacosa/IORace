package com.interordi.iorace.utilities;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;


public class Scores {

	private String header;
	@SuppressWarnings("unused")
	private char targetAxis;
	private char targetDirection;

	private Map< UUID, Integer > scoresPlayers;


	public Scores(String header, char targetAxis, char targetDirection) {
		this.scoresPlayers = new HashMap< UUID, Integer >();
		this.header = header;
		this.targetAxis = targetAxis;
		this.targetDirection = targetDirection;
	}


	//Refresh the current display
	public void refreshDisplay() {

		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective objective = board.getObjective("score");
		board.clearSlot(DisplaySlot.SIDEBAR);
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);

		//Get the top three players
		Map< UUID, Integer > topThree = null;

		if (targetDirection == '+') {
			topThree = scoresPlayers.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
				.limit(3)
				.collect(Collectors.toMap(
					Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)
			);
		} else {
			topThree = scoresPlayers.entrySet().stream()
				.sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
				.limit(3)
				.collect(Collectors.toMap(
					Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)
			);
		}

		//Include the top players
		Set< UUID > toDisplay = new HashSet< UUID >();
		for (UUID topPlayer : topThree.keySet()) {
			toDisplay.add(topPlayer);
		}

		//Include the online players
		for (Player p : Bukkit.getOnlinePlayers()) {
			toDisplay.add(p.getUniqueId());
		}

		//Display only the players we want
		for (UUID uuid : toDisplay) {
			OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(uuid);
			String playerName = offPlayer.getName();
			if (!playerName.isEmpty()) {
				Score myScore = objective.getScore(playerName);
				
				if (!scoresPlayers.containsKey(uuid))
					continue;

				int display = scoresPlayers.get(uuid);
				if ((targetDirection == '+' && display > 0) ||
					(targetDirection == '-' && display < 0))
					myScore.setScore(display);
			}
		}
	}


	//Update a player's score on the global display
	public void updateScore(Player player, int score) {
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective objective = board.getObjective("score");
		if (objective != null) {
			Score myScore = objective.getScore(player.getDisplayName());
			myScore.setScore(score);
		} else {
			Bukkit.getLogger().severe("No objective found!!");
		}

		scoresPlayers.put(player.getUniqueId(), score);

		refreshDisplay();
	}
	
	
	//Load the initial display of the scoreboard
	public void loadScores(Map< UUID, Integer > scores) {
		Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
		Objective objective = board.getObjective("score");

		//If the objective doesn't exist yet, define it
		if (objective != null) {
			objective.unregister();
			objective = null;
		}

		scoresPlayers = scores;

		//Prepare the scoreboard for later updates
		objective = board.registerNewObjective("score", "dummy", header);

		refreshDisplay();
	}

}
