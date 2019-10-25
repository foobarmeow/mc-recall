package meowdev.foobarmeow.recall;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemorySection;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;

public class Main extends JavaPlugin {
	
	static String commandCreateWaypoint = "waypoint";
	static String commandCreateWaypointShort = "wp";
	static String commandDeleteWaypoint = "del";
	static String commandRecall = "recall";
	static String commandRecallShort = "r";
	static String commandList = "list";
	static String waypointsSection = "waypoints";
	
	@Override
	public void onEnable() {
		this.reloadConfig();
		
		// Save the default config if it doesn't exist
		this.saveDefaultConfig();
		
		// Setup /r as the entry point
		this.getCommand("r").setExecutor(this);
	}

	@Override
	public void onDisable() {
		this.saveConfig();
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("r")) {
			sender.sendMessage(ChatColor.RED + "recall can only be used with 'r'");
			return false;
		}

		if (!(sender instanceof Player)) {
			return false;
		}

		Player player = (Player) sender;		
		
		if (args.length == 0) {
			// Send usage
			player.sendMessage(ChatColor.BLUE + this.usage());
			return true;
		}
		
		// If this command is to create a waypoint
		if (args[0].equalsIgnoreCase(Main.commandCreateWaypoint) || args[0].equalsIgnoreCase(Main.commandCreateWaypointShort)) {
			this.createWaypoint(player, args[1]);
			return true;
		}

		// If this command is to delete a waypoint
		if (args[0].equalsIgnoreCase(Main.commandDeleteWaypoint)) {
			this.deleteWaypoint(args[1]);
			player.sendMessage(ChatColor.BLUE + "Deleted waypoint " + args[1]);
			return true;
		}
		
		// If this command is to recall
		if (args[0].equalsIgnoreCase(Main.commandRecall) || args[0].equalsIgnoreCase(Main.commandRecallShort)) {
			// Find the location last set for this player's ID
			if (this.readWaypointFromConfiguration(player.getDisplayName()) != null) {
				Location oldLocation = player.getLocation();
				player.teleport(this.readWaypointFromConfiguration(player.getDisplayName()));
				this.saveWayopointToConfiguration(player.getDisplayName(), oldLocation);
				player.sendMessage(ChatColor.BLUE + "Recalled to location before last teleport");
				return true;
			} else {
				player.sendMessage(ChatColor.RED + "No known last location to recall to...");
				return true;
			}
		}

		// If this command is to list
		if (args[0].equalsIgnoreCase(Main.commandList)) {
			// TODO
			final StringBuilder listBuilder = new StringBuilder();
			listBuilder.append("Waypoint List:\n");
			Map<String, Object> waypointsMap = ((MemorySection) this.getConfig().get(Main.waypointsSection)).getValues(false);
			waypointsMap.forEach((k, v) -> {
				Location loc = (Location) v;
				listBuilder.append(String.format("%s - %d, %d, %d\n", k, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
			});
			player.sendMessage(ChatColor.BLUE + listBuilder.toString());
			return true;
		}
		
		// Otherwise, see if this matches a waypoint
		if (this.readWaypointFromConfiguration(args[0]) != null) {
			Location oldLocation = player.getLocation();
			player.teleport(this.readWaypointFromConfiguration(args[0]));
			this.saveWayopointToConfiguration(player.getDisplayName(), oldLocation);
			player.sendMessage(ChatColor.BLUE + String.format("Teleported to waypoint %s", args[0]));
			return true;
		}

		player.sendMessage(String.format("No waypoint named %s. Create a waypoint with the command 'waypoint <waypoint name>'", args[0]));
		return true;
	}
	
	private String usage() {
		return ChatColor.BLUE + "/r waypoint | wp <waypoint name> - create a waypoint\n/r <existing waypoint name> - teleport to existing waypoint\n/r recall | r - recall to last point before teleport.\n/r del <waypoint name> - delete an existing waypoint"; 
	}
	
	private void createWaypoint(Player player, String name) {
		// Guard against waypoint names being a command name
		if (name.equalsIgnoreCase(commandCreateWaypoint) || 
			name.equalsIgnoreCase(commandCreateWaypointShort) || 
			name.equalsIgnoreCase(commandRecall) || 
			name.equalsIgnoreCase(commandRecallShort) ||
			name.equalsIgnoreCase(commandList)) {
			player.sendMessage(ChatColor.RED + "Cannot create a waypoint with any reserved words: waypoint, recall, r");
			return;
		}
		
		Location loc = player.getLocation();
		this.saveWayopointToConfiguration(name, loc);
		player.sendMessage(ChatColor.BLUE + String.format("Set waypoint %s to %d, %d, %d", name, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
	}
	
	private void deleteWaypoint(String name) {
		String waypointKey = String.format("%s.%s", waypointsSection, name);
		if (!this.getConfig().contains(waypointKey)) {
			return;
		}
		
		this.getConfig().set(waypointKey, null);
	}
	
	private Location readWaypointFromConfiguration(String name) {
		String waypointKey = String.format("%s.%s", waypointsSection, name);
		if (!this.getConfig().contains(waypointKey)) {
			return null;
		}
		
		return (Location) this.getConfig().get(waypointKey);
	}
	
	private void saveWayopointToConfiguration(String name, Location loc) {
		this.getConfig().set(String.format("%s.%s", waypointsSection, name), loc);
		this.saveConfig();
	}
}
