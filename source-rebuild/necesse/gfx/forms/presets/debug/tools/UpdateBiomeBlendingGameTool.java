package necesse.gfx.forms.presets.debug.tools;

import necesse.gfx.forms.presets.debug.DebugForm;
import necesse.level.maps.regionSystem.Region;

public class UpdateBiomeBlendingGameTool extends MouseDebugGameTool {
   public UpdateBiomeBlendingGameTool(DebugForm parent) {
      super(parent, "Update biome blending");
   }

   @Override
   public void init() {
      this.onLeftClick(e -> {
         Region region = this.getLevel().regionManager.getRegionByTile(this.getMouseTileX(), this.getMouseTileY(), false);
         if (region != null) {
            region.updateBiomeBlending();
         }

         return true;
      }, "Refresh region");
      this.onRightClick(e -> {
         this.getLevel().biomeBlendingManager.updateBlends(this.getMouseTileX(), this.getMouseTileY());
         return true;
      }, "Refresh tile");
      this.onKeyClick(82, e -> {
         int tileX = this.getMouseTileX();
         int tileY = this.getMouseTileY();
         Region region = this.getLevel().regionManager.getRegionByTile(tileX, tileY, false);
         if (region != null) {
            int regionTileX = tileX - region.tileXOffset;
            int regionTileY = tileY - region.tileYOffset;
            region.biomeBlendingLayer.getBlendingOptionsByRegion(regionTileX, regionTileY).markUpdateBlendOptions();
         }

         return true;
      }, "Refresh tile");
   }
}
