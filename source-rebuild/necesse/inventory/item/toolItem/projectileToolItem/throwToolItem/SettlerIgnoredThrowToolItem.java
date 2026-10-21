package necesse.inventory.item.toolItem.projectileToolItem.throwToolItem;

import necesse.engine.sound.SoundSettings;
import necesse.gfx.GameResources;
import necesse.inventory.InventoryItem;

public class SettlerIgnoredThrowToolItem extends ThrowToolItem {
   public SettlerIgnoredThrowToolItem(int enchantCost) {
      super(enchantCost, null);
   }

   public SettlerIgnoredThrowToolItem() {
   }

   @Override
   public float getItemAttackerWeaponValueFlat(InventoryItem item) {
      return 0.0F;
   }

   @Override
   protected SoundSettings getSwingSound() {
      return new SoundSettings(GameResources.swing1).volume(0.8F).basePitch(1.1F).pitchVariance(0.1F);
   }
}
