package necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.level.maps.Level;

public class InvisibleSwordToolItem extends GreatswordToolItem {
   public InvisibleSwordToolItem() {
      super(0, null, getThreeChargeLevels(500, 600, 700));
      this.rarity = Item.Rarity.COMMON;
      this.attackDamage.setBaseValue(35.0F).setUpgradedValue(1.0F, 192.50005F);
      this.attackRange.setBaseValue(70);
      this.knockback.setBaseValue(150);
      this.attackXOffset = 12;
      this.attackYOffset = 12;
   }

   @Override
   public GameMessage getNewLocalization() {
      return new StaticMessage("NOT_OBTAINABLE: Invisible Sword");
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

   @Override
   protected SoundSettings getGreatswordSwingSound1() {
      return new SoundSettings(GameResources.woodGreatsword1);
   }

   @Override
   protected SoundSettings getGreatswordSwingSound2() {
      return new SoundSettings(GameResources.woodGreatsword2);
   }

   @Override
   protected SoundSettings getGreatswordSwingSound3() {
      return new SoundSettings(GameResources.woodGreatsword3);
   }
}
