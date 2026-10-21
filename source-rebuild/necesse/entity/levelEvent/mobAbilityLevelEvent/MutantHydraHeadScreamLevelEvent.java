package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Polygon;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.gfx.GameResources;
import necesse.level.maps.LevelObjectHit;

public class MutantHydraHeadScreamLevelEvent extends ShockWaveLevelEvent {
   public static int SCREAM_DISTANCE = 800;
   public static int SCREAM_SPEED = 100;

   public MutantHydraHeadScreamLevelEvent() {
      super(360.0F, SCREAM_SPEED, SCREAM_DISTANCE, 40.0F, 40.0F);
   }

   public MutantHydraHeadScreamLevelEvent(Mob owner, int seed) {
      super(owner, owner.getX(), owner.getY(), new GameRandom(seed), 0.0F, 360.0F, SCREAM_SPEED, SCREAM_DISTANCE, 40.0F, 40.0F);
   }

   @Override
   public void init() {
      super.init();
      if (this.owner != null && this.isClient()) {
         this.getClient().addShockwaveEffect(this.owner.getX(), this.owner.getY(), this.maxDistance + 100.0F, 100.0F, 200.0F, 40.0F, (int)this.expandSpeed);
         SoundManager.playSound(GameResources.mutantHydraScream, SoundEffect.effect(this.owner).volume(2.0F).falloffDistance(SCREAM_DISTANCE + 1000));
      }
   }

   @Override
   public void damageTarget(Mob target) {
      target.buffManager.addBuff(new ActiveBuff(BuffRegistry.Debuffs.HYDRA_DEAFENED, target, 5.0F, this.owner), true);
   }

   @Override
   public boolean canHit(Mob mob) {
      return mob.isPlayer && super.canHit(mob);
   }

   @Override
   protected void spawnHitboxParticles(Polygon hitbox) {
   }

   @Override
   protected void spawnHitboxParticles(float radius, float startAngle, float endAngle) {
   }

   @Override
   public void hitObject(LevelObjectHit hit) {
   }
}
