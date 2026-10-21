package necesse.level.gameObject;

import necesse.level.maps.Level;

public interface TorchHolderInterface {
   DecorDrawOffset getTorchDrawOffset(Level var1, int var2, int var3);

   default boolean canPlaceTorch(Level level, int tileX, int tileY) {
      return true;
   }
}
