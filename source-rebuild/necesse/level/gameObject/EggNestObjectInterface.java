package necesse.level.gameObject;

import necesse.level.maps.Level;

public interface EggNestObjectInterface {
   ProcessObjectHandler getLayEggHandler(Level var1, int var2, int var3);

   ProcessObjectHandler getFertilizeEggHandler(Level var1, int var2, int var3);
}
