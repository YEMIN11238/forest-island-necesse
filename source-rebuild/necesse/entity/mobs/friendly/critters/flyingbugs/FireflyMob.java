package necesse.entity.mobs.friendly.critters.flyingbugs;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import necesse.engine.GameTileRange;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.MobSpawnLocation;
import necesse.entity.mobs.MobTexture;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.BehaviourTreeAI;
import necesse.entity.mobs.ai.behaviourTree.trees.HomesickCritterAI;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.MobChance;
import necesse.level.maps.light.GameLight;

public class FireflyMob extends SmallFlyingBugCritterMob {
   public static MobChance spawnChanceSurface = new MobChance(250) {
      @Override
      public boolean canSpawn(Level level, ServerClient client, Point spawnTile, String purpose) {
         if (!purpose.equals("mobspawning")) {
            return false;
         } else {
            return !level.getWorldEntity().isNight()
               ? false
               : level.entityManager
                  .mobs
                  .streamInRegionsInTileRange(spawnTile.x * 32 + 16, spawnTile.y * 32 + 16, FireflyMob.fireflyRange.maxRange)
                  .noneMatch(
                     m -> GameMath.diagonalMoveDistance(m.getX(), m.getY(), spawnTile.x * 32 + 16, spawnTile.y * 32 + 16)
                        < FireflyMob.fireflyRange.maxRange * 32
                  );
         }
      }

      @Override
      public Mob getMob(Level level, ServerClient client, Point spawnTile) {
         return null;
      }

      @Override
      public Collection<Mob> spawnMob(Level level, ServerClient client, Point spawnTile, Predicate<Mob> beforeTest, Consumer<Mob> beforeAdded, String purpose) {
         ArrayList<Mob> spawnedMobs = FireflyMob.getFirefliesToSpawn(level, client, spawnTile.x, spawnTile.y, 0.3F, beforeTest);
         if (spawnedMobs.isEmpty()) {
            return null;
         }

         while (spawnedMobs.size() > 8) {
            spawnedMobs.remove(GameRandom.globalRandom.nextInt(spawnedMobs.size()));
         }

         for (Mob mob : spawnedMobs) {
            if (beforeAdded != null) {
               beforeAdded.accept(mob);
            }

            level.entityManager.mobs.add(mob);
         }

         return spawnedMobs;
      }
   };
   public static MobChance spawnChanceCave = new MobChance(40) {
      @Override
      public boolean canSpawn(Level level, ServerClient client, Point spawnTile, String purpose) {
         return !purpose.equals("mobspawning")
            ? false
            : level.entityManager
               .mobs
               .streamInRegionsInTileRange(spawnTile.x * 32 + 16, spawnTile.y * 32 + 16, FireflyMob.fireflyRange.maxRange)
               .noneMatch(
                  m -> GameMath.diagonalMoveDistance(m.getX(), m.getY(), spawnTile.x * 32 + 16, spawnTile.y * 32 + 16) < FireflyMob.fireflyRange.maxRange * 32
               );
      }

      @Override
      public Mob getMob(Level level, ServerClient client, Point spawnTile) {
         return null;
      }

      @Override
      public Collection<Mob> spawnMob(Level level, ServerClient client, Point spawnTile, Predicate<Mob> beforeTest, Consumer<Mob> beforeAdded, String purpose) {
         ArrayList<Mob> spawnedMobs = FireflyMob.getFirefliesToSpawn(level, client, spawnTile.x, spawnTile.y, 0.3F, beforeTest);
         if (spawnedMobs.isEmpty()) {
            return null;
         }

         while (spawnedMobs.size() > 5) {
            spawnedMobs.remove(GameRandom.globalRandom.nextInt(spawnedMobs.size()));
         }

         for (Mob mob : spawnedMobs) {
            if (beforeAdded != null) {
               beforeAdded.accept(mob);
            }

            level.entityManager.mobs.add(mob);
         }

         return spawnedMobs;
      }
   };
   public static GameTileRange fireflyRange = new GameTileRange(3);
   public FireflyMob.FireflyType fireflyType;
   protected boolean overrideFireflyType;
   public static final int lightEmissionLevel = 40;
   public static final int drawLightLevel = 120;
   protected float lightStrength;

   public static ArrayList<Mob> getFirefliesToSpawn(Level level, ServerClient client, int tileX, int tileY, float spawnChance, Predicate<Mob> beforeTest) {
      ArrayList<Mob> spawnedMobs = new ArrayList<>();
      int spawnRoomID = level.regionManager.getRoomIDByTile(tileX, tileY);

      for (Point validTile : fireflyRange.getValidTiles(tileX, tileY)) {
         if (GameRandom.globalRandom.getChance(spawnChance)
            && !level.isSolidTile(validTile.x, validTile.y)
            && spawnRoomID == level.regionManager.getRoomIDByTile(validTile.x, validTile.y)) {
            Mob mob = MobRegistry.getMob("firefly", level);
            if (beforeTest == null || beforeTest.test(mob)) {
               Point moveOffset = mob.getPathMoveOffset();
               if (mob.isValidSpawnLocation(level.getServer(), client, validTile.x * 32 + moveOffset.x, validTile.y * 32 + moveOffset.y)) {
                  mob.onSpawned(validTile.x * 32 + moveOffset.x, validTile.y * 32 + moveOffset.y);
                  spawnedMobs.add(mob);
               }
            }
         }
      }

      return spawnedMobs;
   }

   public FireflyMob() {
      this(FireflyMob.FireflyType.YELLOW, true);
   }

   public FireflyMob(FireflyMob.FireflyType fireflyType) {
      this(fireflyType, false);
   }

   public FireflyMob(FireflyMob.FireflyType fireflyType, boolean overrideFireflyType) {
      super(2);
      this.fireflyType = fireflyType;
      this.overrideFireflyType = overrideFireflyType;
   }

   @Override
   public void applyLoadData(LoadData save) {
      if (save.hasLoadDataByName("fireflyType")) {
         this.fireflyType = save.getEnum(FireflyMob.FireflyType.class, "fireflyType");
         this.overrideFireflyType = false;
      }

      super.applyLoadData(save);
   }

   @Override
   public void addSaveData(SaveData save) {
      if (!this.overrideFireflyType) {
         save.addEnum("fireflyType", this.fireflyType);
      }

      super.addSaveData(save);
   }

   @Override
   public LootTable getLootTable() {
      String itemStringID = this.fireflyType == null ? FireflyMob.FireflyType.YELLOW.droppedItemStringID : this.fireflyType.droppedItemStringID;
      return new LootTable(new LootItem(itemStringID));
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextEnum(this.fireflyType);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.fireflyType = reader.getNextEnum(FireflyMob.FireflyType.class);
   }

   @Override
   protected MobTexture getBugTexture() {
      return MobRegistry.Textures.firefly;
   }

   @Override
   public void init() {
      super.init();
      this.ai = new BehaviourTreeAI<>(this, new HomesickCritterAI<>(this));
      if (!this.isClient()) {
         if (this.overrideFireflyType) {
            if (!this.getLevel().getIdentifier().equals(LevelIdentifier.SURFACE_IDENTIFIER)) {
               if (this.getLevel().getIdentifier().equals(LevelIdentifier.CAVE_IDENTIFIER)) {
                  this.fireflyType = FireflyMob.FireflyType.GREEN;
               } else {
                  this.fireflyType = FireflyMob.FireflyType.BLUE;
               }
            }
         }
      }
   }

   @Override
   public void clientTick() {
      super.clientTick();
      float var10003 = this.fireflyType.emissionHue;
      this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y, var10003, 1.0F, this.fireflyType.emissionLevel);
      this.lightStrength = getLightStrength(this.getLevel(), getTileCoordinate(this.x), getTileCoordinate(this.y));
      if (this.lightStrength > 0.7F) {
         if (this.dx == 0.0F && this.dy == 0.0F) {
            if (this.getLevel().tickManager().isGameTickInSecond(14) || GameRandom.globalRandom.getChance(0.02F * this.lightStrength)) {
               this.spawnGlowParticle();
            }
         } else if (this.getLevel().tickManager().isGameTickInSecond(10) || GameRandom.globalRandom.getChance(0.03F * this.lightStrength)) {
            this.spawnGlowParticle();
         }
      }
   }

   @Override
   public float getDespawnOdds() {
      return !this.getWorldEntity().isNight() ? super.getDespawnOdds() * 5.0F : super.getDespawnOdds();
   }

   public static float getLightStrength(Level level, int tileX, int tileY) {
      GameLight light = new GameLight(level.lightManager.getAmbientLight());
      return GameMath.limit(1.0F - (light.getLevel() - 50.0F) / 50.0F, 0.0F, 1.0F);
   }

   public static Color getFireflyGlowColor(FireflyMob.FireflyType type, float lightStrength) {
      Color glowColor = type.glowColor;
      float colorFactor = GameMath.limit(lightStrength, 0.4F, 1.0F);
      return new Color(
         GameMath.limit((int)(glowColor.getRed() * colorFactor), 0, 255),
         GameMath.limit((int)(glowColor.getGreen() * colorFactor), 0, 255),
         GameMath.limit((int)(glowColor.getBlue() * colorFactor), 0, 255),
         255
      );
   }

   protected void spawnGlowParticle() {
      int emissionLevel = (int)(150.0F * this.lightStrength);
      Color tailColor = getFireflyGlowColor(this.fireflyType, this.lightStrength);
      float rndX = this.x + (float)(GameRandom.globalRandom.nextGaussian() * 4.0);
      float rndY = this.y - this.getFlyingHeight() + (float)(GameRandom.globalRandom.nextGaussian() * 4.0);
      this.getLevel()
         .entityManager
         .addParticle(rndX, rndY, Particle.GType.IMPORTANT_COSMETIC)
         .sprite(GameResources.bubbleParticle.sprite(0, 0, 12))
         .color(tailColor)
         .minDrawLight(120)
         .height(10.0F)
         .alpha(this.lightStrength)
         .dontRotate()
         .movesConstant(0.0F, -1.0F)
         .fadesAlphaTimeToCustomAlpha(250, 250, 1.0F)
         .size((options, lifeTime, timeAlive, lifePercent) -> options.size(6))
         .lifeTime(800);
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
      if (!(this.lightStrength <= 0.0F)) {
         Color tailColor = getFireflyGlowColor(this.fireflyType, this.lightStrength);
         int flyHeight = this.getFlyingHeight();
         int drawX = camera.getDrawX(x);
         int drawY = camera.getDrawY(y);
         Point sprite = this.getWingFlapSpriteAnim();
         drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
         final DrawOptions lightOptions = this.getBugTexture()
            .body
            .initDraw()
            .sprite(sprite.x + 2, sprite.y, this.spriteRes)
            .color(tailColor)
            .light(new GameLight(120.0F))
            .alpha(this.lightStrength)
            .pos(drawX - this.spriteRes / 2, drawY - (this.spriteRes / 2 + this.spriteRes / 4) - flyHeight);
         list.add(new MobDrawable() {
            @Override
            public void draw(TickManager tickManager) {
               lightOptions.draw();
            }
         });
      }
   }

   @Override
   protected SoundSettings getAmbientSound() {
      return new SoundSettings(GameResources.fireflyAmbient).volume(0.1F).basePitch(0.97F).pitchVariance(0.03F);
   }

   @Override
   public MobSpawnLocation checkSpawnLocation(MobSpawnLocation location) {
      return super.checkSpawnLocation(location).checkTile((tileX, tileY) -> !this.getLevel().getTile(tileX, tileY).isFloor);
   }

   public enum FireflyType {
      YELLOW(Item.Rarity.NORMAL, "fireflyyellow", 59, new Color(250, 190, 0), 0),
      GREEN(Item.Rarity.COMMON, "fireflygreen", 90, new Color(145, 205, 41), 0),
      BLUE(Item.Rarity.UNCOMMON, "fireflyblue", 200, new Color(25, 159, 255), 30);

      public Item.Rarity rarity;
      public String droppedItemStringID;
      public int emissionHue;
      public int emissionLevel;
      public Color glowColor;

      FireflyType(Item.Rarity rarity, String droppedItemStringID, int emissionHue, Color glowColor, int emissionLevelDifference) {
         this.rarity = rarity;
         this.droppedItemStringID = droppedItemStringID;
         this.emissionHue = emissionHue;
         this.glowColor = glowColor;
         this.emissionLevel = 40 + emissionLevelDifference;
      }
   }
}
