package necesse.entity.mobs.attackHandler;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketFireDeathRipper;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.swordToolItem.RevSwordToolItem;

public class RevSwordAttackHandler extends MousePositionAttackHandler {
   protected SoundPlayer chargeSoundPlayer;
   protected int lastChargeSoundCooldown;
   public RevSwordToolItem toolItem;
   private final InventoryItem item;
   private long lastTime;
   private long timeBuffer;
   private final int attackSeed;
   private int attacks;
   private final GameRandom random = new GameRandom();

   public RevSwordAttackHandler(
      ItemAttackerMob attackerMob, ItemAttackSlot slot, InventoryItem item, RevSwordToolItem toolItem, int seed, int startTargetX, int startTargetY
   ) {
      super(attackerMob, slot, 50, startTargetX, startTargetY);
      this.toolItem = toolItem;
      this.item = item;
      this.attackSeed = seed;
      this.lastTime = attackerMob.getLocalTime();
   }

   @Override
   public void onUpdate() {
      super.onUpdate();
      if (!this.attackerMob.isPlayer
         && this.lastItemAttackerTarget != null
         && !ChaserAINode.hasLineOfSightToTarget(this.attackerMob, this.lastItemAttackerTarget, 5.0F)) {
         this.attackerMob.endAttackHandler(true);
      } else {
         if (this.attackerMob.isClient()) {
            int cooldown = this.getAttackCooldownFlat();
            InventoryItem attackItem = this.item.copy();
            attackItem.getGndData().setInt("currentAttackCooldown", cooldown / 2);
            int distToStartPos = 20;
            Float particleDir = this.getNormalizedVectorToTarget();
            GameRandom random = GameRandom.globalRandom;
            this.attackerMob
               .getLevel()
               .entityManager
               .addTopParticle(
                  this.attackerMob.x - particleDir.x * distToStartPos + random.getFloatBetween(-10.0F, 10.0F),
                  this.attackerMob.y - particleDir.y * distToStartPos - 8.0F + random.getFloatBetween(-10.0F, 10.0F),
                  Particle.GType.COSMETIC
               )
               .sprite(GameResources.smokePuff.sprite(random.nextInt(4), 0, 32))
               .color(new Color(55, 45, 45))
               .sizeFades(16, 32)
               .fadesAlphaTime(100, 100)
               .movesConstant(0.0F, -60.0F)
               .moves(
                  (pos, delta, lifeTime, timeAlive, lifePercent) -> {
                     pos.x = pos.x
                        + (random.nextBoolean() ? (float)(Math.sin(timeAlive / 50.0F) * delta) * 0.1F : (float)(Math.cos(timeAlive / 50.0F) * delta) * 0.1F);
                     pos.y -= (2.0F - lifePercent) * delta * 0.05F;
                  }
               )
               .ignoreLight(true)
               .lifeTime(500);
            if (this.chargeSoundPlayer == null || this.lastChargeSoundCooldown != cooldown) {
               SoundPlayer lastPlayer = this.chargeSoundPlayer;
               float progress = GameMath.getPercentageBetweenTwoNumbers(cooldown, 150.0F, 350.0F);
               float pitch = GameMath.lerp(progress, 1.3F, 1.0F);
               float volume = GameMath.lerp(progress, 0.2F, 0.3F);
               this.chargeSoundPlayer = SoundManager.playSound(
                  GameResources.revSword, SoundEffect.effect(this.attackerMob).volume(volume).pitch(pitch), player -> {
                     if (lastPlayer == null) {
                        player.fadeIn(1.0F);
                     } else {
                        player.copyFadeInProgress(lastPlayer);
                        player.setPosition(lastPlayer.getPositionSeconds());
                     }
                  }
               );
               this.lastChargeSoundCooldown = cooldown;
            }

            if (this.chargeSoundPlayer != null && !this.chargeSoundPlayer.isDone()) {
               this.chargeSoundPlayer.refreshLooping(0.5F);
            }
         }

         int attackX = this.lastX;
         int attackY = this.lastY;
         long currentTime = this.attackerMob.getLevel().getLocalTime();
         if (this.toolItem.canAttack(this.attackerMob.getLevel(), attackX, attackY, this.attackerMob, this.item) == null) {
            this.timeBuffer = this.timeBuffer + (currentTime - this.lastTime);
            int seed = Item.getRandomAttackSeed(this.random.seeded(GameRandom.prime(this.attackSeed * this.attacks)));
            GNDItemMap attackMap = this.attackerMob.showAttackAndSendAttacker(this.item, attackX, attackY, 0, seed);

            while (true) {
               int cooldown = this.getAttackCooldownFlat();
               float multiplier = 1.0F / this.toolItem.getAttackSpeedModifier(this.item, this.attackerMob);
               int actualCooldown = (int)(cooldown * multiplier);
               if (this.timeBuffer < actualCooldown) {
                  break;
               }

               this.timeBuffer -= actualCooldown;
               seed = Item.getRandomAttackSeed(this.random.nextSeeded(GameRandom.prime(this.attackSeed * this.attacks)));
               this.attacks++;
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

               if (this.attackerMob.isServer()) {
                  this.attackerMob.sendAttackerPacket(this.attackerMob, new PacketFireDeathRipper(this.attackerMob));
               } else {
                  int distToStartPos = 30;
                  Float particleDir = this.getNormalizedVectorToTarget();
                  float var10001 = this.attackerMob.x - particleDir.x * distToStartPos;
                  this.attackerMob
                     .getLevel()
                     .entityManager
                     .addTopParticle(var10001, this.attackerMob.y - particleDir.y * distToStartPos - 8.0F, Particle.GType.COSMETIC)
                     .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 22))
                     .color(new Color(255, 188, 78))
                     .sizeFades(11, 22)
                     .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
                        pos.x = pos.x - particleDir.x * delta * 0.2F;
                        pos.y = pos.y - particleDir.y * delta * 0.2F;
                     })
                     .ignoreLight(true)
                     .lifeTime(200);
               }
            }
         } else {
            this.onEndAttack(true);
         }

         this.lastTime = currentTime;
      }
   }

   private Float getNormalizedVectorToTarget() {
      return GameMath.normalize(this.attackerMob.x - this.lastX, this.attackerMob.y - this.lastY);
   }

   private int getAttackCooldownFlat() {
      return this.attacks > 10 ? 100 : GameMath.lerp(this.attacks / 10.0F, 400, 50);
   }

   @Override
   public void onEndAttack(boolean bySelf) {
      this.attackerMob.doAndSendStopAttackAttacker(false);
      this.attacks = 0;
      if (this.chargeSoundPlayer != null) {
         this.chargeSoundPlayer.fadeOutAndStop(0.5F);
      }
   }
}
