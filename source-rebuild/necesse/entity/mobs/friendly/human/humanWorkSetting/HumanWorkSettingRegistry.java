package necesse.entity.mobs.friendly.human.humanWorkSetting;

import java.util.ArrayList;

public class HumanWorkSettingRegistry {
   private boolean registryOpen = true;
   private ArrayList<HumanWorkSetting<?>> actions = new ArrayList<>();

   public void closeRegistry() {
      this.registryOpen = false;
   }

   public final HumanWorkSetting<?> getSetting(int id) {
      if (id >= 0 && id < this.actions.size()) {
         return this.actions.get(id);
      }

      System.err.println("Could not find HumanWorkSetting with id " + id);
      return null;
   }

   public final <C extends HumanWorkSetting<?>> C registerSetting(C action) {
      if (!this.registryOpen) {
         throw new IllegalStateException("Cannot register HumanWorkSetting after initialization, must be done in constructor.");
      }

      if (this.actions.size() >= 32767) {
         throw new IllegalStateException("Cannot register any more HumanWorkSetting");
      }

      this.actions.add(action);
      action.onRegister(this, this.actions.size() - 1);
      return action;
   }

   public boolean isEmpty() {
      return this.actions.isEmpty();
   }

   public Iterable<HumanWorkSetting<?>> getSettings() {
      return this.actions;
   }
}
