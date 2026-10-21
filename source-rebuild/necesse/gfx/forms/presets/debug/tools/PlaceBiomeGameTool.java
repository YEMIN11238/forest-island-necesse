package necesse.gfx.forms.presets.debug.tools;

import necesse.engine.network.packet.PacketChangeBiome;
import necesse.engine.window.WindowManager;
import necesse.gfx.forms.presets.debug.DebugForm;
import necesse.level.maps.biomes.Biome;

public class PlaceBiomeGameTool extends MouseDebugGameTool {
   public Biome biome;

   public PlaceBiomeGameTool(DebugForm parent, Biome biome) {
      super(parent, null);
      this.biome = biome;
   }

   @Override
   public void init() {
      this.onLeftClick(e -> {
         int mouseTileX = this.getMouseTileX();
         int mouseTileY = this.getMouseTileY();
         if (this.getLevel().biomeLayer.getBiome(mouseTileX, mouseTileY) != this.biome) {
            this.parent.client.network.sendPacket(new PacketChangeBiome(this.getLevel(), mouseTileX, mouseTileY, this.biome.getID()));
         }

         return true;
      }, "Place biome");
      this.updatePlaceUsage();
      this.onRightClick(e -> {
         int mouseTileX = this.getMouseTileX();
         int mouseTileY = this.getMouseTileY();
         this.biome = this.getLevel().biomeLayer.getBiome(mouseTileX, mouseTileY);
         this.updatePlaceUsage();
         return true;
      }, "Select biome");
      this.onMouseMove(e -> {
         if (WindowManager.getWindow().isKeyDown(-100)) {
            int mouseTileX = this.getMouseTileX();
            int mouseTileY = this.getMouseTileY();
            if (this.getLevel().biomeLayer.getBiome(mouseTileX, mouseTileY) != this.biome) {
               this.parent.client.network.sendPacket(new PacketChangeBiome(this.getLevel(), mouseTileX, mouseTileY, this.biome.getID()));
            }

            return true;
         } else {
            return false;
         }
      });
   }

   public void updatePlaceUsage() {
      this.keyUsages.put(-100, "Place " + this.biome.getDisplayName());
   }
}
