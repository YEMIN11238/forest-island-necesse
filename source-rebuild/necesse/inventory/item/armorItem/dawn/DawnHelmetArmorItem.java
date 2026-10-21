package necesse.inventory.item.armorItem.dawn;

import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.armorItem.ArmorModifiers;
import necesse.inventory.item.armorItem.SetHelmetArmorItem;
import necesse.inventory.lootTable.presets.IncursionArmorSetsLootTable;
import necesse.inventory.lootTable.presets.IncursionHeadArmorLootTable;
import necesse.level.maps.Level;
import necesse.level.maps.incursion.IncursionData;
import necesse.level.maps.light.GameLight;

public class DawnHelmetArmorItem extends SetHelmetArmorItem {
   public GameTexture brokenTexture;

   public DawnHelmetArmorItem() {
      super(
         23,
         null,
         1900,
         IncursionHeadArmorLootTable.incursionHeadArmor,
         IncursionArmorSetsLootTable.incursionArmorSets,
         Item.Rarity.EPIC,
         "dawnhelmet",
         "dawnchestplate",
         "dawnboots",
         "dawnsetbonus"
      );
      this.facialFeatureDrawOptions = ArmorItem.FacialFeatureDrawMode.OVER_FACIAL_FEATURE;
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.minRaidTier = 1;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
   }

   @Override
   public ArmorModifiers getArmorModifiers(InventoryItem item, Mob mob) {
      return new ArmorModifiers(
         new ModifierValue<>(BuffModifiers.MAX_RESILIENCE_FLAT, 40),
         new ModifierValue<>(BuffModifiers.MELEE_DAMAGE, 0.2F),
         new ModifierValue<>(BuffModifiers.RANGED_DAMAGE, 0.2F)
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
