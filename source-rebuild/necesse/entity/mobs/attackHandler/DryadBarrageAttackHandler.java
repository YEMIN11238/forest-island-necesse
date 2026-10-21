package necesse.entity.mobs.attackHandler;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemAttackerWeaponItem;
import necesse.inventory.item.toolItem.projectileToolItem.magicProjectileToolItem.DryadBarrageToolItem;

public class DryadBarrageAttackHandler extends MouseAngleAttackHandler {
   private final ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC
   );
   public DryadBarrageToolItem toolItem;
   public InventoryItem item;
   public int seed;
   public long startTime;
   private final int maxProjectileCount;
   private final float minChargeTime = 0.5F;
   private final float maxChargeTime = 2.0F;
   protected int endAttackBuffer;
   protected boolean hasPlayedSoundEffect;
   protected SoundPlayer chargeSoundPlayer;

   public DryadBarrageAttackHandler(
      ItemAttackerMob attackerMob,
      ItemAttackSlot slot,
      InventoryItem item,
      DryadBarrageToolItem toolItem,
      int maxProjectileCount,
      int seed,
      int startX,
      int startY
   ) {
      super(attackerMob, slot, 50, 1000.0F, startX, startY);
      this.toolItem = toolItem;
      this.item = item;
      this.seed = seed;
      this.maxProjectileCount = maxProjectileCount;
      this.startTime = attackerMob.getLocalTime();
      this.hasPlayedSoundEffect = false;
      if (attackerMob.isClient()) {
         this.chargeSoundPlayer = SoundManager.playSound(
            new SoundSettings(GameResources.magicbolt1).volume(0.3F).basePitch(0.9F).pitchVariance(0.03F), attackerMob
         );
         if (this.chargeSoundPlayer != null) {
            this.chargeSoundPlayer.refreshLooping();
         }
      }
   }

   public long getTimeSinceStart() {
      return this.attackerMob.getLocalTime() - this.startTime;
   }

   public float getChargePercent() {
      int chargeTime = Math.round(2.0F / this.toolItem.getAttackSpeedModifier(this.item, this.attackerMob) * 1000.0F);
      return (float)Math.min(this.getTimeSinceStart(), chargeTime) / chargeTime;
   }

   @Override
   public void onUpdate() {
      super.onUpdate();
      Float dir = GameMath.getAngleDir(this.currentAngle);
      int attackX = this.attackerMob.getX() + (int)(dir.x * 100.0F);
      int attackY = this.attackerMob.getY() + (int)(dir.y * 100.0F);
      float chargePercent = this.getChargePercent();
      if (chargePercent < 1.0F) {
         this.toolItem.consumeMana(this.toolItem.getManaCost(this.item) / (1000.0F / this.updateInterval), this.attackerMob);
         if (this.attackerMob.buffManager.hasBuff(BuffRegistry.Debuffs.MANA_EXHAUSTION)) {
            this.attackerMob.endAttackHandler(true);
            return;
         }
      }

      if (!this.attackerMob.isPlayer && chargePercent >= 1.0F) {
         this.endAttackBuffer = this.endAttackBuffer + this.updateInterval;
         if (this.endAttackBuffer >= 250) {
            this.endAttackBuffer = 0;
            this.attackerMob.endAttackHandler(true);
            return;
         }
      }

      InventoryItem showItem = this.item.copy();
      this.attackerMob.showAttackAndSendAttacker(showItem, attackX, attackY, 0, this.seed);
      if (this.attackerMob.isClient()) {
         if (this.chargeSoundPlayer != null) {
            this.chargeSoundPlayer.refreshLooping();
         }

         if (!this.hasPlayedSoundEffect && chargePercent >= 1.0F) {
            SoundManager.playSound(GameResources.magicbolt4, SoundEffect.effect(this.attackerMob).pitch(1.4F).volume(0.4F));
            this.hasPlayedSoundEffect = true;
         }

         int particleCount = 4;
         GameRandom random = GameRandom.globalRandom;
         float anglePerParticle = 360.0F / particleCount;
         if (chargePercent >= 1.0F) {
            for (int i = 0; i < particleCount; i++) {
               int angle = (int)(i * anglePerParticle + random.nextFloat() * anglePerParticle);
               float dx = (float)Math.sin(Math.toRadians(angle)) * 50.0F;
               float dy = (float)Math.cos(Math.toRadians(angle)) * 50.0F;
               this.attackerMob
                  .getLevel()
                  .entityManager
                  .addParticle(this.attackerMob, 0.0F, 10.0F, this.typeSwitcher.next())
                  .sprite(GameResources.magicSparkParticles.sprite(random.nextInt(4), 0, 22))
                  .sizeFades(22, 44)
                  .movesFriction(dx, dy, 0.8F)
                  .color(new Color(16, 146, 187))
                  .heightMoves(0.0F, 30.0F)
                  .lifeTime(200);
            }
         } else if ((float)this.getTimeSinceStart() >= 500.0F / this.toolItem.getAttackSpeedModifier(this.item, this.attackerMob)) {
            for (int i = 0; i < particleCount; i++) {
               int angle = (int)(i * anglePerParticle + random.nextFloat() * anglePerParticle);
               float dx = (float)Math.sin(Math.toRadians(angle)) * 10.0F;
               float dy = (float)Math.cos(Math.toRadians(angle)) * 10.0F;
               this.attackerMob
                  .getLevel()
                  .entityManager
                  .addParticle(this.attackerMob, dx, dy, this.typeSwitcher.next())
                  .sprite(GameResources.magicSparkParticles.sprite(random.nextInt(4), 0, 22))
                  .sizeFades(22, 44)
                  .movesFriction(dx, dy, 0.8F)
                  .color(new Color(30, 177, 143))
                  .heightMoves(0.0F, 30.0F)
                  .lifeTime(200);
            }
         }
      }
   }

   private int getProjectileCount() {
      return (int)(this.maxProjectileCount * this.getChargePercent());
   }

   @Override
   public void onEndAttack(boolean bySelf) {
      if ((float)this.getTimeSinceStart() >= 0.5F / this.toolItem.getAttackSpeedModifier(this.item, this.attackerMob) * 1000.0F) {
         if (this.attackerMob.isPlayer) {
            ((PlayerMob)this.attackerMob).constantAttack = true;
         }

         Float dir = GameMath.getAngleDir(this.currentAngle);
         int attackX = this.attackerMob.getX() + (int)(dir.x * 100.0F);
         int attackY = this.attackerMob.getY() + (int)(dir.y * 100.0F);
         InventoryItem attackItem = this.item.copy();
         if (!this.attackerMob.isPlayer && this.lastItemAttackerTarget != null) {
            Point attackPos = ((ItemAttackerWeaponItem)attackItem.item)
               .getItemAttackerAttackPosition(this.attackerMob.getLevel(), this.attackerMob, this.lastItemAttackerTarget, -1, attackItem);
            attackX = attackPos.x;
            attackY = attackPos.y;
         }

         GNDItemMap attackMap = this.attackerMob.showAttackAndSendAttacker(attackItem, attackX, attackY, 0, this.seed);
         this.toolItem.triggerBarrageAttack(this.attackerMob.getLevel(), attackX, attackY, this.attackerMob, attackItem, this.seed, this.getProjectileCount());

         for (ActiveBuff b : this.attackerMob.buffManager.getArrayBuffs()) {
            b.onItemAttacked(attackX, attackY, this.attackerMob, this.attackerMob.getCurrentAttackHeight(), attackItem, this.slot, 0, attackMap);
         }
      } else {
         this.attackerMob.doAndSendStopAttackAttacker(false);
      }

      if (this.attackerMob.isClient() && this.chargeSoundPlayer != null) {
         this.chargeSoundPlayer.stop();
      }
   }
}
