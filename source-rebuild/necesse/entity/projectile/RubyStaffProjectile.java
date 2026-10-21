package necesse.entity.projectile;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.levelEvent.explosionEvent.RubyStaffExplosionEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.ThemeColorRegistry;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public class RubyStaffProjectile extends Projectile {
   protected float startSpeed;
   protected int explosionRange;
   private final ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC
   );
   protected AtomicInteger givesLifeEssence = new AtomicInteger();

   public RubyStaffProjectile() {
   }

   public RubyStaffProjectile(
      Level level,
      Mob owner,
      float x,
      float y,
      float targetX,
      float targetY,
      float speed,
      int distance,
      GameDamage damage,
      int knockback,
      int explosionRange,
      AtomicInteger givesLifeEssence
   ) {
      this.setLevel(level);
      this.setOwner(owner);
      this.x = x;
      this.y = y;
      this.setTarget(targetX, targetY);
      this.speed = speed;
      this.startSpeed = speed;
      this.distance = distance;
      this.setDamage(damage);
      this.knockback = knockback;
      this.explosionRange = explosionRange;
      this.givesLifeEssence = givesLifeEssence;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.startSpeed);
      writer.putNextShortUnsigned(this.explosionRange);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.startSpeed = reader.getNextFloat();
      this.explosionRange = reader.getNextShortUnsigned();
   }

   @Override
   public void init() {
      super.init();
      this.height = 6.0F;
      this.piercing = 0;
      this.width = 32.0F;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.getLevel()
         .entityManager
         .addParticle(
            this.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
            this.y + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
            this.typeSwitcher.next()
         )
         .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 22))
         .color(this.getParticleColor())
         .height(6.0F)
         .sizeFades(22, 44)
         .fadesAlpha(0.1F, 0.1F);
   }

   @Override
   public Color getParticleColor() {
      return ThemeColorRegistry.RUBY.getRandomColor();
   }

   @Override
   public Trail getTrail() {
      return new Trail(this, this.getLevel(), ThemeColorRegistry.RUBY.getRandomColor(), this.getTrailThickness(), 600, this.getHeight());
   }

   @Override
   public float getTrailThickness() {
      return this.traveledDistance <= 50.0F ? 0.0F : GameMath.lerp(GameMath.limit(this.traveledDistance / 300.0F, 0.0F, 1.0F), 2.0F, 12.0F);
   }

   @Override
   public float tickMovement(float delta) {
      float distanceProgress = GameMath.limit(this.traveledDistance / this.distance, 0.0F, 1.0F);
      this.speed = GameMath.lerp((float)Math.pow(distanceProgress, 0.3F), this.startSpeed, 30.0F);
      return super.tickMovement(delta);
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      Mob owner = this.getOwner();
      if (this.isServer() && mob != null && owner != null && this.givesLifeEssence.get() > 0) {
         Float gainMod = owner.buffManager.getModifier(BuffModifiers.LIFE_ESSENCE_GAIN);
         Float durationMod = owner.buffManager.getModifier(BuffModifiers.LIFE_ESSENCE_DURATION);

         for (int i = 0; i < Math.floor(gainMod.floatValue()) + (GameRandom.globalRandom.getChance(gainMod % 1.0F) ? 1 : 0); i++) {
            owner.buffManager.addBuff(new ActiveBuff(BuffRegistry.LIFE_ESSENCE, owner, 60.0F * durationMod, null), true);
         }

         this.givesLifeEssence.decrementAndGet();
      }

      if (this.isServer()) {
         this.triggerExplosion(mob);
      }
   }

   public void triggerExplosion(Mob target) {
      GameDamage explosionDamage = this.getDamage().modDamage(2.0F);
      RubyStaffExplosionEvent event = new RubyStaffExplosionEvent(
         this.x, this.y, this.explosionRange, explosionDamage, false, 0.0F, this.getOwner(), this.givesLifeEssence, target
      );
      this.getLevel().entityManager.events.add(event);
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      OrderableDrawables overlayList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
   }
}
