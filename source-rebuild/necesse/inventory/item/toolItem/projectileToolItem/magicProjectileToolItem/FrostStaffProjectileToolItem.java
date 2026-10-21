package necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem;

import java.awt.geom.Point2D.Float;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.FrostStaffProjectile;
import necesse.entity.projectile.Projectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.MagicWeaponsLootTable;
import necesse.level.maps.Level;

public class FrostStaffProjectileToolItem extends MagicProjectileToolItem {
   public FrostStaffProjectileToolItem() {
      super(500, MagicWeaponsLootTable.magicWeapons);
      this.rarity = Item.Rarity.COMMON;
      this.attackAnimTime.setBaseValue(400);
      this.attackCooldownTime.setBaseValue(500);
      this.attackDamage.setBaseValue(19.0F).setUpgradedValue(1.0F, 84.00002F);
      this.velocity.setBaseValue(400);
      this.attackXOffset = 26;
      this.attackYOffset = 26;
      this.attackRange.setBaseValue(800);
      this.knockback.setBaseValue(50);
      this.manaCost.setBaseValue(1.0F).setUpgradedValue(1.0F, 1.0F);
      this.resilienceGain.setBaseValue(0.75F);
      this.itemAttackerProjectileCanHitWidth = 32.0F;
      this.itemAttackerPredictionDistanceOffset = -20.0F;
      this.canBeUsedForRaids = true;
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      super.setDrawAttackRotation(item, drawOptions, attackDirX, attackDirY, attackProgress);
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "froststafftip"));
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
      int velocity = this.getProjectileVelocity(item, attackerMob);
      int range = this.getAttackRange(item);
      int knockback = this.getKnockback(item, attackerMob);
      Float attackDir = GameMath.normalize(x - attackerMob.x, y - attackerMob.y);
      Float pos1 = GameMath.getPerpendicularPoint(attackerMob.x, attackerMob.y, 10.0F, attackDir);
      int attackRange = this.getAttackRange(item);
      Projectile p1 = new FrostStaffProjectile(
         level, attackerMob, pos1.x, pos1.y, attackDir.x, attackDir.y, velocity / 10.0F, velocity, attackRange, range, this.getAttackDamage(item), knockback
      );
      p1.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
      p1.resetUniqueID(new GameRandom(seed));
      attackerMob.addAndSendAttackerProjectile(p1, 20);
      Float pos2 = GameMath.getPerpendicularPoint(attackerMob.x, attackerMob.y, -10.0F, attackDir);
      Projectile p2 = new FrostStaffProjectile(
         level, attackerMob, pos2.x, pos2.y, attackDir.x, attackDir.y, velocity / 10.0F, velocity, attackRange, range, this.getAttackDamage(item), knockback
      );
      p2.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
      p2.resetUniqueID(new GameRandom(seed));
      attackerMob.addAndSendAttackerProjectile(p2, 20);
      this.consumeMana(attackerMob, item);
      return item;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.jingle).volume(0.6F);
   }
}
