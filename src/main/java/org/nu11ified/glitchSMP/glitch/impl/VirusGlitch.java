package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.Color;
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
import org.nu11ified.glitchSMP.util.WorldGuardHook;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VirusGlitch extends Glitch implements Listener {
    private static final String SCREEN_OVERLAY = "\uE005";
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final Set<UUID> primedPlayers = new HashSet<>();

    public VirusGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.VIRUS,
            GlitchType.VIRUS.getName(),
            GlitchType.VIRUS.getDescription(),
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
    }

    @Override
    protected void onDeactivate(Player player) {
        primedPlayers.remove(player.getUniqueId());
        if (primedPlayers.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player target)) {
            return;
        }
        if (WorldGuardHook.isBlockedTarget(attacker, target, plugin.getGlitchSettings().getDisabledRegion(), plugin.getGlitchSettings().getDisabledWorld(), plugin)) {
            return;
        }
        if (!primedPlayers.remove(attacker.getUniqueId())) {
            return;
        }
        applyVirus(attacker, target);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_HUSK_AMBIENT, 0.6f, 0.8f);
        target.getWorld().spawnParticle(
            Particle.ENTITY_EFFECT,
            target.getLocation().add(0, 1, 0),
            14,
            0.4,
            0.4,
            0.4,
            Color.fromRGB(94, 205, 97)
        );
    }

    private void applyVirus(Player source, Player target) {
        int durationTicks = (int) Math.max(20L, getDurationMillis() / 50L);
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, durationTicks, 1, false, true, true));
        target.sendTitle(SCREEN_OVERLAY, "", 0, durationTicks, 0);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> target.sendTitle("", "", 0, 1, 10), durationTicks + 1L);
        effects.playTick(source, getType());
    }
}
