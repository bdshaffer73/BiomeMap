package com.timgaming.biomemap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Created by pheonix on 7/31/16.
 */
public class Main extends JavaPlugin {

    private CommandSender sender;
    private String sWorld;
    private World world;

    @Override
    public void onEnable() {
        //logic to be performed when the plugin is enabled
        getLogger().info("onEnable has been invoked!");

    }

    @Override
    public void onDisable() {
        //logic to be performed when the plugin is disabled
        getLogger().info("onDisable has been invoked!");

    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){

        this.sender = sender;

        if(cmd.getName().equalsIgnoreCase("Map-Biomes")) {
            if (!(sender instanceof Player)) {

                //Check for a specific sWorld
                try {
                    sWorld = args[0];

                    try {
                        world = Bukkit.getServer().getWorld(sWorld);

                    } catch (Exception e){
                        sender.sendMessage("That world does not exist.");
                        return false;
                    }

                } catch (Exception e) {
                    sender.sendMessage("On which world? Please specify a world as the first argument.");
                    sender.sendMessage("Available worlds are: " + Bukkit.getServer().getWorlds().toString());
                    return false;
                }

                //Generate a pixmap of the biomes of that sWorld
                mapBiomes();

                return true;

            } else {
                //TODO in-game command
                sender.sendMessage("This command can only be run by the server admin.");
            }
        }
        return false;
    }

    private void mapBiomes(){

        String line;
        String[] data;
        int minX = 0, maxX = 0, minZ = 0, maxZ = 0, w = 0, h = 0;

        //Get the file
        FileInputStream config;
        InputStreamReader stream;
        BufferedReader reader;

        try {
            config = new FileInputStream("mapConfig.yml");
            stream = new InputStreamReader(config);
            reader = new BufferedReader(stream);

            //Read it
            try {
                while ((line = reader.readLine()) != null){

                    line = line.replaceAll("\\s", "");
                    data = line.split("=");

                    //Set the map variables by the config data
                    switch (data[0]){
                        case "minX":
                            minX = Integer.parseInt(data[1]);
                            break;
                        case "minY":
                            minZ = Integer.parseInt(data[1]);
                            break;
                        case "maxX":
                            maxX = Integer.parseInt(data[1]);
                            break;
                        case "maxY":
                            maxZ = Integer.parseInt(data[1]);
                            break;
                        case "width":
                            w = Integer.parseInt(data[1]);
                            break;
                        case "height":
                            h = Integer.parseInt(data[1]);
                            break;
                        default:
                            break;
                    }

                }
            } catch (IOException e) {
                System.out.println("File could not be read.");
                e.printStackTrace();
            }

//            System.out.format("World Map: Min X= %d Max X= %d Min Y= %d Max Y= %d Pix Map: Width = %d Height = %d",
//                    minX, maxX, minZ, maxZ, w, h);

        } catch (FileNotFoundException e) {
            System.out.println("File could not be found.");
            e.printStackTrace();
        }

        BufferedImage map = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        int x, z;
        float xScale = (maxX - minX) / w;
        float zScale = (maxZ - minZ) / h;
        if(world != null) {
            for (int py = 0; py < h; py++) {
                for (int px = 0; px < w; px++) {

                    //Get the coordinates in the world that correspond to this pixel
                    x = (int)(px * xScale + minX);
                    z = (int)(py * zScale + minZ);

                    map.setRGB(px, py, biomeToColor(world.getBiome(x, z)));

                }
            }
        }

        File pixmap = new File(sWorld + ".png");
        try {
            ImageIO.write(map, "png", pixmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int biomeToColor(Biome biome){
        switch (biome){
            case FOREST_HILLS:
                return 5524256; //Forest Hills
            default:
                return 16777215; //White
        }
    }
}
