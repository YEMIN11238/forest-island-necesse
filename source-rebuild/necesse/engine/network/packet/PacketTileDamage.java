package necesse.engine.network.packet;

import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.entity.DamagedObjectEntity;
import necesse.entity.TileDamageResult;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;

public class PacketTileDamage extends Packet {
   public final int levelIdentifierHashCode;
   public final int tileX;
   public final int tileY;
   public final int expectedTileID;
   public final int totalDamage;
   public final int addedDamage;
   public final boolean hasExtendedRecoveryStartTime;
   public final boolean destroyed;
   public final boolean showEffect;
   public final int mouseX;
   public final int mouseY;

   public PacketTileDamage(byte[] data) {
      super(data);
      PacketReader reader = new PacketReader(this);
      this.levelIdentifierHashCode = reader.getNextInt();
      this.tileX = reader.getNextInt();
      this.tileY = reader.getNextInt();
      this.expectedTileID = reader.getNextInt();
      this.totalDamage = reader.getNextInt();
      this.addedDamage = reader.getNextInt();
      this.destroyed = reader.getNextBoolean();
      this.hasExtendedRecoveryStartTime = reader.getNextBoolean();
      this.showEffect = reader.getNextBoolean();
      this.mouseX = reader.getNextInt();
      this.mouseY = reader.getNextInt();
   }

   public PacketTileDamage(TileDamageResult damageResult) {
      this(damageResult, damageResult.showEffects);
   }

   public PacketTileDamage(TileDamageResult damageResult, boolean showEffect) {
      this(
         damageResult.damagedObjectEntity.getLevel(),
         damageResult.getTileX(),
         damageResult.getTileY(),
         damageResult.levelTile.tile.getID(),
         damageResult.totalDamage,
         damageResult.addedDamage,
         damageResult.destroyed,
         damageResult.damagedObjectEntity.hasExtendedRecoveryStartTime,
         showEffect,
         damageResult.mouseX,
         damageResult.mouseY
      );
   }

   public PacketTileDamage(
      Level level,
      int tileX,
      int tileY,
      int tileID,
      int totalDamage,
      int addedDamage,
      boolean destroyed,
      boolean hasExtendedRecoveryStartTime,
      boolean showEffect,
      int mouseX,
      int mouseY
   ) {
      this.levelIdentifierHashCode = level.getIdentifierHashCode();
      this.tileX = tileX;
      this.tileY = tileY;
      this.expectedTileID = tileID;
      this.totalDamage = totalDamage;
      this.addedDamage = addedDamage;
      this.destroyed = destroyed;
      this.hasExtendedRecoveryStartTime = hasExtendedRecoveryStartTime;
      this.showEffect = showEffect;
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      PacketWriter writer = new PacketWriter(this);
      writer.putNextInt(this.levelIdentifierHashCode);
      writer.putNextInt(tileX);
      writer.putNextInt(tileY);
      writer.putNextInt(this.expectedTileID);
      writer.putNextInt(totalDamage);
      writer.putNextInt(addedDamage);
      writer.putNextBoolean(destroyed);
      writer.putNextBoolean(hasExtendedRecoveryStartTime);
      writer.putNextBoolean(showEffect);
      writer.putNextInt(mouseX);
      writer.putNextInt(mouseY);
   }

   @Override
   public void processClient(NetworkPacket packet, Client client) {
      if (client.levelManager.checkIfLoadedRegionAtTile(this.levelIdentifierHashCode, this.tileX, this.tileY, true)) {
         Level level = client.getLevel();
         GameTile tile = level.getTile(this.tileX, this.tileY);
         if (this.expectedTileID == -1 || tile.getID() == this.expectedTileID) {
            DamagedObjectEntity damagedEntity = level.entityManager.getOrCreateDamagedObjectEntity(this.tileX, this.tileY);
            damagedEntity.updateTileDamage(this.totalDamage, this.destroyed);
            damagedEntity.hasExtendedRecoveryStartTime = this.hasExtendedRecoveryStartTime;
            tile.onDamaged(level, this.tileX, this.tileY, this.addedDamage, null, null, this.showEffect, this.mouseX, this.mouseY);
         }
      }
   }
}
