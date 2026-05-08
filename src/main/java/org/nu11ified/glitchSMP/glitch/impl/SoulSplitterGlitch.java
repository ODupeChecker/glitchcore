package org.nu11ified.glitchSMP.glitch.impl;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
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

public class SoulSplitterGlitch extends Glitch implements Listener {
    private static final int FALLBACK_SOUL_DURATION_TICKS = 20 * 30;
    private static final double SOUL_LAUNCH_DISTANCE = 3.5;
    private final GlitchSMP plugin;
    private final GlitchEffects effects;
    private final GlitchSettings.GlitchProfile profile;
    private final Set<UUID> primedPlayers = new HashSet<>();
    private final Map<UUID, SoulSplitState> activeSplits = new HashMap<>();
    private final Map<UUID, UUID> bodyLookup = new HashMap<>();

    public SoulSplitterGlitch(GlitchSMP plugin, GlitchSettings.GlitchProfile profile) {
        super(
            GlitchType.SOUL_SPLITTER,
            GlitchType.SOUL_SPLITTER.getName(),
            GlitchType.SOUL_SPLITTER.getDescription(),
            profile.cooldownMillis(),
            profile.durationMillis()
        );
        this.plugin = plugin;
        this.effects = plugin.getGlitchEffects();
        this.profile = profile;
    }

    @Override
    protected void onActivate(Player player) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        primedPlayers.add(player.getUniqueId());
        effects.playActivation(player, getType());
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.9f, 1.3f);
    }

    @Override
    protected void onDeactivate(Player player) {
        primedPlayers.remove(player.getUniqueId());
        effects.playEnd(player, getType());
        if (primedPlayers.isEmpty() && activeSplits.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ArmorStand stand) {
            handleBodyDamage(event, stand);
            return;
        }
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Player target)) {
            return;
        }
        if (WorldGuardHook.isBlockedTarget(player, target, plugin.getGlitchSettings().getDisabledRegion())) {
            return;
        }
        if (!primedPlayers.remove(player.getUniqueId())) {
            return;
        }
        applySoulSplit(player, target);
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        SoulSplitState state = activeSplits.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        if (!state.body.getUniqueId().equals(event.getRightClicked().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        reconnectSoul(player, true);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (activeSplits.containsKey(player.getUniqueId())) {
            reconnectSoul(player, false);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        cleanupSoulSplit(event.getEntity().getUniqueId());
    }

    private void handleBodyDamage(EntityDamageByEntityEvent event, ArmorStand stand) {
        UUID ownerId = bodyLookup.get(stand.getUniqueId());
        if (ownerId == null) {
            return;
        }
        Player owner = plugin.getServer().getPlayer(ownerId);
        event.setCancelled(true);
        if (owner != null && owner.isOnline()) {
            if (event.getDamager() instanceof Entity damager) {
                owner.damage(event.getFinalDamage(), damager);
            } else {
                owner.damage(event.getFinalDamage());
            }
            owner.setNoDamageTicks(0);
            owner.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, owner.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.1);
        }
        applyBodyKnockback(stand, event.getDamager());
        stand.getWorld().playSound(stand.getLocation(), Sound.ENTITY_ARMOR_STAND_HIT, 0.7f, 1.2f);
    }

    private void applySoulSplit(Player caster, Player target) {
        if (activeSplits.containsKey(target.getUniqueId())) {
            reconnectSoul(target, false);
        }
        Location bodyLocation = target.getLocation().clone();
        ArmorStand body = spawnBody(target, bodyLocation);
        bodyLookup.put(body.getUniqueId(), target.getUniqueId());

        SoulSplitState state = new SoulSplitState(
            body,
            target.getGameMode(),
            target.getAllowFlight(),
            target.isFlying(),
            target.isCollidable(),
            target.isInvulnerable()
        );
        activeSplits.put(target.getUniqueId(), state);

        int soulDurationTicks = resolveSoulDurationTicks();
        applySoulState(target, caster, bodyLocation, soulDurationTicks);

        BukkitTask particleTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!target.isOnline() || target.isDead()) {
                return;
            }
            target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 8, 0.4, 0.5, 0.4, 0.02);
            target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, target.getLocation().add(0, 1, 0), 4, 0.3, 0.4, 0.3, 0.01);
        }, 0L, 6L);
        state.setParticleTask(particleTask);

        if (soulDurationTicks > 0) {
            BukkitTask timeoutTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (activeSplits.containsKey(target.getUniqueId())) {
                    reconnectSoul(target, true);
                }
            }, soulDurationTicks);
            state.setTimeoutTask(timeoutTask);
        }
    }

    private void applySoulState(Player target, Player caster, Location bodyLocation, int soulDurationTicks) {
        Vector direction = bodyLocation.toVector().subtract(caster.getLocation().toVector());
        if (direction.lengthSquared() < 0.01) {
            direction = target.getLocation().getDirection();
        }
        Vector normalized = direction.normalize();
        Location soulLocation = bodyLocation.clone().add(normalized.clone().multiply(SOUL_LAUNCH_DISTANCE)).add(0, 0.6, 0);
        target.teleport(soulLocation);
        target.setVelocity(normalized.clone().multiply(1.2).setY(0.4));
        target.setFallDistance(0f);
        target.setGameMode(GameMode.ADVENTURE);
        target.setAllowFlight(true);
        target.setFlying(true);
        target.setInvulnerable(true);
        target.setCollidable(false);
        target.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Math.max(soulDurationTicks, FALLBACK_SOUL_DURATION_TICKS), 0, false, false, false));
        target.sendMessage(ChatColor.AQUA + "Your soul has been severed. Reconnect with your body.");

        bodyLocation.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, bodyLocation.add(0, 1, 0), 18, 0.4, 0.6, 0.4, 0.03);
        bodyLocation.getWorld().playSound(bodyLocation, Sound.ENTITY_VEX_CHARGE, 1.0f, 0.9f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.9f, 1.4f);
    }

    private ArmorStand spawnBody(Player target, Location location) {
        return location.getWorld().spawn(location, ArmorStand.class, stand -> {
            stand.setCustomName(ChatColor.GRAY + target.getName());
            stand.setCustomNameVisible(false);
            stand.setArms(true);
            stand.setBasePlate(false);
            stand.setGravity(true);
            stand.setSilent(true);
            stand.setPersistent(false);
            stand.setVisible(false);
            stand.setCanPickupItems(false);
            if (stand.getEquipment() != null) {
                stand.getEquipment().setArmorContents(target.getInventory().getArmorContents());
                stand.getEquipment().setItemInMainHand(target.getInventory().getItemInMainHand());
                stand.getEquipment().setItemInOffHand(target.getInventory().getItemInOffHand());
                applyPlayerHead(target, stand);
            }
        });
    }

    private void applyPlayerHead(Player target, ArmorStand stand) {
        ItemStack helmet = stand.getEquipment() != null ? stand.getEquipment().getHelmet() : null;
        if (helmet != null && helmet.getType() != Material.AIR) {
            return;
        }
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(target);
            head.setItemMeta(meta);
        }
        if (stand.getEquipment() != null) {
            stand.getEquipment().setHelmet(head);
        }
    }

    private int resolveSoulDurationTicks() {
        if (profile.durationMillis() <= 0) {
            return 0;
        }
        return (int) Math.max(20L, profile.durationMillis() / 50L);
    }

    private void reconnectSoul(Player target, boolean playEffects) {
        SoulSplitState state = activeSplits.remove(target.getUniqueId());
        if (state == null) {
            return;
        }
        state.cancelTasks();
        ArmorStand body = state.body;
        bodyLookup.remove(body.getUniqueId());
        Location reconnectLocation = body.getLocation().clone().add(0, 0.1, 0);
        body.remove();

        target.teleport(reconnectLocation);
        target.setGameMode(state.originalGameMode);
        target.setAllowFlight(state.originalAllowFlight);
        target.setFlying(state.originalAllowFlight && state.originalFlying);
        target.setInvulnerable(state.originalInvulnerable);
        target.setCollidable(state.originalCollidable);
        target.setFallDistance(0f);
        target.setVelocity(new Vector(0, 0, 0));
        target.removePotionEffect(PotionEffectType.INVISIBILITY);
        if (playEffects) {
            reconnectLocation.getWorld().spawnParticle(Particle.SOUL, reconnectLocation.add(0, 1, 0), 16, 0.4, 0.6, 0.4, 0.05);
            reconnectLocation.getWorld().playSound(reconnectLocation, Sound.BLOCK_BEACON_ACTIVATE, 0.9f, 1.1f);
            target.sendMessage(ChatColor.GREEN + "Your soul has reconnected.");
        }
        if (primedPlayers.isEmpty() && activeSplits.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    private void cleanupSoulSplit(UUID playerId) {
        SoulSplitState state = activeSplits.remove(playerId);
        if (state == null) {
            return;
        }
        state.cancelTasks();
        bodyLookup.remove(state.body.getUniqueId());
        state.body.remove();
        if (primedPlayers.isEmpty() && activeSplits.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    private void applyBodyKnockback(ArmorStand stand, Entity damager) {
        Vector direction;
        if (damager instanceof Projectile projectile && projectile.getVelocity().lengthSquared() > 0.01) {
            direction = projectile.getVelocity().normalize();
        } else {
            direction = stand.getLocation().toVector().subtract(damager.getLocation().toVector()).normalize();
        }
        double strength = plugin.getGlitchSettings().getCombatDefaults().knockbackStrength() * profile.knockbackMultiplier();
        stand.setVelocity(direction.multiply(strength).setY(0.15 + strength * 0.35));
    }

    private static class SoulSplitState {
        private final ArmorStand body;
        private final GameMode originalGameMode;
        private final boolean originalAllowFlight;
        private final boolean originalFlying;
        private final boolean originalCollidable;
        private final boolean originalInvulnerable;
        private BukkitTask particleTask;
        private BukkitTask timeoutTask;

        private SoulSplitState(
            ArmorStand body,
            GameMode originalGameMode,
            boolean originalAllowFlight,
            boolean originalFlying,
            boolean originalCollidable,
            boolean originalInvulnerable
        ) {
            this.body = body;
            this.originalGameMode = originalGameMode;
            this.originalAllowFlight = originalAllowFlight;
            this.originalFlying = originalFlying;
            this.originalCollidable = originalCollidable;
            this.originalInvulnerable = originalInvulnerable;
        }

        private void setParticleTask(BukkitTask task) {
            this.particleTask = task;
        }

        private void setTimeoutTask(BukkitTask task) {
            this.timeoutTask = task;
        }

        private void cancelTasks() {
            if (particleTask != null) {
                particleTask.cancel();
            }
            if (timeoutTask != null) {
                timeoutTask.cancel();
            }
        }
    }
}
