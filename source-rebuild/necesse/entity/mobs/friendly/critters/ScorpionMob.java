package necesse.entity.mobs.friendly.critters;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.SoundSettings;
import necesse.entity.mobs.MaskShaderOptions;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobSpawnLocation;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class ScorpionMob extends CritterMob {
   public static LootTable lootTable = new LootTable();

   public ScorpionMob() {
      this.setSpeed(6.0F);
      this.setFriction(3.0F);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-12, -14, 24, 24);
      this.selectBox = new Rectangle(-16, -24, 32, 30);
      this.swimMaskMove = 6;
      this.swimMaskOffset = 30;
      this.swimSinkOffset = 0;
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.scorpion.body, 12, i, 16, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      int drawX = camera.getDrawX(x) - 15;
      int drawY = camera.getDrawY(y) - 22;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions options = MobRegistry.Textures.scorpion
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
            options.draw();
         }
      });
      TextureDrawOptions shadow = MobRegistry.Textures.scorpion
         .shadow
         .initDraw()
         .sprite(0, dir, 32)
         .light(light)
         .pos(drawX + swimMask.drawXOffset, drawY + swimMask.drawYOffset);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public int getSwimMaskMove() {
      return this.getDir() == 2 ? super.getSwimMaskMove() - 4 : super.getSwimMaskMove();
   }

   @Override
   public int getSwimSinkOffset() {
      return this.getDir() == 2 ? super.getSwimSinkOffset() - 2 : super.getSwimSinkOffset();
   }

   @Override
   public int getRockSpeed() {
      return 3;
   }

   @Override
   public MobSpawnLocation checkSpawnLocation(MobSpawnLocation location) {
      return super.checkSpawnLocation(location).checkTile((tileX, tileY) -> this.getLevel().getTileID(tileX, tileY) == TileRegistry.sandID);
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.scorpionAmbient).volume(0.8F);
   }

   @Override
   protected SoundSettings getDeathSound() {
      return new SoundSettings(GameResources.scorpionDeath).volume(1.2F);
   }
}
