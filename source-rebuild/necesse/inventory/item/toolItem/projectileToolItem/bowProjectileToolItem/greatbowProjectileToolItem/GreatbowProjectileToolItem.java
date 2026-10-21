package necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem;

import java.awt.Color;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.attackHandler.GreatbowAttackHandler;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.Projectile;
import necesse.gfx.GameResources;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.arrowItem.ArrowItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.BowProjectileToolItem;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.level.maps.Level;

public class GreatbowProjectileToolItem extends BowProjectileToolItem {
   public Color particleColor = new Color(255, 219, 36);

   public GreatbowProjectileToolItem(int enchantCost, OneOfLootItems lootTableCategory) {
      super(enchantCost, lootTableCategory);
      this.knockback.setBaseValue(100);
      this.raiderDamageModifier = 0.5F;
      this.tierOneEssencesUpgradeRequirement = "bioessence";
      this.tierTwoEssencesUpgradeRequirement = "slimeessence";
      this.tierThreeEssenceUpgradeRequirement = "crystalessence";
   }

   @Override
   protected void addExtraBowTooltips(ListGameTooltips tooltips, InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      super.addExtraBowTooltips(tooltips, item, perspective, blackboard);
      tooltips.add(Localization.translate("itemtooltip", "greatbowtip"));
   }

   @Override
   public void showAttack(
      Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, int animAttack, int seed, GNDItemMap mapContent
   ) {
      if (level.isClient()) {
         if (!item.getGndData().getBoolean("charging")) {
            if (item.getGndData().getBoolean("charged")) {
               float chargePerc = item.getGndData().getFloat("chargePercent");
               SoundSettings sound = this.getGreatbowShootSound(chargePerc);
               sound.setPitchVarianceIfNotSet(0.04F);
               SoundManager.playSound(sound, attackerMob);
            }
         }
      }
   }

   @Override
   public boolean getConstantUse(InventoryItem item) {
      return false;
   }

   @Override
   public Projectile getProjectile(
      Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item, int seed, ArrowItem arrow, boolean consumeAmmo, GNDItemMap mapContent
   ) {
      float percentCharge = item.getGndData().getFloat("chargePercent");
      percentCharge = GameMath.limit(percentCharge, 0.0F, 1.0F);
      float velocityMod;
      float rangeMod;
      float damageMod;
      float knockbackMod;
      float resilienceGainMod;
      if (percentCharge >= 1.0F) {
         velocityMod = 1.0F;
         rangeMod = 1.0F;
         damageMod = 1.0F;
         knockbackMod = 1.0F;
         resilienceGainMod = 1.0F;
      } else {
         velocityMod = GameMath.lerp(percentCharge, 0.1F, 0.4F);
         rangeMod = GameMath.lerp(percentCharge, 0.05F, 0.4F);
         damageMod = GameMath.lerp(percentCharge, 0.05F, 0.4F);
         knockbackMod = GameMath.lerp(percentCharge, 0.05F, 0.2F);
         resilienceGainMod = GameMath.lerp(percentCharge, 0.0F, 0.5F);
      }

      GameDamage damage = arrow.modDamage(this.getAttackDamage(item)).modDamage(damageMod);
      float velocity = arrow.modVelocity(this.getProjectileVelocity(item, attackerMob)) * velocityMod;
      float range = arrow.modRange(this.getAttackRange(item)) * rangeMod;
      float knockback = arrow.modKnockback(this.getKnockback(item, attackerMob)) * knockbackMod;
      float resilienceGain = this.getResilienceGain(item) * resilienceGainMod;
      return this.getProjectile(
         level, x, y, attackerMob, item, seed, arrow, consumeAmmo, velocity, (int)range, damage, (int)knockback, resilienceGain, mapContent
      );
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
      int animTime = this.getAttackAnimTime(item, attackerMob);
      attackerMob.startAttackHandler(new GreatbowAttackHandler(attackerMob, slot, item, this, animTime, this.particleColor, seed, x, y));
      return item;
   }

   public InventoryItem superOnAttack(
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
      return super.onAttack(level, x, y, attackerMob, attackHeight, item, slot, animAttack, seed, mapContent);
   }

   @Override
   public boolean shouldRunOnAttackedBuffEvent(
      Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item, ItemAttackSlot slot, int animAttack, int seed, GNDItemMap mapContent
   ) {
      return false;
   }

   @Override
   public String getTranslatedTypeName() {
      return Localization.translate("item", "greatbow");
   }

   private SoundSettings getGreatbowShootSound(float chargePercent) {
      return chargePercent >= 1.0F ? this.getGreatbowShootSoundStrong() : this.getGreatbowShootSoundWeak();
   }

   protected SoundSettings getGreatbowShootSoundWeak() {
      return new SoundSettings(GameResources.regularGreatBowWeak).volume(0.8F);
   }

   protected SoundSettings getGreatbowShootSoundStrong() {
      return new SoundSettings(GameResources.regularGreatBowStrong).volume(0.8F);
   }
}
