package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.TileRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.level.maps.levelData.OneWorldNPCVillageData;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;
import necesse.level.maps.liquidManager.ClosestHeightResult;
import necesse.level.maps.liquidManager.LiquidManager;

public class VisitLocationRomanceActivity extends RomanceActivity {
   public VisitLocationRomanceActivity.SpecificLocation specificLocation;
   protected long goodLocationChatCooldown;
   public boolean locationApproved;

   public VisitLocationRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public VisitLocationRomanceActivity(PlayerRomanceManager manager, VisitLocationRomanceActivity.SpecificLocation specificLocation) {
      this(manager);
      this.goodLocationChatCooldown = 0L;
      this.specificLocation = specificLocation;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addEnum("specificLocation", this.specificLocation);
      save.addLong("goodLocationChatCooldown", this.goodLocationChatCooldown);
      save.addBoolean("locationApproved", this.locationApproved);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.specificLocation = save.getEnum(VisitLocationRomanceActivity.SpecificLocation.class, "specificLocation", null, false);
      if (this.specificLocation == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " specificsToCheckFor was null on load.");
      }

      this.goodLocationChatCooldown = save.getLong("goodLocationChatCooldown", 0L);
      this.locationApproved = save.getBoolean("locationApproved", false);
   }

   @Override
   public void serverTickInAdventureParty(ServerClient client) {
      super.serverTickInAdventureParty(client);
      if (this.hasInitializedActivity && !this.activityCompleted) {
         this.locationApproved = false;
         PlayerMob playerMob = client.playerMob;
         if (playerMob != null) {
            int playerTileID = playerMob.getLevel().getTileID(playerMob.getTileX(), playerMob.getTileY());
            if (this.specificLocation == VisitLocationRomanceActivity.SpecificLocation.NEAR_WATER) {
               this.lookForNearbyWater(client, playerTileID, playerMob);
            } else if (this.specificLocation == VisitLocationRomanceActivity.SpecificLocation.VILLAGE) {
               this.checkIfInVillage(client, playerMob);
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

   private void goodLocationFound(ServerClient client, PlayerMob playerMob) {
      this.locationApproved = true;
      if (!this.activityCompleted && playerMob.getTime() > this.goodLocationChatCooldown) {
         this.goodLocationChatCooldown = playerMob.getTime() + 60000L;
         RomanceSettlerDialogue dialogue = this.getDialogue(client);
         this.getMob().startExpressionAbility.runAndSend(FormExpressionWheel.Expression.BLUSH, (dialogue.settlerChatsAtCustomMoment.size() + 1) * 4 * 1000);
         dialogue.startYapping(client, dialogue.settlerChatsAtCustomMoment, 4.0F, false, null);
      }
   }

   @Override
   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return this.getMob().getRomancePersonality().visitLocationRomanceActivityDialogue(client, this.specificLocation);
   }

   public enum SpecificLocation {
      NEAR_WATER,
      VILLAGE;
   }
}
