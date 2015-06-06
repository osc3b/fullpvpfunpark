package com.minecraftfunpark.fullpvp;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;

public class Kit{

    private String name = null;
    private ItemStack[] armour = null;
    private ItemStack[] inventory = null;
    private Collection<PotionEffect> potions = null;

    public Kit(String name, ItemStack[] armour, ItemStack[] inventory, Collection<PotionEffect> potions){
        this.name = name;
        this.armour = armour;
        this.inventory = inventory;
        this.potions = potions;
    }


    public void setArmour(ItemStack[] armour) {
        this.armour = armour;
    }

    public void setInventory(ItemStack[] inventory) {
        this.inventory = inventory;
    }

    public void setPotions(Collection<PotionEffect> potions) {
        this.potions = potions;
    }

    public Collection<PotionEffect> getPotions() {
        return potions;
    }

    public ItemStack[] getArmour() {
        return armour;
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public String getName() {
        return name;
    }


}