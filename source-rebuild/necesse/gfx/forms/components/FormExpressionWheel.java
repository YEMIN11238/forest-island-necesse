package necesse.gfx.forms.components;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.InputEvent;
import necesse.engine.input.InputPosition;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.util.GameMath;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.HumanLook;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.forms.controller.ControllerFocus;
import necesse.gfx.forms.controller.ControllerNavigationHandler;
import necesse.gfx.forms.position.FormFixedPosition;
import necesse.gfx.forms.position.FormPosition;
import necesse.gfx.forms.position.FormPositionContainer;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.settler.Settler;
import org.lwjgl.opengl.GL11;

public class FormExpressionWheel extends FormComponent implements FormPositionContainer {
   private FormPosition position;
   private final FormExpressionWheel.Expression[] expressions;
   private FormExpressionWheel.ExpressionWheelDrawOptions drawOptions;
   private final float radius = 100.0F;
   private int hoveringIndex = -1;

   public FormExpressionWheel(int x, int y, FormExpressionWheel.Expression... expressions) {
      this.position = new FormFixedPosition(x, y);
      this.expressions = expressions;
   }

   private void insertFloatColor(float[] values, Color color, int startIndex) {
      int actualIndex = startIndex * 4;
      values[actualIndex] = color.getRed() / 255.0F;
      values[actualIndex + 1] = color.getGreen() / 255.0F;
      values[actualIndex + 2] = color.getBlue() / 255.0F;
      values[actualIndex + 3] = color.getAlpha() / 255.0F;
   }

   private void setGLColor(float[] values, int startIndex) {
      int actualIndex = startIndex * 4;
      GL11.glColor4f(values[actualIndex], values[actualIndex + 1], values[actualIndex + 2], values[actualIndex + 3]);
   }

   protected FormExpressionWheel.ExpressionWheelDrawOptions getDrawOptions() {
      if (this.drawOptions == null) {
         float borderSize = 4.0F;
         float borderSizeHalf = borderSize / 2.0F;
         float maxDistancePerAngleChange = 10.0F;
         float anglerPerWheel = 360.0F / this.expressions.length;
         float circumference = 628.31854F;
         float circumferenceMinusBorders = circumference - this.expressions.length * borderSize;
         float circumferencePerElement = circumferenceMinusBorders / this.expressions.length;
         int edgesPerElement = (int)Math.ceil(circumferencePerElement / maxDistancePerAngleChange);
         float borderCircumferenceAngle = 360.0F * (borderSize / circumference);
         float borderCircumferenceAngleHalf = borderCircumferenceAngle / 2.0F;
         Color selectorInnerColor = new Color(0, 0, 0, 255);
         Color selectorOuterColor = new Color(0, 0, 0, 25);
         Color selectedColor = new Color(100, 100, 100, 100);
         final float[] selectorInnerColorFloat = new float[]{
            selectorInnerColor.getRed() / 255.0F,
            selectorInnerColor.getGreen() / 255.0F,
            selectorInnerColor.getBlue() / 255.0F,
            selectorInnerColor.getAlpha() / 255.0F
         };
         final float[] selectorOuterColorFloat = new float[]{
            selectorOuterColor.getRed() / 255.0F,
            selectorOuterColor.getGreen() / 255.0F,
            selectorOuterColor.getBlue() / 255.0F,
            selectorOuterColor.getAlpha() / 255.0F
         };
         final float[] selectedColorFloat = new float[]{
            selectedColor.getRed() / 255.0F, selectedColor.getGreen() / 255.0F, selectedColor.getBlue() / 255.0F, selectedColor.getAlpha() / 255.0F
         };
         Color borderInnerColor = new Color(255, 255, 255, 200);
         Color borderOuterColor = new Color(255, 255, 255, 100);
         FormExpressionWheel.ExpressionDrawOptions[] expressionDrawOptions = new FormExpressionWheel.ExpressionDrawOptions[this.expressions.length];
         float startAngleOffset = -90.0F;
         java.awt.geom.Point2D.Float[] borderQuads = new java.awt.geom.Point2D.Float[this.expressions.length * 4];
         float[] borderColors = new float[this.expressions.length * 4 * 4];

         for (int i = 0; i < this.expressions.length; i++) {
            final FormExpressionWheel.Expression expression = this.expressions[i];
            float startAngle = i * anglerPerWheel + startAngleOffset;
            float endAngle = startAngle + anglerPerWheel;
            java.awt.geom.Point2D.Float startDir = GameMath.getAngleDir(startAngle);
            java.awt.geom.Point2D.Float startPerp = GameMath.getPerpendicularDir(startDir);
            float borderX1 = startPerp.x * borderSizeHalf;
            float borderX2 = startDir.x * 100.0F + borderX1;
            float borderX3 = startPerp.x * -borderSizeHalf;
            float borderX4 = startDir.x * 100.0F + borderX3;
            float borderY1 = startPerp.y * borderSizeHalf;
            float borderY2 = startDir.y * 100.0F + borderY1;
            float borderY3 = startPerp.y * -borderSizeHalf;
            float borderY4 = startDir.y * 100.0F + borderY3;
            int borderIndex = i * 4;
            borderQuads[borderIndex] = new java.awt.geom.Point2D.Float(borderX1, borderY1);
            borderQuads[borderIndex + 1] = new java.awt.geom.Point2D.Float(borderX2, borderY2);
            borderQuads[borderIndex + 2] = new java.awt.geom.Point2D.Float(borderX4, borderY4);
            borderQuads[borderIndex + 3] = new java.awt.geom.Point2D.Float(borderX3, borderY3);
            this.insertFloatColor(borderColors, borderInnerColor, borderIndex);
            this.insertFloatColor(borderColors, borderOuterColor, borderIndex + 1);
            this.insertFloatColor(borderColors, borderOuterColor, borderIndex + 2);
            this.insertFloatColor(borderColors, borderInnerColor, borderIndex + 3);
            final java.awt.geom.Point2D.Float[] elementEdges = new java.awt.geom.Point2D.Float[edgesPerElement];
            float angleDelta = endAngle - startAngle;
            float centerAngle = startAngle + angleDelta / 2.0F;
            java.awt.geom.Point2D.Float centerDir = GameMath.getAngleDir(centerAngle);
            float centerRadius = Math.max(50.0F, 60.0F);
            final Point centerPos = new Point((int)(centerDir.x * centerRadius), (int)(centerDir.y * centerRadius));
            float anglePerEdge = (angleDelta - borderCircumferenceAngle) / (edgesPerElement - 1);

            for (int edge = 0; edge < edgesPerElement; edge++) {
               float angle = startAngle + borderCircumferenceAngleHalf + edge * anglePerEdge;
               java.awt.geom.Point2D.Float dir = GameMath.getAngleDir(angle);
               elementEdges[edge] = new java.awt.geom.Point2D.Float(dir.x * 100.0F, dir.y * 100.0F);
            }

            expressionDrawOptions[i] = new FormExpressionWheel.ExpressionDrawOptions(
               startAngle - startAngleOffset, endAngle - startAngleOffset, startAngleOffset
            ) {
               @Override
               public void draw(int x, int y, boolean isHovering, PlayerMob perspective) {
                  GameResources.empty.bindTexture();
                  GL11.glLoadIdentity();
                  GL11.glBegin(6);
                  float[] innerColor = selectorInnerColorFloat;
                  float[] outerColor = selectorOuterColorFloat;
                  if (isHovering) {
                     innerColor = selectedColorFloat;
                     outerColor = selectedColorFloat;
                  }

                  GL11.glColor4f(innerColor[0], innerColor[1], innerColor[2], innerColor[3]);
                  GL11.glVertex2f(x, y);

                  for (java.awt.geom.Point2D.Float edge : elementEdges) {
                     GL11.glColor4f(outerColor[0], outerColor[1], outerColor[2], outerColor[3]);
                     GL11.glVertex2f(x + edge.x, y + edge.y);
                  }

                  GL11.glEnd();
                  expression.drawIcon.accept(new Point(x + centerPos.x, y + centerPos.y), perspective);
               }
            };
         }

         FormExpressionWheel.PositionedDrawOptions borderDrawOption = (x, y) -> {
            GameResources.empty.bindTexture();
            GL11.glLoadIdentity();
            GL11.glBegin(7);

            for (int ix = 0; ix < borderQuads.length; ix++) {
               java.awt.geom.Point2D.Float pos = borderQuads[ix];
               this.setGLColor(borderColors, ix);
               GL11.glVertex2f(x + pos.x, y + pos.y);
            }

            GL11.glEnd();
         };
         this.drawOptions = new FormExpressionWheel.ExpressionWheelDrawOptions(expressionDrawOptions, borderDrawOption);
      }

      return this.drawOptions;
   }

   @Override
   public void handleInputEvent(InputEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.isMouseMoveEvent()) {
         this.updateSelected(event.pos.hudX - this.getX(), event.pos.hudY - this.getY());
      }
   }

   @Override
   public void handleControllerEvent(ControllerEvent event, TickManager tickManager, PlayerMob perspective) {
      if (event.getState() == ControllerInput.AIM) {
         this.updateSelected(ControllerInput.AIM.getX(), ControllerInput.AIM.getY());
      } else if (event.getState() == ControllerInput.CURSOR) {
         InputPosition mousePos = WindowManager.getWindow().mousePos();
         this.updateSelected(mousePos.hudX - this.getX(), mousePos.hudY - this.getY());
      }
   }

   private void updateSelected(float dirX, float dirY) {
      java.awt.geom.Point2D.Float dir = GameMath.normalize(dirX, dirY);
      float angle = GameMath.getAngle(dir);
      FormExpressionWheel.ExpressionDrawOptions[] expressionDrawOptions = this.getDrawOptions().expressionDrawOptions;
      this.hoveringIndex = -1;

      for (int i = 0; i < expressionDrawOptions.length; i++) {
         FormExpressionWheel.ExpressionDrawOptions expression = expressionDrawOptions[i];
         float compareAngle = GameMath.fixAngle(angle - expression.angleOffset);
         if (compareAngle >= expression.startAngle && compareAngle <= expression.endAngle) {
            this.hoveringIndex = i;
            break;
         }
      }
   }

   @Override
   public void addNextControllerFocus(
      List<ControllerFocus> list, int currentXOffset, int currentYOffset, ControllerNavigationHandler customNavigationHandler, Rectangle area, boolean draw
   ) {
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      this.getDrawOptions().draw(this.getX(), this.getY(), this.hoveringIndex, perspective);
      FormExpressionWheel.Expression selected = this.getSelected();
      if (selected != null) {
         FontOptions fontOptions = new FontOptions(20).outline();
         String string = selected.displayName.translate();
         int width = FontManager.bit.getWidthCeil(string, fontOptions);
         FontManager.bit.drawString(this.getX() - width / 2, this.getY() - 50.0F - 20.0F, string, fontOptions);
      }
   }

   public FormExpressionWheel.Expression getSelected() {
      return this.hoveringIndex >= 0 && this.hoveringIndex < this.expressions.length ? this.expressions[this.hoveringIndex] : null;
   }

   @Override
   public List<Rectangle> getHitboxes() {
      return Collections.singletonList(new Rectangle(this.getX() - 100, this.getY() - 100, 200, 200));
   }

   @Override
   public FormPosition getPosition() {
      return this.position;
   }

   @Override
   public void setPosition(FormPosition position) {
      this.position = position;
   }

   public enum Expression {
      SAD(
         new LocalMessage("ui", "expressionsad"),
         5000,
         (pos, player) -> {
            HumanLook look = player == null ? new HumanLook() : new HumanLook(player.look);
            Level level = player == null ? null : player.getLevel();
            GameTexture.overrideBlendQuality = GameTexture.BlendQuality.NEAREST;
            look.setEyeType(6);
            int size = 48;
            Settler.getHumanFaceDrawOptions(
                  new HumanDrawOptions(level, look, false), size, pos.x - size / 2, pos.y - size / 2 + 5, humanDrawOptions -> humanDrawOptions.helmet(null)
               )
               .draw();
            GameTexture.overrideBlendQuality = null;
         },
         (progress, humanDrawOptions) -> humanDrawOptions.eyeTypeOverride(6)
      ),
      SURPRISED(
         new LocalMessage("ui", "expressionsurprised"),
         5000,
         (pos, player) -> {
            HumanLook look = player == null ? new HumanLook() : new HumanLook(player.look);
            Level level = player == null ? null : player.getLevel();
            GameTexture.overrideBlendQuality = GameTexture.BlendQuality.NEAREST;
            look.setEyeType(5);
            int size = 48;
            Settler.getHumanFaceDrawOptions(
                  new HumanDrawOptions(level, look, false), size, pos.x - size / 2, pos.y - size / 2 + 5, humanDrawOptions -> humanDrawOptions.helmet(null)
               )
               .draw();
            GameTexture.overrideBlendQuality = null;
         },
         (progress, humanDrawOptions) -> humanDrawOptions.eyeTypeOverride(5)
      ),
      ANGRY(
         new LocalMessage("ui", "expressionangry"),
         5000,
         (pos, player) -> {
            HumanLook look = player == null ? new HumanLook() : new HumanLook(player.look);
            Level level = player == null ? null : player.getLevel();
            GameTexture.overrideBlendQuality = GameTexture.BlendQuality.NEAREST;
            look.setEyeType(7);
            int size = 48;
            Settler.getHumanFaceDrawOptions(
                  new HumanDrawOptions(level, look, false), size, pos.x - size / 2, pos.y - size / 2 + 5, humanDrawOptions -> humanDrawOptions.helmet(null)
               )
               .draw();
            GameTexture.overrideBlendQuality = null;
         },
         (progress, humanDrawOptions) -> humanDrawOptions.eyeTypeOverride(7)
      ),
      BORED(
         new LocalMessage("ui", "expressionbored"),
         5000,
         (pos, player) -> {
            HumanLook look = player == null ? new HumanLook() : new HumanLook(player.look);
            Level level = player == null ? null : player.getLevel();
            GameTexture.overrideBlendQuality = GameTexture.BlendQuality.NEAREST;
            look.setEyeType(3);
            int size = 48;
            Settler.getHumanFaceDrawOptions(
                  new HumanDrawOptions(level, look, false), size, pos.x - size / 2, pos.y - size / 2 + 5, humanDrawOptions -> humanDrawOptions.helmet(null)
               )
               .draw();
            GameTexture.overrideBlendQuality = null;
         },
         (progress, humanDrawOptions) -> humanDrawOptions.eyeTypeOverride(3)
      ),
      HAPPY(
         new LocalMessage("ui", "expressionhappy"),
         5000,
         (pos, player) -> {
            HumanLook look = player == null ? new HumanLook() : new HumanLook(player.look);
            Level level = player == null ? null : player.getLevel();
            GameTexture.overrideBlendQuality = GameTexture.BlendQuality.NEAREST;
            look.setEyeType(12);
            int size = 48;
            Settler.getHumanFaceDrawOptions(
                  new HumanDrawOptions(level, look, false), size, pos.x - size / 2, pos.y - size / 2 + 5, humanDrawOptions -> humanDrawOptions.helmet(null)
               )
               .draw();
            GameTexture.overrideBlendQuality = null;
         },
         (progress, humanDrawOptions) -> humanDrawOptions.eyeTypeOverride(12)
      ),
      BLUSH(
         new LocalMessage("ui", "expressionblush"),
         5000,
         (pos, player) -> {
            HumanLook look = player == null ? new HumanLook() : new HumanLook(player.look);
            Level level = player == null ? null : player.getLevel();
            GameTexture.overrideBlendQuality = GameTexture.BlendQuality.NEAREST;
            look.setEyeType(13);
            int size = 48;
            Settler.getHumanFaceDrawOptions(
                  new HumanDrawOptions(level, look, false), size, pos.x - size / 2, pos.y - size / 2 + 5, humanDrawOptions -> humanDrawOptions.helmet(null)
               )
               .draw();
            GameTexture.overrideBlendQuality = null;
         },
         (progress, humanDrawOptions) -> humanDrawOptions.eyeTypeOverride(13)
      );

      public final GameMessage displayName;
      public final int animationTimeMillis;
      public final BiConsumer<Point, PlayerMob> drawIcon;
      public final BiConsumer<Float, HumanDrawOptions> drawOptionsModifier;

      Expression(
         GameMessage displayName, int animationTimeMillis, BiConsumer<Point, PlayerMob> drawIcon, BiConsumer<Float, HumanDrawOptions> drawOptionsModifier
      ) {
         this.displayName = displayName;
         this.animationTimeMillis = animationTimeMillis;
         this.drawIcon = drawIcon;
         this.drawOptionsModifier = drawOptionsModifier;
      }
   }

   protected abstract static class ExpressionDrawOptions {
      public final float startAngle;
      public final float endAngle;
      public final float angleOffset;

      public ExpressionDrawOptions(float startAngle, float endAngle, float angleOffset) {
         this.startAngle = startAngle;
         this.endAngle = endAngle;
         this.angleOffset = angleOffset;
      }

      public abstract void draw(int var1, int var2, boolean var3, PlayerMob var4);
   }

   protected static class ExpressionWheelDrawOptions {
      public FormExpressionWheel.ExpressionDrawOptions[] expressionDrawOptions;
      public FormExpressionWheel.PositionedDrawOptions borderDrawOptions;

      public ExpressionWheelDrawOptions(
         FormExpressionWheel.ExpressionDrawOptions[] expressionDrawOptions, FormExpressionWheel.PositionedDrawOptions borderDrawOptions
      ) {
         this.expressionDrawOptions = expressionDrawOptions;
         this.borderDrawOptions = borderDrawOptions;
      }

      public void draw(int x, int y, int hoveringIndex, PlayerMob perspective) {
         for (int i = 0; i < this.expressionDrawOptions.length; i++) {
            this.expressionDrawOptions[i].draw(x, y, i == hoveringIndex, perspective);
         }

         this.borderDrawOptions.draw(x, y);
      }
   }

   protected interface PositionedDrawOptions {
      void draw(int var1, int var2);
   }
}
