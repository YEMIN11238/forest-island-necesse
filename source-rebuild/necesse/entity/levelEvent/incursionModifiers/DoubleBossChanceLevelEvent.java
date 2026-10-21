package necesse.entity.levelEvent.incursionModifiers;

import java.awt.Point;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Stream;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.manager.MobHealthChangeListenerEntityComponent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.level.maps.Level;
import necesse.level.maps.levelBuffManager.MobBuffsEntityComponent;

public class DoubleBossChanceLevelEvent extends LevelEvent implements MobHealthChangeListenerEntityComponent, MobBuffsEntityComponent {
   public static ArrayList<DoubleBossChanceLevelEvent.SpawnBossFunction> possibleBossSpawns = new ArrayList<>();
   public HashSet<Integer> triggeredOnBossUniqueIDs = new HashSet<>();

   public DoubleBossChanceLevelEvent() {
      super(true);
      this.shouldSave = true;
   }

   @Override
   public boolean isNetworkImportant() {
      return true;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (!this.triggeredOnBossUniqueIDs.isEmpty()) {
         save.addIntCollection("triggeredOnBossUniqueIDs", this.triggeredOnBossUniqueIDs);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.triggeredOnBossUniqueIDs = new HashSet<>(save.getIntCollection("triggeredOnBossUniqueIDs", new ArrayList<>(), false));
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isClient()) {
         PlayerMob player = this.getClient().getPlayer();
         if (player != null && player.buffManager.hasBuff(BuffRegistry.BOSS_NEARBY)) {
            player.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.SECOND_BOSS_WATCHING, player, 1.0F, null), false);
         }
      }
   }

   @Override
   public Stream<ModifierValue<?>> getLevelModifiers(Mob mob) {
      return mob.isSecondaryIncursionBoss ? Stream.of(new ModifierValue<>(BuffModifiers.MAX_HEALTH, -0.5F)) : Stream.empty();
   }

   @Override
   public void onLevelMobHealthChanged(Mob mob, int beforeHealth, int health, float knockbackX, float knockbackY, Attacker attacker) {
      if (!mob.isClient() && mob.isBoss() && !mob.isSecondaryIncursionBoss() && mob.dropsLoot() && !this.triggeredOnBossUniqueIDs.contains(mob.getUniqueID())) {
         float healthPercent = (float)health / mob.getMaxHealth();
         if (healthPercent <= 0.25F) {
            DoubleBossChanceLevelEvent.SpawnBossFunction spawner = GameRandom.globalRandom.getOneOf(possibleBossSpawns);
            spawner.spawnBoss(mob);
            this.triggeredOnBossUniqueIDs.add(mob.getUniqueID());
         }
      }
   }

   protected static Point getRandomNearbyPosition(Mob mob, float distance) {
      int angle = GameRandom.globalRandom.nextInt(360);
      Float dir = GameMath.getAngleDir(angle);
      int newX = (int)(mob.getX() + dir.x * distance);
      int newY = (int)(mob.getY() + dir.y * distance);
      return new Point(newX, newY);
   }

   static {
      possibleBossSpawns.add(new DoubleBossChanceLevelEvent.SimpleSpawnBossFunction("reaper"));
      possibleBossSpawns.add(new DoubleBossChanceLevelEvent.SimpleSpawnBossFunction("cryoqueen"));
      possibleBossSpawns.add(new DoubleBossChanceLevelEvent.SimpleSpawnBossFunction("pestwarden"));
      possibleBossSpawns.add(new DoubleBossChanceLevelEvent.SimpleSpawnBossFunction("sageandgrit"));
      possibleBossSpawns.add(new DoubleBossChanceLevelEvent.SimpleSpawnBossFunction("motherslime"));
      possibleBossSpawns.add(new DoubleBossChanceLevelEvent.SimpleSpawnBossFunction("nightswarm"));
      possibleBossSpawns.add(new DoubleBossChanceLevelEvent.SimpleSpawnBossFunction("spiderempress"));
      possibleBossSpawns.add(new DoubleBossChanceLevelEvent.SimpleSpawnBossFunction("sunlightchampion"));
      possibleBossSpawns.add(new DoubleBossChanceLevelEvent.SimpleSpawnBossFunction("moonlightdancer"));
      possibleBossSpawns.add(new DoubleBossChanceLevelEvent.SimpleSpawnBossFunction("crystaldragon"));
   }

   public static class SimpleSpawnBossFunction implements DoubleBossChanceLevelEvent.SpawnBossFunction {
      public String bossStringID;

      public SimpleSpawnBossFunction(String bossStringID) {
         this.bossStringID = bossStringID;
      }

      @Override
      public void spawnBoss(Mob originalBoss) {
         Level level = originalBoss.getLevel();
         Mob mob = MobRegistry.getMob(this.bossStringID, level);
         mob.isSecondaryIncursionBoss = true;
         mob.buffManager.forceUpdateBuffs();
         mob.onSpawned(originalBoss.getX(), originalBoss.getY());
         level.entityManager.mobs.add(mob);
      }
   }

   public interface SpawnBossFunction {
      void spawnBoss(Mob var1);
   }
}
