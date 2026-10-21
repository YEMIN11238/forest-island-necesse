package necesse.engine.input;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Stream;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.controller.ControllerButtonState;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.platforms.Platform;
import necesse.engine.window.WindowManager;
import necesse.gfx.GameColor;
import necesse.gfx.fairType.FairTypeDrawOptions;
import necesse.gfx.gameFont.CustomGameFont;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;

public class Control {
   private static boolean controlsLoaded;
   private static final ArrayList<Control> controls = new ArrayList<>();
   private static final HashMap<String, Integer> controlsIDs = new HashMap<>();
   private static final HashSet<Control.ControlGroup> controlGroups = new HashSet<>();
   private static final HashMap<String, Control.ControlGroup> modGroups = new HashMap<>();
   private int key;
   public final int defaultKey;
   private boolean isPressed;
   private boolean isReleased;
   public final GameMessage text;
   public GameMessage tooltip;
   public final String id;
   public final LoadedMod mod;
   private Control.ControlGroup group;
   private final int overlapGroup;
   private InputEvent activateEvent;
   public ControllerButtonState controllerState;
   private final MouseWheelBuffer wheelBuffer = new MouseWheelBuffer(false);
   private static InputManager inputManager;
   public static Control MOUSE1;
   public static Control MOUSE2;
   public static Control MOVE_UP;
   public static Control MOVE_DOWN;
   public static Control MOVE_LEFT;
   public static Control MOVE_RIGHT;
   public static Control MOVE_TO_MOUSE;
   public static Control INVENTORY;
   public static Control HEALTH_POT;
   public static Control MANA_POT;
   public static Control BUFF_POTS;
   public static Control EAT_FOOD;
   public static Control USE_MOUNT;
   public static Control SET_ABILITY;
   public static Control TRINKET_ABILITY;
   public static Control PLACE_TORCH;
   public static Control EXPRESSION_WHEEL;
   public static Control OPEN_ADVENTURE_PARTY;
   public static Control OPEN_ADVENTURE_JOURNAL;
   public static Control OPEN_SETTLEMENT;
   public static Control SMART_MINING;
   public static Control LOOT_ALL;
   public static Control SORT_INVENTORY;
   public static Control QUICK_STACK;
   public static Control NEXT_HOTBAR;
   public static Control PREV_HOTBAR;
   public static Control HOTBAR_SLOT_0;
   public static Control HOTBAR_SLOT_1;
   public static Control HOTBAR_SLOT_2;
   public static Control HOTBAR_SLOT_3;
   public static Control HOTBAR_SLOT_4;
   public static Control HOTBAR_SLOT_5;
   public static Control HOTBAR_SLOT_6;
   public static Control HOTBAR_SLOT_7;
   public static Control HOTBAR_SLOT_8;
   public static Control HOTBAR_SLOT_9;
   public static Control ITEM_SET_0;
   public static Control ITEM_SET_1;
   public static Control ITEM_SET_2;
   public static Control ITEM_SET_3;
   public static Control SHOW_MAP;
   public static Control SCOREBOARD;
   public static Control HIDE_CURSOR;
   public static Control HIDE_UI;
   public static Control SCREENSHOT;
   public static Control ZOOM_IN;
   public static Control ZOOM_OUT;
   public static Control PIPETTE;
   public static Control ERASER;
   public static Control DEBUG_INFO;
   public static Control DEBUG_HUD;
   public static Control INV_LOCK;
   public static Control INV_QUICK_MOVE;
   public static Control INV_QUICK_TRASH;
   public static Control INV_QUICK_DROP;
   public static Control CRAFT_10;
   public static Control CRAFT_ALL;
   public static Control[] HOTBAR_SLOTS;
   public static Control[] ITEM_SETS;

   public Control(int defaultKey, String id) {
      this(defaultKey, id, new LocalMessage("controls", id), "default");
   }

   public Control(int defaultKey, String id, String overlapGroup) {
      this(defaultKey, id, new LocalMessage("controls", id), overlapGroup);
   }

   public Control(int defaultKey, String id, GameMessage text) {
      this(defaultKey, id, text, "default");
   }

   public Control(int defaultKey, String id, GameMessage text, String overlapGroup) {
      this.key = defaultKey;
      this.defaultKey = defaultKey;
      this.id = id;
      this.text = text;
      this.overlapGroup = overlapGroup.hashCode();
      this.mod = LoadedMod.getRunningMod();
   }

   public Control setTooltip(GameMessage message) {
      this.tooltip = message;
      return this;
   }

   public void changeKey(int key) {
      this.key = key;
   }

   public void activate(InputEvent event) {
      if (event.getID() == -103) {
         this.wheelBuffer.add(event);
         if (this.wheelBuffer.useAllScrollY() > 0) {
            this.activateEvent = event;
            this.isPressed = true;
         } else {
            this.activateEvent = event;
            this.isReleased = true;
         }
      } else if (event.getID() == -102) {
         this.wheelBuffer.add(event);
         if (this.wheelBuffer.useAllScrollY() < 0) {
            this.activateEvent = event;
            this.isPressed = true;
         } else {
            this.activateEvent = event;
            this.isReleased = true;
         }
      } else {
         this.activateEvent = event;
         if (event.state) {
            this.isPressed = true;
         } else {
            this.isReleased = true;
         }
      }
   }

   public void reset() {
      this.isPressed = false;
      this.isReleased = false;
   }

   public void restoreDefaultKey() {
      this.changeKey(this.defaultKey);
   }

   public boolean isDown() {
      if (!WindowManager.getWindow().isFocused()) {
         return false;
      } else if (this.activateEvent == null || this.activateEvent.isUsed()) {
         return false;
      } else {
         return this.activateEvent.isMouseWheelEvent() ? this.isPressed : this.activateEvent.state;
      }
   }

   public boolean isPressed() {
      return this.activateEvent != null && !this.activateEvent.isUsed() ? this.isPressed : false;
   }

   public boolean isReleased() {
      return this.activateEvent != null && !this.activateEvent.isUsed() ? this.isReleased : false;
   }

   public boolean isDefaultBind() {
      return this.key == this.defaultKey;
   }

   public InputEvent getEvent() {
      return this.activateEvent;
   }

   public boolean isMouseWheel() {
      return InputEvent.isMouseWheelEvent(this.getKey());
   }

   public int getKey() {
      return this.key;
   }

   public String getKeyName() {
      return Input.getName(this.getKey());
   }

   public String getDefaultKeyName() {
      return Input.getName(this.defaultKey);
   }

   public boolean overlaps(Control other) {
      return other != this && this.getKey() != -1 && other.overlapGroup == this.overlapGroup && this.getKey() == other.getKey();
   }

   public static void loadControls() {
      if (!controlsLoaded) {
         Control.ControlGroup GROUP_GENERAL = new Control.ControlGroup(100, new LocalMessage("controls", "groupgeneral"));
         Control.ControlGroup GROUP_MOVEMENT = new Control.ControlGroup(200, new LocalMessage("controls", "groupmovement"));
         Control.ControlGroup GROUP_MISC = new Control.ControlGroup(300, new LocalMessage("controls", "groupmisc"));
         Control.ControlGroup GROUP_HOTBAR = new Control.ControlGroup(400, new LocalMessage("controls", "grouphotbar"));
         Control.ControlGroup GROUP_ITEM_SETS = new Control.ControlGroup(500, new LocalMessage("controls", "groupitemsets"));
         Control.ControlGroup GROUP_KEYMODS = new Control.ControlGroup(600, new LocalMessage("controls", "groupkeymods"));
         Control.ControlGroup GROUP_DEBUG = new Control.ControlGroup(700, new LocalMessage("controls", "groupdebug"));
         MOUSE1 = addControl(new Control(-100, "mouse1"), GROUP_GENERAL);
         MOUSE2 = addControl(new Control(-99, "mouse2"), GROUP_GENERAL);
         MOVE_UP = addControl(new Control(87, "moveup"), GROUP_MOVEMENT);
         MOVE_LEFT = addControl(new Control(65, "moveleft"), GROUP_MOVEMENT);
         MOVE_DOWN = addControl(new Control(83, "movedown"), GROUP_MOVEMENT);
         MOVE_RIGHT = addControl(new Control(68, "moveright"), GROUP_MOVEMENT);
         MOVE_TO_MOUSE = addControl(new Control(-1, "movetomouse"), GROUP_MOVEMENT);
         INVENTORY = addControl(new Control(69, "inventory"), GROUP_GENERAL);
         HEALTH_POT = addControl(new Control(81, "healthpotion"), GROUP_GENERAL);
         MANA_POT = addControl(new Control(90, "manapotion"), GROUP_GENERAL);
         EAT_FOOD = addControl(new Control(-1, "eatfood"), GROUP_GENERAL);
         PIPETTE = addControl(new Control(-98, "pipette").setTooltip(new LocalMessage("controls", "pipettetip")), GROUP_MISC);
         ERASER = addControl(new Control(71, "eraser").setTooltip(new LocalMessage("controls", "erasertip")), GROUP_MISC);
         SCOREBOARD = addControl(new Control(258, "scoreboard"), GROUP_MISC);
         SHOW_MAP = addControl(new Control(77, "showmap", new LocalMessage("controls", "showworldmap")), GROUP_MISC);
         ZOOM_IN = addControl(new Control(334, "zoomin"), GROUP_MISC);
         ZOOM_OUT = addControl(new Control(333, "zoomout"), GROUP_MISC);
         HIDE_CURSOR = addControl(new Control(292, "hidecursor"), GROUP_MISC);
         HIDE_UI = addControl(new Control(293, "hideui"), GROUP_MISC);
         SCREENSHOT = addControl(new Control(294, "screenshot"), GROUP_MISC);
         DEBUG_INFO = addControl(new Control(290, "debuginfo"), GROUP_DEBUG);
         DEBUG_HUD = addControl(new Control(291, "debughud"), GROUP_DEBUG);
         BUFF_POTS = addControl(new Control(66, "buffpotions"), GROUP_GENERAL);
         PLACE_TORCH = addControl(new Control(82, "placetorch"), GROUP_GENERAL);
         EXPRESSION_WHEEL = addControl(new Control(84, "expressionwheel"), GROUP_GENERAL);
         USE_MOUNT = addControl(new Control(70, "usemount"), GROUP_GENERAL);
         SET_ABILITY = addControl(new Control(86, "setability"), GROUP_GENERAL);
         TRINKET_ABILITY = addControl(new Control(32, "trinketability"), GROUP_GENERAL);
         OPEN_ADVENTURE_PARTY = addControl(new Control(88, "openadventureparty"), GROUP_GENERAL);
         OPEN_ADVENTURE_JOURNAL = addControl(new Control(74, "openadventurejournal"), GROUP_GENERAL);
         OPEN_SETTLEMENT = addControl(new Control(67, "opensettlement"), GROUP_GENERAL);
         SMART_MINING = addControl(new Control(341, "smartmining"), GROUP_GENERAL);
         LOOT_ALL = addControl(new Control(-1, "lootall"), GROUP_GENERAL);
         SORT_INVENTORY = addControl(new Control(-1, "sortinventory"), GROUP_GENERAL);
         QUICK_STACK = addControl(new Control(-1, "quickstack"), GROUP_GENERAL);
         NEXT_HOTBAR = addControl(new Control(-102, "nexthotbar"), GROUP_HOTBAR);
         PREV_HOTBAR = addControl(new Control(-103, "prevhotbar"), GROUP_HOTBAR);
         HOTBAR_SLOT_0 = addControl(new Control(49, "hotbarslot0", new LocalMessage("controls", "hotbarslot").addReplacement("number", "1")), GROUP_HOTBAR);
         HOTBAR_SLOT_1 = addControl(new Control(50, "hotbarslot1", new LocalMessage("controls", "hotbarslot").addReplacement("number", "2")), GROUP_HOTBAR);
         HOTBAR_SLOT_2 = addControl(new Control(51, "hotbarslot2", new LocalMessage("controls", "hotbarslot").addReplacement("number", "3")), GROUP_HOTBAR);
         HOTBAR_SLOT_3 = addControl(new Control(52, "hotbarslot3", new LocalMessage("controls", "hotbarslot").addReplacement("number", "4")), GROUP_HOTBAR);
         HOTBAR_SLOT_4 = addControl(new Control(53, "hotbarslot4", new LocalMessage("controls", "hotbarslot").addReplacement("number", "5")), GROUP_HOTBAR);
         HOTBAR_SLOT_5 = addControl(new Control(54, "hotbarslot5", new LocalMessage("controls", "hotbarslot").addReplacement("number", "6")), GROUP_HOTBAR);
         HOTBAR_SLOT_6 = addControl(new Control(55, "hotbarslot6", new LocalMessage("controls", "hotbarslot").addReplacement("number", "7")), GROUP_HOTBAR);
         HOTBAR_SLOT_7 = addControl(new Control(56, "hotbarslot7", new LocalMessage("controls", "hotbarslot").addReplacement("number", "8")), GROUP_HOTBAR);
         HOTBAR_SLOT_8 = addControl(new Control(57, "hotbarslot8", new LocalMessage("controls", "hotbarslot").addReplacement("number", "9")), GROUP_HOTBAR);
         HOTBAR_SLOT_9 = addControl(new Control(48, "hotbarslot9", new LocalMessage("controls", "hotbarslot").addReplacement("number", "10")), GROUP_HOTBAR);
         HOTBAR_SLOTS = new Control[]{
            HOTBAR_SLOT_0,
            HOTBAR_SLOT_1,
            HOTBAR_SLOT_2,
            HOTBAR_SLOT_3,
            HOTBAR_SLOT_4,
            HOTBAR_SLOT_5,
            HOTBAR_SLOT_6,
            HOTBAR_SLOT_7,
            HOTBAR_SLOT_8,
            HOTBAR_SLOT_9
         };
         ITEM_SET_0 = addControl(new Control(-1, "itemset0", new LocalMessage("controls", "itemset").addReplacement("number", "1")), GROUP_ITEM_SETS);
         ITEM_SET_1 = addControl(new Control(-1, "itemset1", new LocalMessage("controls", "itemset").addReplacement("number", "2")), GROUP_ITEM_SETS);
         ITEM_SET_2 = addControl(new Control(-1, "itemset2", new LocalMessage("controls", "itemset").addReplacement("number", "3")), GROUP_ITEM_SETS);
         ITEM_SET_3 = addControl(new Control(-1, "itemset3", new LocalMessage("controls", "itemset").addReplacement("number", "4")), GROUP_ITEM_SETS);
         ITEM_SETS = new Control[]{ITEM_SET_0, ITEM_SET_1, ITEM_SET_2, ITEM_SET_3};
         INV_LOCK = addControl(new Control(342, "invlock", "invmods"), GROUP_KEYMODS);
         INV_QUICK_MOVE = addControl(new Control(340, "invquickmove", "invmods"), GROUP_KEYMODS);
         INV_QUICK_TRASH = addControl(new Control(341, "invtrash", "invmods"), GROUP_KEYMODS);
         INV_QUICK_DROP = addControl(new Control(-1, "invdrop", "invmods"), GROUP_KEYMODS);
         CRAFT_10 = addControl(new Control(340, "craft10", "craftmods"), GROUP_KEYMODS);
         CRAFT_ALL = addControl(new Control(-1, "craftall", "craftmods"), GROUP_KEYMODS);
         inputManager = Platform.getInputManager();
         ControllerInput.init(inputManager);
         controlsLoaded = true;
      }
   }

   public static void tickControlInputs(Input input, boolean isTyping, TickManager tickManager) {
      for (Control control : controls) {
         if (control != null) {
            boolean reset = true;
            if (control.getKey() != -1 && (!InputEvent.isKeyboardEvent(control.key) || !isTyping)) {
               for (InputEvent event : input.getEvents()) {
                  if (event.getID() == control.getKey()) {
                     control.activate(event);
                     reset = false;
                  }
               }
            }

            if (reset) {
               control.reset();
            }
         }
      }

      ControllerInput.tick(tickManager);
   }

   public static void resetControls() {
      for (Control control : controls) {
         control.reset();
         control.activateEvent = null;
      }
   }

   public static Iterable<Control> getControls() {
      return controls;
   }

   public static Stream<Control> streamControls() {
      return controls.stream();
   }

   public static Iterable<Control.ControlGroup> getGroups() {
      return controlGroups;
   }

   public static Stream<Control.ControlGroup> streamGroups() {
      return controlGroups.stream();
   }

   public static boolean isControlsLoaded() {
      return controlsLoaded;
   }

   private static <T extends Control> T addControl(T control, Control.ControlGroup group) {
      if (controlsIDs.containsKey(control.id)) {
         throw new NullPointerException("ERROR: Conflicted control name: Could not add " + control.id);
      }

      controlsIDs.put(control.id, controls.size());
      controls.add(control);
      controlGroups.add(group);
      group.controls.add(control);
      return control;
   }

   public static <T extends Control> T addModControl(T control) {
      LoadedMod mod = LoadedMod.getRunningMod();
      if (mod == null) {
         throw new IllegalStateException("Cannot add controls when outside mod loading");
      }

      Control.ControlGroup group = modGroups.compute(mod.id, (s, last) -> {
         if (last == null) {
            last = new Control.ControlGroup(Integer.MAX_VALUE, new StaticMessage(mod.name));
         }

         return (Control.ControlGroup)last;
      });
      return addControl(control, group);
   }

   public static Control getControl(String id) {
      return controlsIDs.containsKey(id) ? controls.get(controlsIDs.get(id)) : null;
   }

   public static int getControlIconWidth(FontOptions fontOptions, String modifierKey, Control control, String keyName, FairTypeDrawOptions tooltip) {
      return getDrawControlLogic(fontOptions, 0, 0, modifierKey, control, keyName, tooltip).width;
   }

   public static void drawControlIcon(FontOptions fontOptions, int x, int y, String modifierKey, Control control, String keyName, FairTypeDrawOptions tooltip) {
      getDrawControlLogic(fontOptions, x, y, modifierKey, control, keyName, tooltip).draw();
   }

   public static int getControlIconWidth(FontOptions fontOptions, String modifierKey, int inputKey, String keyName, FairTypeDrawOptions tooltip) {
      return getDrawControlLogic(fontOptions, 0, 0, modifierKey, inputKey, keyName, tooltip).width;
   }

   public static void drawControlIcon(FontOptions fontOptions, int x, int y, String modifierKey, int inputKey, String keyName, FairTypeDrawOptions tooltip) {
      getDrawControlLogic(fontOptions, x, y, modifierKey, inputKey, keyName, tooltip).draw();
   }

   public static Control.DrawFlow getDrawControlLogic(
      FontOptions fontOptions, int x, int y, String modifierKey, Control control, String keyName, FairTypeDrawOptions tooltip
   ) {
      return getDrawControlLogic(fontOptions, x, y, modifierKey, control == null ? -1 : control.getKey(), keyName, tooltip);
   }

   public static Control.DrawFlow getDrawControlLogic(
      FontOptions fontOptions, int x, int y, String modifierKey, int inputKey, String keyName, FairTypeDrawOptions tooltip
   ) {
      int widthFlow = 0;
      ArrayList<Runnable> draw = new ArrayList<>();
      int iconDrawY;
      if (tooltip != null) {
         iconDrawY = y + tooltip.getBoundingBox().height - fontOptions.getSize() - 2;
      } else {
         iconDrawY = y + fontOptions.getSize() / 2 - 8;
      }

      if (modifierKey != null && !modifierKey.isEmpty()) {
         widthFlow += drawKey(x + widthFlow, iconDrawY, draw, modifierKey, fontOptions.getAlpha(), fontOptions.isPixelFont());
         int plusX = x + widthFlow;
         draw.add(() -> FontManager.bit.drawString(plusX, y + 2, "+", fontOptions));
         widthFlow += FontManager.bit.getWidthCeil("+", fontOptions) + 2;
      }

      if (inputKey < -1 && inputKey >= -100) {
         switch (inputKey) {
            case -100:
               widthFlow += drawMouse(x + widthFlow, iconDrawY, draw, 0, fontOptions.getAlpha());
               break;
            case -99:
               widthFlow += drawMouse(x + widthFlow, iconDrawY, draw, 1, fontOptions.getAlpha());
               break;
            case -98:
               widthFlow += drawMouse(x + widthFlow, iconDrawY, draw, 2, fontOptions.getAlpha());
               break;
            default:
               widthFlow += drawMouse(x + widthFlow, iconDrawY, draw, -1, fontOptions.getAlpha());
               int keyX = x + widthFlow;
               draw.add(() -> FontManager.bit.drawString(keyX, y + 2, keyName, fontOptions));
               widthFlow += FontManager.bit.getWidthCeil(keyName, fontOptions);
               String tipPrefix = " - ";
               int prefixX = x + widthFlow;
               draw.add(() -> FontManager.bit.drawString(prefixX, y, tipPrefix, fontOptions));
               widthFlow += FontManager.bit.getWidthCeil(tipPrefix, fontOptions);
         }
      } else {
         widthFlow += drawKey(x + widthFlow, iconDrawY, draw, keyName, fontOptions.getAlpha(), fontOptions.isPixelFont());
      }

      if (tooltip != null) {
         int tipX = x + widthFlow;
         Color defaultColor = fontOptions.getColorObj();
         if (defaultColor == null) {
            defaultColor = GameColor.DEFAULT_COLOR.get();
         }

         Color finalColor = new Color(
            defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), (int)(defaultColor.getAlpha() * fontOptions.getAlpha())
         );
         draw.add(() -> tooltip.draw(tipX + 2, y + 2, finalColor));
         widthFlow += tooltip.getBoundingBox().width + 4;
      }

      return new Control.DrawFlow(widthFlow, draw);
   }

   private static int drawMouse(int x, int y, ArrayList<Runnable> draw, int mouse, float alpha) {
      GameTexture texture = Settings.UI.input;
      int drawX = x + 2;
      int drawY = y + 2;
      draw.add(() -> texture.initDraw().sprite(0, 0, 16).alpha(alpha).draw(drawX, drawY));
      if (mouse == 0) {
         draw.add(() -> texture.initDraw().sprite(1, 0, 16).color(1.0F, 0.2F, 0.2F, alpha).draw(drawX, drawY));
      } else {
         draw.add(() -> texture.initDraw().sprite(1, 0, 16).alpha(alpha).draw(drawX, drawY));
      }

      if (mouse == 1) {
         draw.add(() -> texture.initDraw().sprite(2, 0, 16).color(1.0F, 0.2F, 0.2F, alpha).draw(drawX, drawY));
      } else {
         draw.add(() -> texture.initDraw().sprite(2, 0, 16).alpha(alpha).draw(drawX, drawY));
      }

      if (mouse == 2) {
         draw.add(() -> texture.initDraw().sprite(3, 0, 16).color(1.0F, 0.2F, 0.2F, alpha).draw(drawX, drawY));
      } else {
         draw.add(() -> texture.initDraw().sprite(3, 0, 16).alpha(alpha).draw(drawX, drawY));
      }

      return 16;
   }

   private static int drawKey(int x, int y, ArrayList<Runnable> draw, String keyName, float alpha, boolean pixelFont) {
      int drawX = x + 2;
      int drawY = y + 2;
      int fontOffset;
      int fontSize;
      if (pixelFont && !keyName.chars().anyMatch(ch -> !CustomGameFont.fontTextureContains((char)ch))) {
         fontSize = 16;
         fontOffset = 0;
      } else {
         fontSize = 15;
         fontOffset = 1;
      }

      FontOptions fontOptions = new FontOptions(fontSize).colorf(0.15F, 0.15F, 0.15F, alpha);
      if (pixelFont) {
         fontOptions.forcePixelFont();
      } else {
         fontOptions.forceNonPixelFont();
      }

      int keyFontWidth = FontManager.bit.getWidthCeil(keyName, fontOptions);
      int keyWidth = keyName.length() <= 1 ? Math.max(keyFontWidth, 12) : keyFontWidth;
      GameTexture texture = Settings.UI.input;

      for (int i = 0; i < keyWidth; i += 16) {
         int finalI = i;
         if (i > keyWidth - 16) {
            draw.add(
               () -> texture.initDraw()
                  .spriteSection(1, 1, 16, 0, finalI == 0 ? keyWidth : keyWidth % finalI, 0, 16)
                  .color(1.0F, 1.0F, 1.0F, alpha)
                  .draw(drawX + finalI, drawY)
            );
         } else {
            draw.add(() -> texture.initDraw().sprite(1, 1, 16).color(1.0F, 1.0F, 1.0F, alpha).draw(drawX + finalI, drawY));
         }
      }

      draw.add(() -> texture.initDraw().sprite(0, 1, 16).color(1.0F, 1.0F, 1.0F, alpha).draw(drawX - 16, drawY));
      draw.add(() -> texture.initDraw().sprite(2, 1, 16).color(1.0F, 1.0F, 1.0F, alpha).draw(drawX + keyWidth, drawY));
      int stringDrawX = drawX + keyWidth / 2 - keyFontWidth / 2;
      draw.add(() -> FontManager.bit.drawString(stringDrawX, drawY + fontOffset, keyName, fontOptions));
      return keyWidth + 8;
   }

   public static class ControlGroup {
      public final int sort;
      public final GameMessage displayName;
      private final ArrayList<Control> controls = new ArrayList<>();

      public ControlGroup(int sort, GameMessage displayName) {
         this.sort = sort;
         this.displayName = displayName;
      }

      public Iterable<Control> getControls() {
         return this.controls;
      }
   }

   public static class DrawFlow {
      public final int width;
      public final ArrayList<Runnable> drawLogic;

      public DrawFlow(int width, ArrayList<Runnable> drawLogic) {
         this.width = width;
         this.drawLogic = drawLogic;
      }

      public void draw() {
         this.drawLogic.forEach(Runnable::run);
      }
   }
}
