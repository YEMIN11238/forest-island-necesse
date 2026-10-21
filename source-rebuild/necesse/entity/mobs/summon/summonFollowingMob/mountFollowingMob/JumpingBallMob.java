package necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class JumpingBallMob extends MountFollowingJumpingMob {
   private static final int[] frameOffsets = new int[]{0, -8, -18, -30, -18, -8};

   public JumpingBallMob() {
      super(50);
      this.setSpeed(20.0F);
      this.setFriction(2.0F);
      this.setJumpStrength(100.0F);
      this.setJumpCooldown(100);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-16, -24, 32, 32);
      this.swimMaskMove = 8;
      this.swimMaskOffset = -8;
      this.swimSinkOffset = 0;
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
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 54;
      final int dir = this.getDir();
      boolean inLiquid = this.inLiquid(x, y);
      int handleOffset = 0;
      int spriteX;
      if (inLiquid) {
         spriteX = 0;
      } else {
         spriteX = this.getJumpAnimationFrame(6);
         handleOffset = frameOffsets[spriteX];
      }

      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions body = MobRegistry.Textures.jumpingBall
         .initDraw()
         .sprite(spriteX, inLiquid ? 1 : 0, 64)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      final DrawOptions handle = MobRegistry.Textures.jumpingBall
         .initDraw()
         .sprite(2 + dir, 1, 64)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX + swimMask.drawXOffset, drawY + handleOffset + swimMask.drawYOffset);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            if (dir != 0) {
               handle.draw();
            }
         }

         @Override
         public void drawBehindRider(TickManager tickManager) {
            if (dir == 0) {
               handle.draw();
            }

            swimMask.use();
            body.draw();
            swimMask.stop();
         }
      });
      TextureDrawOptions shadow = MobRegistry.Textures.jumpingBall_shadow.initDraw().sprite(spriteX, 0, 64).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public Point getSpriteOffset(int spriteX, int spriteY) {
      return new Point(this.getRiderDrawXOffset(), this.getRiderDrawYOffset());
   }

   @Override
   public int getRiderMaskYOffset() {
      return -6;
   }

   @Override
   public int getRiderDrawYOffset() {
      return this.inLiquid() ? -2 : frameOffsets[this.getJumpAnimationFrame(6)] - 8;
   }

   @Override
   public GameTexture getRiderMask() {
      return MobRegistry.Textures.mountmask;
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return null;
   }

   @Override
   public void onJump() {
      if (this.isClient()) {
         SoundManager.playSound(new SoundSettings(GameResources.bounce).volume(0.5F).basePitch(1.3F).pitchVariance(0.1F), this);
      }
   }
}
