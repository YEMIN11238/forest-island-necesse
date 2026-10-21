package necesse.entity.mobs.polymorph;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.gameSound.GameSound;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.PolymorphMob;
import necesse.entity.mobs.buffs.staticBuffs.Buff;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SquirrelPolymorphMob extends PolymorphMob {
   public SquirrelPolymorphMob() {
      this.setSpeed(55.0F);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-16, -28, 32, 34);
      this.swimMaskMove = 8;
      this.swimMaskOffset = 0;
      this.swimSinkOffset = 0;
   }

   @Override
   protected GameSound getMobSound() {
      return GameResources.squirrelAmbient;
   }

   @Override
   protected float getMobSoundVolume() {
      return 0.4F;
   }

   @Override
   protected Buff getPolymorphBuff() {
      return BuffRegistry.Debuffs.POLYMORPH_SQUIRREL;
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
      if (this.isMounted()) {
         GameLight light = level.getLightLevel(GameMath.getTileCoordinate(x), GameMath.getTileCoordinate(y));
         int drawX = camera.getDrawX(x) - 32;
         int drawY = camera.getDrawY(y) - 46 - 10;
         int dir = this.getDir();
         Point sprite = this.getAnimSprite(x, y, dir);
         drawY += this.getBobbing(x, y);
         drawY += this.getLevel().getTile(GameMath.getTileCoordinate(x), GameMath.getTileCoordinate(y)).getMobSinkingAmount(this);
         boolean mirror = (dir == 0 || dir == 2) && this.moveX < 0.0F;
         final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
         final DrawOptions options = MobRegistry.Textures.squirrel
            .initDraw()
            .sprite(sprite.x, sprite.y, 64)
            .addMaskShader(swimMask)
            .mirror(mirror, false)
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
         this.addShadowDrawables(tileList, level, x, y, light, camera);
      }
   }

   protected TextureDrawOptions getShadowDrawOptions(int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.small_shadow;
      int drawX = camera.getDrawX(x) - shadowTexture.getWidth() / 2;
      int drawY = camera.getDrawY(y) - shadowTexture.getHeight() / 2;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().light(light).pos(drawX, drawY);
   }
}
