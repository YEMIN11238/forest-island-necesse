package necesse.level.gameObject;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class WaterLanternObject extends TorchObject {
   protected final GameRandom drawRandom = new GameRandom();

   public WaterLanternObject() {
      super("waterlantern", ToolType.ALL, new Color(255, 255, 152), 50.0F, 0.2F, false, 0);
      this.canPlaceOnLiquid = true;
      this.canPlaceOnShore = true;
      this.lightLevel = 150;
      this.lightSat = 0.75F;
      this.lightHue = 30.0F;
      this.replaceRotations = false;
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(Localization.translate("itemtooltip", "waterplacetip"));
      tooltips.add(Localization.translate("controls", "torchplacetip"));
      return tooltips;
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      return !level.isLiquidTile(x, y) ? "notinwater" : super.canPlace(level, layerID, x, y, rotation, byPlayer, ignoreOtherLayers);
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      return !level.isLiquidTile(x, y) ? false : super.isValid(level, layerID, x, y);
   }

   public float getDesiredHeight(Level level, int tileX, int tileY) {
      synchronized (this.drawRandom) {
         int seededOffset = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(3000);
         float perc = GameUtils.getAnimFloat(level.getWorldEntity().getTime() + seededOffset, 3000);
         return GameMath.sin(perc * 360.0F) * 2.0F;
      }
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
      GameTexture texture = this.isActive(level, layerID, tileX, tileY) ? this.texture : this.texture_off;
      int rotation = level.getObjectRotation(layerID, tileX, tileY) % (texture.getWidth() / 32);
      int height = (int)this.getDesiredHeight(level, tileX, tileY);
      final TextureDrawOptions options = texture.initDraw().sprite(rotation, 0, 32, texture.getHeight()).light(light).pos(drawX, drawY - height);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            Performance.record(tickManager, "torchDraw", options::draw);
         }
      });
   }

   @Override
   public boolean isActive(Level level, int layerID, int x, int y) {
      return true;
   }

   @Override
   public void onWireUpdate(Level level, int layerID, int tileX, int tileY, int wireID, boolean active) {
   }
}
