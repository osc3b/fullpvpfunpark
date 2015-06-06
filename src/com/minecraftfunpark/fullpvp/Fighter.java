package com.minecraftfunpark.fullpvp;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Fighter {

    private String displayName = null;
    private String name = null;
    private ItemStack[] oldInventory = null;
    private ItemStack[] oldArmour = null;
    private Kit kit = null;
    private Kit oldKit = null;
    private Arena arena = null;
    private int kills = 0;
    private int deaths = 0;
    private UUID playerUUID = null;
    private Location oldLocation = null;
    private Double health = 0.0;


    public Fighter(Player player, Kit kit, Arena arena){
        this.name = player.getName();
        this.oldInventory = player.getInventory().getContents().clone();
        this.oldArmour = player.getInventory().getArmorContents().clone();
        this.kit = kit;
        this.arena = arena;
        playerUUID = player.getUniqueId();
        oldLocation = player.getLocation();
        health = player.getHealth();
        displayName = player.getName();
        oldKit = kit;
    }

    public Kit getOldKit(){
        return oldKit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Location getOldLocation(){
        return oldLocation;
    }

    public Double getOldHealth(){
        return health;
    }

    public String getName() {
        return name;
    }

    public void setKit(Kit kit) {
        if (!(oldKit.equals(this.kit))){
            oldKit = kit;
        }
        this.kit = kit;
    }

    public void setArena(Arena arena) {
        this.arena = arena;
    }

    public Arena getArena() {
        return arena;
    }

    public Kit getKit() {
        return kit;
    }

    public ItemStack[] getOldArmour() {
        return oldArmour;
    }

    public ItemStack[] getOldInventory() {
        return oldInventory;
    }

    public UUID getPlayerUUID(){
        return playerUUID;
    }

    public void addDeaths(int deaths) {
        this.deaths = deaths + this.deaths;
    }

    public void addKills(int kills) {
        this.kills = kills + this.kills;
    }

    public int getKills(){
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }
}