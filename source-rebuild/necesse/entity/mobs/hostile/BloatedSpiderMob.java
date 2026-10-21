package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.entity.levelEvent.explosionEvent.BloatedSpiderExplosionEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.CollisionPlayerChaserWandererAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class BloatedSpiderMob extends HostileMob {
   public static GameDamage collisionDamage = new GameDamage(375.0F);
   public static GameDamage explosionDamage = new GameDamage(375.0F);

   public BloatedSpiderMob() {
      super(350);
      this.setArmor(30);
      this.setSpeed(45.0F);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-13, -13, 26, 26);
      this.hitBox = new Rectangle(-16, -16, 32, 32);
      this.selectBox = new Rectangle(-16, -16, 32, 32);
      this.swimMaskMove = 16;
      this.swimMaskOffset = 10;
      this.swimSinkOffset = 0;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new CollisionPlayerChaserWandererAI<>(null, 512, null, 100, 40000));
   }

   @Override
   public void handleCollisionHit(Mob target, GameDamage damage, int knockback) {
      super.handleCollisionHit(target, damage, knockback);
      if (!target.isCritter) {
         this.remove(0.0F, 0.0F, null, true);
      }
   }

   @Override
   public GameDamage getCollisionDamage(Mob target, boolean fromPacket, ServerClient packetSubmitter) {
      return collisionDamage;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.bloatedSpider.body, i, 8, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   protected void onDeath(Attacker attacker, HashSet<Attacker> attackers) {
      super.onDeath(attacker, attackers);
      if (this.isServer()) {
         BloatedSpiderExplosionEvent event = new BloatedSpiderExplosionEvent(this.x, this.y, 150, explosionDamage, false, 0.0F, this);
         this.getLevel().entityManager.events.add(event);
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
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 32;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.bloatedSpider
         .body
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
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
      TextureDrawOptions shadow = MobRegistry.Textures.bloatedSpider.shadow.initDraw().sprite(sprite.x, sprite.y, 64).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
   }
}
