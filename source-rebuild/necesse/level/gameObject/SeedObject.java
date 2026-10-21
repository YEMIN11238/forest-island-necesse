package necesse.level.gameObject;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.packet.PacketHitObject;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.SeedObjectEntity;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsPositionMod;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.objectItem.SeedObjectItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.jobs.HarvestCropLevelJob;
import necesse.level.maps.levelData.jobs.TileLevelJob;
import necesse.level.maps.light.GameLight;

public class SeedObject extends GameObject implements PollinateObject {
   protected String textureName;
   public GameTexture texture;
   public GameTexture fertilizedTexture;
   public GameTexture itemTexture;
   public int drawOffset;
   public String productStringID;
   public String seedStringID;
   public int maxSeedAmount;
   public int maxProductAmount;
   public LootItemInterface customSeedLootItem;
   public LootItemInterface customProductLootItem;
   public int minGrowTime;
   public int maxGrowTime;
   public int[] stageIDs = new int[0];
   public int thisStage;
   public boolean canBePlacedAsFlower;
   public int flowerID;
   public int itemSpoilDurationMinutes;
   private final GameRandom drawRandom;

   public SeedObject(
      String textureName,
      int drawOffset,
      int stage,
      String productStringID,
      int maxProductAmount,
      String seedStringID,
      int minGrowTime,
      int maxGrowTime,
      boolean canBePlacedAsFlower,
      int flowerID,
      Color mapColor,
      Item.Rarity rarity,
      int itemSpoilDurationMinutes
   ) {
      this.textureName = textureName;
      this.drawOffset = drawOffset;
      this.thisStage = stage;
      this.productStringID = productStringID;
      this.maxProductAmount = maxProductAmount;
      this.maxSeedAmount = 2;
      this.seedStringID = seedStringID;
      this.minGrowTime = minGrowTime;
      this.maxGrowTime = maxGrowTime;
      this.mapColor = mapColor;
      this.canBePlacedAsFlower = canBePlacedAsFlower;
      this.flowerID = flowerID;
      this.rarity = rarity;
      this.itemSpoilDurationMinutes = itemSpoilDurationMinutes;
      this.displayMapTooltip = true;
      this.setItemCategory("objects", "seeds");
      this.isSeed = true;
      this.objectHealth = 1;
      this.toolType = ToolType.ALL;
      this.attackThrough = true;
      this.drawRandom = new GameRandom();
      this.stackSize = 500;
      this.roomProperties.add("flower");
      this.roomProperties.add("plant");
      this.replaceCategories.add("seed");
      this.canReplaceCategories.add("seed");
      this.replaceRotations = false;
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      GameTexture spriteTexture = GameTexture.fromFile("objects/" + this.textureName, true);
      this.fertilizedTexture = GameTexture.fromFile("objects/fertilizer");
      this.itemTexture = new GameTexture(spriteTexture, 0, 1, 32);
      this.itemTexture.makeFinal();
      this.texture = new GameTexture("objects/" + this.textureName + " stage" + this.thisStage, 64, spriteTexture.getHeight());
      this.texture.copy(spriteTexture, 16, 0, 32 + this.thisStage * 32, 0, 32, spriteTexture.getHeight());
      this.texture.resetTexture();
      this.texture.makeFinal();
      if (this.thisStage == 0) {
         spriteTexture.makeFinal();
      }
   }

   @Override
   public GameMessage getNewLocalization() {
      return this.thisStage == 0 ? super.getNewLocalization() : ObjectRegistry.getObject(ObjectRegistry.getObjectID(this.seedStringID)).getNewLocalization();
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      LootTable out = new LootTable();
      if (this.isLastStage()) {
         if (this.customSeedLootItem != null) {
            out.items.add(this.customSeedLootItem);
         } else if (this.maxSeedAmount > 0) {
            out.items.add(LootItem.between(this.seedStringID, 1, this.maxSeedAmount));
         }

         if (this.customProductLootItem != null) {
            out.items.add(this.customProductLootItem);
         } else if (this.maxProductAmount > 0) {
            out.items.add(LootItem.between(this.productStringID, 1, this.maxProductAmount));
         }
      } else {
         out.items.add(new LootItem(this.seedStringID).preventLootMultiplier());
      }

      return out;
   }

   @Override
   public void tick(Mob mob, Level level, int x, int y) {
      super.tick(mob, level, x, y);
      if (Settings.wavyGrass && mob.getFlyingHeight() < 10 && (mob.dx != 0.0F || mob.dy != 0.0F)) {
         level.makeGrassWeave(x, y, 1000, false);
      }
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
      float rFloat;
      boolean topMirror;
      boolean botMirror;
      synchronized (this.drawRandom) {
         rFloat = (this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextFloat() - 0.5F) * 1.5F;
         topMirror = this.drawRandom.nextBoolean();
         botMirror = this.drawRandom.nextBoolean();
      }

      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      Consumer<TextureDrawOptionsPositionMod> topWaveChange = GameResources.waveShader
         .setupGrassWaveMod(level, tileX, tileY, 1000L, 0.07F, 1, this.drawRandom, getTileSeed(tileX, tileY, 4), topMirror, 2.0F);
      Consumer<TextureDrawOptionsPositionMod> botWaveChange = GameResources.waveShader
         .setupGrassWaveMod(level, tileX, tileY, 1000L, 0.07F, 1, this.drawRandom, getTileSeed(tileX, tileY, 5), botMirror, 2.0F);
      int offset1 = 12 + (int)(rFloat * 4.0F) + this.drawOffset;
      final TextureDrawOptions top = this.texture
         .initDraw()
         .light(light)
         .mirror(topMirror, false)
         .addPositionMod(topWaveChange)
         .pos(drawX - 16 + 4, drawY - this.texture.getHeight() + offset1);
      int offset2 = 24 + (int)(rFloat * 4.0F) + this.drawOffset;
      final TextureDrawOptions bot = this.texture
         .initDraw()
         .light(light)
         .mirror(botMirror, false)
         .addPositionMod(botWaveChange)
         .pos(drawX - 16 - 4, drawY - this.texture.getHeight() + offset2);
      ObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY);
      if (objectEntity instanceof SeedObjectEntity) {
         TextureDrawOptions fertilizedOptions = this.fertilizedTexture.initDraw().pos(drawX, drawY);
         if (((SeedObjectEntity)objectEntity).isFertilized()) {
            tileList.add(tm -> fertilizedOptions.draw());
         }
      }

      final int sortY1 = offset1 - 5;
      final int sortY2 = offset2 - 5;
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return sortY1;
         }

         @Override
         public void draw(TickManager tickManager) {
            top.draw();
         }
      });
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return sortY2;
         }

         @Override
         public void draw(TickManager tickManager) {
            bot.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      float rFloat;
      boolean mirror1;
      boolean mirror2;
      synchronized (this.drawRandom) {
         rFloat = (this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextFloat() - 0.5F) * 1.5F;
         mirror1 = this.drawRandom.nextBoolean();
         mirror2 = this.drawRandom.nextBoolean();
      }

      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int offset1 = 12 + (int)(rFloat * 4.0F) + this.drawOffset;
      this.texture.initDraw().alpha(alpha).mirror(mirror1, false).draw(drawX - 16 + 4, drawY - this.texture.getHeight() + offset1);
      int offset2 = 24 + (int)(rFloat * 4.0F) + this.drawOffset;
      this.texture.initDraw().alpha(alpha).mirror(mirror2, false).draw(drawX - 16 - 4, drawY - this.texture.getHeight() + offset2);
   }

   @Override
   public Item generateNewObjectItem() {
      return new SeedObjectItem(this, () -> this.itemTexture).spoilDuration(this.itemSpoilDurationMinutes);
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      return super.isValid(level, layerID, x, y) && level.getTileID(x, y) == TileRegistry.farmlandID;
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      String error = super.canPlace(level, layerID, x, y, rotation, byPlayer, ignoreOtherLayers);
      if (error != null) {
         return error;
      } else {
         return !level.getTile(x, y).getStringID().equals("farmland") ? "notfarmland" : null;
      }
   }

   @Override
   public boolean canReplace(Level level, int layerID, int tileX, int tileY, int rotation) {
      for (int i = 0; i < this.stageIDs.length - 1; i++) {
         if (level.getObjectID(tileX, tileY) == this.stageIDs[i]) {
            return false;
         }
      }

      return super.canReplace(level, layerID, tileX, tileY, rotation);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(Localization.translate("itemtooltip", "seedtip"));
      return tooltips;
   }

   @Override
   public List<TileLevelJob> getLevelJobs(Level level, int tileX, int tileY) {
      return this.isLastStage() ? Collections.singletonList(new HarvestCropLevelJob(tileX, tileY)) : super.getLevelJobs(level, tileX, tileY);
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return this.isLastStage() ? null : new SeedObjectEntity(level, x, y, this.minGrowTime, this.maxGrowTime);
   }

   @Override
   public PollinateObjectHandler getPollinateHandler(Level level, int tileX, int tileY) {
      final SeedObjectEntity seedObjectEntity = this.getCurrentObjectEntity(level, tileX, tileY, SeedObjectEntity.class);
      return seedObjectEntity != null ? new PollinateObjectHandler(tileX, tileY, seedObjectEntity.fertilizeReservable) {
         @Override
         public boolean canPollinate() {
            return !seedObjectEntity.isFertilized();
         }

         @Override
         public void pollinate() {
            seedObjectEntity.fertilize();
         }

         @Override
         public boolean isValid() {
            return !seedObjectEntity.removed();
         }
      } : null;
   }

   public boolean isLastStage() {
      return this.thisStage >= this.stageIDs.length - 1;
   }

   public boolean isFirstStage() {
      return this.thisStage == 0;
   }

   public int getFirstStageID() {
      return this.stageIDs.length > 0 ? this.stageIDs[0] : -1;
   }

   public int getNextStageID() {
      return this.thisStage < this.stageIDs.length - 1 ? this.stageIDs[this.thisStage + 1] : -1;
   }

   @Override
   public void attackThrough(Level level, int x, int y, GameDamage damage, Attacker attacker) {
      level.getServer().network.sendToClientsWithTile(new PacketHitObject(level, x, y, this, damage), level, x, y);
   }

   @Override
   public void attackThrough(Level level, int x, int y, GameDamage damage) {
      super.attackThrough(level, x, y, damage);
      level.makeGrassWeave(x, y, 1000, false);
   }

   @Override
   public void onMouseHover(Level level, int x, int y, GameCamera camera, PlayerMob perspective, boolean debug) {
      String displayName = null;
      if (this.productStringID != null && this.maxProductAmount > 0) {
         int itemID = ItemRegistry.getItemID(this.productStringID);
         if (itemID != -1) {
            displayName = ItemRegistry.getDisplayName(itemID);
         }
      }

      if (displayName == null && this.seedStringID != null) {
         int itemID = ItemRegistry.getItemID(this.seedStringID);
         if (itemID != -1) {
            displayName = ItemRegistry.getDisplayName(itemID);
         }
      }

      if (displayName != null) {
         GameTooltipManager.addTooltip(new StringTooltips(displayName), TooltipLocation.INTERACT_FOCUS);
      }

      super.onMouseHover(level, x, y, camera, perspective, debug);
   }

   @Override
   public void playDamageSound(Level level, int x, int y, boolean damageDone) {
      SoundManager.playSound(GameResources.grass, SoundEffect.effect(x * 32 + 16, y * 32 + 16));
   }

   public static SeedObject[] registerSeedObjects(
      String stringIDPrefix,
      String textureName,
      String productStringID,
      int productAmount,
      int drawOffset,
      int stages,
      float minGrowTimeInSeconds,
      float maxGrowTimeInSeconds,
      Color debrisColor,
      float brokerValue
   ) {
      return registerSeedObjects(
         stringIDPrefix,
         textureName,
         productStringID,
         productAmount,
         drawOffset,
         stages,
         minGrowTimeInSeconds,
         maxGrowTimeInSeconds,
         debrisColor,
         Item.Rarity.NORMAL,
         0,
         brokerValue
      );
   }

   public static SeedObject[] registerSeedObjects(
      String stringIDPrefix,
      String textureName,
      String productStringID,
      int productAmount,
      int drawOffset,
      int stages,
      float minGrowTimeInSeconds,
      float maxGrowTimeInSeconds,
      Color debrisColor,
      Item.Rarity rarity,
      int itemSpoilDurationMinutes,
      float brokerValue
   ) {
      return registerSeedObjects(
         stringIDPrefix,
         textureName,
         productStringID,
         productAmount,
         drawOffset,
         stages,
         minGrowTimeInSeconds,
         maxGrowTimeInSeconds,
         false,
         0,
         debrisColor,
         rarity,
         itemSpoilDurationMinutes,
         brokerValue
      );
   }

   public static SeedObject[] registerSeedObjects(
      String stringIDPrefix,
      String textureName,
      String productStringID,
      int productAmount,
      int drawOffset,
      int stages,
      float minGrowTimeInSeconds,
      float maxGrowTimeInSeconds,
      boolean canBePlacedAsFlower,
      int flowerID,
      Color debrisColor,
      Item.Rarity rarity,
      int itemSpoilDurationMinutes,
      float brokerValue
   ) {
      int minGrowTime = (int)(minGrowTimeInSeconds * 1000.0F) / stages;
      int maxGrowTime = (int)(maxGrowTimeInSeconds * 1000.0F) / stages;
      int[] ids = new int[stages];
      SeedObject[] objects = new SeedObject[stages];

      for (int i = stages - 1; i >= 0; i--) {
         SeedObject seedObject = new SeedObject(
            textureName,
            drawOffset,
            i,
            productStringID,
            productAmount,
            stringIDPrefix,
            minGrowTime,
            maxGrowTime,
            canBePlacedAsFlower,
            flowerID,
            debrisColor,
            rarity,
            itemSpoilDurationMinutes
         );
         int id = ObjectRegistry.registerObject(stringIDPrefix + (i == 0 ? "" : i), seedObject, brokerValue, i <= 0);
         objects[i] = seedObject;
         ids[i] = id;
      }

      for (SeedObject object : objects) {
         object.stageIDs = ids;
      }

      return objects;
   }
}
