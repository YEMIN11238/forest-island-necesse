package necesse.entity.mobs.friendly;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.FollowPosition;
import necesse.entity.mobs.itemAttacker.FollowerPosition;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class QuickTinkerFollowingMob extends Mob {
   public static FollowPosition QUICK_TINKER_CIRCLE = new FollowPosition((m, i, t) -> null, (m, i, t) -> FollowPosition.circlingPos(m, i, t, 40, 60.0F, 10, 40));
   public Trail trail;
   public float moveAngle;
   private float toMove;
   private FollowerPosition currentPos;
   public int index;
   public int totalOther;
   public Mob followingMob;
   public int keepAliveBuffer;

   public QuickTinkerFollowingMob() {
      super(10);
      this.moveAccuracy = 15;
      this.shouldSave = false;
      this.isStatic = true;
      this.setSpeed(140.0F);
      this.setFriction(2.0F);
      this.collision = new Rectangle(-18, -15, 36, 30);
      this.hitBox = new Rectangle(-18, -15, 36, 36);
      this.selectBox = new Rectangle();
   }

   @Override
   public void init() {
      super.init();
      if (this.isClient()) {
         this.trail = new Trail(this, this.getLevel(), new Color(255, 246, 203), 16.0F, 200, 0.0F);
         this.trail.drawOnTop = true;
         this.trail.removeOnFadeOut = false;
         this.getLevel().entityManager.addTrail(this.trail);
      }

      if (this.isServer()) {
         this.remove();
      }
   }

   @Override
   protected void checkCollision() {
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return null;
   }

   @Override
   public void tickSendSyncPackets() {
      this.nextMovementPacketRegionPositions.clear();
   }

   @Override
   public boolean shouldSendSpawnPacket() {
      return false;
   }

   @Override
   public void requestServerUpdate() {
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public boolean canPushMob(Mob other) {
      return false;
   }

   @Override
   public boolean isHealthBarVisible() {
      return false;
   }

   @Override
   public boolean countDamageDealt() {
      return false;
   }

   @Override
   public void tickMovement(float delta) {
      if (this.followingMob == null) {
         this.remove();
      }

      this.toMove += delta;

      while (this.toMove > 4.0F) {
         float oldX = this.x;
         float oldY = this.y;
         super.tickMovement(4.0F);
         this.toMove -= 4.0F;
         Float d = GameMath.normalize(oldX - this.x, oldY - this.y);
         this.moveAngle = (float)Math.toDegrees(Math.atan2(d.y, d.x)) - 90.0F;
         if (this.trail != null) {
            float trailOffset = 2.0F;
            this.trail.addPoint(new TrailVector(this.x + d.x * trailOffset, this.y + d.y * trailOffset, -d.x, -d.y, 6.0F, 0.0F));
         }
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.keepAliveBuffer--;
      if (this.keepAliveBuffer < 0) {
         this.remove();
      }

      if (this.followingMob != null) {
         this.currentPos = QUICK_TINKER_CIRCLE.getRelativePos(this.followingMob, this.currentPos, this.index, this.totalOther);
         this.setMovement(this.currentPos.movementGetter.apply(this.followingMob));
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 3; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.quickTinker, i, 0, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return new SoundSettings(GameResources.fadedeath1).volume(0.5F);
   }

   @Override
   protected void addDrawables(
      List<MobDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      Level level,
      int x,
      int y,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 16;
      int drawY = camera.getDrawY(y) - 16;
      int anim = GameUtils.getAnim(this.getTime(), 8, 400);
      DrawOptions body = MobRegistry.Textures.quickTinker.initDraw().sprite(anim, 0, 32, 32).light(light).pos(drawX, drawY);
      topList.add(tm -> body.draw());
   }

   @Override
   public void dispose() {
      super.dispose();
      if (this.trail != null) {
         this.trail.removeOnFadeOut = true;
      }
   }
}
