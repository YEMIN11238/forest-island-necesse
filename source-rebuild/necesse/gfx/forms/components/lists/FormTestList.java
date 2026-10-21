package necesse.gfx.forms.components.lists;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;

public class FormTestList extends FormGeneralList<FormTestList.TestElement> {
   private int itemCounter = 0;

   public FormTestList(int x, int y, int width, int height) {
      super(x, y, width, height, 25);

      for (int i = 0; i < 10; i++) {
         this.addElement();
      }
   }

   public void deleteElement() {
      this.elements.remove(this.elements.size() - 1);
   }

   public void addElement() {
      this.elements.add(new FormTestList.TestElement(this.itemCounter++));
   }

   public class TestElement extends FormListElement<FormTestList> {
      private int index;

      public TestElement(int index) {
         this.index = index;
      }

      protected void draw(FormTestList parent, TickManager tickManager, PlayerMob perspective, int elementIndex) {
         float shade = this.isMouseOver(parent) ? 1.0F : 0.8F;
         FontOptions options = new FontOptions(16).colorf(shade, shade, shade);
         FontManager.bit.drawString(4.0F, 2.0F, "Item #" + this.index, options);
      }

      protected void onClick(FormTestList parent, int elementIndex, InputEvent event, PlayerMob perspective) {
         System.out.println("Clicked " + elementIndex + ", " + event.pos.hudX + ", " + event.pos.hudY + " with buttton " + event.getID());
      }

      protected void onControllerEvent(FormTestList parent, int elementIndex, ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
         System.out.println("Clicked " + elementIndex + ", with controller " + event);
      }
   }
}
