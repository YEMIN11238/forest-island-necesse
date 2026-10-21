package necesse.engine.network.gameNetworkData;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;

public class GNDItemFloat extends GNDItem.GNDPrimitive {
   private float data;

   public GNDItemFloat(float data) {
      this.data = data;
   }

   public GNDItemFloat(PacketReader reader) {
      this.readPacket(reader);
   }

   public GNDItemFloat(LoadData data) {
      this.data = data.getFloat("value", 0.0F);
   }

   @Override
   public String toString() {
      return this.data + "f";
   }

   @Override
   public boolean isDefault() {
      return this.data == 0.0F;
   }

   @Override
   public boolean equals(GNDItem item) {
      return item instanceof GNDItem.GNDPrimitive ? this.getDouble() == ((GNDItem.GNDPrimitive)item).getDouble() : false;
   }

   public GNDItemFloat copy() {
      return new GNDItemFloat(this.data);
   }

   @Override
   public void addSaveData(SaveData data) {
      data.addFloat("value", this.data);
   }

   @Override
   public void writePacket(PacketWriter writer) {
      writer.putNextFloat(this.data);
   }

   @Override
   public void readPacket(PacketReader reader) {
      this.data = reader.getNextFloat();
   }

   @Override
   public boolean getBoolean() {
      return this.data != 0.0F;
   }

   @Override
   public byte getByte() {
      return (byte)this.data;
   }

   @Override
   public short getShort() {
      return (short)this.data;
   }

   @Override
   public int getInt() {
      return (int)this.data;
   }

   @Override
   public long getLong() {
      return (long)this.data;
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
