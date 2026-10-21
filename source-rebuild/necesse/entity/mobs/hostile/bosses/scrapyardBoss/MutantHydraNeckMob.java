package necesse.entity.mobs.hostile.bosses.scrapyardBoss;

import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.gameLoop.tickManager.TicksPerSecond;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.FlameTrapEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.BossSimpleChainChildMob;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.LevelMob;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class MutantHydraNeckMob extends BossSimpleChainChildMob<MutantHydraBossMob, MutantHydraNeckMob> {
   public int index;
   public int spriteIndex;
   public int drawOrder;
   public LevelMob<MutantHydraHeadMob> headMob = new LevelMob<>();
   private final TicksPerSecond particleSpawner = TicksPerSecond.ticksPerSecond(20);

   public MutantHydraNeckMob() {
      super(1000);
      this.setSpeed(0.0F);
      this.setFriction(0.0F);
      this.collision = new Rectangle(-10, -10, 20, 20);
      this.hitBox = new Rectangle(-10, -10, 20, 20);
      this.selectBox = new Rectangle(-15, -15, 30, 30);
   }

   public MutantHydraHeadMob getHeadMob() {
      return this.headMob.get(this.getLevel());
   }

   public void apply(MutantHydraBossMob.NeckIndexAndDistance indexAndDistance) {
      this.spriteIndex = indexAndDistance.spriteIndex;
      this.collision = indexAndDistance.neckCountAndDistance.collision;
      this.hitBox = indexAndDistance.neckCountAndDistance.hitbox;
      this.selectBox = indexAndDistance.neckCountAndDistance.selectBox;
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return MutantHydraBossMob.neckContactDamage;
   }

   @Override
   public int getCollisionKnockback(Mob target) {
      return 150;
   }

   @Override
   public boolean isVisible() {
      return super.isVisible() && this.canHit;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isVisible()) {
         MutantHydraHeadMob headMob = this.getHeadMob();
         if (headMob != null && headMob.isRumbling) {
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
                  .colorRandom(130.0F, 0.5F, 0.3F, 10.0F, 0.1F, 0.1F)
                  .sizeFades(20, 30)
                  .heightMoves(10.0F, GameRandom.globalRandom.getFloatBetween(30.0F, 40.0F))
                  .lifeTime(400);
            }
         }
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      if (this.isVisible()) {
         int particles = GameRandom.globalRandom.getIntBetween(1, 2);

         for (int i = 0; i < particles; i++) {
            int spriteX = GameRandom.globalRandom.nextInt(4);
            this.getLevel()
               .entityManager
               .addParticle(
                  new FleshParticle(
                     this.getLevel(),
                     MobRegistry.Textures.mutantHydraNecks,
                     spriteX,
                     3,
                     32,
                     this.x + GameRandom.globalRandom.floatGaussian() * 15.0F,
                     this.y + GameRandom.globalRandom.floatGaussian() * 15.0F,
                     10.0F,
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
         GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
         int spriteRes = 96;
         GameTexture texture = MobRegistry.Textures.mutantHydraNecks;
         int drawX = camera.getDrawX(x) - spriteRes / 2;
         int drawY = camera.getDrawY(y) - spriteRes / 2;
         DrawOptions drawOptions = texture.initDraw()
            .sprite(this.spriteIndex, 0, spriteRes)
            .startGlowOptions(this, this.getID())
            .light(light)
            .applyEnemyTracker(this, perspective)
            .pos(drawX, drawY);
         topList.add(this.drawOrder, tm -> drawOptions.draw());
      }
   }

   @Override
   protected int getDrawSortY(Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective, boolean fromMount) {
      return super.getDrawSortY(level, x, y, tickManager, camera, perspective, fromMount) + 10;
   }

   @Override
   public int getFlyingHeight() {
      return 50;
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.SLOW, 0.0F).max(0.0F));
   }

   @Override
   public boolean canBeHit(Attacker attacker) {
      return super.canBeHit(attacker) && !(attacker instanceof FlameTrapEvent);
   }
}
