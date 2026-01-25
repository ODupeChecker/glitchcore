package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RewindGlitch extends Glitch {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Map<UUID, RewindState> states = new HashMap<>();

    public RewindGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.REWIND,
            GlitchType.REWIND.getName(),
            GlitchType.REWIND.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        UUID uuid = player.getUniqueId();
        if (states.containsKey(uuid)) {
            restore(player, states.remove(uuid));
            return;
        }
        states.put(uuid, RewindState.capture(player));
        effects.playActivation(player, getType());
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 12, 0.4, 0.4, 0.4, 0.02);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
    }

    @Override
    protected void onDeactivate(Player player) {
        UUID uuid = player.getUniqueId();
        RewindState state = states.remove(uuid);
        if (state != null) {
            restore(player, state);
        }
    }

    private void restore(Player player, RewindState state) {
        if (player == null || state == null || !player.isOnline() || !player.isValid()) {
            return;
        }

        try {
            Location location = state.location();
            if (location != null && location.getWorld() != null) {
                player.teleport(location);
            }

            AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : player.getHealth();
            double health = state.health();
            if (Double.isFinite(health)) {
                player.setHealth(Math.min(maxHealth, Math.max(0.0, health)));
            }

            int foodLevel = state.foodLevel();
            if (foodLevel >= 0) {
                player.setFoodLevel(Math.min(20, foodLevel));
            }

            float fallDistance = state.fallDistance();
            if (Float.isFinite(fallDistance)) {
                player.setFallDistance(Math.max(0.0f, fallDistance));
            }

            if (state.velocity() != null) {
                player.setVelocity(state.velocity());
            }

            if (state.contents() != null) {
                player.getInventory().setContents(state.contents());
            }
            if (state.armor() != null) {
                player.getInventory().setArmorContents(state.armor());
            }
            if (state.offhand() != null) {
                player.getInventory().setItemInOffHand(state.offhand());
            }

            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            if (state.effects() != null) {
                for (PotionEffect effect : state.effects()) {
                    if (effect != null) {
                        player.addPotionEffect(effect);
                    }
                }
            }

            Location currentLocation = player.getLocation();
            if (currentLocation != null) {
                effects.playImpact(currentLocation, getType());
                if (currentLocation.getWorld() != null) {
                    player.getWorld().spawnParticle(Particle.PORTAL, currentLocation.add(0, 1, 0), 30, 0.6, 0.6, 0.6, 0.2);
                    player.playSound(currentLocation, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.7f);
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to restore rewind state for " + player.getName() + ": " + exception.getMessage());
        }
    }

    private record RewindState(Location location, double health, int foodLevel, ItemStack[] contents, ItemStack[] armor, ItemStack offhand,
                               Collection<PotionEffect> effects, float fallDistance, org.bukkit.util.Vector velocity) {
        static RewindState capture(Player player) {
            return new RewindState(
                player.getLocation().clone(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                player.getInventory().getItemInOffHand(),
                player.getActivePotionEffects(),
                player.getFallDistance(),
                player.getVelocity()
            );
        }
    }
}
