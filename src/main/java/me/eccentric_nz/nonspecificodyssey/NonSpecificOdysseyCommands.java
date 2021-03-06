package me.eccentric_nz.nonspecificodyssey;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import net.sacredlabyrinth.Phaed.PreciousStones.field.Field;
import net.sacredlabyrinth.Phaed.PreciousStones.field.FieldFlag;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import net.sacredlabyrinth.Phaed.PreciousStones.PreciousStones;

public class NonSpecificOdysseyCommands implements CommandExecutor {

    private final NonSpecificOdyssey plugin;
    private final Random rand = new Random();
    private final HashMap<String, Long> rtpcooldown = new HashMap<String, Long>();

    public NonSpecificOdysseyCommands(NonSpecificOdyssey plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        Player player = null;
        if (args.length > 1)
            player = Bukkit.getPlayerExact(args[1]);

        // If the player typed /randomteleport then do the following...
        // check there is the right number of arguments
        if (cmd.getName().equalsIgnoreCase("randomteleport"))
        {
            if (player == null) {
                sender.sendMessage("[" + plugin.getPluginName() + "] " + "Player not online");
                return true;
            }
            Location random;
            if (args.length > 1)
            {
                // teleport to the specified world
                World world = plugin.getServer().getWorld(args[0]);
                if (world == null) {
                    sender.sendMessage("[" + plugin.getPluginName() + "] " + "Could not find the world '" + args[0] + "'. Are you sure you typed it correctly?");
                    return true;
                }
                random = randomOverworldLocation(world, player);
                //player.sendMessage("Dr0ppin u in, glhf");
                movePlayer(player, random, world);
                return true;
            }
            else
                return false;


        }
        if (cmd.getName().equalsIgnoreCase("biome")) {
            if (args.length < 1) {
                return false;
            }
            String upper = args[0].toUpperCase(Locale.ENGLISH);
            if (upper.equals("LIST")) {
                StringBuilder sb = new StringBuilder();
                for (Biome bi : Biome.values()) {
                    if (!bi.equals(Biome.HELL) && !bi.equals(Biome.SKY)) {
                        sb.append(bi.toString()).append(", ");
                    }
                }
                String b = sb.toString().substring(0, sb.length() - 2);
                sender.sendMessage("Biomes: " + b);
                return true;
            } else {
                if (!sender.hasPermission("nonspecificodyssey.biome." + upper)) {
                    sender.sendMessage("[" + plugin.getPluginName() + "] " + "You do not have permission to use biome teleports!");
                    return true;
                }
                if (player == null) {
                    sender.sendMessage("[" + plugin.getPluginName() + "] " + "This command can only be run by a player!");
                    return true;
                }
                World w = null;
                if (args.length > 1) {
                    plugin.getServer().getWorld(args[1]);
                    if (w == null) {
                        sender.sendMessage("[" + plugin.getPluginName() + "] " + "Could not find the world '" + args[1] + "'. Are you sure you typed it correctly?");
                        return true;
                    }
                }
                try {
                    Biome biome = Biome.valueOf(upper);
                    sender.sendMessage("Searching for biome, this may take some time!");
                    Location nsob = searchBiome(player, biome, w);
                    if (nsob == null) {
                        sender.sendMessage("Could not find biome!");
                        return true;
                    } else {
                        movePlayer(player, nsob, player.getLocation().getWorld());
                    }
                } catch (IllegalArgumentException iae) {
                    sender.sendMessage("Biome type not valid!");
                }
                return true;
            }
        }
        if (cmd.getName().equalsIgnoreCase("nsoadmin")) {
            if (!sender.hasPermission("nonspecificodyssey.admin")) {
                sender.sendMessage("[" + plugin.getPluginName() + "] " + "You do not have permission to change the config!");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage("[" + plugin.getPluginName() + "] " + "Not enough command arguments!");
                return false;
            }
            if (args[0].equalsIgnoreCase("cooldown") || args[0].equalsIgnoreCase("no_damage") || args[0].equalsIgnoreCase("nether") || args[0].equalsIgnoreCase("end")) {
                String option = args[0].toLowerCase();
                boolean bool = !plugin.getConfig().getBoolean(option);
                plugin.getConfig().set(option, bool);
                plugin.saveConfig();
                sender.sendMessage("[" + plugin.getPluginName() + "] " + option + " was set to: " + bool);
                return true;
            }
            if (args[0].equalsIgnoreCase("cooldown_time") || args[0].equalsIgnoreCase("no_damage_time") || args[0].equalsIgnoreCase("max") || args[0].equalsIgnoreCase("step") || args[0].equalsIgnoreCase("initial_step")) {
                if (args.length < 2) {
                    sender.sendMessage("[" + plugin.getPluginName() + "] " + "Not enough command arguments!");
                    return false;
                }
                String option = args[0].toLowerCase();
                int amount = Integer.parseInt(args[1]);
                plugin.getConfig().set(option, amount);
                plugin.saveConfig();
                sender.sendMessage("[" + plugin.getPluginName() + "] " + option + " was set to: " + amount);
                return true;
            }
        }
        return false;
    }

    public Location randomOverworldLocation(World w, Player player) {
        boolean danger = true;
        Location random = null;
        // get max_radius from config
        while (danger == true)
        {
            int x = randomX();
            int z = randomZ();
            //int y = 255;
            int highest = w.getHighestBlockYAt(x, z);
            if (highest > 3)
            {
                Material chkBlock = w.getBlockAt(x, highest, z).getRelative(BlockFace.DOWN).getType();
                if (!chkBlock.equals(Material.WATER) && !chkBlock.equals(Material.STATIONARY_WATER) && !chkBlock.equals(Material.LAVA) && !chkBlock.equals(Material.STATIONARY_LAVA) && !chkBlock.equals(Material.FIRE))
                {
                    random = w.getBlockAt(x, highest, z).getLocation();
                    if (PreciousStones.API().flagAppliesToPlayer(player, FieldFlag.COMMAND_ON_ENTER, random));
                    else
                    {
                        danger = false;
                        random = w.getBlockAt(x, 255, z).getLocation();
                        break;
                    }
                }
            }
        }
        return random;
    }

    private Location randomNetherLocation(World nether) {
        boolean danger = true;
        Location random = null;
        while (danger == true) {
            int x = randomX();
            int z = randomZ();
            int y = 100;
            Block startBlock = nether.getBlockAt(x, y, z);
            while (!startBlock.getType().equals(Material.AIR)) {
                startBlock = startBlock.getRelative(BlockFace.DOWN);
            }
            int air = 0;
            while (startBlock.getType().equals(Material.AIR) && startBlock.getLocation().getBlockY() > 30) {
                startBlock = startBlock.getRelative(BlockFace.DOWN);
                air++;
            }
            Material id = startBlock.getType();
            if ((id.equals(Material.NETHERRACK) || id.equals(Material.SOUL_SAND) || id.equals(Material.GLOWSTONE) || id.equals(Material.NETHER_BRICK) || id.equals(Material.NETHER_FENCE) || id.equals(Material.NETHER_BRICK_STAIRS)) && air >= 4) {
                random = startBlock.getLocation();
                int randomLocY = random.getBlockY();
                random.setY(randomLocY + 1);
                danger = false;
                break;
            }
        }
        return random;
    }

    private Location randomTheEndLocation(World end) {
        boolean danger = true;
        Location random = null;
        while (danger == true) {
            int x = rand.nextInt(240);
            int z = rand.nextInt(240);
            x -= 120;
            z -= 120;
            // get the spawn point
            Location endSpawn = end.getSpawnLocation();
            int highest = end.getHighestBlockYAt(endSpawn.getBlockX() + x, endSpawn.getBlockZ() + z);
            if (highest > 40) {
                Block currentBlock = end.getBlockAt(x, highest, z);
                random = currentBlock.getLocation();
                danger = false;
                break;
            }
        }
        return random;
    }

    public void movePlayer(final Player p, Location l, World from) {

        final UUID uuid = p.getUniqueId();
        plugin.getListener().getTravellers().add(uuid);
        l.setY(l.getY() + 0.2);
        final Location theLocation = l;

        theLocation.setPitch(64);

        final World to = theLocation.getWorld();
        final boolean allowFlight = p.getAllowFlight();
        final boolean crossWorlds = from != to;

        // try loading chunk
        World world = l.getWorld();
        Chunk chunk = world.getChunkAt(l);
        while (!world.isChunkLoaded(chunk)) {
            world.loadChunk(chunk);
        }

        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
        {
            @Override
            public void run()
            {
                p.teleport(theLocation);
                p.getWorld().playSound(theLocation, Sound.ENTITY_ENDERMEN_TELEPORT, 1.0F, 1.0F);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "timetotp " + p.getName());
            }
        }, 5L);
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
        {
            @Override
            public void run()
            {
                if (plugin.getListener().getTravellers().contains(uuid))
                {
                    plugin.getListener().getTravellers().remove(uuid);
                }
            }
        }, 500L);
    }

    private int randomX() {
        int max = plugin.getConfig().getInt("max");
        int wherex;
        wherex = rand.nextInt(max);

        // add chance of negative values
        wherex *= 2;
        wherex -= max;

        return wherex;
    }

    private int randomZ() {
        int max = plugin.getConfig().getInt("max");
        int wherez;
        wherez = rand.nextInt(max);

        // add chance of negative values
        wherez *= 2;
        wherez -= max;

        return wherez;
    }

    public Location searchBiome(Player p, Biome b, World w) {
        Location l = null;
        int startx = p.getLocation().getBlockX();
        int startz = p.getLocation().getBlockZ();
        if (w == null) {
            w = p.getLocation().getWorld();
        }
        int limit = 30000;
        int step = plugin.getConfig().getInt("step");
        // search in a random direction
        Integer[] directions = new Integer[]{0, 1, 2, 3};
        Collections.shuffle(Arrays.asList(directions));
        for (int i = 0; i < 4; i++) {
            switch (directions[i]) {
                case 0:
                    // east
                    startx += plugin.getConfig().getInt("initial_step");
                    for (int east = startx; east < limit; east += step) {
                        Biome chkb = w.getBiome(east, startz);
                        if (chkb.equals(b)) {
                            p.sendMessage("[" + plugin.getPluginName() + "] " + b.toString() + " biome found in an easterly direction!");
                            return new Location(w, east, w.getHighestBlockYAt(east, startz), startz);
                        }
                    }
                    break;
                case 1:
                    startz += plugin.getConfig().getInt("initial_step");
                    // south
                    for (int south = startz; south < limit; south += step) {
                        Biome chkb = w.getBiome(startx, south);
                        if (chkb.equals(b)) {
                            p.sendMessage("[" + plugin.getPluginName() + "] " + b.toString() + " biome found in a southerly direction!");
                            return new Location(w, startx, w.getHighestBlockYAt(startx, south), south);
                        }
                    }
                    break;
                case 2:
                    // west
                    startx -= plugin.getConfig().getInt("initial_step");
                    for (int west = startx; west > -limit; west -= step) {
                        Biome chkb = w.getBiome(west, startz);
                        if (chkb.equals(b)) {
                            p.sendMessage("[" + plugin.getPluginName() + "] " + b.toString() + " biome found in a westerly direction!");
                            return new Location(w, west, w.getHighestBlockYAt(west, startz), startz);
                        }
                    }
                    break;
                case 3:
                    startz -= plugin.getConfig().getInt("initial_step");
                    // north
                    for (int north = startz; north > -limit; north -= step) {
                        Biome chkb = w.getBiome(startx, north);
                        if (chkb.equals(b)) {
                            p.sendMessage("[" + plugin.getPluginName() + "] " + b.toString() + " biome found in a northerly direction!");
                            return new Location(w, startx, w.getHighestBlockYAt(startx, north), north);
                        }
                    }
                    break;
            }
        }
        return l;
    }
}
