package necesse.entity.mobs.attackHandler;

import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketFireSixShooter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.SixShooterProjectileToolItem;

public class SixShooterAttackHandler extends MousePositionAttackHandler {
   private final InventoryItem item;
   private final SixShooterProjectileToolItem toolItem;
   private final int attackSeed;
   private int shotsRemaining = 6;
   private int shots;
   private long timeBuffer;
   private final GameRandom random = new GameRandom();
   private final int timeBetweenReloads = 1500;
   private final int timeBetweenBurstShots = 200;

   public SixShooterAttackHandler(
      ItemAttackerMob itemAttacker,
      ItemAttackSlot slot,
      InventoryItem item,
      SixShooterProjectileToolItem toolItem,
      int seed,
      int startTargetX,
      int startTargetY
   ) {
      super(itemAttacker, slot, 50, startTargetX, startTargetY);
      this.attackSeed = seed;
      this.timeBuffer = 1500L;
      this.item = item;
      this.toolItem = toolItem;
   }

   @Override
   public void onUpdate() {
      super.onUpdate();
      if (!this.attackerMob.isPlayer
         && this.lastItemAttackerTarget != null
         && !ChaserAINode.hasLineOfSightToTarget(this.attackerMob, this.lastItemAttackerTarget, 5.0F)) {
         this.attackerMob.endAttackHandler(true);
      } else {
         int attackX = this.lastX;
         int attackY = this.lastY;
         if (this.toolItem.canAttack(this.attackerMob.getLevel(), attackX, attackY, this.attackerMob, this.item) == null) {
            int seed = Item.getRandomAttackSeed(this.random.seeded(GameRandom.prime(this.attackSeed * this.shots)));
            GNDItemMap attackMap = this.attackerMob.showAttackAndSendAttacker(this.item, attackX, attackY, seed, 0);
            this.timeBuffer = this.timeBuffer + this.updateInterval;

            while (true) {
               float speedModifier = this.getSpeedModifier();
               if ((float)this.timeBuffer < 1500.0F * speedModifier) {
                  break;
               }

               seed = Item.getRandomAttackSeed(this.random.nextSeeded(GameRandom.prime(this.attackSeed * this.shots)));
               this.shots++;
               this.shotsRemaining--;
               this.toolItem
                  .superOnAttack(
                     this.attackerMob.getLevel(),
                     attackX,
                     attackY,
                     this.attackerMob,
                     this.attackerMob.getCurrentAttackHeight(),
                     this.item,
                     this.slot,
                     0,
                     seed,
                     attackMap
                  );

               for (ActiveBuff b : this.attackerMob.buffManager.getArrayBuffs()) {
                  b.onItemAttacked(attackX, attackY, this.attackerMob, this.attackerMob.getCurrentAttackHeight(), this.item, this.slot, 0, attackMap);
               }

               if (this.attackerMob.isClient()) {
                  playFireSound(this.attackerMob);
               } else if (this.attackerMob.isServer()) {
                  this.attackerMob.sendAttackerPacket(this.attackerMob, new PacketFireSixShooter(this.attackerMob));
               }

               if (this.shotsRemaining <= 0) {
                  if (this.attackerMob.isClient()) {
                     this.playReloadEffects(this.attackerMob, 6);
                  }

                  this.shotsRemaining = 6;
                  this.timeBuffer = 0L;
                  break;
               }

               this.timeBuffer = (int)(1300.0F * speedModifier);
            }
         }
      }
   }

   public static void playFireSound(Mob target) {
      SoundManager.playSound(GameResources.explosionHeavy, SoundEffect.effect(target).pitch(2.5F).volume(0.5F));
      SoundManager.playSound(GameResources.crystalHit3, SoundEffect.effect(target).pitch(2.0F).volume(0.5F));
   }

   private void playReloadEffects(Mob target, int particleCount) {
      if (particleCount == 1) {
         SoundManager.playSound(GameResources.crystalHit2, SoundEffect.effect(target).pitch(1.5F).volume(0.5F));
      } else {
         SoundManager.playSound(GameResources.coins, SoundEffect.effect(target).pitch(0.75F).volume(0.5F));
      }

      for (int i = 0; i < particleCount; i++) {
         float xMove = GameRandom.globalRandom.getFloatBetween(-0.05F, 0.05F);
         float yStart = this.attackerMob.y + GameRandom.globalRandom.getFloatBetween(-7.0F, 7.0F);
         this.attackerMob
            .getLevel()
            .entityManager
            .addParticle(this.attackerMob.x + GameRandom.globalRandom.getFloatBetween(-5.0F, 5.0F), this.attackerMob.y, Particle.GType.COSMETIC)
            .sprite(GameResources.bulletCasingParticles.sprite(0, 0, 6, 8))
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               pos.x += xMove;
               pos.y = (float)(yStart - 5.0 * Math.abs(1.3F * Math.cos(10.0F * lifePercent)));
            })
            .rotates(360.0F, 720.0F)
            .size((options, lifeTime, timeAlive, lifePercent) -> options.size(6, 8))
            .fadesAlpha(0.0F, 0.2F)
            .lifeTimeBetween(1000, 2000);
      }
   }

   private float getSpeedModifier() {
      return 1.0F / this.toolItem.getAttackSpeedModifier(this.item, this.attackerMob);
   }

   private int getCanceledReloadTime() {
      if (this.shots == 0) {
         return 0;
      } else {
         return this.shots % 6 != 0 ? 250 * (6 - this.shotsRemaining) : 1500;
      }
   }

   @Override
   public void onEndAttack(boolean bySelf) {
      if (this.shotsRemaining < 6 && this.attackerMob.isClient()) {
         this.playReloadEffects(this.attackerMob, 6 - this.shotsRemaining);
      }

      if (this.attackerMob.isPlayer) {
         this.attackerMob.startItemCooldown(this.toolItem, (int)(this.getCanceledReloadTime() * this.getSpeedModifier()));
      }

      this.attackerMob.doAndSendStopAttackAttacker(false);
   }
}
