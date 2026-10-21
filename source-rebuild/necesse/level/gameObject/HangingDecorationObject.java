package necesse.level.gameObject;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.StaticMultiTile;

public class HangingDecorationObject extends GameObject {
   public String textureName;
   public GameTexture texture;
   public GameMessage customLocalization;

   public HangingDecorationObject(String textureName, Color mapColor) {
      this.textureName = textureName;
      this.drawDamage = false;
      this.objectHealth = 1;
      this.toolType = ToolType.ALL;
      this.validObjectLayers.add(ObjectLayerRegistry.WALL_DECOR);
      this.mapColor = mapColor;
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "landscaping", "misc");
      this.replaceCategories.add("hangingdecoration");
      this.canReplaceCategories.add("hangingdecoration");
   }

   public HangingDecorationObject setCustomLocalization(GameMessage name) {
      this.customLocalization = name;
      return this;
   }

   public HangingDecorationObject setCustomLocalization(String category, String key) {
      return this.setCustomLocalization(new LocalMessage(category, key));
   }

   public HangingDecorationObject setCustomLocalization(String key) {
      return this.setCustomLocalization("object", key);
   }

   @Override
   public GameMessage getNewLocalization() {
      return this.customLocalization != null ? this.customLocalization : super.getNewLocalization();
   }

   @Override
   public MultiTile getMultiTile(int rotation) {
      return new StaticMultiTile(0, 0, 1, 1, true, this.getID());
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
   }

   @Override
   public void addLayerDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int layerID,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      final TextureDrawOptionsEnd options = this.texture
         .initDraw()
         .sprite(0, 0, 32, 32)
         .addObjectDamageOverlay(this, level, layerID, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - 32);
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
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 0, 32, 32).alpha(alpha).draw(drawX, drawY - 32);
   }

   @Override
   protected ObjectHoverHitbox getHoverHitbox(Level level, int layerID, int tileX, int tileY) {
      return new ObjectHoverHitbox(layerID, tileX, tileY, 0, -16, 32, 48, 0);
   }

   public boolean attachesToObject(Level level, int tileX, int tileY) {
      return level.getObject(tileX, tileY).isWall && !level.getObject(tileX, tileY).isDoor;
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      if (this.isTilePlaceOccupied(level, layerID, x, y, ignoreOtherLayers)) {
         return "occupied";
      }

      if (layerID != 0) {
         GameObject object = level.getObject(0, x, y);
         if (object.isWall && !object.isDoor || object.isRock) {
            return "tilecovered";
         }
      }

      return this.attachesToObject(level, x, y - 1) ? null : "nowall";
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      int rotation = level.getObjectRotation(layerID, x, y);
      if (layerID != 0) {
         GameObject object = level.getObject(0, x, y);
         if (object.isWall && !object.isDoor || object.isRock) {
            return false;
         }
      }

      return this.attachesToObject(level, x, y - 1);
   }
}
