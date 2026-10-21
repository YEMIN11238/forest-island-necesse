package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.levelEvent.mobAbilityLevelEvent.SmallGroundWebEvent;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class WebSpinnerMob extends HostileMob {
   public static GameDamage collisionDamage = new GameDamage(80.0F);
   private long sinceLastWeb;
   Point lastWebPos;

   public WebSpinnerMob() {
      super(250);
      this.setArmor(30);
      this.setSpeed(50.0F);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-18, -24, 36, 36);
      this.swimMaskMove = 8;
      this.swimMaskOffset = 26;
      this.swimSinkOffset = 0;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new ConfusedCollisionPlayerChaserWandererAI<>(null, 384, collisionDamage, 25, 5000));
      this.sinceLastWeb = this.getLevel().getWorldEntity().getLocalTime();
      this.lastWebPos = this.getPositionPoint();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.getLevel().getWorldEntity().getLocalTime() - this.sinceLastWeb > 125L && 25.0 < this.getPositionPoint().distance(this.lastWebPos)) {
         SmallGroundWebEvent event = new SmallGroundWebEvent(this, (int)this.x, (int)this.y, GameRandom.globalRandom);
         this.getLevel().entityManager.events.add(event);
         this.sinceLastWeb = this.getLevel().getWorldEntity().getLocalTime();
         this.lastWebPos = this.getPositionPoint();
      }
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.webSpinner.body, i, 4, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 16;
      int drawY = camera.getDrawY(y) - 16;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.webSpinner
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
      TextureDrawOptions shadow = MobRegistry.Textures.webSpinner.shadow.initDraw().sprite(sprite.x, sprite.y, 32).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
   }
}
