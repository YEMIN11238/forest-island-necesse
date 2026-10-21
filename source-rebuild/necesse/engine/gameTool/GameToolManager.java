package necesse.engine.gameTool;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.input.InputEvent;
import necesse.engine.input.controller.ControllerEvent;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.platforms.Platform;
import necesse.engine.util.GameLinkedList;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.Renderer;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;

public class GameToolManager {
   private static final GameLinkedList<GameToolManager.CalledGameTool> gameTools = new GameLinkedList<>();
   private static boolean firstGameToolControllerIsAimBefore;
   private static WindowManager windowManager;

   public static void initialize() {
      windowManager = Platform.getWindowManager();
   }

   public static void preGameTick(TickManager tickManager) {
      if (!gameTools.isEmpty()) {
         GameLinkedList<GameToolManager.CalledGameTool>.Element gameToolElement = gameTools.getFirstElement();
         GameTool gameTool = ((GameToolManager.CalledGameTool)gameToolElement.object).tool;
         if (tickManager.isGameTick()) {
            gameTool.tick();
         }

         if (!gameToolElement.isRemoved()) {
            GameTooltips tooltips = gameTool.getTooltips();
            if (tooltips != null) {
               GameTooltipManager.addTooltip(tooltips, TooltipLocation.PLAYER);
            }
         } else {
            if (!gameTools.isEmpty()) {
               if (gameTools.getFirst().hasBeenPushedBack) {
                  gameTools.getFirst().tool.onRenewed();
                  gameTools.getFirst().hasBeenPushedBack = false;
                  ControllerInput.setAimIsCursor(gameTools.getFirst().controllerIsAimBefore);
               }
            } else {
               ControllerInput.setAimIsCursor(firstGameToolControllerIsAimBefore);
            }

            gameTool = null;
         }

         if (gameTool != null && Input.lastInputIsController) {
            if (gameTool.forceControllerCursor()) {
               if (!ControllerInput.isCursorVisible()) {
                  if (gameTool.canCancel()) {
                     gameTool.isCancelled();
                     gameToolElement.remove();
                     if (!gameTools.isEmpty()) {
                        if (gameTools.getFirst().hasBeenPushedBack) {
                           gameTools.getFirst().tool.onRenewed();
                           gameTools.getFirst().hasBeenPushedBack = false;
                           ControllerInput.setAimIsCursor(gameTools.getFirst().controllerIsAimBefore);
                        }
                     } else {
                        ControllerInput.setAimIsCursor(firstGameToolControllerIsAimBefore);
                     }

                     gameTool = null;
                  } else {
                     ControllerInput.setAimIsCursor(true);
                  }
               }
            } else if (!gameToolElement.hasNext()) {
               firstGameToolControllerIsAimBefore = ControllerInput.isCursorVisible();
            }
         }

         if (gameTool != null) {
            for (InputEvent event : WindowManager.getWindow().getInput().getEvents()) {
               if (event.state && event.getID() == 256 && gameTool.canCancel()) {
                  gameTool.isCancelled();
                  gameToolElement.remove();
                  if (!gameTools.isEmpty()) {
                     if (gameTools.getFirst().hasBeenPushedBack) {
                        gameTools.getFirst().tool.onRenewed();
                        gameTools.getFirst().hasBeenPushedBack = false;
                        ControllerInput.setAimIsCursor(gameTools.getFirst().controllerIsAimBefore);
                     }
                  } else {
                     ControllerInput.setAimIsCursor(firstGameToolControllerIsAimBefore);
                  }

                  event.use();
                  gameTool = null;
                  break;
               }

               if (gameTool.inputEvent(event)) {
                  event.use();
               }

               if (gameToolElement.isRemoved()) {
                  if (!gameTools.isEmpty()) {
                     if (gameTools.getFirst().hasBeenPushedBack) {
                        gameTools.getFirst().tool.onRenewed();
                        gameTools.getFirst().hasBeenPushedBack = false;
                        ControllerInput.setAimIsCursor(gameTools.getFirst().controllerIsAimBefore);
                     }
                  } else {
                     ControllerInput.setAimIsCursor(firstGameToolControllerIsAimBefore);
                  }

                  gameTool = null;
                  break;
               }
            }
         }

         if (gameTool != null) {
            for (ControllerEvent event : ControllerInput.getEvents()) {
               if (event.buttonState
                  && (event.getState() == ControllerInput.MENU_BACK || event.getState() == ControllerInput.MAIN_MENU)
                  && gameTool.canCancel()) {
                  gameTool.isCancelled();
                  gameToolElement.remove();
                  if (!gameTools.isEmpty()) {
                     if (gameTools.getFirst().hasBeenPushedBack) {
                        gameTools.getFirst().tool.onRenewed();
                        gameTools.getFirst().hasBeenPushedBack = false;
                        ControllerInput.setAimIsCursor(gameTools.getFirst().controllerIsAimBefore);
                     }
                  } else {
                     ControllerInput.setAimIsCursor(firstGameToolControllerIsAimBefore);
                  }

                  event.use();
                  GameTool var6 = null;
                  break;
               }

               if (gameTool.controllerEvent(event)) {
                  event.use();
               }

               if (gameToolElement.isRemoved()) {
                  if (!gameTools.isEmpty()) {
                     if (gameTools.getFirst().hasBeenPushedBack) {
                        gameTools.getFirst().tool.onRenewed();
                        gameTools.getFirst().hasBeenPushedBack = false;
                        ControllerInput.setAimIsCursor(gameTools.getFirst().controllerIsAimBefore);
                     }
                  } else {
                     ControllerInput.setAimIsCursor(firstGameToolControllerIsAimBefore);
                  }

                  GameTool var5 = null;
                  break;
               }
            }
         }
      }
   }

   public static void lateTick(TickManager tickManager) {
      if (!gameTools.isEmpty()) {
         GameWindow.CURSOR cursor = gameTools.getFirst().tool.getCursor();
         if (cursor != null) {
            Renderer.setCursor(cursor);
         }
      }
   }

   public static void setGameTool(GameTool tool, Object caller) {
      if (gameTools.stream().noneMatch(e -> e.tool == tool)) {
         if (gameTools.isEmpty()) {
            firstGameToolControllerIsAimBefore = ControllerInput.isCursorVisible();
         } else {
            gameTools.getFirst().tool.onPaused();
            gameTools.getFirst().controllerIsAimBefore = ControllerInput.isCursorVisible();
            gameTools.getFirst().hasBeenPushedBack = true;
         }

         gameTools.addFirst(new GameToolManager.CalledGameTool(caller, tool));
         tool.init();
         if (tool.startControllerCursor()) {
            ControllerInput.setAimIsCursor(true);
         }
      }
   }

   public static void setGameTool(GameTool tool) {
      setGameTool(tool, tool);
   }

   public static GameTool getFirstGameTool(Object caller) {
      for (GameToolManager.CalledGameTool gameTool : gameTools) {
         if (gameTool.caller == caller) {
            return gameTool.tool;
         }
      }

      return null;
   }

   public static GameTool getFirstGameTool() {
      return gameTools.isEmpty() ? null : gameTools.getFirst().tool;
   }

   public static boolean clearGameTools(Object caller) {
      GameToolManager.CalledGameTool prevTool = null;
      if (!gameTools.isEmpty()) {
         prevTool = gameTools.getFirst();
      }

      boolean out = false;

      for (GameLinkedList<GameToolManager.CalledGameTool>.Element e : gameTools.elements()) {
         if (((GameToolManager.CalledGameTool)e.object).caller == caller) {
            ((GameToolManager.CalledGameTool)e.object).tool.isCleared();
            e.remove();
            out = true;
         }
      }

      if (!gameTools.isEmpty() && gameTools.getFirst() != prevTool) {
         if (gameTools.getFirst().hasBeenPushedBack) {
            gameTools.getFirst().tool.onRenewed();
            gameTools.getFirst().hasBeenPushedBack = false;
            ControllerInput.setAimIsCursor(gameTools.getFirst().controllerIsAimBefore);
         }
      } else if (prevTool != null && gameTools.isEmpty()) {
         ControllerInput.setAimIsCursor(firstGameToolControllerIsAimBefore);
      }

      return out;
   }

   public static boolean clearGameTool(GameTool tool) {
      GameToolManager.CalledGameTool prevTool = null;
      if (!gameTools.isEmpty()) {
         prevTool = gameTools.getFirst();
      }

      boolean out = false;

      for (GameLinkedList<GameToolManager.CalledGameTool>.Element e : gameTools.elements()) {
         if (((GameToolManager.CalledGameTool)e.object).tool == tool) {
            ((GameToolManager.CalledGameTool)e.object).tool.isCleared();
            e.remove();
            out = true;
         }
      }

      if (!gameTools.isEmpty() && gameTools.getFirst() != prevTool) {
         if (gameTools.getFirst().hasBeenPushedBack) {
            gameTools.getFirst().tool.onRenewed();
            gameTools.getFirst().hasBeenPushedBack = false;
            ControllerInput.setAimIsCursor(gameTools.getFirst().controllerIsAimBefore);
         }
      } else if (prevTool != null && gameTools.isEmpty()) {
         ControllerInput.setAimIsCursor(firstGameToolControllerIsAimBefore);
      }

      return out;
   }

   public static boolean doesToolForceMenuLayer() {
      return !gameTools.isEmpty() && gameTools.getFirst().tool.shouldForceControllerMenuLayer();
   }

   public static boolean doesToolShowWires() {
      return !gameTools.isEmpty() && gameTools.getFirst().tool.shouldShowWires();
   }

   private static class CalledGameTool {
      public final Object caller;
      public final GameTool tool;
      public boolean controllerIsAimBefore;
      public boolean hasBeenPushedBack;

      public CalledGameTool(Object caller, GameTool tool) {
         this.caller = caller;
         this.tool = tool;
      }
   }
}
