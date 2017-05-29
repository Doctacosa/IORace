package com.interordi.iorace;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
				//If we want to announce deaths, broadcast it
				if (this.announceDeaths)
					plugin.getLogger().info("|IOBC|Player " + p.getName() + " has fallen after " + plugin.getPosition(p) + " metres!");
			}
			
			@SuppressWarnings("unused")
			EntityDamageEvent entityDamageCause = p.getLastDamageCause();
		} else {
			
		}
	}
	
	
	//Respawn to spawn
	//TODO: Make sure this doesn't work on reloggings... It shouldn't, but still...
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player p = event.getPlayer();
		plugin.getLogger().info("Respawning " + p.getName());
		World w = Bukkit.getServer().getWorlds().get(0);
		
		p.teleport(w.getSpawnLocation());
	}


	public void setAnnounceDeaths(boolean value) {
		this.announceDeaths = value;
	}
	
}
