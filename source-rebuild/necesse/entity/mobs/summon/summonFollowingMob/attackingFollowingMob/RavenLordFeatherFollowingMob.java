package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Line2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerFlyingFollowerValidTargetCollisionChaserAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.mobs.buffs.staticBuffs.armorBuffs.setBonusBuffs.RavenlordsHeaddressSetBonusBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.networkField.BooleanNetworkField;
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

public class RavenLordFeatherFollowingMob extends FlyingAttackingFollowingMob {
   public Trail trail;
   public float moveAngle;
   private float toMove;
   public BooleanNetworkField hasTarget;
   public float baseDamage = 50.0F;

   public RavenLordFeatherFollowingMob() {
      super(10);
      this.moveAccuracy = 15;
      this.setFriction(2.0F);
      this.collision = new Rectangle(-8, -8, 16, 16);
      this.hitBox = new Rectangle(-8, -8, 16, 16);
      this.selectBox = new Rectangle();
      this.hasTarget = this.registerNetworkField(new BooleanNetworkField(false));
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
         this.remove(0.0F, 0.0F, null, true);
      }
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PlayerFlyingFollowerValidTargetCollisionChaserAI<RavenLordFeatherFollowingMob>(448, null, 15, 500, 640, 64) {
         public boolean isValidTarget(RavenLordFeatherFollowingMob mob, ItemAttackerMob owner, Mob target) {
            if (owner == null) {
               return false;
            }

            Object result = GameUtils.castRayFirstHit(new Float(owner.x, owner.y, target.x, target.y), 100.0, line -> {
               CollisionFilter collisionFilter = mob.modifyChasingCollisionFilter(new CollisionFilter().projectileCollision(), target);
               return mob.getLevel().collides(line, collisionFilter) ? new Object() : null;
            });
            return result == null;
         }

         public AINodeResult tick(RavenLordFeatherFollowingMob mob, Blackboard<RavenLordFeatherFollowingMob> blackboard) {
            AINodeResult out = super.tick(mob, blackboard);
            Mob chaserTarget = blackboard.getObject(Mob.class, "chaserTarget");
            RavenLordFeatherFollowingMob.this.hasTarget.set(chaserTarget != null);
            return out;
         }
      }, new FlyingAIMover());
      if (this.isClient()) {
         this.trail = new Trail(this, this.getLevel(), new Color(231, 212, 243, 89), 14.0F, 1000, 0.0F);
         this.trail.drawOnTop = true;
         this.trail.removeOnFadeOut = false;
         this.getLevel().entityManager.addTrail(this.trail);
      }
   }

   @Override
   public void tickMovement(float delta) {
      if (this.getAttackOwner() != null) {
         float additionalSpeedBasedOnMovementSpeed = (this.getAttackOwner().buffManager.getModifier(BuffModifiers.SPEED) - 1.0F) * 100.0F / 2.0F;
         if (this.hasTarget.get()) {
            this.setSpeed(250.0F + additionalSpeedBasedOnMovementSpeed);
            this.moveAngle -= delta;
         } else {
            this.setSpeed(120.0F + additionalSpeedBasedOnMovementSpeed);
            this.moveAngle -= 0.1F * delta;
         }

         this.toMove += delta;

         while (this.toMove > 4.0F) {
            float oldX = this.x;
            float oldY = this.y;
            super.tickMovement(4.0F);
            this.toMove -= 4.0F;
            java.awt.geom.Point2D.Float d = GameMath.normalize(oldX - this.x, oldY - this.y);
            if (this.trail != null) {
               float trailOffset = 0.0F;
               this.trail.addPoint(new TrailVector(this.x + d.x * trailOffset, this.y + d.y * trailOffset, -d.x, -d.y, this.trail.thickness, 0.0F));
            }
         }
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      Mob attackOwner = this.getAttackOwner();
      if (attackOwner != null) {
         this.summonDamage = new GameDamage(DamageTypeRegistry.SUMMON, RavenlordsHeaddressSetBonusBuff.getFinalDamage(attackOwner, this.baseDamage));
         if (!attackOwner.buffManager.hasBuff(BuffRegistry.SetBonuses.RAVENLORDS_HEADDRESS)) {
            this.remove(0.0F, 0.0F, null, true);
         }
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 20; i++) {
         this.getLevel()
            .entityManager
            .addParticle(this.x, this.y, Particle.GType.COSMETIC)
            .movesConstantAngle(GameRandom.globalRandom.nextInt(360), GameRandom.globalRandom.getIntBetween(5, 20))
            .color(new Color(239, 235, 255));
      }
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return new SoundSettings(GameResources.spit).volume(0.5F);
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
      DrawOptions body = MobRegistry.Textures.ravenlords_set_feather
         .initDraw()
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .rotate(this.moveAngle, 16, 16)
         .pos(drawX, drawY);
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
