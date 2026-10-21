package necesse.entity.mobs.summon.summonFollowingMob.attackingFollowingMob;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.PlayerFollowerCollisionChaserAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class ArachnidSpiderMob extends AttackingFollowingMob {
   public ArachnidSpiderMob() {
      super(10);
      this.setSpeed(50.0F);
      this.setFriction(2.0F);
      this.attackCooldown = 500;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-13, -14, 26, 24);
      this.swimMaskMove = 8;
      this.swimMaskOffset = 26;
      this.swimSinkOffset = 0;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new PlayerFollowerCollisionChaserAI<>(576, this.summonDamage, 30, 500, 640, 64));
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.arachnidSpider.body, 12, i, 16, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      GameLight light = level.getLightLevel(x / 32, y / 32);
      int drawX = camera.getDrawX(x) - 15;
      int drawY = camera.getDrawY(y) - 22;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += this.getLevel().getTile(x / 32, y / 32).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.arachnidSpider
         .body
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
      TextureDrawOptions shadow = MobRegistry.Textures.spiderHatchling.shadow.initDraw().sprite(sprite.x, sprite.y, 32).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public int getRockSpeed() {
      return 10;
   }
}
