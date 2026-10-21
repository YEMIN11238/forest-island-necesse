package necesse.entity.mobs.attackHandler;

import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketFireDeathRipper;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.DeathRipperProjectileToolItem;

public class DeathRipperAttackHandler extends MousePositionAttackHandler {
   protected SoundPlayer chargeSoundPlayer;
   protected int lastChargeSoundCooldown;
   private final InventoryItem item;
   private final DeathRipperProjectileToolItem toolItem;
   private long lastTime;
   private long timeBuffer;
   private final int attackSeed;
   private int shots;
   private final GameRandom random = new GameRandom();

   public DeathRipperAttackHandler(
      ItemAttackerMob attackerMob,
      ItemAttackSlot slot,
      InventoryItem item,
      DeathRipperProjectileToolItem toolItem,
      int seed,
      int startTargetX,
      int startTargetY
   ) {
      super(attackerMob, slot, 50, startTargetX, startTargetY);
      this.item = item;
      this.toolItem = toolItem;
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
            int cooldown = this.getShootCooldownFlat();
            if (this.chargeSoundPlayer == null || this.lastChargeSoundCooldown != cooldown) {
               SoundPlayer lastPlayer = this.chargeSoundPlayer;
               float progress = GameMath.getPercentageBetweenTwoNumbers(cooldown, 150.0F, 350.0F);
               float pitch = GameMath.lerp(progress, 1.3F, 1.0F);
               float volume = GameMath.lerp(progress, 0.2F, 0.3F);
               this.chargeSoundPlayer = SoundManager.playSound(
                  GameResources.deathRipperCharge, SoundEffect.effect(this.attackerMob).volume(volume).pitch(pitch), player -> {
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
            int seed = Item.getRandomAttackSeed(this.random.seeded(GameRandom.prime(this.attackSeed * this.shots)));
            GNDItemMap attackMap = this.attackerMob.showAttackAndSendAttacker(this.item, attackX, attackY, 0, seed);

            while (true) {
               int cooldown = this.getShootCooldownFlat();
               float multiplier = 1.0F / this.toolItem.getAttackSpeedModifier(this.item, this.attackerMob);
               int actualCooldown = (int)(cooldown * multiplier);
               if (this.timeBuffer < actualCooldown) {
                  break;
               }

               this.timeBuffer -= actualCooldown;
               seed = Item.getRandomAttackSeed(this.random.nextSeeded(GameRandom.prime(this.attackSeed * this.shots)));
               this.shots++;
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
                  this.attackerMob.sendAttackerPacket(this.attackerMob, new PacketFireDeathRipper(this.attackerMob));
               }
            }
         } else {
            this.onEndAttack(true);
         }

         this.lastTime = currentTime;
      }
   }

   public static void playFireSound(Mob target) {
      SoundManager.playSound(GameResources.handgun, SoundEffect.effect(target));
   }

   private int getShootCooldownFlat() {
      if (this.shots > 9) {
         return 150;
      } else if (this.shots > 6) {
         return 200;
      } else {
         return this.shots > 3 ? 275 : 350;
      }
   }

   @Override
   public void onEndAttack(boolean bySelf) {
      this.attackerMob.doAndSendStopAttackAttacker(false);
      this.shots = 0;
      if (this.chargeSoundPlayer != null) {
         this.chargeSoundPlayer.fadeOutAndStop(0.5F);
      }
   }
}
