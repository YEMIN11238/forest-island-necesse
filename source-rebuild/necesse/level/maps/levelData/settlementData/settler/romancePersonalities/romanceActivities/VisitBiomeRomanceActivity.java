package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import java.util.Objects;
import necesse.engine.network.server.ServerClient;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public class VisitBiomeRomanceActivity extends RomanceActivity {
   public String biomeStringID;
   public boolean locationApproved;
   protected long goodLocationChatCooldown;

   public VisitBiomeRomanceActivity(PlayerRomanceManager manager) {
      super(manager);
   }

   public VisitBiomeRomanceActivity(PlayerRomanceManager manager, String biomeStringID) {
      this(manager);
      Objects.requireNonNull(biomeStringID);
      this.biomeStringID = biomeStringID;
      this.goodLocationChatCooldown = 0L;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addBiomeStringID("biomeStringID", this.biomeStringID);
      save.addLong("goodLocationChatCooldown", this.goodLocationChatCooldown);
      save.addBoolean("locationApproved", this.locationApproved);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.biomeStringID = save.getBiomeStringID("biomeStringID", null, false);
      if (this.biomeStringID == null) {
         throw new LoadDataException("Romance Activity: " + this.getStringID() + " biomeStringID was null on load.");
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
         if (playerMob.getLevel() != null) {
            Biome currentBiome = playerMob.getLevel().getBiome(playerMob.getTileX(), playerMob.getTileY());
            if (this.biomeStringID.equals(currentBiome.getStringID())) {
               this.goodLocationFound(client, playerMob);
            }

            if (!this.activityCompleted && this.locationApproved && playerMob.isSittingDown() && this.getMob().isSittingDown()) {
               this.activityCompleted = true;
               RomanceSettlerDialogue dialogue = this.getDialogue(client);
               dialogue.startYapping(client, dialogue.settlerChatsOnCompletingActivity, 4.0F, false, null);
            }
         }
      }
   }

   public void goodLocationFound(ServerClient client, PlayerMob playerMob) {
      this.locationApproved = true;
      if (!this.activityCompleted && playerMob.getTime() > this.goodLocationChatCooldown) {
         this.goodLocationChatCooldown = playerMob.getTime() + 60000L;
         RomanceSettlerDialogue dialogue = this.getDialogue(client);
         this.getMob().startExpressionAbility.runAndSend(FormExpressionWheel.Expression.BLUSH, 8000);
         dialogue.startYapping(client, dialogue.settlerChatsAtCustomMoment, 4.0F, false, null);
      }
   }

   @Override
   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return this.getMob().getRomancePersonality().visitBiomeRomanceActivityDialogue(client, this.biomeStringID);
   }
}
