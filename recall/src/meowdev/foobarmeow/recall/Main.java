package meowdev.foobarmeow.recall;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.io.IOException;
import java.io.FileWriter;
import java.io.FileReader;
import java.util.logging.Level;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class Main extends JavaPlugin {
	
	int stop;
	static String commandCreateWaypoint = "waypoint";
	static String commandRecall = "recall";
	static String commandRecallShort = "r";
	static String commandList = "list";
	
	private HashMap<String, String> playerInfo = new HashMap<String, String>();
	
	@Override
	public void onEnable() {
		this.getCommand("r").setExecutor(this);
		JavaPlugin plugin = this;
		try (FileReader reader = new FileReader("waypoints.json")) {
			// Parse JSON into playerInfo
			HashMap<String, String> pi = new Gson().fromJson(reader, playerInfo.getClass());
			if (pi != null) {
				playerInfo = pi;
			}
		} catch (IOException e) {
			// Do nothing if it doesn't exist, we will write it later
		}
		
		// Setup Scheduler to intermittently save player info
		this.stop = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				if (playerInfo.isEmpty()) {
					return;
				}
				try (FileWriter writer = new FileWriter("waypoints.json")) {
					String jsonStr = new Gson().toJson(playerInfo);
					writer.write(jsonStr);
					writer.flush();
				} catch (IOException e) {
					plugin.getLogger().log(Level.SEVERE, e.getMessage());
				}
			}
		}, 240, 240);
	}

	@Override
	public void onDisable() {
		if (playerInfo.isEmpty()) {
			return;
		}
		try (FileWriter writer = new FileWriter("waypoints.json")) {
			String jsonStr = new Gson().toJson(playerInfo);
			writer.write(jsonStr);
			writer.flush();
			this.getLogger().log(Level.INFO, "Saved waypoints.json on disable");
		} catch (IOException e) {
			this.getLogger().log(Level.SEVERE, e.getMessage());
		}
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
			this.createWaypoint(player, args);
			return true;
		}
		
		// If this command is to recall
		if (args[0].equalsIgnoreCase(this.commandRecall) || args[0].equalsIgnoreCase(commandRecallShort)) {
			// Find the location last set for this player's ID
			if (this.playerInfo.containsKey(player.getDisplayName())) {
				this.teleport(player, player.getDisplayName());
				return true;
			} else {
				player.sendMessage("No known last location to recall to...");
				return true;
			}
		}

		// If this command is to list
		if (args[0].equalsIgnoreCase(this.commandList)) {
			String pi = new GsonBuilder().setPrettyPrinting().create().toJson(playerInfo);
			player.sendMessage(pi);
			return true;
		}
		
		// Otherwise, see if this matches a waypoint
		if (this.playerInfo.containsKey(args[0])) {
			this.teleport(player, args[0]);
			return true;
		}

		player.sendMessage(String.format("No waypoint named %s. Create a waypoint with the command 'waypoint <waypoint name>'", args[0]));
		return true;
	}
	
	private String usage() {
		return "To create a waypoint, type '/r <waypoint name>'.\nTo recall to a waypoint, type '/r <waypoint name>'.\nYour own recall waypoint is keyed on your name. Waypoints can be viewed with the 'list' command.";
	}
	
	private void teleport(Player player, String waypoint) {
		// Save this location so they can recall to it
		Location oldLocation = player.getLocation();
		player.teleport(this.deserializeLocation(this.playerInfo.get(waypoint)));
		this.playerInfo.put(player.getDisplayName(), this.serializeLocation(oldLocation));
	}
	
	private String serializeLocation(Location loc) {
		return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
	}
	
	private Location deserializeLocation(String loc) {
		String[] parts = loc.split(":");
	    World w = Bukkit.getServer().getWorld(parts[0]);
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        return new Location(w, x, y, z);
	}
	
	private void createWaypoint(Player player, String[] args) {
		// Get name of waypoint from args
		final String waypointName = args[1];
		
		if (waypointName.equalsIgnoreCase(commandCreateWaypoint) || 
			waypointName.equalsIgnoreCase(commandRecall) || 
			waypointName.equalsIgnoreCase(commandRecallShort) ||
			waypointName.equalsIgnoreCase(commandList) {
			player.sendMessage("Cannot create a waypoint with any reserved words: waypoint, recall, r");
		}
		
		if (waypointName.equalsIgnoreCase(commandCreateWaypoint)) {
			player.sendMessage("Can't create a waypoint called 'waypoint'");
			return;
		}
		
		final Location location = player.getLocation();
		this.playerInfo.put(waypointName, this.serializeLocation(location));
		player.sendMessage(String.format("Set waypoint %s to %d, %d, %d", waypointName, location.getBlockX(), location.getBlockY(), location.getBlockZ()));
	}
}
