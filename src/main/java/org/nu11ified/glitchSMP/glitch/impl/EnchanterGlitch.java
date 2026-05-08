package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;
import org.nu11ified.glitchSMP.util.WorldGuardHook;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EnchanterGlitch extends Glitch implements Listener {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Map<UUID, BukkitTask> auraTasks = new HashMap<>();

    public EnchanterGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.ENCHANTER,
            GlitchType.ENCHANTER.getName(),
            GlitchType.ENCHANTER.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        UUID uuid = player.getUniqueId();
        activePlayers.add(uuid);
        effects.playActivation(player, getType());
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.3f);
        auraTasks.put(uuid, Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline() && activePlayers.contains(uuid)) {
                player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 8, 0.4, 0.4, 0.4, 0.02);
                effects.playTick(player, getType());
            }
        }, 0L, 20L));
    }

    @Override
    protected void onDeactivate(Player player) {
        UUID uuid = player.getUniqueId();
        activePlayers.remove(uuid);
        BukkitTask task = auraTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        effects.playEnd(player, getType());
        if (activePlayers.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (plugin.getAbilityBlocker().isAbilityBlocked(player)) {
            return;
        }
        if (event.getEntity() instanceof Player target && plugin.getAbilityBlocker().isAbilityBlocked(target)) {
            return;
        }
        if (!activePlayers.contains(player.getUniqueId())) {
            return;
        }
        if (event.getEntity() instanceof Player target && WorldGuardHook.isBlockedTarget(player, target, plugin.getGlitchSettings().getDisabledRegion(), plugin.getGlitchSettings().getDisabledWorld(), plugin)) {
            return;
        }
        ItemStack weapon = player.getInventory().getItemInMainHand();
        int sharpnessLevel = weapon.getEnchantmentLevel(Enchantment.SHARPNESS);
        double bonus = (sharpnessLevel + 1) * 0.5;
        event.setDamage(event.getDamage() + bonus);
        player.getWorld().spawnParticle(Particle.INSTANT_EFFECT, event.getEntity().getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
    }
}
