package necesse.entity.levelEvent.fishingEvent;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.GameLog;
import necesse.engine.input.Control;
import necesse.engine.network.packet.PacketFishingStatus;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FishingHookParticle;
import necesse.entity.particle.Particle;
import necesse.entity.trails.FishingTrail;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.matItem.FishItemInterface;
import necesse.level.maps.LevelTile;
import necesse.level.maps.biomes.FishingLootTable;
import necesse.level.maps.biomes.FishingSpot;

public class WaitFishingPhase extends FishingPhase {
   private final ArrayList<WaitFishingPhase.FishingLure> lures;
   private int tickCounter;
   private int lastSplashTick = -1;

   public WaitFishingPhase(FishingEvent event, Point[] hookPositions) {
      super(event);
      this.lures = new ArrayList<>(event.getLines());

      for (int i = 0; i < event.getLines(); i++) {
         this.lures.add(new WaitFishingPhase.FishingLure(i, hookPositions[i]));
      }
   }

   @Override
   public void tickMovement(float delta) {
      if (this.event.isMine() && Control.MOUSE1.isPressed()) {
         this.event.level.getClient().network.sendPacket(PacketFishingStatus.getReelPacket(this.event));
         this.event.reel();
      }

      this.lures.forEach(WaitFishingPhase.FishingLure::updateLine);
   }

   @Override
   public void clientTick() {
      if (this.event.isReeled) {
         this.reel();
      } else {
         this.event.getFishingMob().showFishingWaitAnimation(this.event.fishingRod, this.event.getTarget().x, this.event.getTarget().y);
         this.tickCounter++;
         this.lures.forEach(WaitFishingPhase.FishingLure::clientTick);
         if (this.tickCounter > 4800) {
            this.over();
         }
      }
   }

   @Override
   public void serverTick() {
      if (this.event.isReeled) {
         this.reel();
      } else {
         this.event.getFishingMob().showFishingWaitAnimation(this.event.fishingRod, this.event.getTarget().x, this.event.getTarget().y);
         this.tickCounter++;
         this.lures.forEach(WaitFishingPhase.FishingLure::serverTick);
         if (!this.event.checkOutsideRange()) {
            if (this.tickCounter > 4800) {
               this.over();
            }
         }
      }
   }

   @Override
   public void end() {
      this.lures.forEach(WaitFishingPhase.FishingLure::remove);
   }

   @Override
   public void over() {
      this.event.getFishingMob().stopFishing();
      this.lures.forEach(WaitFishingPhase.FishingLure::remove);
   }

   private int getNewCatchTick() {
      int rodPower = this.event.fishingRod.fishingPower;
      int baitPower = this.event.bait == null ? 0 : this.event.bait.fishingPower;
      int buffPower = this.event.getMob().buffManager.getModifier(BuffModifiers.FISHING_POWER);
      int totalPower = GameMath.limit(rodPower + baitPower + buffPower, 0, 100);
      float perc = Math.abs(totalPower / 100.0F - 1.0F) * this.event.speedMultiplier;
      int maxTicks = (int)(500.0F * perc);
      return this.tickCounter + (maxTicks <= 0 ? 0 : GameRandom.globalRandom.nextInt(maxTicks)) + 10;
   }

   @Override
   public void addNewCatch(int lineIndex, int inTicks, InventoryItem item) {
      if (lineIndex >= 0 && lineIndex < this.lures.size()) {
         this.lures.get(lineIndex).addCatch(item, inTicks);
      } else {
         GameLog.warn.println("Received invalid new fishing catch index " + lineIndex + " out of " + this.lures.size());
      }
   }

   @Override
   public void reel() {
      this.event.setPhase(new ReelFishingPhase(this.event, this.lures));
   }

   @Override
   public int getTicksToNextCatch() {
      return this.lures.stream().mapToInt(WaitFishingPhase.FishingLure::getTicksToNextCatch).min().orElse(500);
   }

   public class FishingLure {
      public final int lineIndex;
      public FishingTrail line;
      public FishingHookParticle hookParticle;
      public final Point hookPosition;
      private InventoryItem caught;
      public int catchNum;
      public int catchSent;
      public int catchTick;

      public FishingLure(int lineIndex, Point hookPosition) {
         this.lineIndex = lineIndex;
         this.hookPosition = hookPosition;
         if (!WaitFishingPhase.this.event.isClient()) {
            this.catchTick = WaitFishingPhase.this.getNewCatchTick();
         } else {
            this.catchTick = -1;
         }

         this.catchNum++;
         if (!WaitFishingPhase.this.event.level.isServer()) {
            WaitFishingPhase.this.event
               .level
               .entityManager
               .particles
               .add(
                  this.hookParticle = new FishingHookParticle(
                     WaitFishingPhase.this.event.level, hookPosition.x, hookPosition.y, WaitFishingPhase.this.event.fishingRod
                  )
               );
            WaitFishingPhase.this.event
               .level
               .entityManager
               .addTrail(
                  this.line = new FishingTrail(
                     WaitFishingPhase.this.event.getMob(), WaitFishingPhase.this.event.level, this.hookParticle, WaitFishingPhase.this.event.fishingRod
                  )
               );
         }
      }

      public void updateLine() {
         if (this.line != null) {
            this.line.update();
         }
      }

      public void clientTick() {
         if (this.hookParticle != null) {
            this.hookParticle.refreshSpawnTime();
         }

         if (this.caught != null) {
            if (WaitFishingPhase.this.tickCounter == this.catchTick) {
               if (WaitFishingPhase.this.lastSplashTick != WaitFishingPhase.this.tickCounter) {
                  SoundManager.playSound(GameResources.splash, SoundEffect.effect(this.hookPosition.x, this.hookPosition.y));
                  WaitFishingPhase.this.lastSplashTick = WaitFishingPhase.this.tickCounter;
               }

               if (this.hookParticle != null) {
                  this.hookParticle.blob();

                  for (int i = 0; i < 5; i++) {
                     WaitFishingPhase.this.event
                        .level
                        .entityManager
                        .addParticle(this.hookParticle.x, this.hookParticle.y, Particle.GType.IMPORTANT_COSMETIC)
                        .movesConstant((float)(GameRandom.globalRandom.nextGaussian() * 6.0), -GameRandom.globalRandom.nextFloat() * 4.0F)
                        .color(new Color(89, 139, 224));
                  }
               }
            }

            if (WaitFishingPhase.this.tickCounter - this.catchTick > WaitFishingPhase.this.event.fishingRod.reelWindow) {
               this.caught = null;
            }
         }
      }

      public void serverTick() {
         int catchInTicks = this.catchTick - WaitFishingPhase.this.tickCounter;
         if (this.caught == null && catchInTicks <= 20 && this.catchSent < this.catchNum) {
            this.catchSent = this.catchNum++;
            this.caught = this.getNewCatch();
            if (this.caught != null) {
               if (WaitFishingPhase.this.event.level.isServer()) {
                  WaitFishingPhase.this.event
                     .level
                     .getServer()
                     .network
                     .sendToClientsWithEntity(
                        PacketFishingStatus.getUpcomingCatchPacket(WaitFishingPhase.this.event, this.lineIndex, catchInTicks, this.caught),
                        WaitFishingPhase.this.event
                     );
               }
            } else {
               this.catchTick = catchInTicks + WaitFishingPhase.this.getNewCatchTick();
            }
         } else if (this.caught != null && catchInTicks < -WaitFishingPhase.this.event.fishingRod.reelWindow) {
            this.catchTick = WaitFishingPhase.this.getNewCatchTick();
            this.caught = null;
         }
      }

      public InventoryItem getCatch() {
         return this.catchTick - WaitFishingPhase.this.tickCounter < 0 ? this.caught : null;
      }

      public int getTicksToNextCatch() {
         int d = this.catchTick - WaitFishingPhase.this.tickCounter;
         if (d < 0) {
            return this.caught != null ? d : 500;
         } else {
            return d;
         }
      }

      public void addCatch(InventoryItem item, int inTicks) {
         if (item.item instanceof FishItemInterface) {
            FishItemInterface fishItem = (FishItemInterface)item.item;
            Particle particle = fishItem.getParticle(WaitFishingPhase.this.event.level, this.hookPosition.x, this.hookPosition.y, inTicks * 50);
            if (particle != null) {
               WaitFishingPhase.this.event.level.entityManager.addParticle(particle, Particle.GType.CRITICAL);
            }
         }

         this.caught = item;
         this.catchTick = WaitFishingPhase.this.tickCounter + inTicks;
      }

      private InventoryItem getNewCatch() {
         FishingSpot spot = new FishingSpot(
            new LevelTile(WaitFishingPhase.this.event.level, GameMath.getTileCoordinate(this.hookPosition.x), GameMath.getTileCoordinate(this.hookPosition.y)),
            WaitFishingPhase.this.event.fishingRod,
            WaitFishingPhase.this.event.bait
         );
         FishingLootTable lootTable = WaitFishingPhase.this.event.getFishingMob().getFishingLootTable(spot);
         return lootTable == null ? null : lootTable.getRandomItem(spot, GameRandom.globalRandom);
      }

      public void remove() {
         if (this.line != null && !this.line.isRemoved()) {
            this.line.remove();
         }

         if (this.hookParticle != null && !this.hookParticle.removed()) {
            this.hookParticle.remove();
         }
      }
   }
}
