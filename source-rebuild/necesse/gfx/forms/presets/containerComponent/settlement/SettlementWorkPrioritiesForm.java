package necesse.gfx.forms.presets.containerComponent.settlement;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.NavigableSet;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import necesse.engine.ClipboardTracker;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.client.Client;
import necesse.engine.registries.JobTypeRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.job.JobPriority;
import necesse.entity.mobs.job.JobType;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.ContainerComponent;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.FormSwitcher;
import necesse.gfx.forms.components.FormBreakLine;
import necesse.gfx.forms.components.FormComponentList;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormContentIconValueButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormMouseHover;
import necesse.gfx.forms.components.FormSettlerDataActivityLabel;
import necesse.gfx.forms.components.FormSettlerDataIcon;
import necesse.gfx.forms.components.FormSettlerDataNameLabel;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.SavedFormContentBoxScroll;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.position.FormPositionDynamic;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.ui.ButtonColor;
import necesse.inventory.container.settlement.SettlementContainer;
import necesse.inventory.container.settlement.data.SettlementSettlerData;
import necesse.inventory.container.settlement.data.SettlerWorkPrioritiesData;
import necesse.inventory.container.settlement.events.SettlementSettlerBasicsEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerPrioritiesChangedEvent;
import necesse.inventory.container.settlement.events.SettlementSettlerWorkPrioritiesDataEvent;
import necesse.level.maps.hudManager.HudDrawElement;

public class SettlementWorkPrioritiesForm<T extends SettlementContainer> extends Form implements SettlementSubForm {
   public static SavedFormContentBoxScroll lastScroll = new SavedFormContentBoxScroll();
   public final Client client;
   public final T container;
   public final SettlementContainerForm<T> containerForm;
   public int maxHeight;
   public int contentWidth;
   public int contentHeight;
   protected FormSwitcher setCurrentWhenLoaded;
   protected ArrayList<SettlementWorkPrioritiesForm.JobTitlePosition> jobTypes;
   public FormTextInput searchInput;
   public FormComponentList jobTitles;
   public FormContentBox content;
   public ClipboardTracker<SettlementWorkPrioritiesForm.PrioritiesData> listClipboard;
   public ArrayList<SettlementWorkPrioritiesForm.PasteButton> pasteButtons;
   private final ArrayList<HudDrawElement> hudElements = new ArrayList<>();
   private final int namesWidth = 238;

   public SettlementWorkPrioritiesForm(Client client, T container, SettlementContainerForm<T> containerForm) {
      super(800, 250);
      this.client = client;
      this.container = container;
      this.containerForm = containerForm;
      this.maxHeight = 300;
      FormFlow flow = new FormFlow(4);
      int searchY = flow.next(28);
      this.jobTitles = this.addComponent(new FormComponentList());
      ArrayList<JobType> jobTypes = JobTypeRegistry.streamTypes()
         .filter(type -> type.canChangePriority)
         .sorted(Comparator.comparingInt(JobType::getID))
         .collect(Collectors.toCollection(ArrayList::new));
      this.jobTypes = this.addJobTitles(flow, jobTypes);
      int contentHeight = 210;
      this.content = this.addComponent(new FormContentBox(0, flow.next(contentHeight), this.getWidth(), contentHeight));
      int maxWidth = WindowManager.getWindow().getHudWidth() - 200;
      int minWidth = 200;
      this.contentWidth = this.contentWidth + this.content.getScrollBarWidth() + 6;
      this.setWidth(GameMath.limit(this.contentWidth, minWidth, Math.max(maxWidth, minWidth)));
      this.content.setWidth(this.getWidth());
      this.searchInput = this.addComponent(new FormTextInput(4, searchY, FormInputSize.SIZE_24, this.getWidth() - 8, -1, 500));
      this.searchInput.placeHolder = new LocalMessage("ui", "searchtip");
      this.searchInput.rightClickToClear = true;
      this.searchInput.rightClickToClearTooltip = new LocalMessage("controls", "clearsearchtip");
      if (SettlementSettlersForm.lastSettlersSearch != null) {
         this.searchInput.setText(SettlementSettlersForm.lastSettlersSearch);
      }

      this.searchInput.onChange(e -> {
         SettlementSettlersForm.lastSettlersSearch = this.searchInput.getText();
         this.updateContent();
      });
      this.pasteButtons = new ArrayList<>();
      this.listClipboard = new ClipboardTracker<SettlementWorkPrioritiesForm.PrioritiesData>() {
         public SettlementWorkPrioritiesForm.PrioritiesData parse(String clipboard) {
            try {
               return new SettlementWorkPrioritiesForm.PrioritiesData(new LoadData(clipboard));
            } catch (Exception e) {
               return null;
            }
         }

         public void onUpdate(SettlementWorkPrioritiesForm.PrioritiesData value) {
            for (SettlementWorkPrioritiesForm.PasteButton pasteButton : SettlementWorkPrioritiesForm.this.pasteButtons) {
               pasteButton.updateActive(value);
            }
         }
      };
   }

   @Override
   protected void init() {
      super.init();
      this.container.onEvent(SettlementSettlerBasicsEvent.class, event -> {
         if (this.containerForm.isCurrent(this)) {
            this.containerForm.dataManager.requestSettlerData(this.container.prioritiesSubscription, s -> s.workPrioritiesData == null);
         }
      });
      this.container.onEvent(SettlementSettlerWorkPrioritiesDataEvent.class, event -> {
         this.containerForm.dataManager.applySettlerData(event, (data, addon) -> data.workPrioritiesData = addon);
         if (this.setCurrentWhenLoaded != null) {
            this.setCurrentWhenLoaded.makeCurrent(this);
         }

         this.setCurrentWhenLoaded = null;
         if (this.containerForm.isCurrent(this)) {
            this.updateContent();
         }
      });
      this.container.onEvent(SettlementSettlerPrioritiesChangedEvent.class, event -> {
         SettlementSettlerData settler = this.containerForm.dataManager.getSettler(event.mobUniqueID);
         if (settler != null) {
            if (settler.workPrioritiesData != null) {
               for (Entry<JobType, SettlerWorkPrioritiesData.TypePriority> e : event.priorities.entrySet()) {
                  SettlerWorkPrioritiesData.TypePriority priority = settler.workPrioritiesData.priorities.get(e.getKey());
                  if (priority == null) {
                     this.container.prioritiesSubscription.requestData(Collections.singletonList(event.mobUniqueID));
                     return;
                  }

                  priority.priority = e.getValue().priority;
                  priority.disabledByPlayer = e.getValue().disabledByPlayer;
                  this.listClipboard.forceUpdate();
                  this.listClipboard.onUpdate(this.listClipboard.getValue());
               }
            } else {
               this.container.prioritiesSubscription.requestData(Collections.singletonList(event.mobUniqueID));
            }
         } else {
            this.container.requestSettlerBasics.runAndSend();
         }
      });
   }

   public void updateContent() {
      this.hudElements.forEach(HudDrawElement::remove);
      this.hudElements.clear();
      this.content.clearComponents();
      this.pasteButtons.clear();
      FormFlow flow = new FormFlow();
      String searchLower = SettlementSettlersForm.lastSettlersSearch == null ? "" : SettlementSettlersForm.lastSettlersSearch.toLowerCase();
      List<SettlementSettlerData> settlers = this.containerForm
         .dataManager
         .streamSettlers()
         .filter(data -> data.workPrioritiesData != null)
         .filter(data -> data.matchesSearch(searchLower, this.client.getLevel(), this.client.getPlayer()))
         .collect(Collectors.toList());
      if (!settlers.isEmpty()) {
         this.addSettlers(flow, settlers, this.jobTypes);
      }

      if (settlers.isEmpty()) {
         this.jobTitles.setHidden(true);
         this.content.alwaysShowVerticalScrollBar = false;
         if (this.containerForm.dataManager.getTotalSettlers() == 0) {
            this.content
               .addComponent(
                  flow.nextY(new FormLocalLabel("ui", "settlersnosettlers", new FontOptions(16), 0, this.getWidth() / 2, 0, this.getWidth() - 20), 16)
               );
         } else {
            this.content
               .addComponent(
                  flow.nextY(new FormLocalLabel("ui", "settlersnosettlerssearch", new FontOptions(16), 0, this.getWidth() / 2, 0, this.getWidth() - 20), 16)
               );
         }
      } else {
         this.jobTitles.setHidden(false);
         this.content.alwaysShowVerticalScrollBar = true;
      }

      this.contentHeight = flow.next() + 8;
      this.updateSize();
      this.listClipboard.forceUpdate();
      this.listClipboard.onUpdate(this.listClipboard.getValue());
      if (!settlers.isEmpty()) {
         lastScroll.load(this.content);
      }
   }

   private ArrayList<SettlementWorkPrioritiesForm.JobTitlePosition> addJobTitles(FormFlow flow, ArrayList<JobType> jobTypes) {
      int titlesY = flow.next(40);
      int nextY = flow.next();
      ArrayList<SettlementWorkPrioritiesForm.JobTitlePosition> out = new ArrayList<>(jobTypes.size());
      FormFlow horizontalFlow = new FormFlow(238);
      int titleOffset = 0;

      for (final JobType jobType : jobTypes) {
         int buttonWidth = 50;
         int buttonX = horizontalFlow.next(buttonWidth);
         out.add(new SettlementWorkPrioritiesForm.JobTitlePosition(jobType, buttonX + buttonWidth / 2));
         String jobName = jobType.displayName.translate();
         FontOptions jobNameOptions = new FontOptions(12);
         int nameY = titlesY + 5 + titleOffset * 18;
         String maxJobName = GameUtils.maxString(jobName, jobNameOptions, buttonWidth * 2 - 5);
         FormLabel label = this.jobTitles.addComponent(new FormLabel(maxJobName, jobNameOptions, 0, buttonX + buttonWidth / 2, nameY));
         Rectangle labelBox = label.getBoundingBox();
         FormMouseHover mouseHover = this.jobTitles
            .addComponent(new FormMouseHover(buttonX + buttonWidth / 2 - labelBox.width / 2, nameY, labelBox.width, 12) {
               @Override
               public GameTooltips getTooltips(PlayerMob perspective) {
                  StringTooltips tooltips = new StringTooltips();
                  tooltips.add(jobType.displayName.translate());
                  if (jobType.tooltip != null) {
                     tooltips.add(jobType.tooltip.translate(), 400);
                  }

                  return tooltips;
               }
            });
         FormBreakLine breakLine = this.jobTitles
            .addComponent(new FormBreakLine(FormBreakLine.ALIGN_BEGINNING, buttonX + buttonWidth / 2 - 1, nameY + 12, nextY + 5 - (nameY + 12) - 6, false));
         label.setPosition(new FormPositionDynamic(label.getX(), label.getY(), () -> -this.content.getScrollX(), () -> 0));
         mouseHover.setPosition(new FormPositionDynamic(mouseHover.getX(), mouseHover.getY(), () -> -this.content.getScrollX(), () -> 0));
         breakLine.setPosition(new FormPositionDynamic(breakLine.getX(), breakLine.getY(), () -> -this.content.getScrollX(), () -> 0));
         titleOffset = (titleOffset + 1) % 2;
         this.contentWidth = Math.max(horizontalFlow.next(), this.contentWidth);
      }

      return out;
   }

   private void addSettlers(FormFlow flow, List<SettlementSettlerData> list, ArrayList<SettlementWorkPrioritiesForm.JobTitlePosition> jobTypes) {
      for (SettlementSettlerData data : list) {
         int padding = SettlementSettlersForm.SETTLER_LIST_PADDING;
         int height = 32 + padding * 2;
         int startY = flow.next(height);
         int y = startY + padding;
         this.content.addComponent(new FormSettlerDataIcon(5, y, data, this.containerForm));
         int namesX = 40;
         int maxWidth = 238 - namesX - 48;
         FontOptions nameFontOptions = new FontOptions(16);
         this.content.addComponent(new FormSettlerDataNameLabel(this.client, data, nameFontOptions, FairType.TextAlign.LEFT, namesX, y, maxWidth));
         FontOptions activityFontOptions = new FontOptions(12);
         this.content.addComponent(new FormSettlerDataActivityLabel(this.client, data, activityFontOptions, namesX, y + 16, maxWidth));
         this.content
            .addComponent(
               new FormContentIconButton(
                  190, y, FormInputSize.SIZE_24, ButtonColor.BASE, this.getInterfaceStyle().copy_button, new LocalMessage("ui", "copybutton")
               )
            )
            .onClicked(e -> {
               if (data.workPrioritiesData != null) {
                  HashMap<JobType, SettlerWorkPrioritiesData.TypePriority> priorities = new HashMap<>();

                  for (Entry<JobType, SettlerWorkPrioritiesData.TypePriority> entry : data.workPrioritiesData.priorities.entrySet()) {
                     SettlerWorkPrioritiesData.TypePriority typePriorityx = entry.getValue();
                     if (!typePriorityx.disabledBySettler) {
                        priorities.put(entry.getKey(), typePriorityx);
                     }
                  }

                  SettlementWorkPrioritiesForm.PrioritiesData prioritiesData = new SettlementWorkPrioritiesForm.PrioritiesData(priorities);
                  SaveData save = new SaveData("jobs");
                  prioritiesData.addSaveData(save);
                  WindowManager.getWindow().putClipboard(save.getScript());
                  this.listClipboard.forceUpdate();
               }
            });
         FormContentIconButton pasteButton = this.content
            .addComponent(
               new FormContentIconButton(
                  214, y, FormInputSize.SIZE_24, ButtonColor.BASE, this.getInterfaceStyle().paste_button, new LocalMessage("ui", "pastebutton")
               )
            );
         pasteButton.onClicked(e -> {
            if (data.workPrioritiesData != null) {
               SettlementWorkPrioritiesForm.PrioritiesData prioritiesData = this.listClipboard.getValue();
               if (prioritiesData != null) {
                  for (Entry<JobType, SettlerWorkPrioritiesData.TypePriority> entry : prioritiesData.priorities.entrySet()) {
                     SettlerWorkPrioritiesData.TypePriority current = data.workPrioritiesData.priorities.get(entry.getKey());
                     if (current != null) {
                        SettlerWorkPrioritiesData.TypePriority next = entry.getValue();
                        if (current.disabledByPlayer != next.disabledByPlayer || current.priority != next.priority) {
                           current.disabledByPlayer = next.disabledByPlayer;
                           current.priority = next.priority;
                           this.container.setSettlerPriority.runAndSend(data.mobUniqueID, entry.getKey(), current.priority, current.disabledByPlayer);
                        }
                     }
                  }
               }
            }
         });
         pasteButton.setupDragPressOtherButtons("workPrioritiesPasteButton");
         this.pasteButtons.add(new SettlementWorkPrioritiesForm.PasteButton(pasteButton, data));

         for (final SettlementWorkPrioritiesForm.JobTitlePosition jobTitle : jobTypes) {
            int buttonX = jobTitle.buttonX;
            final SettlerWorkPrioritiesData.TypePriority typePriority;
            if (data.workPrioritiesData != null) {
               typePriority = data.workPrioritiesData.priorities.getOrDefault(jobTitle.jobType, new SettlerWorkPrioritiesData.TypePriority(true, 0, false));
            } else {
               typePriority = new SettlerWorkPrioritiesData.TypePriority(true, 0, false);
            }

            FormContentIconValueButton<Integer> button = this.content
               .addComponent(
                  new FormContentIconValueButton<Integer>(buttonX - 12, y, FormInputSize.SIZE_24, ButtonColor.BASE) {
                     @Override
                     public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                        if (!typePriority.disabledBySettler) {
                           int nextValue = typePriority.disabledByPlayer ? Integer.MIN_VALUE : typePriority.priority;
                           if ((Integer)this.getValue() != nextValue) {
                              this.setCurrent(
                                 nextValue,
                                 typePriority.disabledByPlayer
                                    ? this.getInterfaceStyle().priority_disabled
                                    : JobPriority.getJobPriority(typePriority.priority).icon.get()
                              );
                           }
                        }

                        super.draw(tickManager, perspective, renderBox);
                     }

                     @Override
                     public GameTooltips getTooltips() {
                        StringTooltips out = new StringTooltips();
                        out.add(jobTitle.jobType.displayName.translate());
                        if (jobTitle.jobType.tooltip != null) {
                           out.add(jobTitle.jobType.tooltip.translate(), 400);
                        }

                        if (typePriority.disabledBySettler) {
                           out.add(Localization.translate("jobs", "settlerincapable"));
                        } else if (typePriority.disabledByPlayer) {
                           out.add(Localization.translate("ui", "prioritydisabled"));
                        } else {
                           out.add(JobPriority.getJobPriority(typePriority.priority).displayName.translate());
                        }

                        return out;
                     }
                  }
               );
            button.setActive(!typePriority.disabledBySettler);
            if (!typePriority.disabledBySettler) {
               button.setCurrent(
                  typePriority.disabledByPlayer ? Integer.MIN_VALUE : typePriority.priority,
                  typePriority.disabledByPlayer ? this.getInterfaceStyle().priority_disabled : JobPriority.getJobPriority(typePriority.priority).icon.get()
               );
            }

            button.acceptRightClicks = true;
            button.onClicked(e -> {
               if (e.event.getID() != -100 && (!e.event.isControllerEvent() || e.event.getControllerEvent().getState() != ControllerInput.MENU_SELECT)) {
                  if (e.event.getID() == -99 || e.event.isControllerEvent() && e.event.getControllerEvent().getState() == ControllerInput.MENU_BACK) {
                     if (typePriority.disabledByPlayer) {
                        typePriority.disabledByPlayer = false;
                        typePriority.priority = JobPriority.priorities.first().priority;
                     } else {
                        JobPriority last = JobPriority.getJobPriority(typePriority.priority);
                        NavigableSet<JobPriority> next = JobPriority.priorities.tailSet(last, false);
                        if (next.isEmpty()) {
                           typePriority.disabledByPlayer = true;
                           typePriority.priority = JobPriority.priorities.last().priority;
                        } else {
                           typePriority.priority = next.first().priority;
                        }
                     }

                     this.container.setSettlerPriority.runAndSend(data.mobUniqueID, jobTitle.jobType, typePriority.priority, typePriority.disabledByPlayer);
                  }
               } else {
                  if (typePriority.disabledByPlayer) {
                     typePriority.disabledByPlayer = false;
                     typePriority.priority = JobPriority.priorities.last().priority;
                  } else {
                     JobPriority last = JobPriority.getJobPriority(typePriority.priority);
                     NavigableSet<JobPriority> next = JobPriority.priorities.headSet(last, false);
                     if (next.isEmpty()) {
                        typePriority.disabledByPlayer = true;
                        typePriority.priority = JobPriority.priorities.first().priority;
                     } else {
                        typePriority.priority = next.last().priority;
                     }
                  }

                  this.container.setSettlerPriority.runAndSend(data.mobUniqueID, jobTitle.jobType, typePriority.priority, typePriority.disabledByPlayer);
               }
            });
            button.setupDragToOtherButtons("workPrioritiesToggleButton", false, value -> {
               if (value == Integer.MIN_VALUE) {
                  typePriority.disabledByPlayer = true;
                  typePriority.priority = JobPriority.priorities.first().priority;
               } else {
                  typePriority.disabledByPlayer = false;
                  typePriority.priority = value;
               }

               this.container.setSettlerPriority.runAndSend(data.mobUniqueID, jobTitle.jobType, typePriority.priority, typePriority.disabledByPlayer);
               return true;
            });
         }
      }
   }

   public void updateSize() {
      this.setHeight(Math.min(this.maxHeight, this.content.getY() + this.contentHeight));
      this.content.setContentBox(new Rectangle(0, 0, Math.max(this.getWidth(), this.contentWidth), this.contentHeight));
      this.content.setHeight(this.getHeight() - this.content.getY());
      ContainerComponent.setPosInventory(this);
   }

   @Override
   public void onWindowResized(GameWindow window) {
      super.onWindowResized(window);
      this.updateSize();
   }

   @Override
   public void onSetCurrent(boolean current) {
      this.hudElements.forEach(HudDrawElement::remove);
      this.hudElements.clear();
      if (SettlementSettlersForm.lastSettlersSearch != null) {
         this.searchInput.setText(SettlementSettlersForm.lastSettlersSearch, false);
      }

      if (current) {
         this.container.prioritiesSubscription.ensureSubscribed();
      } else {
         this.container.prioritiesSubscription.ensureUnsubscribed();
      }
   }

   @Override
   public void onMenuButtonClicked(FormSwitcher switcher) {
      this.setCurrentWhenLoaded = switcher;
      this.container.prioritiesSubscription.ensureSubscribed();
   }

   @Override
   public void dispose() {
      this.hudElements.forEach(HudDrawElement::remove);
      this.hudElements.clear();
      lastScroll.save(this.content);
      super.dispose();
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      this.listClipboard.update();
      super.draw(tickManager, perspective, renderBox);
   }

   @Override
   public GameMessage getMenuButtonName() {
      return new LocalMessage("ui", "settlementworkpriorities");
   }

   @Override
   public String getTypeString() {
      return "workpriorities";
   }

   protected static class JobTitlePosition {
      public final JobType jobType;
      public final int buttonX;

      public JobTitlePosition(JobType jobType, int buttonX) {
         this.jobType = jobType;
         this.buttonX = buttonX;
      }
   }

   public static class PasteButton {
      public final FormContentIconButton button;
      public final SettlementSettlerData data;

      public PasteButton(FormContentIconButton button, SettlementSettlerData data) {
         this.button = button;
         this.data = data;
      }

      public void updateActive(SettlementWorkPrioritiesForm.PrioritiesData next) {
         if (this.data.workPrioritiesData == null) {
            this.button.setActive(false);
         } else {
            if (next != null && next.priorities != null) {
               for (Entry<JobType, SettlerWorkPrioritiesData.TypePriority> entry : this.data.workPrioritiesData.priorities.entrySet()) {
                  SettlerWorkPrioritiesData.TypePriority mine = entry.getValue();
                  SettlerWorkPrioritiesData.TypePriority their = next.priorities.get(entry.getKey());
                  if (!mine.disabledBySettler
                     && their != null
                     && !their.disabledBySettler
                     && (their.disabledByPlayer != mine.disabledByPlayer || their.priority != mine.priority)) {
                     this.button.setActive(true);
                     return;
                  }
               }
            }

            this.button.setActive(false);
         }
      }
   }

   public static class PrioritiesData {
      public final HashMap<JobType, SettlerWorkPrioritiesData.TypePriority> priorities;

      public PrioritiesData(HashMap<JobType, SettlerWorkPrioritiesData.TypePriority> priorities) {
         this.priorities = priorities;
      }

      public PrioritiesData(LoadData save) {
         this.priorities = new HashMap<>();

         for (LoadData data : save.getLoadData()) {
            if (data.isArray()) {
               String stringID = data.getName();
               int priority = data.getInt("priority");
               boolean disabled = data.getBoolean("disabled");
               JobType jobType = JobTypeRegistry.getJobType(stringID);
               if (jobType != null) {
                  this.priorities.put(jobType, new SettlerWorkPrioritiesData.TypePriority(false, priority, disabled));
               }
            }
         }
      }

      public void addSaveData(SaveData save) {
         for (Entry<JobType, SettlerWorkPrioritiesData.TypePriority> e : this.priorities.entrySet()) {
            SaveData priorityData = new SaveData(e.getKey().getStringID());
            priorityData.addInt("priority", e.getValue().priority);
            priorityData.addBoolean("disabled", e.getValue().disabledByPlayer);
            save.addSaveData(priorityData);
         }
      }
   }
}
