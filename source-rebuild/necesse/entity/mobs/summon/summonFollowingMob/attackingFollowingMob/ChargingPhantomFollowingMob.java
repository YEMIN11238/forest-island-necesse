package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.CooldownAttackTargetAINode;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerFlyingFollowerChargeChaserAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.particle.Particle;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class ChargingPhantomFollowingMob extends FlyingAttackingFollowingMob {
   public Trail trail;
   public float moveAngle;
   private float toMove;

   public ChargingPhantomFollowingMob() {
      super(10);
      this.moveAccuracy = 15;
      this.setSpeed(200.0F);
      this.setFriction(2.0F);
      this.collision = new Rectangle(-18, -15, 36, 30);
      this.hitBox = new Rectangle(-18, -15, 36, 36);
      this.selectBox = new Rectangle();
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return this.summonDamage;
   }

   @Override
   public int getCollisionKnockback(Mob target) {
      return 15;
   }

   @Override
   public void handleCollisionHit(Mob target, GameDamage damage, int knockback) {
      Mob owner = this.getAttackOwner();
      if (owner != null && target != null) {
         target.isServerHit(damage, target.x - owner.x, target.y - owner.y, knockback, this);
         this.collisionHitCooldowns.startCooldown(target);
      }
   }

   @Override
   public void init() {
      super.init();
      this.collisionHitCooldowns.hitCooldown = 750;
      this.ai = new BehaviourTreeAI<>(
         this, new PlayerFlyingFollowerChargeChaserAI<>(576, CooldownAttackTargetAINode.CooldownTimer.TICK, 800, 100, 1024, 64), new FlyingAIMover()
      );
      if (this.isClient()) {
         this.trail = new Trail(this, this.getLevel(), new Color(22, 20, 45), 16.0F, 200, 0.0F);
         this.trail.drawOnTop = true;
         this.trail.removeOnFadeOut = false;
         this.getLevel().entityManager.addTrail(this.trail);
      }
   }

   @Override
   public void tickMovement(float delta) {
      this.toMove += delta;

      while (this.toMove > 4.0F) {
         float oldX = this.x;
         float oldY = this.y;
         super.tickMovement(4.0F);
         this.toMove -= 4.0F;
         Float d = GameMath.normalize(oldX - this.x, oldY - this.y);
         this.moveAngle = (float)Math.toDegrees(Math.atan2(d.y, d.x)) - 90.0F;
         if (this.trail != null) {
            float trailOffset = 5.0F;
            this.trail.addPoint(new TrailVector(this.x + d.x * trailOffset, this.y + d.y * trailOffset, -d.x, -d.y, this.trail.thickness, 0.0F));
         }
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.getLevel()
         .entityManager
         .addParticle(
            this.x + (float)(GameRandom.globalRandom.nextGaussian() * 4.0),
            this.y + (float)(GameRandom.globalRandom.nextGaussian() * 4.0),
            Particle.GType.IMPORTANT_COSMETIC
         )
         .movesConstant(this.dx / 10.0F, this.dy / 10.0F)
         .color(new Color(36, 33, 75));
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 30; i++) {
         this.getLevel()
            .entityManager
            .addParticle(this.x, this.y, Particle.GType.COSMETIC)
            .movesConstantAngle(GameRandom.globalRandom.nextInt(360), GameRandom.globalRandom.getIntBetween(5, 20))
            .color(new Color(36, 33, 75));
      }
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return new SoundSettings(GameResources.fadedeath3).volume(0.5F);
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
      int drawY = camera.getDrawY(y) - 10;
      int anim = GameUtils.getAnim(this.getTime(), 6, 400);
      DrawOptions body = MobRegistry.Textures.chargingPhantom
         .body
         .initDraw()
         .sprite(anim, 0, 32, 64)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .rotate(this.moveAngle, 16, 10)
         .pos(drawX, drawY);
      topList.add(tm -> body.draw());
      DrawOptions shadow = MobRegistry.Textures.chargingPhantom
         .shadow
         .initDraw()
         .sprite(anim, 1, 32, 64)
         .light(light)
         .rotate(this.moveAngle, 16, 10)
         .pos(drawX, drawY + 10);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public void dispose() {
      super.dispose();
      if (this.trail != null) {
         this.trail.removeOnFadeOut = true;
      }
   }
}
