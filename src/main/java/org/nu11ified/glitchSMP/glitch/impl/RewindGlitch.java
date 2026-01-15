package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        UUID uuid = player.getUniqueId();
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
        player.teleport(state.location());
        player.setHealth(Math.min(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(), state.health()));
        player.setFoodLevel(state.foodLevel());
        player.setFallDistance(state.fallDistance());
        player.setVelocity(state.velocity());
        player.getInventory().setContents(state.contents());
        player.getInventory().setArmorContents(state.armor());
        player.getInventory().setItemInOffHand(state.offhand());
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : state.effects()) {
            player.addPotionEffect(effect);
        }
        effects.playImpact(player.getLocation(), getType());
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.6, 0.6, 0.6, 0.2);
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.7f);
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
