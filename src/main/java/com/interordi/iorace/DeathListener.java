package com.interordi.iorace;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;


public class DeathListener implements Listener {
	
	IORace plugin;
	boolean announceDeaths = false;
	
	
	public DeathListener(IORace plugin) {
		this.plugin = plugin;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		@SuppressWarnings("unused")
		EntityType entity = event.getEntityType();
		
		if (event.getEntity() instanceof Player) {
			Player p = (Player)event.getEntity();
			
			if (p.getGameMode() == GameMode.SURVIVAL) {
				Location lastLocation = p.getLocation();
				int lastX = lastLocation.getBlockX();
				
				plugin.thisPlayerWatcher.updateScore(p, true);

				//If we want to announce deaths, broadcast it
				if (this.announceDeaths)
					plugin.getLogger().info("|IOBC|Player " + p.getName() + " has fallen after " + String.format(Locale.US, "%,d", lastX) + " metres!");
			}
			
			@SuppressWarnings("unused")
			EntityDamageEvent entityDamageCause = p.getLastDamageCause();
		} else {
			
		}
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


	public void setAnnounceDeaths(boolean value) {
		this.announceDeaths = value;
	}
	
}
