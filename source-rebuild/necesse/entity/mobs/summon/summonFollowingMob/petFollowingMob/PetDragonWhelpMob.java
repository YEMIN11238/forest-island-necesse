package necesse.entity.mobs.summon.summonFollowingMob.petFollowingMob;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.leaves.PlayerFollowerAINode;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class PetDragonWhelpMob extends PetFollowingMob {
   public PetDragonWhelpMob() {
      super(10);
      this.setSpeed(50.0F);
      this.setFriction(2.0F);
      this.moveAccuracy = 10;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-16, -30, 32, 36);
      this.swimMaskMove = 8;
      this.swimMaskOffset = 0;
      this.swimSinkOffset = 0;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PlayerFollowerAINode<>(480, 64));
   }

   @Override
   public int getFlyingHeight() {
      return 50;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.dragonWhelp, i, 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
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
      int tileX = getTileCoordinate(x);
      int tileY = getTileCoordinate(y);
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 44;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(tileX, tileY).getMobSinkingAmount(this);
      boolean aboveLiquid = level.getTile(tileX, tileY).isLiquid;
      int speed = (int)this.getSpeed();
      int height = aboveLiquid ? speed / 2 : (int)GameMath.min(speed / 2.0F, this.getCurrentSpeed() / 2.0F);
      int anim = GameUtils.getAnim(level.getTime(), 4, 200);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.dragonWhelp
         .initDraw()
         .sprite(aboveLiquid ? anim : sprite.x, sprite.y, 64)
         .addMaskShader(swimMask)
         .startGlowOptions(level, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY - height);
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
      GameTexture shadowTexture = MobRegistry.Textures.dragonWhelp_shadow;
      int height = shadowTexture.getHeight();
      int width = shadowTexture.getWidth();
      int drawX = camera.getDrawX(x) - width / 2;
      int drawY = camera.getDrawY(y) - height / 2;
      float alpha = 1.0F - Math.min(this.getCurrentSpeed() / 2.0F / this.getSpeed(), 1.0F);
      return shadowTexture.initDraw().sprite(0, 0, width, height).alpha(alpha).light(light).pos(drawX, drawY + 4);
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.dragonWhelp).volume(0.3F).basePitch(1.0F);
   }
}
