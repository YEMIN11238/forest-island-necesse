package necesse.level.maps.regionSystem;

public enum RegionType {
   OPEN(0, false, false, false),
   FENCE(0, false, true, true),
   FENCE_GATE(0, true, false, true),
   SOLID(0, false, true, true),
   DOOR(1, true, false, true),
   WALL(2, false, true, true),
   SUMMON_IGNORED(0, false, true, true);

   public final int roomInt;
   public final boolean isDoor;
   public final boolean isAlwaysSolid;
   public final boolean isSolid;

   RegionType(int roomInt, boolean isDoor, boolean isAlwaysSolid, boolean isSolid) {
      this.roomInt = roomInt;
      this.isDoor = isDoor;
      this.isAlwaysSolid = isAlwaysSolid;
      this.isSolid = isSolid;
   }
}
