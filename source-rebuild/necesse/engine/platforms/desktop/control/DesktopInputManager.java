package necesse.engine.platforms.desktop.control;

import java.util.ArrayList;
import java.util.Arrays;
import necesse.engine.input.InputManager;
import necesse.engine.input.InputSource;
import necesse.engine.input.controller.ControllerHandle;
import necesse.engine.platforms.sharedOnPC.input.GLFWInputSource;
import necesse.engine.platforms.sharedOnPC.window.GLFWGameWindow;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import org.lwjgl.glfw.GLFW;

public class DesktopInputManager extends InputManager {
   @Override
   public void initialize() {
      inputSources = new ArrayList<>(Arrays.asList(new GLFWInputSource(true)));
   }

   @Override
   public void initializeInputSources(GameWindow window) {
      for (InputSource inputSource : inputSources) {
         inputSource.initialize(window);
      }
   }

   @Override
   public void dispose() {
      if (inputSources != null) {
         for (InputSource inputSource : inputSources) {
            inputSource.dispose();
         }
      }
   }

   @Override
   public boolean showControllerPanel(ControllerHandle controllerHandle) {
      return false;
   }

   @Override
   public void triggerVibration(ControllerHandle controllerHandle, float leftIntensity, float rightIntensity) {
   }

   @Override
   public void setCursorPosition(int mouseWindowX, int mouseWindowY) {
      GLFW.glfwSetCursorPos(((GLFWGameWindow)WindowManager.getWindow()).getGlfwWindow(), mouseWindowX, mouseWindowY);
   }

   @Override
   public String getKeyName(int key) {
      return GLFW.glfwGetKeyName(key, 0).toUpperCase();
   }
}
