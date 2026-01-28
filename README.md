# GlitchSMP Plugin

GlitchSMP is a Minecraft (1.21.x) plugin that adds equippable "glitches" with custom texturepack icons. Players can equip up to two glitches and activate them with the offhand keybind.

## ✨ Glitches Included

Only the glitches that exist in the bundled texturepack are enabled:

- 🧱 **Bedrock Glitch** — placed blocks become temporarily unbreakable
- ♾ **Immortality Glitch** — ignore all damage briefly
- 🎒 **Inventory Glitch** — scramble inventory and block item usage
- ⏪ **Rewind Glitch** — save your location and snap back within the window
- 🧭 **Chunk Glitch** — one-chunk border only you can exit
- 🦠 **Virus Glitch** — chained infection with a green-screen overlay
- ✨ **Enchanter Glitch** — temporarily boost enchantments by +1
- 🔴 **Redstone Glitch** — bonus damage near redstone power
- 🧊 **Fake Block Glitch** — spawn a fake block from the held block
- ❄ **Freeze Glitch** — freeze chained targets
- 💨 **Dash Glitch** — burst forward in the direction you're facing
- 🌪 **Windburst Glitch** — unleash a barrage of wind charges after activation
- 📘 **Hypnosis Glitch** — force Book & Quill signing + drop
- 🌌 **Gravity Glitch** — low gravity in a short radius
- 🐎 **Horsetamer Glitch** — summon a Skeleton Horse
- 🧠 **Telekinesis Glitch** — control the next player you strike
- ☢ **Raycast Glitch** — fire a beam based on block resistance

## 🎮 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/glitch list [all]` | `glitchsmp.command.glitch.list` | List equipped slots + available glitches |
| `/glitch view [player]` | `glitchsmp.command.glitch` | View a player's equipped glitches |
| `/glitch help` | `glitchsmp.command.glitch` | Show help |
| `/glitches` | `glitchsmp.command.glitches` | Open the operator glitch item GUI |
| `/withdraw [1|2]` | `glitchsmp.command.withdraw` | Withdraw the right/left slot into an item |

> ✅ `/glitches` is operator-only by default.

## 🧩 Equipping & Activation

- **Equip**: right-click a glitch item to place it in the first empty slot.
- **Withdraw**: use `/withdraw` to remove the right slot first, then the left slot. You can also specify `/withdraw 1` or `/withdraw 2`.
- **Activate**:
  - **Right slot** → offhand keybind (default: **F**)
  - **Left slot** → crouch + offhand keybind
- **Action bar**: shows two icons. Empty slots display the glitch token icon.

## 🧪 Crafting

Glitches can be crafted via recipes in `recipes.yml`. You can customize materials and layouts there.

## 🔐 Permissions

```yaml
glitchsmp.command.glitch: true          # /glitch base command
glitchsmp.command.glitch.list: true     # /glitch list
glitchsmp.command.glitches: op          # /glitches GUI
glitchsmp.command.withdraw: true        # /withdraw
```

## 🛠️ Build

This plugin supports Maven and Gradle.

```bash
mvn clean package test
```
