package necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities;

import java.util.ArrayList;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.IDData;
import necesse.engine.registries.IDDataContainer;
import necesse.engine.registries.RomanceActivitiesRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobWasKilledEvent;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.inventory.recipe.Recipe;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.levelData.settlementData.settler.dialogues.RomanceSettlerDialogue;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;

public abstract class RomanceActivity implements IDDataContainer {
   public final IDData idData = new IDData();
   public final PlayerRomanceManager manager;
   public boolean activityCompleted;
   public boolean hasInitializedActivity;

   @Override
   public IDData getIDData() {
      return this.idData;
   }

   public RomanceActivity(PlayerRomanceManager manager) {
      RomanceActivitiesRegistry.instance.applyIDData((Class<? extends RomanceActivity>)this.getClass(), this.idData);
      this.manager = manager;
   }

   public void addSaveData(SaveData save) {
      save.addBoolean("activityCompleted", this.activityCompleted);
      save.addBoolean("hasInitializedActivity", this.hasInitializedActivity);
   }

   public void applyLoadData(LoadData save) {
      this.activityCompleted = save.getBoolean("activityCompleted", false);
      this.hasInitializedActivity = save.getBoolean("hasInitializedActivity", false);
   }

   public HumanMob getMob() {
      return this.manager.romancePersonality.mob;
   }

   public void setInitializedActivity(boolean hasInitializedActivity) {
      this.hasInitializedActivity = hasInitializedActivity;
   }

   public ArrayList<GameMessage> getActivityMessage(ServerClient client, PlayerRomanceManager manager) {
      HumanMob settlerMob = this.getMob();
      if (!manager.isActivityOnCooldown() && this.hasInitializedActivity) {
         ArrayList<GameMessage> activityTipMessage = this.getDialogue(client).activityTipMessages;
         if (client.adventureParty.contains(settlerMob) && !this.activityCompleted && activityTipMessage != null) {
            return activityTipMessage;
         }
      }

      return null;
   }

   public void serverTickInAdventureParty(ServerClient client) {
   }

   public void onMobKilled(ServerClient client, MobWasKilledEvent event) {
   }

   public void onAnimalCaptured(ServerClient client, Mob animalCaptured) {
   }

   public void onCritterNetted(ServerClient client, Mob critterNetted) {
   }

   public void onAnimalReleased(ServerClient client, Mob animalReleased) {
   }

   public void onCraftedRecipe(ServerClient client, Recipe recipe, int amount) {
   }

   public void onObjectPlaced(ServerClient client, GameObject object, int tileX, int tileY, int rotation) {
   }

   public RomanceSettlerDialogue getDialogue(ServerClient client) {
      return null;
   }
}
