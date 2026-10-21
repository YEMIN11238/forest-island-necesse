package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Double;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsPositionMod;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class TreeObject extends GameObject implements ForestryJobObject {
   public int weaveTime = 250;
   public float weaveAmount = 0.02F;
   public int leavesCenterWidth;
   public int leavesMinHeight;
   public int leavesMaxHeight;
   public String leavesTextureName;
   public String logStringID;
   public String saplingStringID;
   protected int[] treeYIndexToLeafYIndexTable;
   protected Color[] treeYIndexToMapColorTable;
   public GameTextureSection leavesTextureSection;
   protected String textureName;
   public GameTexture texture;
   protected final GameRandom drawRandom;

   public TreeObject(
      String textureName,
      String logStringID,
      String saplingStringID,
      Color mapColor,
      int leavesCenterWidth,
      int leavesMinHeight,
      int leavesMaxHeight,
      String leavesTextureName
   ) {
      super(new Rectangle(32, 32));
      this.textureName = textureName;
      this.logStringID = logStringID;
      this.saplingStringID = saplingStringID;
      this.mapColor = mapColor;
      this.leavesCenterWidth = leavesCenterWidth;
      this.leavesMinHeight = leavesMinHeight;
      this.leavesMaxHeight = leavesMaxHeight;
      this.leavesTextureName = leavesTextureName;
      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
      this.displayMapTooltip = true;
      this.isTree = true;
      this.drawDamage = false;
      this.toolType = ToolType.AXE;
      this.drawRandom = new GameRandom();
      this.replaceCategories.add("tree");
      this.replaceRotations = false;
      this.setItemCategory("objects", "landscaping", "plants");
      this.roomProperties.add("plant");
      this.roomProperties.add("tree");
   }

   public TreeObject setTreeYIndexToLeafYIndexTable(int... leafYIndexes) {
      this.treeYIndexToLeafYIndexTable = leafYIndexes;
      return this;
   }

   public TreeObject setTreeYIndexToMapColorTable(Color... mapColors) {
      this.treeYIndexToMapColorTable = mapColors;
      return this;
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
      if (this.leavesTextureName != null) {
         try {
            GameTexture leavesTexture = GameTexture.fromFileRaw("particles/" + this.leavesTextureName);
            this.leavesTextureSection = GameResources.particlesTextureGenerator.addTexture(leavesTexture);
         } catch (FileNotFoundException e) {
            this.leavesTextureSection = null;
         }
      }
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      LootTable lootTable = new LootTable();
      if (this.saplingStringID != null) {
         lootTable.items.add(LootItem.between(this.saplingStringID, 1, 2).splitItems(5));
      }

      if (this.logStringID != null) {
         lootTable.items.add(LootItem.between(this.logStringID, 4, 5).splitItems(5));
      }

      return lootTable;
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      Performance.record(
         tickManager,
         "treeSetup",
         () -> {
            GameLight light = level.getLightLevel(tileX, tileY);
            int drawX = camera.getTileDrawX(tileX);
            int drawY = camera.getTileDrawY(tileY);
            float alpha = 1.0F;
            if (perspective != null && !Settings.hideUI && !Settings.hideCursor) {
               Rectangle alphaRec = new Rectangle(tileX * 32 - 48, tileY * 32 - 100, 128, 100);
               if (perspective.getCollision().intersects(alphaRec)) {
                  alpha = 0.5F;
               } else if (alphaRec.contains(
                  camera.getX() + WindowManager.getWindow().mousePos().sceneX, camera.getY() + WindowManager.getWindow().mousePos().sceneY
               )) {
                  alpha = 0.5F;
               }
            }

            int spriteRes = 128;
            GameTexture texture = this.texture;
            int spriteX = 0;
            if (texture.getWidth() > spriteRes && level.getTileID(tileX, tileY) == TileRegistry.snowID) {
               spriteX = 1;
            }

            boolean mirror;
            synchronized (this.drawRandom) {
               this.drawRandom.setSeed(getTileSeed(tileX, tileY));
               mirror = this.drawRandom.nextBoolean();
            }

            int spriteY = this.getTreeSpriteY(level, tileX, tileY, spriteRes);
            Consumer<TextureDrawOptionsPositionMod> waveChange = GameResources.waveShader
               .setupGrassWaveMod(level, tileX, tileY, this.weaveTime, this.weaveAmount, 2, this.drawRandom, getTileSeed(tileX, tileY, 0), mirror, 3.0F);
            final TextureDrawOptions options = texture.initDraw()
               .sprite(spriteX, spriteY, spriteRes)
               .alpha(alpha)
               .light(light)
               .mirror(mirror, false)
               .addPositionMod(waveChange)
               .pos(drawX - 48, drawY - 96);
            list.add(new LevelSortedDrawable(this, tileX, tileY) {
               @Override
               public int getSortY() {
                  return 16;
               }

               @Override
               public void draw(TickManager tickManager) {
                  Performance.record(tickManager, "treeDraw", () -> options.draw());
               }
            });
         }
      );
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int spriteRes = 128;
      int spriteX = 0;
      if (this.texture.getWidth() > spriteRes && level.getTileID(tileX, tileY) == TileRegistry.snowID) {
         spriteX = 1;
      }

      boolean mirror;
      synchronized (this.drawRandom) {
         this.drawRandom.setSeed(getTileSeed(tileX, tileY));
         mirror = this.drawRandom.nextBoolean();
      }

      int spriteY = this.getTreeSpriteY(level, tileX, tileY, spriteRes);
      this.texture.initDraw().sprite(spriteX, spriteY, spriteRes).alpha(alpha).light(light).mirror(mirror, false).draw(drawX - 48, drawY - 96);
   }

   public int getTreeSpriteY(Level level, int tileX, int tileY, int spriteRes) {
      int spritesHeight = this.texture.getHeight() / spriteRes;
      int spriteY = 0;
      synchronized (this.drawRandom) {
         this.drawRandom.setSeed(getTileSeed(tileX, tileY));
         if (spritesHeight > 1) {
            spriteY = this.drawRandom.nextInt(spritesHeight);
         }

         return spriteY;
      }
   }

   @Override
   public void onDestroyed(Level level, int layerID, int x, int y, Attacker attacker, ServerClient client, ArrayList<ItemPickupEntity> itemsDropped) {
      super.onDestroyed(level, layerID, x, y, attacker, client, itemsDropped);
      if (!level.isServer()) {
         int leaves = GameRandom.globalRandom.getIntBetween(25, 30);
         this.spawnLeafParticles(level, x, y, 20, leaves, new Double(), 0.0F);
      }
   }

   @Override
   public boolean onDamaged(
      Level level, int layerID, int x, int y, int damage, Attacker attacker, ServerClient client, boolean showEffect, int mouseX, int mouseY
   ) {
      boolean out = super.onDamaged(level, layerID, x, y, damage, attacker, client, showEffect, mouseX, mouseY);
      if (showEffect) {
         level.makeGrassWeave(x, y, this.weaveTime, true);
         if (!level.isServer()) {
            int leaves = GameRandom.globalRandom.getIntBetween(0, 2);
            this.spawnLeafParticles(level, x, y, this.leavesMinHeight, leaves, new Double(), 0.0F);
         }
      }

      return out;
   }

   @Override
   public Color getMapColor(Level level, int tileX, int tileY) {
      if (this.treeYIndexToMapColorTable != null) {
         int treeSpriteY = this.getTreeSpriteY(level, tileX, tileY, 128);
         if (treeSpriteY < this.treeYIndexToMapColorTable.length) {
            Color color = this.treeYIndexToMapColorTable[treeSpriteY];
            if (color != null) {
               return color;
            }
         }
      }

      return super.getMapColor(level, tileX, tileY);
   }

   @Override
   public void tickEffect(Level level, int layerID, int tileX, int tileY) {
      super.tickEffect(level, layerID, tileX, tileY);
      if (Settings.windEffects) {
         float windSpeed = level.weatherLayer.getWindSpeed();
         if (windSpeed > 0.2F) {
            float windAmount = level.weatherLayer.getWindAmount(tileX, tileY) * 3.0F;
            if (windAmount > 0.5F) {
               Double windDir = level.weatherLayer.getWindDirNormalized();
               float buffer = 0.016666668F * windAmount * windSpeed;

               while (buffer >= 1.0F || GameRandom.globalRandom.getChance(buffer)) {
                  buffer--;
                  this.spawnLeafParticles(level, tileX, tileY, this.leavesMinHeight, 1, windDir, windAmount * windSpeed);
               }
            }
         }
      }
   }

   protected GameTextureSection getLeafTexture(Level level, int tileX, int tileY) {
      if (this.leavesTextureSection == null) {
         return null;
      }

      int leavesRes = 20;
      int leafSprites = this.leavesTextureSection.getWidth() / leavesRes;
      int leafSpriteY = 0;
      if (this.treeYIndexToLeafYIndexTable != null) {
         int treeSpriteY = this.getTreeSpriteY(level, tileX, tileY, 128);
         if (treeSpriteY < this.treeYIndexToLeafYIndexTable.length) {
            leafSpriteY = this.treeYIndexToLeafYIndexTable[treeSpriteY];
         }
      }

      return this.leavesTextureSection.sprite(GameRandom.globalRandom.nextInt(leafSprites), leafSpriteY, leavesRes);
   }

   public void spawnLeafParticles(Level level, int x, int y, int minStartHeight, int amount, Double windDir, float windSpeed) {
      if (this.leavesTextureSection != null) {
         spawnLeafParticles(
            level, x, y, this.leavesCenterWidth, minStartHeight, this.leavesMaxHeight, amount, windDir, windSpeed, () -> this.getLeafTexture(level, x, y)
         );
      }
   }

   public static void spawnLeafParticles(
      Level level,
      int x,
      int y,
      int centerWidth,
      int minStartHeight,
      int maxStartHeight,
      int amount,
      Double windDir,
      float windSpeed,
      Supplier<GameTextureSection> leavesTextureSectionGetter
   ) {
      boolean alternate = GameRandom.globalRandom.nextBoolean();

      for (int i = 0; i < amount; i++) {
         GameTextureSection texture = leavesTextureSectionGetter.get();
         if (texture != null) {
            float posX = x * 32 + 16
               + (alternate ? GameRandom.globalRandom.getFloatBetween(-1.0F, 0.0F) : GameRandom.globalRandom.getFloatBetween(0.0F, 1.0F)) * centerWidth;
            alternate = !alternate;
            float posY = y * 32 + 32;
            float startHeight = GameRandom.globalRandom.getFloatBetween(minStartHeight, maxStartHeight);
            float startHeightSpeed = GameRandom.globalRandom.getFloatBetween(0.0F, 60.0F);
            float endHeight = GameRandom.globalRandom.getFloatBetween(-10.0F, -5.0F);
            float gravity = GameRandom.globalRandom.getFloatBetween(8.0F, 20.0F);
            boolean mirror = GameRandom.globalRandom.nextBoolean();
            float rotation = GameRandom.globalRandom.getFloatBetween(-100.0F, 100.0F);
            float moveX = GameRandom.globalRandom.floatGaussian() * 5.0F + (float)windDir.x * windSpeed * 10.0F;
            float moveY = GameRandom.globalRandom.floatGaussian() * 2.0F + (float)windDir.y * windSpeed * 10.0F;
            ParticleOption.FrictionMover frictionMover = new ParticleOption.FrictionMover(moveX, moveY, 0.0F);
            ParticleOption.CollisionMover mover = new ParticleOption.CollisionMover(
               level, frictionMover, new CollisionFilter().mobCollision().addFilter(tp -> tp.object().object.isWall)
            );
            int timeToLive = GameRandom.globalRandom.getIntBetween(3000, 8000);
            int timeToFadeOut = GameRandom.globalRandom.getIntBetween(1000, 2000);
            int totalTime = timeToLive + timeToFadeOut;
            ParticleOption.HeightMover heightMover = new ParticleOption.HeightMover(startHeight, startHeightSpeed, gravity, 2.0F, endHeight, 0.0F);
            AtomicReference<Float> floatingTime = new AtomicReference<>(0.0F);
            ParticleOption particle = level.entityManager
               .addParticle(posX, posY, Particle.GType.COSMETIC)
               .sprite(texture)
               .fadesAlphaTime(0, timeToFadeOut)
               .sizeFadesInAndOut(15, 20, 100, 0)
               .height(heightMover)
               .onMoveTick((delta, lifeTime, timeAlive, lifePercent) -> {
                  if (heightMover.currentHeight > endHeight) {
                     floatingTime.set(floatingTime.get() + delta);
                  }
               })
               .modify((options, lifeTime, timeAlive, lifePercent) -> {
                  float angle = GameMath.sin(floatingTime.get() / 5.0F) * rotation;
                  options.rotate(angle, 10, -4);
               })
               .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
                  if (heightMover.currentHeight > endHeight) {
                     mover.tick(pos, delta, lifeTime, timeAlive, lifePercent);
                  }
               })
               .modify((options, lifeTime, timeAlive, lifePercent) -> options.mirror(mirror, false))
               .lifeTime(totalTime);
            if (!particle.isRemoved()) {
               SoundManager.playSound(SoundSettingsRegistry.leavesBreakAmbient, SoundEffect.effect(posX, posY));
            }
         }
      }
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      String error = super.canPlace(level, layerID, x, y, rotation, byPlayer, ignoreOtherLayers);
      if (error != null) {
         return error;
      }

      GameObject[] adj = level.getAdjacentObjects(x, y);

      for (GameObject obj : adj) {
         if (obj.isTree) {
            return "treenear";
         }
      }

      return null;
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      if (super.isValid(level, layerID, x, y)) {
         GameObject[] adj = level.getAdjacentObjects(x, y);

         for (GameObject obj : adj) {
            if (obj.isTree) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public String getSaplingStringID() {
      return this.saplingStringID;
   }
}
