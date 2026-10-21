package necesse.engine.network.gameNetworkData;

import necesse.engine.network.PacketReader;
import necesse.engine.registries.EnchantmentRegistry;
import necesse.engine.save.LoadData;

public class GNDItemEnchantment extends GNDRegistryItem {
   public GNDItemEnchantment(String stringID) {
      super(stringID);
   }

   public GNDItemEnchantment(int id) {
      super(id);
   }

   public GNDItemEnchantment(PacketReader reader) {
      super(reader);
   }

   public GNDItemEnchantment(LoadData data) {
      super(data);
   }

   @Override
   protected int toID(String stringID) {
      return EnchantmentRegistry.getEnchantmentID(stringID);
   }

   @Override
   protected String toStringID(int id) {
      return EnchantmentRegistry.getEnchantmentStringID(id);
   }

   @Override
   public GNDItem copy() {
      return new GNDItemEnchantment(this.getRegistryID());
   }

   public static GNDItemEnchantment convertEnchantmentID(GNDItem item) {
      if (item instanceof GNDItemEnchantment) {
         return (GNDItemEnchantment)item;
      } else if (item instanceof GNDItem.GNDPrimitive) {
         return new GNDItemEnchantment(((GNDItem.GNDPrimitive)item).getShort());
      } else {
         return item == null ? new GNDItemEnchantment(-1) : new GNDItemEnchantment(item.toString());
      }
   }
}
