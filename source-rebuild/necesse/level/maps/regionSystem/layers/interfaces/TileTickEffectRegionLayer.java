package necesse.level.maps.regionSystem.layers.interfaces;

import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;

public interface TileTickEffectRegionLayer {
   void tickTileEffectByRegion(GameCamera var1, PlayerMob var2, int var3, int var4);
}
