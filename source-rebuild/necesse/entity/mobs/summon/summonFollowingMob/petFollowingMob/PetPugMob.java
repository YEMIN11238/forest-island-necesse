package necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.PlayerFollowerAINode;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class PetPugMob extends PetFollowingMob {
   public long nextPetTime;

   public PetPugMob() {
      super(10);
      this.setSpeed(80.0F);
      this.setFriction(2.0F);
      this.moveAccuracy = 10;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-18, -24, 36, 36);
      this.swimMaskMove = 14;
      this.swimMaskOffset = 0;
      this.swimSinkOffset = -6;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PlayerFollowerAINode<>(480, 64));
   }

   @Override
   public void playDeathSound() {
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.pug, i, 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 48 - 7;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += this.getLevel().getTile(x / 16, y / 16).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.pug
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            options.draw();
            swimMask.stop();
         }
      });
      TextureDrawOptions shadow = MobRegistry.Textures.pug_shadow.initDraw().sprite(0, sprite.y, 64).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }

   @Override
   public void interact(PlayerMob player) {
      super.interact(player);
      if (!this.isServer()) {
         this.nextPetTime = this.getTime() + 2000L;

         for (int i = 0; i < 10; i++) {
            float posX = this.x + GameRandom.globalRandom.getIntBetween(-10, 10);
            float posY = this.y + GameRandom.globalRandom.getIntBetween(-5, 5);
            int startHeight = 8 + GameRandom.globalRandom.nextInt(24);
            int lifeTime = GameRandom.globalRandom.getIntBetween(4500, 5500);
            final int swing = GameRandom.globalRandom.getIntBetween(3, 7);
            final float swingOffset = GameRandom.globalRandom.nextFloat();
            float moveX = GameRandom.globalRandom.floatGaussian() * 0.5F;
            float moveY = GameRandom.globalRandom.floatGaussian() * 1.5F;
            final ParticleOption.FrictionMover frictionMover = new ParticleOption.FrictionMover(moveX, moveY, 0.0F);
            this.getLevel()
               .entityManager
               .addParticle(posX, posY, Particle.GType.COSMETIC)
               .sprite(GameResources.heartParticle)
               .dontRotate()
               .color(new Color(255, 255, 255))
               .heightMoves(startHeight, startHeight + 70)
               .moves(new ParticleOption.Mover() {
                  @Override
                  public void tick(Float pos, float delta, int lifeTimex, int timeAlive, float lifePercent) {
                     frictionMover.tick(pos, delta, lifeTimex, timeAlive, lifePercent);
                     float angle = (lifePercent + swingOffset) * 500.0F;
                     pos.x = pos.x + GameMath.sin(angle) * swing * delta / 250.0F;
                  }
               })
               .lifeTime(lifeTime);
         }
      }
   }

   @Override
   public boolean canInteract(Mob mob) {
      return this.nextPetTime <= this.getTime();
   }

   @Override
   protected String getInteractTip(PlayerMob perspective, boolean debug) {
      return Localization.translate("controls", "pettip");
   }
}
