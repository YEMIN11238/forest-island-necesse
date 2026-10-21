package necesse.inventory.item.toolItem.summonToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.SummonWeaponsLootTable;
import necesse.level.maps.Level;

public class SentientSwordSummonToolItem extends SummonToolItem {
   public SentientSwordSummonToolItem() {
      super("sentientsword", FollowPosition.FLYING_CIRCLE_FAST, 2.0F, 750, SummonWeaponsLootTable.summonWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackDamage.setBaseValue(36.0F).setUpgradedValue(1.0F, 70.000015F);
   }

   @Override
   public GameTooltips getSpaceTakenTooltip(InventoryItem item, PlayerMob perspective) {
      return null;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "sentientswordtip"));
      return tooltips;
   }

   @Override
   public void showAttack(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int animAttack, int seed, GNDItemMap mapContent
   ) {
      if (level.isClient()) {
         SoundManager.playSound(
            GameResources.magicbolt4, SoundEffect.effect(attackerMob).volume(0.2F).pitch(GameRandom.globalRandom.getFloatBetween(1.1F, 1.2F))
         );
         SoundManager.playSound(GameResources.shears, SoundEffect.effect(attackerMob).volume(0.8F).pitch(GameRandom.globalRandom.getFloatBetween(2.1F, 2.2F)));
      }
   }
}
