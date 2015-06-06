package com.minecraftfunpark.fullpvp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DeathArenaAPI{

    Plugin plugin = null;
    ArrayList<Arena> arenaArrayList = new ArrayList<Arena>();
    ArrayList<Fighter> fighterList = new ArrayList<Fighter>();
    ArrayList<Kit> kitList = new ArrayList<Kit>();

    public DeathArenaAPI(Plugin plugin){
        this.plugin = plugin;
    }

    public void registerNewKit(String name, ItemStack[] armour, ItemStack[] inventory, Collection<PotionEffect> potions) {
        kitList.add(new Kit(name, armour, inventory, potions));
    }

    public void registerNewArena(Location spawn, String arenaName, String worldName) {
        arenaArrayList.add(new Arena(spawn, arenaName, worldName));
    }

    public void saveAllKits(){
        for (Kit kit : kitList){
            saveKit(kit);
        }
    }

    public void saveKit(Kit kit) {
        FileConfiguration cfg = null;
        File file = new File(plugin.getDataFolder() + "/kits/" + kit.getName() + ".kit");
        cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("inv", kit.getInventory());
        cfg.set("arm", kit.getArmour());
        cfg.set("pot", kit.getPotions());
        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveArena(Arena arena) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/arenas/" + arena.getName() + ".arena"));
        cfg.set("AllowedKits" , arena.getKitWhiteList());
        int x = 0;
        for (Location loc : arena.getSpawns()){
            String xi = String.valueOf(x);
            System.out.println("Saving arena #" + xi);
            cfg.set(xi + ".x", loc.getX());
            cfg.set(xi + ".y", loc.getY());
            cfg.set(xi + ".z", loc.getZ());
            cfg.set(xi + ".pitch", loc.getPitch());
            cfg.set(xi + ".yaw", loc.getYaw());
            cfg.set(xi + ".world", loc.getWorld().getName());
            x = x + 1;
        }
        cfg.set("int", Integer.valueOf(x - 1));
        try{
            cfg.save(new File(plugin.getDataFolder() + "/arenas/" + arena.getName() + ".arena"));
        } catch (Exception ignored ){
            ignored.printStackTrace();
        }
    }



    public void saveAllArenas(){
        for (Arena arena : arenaArrayList){
            try {
                saveArena(arena);
            }catch (Exception ignored){
                ignored.printStackTrace();
            }
        }
    }
    public void loadAllArenas() {
        File file = new File(plugin.getDataFolder() + "/arenas/");
        file.mkdirs();
        String[] cfgList = file.list();
        for (String cfgl : cfgList) {
            loadArena(cfgl.replace(".arena",""));
        }
    }
    public void loadAllKits() {
        File file = new File(plugin.getDataFolder() + "/kits/");
        file.mkdirs();
        String[] cfgList = file.list();
        for (String cfgl : cfgList) {
            kitList.add(loadKit(cfgl.replace(".kit", "")));
        }
    }

    public void loadArena(String name){
        ArrayList<Location> locList = new ArrayList<Location>();
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder() + "/arenas/" + name + ".arena"));
        List<String> strList = (List<String>) cfg.getList("AllowedKits");
        int p = cfg.getInt("int");
        for (int i = 0; i <= p; i++){
            double x = cfg.getDouble(i + ".x");
            double y = cfg.getDouble(i + ".y");
            double z = cfg.getDouble(i + ".z");
            float pitch = (float)cfg.getDouble(i + ".pitch");
            float yaw = (float)cfg.getDouble(i + ".yaw");
            String world = cfg.getString(i + ".world");
            locList.add(new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch));
        }
        String worldn = cfg.getString("0.world");
        Arena arena = new Arena(locList,name,worldn);
        for (String str : strList){
            try{
                arena.addAllowedKit(getKit(str));
            }catch(Exception ignored){}
        }
        arenaArrayList.add(arena);
    }

    public void deletekit(Kit k){
        kitList.remove(k);
    }


    public Kit loadKit(String s) {
        try {
            FileConfiguration cfg = null;
            File file = new File(plugin.getDataFolder() + "/kits/" + s + ".kit");
            cfg = YamlConfiguration.loadConfiguration(file);
            List<ItemStack> Inv = (List<ItemStack>) cfg.getList("inv");
            ItemStack[] KitInventory = (ItemStack[]) Inv.toArray(new ItemStack[36]);
            List<ItemStack> Arm = (List<ItemStack>) cfg.getList("arm");
            ItemStack[] ArmInventory = (ItemStack[]) Arm.toArray(new ItemStack[4]);
            Collection<PotionEffect> PotionEffects = (Collection<PotionEffect>) cfg.getList("pot");
            Kit kit = new Kit(s, ArmInventory, KitInventory, PotionEffects);
            return kit;
        }catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    public void removeArena(Arena arena){
        arenaArrayList.remove(arena);
    }

    public Arena getArena(String name){
        for (Arena arena : arenaArrayList){
            if (arena.getName().equalsIgnoreCase(name)){
                return arena;
            }
        }
        return null;
    }

    public ArrayList<Arena> getArenas(){
        return arenaArrayList;
    }

    public void addFighter(Player player, Kit kit, Arena arena){
        Fighter fighter = new Fighter(player, kit, arena);
        fighterList.add(fighter);
        arena.addFighter(fighter);
    }


    public Boolean testForFighter(String name){
        for (Fighter fighter : fighterList){
            if (fighter.getName().equalsIgnoreCase(name)){
                return true;
            }
        }
        return false;
    }

    public void removeFighter(Fighter fighter){
        if (fighterList.contains(fighter)){
            fighterList.remove(fighter);
            fighter.getArena().removeFighter(fighter);
        }
    }

    public void changeFighterArena(Fighter fighter, Arena newArena){
        if (fighterList.contains(fighter)){
            fighter.getArena().removeFighter(fighter);
            newArena.addFighter(fighter);
        }
    }

    public void allowArena( Kit kit, Arena arena){
        arena.addAllowedKit(kit);
    }

    public void allowArenas( Kit kit, ArrayList<Arena> arenas){
        for (Arena arena : arenas) {
            arena.addAllowedKit(kit);
        }
    }

    public Boolean checkFighter(Player player){
        for (Fighter f : fighterList){
            if (f.getName().equalsIgnoreCase(player.getName())){
                return true;
            }
        }
        return false;
    }

    public void denyArena( Kit kit,Arena arena){
        arena.removeAllowedKit(kit);
    }


    public Kit getKit(String name){
        for (Kit kit : kitList){
            if (kit.getName().equalsIgnoreCase(name)){
                return kit;
            }
        }
        return null;
    }

    public void addArenaSpawn(Arena arena, Location spawnPoint){
        arena.addSpawnPoint(spawnPoint);
    }

    //@param spawnPoint HAS to be a spawn point, has to be THE ACTUAL instance.
    public void removeArenaSpawn(Arena arena, Location spawnPoint){
        arena.removeSpawn(spawnPoint);
    }

    public Fighter getFighter(Player player){
        for (Fighter f : fighterList){
            if (f.getName().equalsIgnoreCase(player.getName())){
                return f;
            }
        }
        return null;
    }

    public ArrayList<Kit> getKitList(){
        return kitList;
    }

    public Boolean testKit(String name){
        for (Kit kit : kitList){
            if (kit.getName().equalsIgnoreCase(name)){
                return true;
            }
        }
        return false;
    }

    public Boolean testArena(String name){
        for (Arena arena : arenaArrayList){
            if (name.equalsIgnoreCase(arena.getName())){
                return true;
            }
        }
        return false;
    }

    public ArrayList<Fighter> getFighterList(){
        return fighterList;
    }
}