package necesse.engine.input.controller;

import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Control;
import necesse.engine.input.Input;
import necesse.engine.input.InputManager;
import necesse.engine.input.InputPosition;
import necesse.engine.input.InputSource;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameMath;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.gameTexture.GameTexture;

public class ControllerInput {
   public static final ControllerRefreshFocusState REFRESH_FOCUS;
   public static final ControllerState REPEAT_EVENT;
   public static final ControllerState CONTROLLER_CONNECTED_EVENT;
   public static final ControllerState CONTROLLER_DISCONNECTED_EVENT;
   public static final ControllerActionSet GAME_CONTROLS;
   public static final ControllerActionSet DESKTOP_CONTROLS;
   public static final ControllerAnalogState MOVE;
   public static final ControllerAnalogState AIM;
   public static final ControllerAnalogState CURSOR;
   public static final ControllerAnalogState DESKTOP_CURSOR;
   public static final ControllerButtonState ATTACK;
   public static final ControllerButtonState INTERACT;
   public static final ControllerButtonState TOGGLE_AIM;
   public static final ControllerButtonState INVENTORY;
   public static final ControllerButtonState HEALTH_POTION;
   public static final ControllerButtonState MANA_POTION;
   public static final ControllerButtonState BUFF_POTIONS;
   public static final ControllerButtonState EAT_FOOD;
   public static final ControllerButtonState USE_MOUNT;
   public static final ControllerButtonState SET_ABILITY;
   public static final ControllerButtonState TRINKET_ABILITY;
   public static final ControllerButtonState PLACE_TORCH;
   public static final ControllerButtonState EXPRESSION_WHEEL;
   public static final ControllerButtonState OPEN_ADVENTURE_PARTY;
   public static final ControllerButtonState OPEN_ADVENTURE_JOURNAL;
   public static final ControllerButtonState OPEN_SETTLEMENT;
   public static final ControllerButtonState MAIN_MENU;
   public static final ControllerButtonState SMART_MINING;
   public static final ControllerButtonState QUICK_STACK_NEARBY;
   public static final ControllerButtonState SHOW_MAP;
   public static final ControllerButtonState SCOREBOARD;
   public static final ControllerButtonState TOGGLE_UI;
   public static final ControllerButtonState ZOOM_IN;
   public static final ControllerButtonState ZOOM_OUT;
   public static final ControllerButtonState NEXT_HOTBAR;
   public static final ControllerButtonState PREV_HOTBAR;
   public static final ControllerActionSetLayer MENU_SET_LAYER;
   public static final ControllerButtonState MENU_UP;
   public static final ControllerButtonState MENU_RIGHT;
   public static final ControllerButtonState MENU_DOWN;
   public static final ControllerButtonState MENU_LEFT;
   public static final ControllerButtonState MENU_SELECT;
   public static final ControllerButtonState MENU_BACK;
   public static final ControllerButtonState MENU_NEXT;
   public static final ControllerButtonState MENU_PREV;
   public static final ControllerButtonState MENU_INTERACT_ITEM;
   public static final ControllerButtonState MENU_ITEM_ACTIONS_MENU;
   public static final ControllerButtonState MENU_QUICK_TRANSFER;
   public static final ControllerButtonState MENU_QUICK_TRASH;
   public static final ControllerButtonState MENU_DROP_ITEM;
   public static final ControllerButtonState MENU_LOCK_ITEM;
   public static final ControllerButtonState MENU_MOVE_ONE_ITEM;
   public static final ControllerButtonState MENU_GET_ONE_ITEM;
   public static final ControllerButtonState MENU_QUICK_STACK;
   public static final ControllerButtonState MENU_LOOT_ALL;
   public static final ControllerButtonState MENU_SORT_INVENTORY;
   public static final ControllerButtonState PIPETTE;
   public static final ControllerButtonState ERASER;
   private static ControllerActionSet activeActionSet;
   private static final ArrayList<ControllerState> systemStates = new ArrayList<>();
   private static final ArrayList<ControllerActionSetLayer> allActionSetLayers = new ArrayList<>();
   private static final ArrayList<ControllerActionSetLayer> activeActionSetLayers = new ArrayList<>();
   private static final ArrayList<ControllerActionSetLayer> actionSetLayersToDisable = new ArrayList<>();
   private static final ArrayList<ControllerActionSetLayer> actionSetLayersToEnable = new ArrayList<>();
   private static final Object newEventsLock = new Object();
   private static final HashSet<ControllerHandle> controllerHandles = new HashSet<>(16);
   protected static HashMap<Integer, ControllerInput.RepeatEventTracker> repeatEvents = new HashMap<>();
   private static ArrayList<ControllerEvent> newEvents = new ArrayList<>();
   private static List<ControllerEvent> events = new ArrayList<>();
   private static boolean lastUsedCursor;
   private static boolean isAimCursor;
   private static float aimX;
   private static float aimY;
   private static float cursorJoystickX;
   private static float cursorJoystickY;
   private static boolean useMoveAsMenuNavigation;
   private static boolean isNavigatingWithMove;
   private static InputManager inputManager;
   private static ControllerHandle latestControllerHandle;
   private static boolean shouldUpdateStateBindOverrides;

   public static Iterable<ControllerActionSetLayer> getActionSetLayers() {
      return allActionSetLayers;
   }

   public static void init(InputManager inputManager) {
      ControllerInput.inputManager = inputManager;

      for (ControllerActionSetLayer actionSetLayer : allActionSetLayers) {
         actionSetLayer.init();
      }

      activeActionSet.init();

      for (ControllerState state : systemStates) {
         state.init();
      }
   }

   public static void setActiveActionSet(ControllerActionSet actionSet) {
      activeActionSet = actionSet;
      activeActionSet.init();

      for (InputSource inputSource : InputManager.getInputSources()) {
         inputSource.onActionSetsChanged();
      }

      updateControllerOverrides();
   }

   public static ControllerActionSet getActiveActionSet() {
      return activeActionSet;
   }

   public static void tick(TickManager tickManager) {
      SameControlBindDetector.clearUsedStatesAfterDetectionTimer();
      GameWindow window = WindowManager.getWindow();
      synchronized (newEventsLock) {
         boolean layersChanged = false;

         for (ControllerActionSetLayer actionSetLayer : actionSetLayersToEnable) {
            if (!isLayerActive(actionSetLayer)) {
               activeActionSetLayers.add(actionSetLayer);
               layersChanged = true;
            }
         }

         actionSetLayersToEnable.clear();

         for (ControllerActionSetLayer actionSetLayer : actionSetLayersToDisable) {
            if (isLayerActive(actionSetLayer)) {
               activeActionSetLayers.remove(actionSetLayer);
               actionSetLayer.resetStates(newEvents, tickManager);
               layersChanged = true;
            }
         }

         actionSetLayersToDisable.clear();
         if (layersChanged || shouldUpdateStateBindOverrides) {
            ArrayList<ControllerBind> usedBinds = new ArrayList<>();

            for (int i = activeActionSetLayers.size() - 1; i >= 0; i--) {
               activeActionSetLayers.get(i).updateOverwrittenStates(usedBinds, newEvents, tickManager);
            }

            activeActionSet.updateOverwrittenStates(usedBinds, newEvents, tickManager);

            for (InputSource inputSource : InputManager.getInputSources()) {
               inputSource.onActionSetsChanged();
            }

            shouldUpdateStateBindOverrides = false;
         }

         ControllerHandle handleIfChanged = activeActionSet.updateStates(newEvents, tickManager);
         if (handleIfChanged != null) {
            if (latestControllerHandle != handleIfChanged) {
               updateControllerOverrides();
            }

            latestControllerHandle = handleIfChanged;
            Input.lastControllerInputTime = System.currentTimeMillis();
         }

         for (ControllerActionSetLayer actionSetLayer : activeActionSetLayers) {
            handleIfChanged = actionSetLayer.updateStates(newEvents, tickManager);
            if (handleIfChanged != null) {
               if (latestControllerHandle != handleIfChanged) {
                  updateControllerOverrides();
               }

               latestControllerHandle = handleIfChanged;
               Input.lastControllerInputTime = System.currentTimeMillis();
            }
         }

         for (ControllerState state : systemStates) {
            if (state instanceof ControllerRefreshFocusState) {
               ((ControllerRefreshFocusState)state).updateState(controllerHandles, newEvents);
            }
         }
      }

      for (int id : repeatEvents.keySet()) {
         ControllerInput.RepeatEventTracker tracker = repeatEvents.get(id);

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
            synchronized (newEventsLock) {
               newEvents.add(
                  ControllerEvent.repeatEvent(
                     tracker.controllerHandle, tracker.callers, tracker.repeatPresses, tracker.nextPressCooldown, ControllerState.getStateFromId(id)
                  )
               );
            }
         }
      }

      if (useMoveAsMenuNavigation) {
         float moveX = MOVE.getX();
         float moveY = MOVE.getY();
         double dist = new Float(moveX, moveY).distance(0.0, 0.0);
         if (dist < 0.5) {
            if (isNavigatingWithMove) {
               repeatEvents.remove(MENU_UP.getID());
               repeatEvents.remove(MENU_RIGHT.getID());
               repeatEvents.remove(MENU_DOWN.getID());
               repeatEvents.remove(MENU_LEFT.getID());
            }

            isNavigatingWithMove = false;
         } else {
            isNavigatingWithMove = true;
            Float norm = GameMath.normalize(moveX, moveY);
            float angle = GameMath.getAngle(new Float(norm.x, norm.y));
            int dir = (int)(GameMath.fixAngle(angle + 90.0F + 45.0F) * 4.0F / 360.0F);
            if (dir == 0) {
               if (!repeatEvents.containsKey(MENU_UP.getID())) {
                  ControllerEvent event = ControllerEvent.buttonEvent(latestControllerHandle, MENU_UP, true);
                  synchronized (newEventsLock) {
                     newEvents.add(event);
                  }

                  startRepeatEvents(event, MENU_UP);
               }
            } else {
               repeatEvents.remove(MENU_UP.getID());
            }

            if (dir == 1) {
               if (!repeatEvents.containsKey(MENU_RIGHT.getID())) {
                  ControllerEvent event = ControllerEvent.buttonEvent(latestControllerHandle, MENU_RIGHT, true);
                  synchronized (newEventsLock) {
                     newEvents.add(event);
                  }

                  startRepeatEvents(event, MENU_RIGHT);
               }
            } else {
               repeatEvents.remove(MENU_RIGHT.getID());
            }

            if (dir == 2) {
               if (!repeatEvents.containsKey(MENU_DOWN.getID())) {
                  ControllerEvent event = ControllerEvent.buttonEvent(latestControllerHandle, MENU_DOWN, true);
                  synchronized (newEventsLock) {
                     newEvents.add(event);
                  }

                  startRepeatEvents(event, MENU_DOWN);
               }
            } else {
               repeatEvents.remove(MENU_DOWN.getID());
            }

            if (dir == 3) {
               if (!repeatEvents.containsKey(MENU_LEFT.getID())) {
                  ControllerEvent event = ControllerEvent.buttonEvent(latestControllerHandle, MENU_LEFT, true);
                  synchronized (newEventsLock) {
                     newEvents.add(event);
                  }

                  startRepeatEvents(event, MENU_LEFT);
               }
            } else {
               repeatEvents.remove(MENU_LEFT.getID());
            }
         }
      } else {
         if (isNavigatingWithMove) {
            repeatEvents.remove(MENU_UP.getID());
            repeatEvents.remove(MENU_RIGHT.getID());
            repeatEvents.remove(MENU_DOWN.getID());
            repeatEvents.remove(MENU_LEFT.getID());
         }

         isNavigatingWithMove = false;
      }

      if ((isNavigatingWithMove || MENU_UP.isDown() || MENU_RIGHT.isDown() || MENU_DOWN.isDown() || MENU_LEFT.isDown()) && lastUsedCursor) {
         lastUsedCursor = false;
         window.submitNextMoveEvent();
      }

      synchronized (newEventsLock) {
         newEvents.removeIf(Objects::isNull);
         events = Collections.unmodifiableList(newEvents);
         newEvents = new ArrayList<>();
      }

      if (CURSOR.getX() != 0.0F || CURSOR.getY() != 0.0F) {
         if (!isCursorVisible()) {
            window.setCursorMode(212993);
         }

         lastUsedCursor = true;
         window.submitNextMoveEvent();
         InputPosition mousePos = window.mousePos();
         int mouseX = GameMath.limit((int)(mousePos.windowX + CURSOR.getX()), 1, window.getWidth() - 1);
         int mouseY = GameMath.limit((int)(mousePos.windowY - CURSOR.getY()), 1, window.getHeight() - 1);
         window.getInput().setCursorPosition(mouseX, mouseY, tickManager);
      }

      if (AIM.getX() == 0.0F && AIM.getY() == 0.0F) {
         aimX = 0.0F;
         aimY = 0.0F;
      } else {
         if (!isAimCursor) {
            aimX = AIM.getX();
            aimY = AIM.getY();
         } else {
            cursorJoystickX = cursorJoystickX + AIM.getX() * tickManager.getDelta() * Settings.cursorJoystickSensitivity;
            cursorJoystickY = cursorJoystickY + AIM.getY() * tickManager.getDelta() * Settings.cursorJoystickSensitivity;
            if (Math.abs(cursorJoystickX) > 1.0F || Math.abs(cursorJoystickY) > 1.0F) {
               int dX = (int)cursorJoystickX;
               int dY = (int)cursorJoystickY;
               cursorJoystickX -= dX;
               cursorJoystickY -= dY;
               InputPosition mousePos = window.mousePos();
               int mouseX = GameMath.limit(mousePos.windowX + dX, 1, window.getWidth() - 1);
               int mouseY = GameMath.limit(mousePos.windowY + dY, 1, window.getHeight() - 1);
               window.getInput().setCursorPosition(mouseX, mouseY, tickManager);
            }

            aimX = 0.0F;
            aimY = 0.0F;
         }

         if (lastUsedCursor) {
            lastUsedCursor = false;
            window.submitNextMoveEvent();
         }
      }

      if (TOGGLE_AIM.isJustPressed()) {
         setAimIsCursor(!isAimCursor);
      }

      if (Input.lastInputIsController && !isCursorVisible()) {
         window.setCursorMode(212994);
      } else {
         window.setCursorMode(212993);
      }
   }

   public static boolean isLayerActive(ControllerActionSetLayer layer) {
      return activeActionSetLayers.contains(layer);
   }

   public static void startRepeatEvents(ControllerEvent event, Object... callers) {
      if (event.getState() != REPEAT_EVENT && event.getUsedState() != REPEAT_EVENT) {
         if (!event.buttonState) {
            throw new IllegalArgumentException("Cannot start from release events");
         }

         if (!event.isButton) {
            throw new IllegalArgumentException("Event must be button");
         }

         ControllerState state;
         if (event.isUsed()) {
            state = event.getUsedState();
         } else {
            state = event.getState();
         }

         if (!repeatEvents.containsKey(state.getID())) {
            repeatEvents.put(state.getID(), new ControllerInput.RepeatEventTracker(event.controllerHandle, callers, System.currentTimeMillis()));
         }
      }
   }

   public static boolean isCursorVisible() {
      return isAimCursor || lastUsedCursor;
   }

   public static void setAimIsCursor(boolean value) {
      boolean before = isAimCursor;
      isAimCursor = value;
      if (!before && isAimCursor && Input.lastInputIsController) {
         GameWindow window = WindowManager.getWindow();
         InputPosition mousePos = window.mousePos();
         if (mousePos.windowX <= 0 || mousePos.windowY <= 0 || mousePos.windowX >= window.getWidth() || mousePos.windowY >= window.getHeight()) {
            window.getInput().setCursorPosition(window.getWidth() / 2, window.getHeight() / 2, GlobalData.getCurrentGameLoop());
         }

         window.setCursorMode(212993);
      }
   }

   public static void updateControllerOverrides() {
      shouldUpdateStateBindOverrides = true;
   }

   public static void registerController(ControllerHandle controllerHandle) {
      controllerHandles.add(controllerHandle);
      synchronized (newEventsLock) {
         newEvents.add(ControllerEvent.customEvent(null, CONTROLLER_CONNECTED_EVENT));
      }

      forceSetLatestControllerHandle(controllerHandle);
      updateControllerOverrides();
      System.out.println("Detected " + controllerHandles.size() + " controllers");
   }

   public static void deregisterController(ControllerHandle controllerHandle) {
      if (controllerHandles.remove(controllerHandle)) {
         synchronized (newEventsLock) {
            newEvents.add(ControllerEvent.customEvent(null, CONTROLLER_DISCONNECTED_EVENT));
         }

         updateControllerOverrides();
         System.out.println("Detected " + controllerHandles.size() + " controllers");
      }
   }

   public static void enableLayer(ControllerActionSetLayer layer) {
      actionSetLayersToEnable.add(layer);
   }

   public static void disableLayer(ControllerActionSetLayer layer) {
      actionSetLayersToDisable.add(layer);
   }

   public static void submitNextRefreshFocusEvent() {
      REFRESH_FOCUS.submitNextEvent();
   }

   public static void submitControllerEvent(ControllerEvent event) {
      synchronized (newEventsLock) {
         newEvents.add(event);
      }
   }

   public static boolean showControllerPanel() {
      return inputManager.showControllerPanel(latestControllerHandle);
   }

   public static void vibrate(float leftIntensity, float rightIntensity) {
      if (latestControllerHandle != null) {
         inputManager.triggerVibration(latestControllerHandle, leftIntensity, rightIntensity);
      }
   }

   public static void saveControllerBinds(SaveData saveData, LoadedMod loadedMod) {
      activeActionSet.saveStateBinds(saveData, loadedMod);

      for (ControllerActionSetLayer layer : allActionSetLayers) {
         layer.saveStateBinds(saveData, loadedMod);
      }
   }

   public static void loadControllerBinds(LoadData loadData, LoadedMod loadedMod) {
      activeActionSet.loadStateBinds(loadData, loadedMod);

      for (ControllerActionSetLayer layer : allActionSetLayers) {
         layer.loadStateBinds(loadData, loadedMod);
      }
   }

   public static boolean hasChanged(ControllerState state) {
      return events.stream().anyMatch(e -> e.getState() == state);
   }

   public static boolean isPressed(ControllerState state) {
      return events.stream().anyMatch(e -> e.getState() == state && e.buttonState);
   }

   public static boolean isReleased(ControllerState state) {
      return events.stream().anyMatch(e -> e.getState() == state && !e.buttonState);
   }

   public static List<ControllerActionSetLayer> getActiveActionSetLayers() {
      return Collections.unmodifiableList(activeActionSetLayers);
   }

   public static GameTexture getStateGlyph(ControllerInputState state) {
      return latestControllerHandle != null ? latestControllerHandle.inputSource.getControllerStateGlyph(state, latestControllerHandle) : null;
   }

   public static List<ControllerEvent> getEvents() {
      return events;
   }

   public static ControllerEvent getEvent(ControllerState state) {
      return events.stream().filter(e -> e.getState() == state).findFirst().orElse(null);
   }

   public static ControllerHandle getLatestControllerHandle() {
      if (latestControllerHandle != null) {
         return latestControllerHandle;
      } else {
         return !controllerHandles.isEmpty() ? controllerHandles.stream().findFirst().get() : null;
      }
   }

   public static void forceSetLatestControllerHandle(ControllerHandle controllerHandle) {
      latestControllerHandle = controllerHandle;
      Input.lastControllerInputTime = System.currentTimeMillis();
      updateControllerOverrides();
   }

   public static float getAimX() {
      return aimX;
   }

   public static float getAimY() {
      return aimY;
   }

   public static void setMoveAsMenuNavigation(boolean value) {
      useMoveAsMenuNavigation = value;
   }

   static {
      activeActionSet = GAME_CONTROLS = new ControllerActionSet("GameControls", new LocalMessage("controller", "layer_gameplay"));
      DESKTOP_CONTROLS = new ControllerActionSet("DesktopControls", new LocalMessage("controller", "layer_desktop"));
      allActionSetLayers.add(MENU_SET_LAYER = new ControllerActionSetLayer("MenuControls", new LocalMessage("controller", "layer_menu")));
      systemStates.add(REFRESH_FOCUS = new ControllerRefreshFocusState());
      systemStates.add(REPEAT_EVENT = new ControllerEmptyState());
      systemStates.add(CONTROLLER_CONNECTED_EVENT = new ControllerEmptyState());
      systemStates.add(CONTROLLER_DISCONNECTED_EVENT = new ControllerEmptyState());
      GAME_CONTROLS.addState(MOVE = new ControllerAnalogState("move", false));
      GAME_CONTROLS.addState(AIM = new ControllerAnalogState("aim", false));
      GAME_CONTROLS.addState(CURSOR = new ControllerAnalogState("cursor", true));
      GAME_CONTROLS.addState(ATTACK = new ControllerButtonState("attack", () -> Control.MOUSE1));
      GAME_CONTROLS.addState(INTERACT = new ControllerButtonState("interact", () -> Control.MOUSE2));
      GAME_CONTROLS.addState(TOGGLE_AIM = new ControllerButtonState("toggleaim", null));
      GAME_CONTROLS.addState(INVENTORY = new ControllerButtonState("inventory", () -> Control.INVENTORY));
      GAME_CONTROLS.addState(HEALTH_POTION = new ControllerButtonState("healthpotion", () -> Control.HEALTH_POT));
      GAME_CONTROLS.addState(MANA_POTION = new ControllerButtonState("manapotion", () -> Control.MANA_POT));
      GAME_CONTROLS.addState(BUFF_POTIONS = new ControllerButtonState("buffpotions", () -> Control.BUFF_POTS));
      GAME_CONTROLS.addState(EAT_FOOD = new ControllerButtonState("eatfood", () -> Control.EAT_FOOD));
      GAME_CONTROLS.addState(USE_MOUNT = new ControllerButtonState("usemount", () -> Control.USE_MOUNT));
      GAME_CONTROLS.addState(SET_ABILITY = new ControllerButtonState("setability", () -> Control.SET_ABILITY));
      GAME_CONTROLS.addState(TRINKET_ABILITY = new ControllerButtonState("trinketability", () -> Control.TRINKET_ABILITY));
      GAME_CONTROLS.addState(PLACE_TORCH = new ControllerButtonState("placetorch", () -> Control.PLACE_TORCH));
      GAME_CONTROLS.addState(EXPRESSION_WHEEL = new ControllerButtonState("expressionwheel", () -> Control.EXPRESSION_WHEEL));
      GAME_CONTROLS.addState(OPEN_ADVENTURE_PARTY = new ControllerButtonState("openadventureparty", () -> Control.OPEN_ADVENTURE_PARTY));
      GAME_CONTROLS.addState(OPEN_ADVENTURE_JOURNAL = new ControllerButtonState("openadventurejournal", () -> Control.OPEN_ADVENTURE_JOURNAL));
      GAME_CONTROLS.addState(OPEN_SETTLEMENT = new ControllerButtonState("opensettlement", () -> Control.OPEN_SETTLEMENT));
      GAME_CONTROLS.addState(MAIN_MENU = new ControllerButtonState("mainmenu", null));
      GAME_CONTROLS.addState(SMART_MINING = new ControllerButtonState("smartmining", () -> Control.SMART_MINING));
      GAME_CONTROLS.addState(QUICK_STACK_NEARBY = new ControllerButtonState("quickstacknearby", () -> Control.QUICK_STACK));
      GAME_CONTROLS.addState(SHOW_MAP = new ControllerButtonState("showmap", () -> Control.SHOW_MAP, new LocalMessage("controls", "showworldmap")));
      GAME_CONTROLS.addState(SCOREBOARD = new ControllerButtonState("scoreboard", () -> Control.SCOREBOARD));
      GAME_CONTROLS.addState(TOGGLE_UI = new ControllerButtonState("toggleui", () -> Control.HIDE_UI));
      GAME_CONTROLS.addState(ZOOM_IN = new ControllerButtonState("zoomin", () -> Control.ZOOM_IN));
      GAME_CONTROLS.addState(ZOOM_OUT = new ControllerButtonState("zoomout", () -> Control.ZOOM_OUT));
      GAME_CONTROLS.addState(NEXT_HOTBAR = new ControllerButtonState("nexthotbar", () -> Control.NEXT_HOTBAR));
      GAME_CONTROLS.addState(PREV_HOTBAR = new ControllerButtonState("prevhotbar", () -> Control.PREV_HOTBAR));
      GAME_CONTROLS.addState(PIPETTE = new ControllerButtonState("pipette", () -> Control.PIPETTE));
      GAME_CONTROLS.addState(ERASER = new ControllerButtonState("eraser", () -> Control.ERASER));
      MENU_SET_LAYER.addState(MENU_UP = new ControllerButtonState("menuup", null));
      MENU_SET_LAYER.addState(MENU_RIGHT = new ControllerButtonState("menuright", null));
      MENU_SET_LAYER.addState(MENU_DOWN = new ControllerButtonState("menudown", null));
      MENU_SET_LAYER.addState(MENU_LEFT = new ControllerButtonState("menuleft", null));
      MENU_SET_LAYER.addState(MENU_SELECT = new ControllerButtonState("menuselect", null));
      MENU_SET_LAYER.addState(MENU_BACK = new ControllerButtonState("menuback", null));
      MENU_SET_LAYER.addState(MENU_NEXT = new ControllerButtonState("menunext", null));
      MENU_SET_LAYER.addState(MENU_PREV = new ControllerButtonState("menuprevious", null));
      MENU_SET_LAYER.addState(MENU_INTERACT_ITEM = new ControllerButtonState("menuinteractitem", null));
      MENU_SET_LAYER.addState(MENU_ITEM_ACTIONS_MENU = new ControllerButtonState("menuitemactionsmenu", null));
      MENU_SET_LAYER.addState(MENU_QUICK_TRANSFER = new ControllerButtonState("menuquicktransfer", null));
      MENU_SET_LAYER.addState(MENU_QUICK_TRASH = new ControllerButtonState("menuquicktrash", null));
      MENU_SET_LAYER.addState(MENU_DROP_ITEM = new ControllerButtonState("menudropitem", null));
      MENU_SET_LAYER.addState(MENU_LOCK_ITEM = new ControllerButtonState("menulockitem", null));
      MENU_SET_LAYER.addState(MENU_MOVE_ONE_ITEM = new ControllerButtonState("menumoveoneitem", null));
      MENU_SET_LAYER.addState(MENU_GET_ONE_ITEM = new ControllerButtonState("menugetoneitem", null));
      MENU_SET_LAYER.addState(MENU_QUICK_STACK = new ControllerButtonState("menuquickstack", () -> Control.QUICK_STACK));
      MENU_SET_LAYER.addState(MENU_LOOT_ALL = new ControllerButtonState("menulootall", () -> Control.LOOT_ALL));
      MENU_SET_LAYER.addState(MENU_SORT_INVENTORY = new ControllerButtonState("menusortinventory", () -> Control.SORT_INVENTORY));
      DESKTOP_CONTROLS.addState(DESKTOP_CURSOR = new ControllerAnalogState("desktopcursor", true));
   }

   protected static class RepeatEventTracker {
      public final ControllerHandle controllerHandle;
      public final Object[] callers;
      public long lastPressTime;
      public int nextPressCooldown;
      public int repeatPresses;

      public RepeatEventTracker(ControllerHandle controllerHandle, Object[] callers, long lastPressTime) {
         this.controllerHandle = controllerHandle;
         this.callers = callers;
         this.lastPressTime = lastPressTime;
         this.nextPressCooldown = 250;
      }
   }
}
