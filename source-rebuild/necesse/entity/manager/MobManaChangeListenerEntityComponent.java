package necesse.entity.manager;

import necesse.entity.mobs.Mob;

public interface MobManaChangeListenerEntityComponent extends EntityComponent {
   void onLevelMobManaChanged(Mob var1, float var2, float var3);
}
