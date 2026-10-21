package necesse.entity.levelEvent;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.projectile.pathProjectile.SurvivorWhipProjectile;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.GameResources;
import necesse.gfx.gameTexture.GameSprite;

public class ShowSurvivorWhipAttackEvent extends ShowAttackAbstractTickEvent<SurvivorWhipProjectile> {
   public ShowSurvivorWhipAttackEvent.TrailOffsetContainer[] trails = null;
   public boolean hasCompletedFirstPart;
   public float lastX;
   public float lastY;

   public ShowSurvivorWhipAttackEvent() {
   }

   public ShowSurvivorWhipAttackEvent(SurvivorWhipProjectile entity, int totalTicksPerAttack) {
      super(entity, totalTicksPerAttack);
   }

   @Override
   public void init() {
      super.init();
      Mob owner = this.entity.getOwner();
      if (owner != null) {
         SoundManager.playSound(GameResources.survivorWhip1, SoundEffect.effect(owner).volume(0.3F));
      }
   }

   public void tick(SurvivorWhipProjectile entity, float currentAttackProgress) {
      Mob owner = entity.getOwner();
      Float attackDir = entity.dir;
      if (owner != null && attackDir != null) {
         SurvivorWhipProjectile.WhipPosition position = SurvivorWhipProjectile.getWhipPositionFinal(
            owner.x, owner.y, attackDir, entity.frontAttackRange, entity.behindAttackRange, currentAttackProgress
         );
         if (position.isSecondPart && !this.hasCompletedFirstPart) {
            this.hasCompletedFirstPart = true;
            SoundManager.playSound(GameResources.survivorWhip2, SoundEffect.effect(owner).volume(0.3F).pitch(0.85F));
            this.trails = null;
         }

         float sine = (float)Math.sin(position.currentAttackProgress * (float) Math.PI);
         float thickness = sine * 25.0F;
         float height = GameMath.lerp(position.currentAttackProgress, 4, 12);
         if (this.trails == null) {
            GameRandom random = new GameRandom(this.getUniqueID()).nextSeeded(93);
            int trailCount = 4;
            this.trails = new ShowSurvivorWhipAttackEvent.TrailOffsetContainer[trailCount];
            this.lastX = owner.x;
            this.lastY = owner.y;

            for (int i = 0; i < this.trails.length; i++) {
               float xOffset = random.getFloatBetween(-8.0F, 8.0F);
               float yOffset = random.getFloatBetween(-8.0F, 8.0F);
               Trail trail = new Trail(
                  new TrailVector(position.x, position.y, position.currentAttackDir.x, position.currentAttackDir.y, thickness, height),
                  this.level,
                  new Color(182, 218, 220),
                  200
               );
               this.trails[i] = new ShowSurvivorWhipAttackEvent.TrailOffsetContainer(trail, xOffset, yOffset);
               trail.removeOnFadeOut = false;
               trail.sprite = new GameSprite(GameResources.chains, 7, 0, 32);
               this.level.entityManager.addTrail(trail);
            }
         } else {
            for (ShowSurvivorWhipAttackEvent.TrailOffsetContainer trail : this.trails) {
               float xOffset = trail.xOffset * sine;
               float yOffset = trail.yOffset * sine;
               trail.trail
                  .addPointIfSameDirection(
                     new TrailVector(
                        position.x + xOffset,
                        position.y + yOffset,
                        position.x + xOffset - (this.lastX + xOffset),
                        position.y + yOffset - (this.lastY + yOffset),
                        thickness,
                        height
                     ),
                     0.2F,
                     5.0F,
                     5.0F
                  );
            }
         }

         this.lastX = position.x;
         this.lastY = position.y;
      }
   }

   public float getAttackProgress(SurvivorWhipProjectile entity) {
      return entity.traveledDistance / entity.distance;
   }

   public boolean isAttackValid(SurvivorWhipProjectile entity) {
      return !entity.removed();
   }

   protected static class TrailOffsetContainer {
      public Trail trail;
      public float xOffset;
      public float yOffset;

      public TrailOffsetContainer(Trail trail, float xOffset, float yOffset) {
         this.trail = trail;
         this.xOffset = xOffset;
         this.yOffset = yOffset;
      }
   }
}
