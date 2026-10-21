package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Line2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
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
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerFlyingFollowerValidTargetCollisionChaserAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class DryadSpiritFollowingMob extends FlyingAttackingFollowingMob {
   public float moveAngle;
   private float toMove;
   protected int deathTime = 4000;
   protected int lifeTime = 0;

   public DryadSpiritFollowingMob() {
      super(10);
      this.moveAccuracy = 15;
      this.setSpeed(160.0F);
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
         if (!target.isBoss()) {
            ActiveBuff ab = new ActiveBuff(BuffRegistry.Debuffs.DRYAD_POSSESSED, target, 3000, owner);
            target.buffManager.addBuff(ab, true);
         }

         this.remove(0.0F, 0.0F, null, true);
      }
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PlayerFlyingFollowerValidTargetCollisionChaserAI<DryadSpiritFollowingMob>(448, null, 15, 500, 640, 64) {
         public boolean isValidTarget(DryadSpiritFollowingMob mob, ItemAttackerMob owner, Mob target) {
            if (owner == null) {
               return false;
            }

            Object result = GameUtils.castRayFirstHit(new Float(owner.x, owner.y, target.x, target.y), 100.0, line -> {
               CollisionFilter collisionFilter = mob.modifyChasingCollisionFilter(new CollisionFilter().projectileCollision(), target);
               return mob.getLevel().collides(line, collisionFilter) ? new Object() : null;
            });
            return result == null;
         }
      }, new FlyingAIMover());
   }

   @Override
   public void tickMovement(float delta) {
      this.toMove += delta;

      while (this.toMove > 4.0F) {
         float oldX = this.x;
         float oldY = this.y;
         super.tickMovement(4.0F);
         this.toMove -= 4.0F;
         java.awt.geom.Point2D.Float d = GameMath.normalize(oldX - this.x, oldY - this.y);
         this.moveAngle = (float)Math.toDegrees(Math.atan2(d.y, d.x)) - 90.0F;
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
         .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
         .movesConstant(this.dx / 10.0F, this.dy / 10.0F)
         .color(new Color(30, 177, 143))
         .sizeFades(12, 24);
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.lifeTime += 50;
      if (this.lifeTime >= this.deathTime) {
         this.remove(0.0F, 0.0F, null, true);
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      GameRandom random = GameRandom.globalRandom;
      float anglePerParticle = 36.0F;

      for (int i = 0; i < 10; i++) {
         int angle = (int)(i * anglePerParticle + random.nextFloat() * anglePerParticle);
         float dx = (float)Math.sin(Math.toRadians(angle)) * 20.0F;
         float dy = (float)Math.cos(Math.toRadians(angle)) * 20.0F;
         this.getLevel()
            .entityManager
            .addParticle(this, Particle.GType.IMPORTANT_COSMETIC)
            .sprite(GameResources.puffParticles.sprite(random.nextInt(5), 0, 12))
            .sizeFades(12, 24)
            .movesFriction(dx * 2.0F, dy * 2.0F, 0.8F)
            .color(new Color(30, 177, 143))
            .heightMoves(0.0F, 30.0F)
            .lifeTime(1500);
      }
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return new SoundSettings(GameResources.fadedeath3).volume(0.5F).basePitch(1.5F);
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
      int drawX = camera.getDrawX(x) - 18;
      int drawY = camera.getDrawY(y) - 18;
      drawY += this.getBobbing(x, y);
      int timePerFrame = 150;
      int spriteIndex = (int)(this.getWorldEntity().getTime() / timePerFrame) % 4;
      final DrawOptions drawOptions = MobRegistry.Textures.dryadSpirit
         .initDraw()
         .sprite(spriteIndex, 0, 36)
         .startGlowOptions(level, this.getID())
         .light(light.minLevelCopy(100.0F))
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.human_baby_shadow;
      int res = shadowTexture.getHeight();
      int drawX = camera.getDrawX(x) - res / 2;
      int drawY = camera.getDrawY(y) - res / 2 + 18;
      drawY += this.getBobbing(x, y);
      int dir = this.getDir();
      return shadowTexture.initDraw().sprite(dir, 0, res).light(light).pos(drawX, drawY);
   }
}
