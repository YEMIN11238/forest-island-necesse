package necesse.inventory.item.toolItem.swordToolItem;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.sound.SoundSettings;
import necesse.gfx.GameResources;
import necesse.inventory.item.Item;

public class SyndicateBatSwordToolItem extends SwordToolItem {
   public SyndicateBatSwordToolItem() {
      super(100, null);
      this.rarity = Item.Rarity.NORMAL;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(15.0F).setUpgradedValue(1.0F, 110.83337F);
      this.attackRange.setBaseValue(75);
      this.knockback.setBaseValue(75);
      this.canBeUsedForRaids = false;
      this.attackXOffset = 10;
      this.attackYOffset = 10;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.woodSword).volume(0.8F);
   }

   @Override
   public GameMessage getNewLocalization() {
      return new StaticMessage("NOT_OBTAINABLE: Syndicate Bat");
   }
}
