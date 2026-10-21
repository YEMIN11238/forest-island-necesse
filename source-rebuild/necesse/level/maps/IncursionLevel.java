package necesse.level.maps;

import java.awt.Point;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import necesse.engine.GameLog;
import necesse.engine.incursionPerkTree.RescueSettlerRewardPerk;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.IncursionPerksRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.util.TeleportResult;
import necesse.engine.world.OneWorldMigration;
import necesse.engine.world.WorldEntity;
import necesse.entity.Entity;
import necesse.entity.levelEvent.IncursionLevelEvent;
import necesse.entity.levelEvent.LevelEvent;
import necesse.entity.levelEvent.TeleportEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.hostile.bosses.ReturnPortalMob;
import necesse.entity.objectEntity.FallenAltarObjectEntity;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.pickup.ItemPickupEntity;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.miscItem.GatewayTabletItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.inventory.lootTable.lootItem.LootItemMultiplierIgnored;
import necesse.level.gameObject.FallenAltarObject;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.RespawnObject;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.generationModules.CaveGeneration;
import necesse.level.maps.generationModules.PresetGeneration;
import necesse.level.maps.incursion.AltarData;
import necesse.level.maps.incursion.IncursionData;
import necesse.level.maps.levelBuffManager.LevelModifiers;

public class IncursionLevel extends Level {
   public static LootTable randomTabletLootTable = new LootTable(
      new LootItemInterface() {
         @Override
         public void addPossibleLoot(LootList list, Object... extra) {
            list.add("gatewaytablet");
         }

         @Override
         public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
            Mob mob = LootTable.expectExtra(Mob.class, extra, 0);
            if (mob != null) {
               Level level = mob.getLevel();
               if (level instanceof IncursionLevel) {
                  IncursionLevel incursionLevel = (IncursionLevel)level;
                  IncursionData incursionData = incursionLevel.incursionData;
                  if (incursionData != null) {
                     IncursionLevelEvent event = incursionLevel.getIncursionLevelEvent();
                     if (event != null) {
                        float chance = event.getRandomTabletDropChance(mob);
                        if (!(chance <= 0.0F)) {
                           LootTable.runChance(
                              random,
                              chance,
                              lootMultiplier,
                              remainingLootMultiplier -> {
                                 int tabletsDropping = 1;
                                 if (incursionData.currentIncursionPerkIDs.contains(IncursionPerksRegistry.CHANCE_TO_DROP_DOUBLE_TABLETS.getID())
                                    && random.getChance(0.35F)) {
                                    tabletsDropping = 2;
                                 }

                                 for (int i = 0; i < tabletsDropping; i++) {
                                    InventoryItem gatewayTablet = new InventoryItem("gatewaytablet");
                                    int tier = incursionData.getTabletTier();

                                    while (random.getChance(0.5F) && tier > 1) {
                                       tier--;
                                    }

                                    GatewayTabletItem.initializeGatewayTablet(gatewayTablet, random, tier, incursionData);
                                    list.add(gatewayTablet);
                                 }
                              }
                           );
                        }
                     }
                  }
               }
            }
         }
      }
   );
   public IncursionData incursionData;
   public int incursionEventUniqueID;
   public LevelIdentifier altarLevelIdentifier;
   public int altarTileX = Integer.MIN_VALUE;
   public int altarTileY = Integer.MIN_VALUE;
   public String upgradeShardsRockID;
   public String alchemyShardsRockID;
   public String essenceRockID;
   public int returnPortalUniqueID;
   public Point returnPortalPosition;

   public IncursionLevel(LevelIdentifier identifier, int width, int height, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
   }

   public IncursionLevel(LevelIdentifier identifier, int width, int height, IncursionData incursion, WorldEntity worldEntity) {
      super(identifier, width, height, worldEntity);
      this.incursionData = incursion;
      this.keepTrackOfReturnedItems = true;
      this.buffManager.forceUpdateBuffs();
      this.upgradeShardsRockID = "upgradesharddeeprock";
      this.alchemyShardsRockID = "alchemysharddeeprock";
      this.essenceRockID = "shadowessencedeeprock";
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addInt("incursionEventUniqueID", this.incursionEventUniqueID);
      if (this.incursionData != null) {
         SaveData incursionSaveData = new SaveData("incursionData");
         this.incursionData.addSaveData(incursionSaveData);
         save.addSaveData(incursionSaveData);
      }

      if (this.altarLevelIdentifier != null) {
         save.addUnsafeString("altarLevel", this.altarLevelIdentifier.stringID);
      }

      if (this.altarTileX != Integer.MIN_VALUE) {
         save.addInt("altarTileX", this.altarTileX);
      }

      if (this.altarTileY != Integer.MIN_VALUE) {
         save.addInt("altarTileY", this.altarTileY);
      }

      save.addInt("returnPortalUniqueID", this.returnPortalUniqueID);
      if (this.returnPortalPosition != null) {
         save.addPoint("returnPortalPosition", this.returnPortalPosition);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.incursionEventUniqueID = save.getInt("incursionEventUniqueID", this.incursionEventUniqueID, false);
      LoadData incursionSaveData = save.getFirstLoadDataByName("incursionData");
      if (incursionSaveData != null) {
         try {
            this.incursionData = IncursionData.fromLoadData(incursionSaveData);
         } catch (Exception e) {
            System.err.println("Could not load incursion data from level " + this.getIdentifier());
            e.printStackTrace();
         }
      } else {
         this.incursionData = null;
      }

      this.buffManager.forceUpdateBuffs();
      String altarLevelIdentifierSave = save.getUnsafeString("altarLevel", null, false);
      if (altarLevelIdentifierSave != null) {
         this.altarLevelIdentifier = new LevelIdentifier(altarLevelIdentifierSave);
      }

      if (save.getInt("altarTileX", this.altarTileX) != Integer.MIN_VALUE) {
         this.altarTileX = save.getInt("altarTileX", this.altarTileX);
      }

      if (save.getInt("altarTileY", this.altarTileY) != Integer.MIN_VALUE) {
         this.altarTileY = save.getInt("altarTileY", this.altarTileY);
      }

      this.returnPortalUniqueID = save.getInt("returnPortalUniqueID", this.returnPortalUniqueID, false);
      this.returnPortalPosition = save.getPoint("returnPortalPosition", this.returnPortalPosition, false);
   }

   @Override
   public void writeLevelDataPacket(PacketWriter writer) {
      super.writeLevelDataPacket(writer);
      writer.putNextInt(this.incursionEventUniqueID);
      if (this.incursionData != null) {
         writer.putNextBoolean(true);
         IncursionData.writePacket(this.incursionData, writer);
      } else {
         writer.putNextBoolean(false);
      }
   }

   @Override
   public void readLevelDataPacket(PacketReader reader) {
      super.readLevelDataPacket(reader);
      this.incursionEventUniqueID = reader.getNextInt();
      if (reader.getNextBoolean()) {
         this.incursionData = IncursionData.fromPacket(reader);
      } else {
         this.incursionData = null;
      }
   }

   public void spawnReturnPortal(float levelX, float levelY) {
      Mob mob = MobRegistry.getMob("returnportal", this);
      this.entityManager.addMob(mob, levelX, levelY);
      this.returnPortalPosition = mob.getPositionPoint();
      this.returnPortalUniqueID = mob.getUniqueID();
   }

   public Point getReturnPortalPosition() {
      Point returnPortalPosition = this.returnPortalPosition;
      if (returnPortalPosition == null && this.returnPortalUniqueID != 0) {
         Mob mob = this.entityManager.mobs.get(this.returnPortalUniqueID, false);
         if (mob != null) {
            returnPortalPosition = mob.getPositionPoint();
            this.returnPortalPosition = returnPortalPosition;
         }
      }

      if (returnPortalPosition == null) {
         returnPortalPosition = this.entityManager
            .mobs
            .stream()
            .filter(m -> m instanceof ReturnPortalMob)
            .map(Entity::getPositionPoint)
            .findFirst()
            .orElse(null);
         if (returnPortalPosition != null) {
            this.returnPortalPosition = returnPortalPosition;
         }
      }

      if (returnPortalPosition == null) {
         returnPortalPosition = new Point(this.tileWidth * 32 / 2, this.tileHeight * 32 / 2);
      }

      return returnPortalPosition;
   }

   public Point getBossPortalCenterPosition() {
      return this.getReturnPortalPosition();
   }

   public int getBossPortalCenterOffset() {
      return 96;
   }

   public GameMessage getBossPortalSpawnedMessage() {
      return new LocalMessage("misc", "bossportal");
   }

   public Point getBossSpawnPosition() {
      return this.getReturnPortalPosition();
   }

   public void updateBossOffsetPosition(Mob mob, int spawnLevelX, int spawnLevelY) {
      int finalLevelX = spawnLevelX;
      int finalLevelY = spawnLevelY;
      PlayerMob closestPlayer = GameUtils.streamNetworkClients(this)
         .map(c -> c.playerMob)
         .filter(Objects::nonNull)
         .min(Comparator.comparingDouble(m -> GameMath.diagonalMoveDistance(finalLevelX - m.x, finalLevelY - m.y)))
         .orElse(null);
      if (closestPlayer != null) {
         spawnLevelX = closestPlayer.getX();
         spawnLevelY = closestPlayer.getY();
      }

      float angle = GameRandom.globalRandom.nextInt(360);
      float nx = GameMath.cos(angle);
      float ny = GameMath.sin(angle);
      float distance = 960.0F;
      mob.setPos(spawnLevelX + (int)(nx * distance), spawnLevelY + (int)(ny * distance), true);
   }

   public void onIncursionObjectiveCompleted(IncursionLevelEvent event) {
   }

   public ReturnPortalMob getReturnPortal() {
      if (this.returnPortalUniqueID == 0) {
         return null;
      }

      Mob mob = this.entityManager.mobs.get(this.returnPortalUniqueID, false);
      return mob instanceof ReturnPortalMob ? (ReturnPortalMob)mob : null;
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultLevelModifiers() {
      Stream<ModifierValue<Boolean>> bannerDisabledStream = Stream.of(new ModifierValue<>(LevelModifiers.BANNER_OF_WAR_DISABLED, true));
      return this.incursionData == null
         ? Stream.concat(super.getDefaultLevelModifiers(), bannerDisabledStream)
         : GameUtils.streamConcat(super.getDefaultLevelModifiers(), this.incursionData.getDefaultLevelModifiers(), bannerDisabledStream);
   }

   @Override
   public Stream<ModifierValue<?>> getMobModifiers(Mob mob) {
      Stream<ModifierValue<?>> out = super.getMobModifiers(mob);
      if (this.incursionData != null) {
         out = Stream.concat(out, this.incursionData.getMobModifiers(mob));
      }

      return out;
   }

   @Override
   public LootTable getExtraMobDrops(Mob mob) {
      if (this.incursionData == null) {
         return super.getExtraMobDrops(mob);
      }

      LootTable lootTable = new LootTable(super.getExtraMobDrops(mob), this.incursionData.getExtraMobDrops(mob));
      if (mob.isHostile && !mob.isSummoned) {
         lootTable = new LootTable(lootTable, new LootItemMultiplierIgnored(randomTabletLootTable));
      }

      return lootTable;
   }

   @Override
   public LootTable getExtraPrivateMobDrops(Mob mob, ServerClient client) {
      return this.incursionData == null
         ? super.getExtraPrivateMobDrops(mob, client)
         : new LootTable(super.getExtraPrivateMobDrops(mob, client), this.incursionData.getExtraPrivateMobDrops(mob, client));
   }

   @Override
   public GameMessage getSetSpawnError(int x, int y, ServerClient client) {
      return new LocalMessage("misc", "spawnincursion");
   }

   @Override
   public LootTable getCrateLootTable(int tileX, int tileY) {
      return this.incursionData != null && this.incursionData.currentIncursionPerkIDs.contains(IncursionPerksRegistry.CRATES_DROP_ALTAR_DUST.getID())
         ? LootTablePresets.incursionCrateWithAltarDust
         : LootTablePresets.incursionCrate;
   }

   @Override
   public boolean isIncursionLevel() {
      return true;
   }

   public IncursionLevelEvent getIncursionLevelEvent() {
      LevelEvent event = this.entityManager.events.get(this.incursionEventUniqueID, false);
      return event instanceof IncursionLevelEvent ? (IncursionLevelEvent)event : null;
   }

   public GameMessage canSummonBoss(String bossStringID) {
      IncursionLevelEvent event = this.getIncursionLevelEvent();
      return event != null ? event.canSpawnBoss(bossStringID) : new LocalMessage("misc", "cannotsummonhere");
   }

   public void onBossSummoned(Mob mob) {
      IncursionLevelEvent event = this.getIncursionLevelEvent();
      if (event != null) {
         event.onBossSummoned(mob);
      }
   }

   @Override
   public GameMessage getLocationMessage(int tileX, int tileY) {
      return this.incursionData != null ? this.incursionData.getDisplayName() : super.getLocationMessage(tileX, tileY);
   }

   public void returnToAltar(ServerClient client) {
      if (this.altarLevelIdentifier != null && this.altarTileX != Integer.MIN_VALUE && this.altarTileY != Integer.MIN_VALUE) {
         LevelEvent e = new TeleportEvent(client, 0, this.altarLevelIdentifier, 3.0F, null, level -> {
            level.regionManager.ensureTileIsLoaded(this.altarTileX, this.altarTileY);
            GameObject object = level.getObject(this.altarTileX, this.altarTileY);
            if (object instanceof FallenAltarObject) {
               return new TeleportResult(true, client.getPlayerPosFromTile(level, this.altarTileX, this.altarTileY));
            }

            this.altarLevelIdentifier = null;
            client.validateSpawnPoint(false);
            Point spawnPos;
            if (!client.isDefaultSpawnPoint()) {
               Point offset = RespawnObject.calculateSpawnOffset(level, client.spawnTile.x, client.spawnTile.y, client);
               if (offset == null) {
                  offset = new Point(0, 0);
               }

               spawnPos = new Point(client.spawnTile.x * 32 + offset.x, client.spawnTile.y * 32 + offset.y);
            } else {
               spawnPos = client.getPlayerPosFromTile(level, client.spawnTile.x, client.spawnTile.y);
            }

            return new TeleportResult(true, spawnPos);
         });
         client.getLevel().entityManager.events.addHidden(e);
      } else {
         this.altarLevelIdentifier = null;
         client.validateSpawnPoint(false);
         LevelEvent e = new TeleportEvent(client, 0, client.spawnLevelIdentifier, 3.0F, null, level -> {
            Point spawnPos;
            if (!client.isDefaultSpawnPoint()) {
               Point offset = RespawnObject.calculateSpawnOffset(level, client.spawnTile.x, client.spawnTile.y, client);
               spawnPos = new Point(client.spawnTile.x * 32 + offset.x, client.spawnTile.y * 32 + offset.y);
            } else {
               spawnPos = client.getPlayerPosFromTile(level, client.spawnTile.x, client.spawnTile.y);
            }

            return new TeleportResult(true, spawnPos);
         });
         client.getLevel().entityManager.events.addHidden(e);
      }
   }

   public FallenAltarObjectEntity getAltarObjectEntity() {
      if (!this.isServer()) {
         return null;
      }

      if (this.altarLevelIdentifier != null) {
         Level altarLevel = this.getServer().world.getLevel(this.altarLevelIdentifier);
         altarLevel.regionManager.ensureTileIsLoaded(this.altarTileX, this.altarTileY);
         ObjectEntity objectEntity = altarLevel.entityManager.getObjectEntity(this.altarTileX, this.altarTileY);
         if (objectEntity instanceof FallenAltarObjectEntity) {
            return (FallenAltarObjectEntity)objectEntity;
         }
      }

      return null;
   }

   public void markCanComplete(
      Supplier<Point> sharedLootDropPositionGetter,
      Supplier<Stream<ServerClient>> clientsStreamGetter,
      Function<ServerClient, Point> privateLootDropPositionGetter
   ) {
      FallenAltarObjectEntity altarObjectEntity = this.getAltarObjectEntity();
      if (altarObjectEntity != null) {
         altarObjectEntity.markCanComplete(this);
      }

      if (this.incursionData != null) {
         int lootDuplicationCounter = 1;
         if (this.incursionData.currentIncursionPerkIDs.contains(IncursionPerksRegistry.INCREASED_BOSS_LOOT.getID())) {
            lootDuplicationCounter = 2;
         }

         for (int i = 0; i < lootDuplicationCounter; i++) {
            Point dropLocation = sharedLootDropPositionGetter.get();
            if (dropLocation == null) {
               dropLocation = new Point(
                  this.tileWidth > 0 ? GameMath.getLevelCoordinate(this.tileWidth) / 2 + 16 : 0,
                  this.tileHeight > 0 ? GameMath.getLevelCoordinate(this.tileHeight) / 2 + 16 : 0
               );
            }

            for (InventoryItem inventoryItem : this.incursionData.getPlayerSharedIncursionCompleteRewards().getRewards(false)) {
               String settlerReward = inventoryItem.getGndData().getString("settlerIncursionReward");
               if (settlerReward != null && !settlerReward.isEmpty()) {
                  RescueSettlerRewardPerk.IncursionMobReward reward = RescueSettlerRewardPerk.settlers.get(settlerReward);
                  if (reward != null) {
                     reward.spawnMob(this, dropLocation.x, dropLocation.y, this.incursionData);
                  } else {
                     GameLog.warn.println("Detected invalid settler reward stringID: " + settlerReward);
                  }
               } else {
                  ItemPickupEntity pickupEntity = inventoryItem.getPickupEntity(this, dropLocation.x, dropLocation.y);
                  pickupEntity.showsLightBeam = true;
                  this.entityManager.pickups.add(pickupEntity);
               }
            }
         }

         Stream<ServerClient> clients = null;
         if (clientsStreamGetter != null) {
            clients = clientsStreamGetter.get();
         }

         Stream<ServerClient> allClients = GameUtils.streamServerClients(this);
         if (clients == null) {
            clients = allClients;
         } else {
            clients = Stream.concat(clients, allClients);
         }

         clients.distinct().forEach(c -> {
            this.incursionData.onCompleted(c);
            if (c.achievementsLoaded()) {
               c.achievements().COMPLETE_INCURSION.markCompleted(c);
            }

            for (InventoryItem inventoryItemx : this.incursionData.getPlayerPersonalIncursionCompleteRewards().getRewards(false)) {
               Point dropLocation = privateLootDropPositionGetter.apply(c);
               if (dropLocation == null) {
                  dropLocation = c.playerMob.getPositionPoint();
               }

               ItemPickupEntity pickupEntityx = inventoryItemx.getPickupEntity(this, dropLocation.x, dropLocation.y);
               pickupEntityx.setReservedAuth(c.authentication);
               pickupEntityx.showsLightBeam = true;
               this.entityManager.pickups.add(pickupEntityx);
            }
         });
      }
   }

   public void generatePresetsBasedOnPerks(AltarData altarData, IncursionData incursionData, PresetGeneration presetGeneration, GameRandom random, Biome biome) {
      for (Integer obtainedPerkID : altarData.obtainedPerkIDs) {
         IncursionPerksRegistry.getPerk(obtainedPerkID).onIncursionStructuresGenerated(altarData, incursionData, presetGeneration, random, biome);
      }
   }

   public void generateOresBasedOnPerks(AltarData altarData, CaveGeneration cg, IncursionLevel incursionLevel, Biome biome, GameRandom random) {
      for (Integer obtainedPerkID : altarData.obtainedPerkIDs) {
         IncursionPerksRegistry.getPerk(obtainedPerkID).onGenerateOres(cg, incursionLevel, biome, random);
      }
   }

   @Override
   public void migrateToOldLevel(OneWorldMigration migrationData, LevelIdentifier oldLevelIdentifier) {
      super.migrateToOldLevel(migrationData, oldLevelIdentifier);
      if (this.altarLevelIdentifier != null) {
         LevelIdentifier oldAltarLevelIdentifier = this.altarLevelIdentifier;
         this.altarLevelIdentifier = migrationData.getNewLevelIdentifier(oldAltarLevelIdentifier);
         Point tileOffset = migrationData.getTilePositionOffset(oldAltarLevelIdentifier);
         if (this.altarTileX != Integer.MIN_VALUE) {
            this.altarTileX = this.altarTileX + tileOffset.x;
         }

         if (this.altarTileY != Integer.MIN_VALUE) {
            this.altarTileY = this.altarTileY + tileOffset.y;
         }
      }
   }
}
