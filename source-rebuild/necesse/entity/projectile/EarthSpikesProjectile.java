package necesse.entity.projectile;

import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GroundPillar;
import necesse.engine.util.GroundPillarList;
import necesse.entity.Entity;
import necesse.entity.manager.GroundPillarHandler;
import necesse.entity.mobs.EarthSpikeMob;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;
import necesse.level.maps.light.GameLight;

public class EarthSpikesProjectile extends Projectile {
   protected int distanceBetweenSpikes = 20;
   protected GameDamage damage;
   protected Mob owner;
   private double distCounter;
   private double distBuffer;
   private final GroundPillarList<EarthSpikesProjectile.SmallEarthSpikePillar> smallPillars = new GroundPillarList<>();

   public EarthSpikesProjectile() {
   }

   public EarthSpikesProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback
   ) {
      this.setLevel(level);
      this.setOwner(owner);
      this.owner = owner;
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.distance = distance;
      this.setDamage(damage);
      this.damage = damage;
      this.knockback = knockback;
   }

   @Override
   public void init() {
      super.init();
      this.height = 0.0F;
      this.piercing = 100;
      this.setWidth(20.0F);
      if (this.isClient()) {
         this.getLevel().entityManager.addPillarHandler(new GroundPillarHandler<EarthSpikesProjectile.SmallEarthSpikePillar>(this.smallPillars) {
            @Override
            protected boolean canRemove() {
               return EarthSpikesProjectile.this.removed();
            }

            @Override
            public double getCurrentDistanceMoved() {
               return EarthSpikesProjectile.this.distCounter;
            }
         });
      }
   }

   @Override
   public Trail getTrail() {
      return null;
   }

   @Override
   public void onMoveTick(Float startPos, double movedDist) {
      super.onMoveTick(startPos, movedDist);
      if (!(this.traveledDistance > this.distance - this.distanceBetweenSpikes)) {
         this.distCounter += movedDist;
         this.distBuffer += movedDist;

         while (this.distBuffer > this.distanceBetweenSpikes) {
            this.distBuffer = this.distBuffer - this.distanceBetweenSpikes;
            synchronized (this.smallPillars) {
               this.smallPillars
                  .add(
                     new EarthSpikesProjectile.SmallEarthSpikePillar(
                        (int)(this.x + GameRandom.globalRandom.floatGaussian() * 6.0F),
                        (int)(this.y + GameRandom.globalRandom.floatGaussian() * 4.0F),
                        this.distCounter,
                        this.getLocalTime()
                     )
                  );
            }
         }
      }
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (mob == null) {
         EarthSpikeMob earthSpike = new EarthSpikeMob(this.owner, this.damage, this.getTime() + 9000L);
         GameRandom random = new GameRandom(this.getUniqueID());
         earthSpike.setPos((int)(x + random.floatGaussian() * 3.0F), (int)(y + random.floatGaussian() * 1.5F), true);
         earthSpike.resetUniqueID(random);
         this.getLevel().entityManager.mobs.addHidden(earthSpike);
      }
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
   }

   @Override
   protected SoundSettings getMoveSound() {
      return new SoundSettings(GameResources.blunthit).volume(0.2F).basePitch(1.2F).pitchVariance(0.0F);
   }

   public static class SmallEarthSpikePillar extends GroundPillar {
      public GameTextureSection texture;
      public boolean mirror = GameRandom.globalRandom.nextBoolean();

      public SmallEarthSpikePillar(int x, int y, double spawnDistance, long spawnTime) {
         super(x, y, spawnDistance, spawnTime);
         this.texture = GameResources.smallEarthSpikes == null
            ? null
            : GameRandom.globalRandom
               .getOneOf(
                  new GameTextureSection(GameResources.smallEarthSpikes).sprite(0, 0, 64),
                  new GameTextureSection(GameResources.smallEarthSpikes).sprite(1, 0, 64),
                  new GameTextureSection(GameResources.smallEarthSpikes).sprite(2, 0, 64)
               );
         this.behaviour = new GroundPillar.TimedBehaviour(750, 150, 150);
      }

      @Override
      public DrawOptions getDrawOptions(Level level, long currentTime, double distanceMoved, GameCamera camera, PlayerMob perspective) {
         GameLight light = level.getLightLevel(Entity.getTileCoordinate(this.x), Entity.getTileCoordinate(this.y));
         int drawX = camera.getDrawX(this.x);
         int drawY = camera.getDrawY(this.y);
         double height = this.getHeight(currentTime, distanceMoved);
         int endY = (int)(height * this.texture.getHeight());
         return this.texture
            .section(0, this.texture.getWidth(), 0, endY)
            .initDraw()
            .mirror(this.mirror, false)
            .light(light)
            .pos(drawX - this.texture.getWidth() / 2, drawY - endY);
      }
   }
}
