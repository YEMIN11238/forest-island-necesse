package necesse.engine.network.gameNetworkData;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public class GNDItemByte extends GNDItem.GNDPrimitive {
   private byte data;

   public GNDItemByte(byte data) {
      this.data = data;
   }

   public GNDItemByte(PacketReader reader) {
      this.readPacket(reader);
   }

   public GNDItemByte(LoadData data) {
      this.data = data.getByte("value", (byte)0);
   }

   @Override
   public String toString() {
      return Byte.toString(this.data);
   }

   @Override
   public boolean isDefault() {
      return this.data == 0;
   }

   @Override
   public boolean equals(GNDItem item) {
      return item instanceof GNDItem.GNDPrimitive ? this.getLong() == ((GNDItem.GNDPrimitive)item).getLong() : false;
   }

   public GNDItemByte copy() {
      return new GNDItemByte(this.data);
   }

   @Override
   public void addSaveData(SaveData data) {
      data.addByte("value", this.data);
   }

   @Override
   public void writePacket(PacketWriter writer) {
      writer.putNextByte(this.data);
   }

   @Override
   public void readPacket(PacketReader reader) {
      this.data = reader.getNextByte();
   }

   @Override
   public boolean getBoolean() {
      return this.data != 0;
   }

   @Override
   public byte getByte() {
      return this.data;
   }

   @Override
   public short getShort() {
      return this.data;
   }

   @Override
   public int getInt() {
      return this.data;
   }

   @Override
   public long getLong() {
      return this.data;
   }

   @Override
   public float getFloat() {
      return this.data;
   }

   @Override
   public double getDouble() {
      return this.data;
   }
}
