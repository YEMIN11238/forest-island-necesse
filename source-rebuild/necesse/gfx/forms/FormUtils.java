package necesse.gfx.forms;

import java.util.ArrayList;
import necesse.engine.util.GameMath;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.position.FormPositionContainer;

public class FormUtils {
   public static FormUtils.GridButtonOptions setupGridButtons(int buttonSize, int buttonPadding) {
      return new FormUtils.GridButtonOptions(buttonSize, buttonPadding);
   }

   public static class GridButtonOptions {
      protected final int buttonSize;
      protected final int buttonPadding;
      protected final ArrayList<FormPositionContainer> buttons = new ArrayList<>();

      public GridButtonOptions(int buttonSize, int buttonPadding) {
         this.buttonSize = buttonSize;
         this.buttonPadding = buttonPadding;
      }

      public FormUtils.GridButtonOptions addButton(FormPositionContainer button) {
         this.buttons.add(button);
         return this;
      }

      public int apply(ComponentListContainer<? super FormComponent> container, FormFlow flow, int startX, boolean centered, int maxWidth) {
         return flow.next(this.apply(container, startX, flow.next(), centered, maxWidth));
      }

      public int apply(ComponentListContainer<? super FormComponent> container, int startX, int startY, boolean centered, int maxWidth) {
         int totalButtons = this.buttons.size();
         int buttonsPerRow = GameMath.limit(maxWidth / (this.buttonSize + this.buttonPadding * 2), 1, totalButtons);
         int totalRows = (totalButtons + buttonsPerRow - 1) / buttonsPerRow;
         if (centered) {
            int buttonsRowWidth = (this.buttonSize + this.buttonPadding * 2) * buttonsPerRow;
            startX += maxWidth / 2 - buttonsRowWidth / 2;
         }

         for (int i = 0; i < totalButtons; i++) {
            int currentRow = i / buttonsPerRow;
            int currentCol = i % buttonsPerRow;
            int currentX = startX + currentCol * (this.buttonSize + this.buttonPadding * 2);
            int currentY = startY + currentRow * (this.buttonSize + this.buttonPadding * 2);
            FormPositionContainer button = this.buttons.get(i);
            container.addComponent((FormComponent)button);
            button.setPosition(currentX, currentY);
         }

         return totalRows * (this.buttonSize + this.buttonPadding * 2);
      }
   }
}
