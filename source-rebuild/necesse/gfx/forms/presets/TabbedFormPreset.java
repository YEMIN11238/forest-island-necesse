package necesse.gfx.forms.presets;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.util.GameMath;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.FormSwitcher;
import necesse.gfx.forms.components.FormComponentList;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormTabContentComponent;
import necesse.gfx.forms.components.FormTabTextComponent;
import necesse.gfx.forms.components.localComponents.FormTabLocalTextComponent;
import necesse.gfx.forms.events.FormEvent;
import necesse.gfx.forms.events.FormEventListener;
import necesse.gfx.forms.events.FormEventsHandler;

public class TabbedFormPreset extends FormComponentList {
   public final Form form;
   protected TabbedFormPreset.TabStyle tabStyle;
   protected final int defaultWidth;
   protected final int defaultHeight;
   protected final int tabSpacing;
   protected final FormSwitcher formSwitcher;
   protected final List<TabbedFormPreset.TabData> tabs;
   protected int currentTabIndex = -1;
   protected boolean tabsDirty = true;
   protected FormEventsHandler<FormEvent<TabbedFormPreset>> tabChangedEvents = new FormEventsHandler<>();

   public TabbedFormPreset(int tabSpacing, TabbedFormPreset.TabStyle tabStyle) {
      this(tabSpacing, tabStyle, 0, 0);
   }

   public TabbedFormPreset(int tabSpacing, TabbedFormPreset.TabStyle tabStyle, int defaultWidth, int defaultHeight) {
      this.tabStyle = tabStyle;
      this.tabSpacing = tabSpacing;
      this.defaultWidth = defaultWidth;
      this.defaultHeight = defaultHeight;
      this.form = this.addComponent(new Form(defaultWidth, defaultHeight));
      this.formSwitcher = this.form.addComponent(new FormSwitcher());
      this.tabs = new ArrayList<>();
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      if (this.tabsDirty) {
         this.recalculateTabPositionAndWidth();
      }

      super.draw(tickManager, perspective, renderBox);
   }

   public Form addTab(String text, String tooltip) {
      return this.addTab(text, tooltip, this.defaultWidth, this.defaultHeight);
   }

   public Form addTab(String text, String tooltip, int width, int height) {
      FormTabTextComponent tab = this.addComponent(
         new FormTabTextComponent(text, tooltip, this.form, 0, FormInputSize.SIZE_20, 0) {
            @Override
            public boolean isSelected() {
               return !TabbedFormPreset.this.tabs.isEmpty()
                  && TabbedFormPreset.this.currentTabIndex >= 0
                  && TabbedFormPreset.this.tabs.get(TabbedFormPreset.this.currentTabIndex).tab == this;
            }
         }
      );
      return this.addTab(tab, width, height);
   }

   public Form addLocalizedTab(GameMessage text, GameMessage tooltip) {
      return this.addLocalizedTab(text, tooltip, this.defaultWidth, this.defaultHeight);
   }

   public Form addLocalizedTab(GameMessage text, GameMessage tooltip, int width, int height) {
      FormTabLocalTextComponent tab = this.addComponent(
         new FormTabLocalTextComponent(text, tooltip, this.form, 0, FormInputSize.SIZE_20) {
            @Override
            public boolean isSelected() {
               return !TabbedFormPreset.this.tabs.isEmpty()
                  && TabbedFormPreset.this.currentTabIndex >= 0
                  && TabbedFormPreset.this.tabs.get(TabbedFormPreset.this.currentTabIndex).tab == this;
            }
         }
      );
      return this.addTab(tab, width, height);
   }

   protected Form addTab(FormTabContentComponent tab, int width, int height) {
      TabbedFormPreset.TabData tabData = new TabbedFormPreset.TabData(tab, this.formSwitcher, width, height);
      this.tabs.add(tabData);
      tab.onClicked(e -> this.selectTab(this.tabs.indexOf(tabData)));
      tab.onWantedWidthChanged(newWidth -> this.tabsDirty = true);
      if (this.tabs.size() == 1) {
         this.selectTab(0);
      }

      this.tabsDirty = true;
      return tabData.content;
   }

   protected void recalculateTabPositionAndWidth() {
      int wantedTotalTabWidth = this.tabs.stream().reduce(0, (sum, tabx) -> sum + tabx.tab.getWantedWidth(), Integer::sum);
      int maxTotalTabWidth = this.form.getWidth() - (2 + this.tabSpacing) * (this.tabs.size() - 1);
      FormTabContentComponent[] sortedTabs = this.tabs
         .stream()
         .sorted(Comparator.comparing(o -> o.tab.getWantedWidth(), Comparator.reverseOrder()))
         .map(tabData -> tabData.tab)
         .toArray(FormTabContentComponent[]::new);

      for (int i = 0; i < sortedTabs.length; i++) {
         FormTabContentComponent tab = sortedTabs[i];
         if (this.tabStyle == TabbedFormPreset.TabStyle.Fill) {
            int tabSize = maxTotalTabWidth / this.tabs.size();
            int newTotalTabSize = tabSize * sortedTabs.length;
            int missingSpace = maxTotalTabWidth - newTotalTabSize;
            if (i < missingSpace) {
               tabSize++;
            }

            tab.setWidth(tabSize);
            wantedTotalTabWidth = maxTotalTabWidth;
         } else {
            tab.setWidth(tab.getWantedWidth());
         }
      }

      if (wantedTotalTabWidth > maxTotalTabWidth) {
         int savingsNeeded = wantedTotalTabWidth - maxTotalTabWidth;

         for (int i = 0; i < sortedTabs.length && savingsNeeded > 0; i++) {
            int currenTabWidth = sortedTabs[i].getWidth();
            int nextTabWidth = i < sortedTabs.length - 1 ? sortedTabs[i + 1].getWidth() : 1;
            int possibleSavings = (currenTabWidth - nextTabWidth) * (i + 1);
            int neededToSavePerTab = GameMath.ceil(Math.min(savingsNeeded, possibleSavings) / (i + 1.0F));
            int newTabWidth = currenTabWidth - neededToSavePerTab;
            int missingSpace = neededToSavePerTab * (i + 1) - savingsNeeded;

            for (int j = 0; j <= i; j++) {
               int extra = j < missingSpace ? 1 : 0;
               sortedTabs[j].setWidth(newTabWidth + extra);
               savingsNeeded -= neededToSavePerTab - extra;
               if (savingsNeeded <= 0) {
                  break;
               }
            }
         }
      }

      int nextTabX = 0;
      int totalTabWidth = GameMath.min(maxTotalTabWidth, wantedTotalTabWidth);
      switch (this.tabStyle) {
         case Right:
            nextTabX = maxTotalTabWidth - totalTabWidth;
            break;
         case Center:
            nextTabX = (maxTotalTabWidth - totalTabWidth) / 2;
      }

      for (TabbedFormPreset.TabData tab : this.tabs) {
         tab.tab.setX(nextTabX);
         nextTabX += tab.tab.getWidth() + 2 + this.tabSpacing;
      }

      this.tabsDirty = false;
   }

   public void selectTab(int index) {
      if (index >= 0 && index < this.tabs.size() && index != this.currentTabIndex) {
         this.currentTabIndex = index;
         FormTabContentComponent newTab = this.tabs.get(this.currentTabIndex).tab;
         Form newContent = this.tabs.get(this.currentTabIndex).content;
         this.formSwitcher.makeCurrent(newContent);
         this.form.setWidth(newContent.getWidth());
         this.form.setHeight(newContent.getHeight());
         int i = 0;

         while (i < this.tabs.size()) {
            this.tabs.get(i).tab.zIndex = i++;
         }

         newTab.zIndex = this.tabs.size();
         this.tabChangedEvents.onEvent(new FormEvent<>(this));
      }
   }

   public int getCurrentTabIndex() {
      return this.currentTabIndex;
   }

   public int getTabCount() {
      return this.tabs.size();
   }

   public TabbedFormPreset onTabChanged(FormEventListener<FormEvent<TabbedFormPreset>> listener) {
      this.tabChangedEvents.addListener(listener);
      return this;
   }

   protected static class TabData {
      public final FormTabContentComponent tab;
      public final Form content;

      public TabData(FormTabContentComponent tab, FormSwitcher formSwitcher, int width, int height) {
         this.tab = tab;
         this.content = new Form(width, height);
         this.content.drawBase = false;
         formSwitcher.addComponent(this.content);
      }
   }

   public enum TabStyle {
      Left,
      Right,
      Center,
      Fill;
   }
}
