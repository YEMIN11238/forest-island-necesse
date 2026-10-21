package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.explosionEvent.splashEvent.LocustDeathSplashEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ability.BooleanMobAbility;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerFlyingFollowerCollisionChaserAI;
import necesse.entity.mobs.ai.behaviourTree.util.FlyingAIMover;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.ThemeColorRegistry;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class LocustFollowingMob extends FlyingAttackingFollowingMob {
   protected boolean isAngry;
   protected float angryProgress = 0.0F;
   protected Color bodyColor;
   protected Color angryColorMiddle;
   protected Color angryColorMax;
   protected final BooleanMobAbility toggleAngryAbility;
   protected Mob currentTarget;

   public LocustFollowingMob() {
      super(10);
      this.accelerationMod = 1.0F;
      this.moveAccuracy = 10;
      this.setSpeed(60.0F);
      this.setFriction(1.0F);
      this.collision = new Rectangle(-5, 2, 10, 16);
      this.hitBox = new Rectangle(-8, 4, 16, 14);
      this.selectBox = new Rectangle(-14, -14, 28, 36);
      this.bodyColor = Color.WHITE;
      this.angryColorMiddle = new Color(68, 211, 255);
      this.angryColorMax = new Color(27, 83, 255);
      this.toggleAngryAbility = this.registerAbility(new BooleanMobAbility() {
         @Override
         protected void run(boolean value) {
            LocustFollowingMob.this.toggleAngry(value);
         }
      });
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(
         this,
         new PlayerFlyingFollowerCollisionChaserAI<LocustFollowingMob>(576, null, 15, 500, 640, 100) {
            public AINodeResult tick(LocustFollowingMob mob, Blackboard<LocustFollowingMob> blackboard) {
               AINodeResult out = super.tick(mob, blackboard);
               Mob chaserTarget = blackboard.getObject(Mob.class, "chaserTarget");
               if (LocustFollowingMob.this.currentTarget != chaserTarget) {
                  LocustFollowingMob.this.currentTarget = chaserTarget;
                  LocustFollowingMob.this.toggleAngryAbility
                     .runAndSend(
                        LocustFollowingMob.this.currentTarget != null && LocustFollowingMob.this.currentTarget != LocustFollowingMob.this.getFollowingMob()
                     );
               }

               return out;
            }
         },
         new FlyingAIMover()
      );
      if (this.isClient()) {
         for (int i = 0; i < 20; i++) {
            this.getLevel()
               .entityManager
               .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
               .movesConstant(GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F), GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F))
               .color(ThemeColorRegistry.SAND.getRandomColor())
               .height(-10.0F);
         }

         for (int i = 0; i < 10; i++) {
            this.getLevel()
               .entityManager
               .addParticle(this.x, this.y, Particle.GType.IMPORTANT_COSMETIC)
               .movesConstant(GameRandom.globalRandom.getFloatBetween(-14.0F, 14.0F), GameRandom.globalRandom.getFloatBetween(-14.0F, 14.0F))
               .color(ThemeColorRegistry.SLIME.getRandomColor().darker())
               .height(-10.0F);
         }

         SoundManager.playSound(GameResources.blunthit, SoundEffect.effect(this).volume(0.05F).pitch(2.0F));
         SoundManager.playSound(GameResources.grass, SoundEffect.effect(this).volume(0.5F).pitch(1.0F));
      }
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return new GameDamage(0.0F);
   }

   @Override
   public int getCollisionKnockback(Mob target) {
      return 10;
   }

   @Override
   public int getFlyingHeight() {
      return Math.abs(this.dx) <= 0.01F && Math.abs(this.dy) <= 0.01F ? 0 : 20;
   }

   @Override
   public void handleCollisionHit(Mob target, GameDamage damage, int knockback) {
      LocustDeathSplashEvent event = new LocustDeathSplashEvent(
         this.x, this.y, 128, this.summonDamage, 0.0F, this.getAttackOwner(), this.angryProgress <= 0.5F ? this.angryColorMiddle : this.bodyColor
      );
      this.getLevel().entityManager.addLevelEvent(event);
      this.remove(0.0F, 0.0F, null, true);
   }

   public void toggleAngry(boolean isAngry) {
      this.isAngry = isAngry;
   }

   @Override
   public void serverTick() {
      super.serverTick();
      Mob attackOwner = this.getAttackOwner();
      if (attackOwner != null && !attackOwner.buffManager.hasBuff(BuffRegistry.SetBonuses.PHARAOH)) {
         this.remove(0.0F, 0.0F, null, true);
      }

      this.updateBodyColor();
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.updateBodyColor();
   }

   protected void updateBodyColor() {
      if (this.isAngry && this.angryProgress <= 1.0F) {
         this.angryProgress += 0.02F;
      } else if (this.angryProgress > 0.0F) {
         this.angryProgress -= 0.02F;
      }

      if (!(this.angryProgress <= 0.0F)) {
         this.bodyColor = this.getLerpedTripleColor(GameMath.limit(this.angryProgress, 0.0F, 1.0F), Color.white, this.angryColorMiddle, this.angryColorMax);
      }
   }

   protected boolean shouldBeIdle() {
      return this.almostZero(this.angryProgress) && this.almostZero(this.dx) && this.almostZero(this.dy);
   }

   protected boolean almostZero(float value) {
      return value <= 0.03F && value >= -0.03F;
   }

   protected Color getLerpedTripleColor(float lerpFloat, Color zero, Color half, Color full) {
      int r = GameMath.lerp(lerpFloat * 2.0F, zero.getRed(), half.getRed());
      int g = GameMath.lerp(lerpFloat * 2.0F, zero.getGreen(), half.getGreen());
      int b = GameMath.lerp(lerpFloat * 2.0F, zero.getBlue(), half.getBlue());
      if (lerpFloat > 0.5F) {
         r = GameMath.lerp(lerpFloat * 2.0F - 1.0F, half.getRed(), full.getRed());
         g = GameMath.lerp(lerpFloat * 2.0F - 1.0F, half.getGreen(), full.getGreen());
         b = GameMath.lerp(lerpFloat * 2.0F - 1.0F, half.getBlue(), full.getBlue());
      }

      return new Color(r, g, b);
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return !this.isMounted()
         ? new CollisionFilter().addFilter(tp -> !tp.object().object.isDoor).mobCollision().summonedMobCollision()
         : new CollisionFilter().mobCollision();
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.locust, i, 8, 32, this.x, this.y, 10.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
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
      GameLight light = level.getLightLevel(GameMath.getTileCoordinate(x), GameMath.getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 32 - 8;
      int dir = this.getDir();
      int time = (int)((float)(level.getWorldEntity().getTime() % 300L) / 75.0F);
      Point p = new Point(time + 1, dir);
      if (this.shouldBeIdle()) {
         p.x = 0;
      }

      float rotate = this.dx / 10.0F;
      final DrawOptions options = MobRegistry.Textures.locust
         .initDraw()
         .sprite(p.x, p.y, 64)
         .startGlowOptions(level, this.getID())
         .colorLight(this.bodyColor, light.minLevelCopy(150.0F * GameMath.limit(this.angryProgress, 0.2F, 1.0F)))
         .applyEnemyTracker(this, perspective)
         .rotate(rotate, 32, 32)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      int spriteCount = 4;
      int sprite = (int)(this.getTime() / 1000.0 * 11.0) % spriteCount;
      GameTexture shadowTexture = MobRegistry.Textures.mosquito_shadow;
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 32 - 8;
      return shadowTexture.initDraw().sprite(sprite + 2, this.getDir(), 64).light(light).pos(drawX, drawY);
   }
}
