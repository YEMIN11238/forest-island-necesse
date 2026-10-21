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
import necesse.entity.projectile.SwampTomeProjectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.MagicWeaponsLootTable;
import necesse.level.maps.Level;

public class SwampTomeProjectileToolItem extends MagicProjectileToolItem {
   public SwampTomeProjectileToolItem() {
      super(850, MagicWeaponsLootTable.magicWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(600);
      this.attackDamage.setBaseValue(48.0F).setUpgradedValue(1.0F, 112.00004F);
      this.velocity.setBaseValue(50);
      this.attackXOffset = 10;
      this.attackYOffset = 10;
      this.attackRange.setBaseValue(2500);
      this.knockback.setBaseValue(10);
      this.manaCost.setBaseValue(1.5F).setUpgradedValue(1.0F, 2.0F);
      this.itemAttackerProjectileCanHitWidth = 12.0F;
      this.itemAttackerPredictionDistanceOffset = -20.0F;
      this.canBeUsedForRaids = true;
      this.raidTicketsModifier = 0.5F;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "swamptometip"));
      return tooltips;
   }

   @Override
   public int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
      return this.getAttackRange(item) / 5;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.swampTome).volume(0.3F);
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
      GameRandom random = new GameRandom(seed);
      Projectile projectile = new SwampTomeProjectile(
         level,
         attackerMob,
         attackerMob.x,
         attackerMob.y,
         x,
         y,
         this.getProjectileVelocity(item, attackerMob),
         this.getAttackRange(item),
         this.getAttackDamage(item),
         this.getKnockback(item, attackerMob)
      );
      projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
      projectile.resetUniqueID(random);
      attackerMob.addAndSendAttackerProjectile(projectile, 20);
      this.consumeMana(attackerMob, item);
      return item;
   }
}
