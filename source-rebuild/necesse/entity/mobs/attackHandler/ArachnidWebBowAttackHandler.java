package necesse.entity.mobs.attackHandler;

import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketFireArachnidWebBow;
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
import necesse.inventory.item.toolItem.projectileToolItem.bowProjectileToolItem.ArachnidWebBowToolItem;

public class ArachnidWebBowAttackHandler extends MousePositionAttackHandler {
   private final InventoryItem item;
   private final ArachnidWebBowToolItem toolItem;
   private final int attackSeed;
   private int shotsRemaining = 3;
   private int shots;
   private long timeBuffer;
   private final GameRandom random = new GameRandom();
   private final int timeBetweenReloads = 500;
   private final int timeBetweenBurstShots = 125;

   public ArachnidWebBowAttackHandler(
      ItemAttackerMob itemAttacker, ItemAttackSlot slot, InventoryItem item, ArachnidWebBowToolItem toolItem, int seed, int startTargetX, int startTargetY
   ) {
      super(itemAttacker, slot, 50, startTargetX, startTargetY);
      this.attackSeed = seed;
      this.timeBuffer = 500L;
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
               if ((float)this.timeBuffer < 500.0F * speedModifier) {
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
                  this.attackerMob.sendAttackerPacket(this.attackerMob, new PacketFireArachnidWebBow(this.attackerMob));
               }

               if (this.shotsRemaining <= 0) {
                  this.shotsRemaining = 3;
                  this.timeBuffer = 0L;
                  break;
               }

               this.timeBuffer = (int)(375.0F * speedModifier);
            }
         }
      }
   }

   public static void playFireSound(Mob target) {
      SoundManager.playSound(GameResources.bow, SoundEffect.effect(target));
   }

   private float getSpeedModifier() {
      return 1.0F / this.toolItem.getAttackSpeedModifier(this.item, this.attackerMob);
   }

   @Override
   public void onEndAttack(boolean bySelf) {
      if (this.attackerMob.isPlayer) {
         this.attackerMob.startItemCooldown(this.toolItem, (int)(500.0F * this.getSpeedModifier()));
      }

      this.attackerMob.doAndSendStopAttackAttacker(false);
   }
}
