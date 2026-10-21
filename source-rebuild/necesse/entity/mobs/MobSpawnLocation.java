package necesse.entity.mobs;

import java.util.function.BiFunction;
import java.util.function.Predicate;
import necesse.engine.modifiers.Modifier;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.buffs.BuffModifiers;

public class MobSpawnLocation {
   public final Mob mob;
   public final int x;
   public final int y;
   private boolean valid;

   public MobSpawnLocation(Mob mob, int x, int y) {
      this.mob = mob;
      this.x = x;
      this.y = y;
      this.valid = true;
   }

   public void apply() {
      this.mob.setPos(this.x, this.y, true);
   }

   public boolean valid() {
      return this.valid;
   }

   public boolean validAndApply() {
      if (this.valid()) {
         this.apply();
         return true;
      } else {
         return false;
      }
   }

   public MobSpawnLocation checkMobSpawnLocation() {
      return this.mob.checkSpawnLocation(this);
   }

   public MobSpawnLocation checkMaxHostilesAround(int maxMobs, int tileRange, ServerClient client) {
      return this.checkMaxMobsAround(maxMobs, tileRange, m -> m.isHostile, client);
   }

   public MobSpawnLocation checkMaxMobsAround(int maxMobs, int tileRange, Predicate<Mob> filter, ServerClient client) {
      if (this.valid) {
         long count = this.mob
            .getLevel()
            .entityManager
            .mobs
            .streamInRegionsInTileRange(this.x, this.y, tileRange)
            .filter(m -> GameMath.squareDistance(this.x, this.y, m.x, m.y) <= tileRange * 32)
            .filter(filter)
            .count();
         float multiplier = 1.0F;
         if (client != null) {
            multiplier = client.playerMob.buffManager.getModifier(BuffModifiers.MOB_SPAWN_CAP);
         }

         this.valid = (float)count < maxMobs * multiplier;
      }

      return this;
   }

   public MobSpawnLocation checkLocation(BiFunction<Integer, Integer, Boolean> logic) {
      this.valid = this.valid && logic.apply(this.x, this.y);
      return this;
   }

   public MobSpawnLocation checkTile(BiFunction<Integer, Integer, Boolean> logic) {
      this.valid = this.valid && logic.apply(GameMath.getTileCoordinate(this.x), GameMath.getTileCoordinate(this.y));
      return this;
   }

   public MobSpawnLocation checkLightThreshold(ServerClient client) {
      Modifier<Integer> modifier = BuffModifiers.MOB_SPAWN_LIGHT_THRESHOLD;
      int threshold;
      if (client != null) {
         threshold = client.playerMob.buffManager.getAndApplyModifiers(modifier, this.mob.spawnLightThreshold);
      } else {
         threshold = this.mob
            .spawnLightThreshold
            .limits
            .applyModifierLimits(modifier, modifier.finalLimit(modifier.appendManager(modifier.defaultBuffManagerValue, this.mob.spawnLightThreshold.value)));
      }

      return this.checkMaxLightThreshold(threshold);
   }

   public MobSpawnLocation checkStaticLightThreshold(ServerClient client) {
      Modifier<Integer> modifier = BuffModifiers.MOB_SPAWN_LIGHT_THRESHOLD;
      int threshold;
      if (client != null) {
         threshold = client.playerMob.buffManager.getAndApplyModifiers(modifier, this.mob.spawnLightThreshold);
      } else {
         threshold = this.mob
            .spawnLightThreshold
            .limits
            .applyModifierLimits(modifier, modifier.finalLimit(modifier.appendManager(modifier.defaultBuffManagerValue, this.mob.spawnLightThreshold.value)));
      }

      return this.checkMaxStaticLightThreshold(threshold);
   }

   public MobSpawnLocation checkMaxLightThreshold(int maxLight) {
      return this.checkTile((tileX, tileY) -> this.mob.getLevel().lightManager.getAmbientAndStaticLightLevelFloat(tileX, tileY) <= maxLight);
   }

   public MobSpawnLocation checkMinLightThreshold(int minLight) {
      return this.checkTile((tileX, tileY) -> this.mob.getLevel().lightManager.getAmbientAndStaticLightLevelFloat(tileX, tileY) >= minLight);
   }

   public MobSpawnLocation checkMaxStaticLightThreshold(int maxLight) {
      return this.checkTile((tileX, tileY) -> this.mob.getLevel().lightManager.getStaticLight(tileX, tileY).getLevel() <= maxLight);
   }

   public MobSpawnLocation checkMinStaticLightThreshold(int minLight) {
      return this.checkTile((tileX, tileY) -> this.mob.getLevel().lightManager.getStaticLight(tileX, tileY).getLevel() >= minLight);
   }

   public MobSpawnLocation checkNotLevelCollides() {
      return this.checkLocation((x, y) -> !this.mob.collidesWith(this.mob.getLevel(), x, y));
   }

   public MobSpawnLocation checkNotOnSurfaceInsideOnFloor() {
      return this.checkTile((x, y) -> this.mob.getLevel().isCave || this.mob.getLevel().isOutside(x, y) || !this.mob.getLevel().getTile(x, y).isFloor);
   }

   public MobSpawnLocation checkNotInsideOnFloor() {
      return this.checkTile((x, y) -> this.mob.getLevel().isOutside(x, y) || !this.mob.getLevel().getTile(x, y).isFloor);
   }

   public MobSpawnLocation checkNotSolidTile() {
      return this.checkTile((x, y) -> !this.mob.getLevel().isSolidTile(x, y));
   }

   public MobSpawnLocation checkNotInLiquid() {
      return this.checkTile((tileX, tileY) -> !this.mob.getLevel().getTile(tileX, tileY).isLiquid);
   }

   public MobSpawnLocation checkInLiquid() {
      return this.checkTile((tileX, tileY) -> this.mob.getLevel().getTile(tileX, tileY).isLiquid);
   }
}
