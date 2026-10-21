package necesse.engine.modLoader;

import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public final class ModNetworkData {
   public final LoadedMod.SaveType type;
   public final String id;
   public final String name;
   public final String version;
   public final long steamWorkshopID;
   public final boolean clientside;

   public ModNetworkData(LoadedMod mod) {
      this.type = mod.getSaveType();
      this.id = mod.id;
      this.version = mod.version;
      this.name = mod.name;
      this.clientside = mod.clientside;
      this.steamWorkshopID = mod.getSteamWorkshopID();
   }

   public ModNetworkData(PacketReader reader) {
      this.type = reader.getNextEnum(LoadedMod.SaveType.class);
      this.id = reader.getNextString();
      this.version = reader.getNextString();
      this.name = reader.getNextString();
      this.clientside = reader.getNextBoolean();
      this.steamWorkshopID = reader.getNextLong();
   }

   public static ModNetworkData fromPacketReader(PacketReader reader) {
      return new ModNetworkData(reader);
   }

   public void write(PacketWriter writer) {
      writer.putNextEnum(this.type);
      writer.putNextString(this.id);
      writer.putNextString(this.version);
      writer.putNextString(this.name);
      writer.putNextBoolean(this.clientside);
      writer.putNextLong(this.steamWorkshopID);
   }
}
