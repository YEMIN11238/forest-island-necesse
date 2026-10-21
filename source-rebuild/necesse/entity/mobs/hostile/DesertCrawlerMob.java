package necesse.entity.mobs.hostile;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.LinkedList;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.MobRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GroundPillar;
import necesse.engine.util.GroundPillarList;
import necesse.entity.Entity;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.ConfusedCollisionPlayerChaserWandererAI;
import necesse.entity.particle.FleshParticle;
import necesse.entity.particle.Particle;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.inventory.lootTable.LootTable;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class DesertCrawlerMob extends HostileMob {
   public static LootTable lootTable = new LootTable();
   public static GameDamage baseDamage = new GameDamage(90.0F);
   public static GameDamage incursionDamage = new GameDamage(100.0F);
   private final GroundPillarList<DesertCrawlerMob.Mound> mounds = new GroundPillarList<>();
   private int moundCounter;
   private double moveBuffer;
   private double moveCounter;

   public DesertCrawlerMob() {
      super(350);
      this.setSpeed(45.0F);
      this.setFriction(4.0F);
      this.setKnockbackModifier(0.4F);
      this.setArmor(20);
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-16, -16, 32, 32);
      this.selectBox = new Rectangle(-23, -23, 46, 46);
   }

   @Override
   public void init() {
      super.init();
      GameDamage damage;
      if (this.getLevel() instanceof IncursionLevel) {
         this.setMaxHealth(450);
         this.setHealthHidden(this.getMaxHealth());
         this.setArmor(30);
         damage = incursionDamage;
      } else {
         damage = baseDamage;
      }

      this.ai = new BehaviourTreeAI<>(this, new ConfusedCollisionPlayerChaserWandererAI<>(null, 512, damage, 100, 40000));
      this.addNewMound(new Float(0.0F, 1.0F), true);
      this.addNewMound(new Float(1.0F, 0.0F), true);
      this.addNewMound(new Float(1.0F, 0.0F), true);
   }

   @Override
   public LootTable getLootTable() {
      return lootTable;
   }

   @Override
   public void tickMovement(float delta) {
      while (delta > 0.0F) {
         int rockSpeed = this.getRockSpeed();
         Float lastPos = new Float(this.x, this.y);
         super.tickMovement(Math.min(rockSpeed * 2, delta));
         delta -= rockSpeed * 2;
         double dist = lastPos.distance(this.x, this.y);
         this.moveBuffer += dist;

         for (this.moveCounter += dist; this.moveBuffer > rockSpeed; this.moveBuffer -= rockSpeed) {
            this.addNewMound(GameMath.normalize(this.x - lastPos.x, this.y - lastPos.y), false);
         }
      }
   }

   private void addNewMound(Float dir, boolean isFirst) {
      Float perpDir = GameMath.getPerpendicularDir(dir.x, dir.y);
      int offset = this.moundCounter % 2 == 0 ? 5 : -5;
      synchronized (this.mounds) {
         this.mounds
            .add(
               new DesertCrawlerMob.Mound(
                  this,
                  this.getX() + (int)(perpDir.x * offset + dir.x * 10.0F) + GameRandom.globalRandom.getIntBetween(-2, 2) + (isFirst ? offset : 0),
                  this.getY() + (int)(perpDir.y * offset * 0.7F + dir.y * 10.0F) + GameRandom.globalRandom.getIntBetween(-2, 2),
                  this.moveCounter - (isFirst ? 20 : 0),
                  this.getLocalTime()
               )
            );
      }

      this.moundCounter++;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      synchronized (this.mounds) {
         this.mounds.clean(this.getLocalTime(), this.moveCounter);
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      synchronized (this.mounds) {
         this.mounds.clean(this.getLocalTime(), this.moveCounter);
      }
   }

   @Override
   public CollisionFilter getLevelCollisionFilter() {
      return super.getLevelCollisionFilter().allLiquidTiles();
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 4; i++) {
         this.getLevel()
            .entityManager
            .addParticle(
               new FleshParticle(this.getLevel(), MobRegistry.Textures.desertCrawler, i, 0, 32, this.x, this.y, 20.0F, knockbackX, knockbackY),
               Particle.GType.IMPORTANT_COSMETIC
            );
      }
   }

   @Override
   protected void addExtraDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      Level level,
      int x,
      int y,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      super.addExtraDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
      long currentTime = this.getLocalTime();
      LinkedList<DesertCrawlerMob.Mound> mounds;
      synchronized (this.mounds) {
         this.mounds.clean(currentTime, this.moveCounter);
         mounds = this.mounds.copyPillars();
      }

      for (final DesertCrawlerMob.Mound mound : mounds) {
         final DrawOptions moundDraw = mound.getDrawOptions(level, currentTime, this.moveCounter, camera, perspective);
         if (moundDraw != null) {
            list.add(new LevelSortedDrawable(this) {
               @Override
               public int getSortY() {
                  return mound.y;
               }

               @Override
               public void draw(TickManager tickManager) {
                  moundDraw.draw();
               }
            });
         }
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
   }

   @Override
   public int getRockSpeed() {
      return 5;
   }

   private static class Mound extends GroundPillar {
      public Mob mob;
      public int res = 32;
      public GameTextureSection texture;

      public Mound(Mob mob, int x, int y, double spawnDistance, long spawnTime) {
         super(x, y, spawnDistance, spawnTime);
         this.mob = mob;
         this.texture = null;
         GameTexture sprites = MobRegistry.Textures.mounds32;
         if (sprites != null) {
            int sprite = GameRandom.globalRandom.nextInt(sprites.getWidth() / this.res);
            this.texture = new GameTextureSection(sprites).sprite(sprite, 0, this.res, sprites.getHeight());
         }

         this.behaviour = new GroundPillar.DistanceTimedBehaviour(1000, 0, 500, 50.0, 20.0, 100.0);
      }

      @Override
      public DrawOptions getDrawOptions(Level level, long currentTime, double distanceMoved, GameCamera camera, PlayerMob perspective) {
         GameTile tile = level.getTile(Entity.getTileCoordinate(this.x), Entity.getTileCoordinate(this.y));
         if (tile.isLiquid) {
            return null;
         }

         Color color = tile.getMapColor(level, Entity.getTileCoordinate(this.x), Entity.getTileCoordinate(this.y));
         GameLight light = level.getLightLevel(Entity.getTileCoordinate(this.x), Entity.getTileCoordinate(this.y));
         int drawX = camera.getDrawX(this.x);
         int drawY = camera.getDrawY(this.y);
         double height = this.getHeight(currentTime, distanceMoved);
         int endY = (int)(height * this.texture.getHeight());
         return this.texture
            .section(0, this.texture.getWidth(), 0, endY)
            .initDraw()
            .startGlowOptions(level, this.mob.getID())
            .color(color)
            .light(light)
            .applyEnemyTracker(this.mob, perspective)
            .pos(drawX - this.texture.getWidth() / 2, drawY - endY);
      }
   }
}
