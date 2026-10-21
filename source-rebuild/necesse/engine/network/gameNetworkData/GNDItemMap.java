package necesse.engine.network.gameNetworkData;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.GNDRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameUtils;

public class GNDItemMap extends GNDItem {
   private final HashMap<Integer, GNDItemMap.MapValue> dataMap = new HashMap<>();
   private final HashSet<Integer> isDirty = new HashSet<>();

   public GNDItemMap() {
   }

   public GNDItemMap(Packet p) {
      this(new PacketReader(p));
   }

   public GNDItemMap(PacketReader reader) {
      this();
      this.readPacket(reader);
   }

   public GNDItemMap(LoadData save) {
      this();
      this.applyLoadData(save);
   }

   @Override
   public void addSaveData(SaveData data) {
      for (int key : this.dataMap.keySet()) {
         GNDItemMap.MapValue value = this.dataMap.get(key);
         GNDItem item = value.item;
         SaveData itemData = new SaveData(value.hashString != null ? value.hashString : Integer.toString(key));
         GNDRegistry.writeGNDItem(itemData, item);
         data.addSaveData(itemData);
      }
   }

   public void applyLoadData(LoadData save) {
      this.dataMap.clear();

      for (LoadData comp : save.getLoadData()) {
         String key = comp.getName();
         this.setItem(key, GNDRegistry.loadGNDItem(comp));
      }
   }

   @Override
   public String toString() {
      StringBuilder s = new StringBuilder("map[");
      Iterator<Entry<Integer, GNDItemMap.MapValue>> it = this.dataMap.entrySet().iterator();

      while (it.hasNext()) {
         Entry<Integer, GNDItemMap.MapValue> next = it.next();
         s.append(next.getValue().hashString != null ? next.getValue().hashString : next.getKey().toString())
            .append(" = ")
            .append(next.getValue().item.toString());
         if (it.hasNext()) {
            s.append(",");
         }
      }

      return s + "]";
   }

   @Override
   public boolean isDefault() {
      return false;
   }

   @Override
   public boolean equals(GNDItem item) {
      return item instanceof GNDItemMap ? this.sameKeys((GNDItemMap)item) : false;
   }

   public GNDItemMap copy() {
      GNDItemMap copy = new GNDItemMap();

      for (int key : this.dataMap.keySet()) {
         GNDItemMap.MapValue mapValue = this.dataMap.get(key);
         if (mapValue.hashString != null) {
            copy.setItem(mapValue.hashString, mapValue.item.copy());
         } else {
            copy.setItem(key, mapValue.item.copy());
         }
      }

      return copy;
   }

   public Packet getContentPacket() {
      Packet p = new Packet();
      PacketWriter writer = new PacketWriter(p);
      this.writePacket(writer);
      return p;
   }

   @Override
   public void writePacket(PacketWriter writer) {
      writer.putNextShortUnsigned(this.dataMap.size());

      for (int key : this.dataMap.keySet()) {
         GNDItem item = this.dataMap.get(key).item;
         writer.putNextInt(key);
         GNDRegistry.writeGNDItem(writer, item);
      }
   }

   @Override
   public void readPacket(PacketReader reader) {
      this.dataMap.clear();
      int size = reader.getNextShortUnsigned();

      for (int i = 0; i < size; i++) {
         int hash = reader.getNextInt();
         this.setItem(hash, GNDRegistry.readGNDItem(reader));
      }
   }

   public void writeDirtyPacket(PacketWriter writer) {
      writer.putNextShortUnsigned(this.isDirty.size());

      for (int key : this.isDirty) {
         writer.putNextInt(key);
         GNDItem item = this.getItem(key);
         if (item != null) {
            writer.putNextBoolean(true);
            GNDRegistry.writeGNDItem(writer, item);
         } else {
            writer.putNextBoolean(false);
         }
      }

      writer.putNextShortUnsigned(this.getMapSize());
   }

   public boolean readDirtyPacket(PacketReader reader) {
      int count = reader.getNextShortUnsigned();

      for (int i = 0; i < count; i++) {
         int hash = reader.getNextInt();
         if (reader.getNextBoolean()) {
            this.setItem(hash, GNDRegistry.readGNDItem(reader));
         } else {
            this.clearItem(hash);
         }
      }

      int expectedSize = reader.getNextShortUnsigned();
      return expectedSize != this.getMapSize();
   }

   public void clearAll() {
      this.dataMap.clear();
      this.isDirty.clear();
   }

   public void addAll(GNDItemMap other) {
      for (Entry<Integer, GNDItemMap.MapValue> entry : other.dataMap.entrySet()) {
         GNDItemMap.MapValue otherValue = entry.getValue();
         this.setItem(entry.getKey(), otherValue);
      }
   }

   public boolean isEmpty() {
      return this.dataMap.isEmpty();
   }

   public boolean hasKey(String key) {
      return this.dataMap.containsKey(key.hashCode());
   }

   public int getMapSize() {
      return this.dataMap.size();
   }

   public void markDirty(int hash) {
      this.isDirty.add(hash);
   }

   public void markDirty(String key) {
      this.markDirty(getKeyHash(key));
   }

   public boolean isDirty() {
      return !this.isDirty.isEmpty();
   }

   public Collection<Integer> getDirtyKeys() {
      return this.isDirty;
   }

   public void clean(int hash) {
      this.isDirty.remove(hash);
   }

   public void clean(String key) {
      this.clean(getKeyHash(key));
   }

   public void cleanAll() {
      this.isDirty.clear();
   }

   public int getDirtyCount() {
      return this.isDirty.size();
   }

   private GNDItemMap setItem(int key, GNDItemMap.MapValue value) {
      if (value != null && value.item != null && !value.item.isDefault()) {
         if (value.hashString == null) {
            GNDItemMap.MapValue prevValue = this.dataMap.get(key);
            if (prevValue != null) {
               value = new GNDItemMap.MapValue(prevValue.hashString, value.item);
            }
         }

         GNDItemMap.MapValue oldValue = this.dataMap.put(key, value);
         if (oldValue == null || !oldValue.item.equals(value.item)) {
            this.markDirty(key);
         }
      } else {
         GNDItemMap.MapValue removed = this.dataMap.remove(key);
         if (removed != null && !removed.item.isDefault()) {
            this.markDirty(key);
         }
      }

      return this;
   }

   public GNDItemMap setItem(int hash, GNDItem data) {
      return this.setItem(hash, new GNDItemMap.MapValue(null, data));
   }

   public GNDItemMap clearItem(int hash) {
      return this.setItem(hash, (GNDItem)null);
   }

   public GNDItem getItem(int hash) {
      GNDItemMap.MapValue value = this.dataMap.get(hash);
      return value == null ? null : value.item;
   }

   public Set<Integer> getKeySet() {
      return this.dataMap.keySet();
   }

   public Set<String> getKeyStringSet() {
      return this.dataMap
         .entrySet()
         .stream()
         .map(v -> v.getValue().hashString == null ? Integer.toString(v.getKey()) : v.getValue().hashString)
         .collect(Collectors.toSet());
   }

   public static int getKeyHash(String key) {
      Integer code = GameUtils.tryParseInt(key);
      return code != null ? code : key.hashCode();
   }

   public GNDItemMap setItem(String key, GNDItem data) {
      GNDItemMap.MapValue value = new GNDItemMap.MapValue(key, data);
      Integer hash = GameUtils.tryParseInt(key);
      if (hash != null) {
         value.hashString = null;
      } else {
         hash = key.hashCode();
      }

      return this.setItem(hash, value);
   }

   public GNDItemMap clearItem(String key) {
      return this.setItem(key, null);
   }

   public GNDItem getItem(String key) {
      boolean keyIsInt = true;
      Integer hash = GameUtils.tryParseInt(key);
      if (hash == null) {
         hash = key.hashCode();
         keyIsInt = false;
      }

      GNDItemMap.MapValue value = this.dataMap.get(hash);
      if (value != null) {
         if (!keyIsInt) {
            value.hashString = key;
         }

         return value.item;
      } else {
         return null;
      }
   }

   public GNDItemMap setBoolean(String key, boolean data) {
      return this.setItem(key, new GNDItemBoolean(data));
   }

   public boolean getBoolean(String key) {
      try {
         GNDItem item = this.getItem(key);
         return item != null && ((GNDItem.GNDPrimitive)item).getBoolean();
      } catch (ClassCastException e) {
         return false;
      }
   }

   public GNDItemMap setByte(String key, byte data) {
      return this.setItem(key, new GNDItemByte(data));
   }

   public byte getByte(String key) {
      return this.getByte(key, (byte)0);
   }

   public byte getByte(String key, byte defaultValue) {
      try {
         GNDItem item = this.getItem(key);
         return item == null ? defaultValue : ((GNDItem.GNDPrimitive)item).getByte();
      } catch (ClassCastException e) {
         return defaultValue;
      }
   }

   public GNDItemMap setByteUnsigned(String key, int data) {
      return this.setItem(key, new GNDItemByte((byte)data));
   }

   public int getByteUnsigned(String key) {
      return this.getByteUnsigned(key, 0);
   }

   public int getByteUnsigned(String key, int defaultValue) {
      try {
         GNDItem item = this.getItem(key);
         return item == null ? defaultValue : ((GNDItem.GNDPrimitive)item).getByte() & 0xFF;
      } catch (ClassCastException e) {
         return defaultValue;
      }
   }

   public GNDItemMap setShort(String key, short data) {
      return this.setItem(key, new GNDItemShort(data));
   }

   public short getShort(String key) {
      return this.getShort(key, (short)0);
   }

   public short getShort(String key, short defaultValue) {
      try {
         GNDItem item = this.getItem(key);
         return item == null ? defaultValue : ((GNDItem.GNDPrimitive)item).getShort();
      } catch (ClassCastException e) {
         return defaultValue;
      }
   }

   public GNDItemMap setShortUnsigned(String key, int data) {
      return this.setItem(key, new GNDItemShort((short)data));
   }

   public int getShortUnsigned(String key) {
      return this.getShortUnsigned(key, 0);
   }

   public int getShortUnsigned(String key, int defaultValue) {
      try {
         GNDItem item = this.getItem(key);
         return item == null ? defaultValue : ((GNDItem.GNDPrimitive)item).getShort() & 65535;
      } catch (ClassCastException e) {
         return defaultValue;
      }
   }

   public GNDItemMap setInt(String key, int data) {
      return this.setItem(key, new GNDItemInt(data));
   }

   public int getInt(String key) {
      return this.getInt(key, 0);
   }

   public int getInt(String key, int defaultValue) {
      try {
         GNDItem item = this.getItem(key);
         return item == null ? defaultValue : ((GNDItem.GNDPrimitive)item).getInt();
      } catch (ClassCastException e) {
         return defaultValue;
      }
   }

   public GNDItemMap setIntUnsigned(String key, long data) {
      return this.setItem(key, new GNDItemInt((int)data));
   }

   public long getIntUnsigned(String key) {
      return this.getIntUnsigned(key, 0L);
   }

   public long getIntUnsigned(String key, long defaultValue) {
      try {
         GNDItem item = this.getItem(key);
         return item == null ? defaultValue : Integer.toUnsignedLong(((GNDItem.GNDPrimitive)item).getInt());
      } catch (ClassCastException e) {
         return defaultValue;
      }
   }

   public GNDItemMap setLong(String key, long data) {
      return this.setItem(key, new GNDItemLong(data));
   }

   public long getLong(String key) {
      return this.getLong(key, 0L);
   }

   public long getLong(String key, long defaultValue) {
      try {
         GNDItem item = this.getItem(key);
         return item == null ? defaultValue : ((GNDItem.GNDPrimitive)item).getLong();
      } catch (ClassCastException e) {
         return defaultValue;
      }
   }

   public GNDItemMap setFloat(String key, float data) {
      return this.setItem(key, new GNDItemFloat(data));
   }

   public float getFloat(String key) {
      return this.getFloat(key, 0.0F);
   }

   public float getFloat(String key, float defaultValue) {
      try {
         GNDItem item = this.getItem(key);
         return item == null ? defaultValue : ((GNDItem.GNDPrimitive)item).getFloat();
      } catch (ClassCastException e) {
         return defaultValue;
      }
   }

   public GNDItemMap setDouble(String key, double data) {
      return this.setItem(key, new GNDItemDouble(data));
   }

   public double getDouble(String key) {
      return this.getDouble(key, 0.0);
   }

   public double getDouble(String key, double defaultValue) {
      try {
         GNDItem item = this.getItem(key);
         return item == null ? defaultValue : ((GNDItem.GNDPrimitive)item).getDouble();
      } catch (ClassCastException e) {
         return defaultValue;
      }
   }

   public GNDItemMap setString(String key, String data) {
      return this.setItem(key, new GNDItemString(data));
   }

   public String getString(String key) {
      return this.getString(key, "");
   }

   public String getString(String key, String defaultValue) {
      GNDItem item = this.getItem(key);
      return item == null ? defaultValue : item.toString();
   }

   public int copyKeysToTarget(GNDItemMap target, String... keys) {
      int copied = 0;

      for (String key : keys) {
         GNDItem item = this.getItem(key);
         if (item != null) {
            copied++;
            target.setItem(key, item);
         }
      }

      return copied;
   }

   public boolean sameKeys(GNDItemMap other, String... keys) {
      return sameKeys(this, other, keys);
   }

   public static boolean sameKeys(GNDItemMap map1, GNDItemMap map2, String... keys) {
      HashSet<String> comparedKeys = new HashSet<>();
      if (keys.length == 0) {
         for (String key1 : map1.getKeyStringSet()) {
            if (map2.hasKey(key1)) {
               if (!equals(map1.getItem(key1), map2.getItem(key1))) {
                  return false;
               }
            } else if (!map1.getItem(key1).isDefault()) {
               return false;
            }

            comparedKeys.add(key1);
         }

         for (String key2 : map2.getKeyStringSet()) {
            if (!comparedKeys.contains(key2)) {
               if (map1.hasKey(key2)) {
                  if (!equals(map2.getItem(key2), map1.getItem(key2))) {
                     return false;
                  }
               } else if (!map2.getItem(key2).isDefault()) {
                  return false;
               }
            }
         }
      } else {
         for (String key : keys) {
            boolean map1HasKey = map1.hasKey(key);
            boolean map2HasKey = map2.hasKey(key);
            if (map1HasKey && map2HasKey) {
               if (!equals(map1.getItem(key), map2.getItem(key))) {
                  return false;
               }
            } else if (map1HasKey) {
               if (!map1.getItem(key).isDefault()) {
                  return false;
               }
            } else if (map2HasKey && !map2.getItem(key).isDefault()) {
               return false;
            }
         }
      }

      return true;
   }

   private static class MapValue {
      private String hashString;
      private final GNDItem item;

      public MapValue(String hashString, GNDItem item) {
         this.hashString = hashString;
         this.item = item;
      }
   }
}
