package necesse.engine.network.client;

import necesse.engine.GameLog;
import necesse.engine.dlc.DLC;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.PacketReader;
import necesse.engine.network.packet.PacketClientInstalledDLC;
import necesse.engine.network.packet.PacketPlayerAppearance;
import necesse.engine.network.packet.PacketPlayerBuffs;
import necesse.engine.network.packet.PacketPlayerGeneral;
import necesse.engine.network.packet.PacketPlayerInventory;
import necesse.engine.network.packet.PacketPlayerInventoryPart;
import necesse.engine.network.packet.PacketPlayerLevelChange;
import necesse.engine.network.packet.PacketPlayerRespawn;
import necesse.engine.network.packet.PacketRequestPacket;
import necesse.engine.network.packet.PacketRequestPlayerData;
import necesse.engine.util.GameMath;
import necesse.engine.util.LevelIdentifier;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.Level;

public class ClientClient extends NetworkClient {
   private final Client client;
   private LevelIdentifier levelIdentifier;
   public int latency;
   public boolean loadedPlayer;
   protected long lastRequestLevelDataTime;
   protected boolean lastPrivateSyncError = false;

   public ClientClient(Client client, int slot, PacketPlayerGeneral generalPacket) {
      super(slot, generalPacket.authentication);
      this.client = client;
      this.makeClientClient();
      this.pvpEnabled = false;
      this.setTeamID(-1);
      this.applyGeneralPacket(generalPacket);
   }

   public void applySyncPacket(PacketReader reader) {
      if (reader.getNextBoolean()) {
         if (!this.hasSpawned) {
            this.applySpawned(0);
         }
      } else {
         this.hasSpawned = false;
      }

      if (reader.getNextBoolean()) {
         this.die(reader.getNextInt());
      } else {
         this.isDead = false;
      }

      this.pvpEnabled = reader.getNextBoolean();
      this.setTeamID(reader.getNextInt());
      if (reader.getNextBoolean() && this.client.getLevel() != null && !this.isSamePlace(this.client.getLevel())) {
         if (this.slot == this.client.slot) {
            this.client.reloadMap();
         } else {
            this.client.network.sendPacket(new PacketRequestPlayerData(this.slot));
         }
      }

      if (this.slot == this.client.slot) {
         if (reader.getNextBoolean()) {
            this.client.syncOpenContainer(reader.getNextInt());
         } else {
            this.client.syncOpenContainer(-1);
         }
      }

      boolean containsPositionData = reader.getNextBoolean();
      if (containsPositionData) {
         int x = reader.getNextInt();
         int y = reader.getNextInt();
         int dir = reader.getNextByteUnsigned();
         int tileX = GameMath.getTileCoordinate(x);
         int tileY = GameMath.getTileCoordinate(y);
         if (this.loadedPlayer && this.playerMob != null) {
            ClientLevelLoading loading = this.client.levelManager.loading();
            if (loading != null && !loading.isTileLoaded(tileX, tileY)) {
               this.playerMob.setPos(x, y, true);
               this.playerMob.setDir(dir);
            }
         }
      }

      this.relationshipData.readPacket(reader);
   }

   public void applyPrivateSyncPacket(PacketReader reader) {
      boolean lastPrivateSyncError = this.lastPrivateSyncError;
      this.lastPrivateSyncError = false;
      LevelIdentifier identifier = new LevelIdentifier(reader);
      if (!identifier.equals(this.levelIdentifier)) {
         this.lastPrivateSyncError = true;
         if (lastPrivateSyncError) {
            System.out.println("Detected desync of level between the server and client. Requesting level change...");
            this.client.network.sendPacket(new PacketRequestPacket(PacketRequestPacket.RequestType.LEVEL_CHANGE));
         }
      }

      int loadedRegionsHashCode = reader.getNextInt();
      ClientLevelLoading loading = this.client.levelManager.loading();
      if (loading != null && loading.isLoadingDone()) {
         int myLoadedHashCode = loading.getLoadedRegionsHashCode();
         if (loadedRegionsHashCode != myLoadedHashCode) {
            this.lastPrivateSyncError = true;
            if (lastPrivateSyncError) {
               System.out.println("Detected desync of loaded regions between the server and client. Requesting loaded regions...");
               this.client.network.sendPacket(new PacketRequestPacket(PacketRequestPacket.RequestType.LOADED_REGIONS));
            }
         }
      }
   }

   public void tickMovement(Client client, float delta) {
      if (this.playerMob != null) {
         if (this.loadedPlayer) {
            if (this.isSamePlace(client.getLevel())) {
               this.playerMob.setLevel(client.getLevel());
               synchronized (this.playerMob.getLevel().entityManager.lock) {
                  this.playerMob.getLevel().entityManager.players.updateRegion(this.playerMob);
                  if (!this.isDead() && this.hasSpawned) {
                     this.playerMob.tickMovement(delta);
                  }
               }
            } else {
               if (client.slot == this.slot) {
                  long timeSinceLastRequest = System.currentTimeMillis() - this.lastRequestLevelDataTime;
                  if (timeSinceLastRequest >= 1000L) {
                     GameLog.warn.println("Unexpected level for local player, sending packet request for level change...");
                     client.network.sendPacket(new PacketRequestPacket(PacketRequestPacket.RequestType.LEVEL_CHANGE));
                     this.lastRequestLevelDataTime = System.currentTimeMillis();
                  }
               }

               this.playerMob.setLevel(null);
               this.playerMob.updateRegion(null, null, null);
            }
         } else {
            this.playerMob.setLevel(null);
            this.playerMob.updateRegion(null, null, null);
         }
      }
   }

   public void tick(Client client) {
      if (this.playerMob != null && this.loadedPlayer && this.isSamePlace(client.getLevel())) {
         this.playerMob.setLevel(client.getLevel());
         synchronized (this.playerMob.getLevel().entityManager.lock) {
            if (this.hasSpawned) {
               if (!this.isDead()) {
                  this.playerMob.clientTick();
               }

               this.playerMob.tickSync();
            }
         }
      }
   }

   public void die(int respawnTime) {
      if (!this.isDead()) {
         this.playerMob.remove(0.0F, 0.0F, null, true);
         this.isDead = true;
         if (this.client.getSlot() == this.slot) {
            this.client.isDead = true;
            this.client.respawnTime = this.client.worldEntity == null ? respawnTime : this.client.worldEntity.getTime() + respawnTime;
            this.client.closeContainer(false);
         }
      }
   }

   public void respawn(PacketPlayerRespawn packet) {
      if (!this.isSamePlace(packet.levelIdentifier)) {
         this.hasSpawned = false;
      }

      this.setLevelIdentifier(packet.levelIdentifier);
      if (this.playerMob != null) {
         this.playerMob.setHealth(Math.max(this.playerMob.getMaxHealth() / 2, 1));
         this.playerMob.hungerLevel = Math.max(0.5F, this.playerMob.hungerLevel);
         this.playerMob.setPos(packet.playerX, packet.playerY, true);
         this.playerMob.dx = 0.0F;
         this.playerMob.dy = 0.0F;
         this.playerMob.restore();
      }

      this.isDead = false;
   }

   public void applyLevelChangePacket(PacketPlayerLevelChange packet) {
      this.setLevelIdentifier(packet.identifier);
      if (this.playerMob != null) {
         if (!packet.mountFollow) {
            this.playerMob.dismount();
         }

         this.playerMob.onLevelChanged();
      }

      this.hasSpawned = false;
   }

   public void applyInventoryPacket(PacketPlayerInventory packet) {
      if (this.loadedPlayer) {
         this.playerMob.applyInventoryPacket(packet);
      }
   }

   public void applyInventoryPartPacket(PacketPlayerInventoryPart packet) {
      if (this.loadedPlayer) {
         this.playerMob.getInv().applyInventoryPartPacket(packet);
      }
   }

   public void applyAppearancePacket(PacketPlayerAppearance packet) {
      if (this.loadedPlayer) {
         this.playerMob.applyAppearancePacket(packet);
      }
   }

   public void applyGeneralPacket(PacketPlayerGeneral packet) {
      this.setLevelIdentifier(packet.levelIdentifier);
      Level level = this.client.getLevel();
      if (!this.isSamePlace(level)) {
         level = null;
      }

      if (this.playerMob == null) {
         this.playerMob = new PlayerMob(this.slot, this);
         if (this.client.getSlot() == this.slot) {
            this.playerMob.staySmoothSnapped = true;
         }
      }

      this.playerMob.setLevel(level);
      this.playerMob.setWorldData(this.client.worldEntity, this.client.worldSettings);
      this.playerMob.applySpawnPacket(new PacketReader(packet.playerSpawnContent));
      this.pvpEnabled = packet.pvpEnabled;
      if (packet.hasSpawned) {
         this.applySpawned(packet.remainingSpawnInvincibilityTime);
      } else {
         this.hasSpawned = false;
      }

      this.setTeamID(packet.team);
      this.playerMob.playerName = packet.name;
      this.playerMob.setUniqueID(this.slot);
      if (!this.isDead) {
         this.playerMob.restore();
      }

      this.playerMob.init();
      if (packet.isDead) {
         this.die(packet.remainingRespawnTime);
      } else {
         this.isDead = false;
      }

      this.relationshipData.readPacket(new PacketReader(packet.relationshipData));
      this.loadedPlayer = true;
      this.client.loading.playersPhase.submitLoadedPlayer(this.slot);
      this.applyClientInstalledDLCPacket(packet.installedDLC);
   }

   public void applyClientInstalledDLCPacket(PacketClientInstalledDLC packet) {
      for (int i = 0; i < packet.installedDLC.length; i++) {
         int id = packet.installedDLC[i];
         this.installedDLC.put(id, DLC.DLCs.get(id));
      }
   }

   public void applyPacketPlayerBuffs(PacketPlayerBuffs packet) {
      if (this.loadedPlayer) {
         packet.apply(this.playerMob);
         this.playerMob.equipmentBuffManager.updateAll();
      }
   }

   public void resetLoaded() {
      this.loadedPlayer = false;
   }

   public void applySpawned(int remainingInvincibilityTime) {
      this.hasSpawned = true;
      if (this.playerMob != null) {
         this.playerMob.refreshSpawnTime(remainingInvincibilityTime);
         this.playerMob.updateMount();
         this.playerMob.updateRider();
      }
   }

   @Override
   public boolean pvpEnabled() {
      return this.client.worldSettings.forcedPvP || this.pvpEnabled;
   }

   @Override
   public String getName() {
      return this.playerMob == null ? "N/A" : this.playerMob.getDisplayName();
   }

   public void setLevelIdentifier(LevelIdentifier identifier) {
      if ((this.levelIdentifier == null || !this.levelIdentifier.equals(identifier)) && this.playerMob != null) {
         this.playerMob.boomerangs.clear();
         this.playerMob.toolHits.clear();
      }

      this.levelIdentifier = identifier;
   }

   @Override
   public LevelIdentifier getLevelIdentifier() {
      return this.levelIdentifier;
   }

   public Client getClient() {
      return this.client;
   }

   public boolean isLocalClient() {
      return this.slot == this.client.getSlot();
   }
}
