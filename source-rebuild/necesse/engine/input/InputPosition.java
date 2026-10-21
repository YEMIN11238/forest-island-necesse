package necesse.engine.input;

import java.awt.Point;
import java.util.function.Function;
import necesse.engine.window.GameWindow;

public class InputPosition {
   public final int windowX;
   public final int windowY;
   public final int sceneX;
   public final int sceneY;
   public final int hudX;
   public final int hudY;

   InputPosition(int windowX, int windowY, int sceneX, int sceneY, int hudX, int hudY) {
      this.windowX = windowX;
      this.windowY = windowY;
      this.sceneX = sceneX;
      this.sceneY = sceneY;
      this.hudX = hudX;
      this.hudY = hudY;
   }

   public static InputPosition dummyPos() {
      return new InputPosition(0, 0, 0, 0, 0, 0);
   }

   public static InputPosition fromWindowPos(Input input, int windowX, int windowY) {
      return fromWindowPos(input.window, windowX, windowY);
   }

   public static InputPosition fromWindowPos(GameWindow window, int windowX, int windowY) {
      int sceneX = (int)((float)windowX * window.getSceneWidth() / window.getWidth());
      int sceneY = (int)((float)windowY * window.getSceneHeight() / window.getHeight());
      int hudX = (int)((float)windowX * window.getHudWidth() / window.getWidth());
      int hudY = (int)((float)windowY * window.getHudHeight() / window.getHeight());
      return new InputPosition(windowX, windowY, sceneX, sceneY, hudX, hudY);
   }

   public static InputPosition fromScenePos(Input input, int sceneX, int sceneY) {
      return fromScenePos(input.window, sceneX, sceneY);
   }

   public static InputPosition fromScenePos(GameWindow window, int sceneX, int sceneY) {
      int windowX = (int)(sceneX / ((float)window.getSceneWidth() / window.getWidth()));
      int windowY = (int)(sceneY / ((float)window.getSceneHeight() / window.getHeight()));
      int hudX = (int)((float)windowX * window.getHudWidth() / window.getWidth());
      int hudY = (int)((float)windowY * window.getHudHeight() / window.getHeight());
      return new InputPosition(windowX, windowY, sceneX, sceneY, hudX, hudY);
   }

   public static InputPosition fromHudPos(Input input, int hudX, int hudY) {
      return fromHudPos(input.window, hudX, hudY);
   }

   public static InputPosition fromHudPos(GameWindow window, int hudX, int hudY) {
      int windowX = (int)(hudX / ((float)window.getHudWidth() / window.getWidth()));
      int windowY = (int)(hudY / ((float)window.getHudHeight() / window.getHeight()));
      int sceneX = (int)((float)windowX * window.getSceneWidth() / window.getWidth());
      int sceneY = (int)((float)windowY * window.getSceneHeight() / window.getHeight());
      return new InputPosition(windowX, windowY, sceneX, sceneY, hudX, hudY);
   }

   public enum InputPositionGetter {
      WINDOW(p -> new Point(p.windowX, p.windowY)),
      SCENE(p -> new Point(p.sceneX, p.sceneY)),
      HUD(p -> new Point(p.hudX, p.hudY));

      private final Function<InputPosition, Point> getter;

      InputPositionGetter(Function<InputPosition, Point> getter) {
         this.getter = getter;
      }

      public Point get(InputPosition position) {
         return this.getter.apply(position);
      }

      public Point get(InputEvent event) {
         return this.get(event.pos);
      }
   }
}
