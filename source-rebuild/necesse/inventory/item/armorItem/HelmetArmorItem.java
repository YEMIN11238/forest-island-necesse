package necesse.inventory.item.armorItem;

import necesse.engine.localization.Localization;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.entity.mobs.gameDamageType.DamageType;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;

public class HelmetArmorItem extends ArmorItem {
   public DamageType damageClass;
   public String hairMaskTextureName;
   public GameTexture hairMaskTexture;

   public HelmetArmorItem(int armorValue, DamageType damageClass, int enchantCost, String textureName, OneOfLootItems lootTableCategory) {
      super(ArmorItem.ArmorType.HEAD, armorValue, enchantCost, lootTableCategory, textureName);
      this.damageClass = damageClass;
      if (armorValue > 0) {
         int upgradedValue;
         if (damageClass == DamageTypeRegistry.MELEE) {
            upgradedValue = 30;
         } else if (damageClass == DamageTypeRegistry.RANGED) {
            upgradedValue = 24;
         } else if (damageClass == DamageTypeRegistry.MAGIC) {
            upgradedValue = 22;
         } else if (damageClass == DamageTypeRegistry.SUMMON) {
            upgradedValue = 17;
         } else {
            upgradedValue = 30;
         }

         this.armorValue.setUpgradedValue(1.0F, upgradedValue);
         if (armorValue > upgradedValue) {
            this.armorValue.setBaseValue(upgradedValue);
         }
      }

      this.tierOneEssencesUpgradeRequirement = "shadowessence";
      this.tierTwoEssencesUpgradeRequirement = "slimeessence";
      this.tierThreeEssenceUpgradeRequirement = "radiatedessence";
   }

   public HelmetArmorItem(int armorValue, DamageType damageClass, int enchantCost, Item.Rarity itemRarity, String textureName, OneOfLootItems lootTableCategory) {
      this(armorValue, damageClass, enchantCost, textureName, lootTableCategory);
      this.rarity = itemRarity;
   }

   @Override
   protected void loadArmorTexture() {
      super.loadArmorTexture();
      if (this.hairMaskTextureName != null) {
         this.hairMaskTexture = GameTexture.fromFile("player/armor/" + this.hairMaskTextureName);
      } else {
         this.hairMaskTexture = this.loadTextureIfExists("player/armor/" + this.textureName + "_hairmask");
      }

      if (this.hairMaskTexture != null) {
         this.hairMaskTexture.setBlendQuality(GameTexture.BlendQuality.NEAREST);
      }
   }

   @Override
   public String getTranslatedTypeName() {
      return Localization.translate("item", "helmet");
   }
}
