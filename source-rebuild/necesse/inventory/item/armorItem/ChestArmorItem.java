package necesse.inventory.item.armorItem;

import java.awt.Color;
import necesse.engine.localization.Localization;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class ChestArmorItem extends ArmorItem {
   public final String armsTextureName;
   public GameTexture rightArmsTexture;
   public GameTexture leftArmsTexture;
   public GameTexture frontLeftArmsTexture;
   public GameTexture frontRightArmsTexture;

   public ChestArmorItem(int armorValue, int enchantCost, String bodyTextureName, String armsTextureName, OneOfLootItems lootTableCategory) {
      super(ArmorItem.ArmorType.CHEST, armorValue, enchantCost, lootTableCategory, bodyTextureName);
      this.armsTextureName = armsTextureName;
      this.tierOneEssencesUpgradeRequirement = "cryoessence";
      this.tierTwoEssencesUpgradeRequirement = "bloodessence";
      this.tierThreeEssenceUpgradeRequirement = "crystalessence";
   }

   public ChestArmorItem(int armorValue, int enchantCost, Item.Rarity rarity, String bodyTextureName, String armsTextureName, OneOfLootItems lootTableCategory) {
      this(armorValue, enchantCost, bodyTextureName, armsTextureName, lootTableCategory);
      this.rarity = rarity;
   }

   @Override
   protected void loadArmorTexture() {
      super.loadArmorTexture();
      if (this.armsTextureName != null) {
         this.leftArmsTexture = GameTexture.fromFile("player/armor/" + this.armsTextureName + "_left");
         this.rightArmsTexture = GameTexture.fromFile("player/armor/" + this.armsTextureName + "_right");
         this.frontLeftArmsTexture = this.loadTextureIfExists("player/armor/" + this.armsTextureName + "_left_front");
         this.frontRightArmsTexture = this.loadTextureIfExists("player/armor/" + this.armsTextureName + "_right_front");
      }
   }

   public GameTexture getArmorLeftArmsTexture(
      InventoryItem item, Level level, PlayerMob player, InventoryItem headItem, InventoryItem chestItem, InventoryItem feetItem
   ) {
      return this.leftArmsTexture;
   }

   public GameTexture getArmorRightArmsTexture(
      InventoryItem item, Level level, PlayerMob player, InventoryItem headItem, InventoryItem chestItem, InventoryItem feetItem
   ) {
      return this.rightArmsTexture;
   }

   public GameTexture getFrontArmorRightArmsTexture(InventoryItem item, PlayerMob player) {
      return this.frontRightArmsTexture;
   }

   public GameTexture getFrontArmorLeftArmsTexture(InventoryItem item, PlayerMob player) {
      return this.frontLeftArmsTexture;
   }

   public GameSprite getAttackArmSprite(
      InventoryItem item, Level level, PlayerMob player, InventoryItem headItem, InventoryItem chestItem, InventoryItem feetItem
   ) {
      GameTexture armorTexture = this.getArmorTexture(item, level, player, headItem, chestItem, feetItem);
      return armorTexture == null ? null : new GameSprite(armorTexture, 0, 8, 32);
   }

   public DrawOptions getArmorLeftArmsDrawOptions(
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
      GameTexture armorTexture = this.getArmorLeftArmsTexture(item, level, player, headItem, chestItem, feetItem);
      if (armorTexture != null) {
         Color col = this.getDrawColor(item, player);
         return armorTexture.initDraw()
            .sprite(spriteX, spriteY, spriteRes)
            .startGlowOptions(level, glowHash)
            .colorLight(col, light)
            .apply(hasGlowEffect)
            .alpha(alpha)
            .size(width, height)
            .mirror(mirrorX, mirrorY)
            .addMaskShader(mask)
            .pos(drawX, drawY);
      } else {
         return () -> {};
      }
   }

   public DrawOptions getArmorRightArmsDrawOptions(
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
      GameTexture armorTexture = this.getArmorRightArmsTexture(item, level, player, headItem, chestItem, feetItem);
      if (armorTexture != null) {
         Color col = this.getDrawColor(item, player);
         return armorTexture.initDraw()
            .sprite(spriteX, spriteY, spriteRes)
            .startGlowOptions(level, glowHash)
            .colorLight(col, light)
            .apply(hasGlowEffect)
            .alpha(alpha)
            .size(width, height)
            .mirror(mirrorX, mirrorY)
            .addMaskShader(mask)
            .pos(drawX, drawY);
      } else {
         return () -> {};
      }
   }

   public DrawOptions getFrontArmorLeftArmsDrawOptions(
      InventoryItem item,
      Level level,
      PlayerMob player,
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
      GameTexture backArmorTexture = this.getFrontArmorLeftArmsTexture(item, player);
      if (backArmorTexture != null) {
         Color col = this.getDrawColor(item, player);
         return backArmorTexture.initDraw()
            .sprite(spriteX, spriteY, spriteRes)
            .startGlowOptions(level, glowHash)
            .colorLight(col, light)
            .apply(hasGlowEffect)
            .alpha(alpha)
            .size(width, height)
            .mirror(mirrorX, mirrorY)
            .addMaskShader(mask)
            .pos(drawX, drawY);
      } else {
         return null;
      }
   }

   public DrawOptions getFrontArmorRightArmsDrawOptions(
      InventoryItem item,
      Level level,
      PlayerMob player,
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
      GameTexture backArmorTexture = this.getFrontArmorRightArmsTexture(item, player);
      if (backArmorTexture != null) {
         Color col = this.getDrawColor(item, player);
         return backArmorTexture.initDraw()
            .sprite(spriteX, spriteY, spriteRes)
            .startGlowOptions(level, glowHash)
            .colorLight(col, light)
            .apply(hasGlowEffect)
            .alpha(alpha)
            .size(width, height)
            .mirror(mirrorX, mirrorY)
            .addMaskShader(mask)
            .pos(drawX, drawY);
      } else {
         return null;
      }
   }

   @Override
   public String getTranslatedTypeName() {
      return Localization.translate("item", "bodyarmor");
   }
}
