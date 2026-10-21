package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.entity.mobs.DeathMessageTable;
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
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class IncursionCrawlingZombieMob extends HostileMob {
   public static LootTable lootTable = ZombieMob.lootTable;
   private final GameDamage damage = new GameDamage(115.0F);

   public IncursionCrawlingZombieMob() {
      super(350);
      this.setSpeed(55.0F);
      this.setFriction(4.0F);
      this.isSummoned = true;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-18, -24, 36, 36);
      this.swimMaskMove = 16;
      this.swimMaskOffset = 0;
      this.swimSinkOffset = 0;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new ConfusedCollisionPlayerChaserWandererAI<>(this::shouldLevelRetreat, 576, this.damage, 100, 40000));
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("zombie", 3);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.crawlingZombie.body, 12, i, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      int drawX = camera.getDrawX(x) - 32;
      int drawY = camera.getDrawY(y) - 40;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions body = MobRegistry.Textures.crawlingZombie
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
            body.draw();
         }
      });
      TextureDrawOptions shadow = MobRegistry.Textures.crawlingZombie
         .shadow
         .initDraw()
         .sprite(sprite.x, sprite.y, 64)
         .light(light)
         .pos(drawX + swimMask.drawXOffset, drawY + swimMask.drawYOffset);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public int getSwimMaskOffset() {
      int dir = this.getDir();
      return dir != 0 && dir != 2 ? super.getSwimMaskOffset() : super.getSwimMaskOffset() - 4;
   }

   @Override
   public int getRockSpeed() {
      return 15;
   }
}
