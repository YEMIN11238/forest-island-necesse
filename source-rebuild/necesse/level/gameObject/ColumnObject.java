package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class ColumnObject extends GameObject {
   protected GameTexture texture;
   protected String textureName;
   protected boolean hasRotation;
   protected int yOffset = -3;

   public ColumnObject(String textureName, Color mapColor, ToolType toolType, int collisionWidth, int collisionHeight) {
      super(new Rectangle((32 - collisionWidth) / 2, (32 - collisionHeight) / 2, collisionWidth, collisionHeight));
      this.textureName = textureName;
      this.mapColor = mapColor;
      this.toolType = toolType;
      this.isLightTransparent = true;
      this.stackSize = 500;
      this.setItemCategory("objects", "columns");
      this.setCraftingCategory("objects", "columns");
      this.replaceCategories.add("column");
      this.canReplaceCategories.add("furniture");
      this.canReplaceCategories.add("column");
      this.canReplaceCategories.add("torch");
      this.replaceRotations = false;
      this.hoverHitbox = new Rectangle(0, -32, 32, 64);
   }

   public ColumnObject(String textureName, Color mapColor, ToolType toolType) {
      this(textureName, mapColor, toolType, 28, 22);
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
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
      int drawY = camera.getTileDrawY(tileY) - this.texture.getHeight() + 32;
      final TextureDrawOptions drawOptions;
      if (this.hasRotation) {
         byte rotation = level.getObjectRotation(tileX, tileY);
         int spriteWidth = this.texture.getWidth() / 4;
         int xOffset = (spriteWidth - 32) / 2;
         drawOptions = this.texture
            .initDraw()
            .sprite(rotation % 4, 0, spriteWidth, this.texture.getHeight())
            .addObjectDamageOverlay(this, level, tileX, tileY)
            .light(light)
            .pos(drawX - xOffset, drawY + this.yOffset);
      } else {
         int xOffset = (this.texture.getWidth() - 32) / 2;
         drawOptions = this.texture.initDraw().addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX - xOffset, drawY + this.yOffset);
      }

      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY) - this.texture.getHeight() + 32;
      if (this.hasRotation) {
         int spriteWidth = this.texture.getWidth() / 4;
         int xOffset = (spriteWidth - 32) / 2;
         this.texture.initDraw().sprite(rotation % 4, 0, spriteWidth, this.texture.getHeight()).alpha(alpha).draw(drawX - xOffset, drawY + this.yOffset);
      } else {
         int xOffset = (this.texture.getWidth() - 32) / 2;
         this.texture.initDraw().alpha(alpha).draw(drawX - xOffset, drawY + this.yOffset);
      }
   }
}
