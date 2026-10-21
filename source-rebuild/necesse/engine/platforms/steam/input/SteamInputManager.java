package necesse.engine.platforms.steam.input;

import com.codedisaster.steamworks.SteamController;
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

public class SteamInputManager extends InputManager {
   private SteamController steamController;
   private SteamControllerInputSource steamControllerInputSource;

   @Override
   public void initialize() {
      this.steamController = new SteamController();
      this.steamController.init();
      GLFWInputSource glfwInputSource = new GLFWInputSource(true);
      glfwInputSource.allowControllerRebinding = false;
      this.steamControllerInputSource = new SteamControllerInputSource(this.steamController);
      InputManager.inputSources = new ArrayList<>(Arrays.asList(glfwInputSource, this.steamControllerInputSource));
   }

   @Override
   public void initializeInputSources(GameWindow window) {
      for (InputSource inputSource : InputManager.inputSources) {
         inputSource.initialize(window);
      }
   }

   @Override
   public void dispose() {
      if (InputManager.inputSources != null) {
         for (InputSource inputSource : InputManager.inputSources) {
            inputSource.dispose();
         }
      }

      if (this.steamController != null) {
         this.steamController.shutdown();
      }
   }

   @Override
   public boolean showControllerPanel(ControllerHandle controllerHandle) {
      if (controllerHandle instanceof SteamGameControllerHandle) {
         this.steamController.showBindingPanel(((SteamGameControllerHandle)controllerHandle).steamNativeControllerHandle);
         return true;
      } else {
         this.steamController.showBindingPanel(this.steamControllerInputSource.STEAM_CONTROLLER_HANDLE_ALL_CONTROLLERS.steamNativeControllerHandle);
         return true;
      }
   }

   @Override
   public void triggerVibration(ControllerHandle controllerHandle, float leftIntensity, float rightIntensity) {
      short left = (short)(leftIntensity * 65535.0F);
      short right = (short)(rightIntensity * 65535.0F);
      this.steamController.triggerVibration(((SteamGameControllerHandle)controllerHandle).steamNativeControllerHandle, left, right);
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
