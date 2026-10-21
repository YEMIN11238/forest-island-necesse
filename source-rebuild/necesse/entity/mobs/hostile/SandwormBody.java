package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameLoop.tickManager.TicksPerSecond;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.ComputedValue;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.WormMobHead;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class SandwormBody extends HostileWormMobBody<SandwormHead, SandwormBody> {
   ParticleTypeSwitcher pTypeSwitcher = new ParticleTypeSwitcher(Particle.GType.COSMETIC, Particle.GType.IMPORTANT_COSMETIC);
   public Point sprite = new Point(0, 0);
   private final TicksPerSecond particleSpawner = TicksPerSecond.ticksPerSecond(20);
   public GameDamage collisionDamage;

   public SandwormBody() {
      super(1200);
      this.setArmor(15);
      this.collision = new Rectangle(-20, -15, 40, 30);
      this.hitBox = new Rectangle(-25, -20, 50, 40);
      this.selectBox = new Rectangle(-32, -60, 64, 64);
   }

   @Override
   public void init() {
      super.init();
      if (this.getLevel() instanceof IncursionLevel) {
         this.collisionDamage = SandwormHead.bodyCollisionDamage;
      } else {
         this.collisionDamage = SandwormHead.incursionBodyCollisionDamage;
      }
   }

   @Override
   public GameMessage getLocalization() {
      return new LocalMessage("mob", "sandworm");
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return this.collisionDamage;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isVisible()) {
         this.particleSpawner.gameTick();

         while (this.particleSpawner.shouldTick()) {
            ComputedValue<GameObject> obj = new ComputedValue<>(() -> this.getLevel().getObject(this.getTileX(), this.getTileY()));
            if (!(this.height < 20.0F) || !obj.get().isWall && !obj.get().isRock) {
               if (this.height < 0.0F) {
                  this.getLevel()
                     .entityManager
                     .addParticle(
                        this.x + GameRandom.globalRandom.floatGaussian() * 10.0F,
                        this.y + GameRandom.globalRandom.floatGaussian() * 7.0F + 5.0F,
                        this.pTypeSwitcher.next()
                     )
                     .movesConstant(GameRandom.globalRandom.floatGaussian() * 6.0F, GameRandom.globalRandom.floatGaussian() * 3.0F)
                     .smokeColor()
                     .heightMoves(10.0F, GameRandom.globalRandom.getFloatBetween(30.0F, 40.0F))
                     .lifeTime(200);
               }
            } else {
               this.getLevel()
                  .entityManager
                  .addTopParticle(
                     this.x + GameRandom.globalRandom.floatGaussian() * 10.0F,
                     this.y + GameRandom.globalRandom.floatGaussian() * 7.0F + 5.0F,
                     this.pTypeSwitcher.next()
                  )
                  .movesConstant(GameRandom.globalRandom.floatGaussian() * 6.0F, GameRandom.globalRandom.floatGaussian() * 3.0F)
                  .smokeColor()
                  .heightMoves(10.0F, GameRandom.globalRandom.getFloatBetween(30.0F, 40.0F))
                  .lifeTime(200);
            }
         }
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      if (this.isVisible()) {
         for (int i = 0; i < 3; i++) {
            this.getLevel()
               .entityManager
               .addParticle(
                  new FleshParticle(
                     this.getLevel(), MobRegistry.Textures.sandWorm, 2, GameRandom.globalRandom.nextInt(6), 32, this.x, this.y, 20.0F, knockbackX, knockbackY
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
         int drawX = camera.getDrawX(x) - 32;
         int drawY = camera.getDrawY(y);
         WormMobHead.addDrawable(
            list,
            this,
            new GameSprite(MobRegistry.Textures.sandWorm, this.sprite.x, this.sprite.y, 64),
            MobRegistry.Textures.sandWorm_mask,
            light,
            (int)this.height,
            drawX,
            drawY,
            64,
            perspective
         );
         this.addShadowDrawables(tileList, level, x, y, light, camera);
      }
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.sandWorm_shadow;
      int drawX = camera.getDrawX(x) - shadowTexture.getWidth() / 2;
      int drawY = camera.getDrawY(y) - shadowTexture.getHeight() / 2;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().light(light).pos(drawX, drawY);
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.FIRE_DAMAGE, 0.0F).max(0.0F));
   }
}
