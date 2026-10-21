package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.TileRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.PlayerMob;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.levelData.OneWorldNPCVillageData;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;
import necesse.level.maps.liquidManager.ClosestHeightResult;
import necesse.level.maps.liquidManager.LiquidManager;

public class VisitBiomeAndLocationRomanceActivity extends VisitBiomeRomanceActivity {
   public VisitBiomeAndLocationRomanceActivity.SpecificLocation specificLocation;

   public VisitBiomeAndLocationRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public VisitBiomeAndLocationRomanceActivity(
      PlayerRomanceManager manager, String biomeID, VisitBiomeAndLocationRomanceActivity.SpecificLocation specificLocation
   ) {
      super(manager, biomeID);
      this.specificLocation = specificLocation;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addEnum("specificsToCheckFor", this.specificLocation);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.specificLocation = save.getEnum(VisitBiomeAndLocationRomanceActivity.SpecificLocation.class, "specificsToCheckFor", null, false);
      if (this.specificLocation == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " specificsToCheckFor was null on load.");
      }
   }

   @Override
   public void serverTickInAdventureParty(ServerClient client) {
      if (this.hasInitializedActivity && !this.activityCompleted) {
         this.locationApproved = false;
         PlayerMob playerMob = client.playerMob;
         if (playerMob.getLevel() != null) {
            Biome currentBiome = playerMob.getLevel().getBiome(playerMob.getTileX(), playerMob.getTileY());
            if (this.biomeStringID.equals(currentBiome.getStringID())) {
               switch (this.specificLocation) {
                  case NEAR_WATER:
                     int playerTileID = playerMob.getLevel().getTileID(playerMob.getTileX(), playerMob.getTileY());
                     this.lookForNearbyWater(client, playerTileID, playerMob);
                     break;
                  case VILLAGE:
                     this.checkIfInVillage(client, playerMob);
                     break;
                  default:
                     this.goodLocationFound(client, playerMob);
               }
            }

            if (!this.activityCompleted && this.locationApproved && playerMob.isSittingDown() && this.getMob().isSittingDown()) {
               this.activityCompleted = true;
               RomanceSettlerDialogue dialogue = this.getDialogue(client);
               dialogue.startYapping(client, dialogue.settlerChatsOnCompletingActivity, 4.0F, false, null);
            }
         }
      }
   }

   private void lookForNearbyWater(ServerClient client, int playerTileID, PlayerMob playerMob) {
      if (playerTileID != TileRegistry.waterID) {
         LiquidManager liquidManager = playerMob.getLevel().liquidManager;
         if (liquidManager.getHeight(playerMob.getTileX(), playerMob.getTileY()) < 10) {
            ClosestHeightResult closestHeightTile = liquidManager.findClosestHeightTile(playerMob.getTileX(), playerMob.getTileY(), -1, 1, tile -> true);
            if (closestHeightTile.found != null) {
               int closestTileID = playerMob.getLevel().getTileID(closestHeightTile.found.x, closestHeightTile.found.y);
               if (closestTileID == TileRegistry.waterID) {
                  this.goodLocationFound(client, playerMob);
               }
            }
         }
      }
   }

   private void checkIfInVillage(ServerClient client, PlayerMob playerMob) {
      OneWorldNPCVillageData villageData = OneWorldNPCVillageData.getVillageData(client.getLevel(), false);
      if (villageData != null && villageData.isVillageTile(playerMob.getTileX(), playerMob.getTileY())) {
         this.goodLocationFound(client, playerMob);
      }
   }

   @Override
   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return this.getMob().getRomancePersonality().visitBiomeAndLocationRomanceActivityDialogue(client, this.biomeStringID, this.specificLocation);
   }

   public enum SpecificLocation {
      NEAR_WATER,
      VILLAGE;
   }
}
