package necesse.entity.mobs.hostile;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.stream.Stream;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.DeathMessageTable;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionPlayerChaserWandererAI;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class HaywireShredderMob extends HostileMob {
   public static GameDamage collisionDamage = new GameDamage(90.0F);

   public HaywireShredderMob() {
      super(400);
      this.setDefaultStats();
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -32, 28, 48);
      this.selectBox = new Rectangle(-14, -61, 28, 80);
      this.swimMaskMove = 32;
      this.swimMaskOffset = -32;
      this.swimSinkOffset = -8;
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultModifiers() {
      return Stream.of(new ModifierValue<>(BuffModifiers.BOUNCY, true));
   }

   private void setDefaultStats() {
      this.setArmor(40);
      this.setSpeed(200.0F);
      this.setFriction(0.5F);
      this.setKnockbackModifier(1.25F);
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new CollisionPlayerChaserWandererAI<>(null, 512, collisionDamage, 100, 40000));
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.currentSpeed > 10.0F) {
         int moveX = GameRandom.globalRandom.getIntBetween(-55, 55);
         int moveY = GameRandom.globalRandom.getIntBetween(-1, -20);
         this.getLevel()
            .entityManager
            .addParticle(this.x, this.y, Particle.GType.COSMETIC)
            .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 22))
            .color(new Color(255, 188, 78))
            .sizeFades(22, 44)
            .movesConstant(moveX, moveY)
            .ignoreLight(true)
            .givesLight(37.0F, 1.0F)
            .lifeTime(200);
         SoundManager.playSound(new SoundSettings(GameResources.haywireShredder).volume(this.currentSpeed / 100.0F / 3.0F).pitchVariance(0.1F), this);
      }
   }

   @Override
   public int stoppingDistance(float friction, float currentSpeed) {
      return 0;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(
                  this.getLevel(),
                  MobRegistry.Textures.haywireShredder,
                  GameRandom.globalRandom.nextInt(5),
                  12,
                  32,
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

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("scrapyard", 5);
   }

   @Override
   public void addDrawables(
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
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 80;
      int dir = this.getDir();
      float angle = GameMath.limit(this.dx / 10.0F, -10.0F, 10.0F) + (this.currentSpeed > 10.0F ? GameRandom.globalRandom.getFloatBetween(-5.0F, 5.0F) : 0.0F);
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions drawOptions = MobRegistry.Textures.haywireShredder
         .initDraw()
         .sprite(sprite.x, sprite.y, 64, 96)
         .addMaskShader(swimMask)
         .rotate(angle, 32, 80)
         .light(light)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            drawOptions.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   public boolean isSlimeImmune() {
      return true;
   }
}
