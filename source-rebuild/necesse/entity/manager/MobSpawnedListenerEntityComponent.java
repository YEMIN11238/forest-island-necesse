package necesse.entity.manager;

import necesse.entity.mobs.Mob;

public interface MobSpawnedListenerEntityComponent extends EntityComponent {
   void onLevelMobSpawned(Mob var1);
}
