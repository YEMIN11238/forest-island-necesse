package necesse.inventory.item.armorItem.dawn;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.ChestArmorItem;
import necesse.inventory.lootTable.presets.IncursionBodyArmorLootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class DawnChestplateArmorItem extends ChestArmorItem {
   public GameTexture brokenTexture;
   public GameTexture brokenLeftArmTexture;
   public GameTexture brokenRightArmTexture;

   public DawnChestplateArmorItem() {
      super(29, 1900, Item.Rarity.EPIC, "dawnchestplate", "dawnarms", IncursionBodyArmorLootTable.incursionBodyArmor);
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(new ModifierValue<>(BuffModifiers.RESILIENCE_GAIN, 0.25F), new ModifierValue<>(BuffModifiers.ATTACK_SPEED, 0.1F));
   }

   @Override
   protected void loadArmorTexture() {
      super.loadArmorTexture();
      this.brokenTexture = GameTexture.fromFile("player/armor/" + this.textureName + "_broken");
      this.brokenLeftArmTexture = GameTexture.fromFile("player/armor/dawnarms_left_broken");
      this.brokenRightArmTexture = GameTexture.fromFile("player/armor/dawnarms_right_broken");
   }

   @Override
   public GameTexture getArmorLeftArmsTexture(
      InventoryItem item, Level level, PlayerMob player, InventoryItem headItem, InventoryItem chestItem, InventoryItem feetItem
   ) {
      return level != null && level.getWorldEntity().isNight()
         ? this.brokenLeftArmTexture
         : super.getArmorLeftArmsTexture(item, level, player, headItem, chestItem, feetItem);
   }

   @Override
   public GameTexture getArmorRightArmsTexture(
      InventoryItem item, Level level, PlayerMob player, InventoryItem headItem, InventoryItem chestItem, InventoryItem feetItem
   ) {
      return level != null && level.getWorldEntity().isNight()
         ? this.brokenRightArmTexture
         : super.getArmorRightArmsTexture(item, level, player, headItem, chestItem, feetItem);
   }

   @Override
   public GameSprite getAttackArmSprite(
      InventoryItem item, Level level, PlayerMob player, InventoryItem headItem, InventoryItem chestItem, InventoryItem feetItem
   ) {
      if (level != null && level.getWorldEntity().isNight()) {
         return this.brokenTexture == null ? null : new GameSprite(this.brokenTexture, 0, 8, 32);
      } else {
         return super.getAttackArmSprite(item, level, player, headItem, chestItem, feetItem);
      }
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
      return level != null && level.getWorldEntity().isNight()
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
