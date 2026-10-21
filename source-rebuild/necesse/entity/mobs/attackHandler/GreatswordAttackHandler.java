package necesse.entity.mobs.attackHandler;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.Arrays;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.particle.Particle;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemAttackerWeaponItem;
import necesse.inventory.item.toolItem.swordToolItem.greatswordToolItem.GreatswordToolItem;

public class GreatswordAttackHandler extends MouseAngleAttackHandler {
   public GreatswordToolItem toolItem;
   public InventoryItem item;
   public int seed;
   public long startTime;
   public GreatswordChargeLevel[] chargeLevels;
   public int chargeTimeRemaining;
   public int currentChargeLevel;
   public int timeSpentUpToCurrentChargeLevel;
   public boolean endedByInteract;
   protected int endAttackBuffer;

   public GreatswordAttackHandler(
      ItemAttackerMob attackerMob,
      ItemAttackSlot slot,
      InventoryItem item,
      GreatswordToolItem toolItem,
      int seed,
      int startX,
      int startY,
      GreatswordChargeLevel... chargeLevels
   ) {
      super(attackerMob, slot, 50, 1000.0F, startX, startY);
      this.toolItem = toolItem;
      this.item = item;
      this.seed = seed;
      this.chargeLevels = chargeLevels;
      this.currentChargeLevel = -1;
      this.chargeTimeRemaining = Arrays.stream(chargeLevels).mapToInt(l -> l.timeToCharge).sum();
      this.timeSpentUpToCurrentChargeLevel = 0;
      if (chargeLevels.length == 0) {
         throw new IllegalArgumentException("Must have at least one charge level for greatswords");
      }

      this.startTime = attackerMob.getLevel().getWorldEntity().getLocalTime();
   }

   public long getTimeSinceStart() {
      return this.attackerMob.getWorldEntity().getLocalTime() - this.startTime;
   }

   public void updateCurrentChargeLevel() {
      while (this.currentChargeLevel < this.chargeLevels.length - 1) {
         long timeSinceStart = this.getTimeSinceStart();
         long timeSpentOnCurrent = timeSinceStart - this.timeSpentUpToCurrentChargeLevel;
         GreatswordChargeLevel nextLevel = this.chargeLevels[this.currentChargeLevel + 1];
         long timeToChargeNextLevel = Math.round(nextLevel.timeToCharge * (1.0F / this.toolItem.getAttackSpeedModifier(this.item, this.attackerMob)));
         if (timeSpentOnCurrent >= timeToChargeNextLevel) {
            this.timeSpentUpToCurrentChargeLevel = (int)(this.timeSpentUpToCurrentChargeLevel + timeToChargeNextLevel);
            this.chargeTimeRemaining = this.chargeTimeRemaining - nextLevel.timeToCharge;
            this.currentChargeLevel++;
            nextLevel.onReachedLevel(this);
            continue;
         }
         break;
      }
   }

   public float getChargePercent() {
      int chargeTime = this.timeSpentUpToCurrentChargeLevel
         + Math.round(this.chargeTimeRemaining * (1.0F / this.toolItem.getAttackSpeedModifier(this.item, this.attackerMob)));
      return (float)Math.min(this.getTimeSinceStart(), chargeTime) / chargeTime;
   }

   @Override
   public int getNextItemAttackerAngle(Mob currentTarget) {
      InventoryItem attackItem = this.item.copy();
      attackItem.getGndData().setFloat("skillPercent", 1.0F);
      Point attackPos = ((ItemAttackerWeaponItem)attackItem.item)
         .getItemAttackerAttackPosition(this.attackerMob.getLevel(), this.attackerMob, currentTarget, -1, attackItem);
      return (int)GameMath.fixAngle(GameMath.getAngle(new Float(attackPos.x - this.attackerMob.x, attackPos.y - this.attackerMob.y)));
   }

   @Override
   public void onUpdate() {
      super.onUpdate();
      this.updateCurrentChargeLevel();
      Float dir = GameMath.getAngleDir(this.currentAngle);
      int attackX = this.attackerMob.getX() + (int)(dir.x * 100.0F);
      int attackY = this.attackerMob.getY() + (int)(dir.y * 100.0F);
      float chargePercent = this.getChargePercent();
      if (!this.attackerMob.isPlayer && this.currentChargeLevel >= this.chargeLevels.length - 1) {
         this.endAttackBuffer = this.endAttackBuffer + this.updateInterval;
         if (this.endAttackBuffer >= 250) {
            this.endAttackBuffer = 0;
            this.attackerMob.endAttackHandler(true);
            return;
         }
      }

      InventoryItem showItem = this.item.copy();
      showItem.getGndData().setFloat("chargePercent", chargePercent);
      showItem.getGndData().setBoolean("charging", true);
      this.attackerMob.showAttackAndSendAttacker(showItem, attackX, attackY, 0, this.seed);
      if (this.currentChargeLevel >= 0) {
         this.chargeLevels[this.currentChargeLevel].updateAtLevel(this, showItem);
      }
   }

   public void drawWeaponParticles(InventoryItem showItem, Color color) {
      float chargePercent = showItem.getGndData().getFloat("chargePercent");
      showItem.getGndData().setBoolean("charging", true);
      float angle = this.toolItem.getSwingRotation(showItem, this.attackerMob.getDir(), chargePercent);
      int attackDir = this.attackerMob.getDir();
      int offsetX = 0;
      int offsetY = 0;
      if (attackDir == 0) {
         angle = -angle - 90.0F;
         offsetY = -8;
      } else if (attackDir == 1) {
         angle = -angle + 180.0F + 45.0F;
         offsetX = 8;
      } else if (attackDir == 2) {
         angle = -angle + 90.0F;
         offsetY = 12;
      } else {
         angle = angle + 90.0F + 45.0F;
         offsetX = -8;
      }

      float dx = GameMath.sin(angle);
      float dy = GameMath.cos(angle);
      int range = GameRandom.globalRandom.getIntBetween(0, this.toolItem.getAttackRange(this.item));
      this.attackerMob
         .getLevel()
         .entityManager
         .addParticle(
            this.attackerMob.x + offsetX + dx * range + GameRandom.globalRandom.floatGaussian() * 3.0F,
            this.attackerMob.y + 4.0F + GameRandom.globalRandom.floatGaussian() * 4.0F,
            Particle.GType.IMPORTANT_COSMETIC
         )
         .movesConstant(this.attackerMob.dx, this.attackerMob.dy)
         .color(color)
         .height(20.0F - dy * range - offsetY);
   }

   public void drawParticleExplosion(int particleCount, Color color, int minForce, int maxForce) {
      ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);
      float anglePerParticle = 360.0F / particleCount;

      for (int i = 0; i < particleCount; i++) {
         int angle = (int)(i * anglePerParticle + GameRandom.globalRandom.nextFloat() * anglePerParticle);
         float dx = (float)Math.sin(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(minForce, maxForce);
         float dy = (float)Math.cos(Math.toRadians(angle)) * GameRandom.globalRandom.getIntBetween(minForce, maxForce) * 0.8F;
         this.attackerMob
            .getLevel()
            .entityManager
            .addParticle(this.attackerMob, typeSwitcher.next())
            .movesFriction(dx, dy, 0.8F)
            .color(color)
            .heightMoves(0.0F, 30.0F)
            .lifeTime(500);
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
      this.updateCurrentChargeLevel();
      if (this.currentChargeLevel >= 0 && !this.endedByInteract) {
         if (this.attackerMob.isPlayer) {
            ((PlayerMob)this.attackerMob).constantAttack = true;
         }

         Float dir = GameMath.getAngleDir(this.currentAngle);
         int attackX = this.attackerMob.getX() + (int)(dir.x * 100.0F);
         int attackY = this.attackerMob.getY() + (int)(dir.y * 100.0F);
         InventoryItem attackItem = this.item.copy();
         GreatswordChargeLevel currentLevel = this.chargeLevels[this.currentChargeLevel];
         attackItem.getGndData().setInt("cooldown", this.toolItem.getAttackAnimTime(attackItem, this.attackerMob) + 100);
         attackItem.getGndData().setBoolean("charged", true);
         attackItem.getGndData().setInt("currentChargeLevel", this.currentChargeLevel);
         currentLevel.setupAttackItem(this, attackItem);
         if (!this.attackerMob.isPlayer && this.lastItemAttackerTarget != null) {
            Point attackPos = ((ItemAttackerWeaponItem)attackItem.item)
               .getItemAttackerAttackPosition(this.attackerMob.getLevel(), this.attackerMob, this.lastItemAttackerTarget, -1, attackItem);
            attackX = attackPos.x;
            attackY = attackPos.y;
         }

         GNDItemMap attackMap = this.attackerMob.showAttackAndSendAttacker(attackItem, attackX, attackY, 0, this.seed);
         this.toolItem
            .superOnAttack(
               this.attackerMob.getLevel(),
               attackX,
               attackY,
               this.attackerMob,
               this.attackerMob.getCurrentAttackHeight(),
               attackItem,
               this.slot,
               0,
               this.seed,
               attackMap
            );

         for (ActiveBuff b : this.attackerMob.buffManager.getArrayBuffs()) {
            b.onItemAttacked(attackX, attackY, this.attackerMob, this.attackerMob.getCurrentAttackHeight(), attackItem, this.slot, 0, attackMap);
         }
      } else {
         this.attackerMob.doAndSendStopAttackAttacker(false);
      }
   }
}
