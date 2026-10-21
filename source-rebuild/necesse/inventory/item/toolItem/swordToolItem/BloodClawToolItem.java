package necesse.inventory.item.toolItem.swordToolItem;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.LinkedList;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LineHitbox;
import necesse.entity.levelEvent.SwordCleanSliceAttackMobEvent;
import necesse.entity.levelEvent.mobAbilityLevelEvent.ToolItemMobAbilityEvent;
import necesse.entity.mobs.AttackAnimMob;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.projectile.BloodClawProjectile;
import necesse.entity.projectile.Projectile;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.enchants.ToolItemModifiers;
import necesse.inventory.item.Item;
import necesse.inventory.item.ItemControllerInteract;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.lootTable.presets.IncursionCloseRangeWeaponsLootTable;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.incursion.IncursionData;

public class BloodClawToolItem extends SwordToolItem implements ItemInteractAction {
   public BloodClawToolItem() {
      super(1900, IncursionCloseRangeWeaponsLootTable.incursionCloseRangeWeapons);
      this.rarity = Item.Rarity.EPIC;
      this.attackAnimTime.setBaseValue(180);
      this.attackDamage.setBaseValue(45.0F).setUpgradedValue(1.0F, 58.33335F);
      this.attackRange.setBaseValue(100);
      this.knockback.setBaseValue(40);
      this.attackXOffset = 4;
      this.attackYOffset = 4;
      this.resilienceGain.setBaseValue(1.25F).setUpgradedValue(1.0F, 1.25F).setUpgradedValue(10.0F, 1.5F);
      this.canBeUsedForRaids = true;
      this.useForRaidsOnlyIfObtained = true;
      this.minRaidTier = 1;
      this.maxRaidTier = IncursionData.ITEM_TIER_UPGRADE_CAP;
      this.raidTicketsModifier = 0.5F;
   }

   @Override
   public ListGameTooltips getPreEnchantmentTooltips(InventoryItem var1, PlayerMob var2, GameBlackboard var3) {
      ListGameTooltips var4 = super.getPreEnchantmentTooltips(var1, var2, var3);
      var4.add(Localization.translate("itemtooltip", "bloodclawtip"), 400);
      var4.add(Localization.translate("itemtooltip", "bloodclawsecondarytip"), 400);
      return var4;
   }

   public void showBloodClawAttack(Level var1, AttackAnimMob var2, int var3, final InventoryItem var4) {
      GameRandom var5 = new GameRandom(var3);
      int var6 = var5.getIntBetween(30, 45);
      int var7 = var5.getIntBetween(30, 45);
      boolean var8 = var5.getEveryXthChance(2);
      boolean var9 = var5.getEveryXthChance(2);
      if (var8) {
         var6 = -var6;
      }

      if (var9) {
         var7 = -var7;
      }

      final int var10 = var7;
      final int var11 = var6;
      final Float var12 = GameMath.normalize(-var6, -var7);
      final double var13 = Math.sqrt(var6 * var6 + var7 * var7) * 2.0;
      var1.entityManager.events.addHidden(new SwordCleanSliceAttackMobEvent(var2, var3, 12) {
         Trail trail = null;
         final float maxSliceThickness = 20.0F;
         float thickness;
         final float mobStartX = this.entity.x;
         final float mobStartY = this.entity.y;

         @Override
         public void tick(float var1, float var2x) {
            if (var2x < 0.33F) {
               this.thickness = 60.0F * var2x;
            } else if (var2x > 0.66F) {
               this.thickness = 60.0F * (1.0F - var2x);
            } else {
               this.thickness = 20.0F;
            }

            int var3 = BloodClawToolItem.this.getAttackRange(var4);
            Float var4x = GameMath.getAngleDir(var1);
            float var5 = this.mobStartX + var4x.x * (var3 * 0.7F) + this.entity.getCurrentAttackDrawXOffset();
            var5 = (float)(var5 + (var11 + var12.x * var2x * var13));
            float var6 = this.mobStartY + var4x.y * (var3 * 0.7F) + this.entity.getCurrentAttackDrawYOffset();
            var6 = (float)(var6 + (var10 + var12.y * var2x * var13));
            if (this.trail == null) {
               this.trail = new Trail(new TrailVector(var5, var6, var12.x, var12.y, this.thickness, 0.0F), this.level, new Color(164, 0, 0), 500);
               this.trail.removeOnFadeOut = false;
               this.trail.sprite = new GameSprite(GameResources.chains, 7, 0, 32);
               this.level.entityManager.addTrail(this.trail);
            } else {
               this.trail.addPoint(new TrailVector(var5, var6, var12.x, var12.y, this.thickness, 0.0F));
            }
         }

         @Override
         public void onDispose() {
            super.onDispose();
            if (this.trail != null) {
               this.trail.removeOnFadeOut = true;
            }
         }
      });
   }

   @Override
   public void showAttack(Level var1, int var2, int var3, ItemAttackerMob var4, int var5, InventoryItem var6, int var7, int var8, GNDItemMap var9) {
      super.showAttack(var1, var2, var3, var4, var5, var6, var7, var8, var9);
      if (var1.isClient()) {
         this.showBloodClawAttack(var1, var4, var8, var6);
      }
   }

   @Override
   public void hitMob(InventoryItem var1, ToolItemMobAbilityEvent var2, Level var3, Mob var4, Mob var5) {
      super.hitMob(var1, var2, var3, var4, var5);
      if (var5.isServer()) {
         ActiveBuff var6 = new ActiveBuff(BuffRegistry.BLOOD_CLAW_STACKS_BUFF, var5, 10.0F, var5);
         var5.addBuff(var6, true);
      }
   }

   @Override
   public boolean canItemAttackerHitTarget(ItemAttackerMob var1, float var2, float var3, Mob var4, InventoryItem var5) {
      float var6 = var1.getDistance(var4);
      int var7 = this.getAttackRange(var5);
      if (var6 < var7) {
         return super.canItemAttackerHitTarget(var1, var2, var3, var4, var5);
      } else {
         return var6 < var7 * 5
            ? !var1.getLevel()
               .collides(
                  new LineHitbox(var2, var3, var4.x, var4.y, 45.0F), var1.modifyChasingCollisionFilter(new CollisionFilter().projectileCollision(), var4)
               )
            : false;
      }
   }

   @Override
   public boolean canLevelInteract(Level var1, int var2, int var3, ItemAttackerMob var4, InventoryItem var5) {
      return var4.buffManager.hasBuff(BuffRegistry.BLOOD_CLAW_STACKS_BUFF);
   }

   @Override
   public InventoryItem onLevelInteract(
      Level var1, int var2, int var3, ItemAttackerMob var4, int var5, InventoryItem var6, ItemAttackSlot var7, int var8, GNDItemMap var9
   ) {
      if (var4.buffManager.hasBuff(BuffRegistry.BLOOD_CLAW_STACKS_BUFF)) {
         int var10 = var4.buffManager.getStacks(BuffRegistry.BLOOD_CLAW_STACKS_BUFF);
         this.consumeLife(var10, var4, var6);

         for (int var11 = 0; var11 < var10; var11++) {
            float var12 = 2.5F;
            float var13 = 300.0F;
            float var14 = Math.round(
               this.getEnchantment(var6).applyModifierLimited(ToolItemModifiers.VELOCITY, ToolItemModifiers.VELOCITY.defaultBuffManagerValue)
                  * var13
                  * var4.buffManager.getModifier(BuffModifiers.PROJECTILE_VELOCITY)
            );
            GameRandom var15 = new GameRandom(var8);
            GameDamage var16 = this.getAttackDamage(var6).modFinalMultiplier(0.75F);
            Projectile var17 = new BloodClawProjectile(var1, var4.x, var4.y, var2, var3, var14, (int)(this.getAttackRange(var6) * var12), var16, var4);
            var17.getUniqueID(var15);
            var4.addAndSendAttackerProjectile(var17, 20, var15.getIntBetween(-30, 30));
         }

         var4.buffManager.removeBuff(BuffRegistry.BLOOD_CLAW_STACKS_BUFF, var1.isServer());
         if (var1.isClient()) {
            SoundManager.playSound(GameResources.bloodClawShoot, SoundEffect.effect(var4).volume(0.8F));
         }
      }

      return var6;
   }

   @Override
   public ItemControllerInteract getControllerInteract(
      Level var1, PlayerMob var2, InventoryItem var3, boolean var4, int var5, LinkedList<Rectangle> var6, LinkedList<Rectangle> var7
   ) {
      Float var8 = var2.getControllerAimDir();
      Point var9 = this.getControllerAttackLevelPos(var1, var8.x, var8.y, var2, var3);
      return new ItemControllerInteract(var9.x, var9.y) {
         @Override
         public DrawOptions getDrawOptions(GameCamera var1) {
            return null;
         }

         @Override
         public void onCurrentlyFocused(GameCamera var1) {
         }
      };
   }

   @Override
   protected SoundSettings getAttackSound() {
      return new SoundSettings(GameResources.bloowClawSwing).volume(0.2F);
   }
}
