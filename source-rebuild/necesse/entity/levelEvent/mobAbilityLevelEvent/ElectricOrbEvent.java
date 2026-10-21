package necesse.entity.levelEvent.mobAbilityLevelEvent;

import java.awt.Color;
import java.awt.Shape;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.levelEvent.explosionEvent.ElectricOrbExplosionLevelEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.particle.ElectricOrbParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.level.maps.LevelObjectHit;

public class ElectricOrbEvent extends GroundEffectEvent {
   private ElectricOrbParticle particle;
   private GameDamage damage;
   private int tickCounter;
   private Mob owner;
   private final float lifetime = 0.75F;

   public ElectricOrbEvent() {
   }

   public ElectricOrbEvent(Mob owner, int x, int y, GameRandom uniqueID, GameDamage damage) {
      super(owner, x, y, uniqueID);
      this.owner = owner;
      this.damage = damage;
   }

   @Override
   public void init() {
      super.init();
      if (this.isClient()) {
         this.level.entityManager.addParticle(this.particle = new ElectricOrbParticle(this.level, this.x, this.y, 750L), true, Particle.GType.CRITICAL);
         GameRandom random = GameRandom.globalRandom;
         ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);

         for (int i = 0; i < 20; i++) {
            float particleX = GameRandom.globalRandom.floatGaussian() * 25.0F;
            float particleY = GameRandom.globalRandom.floatGaussian() * 25.0F;
            float var10001 = this.x;
            this.getLevel()
               .entityManager
               .addParticle(var10001, this.y, typeSwitcher.next())
               .sprite(GameResources.magicSparkParticles.sprite(random.nextInt(4), 0, 22))
               .sizeFades(22, 44)
               .movesFriction(particleX, particleY, 0.8F)
               .color(new Color(95, 205, 228))
               .givesLight(190.0F, 0.9F)
               .ignoreLight(true)
               .heightMoves(0.0F, 30.0F)
               .lifeTime(500);
         }
      }
   }

   @Override
   public Shape getHitBox() {
      return null;
   }

   @Override
   public void clientHit(Mob target) {
   }

   @Override
   public void serverHit(Mob target, boolean clientSubmitted) {
   }

   @Override
   public void hitObject(LevelObjectHit hit) {
   }

   @Override
   public void clientTick() {
      this.tickCounter++;
      if (this.tickCounter > 15.0F) {
         this.over();
      } else {
         super.clientTick();
      }
   }

   @Override
   public void serverTick() {
      this.tickCounter++;
      if (this.tickCounter > 15.0F) {
         ElectricOrbExplosionLevelEvent event = new ElectricOrbExplosionLevelEvent(this.x, this.y, 100, this.damage, false, 0.0F, this.owner);
         this.level.entityManager.events.add(event);
         this.over();
      } else {
         super.serverTick();
      }
   }

   @Override
   public void over() {
      super.over();
      if (this.particle != null) {
         this.particle.despawnNow();
      }
   }
}
