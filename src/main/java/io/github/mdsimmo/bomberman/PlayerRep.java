package io.github.mdsimmo.bomberman;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/**
 * This is a class that holds all the extra data needed for a bomberman player <br>
 * When initialised, the player will automatically be made to join the game
 */
public class PlayerRep implements Listener {

	private static Plugin plugin = Bomberman.instance;
	public Player player;
	public ItemStack[] spawnInventory;
	public Location spawn;
	public Game game;
	public double spawnHealth, spawnHealthScale, spawnMaxHealth;
	public boolean isPlaying = false;
	public int immunity = 0;
	
	public PlayerRep(Player player, Game game) {
		this.player = player;
		this.game = game;
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		game.observers.add(this);
	}
		
	public void joinGame() {
		this.spawn = player.getLocation();
		Vector spawn = game.findSpareSpawn();
		if (spawn == null) {
			player.sendMessage("game full");
			return;
		} else
			player.teleport(game.loc.clone().add(spawn));
		
		spawnHealth = player.getHealth();
		player.setHealth(game.lives);
		player.setMaxHealth(game.lives);
		spawnHealthScale = player.getHealthScale();
		spawnMaxHealth = player.getMaxHealth();
		player.setHealthScale(game.lives*2);
		spawnInventory = player.getInventory().getContents();
		player.getInventory().setContents(new ItemStack[] {
				new ItemStack(Material.TNT, game.bombs),
				new ItemStack(Material.BLAZE_POWDER, game.power)
			});
		
		isPlaying = true;
		game.players.add(this);
	}
	
	
	
	/**
	 * restores the player to how they were before joining
	 */
	public void kill() {
		player.sendMessage(ChatColor.RED + "Game Over!");
		reset();
	}
	
	public void reset() {
		if (isPlaying) {
			isPlaying = false;
			game.players.remove(this);
			player.getInventory().setContents(spawnInventory);
			player.setMaxHealth(spawnMaxHealth);
			player.setHealth(spawnHealth);
			player.setHealthScale(spawnHealthScale);
			player.teleport(spawn);
			player.setFireTicks(0);
			game.alertRemoval(this);
		}
	}
	
	@EventHandler
	public void onPlayerPlaceTNT(BlockPlaceEvent e) {
		if (e.isCancelled()) return;
		Block b = e.getBlock();
		if (e.getPlayer() == player && isPlaying) {
			if (b.getType() == Material.TNT && game.isPlaying) {
				new Bomb(game, this, e.getBlock());
			} else {
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
    public void playerMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (p == this.player && !game.isPlaying && isPlaying) {
        	Location from = e.getFrom();
            double xfrom = e.getFrom().getX();
            double yfrom = e.getFrom().getY();
            double zfrom = e.getFrom().getZ();
            double xto = e.getTo().getX();
            double yto = e.getTo().getY();
            double zto = e.getTo().getZ();
            if (!(xfrom == xto && yfrom == yto && zfrom == zto)) {
                p.teleport(from);
            }
        }
    }
	
	public int bombStrength() {
		int strength = 0;
		for (ItemStack stack : player.getInventory().getContents()) {
			if (stack != null && stack.getType() == Material.BLAZE_POWDER) {
				strength += stack.getAmount();
			}
		}
		return strength;
	}
	
	public void damage() {
		if (immunity <= 0) {
			if (player.getHealth() > 1) {
				player.damage(1);
				new Immunity();
			} else { 
				kill();
			}
		}
	}
	
	private class Immunity implements Runnable {
		
		public Immunity() {
			immunity++;
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, this, 20);
		}
		
		@Override
		public void run() {
			immunity--;
		}
	}
	
	@EventHandler
	public void onPlayerRegen(EntityRegainHealthEvent e) {
		if (e.getEntity() == player && isPlaying) {
			if (e.getRegainReason() == RegainReason.MAGIC)
				player.setHealth(Math.min(player.getHealth()+1, player.getMaxHealth()));
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent e) {
		if (e.getPlayer() == player) {
			reset();
		}
	}
}
