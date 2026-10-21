package necesse.gfx.forms.components;

import java.util.function.Function;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.StaticMessage;

public class FormHorizontalIntScroll extends FormHorizontalScroll<Integer> {
   public FormHorizontalIntScroll(
      int x, int y, int width, FormHorizontalScroll.DrawOption drawOption, Function<Integer, GameMessage> textGetter, int value, int minValue, int maxValue
   ) {
      super(x, y, width, drawOption, 0);
      this.set(textGetter, value, minValue, maxValue);
   }

   public FormHorizontalIntScroll(int x, int y, int width, FormHorizontalScroll.DrawOption drawOption, GameMessage text, int value, int minValue, int maxValue) {
      super(x, y, width, drawOption, 0);
      this.set(text, value, minValue, maxValue);
   }

   public FormHorizontalIntScroll(int x, int y, int width, FormHorizontalScroll.DrawOption drawOption, String text, int value, int minValue, int maxValue) {
      this(x, y, width, drawOption, new StaticMessage(text), value, minValue, maxValue);
   }

   public void set(Function<Integer, GameMessage> textGetter, int value, int minValue, int maxValue) {
      FormHorizontalScroll.ScrollElement<Integer>[] data = new FormHorizontalScroll.ScrollElement[maxValue - minValue + 1];

      for (int i = minValue; i <= maxValue; i++) {
         int index = i - minValue;
         data[index] = new FormHorizontalScroll.ScrollElement<>(i, textGetter.apply(i));
      }

      this.setData(data);
      this.setElement(new FormHorizontalScroll.ScrollElement<>(value, textGetter.apply(value)));
   }

   public void set(GameMessage text, int value, int minValue, int maxValue) {
      this.set(i -> text, value, minValue, maxValue);
   }

   public void set(String text, int value, int minValue, int maxValue) {
      this.set(new StaticMessage(text), value, minValue, maxValue);
   }
}
