package necesse.gfx.forms.components;

import java.util.function.Supplier;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.util.GameUtils;

public class FormPlayerStatLongComponent extends FormPlayerStatComponent<Long> {
   private final String suffix;

   public FormPlayerStatLongComponent(int x, int y, int width, GameMessage displayName, Supplier<Long> statSupplier, String suffix) {
      super(x, y, width, displayName, statSupplier, s -> GameUtils.metricNumber(s) + suffix);
      this.suffix = suffix;
   }

   public FormPlayerStatLongComponent(int x, int y, int width, GameMessage displayName, Supplier<Long> statSupplier) {
      this(x, y, width, displayName, statSupplier, "");
   }

   public String getTooltip(boolean couldFitData, Long value, String formattedValue) {
      return couldFitData && formattedValue.equals(value + this.suffix) ? null : this.displayName.translate() + ": " + GameUtils.formatNumber(value);
   }
}
