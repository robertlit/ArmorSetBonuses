package me.robertlit.armorsetbonuses;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ArmorSetBonuses extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadEffects();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private final Map<UUID, ArmorSetType> lastSetTypeMap = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        handleArmorChange((Player) event.getWhoClicked());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        handleArmorChange(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        handleArmorChange(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ArmorSetType last = lastSetTypeMap.get(player.getUniqueId());
        if (last == null) {
            return;
        }
        player.removePotionEffect(effectMap.get(last).getType());
        lastSetTypeMap.remove(player.getUniqueId());
    }

    private void handleArmorChange(Player player) {
        getServer().getScheduler().runTask(this, () -> {
            ArmorSetType setType = matchFullSetType(player);
            if (setType != null) {
                if (!effectMap.containsKey(setType)) {
                    return;
                }
                lastSetTypeMap.put(player.getUniqueId(), setType);
                PotionEffect potionEffect = effectMap.get(setType);
                player.addPotionEffect(potionEffect);
                return;
            }
            ArmorSetType last = lastSetTypeMap.get(player.getUniqueId());
            if (last == null) {
                return;
            }
            player.removePotionEffect(effectMap.get(last).getType());
            lastSetTypeMap.remove(player.getUniqueId());
        });
    }

    private final Map<ArmorSetType, PotionEffect> effectMap = new HashMap<>();
    private void loadEffects() {
        for (String key : getConfig().getKeys(false)) {
            ArmorSetType armorSetType;
            try {
                armorSetType = ArmorSetType.valueOf(key);
            } catch (IllegalArgumentException ex) {
                getLogger().info("Could not find armor of type " + key);
                continue;
            }

            PotionEffectType effectType = PotionEffectType.getByName(getConfig().getString(key + ".effect", ""));
            if (effectType == null) {
                getLogger().info("Could not find effect of type " + getConfig().getString(key + ".effect"));
                continue;
            }

            int amplifier = getConfig().getInt(key + ".amplifier", 0);

            PotionEffect potionEffect = new PotionEffect(effectType, Integer.MAX_VALUE, amplifier, true, false);

            effectMap.put(armorSetType, potionEffect);
        }
    }

    private ArmorSetType matchFullSetType(Player player) {
        PlayerInventory inventory = player.getInventory();
        Supplier<Stream<ItemStack>> streamSupplier = () -> Stream.of(inventory.getArmorContents()).filter(Objects::nonNull);
        if (streamSupplier.get().count() != 4) return null;
        for (ArmorSetType type : ArmorSetType.values()) {
            if (streamSupplier.get().allMatch(item -> item.getType().toString().contains(type.toString()))) {
                return type;
            }
        }
        return null;
    }

    enum ArmorSetType {
        LEATHER, CHAIN, GOLD, IRON, DIAMOND, NETHERITE
    }
}
