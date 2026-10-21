package necesse.entity.particle;

import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.HashMapArrayList;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;

public class ParticleOptions {
   private static final int sortYAccuracy = 4;
   private static final int sortYHalf = 2;
   private final Level level;
   private HashMapArrayList<Integer, ParticleOptions.OptionContainer> sorted = new HashMapArrayList<>();
   private final GameLinkedList<ParticleOptions.OptionContainer> newSorted = new GameLinkedList<>();
   private final HashMapArrayList<Integer, ParticleOptions.OptionContainer> top = new HashMapArrayList<>();
   private final HashMapArrayList<Integer, ParticleOptions.OptionContainer> tile = new HashMapArrayList<>();
   private final LinkedList<ParticleOptions.OptionContainer> newTop = new LinkedList<>();
   private final LinkedList<ParticleOptions.OptionContainer> newTile = new LinkedList<>();
   private int count;

   public ParticleOptions(Level level) {
      this.level = level;
   }

   public void add(ParticleOption particle) {
      if (!this.level.isServer()) {
         ParticleOptions.OptionContainer p = new ParticleOptions.OptionContainer(this.level.getLocalTime(), particle, 0);
         synchronized (this.newSorted) {
            this.newSorted.addLast(p);
         }
      }
   }

   public void addTop(ParticleOption particle, int drawOrder) {
      if (!this.level.isServer()) {
         synchronized (this.newTop) {
            this.newTop.add(new ParticleOptions.OptionContainer(this.level.getLocalTime(), particle, drawOrder));
         }
      }
   }

   public void addTile(ParticleOption particle, int drawOrder) {
      if (!this.level.isServer()) {
         synchronized (this.newTile) {
            this.newTile.add(new ParticleOptions.OptionContainer(this.level.getLocalTime(), particle, drawOrder));
         }
      }
   }

   private boolean tickSorted(ParticleOptions.OptionContainer p, long time, float delta, HashMapArrayList<Integer, ParticleOptions.OptionContainer> newSorted) {
      int alive = (int)(time - p.spawnTime);
      float lifePercent = (float)alive / p.option.lifeTime;
      if (p.option.removed) {
         return false;
      } else if (alive > p.option.lifeTime) {
         p.option.tickProgress(p.option.lifeTime, p.option.lifeTime, 1.0F);
         p.option.remove();
         return false;
      } else {
         p.option.tick(this.level, delta, p.option.lifeTime, alive, lifePercent);
         newSorted.add((int)(p.option.getLevelPos().y / 4.0F), p);
         return true;
      }
   }

   public void tickMovement(float delta) {
      int count = 0;
      long time = this.level.getLocalTime();
      HashMapArrayList<Integer, ParticleOptions.OptionContainer> newSorted = new HashMapArrayList<>(100);
      synchronized (this.newSorted) {
         for (ParticleOptions.OptionContainer p : this.newSorted) {
            this.tickSorted(p, time, delta, newSorted);
         }

         this.newSorted.clear();
      }

      for (ArrayList<ParticleOptions.OptionContainer> list : this.sorted.values()) {
         for (ParticleOptions.OptionContainer p : list) {
            if (this.tickSorted(p, time, delta, newSorted)) {
               count++;
            }
         }
      }

      synchronized (this) {
         this.sorted = newSorted;
      }

      synchronized (this.tile) {
         synchronized (this.newTile) {
            for (ParticleOptions.OptionContainer optionContainer : this.newTile) {
               this.tile.add(optionContainer.drawOrder, optionContainer);
            }

            this.newTile.clear();
            count += this.tickMovement(delta, time, this.tile);
         }
      }

      synchronized (this.top) {
         synchronized (this.newTop) {
            for (ParticleOptions.OptionContainer optionContainer : this.newTop) {
               this.top.add(optionContainer.drawOrder, optionContainer);
            }

            this.newTop.clear();
         }

         count += this.tickMovement(delta, time, this.top);
      }

      this.count = count;
   }

   protected int tickMovement(float delta, long time, HashMapArrayList<Integer, ParticleOptions.OptionContainer> map) {
      int count = 0;
      LinkedList<Integer> emptyKeys = new LinkedList<>();

      for (Entry<Integer, ArrayList<ParticleOptions.OptionContainer>> entry : map.entrySet()) {
         ArrayList<ParticleOptions.OptionContainer> list = entry.getValue();

         for (int i = 0; i < list.size(); i++) {
            ParticleOptions.OptionContainer p = list.get(i);
            int alive = (int)(time - p.spawnTime);
            float lifePercent = (float)alive / p.option.lifeTime;
            if (p.option.removed) {
               list.remove(i);
               i--;
               if (list.isEmpty()) {
                  emptyKeys.add(entry.getKey());
               }
            } else if (alive > p.option.lifeTime) {
               p.option.tickProgress(p.option.lifeTime, p.option.lifeTime, 1.0F);
               p.option.remove();
               list.remove(i);
               i--;
               if (list.isEmpty()) {
                  emptyKeys.add(entry.getKey());
               }
            } else {
               p.option.tick(this.level, delta, p.option.lifeTime, alive, lifePercent);
               count++;
            }
         }
      }

      for (int drawOrder : emptyKeys) {
         map.clear(drawOrder);
      }

      return count;
   }

   public void tick() {
      synchronized (this) {
         this.sorted.values().stream().flatMap(Collection::stream).filter(p -> p.option.lightLevel > 0).forEach(p -> {
            Float levelPos = p.option.getLevelPos();
            this.level.lightManager.refreshParticleLightFloat(levelPos.x, levelPos.y, p.option.lightHue, p.option.lightSat, p.option.lightLevel);
         });
      }

      synchronized (this.tile) {
         this.tile.values().stream().flatMap(Collection::stream).filter(p -> p.option.lightLevel > 0).forEach(p -> {
            Float levelPos = p.option.getLevelPos();
            this.level.lightManager.refreshParticleLightFloat(levelPos.x, levelPos.y, p.option.lightHue, p.option.lightSat, p.option.lightLevel);
         });
      }

      synchronized (this.top) {
         this.top.values().stream().flatMap(Collection::stream).filter(p -> p.option.lightLevel > 0).forEach(p -> {
            Float levelPos = p.option.getLevelPos();
            this.level.lightManager.refreshParticleLightFloat(levelPos.x, levelPos.y, p.option.lightHue, p.option.lightSat, p.option.lightLevel);
         });
      }
   }

   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      Level level,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      long time = level.getLocalTime();
      synchronized (this) {
         for (Entry<Integer, ArrayList<ParticleOptions.OptionContainer>> entry : this.sorted.entrySet()) {
            final int sortY = entry.getKey() * 4 + 2;
            final SharedTextureDrawOptions options = new SharedTextureDrawOptions(GameResources.generatedParticlesTexture);

            for (ParticleOptions.OptionContainer p : entry.getValue()) {
               int timePassed = (int)(time - p.spawnTime);
               float lifePercent = Math.min((float)timePassed / p.option.lifeTime, 1.0F);
               p.option.addDrawOptions(options, level, p.option.lifeTime, timePassed, lifePercent, camera);
            }

            list.add(new LevelSortedDrawable(options) {
               @Override
               public int getSortY() {
                  return sortY;
               }

               @Override
               public void draw(TickManager tickManager) {
                  options.draw();
               }
            });
         }
      }

      synchronized (this.tile) {
         for (Entry<Integer, ArrayList<ParticleOptions.OptionContainer>> entry : this.tile.entrySet()) {
            ArrayList<ParticleOptions.OptionContainer> currentList = entry.getValue();
            SharedTextureDrawOptions options = new SharedTextureDrawOptions(GameResources.generatedParticlesTexture);

            for (ParticleOptions.OptionContainer p : currentList) {
               int timePassed = (int)(time - p.spawnTime);
               float lifePercent = (float)timePassed / p.option.lifeTime;
               p.option.addDrawOptions(options, level, p.option.lifeTime, timePassed, lifePercent, camera);
            }

            tileList.add(entry.getKey(), tm -> options.draw());
         }
      }

      synchronized (this.top) {
         for (Entry<Integer, ArrayList<ParticleOptions.OptionContainer>> entry : this.top.entrySet()) {
            ArrayList<ParticleOptions.OptionContainer> currentList = entry.getValue();
            SharedTextureDrawOptions options = new SharedTextureDrawOptions(GameResources.generatedParticlesTexture);

            for (ParticleOptions.OptionContainer p : currentList) {
               int timePassed = (int)(time - p.spawnTime);
               float lifePercent = (float)timePassed / p.option.lifeTime;
               p.option.addDrawOptions(options, level, p.option.lifeTime, timePassed, lifePercent, camera);
            }

            topList.add(entry.getKey(), tm -> options.draw());
         }
      }
   }

   public int count() {
      return this.count;
   }

   protected static class OptionContainer {
      protected final long spawnTime;
      protected final ParticleOption option;
      protected final int drawOrder;

      public OptionContainer(long spawnTime, ParticleOption option, int drawOrder) {
         this.spawnTime = spawnTime;
         this.option = option;
         this.drawOrder = drawOrder;
      }
   }
}
