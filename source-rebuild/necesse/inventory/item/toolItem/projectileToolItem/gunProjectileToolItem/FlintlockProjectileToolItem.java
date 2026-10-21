package necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem;

import necesse.engine.sound.SoundSettings;
import necesse.gfx.GameResources;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.GunWeaponsLootTable;

public class FlintlockProjectileToolItem extends GunProjectileToolItem {
   public FlintlockProjectileToolItem() {
      super(NORMAL_AMMO_TYPES, 1150, GunWeaponsLootTable.gunWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(400);
      this.attackDamage.setBaseValue(47.0F).setUpgradedValue(1.0F, 99.166695F);
      this.attackXOffset = 8;
      this.attackYOffset = 10;
      this.attackRange.setBaseValue(800);
      this.velocity.setBaseValue(450);
      this.addGlobalIngredient("bulletuser");
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.flintLock).volume(0.5F);
   }
}
