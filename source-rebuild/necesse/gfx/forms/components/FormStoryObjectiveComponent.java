package necesse.gfx.forms.components;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import necesse.engine.Settings;
import necesse.engine.achievements.Achievement;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameBackground;
import necesse.gfx.drawOptions.DrawOptionsBox;
import necesse.gfx.fairType.FairItemGlyph;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.Form;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTable;

public class FormStoryObjectiveComponent extends Form {
   public final StoryObjective quest;
   public final boolean isInForm;

   public FormStoryObjectiveComponent(int width, StoryObjective quest, boolean isInForm) {
      super("story_" + quest.getStringID(), width, 10);
      this.quest = quest;
      this.isInForm = isInForm;
      if (isInForm) {
         this.setBackground(GameBackground.textBox);
      } else {
         this.drawBase = false;
      }

      this.reset();
   }

   public void reset() {
      this.clearComponents();
      FormFlow flow = new FormFlow(5);
      this.quest.setupFormComponent(this, flow);
      flow.next(5);
      this.setHeight(flow.next());
   }

   public FontOptions getDefaultFontOptions(int size) {
      FontOptions fontOptions = new FontOptions(size);
      if (!this.isInForm) {
         fontOptions.outline();
      }

      return fontOptions;
   }

   public Color getDefaultTextColor() {
      return this.isInForm ? this.getInterfaceStyle().textBoxTextColor : new Color(220, 220, 220);
   }

   public FormFairTypeLabel addText(FormFlow flow, int startX, GameMessage message, int textSize, boolean faded) {
      FontOptions fontOptions = this.getDefaultFontOptions(textSize);
      FormFairTypeLabel label = this.addComponent(new FormFairTypeLabel(message, startX, 0));
      label.setMaxWidth(this.getWidth() - startX - 10);
      label.setFontOptions(fontOptions);
      Color color = this.getDefaultTextColor();
      if (faded) {
         label.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
      } else {
         label.setColor(color);
      }

      label.setParsers(
         TypeParsers.GAME_COLOR,
         TypeParsers.InputIcon(fontOptions),
         TypeParsers.ItemIcon(fontOptions.getSize()),
         TypeParsers.MobIcon(fontOptions.getSize()),
         TypeParsers.GameEmote(fontOptions.getSize())
      );
      flow.nextY(label);
      return label;
   }

   public FormFairTypeLabel addText(FormFlow flow, GameMessage message, int textSize, boolean faded) {
      return this.addText(flow, 10, message, textSize, faded);
   }

   public FormFairTypeLabel addText(FormFlow flow, GameMessage message, int textSize) {
      return this.addText(flow, 10, message, textSize, false);
   }

   public FormFairTypeLabel addTitle(FormFlow flow, GameMessage message) {
      return this.addText(flow, 10, message, 20, false);
   }

   public FormFairTypeLabel addObjective(FormFlow flow, int number, GameMessage message, boolean completed) {
      flow.next(2);
      FormFairTypeLabel label = this.addText(flow, 42, number > 0 ? new GameMessageBuilder().append(number + ". ").append(message) : message, 16, completed);
      GameTexture icon = completed ? this.getInterfaceStyle().button_checked_20.texture : this.getInterfaceStyle().button_escaped_20.texture;
      FairTypeDrawOptions labelDrawOptions = label.drawOptions.get();
      int lineHeight = labelDrawOptions.getLineHeightCeil(0);
      int iconY = label.getY() + lineHeight - 20 + 1;
      this.addComponent(new FormIcon(20, iconY, 20, 20, icon, 1.0F));
      return label;
   }

   public FormFairTypeLabel addObjectiveNote(FormFlow flow, GameMessage message, boolean completed) {
      flow.next(2);
      return this.addText(flow, 40, message, 12, completed);
   }

   public void addProgressbar(FormFlow flow, int startX, final DoubleSupplier progress, final Supplier<String> textGetter) {
      final int fontSize = 16;
      this.addComponent(
         flow.nextY(
            new FormCustomDraw(startX, 0, this.getWidth() - startX - 10, fontSize) {
               @Override
               public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
                  float floatProgress = GameMath.limit((float)progress.getAsDouble(), 0.0F, 1.0F);
                  Color col = floatProgress >= 1.0F ? Settings.UI.successTextColor : Settings.UI.errorTextColor;
                  FontOptions fontOptions = FormStoryObjectiveComponent.this.getDefaultFontOptions(fontSize).color(col);
                  String text = textGetter.get();
                  DrawOptionsBox progressBox = Achievement.getProgressbarTextDrawBox(
                     this.getX(), this.getY(), this.width, 5, floatProgress, Settings.UI.progressBarOutline, Settings.UI.progressBarFill, text, fontOptions
                  );
                  progressBox.draw();
               }
            }
         )
      );
   }

   public void addProgressbar(FormFlow flow, DoubleSupplier progress, Supplier<String> textGetter) {
      this.addProgressbar(flow, 10, progress, textGetter);
   }

   public void addProgressbar(FormFlow flow, DoubleSupplier progress) {
      this.addProgressbar(flow, progress, () -> GameMath.limit((int)(progress.getAsDouble() * 100.0), 0, 100) + "%");
   }

   public void addProgressbar(FormFlow flow, IntSupplier currentValue, int maxValue) {
      this.addProgressbar(flow, () -> (double)currentValue.getAsInt() / maxValue, () -> Math.min(currentValue.getAsInt(), maxValue) + "/" + maxValue);
   }

   public void addCompleteText(FormFlow flow) {
      boolean completed = this.quest.isCompleted();
      flow.next(2);
      FormFairTypeLabel completedLabel = this.addText(flow, 20, new LocalMessage("quests", completed ? "complete" : "incomplete"), 16, false);
      completedLabel.setColor(completed ? this.getInterfaceStyle().successTextColor : this.getInterfaceStyle().errorTextColor);
      flow.next(2);
   }

   public void addRewardsText(FormFlow flow, LootList list) {
      StringBuilder builder = new StringBuilder();
      boolean isFirstReward = true;

      for (Item item : list.getItems()) {
         if (!isFirstReward) {
            builder.append(", ");
         }

         builder.append(TypeParsers.getItemParseString(new InventoryItem(item)));
         builder.append(" ").append(ItemRegistry.getDisplayName(item.getID()));
         isFirstReward = false;
      }

      if (builder.length() != 0) {
         FontOptions rewardFontOptions = this.getDefaultFontOptions(12);
         FairType fairType = new FairType();
         fairType.append(rewardFontOptions, Localization.translate("quests", "reward", "reward", builder.toString()));
         fairType.applyParsers(TypeParsers.ItemIcon(rewardFontOptions.getSize(), true, FairItemGlyph::onlyShowNameTooltip));
         FormFairTypeLabel label = this.addComponent(new FormFairTypeLabel("", 10, 0));
         label.setMaxWidth(this.getWidth() - 20);
         label.setColor(this.getDefaultTextColor());
         label.setCustomFairType(fairType);
         flow.nextY(label);
      }
   }

   public void addRewardsText(FormFlow flow) {
      LootList lootlist = new LootList();
      LootTable rewards = this.quest.getRewards();
      if (rewards != null) {
         rewards.addPossibleLoot(lootlist);
         this.addRewardsText(flow, lootlist);
      }
   }

   public void addClaimText(FormFlow flow) {
      if (this.quest.isCompleted()) {
         this.addText(flow, new LocalMessage("quests", "claimrewardelder"), 16);
      }
   }
}
