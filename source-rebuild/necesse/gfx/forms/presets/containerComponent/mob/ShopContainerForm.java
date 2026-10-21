package necesse.gfx.forms.presets.containerComponent.mob;

import java.awt.Rectangle;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.GameLog;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.Client;
import necesse.engine.network.packet.PacketShopContainerUpdate;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.ExpeditionList;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.components.FormDialogueOption;
import necesse.gfx.forms.components.FormProgressBar;
import necesse.gfx.forms.components.FormSettlerHappinessBar;
import necesse.gfx.forms.components.FormSettlerHungerBar;
import necesse.gfx.forms.components.FormSettlerRecreationBar;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.presets.NoticeForm;
import necesse.gfx.forms.presets.SelectSettlementContinueForm;
import necesse.gfx.forms.presets.containerComponent.PartyConfigForm;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.container.events.ShopThoughtsUpdateEvent;
import necesse.inventory.container.mob.ContainerExpedition;
import necesse.inventory.container.mob.NetworkSettlerThought;
import necesse.inventory.container.mob.ShopContainer;
import necesse.inventory.container.mob.ShopContainerInventoryClaimedEvent;
import necesse.inventory.container.mob.ShopContainerPartyResponseEvent;
import necesse.inventory.container.mob.ShopContainerPartyUpdateEvent;
import necesse.inventory.container.settlement.events.SettlementOpenSettlementListEvent;
import necesse.level.maps.levelData.settlementData.settler.dialogues.SettlerDialogue;

public class ShopContainerForm<T extends ShopContainer> extends MobContainerFormSwitcher<T> {
   public static int minDialogueHeight = 220;
   public static int maxDialogueHeight = 520;
   public static int dialogueWidth = 508;
   public int width = dialogueWidth;
   public int minHeight = minDialogueHeight;
   public int maxHeight = Math.max(this.minHeight, Math.min(maxDialogueHeight, ContainerComponent.getMaxFocusHeight()));
   public DialogueForm workInvForm;
   public DialogueForm missionFailedForm;
   public DialogueForm moodForm;
   public DialogueForm recruitForm;
   public DialogueForm whatCanYouDoForm;
   public WorkInventoryDialogueForm dialogueForm;
   public ShopContainerShopForm shopForm;
   public ShopQuestsForm questForm;
   public DialogueForm[] expeditionsForms;
   public DialogueForm[] unavailableExpeditionsForms;
   public DialogueForm expeditionFocus;
   public boolean waitingForPartyConfirm;
   public DialogueForm partyResponseForm;
   public PartyConfigForm partyConfigForm;
   public FormDialogueOption acceptRecruitButton;
   public int expeditionBuyCost;
   public FormDialogueOption expeditionBuyButton;

   public ShopContainerForm(Client client, T container) {
      super(client, container);
      container.onEvent(ShopContainerPartyUpdateEvent.class, event -> this.updateDialogue());
      this.workInvForm = this.addComponent(
         new DialogueForm("workinv", this.width, this.minHeight, this.maxHeight, true, container.humanShop, true, container.romanceLevel, null)
      );
      container.onEvent(ShopContainerInventoryClaimedEvent.class, event -> {
         if (this.isCurrent(this.workInvForm)) {
            this.makeCurrent(this.dialogueForm);
         }

         this.dialogueForm.updateInventoryForm();
      });
      this.dialogueForm = this.addComponent(
         new WorkInventoryDialogueForm(
            this,
            new DialogueForm(
               "dialogue", this.width, this.minHeight, this.maxHeight, true, container.humanShop, true, container.romanceLevel, container.introMessage
            )
         )
      );
      this.moodForm = this.addComponent(new DialogueForm("mood", this.width, this.minHeight, this.maxHeight, true));
      this.updateMoodForm();
      container.onEvent(ShopThoughtsUpdateEvent.class, event -> this.updateMoodForm());
      this.partyResponseForm = this.addComponent(
         new DialogueForm("partyresponse", this.width, this.minHeight, this.maxHeight, true, container.humanShop, false, container.romanceLevel, null)
      );
      this.partyConfigForm = this.addComponent(
         new PartyConfigForm(client, container, container.PARTY_SLOTS_START, container.PARTY_SLOTS_END, this.width, null, () -> {
            container.setIsInPartyConfig.runAndSend(false);
            this.makeCurrent(this.dialogueForm);
         }, () -> ContainerComponent.setPosFocus(this.partyConfigForm))
      );
      container.onEvent(ShopContainerPartyResponseEvent.class, e -> {
         if (this.waitingForPartyConfirm) {
            if (e.error != null) {
               this.partyResponseForm.reset(container.humanShop, false, container.romanceLevel, e.error);
               this.partyResponseForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> this.makeCurrent(this.dialogueForm));
               this.makeCurrent(this.partyResponseForm);
            } else {
               container.setIsInPartyConfig.runAndSend(true);
               this.makeCurrent(this.partyConfigForm);
            }

            this.waitingForPartyConfirm = false;
         }
      });
      this.questForm = this.addComponent(new ShopQuestsForm("quest", this.width, this.minHeight, this.maxHeight, client, container) {
         @Override
         public void backPressed() {
            ShopContainerForm.this.makeCurrent(ShopContainerForm.this.dialogueForm);
         }
      });
      this.shopForm = this.addComponent(new ShopContainerShopForm(container, this.width, this.minHeight, this.maxHeight) {
         @Override
         public void onBackPressed() {
            ShopContainerForm.this.makeCurrent(ShopContainerForm.this.dialogueForm);
         }
      });
      this.recruitForm = this.addComponent(new DialogueForm("recruit", this.width, this.minHeight, this.maxHeight, true));
      container.setupRecruitDialogue(this.recruitForm);
      if (container.recruitItems != null) {
         LocalMessage recruitMessage = new LocalMessage("ui", "acceptbutton");
         if (container.humanShop.isDowned()) {
            recruitMessage = new LocalMessage("ui", "settlerrevivebutton");
         }

         this.acceptRecruitButton = this.recruitForm.addDialogueOption(recruitMessage, () -> {
            NoticeForm loadingForm = new NoticeForm("loadingsettlements", 300, 400);
            loadingForm.setButtonCooldown(-2);
            loadingForm.setupNotice(new LocalMessage("ui", "loadingdotdot"));
            this.addComponent(loadingForm, (form, switched) -> {
               if (!switched) {
                  this.removeComponent(form);
               }
            });
            this.makeCurrent(loadingForm);
            container.acceptRecruitAction.runAndSend();
         });
      } else {
         this.acceptRecruitButton = null;
      }

      this.updateAcceptRecruitButton();
      if (container.humanShop.isDowned()) {
         this.recruitForm.addDialogueOption(new LocalMessage("ui", "settlergoodbye"), () -> client.closeContainer(true));
      } else {
         this.recruitForm.addDialogueOption(new LocalMessage("ui", "settlerwhatcanyoudo"), () -> this.makeCurrent(this.whatCanYouDoForm));
         this.recruitForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> this.makeCurrent(this.dialogueForm));
      }

      GameMessageBuilder builder = new GameMessageBuilder();
      container.humanShop
         .jobTypeHandler
         .getTypePriorities()
         .stream()
         .filter(t -> !t.disabledBySettler && t.type.displayName != null)
         .sorted(Comparator.comparingInt(t -> t.type.getID()))
         .forEachOrdered(t -> builder.append("\n\t- ").append(t.type.displayName));
      if (container.humanShop.canJoinAdventureParties) {
         builder.append("\n").append("ui", "settlericanadventure");
      }

      GameMessage whatCanYouDoMessage = new LocalMessage("ui", "settlericando", "list", builder);
      this.whatCanYouDoForm = this.addComponent(
         new DialogueForm(
            "whatcanyoudo", this.width, this.minHeight, this.maxHeight, true, container.humanShop, true, container.romanceLevel, whatCanYouDoMessage
         )
      );
      this.whatCanYouDoForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> this.makeCurrent(this.recruitForm));
      this.missionFailedForm = this.addComponent(
         new DialogueForm(
            "missionFailed",
            this.width,
            this.minHeight,
            this.maxHeight,
            true,
            container.humanShop,
            false,
            container.romanceLevel,
            container.missionFailedMessage != null ? container.missionFailedMessage : new LocalMessage("ui", "settlermissionfailed")
         )
      );
      this.missionFailedForm.addDialogueOption(new LocalMessage("ui", "continuebutton"), () -> {
         container.continueFailedMissionAction.runAndSend();
         this.makeCurrent(this.dialogueForm);
      });
      this.expeditionsForms = new DialogueForm[container.expeditionLists.size()];
      this.unavailableExpeditionsForms = new DialogueForm[container.expeditionLists.size()];

      for (int i = 0; i < container.expeditionLists.size(); i++) {
         int listIndex = i;
         ExpeditionList expeditionList = container.expeditionLists.get(i);
         DialogueForm expeditionsForm = this.addComponent(
            new DialogueForm(
               "expeditions" + i,
               this.width,
               this.minHeight,
               this.maxHeight,
               true,
               container.humanShop,
               false,
               container.romanceLevel,
               expeditionList.selectMessage
            )
         );
         this.expeditionsForms[i] = expeditionsForm;
         expeditionsForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> this.makeCurrent(this.dialogueForm));
         DialogueForm unavailableForm = this.addComponent(
            new DialogueForm(
               "expeditionsUnavailable" + i,
               this.width,
               this.minHeight,
               this.maxHeight,
               true,
               container.humanShop,
               false,
               container.romanceLevel,
               expeditionList.selectMessage
            )
         );
         this.unavailableExpeditionsForms[i] = unavailableForm;
         unavailableForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> this.makeCurrent(expeditionsForm));
         boolean addedUnavailable = false;

         for (int j = 0; j < expeditionList.expeditions.size(); j++) {
            int expeditionIndex = j;
            ContainerExpedition expedition = expeditionList.expeditions.get(j);
            if (expedition.available) {
               expeditionsForm.addDialogueOption(
                  expedition.expedition.getDisplayName(),
                  () -> this.focusOnExpedition(listIndex, expeditionIndex, expeditionList.focusMessage, expedition, expeditionsForm)
               );
            } else {
               addedUnavailable = true;
               FormDialogueOption option = unavailableForm.addDialogueOption(expedition.expedition.getDisplayName(), () -> {});
               option.setActive(false);
               GameMessage unavailableMessage = expedition.expedition.getUnavailableMessage();
               if (unavailableMessage != null) {
                  option.tooltipsSupplier = () -> new StringTooltips(unavailableMessage.translate());
               }
            }
         }

         if (addedUnavailable) {
            expeditionsForm.addDialogueOption(expeditionList.moreOptionsDialogue, () -> this.makeCurrent(unavailableForm));
         }
      }

      this.expeditionFocus = this.addComponent(new DialogueForm("expeditionFocus", this.width, this.minHeight, this.maxHeight, true));
      if (container.humanShop.missionFailed) {
         this.makeCurrent(this.missionFailedForm);
      } else if (container.workInvNotificationMessage != null && !container.humanShop.workInventory.isEmpty()) {
         this.makeCurrent(this.workInvForm);
      } else if (container.startInRecruitment) {
         this.makeCurrent(this.recruitForm);
      } else if (container.quests != null && !container.quests.isEmpty()) {
         this.makeCurrent(this.questForm);
      } else {
         this.makeCurrent(this.dialogueForm);
      }
   }

   @Override
   protected void init() {
      super.init();

      for (SettlerDialogue dialogue : this.container.dialogues) {
         dialogue.initForm(this.container, this);
      }

      for (ShopContainer.ContainerWorkSetting<?> workSetting : this.container.workSettings) {
         workSetting.handler.initForm(this);
      }

      this.updateWorkInventoryForm();
      this.updateDialogue();
   }

   public void updateDialogue() {
      this.dialogueForm.reset(this.container.humanShop, true, this.container.romanceLevel, this.container.introMessage);
      this.setupExtraDialogueOptions();
      if (this.container.recruitItems != null || this.container.recruitError != null) {
         this.dialogueForm.addDialogueOption(new LocalMessage("ui", "settlerjoinme"), () -> this.makeCurrent(this.recruitForm));
      } else if (this.isCurrent(this.recruitForm)) {
         this.makeCurrent(this.dialogueForm);
      }

      for (ShopContainer.ContainerWorkSetting<?> workSetting : this.container.workSettings) {
         workSetting.handler.updateDialogue(this);
      }

      this.dialogueForm.addDialogueOption(new LocalMessage("ui", "settlergoodbye"), () -> this.client.closeContainer(true));
      if (this.isInitialized()) {
         this.onWindowResized(WindowManager.getWindow());
      }
   }

   protected void setupExtraDialogueOptions() {
      this.addMoodDialogueOption();
      this.addQuestsDialogueOption();
      this.setupSettlerDialogues();
      this.addAdventurePartyDialogueOptions();
      this.addShopDialogueOptions();
      this.addExpeditionsDialogueOptions();
   }

   protected void addMoodDialogueOption() {
      if (this.container.thoughts != null) {
         this.dialogueForm.addDialogueOption(new LocalMessage("ui", "settlermood"), () -> {
            this.container.requestThoughts.runAndSend();
            this.makeCurrent(this.moodForm);
         });
      }
   }

   public void updateMoodForm() {
      int scrollY = this.moodForm.content.getScrollY();
      if (this.container.thoughts != null) {
         this.moodForm
            .reset(
               this.container.humanShop,
               true,
               this.container.romanceLevel,
               (content, flow) -> {
                  if (this.container.humanShop.isOnStrike() && this.container.strikeMessage != null) {
                     Runnable chatBubble = DialogueForm.startChatBubble(content, flow);
                     DialogueForm.addText(content, flow, this.container.strikeMessage, true);
                     chatBubble.run();
                     flow.next(10);
                  }

                  content.addComponent(flow.nextY(new FormLocalLabel("ui", "settlerhappinessbar", new FontOptions(16), -1, 10, 0, content.getWidth() - 20)));
                  flow.next(5);
                  content.addComponent(flow.nextY(new FormSettlerHappinessBar(10, 0, content.getWidth() - 30, false) {
                     @Override
                     public int getHappinessLevel() {
                        return ShopContainerForm.this.container.settlerHappiness;
                     }

                     @Override
                     public String getSettlerName() {
                        return ShopContainerForm.this.container.humanShop.getSettlerName();
                     }
                  }, 4));
                  int needsMaxWidth = content.getWidth();
                  int needsBarSplitWidth = 20;
                  int needsBarWidth = (needsMaxWidth - 30 - needsBarSplitWidth) / 2;
                  FormLocalLabel hungerLabel = content.addComponent(new FormLocalLabel("ui", "settlerhungerbar", new FontOptions(16), -1, 10, 0, needsBarWidth));
                  FormLocalLabel recreationLabel = content.addComponent(
                     new FormLocalLabel("ui", "settlerrecreationbar", new FontOptions(16), -1, 10 + needsBarWidth + needsBarSplitWidth, 0, needsBarWidth)
                  );
                  flow.next(Math.max(hungerLabel.getBoundingBox().height, recreationLabel.getBoundingBox().height) + 5);
                  int needsBarStartY = flow.next();
                  hungerLabel.setY(needsBarStartY - hungerLabel.getBoundingBox().height - 5);
                  recreationLabel.setY(needsBarStartY - recreationLabel.getBoundingBox().height - 5);
                  FormProgressBar hungerBar = content.addComponent(new FormSettlerHungerBar(10, needsBarStartY, needsBarWidth, false) {
                     @Override
                     public float getHungerLevel() {
                        return ShopContainerForm.this.container.humanShop.getHungerLevel();
                     }
                  });
                  content.addComponent(new FormSettlerRecreationBar(10 + needsBarWidth + needsBarSplitWidth, needsBarStartY, needsBarWidth, false) {
                     @Override
                     public float getRecreationLevel() {
                        return ShopContainerForm.this.container.humanShop.getRecreationLevel();
                     }
                  });
                  flow.next(hungerBar.getFullHeight() + 8);
                  FontOptions fontOptions = new FontOptions(16);
                  needsBarSplitWidth = 0;
                  List<NetworkSettlerThought> thoughts = this.container
                     .thoughts
                     .stream()
                     .filter(t -> t.thought.shouldShowThought(this.container.humanShop))
                     .sorted(Comparator.comparingInt(t -> -t.getHappinessModifier(this.container.humanShop)))
                     .collect(Collectors.toList());

                  for (NetworkSettlerThought thought : thoughts) {
                     needsBarSplitWidth = Math.max(
                        needsBarSplitWidth,
                        FontManager.bit.getWidthCeil(Integer.toString(Math.abs(thought.getHappinessModifier(this.container.humanShop))), fontOptions)
                     );
                  }

                  if (!thoughts.isEmpty()) {
                     flow.next(16);
                     Runnable thoughtBubble = DialogueForm.startThoughtBubble(content, flow);
                     int prefixWidth = Math.max(FontManager.bit.getWidthCeil("-", fontOptions), FontManager.bit.getWidthCeil("+", fontOptions));
                     DialogueForm.addText(
                        content,
                        flow,
                        new LocalMessage("settlement", "thoughts"),
                        prefixWidth + needsBarSplitWidth + 10,
                        content.getWidth() - prefixWidth - needsBarSplitWidth - 20,
                        true
                     );

                     for (NetworkSettlerThought thought : thoughts) {
                        int happiness = thought.getHappinessModifier(this.container.humanShop);
                        String prefix = happiness == 0 ? "" : (happiness > 0 ? "+" : "-");
                        int x = 15;
                        DialogueForm.addText(
                           content,
                           flow.split(),
                           new StaticMessage(prefix),
                           x + prefixWidth / 2 - FontManager.bit.getWidthCeil(prefix, fontOptions) / 2,
                           Integer.MAX_VALUE,
                           true
                        );
                        x += prefixWidth;
                        DialogueForm.addText(content, flow.split(), new StaticMessage(Integer.toString(Math.abs(happiness))), x, Integer.MAX_VALUE, true);
                        x += needsBarSplitWidth + 10;
                        DialogueForm.addText(content, flow, thought.getDescription(this.container.humanShop), x, content.getWidth() - x - 20, true);
                     }

                     thoughtBubble.run();
                  }
               }
            );
      } else {
         this.moodForm.reset(this.container.humanShop, true, this.container.romanceLevel, new StaticMessage(""));
      }

      this.moodForm.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> this.makeCurrent(this.dialogueForm));
      this.moodForm.content.setScrollY(scrollY);
   }

   protected void addQuestsDialogueOption() {
      if (this.container.quests != null) {
         this.dialogueForm.addDialogueOption(new LocalMessage("ui", "elderwantquest"), () -> {
            this.questForm.updateActive();
            this.makeCurrent(this.questForm);
         });
      }
   }

   protected void setupSettlerDialogues() {
      for (SettlerDialogue dialogue : this.container.dialogues) {
         dialogue.setupDialogueOptions(this.container, this);
      }
   }

   protected void addAdventurePartyDialogueOptions() {
      if (this.container.hasSettlerAccess && this.container.canJoinAdventureParties && !this.container.isInYourAdventureParty) {
         if (this.isCurrent(this.partyConfigForm)) {
            this.container.setIsInPartyConfig.runAndSend(false);
            this.makeCurrent(this.dialogueForm);
         }

         this.dialogueForm.addDialogueOption(new LocalMessage("ui", "settlerjoinparty"), () -> {
            this.container.joinAdventurePartyAction.runAndSend();
            this.waitingForPartyConfirm = true;
         });
         if (this.container.isSettlerOutsideSettlement) {
            this.dialogueForm
               .addDialogueOption(new LocalMessage("ui", "settlerreturntosettlement"), () -> this.container.returnToSettlementAction.runAndSend());
         }
      } else if (this.container.isInYourAdventureParty) {
         this.dialogueForm.addDialogueOption(new LocalMessage("ui", "confiureadventureparty"), () -> {
            this.container.setIsInPartyConfig.runAndSend(true);
            this.makeCurrent(this.partyConfigForm);
         });
         this.dialogueForm.addDialogueOption(new LocalMessage("ui", "settlerleaveparty"), () -> this.container.leaveAdventurePartyAction.runAndSend());
      }
   }

   protected void addShopDialogueOptions() {
      boolean hasSellingItems = this.container.sellingItems != null && !this.container.sellingItems.isEmpty();
      boolean hasBuyingItems = this.container.buyingItems != null && !this.container.buyingItems.isEmpty();
      if (hasSellingItems || hasBuyingItems) {
         this.dialogueForm.addDialogueOption(new LocalMessage("ui", "traderwantshop"), () -> this.makeCurrent(this.shopForm));
      }
   }

   protected void addExpeditionsDialogueOptions() {
      for (int i = 0; i < this.container.expeditionLists.size(); i++) {
         int finalI = i;
         ExpeditionList expeditionList = this.container.expeditionLists.get(finalI);
         this.dialogueForm.addDialogueOption(expeditionList.selectDialogue, () -> this.makeCurrent(this.expeditionsForms[finalI]));
      }
   }

   protected void updateWorkInventoryForm() {
      this.container.setupWorkInvDialogue(this.workInvForm);
      this.dialogueForm.updateInventoryForm();
      this.workInvForm.addDialogueOption(new LocalMessage("ui", "settlerreceiveitems"), () -> {
         this.container.workItemsAction.runAndSend(ShopContainer.WorkItemsAction.RECEIVE);
         this.container.humanShop.workInventory.clear();
         this.dialogueForm.updateInventoryForm();
         if (this.container.startInRecruitment) {
            this.makeCurrent(this.recruitForm);
         } else {
            this.makeCurrent(this.dialogueForm);
         }
      });
      this.workInvForm.addDialogueOption(new LocalMessage("ui", "continuebutton"), () -> {
         this.container.workItemsAction.runAndSend(ShopContainer.WorkItemsAction.CONTINUE);
         if (this.container.startInRecruitment) {
            this.makeCurrent(this.recruitForm);
         } else {
            this.makeCurrent(this.dialogueForm);
         }
      });
      if (Input.lastInputIsController) {
         this.workInvForm.content.setScrollY(100000);
      }

      if (this.isCurrent(this.workInvForm) && this.container.humanShop.workInventory.isEmpty()) {
         this.makeCurrent(this.dialogueForm);
      }
   }

   public void updateAcceptRecruitButton() {
      if (this.acceptRecruitButton != null) {
         this.acceptRecruitButton.setActive(this.container.canPayForRecruit());
      }
   }

   public void openSettlementList(int mobUniqueID, List<SettlementOpenSettlementListEvent.SettlementOption> options) {
      if (this.container.humanShop.getUniqueID() != mobUniqueID) {
         GameLog.warn.println("Received wrong settlement list for mob id " + mobUniqueID);
      } else {
         SelectSettlementContinueForm.Option[] selectOptions = options.stream()
            .map(o -> new SelectSettlementContinueForm.Option(true, o.settlementUniqueID, o.name) {
               @Override
               public void onSelected(SelectSettlementContinueForm form) {
                  ShopContainerForm.this.client.network.sendPacket(PacketShopContainerUpdate.recruitSettler(mobUniqueID, this.settlementUniqueID));
                  NoticeForm loadingForm = new NoticeForm("loadingresponse", 300, 400);
                  loadingForm.setButtonCooldown(-2);
                  loadingForm.setupNotice(new LocalMessage("ui", "loadingdotdot"));
                  ShopContainerForm.this.addComponent(loadingForm, (lForm, switched) -> {
                     if (!switched) {
                        ShopContainerForm.this.removeComponent(lForm);
                     }
                  });
                  ShopContainerForm.this.makeCurrent(loadingForm);
               }
            })
            .toArray(SelectSettlementContinueForm.Option[]::new);
         SelectSettlementContinueForm selectSettlementForm = new SelectSettlementContinueForm(
            "movetosettlement", 300, 400, new LocalMessage("ui", "settlementselect"), selectOptions
         ) {
            @Override
            public void onCancel() {
               ShopContainerForm.this.makeCurrent(ShopContainerForm.this.recruitForm);
               this.removeComponent(this);
            }
         };
         this.addComponent(selectSettlementForm);
         this.makeCurrent(selectSettlementForm);
      }
   }

   public void submitRecruitResponse(int mobUniqueID, GameMessage error) {
      if (this.container.humanShop.getUniqueID() != mobUniqueID) {
         GameLog.warn.println("Received wrong recruit response for mob id " + mobUniqueID);
      } else {
         if (error == null) {
            this.container.payForRecruit();
            this.client.closeContainer(true);
         } else {
            NoticeForm errorForm = new NoticeForm("loadingresponse", 300, 400);
            errorForm.setupNotice(error);
            errorForm.setButtonCooldown(50);
            errorForm.onContinue(() -> this.makeCurrent(this.recruitForm));
            this.addComponent(errorForm, (form, switched) -> {
               if (!switched) {
                  this.removeComponent(form);
               }
            });
            this.makeCurrent(errorForm);
         }
      }
   }

   public void updateExpeditionBuyButton(PlayerMob perspective) {
      if (this.expeditionBuyButton != null) {
         int amount = perspective.getInv().getAmount(ItemRegistry.getItem("coin"), true, true, false, false, "buy");
         this.expeditionBuyButton.setActive(amount >= this.expeditionBuyCost);
      }
   }

   public void focusOnExpedition(int listIndex, int expeditionIndex, GameMessage costMessage, ContainerExpedition expedition, DialogueForm expeditionsForm) {
      this.expeditionBuyCost = expedition.price;
      String message = costMessage.translate()
         .replace("<expedition>", expedition.expedition.getDisplayName().translate())
         .replace("<cost>", Integer.toString(this.expeditionBuyCost))
         .replace("<chance>", Integer.toString((int)(expedition.successChance * 100.0F)));
      this.expeditionFocus.reset(this.container.humanShop, false, this.container.romanceLevel, new StaticMessage(message));
      this.expeditionBuyButton = this.expeditionFocus
         .addDialogueOption(new LocalMessage("ui", "buybutton"), () -> this.container.buyExpeditionButton.runAndSend(listIndex, expeditionIndex));
      this.expeditionFocus.addDialogueOption(new LocalMessage("ui", "backbutton"), () -> {
         this.makeCurrent(expeditionsForm);
         this.expeditionBuyButton = null;
      });
      this.updateExpeditionBuyButton(this.client.getPlayer());
      this.makeCurrent(this.expeditionFocus);
      ContainerComponent.setPosFocus(this.expeditionFocus);
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      ContainerComponent.setPosFocus(this.missionFailedForm);
      ContainerComponent.setPosFocus(this.workInvForm);
      ContainerComponent.setPosFocus(this.dialogueForm.dialogueForm);
      ContainerComponent.setPosFocus(this.moodForm);
      ContainerComponent.setPosFocus(this.partyResponseForm);
      ContainerComponent.setPosFocus(this.partyConfigForm);
      ContainerComponent.setPosFocus(this.recruitForm);
      ContainerComponent.setPosFocus(this.whatCanYouDoForm);
      ContainerComponent.setPosFocus(this.questForm);

      for (DialogueForm expeditionsForm : this.expeditionsForms) {
         ContainerComponent.setPosFocus(expeditionsForm);
      }

      for (DialogueForm expeditionsForm : this.unavailableExpeditionsForms) {
         ContainerComponent.setPosFocus(expeditionsForm);
      }

      ContainerComponent.setPosFocus(this.expeditionFocus);

      for (SettlerDialogue dialogue : this.container.dialogues) {
         dialogue.onWindowResized(window, this.container, this);
      }
   }

   @Override
   public boolean shouldOpenInventory() {
      return true;
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      this.updateExpeditionBuyButton(perspective);
      if (this.isCurrent(this.recruitForm)) {
         this.updateAcceptRecruitButton();
      }

      super.draw(tickManager, perspective, renderBox);
   }
}
