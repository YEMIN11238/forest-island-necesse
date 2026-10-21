package necesse.gfx.forms.components.lists;

import java.awt.Color;
import java.util.ArrayList;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.util.GameUtils;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.level.maps.biomes.Biome;

public abstract class FormBiomeList extends FormGeneralList<FormBiomeList.BiomeElement> {
   private String filter;
   private ArrayList<FormBiomeList.BiomeElement> allElements;

   public FormBiomeList(int x, int y, int width, int height) {
      super(x, y, width, height, 20);
      this.setFilter("");
   }

   @Override
   public void reset() {
      super.reset();
      this.allElements = new ArrayList<>();
   }

   public void populateIfNotAlready() {
      if (this.allElements.isEmpty()) {
         for (Biome biome : BiomeRegistry.getBiomes()) {
            this.allElements.add(new FormBiomeList.BiomeElement(biome));
         }

         this.setFilter(this.filter);
         this.resetScroll();
      }
   }

   public void setFilter(String filter) {
      if (filter != null) {
         this.filter = filter;
         this.elements = new ArrayList<>();

         for (FormBiomeList.BiomeElement t : this.allElements) {
            if (t.biome.getDisplayName().contains(filter) || t.biome.getStringID().contains(filter)) {
               this.elements.add(t);
            }
         }

         this.limitMaxScroll();
      }
   }

   public abstract void onClicked(Biome var1);

   public class BiomeElement extends FormListElement<FormBiomeList> {
      public Biome biome;

      public BiomeElement(Biome biome) {
         this.biome = biome;
      }

      protected void draw(FormBiomeList parent, TickManager tickManager, PlayerMob perspective, int elementIndex) {
         Color col = this.isHovering() ? FormBiomeList.this.getInterfaceStyle().highlightTextColor : FormBiomeList.this.getInterfaceStyle().activeTextColor;
         String desc = this.biome.getID() + ":" + this.biome.getDisplayName();
         FontOptions options = new FontOptions(16).color(col);
         String str = GameUtils.maxString(desc, options, parent.width - 20);
         FontManager.bit.drawString(10.0F, 2.0F, str, options);
         if (this.isMouseOver(parent) && !str.equals(desc)) {
            GameTooltipManager.addTooltip(new StringTooltips(desc), TooltipLocation.FORM_FOCUS);
         }
      }

      protected void onClick(FormBiomeList parent, int elementIndex, InputEvent event, PlayerMob perspective) {
         if (event.getID() == -100) {
            FormBiomeList.this.playTickSound();
            FormBiomeList.this.onClicked(this.biome);
         }
      }

      protected void onControllerEvent(FormBiomeList parent, int elementIndex, ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
         if (event.getState() == ControllerInput.MENU_SELECT) {
            FormBiomeList.this.playTickSound();
            FormBiomeList.this.onClicked(this.biome);
            event.use();
         }
      }

      @Override
      public void drawControllerFocus(ControllerFocus current) {
         super.drawControllerFocus(current);
         GameTooltipManager.addControllerGlyph(Localization.translate("ui", "selectbutton"), ControllerInput.MENU_SELECT);
      }
   }
}
