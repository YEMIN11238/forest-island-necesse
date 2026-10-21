package necesse.entity.projectile;

import java.awt.Color;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.entity.levelEvent.explosionEvent.BombExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.ExplosionEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.DamageSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.FreezeSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.HealSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.NecroPoisonSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.PolymorphSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.SimpleSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.SlimeSplashEvent;
import necesse.entity.levelEvent.explosionEvent.splashEvent.SmiteSplashEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.ThemeColorRange;
import necesse.gfx.ThemeColorRegistry;
import necesse.level.maps.Level;

public class UnlabeledPotionProjectile extends FlaskProjectile {
   protected static final UnlabeledPotionProjectile.PotionPair[] potionPairs = new UnlabeledPotionProjectile.PotionPair[]{
      new UnlabeledPotionProjectile.PotionPair(
         50,
         ThemeColorRegistry.LIGHTNING,
         projectile -> new SmiteSplashEvent(projectile.x, projectile.y, 128, projectile.getDamage().modFinalMultiplier(0.6F), 0.0F, projectile.getOwner())
      ),
      new UnlabeledPotionProjectile.PotionPair(
         50,
         ThemeColorRegistry.FIRE,
         projectile -> new SimpleSplashEvent(
            projectile.x,
            projectile.y,
            96,
            new GameDamage(0.0F),
            0.0F,
            projectile.getOwner(),
            BuffRegistry.Debuffs.GENERIC_ONFIRE,
            3000,
            ThemeColorRegistry.FIRE
         )
      ),
      new UnlabeledPotionProjectile.PotionPair(
         50,
         ThemeColorRegistry.POISON,
         projectile -> new SimpleSplashEvent(
            projectile.x,
            projectile.y,
            96,
            new GameDamage(0.0F),
            0.0F,
            projectile.getOwner(),
            BuffRegistry.Debuffs.GENERIC_POISON,
            4000,
            ThemeColorRegistry.POISON
         )
      ),
      new UnlabeledPotionProjectile.PotionPair(
         50,
         ThemeColorRegistry.BLOOD,
         projectile -> new DamageSplashEvent(projectile.x, projectile.y, 128, projectile.getDamage().modFinalMultiplier(2.0F), 0.0F, projectile.getOwner())
      ),
      new UnlabeledPotionProjectile.PotionPair(
         25,
         ThemeColorRegistry.EMBERGLOW,
         projectile -> new BombExplosionEvent(
            projectile.x, projectile.y, 224, projectile.getDamage().modFinalMultiplier(4.0F), false, false, 0.0F, projectile.getOwner()
         )
      ),
      new UnlabeledPotionProjectile.PotionPair(
         20,
         ThemeColorRegistry.NECROTIC,
         projectile -> new NecroPoisonSplashEvent(projectile.x, projectile.y, 96, projectile.getDamage().modFinalMultiplier(0.2F), 0.0F, projectile.getOwner())
      ),
      new UnlabeledPotionProjectile.PotionPair(
         15,
         ThemeColorRegistry.SLIME,
         projectile -> new SlimeSplashEvent(projectile.x, projectile.y, 64, projectile.getDamage().modFinalMultiplier(0.5F), 0.0F, projectile.getOwner())
      ),
      new UnlabeledPotionProjectile.PotionPair(
         5,
         ThemeColorRegistry.WATER,
         projectile -> new FreezeSplashEvent(projectile.x, projectile.y, 96, new GameDamage(0.0F), 0.0F, projectile.getOwner(), 5000)
      ),
      new UnlabeledPotionProjectile.PotionPair(
         5,
         ThemeColorRegistry.POLYMORPH,
         projectile -> new PolymorphSplashEvent(projectile.x, projectile.y, 128, new GameDamage(0.0F), 0.0F, projectile.getOwner(), 5000)
      ),
      new UnlabeledPotionProjectile.PotionPair(
         5, ThemeColorRegistry.HEAL, projectile -> new HealSplashEvent(projectile.x, projectile.y, 96, new GameDamage(0.0F), 0.0F, projectile.getOwner())
      )
   };
   protected int selectedPotionIndex = 0;

   public UnlabeledPotionProjectile() {
   }

   public UnlabeledPotionProjectile(
      Level level, Mob owner, float x, float y, float targetX, float targetY, float speed, int distance, GameDamage damage, int knockback, GameRandom random
   ) {
      super(level, owner, x, y, targetX, targetY, speed, distance, damage, knockback);
      TicketSystemList ticketList = new TicketSystemList();

      for (int i = 0; i < potionPairs.length; i++) {
         ticketList.addObject(potionPairs[i].chanceTickets, i);
      }

      this.selectedPotionIndex = (Integer)ticketList.getRandomObject(random);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.selectedPotionIndex);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.selectedPotionIndex = reader.getNextInt();
   }

   @Override
   protected void spawnSplashEvent() {
      this.getLevel().entityManager.addLevelEvent(this.getSplashEvent());
   }

   @Override
   public Color getParticleColor() {
      return potionPairs[this.selectedPotionIndex].trailColorRange.getRandomColor();
   }

   protected ExplosionEvent getSplashEvent() {
      ExplosionEvent splashEvent = potionPairs[this.selectedPotionIndex].explosionEventConstructor.constructEvent(this);
      splashEvent.x = this.x;
      splashEvent.y = this.y;
      return splashEvent;
   }

   @Override
   protected void spawnDeathParticles() {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(), MobRegistry.Textures.unlabeledpotiondebris, i, 0, 32, this.x, this.y, this.height, this.dx * 5.0F, this.dy * 5.0F
               ),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @FunctionalInterface
   protected interface ExplosionEventConstructor {
      ExplosionEvent constructEvent(UnlabeledPotionProjectile var1);
   }

   protected static class PotionPair {
      public int chanceTickets;
      public UnlabeledPotionProjectile.ExplosionEventConstructor explosionEventConstructor;
      public ThemeColorRange trailColorRange;

      public PotionPair(int chanceTickets, ThemeColorRange themeColorRange, UnlabeledPotionProjectile.ExplosionEventConstructor explosionEventConstructor) {
         this.chanceTickets = chanceTickets;
         this.trailColorRange = themeColorRange;
         this.explosionEventConstructor = explosionEventConstructor;
      }
   }
}
