package necesse.inventory.item.toolItem.projectileToolItem;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.Set;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.registries.EnchantmentRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.inventory.enchants.Enchantable;
import necesse.inventory.enchants.ItemEnchantment;
import necesse.inventory.enchants.ToolItemEnchantment;
import necesse.inventory.enchants.ToolItemModifiers;
import necesse.inventory.item.toolItem.ToolItem;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.Level;

public class ProjectileToolItem extends ToolItem {
   protected float itemAttackerProjectileCanHitWidth = 8.0F;
   protected float itemAttackerPredictionDistanceOffset = 0.0F;
   protected IntUpgradeValue velocity = new IntUpgradeValue(50, 0.0F);

   public ProjectileToolItem(int enchantCost, OneOfLootItems lootTableCategory) {
      super(enchantCost, lootTableCategory);
      this.setItemCategory("equipment", "weapons");
      this.knockback.setBaseValue(25);
   }

   public int getFlatVelocity(InventoryItem item) {
      GNDItemMap gndData = item.getGndData();
      return gndData.hasKey("velocity") ? gndData.getInt("velocity") : this.velocity.getValue(this.getUpgradeTier(item));
   }

   @Deprecated
   public int getVelocity(InventoryItem item, Mob mob) {
      return this.getProjectileVelocity(item, mob);
   }

   public int getProjectileVelocity(InventoryItem item, Mob mob) {
      int velocity = this.getFlatVelocity(item);
      return Math.round(
         this.getEnchantment(item).applyModifierLimited(ToolItemModifiers.VELOCITY, ToolItemModifiers.VELOCITY.defaultBuffManagerValue)
            * velocity
            * mob.buffManager.getModifier(BuffModifiers.PROJECTILE_VELOCITY)
      );
   }

   public int getThrowingVelocity(InventoryItem item, Mob mob) {
      int velocity = this.getFlatVelocity(item);
      return Math.round(
         this.getEnchantment(item).applyModifierLimited(ToolItemModifiers.VELOCITY, ToolItemModifiers.VELOCITY.defaultBuffManagerValue)
            * velocity
            * mob.buffManager.getModifier(BuffModifiers.THROWING_VELOCITY)
      );
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.pointRotation(attackDirX, attackDirY);
   }

   @Override
   public boolean animDrawBehindHand(InventoryItem item) {
      return true;
   }

   @Override
   protected SoundSettings getSwingSound() {
      return null;
   }

   @Override
   public Point getItemAttackerAttackPosition(Level level, ItemAttackerMob attackerMob, Mob target, int seed, InventoryItem item) {
      return this.applyInaccuracy(
         attackerMob,
         item,
         this.getPredictedItemAttackerAttackPosition(
            attackerMob, target, this.getProjectileVelocity(item, attackerMob), this.itemAttackerPredictionDistanceOffset
         )
      );
   }

   @Override
   public boolean canItemAttackerHitTarget(ItemAttackerMob attackerMob, float fromX, float fromY, Mob target, InventoryItem item) {
      float width = this.getItemAttackerProjectileCanHitWidth(attackerMob, target, item);
      return this.itemAttackerHasLineOfSightToTarget(attackerMob, fromX, fromY, target, width);
   }

   protected float getItemAttackerProjectileCanHitWidth(ItemAttackerMob attackerMob, Mob target, InventoryItem item) {
      return this.itemAttackerProjectileCanHitWidth;
   }

   @Override
   public GameMessage getItemAttackerCanUseError(ItemAttackerMob mob, InventoryItem item) {
      return null;
   }

   @Override
   public int getItemAttackerAttackRange(ItemAttackerMob mob, InventoryItem item) {
      return this.getAttackRange(item) / 2;
   }

   @Override
   public ToolItemEnchantment getRandomEnchantment(GameRandom random, InventoryItem item) {
      if (this.getDamageType(item) == DamageTypeRegistry.MAGIC) {
         return Enchantable.getRandomEnchantment(random, EnchantmentRegistry.magicItemEnchantments, this.getEnchantmentID(item), ToolItemEnchantment.class);
      } else {
         return this.getDamageType(item) == DamageTypeRegistry.MELEE
            ? Enchantable.getRandomEnchantment(random, EnchantmentRegistry.meleeItemEnchantments, this.getEnchantmentID(item), ToolItemEnchantment.class)
            : Enchantable.getRandomEnchantment(random, EnchantmentRegistry.rangedItemEnchantments, this.getEnchantmentID(item), ToolItemEnchantment.class);
      }
   }

   @Override
   public boolean isValidEnchantment(InventoryItem item, ItemEnchantment enchantment) {
      if (this.getDamageType(item) == DamageTypeRegistry.MAGIC) {
         return EnchantmentRegistry.magicItemEnchantments.contains(enchantment.getID());
      } else {
         return this.getDamageType(item) == DamageTypeRegistry.MELEE
            ? EnchantmentRegistry.meleeItemEnchantments.contains(enchantment.getID())
            : EnchantmentRegistry.rangedItemEnchantments.contains(enchantment.getID());
      }
   }

   @Override
   public Set<Integer> getValidEnchantmentIDs(InventoryItem item) {
      if (this.getDamageType(item) == DamageTypeRegistry.MAGIC) {
         return EnchantmentRegistry.magicItemEnchantments;
      } else {
         return this.getDamageType(item) == DamageTypeRegistry.MELEE ? EnchantmentRegistry.meleeItemEnchantments : EnchantmentRegistry.rangedItemEnchantments;
      }
   }

   protected Point controlledRangePosition(
      GameRandom random, Mob mob, int targetX, int targetY, InventoryItem item, int controlledMinRange, int controlledInaccuracy
   ) {
      return controlledRangePosition(random, mob.getX(), mob.getY(), targetX, targetY, this.getAttackRange(item), controlledMinRange, controlledInaccuracy);
   }

   public static Point controlledRangePosition(
      GameRandom random, int startX, int startY, int targetX, int targetY, int attackRange, int controlledMinRange, int controlledInaccuracy
   ) {
      float fX = targetX;
      float fY = targetY;
      float range = (float)new Point(startX, startY).distance(fX, fY);
      Float norm = GameMath.normalize(fX - startX, fY - startY);
      if (range > attackRange) {
         fX = (int)(startX + norm.x * attackRange);
         fY = (int)(startY + norm.y * attackRange);
      } else if (range < controlledMinRange) {
         fX = (int)(startX + norm.x * controlledMinRange);
         fY = (int)(startY + norm.y * controlledMinRange);
      }

      float prcRange = (float)new Point(startX, startY).distance(fX, fY) / attackRange;
      if (controlledInaccuracy > 0) {
         fX += (random.nextFloat() * 2.0F - 1.0F) * controlledInaccuracy * prcRange;
         fY += (random.nextFloat() * 2.0F - 1.0F) * controlledInaccuracy * prcRange;
      }

      return new Point((int)fX, (int)fY);
   }
}
