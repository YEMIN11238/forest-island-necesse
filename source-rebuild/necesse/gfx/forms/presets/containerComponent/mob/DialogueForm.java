package necesse.gfx.forms.presets.containerComponent.mob;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modifiers.ModifierTooltip;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.IDDataContainer;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.GameBackground;
import necesse.gfx.GameBackgroundPaddingWrapper;
import necesse.gfx.GameBackgroundWrapper;
import necesse.gfx.GameColor;
import necesse.gfx.drawOptions.ChatBubbleDrawOptions;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.PositionedDrawOptionsBox;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.ComponentListContainer;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.FormSwitchedComponent;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormCustomDraw;
import necesse.gfx.forms.components.FormDialogueOption;
import necesse.gfx.forms.components.FormFairTypeLabel;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormIcon;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormMouseHover;
import necesse.gfx.forms.components.FormSettlerIconAbstract;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.BackgroundedGameTooltips;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.HorizontalListGameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.SpacerGameTooltip;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.gfx.ui.GameInterfaceStyle;
import necesse.level.maps.levelData.settlementData.settler.QualityOfLifeDescriptionAbstract;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import necesse.level.maps.levelData.settlementData.settler.personalities.SettlerPersonality;

public class DialogueForm extends Form implements FormSwitchedComponent {
   public int minHeight;
   public int maxHeight;
   public FormContentBox content;
   public FormFlow flow;
   public int optionCounter;
   private boolean shouldAddChatBubble = false;
   private FormCustomDraw dialogueOptionsChatBubble;
   private final ArrayList<FormDialogueOption> dialogueOptions = new ArrayList<>();

   public DialogueForm(String name, int width, int minHeight, int maxHeight, boolean addDialogueOptionsChatBubble) {
      super(name, width, minHeight);
      this.minHeight = minHeight;
      this.maxHeight = maxHeight;
      this.shouldAddChatBubble = addDialogueOptionsChatBubble;
      this.content = this.addComponent(new FormContentBox(0, 0, width, minHeight));
      this.optionCounter = 1;
      this.flow = new FormFlow(5);
   }

   public DialogueForm(
      String name,
      int width,
      int minHeight,
      int maxHeight,
      boolean addDialogueOptionsChatBubble,
      HumanMob humanMob,
      boolean showPersonality,
      int romanceLevel,
      GameMessage intro
   ) {
      this(name, width, minHeight, maxHeight, addDialogueOptionsChatBubble);
      this.reset(humanMob, showPersonality, romanceLevel, intro);
   }

   public void reset(BiConsumer<FormContentBox, FormFlow> adder) {
      this.dialogueOptions.clear();
      this.content.clearComponents();
      this.dialogueOptionsChatBubble = null;
      this.optionCounter = 1;
      this.flow = new FormFlow(5);
      adder.accept(this.content, this.flow);
      this.flow.next(10);
      this.updateContentAndHeight();
   }

   public static void addSettlerHeader(
      final HumanMob humanMob, boolean showPersonality, int romanceLevel, int maxWidth, ComponentListContainer<? super FormComponent> container, FormFlow flow
   ) {
      int startX = 10;
      int startY = flow.next();
      int iconEndY = 0;
      if (!humanMob.shouldIgnoreQualityOfLifeAndHappiness() && humanMob.isSettler()) {
         final int qualityOfLifeLevel = humanMob.getSettlerQualityOfLife();
         if (qualityOfLifeLevel >= 0) {
            final QualityOfLifeDescriptionAbstract qualityOfLife = Settler.getQualityOfLife(qualityOfLifeLevel);
            GameInterfaceStyle interfaceStyle = ((FormComponent)container).getInterfaceStyle();
            final PositionedDrawOptionsBox iconDrawOptions = qualityOfLife.getIconDrawOptions(qualityOfLifeLevel, interfaceStyle);
            Rectangle boundingBox = iconDrawOptions.getBoundingBox(0, 0);
            int width = boundingBox.x + boundingBox.width;
            int height = boundingBox.y + boundingBox.height;
            container.addComponent(
               new FormCustomDraw(maxWidth - width - 4, startY, width, height) {
                  @Override
                  public void addNextControllerFocus(
                     List<ControllerFocus> list,
                     int currentXOffset,
                     int currentYOffset,
                     ControllerNavigationHandler customNavigationHandler,
                     Rectangle area,
                     boolean draw
                  ) {
                     ControllerFocus.add(
                        list, area, this, this.getBoundingBox(), currentXOffset, currentYOffset, this.controllerInitialFocusPriority, customNavigationHandler
                     );
                  }

                  @Override
                  public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                     iconDrawOptions.draw(this.getX(), this.getY());
                     if (this.isHovering()) {
                        ListGameTooltips tooltips = new ListGameTooltips();
                        tooltips.add(qualityOfLife.getDescription());
                        tooltips.add(new SpacerGameTooltip(10));
                        tooltips.add(new LocalMessage("settlement", "qoltip1"), 400);
                        tooltips.add(new SpacerGameTooltip(10));
                        tooltips.add(new LocalMessage("settlement", "qoltip2"), 400);

                        for (ModifierValue<?> qolModifier : HumanMob.QOL_MODIFIERS) {
                           tooltips.add(qolModifier.getTooltip(true).toTooltip(true));
                        }

                        tooltips.add(new SpacerGameTooltip(10));
                        tooltips.add(new LocalMessage("settlement", "qoltip3"), 400);

                        for (ModifierValue<?> qolModifier : HumanMob.QOL_MODIFIERS) {
                           ModifierTooltip tooltip = qolModifier.stack(qualityOfLifeLevel).getTooltip(true);
                           tooltips.add(tooltip.toTooltip(true));
                        }

                        GameTooltipManager.addTooltip(tooltips, GameBackground.itemTooltip, TooltipLocation.FORM_FOCUS);
                     }
                  }
               }
            );
            maxWidth -= 4 + width + 4;
            iconEndY = Math.max(startY + height, iconEndY);
         }
      }

      startY += 5;
      container.addComponent(new FormSettlerIconAbstract(5, startY) {
         @Override
         public DrawOptions getFaceDrawOptions(int size, int drawX, int drawY) {
            return Settler.getHumanFaceDrawOptions(humanMob.getCommandFaceDrawOptions(), 32, drawX, drawY);
         }

         @Override
         public GameTooltips getTooltips() {
            return null;
         }
      });
      iconEndY = Math.max(startY + 30, iconEndY);
      int var16 = 45;
      if (romanceLevel > 0) {
         maxWidth -= 26;
      }

      String mainHeader = GameUtils.maxString(humanMob.getDisplayName(), new FontOptions(20), maxWidth - var16 - 5);
      FormLabel label = container.addComponent(new FormLabel(mainHeader, new FontOptions(20), -1, var16, startY));
      startY += label.getBoundingBox().height + 2;
      if (romanceLevel > 0) {
         int heartIconX = label.getBoundingBox().x + label.getBoundingBox().width + 5;
         final GameMessage datingTooltip = humanMob.getRomancePersonality().getDatingTooltipMessageBasedOnRomanceLevel(romanceLevel);
         FormIcon formIcon = new FormIcon(heartIconX, label.getY() - 2, 26, 22, GameInterfaceStyle.primal.heart_fill, Color.WHITE) {
            @Override
            public void addNextControllerFocus(
               List<ControllerFocus> list,
               int currentXOffset,
               int currentYOffset,
               ControllerNavigationHandler customNavigationHandler,
               Rectangle area,
               boolean draw
            ) {
               ControllerFocus.add(
                  list, area, this, this.getBoundingBox(), currentXOffset, currentYOffset, this.controllerInitialFocusPriority, customNavigationHandler
               );
            }

            @Override
            public GameTooltips getTooltips() {
               if (datingTooltip != null) {
                  StringTooltips tooltips = new StringTooltips();
                  tooltips.add(datingTooltip.translate(), 350);
                  return tooltips;
               } else {
                  return null;
               }
            }
         };
         container.addComponent(formIcon);
      }

      if (showPersonality) {
         ArrayList<DialogueForm.PersonalityData> personalityData = getPersonalityData(humanMob);
         ArrayList<Form> personalityLabels = new ArrayList<>(personalityData.size());

         for (DialogueForm.PersonalityData data : personalityData) {
            personalityLabels.add(data.toFormLabel(GameBackground.textBox, Settings.UI.textBoxTextColor, Settings.UI.incursionTierPurple));
         }

         for (Form personalityLabel : personalityLabels) {
            container.addComponent(personalityLabel);
         }

         startY = assignPersonalityLabelPositions(personalityLabels, var16, startY, maxWidth - 10);
      }

      flow.next(Math.max(startY, iconEndY) - flow.next() + 5);
   }

   public static ArrayList<DialogueForm.PersonalityData> getPersonalityData(HumanMob humanMob) {
      ArrayList<SettlerPersonality> personalities = new ArrayList<>(humanMob.getPersonalities());
      Comparator<SettlerPersonality> comparator = Comparator.comparingInt(p -> p.isBonusPerk() ? -1 : 0);
      comparator = comparator.thenComparingInt(IDDataContainer::getID);
      personalities.sort(comparator);
      ArrayList<DialogueForm.PersonalityData> personalityLabels = new ArrayList<>(personalities.size());

      for (SettlerPersonality personality : personalities) {
         personalityLabels.add(new DialogueForm.PersonalityData(personality.getName(), personality.isBonusPerk(), personality::getTooltip));
      }

      String relationshipName = humanMob.getRomancePersonality().getInRelationshipName();
      if (relationshipName != null) {
         LocalMessage title = new LocalMessage("romance", "lifepartner");
         LocalMessage message = new LocalMessage("romance", "lifepartner_desc")
            .addReplacement("settler", GameColor.ITEM_QUEST.getColorCode() + humanMob.getSettlerName() + GameColor.NO_COLOR.getColorCode())
            .addReplacement("partner", GameColor.ITEM_QUEST.getColorCode() + relationshipName + GameColor.NO_COLOR.getColorCode());
         StringTooltips tooltip = new StringTooltips(message.translate(), 400);
         personalityLabels.add(new DialogueForm.PersonalityData(title, false, () -> tooltip));
      }

      return personalityLabels;
   }

   public static int assignPersonalityLabelPositions(ArrayList<Form> personalityLabels, int startX, int startY, int maxWidth) {
      boolean firstOnLine = true;
      int currentX = startX;
      int currentY = startY;

      for (Form personality : personalityLabels) {
         if (!firstOnLine && currentX + personality.getWidth() > maxWidth) {
            currentY = startY;
            currentX = startX;
            firstOnLine = true;
         }

         if (firstOnLine) {
            personality.setPosition(currentX, currentY);
            currentX += personality.getWidth() + 4;
            firstOnLine = false;
            startY = Math.max(currentY + personality.getHeight() + 4, startY);
         } else {
            personality.setPosition(currentX, currentY);
            currentX += personality.getWidth() + 4;
            startY = Math.max(currentY + personality.getHeight() + 4, startY);
         }
      }

      return startY;
   }

   public static GameTooltips toHorizontalOverviewTooltips(
      ArrayList<DialogueForm.PersonalityData> personalityData, GameBackground background, Color starColor, int maxWidth
   ) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.yMargin = 6;
      HorizontalListGameTooltips currentHorizontalTips = new HorizontalListGameTooltips();
      currentHorizontalTips.xMargin = 6;
      new FontOptions(Settings.tooltipTextSize);

      for (DialogueForm.PersonalityData data : personalityData) {
         StringTooltips titleTooltip = new StringTooltips(data.getTitleFairTypeParseString(starColor).translate());
         if (background != null) {
            currentHorizontalTips.add(new BackgroundedGameTooltips(titleTooltip, background));
         } else {
            currentHorizontalTips.add(titleTooltip);
         }

         if (currentHorizontalTips.getWidth() >= maxWidth) {
            tooltips.add(currentHorizontalTips);
            currentHorizontalTips = new HorizontalListGameTooltips();
            currentHorizontalTips.xMargin = 6;
         }
      }

      tooltips.add(currentHorizontalTips);
      return tooltips;
   }

   public void reset(HumanMob humanMob, boolean showPersonality, int romanceLevel, BiConsumer<FormContentBox, FormFlow> adder) {
      this.reset((content, flow) -> {
         addSettlerHeader(humanMob, showPersonality, romanceLevel, this.getWidth(), content, flow);
         if (adder != null) {
            adder.accept(content, flow);
         }
      });
   }

   public void reset(HumanMob humanMob, boolean showPersonality, int romanceLevel, GameMessage intro) {
      this.reset(humanMob, showPersonality, romanceLevel, (content, flow) -> {
         if (intro != null) {
            Runnable chatBubble = startChatBubble(content, flow);
            addText(content, flow, intro, true);
            chatBubble.run();
         }
      });
   }

   public void reset(GameMessage header, BiConsumer<FormContentBox, FormFlow> adder) {
      this.reset((content, flow) -> {
         if (header != null) {
            String mainHeader = GameUtils.maxString(header.translate(), new FontOptions(20), this.getWidth() - 15);
            content.addComponent(flow.nextY(new FormLabel(mainHeader, new FontOptions(20), -1, 10, 0), 5));
         }

         if (adder != null) {
            adder.accept(content, flow);
         }
      });
   }

   public void reset(GameMessage header) {
      this.reset(header, (BiConsumer<FormContentBox, FormFlow>)null);
   }

   public void reset(GameMessage header, GameMessage intro, BiConsumer<FormContentBox, FormFlow> adder) {
      this.reset(header, (content, flow) -> {
         if (intro != null) {
            addText(content, flow, intro, false);
         }

         if (adder != null) {
            adder.accept(content, flow);
         }
      });
   }

   public void reset(GameMessage header, GameMessage intro) {
      this.reset(header, intro, null);
   }

   public static Runnable startChatBubble(FormContentBox content, FormFlow flow) {
      int startY = flow.next(10);
      return () -> {
         flow.next(10);
         int height = flow.next() - startY;
         content.addComponent(new FormCustomDraw(5, startY, content.getWidth() - 20, height) {
            @Override
            public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
               new ChatBubbleDrawOptions(this.width, this.height).setTopIndicator(20, false).pos(this.getX(), this.getY()).draw();
            }
         }, -10);
      };
   }

   public static Runnable startThoughtBubble(FormContentBox content, FormFlow flow) {
      int startY = flow.next(10);
      return () -> {
         flow.next(15);
         int height = flow.next() - startY;
         content.addComponent(
            new FormCustomDraw(5, startY, content.getWidth() - 20, height) {
               @Override
               public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                  new ChatBubbleDrawOptions(this.getInterfaceStyle().thoughtbubble, this.width, this.height)
                     .setTopIndicator(20, false)
                     .pos(this.getX(), this.getY())
                     .draw();
               }
            },
            -10
         );
      };
   }

   public static FormFairTypeLabel addText(FormContentBox content, FormFlow flow, GameMessage text, int x, int width, boolean insideChatBubble) {
      return addText(content, flow, text, new FontOptions(16), x, width, insideChatBubble);
   }

   public static FormFairTypeLabel addText(
      FormContentBox content, FormFlow flow, GameMessage text, FontOptions fontOptions, int x, int width, boolean insideChatBubble
   ) {
      FormFairTypeLabel fairType = content.addComponent(new FormFairTypeLabel(text, x, 0));
      fairType.setMaxWidth(width);
      if (insideChatBubble) {
         fairType.setColor(fairType.getInterfaceStyle().chatBubbleActiveTextColor);
      }

      fairType.setFontOptions(fontOptions);
      fairType.setParsers(
         TypeParsers.GAME_COLOR,
         TypeParsers.InputIcon(fontOptions),
         TypeParsers.ItemIcon(fontOptions.getSize()),
         TypeParsers.MobIcon(fontOptions.getSize()),
         TypeParsers.highlightParser("**", "**", true, () -> Settings.UI.incursionModifierOrange),
         TypeParsers.GameEmote(fontOptions.getSize()),
         TypeParsers.lineHeightParser(fontOptions.getSize())
      );
      fairType.setLineSpacing(4);
      flow.nextY(fairType);
      return fairType;
   }

   public static FormFairTypeLabel addText(FormContentBox content, FormFlow flow, GameMessage text, boolean insideChatBubble) {
      return addText(content, flow, text, 15, content.getWidth() - 35, insideChatBubble);
   }

   public static FormFairTypeLabel addText(FormContentBox content, FormFlow flow, GameMessage text, FontOptions fontOptions, boolean insideChatBubble) {
      return addText(content, flow, text, fontOptions, 15, content.getWidth() - 35, insideChatBubble);
   }

   public FormDialogueOption addDialogueOption(GameMessage message, Runnable onClick) {
      return this.addDialogueOption(this.optionCounter++, message, onClick);
   }

   public FormDialogueOption addDialogueOption(int optionNumber, GameMessage message, Runnable onClick) {
      FormDialogueOption dialogueOption = this.addDialogueOption(
         new FormDialogueOption(optionNumber, this.shouldAddChatBubble, message, new FontOptions(16), 0, 0, this.getWidth() - 40)
      );
      dialogueOption.onClicked(e -> onClick.run());
      return dialogueOption;
   }

   public FormDialogueOption addDialogueOption(FormDialogueOption dialogueOption) {
      if (this.dialogueOptionsChatBubble == null && this.shouldAddChatBubble) {
         int startY = this.flow.next(10);
         this.dialogueOptionsChatBubble = this.content.addComponent(new FormCustomDraw(5, startY, this.content.getWidth() - 20, 0) {
            @Override
            public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
               if (this.height > 0) {
                  new ChatBubbleDrawOptions(this.width, this.height).setBotIndicator(this.width - 20, false).pos(this.getX(), this.getY()).draw();
               }
            }
         }, -10);
      }

      dialogueOption.setPosition(20, 0);
      dialogueOption.setMaxWidth(this.getWidth() - 40);
      dialogueOption.setParsers(
         TypeParsers.GAME_COLOR,
         TypeParsers.InputIcon(dialogueOption.fontOptions),
         TypeParsers.ItemIcon(dialogueOption.fontOptions.getSize()),
         TypeParsers.MobIcon(dialogueOption.fontOptions.getSize()),
         TypeParsers.highlightParser("**", "**", true, () -> Settings.UI.incursionModifierOrange),
         TypeParsers.GameEmote(dialogueOption.fontOptions.getSize()),
         TypeParsers.lineHeightParser(dialogueOption.fontOptions.getSize())
      );
      this.content.addComponent(this.flow.nextY(dialogueOption, 6));
      this.dialogueOptions.add(dialogueOption);
      this.updateContentAndHeight();
      return dialogueOption;
   }

   public void updateContentAndHeight() {
      if (this.dialogueOptionsChatBubble != null) {
         this.dialogueOptionsChatBubble.height = Math.max(this.flow.next() + 5, this.content.getHeight() - 15) - this.dialogueOptionsChatBubble.getY();
         int contentHeight = this.dialogueOptionsChatBubble.getY() + this.dialogueOptionsChatBubble.height + 20;
         this.content.setContentBox(new Rectangle(0, 0, this.getWidth(), contentHeight));
         this.setHeight(GameMath.limit(contentHeight, this.minHeight, this.maxHeight));
      } else {
         this.content.setContentBox(new Rectangle(0, 0, this.getWidth(), this.flow.next() + 5));
         this.setHeight(GameMath.limit(this.flow.next() + 5, this.minHeight, this.maxHeight));
      }
   }

   public int getContentHeight() {
      return this.flow.next();
   }

   @Override
   public void setHeight(int height) {
      super.setHeight(height);
      this.content.setHeight(height);
   }

   @Override
   public void onSwitched(boolean active) {
      if (active) {
         this.setNextControllerFocus(this.dialogueOptions.toArray(new FormDialogueOption[0]));
      }
   }

   public static class PersonalityData {
      public GameMessage title;
      public boolean addStar;
      public Supplier<GameTooltips> tooltipsSupplier;

      public PersonalityData(GameMessage title, boolean addStar, Supplier<GameTooltips> tooltipsSupplier) {
         this.title = title;
         this.addStar = addStar;
         this.tooltipsSupplier = tooltipsSupplier;
      }

      public Form toFormLabel(GameBackground background, Color titleColor, final Color starColor) {
         Form form = new Form(10, 10);
         int currentX = 5;
         if (this.addStar && starColor != null) {
            final GameTexture starIcon = form.getInterfaceStyle().star_icon;
            form.addComponent(new FormCustomDraw(currentX, 2, starIcon.getWidth(), starIcon.getHeight()) {
               @Override
               public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                  starIcon.initDraw().color(starColor).draw(this.getX(), this.getY());
               }
            });
            currentX += starIcon.getWidth() + 2;
         }

         FormLabel label = form.addComponent(new FormLabel(this.title.translate(), new FontOptions(16), -1, currentX, 2));
         form.setBackground(background);
         label.setColor(titleColor);
         Rectangle boundingBox = label.getBoundingBox();
         form.setWidth(boundingBox.x + boundingBox.width + 10);
         form.setHeight(20);
         form.addComponent(new FormMouseHover(0, 0, form.getWidth(), form.getHeight(), false, true) {
            @Override
            public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
               super.draw(tickManager, perspective, renderBox);
               if (this.isHovering()) {
                  GameTooltips tooltips = PersonalityData.this.tooltipsSupplier.get();
                  if (tooltips != null) {
                     GameBackgroundWrapper background = new GameBackgroundPaddingWrapper(GameBackgroundWrapper.itemTooltip, 2);
                     GameTooltipManager.addTooltip(tooltips, background, TooltipLocation.FORM_FOCUS);
                  }
               }
            }
         }, 1000);
         return form;
      }

      public GameMessage getTitleFairTypeParseString(Color starColor) {
         return !this.addStar
            ? this.title
            : new GameMessageBuilder()
               .append(GameColor.getCustomColorCode(starColor))
               .append(TypeParsers.getGameEmoteParseString("star"))
               .append(GameColor.NO_COLOR.getColorCode())
               .append(this.title);
      }
   }
}
