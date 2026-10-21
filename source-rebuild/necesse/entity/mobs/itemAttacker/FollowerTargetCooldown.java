package necesse.entity.mobs.itemAttacker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PathDoorOption;
import necesse.entity.mobs.ai.path.RegionPathfinding;
import necesse.level.maps.Level;

public class FollowerTargetCooldown {
   public final ItemAttackerMob owner;
   protected HashMap<Integer, FollowerTargetCooldown.Cooldown> cache = new HashMap<>();
   protected int minCachedTime = 1000;
   protected int maxCachedTime = 2000;

   public FollowerTargetCooldown(ItemAttackerMob owner) {
      this.owner = owner;
   }

   public boolean canMoveTo(Mob target, int tileX, int tileY) {
      Level level = this.owner.getLevel();
      int regionID = level.getRegionID(tileX, tileY);
      PathDoorOption doorOption = level.regionManager.BASIC_DOOR_OPTIONS;
      if (doorOption != null) {
         return doorOption.canMoveToTile(this.owner.getTileX(), this.owner.getTileY(), tileX, tileY, target.canBeTargetedFromAdjacentTiles());
      }

      FollowerTargetCooldown.Cooldown result = this.cache
         .compute(
            regionID,
            (p, last) -> (FollowerTargetCooldown.Cooldown)(last != null && last.cooldown > this.owner.getTime()
               ? last
               : new FollowerTargetCooldown.Cooldown(
                  RegionPathfinding.canMoveToTile(
                     this.owner.getLevel(), this.owner.getTileX(), this.owner.getTileY(), tileX, tileY, doorOption, target.canBeTargetedFromAdjacentTiles()
                  ),
                  this.owner.getTime() + GameRandom.globalRandom.getIntBetween(this.minCachedTime, this.maxCachedTime)
               ))
         );
      return result.canMoveTo;
   }

   public boolean canMoveTo(Mob target) {
      return this.canMoveTo(target, target.getTileX(), target.getTileY());
   }

   public void cleanCache() {
      HashSet<Integer> removes = new HashSet<>();

      for (Entry<Integer, FollowerTargetCooldown.Cooldown> e : this.cache.entrySet()) {
         FollowerTargetCooldown.Cooldown value = e.getValue();
         if (value.cooldown <= this.owner.getTime()) {
            removes.add(e.getKey());
         }
      }

      for (Integer remove : removes) {
         this.cache.remove(remove);
      }
   }

   protected static class Cooldown {
      public boolean canMoveTo;
      public long cooldown;

      public Cooldown(boolean canMoveTo, long cooldown) {
         this.canMoveTo = canMoveTo;
         this.cooldown = cooldown;
      }
   }
}
