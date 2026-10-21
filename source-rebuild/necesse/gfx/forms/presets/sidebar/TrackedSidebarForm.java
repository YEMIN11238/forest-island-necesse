package necesse.gfx.forms.presets.sidebar;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import necesse.engine.GameCache;
import necesse.engine.GlobalData;
import necesse.engine.achievements.Achievement;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.quest.Quest;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.state.MainGame;
import necesse.engine.state.State;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.MainGameFormManager;
import necesse.gfx.forms.components.FormAchievementTrackedComponent;
import necesse.gfx.forms.components.FormContentVarToggleButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormQuestTrackedComponent;
import necesse.gfx.forms.components.FormStoryObjectiveComponent;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.components.localComponents.FormLocalTextButton;
import necesse.gfx.forms.presets.PauseMenuForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.ButtonIcon;

public class TrackedSidebarForm extends SidebarForm {
   private static final LinkedList<TrackedSidebarForm> forms = new LinkedList<>();
   private static final LinkedList<Integer> trackedQuests = new LinkedList<>();
   private static final LinkedList<Achievement> trackedAchievements = new LinkedList<>();
   private final MainGameFormManager mainFormManager;
   private Client client;
   private final LinkedList<Quest> quests = new LinkedList<>();

   @Deprecated
   public static void removeTrackedQuest(Client client, Quest quest) {
      removeTrackedQuest(client, quest.getUniqueID());
   }

   @Deprecated
   public static void removeTrackedQuest(Client client, int questUniqueID) {
      trackedQuests.remove(Integer.valueOf(questUniqueID));
      updateTrackedList();
      saveTrackedQuests(client);
   }

   public static boolean isCachedQuestTracked(Quest quest) {
      return trackedQuests.contains(quest.getUniqueID());
   }

   public static void removeCachedTrackedQuest(Client client, int questUniqueID) {
      trackedQuests.remove(Integer.valueOf(questUniqueID));
      saveTrackedQuests(client);
   }

   public static void addTrackedAchievement(Achievement achievement) {
      if (!trackedAchievements.contains(achievement)) {
         trackedAchievements.add(achievement);
         updateTrackedList();
         saveTrackedAchievements();
      }
   }

   public static void removeTrackedAchievement(Achievement achievement) {
      trackedAchievements.remove(achievement);
      updateTrackedList();
      saveTrackedAchievements();
   }

   public static boolean isAchievementTracked(Achievement achievement) {
      return trackedAchievements.contains(achievement);
   }

   public static void loadTrackedQuests(Client client) {
      trackedQuests.clear();
      LoadData questsSave = GameCache.getSave(client.loading.getClientCachePath("TrackedQuests"));
      if (questsSave != null) {
         for (int questID : questsSave.getIntArray("quests", new int[0])) {
            trackedQuests.add(questID);
         }

         updateTrackedList();
      }
   }

   private static void saveTrackedQuests(Client client) {
      trackedQuests.removeIf(i -> client.quests.getQuest(i) == null);
      SaveData questsSave = new SaveData("TrackedQuests");
      questsSave.addIntArray("quests", trackedQuests.stream().mapToInt(i -> i).toArray());
      GameCache.cacheSave(questsSave, client.loading.getClientCachePath("TrackedQuests"));
   }

   public static void loadTrackedAchievements() {
      LoadData achievementsSave = GameCache.getSave("trackedachievements");
      if (achievementsSave != null) {
         for (String achievementStringID : achievementsSave.getStringArray("achievements", new String[0])) {
            GlobalData.achievements()
               .getAchievements()
               .stream()
               .filter(a -> a.stringID.equals(achievementStringID))
               .findFirst()
               .ifPresent(a -> trackedAchievements.add(a));
         }
      }
   }

   private static void saveTrackedAchievements() {
      SaveData achievementsSave = new SaveData("TrackedAchievements");
      achievementsSave.addStringArray("achievements", trackedAchievements.stream().map(a -> a.stringID).toArray(String[]::new));
      GameCache.cacheSave(achievementsSave, "trackedachievements");
   }

   public static void updateTrackedList() {
      forms.forEach(f -> f.updateList(true));
   }

   public TrackedSidebarForm(MainGameFormManager mainFormManager) {
      super("trackedquests", mainFormManager.getSidebarWidth(), 0);
      this.mainFormManager = mainFormManager;
      this.drawBase = false;
   }

   @Override
   public void onSidebarUpdate(int x, int y) {
      super.onSidebarUpdate(x, y);
      this.updateList(false);
   }

   public void updateList(boolean updateSidebar) {
      this.quests.clear();
      ArrayList<StoryObjective> objectives = this.client.storyManager.getVisibleObjectives();

      for (Integer trackedQuest : trackedQuests.toArray(new Integer[0])) {
         Quest quest = this.client.quests.getQuest(trackedQuest);
         if (quest == null) {
            removeCachedTrackedQuest(this.client, trackedQuest);
            return;
         }

         this.quests.add(quest);
      }

      for (Integer trackedQuest : this.client.trackedQuests.toArray(new Integer[0])) {
         Quest quest = this.client.quests.getQuest(trackedQuest);
         if (quest == null || this.quests.contains(quest)) {
            removeCachedTrackedQuest(this.client, trackedQuest);
            return;
         }

         this.quests.add(quest);
      }

      this.clearComponents();
      this.setWidth(this.mainFormManager.getSidebarWidth());
      FormFlow flow = new FormFlow(0);
      if (!objectives.isEmpty()) {
         int buttonY = flow.next(20);
         FormContentVarToggleButton toggleIcon = this.addComponent(
            new FormContentVarToggleButton(5, buttonY, 24, FormInputSize.SIZE_24, ButtonColor.BASE, () -> this.client.storyManager.hideStoryObjectives) {
               @Override
               protected void drawContent(int x, int y, int width, int height) {
                  ButtonIcon icon = this.isToggled() ? this.getInterfaceStyle().button_collapsed_24 : this.getInterfaceStyle().button_expanded_24;
                  icon.texture.initDraw().color(icon.colorGetter.apply(this.getButtonState())).mirror(false, !this.isToggled()).draw(x - 2, y - 2);
               }
            }
         );
         toggleIcon.onClicked(e -> {
            this.client.storyManager.hideStoryObjectives = !this.client.storyManager.hideStoryObjectives;
            this.client.storyManager.updateFormComponents();
         });
         this.addComponent(new FormLocalLabel("ui", "storyobjectives", new FontOptions(20).outline().color(new Color(220, 220, 220)), -1, 33, buttonY + 4));
         if (!this.client.storyManager.hideStoryObjectives) {
            boolean addDivider = false;

            for (StoryObjective objective : objectives) {
               if (addDivider) {
                  flow.next(10);
               }

               FormStoryObjectiveComponent objectiveComponent = this.addComponent(
                  flow.nextY(new FormStoryObjectiveComponent(Math.min(this.getWidth(), 400), objective, false))
               );
               objectiveComponent.setX(10);
               addDivider = true;
            }
         }

         flow.next(5);
      }

      if (!trackedAchievements.isEmpty()) {
         if (!this.isEmpty()) {
            flow.next(5);
         }

         this.addComponent(new FormLocalLabel("ui", "achievements", new FontOptions(20).outline().color(new Color(220, 220, 220)), -1, 5, flow.next(20)));
         boolean addDivider = false;

         for (Achievement achievement : trackedAchievements) {
            if (addDivider) {
               flow.next(10);
            }

            this.addComponent(flow.nextY(new FormAchievementTrackedComponent(0, 0, this.getWidth(), Math.min(this.getWidth(), 200), achievement)));
            if (achievement == GlobalData.achievements().OBTAIN_ITEMS) {
               this.addComponent(
                     new FormLocalTextButton(
                        new LocalMessage("stats", "show_item_list"), 0, flow.next(24), Math.min(this.getWidth(), 200), FormInputSize.SIZE_20, ButtonColor.BASE
                     ),
                     5
                  )
                  .onClicked(e -> {
                     State state = GlobalData.getCurrentState();
                     if (state instanceof MainGame) {
                        state.setRunning(false);
                        PauseMenuForm pauseMenu = ((MainGame)state).formManager.pauseMenu;
                        pauseMenu.makeCurrent(pauseMenu.stats);
                        pauseMenu.stats.reset();
                        pauseMenu.stats.playerStatsOption.makeCurrent();
                        pauseMenu.stats.playerStats.switcher.makeCurrent(pauseMenu.stats.playerStats.itemsObtained);
                        pauseMenu.stats.playerStats.subMenuBackPressed = () -> pauseMenu.stats.achievementsOption.makeCurrent();
                     }
                  });
            }

            addDivider = true;
         }

         flow.next(5);
      }

      if (!this.quests.isEmpty()) {
         if (!this.isEmpty()) {
            flow.next(5);
         }

         this.addComponent(new FormLocalLabel("ui", "quests", new FontOptions(20).outline().color(new Color(220, 220, 220)), -1, 5, flow.next(20)));
         boolean addDivider = false;

         for (Quest quest : this.quests) {
            if (addDivider) {
               flow.next(10);
            }

            this.addComponent(
               flow.nextY(new FormQuestTrackedComponent(10, 0, Math.min(this.getWidth(), 300), Math.min(this.getWidth(), 200), this.client.getClient(), quest))
            );
            addDivider = true;
         }

         flow.next(5);
      }

      this.setHeight(flow.next());
      if (updateSidebar) {
         this.mainFormManager.fixSidebar();
      }
   }

   @Override
   public boolean shouldSkipRenderBoxCheck() {
      return true;
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      boolean update = this.client.storyManager.shouldUpdateFormComponentsAndReset();
      if (this.quests.stream().anyMatch(Quest::isRemoved)) {
         update = true;
      }

      if (trackedAchievements.removeIf(Achievement::isCompleted)) {
         saveTrackedAchievements();
         update = true;
      }

      if (update) {
         this.updateList(true);
      }

      super.draw(tickManager, perspective, renderBox);
   }

   @Override
   public List<Rectangle> getHitboxes() {
      return super.getHitboxes();
   }

   @Override
   public boolean isMouseOver(InputEvent event) {
      return false;
   }

   @Override
   public boolean isValid(Client client) {
      return true;
   }

   @Override
   public void onAdded(Client client) {
      super.onAdded(client);
      this.client = client;
      this.updateList(true);
      forms.add(this);
   }

   @Override
   public void onRemoved(Client client) {
      super.onRemoved(client);
      forms.remove(this);
   }
}
