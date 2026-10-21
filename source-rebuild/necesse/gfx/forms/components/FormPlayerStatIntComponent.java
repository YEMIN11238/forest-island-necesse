package necesse.gfx.forms.components;

import java.util.function.Supplier;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.util.GameUtils;

public class FormPlayerStatIntComponent extends FormPlayerStatComponent<Integer> {
   private final String suffix;

   public FormPlayerStatIntComponent(int x, int y, int width, GameMessage displayName, Supplier<Integer> statSupplier, String suffix) {
      super(x, y, width, displayName, statSupplier, s -> GameUtils.metricNumber(s.intValue()) + suffix);
      this.suffix = suffix;
   }

   public FormPlayerStatIntComponent(int x, int y, int width, GameMessage displayName, Supplier<Integer> statSupplier) {
      this(x, y, width, displayName, statSupplier, "");
   }

   public String getTooltip(boolean couldFitData, Integer value, String formattedValue) {
      return couldFitData && formattedValue.equals(value + this.suffix) ? null : this.displayName.translate() + ": " + GameUtils.formatNumber(value.intValue());
   }
}
