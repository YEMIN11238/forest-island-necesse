package necesse.entity.levelEvent.fishingEvent;

import java.awt.Point;
import java.util.ArrayList;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.SoundSettings;
import necesse.entity.mobs.GameDamage;
import necesse.entity.particle.FishingHookParticle;
import necesse.entity.projectile.FishingHookProjectile;
import necesse.entity.trails.FishingTrail;
import necesse.gfx.GameResources;

public class HookFishingPhase extends FishingPhase {
   private final ArrayList<HookFishingPhase.HookLineParticles> lines;
   private int landed;
   private SoundPlayer reelSoundPlayer;

   public HookFishingPhase(FishingEvent event) {
      super(event);
      this.lines = new ArrayList<>(event.getLines());

      for (int i = 0; i < event.getLines(); i++) {
         this.lines.add(new HookFishingPhase.HookLineParticles(i));
      }

      event.getFishingMob().showFishingWaitAnimation(event.fishingRod, event.getTarget().x, event.getTarget().y);
   }

   @Override
   public void tickMovement(float delta) {
      this.landed = 0;

      for (HookFishingPhase.HookLineParticles line : this.lines) {
         line.tickLanded();
         if (line.landed) {
            this.landed++;
         }
      }
   }

   @Override
   public void clientTick() {
      if (this.reelSoundPlayer == null || this.reelSoundPlayer.isDone()) {
         this.reelSoundPlayer = SoundManager.playSound(new SoundSettings(GameResources.fishingRodReel).volume(0.6F).basePitch(1.2F), this.event.getMob());
      }

      if (this.reelSoundPlayer != null) {
         this.reelSoundPlayer.refreshLooping(0.2F);
      }

      if (this.landed == this.lines.size()) {
         this.event.setPhase(new WaitFishingPhase(this.event, this.getHookPositions()));
      } else {
         this.event.getFishingMob().showFishingWaitAnimation(this.event.fishingRod, this.event.getTarget().x, this.event.getTarget().y);
      }
   }

   @Override
   public void serverTick() {
      if (this.landed == this.lines.size()) {
         this.event.setPhase(new WaitFishingPhase(this.event, this.getHookPositions()));
      } else {
         this.event.getFishingMob().showFishingWaitAnimation(this.event.fishingRod, this.event.getTarget().x, this.event.getTarget().y);
         this.event.checkOutsideRange();
      }
   }

   private Point[] getHookPositions() {
      Point[] positions = new Point[this.lines.size()];

      for (int i = 0; i < this.lines.size(); i++) {
         positions[i] = this.lines.get(i).targetPos;
      }

      return positions;
   }

   @Override
   public void end() {
      this.lines.forEach(HookFishingPhase.HookLineParticles::remove);
   }

   @Override
   public void over() {
      this.event.getFishingMob().stopFishing();
      this.lines.forEach(HookFishingPhase.HookLineParticles::remove);
   }

   public class HookLineParticles {
      public FishingTrail line;
      public FishingHookProjectile hook = new FishingHookProjectile(HookFishingPhase.this.event.level, HookFishingPhase.this.event);
      public FishingHookParticle hookParticle;
      public boolean landed;
      public final Point targetPos;

      public HookLineParticles(int lineIndex) {
         this.targetPos = HookFishingPhase.this.event.getRandomTarget(lineIndex);
         float var10003 = this.targetPos.x;
         float var10004 = this.targetPos.y;
         this.hook
            .applyData(
               HookFishingPhase.this.event.getMob().x,
               HookFishingPhase.this.event.getMob().y,
               var10003,
               var10004,
               HookFishingPhase.this.event.fishingRod.hookSpeed,
               (int)HookFishingPhase.this.event.getMob().getPositionPoint().distance(this.targetPos.x, this.targetPos.y),
               new GameDamage(0.0F),
               HookFishingPhase.this.event.getMob()
            );
         this.hook.setStartHeight(30);
         HookFishingPhase.this.event.level.entityManager.projectiles.addHidden(this.hook);
         if (!HookFishingPhase.this.event.level.isServer()) {
            HookFishingPhase.this.event
               .level
               .entityManager
               .addTrail(
                  this.line = new FishingTrail(
                     HookFishingPhase.this.event.getMob(), HookFishingPhase.this.event.level, this.hook, HookFishingPhase.this.event.fishingRod
                  )
               );
         }
      }

      public void tickLanded() {
         if (this.hookParticle != null) {
            this.hookParticle.refreshSpawnTime();
         }

         if (this.line != null) {
            this.line.update();
         }

         if (this.hook != null) {
            if (this.hook.removed()) {
               this.landed = true;
               this.targetPos.x = this.hook.getX();
               this.targetPos.y = this.hook.getY();
               this.hook = null;
               if (this.line != null && !this.line.isRemoved()) {
                  this.line.remove();
               }

               HookFishingPhase.this.event
                  .level
                  .entityManager
                  .particles
                  .add(
                     this.hookParticle = new FishingHookParticle(
                        HookFishingPhase.this.event.level, this.targetPos.x, this.targetPos.y, HookFishingPhase.this.event.fishingRod
                     )
                  );
               HookFishingPhase.this.event
                  .level
                  .entityManager
                  .addTrail(
                     this.line = new FishingTrail(
                        HookFishingPhase.this.event.getMob(), HookFishingPhase.this.event.level, this.hookParticle, HookFishingPhase.this.event.fishingRod
                     )
                  );
            } else {
               float life = Math.abs(this.hook.getLifeTime() - 1.0F);
               this.hook.speed = HookFishingPhase.this.event.fishingRod.hookSpeed * (life / 2.0F + 0.5F);
            }
         }
      }

      public void remove() {
         if (this.line != null && !this.line.isRemoved()) {
            this.line.remove();
         }

         if (this.hook != null && !this.hook.removed()) {
            this.hook.remove();
         }

         if (this.hookParticle != null && !this.hookParticle.removed()) {
            this.hookParticle.remove();
         }
      }
   }
}
