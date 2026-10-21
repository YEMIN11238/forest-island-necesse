package necesse.entity.projectile.followingProjectile;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Point2D.Float;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.trails.Trail;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObjectHit;

public class TicTacToePunishProjectile extends FollowingProjectile {
   private boolean isXProjectile;
   private float startVelocity;
   private float maxVelocity;

   public TicTacToePunishProjectile() {
   }

   public TicTacToePunishProjectile(Level level, float x, float y, Mob target, int seed, float speed, GameDamage damage, int knockback, boolean isXProjectile) {
      this();
      this.setLevel(level);
      this.x = x;
      this.y = y;
      Float dir = GameMath.normalize(target.getX() - x, target.getY() - y);
      GameRandom random = new GameRandom(seed);
      this.setTarget(x - dir.x * 100.0F + random.getFloatBetween(-5.0F, 5.0F), y - dir.y * 100.0F + random.getFloatBetween(-5.0F, 5.0F));
      this.target = target;
      this.startVelocity = speed / 15.0F;
      this.maxVelocity = speed;
      this.speed = this.startVelocity;
      this.setDamage(damage);
      this.setDistance(4000);
      this.knockback = knockback;
      this.isXProjectile = isXProjectile;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextFloat(this.startVelocity);
      writer.putNextFloat(this.maxVelocity);
      writer.putNextBoolean(this.isXProjectile);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.startVelocity = reader.getNextFloat();
      this.maxVelocity = reader.getNextFloat();
      this.isXProjectile = reader.getNextBoolean();
   }

   @Override
   public void init() {
      super.init();
      this.turnSpeed = 0.75F;
      this.isSolid = false;
      this.givesLight = true;
      this.height = 16.0F;
      this.piercing = 0;
      this.trailOffset = 0.0F;
      this.particleDirOffset = -24.0F;
      this.setWidth(8.0F);
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.target == null) {
         this.remove();
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.target == null) {
         this.remove();
      }
   }

   @Override
   public void onMoveTick(Float startPos, double movedDist) {
      super.onMoveTick(startPos, movedDist);
      float progress = GameMath.limit(this.traveledDistance / 300.0F, 0.0F, 1.0F);
      this.speed = GameMath.lerp(progress, this.startVelocity, this.maxVelocity);
   }

   @Override
   public float getTurnSpeed(int targetX, int targetY, float delta) {
      return this.getTurnSpeed(delta) * this.dynamicTurnSpeedMod(targetX, targetY, this.getTurnRadius());
   }

   @Override
   public void doHitLogic(Mob mob, LevelObjectHit object, float x, float y) {
      super.doHitLogic(mob, object, x, y);
      if (mob != null && this.isClient()) {
         SoundManager.playSound(GameResources.explosionLight, SoundEffect.effect(this).falloffDistance(1400).volume(mob.isPlayer ? 0.6F : 1.5F));
         SoundManager.playSound(GameResources.electricExplosion, SoundEffect.effect(this).falloffDistance(1400).volume(mob.isPlayer ? 0.6F : 1.5F));

         for (int i = 0; i < 30; i++) {
            int lifeTime = GameRandom.globalRandom.getIntBetween(500, 3000);
            float lifePerc = lifeTime / 3000.0F;
            float startHeight = 10.0F;
            float height = startHeight + GameRandom.globalRandom.getIntBetween(50, 100) * lifePerc;
            this.getLevel()
               .entityManager
               .addTopParticle(
                  x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
                  y + GameRandom.globalRandom.getFloatBetween(-5.0F, 5.0F),
                  Particle.GType.IMPORTANT_COSMETIC
               )
               .sizeFades(20, 30)
               .movesFriction(
                  GameRandom.globalRandom.getFloatBetween(-40.0F, 40.0F) + this.dx * this.speed / 4.0F,
                  GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F) + this.dy * this.speed / 4.0F,
                  0.5F
               )
               .heightMoves(startHeight, height)
               .colorRandom(this.getParticleColor(), 10.0F, 0.1F, 0.1F)
               .lifeTime(lifeTime);
         }
      }
   }

   @Override
   protected int getExtraSpinningParticles() {
      return 5;
   }

   @Override
   public Color getParticleColor() {
      return this.isXProjectile ? new Color(255, 125, 131) : new Color(148, 225, 255);
   }

   @Override
   public Trail getTrail() {
      Trail trail = new Trail(this, this.getLevel(), this.getParticleColor(), 12.0F, 500, 18.0F);
      trail.drawOnTop = true;
      return trail;
   }

   @Override
   public boolean canHit(Mob mob) {
      return mob == this.target;
   }

   @Override
   protected Stream<Mob> streamTargets(Mob owner, Shape hitBounds) {
      return this.target instanceof Mob ? Stream.of((Mob)this.target) : super.streamTargets(owner, hitBounds);
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
