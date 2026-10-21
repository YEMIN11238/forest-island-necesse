package necesse.entity.manager;

import necesse.entity.mobs.Mob;

public interface OnMobAddedListenerEntityComponent extends EntityComponent {
   void onMobSpawned(Mob var1);
}
