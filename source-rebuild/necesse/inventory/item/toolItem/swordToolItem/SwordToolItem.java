package necesse.inventory.item.toolItem.swordToolItem;

import java.awt.Shape;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Function;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.registries.EnchantmentRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LineHitbox;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ToolItemMobAbilityEvent;
import necesse.entity.mobs.AttackAnimMob;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.gfx.drawOptions.itemAttack.ItemAttackDrawOptions;
import necesse.inventory.InventoryItem;
import necesse.inventory.enchants.Enchantable;
import necesse.inventory.enchants.ItemEnchantment;
import necesse.inventory.enchants.ToolItemEnchantment;
import necesse.inventory.item.ItemCategory;
import necesse.inventory.item.ItemStatTipList;
import necesse.inventory.item.toolItem.ToolItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.Level;

public class SwordToolItem extends ToolItem {
   public SwordToolItem(int enchantCost, OneOfLootItems lootTableCategory) {
      super(enchantCost, lootTableCategory);
      this.setItemCategory("equipment", "weapons", "meleeweapons");
      this.setItemCategory(ItemCategory.equipmentManager, "weapons", "meleeweapons");
      this.setItemCategory(ItemCategory.craftingManager, "equipment", "weapons", "meleeweapons");
      this.keyWords.add("sword");
      this.damageType = DamageTypeRegistry.MELEE;
      this.width = 15.0F;
      this.showAttackAllDirections = true;
      this.resilienceGain.setBaseValue(3.0F).setUpgradedValue(1.0F, 3.0F).setUpgradedValue(10.0F, 4.0F);
      this.tierOneEssencesUpgradeRequirement = "shadowessence";
      this.tierTwoEssencesUpgradeRequirement = "spideressence";
      this.tierThreeEssenceUpgradeRequirement = "radiatedessence";
   }

   @Override
   public boolean animDrawBehindHand(InventoryItem item) {
      return true;
   }

   @Override
   public void addStatTooltips(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem, ItemAttackerMob perspective, boolean forceAdd) {
      this.addAttackDamageTip(list, currentItem, lastItem, perspective, forceAdd);
      this.addAttackSpeedTip(list, currentItem, lastItem, perspective);
      this.addResilienceGainTip(list, currentItem, lastItem, perspective, forceAdd);
      this.addKnockbackTip(list, currentItem, lastItem, perspective);
      this.addCritChanceTip(list, currentItem, lastItem, perspective, forceAdd);
   }

   @Override
   public void setDrawAttackRotation(InventoryItem item, ItemAttackDrawOptions drawOptions, float attackDirX, float attackDirY, float attackProgress) {
      drawOptions.rotation(this.getSwingRotation(item, drawOptions.dir, attackProgress) - 90.0F);
   }

   public float getSwingRotation(InventoryItem item, int dir, float attackProgress) {
      if (item.getGndData().getBoolean("charging")) {
         float chargePercent = item.getGndData().getFloat("chargePercent");
         chargePercent = GameMath.limit(chargePercent, 0.0F, 1.0F);
         attackProgress = 0.2F - chargePercent * 0.2F;
      }

      return this.getAnimInverted(item) ? this.getSwingRotationInverted(item, dir, attackProgress) : this.getSwingRotationNormal(item, dir, attackProgress);
   }

   public float getSwingRotationNormal(InventoryItem item, int dir, float attackProgress) {
      float angle = this.getSwingRotationAngle(item, dir);
      float offset = this.getSwingRotationOffset(item, dir, angle);
      return ItemAttackDrawOptions.getSwingRotation(attackProgress, angle, offset);
   }

   public float getSwingRotationInverted(InventoryItem item, int dir, float attackProgress) {
      float angle = this.getSwingRotationAngle(item, dir);
      float offset = this.getSwingRotationOffset(item, dir, angle);
      return ItemAttackDrawOptions.getSwingRotationInv(attackProgress, angle, offset);
   }

   public float getSwingRotationAngle(InventoryItem item, int dir) {
      return 150.0F;
   }

   public float getSwingRotationOffset(InventoryItem item, int dir, float swingAngle) {
      if (dir == 1) {
         return (swingAngle - 90.0F) / 2.0F;
      } else {
         return dir == 3 ? (swingAngle - 90.0F) / 2.0F : 0.0F;
      }
   }

   public float getHitboxSwingAngle(InventoryItem item, int dir) {
      return 150.0F;
   }

   public float getHitboxSwingAngleOffset(InventoryItem item, int dir, float swingAngle) {
      return 0.0F;
   }

   public Function<Float, Float> getSwingDirection(InventoryItem item, AttackAnimMob mob) {
      int attackDir = mob.getDir();
      float animSwingAngle = this.getHitboxSwingAngle(item, attackDir);
      float animSwingAngleOffset = this.getHitboxSwingAngleOffset(item, attackDir, animSwingAngle);
      Function<Float, Float> angleGetter;
      if (attackDir == 0) {
         if (this.getAnimInverted(item)) {
            angleGetter = progress -> -progress * animSwingAngle - animSwingAngleOffset;
         } else {
            angleGetter = progress -> 180.0F + progress * animSwingAngle + animSwingAngleOffset;
         }
      } else if (attackDir == 1) {
         if (this.getAnimInverted(item)) {
            angleGetter = progress -> 90.0F - progress * animSwingAngle - animSwingAngleOffset;
         } else {
            angleGetter = progress -> 270.0F + progress * animSwingAngle + animSwingAngleOffset;
         }
      } else if (attackDir == 2) {
         if (this.getAnimInverted(item)) {
            angleGetter = progress -> 180.0F - progress * animSwingAngle - animSwingAngleOffset;
         } else {
            angleGetter = progress -> progress * animSwingAngle + animSwingAngleOffset;
         }
      } else if (this.getAnimInverted(item)) {
         angleGetter = progress -> 90.0F + progress * animSwingAngle + animSwingAngleOffset;
      } else {
         angleGetter = progress -> 270.0F - progress * animSwingAngle - animSwingAngleOffset;
      }

      return angleGetter;
   }

   @Override
   public ArrayList<Shape> getHitboxes(InventoryItem item, AttackAnimMob mob, int aimX, int aimY, ToolItemMobAbilityEvent event, boolean forDebug) {
      ArrayList<Shape> out = new ArrayList<>();
      int attackRange = this.getAttackRange(item);
      float lastProgress = event.lastHitboxProgress;
      float nextProgress = mob.getAttackAnimProgress();
      float circumference = (float)(Math.PI * attackRange);
      float percPerWidth = Math.max(10.0F, this.width) / circumference;
      java.awt.geom.Point2D.Float base = new java.awt.geom.Point2D.Float(mob.x, mob.y);
      int attackDir = mob.getDir();
      if (attackDir == 0) {
         base.x += 8.0F;
      } else if (attackDir == 2) {
         base.x -= 8.0F;
      }

      for (float progress = lastProgress; progress <= nextProgress; progress += percPerWidth) {
         float angle = this.getSwingDirection(item, mob).apply(progress);
         java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(angle);
         java.awt.geom.Line2D.Float attackLine = new java.awt.geom.Line2D.Float(base.x, base.y, dir.x * attackRange + mob.x, dir.y * attackRange + mob.y);
         if (this.width > 0.0F) {
            out.add(new LineHitbox(attackLine, this.width));
         } else {
            out.add(attackLine);
         }

         if (!forDebug) {
            event.lastHitboxProgress = progress;
         }
      }

      return out;
   }

   @Override
   public void showAttack(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int animAttack, int seed, GNDItemMap mapContent
   ) {
      if (!item.getGndData().getBoolean("charging")) {
         super.showAttack(level, x, y, attackerMob, attackHeight, item, animAttack, seed, mapContent);
      }
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.regularSwords).volume(0.1F);
   }

   @Override
   public ToolItemEnchantment getRandomEnchantment(GameRandom random, InventoryItem item) {
      return Enchantable.getRandomEnchantment(random, EnchantmentRegistry.meleeItemEnchantments, this.getEnchantmentID(item), ToolItemEnchantment.class);
   }

   @Override
   public boolean isValidEnchantment(InventoryItem item, ItemEnchantment enchantment) {
      return EnchantmentRegistry.meleeItemEnchantments.contains(enchantment.getID());
   }

   @Override
   public Set<Integer> getValidEnchantmentIDs(InventoryItem item) {
      return EnchantmentRegistry.meleeItemEnchantments;
   }

   @Override
   public String getTranslatedTypeName() {
      return Localization.translate("item", "sword");
   }
}
