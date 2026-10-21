package necesse.gfx.gameTooltips;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.function.Supplier;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.FormManager;
import necesse.gfx.forms.components.FormComponent;

public class FormComponentsTooltip implements GameTooltips {
   private final FormManager formManager = new FormManager();
   private final Form form = this.formManager.addComponent(new Form(0, 0));
   private boolean shouldUpdateSize;

   public FormComponentsTooltip() {
      this.form.drawBase = false;
      this.form.shouldLimitDrawArea = false;
   }

   public void addComponent(FormComponent component) {
      this.shouldUpdateSize = true;
      this.form.addComponent(component);
   }

   private void updateSize() {
      if (this.shouldUpdateSize) {
         Rectangle box = this.form.getBoundingBox();
         this.form.setWidth(box.width);
         this.form.setHeight(box.height);
         this.shouldUpdateSize = false;
      }
   }

   @Override
   public int getHeight() {
      this.updateSize();
      return this.form.getHeight();
   }

   @Override
   public int getWidth() {
      this.updateSize();
      return this.form.getWidth();
   }

   @Override
   public void draw(int x, int y, Supplier<Color> defaultColor) {
      this.form.setPosition(x, y);
      this.formManager.draw(null, null);
   }

   @Override
   public int getDrawOrder() {
      return 0;
   }

   @Override
   public boolean matchesSearch(String search) {
      return false;
   }
}
