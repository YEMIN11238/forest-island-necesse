package necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.PlayerFollowerAINode;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class TameOstrichMob extends MountFollowingMob {
   public TameOstrichMob() {
      super(50);
      this.setSpeed(65.0F);
      this.setFriction(3.0F);
      this.setSwimSpeed(0.4F);
      this.moveAccuracy = 10;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-16, -12, 32, 24);
      this.selectBox = new Rectangle(-18, -83, 36, 90);
      this.swimMaskMove = 32;
      this.swimMaskOffset = -32;
      this.swimSinkOffset = -2;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PlayerFollowerAINode<>(480, 64));
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(), MobRegistry.Textures.ostrich, GameRandom.globalRandom.nextInt(5), 12, 32, this.x, this.y, 10.0F, knockbackX, knockbackY
               ),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   public void addDrawables(
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
      int drawX = camera.getDrawX(x) - 22 - 10;
      int drawY = camera.getDrawY(y) - 44 - 32 - 11;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions behind = MobRegistry.Textures.ostrich
         .initDraw()
         .sprite(sprite.x, sprite.y, 64, 96)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      final DrawOptions front;
      if (!this.isMounted()) {
         front = () -> {};
      } else {
         front = MobRegistry.Textures.ostrichMount
            .initDraw()
            .sprite(sprite.x, sprite.y, 64, 96)
            .addMaskShader(swimMask)
            .startGlowOptions(level, this.getID())
            .light(light)
            .applyEnemyTracker(this, perspective)
            .pos(drawX, drawY);
      }

      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            front.draw();
            swimMask.stop();
         }

         @Override
         public void drawBehindRider(TickManager tickManager) {
            swimMask.use();
            behind.draw();
            swimMask.stop();
         }
      });
      TextureDrawOptions shadow = MobRegistry.Textures.ostrich_shadow.initDraw().sprite(sprite.x, sprite.y, 64, 96).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public int getRockSpeed() {
      return 20;
   }

   @Override
   public Point getSpriteOffset(int spriteX, int spriteY) {
      Point p = new Point(0, 0);
      if (spriteX == 1 || spriteX == 2) {
         p.y = 2;
      }

      p.x = p.x + this.getRiderDrawXOffset();
      p.y = p.y + this.getRiderDrawYOffset();
      return p;
   }

   @Override
   public int getRiderDrawYOffset() {
      return this.getSwimMaskShaderOptions(this.inLiquidFloat(this.getDrawX(), this.getDrawY())).drawYOffset - 38;
   }

   @Override
   public GameTexture getRiderMask() {
      return MobRegistry.Textures.mountmask;
   }

   @Override
   protected void doMountedLogic() {
      if (!this.isServer()) {
         int particleCount = 40;

         for (int i = 0; i < particleCount; i++) {
            this.getLevel()
               .entityManager
               .addParticle(
                  this.x + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
                  this.y + 16.0F + (float)(GameRandom.globalRandom.nextGaussian() * 8.0),
                  Particle.GType.IMPORTANT_COSMETIC
               )
               .sprite(GameResources.smokePuff.sprite(0, 0, 32))
               .lifeTime(750)
               .fadesAlphaTime(100, 250)
               .movesFriction(16.0F * (float)GameRandom.globalRandom.nextGaussian(), 5.0F * (float)GameRandom.globalRandom.nextGaussian(), 1.0F)
               .sizeFades(14, 18)
               .heightMoves(20.0F, 64.0F);
         }
      }
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.ostrichAmbient).volume(1.1F);
   }
}
