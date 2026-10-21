package necesse.inventory.container.settlement.data;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.inventory.InventoryItem;

public class SettlerGearData {
   public InventoryItem weapon;
   public InventoryItem headArmor;
   public InventoryItem chestArmor;
   public InventoryItem bootsArmor;
   public InventoryItem headCosmetic;
   public InventoryItem chestCosmetic;
   public InventoryItem bootsCosmetic;

   public SettlerGearData(
      InventoryItem weapon,
      InventoryItem headArmor,
      InventoryItem chestArmor,
      InventoryItem bootsArmor,
      InventoryItem headCosmetic,
      InventoryItem chestCosmetic,
      InventoryItem bootsCosmetic
   ) {
      this.weapon = weapon == null ? null : weapon.copy();
      this.headArmor = headArmor == null ? null : headArmor.copy();
      this.chestArmor = chestArmor == null ? null : chestArmor.copy();
      this.bootsArmor = bootsArmor == null ? null : bootsArmor.copy();
      this.headCosmetic = headCosmetic == null ? null : headCosmetic.copy();
      this.chestCosmetic = chestCosmetic == null ? null : chestCosmetic.copy();
      this.bootsCosmetic = bootsCosmetic == null ? null : bootsCosmetic.copy();
   }

   public SettlerGearData(PacketReader reader) {
      this.weapon = InventoryItem.fromContentPacket(reader);
      this.headArmor = InventoryItem.fromContentPacket(reader);
      this.chestArmor = InventoryItem.fromContentPacket(reader);
      this.bootsArmor = InventoryItem.fromContentPacket(reader);
      this.headCosmetic = InventoryItem.fromContentPacket(reader);
      this.chestCosmetic = InventoryItem.fromContentPacket(reader);
      this.bootsCosmetic = InventoryItem.fromContentPacket(reader);
   }

   public void writeContentPacket(PacketWriter writer) {
      InventoryItem.addPacketContent(this.weapon, writer);
      InventoryItem.addPacketContent(this.headArmor, writer);
      InventoryItem.addPacketContent(this.chestArmor, writer);
      InventoryItem.addPacketContent(this.bootsArmor, writer);
      InventoryItem.addPacketContent(this.headCosmetic, writer);
      InventoryItem.addPacketContent(this.chestCosmetic, writer);
      InventoryItem.addPacketContent(this.bootsCosmetic, writer);
   }
}
