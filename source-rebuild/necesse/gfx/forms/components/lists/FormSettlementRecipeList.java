package necesse.gfx.forms.components.lists;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.function.Predicate;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameBackground;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Recipes;
import necesse.inventory.recipe.Tech;

public abstract class FormSettlementRecipeList extends FormGeneralGridList<FormSettlementRecipeList.RecipeElement> {
   protected Predicate<Recipe> filter;
   protected boolean shouldUpdate;

   public FormSettlementRecipeList(int x, int y, int width, int height, Predicate<Recipe> filter) {
      super(x, y, width, height, 36, 36);
      this.filter = filter;
      this.shouldUpdate = true;
   }

   public FormSettlementRecipeList(int x, int y, int width, int height, Tech... techs) {
      this(x, y, width, height, r -> r.matchesTechs(techs));
   }

   public void setFilter(Predicate<Recipe> filter) {
      this.filter = filter;
      this.shouldUpdate = true;
   }

   public void update() {
      this.shouldUpdate = true;
   }

   protected void forceUpdate() {
      this.shouldUpdate = false;
      this.elements.clear();
      Recipes.streamRecipes()
         .filter(r -> this.filter == null || this.filter.test(r))
         .forEach(r -> this.elements.add(new FormSettlementRecipeList.RecipeElement(r)));
      this.limitMaxScroll();
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      if (this.shouldUpdate) {
         this.forceUpdate();
      }

      super.draw(tickManager, perspective, renderBox);
   }

   public abstract void onRecipeClicked(Recipe var1, PlayerMob var2);

   public class RecipeElement extends FormListGridElement<FormSettlementRecipeList> {
      public final Recipe recipe;

      public RecipeElement(Recipe recipe) {
         this.recipe = recipe;
      }

      protected void draw(FormSettlementRecipeList parent, TickManager tickManager, PlayerMob perspective, int elementIndex) {
         boolean mouseOver = this.isMouseOver(parent);
         Color color = FormSettlementRecipeList.this.getInterfaceStyle().activeElementColor;
         if (mouseOver) {
            color = FormSettlementRecipeList.this.getInterfaceStyle().highlightElementColor;
         }

         GameTexture slotTexture = mouseOver
            ? FormSettlementRecipeList.this.getInterfaceStyle().inventoryslot_small.highlighted
            : FormSettlementRecipeList.this.getInterfaceStyle().inventoryslot_small.active;
         slotTexture.initDraw().color(color).draw(2, 2);
         this.recipe.draw(2, 2, perspective);
         if (mouseOver) {
            GameTooltipManager.addTooltip(
               this.recipe.getTooltip(null, perspective, new GameBlackboard()), GameBackground.getItemTooltipBackground(), TooltipLocation.FORM_FOCUS
            );
         }
      }

      protected void onClick(FormSettlementRecipeList parent, int elementIndex, InputEvent event, PlayerMob perspective) {
         FormSettlementRecipeList.this.onRecipeClicked(this.recipe, perspective);
      }

      protected void onControllerEvent(FormSettlementRecipeList parent, int elementIndex, ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
         if (event.getState() == ControllerInput.MENU_SELECT) {
            FormSettlementRecipeList.this.onRecipeClicked(this.recipe, perspective);
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
