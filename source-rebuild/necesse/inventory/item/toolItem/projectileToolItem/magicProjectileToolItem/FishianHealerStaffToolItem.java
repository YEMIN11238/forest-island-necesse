package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class FishianHealerStaffToolItem extends MagicProjectileToolItem {
   public FishianHealerStaffToolItem() {
      super(0, null);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(1000);
      this.attackDamage.setBaseValue(1.0F).setUpgradedValue(1.0F, 1.0F);
      this.attackXOffset = 30;
      this.attackYOffset = 30;
      this.attackRange.setBaseValue(1000);
      this.manaCost.setBaseValue(10.0F);
      this.resilienceGain.setBaseValue(0.0F);
   }

   @Override
   public GameMessage getNewLocalization() {
      return new StaticMessage("NOT_OBTAINABLE: Fishian Healer Staff");
   }

   @Override
   public void showAttack(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int animAttack, int seed, GNDItemMap mapContent
   ) {
      if (level.isClient()) {
         SoundManager.playSound(
            GameResources.magicbolt1, SoundEffect.effect(attackerMob).volume(0.3F).pitch(GameRandom.globalRandom.getFloatBetween(1.5F, 1.6F))
         );
      }
   }

   @Override
   public InventoryItem onAttack(
      Level level,
      int x,
      int y,
      ItemAttackerMob attackerMob,
      int attackHeight,
      InventoryItem item,
      ItemAttackSlot slot,
      int animAttack,
      int seed,
      GNDItemMap mapContent
   ) {
      return item;
   }
}
