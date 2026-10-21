package necesse.engine.platforms.sharedOnPC.input;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import necesse.engine.GameLog;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.controller.ControllerAnalogState;
import necesse.engine.input.controller.ControllerBind;
import necesse.engine.input.controller.ControllerButtonState;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerGlyphCollections;
import necesse.engine.input.controller.ControllerHandle;
import necesse.engine.input.controller.ControllerInputState;
import necesse.engine.platforms.sharedOnPC.input.controller.GLFWControllerHandle;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.MergeFunction;
import org.lwjgl.glfw.GLFWGamepadState;

public class GLFWControllerBind extends ControllerBind {
   private final HashMap<ControllerHandle, GameTexture> controllerGlyphs = new HashMap<>();
   private GLFWControllerBind.BindType bindType;
   private int[] binds;

   public GLFWControllerBind(GLFWControllerBind.BindType bindType, int[] binds) {
      this.bindType = bindType;
      this.binds = Arrays.stream(binds).sorted().toArray();
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.bindType, Arrays.hashCode(this.binds));
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         GLFWControllerBind that = (GLFWControllerBind)o;
         return this.bindType == that.bindType && Objects.deepEquals(this.binds, that.binds);
      } else {
         return false;
      }
   }

   @Override
   public boolean isBound() {
      return this.bindType != GLFWControllerBind.BindType.NotSet && this.binds.length > 0;
   }

   @Override
   public void saveBind(SaveData saveData) {
      saveData.addEnum("glfw_bindtype", this.bindType);
      saveData.addIntArray("glfw_binds", this.binds);
   }

   @Override
   public void loadBind(LoadData loadData) {
      if (loadData != null && !loadData.isEmpty()) {
         this.bindType = loadData.getEnum(GLFWControllerBind.BindType.class, "glfw_bindtype");
         this.binds = loadData.getIntArray("glfw_binds");
      }
   }

   @Override
   public GameTexture getGlyph(ControllerHandle controllerHandle) {
      return this.controllerGlyphs.getOrDefault(controllerHandle, this.findGlyph((GLFWControllerHandle)controllerHandle));
   }

   private GameTexture findGlyph(GLFWControllerHandle controllerHandle) {
      if (!this.isBound()) {
         return null;
      }

      ControllerGlyphCollections.GlyphCollection glyphCollection = null;
      if (this.bindType == GLFWControllerBind.BindType.NotSet) {
         return null;
      }

      if (this.bindType == GLFWControllerBind.BindType.GamepadButton) {
         switch (this.binds[0]) {
            case 0:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_A;
               break;
            case 1:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_B;
               break;
            case 2:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_X;
               break;
            case 3:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_Y;
               break;
            case 4:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_LEFT_BUMPER;
               break;
            case 5:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_RIGHT_BUMPER;
               break;
            case 6:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_BACK;
               break;
            case 7:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_START;
               break;
            case 8:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_GUIDE;
               break;
            case 9:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_LEFT_THUMB;
               break;
            case 10:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_RIGHT_THUMB;
               break;
            case 11:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_DPAD_UP;
               break;
            case 12:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_DPAD_RIGHT;
               break;
            case 13:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_DPAD_DOWN;
               break;
            case 14:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_BUTTON_DPAD_LEFT;
         }
      } else if (this.bindType == GLFWControllerBind.BindType.GamepadAxis) {
         switch (this.binds[0] + 100) {
            case 100:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_AXIS_LEFT_X;
               break;
            case 101:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_AXIS_LEFT_Y;
               break;
            case 102:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_AXIS_RIGHT_X;
               break;
            case 103:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_AXIS_RIGHT_Y;
               break;
            case 104:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_AXIS_LEFT_TRIGGER;
               break;
            case 105:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_AXIS_RIGHT_TRIGGER;
               break;
            case 206:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_AXIS_LEFT;
               break;
            case 207:
               glyphCollection = ControllerGlyphCollections.GAMEPAD_AXIS_RIGHT;
         }
      } else {
         if (this.bindType == GLFWControllerBind.BindType.Axis) {
            if (controllerHandle.type == ControllerHandle.ControllerType.XboxGeneric && ControllerGlyphCollections.XBOXGENERIC.containsKey(this.binds[0] + 100)
               )
             {
               return ControllerGlyphCollections.XBOXGENERIC.get(this.binds[0] + 100).getTexture();
            }

            if (controllerHandle.type == ControllerHandle.ControllerType.XboxLinuxWireless
               && ControllerGlyphCollections.XBOXLINUXWIRELESS.containsKey(this.binds[0] + 100)) {
               return ControllerGlyphCollections.XBOXLINUXWIRELESS.get(this.binds[0] + 100).getTexture();
            }

            if (controllerHandle.type == ControllerHandle.ControllerType.XboxLinuxWired
               && ControllerGlyphCollections.XBOXLINUXWIRED.containsKey(this.binds[0] + 100)) {
               return ControllerGlyphCollections.XBOXLINUXWIRED.get(this.binds[0] + 100).getTexture();
            }

            GameTexture texture = GameTexture.fromFile("ui/input/Generic/generic_stick", true).croppedToNonTransparent(true);
            this.mergeWithNumber(texture, this.binds[0], 0, -5);
            this.controllerGlyphs.put(controllerHandle, texture);
            return texture;
         }

         if (this.bindType == GLFWControllerBind.BindType.Hat) {
            GameTexture texture = GameTexture.fromFile("ui/input/Generic/xbox_dpad", true).croppedToNonTransparent(true);
            this.mergeWithNumber(texture, this.binds[0], 0, 0);
            this.controllerGlyphs.put(controllerHandle, texture);
            return texture;
         }

         if (this.bindType == GLFWControllerBind.BindType.Button) {
            if (controllerHandle.type == ControllerHandle.ControllerType.XboxGeneric && ControllerGlyphCollections.XBOXGENERIC.containsKey(this.binds[0])) {
               return ControllerGlyphCollections.XBOXGENERIC.get(this.binds[0]).getTexture();
            }

            if (controllerHandle.type == ControllerHandle.ControllerType.XboxLinuxWireless
               && ControllerGlyphCollections.XBOXLINUXWIRELESS.containsKey(this.binds[0])) {
               return ControllerGlyphCollections.XBOXLINUXWIRELESS.get(this.binds[0]).getTexture();
            }

            if (controllerHandle.type == ControllerHandle.ControllerType.XboxLinuxWired && ControllerGlyphCollections.XBOXLINUXWIRED.containsKey(this.binds[0])
               )
             {
               return ControllerGlyphCollections.XBOXLINUXWIRED.get(this.binds[0]).getTexture();
            }

            HashMap<Integer, ControllerGlyphCollections.Glyph> glyphMap = null;
            switch (controllerHandle.type) {
               case PlayStation4:
                  glyphMap = ControllerGlyphCollections.PLAYSTATION4_NON_GAMEPAD_GLYPHS;
                  break;
               case PlayStation5:
                  glyphMap = ControllerGlyphCollections.PLAYSTATION5_NON_GAMEPAD_GLYPHS;
            }

            if (glyphMap != null && glyphMap.containsKey(this.binds[0])) {
               GameTexture texture = glyphMap.get(this.binds[0]).getTexture();
               this.controllerGlyphs.put(controllerHandle, texture);
               return texture;
            }

            GameTexture texture = GameTexture.fromFile("ui/input/Generic/generic_button_circle", true).croppedToNonTransparent(true);
            this.mergeWithNumber(texture, this.binds[0], 0, 0);
            this.controllerGlyphs.put(controllerHandle, texture);
            return texture;
         }
      }

      if (glyphCollection != null) {
         GameTexture glyph = null;
         switch (controllerHandle.type) {
            case PlayStation4:
               glyph = glyphCollection.playStation4.getTexture();
               break;
            case PlayStation5:
               glyph = glyphCollection.playStation5.getTexture();
               break;
            case Xbox:
               glyph = glyphCollection.xbox.getTexture();
         }

         this.controllerGlyphs.put(controllerHandle, glyph);
         return glyph;
      } else {
         return null;
      }
   }

   private void mergeWithNumber(GameTexture texture, int number, int xOffset, int yOffset) {
      if (number >= 0 && number <= 99) {
         char[] digits = String.valueOf(number).toCharArray();
         GameTexture firstFlair = this.getTextureForDigit(Character.getNumericValue(digits[0]));
         int startY = (texture.getHeight() - firstFlair.getHeight()) / 2 + yOffset;
         if (digits.length == 1) {
            int startXSingle = (texture.getWidth() - firstFlair.getWidth()) / 2;
            texture.merge(firstFlair, startXSingle + xOffset, startY, MergeFunction.NORMAL);
         } else {
            GameTexture secondFlair = this.getTextureForDigit(Character.getNumericValue(digits[1]));
            int combinedWidth = firstFlair.getWidth() + secondFlair.getWidth() + 1;
            int startXFirst = (texture.getWidth() - combinedWidth) / 2;
            int startXSecond = texture.getWidth() / 2 + 1;
            texture.merge(firstFlair, startXFirst + xOffset, startY, MergeFunction.NORMAL);
            texture.merge(secondFlair, startXSecond + xOffset, startY, MergeFunction.NORMAL);
         }
      } else {
         GameLog.warn.println("Could not create glyph for button with number below 0 or above 99. Number is: " + number);
      }
   }

   private GameTexture getTextureForDigit(int digit) {
      switch (digit) {
         case 0:
            return ControllerGlyphCollections.FLAIR_0.getTexture();
         case 1:
            return ControllerGlyphCollections.FLAIR_1.getTexture();
         case 2:
            return ControllerGlyphCollections.FLAIR_2.getTexture();
         case 3:
            return ControllerGlyphCollections.FLAIR_3.getTexture();
         case 4:
            return ControllerGlyphCollections.FLAIR_4.getTexture();
         case 5:
            return ControllerGlyphCollections.FLAIR_5.getTexture();
         case 6:
            return ControllerGlyphCollections.FLAIR_6.getTexture();
         case 7:
            return ControllerGlyphCollections.FLAIR_7.getTexture();
         case 8:
            return ControllerGlyphCollections.FLAIR_8.getTexture();
         case 9:
            return ControllerGlyphCollections.FLAIR_9.getTexture();
         default:
            return null;
      }
   }

   public int[] getBinds() {
      return this.binds;
   }

   public GLFWControllerBind.BindType getBindType() {
      return this.bindType;
   }

   public boolean updateStateIfInput(
      ControllerInputState state, GLFWControllerHandle controllerHandle, ArrayList<ControllerEvent> outEvents, TickManager tickManager
   ) {
      if (this.bindType == GLFWControllerBind.BindType.NotSet) {
         return false;
      }

      GLFWGamepadState gamepadState = controllerHandle.getGamepadState();
      ByteBuffer buttonStates = controllerHandle.getButtonStates();
      FloatBuffer axisStates = controllerHandle.getAxisStates();
      ByteBuffer hatStates = controllerHandle.getHatStates();
      if (state instanceof ControllerButtonState) {
         int firstBind = this.binds[0];
         if (this.bindType == GLFWControllerBind.BindType.GamepadButton && firstBind < 15 && gamepadState.buttons(firstBind) == 1
            || this.bindType == GLFWControllerBind.BindType.GamepadAxis && firstBind < 6 && gamepadState.axes(firstBind) >= 0.25F
            || this.bindType == GLFWControllerBind.BindType.Button
               && buttonStates != null
               && firstBind < buttonStates.capacity()
               && buttonStates.get(firstBind) == 1
            || this.bindType == GLFWControllerBind.BindType.Axis
               && axisStates != null
               && firstBind < axisStates.capacity()
               && axisStates.get(firstBind) >= 0.25F) {
            ((ControllerButtonState)state).updateState(true, controllerHandle, this, outEvents, tickManager);
            return true;
         }
      } else if (state instanceof ControllerAnalogState) {
         ControllerAnalogState controllerAnalogState = (ControllerAnalogState)state;
         float x = 0.0F;
         float y = 0.0F;
         int firstBind = this.binds[0];
         if (this.bindType == GLFWControllerBind.BindType.GamepadAxis) {
            if (firstBind == 106) {
               x = gamepadState.axes(0);
               y = gamepadState.axes(1);
            } else if (firstBind == 107) {
               x = gamepadState.axes(2);
               y = gamepadState.axes(3);
            }
         } else if (this.bindType == GLFWControllerBind.BindType.Hat && hatStates != null && this.binds[0] < hatStates.capacity()) {
            byte hatState = hatStates.get(this.binds[0]);
            if ((hatState & 2) != 0) {
               x = 1.0F;
            }

            if ((hatState & 4) != 0) {
               y = 1.0F;
            }

            if ((hatState & 8) != 0) {
               x = -1.0F;
            }

            if ((hatState & 1) != 0) {
               y = -1.0F;
            }
         } else if (this.bindType == GLFWControllerBind.BindType.Axis
            && axisStates != null
            && this.binds[0] < axisStates.capacity()
            && this.binds[1] < axisStates.capacity()) {
            x = axisStates.get(this.binds[0]);
            y = axisStates.get(this.binds[1]);
         }

         boolean analogIsNotZero = x <= -0.1F || x >= 0.1F || y < -0.1F || y >= 0.1F;
         if (analogIsNotZero) {
            controllerAnalogState.updateState(x, y, controllerHandle, outEvents);
            return true;
         }
      }

      return false;
   }

   public enum BindType {
      NotSet,
      GamepadButton,
      GamepadAxis,
      Button,
      Axis,
      Hat;
   }
}
