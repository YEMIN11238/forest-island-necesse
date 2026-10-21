package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class BloodStainsObject extends GameObject {
   protected String textureName;
   GameTexture texture;
   protected int yOffset = -3;
   protected final GameRandom drawRandom;

   public BloodStainsObject(String textureName, Color mapColor, ToolType toolType, int collisionWidth, int collisionHeight) {
      super(new Rectangle((32 - collisionWidth) / 2, (32 - collisionHeight) / 2, collisionWidth, collisionHeight));
      this.textureName = textureName;
      this.mapColor = mapColor;
      this.toolType = toolType;
      this.isLightTransparent = true;
      this.stackSize = 500;
      this.drawRandom = new GameRandom();
      this.replaceRotations = false;
      this.hoverHitbox = new Rectangle(0, 0, 32, 32);
      this.setItemCategory("objects", "misc");
   }

   public BloodStainsObject(String textureName, Color mapColor, ToolType toolType) {
      this(textureName, mapColor, toolType, 0, 0);
   }

   @Override
   public void loadTextures() {
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
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
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int spriteRes = 32;
      int spritesWidth = this.texture.getWidth() / spriteRes;
      int spriteX = 0;
      boolean mirror;
      synchronized (this.drawRandom) {
         this.drawRandom.setSeed(getTileSeed(tileX, tileY));
         if (spritesWidth > 1) {
            spriteX = this.drawRandom.nextInt(spritesWidth);
         }

         mirror = this.drawRandom.nextBoolean();
      }

      final DrawOptions options = this.texture
         .initDraw()
         .section(spriteX * spriteRes, spriteX * spriteRes + spriteRes, 0, 32)
         .light(light)
         .mirror(mirror, false)
         .pos(drawX, drawY);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 0;
         }

         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int spriteRes = 32;
      int spritesWidth = this.texture.getWidth() / spriteRes;
      int spriteX = 0;
      boolean mirror;
      synchronized (this.drawRandom) {
         this.drawRandom.setSeed(getTileSeed(tileX, tileY));
         if (spritesWidth > 1) {
            spriteX = this.drawRandom.nextInt(spritesWidth);
         }

         mirror = this.drawRandom.nextBoolean();
      }

      this.texture.initDraw().sprite(spriteX, 0, spriteRes).light(light).mirror(mirror, false).alpha(alpha).draw(drawX, drawY);
   }
}
