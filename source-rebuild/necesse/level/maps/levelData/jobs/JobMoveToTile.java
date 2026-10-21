package necesse.level.maps.levelData.jobs;

import java.util.Objects;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.mobs.mobMovement.MobMovement;

public class JobMoveToTile {
   public final MobMovement custom;
   public final int tileX;
   public final int tileY;
   public final boolean acceptAdjacentTiles;
   public final int maxPathIterations;

   public JobMoveToTile(int tileX, int tileY, boolean acceptAdjacentTiles, int maxPathIterations) {
      this.custom = null;
      this.tileX = tileX;
      this.tileY = tileY;
      this.acceptAdjacentTiles = acceptAdjacentTiles;
      this.maxPathIterations = maxPathIterations;
   }

   public JobMoveToTile(int tileX, int tileY, boolean acceptAdjacentTiles) {
      this(tileX, tileY, acceptAdjacentTiles, HumanMob.defaultJobPathIterations);
   }

   public JobMoveToTile(MobMovement custom) {
      this.custom = custom;
      this.tileX = -1;
      this.tileY = -1;
      this.acceptAdjacentTiles = false;
      this.maxPathIterations = 0;
   }

   public boolean equals(JobMoveToTile other) {
      if (this == other) {
         return true;
      } else if (other == null) {
         return false;
      } else if (!Objects.equals(this.custom, other.custom)) {
         return false;
      } else {
         return this.custom != null ? true : this.tileX == other.tileX && this.tileY == other.tileY && this.acceptAdjacentTiles == other.acceptAdjacentTiles;
      }
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof JobMoveToTile ? this.equals((JobMoveToTile)obj) : super.equals(obj);
   }
}
