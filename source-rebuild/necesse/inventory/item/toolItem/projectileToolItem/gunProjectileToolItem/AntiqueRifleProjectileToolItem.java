package necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem;

import necesse.engine.sound.SoundSettings;
import necesse.gfx.GameResources;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.GunWeaponsLootTable;
import necesse.level.maps.incursion.IncursionData;

public class AntiqueRifleProjectileToolItem extends GunProjectileToolItem {
   public AntiqueRifleProjectileToolItem() {
      super(NORMAL_AMMO_TYPES, 1750, GunWeaponsLootTable.gunWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(600);
      this.attackDamage.setBaseValue(125.0F).setUpgradedValue(1.0F, 140.00003F);
      this.attackXOffset = 20;
      this.attackYOffset = 10;
      this.velocity.setBaseValue(650);
      this.moveDist = 65;
      this.attackRange.setBaseValue(2000);
      this.addGlobalIngredient("bulletuser");
      this.canBeUsedForRaids = true;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
      this.raidTicketsModifier = 0.5F;
      this.useForRaidsOnlyIfObtained = true;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.antiqueRifle).volume(0.2F);
   }
}
