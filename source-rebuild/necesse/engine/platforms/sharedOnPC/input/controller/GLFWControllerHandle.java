package necesse.engine.platforms.sharedOnPC.input.controller;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Objects;
import necesse.engine.input.InputSource;
import necesse.engine.input.controller.ControllerHandle;
import necesse.engine.platforms.Platform;
import necesse.engine.platforms.PlatformManager;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

public class GLFWControllerHandle extends ControllerHandle {
   public final int nativeControllerHandle;
   public final String name;
   public final String guid;
   private final GLFWGamepadState gamepadState = GLFWGamepadState.create();
   private ByteBuffer buttonStates;
   private FloatBuffer axisStates;
   private ByteBuffer hatStates;

   public GLFWControllerHandle(InputSource inputSource, int nativeControllerHandle) {
      super(inputSource, getControllerType(nativeControllerHandle));
      this.nativeControllerHandle = nativeControllerHandle;
      this.name = GLFW.glfwJoystickIsGamepad(nativeControllerHandle)
         ? GLFW.glfwGetGamepadName(nativeControllerHandle)
         : GLFW.glfwGetJoystickName(nativeControllerHandle);
      this.guid = GLFW.glfwGetJoystickGUID(nativeControllerHandle);
   }

   private static ControllerHandle.ControllerType getControllerType(int nativeControllerHandle) {
      String gamepadName = GLFW.glfwGetGamepadName(nativeControllerHandle);
      String joystickName = GLFW.glfwGetJoystickName(nativeControllerHandle);
      if (gamepadName != null && gamepadName.toLowerCase().contains("xinput")) {
         return ControllerHandle.ControllerType.Xbox;
      }

      if (Objects.equals(gamepadName, "PS4 Controller")) {
         return ControllerHandle.ControllerType.PlayStation4;
      }

      if (!Objects.equals(gamepadName, "PS5 Controller") && !Objects.equals(gamepadName, "Sony DualSense")) {
         if (joystickName == null || !joystickName.toLowerCase().contains("xbox")) {
            return ControllerHandle.ControllerType.Unknown;
         } else if (PlatformManager.getPlatform().getOperatingSystemFamily() != Platform.OperatingSystemFamily.Linux) {
            return ControllerHandle.ControllerType.XboxGeneric;
         } else {
            return joystickName.contains("Wireless") ? ControllerHandle.ControllerType.XboxLinuxWireless : ControllerHandle.ControllerType.XboxLinuxWired;
         }
      } else {
         return ControllerHandle.ControllerType.PlayStation5;
      }
   }

   public void updateStates() {
      GLFW.glfwGetGamepadState(this.nativeControllerHandle, this.gamepadState);
      this.buttonStates = GLFW.glfwGetJoystickButtons(this.nativeControllerHandle);
      this.axisStates = GLFW.glfwGetJoystickAxes(this.nativeControllerHandle);
      this.hatStates = GLFW.glfwGetJoystickHats(this.nativeControllerHandle);
   }

   public boolean isSupportedAsGamepad() {
      switch (this.type) {
         case XboxGeneric:
         case Unknown:
         case XboxLinuxWireless:
         case XboxLinuxWired:
            return false;
         case Xbox:
         case PlayStation4:
         case PlayStation5:
            return true;
         default:
            return false;
      }
   }

   public GLFWGamepadState getGamepadState() {
      return this.gamepadState;
   }

   public ByteBuffer getButtonStates() {
      return this.buttonStates;
   }

   public FloatBuffer getAxisStates() {
      return this.axisStates;
   }

   public ByteBuffer getHatStates() {
      return this.hatStates;
   }

   public enum controllerType {
      Unknown,
      PlayStation,
      Xbox,
      NintendoSwitch,
      SteamDeck;
   }
}
