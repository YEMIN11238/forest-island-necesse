package necesse.gfx.drawOptions.itemAttack;

import necesse.gfx.drawOptions.DrawOptions;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public interface HumanAttackDrawOptions {
   HumanAttackDrawOptions setOffsets(int var1, int var2, int var3, int var4, float var5, int var6, int var7, int var8, int var9, int var10);

   HumanAttackDrawOptions light(Level var1, boolean var2, int var3, GameLight var4);

   HumanAttackDrawOptions skinColor(int var1);

   DrawOptions pos(int var1, int var2);
}
