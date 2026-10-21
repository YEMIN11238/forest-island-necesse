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

public class StreetlampObject extends GameObject {
   public GameTexture texture;

   public StreetlampObject() {
      super(new Rectangle(11, 11, 10, 10));
      this.mapColor = new Color(255, 255, 152);
      this.displayMapTooltip = true;
      this.lightLevel = 200;
      this.objectHealth = 1;
      this.stackSize = 500;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.setItemCategory("objects", "lighting");
      this.setCraftingCategory("objects", "lighting");
      this.roomProperties.add("lights");
      this.canPlaceOnShore = true;
      this.replaceCategories.add("torch");
      this.canReplaceCategories.add("torch");
      this.canReplaceCategories.add("furniture");
      this.canReplaceCategories.add("column");
      this.replaceRotations = false;
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.getStringID());
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
      boolean active = this.isActive(level, tileX, tileY);
      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(0, active ? 0 : 1, 32, 96)
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - this.texture.getHeight() / 2 + 32);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 0, 32, this.texture.getHeight() / 2).alpha(alpha).draw(drawX, drawY - this.texture.getHeight() / 2 + 32);
   }

   @Override
   public int getLightLevel(Level level, int layerID, int tileX, int tileY) {
      return this.isActive(level, tileX, tileY) ? this.lightLevel : 0;
   }

   public boolean isActive(Level level, int x, int y) {
      byte rotation = level.getObjectRotation(x, y);
      return this.getMultiTile(rotation).streamIDs(x, y).noneMatch(c -> level.wireManager.isWireActiveAny(c.tileX, c.tileY));
   }

   @Override
   public void onWireUpdate(Level level, int layerID, int tileX, int tileY, int wireID, boolean active) {
      byte rotation = level.getObjectRotation(tileX, tileY);
      Rectangle rect = this.getMultiTile(rotation).getTileRectangle(tileX, tileY);
      level.lightManager.updateStaticLight(rect.x, rect.y, rect.x + rect.width - 1, rect.y + rect.height - 1, true);
   }
}
