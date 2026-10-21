package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class FuelSkullEncasingObject extends GameObject {
   protected String textureName;
   public GameTexture texture;
   public GameTexture lightTexture;

   public FuelSkullEncasingObject(String textureName, Color mapColor) {
      super(new Rectangle(0, -14, 32, 46));
      this.textureName = textureName;
      this.mapColor = mapColor;
      this.isIncursionExtractionObject = true;
      this.isOre = true;
      this.displayMapTooltip = true;
      this.isLightTransparent = true;
      this.canPlaceOnLiquid = true;
      this.setItemCategory("objects", "landscaping", "incursionrocksandores");
      this.setCraftingCategory("objects", "landscaping", "incursionrocksandores");
   }

   @Override
   public GameMessage getNewLocalization() {
      return new LocalMessage("object", this.textureName);
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
      this.lightTexture = GameTexture.fromFile("objects/" + this.textureName + "_light");
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return level.objectLayer.isPlayerPlaced(tileX, tileY)
         ? super.getLootTable(level, layerID, tileX, tileY)
         : new LootTable(LootItem.between("fuelskull", 1, 2));
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
      final TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(0, 0, 32, this.texture.getHeight())
         .light(light)
         .pos(drawX, drawY - this.texture.getHeight() + 32);
      final TextureDrawOptions lightOptions = this.lightTexture
         .initDraw()
         .sprite(0, 0, 32, this.lightTexture.getHeight())
         .pos(drawX, drawY - this.texture.getHeight() + 32);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            options.draw();
            lightOptions.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - this.texture.getHeight() + 32);
   }
}
