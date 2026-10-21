package necesse.inventory.item.armorItem.dusk;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.BootsArmorItem;
import necesse.inventory.item.upgradeUtils.FloatUpgradeValue;
import necesse.inventory.lootTable.presets.IncursionFeetArmorLootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class DuskBootsArmorItem extends BootsArmorItem {
   public FloatUpgradeValue speed = new FloatUpgradeValue().setBaseValue(0.25F).setUpgradedValue(1.0F, 0.25F);
   public GameTexture brokenTexture;

   public DuskBootsArmorItem() {
      super(17, 1900, Item.Rarity.EPIC, "duskboots", IncursionFeetArmorLootTable.incursionFeetArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.DASH_COOLDOWN, -0.15F),
         new ModifierValue<>(BuffModifiers.MANA_USAGE, -0.15F),
         new ModifierValue<>(BuffModifiers.SPEED, this.speed.getValue(this.getUpgradeTier(item)))
      );
   }

   @Override
   protected void loadArmorTexture() {
      super.loadArmorTexture();
      this.brokenTexture = GameTexture.fromFile("player/armor/" + this.textureName + "_broken");
   }

   @Override
   public DrawOptions getArmorDrawOptions(
      InventoryItem item,
      Level level,
      PlayerMob player,
      InventoryItem headItem,
      InventoryItem chestItem,
      InventoryItem feetItem,
      int spriteX,
      int spriteY,
      int spriteRes,
      int drawX,
      int drawY,
      int width,
      int height,
      boolean mirrorX,
      boolean mirrorY,
      GameLight light,
      boolean hasGlowEffect,
      int glowHash,
      float alpha,
      MaskShaderOptions mask
   ) {
      return level != null && !level.getWorldEntity().isNight()
         ? this.brokenTexture
            .initDraw()
            .sprite(spriteX, spriteY, spriteRes)
            .startGlowOptions(level, glowHash)
            .light(light)
            .apply(hasGlowEffect)
            .alpha(alpha)
            .size(width, height)
            .mirror(mirrorX, mirrorY)
            .addMaskShader(mask)
            .pos(drawX, drawY)
         : super.getArmorDrawOptions(
            item,
            level,
            player,
            headItem,
            chestItem,
            feetItem,
            spriteX,
            spriteY,
            spriteRes,
            drawX,
            drawY,
            width,
            height,
            mirrorX,
            mirrorY,
            light,
            hasGlowEffect,
            glowHash,
            alpha,
            mask
         );
   }
}
