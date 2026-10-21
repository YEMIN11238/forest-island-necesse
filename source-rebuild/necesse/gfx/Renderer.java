package necesse.gfx;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D.Float;
import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import necesse.engine.GameInfo;
import necesse.engine.GameLoadingScreen;
import necesse.engine.GameLog;
import necesse.engine.GlobalData;
import necesse.engine.MouseDraggingElement;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.PerformanceTimerAverage;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.input.InputPosition;
import necesse.engine.input.controller.ControllerGlyphTip;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.ModLoader;
import necesse.engine.network.client.Client;
import necesse.engine.platforms.PlatformManager;
import necesse.engine.screenHudManager.ScreenHudManager;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.state.State;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameUtils;
import necesse.engine.util.HashMapSet;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.fairType.FairCharacterGlyph;
import necesse.gfx.fairType.FairType;
import necesse.gfx.forms.components.chat.ChatMessage;
import necesse.gfx.forms.components.chat.ChatMessageList;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameFrameBuffer;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.shader.GameShader;
import necesse.level.maps.Level;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.Platform;

public class Renderer {
   private static LinkedList<GameShader> shaderHistory;
   private static final AtomicBoolean screenshotCooldown = new AtomicBoolean(false);
   private static long screenshotTime;
   private static final long screenshotEffectTime = 350L;
   private static MouseDraggingElement mouseDraggingElement;
   public static ScreenHudManager hudManager = new ScreenHudManager();
   private static final HashMapSet<String, Integer> glErrorPrints = new HashMapSet<>();
   public static int cursorSizeOffset = 1;
   public static float[] cursorSizes = new float[]{0.75F, 1.0F, 1.25F, 1.5F, 1.75F};
   private static boolean changedCursor;
   private static boolean forceCursorChange;
   private static GameWindow.CURSOR currentCursor = null;
   private static int currentCursorSize;
   private static GameWindow.CURSOR nextCursor = null;

   public static float getCursorSizeZoom(int size) {
      return cursorSizes[GameMath.limit(size + cursorSizeOffset, 0, cursorSizes.length - 1)];
   }

   public static void initialize() {
      shaderHistory = new LinkedList<>();
   }

   public static void preGameTick(TickManager tickManager) {
      hudManager.preGameTick(tickManager);
   }

   public static void drawTick(TickManager tickManager) {
      Performance.recordConstant(
         tickManager,
         "drawTime",
         () -> {
            boolean drawUI = !Settings.reduceUIFramerate || tickManager.isGameTick();
            GameWindow window = WindowManager.getWindow();
            if (!tickManager.isBehind() || !TickManager.skipDrawIfBehind) {
               clearShaderHistory();
               State currentState = GlobalData.getCurrentState();
               boolean sceneDrawPrepared = Performance.record(
                  tickManager, "prepareDrawScene", () -> currentState.prepareDrawScene(tickManager, window.wasResizedThisFrame())
               );
               if (!sceneDrawPrepared) {
                  window.update();
               } else {
                  GL11.glClear(16640);
                  Performance.record(tickManager, "drawScene", () -> {
                     try {
                        window.startSceneDraw();
                        currentState.drawScene(tickManager, window.wasResizedThisFrame());
                     } finally {
                        window.endSceneDraw();
                     }
                  });
                  Performance.record(tickManager, "sceneOverlay", () -> {
                     try {
                        window.startSceneOverlayDraw();
                        currentState.drawSceneOverlay(tickManager);
                        GameTooltipManager.drawControllerInputTooltips();
                     } finally {
                        window.endSceneOverlayDraw();
                     }
                  });
                  Performance.record(tickManager, "postProcess", () -> window.renderScene(null));
                  Performance.record(tickManager, "hudDraw", () -> {
                     if (drawUI) {
                        try {
                           window.startHudDraw();
                           drawDebug(tickManager);
                           currentState.drawHud(tickManager);
                           boolean drewDraggingElement = false;
                           MouseDraggingElement draggingElement = mouseDraggingElement;
                           if (draggingElement != null) {
                              if (!draggingElement.isKeyDown(window.getInput())) {
                                 mouseDraggingElement = null;
                              } else {
                                 drewDraggingElement = draggingElement.draw(window.mousePos().hudX, window.mousePos().hudY);
                              }
                           }

                           hudManager.cleanUp();
                           if (!Settings.hideUI) {
                              if (!drewDraggingElement && !Settings.hideCursor) {
                                 GameTooltipManager.drawHudTooltips();
                              }

                              hudManager.draw(tickManager);
                           }

                           drawScreenshotEffect();
                        } finally {
                           window.endHudDraw();
                        }
                     }
                  });
                  window.renderHud(hudManager.getHudAlpha());
                  if (drawUI) {
                     if (!changedCursor) {
                        changeCursor(GameWindow.CURSOR.DEFAULT, window);
                     } else {
                        changeCursor(nextCursor, window);
                     }

                     changedCursor = false;
                  }

                  if (Settings.drawCursorManually && PlatformManager.getPlatform().isSteamDeck() && ControllerInput.isCursorVisible()) {
                     GameWindow.CURSOR cursor = window.getCursor();
                     if (cursor != null) {
                        GameTexture cursorTexture = cursor.getTexture();
                        if (cursorTexture != null) {
                           window.getInput().updateNextMousePos();
                           InputPosition position = window.mousePos();
                           float size = getCursorSizeZoom(Settings.cursorSize);
                           float xOffset = (cursor.xOffset - 1) * size;
                           float yOffset = (cursor.yOffset - 1) * size;
                           cursorTexture.initDraw()
                              .size((int)(size * 20.0F), (int)(size * 20.0F))
                              .draw((int)(position.windowX + xOffset), (int)(position.windowY + yOffset));
                        }
                     }
                  }

                  window.update();
               }
            }
         }
      );
   }

   private static void drawDebug(TickManager tickManager) {
      GameWindow window = WindowManager.getWindow();
      int rightDrawY = window.getHudHeight() - 2 - (Input.lastInputIsController && Settings.showControlTips ? ControllerGlyphTip.getHeight() + 4 : 0);
      if (!Settings.hideUI && (Settings.showDebugInfo || GlobalData.debugActive())) {
         FontManager.bit
            .drawString(
               10.0F,
               10.0F,
               "FPS, TPS: "
                  + tickManager.getFPS()
                  + " ("
                  + GameMath.toDecimals(tickManager.getFullDelta(), 2)
                  + "), "
                  + tickManager.getTPS()
                  + " ("
                  + tickManager.getTick()
                  + ")",
               new FontOptions(16)
            );
         PerformanceTimerAverage previousAverage = tickManager.getPreviousAverage();
         int leftDrawY = window.getHudHeight() - 10;
         if (previousAverage != null) {
            Collection<PerformanceTimerAverage> values = previousAverage.getChildren().values();
            TableContentDraw tableDraw = new TableContentDraw();
            FontOptions timerFontOptions = new FontOptions(16);

            for (PerformanceTimerAverage t : values) {
               tableDraw.newRow()
                  .addTextColumn(t.name, timerFontOptions, 10, 0)
                  .addTextColumn(GameMath.toDecimals(t.getAverageTimePercent(), 3) + "%", timerFontOptions, 20, 0)
                  .addTextColumn(GameUtils.getTimeStringNano(t.getAverageTime()), timerFontOptions, 20, 0);
            }

            tableDraw.newRow()
               .addTextColumn("Loop", timerFontOptions)
               .addTextColumn(GameUtils.getTimeStringNano(previousAverage.getAverageTime()), timerFontOptions);
            leftDrawY -= tableDraw.getHeight();
            tableDraw.draw(10, leftDrawY);
         }

         FontOptions subFontOptions = new FontOptions(12);
         rightDrawY -= 12;
         String infoString = Platform.get().getName() + ", build " + PlatformManager.getPlatform().getPlatformAppBuild();
         FontManager.bit
            .drawString(window.getHudWidth() - 4 - FontManager.bit.getWidthCeil(infoString, subFontOptions), rightDrawY, infoString, subFontOptions);
         if (ModLoader.getEnabledMods().size() > 0) {
            rightDrawY -= 12;
            String modsString = ModLoader.getEnabledMods().size() + " mod(s) loaded";
            FontManager.bit
               .drawString(window.getHudWidth() - 4 - FontManager.bit.getWidthCeil(modsString, subFontOptions), rightDrawY, modsString, subFontOptions);
         }

         rightDrawY -= 20;
         String versionString = "v. 1.3.3";
         FontOptions versionFontOptions = new FontOptions(20);
         FontManager.bit
            .drawString(
               window.getHudWidth() - 4 - FontManager.bit.getWidthCeil(versionString, versionFontOptions), rightDrawY, versionString, versionFontOptions
            );
      }

      if (GameInfo.bottomRightInfo != null) {
         rightDrawY -= 28;
         FontOptions infoFontOptions = new FontOptions(20).color(new Color(255, 255, 255, 150));
         FontManager.bit
            .drawString(
               window.getHudWidth() - 12 - FontManager.bit.getWidthCeil(GameInfo.bottomRightInfo, infoFontOptions),
               rightDrawY,
               GameInfo.bottomRightInfo,
               infoFontOptions
            );
      }
   }

   public static void setCursor(GameWindow.CURSOR cursor) {
      nextCursor = cursor;
      changedCursor = true;
   }

   public static void reapplyCursor(GameWindow window) {
      forceCursorChange = true;
      if (currentCursor != null) {
         changeCursor(currentCursor, window);
      }
   }

   private static void changeCursor(GameWindow.CURSOR cursor, GameWindow window) {
      if (forceCursorChange || currentCursor != cursor || currentCursorSize != Settings.cursorSize) {
         currentCursor = cursor;
         currentCursorSize = Settings.cursorSize;
         window.setCursor(cursor);
      }
   }

   public static void setCursorColor(Color color) {
      Settings.cursorColor = color;
      GameResources.loadCursors();
      setCursor(currentCursor);
      forceCursorChange = true;
   }

   public static void setCursorSize(int size) {
      Settings.cursorSize = size;
      GameResources.loadCursors();
      setCursor(currentCursor);
      forceCursorChange = true;
   }

   public static GameTexture getQuadTexture() {
      return GameResources.empty;
   }

   public static TextureDrawOptionsEnd initQuadDraw(int width, int height) {
      return getQuadTexture().initDraw().size(width, height);
   }

   private static void drawScreenshotEffect() {
      long delta = System.currentTimeMillis() - screenshotTime;
      if (delta < 350L) {
         float progress = (float)delta / 350.0F;
         float floatHeight;
         if (progress > 0.5F) {
            floatHeight = Math.abs(progress - 1.0F);
         } else {
            floatHeight = progress;
         }

         GameWindow window = WindowManager.getWindow();
         int effectHeight = (int)(window.getHudHeight() * Math.abs(floatHeight - 0.5F));
         initQuadDraw(window.getHudWidth(), window.getHudHeight() / 2).color(Color.BLACK).draw(0, -effectHeight);
         initQuadDraw(window.getHudWidth(), window.getHudHeight() / 2).color(Color.BLACK).draw(0, window.getHudHeight() / 2 + effectHeight);
      }
   }

   public static void drawLineAdv(int x1, int y1, int x2, int y2, float[] advCol) {
      GameResources.empty.bindTexture();
      GL11.glLoadIdentity();
      GL11.glBegin(1);
      GL11.glColor4f(advCol[0], advCol[1], advCol[2], advCol[3]);
      GL11.glVertex2f(x1, y1);
      GL11.glColor4f(advCol[4], advCol[5], advCol[6], advCol[7]);
      GL11.glVertex2f(x2, y2);
      GL11.glEnd();
   }

   public static void drawRectangleLines(Rectangle rectangle, float xOffset, float yOffset, float red, float green, float blue, float alpha) {
      GameResources.empty.bindTexture();
      GL11.glLoadIdentity();
      GL11.glBegin(2);
      GL11.glColor4f(red, green, blue, alpha);
      GL11.glVertex2f(xOffset + rectangle.x, yOffset + rectangle.y);
      GL11.glVertex2f(xOffset + rectangle.x + rectangle.width, yOffset + rectangle.y);
      GL11.glVertex2f(xOffset + rectangle.x + rectangle.width, yOffset + rectangle.y + rectangle.height);
      GL11.glVertex2f(xOffset + rectangle.x, yOffset + rectangle.y + rectangle.height);
      GL11.glEnd();
   }

   public static void drawRectangleLines(Rectangle rectangle, GameCamera camera, float red, float green, float blue, float alpha) {
      drawRectangleLines(rectangle, -camera.getX(), -camera.getY(), red, green, blue, alpha);
   }

   public static void drawRectangleLines(Rectangle rectangle, float red, float green, float blue, float alpha) {
      drawRectangleLines(rectangle, 0.0F, 0.0F, red, green, blue, alpha);
   }

   public static void drawShapeLines(Shape shape, float xOffset, float yOffset, float red, float green, float blue, float alpha) {
      float[] coords = new float[6];
      LinkedList<Runnable> runnables = new LinkedList<>();

      for (PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
         int SG = it.currentSegment(coords);
         if (SG == 4) {
            break;
         }

         if (SG == 1) {
            Float point = new Float(coords[0] + xOffset, coords[1] + yOffset);
            runnables.add(() -> GL11.glVertex2f(point.x, point.y));
         } else if (SG == 0) {
            runnables.add(() -> {
               GL11.glEnd();
               GL11.glBegin(3);
            });
         }
      }

      GameResources.empty.bindTexture();
      GL11.glLoadIdentity();
      GL11.glColor4f(red, green, blue, alpha);
      GL11.glBegin(3);

      for (Runnable runnable : runnables) {
         runnable.run();
      }

      GL11.glEnd();
   }

   public static void drawShape(Shape shape, float xOffset, float yOffset, boolean filled, float red, float green, float blue, float alpha) {
      float[] coords = new float[6];
      LinkedList<Float> points = new LinkedList<>();

      for (PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
         int SG = it.currentSegment(coords);
         if (SG == 4) {
            break;
         }

         if (SG == 0 || SG == 1) {
            points.add(new Float(coords[0] + xOffset, coords[1] + yOffset));
         }
      }

      GameResources.empty.bindTexture();
      GL11.glLoadIdentity();
      GL11.glColor4f(red, green, blue, alpha);
      if (points.size() <= 2) {
         GL11.glBegin(1);
      } else {
         GL11.glBegin(filled ? 9 : 2);
      }

      for (Float point : points) {
         GL11.glVertex2f(point.x, point.y);
      }

      GL11.glEnd();
   }

   public static void drawShape(Shape shape, boolean filled, float red, float green, float blue, float alpha) {
      drawShape(shape, 0.0F, 0.0F, filled, red, green, blue, alpha);
   }

   public static void drawShape(Shape shape, GameCamera camera, boolean filled, float red, float green, float blue, float alpha) {
      drawShape(shape, -camera.getX(), -camera.getY(), filled, red, green, blue, alpha);
   }

   public static void drawCircle(int centerX, int centerY, int radius, int segments, Color color, boolean filled) {
      drawCircle(
         centerX, centerY, radius, segments, color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F, filled
      );
   }

   public static void drawCircle(int centerX, int centerY, int radius, int segments, float red, float green, float blue, float alpha, boolean filled) {
      GameResources.empty.bindTexture();
      GL11.glLoadIdentity();
      GL11.glColor4f(red, green, blue, alpha);
      GL11.glBegin(filled ? 9 : 2);

      for (int i = 0; i < segments; i++) {
         double theta = (Math.PI * 2) * i / segments;
         double x = radius * Math.cos(theta);
         double y = radius * Math.sin(theta);
         GL11.glVertex2d(x + centerX, y + centerY);
      }

      GL11.glEnd();
   }

   public static void drawLine(int x1, int y1, int x2, int y2, Color color) {
      drawLineRGBA(x1, y1, x2, y2, color.getRed() / 255.0F, color.getGreen() / 255.0F, color.getBlue() / 255.0F, color.getAlpha() / 255.0F);
   }

   public static void drawLineRGBA(int x1, int y1, int x2, int y2, float red, float green, float blue, float alpha) {
      GameResources.empty.bindTexture();
      GL11.glLoadIdentity();
      GL11.glColor4f(red, green, blue, alpha);
      GL11.glBegin(1);
      GL11.glVertex2f(x1, y1);
      GL11.glVertex2f(x2, y2);
      GL11.glEnd();
   }

   private static boolean isDrawingScreenshotEffect() {
      return System.currentTimeMillis() - screenshotTime < 350L;
   }

   private static void clearShaderHistory() {
      while (!shaderHistory.isEmpty()) {
         stopShader(shaderHistory.getLast());
      }

      GameResources.sceneShader.use();
   }

   public static void stopShader(GameShader shader) {
      boolean fail = false;
      if (!shaderHistory.isEmpty()) {
         GameShader lastShader = shaderHistory.getLast();
         if (lastShader == shader) {
            shaderHistory.removeLast();
            GameShader nextShader = shaderHistory.isEmpty() ? null : shaderHistory.getLast();
            if (nextShader != null) {
               if (nextShader != shader) {
                  nextShader._ScreenUse();
               }
            } else {
               GL20.glUseProgram(0);
            }
         } else {
            fail = true;
         }
      } else {
         fail = true;
      }

      if (fail) {
         System.err.println("Stopped wrong shader. Remember to stop shader again before stopping another.");
         GameShader lastShader = shaderHistory.isEmpty() ? null : shaderHistory.getLast();
         System.err.println("Current shader in use is " + lastShader);
      }
   }

   public static void useShader(GameShader shader) {
      GameShader lastShader = shaderHistory.isEmpty() ? null : shaderHistory.getLast();
      shaderHistory.add(shader);
      if (shader != null) {
         if (lastShader != shader) {
            shader._ScreenUse();
         }
      } else if (lastShader != null) {
         GL20.glUseProgram(0);
      }
   }

   public static GameShader getCurrentShader() {
      return shaderHistory.isEmpty() ? null : shaderHistory.getLast();
   }

   public static void takeSceneAndHudShot(TickManager tickManager, ChatMessageList chat) {
      if (screenshotCooldown.get()) {
         if (chat != null) {
            chat.addMessage("Cannot take scene and hudshot at this time.");
         }
      } else {
         GameWindow window = WindowManager.getWindow();
         screenshotCooldown.set(true);

         try {
            int bpp = 4;
            int frameWidth = window.getFrameWidth();
            int frameHeight = window.getFrameHeight();
            GameFrameBuffer sceneBuffer = window.getNewFrameBuffer(frameWidth, frameHeight);

            try {
               clearShaderHistory();
               GL11.glClear(16640);
               if (sceneBuffer.isComplete()) {
                  try {
                     window.startSceneDraw();
                     GlobalData.getCurrentState().drawScene(tickManager, true);
                  } finally {
                     window.endSceneDraw();
                  }

                  try {
                     window.startSceneOverlayDraw();
                     GlobalData.getCurrentState().drawSceneOverlay(tickManager);
                  } finally {
                     window.endSceneOverlayDraw();
                  }

                  sceneBuffer.bindFrameBuffer();
                  GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                  GL11.glClear(16384);
                  window.renderScene(sceneBuffer);
                  ByteBuffer hudBuffer = readColorBufferFromFrameBuffer(sceneBuffer, true);
                  String buffer = GlobalData.appDataPath() + "screenshots/scene " + new SimpleDateFormat("yyyy-MM-dd HH'h'mm'm'ss's'").format(new Date());
                  new Thread(new Renderer.ShotSave(buffer, hudBuffer, frameWidth, frameHeight, bpp, "screenshotsave", chat)).start();
               } else {
                  GameLog.err.println("An error occurred creating sceneshot frame buffer.");
                  if (chat != null) {
                     chat.addMessage("There was an error taking sceneshot");
                  }
               }
            } finally {
               sceneBuffer.unbindFrameBuffer();
               sceneBuffer.dispose();
            }

            GL20.glUseProgram(0);
            GameFrameBuffer var63 = window.getNewFrameBuffer(frameWidth, frameHeight);

            try {
               if (var63.isComplete()) {
                  try {
                     window.startHudDraw();
                     GlobalData.getCurrentState().drawHud(tickManager);
                     hudManager.cleanUp();
                     GameTooltipManager.drawHudTooltips();
                  } finally {
                     window.endHudDraw();
                  }

                  var63.bindFrameBuffer();
                  GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                  GL11.glClear(16384);
                  window.renderHud(1.0F);
                  ByteBuffer var64 = readColorBufferFromFrameBuffer(var63, true);
                  String filePath = GlobalData.appDataPath() + "screenshots/hud " + new SimpleDateFormat("yyyy-MM-dd HH'h'mm'm'ss's'").format(new Date());
                  new Thread(new Renderer.ShotSave(filePath, var64, frameWidth, frameHeight, bpp, "screenshotsave", chat)).start();
               } else {
                  GameLog.err.println("An error occurred creating hudshot frame buffer.");
                  if (chat != null) {
                     chat.addMessage("There was an error taking hudshot");
                  }
               }
            } finally {
               var63.unbindFrameBuffer();
               var63.dispose();
            }
         } catch (Exception e) {
            screenshotCooldown.set(false);
            e.printStackTrace();
         } finally {
            if (!shaderHistory.isEmpty()) {
               GameShader lastShader = shaderHistory.getLast();
               if (lastShader != null) {
                  lastShader._ScreenUse();
               } else {
                  GL20.glUseProgram(0);
               }
            }
         }

         window.makeCurrent();
         window.getInput().clearInput();
         window.updateResize();
      }
   }

   public static void takeMapshot(Level level, Client client, GameCamera camera) {
      if (screenshotCooldown.get()) {
         if (client != null) {
            client.chat.addMessage("Cannot take mapshot at this time.");
         }
      } else {
         GameWindow window = WindowManager.getWindow();
         screenshotCooldown.set(true);

         try {
            GameLoadingScreen.clearLog();
            GameLoadingScreen.drawLoadingString(Localization.translate("loading", "mapshot"));
            int bpp = 3;
            GL20.glUseProgram(0);
            GameFrameBuffer frameBuffer = window.getNewFrameBuffer(camera.getWidth(), camera.getHeight());

            try {
               frameBuffer.bindFrameBuffer();
               if (frameBuffer.isComplete()) {
                  boolean lastHideUI = Settings.hideUI;
                  Settings.hideUI = true;
                  level.runGLContextRunnables();
                  level.drawUtils.prepareDraw(camera, client == null ? null : client.getPlayer(), null, true);
                  level.drawUtils.draw(camera, client == null ? null : client.getPlayer(), null, true);
                  window.update();
                  Settings.hideUI = lastHideUI;
                  ByteBuffer buffer = readColorBufferFromFrameBuffer(frameBuffer, false);
                  if (client != null) {
                     client.chat.addMessage(Localization.translate("misc", "mapshottip"));
                  }

                  String filePath = GlobalData.appDataPath() + "screenshots/" + new SimpleDateFormat("yyyy-MM-dd HH'h'mm'm'ss's'").format(new Date());
                  new Thread(
                        new Renderer.ShotSave(filePath, buffer, camera.getWidth(), camera.getHeight(), bpp, "mapshotsave", client == null ? null : client.chat)
                     )
                     .start();
               } else {
                  GameLog.err.println("An error occurred creating mapshot frame buffer.");
                  if (client != null) {
                     client.chat.addMessage("There was an error taking mapshot");
                  }
               }
            } finally {
               frameBuffer.unbindFrameBuffer();
               frameBuffer.dispose();
               if (!shaderHistory.isEmpty()) {
                  GameShader lastShader = shaderHistory.getLast();
                  if (lastShader != null) {
                     lastShader._ScreenUse();
                  } else {
                     GL20.glUseProgram(0);
                  }
               }
            }
         } catch (Exception e) {
            screenshotCooldown.set(false);
            e.printStackTrace();
         }

         window.makeCurrent();
         window.getInput().clearInput();
         window.tickWindowResize(true);
      }
   }

   public static void takeScreenshot(ChatMessageList chat) {
      if (!screenshotCooldown.get() && !isDrawingScreenshotEffect()) {
         GameWindow window = WindowManager.getWindow();
         SoundManager.playSound(GameResources.cameraShutter, SoundEffect.ui());
         screenshotCooldown.set(true);
         screenshotTime = System.currentTimeMillis();

         try {
            GL11.glReadBuffer(1028);
            int width = window.getWidth();
            int height = window.getHeight();
            int bpp = 4;
            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * bpp);
            GL11.glReadPixels(0, 0, width, height, 6408, 5121, buffer);
            String path = GlobalData.appDataPath() + "screenshots/" + new SimpleDateFormat("yyyy-MM-dd HH'h'mm'm'ss's'").format(new Date());
            new Thread(new Renderer.ShotSave(path, buffer, width, height, bpp, "screenshotsave", chat)).start();
         } catch (Exception e) {
            screenshotCooldown.set(false);
            e.printStackTrace();
         }

         window.getInput().clearInput();
      } else {
         if (chat != null) {
            chat.addMessage("Cannot take screenshot at this time.");
         }
      }
   }

   public static GameFrameBuffer getNewFrameBuffer(int width, int height) {
      GameWindow window = WindowManager.getWindow();
      return window.getNewFrameBuffer(width, height);
   }

   public static void renderLevelToBuffer(GameFrameBuffer frameBuffer, Level level, PlayerMob perspective, GameCamera camera, TickManager tickManager) {
      GameWindow window = WindowManager.getWindow();

      try {
         GL20.glUseProgram(0);

         try {
            frameBuffer.bindFrameBuffer();
            frameBuffer.clearColor();
            frameBuffer.clearDepth();
            if (frameBuffer.isComplete()) {
               boolean lastHideUI = Settings.hideUI;
               Settings.hideUI = true;
               level.drawUtils.prepareDraw(camera, perspective, tickManager, true);
               level.drawUtils.draw(camera, perspective, tickManager, true);
               Settings.hideUI = lastHideUI;
            } else {
               GameLog.err.println("An error occurred creating level shot frame buffer.");
            }
         } finally {
            frameBuffer.unbindFrameBuffer();
            if (!shaderHistory.isEmpty()) {
               GameShader lastShader = shaderHistory.getLast();
               if (lastShader != null) {
                  lastShader._ScreenUse();
               } else {
                  GL20.glUseProgram(0);
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }

      window.makeCurrent();
   }

   public static ByteBuffer readColorBufferFromFrameBuffer(GameFrameBuffer frameBuffer, boolean includeAlpha) {
      int bpp = includeAlpha ? 4 : 3;
      ByteBuffer buffer = BufferUtils.createByteBuffer(frameBuffer.getWidth() * frameBuffer.getHeight() * bpp);
      GL11.glBindTexture(3553, frameBuffer.getColorBufferTextureID());
      int oldPackAlignment = GL11.glGetInteger(3333);
      GL11.glPixelStorei(3333, 1);
      GL11.glGetTexImage(3553, 0, includeAlpha ? 6408 : 6407, 5121, buffer);
      GL11.glPixelStorei(3333, oldPackAlignment);
      return buffer;
   }

   public static void setMouseDraggingElement(MouseDraggingElement e) {
      mouseDraggingElement = e;
   }

   public static MouseDraggingElement getMouseDraggingElement() {
      return mouseDraggingElement;
   }

   public static int queryGLError(String uniqueID) {
      int error = GL11.glGetError();
      if (uniqueID != null) {
         printGLError(uniqueID, error);
      }

      return error;
   }

   public static void printGLError(String uniqueID, int error) {
      if (error != 0) {
         switch (error) {
            case 1280:
               if (uniqueID != null && !glErrorPrints.get(uniqueID).contains(error)) {
                  System.err.println("GLError." + uniqueID + ": GL_INVALID_ENUM");
                  new Throwable().printStackTrace(System.err);
                  glErrorPrints.add(uniqueID, error);
               }
               break;
            case 1281:
               if (uniqueID != null && !glErrorPrints.get(uniqueID).contains(error)) {
                  System.err.println("GLError." + uniqueID + ": GL_INVALID_VALUE");
                  new Throwable().printStackTrace(System.err);
                  glErrorPrints.add(uniqueID, error);
               }
               break;
            case 1282:
               if (uniqueID != null && !glErrorPrints.get(uniqueID).contains(error)) {
                  System.err.println("GLError." + uniqueID + ": GL_INVALID_OPERATION");
                  new Throwable().printStackTrace(System.err);
                  glErrorPrints.add(uniqueID, error);
               }
               break;
            case 1283:
               if (uniqueID != null && !glErrorPrints.get(uniqueID).contains(error)) {
                  System.err.println("GLError." + uniqueID + ": GL_STACK_OVERFLOW");
                  new Throwable().printStackTrace(System.err);
                  glErrorPrints.add(uniqueID, error);
               }
               break;
            case 1284:
               if (uniqueID != null && !glErrorPrints.get(uniqueID).contains(error)) {
                  System.err.println("GLError." + uniqueID + ": GL_STACK_UNDERFLOW");
                  new Throwable().printStackTrace(System.err);
                  glErrorPrints.add(uniqueID, error);
               }
               break;
            case 1285:
               if (uniqueID != null && !glErrorPrints.get(uniqueID).contains(error)) {
                  System.err.println("GLError." + uniqueID + ": GL_OUT_OF_MEMORY");
                  new Throwable().printStackTrace(System.err);
                  glErrorPrints.add(uniqueID, error);
               }
         }
      }
   }

   private static class ShotSave implements Runnable {
      private final ByteBuffer buffer;
      private final String filePath;
      private final String translateKey;
      private final int imageWidth;
      private final int imageHeight;
      private final int imageBPP;
      private final ChatMessageList chat;

      public ShotSave(String filePath, ByteBuffer buffer, int imageWidth, int imageHeight, int imageBPP, String translateKey, ChatMessageList chat) {
         this.filePath = filePath;
         this.buffer = buffer;
         this.imageWidth = imageWidth;
         this.imageHeight = imageHeight;
         this.imageBPP = imageBPP;
         this.translateKey = translateKey;
         this.chat = chat;
      }

      @Override
      public void run() {
         try {
            int size = this.imageWidth * this.imageHeight * this.imageBPP;
            if (this.buffer.capacity() != size) {
               throw new IllegalArgumentException("Buffer incorrect size: " + this.buffer.capacity() + ", expected: " + size);
            }

            File file = new File(this.filePath + ".png");
            GameUtils.mkDirs(file);
            ByteBuffer flippedBuffer = BufferUtils.createByteBuffer(size);

            for (int y = this.imageHeight - 1; y >= 0; y--) {
               int index = this.imageWidth * y * this.imageBPP;
               byte[] row = new byte[this.imageWidth * this.imageBPP];
               this.buffer.position(index);
               this.buffer.get(row, 0, row.length);
               flippedBuffer.put(row, 0, row.length);
            }

            flippedBuffer.position(0);
            STBImageWrite.stbi_write_png(file.getAbsolutePath(), this.imageWidth, this.imageHeight, this.imageBPP, flippedBuffer, 0);
            if (this.chat != null) {
               FairType type = new FairType();
               type.append(
                  FairCharacterGlyph.fromStringToOpenFile(
                     ChatMessage.fontOptions, Localization.translate("misc", this.translateKey, "path", file.getAbsolutePath()), file
                  )
               );
               this.chat.addMessage(type);
            }
         } catch (Exception e) {
            this.chat.addMessage(GameColor.RED.getColorCode() + "Error saving image: " + e.getMessage());
            e.printStackTrace();
            Renderer.screenshotCooldown.set(false);
         }

         Renderer.screenshotCooldown.set(false);
      }
   }
}
