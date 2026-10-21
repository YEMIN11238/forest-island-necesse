package necesse.level.maps.levelData.settlementData;

public interface ZoneTester {
   boolean containsTile(int var1, int var2);

   default ZoneTester and(ZoneTester other) {
      return other == null ? this : (tileX, tileY) -> this.containsTile(tileX, tileY) && other.containsTile(tileX, tileY);
   }

   default ZoneTester or(ZoneTester other) {
      return other == null ? this : (tileX, tileY) -> this.containsTile(tileX, tileY) || other.containsTile(tileX, tileY);
   }
}
