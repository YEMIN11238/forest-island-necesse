package necesse.level.maps.incursion;

import java.awt.Color;
import java.awt.Point;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import necesse.engine.Settings;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.server.Server;
import necesse.engine.registries.IDData;
import necesse.engine.registries.IncursionBiomeRegistry;
import necesse.engine.registries.IncursionPerksRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.RegistryClosedException;
import necesse.engine.registries.TileRegistry;
import necesse.engine.registries.UniqueIncursionRewardsRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TicketSystemList;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.FallenAltarObjectEntity;
import necesse.gfx.GameColor;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.object.fallenAltar.FallenAltarContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.miscItem.GatewayTabletItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.generationModules.PresetGeneration;

public abstract class IncursionBiome {
   public final IDData idData = new IDData();
   public GameMessage displayName;
   protected GameTexture tabletTexture;
   public String bossMobStringID;
   public String requiredPerkStringID;

   public final String getStringID() {
      return this.idData.getStringID();
   }

   public final int getID() {
      return this.idData.getID();
   }

   public IncursionBiome(String bossMobStringID) {
      if (IncursionBiomeRegistry.instance.isClosed()) {
         throw new RegistryClosedException(
            "Cannot construct IncursionBiome objects when registry is closed, since they are a static registered objects. Use IncursionBiome.getBiome(...) to get incursion biomes."
         );
      }

      this.bossMobStringID = bossMobStringID;
   }

   public void onIncursionBiomeRegistryClosed() {
   }

   public GameMessage getNewLocalization() {
      return new LocalMessage("biome", this.getStringID());
   }

   public void updateLocalDisplayName() {
      this.displayName = this.getNewLocalization();
   }

   public GameMessage getLocalization() {
      return this.displayName;
   }

   public void loadTextures() {
      try {
         this.tabletTexture = GameTexture.fromFileRaw("items/" + this.getStringID() + "tablet");
      } catch (FileNotFoundException e) {
         this.tabletTexture = null;
      }
   }

   public GameSprite getTabletSprite() {
      return this.tabletTexture == null ? null : new GameSprite(this.tabletTexture);
   }

   public int increaseTabletTierByX(IncursionData incursionData, int currentTier, int amountToIncrease) {
      int newTier = currentTier + amountToIncrease;
      int maxRollableTier = UniqueIncursionRewardsRegistry.getMaxTierAvailableForTabletDrop(incursionData.nextIncursionPerkIDs);
      return GameMath.limit(newTier, IncursionData.MINIMUM_TIER, maxRollableTier);
   }

   public abstract Collection<Item> getExtractionItems(IncursionData var1);

   public abstract LootTable getHuntDrop(IncursionData var1);

   public LootTable getBossDrop(final IncursionData incursionData) {
      return new LootTable(
         new LootItemInterface() {
            @Override
            public void addPossibleLoot(LootList list, Object... extra) {
               InventoryItem gatewayTablet = new InventoryItem("gatewaytablet");
               gatewayTablet.getGndData().setInt("displayTier", IncursionBiome.this.increaseTabletTierByX(incursionData, incursionData.getTabletTier(), 1));
               list.addCustom(gatewayTablet);
            }

            @Override
            public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
               InventoryItem gatewayTablet = new InventoryItem("gatewaytablet");
               GatewayTabletItem.initializeGatewayTablet(
                  gatewayTablet, random, IncursionBiome.this.increaseTabletTierByX(incursionData, incursionData.getTabletTier(), 1), incursionData
               );
               list.add(gatewayTablet);
            }
         }
      );
   }

   public abstract TicketSystemList<Supplier<IncursionData>> getAvailableIncursions(int var1, IncursionData var2);

   public void addDefaultTabletItems(GatewayTabletItem tabletItem, List<InventoryItem> list, PlayerMob player) {
      for (int tier = 1; tier <= 10; tier++) {
         TicketSystemList<Supplier<IncursionData>> incursions = this.getAvailableIncursions(tier, null);

         for (Supplier<IncursionData> supplier : incursions.getAll()) {
            InventoryItem invItem = new InventoryItem(tabletItem);
            IncursionData data = supplier.get();
            GatewayTabletItem.setIncursionData(invItem, data);
            list.add(invItem);
         }
      }
   }

   public ArrayList<FairType> getPrivateDropsDisplay(FontOptions fontOptions) {
      return null;
   }

   public IncursionData getRandomIncursion(GameRandom seededRandom, int tabletTier, IncursionData incursionData) {
      Supplier<IncursionData> getter = this.getAvailableIncursions(tabletTier, incursionData).getRandomObject(seededRandom);
      return getter == null ? null : getter.get();
   }

   public boolean canDropInIncursion(IncursionData incursionData) {
      if (this.requiredPerkStringID == null) {
         return true;
      }

      if (incursionData == null) {
         return false;
      }

      int perkID = IncursionPerksRegistry.getPerkID(this.requiredPerkStringID);
      return incursionData.currentIncursionPerkIDs.contains(perkID);
   }

   public int getUniqueModifierTickets(UniqueIncursionModifier modifier) {
      return 100;
   }

   public String getCanOpenError(BiomeMissionIncursionData incursionData, FallenAltarContainer container) {
      return null;
   }

   public abstract IncursionLevel getNewIncursionLevel(
      FallenAltarObjectEntity var1, LevelIdentifier var2, BiomeMissionIncursionData var3, Server var4, WorldEntity var5, AltarData var6
   );

   public abstract ArrayList<Color> getFallenAltarGatewayColorsForBiome();

   public static void addReturnPortalOnTile(Level level, int portalTileX, int portalTileY) {
      addReturnPortal(level, portalTileX * 32 + 16, portalTileY * 32 + 16);
   }

   public static void addReturnPortal(Level level, float portalLevelX, float portalLevelY) {
      if (level instanceof IncursionLevel) {
         ((IncursionLevel)level).spawnReturnPortal(portalLevelX, portalLevelY);
      } else {
         level.entityManager.addMob(MobRegistry.getMob("returnportal", level), portalLevelX, portalLevelY);
      }
   }

   public static Point generateEntrance(
      Level level,
      PresetGeneration presets,
      GameRandom random,
      int spawnSize,
      int baseTileID,
      String brickTileStringID,
      String floorTileStringID,
      String columnStringID,
      boolean hasBiggerArenaPerk
   ) {
      int spawnPadding = 40;
      int spawnMidX = random.getIntOffset(level.tileWidth / 2, level.tileWidth / 2 - spawnPadding - spawnSize / 2);
      int spawnMidY = random.getIntOffset(level.tileHeight / 2, level.tileHeight / 2 - spawnPadding - spawnSize / 2);
      return generateEntrance(
         level, spawnMidX, spawnMidY, presets, random, spawnSize, baseTileID, brickTileStringID, floorTileStringID, columnStringID, hasBiggerArenaPerk
      );
   }

   public static Point generateEntrance(
      Level level,
      int posX,
      int posY,
      PresetGeneration presets,
      GameRandom random,
      int spawnSize,
      int baseTileID,
      String brickTileStringID,
      String floorTileStringID,
      String columnStringID,
      boolean hasBiggerArenaPerk
   ) {
      generateEntrance(level, presets, random, posX, posY, spawnSize, baseTileID, brickTileStringID, floorTileStringID, columnStringID, hasBiggerArenaPerk);
      return new Point(posX, posY);
   }

   public static void generateEntrance(
      Level level,
      PresetGeneration presets,
      GameRandom random,
      int spawnMidX,
      int spawnMidY,
      int spawnSize,
      int baseTileID,
      String brickTileStringID,
      String floorTileStringID,
      String columnStringID,
      boolean hasBiggerArenaPerk
   ) {
      if (hasBiggerArenaPerk) {
         spawnSize += (int)(spawnSize * 0.5F);
      }

      int brickTile = brickTileStringID == null ? -1 : TileRegistry.getTileID(brickTileStringID);
      int floorTile = floorTileStringID == null ? -1 : TileRegistry.getTileID(floorTileStringID);
      Function<GameRandom, Integer> floorFunction;
      if (brickTile != -1) {
         if (floorTile != -1) {
            floorFunction = r -> r.getChance(0.75F) ? brickTile : floorTile;
         } else {
            floorFunction = r -> brickTile;
         }
      } else if (floorTile != -1) {
         floorFunction = r -> floorTile;
      } else {
         floorFunction = null;
      }

      float maxDistance = spawnSize / 2.0F * 32.0F;

      for (int tileX = spawnMidX - spawnSize; tileX <= spawnMidX + spawnSize; tileX++) {
         for (int tileY = spawnMidY - spawnSize; tileY < spawnMidY + spawnSize; tileY++) {
            float distance = (float)new Point(spawnMidX * 32 + 16, spawnMidY * 32 + 16).distance(tileX * 32 + 16, tileY * 32 + 16);
            float distancePerc = distance / maxDistance;
            if (distancePerc < 0.5F) {
               if (floorFunction != null) {
                  level.setTile(tileX, tileY, floorFunction.apply(random));
               }

               level.setObject(tileX, tileY, 0);
            } else if (distancePerc <= 1.0F) {
               float chance = Math.abs((distancePerc - 0.5F) * 2.0F - 1.0F) * 2.0F;
               if (random.getChance(chance)) {
                  if (random.getChance(0.75F)) {
                     if (floorFunction != null) {
                        level.setTile(tileX, tileY, floorFunction.apply(random));
                     }
                  } else if (baseTileID != -1) {
                     level.setTile(tileX, tileY, baseTileID);
                  }

                  level.setObject(tileX, tileY, 0);
               }
            }
         }
      }

      if (columnStringID != null) {
         int totalColumns = random.getIntBetween(6, 8);
         if (hasBiggerArenaPerk) {
            totalColumns += 8;
         }

         float columnAngle = random.nextInt(360);
         float anglePerColumn = 360.0F / totalColumns;
         int columnID = ObjectRegistry.getObjectID(columnStringID);

         for (int i = 0; i < totalColumns; i++) {
            columnAngle += random.getFloatOffset(anglePerColumn, anglePerColumn / 10.0F);
            java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(columnAngle);
            float distance = spawnSize / 3.5F * 32.0F;
            if (hasBiggerArenaPerk) {
               distance = spawnSize / 2.5F * 32.0F;
            }

            int tileX = GameMath.getTileCoordinate(spawnMidX * 32 + 16 + dir.x * distance);
            int tileY = GameMath.getTileCoordinate(spawnMidY * 32 + 16 + dir.y * distance);
            level.setObject(tileX, tileY, columnID);
         }
      }

      int torchID = ObjectRegistry.getObjectID("torch");
      if (hasBiggerArenaPerk) {
         addTorchesToArena(random, level, spawnSize, spawnMidX, spawnMidY, torchID);
      }

      presets.addOccupiedSpace(spawnMidX - spawnSize / 2, spawnMidY - spawnSize / 2, spawnSize, spawnSize);
      addReturnPortalOnTile(level, spawnMidX, spawnMidY);
   }

   public static void addTorchesToArena(GameRandom random, Level level, int spawnSize, int spawnMidX, int spawnMidY, int torchID) {
      int totalTorches = 86;
      float torchAngle = random.nextInt(360);
      int torchesPerCircle = 4;
      int torchCounter = 0;
      float anglePerTorch = 360.0F / torchesPerCircle;
      float distanceMultiplier = 10.0F;
      float distanceRemovedPerCircle = 2.5F;

      for (int i = 0; i < totalTorches; i++) {
         torchAngle += random.getFloatOffset(anglePerTorch, anglePerTorch / 10.0F);
         java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(torchAngle);
         float distance = spawnSize / distanceMultiplier * 32.0F;
         int tileX = GameMath.getTileCoordinate(spawnMidX * 32 + 16 + dir.x * distance);
         int tileY = GameMath.getTileCoordinate(spawnMidY * 32 + 16 + dir.y * distance);
         if (level.getObjectID(tileX, tileY) == 0) {
            level.setObject(tileX, tileY, torchID);
         }

         if (torchCounter >= torchesPerCircle) {
            torchCounter = 0;
            torchesPerCircle += 4;
            distanceMultiplier -= distanceRemovedPerCircle;
            if (distanceMultiplier < 2.5F) {
               distanceMultiplier = 2.5F;
            }

            if (distanceRemovedPerCircle > 1.0F) {
               distanceRemovedPerCircle -= 0.5F;
            }

            anglePerTorch = 360.0F / torchesPerCircle;
         }

         torchCounter++;
      }
   }

   public static HashMap<Integer, Float> saveMobHealths(Level level) {
      HashMap<Integer, Float> saved = new HashMap<>();

      for (Mob mob : level.entityManager.mobs) {
         saved.put(mob.getUniqueID(), (float)mob.getHealth() / mob.getMaxHealth());
      }

      return saved;
   }

   public static void applyMobHealths(Level level, HashMap<Integer, Float> saved) {
      for (Mob mob : level.entityManager.mobs) {
         saved.computeIfPresent(mob.getUniqueID(), (uniqueID, healthPercent) -> {
            mob.setHealthHidden((int)(mob.getMaxHealth() * healthPercent));
            return (Float)healthPercent;
         });
      }
   }

   public LootTable getExtraIncursionDrops(Mob mob) {
      LootTable mobDrops = new LootTable(this.getHuntDrop(null), new LootItem("gatewaytablet", 1));
      return mob.isBoss() ? new LootTable(mobDrops, new LootItem("upgradeshard", 1), new LootItem("alchemyshard", 1)) : mobDrops;
   }

   public GameTooltips getKnownIncursionDataPreRewardsTooltip(IncursionData incursionData, GameColor color) {
      return null;
   }

   public GameTooltips getUnknownIncursionDataTooltip(GameColor color) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(new StringTooltips(Localization.translate("ui", "incursionrandommodifier"), color));
      tooltips.add(new StringTooltips(Localization.translate("ui", "incursionrandomreward"), color));
      return tooltips;
   }

   public void setupTypeAndTierLabels(BiomeMissionIncursionData data, int fontSize, FormContentBox content, FormFlow flow) {
      LocalMessage tierText = new LocalMessage(
         "item", "tierandincursiontype", "tiernumber", data.getTabletTier(), "incursiontype", data.getIncursionMissionTypeName()
      );
      content.addComponent(
            flow.nextY(new FormLocalLabel(tierText, new FontOptions(fontSize), 0, content.getMinContentWidth() / 2, 0, content.getMinContentWidth() - 10), 4)
         )
         .setColor(Settings.UI.incursionTierPurple);
   }
}
