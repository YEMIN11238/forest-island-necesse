package necesse.level.maps.levelData.settlementData.settler.dialogues;

import java.awt.Rectangle;
import java.util.ArrayList;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketMobChat;
import necesse.engine.network.server.ServerClient;
import necesse.engine.window.GameWindow;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.ai.behaviourTree.event.StartStaringAIEvent;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormDialogueOption;
import necesse.gfx.forms.components.FormExpressionWheel;
import necesse.gfx.forms.presets.containerComponent.mob.DialogueForm;
import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.inventory.container.customAction.EmptyCustomAction;
import necesse.inventory.container.mob.ShopContainer;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.PlayerRomanceManager;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.SettlerRomancePersonality;
import necesse.level.maps.levelData.settlementData.settler.romancePersonalities.romanceActivities.RomanceActivity;

public class RomanceSettlerDialogue extends SettlerDialogue {
   protected SettlerRomancePersonality romancePersonality;
   protected int cooldownInDaysBeforeNextActivity = -1;
   protected boolean shouldJoinAdventurePartyOnAccept;
   protected boolean hasCompletedActivity;
   protected boolean hasInitializedActivity;
   protected ArrayList<GameMessage> mainText = new ArrayList<>();
   protected GameMessage acceptActivityText;
   protected GameMessage declineActivityText;
   protected EmptyCustomAction clickedAcceptAction;
   protected EmptyCustomAction clickedDeclineAction;
   protected boolean completed;
   protected DialogueForm subForm;
   protected FormDialogueOption startActivityButton;
   protected FormExpressionWheel.Expression customExpressionOnRomanceLevelIncrease;
   public ArrayList<GameMessage> activityTipMessages = new ArrayList<>();
   protected ArrayList<GameMessage> settlerChatsAfterAccepting = new ArrayList<>();
   protected ArrayList<GameMessage> settlerChatsAfterDeclining = new ArrayList<>();
   public ArrayList<GameMessage> settlerChatsOnCompletingActivity = new ArrayList<>();
   public ArrayList<GameMessage> settlerChatsOnDoingActivityWrong = new ArrayList<>();
   public ArrayList<GameMessage> settlerChatsAtCustomMoment = new ArrayList<>();

   public RomanceSettlerDialogue(HumanMob settlerMob) {
      super(settlerMob);
      this.romancePersonality = settlerMob.getRomancePersonality();
   }

   public RomanceSettlerDialogue(HumanMob settlerMob, boolean hasInitializedActivity, boolean shouldJoinAdventurePartyOnAccept) {
      this(settlerMob);
      this.shouldJoinAdventurePartyOnAccept = shouldJoinAdventurePartyOnAccept;
      this.hasInitializedActivity = hasInitializedActivity;
   }

   public RomanceSettlerDialogue(HumanMob settlerMob, boolean hasInitializedActivity, boolean shouldJoinAdventurePartyOnAccept, boolean hasCompletedActivity) {
      this(settlerMob, hasInitializedActivity, shouldJoinAdventurePartyOnAccept);
      this.hasCompletedActivity = hasCompletedActivity;
   }

   public RomanceSettlerDialogue(
      HumanMob settlerMob,
      boolean hasInitializedActivity,
      boolean shouldJoinAdventurePartyOnAccept,
      boolean hasCompletedActivity,
      FormExpressionWheel.Expression customExpressionOnRomanceLevelIncrease
   ) {
      this(settlerMob, hasInitializedActivity, shouldJoinAdventurePartyOnAccept, hasCompletedActivity);
      this.customExpressionOnRomanceLevelIncrease = customExpressionOnRomanceLevelIncrease;
   }

   public RomanceSettlerDialogue addMainTextSection(GameMessage message) {
      this.mainText.add(message);
      return this;
   }

   public RomanceSettlerDialogue setAcceptActivityButtonText(GameMessage message) {
      this.acceptActivityText = message;
      return this;
   }

   public RomanceSettlerDialogue setDeclineActivityButtonText(GameMessage message) {
      this.declineActivityText = message;
      return this;
   }

   public RomanceSettlerDialogue addSettlerChatAfterAccepting(GameMessage message) {
      this.settlerChatsAfterAccepting.add(message);
      return this;
   }

   public RomanceSettlerDialogue addSettlerChatAfterDeclining(GameMessage message) {
      this.settlerChatsAfterDeclining.add(message);
      return this;
   }

   public RomanceSettlerDialogue addSettlerChatOnCompletingActivity(GameMessage message) {
      this.settlerChatsOnCompletingActivity.add(message);
      return this;
   }

   public RomanceSettlerDialogue addSettlerChatOnDoingActivityWrong(GameMessage message) {
      this.settlerChatsOnDoingActivityWrong.add(message);
      return this;
   }

   public RomanceSettlerDialogue addSettlerChatAtCustomMoment(GameMessage message) {
      this.settlerChatsAtCustomMoment.add(message);
      return this;
   }

   public RomanceSettlerDialogue addActivityMessageTip(GameMessage message) {
      this.activityTipMessages.add(message);
      return this;
   }

   public RomanceSettlerDialogue addMainTextSections(String category, String key, int count, Object... replacements) {
      if (count <= 1) {
         this.addMainTextSection(new LocalMessage(category, key, replacements));
      } else {
         for (int i = 0; i < count; i++) {
            this.addMainTextSection(new LocalMessage(category, key + (i + 1), replacements));
         }
      }

      return this;
   }

   public RomanceSettlerDialogue addSettlerChatsAfterAccepting(String category, String key, int count, Object... replacements) {
      if (count <= 1) {
         this.addSettlerChatAfterAccepting(new LocalMessage(category, key, replacements));
      } else {
         for (int i = 0; i < count; i++) {
            this.addSettlerChatAfterAccepting(new LocalMessage(category, key + (i + 1), replacements));
         }
      }

      return this;
   }

   public RomanceSettlerDialogue addSettlerChatsAfterDeclining(String category, String key, int count, Object... replacements) {
      if (count <= 1) {
         this.addSettlerChatAfterDeclining(new LocalMessage(category, key, replacements));
      } else {
         for (int i = 0; i < count; i++) {
            this.addSettlerChatAfterDeclining(new LocalMessage(category, key + (i + 1), replacements));
         }
      }

      return this;
   }

   public RomanceSettlerDialogue addSettlerChatsOnCompletingActivity(String category, String key, int count, Object... replacements) {
      if (count <= 1) {
         this.addSettlerChatOnCompletingActivity(new LocalMessage(category, key, replacements));
      } else {
         for (int i = 0; i < count; i++) {
            this.addSettlerChatOnCompletingActivity(new LocalMessage(category, key + (i + 1), replacements));
         }
      }

      return this;
   }

   public RomanceSettlerDialogue addSettlerChatsAtCustomMoment(String category, String key, int count, Object... replacements) {
      if (count <= 1) {
         this.addSettlerChatAtCustomMoment(new LocalMessage(category, key, replacements));
      } else {
         for (int i = 0; i < count; i++) {
            this.addSettlerChatAtCustomMoment(new LocalMessage(category, key + (i + 1), replacements));
         }
      }

      return this;
   }

   public RomanceSettlerDialogue addActivitiesMessageTip(String category, String key, int count, Object... replacements) {
      if (count <= 1) {
         this.addActivityMessageTip(new LocalMessage(category, key, replacements));
      } else {
         for (int i = 0; i < count; i++) {
            this.addActivityMessageTip(new LocalMessage(category, key + (i + 1), replacements));
         }
      }

      return this;
   }

   public RomanceSettlerDialogue setCooldownInDaysBeforeNextActivity(int cooldownInDays) {
      this.cooldownInDaysBeforeNextActivity = cooldownInDays;
      return this;
   }

   @Override
   protected void setupContainerPacket(PacketWriter writer) {
      super.setupContainerPacket(writer);
      writer.putNextInt(this.cooldownInDaysBeforeNextActivity);
      writer.putNextBoolean(this.shouldJoinAdventurePartyOnAccept);
      writer.putNextBoolean(this.hasCompletedActivity);
      writer.putNextBoolean(this.hasInitializedActivity);
      writer.putNextInt(this.mainText.size());

      for (GameMessage message : this.mainText) {
         message.writePacket(writer);
      }

      this.acceptActivityText.writePacket(writer);
      boolean hasDeclineActivityText = this.declineActivityText != null;
      writer.putNextBoolean(hasDeclineActivityText);
      if (hasDeclineActivityText) {
         this.declineActivityText.writePacket(writer);
      }
   }

   @Override
   protected void applyContainerPacket(PacketReader reader) {
      super.applyContainerPacket(reader);
      this.cooldownInDaysBeforeNextActivity = reader.getNextInt();
      this.shouldJoinAdventurePartyOnAccept = reader.getNextBoolean();
      this.hasCompletedActivity = reader.getNextBoolean();
      this.hasInitializedActivity = reader.getNextBoolean();
      int mainTextCount = reader.getNextInt();

      for (int i = 0; i < mainTextCount; i++) {
         this.mainText.add(GameMessage.fromPacket(reader));
      }

      this.acceptActivityText = GameMessage.fromPacket(reader);
      if (reader.getNextBoolean()) {
         this.declineActivityText = GameMessage.fromPacket(reader);
      }
   }

   @Override
   public void init(final ShopContainer container) {
      super.init(container);
      this.clickedAcceptAction = container.registerAction(new EmptyCustomAction() {
         @Override
         protected void run() {
            RomanceSettlerDialogue.this.onAcceptClicked(container);
         }
      });
      this.clickedDeclineAction = container.registerAction(new EmptyCustomAction() {
         @Override
         protected void run() {
            RomanceSettlerDialogue.this.onDeclineClicked(container);
         }
      });
   }

   public boolean onAcceptClicked(ShopContainer container) {
      if (!container.client.isServer()) {
         return false;
      }

      ServerClient serverClient = container.client.getServerClient();
      PlayerRomanceManager romanceManager = this.romancePersonality.getPlayerManager(serverClient, true);
      RomanceActivity currentRomanceActivity = romanceManager.getCurrentRomanceActivity();
      currentRomanceActivity.setInitializedActivity(true);
      if (currentRomanceActivity.activityCompleted) {
         if (this.cooldownInDaysBeforeNextActivity != -1) {
            romanceManager.increaseRomanceLevel(this.cooldownInDaysBeforeNextActivity);
         } else if (this.customExpressionOnRomanceLevelIncrease != null) {
            romanceManager.increaseRomanceLevelWithCustomExpression(this.customExpressionOnRomanceLevelIncrease);
         } else {
            romanceManager.increaseRomanceLevel();
         }
      }

      if (this.shouldJoinAdventurePartyOnAccept) {
         this.joinAdventureParty(container);
      }

      this.startYapping(serverClient, this.settlerChatsAfterAccepting, 4.0F, true, () -> {
         if (!this.shouldJoinAdventurePartyOnAccept) {
            this.settlerMob.clearCommandsOrders(null);
            this.settlerMob.jobTypeHandler.startGlobalCooldown(this.settlerMob.getTime(), 4000);
         }
      });
      return true;
   }

   public void onDeclineClicked(ShopContainer container) {
      if (container.client.isServer()) {
         this.startYapping(container.client.getServerClient(), this.settlerChatsAfterDeclining, 4.0F, false, null);
         this.settlerMob.startExpressionAbility.runAndSend(FormExpressionWheel.Expression.ANGRY, 5000);
      }
   }

   public void startYapping(
      ServerClient serverClient,
      final ArrayList<GameMessage> messages,
      float timeBetweenMessagesInSeconds,
      boolean standStillAndStare,
      final Runnable onLastYappingRan
   ) {
      this.settlerMob.cancelJob();
      if (standStillAndStare) {
         int totalTime = (int)(timeBetweenMessagesInSeconds * messages.size() * 1000.0F);
         this.settlerMob.ai.blackboard.submitEvent("startstaring", new StartStaringAIEvent(totalTime, () -> serverClient.playerMob.getPositionPoint()));
      }

      for (int i = 0; i < messages.size(); i++) {
         final GameMessage responseMessage = messages.get(i);
         final int currentResponse = i;
         this.settlerMob
            .getLevel()
            .entityManager
            .events
            .addHidden(
               new WaitForSecondsEvent(timeBetweenMessagesInSeconds * i) {
                  @Override
                  public void onWaitOver() {
                     RomanceSettlerDialogue.this.settlerMob
                        .getServer()
                        .network
                        .sendToClientsWithEntity(
                           new PacketMobChat(RomanceSettlerDialogue.this.settlerMob.getUniqueID(), responseMessage), RomanceSettlerDialogue.this.settlerMob
                        );
                     if (onLastYappingRan != null && currentResponse == messages.size() - 1) {
                        onLastYappingRan.run();
                     }
                  }
               }
            );
      }
   }

   @Override
   public void initForm(ShopContainer container, ShopContainerForm<?> containerForm) {
      super.initForm(container, containerForm);
      if (this.completed) {
         this.subForm = null;
      } else {
         this.subForm = containerForm.addComponent(
            new DialogueForm("romancedialogueform", containerForm.width, containerForm.minHeight, containerForm.maxHeight, true) {
               @Override
               public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                  RomanceSettlerDialogue.this.startActivityButton.setActive(true);
                  super.draw(tickManager, perspective, renderBox);
               }
            }
         );
         this.subForm.reset(container.humanShop, true, container.romanceLevel, (contentBox, flow) -> {
            Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);

            for (GameMessage mainTextMessage : this.mainText) {
               GameMessage introMessage = new GameMessageBuilder().append(mainTextMessage);
               DialogueForm.addText(contentBox, flow, introMessage, true);
            }

            chatBubble.run();
         });
         if (!container.humanShop.missionFailed
            && !container.startInRecruitment
            && (!container.isInYourAdventureParty || this.hasCompletedActivity || !this.hasInitializedActivity)) {
            containerForm.makeCurrent(this.subForm);
         }
      }
   }

   @Override
   public void setupDialogueOptions(ShopContainer container, ShopContainerForm<?> containerForm) {
      super.setupDialogueOptions(container, containerForm);
      if (this.completed) {
         this.subForm = null;
      } else {
         this.setupGenericDialogueOptions(container, containerForm, this.subForm);
      }
   }

   public Form getConfirmClickAcceptForm(ShopContainer container, ShopContainerForm<?> containerForm, DialogueForm subForm, Runnable confirmClicked) {
      return null;
   }

   public Form getConfirmClickDeclineForm(ShopContainer container, ShopContainerForm<?> containerForm, DialogueForm subForm, Runnable confirmClicked) {
      return null;
   }

   public void setupGenericDialogueOptions(ShopContainer container, ShopContainerForm<?> containerForm, DialogueForm subForm) {
      subForm.reset(container.humanShop, true, container.romanceLevel, (contentBox, flow) -> {
         Runnable chatBubble = DialogueForm.startChatBubble(contentBox, flow);

         for (GameMessage mainTextMessage : this.mainText) {
            GameMessage introMessage = new GameMessageBuilder().append(mainTextMessage);
            DialogueForm.addText(contentBox, flow, introMessage, true);
         }

         chatBubble.run();
      });
      this.startActivityButton = subForm.addDialogueOption(this.acceptActivityText, () -> {
         Runnable confirmClicked = () -> {
            this.clickedAcceptAction.runAndSend();
            containerForm.getClient().closeContainer(true);
         };
         Form confirmForm = this.getConfirmClickAcceptForm(container, containerForm, subForm, confirmClicked);
         if (confirmForm != null) {
            containerForm.addAndMakeCurrentTemporary(confirmForm);
         } else {
            confirmClicked.run();
         }
      });
      if (this.declineActivityText != null) {
         subForm.addDialogueOption(this.declineActivityText, () -> {
            Runnable confirmClicked = () -> {
               if (this.settlerChatsAfterDeclining.isEmpty()) {
                  containerForm.makeCurrent(containerForm.dialogueForm);
               } else {
                  this.clickedDeclineAction.runAndSend();
                  containerForm.getClient().closeContainer(true);
               }
            };
            Form confirmForm = this.getConfirmClickDeclineForm(container, containerForm, subForm, confirmClicked);
            if (confirmForm != null) {
               containerForm.addAndMakeCurrentTemporary(confirmForm);
            } else {
               confirmClicked.run();
            }
         });
      }
   }

   @Override
   public <T extends ShopContainer> void onWindowResized(GameWindow window, ShopContainer container, ShopContainerForm<?> containerForm) {
      super.onWindowResized(window, container, containerForm);
      if (this.subForm != null) {
         ContainerComponent.setPosFocus(this.subForm);
      }
   }
}
