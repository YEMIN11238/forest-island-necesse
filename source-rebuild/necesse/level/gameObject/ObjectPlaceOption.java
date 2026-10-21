package necesse.level.gameObject;

import java.util.Objects;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.ObjectRegistry;

public class ObjectPlaceOption {
   public final int tileX;
   public final int tileY;
   public final GameObject object;
   public final int rotation;
   public final boolean strictRotation;

   public ObjectPlaceOption(int tileX, int tileY, GameObject object, int rotation, boolean strictRotation) {
      Objects.requireNonNull(object);
      this.tileX = tileX;
      this.tileY = tileY;
      this.object = object;
      this.rotation = rotation;
      this.strictRotation = strictRotation;
   }

   public ObjectPlaceOption(PacketReader reader) {
      this.tileX = reader.getNextInt();
      this.tileY = reader.getNextInt();
      this.object = ObjectRegistry.getObject(reader.getNextShortUnsigned());
      this.rotation = reader.getNextByteUnsigned();
      this.strictRotation = true;
   }

   public void writePacket(PacketWriter writer) {
      writer.putNextInt(this.tileX);
      writer.putNextInt(this.tileY);
      writer.putNextShortUnsigned(this.object.getID());
      writer.putNextByteUnsigned(this.rotation);
   }

   public ObjectPlaceOption(String prefix, GNDItemMap map) {
      if (prefix == null) {
         prefix = "";
      }

      this.tileX = map.getInt(prefix + (prefix.isEmpty() ? "tileX" : "TileX"));
      this.tileY = map.getInt(prefix + (prefix.isEmpty() ? "tileY" : "TileY"));
      this.object = ObjectRegistry.getObject(map.getInt(prefix + (prefix.isEmpty() ? "objectID" : "ObjectID")));
      this.rotation = map.getInt(prefix + (prefix.isEmpty() ? "rotation" : "Rotation"));
      this.strictRotation = true;
   }

   public void writeGNDMap(String prefix, GNDItemMap map) {
      if (prefix == null) {
         prefix = "";
      }

      map.setInt(prefix + (prefix.isEmpty() ? "tileX" : "TileX"), this.tileX);
      map.setInt(prefix + (prefix.isEmpty() ? "tileY" : "TileY"), this.tileY);
      map.setInt(prefix + (prefix.isEmpty() ? "objectID" : "ObjectID"), this.object.getID());
      map.setInt(prefix + (prefix.isEmpty() ? "rotation" : "Rotation"), this.rotation);
   }

   public boolean isSame(ObjectPlaceOption other) {
      return this == other
         ? true
         : this.tileX == other.tileX
            && this.tileY == other.tileY
            && this.object == other.object
            && (!this.strictRotation || !other.strictRotation || this.rotation == other.rotation);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return o != null && this.getClass() == o.getClass() ? this.isSame((ObjectPlaceOption)o) : false;
      }
   }

   @Override
   public String toString() {
      return "ObjectPlaceOption{tileX="
         + this.tileX
         + ", tileY="
         + this.tileY
         + ", object="
         + this.object
         + ", rotation="
         + this.rotation
         + ", strictRotation="
         + this.strictRotation
         + '}';
   }
}
