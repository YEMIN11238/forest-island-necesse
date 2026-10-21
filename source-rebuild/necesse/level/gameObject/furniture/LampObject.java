package necesse.level.gameObject.furniture;

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
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.objectItem.TorchObjectItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class LampObject extends FurnitureObject {
   protected String textureName;
   public GameTexture texture;
   public GameTexture texture_off;

   public LampObject(String textureName, Rectangle collision, ToolType toolType, Color mapColor, float lightHue, float lightSat) {
      super(collision);
      this.textureName = textureName;
      this.toolType = toolType;
      this.mapColor = mapColor;
      this.objectHealth = 50;
      this.stackSize = 500;
      this.lightLevel = 150;
      this.lightHue = lightHue;
      this.lightSat = lightSat;
      this.isLightTransparent = true;
      this.setItemCategory("objects", "lighting");
      this.setCraftingCategory("objects", "lighting");
      this.roomProperties.add("lights");
      this.furnitureType = "lamp";
   }

   public LampObject(String textureName, Rectangle collision, Color mapColor, float lightHue, float lightSat) {
      this(textureName, collision, ToolType.ALL, mapColor, lightHue, lightSat);
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
      this.texture_off = GameTexture.fromFile("objects/" + this.textureName + "_off");
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
      GameTexture texture = this.isActive(level, tileX, tileY) ? this.texture : this.texture_off;
      int rotation = level.getObjectRotation(tileX, tileY) % (texture.getWidth() / 32);
      final TextureDrawOptions options = texture.initDraw()
         .sprite(rotation, 0, 32, texture.getHeight())
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - texture.getHeight() + 32);
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
      int var10 = (byte)(rotation % (this.texture.getWidth() / 32));
      this.texture.initDraw().sprite(var10, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - this.texture.getHeight() + 32);
   }

   @Override
   public Item generateNewObjectItem() {
      return new TorchObjectItem(this);
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
