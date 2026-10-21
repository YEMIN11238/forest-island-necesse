package necesse.entity.mobs.hostile.bosses.ascendedWizard;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ProjectileRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.AscendedPylonDummyMob;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.LevelMob;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.FlyingHostileMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class AscendedBeamMob extends FlyingHostileMob {
   public final LevelMob<Mob> master = new LevelMob<>();
   public GameDamage damage = AscendedPylonDummyMob.ASCENDED_BEAM_DAMAGE;
   public int remainingLifeTime = 15000;
   protected long spawnTime;
   private SoundPlayer sound;
   private float particleBuffer;
   protected ParticleTypeSwitcher particleTypeSwitcher = new ParticleTypeSwitcher(
      Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC, Particle.GType.CRITICAL
   );

   public AscendedBeamMob() {
      super(100);
      this.moveAccuracy = 5;
      this.setSpeed(100.0F);
      this.setFriction(1.0F);
      this.setKnockbackModifier(0.0F);
      this.collision = new Rectangle(-25, -30, 50, 50);
      this.hitBox = new Rectangle(-30, -35, 60, 60);
      this.selectBox = new Rectangle(-20, -40, 40, 60);
      this.isSummoned = true;
      this.isStatic = true;
      this.shouldSave = false;
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.remainingLifeTime);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.remainingLifeTime = reader.getNextInt();
   }

   @Override
   public void init() {
      super.init();
      this.spawnTime = this.getTime();
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.sound == null || this.sound.isDone()) {
         this.sound = SoundManager.playSound(GameResources.laserBeam1, SoundEffect.effect(this).falloffDistance(600).volume(0.0F));
         if (this.sound != null) {
            this.sound.fadeIn(1.0F);
            this.sound.effect.volume(0.2F);
         }
      }

      if (this.sound != null) {
         this.sound.refreshLooping(1.0F);
      }

      this.remainingLifeTime -= 50;
      if (this.remainingLifeTime >= 500) {
         this.getLevel()
            .entityManager
            .addParticle(this, GameRandom.globalRandom.getIntBetween(-20, 20), 0.0F, Particle.GType.COSMETIC)
            .sprite(GameResources.ascendedParticle.sprite(0, 0, 20))
            .ignoreLight(true)
            .sizeFades(20, 40)
            .movesConstant(GameRandom.globalRandom.getIntBetween(-20, 20), GameRandom.globalRandom.getIntBetween(-20, -40))
            .height(16.0F)
            .lifeTime(500);
         float particlesPerSecond = 80.0F;
         this.particleBuffer += particlesPerSecond / 20.0F;

         while (this.particleBuffer >= 1.0F) {
            this.particleBuffer--;
            GameRandom random = GameRandom.globalRandom;
            AtomicReference<Float> currentAngle = new AtomicReference<>(random.nextFloat() * 360.0F);
            float distance = 75.0F;
            this.getLevel()
               .entityManager
               .addParticle(
                  this.x + GameMath.sin(currentAngle.get()) * distance + random.getIntBetween(-5, 5),
                  this.y + GameMath.cos(currentAngle.get()) * distance + random.getIntBetween(-5, 5) * 0.85F,
                  this.particleTypeSwitcher.next()
               )
               .sprite(GameResources.puffParticles.sprite(random.getIntBetween(0, 4), 0, 12))
               .height(16.0F)
               .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
                  float angle = currentAngle.accumulateAndGet(delta * 150.0F / 250.0F, Float::sum);
                  float distY = distance * lifePercent * 0.85F;
                  pos.x = this.x + GameMath.sin(angle) * (distance * lifePercent);
                  pos.y = this.y + GameMath.cos(angle) * distY * 0.85F;
               })
               .color((options, lifeTime, timeAlive, lifePercent) -> {
                  float clampedLifePercent = Math.max(0.0F, Math.min(1.0F, lifePercent));
                  options.color(new Color(255, (int)(255.0F - 255.0F * clampedLifePercent), (int)(255.0F - 24.0F * clampedLifePercent)));
               })
               .ignoreLight(true)
               .lifeTime(1000)
               .sizeFades(50, 24);
         }
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      this.remainingLifeTime -= 50;
      if (this.remainingLifeTime <= 0) {
         this.remove(0.0F, 0.0F, null, true);
      }
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return AscendedPylonDummyMob.ASCENDED_BEAM_DAMAGE;
   }

   @Override
   public int getCollisionKnockback(Mob target) {
      return 150;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return new SoundSettings(GameResources.magicbolt2).volume(0.8F).pitchVariance(0.1F);
   }

   @Override
   protected void addDrawables(
      List<MobDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      Level level,
      int x,
      int y,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
      int projectileID = ProjectileRegistry.getProjectileID("ascendedbeam");
      GameTexture texture = ProjectileRegistry.Textures.getTexture(projectileID);
      GameTexture shadowTexture = ProjectileRegistry.Textures.getShadowTexture(projectileID);
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - texture.getWidth() / 2 + 16;
      int drawY = camera.getDrawY(y) - texture.getHeight();
      int anim = GameUtils.getAnim(this.getWorldEntity().getTime(), 2, 200);
      float alpha = 1.0F;
      if (this.remainingLifeTime < 500) {
         alpha *= this.remainingLifeTime / 500.0F;
      }

      long timeSinceSpawned = this.getTime() - this.spawnTime;
      if (timeSinceSpawned < 500L) {
         alpha *= (float)timeSinceSpawned / 500.0F;
      }

      final TextureDrawOptions options = texture.initDraw()
         .sprite(anim, 0, 32, 640)
         .startGlowOptions(level, this.getID())
         .light(light.minLevelCopy(150.0F))
         .applyEnemyTracker(this, perspective)
         .alpha(alpha)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
      TextureDrawOptions shadowOptions = shadowTexture.initDraw()
         .light(light.minLevelCopy(150.0F))
         .pos(drawX - shadowTexture.getWidth() / 2 + 16, drawY + texture.getHeight() - shadowTexture.getHeight() / 2 - 16);
      tileList.add(tm -> shadowOptions.draw());
   }

   @Override
   public Mob getAttackOwner() {
      Mob master = this.master.get(this.getLevel());
      return master != null ? master : super.getAttackOwner();
   }

   @Override
   public GameMessage getAttackerName() {
      Mob master = this.master.get(this.getLevel());
      return master != null ? master.getAttackerName() : super.getAttackerName();
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      Mob master = this.master.get(this.getLevel());
      return master != null ? master.getDeathMessages() : super.getDeathMessages();
   }
}
