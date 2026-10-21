package necesse.engine.input;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.platforms.Platform;
import necesse.engine.window.GameWindow;

public class Input {
   public static long lastCursorInputTime;
   public static long lastKeyboardInputTime;
   public static long lastControllerInputTime;
   public static boolean lastInputIsController;
   final GameWindow window;
   protected final Object newEventsLock = new Object();
   protected ArrayList<InputEvent> newEvents = new ArrayList<>();
   protected List<InputEvent> events = new ArrayList<>();
   protected HashMap<Integer, InputEvent> downEvents = new HashMap<>();
   protected final Object repeatEventsLock = new Object();
   protected HashMap<Integer, Input.RepeatEventTracker> repeatEvents = new HashMap<>();
   private boolean clearInput = false;
   public static InputPosition mousePos = new InputPosition(0, 0, 0, 0, 0, 0);
   public static Point setCursorPos;
   public static boolean isTyping = false;
   public static boolean updateNextMousePos = false;
   private boolean submitNextMoveEvent = false;
   private static final HashMap<Integer, String> specialInputNames = new HashMap<>();

   public Input(GameWindow window) {
      this.window = window;
   }

   public void startRepeatEvents(InputEvent event, Object... callers) {
      if (event.getID() != -105) {
         if (!event.state) {
            throw new IllegalArgumentException("Cannot start from release events");
         }

         if (!event.isMouseClickEvent() && !event.isKeyboardEvent()) {
            throw new IllegalArgumentException("Event must be mouse click or keyboard");
         }

         synchronized (this.repeatEventsLock) {
            if (!this.repeatEvents.containsKey(event.getID())) {
               this.repeatEvents.put(event.getID(), new Input.RepeatEventTracker(callers, System.currentTimeMillis()));
            }
         }
      }
   }

   public void stopRepeatEvent(InputEvent event) {
      synchronized (this.repeatEventsLock) {
         this.repeatEvents.remove(event.getID());
      }
   }

   public void setCursorPosition(int mouseWindowX, int mouseWindowY, TickManager tickManager) {
      setCursorPos = new Point(mouseWindowX, mouseWindowY);
      mousePos = InputPosition.fromWindowPos(this.window, mouseWindowX, mouseWindowY);
      Platform.getInputManager().setCursorPosition(mouseWindowX, mouseWindowY);
      synchronized (this.newEventsLock) {
         this.newEvents.add(InputEvent.MouseMoveEvent(this.mousePos(), tickManager));
      }
   }

   public void tick(boolean _isTyping, TickManager tickManager) {
      isTyping = _isTyping;
      synchronized (this.repeatEventsLock) {
         for (int id : this.repeatEvents.keySet()) {
            Input.RepeatEventTracker tracker = this.repeatEvents.get(id);

            while (tracker.lastPressTime + tracker.nextPressCooldown <= System.currentTimeMillis()) {
               tracker.lastPressTime = System.currentTimeMillis();
               if (tracker.repeatPresses < 5) {
                  tracker.nextPressCooldown = 75;
               } else if (tracker.repeatPresses < 10) {
                  tracker.nextPressCooldown = 50;
               } else if (tracker.repeatPresses < 25) {
                  tracker.nextPressCooldown = 25;
               } else if (tracker.repeatPresses < 50) {
                  tracker.nextPressCooldown = 10;
               } else if (tracker.repeatPresses < 100) {
                  tracker.nextPressCooldown = 5;
               } else {
                  tracker.nextPressCooldown = 1;
               }

               tracker.repeatPresses++;
               tracker.lastPressTime = tracker.lastPressTime + tracker.nextPressCooldown;
               synchronized (this.newEventsLock) {
                  this.newEvents
                     .add(InputEvent.RepeatEvent(tracker.callers, tracker.repeatPresses, tracker.nextPressCooldown, this.mousePos(), tickManager, id));
               }
            }
         }
      }

      if (this.submitNextMoveEvent) {
         synchronized (this.newEventsLock) {
            this.newEvents.add(InputEvent.MouseMoveEvent(this.mousePos(), tickManager));
         }

         this.submitNextMoveEvent = false;
      }

      if (this.clearInput) {
         this.newEvents.clear();
         this.clearInput = false;
      }

      synchronized (this.newEventsLock) {
         this.newEvents.removeIf(Objects::isNull);
         this.events = Collections.unmodifiableList(this.newEvents);
         this.newEvents = new ArrayList<>();
      }

      lastInputIsController = lastControllerInputTime > lastKeyboardInputTime && lastControllerInputTime > lastCursorInputTime - 200L;
   }

   public void clearInput() {
      this.clearInput = true;
   }

   public List<InputEvent> getEvents() {
      return this.events;
   }

   public InputEvent getEvent(int key) {
      return this.events.stream().filter(e -> e.getID() == key).findFirst().orElse(null);
   }

   public boolean isPressed(int key) {
      return this.events.stream().anyMatch(e -> e.getID() == key && e.state);
   }

   public boolean isReleased(int key) {
      return this.events.stream().anyMatch(e -> e.getID() == key && !e.state);
   }

   public boolean hasChanged(int key) {
      return this.events.stream().anyMatch(e -> e.getID() == key);
   }

   public boolean isKeyDown(int key) {
      return this.downEvents.containsKey(key);
   }

   public InputPosition mousePos() {
      return mousePos;
   }

   public void updateNextMousePos() {
      updateNextMousePos = true;
   }

   public void submitNextMoveEvent() {
      this.submitNextMoveEvent = true;
   }

   public void submitNonButtonInputEvent(InputEvent event) {
      synchronized (this.newEventsLock) {
         this.newEvents.add(event);
      }
   }

   public void submitButtonInputEvent(InputEvent event) {
      synchronized (this.newEventsLock) {
         this.newEvents.add(event);
      }

      if (event.state) {
         this.downEvents.put(event.getID(), event);
      } else {
         int id = event.getID();
         event.downStateEvent = this.downEvents.get(id);
         this.downEvents.remove(id);
         this.stopRepeatEvent(event);
      }
   }

   public void dispose() {
   }

   public static boolean isFunctionKey(int id) {
      return id >= 0 && specialInputNames.containsKey(id);
   }

   public static int getSystemControlKey() {
      switch (org.lwjgl.system.Platform.get()) {
         case MACOSX:
            return 341;
         case WINDOWS:
         default:
            return 341;
      }
   }

   public static String getName(int id) {
      if (id < -1 && id >= -100) {
         return "MOUSE" + (id + 101);
      }

      String name = specialInputNames.getOrDefault(id, null);
      if (name != null) {
         return name;
      }

      try {
         switch (id) {
            case -1:
               return "NOT SET";
            case 32:
               return "SPACE";
            default:
               return Platform.getInputManager().getKeyName(id);
         }
      } catch (Exception e) {
         return "N/A";
      }
   }

   public static String getName(InputEvent event) {
      return getName(event.getID());
   }

   static {
      specialInputNames.put(-102, "SCROLLDOWN");
      specialInputNames.put(-103, "SCROLLUP");
      specialInputNames.put(-101, "MOUSEMOVE");
      specialInputNames.put(-100, "LEFT-CLICK");
      specialInputNames.put(-99, "RIGHT-CLICK");
      specialInputNames.put(256, "ESCAPE");
      specialInputNames.put(257, "ENTER");
      specialInputNames.put(258, "TAB");
      specialInputNames.put(259, "BACKSPACE");
      specialInputNames.put(260, "INSERT");
      specialInputNames.put(261, "DELETE");
      specialInputNames.put(262, "RIGHT");
      specialInputNames.put(263, "LEFT");
      specialInputNames.put(264, "DOWN");
      specialInputNames.put(265, "UP");
      specialInputNames.put(266, "PAGE_UP");
      specialInputNames.put(267, "PAGE_DOWN");
      specialInputNames.put(268, "HOME");
      specialInputNames.put(269, "END");
      specialInputNames.put(280, "CAPS_LOCK");
      specialInputNames.put(281, "SCROLL_LOCK");
      specialInputNames.put(282, "NUM_LOCK");
      specialInputNames.put(283, "PRINT_SCREEN");
      specialInputNames.put(284, "PAUSE");
      specialInputNames.put(290, "F1");
      specialInputNames.put(291, "F2");
      specialInputNames.put(292, "F3");
      specialInputNames.put(293, "F4");
      specialInputNames.put(294, "F5");
      specialInputNames.put(295, "F6");
      specialInputNames.put(296, "F7");
      specialInputNames.put(297, "F8");
      specialInputNames.put(298, "F9");
      specialInputNames.put(299, "F10");
      specialInputNames.put(300, "F11");
      specialInputNames.put(301, "F12");
      specialInputNames.put(302, "F13");
      specialInputNames.put(303, "F14");
      specialInputNames.put(304, "F15");
      specialInputNames.put(305, "F16");
      specialInputNames.put(306, "F17");
      specialInputNames.put(307, "F18");
      specialInputNames.put(308, "F19");
      specialInputNames.put(309, "F20");
      specialInputNames.put(310, "F21");
      specialInputNames.put(311, "F22");
      specialInputNames.put(312, "F23");
      specialInputNames.put(313, "F24");
      specialInputNames.put(314, "F25");
      specialInputNames.put(320, "KP_0");
      specialInputNames.put(321, "KP_1");
      specialInputNames.put(322, "KP_2");
      specialInputNames.put(323, "KP_3");
      specialInputNames.put(324, "KP_4");
      specialInputNames.put(325, "KP_5");
      specialInputNames.put(326, "KP_6");
      specialInputNames.put(327, "KP_7");
      specialInputNames.put(328, "KP_8");
      specialInputNames.put(329, "KP_9");
      specialInputNames.put(330, "KP_DECIMAL");
      specialInputNames.put(331, "KP_DIVIDE");
      specialInputNames.put(332, "KP_MULTIPLY");
      specialInputNames.put(333, "KP_SUBTRACT");
      specialInputNames.put(334, "KP_ADD");
      specialInputNames.put(335, "KP_ENTER");
      specialInputNames.put(336, "KP_EQUAL");
      specialInputNames.put(340, "LSHIFT");
      specialInputNames.put(341, "LCTRL");
      specialInputNames.put(342, "LALT");
      specialInputNames.put(343, "LEFT_SUPER");
      specialInputNames.put(344, "RSHIFT");
      specialInputNames.put(345, "RCTRL");
      specialInputNames.put(346, "RALT");
      specialInputNames.put(347, "RIGHT_SUPER");
      specialInputNames.put(348, "MENU");
   }

   protected static class RepeatEventTracker {
      public final Object[] callers;
      public long lastPressTime;
      public int nextPressCooldown;
      public int repeatPresses;

      public RepeatEventTracker(Object[] callers, long lastPressTime) {
         this.callers = callers;
         this.lastPressTime = lastPressTime;
         this.nextPressCooldown = 250;
      }
   }
}
