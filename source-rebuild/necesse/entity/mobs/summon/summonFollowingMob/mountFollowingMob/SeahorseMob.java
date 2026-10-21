package necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.PlayerFollowerAINode;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SeahorseMob extends MountFollowingJumpingMob {
   private double waterSoundTimer;

   public SeahorseMob() {
      super(50);
      this.setSpeed(20.0F);
      this.setFriction(3.0F);
      this.setSwimSpeed(5.0F);
      this.setJumpStrength(100.0F);
      this.setJumpCooldown(100);
      this.setJumpAnimationTime(400);
      this.moveAccuracy = 10;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-16, -12, 32, 24);
      this.selectBox = new Rectangle(-18, -83, 36, 90);
      this.swimMaskMove = 32;
      this.swimMaskOffset = -40;
      this.swimSinkOffset = -6;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PlayerFollowerAINode<>(480, 64));
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
      int jumpHeight = this.getJumpHeight();
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions back = MobRegistry.Textures.seahorse_back
         .initDraw()
         .sprite(sprite.x, sprite.y, 64, 96)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY - jumpHeight);
      final DrawOptions front = MobRegistry.Textures.seahorse_front
         .initDraw()
         .sprite(sprite.x, sprite.y, 64, 96)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY - jumpHeight);
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
            back.draw();
            swimMask.stop();
         }
      });
      TextureDrawOptions shadow = MobRegistry.Textures.seahorse_shadow.initDraw().sprite(sprite.x, sprite.y, 64, 96).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
   }

   public int getJumpHeight() {
      int jumpFrames = 5;
      int jumpFrame = this.getJumpAnimationFrame(jumpFrames);
      double sin = GameMath.sin((float)jumpFrame / jumpFrames * 180.0F);
      return (int)(sin * 20.0);
   }

   @Override
   public int getRockSpeed() {
      return 20;
   }

   @Override
   public Point getAnimSprite(int x, int y, int dir) {
      Point p = new Point(0, dir);
      if (this.dx == 0.0F & this.dy == 0.0F) {
         p.x = 0;
      } else {
         p.x = (int)(this.getDistanceRan() / this.getRockSpeed()) % 4 + 1;
      }

      return p;
   }

   @Override
   public Point getSpriteOffset(int spriteX, int spriteY) {
      Point p = new Point(0, 0);
      if (spriteX == 2) {
         p.y = 2;
      }

      p.x = p.x + this.getRiderDrawXOffset();
      p.y = p.y + this.getRiderDrawYOffset();
      if (spriteY == 0) {
         p.y += 5;
      }

      return p;
   }

   @Override
   public int getRiderDrawYOffset() {
      return this.getSwimMaskShaderOptions(this.inLiquidFloat(this.getDrawX(), this.getDrawY())).drawYOffset - 38 - this.getJumpHeight();
   }

   @Override
   public int getRiderDrawXOffset() {
      return super.getRiderDrawXOffset();
   }

   @Override
   public GameTexture getRiderMask() {
      return MobRegistry.Textures.mountmask;
   }

   @Override
   public int getRiderMaskYOffset() {
      return -8;
   }

   @Override
   public int getSwimMaskOffset() {
      return super.getSwimMaskOffset() - this.getJumpHeight();
   }

   @Override
   public float getSwimSpeedModifier() {
      return this.inLiquid() ? this.getSwimSpeed() : 1.0F;
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
   public void tickMovement(float delta) {
      super.tickMovement(delta);
      this.waterSoundTimer = this.waterSoundTimer + delta * Math.max(0.2F, this.getCurrentSpeed() / 30.0F);
      if (this.waterSoundTimer >= 600.0 && this.getCurrentSpeed() >= 5.0F) {
         this.waterSoundTimer = 0.0;
         if (this.inLiquid()) {
            SoundManager.playSound(new SoundSettings(GameResources.waterblob).volume(0.1F), this);
         }
      }
   }

   @Override
   public void onJump() {
      if (!this.isClient()) {
         SoundManager.playSound(new SoundSettings(GameResources.slimeSplash1).volume(0.2F), this);
         if (this.inLiquidFloat() > 0.0F) {
            SoundManager.playSound(new SoundSettings(GameResources.watersplash).volume(0.1F), this);
         }
      }
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.seahorseAmbient).volume(0.4F);
   }
}
