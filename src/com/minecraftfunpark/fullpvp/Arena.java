package com.minecraftfunpark.fullpvp;

import org.bukkit.Location;

import java.util.ArrayList;

public class Arena {

    private ArrayList<Location> spawns = new ArrayList<Location>();
    String name = null;
    String worldName = null;
    private ArrayList<Fighter> players = new ArrayList<Fighter>();
    private ArrayList<String> kitWhiteList = new ArrayList<String>();

    public Arena(Location spawn,String name, String worldName){
        spawns.add(spawn);
        this.name = name;
        this.worldName = worldName;
    }

    public void addAllowedKit(Kit kit){
        kitWhiteList.add(kit.getName());
    }

    public void removeAllowedKit(Kit kit){
        kitWhiteList.remove(kit.getName());
    }

    public Arena(ArrayList<Location> spawns,String name, String worldName){
        this.spawns.addAll(spawns);
        this.name = name;
        this.worldName = worldName;
    }

    public ArrayList<String> getKitWhiteList(){
        return kitWhiteList;
    }

    public void addSpawnPoint(Location location){
        if (location.getWorld().getName().equalsIgnoreCase(worldName)){
            spawns.add(location);
        } else {
            throw new NullPointerException();
        }
    }

    public Boolean testForKit(String name){
        return kitWhiteList.contains(name);
    }

    public ArrayList<Location> getSpawns() {
        return spawns;
    }

    public void removeSpawn(Location spawn){
        if (!(spawns.contains(spawn))){
            throw new NullPointerException();
        }
        spawns.remove(spawn);
    }
    
    public String getName(){
        return name;
    }

    public void addFighter(Fighter fighter){
        players.add(fighter);
    }

    public ArrayList<Fighter> getFighters(){
        return players;
    }

    public void removeFighter(Fighter fighter){
        players.remove(fighter);
    }


}