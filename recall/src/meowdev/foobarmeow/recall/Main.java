package meowdev.foobarmeow.recall;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Location;


public class Main extends JavaPlugin {
	
	int stop;
	static String commandCreateWaypoint = "waypoint";
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
			sender.sendMessage("recall can only be used with 'r'");
			return false;
		}

		if (!(sender instanceof Player)) {
			return false;
		}

		final Player player = (Player) sender;		
		
		if (args.length == 0) {
			// Send usage
			player.sendMessage(this.usage());
			return true;
		}
		
		// If this command is to create a waypoint
		if (args[0].equalsIgnoreCase(this.commandCreateWaypoint)) {
			this.createWaypoint(player, args[1]);
			return true;
		}

		// If this command is to delete a waypoint
		if (args[0].equalsIgnoreCase(this.commandDeleteWaypoint)) {
			this.deleteWaypoint(args[1]);
			player.sendMessage("Deleted waypoint " + args[1]);
			return true;
		}
		
		// If this command is to recall
		if (args[0].equalsIgnoreCase(this.commandRecall) || args[0].equalsIgnoreCase(commandRecallShort)) {
			// Find the location last set for this player's ID
			if (this.readWaypointFromConfiguration(player.getDisplayName()) != null) {
				Location oldLocation = player.getLocation();
				player.teleport(this.readWaypointFromConfiguration(player.getDisplayName()));
				this.saveWayopointToConfiguration(player.getDisplayName(), oldLocation);
				return true;
			} else {
				player.sendMessage("No known last location to recall to...");
				return true;
			}
		}

		// If this command is to list
		if (args[0].equalsIgnoreCase(this.commandList)) {
			// TODO
			return true;
		}
		
		// Otherwise, see if this matches a waypoint
		if (this.readWaypointFromConfiguration(args[0]) != null) {
			Location oldLocation = player.getLocation();
			player.teleport(this.readWaypointFromConfiguration(args[0]));
			this.saveWayopointToConfiguration(player.getDisplayName(), oldLocation);
			return true;
		}

		player.sendMessage(String.format("No waypoint named %s. Create a waypoint with the command 'waypoint <waypoint name>'", args[0]));
		return true;
	}
	
	private String usage() {
		return "To create a waypoint, type '/r <waypoint name>'.\nTo recall to a waypoint, type '/r <waypoint name>'.\nYour own recall waypoint is keyed on your name. Waypoints can be viewed with the 'list' command.";
	}
	
	private void createWaypoint(Player player, String name) {
		// Guard against waypoint names being a command name
		if (name.equalsIgnoreCase(commandCreateWaypoint) || 
			name.equalsIgnoreCase(commandRecall) || 
			name.equalsIgnoreCase(commandRecallShort) ||
			name.equalsIgnoreCase(commandList)) {
			player.sendMessage("Cannot create a waypoint with any reserved words: waypoint, recall, r");
			return;
		}
		
		Location loc = player.getLocation();
		this.saveWayopointToConfiguration(name, loc);
		player.sendMessage(String.format("Set waypoint %s to %d, %d, %d", name, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
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
