package me.Carnage.cCIPHelius;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public final class CCIPHelius extends JavaPlugin implements Listener, TabCompleter {

    private final HashMap<UUID, HashMap<Material, Long>> cooldowns = new HashMap<>();
    private final HashMap<UUID, Queue<Location>> lastLocations = new HashMap<>();
    private final long cooldownTime = 120000; // 120 seconds cooldown
    private final HashMap<UUID, Long> blockPlaceRestrictions = new HashMap<>();

    private final String[] vanillaPowers = {"ObsidianShard", "HealingWand", "PrismarinePearl", "HealingTears", "ShulkerShell", "DragonScale", "StrengthenBlazeRod", "ChorusFruitofFloating"};
    private final String[] customPowers = {"TimeWrap","PlaceBlocker"};

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("CCIPHelius").setExecutor(this);
        getCommand("CCIPHelius").setTabCompleter(this);
        getLogger().info("CCIPHelius Plugin has been enabled!");

        // Schedule a repeating task to store player locations
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID playerId = player.getUniqueId();
                lastLocations.putIfAbsent(playerId, new LinkedList<>());

                Queue<Location> locations = lastLocations.get(playerId);
                if (locations.size() >= 60) {
                    locations.poll(); // Remove the oldest location if we already have 60
                }
                locations.offer(player.getLocation()); // Add the current location
            }
        }, 0L, 20L); // Store every second (20 ticks)
    }

    private void teleportToLastLocation(Player player) {
        UUID playerId = player.getUniqueId();
        Queue<Location> locations = lastLocations.get(playerId);

        if (locations != null && locations.size() == 60) { // Ensure we have 60 locations stored
            Location lastLocation = locations.poll(); // Get the location from 60 seconds ago
            player.teleport(lastLocation);
            player.sendMessage("§aTeleported to your location from 60 seconds ago!");
        } else {
            player.sendMessage("§cNo previous location found!");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("CCIPHelius Plugin has been disabled!");
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() == Material.CHORUS_FRUIT) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot eat this item!");
        }
    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item != null && item.getType() != Material.AIR) {
                if (isCustomItem(item)) {
                    UUID playerId = player.getUniqueId();
                    long currentTime = System.currentTimeMillis();

                    // Check if the item has a cooldown
                    if (checkCooldown(playerId, item.getType(), currentTime)) {
                        switch (item.getType()) {
                            case CLOCK:
                                teleportToLastLocation(player);
                                startCooldown(playerId, item.getType(), player);
                                startTimer(playerId, player); // Start the timer
                                break;
                            case BONE:
                                // Implement bone effect
                                handleBoneEffect(player);
                                startCooldown(playerId, item.getType(), player);
                                break;
                            case OBSIDIAN:
                                startCooldown(playerId, item.getType(), player);
                                applyItemEffects(player, item);
                                break;
                            case BAMBOO:
                                startCooldown(playerId, item.getType(), player);
                                applyItemEffects(player, item);
                                break;
                            case PRISMARINE_SHARD:
                                startCooldown(playerId, item.getType(), player);
                                applyItemEffects(player, item);
                                break;
                            case GHAST_TEAR:
                                startCooldown(playerId, item.getType(), player);
                                applyItemEffects(player, item);
                                break;
                            case SHULKER_SHELL:
                                startCooldown(playerId, item.getType(), player);
                                applyItemEffects(player, item);
                                break;
                            case PURPLE_DYE: // Dragon Scale
                                startCooldown(playerId, item.getType(), player);
                                applyItemEffects(player, item);
                                break;
                            case BLAZE_ROD:
                                startCooldown(playerId, item.getType(), player);
                                applyItemEffects(player, item);
                                break;
                            case CHORUS_FRUIT:
                                startCooldown(playerId, item.getType(), player);
                                applyItemEffects(player, item);
                                break;
                            default:
                                break;
                        }
                        setCooldown(playerId, item.getType(), currentTime);
                    } else {
                        player.sendMessage("§4§lYou must wait before using this item again!");
                    }
                }
            }
        }
    }

    private void handleBoneEffect(Player player) {
        Player target = getTargetPlayer(player);
        if (target != null) {
            target.sendMessage("§cYou have been hit with a bone and cannot place blocks for 15 seconds!");
            blockPlaceRestrictions.put(target.getUniqueId(), System.currentTimeMillis() + (15 * 1000)); // 15 seconds from now

            // Schedule a task to remove the restriction after 15 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    blockPlaceRestrictions.remove(target.getUniqueId());
                    target.sendMessage("§aYou can now place blocks again!");
                }
            }.runTaskLater(this, 15 * 20); // 15 seconds in ticks
        } else {
            player.sendMessage("§cNo target found!");
        }
    }
    private Player getTargetPlayer(Player player) {
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.equals(player) && target.getLocation().distance(player.getLocation()) < 5) { // 5 blocks radius
                return target;
            }
        }
        return null; // No target found
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (blockPlaceRestrictions.containsKey(player.getUniqueId())) {
            event.setCancelled(true); // Cancel the block placement
            player.sendMessage("§cYou cannot place blocks right now!");
        }
    }


    private void startCooldown(UUID playerId, Material itemType, Player player) {
        long currentTime = System.currentTimeMillis();
        cooldowns.putIfAbsent(playerId, new HashMap<>());
        cooldowns.get(playerId).put(itemType, currentTime);

        // Set the player's XP level based on the cooldown time
        int xpLevels = (int) (cooldownTime / 12000); // 120 seconds cooldown, 12000 ticks = 120 seconds
        player.setLevel(xpLevels); // Set the player's XP level for cooldown
    }

    private void startTimer(UUID playerId, Player player) {
        new BukkitRunnable() {
            private int remainingTime = 60; // 60 seconds for the timer
            private boolean cooldownExpired = false; // Track if cooldown has expired

            @Override
            public void run() {
                // Check if the player has the clock in their hotbar
                boolean hasClockInHotbar = false;
                ItemStack[] hotbar = player.getInventory().getContents(); // Get the player's inventory

                // Check the first 9 slots (hotbar)
                for (int i = 0; i < 9; i++) {
                    ItemStack item = hotbar[i];
                    if (item != null && item.getType() == Material.CLOCK) {
                        hasClockInHotbar = true;
                        break;
                    }
                }

                if (!hasClockInHotbar) {
                    this.cancel(); // Stop the timer if the player does not have the clock in the hotbar
                    return;
                }

                long currentTime = System.currentTimeMillis();

                // Check if the cooldown has expired
                if (!cooldownExpired) {
                    if (checkCooldown(playerId, Material.CLOCK, currentTime)) {
                        // Cooldown has expired
                        //System.out.println("§eCooldown is over! Starting timer..."); // Notify the player
                        player.setLevel(0); // Reset the player's XP level
                        cooldownExpired = true; // Mark cooldown as expired

                        // Create an ItemStack from Material.CLOCK
                        ItemStack clockItem = new ItemStack(Material.CLOCK);

                        // Apply effects based on the item held
                        applyItemEffects(player, clockItem); // Pass the ItemStack instead

                    } else {
                        // Cooldown is still active
                        //System.out.println("Cooldown still active");
                        return; // Do not proceed with the timer if cooldown is active
                    }
                }

                // Now that the cooldown has expired, show the timer
                if (remainingTime <= 0) {
                    // Notify the player that the time is over
                    sendActionBar(player, "§a60 seconds are over!"); // Notify the player
                    this.cancel(); // Stop the timer
                } else {
                    // Show seconds left only after cooldown has expired
                    sendActionBar(player, "§eSeconds left: " + remainingTime); // Notify the player with remaining time

                }
                remainingTime--; // Decrease the remaining time by 1 second
            }
        }.runTaskTimer(this, 0L, 20L); // Schedule the task to run every second
    }

    private boolean isCustomItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && (meta.hasDisplayName() || meta.hasLore());
    }

    private boolean checkCooldown(UUID playerId, Material itemType, long currentTime) {
        if (!cooldowns.containsKey(playerId)) {
            return true; // No cooldowns set for this player
        }
        HashMap<Material, Long> playerCooldowns = cooldowns.get(playerId);
        return !playerCooldowns.containsKey(itemType) || (currentTime - playerCooldowns.get(itemType)) >= cooldownTime;
    }

    private void setCooldown(UUID playerId, Material itemType, long currentTime) {
        cooldowns.putIfAbsent(playerId, new HashMap<>());
        cooldowns.get(playerId).put(itemType, currentTime);
        displayCooldown(playerId, itemType);
    }

    private void displayCooldown(UUID playerId, Material itemType) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && cooldowns.containsKey(playerId)) {
                    HashMap<Material, Long> playerCooldowns = cooldowns.get(playerId);
                    if (playerCooldowns.containsKey(itemType)) {
                        long elapsed = System.currentTimeMillis() - playerCooldowns.get(itemType);
                        int remainingSeconds = (int) ((cooldownTime - elapsed) / 1000); // Calculate remaining seconds

                        // Calculate the XP level based on the remaining time
                        int xpLevel = Math.max(remainingSeconds, 0); // XP level should be the remaining seconds
                        player.setLevel(xpLevel); // Set the XP level, ensuring it doesn't go negative

                        if (elapsed >= cooldownTime) {
                            playerCooldowns.remove(itemType); // Remove the cooldown for this item
                            player.setLevel(0); // Reset the XP level
                            sendActionBar(player, "§c§lCooldown is over for " + itemType.name() + "!"); // Notify the player above the hotbar
                            this.cancel(); // Stop the task
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L); // Update every second (20 ticks)
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("CCIPHelius")) {
            if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
                String playerName = args[1];
                String category = args[2];
                String itemName = args[3];

                Player targetPlayer = Bukkit.getPlayer(playerName);
                if (targetPlayer != null) {
                    ItemStack itemToGive = getItemFromCategory(category, itemName);
                    if (itemToGive != null) {
                        targetPlayer.getInventory().addItem(itemToGive);
                        targetPlayer.sendMessage("You have been given " + itemToGive.getItemMeta().getDisplayName() + "!");
                        sender.sendMessage("Gave " + itemToGive.getItemMeta().getDisplayName() + " to " + playerName + ".");
                    } else {
                        sender.sendMessage("Item not found in the specified category.");
                    }
                } else {
                    sender.sendMessage("Player not found.");
                }
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                // Reload the plugin's configuration
                reloadConfig();
                sender.sendMessage("CCIPHelius configuration has been reloaded.");
                return true;
            } else {
                sender.sendMessage("Usage: /CCIPHelius give <PlayerName> <Category> <ItemName> or /CCIPHelius reload");
                return true;
            }
        }
        return false;
    }

    private ItemStack getItemFromCategory(String category, String itemName) {
        Material material = null;
        ItemMeta itemMeta = null;

        if (category.equalsIgnoreCase("VanillaPowers")) {
            switch (itemName.toLowerCase()) {
                case "obsidianshard":
                    material = Material.OBSIDIAN;
                    itemMeta = createItemMeta("§5§l§nObsidian Shard",
                            "§8A shard of obsidian,",
                            "§8imbued with dark power.",
                            "",
                            "§5Cooldown: 120 seconds",
                            "",
                            "",
                            "§dGrants the player Strength and Slowness",
                            "",
                            "",
                            "",
                            "",
                            "§9§l§ki §9§o§lMYTHIC RARITY ITEM§9§l§Ki");
                    break;
                case "healingwand":
                    material = Material.BAMBOO;
                    itemMeta = createItemMeta("§6§l§nHealing Wand",
                            "§2A mystical wand from depths of the jungle,",
                            "§2granting the sacred powers of an old tribe",
                            "",
                            "§5Cooldown: 120 seconds",
                            "",
                            "",
                            "§dGrants the player Regeneration and Speed",
                            "",
                            "",
                            "",
                            "",
                            "§9§l§ki §9§o§lMYTHIC RARITY ITEM §9§l§Ki");
                    break;
                case "prismarinepearl":
                    material = Material.PRISMARINE_SHARD;
                    itemMeta = createItemMeta("§b§l§nPrismarine Pearl",
                            "§bA shimmering shard from the ocean,",
                            "§bproviding the gift of water breathing.",
                            "",
                            "§5Cooldown: 120 seconds",
                            "",
                            "",
                            "§dGrants the player Dolphin Grace and Water Breathing",
                            "",
                            "",
                            "",
                            "",
                            "§9§l§ki §9§o§lMYTHIC RARITY ITEM §9§l§Ki");
                    break;
                case "healingtears":
                    material = Material.GHAST_TEAR;
                    itemMeta = createItemMeta("§c§l§nHealing Tears",
                            "§fA tear shed by a ghast,",
                            "§fknown for its healing properties.",
                            "",
                            "§5Cooldown: 120 seconds",
                            "",
                            "",
                            "§dGrants the player Regeneration and Absorption",
                            "",
                            "",
                            "",
                            "",
                            "§9§l§ki §9§o§lMYTHIC RARITY ITEM §9§l§Ki");
                    break;
                case "shulkershell":
                    material = Material.SHULKER_SHELL;
                    itemMeta = createItemMeta("§d§l§nShulker Shell",
                            "§5A shell from a shulker,",
                            "§5used to create unique potions.",
                            "",
                            "§5Cooldown: 120 seconds",
                            "",
                            "",
                            "§dGrants the player Levitation",
                            "",
                            "",
                            "",
                            "",
                            "§9§l§ki §9§o§lMYTHIC RARITY ITEM §9§l§Ki");
                    break;
                case "dragonscale":
                    material = Material.PURPLE_DYE;
                    itemMeta = createItemMeta("§5§l§nDragon Scale",
                            "§dA scale from a mighty dragon,",
                            "§dproviding protection from fire.",
                            "",
                            "§dCooldown: 120 seconds",
                            "",
                            "",
                            "§dGrants the player Haste",
                            "",
                            "",
                            "",
                            "",
                            "§9§l§ki §9§o§lMYTHIC RARITY ITEM §9§l§Ki");
                    break;
                case "strenghthenblazerod":
                    material = Material.BLAZE_ROD;
                    itemMeta = createItemMeta("§e§l§nStrengthen BlazeRod",
                            "§6A rod from a blaze,",
                            "§6granting strength to its wielder.",
                            "",
                            "§6Cooldown: 120 seconds",
                            "",
                            "",
                            "§dGrants the player Fire Resistance and Strength",
                            "",
                            "",
                            "",
                            "",
                            "§9§l§ki §9§o§lMYTHIC RARITY ITEM §9§l§Ki");
                    break;
                case "chorusfruitoffloating":
                    material = Material.CHORUS_FRUIT;
                    itemMeta = createItemMeta("§a§l§nChorus Fruit of Floating",
                            "§7A fruit from the End,",
                            "§7allowing you to float gently.",
                            "",
                            "§6Cooldown: 120 seconds",
                            "",
                            "",
                            "§dGrants the player Slow Falling",
                            "",
                            "",
                            "",
                            "",
                            "§9§l§ki §9§o§lMYTHIC RARITY ITEM §9§l§Ki");
                    break;
                default:
                    return null;
            }
        } else if (category.equalsIgnoreCase("CustomPowers")) {
            switch (itemName.toLowerCase()) {
                case "timewrap":
                    material = Material.CLOCK;
                    itemMeta = createItemMeta("§e§l§nTime Wrap",
                            "§7Use this clock to teleport",
                            "§7to your location from 60 seconds ago.",
                            "",
                            "§5Cooldown: 120 seconds",
                            "",
                            "",
                            "",
                            "§9§l§ki §9§o§lMYTHIC RARITY ITEM §9§l§Ki");
                    break;
                case "placeblocker":
                    material = Material.BONE;
                    itemMeta = createItemMeta("§9§l§nPlace Blocker",
                            "§7Use this bone to stop",
                            "§7your enemy from placing blocks for 15 seconds",
                            "",
                            "§5Cooldown: 120 seconds",
                            "",
                            "",
                            "",
                            "§9§l§ki §9§o§lMYTHIC RARITY ITEM §9§l§Ki");
                    break;
                default:
                    return null;
            }
        }
        if (material != null && itemMeta != null) {
            ItemStack itemStack = new ItemStack(material);
            itemStack.setItemMeta(itemMeta);
            return itemStack;
        }
        return null;
    }

    private ItemMeta createItemMeta(String displayName, String... loreLines) {
        ItemMeta meta = Bukkit.getItemFactory().getItemMeta(Material.OBSIDIAN); // Use any material to create the meta
        meta.setDisplayName(displayName);
        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(line);
        }
        meta.setLore(lore);
        return meta;
    }

    private void applyItemEffects(Player player, ItemStack item) {
        if (item.getType() == Material.OBSIDIAN) {
            applyObsidianShardEffects(player);
        } else if (item.getType() == Material.BAMBOO) {
            applyBambooEffects(player);
        } else if (item.getType() == Material.PRISMARINE) {
            applyPrismarinePearlEffects(player);
        } else if (item.getType() == Material.GHAST_TEAR) {
            applyGhastTearEffects(player);
        } else if (item.getType() == Material.SHULKER_SHELL) {
            applyShulkerShellEffects(player);
        } else if (item.getType() == Material.PURPLE_DYE) {
            applyDragonScaleEffects(player);
        } else if (item.getType() == Material.BLAZE_ROD) {
            applyBlazeRodEffects(player);
        } else if (item.getType() == Material.CHORUS_FRUIT) {
            applyChorusFruitEffects(player);
        }
    }

    private void applyObsidianShardEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 2400, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1200, 2));
        sendActionBar(player,"§2§lYou feel empowered by the Obsidian Block!");
    }

    private void applyBambooEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 2400, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 2400, 1));
        sendActionBar(player,"§2§lYou feel a surge of energy from the Healing Wand!");
    }

    private void applyPrismarinePearlEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 2400, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 2400, 1));
        sendActionBar(player,"§2§lYou can breathe underwater thanks to the Prismarine Pearl!");
    }

    private void applyGhastTearEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 2400, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 2));
        sendActionBar(player,"§2§lYou feel healed by the Ghast Tear!");
    }

    private void applyShulkerShellEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 2400, 1));
        sendActionBar(player,"§2§lYou are levitating thanks to the Shulker Shell!");
    }

    private void applyDragonScaleEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 2400, 2));
        sendActionBar(player,"§2§lYou feel the power of the Ender Dragon in your hands!");
    }

    private void applyBlazeRodEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 2400, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 2400, 1));
        sendActionBar(player,"§2§lYou feel stronger with the Blaze Rod!");
    }

    private void applyChorusFruitEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 600, 1));
        sendActionBar(player,"§2§lYou are floating gently thanks to the Chorus Fruit!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("give");
            completions.add("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            completions.add("VanillaPowers");
            completions.add("CustomPowers");
        } else if (args.length == 4) {
            if (args[2].equalsIgnoreCase("VanillaPowers")) {
                for (String item : vanillaPowers) {
                    completions.add(item);
                }
            } else if (args[2].equalsIgnoreCase("CustomPowers")) {
                for (String item : customPowers) {
                    completions.add (item);
                }
            }
        }
        return completions;
    }
}