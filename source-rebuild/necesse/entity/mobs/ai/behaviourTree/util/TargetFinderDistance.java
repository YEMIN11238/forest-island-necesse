package necesse.entity.mobs.ai.behaviourTree.util;

import java.awt.Point;
import necesse.engine.util.gameAreaSearch.GameAreaStream;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;

public class TargetFinderDistance<T extends Mob> {
   public int searchDistance;
   public int targetLostAddedDistance;

   public TargetFinderDistance(int searchDistance) {
      this.searchDistance = searchDistance;
      this.targetLostAddedDistance = 64;
   }

   public float getSearchDistanceMod(T mob, Mob target) {
      float ownerMod = mob.buffManager.getModifier(BuffModifiers.CHASER_RANGE);
      float targetMod = target == null ? 1.0F : target.buffManager.getModifier(BuffModifiers.TARGET_RANGE);
      return mob.getLevel().entityManager.getChaserDistanceMod() * ownerMod * targetMod;
   }

   protected int getSearchDistanceFlat(T mob, Mob target) {
      return this.searchDistance;
   }

   protected int getTargetLostDistanceFlat(T mob, Mob target) {
      return this.getSearchDistanceFlat(mob, target) + this.targetLostAddedDistance;
   }

   public final float getDistance(Point base, Mob mob) {
      return this.getDistance(base, mob.x, mob.y);
   }

   public float getDistance(Point base, float x, float y) {
      return (float)base.distance(x, y);
   }

   public int getSearchDistance(T mob, Mob target) {
      return (int)(this.getSearchDistanceFlat(mob, target) * this.getSearchDistanceMod(mob, target));
   }

   public int getTargetLostDistance(T mob, Mob target) {
      return (int)(this.getTargetLostDistanceFlat(mob, target) * this.getSearchDistanceMod(mob, target));
   }

   public GameAreaStream<Mob> streamMobsAndPlayersInRange(Point base, T mob) {
      int distance = (int)(this.getSearchDistance(mob, null) * BuffModifiers.MAX_TARGET_RANGE_MODIFIER);
      float var10001 = base.x;
      float var10002 = base.y;
      return mob.getLevel().entityManager.streamAreaMobsAndPlayers(var10001, var10002, distance);
   }

   public GameAreaStream<PlayerMob> streamPlayersInRange(Point base, T mob) {
      int distance = (int)(this.getSearchDistance(mob, null) * BuffModifiers.MAX_TARGET_RANGE_MODIFIER);
      float var10001 = base.x;
      float var10002 = base.y;
      return mob.getLevel().entityManager.players.streamArea(var10001, var10002, distance);
   }

   public GameAreaStream<Mob> streamMobsInRange(Point base, T mob) {
      int distance = (int)(this.getSearchDistance(mob, null) * BuffModifiers.MAX_TARGET_RANGE_MODIFIER);
      float var10001 = base.x;
      float var10002 = base.y;
      return mob.getLevel().entityManager.mobs.streamArea(var10001, var10002, distance);
   }
}
