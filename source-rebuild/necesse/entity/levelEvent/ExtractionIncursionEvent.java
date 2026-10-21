package necesse.entity.levelEvent;

import java.util.ArrayList;
import java.util.stream.Stream;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.entity.manager.ObjectDestroyedListenerEntityComponent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.levelBuffManager.LevelBuffsEntityComponent;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.levelBuffManager.MobBuffsEntityComponent;

public class ExtractionIncursionEvent
   extends IncursionLevelEvent
   implements ObjectDestroyedListenerEntityComponent,
   MobBuffsEntityComponent,
   LevelBuffsEntityComponent {
   public int startObjects = -1;
   public int doneAtRemainingObjects = -1;
   public int remainingObjects = -1;
   public int mobSpawnsGraceTime = 3000;
   public long lastObjectDestroyedTime;
   public int startMobSpawns = 20;
   public int maxMobsSpawns = 220;
   public int currentMobsSpawned = 0;

   public ExtractionIncursionEvent() {
   }

   public ExtractionIncursionEvent(String bossStringID) {
      super(bossStringID);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addInt("startObjects", this.startObjects);
      save.addInt("doneAtRemainingObjects", this.doneAtRemainingObjects);
      save.addInt("currentMobsSpawned", this.currentMobsSpawned);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.startObjects = save.getInt("startObjects", this.startObjects, false);
      this.doneAtRemainingObjects = save.getInt("doneAtRemainingObjects", this.doneAtRemainingObjects, false);
      this.currentMobsSpawned = save.getInt("currentMobsSpawned", this.currentMobsSpawned, false);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.startObjects);
      writer.putNextInt(this.doneAtRemainingObjects);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.startObjects = reader.getNextInt();
      this.doneAtRemainingObjects = reader.getNextInt();
   }

   @Override
   public void setupUpdatePacket(PacketWriter writer) {
      super.setupUpdatePacket(writer);
      writer.putNextInt(this.remainingObjects);
      writer.putNextInt(this.currentMobsSpawned);
   }

   @Override
   public void applyUpdatePacket(PacketReader reader) {
      super.applyUpdatePacket(reader);
      this.remainingObjects = reader.getNextInt();
      this.currentMobsSpawned = reader.getNextInt();
   }

   @Override
   public void init() {
      super.init();
      if (!this.isClient()) {
         this.remainingObjects = 0;
         this.level.regionManager.ensureEntireLevelIsLoaded();

         for (int x = 0; x < this.level.tileWidth; x++) {
            for (int y = 0; y < this.level.tileHeight; y++) {
               if (this.level.getObject(x, y).isIncursionExtractionObject && !this.level.objectLayer.isPlayerPlaced(x, y)) {
                  this.remainingObjects++;
               }
            }
         }

         if (this.startObjects == -1 || this.doneAtRemainingObjects == -1) {
            this.startObjects = this.remainingObjects;
            this.doneAtRemainingObjects = this.startObjects / 5;
         }
      }
   }

   @Override
   public void onObjectDestroyed(GameObject object, int layerID, int tileX, int tileY, ServerClient client, ArrayList<ItemPickupEntity> itemsDropped) {
      if (object.isIncursionExtractionObject && !this.level.objectLayer.isPlayerPlaced(tileX, tileY)) {
         this.lastObjectDestroyedTime = this.getTime();
         this.remainingObjects--;
         this.isDirty = true;
      }
   }

   @Override
   public void onMobSpawned(Mob mob) {
      super.onMobSpawned(mob);
      if (mob.isHostile) {
         this.currentMobsSpawned++;
         this.isDirty = true;
      }
   }

   public float getPercentProgress() {
      return 1.0F
         - GameMath.limit((float)(this.remainingObjects - this.doneAtRemainingObjects) / (this.startObjects - this.doneAtRemainingObjects), 0.0F, 1.0F);
   }

   @Override
   public Stream<ModifierValue<?>> getLevelModifiers() {
      return !this.isDone && !this.isFighting && !this.bossPortalSpawned
         ? Stream.empty()
         : Stream.of(new ModifierValue<>(LevelModifiers.ENEMIES_RETREATING, true));
   }

   @Override
   public Stream<ModifierValue<?>> getLevelModifiers(Mob mob) {
      if (!this.isDone && !this.isFighting && !this.bossPortalSpawned) {
         if (mob.isPlayer) {
            float percent = (float)Math.pow(this.getPercentProgress(), 2.0);
            int expectedMobSpawns = this.startMobSpawns + Math.round(this.maxMobsSpawns * percent);
            ModifierValue<Float> spawnRateModifier;
            if (this.currentMobsSpawned < expectedMobSpawns && this.lastObjectDestroyedTime + this.mobSpawnsGraceTime < this.getTime()) {
               int delta = expectedMobSpawns - this.currentMobsSpawned;
               float rate = delta / 25.0F + this.getPercentProgress();
               spawnRateModifier = new ModifierValue<>(BuffModifiers.MOB_SPAWN_RATE, rate);
            } else {
               spawnRateModifier = new ModifierValue<>(BuffModifiers.MOB_SPAWN_RATE, 0.0F).max(0.0F, 1000000);
            }

            return Stream.of(spawnRateModifier, new ModifierValue<>(BuffModifiers.MOB_SPAWN_LIGHT_THRESHOLD).min(150));
         }

         if (mob.isHostile) {
            return Stream.of(new ModifierValue<>(BuffModifiers.CHASER_RANGE, 0.5F + this.getPercentProgress()));
         }
      } else if (mob.isPlayer) {
         return Stream.of(new ModifierValue<>(BuffModifiers.MOB_SPAWN_RATE, 0.0F).max(0.0F, 1000000));
      }

      return Stream.empty();
   }

   @Override
   public boolean isObjectiveDone() {
      return this.remainingObjects <= this.doneAtRemainingObjects;
   }

   @Override
   public int getObjectiveCurrent() {
      return this.startObjects - this.remainingObjects;
   }

   @Override
   public int getObjectiveMax() {
      return this.startObjects - this.doneAtRemainingObjects;
   }
}
