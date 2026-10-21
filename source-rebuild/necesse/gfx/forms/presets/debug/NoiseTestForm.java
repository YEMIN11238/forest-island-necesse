package necesse.gfx.forms.presets.debug;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import necesse.engine.GameRandomNoise;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormCheckBox;
import necesse.gfx.forms.components.FormContentIconButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.FormSlider;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.forms.components.FormTextInput;
import necesse.gfx.forms.components.FormTextureMapBox;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.ui.ButtonColor;

public abstract class NoiseTestForm extends Form {
   protected int textureTileSize = 128;
   protected String seed = getRandomSeed();
   protected GameRandomNoise noise = new GameRandomNoise(this.seed.hashCode());
   protected FormSlider resolutionSlider;
   protected FormSlider octavesSlider;
   protected FormSlider persistenceSlider;
   private final FormCheckBox useSimplexCheckbox;
   protected FormTextureMapBox map;
   protected FormLabel debugLabel;
   protected ThreadPoolExecutor executor;
   protected HashMap<Point, GameTexture> textures = new HashMap<>();
   protected final LinkedList<GameTexture> texturesToDelete = new LinkedList<>();
   protected int texturesStartX = 0;
   protected int texturesStartY = 0;
   protected int texturesEndX = -1;
   protected int texturesEndY = -1;

   protected static String getRandomSeed() {
      StringBuilder seed = new StringBuilder();
      String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

      for (int i = 0; i < 5; i++) {
         int index = GameRandom.globalRandom.nextInt(alphabet.length());
         seed.append(alphabet, index, index + 1);
      }

      return seed.toString();
   }

   public NoiseTestForm() {
      super(400, 400);
      FormFlow flow = new FormFlow(4);
      this.addComponent(flow.nextY(new FormLabel("Noise tests", new FontOptions(20), -1, 4, 4), 4));
      this.addComponent(
            new FormContentIconButton(this.getWidth() - 24 - 4, 4, FormInputSize.SIZE_24, ButtonColor.BASE, this.getInterfaceStyle().container_storage_remove)
         )
         .onClicked(e -> this.removeForm());
      this.setDraggingBox(new Rectangle(this.getWidth(), flow.next()));
      this.addComponent(flow.nextY(new FormLabel("Seed", new FontOptions(16), -1, 10, 0), 4));
      int seedY = flow.next(32);
      FormTextInput input = this.addComponent(new FormTextInput(4, seedY, FormInputSize.SIZE_24, this.getWidth() - 8 - 24, -1));
      this.addComponent(
            new FormContentIconButton(this.getWidth() - 24 - 4, seedY, FormInputSize.SIZE_24, ButtonColor.BASE, this.getInterfaceStyle().inventory_sort)
         )
         .onClicked(e -> {
            this.seed = getRandomSeed();
            this.reloadNoise();
            this.reloadTextures();
            input.setText(this.seed);
         });
      input.setText(this.seed, false);
      input.onSubmit(e -> {
         this.seed = e.from.getText();
         this.reloadNoise();
         this.reloadTextures();
      });
      this.resolutionSlider = this.addComponent(flow.nextY(new FormSlider("Resolution", 10, 0, 5, 1, 20, this.getWidth() - 20), 10));
      this.resolutionSlider.drawValueInPercent = false;
      this.resolutionSlider.onChanged(e -> this.reloadTextures());
      this.octavesSlider = this.addComponent(flow.nextY(new FormSlider("Octaves", 10, 0, 1, 1, 10, this.getWidth() - 20), 10));
      this.octavesSlider.drawValueInPercent = false;
      this.octavesSlider.onChanged(e -> this.reloadTextures());
      this.persistenceSlider = this.addComponent(flow.nextY(new FormSlider("Persistence", 10, 0, 50, 0, 100, this.getWidth() - 20), 10));
      this.persistenceSlider.drawValueInPercent = true;
      this.persistenceSlider.onChanged(e -> this.reloadTextures());
      this.useSimplexCheckbox = this.addComponent(flow.nextY(new FormCheckBox("Use Simplex Noise", 10, 0, this.getWidth() - 20), 10).useButtonTexture());
      this.useSimplexCheckbox.onClicked(e -> this.reloadTextures());
      this.addComponent(flow.nextY(new FormTextButton("Reload noise", 4, 0, this.getWidth() - 8, FormInputSize.SIZE_24, ButtonColor.BASE), 4)).onClicked(e -> {
         this.reloadNoise();
         this.reloadTextures();
      });
      this.reloadNoise();
      int renderSize = this.getWidth();
      int[] zoomLevels = new int[]{2, 4, 6, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256, 384, 512};
      this.map = this.addComponent(
         flow.nextY(new FormTextureMapBox(0, 0, renderSize, renderSize, this.textureTileSize, zoomLevels, 3, true) {
            @Override
            public int getTileScale() {
               return 32;
            }

            @Override
            public Rectangle getTileBounds() {
               int size = 20000;
               return new Rectangle(-size, -size, size * 2, size * 2);
            }

            @Override
            public void drawMapTexture(int textureX, int textureY, double tileScale, int drawX, int drawY) {
               synchronized (this) {
                  GameTexture texture = NoiseTestForm.this.textures.get(new Point(textureX, textureY));
                  if (texture != null) {
                     int textureWidth = (int)(tileScale * NoiseTestForm.this.textureTileSize);
                     int textureHeight = (int)(tileScale * NoiseTestForm.this.textureTileSize);
                     texture.initDraw().size(textureWidth, textureHeight).draw(drawX, drawY);
                  }
               }
            }
         })
      );
      flow.next(4);
      this.debugLabel = this.addComponent(new FormLabel("", new FontOptions(16), -1, 10, flow.next(24)));
      this.setHeight(flow.next());
      GameWindow window = WindowManager.getWindow();
      this.setPosMiddle(window.getHudWidth() / 2, window.getHudHeight() / 2);
   }

   @Override
   protected void init() {
      super.init();
      this.reloadTextures();
   }

   public int getScale() {
      return this.resolutionSlider != null ? this.resolutionSlider.getValue() * 10 : 1;
   }

   public int getOctaves() {
      return this.octavesSlider != null ? this.octavesSlider.getValue() : 1;
   }

   public double getPersistence() {
      return this.persistenceSlider != null ? this.persistenceSlider.getPercentage() : 1.0;
   }

   protected synchronized void reloadNoise() {
      this.noise = new GameRandomNoise(this.seed.hashCode());
      this.texturesStartX = 0;
      this.texturesStartY = 0;
      this.texturesEndX = -1;
      this.texturesEndY = -1;
   }

   protected synchronized void reloadTextures() {
      this.textures = new HashMap<>();
      if (this.executor != null) {
         this.executor.shutdownNow();
      }

      this.executor = new ThreadPoolExecutor(0, 4, 1L, TimeUnit.MINUTES, new LinkedBlockingDeque<>(), r -> new Thread(null, r, "one-world-test-loader"));
      this.executor.allowCoreThreadTimeOut(true);
      this.executor.setCorePoolSize(4);
      this.texturesStartX = 0;
      this.texturesStartY = 0;
      this.texturesEndX = -1;
      this.texturesEndY = -1;
   }

   public synchronized void updateAndScheduleTextures() {
      Rectangle bounds = this.map.getCurrentDrawBounds();
      int modBoundsX = Math.floorMod(bounds.x, this.textureTileSize);
      int modBoundsY = Math.floorMod(bounds.y, this.textureTileSize);
      int modBoundsWidth = bounds.width % this.textureTileSize;
      int modBoundsHeight = bounds.height % this.textureTileSize;
      int texturesStartX = (int)Math.floor((double)bounds.x / this.textureTileSize);
      int texturesStartY = (int)Math.floor((double)bounds.y / this.textureTileSize);
      int texturesEndX = texturesStartX
         + (int)Math.ceil((double)bounds.width / this.textureTileSize)
         + (this.textureTileSize - modBoundsWidth <= modBoundsX ? 1 : 0);
      int texturesEndY = texturesStartY
         + (int)Math.ceil((double)bounds.height / this.textureTileSize)
         + (this.textureTileSize - modBoundsHeight <= modBoundsY ? 1 : 0);
      int prevTexturesStartX = this.texturesStartX;
      int prevTexturesEndX = this.texturesEndX;
      int prevTexturesStartY = this.texturesStartY;
      int prevTexturesEndY = this.texturesEndY;
      this.texturesStartX = texturesStartX;
      this.texturesEndX = texturesEndX;
      this.texturesStartY = texturesStartY;
      this.texturesEndY = texturesEndY;
      if (texturesStartX < prevTexturesStartX) {
         for (int x = texturesStartX; x <= prevTexturesStartX; x++) {
            for (int y = this.texturesStartY; y <= this.texturesEndY; y++) {
               this.startLoadTexture(x, y);
            }
         }
      } else if (texturesStartX > prevTexturesStartX) {
         for (int x = prevTexturesStartX; x < texturesStartX; x++) {
            for (int y = this.texturesStartY; y <= this.texturesEndY; y++) {
               GameTexture remove = this.textures.remove(new Point(x, y));
               if (remove != null) {
                  remove.delete();
               }
            }
         }
      }

      if (texturesEndX > prevTexturesEndX) {
         for (int x = prevTexturesEndX; x <= texturesEndX; x++) {
            for (int y = this.texturesStartY; y <= this.texturesEndY; y++) {
               this.startLoadTexture(x, y);
            }
         }
      } else if (texturesEndX < prevTexturesEndX) {
         for (int x = texturesEndX; x <= prevTexturesEndX; x++) {
            for (int y = this.texturesStartY; y <= this.texturesEndY; y++) {
               GameTexture remove = this.textures.remove(new Point(x, y));
               if (remove != null) {
                  remove.delete();
               }
            }
         }
      }

      if (texturesStartY < prevTexturesStartY) {
         for (int x = this.texturesStartX; x <= this.texturesEndX; x++) {
            for (int y = texturesStartY; y <= prevTexturesStartY; y++) {
               this.startLoadTexture(x, y);
            }
         }
      } else if (texturesStartY > prevTexturesStartY) {
         for (int x = this.texturesStartX; x <= this.texturesEndX; x++) {
            for (int y = prevTexturesStartY; y < texturesStartY; y++) {
               GameTexture remove = this.textures.remove(new Point(x, y));
               if (remove != null) {
                  remove.delete();
               }
            }
         }
      }

      if (texturesEndY > prevTexturesEndY) {
         for (int x = this.texturesStartX; x <= this.texturesEndX; x++) {
            for (int y = prevTexturesEndY; y <= texturesEndY; y++) {
               this.startLoadTexture(x, y);
            }
         }
      } else if (texturesEndY < prevTexturesEndY) {
         for (int x = this.texturesStartX; x <= this.texturesEndX; x++) {
            for (int y = texturesEndY; y <= prevTexturesEndY; y++) {
               GameTexture remove = this.textures.remove(new Point(x, y));
               if (remove != null) {
                  remove.delete();
               }
            }
         }
      }

      this.debugLabel
         .setText(
            bounds.x
               + "x"
               + bounds.y
               + ", "
               + bounds.width
               + "x"
               + bounds.height
               + ", "
               + this.executor.getActiveCount()
               + ", "
               + this.executor.getQueue().size()
         );
   }

   public synchronized void startLoadTexture(int textureX, int textureY) {
      if (this.executor != null) {
         HashMap<Point, GameTexture> textures = this.textures;
         int scale = this.getScale();
         int octaves = this.getOctaves();
         double persistence = this.getPersistence();
         this.executor.submit(() -> {
            if (textures == this.textures) {
               if (textureX >= this.texturesStartX && textureX <= this.texturesEndX && textureY >= this.texturesStartY && textureY <= this.texturesEndY) {
                  GameTexture texture = new GameTexture("NoiseTest", this.textureTileSize, this.textureTileSize);
                  texture.setBlendQuality(GameTexture.BlendQuality.NEAREST);

                  for (int x = 0; x < this.textureTileSize; x++) {
                     for (int y = 0; y < this.textureTileSize; y++) {
                        double noiseX = (double)(x + textureX * this.textureTileSize) / scale;
                        double noiseY = (double)(y + textureY * this.textureTileSize) / scale;
                        double value;
                        if (octaves > 1) {
                           if (this.useSimplexCheckbox.checked) {
                              value = this.noise.simplex2Fractal(noiseX, noiseY, octaves, persistence);
                           } else {
                              value = this.noise.perlin2Fractal(noiseX, noiseY, octaves, persistence);
                           }
                        } else if (this.useSimplexCheckbox.checked) {
                           value = this.noise.simplex2(noiseX, noiseY);
                        } else {
                           value = this.noise.perlin2(noiseX, noiseY);
                        }

                        float percentValue = (float)GameMath.map(value, -1.0, 1.0, 0.0, 1.0);
                        Color color = new Color(percentValue, percentValue, percentValue, 1.0F);
                        texture.setPixel(x, y, color);
                     }
                  }

                  if (textures == this.textures) {
                     this.putTexture(textureX, textureY, texture);
                  }
               }
            }
         });
      }
   }

   protected synchronized void putTexture(int textureX, int textureY, GameTexture texture) {
      Point point = new Point(textureX, textureY);
      GameTexture last = this.textures.get(point);
      if (last != null) {
         synchronized (this.texturesToDelete) {
            this.texturesToDelete.add(last);
         }
      }

      if (textureX >= this.texturesStartX && textureX <= this.texturesEndX && textureY >= this.texturesStartY && textureY <= this.texturesEndY) {
         this.textures.put(point, texture);
      }
   }

   @Override
   public void draw(TickManager tickManager, PlayerMob perspective, Rectangle renderBox) {
      this.updateAndScheduleTextures();
      synchronized (this.texturesToDelete) {
         for (GameTexture texture : this.texturesToDelete) {
            texture.delete();
         }

         this.texturesToDelete.clear();
      }

      super.draw(tickManager, perspective, renderBox);
   }

   public synchronized void deleteTextures() {
      for (GameTexture value : this.textures.values()) {
         value.delete();
      }

      this.textures.clear();
   }

   @Override
   public void dispose() {
      super.dispose();
      this.deleteTextures();
      if (this.executor != null) {
         this.executor.shutdownNow();
      }
   }

   public abstract void removeForm();
}
