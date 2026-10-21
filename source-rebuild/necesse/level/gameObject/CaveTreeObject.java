package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.function.Consumer;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.TileRegistry;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsPositionMod;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class CaveTreeObject extends TreeObject {
   public CaveTreeObject(
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
      Performance.record(
         tickManager,
         "treeSetup",
         () -> {
            GameLight light = level.getLightLevel(tileX, tileY);
            int drawX = camera.getTileDrawX(tileX);
            int drawY = camera.getTileDrawY(tileY);
            float alpha = 1.0F;
            if (perspective != null && !Settings.hideUI && !Settings.hideCursor) {
               Rectangle alphaRec = new Rectangle(tileX * 32 - 48, tileY * 32 - 100, 128, 100);
               if (perspective.getCollision().intersects(alphaRec)) {
                  alpha = 0.5F;
               } else if (alphaRec.contains(
                  camera.getX() + WindowManager.getWindow().mousePos().sceneX, camera.getY() + WindowManager.getWindow().mousePos().sceneY
               )) {
                  alpha = 0.5F;
               }
            }

            int spriteRes = 128;
            int spriteX = 0;
            if (this.texture.getWidth() > spriteRes && level.getTileID(tileX, tileY) == TileRegistry.snowID) {
               spriteX = 1;
            } else if (this.texture.getWidth() > spriteRes * 2 && level.getTileID(tileX, tileY) == TileRegistry.basaltRockID) {
               spriteX = 2;
            }

            boolean mirror;
            synchronized (this.drawRandom) {
               this.drawRandom.setSeed(getTileSeed(tileX, tileY));
               mirror = this.drawRandom.nextBoolean();
            }

            int spriteY = this.getTreeSpriteY(level, tileX, tileY, spriteRes);
            Consumer<TextureDrawOptionsPositionMod> waveChange = GameResources.waveShader
               .setupGrassWaveMod(level, tileX, tileY, this.weaveTime, this.weaveAmount, 2, this.drawRandom, getTileSeed(tileX, tileY, 0), mirror, 3.0F);
            final TextureDrawOptions options = this.texture
               .initDraw()
               .sprite(spriteX, spriteY, spriteRes)
               .alpha(alpha)
               .light(light)
               .mirror(mirror, false)
               .addPositionMod(waveChange)
               .pos(drawX - 48, drawY - 96);
            list.add(new LevelSortedDrawable(this, tileX, tileY) {
               @Override
               public int getSortY() {
                  return 16;
               }

               @Override
               public void draw(TickManager tickManager) {
                  Performance.record(tickManager, "treeDraw", () -> options.draw());
               }
            });
         }
      );
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int spriteRes = 128;
      int spriteX = 0;
      if (this.texture.getWidth() > spriteRes && level.getTileID(tileX, tileY) == TileRegistry.snowID) {
         spriteX = 1;
      } else if (this.texture.getWidth() > spriteRes * 2 && level.getTileID(tileX, tileY) == TileRegistry.basaltRockID) {
         spriteX = 2;
      }

      boolean mirror;
      synchronized (this.drawRandom) {
         this.drawRandom.setSeed(getTileSeed(tileX, tileY));
         mirror = this.drawRandom.nextBoolean();
      }

      int spriteY = this.getTreeSpriteY(level, tileX, tileY, spriteRes);
      this.texture.initDraw().sprite(spriteX, spriteY, spriteRes).alpha(alpha).light(light).mirror(mirror, false).draw(drawX - 48, drawY - 96);
   }
}
