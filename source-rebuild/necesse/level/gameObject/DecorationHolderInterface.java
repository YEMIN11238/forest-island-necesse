package necesse.level.gameObject;

import java.awt.Dimension;
import necesse.level.maps.Level;

public interface DecorationHolderInterface {
   DecorDrawOffset getDecorationDrawOffset(Level var1, int var2, int var3, GameObject var4);

   boolean canPlaceDecoration(Level var1, int var2, int var3);

   Dimension getMaxDecorationSize(Level var1, int var2, int var3);
}
