package necesse.gfx.forms.presets.debug;

import necesse.engine.util.EventVariable;
import necesse.gfx.GameResources;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormSlider;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.gameFont.FontOptions;

public class DebugSceneForm extends Form {
   public final DebugForm parent;
   public static EventVariable<Float> brightness = new EventVariable<>(1.0F);
   public static EventVariable<Float> contrast = new EventVariable<>(1.0F);
   public static EventVariable<Float> gamma = new EventVariable<>(1.0F);
   public static EventVariable<Float> vibrance = new EventVariable<>(0.0F);
   public static EventVariable<Float> vibranceRedBalance = new EventVariable<>(1.0F);
   public static EventVariable<Float> vibranceGreenBalance = new EventVariable<>(1.0F);
   public static EventVariable<Float> vibranceBlueBalance = new EventVariable<>(1.0F);
   public static EventVariable<Float> red = new EventVariable<>(1.0F);
   public static EventVariable<Float> green = new EventVariable<>(1.0F);
   public static EventVariable<Float> blue = new EventVariable<>(1.0F);

   public DebugSceneForm(String name, DebugForm parent) {
      super(name, 240, 400);
      this.parent = parent;
      FormFlow flow = new FormFlow(10);
      this.addComponent(new FormLabel("Scene", new FontOptions(20), 0, this.getWidth() / 2, flow.next(20)));
      final int scale = 20;
      FormSlider brightnessSlider = this.addComponent(
         flow.nextY(new FormSlider("Brightness", 5, 10, (int)(brightness.get() * scale), 0, 2 * scale, this.getWidth() - 10) {
            @Override
            public String getValueText() {
               return (int)((float)this.getValue() / scale * 100.0F) + " %";
            }
         }, 5)
      );
      brightnessSlider.onChanged(e -> brightness.set((float)e.from.getValue() / scale));
      brightness.addChangeListener(value -> brightnessSlider.setValue((int)(value * scale)), this::isDisposed);
      FormSlider contestSlider = this.addComponent(
         flow.nextY(new FormSlider("Contrast", 5, 10, (int)(contrast.get() * scale), 0, 2 * scale, this.getWidth() - 10) {
            @Override
            public String getValueText() {
               return (int)((float)this.getValue() / scale * 100.0F) + " %";
            }
         }, 5)
      );
      contestSlider.onChanged(e -> contrast.set((float)e.from.getValue() / scale));
      contrast.addChangeListener(value -> contestSlider.setValue((int)(value * scale)), this::isDisposed);
      FormSlider gammaSlider = this.addComponent(
         flow.nextY(new FormSlider("Gamma", 5, 10, (int)(gamma.get() * scale), 0, 2 * scale, this.getWidth() - 10) {
            @Override
            public String getValueText() {
               return (int)((float)this.getValue() / scale * 100.0F) + " %";
            }
         }, 5)
      );
      gammaSlider.onChanged(e -> gamma.set((float)e.from.getValue() / scale));
      gamma.addChangeListener(value -> gammaSlider.setValue((int)(value * scale)), this::isDisposed);
      FormSlider vibranceSlider = this.addComponent(
         flow.nextY(new FormSlider("Vibrance", 5, 10, (int)(vibrance.get() * scale), -2 * scale, 2 * scale, this.getWidth() - 10) {
            @Override
            public String getValueText() {
               return (int)((float)this.getValue() / scale * 100.0F) + " %";
            }
         }, 5)
      );
      vibranceSlider.onChanged(e -> vibrance.set((float)e.from.getValue() / scale));
      vibrance.addChangeListener(value -> vibranceSlider.setValue((int)(value * scale)), this::isDisposed);
      FormSlider vibranceRedSlider = this.addComponent(
         flow.nextY(new FormSlider("Vibrance Red", 5, 10, (int)(vibranceRedBalance.get() * scale), 0, 1 * scale, this.getWidth() - 10) {
            @Override
            public String getValueText() {
               return (int)((float)this.getValue() / scale * 100.0F) + " %";
            }
         }, 5)
      );
      vibranceRedSlider.onChanged(e -> vibranceRedBalance.set((float)e.from.getValue() / scale));
      vibranceRedBalance.addChangeListener(value -> vibranceRedSlider.setValue((int)(value * scale)), this::isDisposed);
      FormSlider vibranceGreenSlider = this.addComponent(
         flow.nextY(new FormSlider("Vibrance Green", 5, 10, (int)(vibranceGreenBalance.get() * scale), 0, 1 * scale, this.getWidth() - 10) {
            @Override
            public String getValueText() {
               return (int)((float)this.getValue() / scale * 100.0F) + " %";
            }
         }, 5)
      );
      vibranceGreenSlider.onChanged(e -> vibranceGreenBalance.set((float)e.from.getValue() / scale));
      vibranceGreenBalance.addChangeListener(value -> vibranceGreenSlider.setValue((int)(value * scale)), this::isDisposed);
      FormSlider vibranceBlueSlider = this.addComponent(
         flow.nextY(new FormSlider("Vibrance Blue", 5, 10, (int)(vibranceBlueBalance.get() * scale), 0, 1 * scale, this.getWidth() - 10) {
            @Override
            public String getValueText() {
               return (int)((float)this.getValue() / scale * 100.0F) + " %";
            }
         }, 5)
      );
      vibranceBlueSlider.onChanged(e -> vibranceBlueBalance.set((float)e.from.getValue() / scale));
      vibranceBlueBalance.addChangeListener(value -> vibranceBlueSlider.setValue((int)(value * scale)), this::isDisposed);
      flow.next(10);
      FormSlider redSlider = this.addComponent(
         flow.nextY(new FormSlider("Red color", 5, 10, (int)(red.get() * scale), 0, 2 * scale, this.getWidth() - 10) {
            @Override
            public String getValueText() {
               return (int)((float)this.getValue() / scale * 100.0F) + " %";
            }
         }, 5)
      );
      redSlider.onChanged(e -> red.set((float)e.from.getValue() / scale));
      red.addChangeListener(value -> redSlider.setValue((int)(value * scale)), this::isDisposed);
      FormSlider greenSlider = this.addComponent(
         flow.nextY(new FormSlider("Green color", 5, 10, (int)(green.get() * scale), 0, 2 * scale, this.getWidth() - 10) {
            @Override
            public String getValueText() {
               return (int)((float)this.getValue() / scale * 100.0F) + " %";
            }
         }, 5)
      );
      greenSlider.onChanged(e -> green.set((float)e.from.getValue() / scale));
      green.addChangeListener(value -> greenSlider.setValue((int)(value * scale)), this::isDisposed);
      FormSlider blueSlider = this.addComponent(
         flow.nextY(new FormSlider("Blue color", 5, 10, (int)(blue.get() * scale), 0, 2 * scale, this.getWidth() - 10) {
            @Override
            public String getValueText() {
               return (int)((float)this.getValue() / scale * 100.0F) + " %";
            }
         }, 5)
      );
      blueSlider.onChanged(e -> blue.set((float)e.from.getValue() / scale));
      blue.addChangeListener(value -> blueSlider.setValue((int)(value * scale)), this::isDisposed);
      this.addComponent(new FormTextButton("Back", 0, flow.next(40), this.getWidth())).onClicked(e -> parent.makeCurrent(parent.mainMenu));
      this.setHeight(flow.next());
   }

   static {
      brightness.addChangeListener(value -> {
         GameResources.debugColorShader.use();
         GameResources.debugColorShader.pass1f("brightness", value);
         GameResources.debugColorShader.stop();
      }, () -> false);
      contrast.addChangeListener(value -> {
         GameResources.debugColorShader.use();
         GameResources.debugColorShader.pass1f("contrast", value);
         GameResources.debugColorShader.stop();
      }, () -> false);
      gamma.addChangeListener(value -> {
         GameResources.debugColorShader.use();
         GameResources.debugColorShader.pass1f("gamma", value);
         GameResources.debugColorShader.stop();
      }, () -> false);
      vibrance.addChangeListener(value -> {
         GameResources.debugColorShader.use();
         GameResources.debugColorShader.pass1f("vibrance", value);
         GameResources.debugColorShader.stop();
      }, () -> false);
      vibranceRedBalance.addChangeListener(value -> {
         GameResources.debugColorShader.use();
         GameResources.debugColorShader.pass1f("vibranceRedBalance", value);
         GameResources.debugColorShader.stop();
      }, () -> false);
      vibranceGreenBalance.addChangeListener(value -> {
         GameResources.debugColorShader.use();
         GameResources.debugColorShader.pass1f("vibranceGreenBalance", value);
         GameResources.debugColorShader.stop();
      }, () -> false);
      vibranceBlueBalance.addChangeListener(value -> {
         GameResources.debugColorShader.use();
         GameResources.debugColorShader.pass1f("vibranceBlueBalance", value);
         GameResources.debugColorShader.stop();
      }, () -> false);
      red.addChangeListener(value -> {
         GameResources.debugColorShader.use();
         GameResources.debugColorShader.pass1f("red", value);
         GameResources.debugColorShader.stop();
      }, () -> false);
      green.addChangeListener(value -> {
         GameResources.debugColorShader.use();
         GameResources.debugColorShader.pass1f("green", value);
         GameResources.debugColorShader.stop();
      }, () -> false);
      blue.addChangeListener(value -> {
         GameResources.debugColorShader.use();
         GameResources.debugColorShader.pass1f("blue", value);
         GameResources.debugColorShader.stop();
      }, () -> false);
   }
}
