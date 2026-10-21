package necesse.entity.mobs.attackHandler;

import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketFireShardCannon;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.projectileToolItem.gunProjectileToolItem.ShardCannonProjectileToolItem;

public class ShardCannonAttackHandler extends MousePositionAttackHandler {
   private final InventoryItem item;
   private final ShardCannonProjectileToolItem toolItem;
   private long lastTime;
   private long timeBuffer;
   private final int attackSeed;
   private int shots;
   private final GameRandom random = new GameRandom();

   public ShardCannonAttackHandler(
      ItemAttackerMob attackerMob,
      ItemAttackSlot slot,
      InventoryItem item,
      ShardCannonProjectileToolItem toolItem,
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
         int attackX = this.lastX;
         int attackY = this.lastY;
         long currentTime = this.attackerMob.getLevel().getLocalTime();
         if (this.toolItem.canAttack(this.attackerMob.getLevel(), attackX, attackY, this.attackerMob, this.item) == null) {
            this.timeBuffer = this.timeBuffer + (currentTime - this.lastTime);
            int seed = Item.getRandomAttackSeed(this.random.seeded(GameRandom.prime(this.attackSeed * this.shots)));
            GNDItemMap attackMap = this.attackerMob.showAttackAndSendAttacker(this.item, attackX, attackY, 0, seed);

            while (true) {
               int cooldown = this.getShootCooldown();
               if (this.timeBuffer < cooldown) {
                  break;
               }

               this.timeBuffer -= cooldown;
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
                  this.attackerMob.sendAttackerPacket(this.attackerMob, new PacketFireShardCannon(this.attackerMob));
               }
            }
         }

         this.lastTime = currentTime;
      }
   }

   public static void playFireSound(Mob target) {
      SoundManager.playSound(GameResources.jingle, SoundEffect.effect(target).pitch(GameRandom.globalRandom.getFloatBetween(1.5F, 1.75F)));
      SoundManager.playSound(
         GameRandom.globalRandom.getOneOf(GameResources.crystalHit2, GameResources.crystalHit3),
         SoundEffect.effect(target).pitch(GameRandom.globalRandom.getFloatBetween(0.9F, 1.1F))
      );
   }

   private int getShootCooldown() {
      float multiplier = 1.0F / this.toolItem.getAttackSpeedModifier(this.item, this.attackerMob);
      return (int)(multiplier * 150.0F);
   }

   @Override
   public void onEndAttack(boolean bySelf) {
      this.attackerMob.doAndSendStopAttackAttacker(false);
   }
}
