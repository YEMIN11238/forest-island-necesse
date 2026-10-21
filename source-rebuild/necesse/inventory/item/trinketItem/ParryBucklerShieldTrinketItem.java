package necesse.inventory.item.trinketItem;

import necesse.engine.localization.Localization;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasHitEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.TrinketsLootTable;

public class ParryBucklerShieldTrinketItem extends ShieldTrinketItem {
   public ParryBucklerShieldTrinketItem(Item.Rarity rarity, int enchantCost) {
      super(rarity, 3, 0.25F, 5000, 0.35F, 65, 360.0F, enchantCost, TrinketsLootTable.trinkets);
      this.isPerfectBlocker = true;
   }

   @Override
   public ListGameTooltips getExtraShieldTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getExtraShieldTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "parrybucklertip"), 400);
      return tooltips;
   }

   @Override
   public float getShieldFinalDamageMultiplier(InventoryItem item, Mob mob) {
      return this.canPerfectBlock(mob) ? 0.0F : super.getShieldFinalDamageMultiplier(item, mob);
   }

   @Override
   public float getShieldStaminaUsageOnBlock(InventoryItem item, Mob mob) {
      return this.canPerfectBlock(mob) ? 0.1F : super.getShieldStaminaUsageOnBlock(item, mob);
   }

   @Override
   public int getKnockback(Mob mob) {
      return this.canPerfectBlock(mob) ? 200 : super.getKnockback(mob);
   }

   @Override
   public void playHitSound(InventoryItem item, Mob mob, MobWasHitEvent hitEvent) {
      SoundManager.playSound(GameResources.cling, SoundEffect.effect(mob).pitch(1.5F).volume(0.8F));
   }

   @Override
   public void onPerfectBlock(Mob mob) {
      super.onPerfectBlock(mob);
      mob.buffManager.addBuff(new ActiveBuff(BuffRegistry.PARRY_BUCKLER_PARRIED, mob, 5000, null), false);
   }
}
