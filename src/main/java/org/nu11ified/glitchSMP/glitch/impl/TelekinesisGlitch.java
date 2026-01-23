package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.nu11ified.glitchSMP.GlitchSMP;
import org.nu11ified.glitchSMP.config.GlitchSettings;
import org.nu11ified.glitchSMP.effects.GlitchEffects;
import org.nu11ified.glitchSMP.glitch.Glitch;
import org.nu11ified.glitchSMP.glitch.GlitchType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TelekinesisGlitch extends Glitch implements Listener {
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Set<UUID> primedPlayers = new HashSet<>();

    public TelekinesisGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.TELEKINESIS,
            GlitchType.TELEKINESIS.getName(),
            GlitchType.TELEKINESIS.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
    }

    @Override
    protected void onActivate(Player player) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        primedPlayers.add(player.getUniqueId());
        effects.playActivation(player, getType());
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.9f, 1.5f);
    }

    @Override
    protected void onDeactivate(Player player) {
        primedPlayers.remove(player.getUniqueId());
        effects.playEnd(player, getType());
        if (primedPlayers.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Player target)) {
            return;
        }
        if (!primedPlayers.remove(player.getUniqueId())) {
            return;
        }
        if (plugin.getAbilityBlocker().isAbilityBlocked(player) || plugin.getAbilityBlocker().isAbilityBlocked(target)) {
            return;
        }
        target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 40, 1, false, true, true));
        target.getWorld().spawnParticle(Particle.INSTANT_EFFECT, target.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.05);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 0.8f, 1.2f);
    }
}
