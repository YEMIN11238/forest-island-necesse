package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.VenomStaffProjectile;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.MagicWeaponsLootTable;
import necesse.level.maps.Level;

public class VenomStaffProjectileToolItem extends MagicProjectileToolItem {
   public VenomStaffProjectileToolItem() {
      super(350, MagicWeaponsLootTable.magicWeapons);
      this.rarity = Item.Rarity.UNCOMMON;
      this.attackAnimTime.setBaseValue(500);
      this.itemCooldownTime.setBaseValue(2000);
      this.attackDamage.setBaseValue(20.0F).setUpgradedValue(1.0F, 63.00002F);
      this.velocity.setBaseValue(150);
      this.attackXOffset = 20;
      this.attackYOffset = 20;
      this.attackRange.setBaseValue(300);
      this.manaCost.setBaseValue(5.0F).setUpgradedValue(1.0F, 5.0F);
      this.resilienceGain.setBaseValue(2.0F);
      this.itemAttackerProjectileCanHitWidth = 5.0F;
      this.itemAttackerPredictionDistanceOffset = -20.0F;
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.5F;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "venomstafftip"));
      return tooltips;
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
      int distance = (int)Math.min(this.getAttackRange(item), attackerMob.getDistance(x, y));
      Projectile projectile = new VenomStaffProjectile(
         level,
         attackerMob.x,
         attackerMob.y,
         x,
         y,
         this.getProjectileVelocity(item, attackerMob),
         distance,
         this.getAttackDamage(item),
         this.getResilienceGain(item),
         attackerMob
      );
      projectile.resetUniqueID(new GameRandom(seed));
      attackerMob.addAndSendAttackerProjectile(projectile, 20);
      this.consumeMana(attackerMob, item);
      return item;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.slimeSpawn);
   }
}
