package necesse.gfx.shader;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Objects;
import necesse.engine.GlobalData;
import necesse.engine.input.InputEvent;
import necesse.engine.input.InputPosition;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.Renderer;
import necesse.gfx.gameTexture.GameFrameBuffer;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.shader.shaderVariable.ShaderBooleanVariable;

public class FormShader extends GameShader {
   private FormShader.FormShaderState currentState = null;

   public FormShader() {
      super("vertForm", "fragForm");
      this.addVariable(new ShaderBooleanVariable("drawOutside"));
   }

   public FormShader.FormShaderState startState(Point offset, Rectangle drawLimit) {
      this.currentState = new FormShader.FormShaderState(this.currentState, offset, drawLimit);
      this.currentState.apply();
      return this.currentState;
   }

   @Override
   public void use() {
      super.use();
      this.pass1i("recolorLookupTexture", 1);
      GameWindow window = WindowManager.getWindow();
      this.currentState = new FormShader.FormShaderState(null, new Point(0, 0), new Rectangle(0, 0, window.getHudWidth(), window.getHudHeight()));
      this.currentState.apply();
   }

   public void usePrevState() {
      super.use();
      if (this.currentState != null) {
         this.currentState.apply();
      }
   }

   public Point getCurrentOffset() {
      return this.currentState == null ? new Point(0, 0) : this.currentState.offset;
   }

   public Rectangle getCurrentDrawLimit() {
      return this.currentState == null ? new Rectangle(Integer.MAX_VALUE, Integer.MAX_VALUE) : this.currentState.drawLimit;
   }

   public InputPosition getCurrentMousePos() {
      return this.currentState == null ? WindowManager.getWindow().mousePos() : this.currentState.mouseEvent.pos;
   }

   public void passRecolor(GameTexture texture, int yIndex) {
      if (texture != null) {
         this.pass1i("recolorIndex", yIndex);
         this.pass2i("recolorTextureSize", texture.getWidth(), texture.getHeight());
      } else {
         this.pass1i("recolorIndex", -1);
      }
   }

   public class FormShaderState {
      private final FormShader.FormShaderState prevState;
      public final Point offset;
      public final Rectangle drawLimit;
      public final InputEvent mouseEvent;

      private FormShaderState(FormShader.FormShaderState prevState, Point offset, Rectangle drawLimit) {
         this.prevState = prevState;
         if (prevState != null) {
            if (offset == null) {
               offset = prevState.offset;
            } else {
               offset.x = offset.x + prevState.offset.x;
               offset.y = offset.y + prevState.offset.y;
            }
         }

         Objects.requireNonNull(offset);
         offset = new Point(offset);
         this.offset = offset;
         if (drawLimit != null) {
            drawLimit = new Rectangle(drawLimit);
            drawLimit.x = drawLimit.x + offset.x;
            drawLimit.y = drawLimit.y + offset.y;
            if (prevState != null) {
               drawLimit = prevState.drawLimit.intersection(drawLimit);
            }
         } else if (prevState != null) {
            drawLimit = new Rectangle(prevState.drawLimit);
         } else {
            drawLimit = new Rectangle(0, 0, WindowManager.getWindow().getHudWidth(), WindowManager.getWindow().getHudHeight());
         }

         this.drawLimit = drawLimit;
         GameWindow window = WindowManager.getWindow();
         int mouseX = Integer.MIN_VALUE;
         if (window.mousePos().hudX >= drawLimit.x && window.mousePos().hudX < drawLimit.x + drawLimit.width) {
            mouseX = window.mousePos().hudX - offset.x;
         }

         int mouseY = Integer.MIN_VALUE;
         if (window.mousePos().hudY >= drawLimit.y && window.mousePos().hudY < drawLimit.y + drawLimit.height) {
            mouseY = window.mousePos().hudY - offset.y;
         }

         this.mouseEvent = InputEvent.MouseMoveEvent(InputPosition.fromHudPos(window.getInput(), mouseX, mouseY), GlobalData.getCurrentGameLoop());
      }

      private void apply() {
         GameFrameBuffer currentBuffer = WindowManager.getWindow().getCurrentBuffer();
         FormShader.this.pass2f("pixelOffset", (float)this.offset.x / currentBuffer.getWidth() * 2.0F, (float)this.offset.y / currentBuffer.getHeight() * 2.0F);
         int y = -this.drawLimit.height + currentBuffer.getHeight() - this.drawLimit.y;
         FormShader.this.pass4f("drawLimit", this.drawLimit.x, y, this.drawLimit.width, this.drawLimit.height);
      }

      public void end() {
         if (this.prevState == null) {
            throw new IllegalStateException("Cannot end Form shader state: Not started yet");
         }

         FormShader.this.currentState = this.prevState;
         FormShader.this.currentState.apply();
      }

      public void drawDebugRects() {
         Renderer.initQuadDraw(this.drawLimit.x + this.drawLimit.width - this.offset.x, this.drawLimit.y + this.drawLimit.height - this.offset.y)
            .color(new Color(0, 0, 255, 100))
            .draw(0, 0);
      }
   }
}
