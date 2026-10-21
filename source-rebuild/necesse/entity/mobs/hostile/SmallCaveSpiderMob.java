package necesse.entity.mobs.hostile;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.TileRegistry;
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
import necesse.inventory.item.matItem.MultiTextureMatItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.TilePosition;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.light.GameLight;

public class SmallCaveSpiderMob extends HostileMob {
   public static LootTable lootTable = new LootTable(
      LootItem.between("silk", 1, 2), LootItem.between("cavespidergland", 1, 2, MultiTextureMatItem.getGNDData(2))
   );
   public static GameDamage baseDamage = new GameDamage(70.0F);
   public static GameDamage incursionDamage = new GameDamage(75.0F);

   public SmallCaveSpiderMob() {
      super(350);
      this.setSpeed(50.0F);
      this.setFriction(3.0F);
      this.setKnockbackModifier(0.6F);
      this.setArmor(25);
      this.collision = new Rectangle(-11, -11, 22, 22);
      this.hitBox = new Rectangle(-25, -18, 50, 36);
      this.selectBox = new Rectangle(-25, -35, 50, 40);
      this.swimMaskMove = 16;
      this.swimMaskOffset = -16;
      this.swimSinkOffset = 0;
   }

   @Override
   public void init() {
      super.init();
      GameDamage damage;
      if (this.getLevel() instanceof IncursionLevel) {
         this.setMaxHealth(400);
         this.setHealthHidden(this.getMaxHealth());
         this.setArmor(30);
         damage = incursionDamage;
      } else {
         damage = baseDamage;
      }

      this.ai = new BehaviourTreeAI<>(this, new ConfusedCollisionPlayerChaserWandererAI<>(null, 384, damage, 100, 40000));
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public DeathMessageTable getDeathMessages() {
      return this.getDeathMessages("cavespider", 3);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 6; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.smallSwampCaveSpider.body, i, 8, 48, this.x, this.y, 20.0F, knockbackX, knockbackY),
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
      int drawX = camera.getDrawX(x) - 48;
      int drawY = camera.getDrawY(y) - 60;
      int dir = this.getDir();
      Point sprite = this.getAnimSprite(x, y, dir);
      drawY += this.getBobbing(x, y);
      drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
      final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
      final DrawOptions body = MobRegistry.Textures.smallSwampCaveSpider
         .body
         .initDraw()
         .sprite(sprite.x, sprite.y, 96)
         .addMaskShader(swimMask)
         .startGlowOptions(this, this.getID())
         .light(light)
         .applyEnemyTracker(this, perspective)
         .pos(drawX, drawY);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            swimMask.use();
            body.draw();
            swimMask.stop();
         }
      });
      TextureDrawOptions shadow = MobRegistry.Textures.smallSwampCaveSpider.shadow.initDraw().sprite(0, sprite.y, 96).light(light).pos(drawX, drawY);
      tileList.add(tm -> shadow.draw());
   }

   @Override
   public int getRockSpeed() {
      return 15;
   }

   @Override
   public float getAttackingMovementModifier() {
      return 0.0F;
   }

   @Override
   public int getTileWanderPriority(TilePosition pos, Biome baseBiome) {
      return pos.tileID() == TileRegistry.spiderNestID ? 1000 : super.getTileWanderPriority(pos, baseBiome);
   }
}
