package necesse.engine.util;

import java.awt.Rectangle;
import necesse.engine.GameLog;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;

public class ZoningChange {
   private Packet packet;

   private ZoningChange(Packet packet) {
      this.packet = packet;
   }

   public boolean applyTo(Zoning zone) {
      if (zone == null) {
         return false;
      }

      PacketReader reader = new PacketReader(this.packet);
      ZoningChange.ChangeType[] types = ZoningChange.ChangeType.values();
      int typeIndex = reader.getNextMaxValue(types.length + 1);
      if (typeIndex >= 0 && typeIndex < types.length) {
         ZoningChange.ChangeType type = types[typeIndex];
         switch (type) {
            case EXPAND: {
               int x = reader.getNextInt();
               int y = reader.getNextInt();
               int width = reader.getNextInt();
               int height = reader.getNextInt();
               return zone.addRectangle(new Rectangle(x, y, width, height));
            }
            case SHRINK: {
               int x = reader.getNextInt();
               int y = reader.getNextInt();
               int width = reader.getNextInt();
               int height = reader.getNextInt();
               return zone.removeRectangle(new Rectangle(x, y, width, height));
            }
            case INVERT: {
               int x = reader.getNextInt();
               int y = reader.getNextInt();
               int width = reader.getNextInt();
               int height = reader.getNextInt();
               zone.invert(new Rectangle(x, y, width, height));
               return true;
            }
            case INVERT_FULL:
               zone.invert();
               return true;
            case FULL:
               zone.readZonePacket(reader);
               return true;
            default:
               return false;
         }
      } else {
         GameLog.warn.println("Tried to apply invalid ItemCategoriesFilterChange to type index " + typeIndex);
         return false;
      }
   }

   public void write(PacketWriter writer) {
      writer.putNextContentPacket(this.packet);
   }

   public static ZoningChange fromPacket(PacketReader reader) {
      return new ZoningChange(reader.getNextContentPacket());
   }

   public static ZoningChange expand(Rectangle rectangle) {
      Packet packet = new Packet();
      PacketWriter writer = new PacketWriter(packet);
      writer.putNextMaxValue(ZoningChange.ChangeType.EXPAND.ordinal(), ZoningChange.ChangeType.values().length + 1);
      writer.putNextInt(rectangle.x);
      writer.putNextInt(rectangle.y);
      writer.putNextInt(rectangle.width);
      writer.putNextInt(rectangle.height);
      return new ZoningChange(packet);
   }

   public static ZoningChange shrink(Rectangle rectangle) {
      Packet packet = new Packet();
      PacketWriter writer = new PacketWriter(packet);
      writer.putNextMaxValue(ZoningChange.ChangeType.SHRINK.ordinal(), ZoningChange.ChangeType.values().length + 1);
      writer.putNextInt(rectangle.x);
      writer.putNextInt(rectangle.y);
      writer.putNextInt(rectangle.width);
      writer.putNextInt(rectangle.height);
      return new ZoningChange(packet);
   }

   public static ZoningChange invert(Rectangle rectangle) {
      Packet packet = new Packet();
      PacketWriter writer = new PacketWriter(packet);
      writer.putNextMaxValue(ZoningChange.ChangeType.INVERT.ordinal(), ZoningChange.ChangeType.values().length + 1);
      writer.putNextInt(rectangle.x);
      writer.putNextInt(rectangle.y);
      writer.putNextInt(rectangle.width);
      writer.putNextInt(rectangle.height);
      return new ZoningChange(packet);
   }

   public static ZoningChange fullInvert() {
      Packet packet = new Packet();
      PacketWriter writer = new PacketWriter(packet);
      writer.putNextMaxValue(ZoningChange.ChangeType.INVERT_FULL.ordinal(), ZoningChange.ChangeType.values().length + 1);
      return new ZoningChange(packet);
   }

   public static ZoningChange full(Zoning zone) {
      Packet packet = new Packet();
      PacketWriter writer = new PacketWriter(packet);
      writer.putNextMaxValue(ZoningChange.ChangeType.FULL.ordinal(), ZoningChange.ChangeType.values().length + 1);
      zone.writeZonePacket(writer);
      return new ZoningChange(packet);
   }

   private enum ChangeType {
      EXPAND,
      SHRINK,
      INVERT,
      INVERT_FULL,
      FULL;
   }
}
