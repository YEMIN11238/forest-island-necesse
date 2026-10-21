package necesse.level.gameObject;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.gameTile.SpiritWaterTile;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class DryadTreeObject extends TreeObject {
   public DryadTreeObject(
      String textureName,
      String logStringID,
      String saplingStringID,
      Color mapColor,
      int leavesCenterWidth,
      int leavesMinHeight,
      int leavesMaxHeight,
      String leavesTextureName
   ) {
      super(textureName, logStringID, saplingStringID, mapColor, leavesCenterWidth, leavesMinHeight, leavesMaxHeight, leavesTextureName);
      this.canPlaceOnLiquid = true;
      this.canPlaceOnShore = true;
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      super.addDrawables(list, tileList, level, tileX, tileY, tickManager, camera, perspective);
      final float alpha = SpiritWaterTile.getSpiritCorruptedFadeProgress(level);
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int spriteRes = 128;
      int spriteX = 2;
      int spritesHeight = this.texture.getHeight() / spriteRes;
      int spriteY = 0;
      boolean mirror;
      synchronized (this.drawRandom) {
         this.drawRandom.setSeed(getTileSeed(tileX, tileY));
         if (spritesHeight > 1) {
            spriteY = this.drawRandom.nextInt(spritesHeight);
         }

         mirror = this.drawRandom.nextBoolean();
      }

      final TextureDrawOptions spiritEyesOverlay = this.texture
         .initDraw()
         .sprite(spriteX, spriteY, spriteRes)
         .light(light)
         .alpha(alpha)
         .colorLight(new Color(159, 222, 201), new GameLight(alpha * 255.0F))
         .mirror(mirror, false)
         .pos(drawX - 48, drawY - 96);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            if (alpha > 0.0F) {
               spiritEyesOverlay.draw();
            }
         }
      });
   }
}
