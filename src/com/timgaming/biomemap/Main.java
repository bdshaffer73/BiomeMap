package com.timgaming.biomemap;

import net.minecraft.server.v1_9_R2.BiomeVoid;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

/**
 * Created by pheonix on 7/31/16.
 */
public class Main extends JavaPlugin {

    private CommandSender sender;
    private Player player;
    private String sWorld;
    private String settingsPath = "./plugins/BiomeMap/";
    private World world;
    private ArrayList<String> biomes;
    private ArrayList<Integer> bColors;

    @Override
    public void onEnable() {
        //logic to be performed when the plugin is enabled
    }

    @Override
    public void onDisable() {
        //logic to be performed when the plugin is disabled
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        this.sender = sender;

        if (cmd.getName().equalsIgnoreCase("Map-Biomes")) {
            if (!(sender instanceof Player)) {

                //Check for a specific world
                try {
                    sWorld = args[0];

                    try {
                        world = Bukkit.getServer().getWorld(sWorld);

                    } catch (Exception e) {
                        sender.sendMessage("That world does not exist.");
                        return false;
                    }

                } catch (Exception e) {
                    sender.sendMessage("On which world? Please specify a world as the first argument.");
                    sender.sendMessage("Available worlds are: " + Bukkit.getServer().getWorlds().toString());
                    return false;
                }

                sender.sendMessage("Mapping biomes...");
                //Generate a pixmap of the biomes of that world
                mapBiomes();
                sender.sendMessage("Biomes mapped!");

                return true;

            } else {
                player = (Player) sender;

                if(player.isOp()) {
                    world = player.getWorld();

                    player.sendMessage("Mapping biomes...");
                    //Generate a pixmap of the biomes of that world
                    mapBiomes();
                    player.sendMessage("Biomes mapped!");
                    return true;

                } else
                    sender.sendMessage("This command can only be run by the server admin.");
            }
        }
        return false;
    }

    private void mapBiomes() {

        String line;
        String[] data;
        int minX = 0, maxX = 0, minZ = 0, maxZ = 0;
        int size = 0;

        try {
            sender.sendMessage("Getting configuration...");
            FileInputStream config = new FileInputStream(settingsPath + "mapconfig.yml");
            InputStreamReader stream = new InputStreamReader(config);
            BufferedReader reader = new BufferedReader(stream);

            //Read it
            try {
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("#")) {
                        line = line.replaceAll("\\s", "");
                        data = line.split("=");

                        //Set the map variables by the config data
                        switch (data[0]) {
                            case "minX":
                                minX = Integer.parseInt(data[1]);
                                break;
                            case "minZ":
                                minZ = Integer.parseInt(data[1]);
                                break;
                            case "maxX":
                                maxX = Integer.parseInt(data[1]);
                                break;
                            case "maxZ":
                                maxZ = Integer.parseInt(data[1]);
                                break;
                            case "image-size":
                                size = Integer.parseInt(data[1]);
                                break;
                            default:
                                break;
                        }
                    }
                }
            } catch (IOException e) {
                sender.sendMessage("Settings file could not be read.");
            }

            try {
                reader.close();
                stream.close();
                config.close();
            } catch (IOException e) {
                sender.sendMessage("Config file failed to close.");
            }
        } catch (FileNotFoundException e) {
            sender.sendMessage("Settings file not found.");
        }

        //Create the image
        int x, z, scale, minXZ;
        float iWidth, iHeight, wWidth, wHeight, xScale, zScale;
        wWidth = maxX - minX;
        wHeight = maxZ - minZ;
        xScale = wWidth / size;//Calculate the number of blocks per pixel on the x-axis (scale)
        zScale = wHeight / size;//Calculate the number of blocks per pixel on the z-axis (scale)
        scale = (int) ((xScale > zScale) ? xScale : zScale);//keep the bigger scale
        minXZ = (Math.abs(minX) > Math.abs(minZ)) ? minX : minZ;//keep the more distant min coordinate
        iWidth = wHeight <= wWidth ? size : (wWidth / scale);
        iHeight = wWidth >= wHeight ? size : (wHeight / scale);
        BufferedImage map = new BufferedImage((int)iWidth, (int)iHeight, BufferedImage.TYPE_INT_RGB);

        sender.sendMessage("Creating a map of size " + (int)iWidth + "x" + (int)iHeight + ", with a resolution of " +
                scale + " block(s) per pixel.");
        sender.sendMessage("Gathering data...");

        setColors();

        if (world != null) {
            for (int py = 0; py < iHeight; py++) {
                for (int px = 0; px < iWidth; px++) {

                    //Get the coordinates in the world that correspond to this pixel
                    x = px * scale + minXZ;
                    z = py * scale + minXZ;

                    map.setRGB(px, py, biomeToColor(world.getBiome(x, z)));
                }
            }
        }

        sender.sendMessage("Saving file.");

        File pixmap = new File(settingsPath + sWorld + ".png");
        try {
            ImageIO.write(map, "png", pixmap);
        } catch (IOException e) {
            e.printStackTrace();
        }

        biomes.clear();
        bColors.clear();
    }

    /**
     * Searches the list of biomes for the biome provided, and returns an int conversion of the corresponding hex color code.
     *
     * @param biome The biome to be searched for.
     * @return The integer color code for the given biome.
     */
    private int biomeToColor(Biome biome) {

        int rgb = 1644825;//Dark gray
        String name;

        for(int i = 0; i < biomes.size(); i++){ //Find the biome in the list by the biome provided
            name = biomes.get(i);
            if(name.equals(biome.name())) {
                rgb = bColors.get(i);           //Gets the integer code of the color corresponding to the found biome.
                i = biomes.size();
            }
        }

        return rgb;
    }

    /**
     * Sets the list of color codes and
     */
    private void setColors() {

        String line;
        String[] data;

        biomes = new ArrayList<>();
        bColors = new ArrayList<>();

        try {
            FileInputStream table = new FileInputStream(settingsPath + "biomecolors.settings");
            InputStreamReader reader = new InputStreamReader(table);
            BufferedReader buffer = new BufferedReader(reader);

            try {
                while ((line = buffer.readLine()) != null) {
                    if (!line.startsWith("#")) {                //If it's not a comment
                        line = line.replaceAll("\\s", "");
                        data = line.split(":");
                        biomes.add(data[0]);                    //Add the biome name to the biomes list
                        bColors.add(Integer.decode(data[1]));   //Add the biome color (int) to the bColors list
                    }
                }
            } catch (IOException e) {
                sender.sendMessage("Color settings file could not be read.");
            }

            try {
                buffer.close();
                reader.close();
                table.close();
            } catch (IOException e) {
                sender.sendMessage("Color settings file failed to close.");
            }

        } catch (FileNotFoundException e) {
            sender.sendMessage("Color settings file not found.");
        }

    }
}
