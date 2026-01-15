package org.nu11ified.glitchSMP.glitch;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum of all available glitch types in the Glitch SMP plugin.
 */
public enum GlitchType {
    BEDROCK(
        "Bedrock Glitch",
        "All placed blocks become temporarily unbreakable.",
        minutesToMillis(2),
        secondsToMillis(15),
        14,
        ""
    ),
    IMMORTALITY(
        "Immortality Glitch",
        "Ignore all incoming damage for a short time.",
        minutesToMillis(5),
        secondsToMillis(30),
        19,
        ""
    ),
    INVENTORY(
        "Inventory Glitch",
        "Scrambles a target's inventory and blocks item usage.",
        minutesToMillis(2),
        secondsToMillis(7),
        16,
        ""
    ),
    REWIND(
        "Rewind Glitch",
        "Warp back to where you activated the glitch.",
        minutesToMillis(1),
        secondsToMillis(15),
        17,
        ""
    ),
    CHUNK(
        "Chunk Glitch",
        "Create a one-chunk border no one can cross.",
        minutesToMillis(2),
        secondsToMillis(10),
        18,
        ""
    ),
    VIRUS(
        "Virus Glitch",
        "Pass a green-screen infection to chained targets.",
        minutesToMillis(2),
        secondsToMillis(10),
        6,
        ""
    ),
    ENCHANTER(
        "Enchanter Glitch",
        "Temporarily boosts held tool enchantments by +1.",
        minutesToMillis(3),
        secondsToMillis(30),
        5,
        ""
    ),
    REDSTONE(
        "Redstone Glitch",
        "Gain bonus damage near redstone power sources.",
        minutesToMillis(4),
        secondsToMillis(30),
        4,
        ""
    ),
    FAKE_BLOCK(
        "Fake Block Glitch",
        "Spawn a fake block from the one you're holding.",
        minutesToMillis(2),
        0,
        3,
        ""
    ),
    FREEZE(
        "Freeze Glitch",
        "Hit a target in 10s to freeze them in ice.",
        minutesToMillis(1) + secondsToMillis(30),
        secondsToMillis(10),
        2,
        ""
    ),
    WINDBURST(
        "Windburst Glitch",
        "Launch yourself forward at blazing speed.",
        secondsToMillis(30),
        0,
        7,
        ""
    ),
    HYPNOSIS(
        "Hypnosis Glitch",
        "Next hit forces a target to click the green.",
        minutesToMillis(2),
        secondsToMillis(6),
        9,
        ""
    ),
    GRAVITY(
        "Gravity Glitch",
        "Low gravity affects players in a small radius.",
        minutesToMillis(1),
        secondsToMillis(15),
        10,
        ""
    ),
    HORSETAMER(
        "Horsetamer Glitch",
        "Summon a Skeleton Horse.",
        minutesToMillis(10),
        0,
        11,
        ""
    ),
    TELEKINESIS(
        "Telekinesis Glitch",
        "Control the next player you strike after activation.",
        minutesToMillis(3),
        secondsToMillis(15),
        12,
        ""
    ),
    RAYCAST(
        "Raycast Glitch",
        "Fire a beam with damage based on block resistance.",
        minutesToMillis(2),
        0,
        13,
        ""
    );

    private final String name;
    private final String description;
    private final long cooldownMillis;
    private final long durationMillis;
    private final int modelData;
    private final String actionBarIcon;

    GlitchType(String name, String description, long cooldownMillis, long durationMillis, int modelData, String actionBarIcon) {
        this.name = name;
        this.description = description;
        this.cooldownMillis = cooldownMillis;
        this.durationMillis = durationMillis;
        this.modelData = modelData;
        this.actionBarIcon = actionBarIcon;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getCooldownMillis() {
        return cooldownMillis;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public int getModelData() {
        return modelData;
    }

    public String getActionBarIcon() {
        return actionBarIcon;
    }

    public static Optional<GlitchType> fromModelData(int modelData) {
        return Arrays.stream(values())
            .filter(type -> type.modelData == modelData)
            .findFirst();
    }

    private static long minutesToMillis(int minutes) {
        return minutes * 60_000L;
    }

    private static long secondsToMillis(int seconds) {
        return seconds * 1_000L;
    }
}
