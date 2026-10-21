package necesse.entity.levelEvent;

import java.util.HashSet;
import java.util.stream.Stream;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.entity.manager.MobDeathListenerEntityComponent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.level.maps.levelBuffManager.LevelBuffsEntityComponent;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.levelBuffManager.MobBuffsEntityComponent;

public class HuntIncursionEvent extends IncursionLevelEvent implements MobDeathListenerEntityComponent, MobBuffsEntityComponent, LevelBuffsEntityComponent {
   public int progress;
   public int max;

   public HuntIncursionEvent() {
   }

   public HuntIncursionEvent(String bossStringID, int progress, int max) {
      super(bossStringID);
      this.bossStringID = bossStringID;
      this.progress = progress;
      this.max = max;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addInt("progress", this.progress);
      save.addInt("max", this.max);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.progress = save.getInt("progress", this.progress, false);
      this.max = save.getInt("max", this.max, false);
   }

   @Override
   public void setupUpdatePacket(PacketWriter writer) {
      super.setupUpdatePacket(writer);
      writer.putNextInt(this.progress);
      writer.putNextInt(this.max);
   }

   @Override
   public void applyUpdatePacket(PacketReader reader) {
      super.applyUpdatePacket(reader);
      this.progress = reader.getNextInt();
      this.max = reader.getNextInt();
   }

   @Override
   public void onLevelMobDied(Mob mob, Attacker attacker, HashSet<Attacker> attackers) {
      if (mob.isHostile && mob.countKillStat()) {
         this.progress++;
         this.isDirty = true;
      }
   }

   @Override
   public Stream<ModifierValue<?>> getLevelModifiers() {
      return !this.isDone && !this.isFighting && !this.bossPortalSpawned
         ? Stream.empty()
         : Stream.of(new ModifierValue<>(LevelModifiers.ENEMIES_RETREATING, true));
   }

   public float getPercentProgress() {
      return GameMath.limit((float)this.progress / this.max, 0.0F, 1.0F);
   }

   @Override
   public Stream<ModifierValue<?>> getLevelModifiers(Mob mob) {
      if (!this.isDone && !this.isFighting && !this.bossPortalSpawned) {
         if (mob.isPlayer) {
            return Stream.of(
               new ModifierValue<>(BuffModifiers.MOB_SPAWN_RATE, 1.0F + this.getPercentProgress()),
               new ModifierValue<>(BuffModifiers.MOB_SPAWN_LIGHT_THRESHOLD).min(150)
            );
         }

         if (mob.isHostile) {
            return Stream.of(new ModifierValue<>(BuffModifiers.CHASER_RANGE, this.getPercentProgress()));
         }
      } else if (mob.isPlayer) {
         return Stream.of(new ModifierValue<>(BuffModifiers.MOB_SPAWN_RATE, 0.0F).max(0.0F, 1000000));
      }

      return Stream.empty();
   }

   @Override
   public boolean isObjectiveDone() {
      return this.progress >= this.max;
   }

   @Override
   public int getObjectiveCurrent() {
      return this.progress;
   }

   @Override
   public int getObjectiveMax() {
      return this.max;
   }
}
