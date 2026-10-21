package necesse.gfx.shader.shaderVariable;

import necesse.engine.util.EventVariable;
import necesse.engine.util.GameMath;
import necesse.gfx.forms.ComponentListContainer;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormSlider;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.shader.GameShader;

public class ShaderFloatVariable extends ShaderVariable<Float> {
   protected boolean hasSlider;
   protected float min;
   protected float max;
   protected int scale;

   public ShaderFloatVariable(String varName) {
      super(varName);
   }

   public ShaderFloatVariable(String varName, float min, float max, int scale) {
      this(varName);
      this.hasSlider = true;
      this.min = min;
      this.max = max;
      this.scale = scale;
   }

   public ShaderFloatVariable(String varName, float min, float max) {
      this(varName, min, max, 100);
   }

   @Override
   public void addInputs(GameShader shader, ComponentListContainer<? super FormComponent> form, int x, FormFlow yFlow, int width) {
      EventVariable<Float> value = new EventVariable<>(shader.get1f(this.varName));
      form.addComponent(new FormLabel(this.varName, new FontOptions(16), -1, x, yFlow.next(18), width));
      FormTextInput input = form.addComponent(new FormTextInput(x, yFlow.next(22), FormInputSize.SIZE_20, width, 100));
      input.setText(Float.toString(value.get()));
      input.onChange(e -> {
         try {
            float newValue = Float.parseFloat(input.getText());
            value.set(newValue);
         } catch (NumberFormatException var4x) {
         }
      });
      value.addChangeListener(newValue -> input.setText(Float.toString(newValue)), input::isDisposed);
      if (this.hasSlider) {
         FormSlider slider = form.addComponent(
            yFlow.nextY(new FormSlider("", x, 10, (int)((value.get() - this.min) * this.scale / (this.max - this.min)), 0, this.scale, width), 5)
         );
         slider.drawValue = false;
         slider.onChanged(e -> {
            float var = this.min + (float)e.from.getValue() / this.scale * (this.max - this.min);
            var = GameMath.toDecimals(var, (int)Math.ceil(Math.log(this.scale)));
            value.set(var);
         });
         value.addChangeListener(newValue -> slider.setValue((int)((newValue - this.min) * this.scale / (this.max - this.min))), slider::isDisposed);
      }

      value.addChangeListener(newValue -> {
         shader.use();
         shader.pass1f(this.varName, newValue);
         shader.stop();
      }, () -> false);
   }
}
