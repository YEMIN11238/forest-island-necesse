package necesse.inventory.item.toolItem.projectileToolItem.meleeProjectileToolItem;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.boomerangProjectile.AnchorBoomerangProjectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.projectileToolItem.throwToolItem.boomerangToolItem.BoomerangToolItem;
import necesse.inventory.lootTable.presets.ThrowWeaponsLootTable;
import necesse.level.maps.Level;

public class AnchorAndChainToolItem extends BoomerangToolItem {
   public AnchorAndChainToolItem() {
      super(1600, ThrowWeaponsLootTable.throwWeapons, "anchor");
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(500);
      this.attackDamage.setBaseValue(70.0F).setUpgradedValue(1.0F, 99.166695F);
      this.velocity.setBaseValue(250);
      this.knockback.setBaseValue(0);
      this.resilienceGain.setBaseValue(2.0F).setUpgradedValue(1.0F, 2.0F).setUpgradedValue(10.0F, 3.0F);
      this.attackXOffset = 8;
      this.attackYOffset = 6;
      this.attackRange.setBaseValue(350);
      this.itemAttackerProjectileCanHitWidth = 8.0F;
      this.itemAttackerPredictionDistanceOffset = -20.0F;
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.5F;
      this.useForRaidsOnlyIfObtained = true;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "anchorandchaintip"));
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
      Projectile projectile = new AnchorBoomerangProjectile(
         level,
         attackerMob.x,
         attackerMob.y,
         x,
         y,
         this.getProjectileVelocity(item, attackerMob),
         this.getAttackRange(item),
         this.getAttackDamage(item),
         this.getKnockback(item, attackerMob),
         attackerMob
      );
      projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
      projectile.resetUniqueID(new GameRandom(seed));
      attackerMob.boomerangs.add(projectile);
      attackerMob.addAndSendAttackerProjectile(projectile, 20);
      return item;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.swing2).volume(0.3F).pitchVariance(0.4F);
   }
}
