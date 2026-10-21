package necesse.entity.levelEvent;

import java.util.stream.Stream;
import necesse.engine.modifiers.ModifierValue;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.level.maps.levelBuffManager.LevelBuffsEntityComponent;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.levelBuffManager.MobBuffsEntityComponent;

public class TrialIncursionEvent extends IncursionLevelEvent implements MobBuffsEntityComponent, LevelBuffsEntityComponent {
   public TrialIncursionEvent() {
   }

   public TrialIncursionEvent(String bossStringID) {
      super(bossStringID);
   }

   @Override
   public void init() {
      super.init();
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
            return Stream.of(new ModifierValue<>(BuffModifiers.MOB_SPAWN_RATE, 1.0F), new ModifierValue<>(BuffModifiers.MOB_SPAWN_LIGHT_THRESHOLD).min(150));
         }
      } else if (mob.isPlayer) {
         return Stream.of(new ModifierValue<>(BuffModifiers.MOB_SPAWN_RATE, 0.0F).max(0.0F, 1000000));
      }

      return Stream.empty();
   }

   @Override
   public boolean isObjectiveDone() {
      return true;
   }

   @Override
   public int getObjectiveCurrent() {
      return 0;
   }

   @Override
   public int getObjectiveMax() {
      return 0;
   }
}
