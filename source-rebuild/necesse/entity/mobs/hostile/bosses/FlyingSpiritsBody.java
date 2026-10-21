package necesse.entity.mobs.hostile.bosses;

import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameLoop.tickManager.TicksPerSecond;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.WormMobHead;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class FlyingSpiritsBody extends BossWormMobBody<FlyingSpiritsHead, FlyingSpiritsBody> {
   public int shadowSprite = 0;
   public int spriteY;
   public FlyingSpiritsHead.Variant variant = FlyingSpiritsHead.Variant.GRIT;
   public boolean spawnsParticles;
   public TicksPerSecond particleSpawner = TicksPerSecond.ticksPerSecond(50);
   public GameDamage collisionDamage;

   public FlyingSpiritsBody() {
      super(1000);
      this.isSummoned = true;
      this.collision = new Rectangle(-25, -20, 50, 40);
      this.hitBox = new Rectangle(-30, -25, 60, 50);
      this.selectBox = new Rectangle(-40, -60, 80, 64);
   }

   @Override
   public GameMessage getLocalization() {
      FlyingSpiritsHead head = this.master.get(this.getLevel());
      return head != null ? head.getLocalization() : new StaticMessage("flyingspiritsbody");
   }

   @Override
   public void init() {
      super.init();
      if (this.getLevel() instanceof IncursionLevel) {
         this.collisionDamage = FlyingSpiritsHead.baseBodyCollisionDamage;
      } else {
         this.collisionDamage = FlyingSpiritsHead.incursionBodyCollisionDamage;
      }
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return this.collisionDamage;
   }

   @Override
   public boolean canCollisionHit(Mob target) {
      return this.height < 45.0F && super.canCollisionHit(target);
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.spawnsParticles && this.isVisible()) {
         this.particleSpawner.gameTick();

         while (this.particleSpawner.shouldTick()) {
            this.getLevel()
               .entityManager
               .addParticle(
                  this.x + GameRandom.globalRandom.floatGaussian() * 15.0F,
                  this.y + GameRandom.globalRandom.floatGaussian() * 10.0F + 5.0F,
                  Particle.GType.COSMETIC
               )
               .movesConstant(GameRandom.globalRandom.floatGaussian() * 6.0F, GameRandom.globalRandom.floatGaussian() * 3.0F)
               .sizeFades(15, 25)
               .color(ParticleOption.randomizeColor(this.variant.particleHue, 0.5F, 0.44F, 0.0F, 0.0F, 0.1F))
               .heightMoves(this.height + 10.0F, this.height + GameRandom.globalRandom.getFloatBetween(30.0F, 40.0F))
               .lifeTime(350);
         }
      }
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(
         new ModifierValue<>(BuffModifiers.SLOW, 0.0F).max(0.0F),
         new ModifierValue<>(BuffModifiers.POISON_DAMAGE_FLAT, 0.0F).max(0.0F),
         new ModifierValue<>(BuffModifiers.FIRE_DAMAGE_FLAT, 0.0F).max(0.0F),
         new ModifierValue<>(BuffModifiers.FROST_DAMAGE_FLAT, 0.0F).max(0.0F)
      );
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      if (this.isVisible()) {
         for (int i = 0; i < 4; i++) {
            this.getLevel()
               .entityManager
               .addParticle(
                  new FleshParticle(
                     this.getLevel(),
                     MobRegistry.Textures.flyingSpirits,
                     4 + this.variant.spriteX,
                     GameRandom.globalRandom.nextInt(6),
                     64,
                     this.x,
                     this.y,
                     20.0F,
                     knockbackX,
                     knockbackY
                  ),
                  Particle.GType.IMPORTANT_COSMETIC
               );
         }
      }
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
      if (this.isVisible()) {
         GameLight light = level.getLightLevel(this);
         int drawX = camera.getDrawX(x) - 64;
         int drawY = camera.getDrawY(y);
         if (this.next != null) {
            Float dir = new Float(this.next.x - x, this.next.y - this.next.height - (y - this.height));
            float angle = GameMath.fixAngle(GameMath.getAngle(dir));
            MobDrawable drawOptions = WormMobHead.getAngledDrawable(
               this,
               new GameSprite(MobRegistry.Textures.flyingSpirits, this.variant.spriteX, this.spriteY, 128),
               null,
               light,
               (int)this.height,
               angle,
               drawX,
               drawY,
               96,
               perspective
            );
            topList.add(drawOptions);
         }

         this.addShadowDrawables(tileList, level, x, y, light, camera);
      }
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.swampGuardian_shadow;
      int res = shadowTexture.getHeight();
      int drawX = camera.getDrawX(x) - res / 2;
      int drawY = camera.getDrawY(y) - res / 2;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(this.shadowSprite, 0, res).light(light).pos(drawX, drawY);
   }
}
