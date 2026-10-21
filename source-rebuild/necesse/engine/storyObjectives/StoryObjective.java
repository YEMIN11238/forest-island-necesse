package necesse.engine.storyObjectives;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import necesse.engine.Settings;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.playerStats.PlayerStats;
import necesse.engine.registries.IDData;
import necesse.engine.registries.IDDataContainer;
import necesse.engine.registries.StoryObjectiveRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.state.MainGame;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.world.worldData.SettlementsWorldData;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.DrawOptionsList;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormStoryObjectiveComponent;
import necesse.gfx.gameFont.FontOptions;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.levelData.settlementData.CachedSettlementData;
import necesse.level.maps.levelData.settlementData.ServerSettlementData;

public abstract class StoryObjective implements IDDataContainer {
   public static final Color OBJECTIVE_TEXT_COLOR = new Color(200, 50, 50);
   public static final int OBJECTIVE_TEXT_MAX_WIDTH = 200;
   public static final FontOptions OBJECTIVE_TEXT_OPTIONS = new FontOptions(16).outline();
   public final IDData idData = new IDData();
   public final StoryObjectiveManager manager;
   private boolean isCompleted;
   private boolean isClaimed;

   @Override
   public IDData getIDData() {
      return this.idData;
   }

   public StoryObjective(StoryObjectiveManager manager) {
      this.manager = manager;
   }

   public void addSaveData(SaveData save) {
   }

   public void applyLoadData(LoadData save) {
   }

   public void markLoadDataCompleted(boolean isClaimed) {
      this.isCompleted = true;
      this.isClaimed = isClaimed;
   }

   public void writePacket(PacketWriter writer) {
      writer.putNextBoolean(this.isCompleted);
      if (this.isCompleted) {
         writer.putNextBoolean(this.isClaimed);
      }
   }

   public void readPacket(PacketReader reader) {
      boolean wasCompleted = this.isCompleted;
      this.isCompleted = reader.getNextBoolean();
      if (this.isCompleted) {
         this.isClaimed = reader.getNextBoolean();
      } else {
         this.isClaimed = false;
      }

      if (!wasCompleted && this.isCompleted && this.manager.isClient()) {
         this.onCompletedClient();
      }
   }

   public void markDirty() {
      this.manager.markObjectiveDirty(this);
   }

   public void serverTickCurrentObjective() {
   }

   public void clientTickCurrentObjective() {
   }

   public DrawOptions getCurrentObjectiveLevelHudDrawOptions(GameCamera camera, PlayerMob perspective) {
      return null;
   }

   public void drawCurrentObjectiveOverForms(MainGame mainGame, PlayerMob perspective) {
   }

   public void markCompleted() {
      if (!this.manager.isServer()) {
         throw new IllegalStateException("Cannot complete story objectives from the client");
      }

      if (!this.isCompleted) {
         this.isCompleted = true;
         this.markDirty();
         this.manager.updateCurrentAndClaimableObjectives();
         this.manager.updateObjectiveListeners();
         if (this.manager.isServer()) {
            ServerClient client = this.manager.getServerClient();
            SettlementsWorldData worldData = SettlementsWorldData.getSettlementsData(client.getServer());

            for (CachedSettlementData cache : worldData.collectCachedSettlements(
               data -> data.getOwnerAuth() == client.authentication || client.isSameTeam(data.getTeamID())
            )) {
               ServerSettlementData settlement = worldData.getServerData(cache.uniqueID);
               if (settlement != null) {
                  settlement.storyObjectives.onTeamMemberCompletedStoryObjective(this);
               }
            }
         }
      }
   }

   public void onCompletedClient() {
      this.manager.hideStoryObjectives = false;
   }

   public void claimRewards(ServerSettlementData serverSettlementData) {
      if (!this.isCompleted) {
         throw new IllegalStateException("Cannot claim story objective rewards before completing it");
      }

      if (this.isClaimed) {
         throw new IllegalStateException("Cannot claim story objective rewards more than once");
      }

      if (this.manager.isServer()) {
         LootTable rewards = this.getRewards();
         if (rewards != null) {
            ArrayList<InventoryItem> rewardItems = new ArrayList<>();
            rewards.addItems(rewardItems, GameRandom.globalRandom, 1.0F, this.manager.getServerClient(), serverSettlementData);

            for (InventoryItem rewardItem : rewardItems) {
               this.manager
                  .getServerClient()
                  .playerMob
                  .getInv()
                  .addItemsDropRemaining(rewardItem, "reward", this.manager.getServerClient().playerMob, true, true, false);
            }
         }
      }

      this.isClaimed = true;
      this.manager.updateCurrentAndClaimableObjectives();
      this.manager.updateObjectiveListeners();
      if (this.manager.isServer()) {
         this.markDirty();
      } else {
         this.manager.updateFormComponents();
      }
   }

   public void debugMarkCompletedAndClaimed() {
      this.isCompleted = true;
      this.isClaimed = true;
   }

   public boolean isCurrentObjective() {
      return this.manager.getCurrentObjective() == this;
   }

   public boolean isCompleted() {
      return this.isCompleted;
   }

   public boolean isClaimed() {
      return this.isClaimed;
   }

   public abstract LootTable getRewards();

   public abstract void setupFormComponent(FormStoryObjectiveComponent var1, FormFlow var2);

   public final void migrateFromCharacterStats(Server server, PlayerStats characterStats) {
      this.migrateFromCharacterStats(server, characterStats, () -> this.isCompleted = true, () -> this.isClaimed = true);
   }

   protected void migrateFromCharacterStats(Server server, PlayerStats characterStats, Runnable markComplete, Runnable markClaimed) {
   }

   public int getSortedIndex() {
      return StoryObjectiveRegistry.getSortedIndex(this.getID());
   }

   public int getMajorObjectiveSortedIndex() {
      return StoryObjectiveRegistry.getMajorObjectiveIndex(this.getID());
   }

   public boolean isMajorObjective() {
      return StoryObjectiveRegistry.isMajorObjective(this.getID());
   }

   public boolean shouldAddListenersIfNotCurrent() {
      return this.isMajorObjective();
   }

   public PlayerMob getPlayer() {
      return this.manager.isClient() ? this.manager.getClient().getPlayer() : this.manager.getServerClient().playerMob;
   }

   public static FairTypeDrawOptions getTextDrawOptions(FairType type) {
      return type.getDrawOptions(FairType.TextAlign.CENTER, 200, false, true);
   }

   public static FairType getObjectiveText(String message) {
      return new FairType()
         .append(OBJECTIVE_TEXT_OPTIONS, message)
         .applyParsers(
            TypeParsers.GAME_COLOR,
            TypeParsers.REMOVE_URL,
            TypeParsers.URL_OPEN,
            TypeParsers.GameEmote(OBJECTIVE_TEXT_OPTIONS.getSize()),
            TypeParsers.ItemIcon(OBJECTIVE_TEXT_OPTIONS.getSize()),
            TypeParsers.MobIcon(OBJECTIVE_TEXT_OPTIONS.getSize()),
            TypeParsers.InputIcon(OBJECTIVE_TEXT_OPTIONS)
         );
   }

   public static DrawOptions getLevelTextDrawOptions(GameMessage text, int levelX, int levelY, GameCamera camera, PlayerMob perspective, boolean drawArrow) {
      return getLevelTextDrawOptions(getTextDrawOptions(getObjectiveText(text.translate())), levelX, levelY, camera, perspective, drawArrow);
   }

   public static DrawOptions getLevelTextDrawOptions(
      FairTypeDrawOptions text, int levelX, int levelY, GameCamera camera, PlayerMob perspective, boolean drawArrow
   ) {
      int drawX = camera.getDrawX(levelX);
      int drawY = camera.getDrawY(levelY);
      int distance = -1;
      Rectangle camBounds = camera.getBounds();
      camBounds.x += 25;
      camBounds.y += 25;
      camBounds.width -= 50;
      camBounds.width -= 50;
      if (!camBounds.contains(levelX, levelY)) {
         int screenSize = Math.min(camera.getWidth(), camera.getHeight());
         int widthHalf = camera.getWidth() / 2;
         int heightHalf = camera.getHeight() / 2;
         Float dir = GameMath.normalize(levelX - (camera.getX() + widthHalf), levelY - (camera.getY() + heightHalf));
         drawX = widthHalf + (int)(dir.x * screenSize / 3.0F);
         drawY = heightHalf + (int)(dir.y * screenSize / 3.0F);
         distance = (int)perspective.getDistance(levelX, levelY);
      }

      FairTypeDrawOptions subtitle = null;
      if (distance != -1) {
         subtitle = new FairType()
            .append(OBJECTIVE_TEXT_OPTIONS, "\n(" + (int)GameMath.pixelsToMeters(distance) + "m)")
            .getDrawOptions(FairType.TextAlign.CENTER, 200, false, true);
      }

      return getTextDrawOptions(text, subtitle, drawX, drawY, distance == -1 && drawArrow);
   }

   public static DrawOptions getTextDrawOptions(FairTypeDrawOptions text, FairTypeDrawOptions subtitle, int drawX, int drawY, boolean drawArrow) {
      DrawOptionsList list = new DrawOptionsList();
      list.add(() -> {
         int y = drawY - (drawArrow ? 10 : 0);
         if (subtitle != null) {
            y -= subtitle.getBoundingBox().height;
            subtitle.draw(drawX, y, OBJECTIVE_TEXT_COLOR);
         }

         if (text != null) {
            y -= text.getBoundingBox().height;
            text.draw(drawX, y, OBJECTIVE_TEXT_COLOR);
         }
      });
      if (drawArrow) {
         list.add(Settings.UI.tutorial_arrow.initDraw().pos(drawX - Settings.UI.tutorial_arrow.getWidth() / 2, drawY - Settings.UI.tutorial_arrow.getHeight()));
      }

      return list;
   }

   public static DrawOptions getTextDrawOptions(
      FairTypeDrawOptions text, FairTypeDrawOptions subtitle, FormComponent component, boolean acceptScroll, int xOffset, int yOffset, boolean drawArrow
   ) {
      Point screenPosition = component.getScreenPosition(acceptScroll);
      return screenPosition == null ? () -> {} : getTextDrawOptions(text, subtitle, screenPosition.x + xOffset, screenPosition.y + yOffset, drawArrow);
   }

   public static DrawOptions getTextDrawOptions(
      FairTypeDrawOptions text, FairTypeDrawOptions subtitle, FormComponent component, int xOffset, int yOffset, boolean drawArrow
   ) {
      return getTextDrawOptions(text, subtitle, component, false, xOffset, yOffset, drawArrow);
   }
}
