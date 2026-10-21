package necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.PlayerFollowerAINode;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class PetWalkingTorchMob extends PetFollowingMob {
   public PetWalkingTorchMob() {
      super(10);
      this.setSpeed(50.0F);
      this.setFriction(2.0F);
      this.moveAccuracy = 10;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-16, -26, 32, 36);
      this.swimMaskMove = 12;
      this.swimMaskOffset = 24;
      this.swimSinkOffset = 0;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PlayerFollowerAINode<>(480, 32));
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y, 270.0F, 0.5F, 165);
   }

   @Override
   public void playDeathSound() {
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
      int drawX = camera.getDrawX(x) - 16;
      int drawY = camera.getDrawY(y) - 26;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.walkingTorch
         .initDraw()
         .sprite(sprite.x, sprite.y, 32)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            options.draw();
            swimMask.stop();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   @Override
   protected TextureDrawOptions getShadowDrawOptions(Level level, int x, int y, GameLight light, GameCamera camera) {
      GameTexture shadowTexture = MobRegistry.Textures.human_baby_shadow;
      int res = shadowTexture.getHeight();
      int drawX = camera.getDrawX(x) - res / 2;
      int drawY = camera.getDrawY(y) - res / 2;
      drawY += this.getBobbing(x, y);
      return shadowTexture.initDraw().sprite(this.getDir(), 0, res).light(light).pos(drawX, drawY);
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 7; i++) {
         this.getLevel()
            .entityManager
            .addParticle(this.x, this.y, Particle.GType.CRITICAL)
            .movesConstant(GameRandom.globalRandom.getFloatBetween(-12.0F, 12.0F), GameRandom.globalRandom.getFloatBetween(-12.0F, 12.0F))
            .color(GameRandom.globalRandom.getOneOf(new Color(168, 21, 72), new Color(174, 10, 35), new Color(243, 173, 194)))
            .heightMoves(8.0F, 16.0F)
            .sizeFades(12, 18)
            .givesLight(250.0F, 0.3F)
            .lifeTime(600);
      }
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.walkingTorch).volume(0.4F);
   }
}
