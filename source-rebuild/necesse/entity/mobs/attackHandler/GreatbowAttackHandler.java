package necesse.entity.mobs.attackHandler;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemAttackerWeaponItem;
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.greatbowProjectileToolItem.GreatbowProjectileToolItem;

public class GreatbowAttackHandler extends MousePositionAttackHandler {
   protected SoundPlayer drawBowSoundPlayer;
   public int chargeTime;
   public boolean fullyCharged;
   public GreatbowProjectileToolItem toolItem;
   public long startTime;
   public InventoryItem item;
   public int seed;
   public Color particleColors;
   public boolean endedByInteract;
   protected int endAttackBuffer;

   public GreatbowAttackHandler(
      ItemAttackerMob attackerMob,
      ItemAttackSlot slot,
      InventoryItem item,
      GreatbowProjectileToolItem toolItem,
      int chargeTime,
      Color particleColors,
      int seed,
      int startTargetX,
      int startTargetY
   ) {
      super(attackerMob, slot, 20, startTargetX, startTargetY);
      this.item = item;
      this.toolItem = toolItem;
      this.chargeTime = chargeTime;
      this.particleColors = particleColors;
      this.seed = seed;
      this.startTime = attackerMob.getWorldEntity().getLocalTime();
      if (attackerMob.isClient()) {
         this.drawBowSoundPlayer = SoundManager.playSound(
            GameResources.greatbowCharge, SoundEffect.effect(attackerMob).volume(0.2F).pitch(GameRandom.globalRandom.getFloatBetween(0.75F, 0.78F))
         );
      }
   }

   public long getTimeSinceStart() {
      return this.attackerMob.getWorldEntity().getLocalTime() - this.startTime;
   }

   public float getChargePercent() {
      return (float)this.getTimeSinceStart() / this.chargeTime;
   }

   @Override
   public Point getNextItemAttackerLevelPos(Mob currentTarget) {
      InventoryItem attackItem = this.item.copy();
      attackItem.getGndData().setFloat("skillPercent", 1.0F);
      return ((ItemAttackerWeaponItem)attackItem.item)
         .getItemAttackerAttackPosition(this.attackerMob.getLevel(), this.attackerMob, currentTarget, -1, attackItem);
   }

   @Override
   public void onUpdate() {
      super.onUpdate();
      Float dir = GameMath.normalize(this.lastX - this.attackerMob.x, this.lastY - this.attackerMob.y);
      float chargePercent = this.getChargePercent();
      InventoryItem showItem = this.item.copy();
      showItem.getGndData().setBoolean("charging", true);
      showItem.getGndData().setFloat("chargePercent", chargePercent);
      this.attackerMob.showAttackAndSendAttacker(showItem, this.lastX, this.lastY, 0, this.seed);
      if (chargePercent >= 1.0F) {
         if (!this.attackerMob.isPlayer) {
            this.endAttackBuffer = this.endAttackBuffer + this.updateInterval;
            if (this.endAttackBuffer >= 350) {
               this.endAttackBuffer = 0;
               this.attackerMob.endAttackHandler(true);
               return;
            }
         }

         if (this.attackerMob.isClient()) {
            this.attackerMob
               .getLevel()
               .entityManager
               .addParticle(
                  this.attackerMob.x + dir.x * 16.0F + (float)(GameRandom.globalRandom.nextGaussian() * 6.0),
                  this.attackerMob.y + 4.0F + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
                  Particle.GType.IMPORTANT_COSMETIC
               )
               .movesConstant(this.attackerMob.dx / 10.0F, this.attackerMob.dy / 10.0F)
               .color(this.particleColors)
               .height(20.0F - dir.y * 16.0F);
         }

         if (!this.fullyCharged) {
            this.fullyCharged = true;
            if (this.attackerMob.isClient()) {
               int particles = 35;
               float anglePerParticle = 360.0F / particles;
               ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);

               for (int i = 0; i < particles; i++) {
                  int angle = (int)(i * anglePerParticle + GameRandom.globalRandom.nextFloat() * anglePerParticle);
                  float dx = (float)Math.sin(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(30, 50);
                  float dy = (float)Math.cos(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(30, 50) * 0.8F;
                  this.attackerMob
                     .getLevel()
                     .entityManager
                     .addParticle(this.attackerMob, typeSwitcher.next())
                     .movesFriction(dx, dy, 0.8F)
                     .color(this.particleColors)
                     .heightMoves(0.0F, 30.0F)
                     .lifeTime(500);
               }

               if (this.drawBowSoundPlayer != null) {
                  this.drawBowSoundPlayer.stop();
               }

               SoundManager.playSound(GameResources.greatbowChargeComplete, SoundEffect.effect(this.attackerMob).volume(0.2F).pitch(1.1F));
            }
         }
      }
   }

   @Override
   public void onMouseInteracted(int levelX, int levelY) {
      this.endedByInteract = true;
      this.attackerMob.endAttackHandler(false);
   }

   @Override
   public void onControllerInteracted(float aimX, float aimY) {
      this.endedByInteract = true;
      this.attackerMob.endAttackHandler(false);
   }

   @Override
   public void onEndAttack(boolean bySelf) {
      float chargePercent = this.getChargePercent();
      if (this.drawBowSoundPlayer != null) {
         this.drawBowSoundPlayer.stop();
      }

      if (!this.endedByInteract && (this.getTimeSinceStart() >= 200L || chargePercent >= 0.5F)) {
         if (this.attackerMob.isPlayer) {
            ((PlayerMob)this.attackerMob).constantAttack = true;
         }

         InventoryItem attackItem = this.item.copy();
         attackItem.getGndData().setFloat("chargePercent", chargePercent);
         attackItem.getGndData().setBoolean("charged", true);
         if (!this.attackerMob.isPlayer && this.lastItemAttackerTarget != null) {
            Point attackPos = ((ItemAttackerWeaponItem)attackItem.item)
               .getItemAttackerAttackPosition(this.attackerMob.getLevel(), this.attackerMob, this.lastItemAttackerTarget, -1, attackItem);
            this.lastX = attackPos.x;
            this.lastY = attackPos.y;
         }

         GNDItemMap attackMap = this.attackerMob.showAttackAndSendAttacker(attackItem, this.lastX, this.lastY, 0, this.seed);
         this.toolItem
            .superOnAttack(
               this.attackerMob.getLevel(),
               this.lastX,
               this.lastY,
               this.attackerMob,
               this.attackerMob.getCurrentAttackHeight(),
               attackItem,
               this.slot,
               0,
               this.seed,
               attackMap
            );

         for (ActiveBuff b : this.attackerMob.buffManager.getArrayBuffs()) {
            b.onItemAttacked(this.lastX, this.lastY, this.attackerMob, this.attackerMob.getCurrentAttackHeight(), attackItem, this.slot, 0, attackMap);
         }
      }

      this.attackerMob.doAndSendStopAttackAttacker(false);
   }
}
