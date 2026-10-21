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
import necesse.entity.projectile.IcicleStaffProjectile;
import necesse.entity.projectile.modifiers.ResilienceOnHitProjectileModifier;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.presets.MagicWeaponsLootTable;
import necesse.level.maps.Level;

public class IcicleStaffProjectileToolItem extends MagicProjectileToolItem {
   public IcicleStaffProjectileToolItem() {
      super(1450, MagicWeaponsLootTable.magicWeapons);
      this.rarity = Item.Rarity.RARE;
      this.attackAnimTime.setBaseValue(300);
      this.attackDamage.setBaseValue(62.0F).setUpgradedValue(1.0F, 112.00004F);
      this.velocity.setBaseValue(150);
      this.attackXOffset = 14;
      this.attackYOffset = 4;
      this.attackRange.setBaseValue(900);
      this.knockback.setBaseValue(50);
      this.manaCost.setBaseValue(2.75F).setUpgradedValue(1.0F, 2.75F);
      this.itemAttackerProjectileCanHitWidth = 5.0F;
      this.canBeUsedForRaids = true;
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.pointRotation(attackDirX, attackDirY).forEachItemSprite(i -> i.itemRotateOffset(45.0F));
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getPreEnchantmentTooltips(item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "iciclestafftip"));
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
      GameRandom random = new GameRandom(seed);
      Float dir = GameMath.normalize(attackerMob.x - x, attackerMob.y - y);
      int offsetDistance = random.getIntBetween(30, 50);
      Float offset = new Float(dir.x * offsetDistance, dir.y * offsetDistance);
      offset = GameMath.getPerpendicularPoint(offset, random.getIntBetween(-50, 50), dir);
      IcicleStaffProjectile projectile = new IcicleStaffProjectile(
         level,
         attackerMob,
         attackerMob.x + offset.x,
         attackerMob.y + offset.y,
         x,
         y,
         this.getProjectileVelocity(item, attackerMob),
         this.getAttackRange(item),
         this.getAttackDamage(item),
         this.getKnockback(item, attackerMob)
      );
      projectile.setModifier(new ResilienceOnHitProjectileModifier(this.getResilienceGain(item)));
      projectile.resetUniqueID(random);
      attackerMob.addAndSendAttackerProjectile(projectile);
      this.consumeMana(attackerMob, item);
      return item;
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.jingle).volume(0.4F);
   }
}
