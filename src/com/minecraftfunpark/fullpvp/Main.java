package com.minecraftfunpark.fullpvp;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener{
    Plugin plugin = this;

    DeathArenaAPI dapi = new DeathArenaAPI(this);

    ArrayList<Fighter> fighters = new ArrayList<Fighter>();
    HashMap<UUID,Integer> killMap = new HashMap<UUID, Integer>();
    HashMap<UUID,Integer> deathMap = new HashMap<UUID, Integer>();

    boolean dropItemOnDeath;
    int itemDropID;
    int itemDropPotAmplifier;
    int itemDropPotDuration;
    short itemDropDamage;

    public String translateColourCodes(String string){
        return string.replace("&","§");
    }

    public void onEnable(){
        Updater updater;
        getConfig().options().copyDefaults(true);
        saveConfig();
        getConfig().addDefault("EnableAutoUpdater",true);
        if (false) {
            if (getConfig().getBoolean("EnableAutoUpdater")) {
                int myID = 72548;
                updater = new Updater(this, myID, getFile(), Updater.UpdateType.DEFAULT, true);
            } else {
                getLogger().warning("[KitPvP] The auto-updater is not enabled in the KitPvP config.");
            }
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        dapi.loadAllKits();
        dapi.loadAllArenas();
        loadAllStats();
        dropItemOnDeath = getConfig().getBoolean("DropItemOnDeath");
        itemDropID = getConfig().getInt("DropItemID");
        itemDropPotAmplifier = getConfig().getInt("DropItemRegenPotency");
        itemDropPotDuration = getConfig().getInt("DropItemRegenDuration");
        itemDropDamage = (short) getConfig().getInt("DropItemDamageValue");
    }

    public void onDisable(){
        dapi.saveAllArenas();
        dapi.saveAllKits();
        for (UUID fighter : killMap.keySet()){
            saveStats(fighter);
        }
    }

    public void loadAllStats(){
        FileConfiguration cfg = null;
        File file = new File(this.getDataFolder() + "/stats/all.stats");
        cfg = YamlConfiguration.loadConfiguration(file);
        for (String s : cfg.getKeys(false)) {
            UUID playerUUID = UUID.fromString(s);
            int kills = cfg.getInt(s + ".kills");
            int deaths = cfg.getInt(s + ".deaths");
            killMap.put(playerUUID,kills);
            deathMap.put(playerUUID,deaths);
        }
    }

    public void saveStats(UUID uuid){
        FileConfiguration cfg = null;
        File file1 = new File(this.getDataFolder() + "/stats/");
        file1.mkdirs();
        File file = new File(this.getDataFolder() + "/stats/all.stats");
        cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set(uuid + ".kills",killMap.get(uuid));
        cfg.set(uuid + ".deaths",deathMap.get(uuid));
        cfg.set(uuid + ".name", Bukkit.getOfflinePlayer(uuid).getName());
        try {
            cfg.save(file);
        }catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    public void Join(Player player, Arena arena, Kit kit){
        if (dapi.testForFighter(player.getName())){
            return;
        }
        dapi.addFighter(player, kit, arena);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        for (PotionEffectType potionEffect : PotionEffectType.values()){
            try {
                player.removePotionEffect(potionEffect);
            } catch (Exception ignore){}
        }
        player.teleport(arena.getSpawns().get(randInt(0,arena.getSpawns().size() - 1)), PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.getInventory().setContents(kit.getInventory());
        player.getInventory().setArmorContents(kit.getArmour());
        player.addPotionEffects(kit.getPotions());
        player.sendMessage(translateColourCodes(getConfig().getString("joinMessage")));
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setGameMode(GameMode.SURVIVAL);
        player.updateInventory();
    }

    public void Leave(Fighter fighter){
        Player player = Bukkit.getPlayer(fighter.getName());
        dapi.removeFighter(fighter);
        player.getInventory().setArmorContents(null);
        player.getInventory().setArmorContents(null);
        for (PotionEffectType potionEffect : PotionEffectType.values()){
            try {
                player.removePotionEffect(potionEffect);
            } catch (Exception ignore){}
        }
        player.teleport(fighter.getOldLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.getInventory().setArmorContents(fighter.getOldArmour());
        player.getInventory().setContents(fighter.getOldInventory());
        player.setHealth(fighter.getOldHealth());
        player.sendMessage(translateColourCodes(getConfig().getString("leaveMessage")));
        player.updateInventory();
    }


    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent e) {
        if (e.getItem().equals(Material.ARROW)) return;
        if (dapi.testForFighter(e.getPlayer().getName())) {
            if (e.getItem().getItemStack().getTypeId() == itemDropID){
                PotionEffect potion = new PotionEffect(PotionEffectType.REGENERATION,itemDropPotDuration,itemDropPotAmplifier);
                e.getPlayer().addPotionEffect(potion);
                e.setCancelled(true);
                e.getItem().remove();
                e.getPlayer().playSound(e.getPlayer().getLocation().add(0,-1,0),Sound.ITEM_PICKUP,1.0F,1.0F);
                return;
            }
            if (getConfig().getBoolean("denyItemPickup")){
              e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event){
        if (dapi.testForFighter(event.getEntity().getName())){
            if (getConfig().getBoolean("keepFood")) {
                Player player = (Player) event.getEntity();
                event.setCancelled(true);
                player.setFoodLevel(getConfig().getInt("foodLevel"));
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event){
        if (dapi.testForFighter(event.getPlayer().getName())){
            Leave(dapi.getFighter(event.getPlayer()));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e){
        if (e.getBlock().getType().equals(Material.WEB)){
            return;
        }
        if (dapi.testForFighter(e.getPlayer().getName())){
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSignUpdate(SignChangeEvent e){
        if (!e.getPlayer().hasPermission("kitpvp.signs")) {
            return;
        }
        if (!e.getLine(0).equalsIgnoreCase(getConfig().getString("SignStart"))) {
            return;
        }
        String begin = getConfig().getString("SignStart");
        String prefix = translateColourCodes(getConfig().getString("SignColor"));
        String type = e.getLine(1);
        Player player = e.getPlayer();
        if (type.isEmpty()){
            player.sendMessage(ChatColor.RED + "You have to specialise this sign.");
            e.getBlock().breakNaturally();
            player.sendMessage(ChatColor.BLUE + "Can be: " + ChatColor.GRAY + "stats, join, leave, kit or arena");
            return;
        }
        if (type.equalsIgnoreCase("stats")){
            e.setLine(0,prefix + begin);
            player.sendMessage(ChatColor.BLUE + "Stats sign created!");
        }else if (type.equalsIgnoreCase("join")){
            if (e.getLine(2).isEmpty()){
                player.sendMessage(ChatColor.RED + "You've got to specify a kit.");
                return;
            }
            if (e.getLine(3).isEmpty()){
                player.sendMessage(ChatColor.RED + "You've got to specify an arena.");
                return;
            }
            String kitName = e.getLine(2);
            String arenaName = e.getLine(3);
            if (!dapi.testKit(kitName)){
                player.sendMessage(ChatColor.RED + "The kit "+ ChatColor.GRAY + kitName + ChatColor.RED + " was not valid");
                e.getBlock().breakNaturally();
                return;
            }
            if (!dapi.testArena(arenaName)){
                player.sendMessage(ChatColor.RED + "The arena "+ ChatColor.GRAY + arenaName + ChatColor.RED + " was not valid");
                e.getBlock().breakNaturally();
                return;
            }
            Arena arena = dapi.getArena(arenaName);
            if (!arena.testForKit(kitName)){
                player.sendMessage(ChatColor.RED + "The kit you specified is not allowed in the arena.");
            }
            e.setLine(0,prefix + begin);
            e.getPlayer().sendMessage(ChatColor.GREEN + "You have successfully created a join sign!");
        }else if (type.equalsIgnoreCase("leave")){
            e.setLine(0,prefix + begin);
        }else if (type.equalsIgnoreCase("kit")){
            String kitName = e.getLine(2);
            if (!dapi.testKit(kitName)){
                e.getPlayer().sendMessage(ChatColor.RED + "That kit was not recognised.");
                e.getBlock().breakNaturally();
                return;
            }
            e.setLine(0, prefix + begin);
        }else if (type.equalsIgnoreCase("arena")){

        } else {
            e.getPlayer().sendMessage(ChatColor.RED + "Error: Can only be 'stats', 'join', 'leave', 'kit' or 'arena'");
            e.getBlock().breakNaturally();
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e)
    {
        if (dapi.testForFighter(e.getPlayer().getName())){
            Block b = e.getBlock();
            ItemStack s = new ItemStack(b.getType());

            final Block bl = e.getBlock();
            final Block block = e.getBlockReplacedState().getBlock();
            if (s.getType().equals(Material.WEB)) {
                getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new BukkitRunnable()
                {
                    public void run()
                    {
                        bl.getWorld().getBlockAt(bl.getLocation()).breakNaturally(new ItemStack(Material.AIR));
                    }
                }, getConfig().getLong("CobwebDestroyTimeInSeconds") * 20L);
            }
            if (s.getType().equals(new ItemStack(Material.TNT).getType())){
                Material material = e.getBlockReplacedState().getType();
                b.setType(material);
                Location loc = e.getBlockPlaced().getLocation().add(0.0D, 1.0D, 0.0D);
                World w = loc.getWorld();
                TNTPrimed tntPrimed = (TNTPrimed) w.spawnEntity(loc, EntityType.PRIMED_TNT);
                tntPrimed.setFuseTicks(getConfig().getInt("TNTFuseTicks"));
                Location loc1 = e.getBlockPlaced().getLocation();
                loc1.getWorld().playSound(loc, Sound.FUSE, 1.0F, 1.0F);
            }
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent e){
        final Location loc = e.getBlock().getLocation();
        if (e.getCause().equals(BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL)){
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new BukkitRunnable() {
                @Override
                public void run() {
                    loc.getWorld().getBlockAt(loc).setType(Material.AIR);
                }
            },getConfig().getInt("FireDestroyTimeTicks"));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e){
        if (!(dapi.testForFighter(e.getPlayer().getName()))) return;
        if (!(getConfig().getBoolean("disallowItemDropping"))) return;
        e.setCancelled(true);
        e.getPlayer().sendMessage(translateColourCodes(getConfig().getString("noItemDropMessage")));
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e){
        if (e.getMessage().startsWith("/kitpvp")){
            return;
        }
        if (e.getMessage().equalsIgnoreCase("/leave")) {
            if (dapi.testForFighter(e.getPlayer().getName())) {
                Leave(dapi.getFighter(e.getPlayer()));
                e.setCancelled(true);
                return;
            }
        }
        if (dapi.testForFighter(e.getPlayer().getName())) {
            for (String str : getConfig().getStringList("CommandWhiteList")) {
                if (e.getMessage().equalsIgnoreCase("/" + str)) {
                    return;
                }
            }
            e.getPlayer().sendMessage(translateColourCodes(getConfig().getString("commandDenied")));
            e.setCancelled(true);
        }


    }


    @EventHandler
    public void onEntityExplodeEvent(EntityExplodeEvent e){

        ArrayList<World> worldList = new ArrayList<World>();
        for (String s : getConfig().getStringList("EnableTNTInTheseWorlds")){
            worldList.add(Bukkit.getWorld(s));
        }
        if (worldList.contains(e.getLocation().getWorld())){
            try{
                e.setCancelled(true);

            }catch (Exception ex){
                e.setCancelled(true);
                return;
            }
            if (e.getEntity().getType().equals(EntityType.PRIMED_TNT)){
                e.setCancelled(true);
                World w = e.getEntity().getLocation().getWorld();
                w.createExplosion(e.getLocation(), 0F);
            }
        }
    }

    public void addDeath(Fighter fighter){
        int deaths = 0;
        if (deathMap.containsKey(fighter.getPlayerUUID())){
            deaths = deathMap.get(fighter.getPlayerUUID());
            deathMap.remove(fighter.getPlayerUUID());
        }
        deathMap.put(fighter.getPlayerUUID(), (deaths + 1));
    }

    public void addKill(Fighter fighter){
        int kills = 0;
        if (killMap.containsKey(fighter.getPlayerUUID())){
            kills = killMap.get(fighter.getPlayerUUID());
            killMap.remove(fighter.getPlayerUUID());
        }
        killMap.put(fighter.getPlayerUUID(),(kills + 1));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e){
        if (!(dapi.testForFighter(e.getEntity().getName()))) return;
        e.getDrops().clear();
        if (dropItemOnDeath) {
            e.getDrops().add(new ItemStack(itemDropID, 1, itemDropDamage));
        }
        reloadConfig();
        e.setDeathMessage(null);
        Fighter victim = dapi.getFighter(e.getEntity());
        boolean killedByFighter;
        try {
            killedByFighter = dapi.testForFighter(e.getEntity().getKiller().getName());
        } catch (Exception ex){
            killedByFighter = false;
        }
        addDeath(victim);
        Player p = Bukkit.getPlayer(victim.getPlayerUUID());
        p.getInventory().setArmorContents(null);
        p.getInventory().clear();
        for (PotionEffectType pot: PotionEffectType.values()){
            try {
                p.removePotionEffect(pot);
            } catch (Exception ignore){}
        }
        if (killedByFighter){
            addKill(dapi.getFighter(e.getEntity().getKiller()));
            Fighter killer = dapi.getFighter(e.getEntity().getKiller());
            if (getConfig().getBoolean("broadcastKill")){
                boolean inGame = getConfig().getBoolean("onlyInGame");
                boolean realName = getConfig().getBoolean("useActualPlayerNamesInTheKillMessage");
                String message = getConfig().getString("killMessage");
                if (message.contains("%killername%")){
                    if (realName) {
                        message = message.replace("%killername%", killer.getDisplayName());
                    } else {
                        message = message.replace("%killername%", killer.getName());
                    }
                }
                if (message.contains("%victimname%")){
                    if (realName) {
                        message = message.replace("%victimname%", victim.getDisplayName());
                    } else {
                        message = message.replace("%victimname%", victim.getName());
                    }
                }
                if (message.contains("%killerkit%")){
                    message = message.replace("%killerkit%",killer.getOldKit().getName());
                }
                if (message.contains("%victimkit%")){
                    message = message.replace("%victimkit%", victim.getOldKit().getName());
                }
                if (message.contains("%health%")){
                    message = message.replace("%health%", String.valueOf(Bukkit.getPlayer(killer.getPlayerUUID()).getHealth())); //toString
                }
                if (message.contains("%hearts%")){
                    Player player = Bukkit.getPlayer(killer.getPlayerUUID());
                    Long health = Math.round(player.getHealth());
                    Double newHealth = Double.valueOf(health);
                    String hearts = String.valueOf(newHealth / 2);
                    message = message.replace("%hearts%", hearts);
                }
                if (inGame){
                    for (Fighter fighter : dapi.getFighterList()){
                        Bukkit.getPlayer(fighter.getPlayerUUID()).sendMessage(translateColourCodes(message));
                    }
                } else {
                    Bukkit.getServer().broadcast(translateColourCodes(message),"kitpvp.broadcastKill");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onReSpawn(PlayerRespawnEvent e){
        if (!(dapi.testForFighter(e.getPlayer().getName()))) return;
        Fighter fighter = dapi.getFighter(e.getPlayer());
        final Player player = e.getPlayer();
        Arena arena = fighter.getArena();
        final Kit kit = fighter.getKit();
        e.setRespawnLocation(arena.getSpawns().get(randInt(0, arena.getSpawns().size() - 1)));
        player.getInventory().setContents(kit.getInventory());
        player.getInventory().setArmorContents(kit.getArmour());
        int i = Bukkit.getScheduler().scheduleSyncDelayedTask(this, new BukkitRunnable() {
            @Override
            public void run() {
                player.addPotionEffects(kit.getPotions());
            }
        },5L);
        player.updateInventory();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        if (e.getAction().equals(Action.LEFT_CLICK_BLOCK) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            if ((e.getClickedBlock().getState().getType().equals(Material.SIGN_POST)) || (e.getClickedBlock().getState().getType().equals(Material.WALL_SIGN))) {
                Sign sign = (Sign) e.getClickedBlock().getState();
                if (sign.getLine(0).equalsIgnoreCase(translateColourCodes(getConfig().getString("SignColor")) + getConfig().getString("SignStart"))) {
                    String line1 = sign.getLine(1);
                    Player player = e.getPlayer();
                    if (line1.equalsIgnoreCase("join")) {
                        if (sign.getLine(2).isEmpty() || sign.getLine(3).isEmpty()) {
                            e.getPlayer().sendMessage(ChatColor.RED + "That sign does not have the correct parameters.");
                            e.getPlayer().sendMessage(ChatColor.BLUE + translateColourCodes("It should contain the kit &nand&1 the arena."));
                            return;
                        }
                        String kitName, arenaName;
                        kitName = sign.getLine(2);
                        arenaName = sign.getLine(3);
                        if (!dapi.testKit(kitName)) {
                            e.getPlayer().sendMessage(ChatColor.RED + "That kit (line 3) was not recognised.");
                            return;
                        }
                        if (!dapi.testArena(arenaName)) {
                            e.getPlayer().sendMessage(ChatColor.RED + "The arena (Line 4) was not recognised.");
                            return;
                        }

                        Kit kit = dapi.getKit(kitName);
                        Arena arena = dapi.getArena(arenaName);

                        if (!(e.getPlayer().hasPermission("kitpvp.kit." + kitName))) {
                            e.getPlayer().sendMessage(ChatColor.RED + "You do not have permission for that kit.");
                            return;
                        }
                        if (!(e.getPlayer().hasPermission("kitpvp.arena." + arenaName))) {
                            e.getPlayer().sendMessage(ChatColor.RED + "You do not have permission for that arena.");
                            return;
                        }
                        if (!arena.testForKit(kit.getName())) {
                            e.getPlayer().sendMessage(ChatColor.RED + "The kit (line 3) was not accepted by the arena (line 4).");
                            return;
                        }
                        Join(e.getPlayer(), arena, kit);
                    }else if (sign.getLine(1).equalsIgnoreCase("leave")){
                        if (!(dapi.checkFighter(e.getPlayer()))){
                            player.sendMessage(ChatColor.RED + "No estas jugando.");
                            return;
                        }
                        Leave(dapi.getFighter(player));
                    } else if (sign.getLine(1).equalsIgnoreCase("kit")){
                        if (sign.getLine(2).isEmpty()) {
                            e.getPlayer().sendMessage(ChatColor.RED + "That sign does not have the correct parameters.");
                            e.getPlayer().sendMessage(ChatColor.BLUE + translateColourCodes("It should contain the kit."));
                            return;
                        }
                        String kitName, arenaName;
                        kitName = sign.getLine(2);
                        arenaName = dapi.getFighter(player).getArena().getName();
                        Fighter fighter = dapi.getFighter(player);
                        if (!dapi.testKit(kitName)) {
                            e.getPlayer().sendMessage(ChatColor.RED + "That kit (line 3) was not recognised.");
                            return;
                        }
                        Kit kit = dapi.getKit(kitName);
                        Arena arena = dapi.getArena(arenaName);

                        if ((!(e.getPlayer().hasPermission("kitpvp.kit." + kitName))) && (!(e.getPlayer().hasPermission("kitpvp.kit.*")))) {
                            e.getPlayer().sendMessage(ChatColor.RED + "No tienes permiso para usar este kit.");
                            return;
                        }
                        if (!arena.testForKit(kit.getName())) {
                            e.getPlayer().sendMessage(ChatColor.RED + "Este kit no puede ser utilizado en esta arena.");
                            return;
                        }
                        fighter.setKit(kit);
                        player.sendMessage(ChatColor.BLUE + "Tu kit cambiara la proxima vez que reaparezcas.");
                    } else if (sign.getLine(1).equalsIgnoreCase("arena")){
                        Fighter fighter = dapi.getFighter(player);
                        if (sign.getLine(2).isEmpty()) {
                            e.getPlayer().sendMessage(ChatColor.RED + "That sign does not have the correct parameters.");
                            e.getPlayer().sendMessage(ChatColor.BLUE + translateColourCodes("It should contain the arena."));
                            return;
                        }
                        String kitName, arenaName;
                        kitName = dapi.getFighter(player).getName();
                        arenaName = sign.getLine(3);
                        if (!dapi.testArena(arenaName)) {
                            e.getPlayer().sendMessage(ChatColor.RED + "The arena (Line 3) was not recognised.");
                            return;
                        }

                        Kit kit = dapi.getKit(kitName);
                        Arena arena = dapi.getArena(arenaName);
                        if ((!(e.getPlayer().hasPermission("kitpvp.arena." + arenaName)))  && (!(e.getPlayer().hasPermission("kitpvp.arena.*")))) {
                            e.getPlayer().sendMessage(ChatColor.RED + "You do not have permission for that arena.");
                            return;
                        }
                        if (!arena.testForKit(kit.getName())) {
                            e.getPlayer().sendMessage(ChatColor.RED + "The arena (line 3) does not accept the kit you currently have.");
                            return;
                        }
                        player.sendMessage(ChatColor.BLUE + "You will have the new arena next time you respawn.");
                        fighter.setArena(arena);
                    }
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("kitpvp") || cmd.getName().equalsIgnoreCase("deatharena")){

            Boolean isPlayer = sender instanceof Player;
            if (args.length == 0){
                sender.sendMessage(ChatColor.GREEN + "=-=-=-= =-= KitPvP help =-= =-=-=-=");
                sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp join <arena> <kit> " + ChatColor.AQUA + "Join an arena with the specified kit.");
                sender.sendMessage(ChatColor.DARK_BLUE + "/kitpvp leave " + ChatColor.BLUE + "Leaves your current game.");
                sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp stats [Player] " + ChatColor.AQUA + "Tells you the [Player]'s stats. Yours if left blank.");
                sender.sendMessage(ChatColor.DARK_BLUE + "/kitpvp listKits " + ChatColor.BLUE + "Returns a list of the kits");
                sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp manageArena <ArenaName> <help|addSpawn|removeNearestSpawn|addKit|removeKit> [arg] " + ChatColor.AQUA +
                        "Does the specified action to the <Arena>.  Use [arg] for \"addKit\" and for \"removeKit\".");
                sender.sendMessage(ChatColor.DARK_BLUE + "/kitpvp setinv <KitName> [[-a]] " + ChatColor.BLUE + "Sets the [kit] to your inventory. Add -a to enable in all arenas.");
                sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp addarena <arenaName> " + ChatColor.AQUA + "Adds an arena with a spawnpoint where you're standing.");
                sender.sendMessage(ChatColor.DARK_BLUE + "/kitpvp delArena <ArenaName> " + ChatColor.BLUE + "Permanently deletes an arena." +
                        ChatColor.RED +  " Warning: cannot be undone.");
                return true;
            }
            if (args[0].equalsIgnoreCase("help")){
                sender.sendMessage(ChatColor.GREEN + "=-=-=-= =-= KitPvP help =-= =-=-=-=");
                sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp join <arena> <kit> " + ChatColor.AQUA + "Join an arena with the specified kit.");
                sender.sendMessage(ChatColor.DARK_BLUE + "/kitpvp leave " + ChatColor.BLUE + "Leaves your current game.");
                sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp stats [Player] " + ChatColor.AQUA + "Tells you the [Player]'s stats. Yours if left blank.");
                sender.sendMessage(ChatColor.DARK_BLUE + "/kitpvp setinv <KitName> [[-a]] " + ChatColor.BLUE + "Sets the [kit] to your inventory. Add -a to enable in all arenas.");
                sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp addarena <arenaName> " + ChatColor.AQUA + "Adds an arena with a spawnpoint where you're standing.");
                sender.sendMessage(ChatColor.DARK_BLUE + "/kitpvp listKits " + ChatColor.BLUE + "Returns a list of the kits");
                sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp manageArena <ArenaName> <help|addSpawn|removeNearestSpawn|addKit|removeKit> [arg] " + ChatColor.AQUA +
                        "Does the specified action to the <Arena>.  Use [arg] for \"addKit\" and for \"removeKit\".");
                sender.sendMessage(ChatColor.DARK_BLUE + "/kitpvp delArena <ArenaName> " + ChatColor.BLUE + "Permanently deletes an arena." +
                        ChatColor.RED +  " Warning: cannot be undone.");
                return true;
            }
            String arg1 = args[0];
            if (arg1.equalsIgnoreCase("join")){
                if (!isPlayer){
                    sender.sendMessage(ChatColor.RED + "Sorry, the console cannot join a game.");
                    return true;
                }
                if (!(sender.hasPermission("kitpvp.join"))){
                    sender.sendMessage(ChatColor.RED + "You require " + ChatColor.DARK_RED + "kitpvp.join" + ChatColor.RED +
                            " to perform this command.");
                    return true;
                }
                if (args.length != 3){
                    if (args.length > 3){
                        sender.sendMessage(ChatColor.RED + "Too many arguments. Use the command like this:");
                    } else {
                        sender.sendMessage(ChatColor.RED + "Not enough arguments. Use the command like this:");
                    }
                    sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp join <arena> <kit> " + ChatColor.AQUA + "Join an arena with the specified kit.");
                    return true;
                }
                Player player = (Player) sender;
                String arenaName = args[1];
                String kitName = args[2];
                if ((!dapi.testKit(kitName)) || (!(sender.hasPermission("kitpvp.kit." + kitName)))  || (!(sender.hasPermission("kitpvp.kit.*")))){
                    player.sendMessage(ChatColor.RED + "That kit is not valid.  Here are a list of kits that you have access to.");
                    StringBuilder strB = new StringBuilder();
                    for (Kit k :dapi.getKitList()){
                        if (player.hasPermission("kitpvp.kit." + k.getName()) ||   (player.hasPermission("kitpvp.kit.*"))){
                            strB.append(k.getName());
                            strB.append(", ");
                        }
                    }
                    player.sendMessage(ChatColor.GRAY + strB.toString());
                    return true;
                }
                if ((!dapi.testArena(arenaName)) ||  (!(sender.hasPermission("kitpvp.arena.*"))) || (!(sender.hasPermission("kitpvp.arena." + arenaName)))){
                    player.sendMessage(ChatColor.RED + "That arena is not valid.  Here are a list of arenas that you have access to.");
                    StringBuilder strB = new StringBuilder();
                    for (Arena a :dapi.getArenas()){
                        if (player.hasPermission("kitpvp.arena." + a.getName())){
                            strB.append(a.getName() + ", ");
                        }
                    }
                    player.sendMessage(ChatColor.GRAY + strB.toString());
                    return true;
                }
                if (!(dapi.getArena(arenaName).testForKit(kitName))){
                    player.sendMessage(ChatColor.RED + "That kit is not available for that arena.");
                    return true;
                }
                Arena arena = dapi.getArena(arenaName);
                Kit kit = dapi.getKit(kitName);
                Join(player,arena,kit);

            }else if (arg1.equalsIgnoreCase("leave")){
                if (!(sender instanceof Player)){
                    sender.sendMessage(ChatColor.RED + "The console cannot leave. MUHAHAHAHAHA");
                    return true;
                }
                Player player = (Player) sender;
                if (dapi.testForFighter(player.getName())){
                    Leave(dapi.getFighter(player));
                } else {
                    sender.sendMessage(ChatColor.RED + "You're not in-game.");
                }

            }else if (arg1.equalsIgnoreCase("stats")){
                if (args.length == 1){ //without playername
                    if (!isPlayer){
                        sender.sendMessage(ChatColor.RED + "You cannot check your stats. Follow with a playername to check theirs.");
                        return true;
                    }
                    Player player = (Player) sender;
                    UUID uuid = player.getUniqueId();
                    String kills = translateColourCodes(getConfig().getString("Stats_kills_self"));
                    if (killMap.containsKey(uuid)){
                        kills = kills.replaceAll("%kills%",String.valueOf(killMap.get(uuid)));
                    } else {
                        kills = kills.replaceAll("%kills%","N/A");
                    }
                    player.sendMessage(kills);
                    String deaths = translateColourCodes(getConfig().getString("Stats_deaths_self"));
                    if (deathMap.containsKey(uuid)){
                        deaths = deaths.replaceAll("%deaths%",String.valueOf(deathMap.get(uuid)));
                    } else {
                        deaths = deaths.replaceAll("%kills%","N/A");
                    }
                    player.sendMessage(deaths);
                    String KDR = translateColourCodes(getConfig().getString("Stats_KillToDeathRatio_self"));
                    if (killMap.containsKey(uuid) && deathMap.containsKey(uuid)){
                        KDR = KDR.replaceAll("%KDR%",String.valueOf(Double.valueOf(killMap.get(uuid)) / (Double.valueOf(deathMap.get(uuid)))));
                    } else {
                        KDR = KDR.replaceAll("%kills%","N/A");
                    }
                    player.sendMessage(KDR);
                    return true;
                }else {
                    UUID uuid = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
                    if (uuid == null){
                        sender.sendMessage(ChatColor.RED + "Unknown player.");
                        return true;
                    }
                    String kills = translateColourCodes(getConfig().getString("Stats_kills_other"));
                    kills = kills.replaceAll("%playername%", args[1]);
                    if (killMap.containsKey(uuid)){
                        kills = kills.replaceAll("%kills%",String.valueOf(killMap.get(uuid)));
                    } else {
                        kills = kills.replaceAll("%kills%","N/A");
                    }
                    sender.sendMessage(kills);
                    String deaths = translateColourCodes(getConfig().getString("Stats_deaths_other"));
                    deaths = deaths.replaceAll("%playername%", args[1]);
                    if (deathMap.containsKey(uuid)){
                        deaths = deaths.replaceAll("%deaths%",String.valueOf(deathMap.get(uuid)));
                    } else {
                        deaths = deaths.replaceAll("%deaths%", "N/A");
                    }
                    sender.sendMessage(deaths);
                    String KDR = translateColourCodes(getConfig().getString("Stats_KillToDeathRatio_other"));
                    KDR = KDR.replaceAll("%playername%", args[1]);
                    if (killMap.containsKey(uuid) && deathMap.containsKey(uuid)){
                        KDR = KDR.replaceAll("%KDR%",String.valueOf(Double.valueOf(killMap.get(uuid)) / (Double.valueOf(deathMap.get(uuid)))));
                    } else {
                        KDR = KDR.replaceAll("%KDR%", "N/A");
                    }
                    sender.sendMessage(KDR);
                    return true;            //with playername

                }
            }else if (arg1.equalsIgnoreCase("setInv")){
                if (!(sender.hasPermission("kitpvp.setinv"))){
                    sender.sendMessage(ChatColor.RED + "No permissions.");
                    return true;
                }
                if (!isPlayer){
                    sender.sendMessage(ChatColor.RED + "Error: Console is denied access to this command.");
                    return true;
                }
                if (args.length < 2){
                    sender.sendMessage(ChatColor.RED + "Not enough arguments.");
                    return true;
                }
                if (args.length > 3){
                    sender.sendMessage(ChatColor.RED + "Too many arguments.");
                    return true;
                }
                Player player = (Player) sender;
                if (dapi.testKit(args[1])){
                    dapi.deletekit(dapi.getKit(args[1]));
                }
                dapi.registerNewKit(args[1],player.getInventory().getArmorContents(),player.getInventory().getContents(),player.getActivePotionEffects());
                player.sendMessage(ChatColor.GREEN + "Kit has been made :D");
                if (args.length == 3){
                    if (args[2].equalsIgnoreCase("-a")){
                        dapi.allowArenas(dapi.getKit(args[1]), dapi.getArenas());
                    }
                }
            }else if (arg1.equalsIgnoreCase("addArena")){
                if (!(sender.hasPermission("kitpvp.addarena"))){
                    sender.sendMessage(ChatColor.RED + "No permissions.");
                    return true;
                }
                if (!isPlayer){
                    sender.sendMessage(ChatColor.RED + "Error: Console is denied access to this command.");
                }
                if (args.length < 2){
                    sender.sendMessage(ChatColor.RED + "Not enough arguments.");
                    return true;
                }
                if (args.length > 2){
                    sender.sendMessage(ChatColor.RED + "Too many arguments.");
                    return true;
                }
                Player player = (Player) sender;
                dapi.registerNewArena(player.getLocation(),args[1],player.getWorld().getName());
                player.sendMessage(ChatColor.GREEN + "Added new arena.");
            }else if (arg1.equalsIgnoreCase("listKits")){
                if (dapi.getKitList().isEmpty()){
                    sender.sendMessage(ChatColor.RED + "No kits exist.");
                    return true;
                }
                StringBuilder strb = new StringBuilder();
                for (Kit kit : dapi.getKitList()){
                    if (sender.hasPermission("kitpvp.kit." + kit.getName())){
                        strb.append(kit.getName());
                        strb.append(", ");
                    }
                }
                sender.sendMessage(ChatColor.BLUE + "You have access to the following kits: ");
                sender.sendMessage(ChatColor.GRAY + strb.toString());
                return true;
            }else if (arg1.equalsIgnoreCase("manageArena")){
                if (!(sender.hasPermission("kitpvp.managearena"))){
                    sender.sendMessage(ChatColor.RED + "Access denied.");
                    return true;
                }
                if (dapi.getArenas().isEmpty()){
                    sender.sendMessage(ChatColor.RED + "There are no arenas to manage.");
                    return true;
                }
                if (args.length < 3 ){
                    sender.sendMessage(ChatColor.RED + "Incorrect command syntax - not enough arguments.");
                    sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp manageArena <ArenaName> <help|addSpawn|removeNearestSpawn|addKit|removeKit> [arg] " + ChatColor.AQUA +
                            "Does the specified action to the <Arena>.  Use [arg] for \"addKit\" and for \"removeKit\".");
                    return true;
                }
                String arenaName = args[1];
                String action = args[2];
                if (arenaName.equalsIgnoreCase("help") || action.equalsIgnoreCase("help")) {
                    sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp manageArena <ArenaName> <help|addSpawn|removeNearestSpawn|addKit|removeKit> [arg] " + ChatColor.AQUA +
                            "Does the specified action to the <Arena>.  Use [arg] for \"addKit\" and for \"removeKit\".");
                    return true;
                }
                if (!dapi.testArena(arenaName)) {
                    sender.sendMessage(ChatColor.RED + "Esta arena no existe.");
                    return true;
                }
                Arena arena = dapi.getArena(arenaName);

                if (action.equalsIgnoreCase("addSpawn")){
                    if (!(sender instanceof Player)){
                        sender.sendMessage(ChatColor.RED + "The console cannot do this.");
                        return true;
                    }
                    Player player = (Player) sender;
                    dapi.addArenaSpawn(arena,player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "You've successfully added a new spawn to the arena.");
                    return true;
                }else if (action.equalsIgnoreCase("removeNearestSpawn")){
                    if (!(sender instanceof Player)){
                        sender.sendMessage(ChatColor.RED + "The console cannot do this.");
                        return true;
                    }
                    Player player = (Player) sender;
                    Location near = player.getLocation();
                    ArrayList<Location> locMap = dapi.getArena(arenaName).getSpawns();
                    HashMap<Double,Location> dMap = new HashMap<Double, Location>();
                    for (Location loc : locMap){
                        double x = loc.getX() - near.getX();
                        double y = loc.getY() - near.getY();
                        double z = loc.getZ() - near.getZ();
                        double total = x + y + z;
                        dMap.put(total,loc);
                    }
                    double lowest = 0;
                    for (double total : dMap.keySet()){
                        if (lowest == 0){
                            lowest = total;
                            continue;
                        }
                        if (total < lowest){
                            lowest = total;
                        }
                    }
                    Location closest = dMap.get(lowest);
                    dapi.removeArenaSpawn(arena,closest);
                    sender.sendMessage(ChatColor.GREEN + "Success : nearest arena removed");
                }else if (action.equalsIgnoreCase("addkit")){
                    if (args.length < 4){
                        sender.sendMessage(ChatColor.RED + "You've got to specify a kit to add");
                        return true;
                    }
                    String kitName = args[3];
                    if (!(dapi.testKit(kitName))){
                        sender.sendMessage(ChatColor.RED + "That kit was not recognised.");
                        return true;
                    }
                    dapi.allowArena(dapi.getKit(kitName),arena);
                    sender.sendMessage(ChatColor.GRAY + "Success : Kit was allowed in arena!");
                }else if (action.equalsIgnoreCase("removekit")){
                    if (args.length < 4){
                        sender.sendMessage(ChatColor.RED + "You've got to specify a kit to remove");
                        return true;
                    }
                    String kitName = args[3];
                    if (!(dapi.testKit(kitName))){
                        sender.sendMessage(ChatColor.RED + "That kit was not recognised.");
                        return true;
                    }
                    dapi.denyArena(dapi.getKit(kitName),arena);
                    sender.sendMessage(ChatColor.GRAY + "Success : Kit was removed in arena!");
                } else {
                    sender.sendMessage(ChatColor.RED + "You have entered an incorrect parameter (" + action + ").");
                }
            }else if (arg1.equalsIgnoreCase("delArena")){
                if (!(sender.hasPermission("kitpvp.delarena"))){
                    sender.sendMessage(ChatColor.RED + "Access denied.");
                    return true;
                }

                if (args.length < 3){
                    sender.sendMessage(ChatColor.RED + "Not enough arguments - you need to specify an arena.");
                    return true;
                } //Not enough args

                String ArenaName = args[2];
                if (!(dapi.testArena(ArenaName))){
                    sender.sendMessage(ChatColor.RED + "Arena invalida.");
                    return true;
                }
                dapi.removeArena(dapi.getArena(ArenaName));
                sender.sendMessage(ChatColor.BLUE + "Done, but you will still need to delete /kitpvp/arenas/" + ArenaName + ".arena");
            }else{
                sender.sendMessage(ChatColor.RED + "Unknown KitPvP command.  Displaying help.");
                sender.sendMessage(ChatColor.GREEN + "=-=-=-= =-= KitPvP help =-= =-=-=-=");
                sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp join <arena> <kit> " + ChatColor.AQUA + "Join an arena with the specified kit.");
                sender.sendMessage(ChatColor.DARK_BLUE + "/kitpvp leave " + ChatColor.BLUE + "Leaves your current game.");
                sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp stats [Player] " + ChatColor.AQUA + "Tells you the [Player]'s stats. Yours if left blank.");
                sender.sendMessage(ChatColor.DARK_BLUE + "/kitpvp setinv <KitName> [[-a]] " + ChatColor.BLUE + "Sets the [kit] to your inventory. Add -a to enable in all arenas.");
                sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp addarena <arenaName> " + ChatColor.AQUA + "Adds an arena with a spawnpoint where you're standing.");
                sender.sendMessage(ChatColor.DARK_BLUE + "/kitpvp listKits " + ChatColor.BLUE + "Returns a list of the kits");
                sender.sendMessage(ChatColor.DARK_AQUA + "/kitpvp manageArena <ArenaName> <help|addSpawn|removeNearestSpawn|addKit|removeKit> [arg] " + ChatColor.AQUA +
                        "Does the specified action to the <Arena>.  Use [arg] for \"addKit\" and for \"removeKit\".");
                sender.sendMessage(ChatColor.DARK_BLUE + "/kitpvp delArena <ArenaName> " + ChatColor.BLUE + "Permanently deletes an arena." +
                        ChatColor.RED +  " Warning: cannot be undone.");
                return true;
            }
        }
        return true;
    }

    public int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }
}