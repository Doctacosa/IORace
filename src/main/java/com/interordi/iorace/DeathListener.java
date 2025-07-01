package com.interordi.iorace;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;


public class DeathListener implements Listener {
	
	private IORace plugin;
	private char targetAxis;
	@SuppressWarnings("unused")
	private char targetDirection;

	private boolean announceDeaths = false;
	private boolean useIOChatBridge = false;
	
	
	public DeathListener(IORace plugin, char targetAxis, char targetDirection) {
		this.plugin = plugin;
		this.targetAxis = targetAxis;
		this.targetDirection = targetDirection;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player p = (Player)event.getEntity();

		if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
			Location lastLocation = p.getLocation();
			int lastCoord = 0;
			if (targetAxis == 'x')
				lastCoord = lastLocation.getBlockX();
			else if (targetAxis == 'y')
				lastCoord = lastLocation.getBlockY();
			else
				lastCoord = lastLocation.getBlockZ();
			
			plugin.thisPlayerWatcher.recordDeath(p, lastCoord);
			plugin.thisPlayerWatcher.updateScore(p, true);

			//If we want to announce deaths, broadcast it
			if (this.announceDeaths) {
				String message = "Player " + p.getName() + " has fallen after " + String.format(Locale.US, "%,d", lastCoord) + " metres!";
				if (useIOChatBridge)
					plugin.getLogger().info("|IOBC|" + message);
				else
					Bukkit.getServer().broadcastMessage(message);
			}
		}
		
		@SuppressWarnings("unused")
		EntityDamageEvent entityDamageCause = p.getLastDamageCause();
	}


	//Respawn to spawn
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player p = event.getPlayer();
		plugin.getLogger().info("Respawning " + p.getName());
		World w = Bukkit.getServer().getWorlds().get(0);
		
		//Run this a few ticks later to let the server catch-up
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				p.teleport(w.getSpawnLocation());
			}
		}, 10);
	}


	public void setAnnounceDeaths(boolean value, boolean useIOChatBridge) {
		this.announceDeaths = value;
		this.useIOChatBridge = useIOChatBridge;
	}
	
}
