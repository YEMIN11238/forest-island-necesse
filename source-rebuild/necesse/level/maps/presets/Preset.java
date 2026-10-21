package necesse.level.maps.presets;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import necesse.engine.GameLog;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.JournalRegistry;
import necesse.engine.registries.LogicGateRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.registries.TileRegistry;
import necesse.engine.registries.VersionMigration;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LevelIdentifier;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.SignObjectEntity;
import necesse.gfx.camera.GameCamera;
import necesse.inventory.lootTable.LootTable;
import necesse.level.gameLogicGate.entities.LogicGateEntity;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.multiTile.MultiTile;

public class Preset {
   public int width;
   public int height;
   public int[] tiles;
   public int[][] objects;
   public byte[][] objectRotations;
   public boolean clearOtherWires = false;
   public byte[] wires;
   private Boolean hasAnyWireData = null;
   public HashMap<Point, Preset.PresetLogicGate> logicGates = new HashMap<>();
   public HashMap<Point, Preset.PresetObjectEntity> objectEntities = new HashMap<>();
   public ArrayList<Preset.ApplyPredicate> applyPredicates = new ArrayList<>();
   public ArrayList<Consumer<GameBlackboard>> blackboardSetups = new ArrayList<>();
   public ArrayList<Preset.CustomApply> customPreApplies = new ArrayList<>();
   public ArrayList<Preset.CustomApply> customApplies = new ArrayList<>();
   public ArrayList<Preset.TileApplyListener> tileApplyListeners = new ArrayList<>();
   public ArrayList<Preset.ObjectApplyListener> objectApplyListeners = new ArrayList<>();
   public ArrayList<Preset.WireApplyListener> wireApplyListeners = new ArrayList<>();

   public Preset(int width, int height) {
      this.width = width;
      this.height = height;
      this.clearPreset();
   }

   public static LoadData getLoadDataFromScript(String script) {
      try {
         LoadData loadData = new LoadData(script);
         if (loadData.hasLoadDataByName("width") && loadData.hasLoadDataByName("height")) {
            return loadData;
         } else {
            throw new LoadDataException("Preset script does not contain width and height data");
         }
      } catch (Exception e) {
         try {
            script = GameUtils.getStringFromBytes(GameUtils.decompressData(GameUtils.fromBase64(script)));
            return new LoadData(script);
         } catch (Exception e2) {
            throw new RuntimeException(e2);
         }
      }
   }

   public Preset(String script) {
      this(getLoadDataFromScript(script));
   }

   public Preset(LoadData save) {
      this(save.getInt("width"), save.getInt("height"));
      this.applySave(0, 0, save);
   }

   public static Preset copyFromLevel(Level level, int x, int y, int width, int height) {
      Preset preset = new Preset(width, height);
      preset.copyFromLevel(level, x, y);
      return preset;
   }

   public void clearPreset() {
      this.tiles = new int[this.width * this.height];
      this.objects = new int[ObjectLayerRegistry.getTotalLayers()][this.width * this.height];
      this.objectRotations = new byte[ObjectLayerRegistry.getTotalLayers()][this.width * this.height];
      this.wires = new byte[this.width * this.height];
      this.hasAnyWireData = null;

      for (int x = 0; x < this.width; x++) {
         for (int y = 0; y < this.height; y++) {
            this.setTile(x, y, -1);

            for (int layer = 0; layer < this.objects.length; layer++) {
               this.setObjectLayer(layer, x, y, -1);
            }
         }
      }
   }

   protected Preset newObject(int width, int height) {
      return new Preset(width, height);
   }

   public Preset copy() {
      return this.copy(new PresetCopyFilter());
   }

   public Preset copy(PresetCopyFilter filter) {
      Preset preset = this.newObject(this.width, this.height);
      preset.copyData(this, filter);
      return preset;
   }

   private void copyData(Preset from, PresetCopyFilter filter) {
      if (filter.acceptTiles) {
         this.tiles = (int[])from.tiles.clone();
      }

      if (filter.acceptObjects) {
         this.objects = Arrays.stream(from.objects).map(rec$ -> (int[])((int[])rec$).clone()).toArray(int[][]::new);
         this.objectRotations = Arrays.stream(from.objectRotations).map(rec$ -> (byte[])((byte[])rec$).clone()).toArray(byte[][]::new);
         if (filter.acceptObjectEntities) {
            this.objectEntities.putAll(from.objectEntities);
         }
      }

      if (filter.acceptWires) {
         this.wires = (byte[])from.wires.clone();
         this.hasAnyWireData = from.hasAnyWireData;
         this.logicGates.putAll(from.logicGates);
      }

      this.applyPredicates.addAll(from.applyPredicates);
      this.customPreApplies.addAll(from.customPreApplies);
      this.customApplies.addAll(from.customApplies);
      this.tileApplyListeners.addAll(from.tileApplyListeners);
      this.objectApplyListeners.addAll(from.objectApplyListeners);
      this.wireApplyListeners.addAll(from.wireApplyListeners);
   }

   public final Preset tryMirrorX() {
      try {
         return this.mirrorX();
      } catch (PresetMirrorException e) {
         return this.copy();
      }
   }

   public Preset mirrorX() throws PresetMirrorException {
      Preset preset = this.newObject(this.width, this.height);
      preset.mirrorXData(this);
      return preset;
   }

   private void mirrorXData(Preset from) throws PresetMirrorException {
      boolean[] computed = new boolean[this.width * this.height];

      for (int x = 0; x < this.width; x++) {
         int mirrorX = this.getMirroredX(x);

         for (int y = 0; y < this.height; y++) {
            setInt(this.tiles, this.width, mirrorX, y, getInt(from.tiles, this.width, x, y));

            for (int layer = 0; layer < from.objects.length; layer++) {
               int objectID = getInt(from.objects[layer], this.width, x, y);
               if (objectID != -1) {
                  GameObject obj = ObjectRegistry.getObject(objectID);
                  if (obj != null) {
                     byte objectRotation = getByte(from.objectRotations[layer], this.width, x, y);
                     MultiTile multiTile = obj.getMultiTile(objectRotation);
                     Point posOffset = multiTile.getMirrorXPosOffset();
                     if (posOffset == null) {
                        throw new PresetMirrorException(new LocalMessage("ui", "presetmirrorerror", "item", obj.getLocalization()));
                     }

                     setInt(this.objects[layer], this.width, mirrorX + posOffset.x, y + posOffset.y, objectID);
                     setByte(this.objectRotations[layer], this.width, mirrorX + posOffset.x, y + posOffset.y, (byte)multiTile.getXMirrorRotation());
                     setBoolean(computed, this.width, mirrorX, y, true);
                  } else {
                     setInt(this.objects[layer], this.width, mirrorX, y, objectID);
                  }
               } else if (!getBoolean(computed, this.width, mirrorX, y)) {
                  setInt(this.objects[layer], this.width, mirrorX, y, objectID);
               }
            }

            setByte(this.wires, this.width, mirrorX, y, getByte(from.wires, this.width, x, y));
         }
      }

      this.hasAnyWireData = null;
      from.logicGates
         .forEach(
            (p, lg) -> this.logicGates
               .put(new Point(this.getMirroredX(p.x), p.y), new Preset.PresetLogicGate(lg.logicGateID, lg.data, !lg.mirrorX, lg.mirrorY, lg.rotation))
         );
      from.objectEntities
         .forEach(
            (p, lg) -> this.objectEntities
               .put(new Point(this.getMirroredX(p.x), p.y), new Preset.PresetObjectEntity(lg.data, !lg.mirrorX, lg.mirrorY, lg.rotation))
         );

      for (Preset.ApplyPredicate applyPredicate : from.applyPredicates) {
         this.applyPredicates.add(applyPredicate.mirrorX(this.width));
      }

      for (Preset.CustomApply customApply : from.customPreApplies) {
         this.customPreApplies.add(customApply.mirrorX(this.width));
      }

      for (Preset.CustomApply customApply : from.customApplies) {
         this.customApplies.add(customApply.mirrorX(this.width));
      }

      this.tileApplyListeners.addAll(from.tileApplyListeners);
      this.objectApplyListeners.addAll(from.objectApplyListeners);
      this.wireApplyListeners.addAll(from.wireApplyListeners);
   }

   public final Preset tryMirrorY() {
      try {
         return this.mirrorY();
      } catch (PresetMirrorException e) {
         return this.copy();
      }
   }

   public Preset mirrorY() throws PresetMirrorException {
      Preset preset = this.newObject(this.width, this.height);
      preset.mirrorYData(this);
      return preset;
   }

   private void mirrorYData(Preset from) throws PresetMirrorException {
      boolean[] computed = new boolean[this.width * this.height];

      for (int y = 0; y < this.height; y++) {
         int mirrorY = this.getMirroredY(y);

         for (int x = 0; x < this.width; x++) {
            setInt(this.tiles, this.width, x, mirrorY, getInt(from.tiles, this.width, x, y));

            for (int layer = 0; layer < from.objects.length; layer++) {
               int objectID = getInt(from.objects[layer], this.width, x, y);
               if (objectID != -1) {
                  GameObject obj = ObjectRegistry.getObject(objectID);
                  if (obj != null) {
                     byte objectRotation = getByte(from.objectRotations[layer], this.width, x, y);
                     MultiTile multiTile = obj.getMultiTile(objectRotation);
                     Point posOffset = multiTile.getMirrorYPosOffset();
                     if (posOffset == null) {
                        throw new PresetMirrorException(new LocalMessage("ui", "presetmirrorerror", "item", obj.getLocalization()));
                     }

                     setInt(this.objects[layer], this.width, x + posOffset.x, mirrorY + posOffset.y, objectID);
                     setByte(this.objectRotations[layer], this.width, x + posOffset.x, mirrorY + posOffset.y, (byte)multiTile.getYMirrorRotation());
                     setBoolean(computed, this.width, x, mirrorY, true);
                  } else {
                     setInt(this.objects[layer], this.width, x, mirrorY, objectID);
                  }
               } else if (!getBoolean(computed, this.width, x, mirrorY)) {
                  setInt(this.objects[layer], this.width, x, mirrorY, -1);
               }
            }

            setByte(this.wires, this.width, x, mirrorY, getByte(from.wires, this.width, x, y));
         }
      }

      this.hasAnyWireData = null;
      from.logicGates
         .forEach(
            (p, lg) -> this.logicGates
               .put(new Point(p.x, this.getMirroredY(p.y)), new Preset.PresetLogicGate(lg.logicGateID, lg.data, lg.mirrorX, !lg.mirrorY, lg.rotation))
         );
      from.objectEntities
         .forEach(
            (p, lg) -> this.objectEntities
               .put(new Point(p.x, this.getMirroredY(p.y)), new Preset.PresetObjectEntity(lg.data, lg.mirrorX, !lg.mirrorY, lg.rotation))
         );

      for (Preset.ApplyPredicate applyPredicate : from.applyPredicates) {
         this.applyPredicates.add(applyPredicate.mirrorY(this.height));
      }

      for (Preset.CustomApply customApply : from.customPreApplies) {
         this.customPreApplies.add(customApply.mirrorY(this.height));
      }

      for (Preset.CustomApply customApply : from.customApplies) {
         this.customApplies.add(customApply.mirrorY(this.height));
      }

      this.tileApplyListeners.addAll(from.tileApplyListeners);
      this.objectApplyListeners.addAll(from.objectApplyListeners);
      this.wireApplyListeners.addAll(from.wireApplyListeners);
   }

   public final Preset tryRotate(PresetRotation rotation) {
      try {
         return this.rotate(rotation);
      } catch (PresetRotateException e) {
         return this.copy();
      }
   }

   public Preset rotate(PresetRotation rotation) throws PresetRotateException {
      Point dim = PresetUtils.getRotatedPoint(this.width, this.height, 0, 0, rotation);
      Preset preset = this.newObject(Math.abs(dim.x), Math.abs(dim.y));
      preset.rotateData(this, rotation);
      return preset;
   }

   private void rotateData(Preset from, PresetRotation rotation) throws PresetRotateException {
      boolean[] computed = new boolean[this.width * this.height];

      for (int x = 0; x < from.width; x++) {
         for (int y = 0; y < from.height; y++) {
            Point rp = PresetUtils.getRotatedPointInSpace(x, y, from.width, from.height, rotation);
            setInt(this.tiles, this.width, rp.x, rp.y, getInt(from.tiles, from.width, x, y));

            for (int layer = 0; layer < from.objects.length; layer++) {
               int objectID = getInt(from.objects[layer], from.width, x, y);
               if (objectID != -1) {
                  GameObject obj = ObjectRegistry.getObject(objectID);
                  if (obj != null) {
                     byte objectRotation = getByte(from.objectRotations[layer], from.width, x, y);
                     MultiTile multiTile = obj.getMultiTile(objectRotation);
                     Point posOffset = multiTile.getPresetRotationOffset(rotation);
                     if (posOffset == null) {
                        throw new PresetRotateException(new LocalMessage("ui", "presetrotateerror", "item", obj.getLocalization()));
                     }

                     setInt(this.objects[layer], this.width, rp.x + posOffset.x, rp.y + posOffset.y, objectID);
                     setByte(this.objectRotations[layer], this.width, rp.x + posOffset.x, rp.y + posOffset.y, (byte)multiTile.getPresetRotation(rotation));
                     setBoolean(computed, this.width, rp.x, rp.y, true);
                  } else {
                     setInt(this.objects[layer], this.width, rp.x, rp.y, objectID);
                  }
               } else if (!getBoolean(computed, this.width, rp.x, rp.y)) {
                  setInt(this.objects[layer], this.width, rp.x, rp.y, -1);
               }
            }

            setByte(this.wires, this.width, rp.x, rp.y, getByte(from.wires, from.width, x, y));
         }
      }

      this.hasAnyWireData = null;
      from.logicGates
         .forEach(
            (p, lg) -> this.logicGates
               .put(
                  PresetUtils.getRotatedPointInSpace(p.x, p.y, from.width, from.height, rotation),
                  new Preset.PresetLogicGate(lg.logicGateID, lg.data, false, false, PresetRotation.addRotations(lg.rotation, rotation))
               )
         );
      from.objectEntities
         .forEach(
            (p, lg) -> this.objectEntities
               .put(
                  PresetUtils.getRotatedPointInSpace(p.x, p.y, from.width, from.height, rotation),
                  new Preset.PresetObjectEntity(lg.data, false, false, PresetRotation.addRotations(lg.rotation, rotation))
               )
         );

      for (Preset.ApplyPredicate applyPredicate : from.applyPredicates) {
         this.applyPredicates.add(applyPredicate.rotate(rotation, from.width, from.height));
      }

      for (Preset.CustomApply customApply : from.customPreApplies) {
         this.customPreApplies.add(customApply.rotate(rotation, from.width, from.height));
      }

      for (Preset.CustomApply customApply : from.customApplies) {
         this.customApplies.add(customApply.rotate(rotation, from.width, from.height));
      }

      this.tileApplyListeners.addAll(from.tileApplyListeners);
      this.objectApplyListeners.addAll(from.objectApplyListeners);
      this.wireApplyListeners.addAll(from.wireApplyListeners);
   }

   public void drawPlacePreview(Level level, int levelX, int levelY, PlayerMob perspective, GameCamera camera) {
      for (int x = 0; x < this.width; x++) {
         int tileX = levelX + x;
         if (level.isTileXWithinBounds(tileX)) {
            for (int y = 0; y < this.height; y++) {
               int tileY = levelY + y;
               if (level.isTileYWithinBounds(tileY)) {
                  int tile = getInt(this.tiles, this.width, x, y);
                  if (tile != -1) {
                     TileRegistry.getTile(tile).drawPreview(level, tileX, tileY, 0.5F, perspective, camera);
                  }
               }
            }
         }
      }

      for (int x = 0; x < this.width; x++) {
         int tileX = levelX + x;
         if (level.isTileXWithinBounds(tileX)) {
            for (int y = 0; y < this.height; y++) {
               int tileY = levelY + y;
               if (level.isTileYWithinBounds(tileY)) {
                  for (int layer = 0; layer < this.objects.length; layer++) {
                     int object = getInt(this.objects[layer], this.width, x, y);
                     if (object != -1) {
                        byte objectRotation = getByte(this.objectRotations[layer], this.width, x, y);
                        ObjectRegistry.getObject(object).drawPreview(level, tileX, tileY, objectRotation, 0.5F, perspective, camera);
                     }
                  }
               }
            }
         }
      }

      this.logicGates.forEach((tile, logicGate) -> {
         int tileXx = levelX + tile.x;
         int tileYx = levelY + tile.y;
         if (level.isTileWithinBounds(tileXx, tileYx)) {
            LogicGateRegistry.getLogicGate(logicGate.logicGateID).drawPreview(level, tileXx, tileYx, 0.5F, perspective, camera);
         }
      });

      for (int x = 0; x < this.width; x++) {
         int tileX = levelX + x;
         if (level.isTileXWithinBounds(tileX)) {
            for (int y = 0; y < this.height; y++) {
               int tileY = levelY + y;
               if (level.isTileYWithinBounds(tileY)) {
                  for (int wireID = 0; wireID < 4; wireID++) {
                     if (this.hasWire(x, y, wireID)) {
                        level.wireManager.drawWirePreset(tileX, tileY, camera, wireID);
                     }
                  }
               }
            }
         }
      }
   }

   public LinkedList<Preset.UndoLogic> applyToLevel(Level level, int levelX, int levelY) {
      return this.applyToLevel(level, levelX, levelY, new GameBlackboard());
   }

   public LinkedList<Preset.UndoLogic> applyToLevel(Level level, int levelX, int levelY, GameBlackboard blackboard) {
      for (Consumer<GameBlackboard> consumer : this.blackboardSetups) {
         consumer.accept(blackboard);
      }

      LinkedList<Preset.UndoLogic> undoLogics = new LinkedList<>();

      for (Preset.CustomApply ca : this.customPreApplies) {
         Preset.UndoLogic undoLogic = ca.applyToLevel(level, levelX, levelY, blackboard);
         if (undoLogic != null) {
            undoLogics.add(undoLogic);
         }
      }

      for (int i = 0; i < this.width; i++) {
         int tileX = levelX + i;
         if (level.isTileXWithinBounds(tileX)) {
            for (int j = 0; j < this.height; j++) {
               int tileY = levelY + j;
               if (level.isTileYWithinBounds(tileY)) {
                  int tile = getInt(this.tiles, this.width, i, j);
                  if (tile != -1) {
                     level.setTile(tileX, tileY, tile);

                     for (Preset.TileApplyListener listener : this.tileApplyListeners) {
                        listener.onTileApply(level, tileX, tileY, tile, blackboard);
                     }
                  }

                  byte wireData = getByte(this.wires, this.width, i, j);

                  for (int wireID = 0; wireID < 4; wireID++) {
                     if (this.clearOtherWires) {
                        if (GameMath.getBit(wireData, wireID * 2)) {
                           boolean isThere = GameMath.getBit(wireData, wireID * 2 + 1);
                           level.wireManager.setWire(tileX, tileY, wireID, isThere);

                           for (Preset.WireApplyListener listener : this.wireApplyListeners) {
                              listener.onWireApply(level, tileX, tileY, wireID, isThere, blackboard);
                           }
                        }
                     } else {
                        boolean isThere = GameMath.getBit(wireData, wireID * 2 + 1);
                        if (isThere) {
                           level.wireManager.setWire(tileX, tileY, wireID, isThere);

                           for (Preset.WireApplyListener listener : this.wireApplyListeners) {
                              listener.onWireApply(level, tileX, tileY, wireID, isThere, blackboard);
                           }
                        }
                     }
                  }

                  if (this.clearOtherWires) {
                     level.logicLayer.clearLogicGate(tileX, tileY);
                  }

                  for (int layer = 0; layer < this.objects.length; layer++) {
                     int object = getInt(this.objects[layer], this.width, i, j);
                     if (object != -1) {
                        byte objectRotation = getByte(this.objectRotations[layer], this.width, i, j);
                        level.objectLayer.setObject(layer, tileX, tileY, object);
                        level.objectLayer.setObjectRotation(layer, tileX, tileY, objectRotation);

                        for (Preset.ObjectApplyListener listener : this.objectApplyListeners) {
                           listener.onObjectApply(level, layer, tileX, tileY, object, objectRotation, blackboard);
                        }
                     }
                  }
               }
            }
         }
      }

      this.logicGates.forEach((tilex, presetLogicGate) -> {
         int tileX = levelX + tilex.x;
         int tileYx = levelY + tilex.y;
         if (level.isTileWithinBounds(tileX, tileYx)) {
            presetLogicGate.applyToLevel(level, tileX, tileYx, blackboard);
         }
      });
      this.objectEntities.forEach((tilex, presetObjectEntity) -> {
         int tileX = levelX + tilex.x;
         int tileYx = levelY + tilex.y;
         if (level.isTileWithinBounds(tileX, tileYx)) {
            presetObjectEntity.applyToLevel(level, tileX, tileYx, blackboard);
         }
      });

      for (Preset.CustomApply ca : this.customApplies) {
         Preset.UndoLogic undoLogic = ca.applyToLevel(level, levelX, levelY, blackboard);
         if (undoLogic != null) {
            undoLogics.add(undoLogic);
         }
      }

      return undoLogics;
   }

   public void applyToLevelCentered(Level level, int x, int y) {
      this.applyToLevelCentered(level, x, y, new GameBlackboard());
   }

   public void applyToLevelCentered(Level level, int x, int y, GameBlackboard blackboard) {
      this.applyToLevel(level, x - this.width / 2, y - this.height / 2, blackboard);
   }

   public boolean canApplyToLevel(Level level, int levelX, int levelY) {
      for (Preset.ApplyPredicate applyPredicate : this.applyPredicates) {
         if (!applyPredicate.canApplyToLevel(level, levelX, levelY)) {
            return false;
         }
      }

      return true;
   }

   public boolean canApplyToLevelCentered(Level level, int x, int y) {
      return this.canApplyToLevel(level, x - this.width / 2, y - this.height / 2);
   }

   public Preset subPreset(int x, int y, int width, int height) {
      width = Math.min(this.width - x, width);
      height = Math.min(this.height - y, height);
      Preset sub = new Preset(width, height);
      sub.hasAnyWireData = null;

      for (int subX = 0; subX < width; subX++) {
         for (int subY = 0; subY < height; subY++) {
            setInt(sub.tiles, width, subX, subY, getInt(this.tiles, this.width, x + subX, y + subY));

            for (int layer = 0; layer < this.objects.length; layer++) {
               setInt(sub.objects[layer], width, subX, subY, getInt(this.objects[layer], this.width, x + subX, y + subY));
               setByte(sub.objectRotations[layer], width, subX, subY, getByte(this.objectRotations[layer], this.width, x + subX, y + subY));
            }

            setByte(sub.wires, width, subX, subY, getByte(this.wires, this.width, x + subX, y + subY));
         }
      }

      return sub;
   }

   public Preset subPresetFull(int x, int y, int width, int height) {
      width = Math.min(this.width - x, width);
      height = Math.min(this.height - y, height);
      Preset sub = new Preset(width, height);
      sub.applyPreset(-x, -y, this);
      return sub;
   }

   public void applyPresetRaw(int x, int y, Preset other) throws PresetCopyException {
      for (int i = Math.max(x, 0); i < x + other.width; i++) {
         if (i < this.width) {
            for (int j = Math.max(y, 0); j < y + other.height; j++) {
               if (j < this.height) {
                  this.setTile(i, j, other.getTile(i - x, j - y));
                  this.setObject(i, j, other.getObject(i - x, j - y));
                  this.setRotation(i, j, other.getObjectRotation(i - x, j - y));
                  this.setWireData(i, j, other.getWireData(i - x, j - y));
               }
            }
         }
      }

      for (Entry<Point, Preset.PresetLogicGate> entry : other.logicGates.entrySet()) {
         Point otherPos = entry.getKey();
         Preset.PresetLogicGate gate = entry.getValue();
         Point newPos = new Point(otherPos.x + x, otherPos.y + y);
         if (newPos.x >= 0 && newPos.y >= 0 && newPos.x < this.width && newPos.y < this.height) {
            this.logicGates.put(newPos, gate.copy());
         }
      }

      this.blackboardSetups.addAll(other.blackboardSetups);

      for (Preset.ApplyPredicate predicate : other.applyPredicates) {
         this.applyPredicates.add(predicate.copy(x, y, this.width, this.height));
      }

      for (Preset.CustomApply apply : other.customPreApplies) {
         Preset.CustomApply copy = apply.copy(x, y, this.width, this.height);
         if (copy != null) {
            this.customPreApplies.add(copy);
         }
      }

      for (Preset.CustomApply apply : other.customApplies) {
         Preset.CustomApply copy = apply.copy(x, y, this.width, this.height);
         if (copy != null) {
            this.customApplies.add(copy);
         }
      }

      this.tileApplyListeners.addAll(other.tileApplyListeners);
      this.objectApplyListeners.addAll(other.objectApplyListeners);
      this.wireApplyListeners.addAll(other.wireApplyListeners);
   }

   public void applyPreset(int x, int y, Preset other) {
      try {
         this.applyPresetRaw(x, y, other);
      } catch (PresetCopyException var5) {
      }
   }

   public void applyScript(String script) {
      this.applyScript(0, 0, script);
   }

   public void applyScript(int x, int y, String script) {
      this.applySave(x, y, new LoadData(script));
   }

   public void applySave(int x, int y, LoadData save) {
      int scriptWidth = save.getInt("width");
      int scriptHeight = save.getInt("height");
      int[] scriptTiles = null;
      if (save.hasLoadDataByName("tiles")) {
         scriptTiles = save.getIntArray("tiles");
      }

      if (scriptTiles != null) {
         if (save.hasLoadDataByName("tileIDs")) {
            String[] tilesFrom = new String[TileRegistry.getTileStringIDs().length];
            String[] tileIDs = save.getStringArray("tileIDs");

            for (int i = 0; i < tileIDs.length; i += 2) {
               try {
                  int id = Integer.parseInt(tileIDs[i]);
                  if (id >= 0) {
                     if (tilesFrom.length <= id) {
                        tilesFrom = Arrays.copyOf(tilesFrom, id + 1);
                     }

                     tilesFrom[id] = tileIDs[i + 1];
                  }
               } catch (NumberFormatException var24) {
               }
            }

            VersionMigration.convertArray(scriptTiles, tilesFrom, TileRegistry.getTileStringIDs(), -1, VersionMigration.oldTileStringIDs);
         } else if (save.hasLoadDataByName("tileData")) {
            String[] tilesFrom = save.getStringArray("tileData");
            String[] tilesTo = TileRegistry.getTileStringIDs();
            if (!Arrays.equals(tilesFrom, tilesTo)) {
               VersionMigration.convertArray(scriptTiles, tilesFrom, tilesTo, -1, VersionMigration.oldTileStringIDs);
            }
         }
      }

      int[][] scriptObjects = new int[ObjectLayerRegistry.getTotalLayers()][];

      for (int layer = 0; layer < scriptObjects.length; layer++) {
         String saveStringID = layer == 0 ? "objects" : ObjectLayerRegistry.getLayerStringID(layer) + "Objects";
         if (save.hasLoadDataByName(saveStringID)) {
            scriptObjects[layer] = save.getIntArray(saveStringID);
         } else if ((layer > 0 && save.hasLoadDataByName(saveStringID + "Clear") || save.hasLoadDataByName("objects"))
            && save.getBoolean(saveStringID + "Clear", true, false)) {
            scriptObjects[layer] = new int[this.width * this.height];
         }
      }

      if (save.hasLoadDataByName("objectIDs")) {
         String[] objectsFrom = new String[ObjectRegistry.getObjectStringIDs().length];
         String[] objectIDs = save.getStringArray("objectIDs");

         for (int i = 0; i < objectIDs.length; i += 2) {
            try {
               int id = Integer.parseInt(objectIDs[i]);
               if (id >= 0) {
                  if (objectsFrom.length <= id) {
                     objectsFrom = Arrays.copyOf(objectsFrom, id + 1);
                  }

                  objectsFrom[id] = objectIDs[i + 1];
               }
            } catch (NumberFormatException var23) {
            }
         }

         for (int[] layerObjects : scriptObjects) {
            if (layerObjects != null && layerObjects.length != 0) {
               VersionMigration.convertArray(layerObjects, objectsFrom, ObjectRegistry.getObjectStringIDs(), -1, VersionMigration.oldObjectStringIDs);
            }
         }
      } else if (save.hasLoadDataByName("objectData")) {
         String[] objectsFrom = save.getStringArray("objectData");
         String[] objectsTo = ObjectRegistry.getObjectStringIDs();
         if (!Arrays.equals(objectsFrom, objectsTo)) {
            for (int[] layerObjects : scriptObjects) {
               if (layerObjects != null && layerObjects.length != 0) {
                  VersionMigration.convertArray(layerObjects, objectsFrom, objectsTo, -1, VersionMigration.oldObjectStringIDs);
               }
            }
         }
      }

      byte[][] scriptObjectRotations = new byte[ObjectLayerRegistry.getTotalLayers()][];

      for (int layer = 0; layer < scriptObjectRotations.length; layer++) {
         String saveStringID = layer == 0 ? "rotations" : ObjectLayerRegistry.getLayerStringID(layer) + "Rotations";
         if (save.hasLoadDataByName(saveStringID)) {
            scriptObjectRotations[layer] = save.getByteArray(saveStringID);
         }
      }

      this.clearOtherWires = save.getBoolean("clearOtherWires", false, false);
      byte[] scriptWireData = null;
      if (save.hasLoadDataByName("wire")) {
         scriptWireData = save.getByteArray("wire");
      }

      for (int i = Math.max(x, 0); i < x + scriptWidth; i++) {
         if (i < this.width) {
            for (int j = Math.max(y, 0); j < y + scriptHeight; j++) {
               if (j < this.height) {
                  if (scriptTiles != null) {
                     this.setTile(i, j, getInt(scriptTiles, scriptWidth, i - x, j - y));
                  }

                  for (int layer = 0; layer < scriptObjects.length; layer++) {
                     int[] layerObjects = scriptObjects[layer];
                     if (layerObjects != null && layerObjects.length != 0) {
                        this.setObjectLayer(layer, i, j, getInt(layerObjects, scriptWidth, i - x, j - y));
                     }
                  }

                  for (int layer = 0; layer < scriptObjectRotations.length; layer++) {
                     byte[] layerObjectRotations = scriptObjectRotations[layer];
                     if (layerObjectRotations != null && layerObjectRotations.length != 0) {
                        this.setRotationLayer(layer, i, j, getByte(layerObjectRotations, scriptWidth, i - x, j - y));
                     }
                  }

                  if (scriptWireData != null) {
                     this.setWireData(i, j, getByte(scriptWireData, scriptWidth, i - x, j - y));
                  }
               }
            }
         }
      }

      LoadData logicGates = save.getFirstLoadDataByName("logicGates");
      if (logicGates != null && logicGates.isArray()) {
         for (LoadData data : logicGates.getLoadDataByName("gate")) {
            try {
               int tileX = data.getInt("tileX");
               int tileY = data.getInt("tileY");
               String gateStringID = data.getUnsafeString("stringID");
               int logicGateID = LogicGateRegistry.getLogicGateID(gateStringID);
               if (logicGateID != -1) {
                  LoadData gateData = data.getFirstLoadDataByName("data");
                  boolean mirrorX = data.getBoolean("mirrorX", false, false);
                  boolean mirrorY = data.getBoolean("mirrorY", false, false);
                  int rotationOffset = data.getInt("rotation", 0, false);
                  this.logicGates
                     .put(
                        new Point(tileX, tileY),
                        new Preset.PresetLogicGate(
                           logicGateID, gateData == null ? null : gateData.toSaveData(), mirrorX, mirrorY, PresetRotation.toRotationAngle(rotationOffset)
                        )
                     );
               }
            } catch (Exception var22) {
            }
         }
      }

      LoadData objectEntities = save.getFirstLoadDataByName("objectEntities");
      if (objectEntities != null && objectEntities.isArray()) {
         for (LoadData data : objectEntities.getLoadDataByName("entity")) {
            try {
               int tileX = data.getInt("tileX");
               int tileY = data.getInt("tileY");
               LoadData gateData = data.getFirstLoadDataByName("data");
               boolean mirrorX = data.getBoolean("mirrorX", false, false);
               boolean mirrorY = data.getBoolean("mirrorY", false, false);
               int rotationOffset = data.getInt("rotation", 0, false);
               this.objectEntities
                  .put(
                     new Point(tileX, tileY),
                     new Preset.PresetObjectEntity(
                        gateData == null ? null : gateData.toSaveData(), mirrorX, mirrorY, PresetRotation.toRotationAngle(rotationOffset)
                     )
                  );
            } catch (Exception var21) {
            }
         }
      }
   }

   public SaveData getSaveData(String saveName) {
      return this.getSaveData(saveName, new PresetCopyFilter());
   }

   public SaveData getSaveData() {
      return this.getSaveData("PRESET");
   }

   public SaveData getSaveData(String saveName, PresetCopyFilter filter) {
      SaveData save = new SaveData(saveName);
      save.addInt("width", this.width);
      save.addInt("height", this.height);
      if (filter.acceptTiles) {
         HashSet<Integer> tilesUsed = new HashSet<>();

         for (int tile : this.tiles) {
            if (tile >= 0) {
               tilesUsed.add(tile);
            }
         }

         if (!tilesUsed.isEmpty()) {
            String[] tileIDs = new String[tilesUsed.size() * 2];
            int i = 0;

            for (int tile : tilesUsed) {
               tileIDs[i++] = Integer.toString(tile);
               tileIDs[i++] = TileRegistry.getTile(tile).getStringID();
            }

            save.addStringArray("tileIDs", tileIDs);
         }

         save.addIntArray("tiles", this.tiles);
      }

      if (filter.acceptObjects) {
         HashSet<Integer> objectsUsed = new HashSet<>();
         boolean addAir = false;
         boolean[] layerHasData = new boolean[this.objects.length];
         boolean[] layerHasObjects = new boolean[this.objects.length];

         for (int layer = 0; layer < this.objects.length; layer++) {
            for (int object : this.objects[layer]) {
               if (object >= 0) {
                  objectsUsed.add(object);
                  addAir = true;
                  layerHasData[layer] = true;
                  if (object > 0) {
                     layerHasObjects[layer] = true;
                  }
               }
            }
         }

         if (addAir) {
            objectsUsed.add(0);
         }

         if (!objectsUsed.isEmpty()) {
            String[] objectIDs = new String[objectsUsed.size() * 2];
            int i = 0;

            for (int object : objectsUsed) {
               objectIDs[i++] = Integer.toString(object);
               objectIDs[i++] = ObjectRegistry.getObject(object).getStringID();
            }

            save.addStringArray("objectIDs", objectIDs);
         }

         save.addIntArray("objects", this.objects[0]);
         save.addByteArray("rotations", this.objectRotations[0]);

         for (int layer = 1; layer < this.objects.length; layer++) {
            if (layerHasData[layer]) {
               if (layerHasObjects[layer]) {
                  save.addIntArray(ObjectLayerRegistry.getLayerStringID(layer) + "Objects", this.objects[layer]);
                  save.addByteArray(ObjectLayerRegistry.getLayerStringID(layer) + "Rotations", this.objectRotations[layer]);
               } else {
                  save.addBoolean(ObjectLayerRegistry.getLayerStringID(layer) + "ObjectsClear", true);
               }
            }
         }
      }

      if (filter.acceptWires) {
         save.addBoolean("clearOtherWires", this.clearOtherWires);
         if (this.clearOtherWires || this.hasAnyWireData()) {
            save.addByteArray("wire", this.wires);
         }

         if (!this.logicGates.isEmpty()) {
            SaveData logicGatesData = new SaveData("logicGates");
            this.logicGates.forEach((point, gate) -> {
               SaveData data = new SaveData("gate");
               data.addInt("tileX", point.x);
               data.addInt("tileY", point.y);
               if (gate.mirrorX) {
                  data.addBoolean("mirrorX", true);
               }

               if (gate.mirrorY) {
                  data.addBoolean("mirrorY", true);
               }

               if (gate.rotation != null) {
                  data.addInt("rotation", gate.rotation.dirOffset);
               }

               data.addUnsafeString("stringID", LogicGateRegistry.getLogicGateStringID(gate.logicGateID));
               if (gate.data != null) {
                  data.addSaveData(gate.data);
               }

               logicGatesData.addSaveData(data);
            });
            save.addSaveData(logicGatesData);
         }
      }

      if (filter.acceptObjects && filter.acceptObjectEntities && !this.objectEntities.isEmpty()) {
         SaveData objectEntityData = new SaveData("objectEntities");
         this.objectEntities.forEach((point, objectEntity) -> {
            SaveData data = new SaveData("entity");
            data.addInt("tileX", point.x);
            data.addInt("tileY", point.y);
            if (objectEntity.mirrorX) {
               data.addBoolean("mirrorX", true);
            }

            if (objectEntity.mirrorY) {
               data.addBoolean("mirrorY", true);
            }

            if (objectEntity.rotation != null) {
               data.addInt("rotation", objectEntity.rotation.dirOffset);
            }

            if (objectEntity.data != null) {
               data.addSaveData(objectEntity.data);
            }

            objectEntityData.addSaveData(data);
         });
         save.addSaveData(objectEntityData);
      }

      return save;
   }

   public SaveData getSaveData(PresetCopyFilter filter) {
      return this.getSaveData("PRESET", filter);
   }

   public String getScript() {
      return this.getSaveData().getScript();
   }

   public String getCompressedBase64Script() {
      SaveData saveData = this.getSaveData();

      try {
         return GameUtils.toBase64(GameUtils.compressData(GameUtils.getStringBytes(saveData.getScript(true))));
      } catch (Exception e) {
         return saveData.getScript(false);
      }
   }

   public void copyFromLevel(Level level, int x, int y) {
      this.copyFromLevel(level, x, y, new PresetCopyFilter());
   }

   public void copyFromLevel(Level level, int x, int y, PresetCopyFilter filter) {
      this.copyFromLevel(level, x, y, 0, 0, this.width, this.height, filter);
   }

   public void copyFromLevel(
      Level level, int levelTileX, int levelTileY, int presetTileX, int presetTileY, int tileWidth, int tileHeight, PresetCopyFilter filter
   ) {
      for (int tileX = 0; tileX < tileWidth; tileX++) {
         int currentPresetTileX = presetTileX + tileX;
         if (currentPresetTileX >= 0 && currentPresetTileX < this.width) {
            int currentLevelTileX = levelTileX + tileX;

            for (int tileY = 0; tileY < tileHeight; tileY++) {
               int currentPresetTileY = presetTileY + tileY;
               if (currentPresetTileY >= 0 && currentPresetTileY < this.height) {
                  int currentLevelTileY = levelTileY + tileY;
                  if (filter.acceptTiles) {
                     this.setTile(currentPresetTileX, currentPresetTileY, level.getTileID(currentLevelTileX, currentLevelTileY));
                  }

                  if (filter.acceptObjects) {
                     for (int layer = 0; layer < ObjectLayerRegistry.getTotalLayers(); layer++) {
                        this.setObjectLayer(layer, currentPresetTileX, currentPresetTileY, level.getObjectID(layer, currentLevelTileX, currentLevelTileY));
                        this.setRotationLayer(
                           layer, currentPresetTileX, currentPresetTileY, level.getObjectRotation(layer, currentLevelTileX, currentLevelTileY)
                        );
                     }

                     if (filter.acceptObjectEntities) {
                        this.setObjectEntity(currentPresetTileX, currentPresetTileY, level.entityManager.getObjectEntity(currentLevelTileX, currentLevelTileY));
                     }
                  }

                  if (filter.acceptWires) {
                     for (int wireID = 0; wireID < 4; wireID++) {
                        this.putWire(currentPresetTileX, currentPresetTileY, wireID, level.wireManager.hasWire(currentLevelTileX, currentLevelTileY, wireID));
                     }

                     if (level.logicLayer.hasGate(currentLevelTileX, currentLevelTileY)) {
                        this.setLogicGate(currentPresetTileX, currentPresetTileY, level.logicLayer.getEntity(currentLevelTileX, currentLevelTileY));
                     } else {
                        this.setLogicGate(currentPresetTileX, currentPresetTileY, null);
                     }
                  }
               }
            }
         }
      }
   }

   public static void setInt(int[] buffer, int width, int x, int y, int value) {
      if (x + y * width < buffer.length) {
         buffer[x + y * width] = value;
      }
   }

   public static int getInt(int[] buffer, int width, int x, int y) {
      return x + y * width >= buffer.length ? -1 : buffer[x + y * width];
   }

   public static void setByte(byte[] buffer, int width, int x, int y, byte value) {
      if (x + y * width < buffer.length) {
         buffer[x + y * width] = value;
      }
   }

   public static byte getByte(byte[] buffer, int width, int x, int y) {
      return x + y * width >= buffer.length ? -1 : buffer[x + y * width];
   }

   public static void setBoolean(boolean[] buffer, int width, int x, int y, boolean value) {
      if (x + y * width < buffer.length) {
         buffer[x + y * width] = value;
      }
   }

   public static boolean getBoolean(boolean[] buffer, int width, int x, int y) {
      return x + y * width >= buffer.length ? false : buffer[x + y * width];
   }

   public void setTile(int x, int y, int tile) {
      setInt(this.tiles, this.width, x, y, tile);
   }

   public void setObjectLayer(int layerID, int x, int y, int object, int rotation) {
      setInt(this.objects[layerID], this.width, x, y, object);
      setByte(this.objectRotations[layerID], this.width, x, y, (byte)rotation);
   }

   public void setObjectLayer(int layerID, int x, int y, int object) {
      this.setObjectLayer(layerID, x, y, object, 0);
   }

   public void setObject(int x, int y, int object, int rotation) {
      this.setObjectLayer(0, x, y, object, rotation);
   }

   public void setObject(int x, int y, int object) {
      this.setObject(x, y, object, 0);
   }

   public void setRotationLayer(int layerID, int x, int y, int rotation) {
      setByte(this.objectRotations[layerID], this.width, x, y, (byte)rotation);
   }

   public void setRotation(int x, int y, int rotation) {
      this.setRotationLayer(0, x, y, rotation);
   }

   public void setWireData(int x, int y, byte wireData) {
      setByte(this.wires, this.width, x, y, wireData);
      this.hasAnyWireData = null;
   }

   public void putWire(int x, int y, int wireID, boolean isThere) {
      byte wireData = this.getWireData(x, y);
      wireData = GameMath.setBit(wireData, wireID * 2, true);
      wireData = GameMath.setBit(wireData, wireID * 2 + 1, isThere);
      this.setWireData(x, y, wireData);
   }

   public void clearWire(int x, int y, int wireID) {
      this.setWireData(x, y, GameMath.setBit(this.getWireData(x, y), wireID * 2, false));
   }

   public boolean hasWire(int x, int y, int wireID) {
      byte wireData = this.getWireData(x, y);
      return GameMath.getBit(wireData, wireID * 2) && GameMath.getBit(wireData, wireID * 2 + 1);
   }

   public boolean doesSetWire(int x, int y, int wireID) {
      return GameMath.getBit(this.getWireData(x, y), wireID * 2);
   }

   public boolean hasAnyWireData() {
      if (this.hasAnyWireData == null) {
         this.hasAnyWireData = false;

         for (byte wireData : this.wires) {
            for (int wireID = 0; wireID < 4; wireID++) {
               if (GameMath.getBit(wireData, wireID * 2 + 1)) {
                  this.hasAnyWireData = true;
                  break;
               }
            }

            if (this.hasAnyWireData) {
               break;
            }
         }
      }

      return this.hasAnyWireData;
   }

   public int getTile(int x, int y) {
      return getInt(this.tiles, this.width, x, y);
   }

   public int getObject(int layerID, int x, int y) {
      return getInt(this.objects[layerID], this.width, x, y);
   }

   public int getObject(int x, int y) {
      return this.getObject(0, x, y);
   }

   public byte getObjectRotation(int layerID, int x, int y) {
      return getByte(this.objectRotations[layerID], this.width, x, y);
   }

   public byte getObjectRotation(int x, int y) {
      return this.getObjectRotation(0, x, y);
   }

   public byte getWireData(int x, int y) {
      return getByte(this.wires, this.width, x, y);
   }

   public void fillTile(int x, int y, int width, int height, int tile) {
      for (int i = x; i < width + x; i++) {
         for (int j = y; j < height + y; j++) {
            this.setTile(i, j, tile);
         }
      }
   }

   public void fillObject(int x, int y, int width, int height, int object, int rotation) {
      for (int i = x; i < width + x; i++) {
         for (int j = y; j < height + y; j++) {
            this.setObject(i, j, object, rotation);
         }
      }
   }

   public void fillObject(int x, int y, int width, int height, int object) {
      this.fillObject(x, y, width, height, object, 0);
   }

   public void boxObject(int x, int y, int width, int height, int object, int rotation) {
      this.fillObject(x, y, width, 1, object, rotation);
      this.fillObject(x, y, 1, height, object, rotation);
      this.fillObject(x, y + height - 1, width, 1, object, rotation);
      this.fillObject(x + width - 1, y, 1, height, object, rotation);
   }

   public void boxObject(int x, int y, int width, int height, int object) {
      this.boxObject(x, y, width, height, object, 0);
   }

   public void boxTile(int x, int y, int width, int height, int tile) {
      this.fillTile(x, y, width, 1, tile);
      this.fillTile(x, y, 1, height, tile);
      this.fillTile(x, y + height - 1, width, 1, tile);
      this.fillTile(x + width - 1, y, 1, height, tile);
   }

   public void replaceTile(String oldTileID, String newTileID) {
      this.replaceTile(TileRegistry.getTileID(oldTileID), TileRegistry.getTileID(newTileID));
   }

   public void replaceNonEmptyTiles(int oldTile, int newTile) {
      if (oldTile > 0 && newTile >= 0) {
         this.replaceTile(oldTile, newTile);
      }
   }

   public Preset randomlyReplaceObjects(int oldObjectID, Supplier<Integer> getRandomObjectID) {
      return this.randomlyReplaceObjects(0, oldObjectID, getRandomObjectID);
   }

   public Preset randomlyReplaceObjects(int objectLayer, int oldObjectID, Supplier<Integer> getRandomObjectID) {
      if (oldObjectID != -1) {
         for (int i = 0; i < this.objects[objectLayer].length; i++) {
            int newObject = getRandomObjectID.get();
            if (newObject != -1 && this.objects[objectLayer][i] == oldObjectID) {
               this.objects[objectLayer][i] = newObject;
            }
         }
      }

      return this;
   }

   public void replaceTile(int oldTile, int newTile) {
      for (int i = 0; i < this.tiles.length; i++) {
         if (this.tiles[i] == oldTile) {
            this.tiles[i] = newTile;
         }
      }
   }

   public void replaceObjectLayer(int layerID, int oldObject, int newObject, int newRotation) {
      for (int i = 0; i < this.objects[layerID].length; i++) {
         if (this.objects[layerID][i] == oldObject) {
            this.objects[layerID][i] = newObject;
            if (newRotation != -1) {
               this.objectRotations[layerID][i] = (byte)newRotation;
            }
         }
      }
   }

   public void replaceObjectAll(int oldObject, int newObject, int newRotation) {
      for (int layer = 0; layer < this.objects.length; layer++) {
         this.replaceObjectLayer(layer, oldObject, newObject, newRotation);
      }
   }

   public void replaceObject(int objectLayerID, int oldObject, int newObject, int newRotation) {
      this.replaceObjectLayer(objectLayerID, oldObject, newObject, newRotation);
   }

   public void replaceObject(int oldObject, int newObject, int newRotation) {
      this.replaceObjectLayer(0, oldObject, newObject, newRotation);
   }

   public void replaceObject(String oldObjectID, String newObjectID) {
      this.replaceObject(ObjectRegistry.getObjectID(oldObjectID), ObjectRegistry.getObjectID(newObjectID), -1);
   }

   public void replaceObject(int oldObject, int newObject) {
      this.replaceObject(oldObject, newObject, -1);
   }

   public void replaceNonEmptyObjects(int oldObject, int newObject) {
      if (oldObject > 0 && newObject >= 0) {
         this.replaceObject(oldObject, newObject, -1);
      }
   }

   public void replaceNonEmptyObjects(int objectLayerID, int oldObject, int newObject) {
      if (oldObject > 0 && newObject >= 0) {
         this.replaceObject(objectLayerID, oldObject, newObject, -1);
      }
   }

   public void iteratePreset(BiConsumer<Integer, Integer> consumer) {
      for (int x = 0; x < this.width; x++) {
         for (int y = 0; y < this.height; y++) {
            consumer.accept(x, y);
         }
      }
   }

   public void iteratePreset(BiFunction<Integer, Integer, Boolean> filter, BiConsumer<Integer, Integer> consumer) {
      this.iteratePreset((x, y) -> {
         if (filter.apply(x, y)) {
            consumer.accept(x, y);
         }
      });
   }

   public void setLogicGate(int tileX, int tileY, LogicGateEntity entity) {
      if (tileX >= 0 && tileY >= 0 && tileX < this.width && tileY < this.height) {
         if (entity == null) {
            this.logicGates.remove(new Point(tileX, tileY));
         } else {
            SaveData data = new SaveData("data");
            entity.addPresetSaveData(data);
            this.logicGates.put(new Point(tileX, tileY), new Preset.PresetLogicGate(entity.logicGateID, data, false, false, null));
         }
      } else {
         throw new IllegalArgumentException("Cannot add logic gate outside preset bounds");
      }
   }

   public void setObjectEntity(int tileX, int tileY, ObjectEntity entity) {
      if (tileX >= 0 && tileY >= 0 && tileX < this.width && tileY < this.height) {
         if (entity == null) {
            this.objectEntities.remove(new Point(tileX, tileY));
         } else {
            SaveData data = new SaveData("data");
            entity.addPresetSaveData(data);
            if (!data.isEmpty()) {
               this.objectEntities.put(new Point(tileX, tileY), new Preset.PresetObjectEntity(data, false, false, null));
            }
         }
      } else {
         throw new IllegalArgumentException("Cannot add object entity outside preset bounds");
      }
   }

   public void onTileApply(Preset.TileApplyListener listener) {
      Objects.requireNonNull(listener);
      this.tileApplyListeners.add(listener);
   }

   public void onObjectApply(Preset.ObjectApplyListener listener) {
      Objects.requireNonNull(listener);
      this.objectApplyListeners.add(listener);
   }

   public void onWireApply(Preset.WireApplyListener listener) {
      Objects.requireNonNull(listener);
      this.wireApplyListeners.add(listener);
   }

   public void addCanApplyPredicate(Preset.ApplyPredicate apply) {
      Objects.requireNonNull(apply);
      this.applyPredicates.add(apply);
   }

   public void addCanApplyPredicate(int tileX, int tileY, int dir, Preset.ApplyPredicateFunction apply, boolean limitWithinBounds) {
      Objects.requireNonNull(apply);
      this.addCanApplyPredicate(new Preset.ApplyTilePredicate(tileX, tileY, dir, apply, limitWithinBounds));
   }

   public void addCanApplyPredicate(int tileX, int tileY, int dir, Preset.ApplyPredicateFunction apply) {
      this.addCanApplyPredicate(tileX, tileY, dir, apply, false);
   }

   public void addCanApplyAreaPredicate(int startX, int startY, int endX, int endY, int dir, Preset.ApplyAreaPredicateFunction apply, boolean limitWithinBounds) {
      Objects.requireNonNull(apply);
      this.addCanApplyPredicate(new Preset.ApplyAreaPredicate(startX, startY, endX, endY, dir, apply, limitWithinBounds));
   }

   public void addCanApplyAreaPredicate(int startX, int startY, int endX, int endY, int dir, Preset.ApplyAreaPredicateFunction apply) {
      this.addCanApplyAreaPredicate(startX, startY, endX, endY, dir, apply, false);
   }

   public void addCanApplyRectPredicate(
      int tileX, int tileY, int width, int height, int dir, Preset.ApplyAreaPredicateFunction apply, boolean limitWithinBounds
   ) {
      this.addCanApplyAreaPredicate(tileX, tileY, tileX + width - 1, tileY + height - 1, dir, apply, limitWithinBounds);
   }

   public void addCanApplyRectPredicate(int tileX, int tileY, int width, int height, int dir, Preset.ApplyAreaPredicateFunction apply) {
      this.addCanApplyAreaPredicate(tileX, tileY, tileX + width - 1, tileY + height - 1, dir, apply);
   }

   public void addCanApplyAreaEachPredicate(int startX, int startY, int endX, int endY, int dir, Preset.ApplyPredicateFunction apply, boolean limitWithinBounds) {
      Objects.requireNonNull(apply);
      this.addCanApplyPredicate(new Preset.ApplyAreaPredicate(startX, startY, endX, endY, dir, apply, limitWithinBounds));
   }

   public void addCanApplyAreaEachPredicate(int startX, int startY, int endX, int endY, int dir, Preset.ApplyPredicateFunction apply) {
      this.addCanApplyAreaEachPredicate(startX, startY, endX, endY, dir, apply, false);
   }

   public void addCanApplyRectEachPredicate(
      int tileX, int tileY, int width, int height, int dir, Preset.ApplyPredicateFunction apply, boolean limitWithinBounds
   ) {
      this.addCanApplyAreaEachPredicate(tileX, tileY, tileX + width - 1, tileY + height - 1, dir, apply, limitWithinBounds);
   }

   public void addCanApplyRectEachPredicate(int tileX, int tileY, int width, int height, int dir, Preset.ApplyPredicateFunction apply) {
      this.addCanApplyAreaEachPredicate(tileX, tileY, tileX + width - 1, tileY + height - 1, dir, apply);
   }

   public void addCustomPreApply(Preset.CustomApply apply) {
      Objects.requireNonNull(apply);
      this.customPreApplies.add(apply);
   }

   public void addCustomPreApply(int tileX, int tileY, int dir, Preset.CustomApplyFunction apply, boolean limitWithinBounds) {
      Objects.requireNonNull(apply);
      this.addCustomPreApply(new Preset.CustomApplyTile(tileX, tileY, dir, apply, limitWithinBounds));
   }

   public void addCustomPreApply(int tileX, int tileY, int dir, Preset.CustomApplyFunction apply) {
      this.addCustomPreApply(tileX, tileY, dir, apply, true);
   }

   public void addCustomPreApplyArea(int startX, int startY, int endX, int endY, int dir, Preset.CustomApplyAreaFunction apply, boolean limitWithinBounds) {
      Objects.requireNonNull(apply);
      this.addCustomPreApply(new Preset.CustomApplyArea(startX, startY, endX, endY, dir, apply, limitWithinBounds));
   }

   public void addCustomPreApplyArea(int startX, int startY, int endX, int endY, int dir, Preset.CustomApplyAreaFunction apply) {
      this.addCustomPreApplyArea(startX, startY, endX, endY, dir, apply, true);
   }

   public void addCustomPreApplyRect(int tileX, int tileY, int width, int height, int dir, Preset.CustomApplyAreaFunction apply, boolean limitWithinBounds) {
      this.addCustomPreApplyArea(tileX, tileY, tileX + width - 1, tileY + height - 1, dir, apply, limitWithinBounds);
   }

   public void addCustomPreApplyRect(int tileX, int tileY, int width, int height, int dir, Preset.CustomApplyAreaFunction apply) {
      this.addCustomPreApplyArea(tileX, tileY, tileX + width - 1, tileY + height - 1, dir, apply);
   }

   public void addCustomPreApplyAreaEach(int startX, int startY, int endX, int endY, int dir, Preset.CustomApplyFunction apply, boolean limitWithinBounds) {
      Objects.requireNonNull(apply);
      this.addCustomPreApply(new Preset.CustomApplyArea(startX, startY, endX, endY, dir, apply, limitWithinBounds));
   }

   public void addCustomPreApplyAreaEach(int startX, int startY, int endX, int endY, int dir, Preset.CustomApplyFunction apply) {
      this.addCustomPreApplyAreaEach(startX, startY, endX, endY, dir, apply, true);
   }

   public void addCustomPreApplyRectEach(int tileX, int tileY, int width, int height, int dir, Preset.CustomApplyFunction apply, boolean limitWithinBounds) {
      this.addCustomPreApplyAreaEach(tileX, tileY, tileX + width - 1, tileY + height - 1, dir, apply, limitWithinBounds);
   }

   public void addCustomPreApplyRectEach(int tileX, int tileY, int width, int height, int dir, Preset.CustomApplyFunction apply) {
      this.addCustomPreApplyAreaEach(tileX, tileY, tileX + width - 1, tileY + height - 1, dir, apply);
   }

   public void setupBlackboard(Consumer<GameBlackboard> blackboardConsumer) {
      Objects.requireNonNull(blackboardConsumer);
      this.blackboardSetups.add(blackboardConsumer);
   }

   public void addCustomApply(Preset.CustomApply apply) {
      Objects.requireNonNull(apply);
      this.customApplies.add(apply);
   }

   public void addCustomApply(int tileX, int tileY, int dir, Preset.CustomApplyFunction apply, boolean limitWithinBounds) {
      Objects.requireNonNull(apply);
      this.addCustomApply(new Preset.CustomApplyTile(tileX, tileY, dir, apply, limitWithinBounds));
   }

   public void addCustomApply(int tileX, int tileY, int dir, Preset.CustomApplyFunction apply) {
      this.addCustomApply(tileX, tileY, dir, apply, true);
   }

   public void addCustomApplyArea(int startX, int startY, int endX, int endY, int dir, Preset.CustomApplyAreaFunction apply, boolean limitWithinBounds) {
      Objects.requireNonNull(apply);
      this.addCustomApply(new Preset.CustomApplyArea(startX, startY, endX, endY, dir, apply, limitWithinBounds));
   }

   public void addCustomApplyArea(int startX, int startY, int endX, int endY, int dir, Preset.CustomApplyAreaFunction apply) {
      this.addCustomApplyArea(startX, startY, endX, endY, dir, apply, true);
   }

   public void addCustomApplyRect(int tileX, int tileY, int width, int height, int dir, Preset.CustomApplyAreaFunction apply, boolean limitWithinBounds) {
      this.addCustomApplyArea(tileX, tileY, tileX + width - 1, tileY + height - 1, dir, apply, limitWithinBounds);
   }

   public void addCustomApplyRect(int tileX, int tileY, int width, int height, int dir, Preset.CustomApplyAreaFunction apply) {
      this.addCustomApplyArea(tileX, tileY, tileX + width - 1, tileY + height - 1, dir, apply);
   }

   public void addCustomApplyAreaEach(int startX, int startY, int endX, int endY, int dir, Preset.CustomApplyFunction apply, boolean limitWithinBounds) {
      Objects.requireNonNull(apply);
      this.addCustomApply(new Preset.CustomApplyArea(startX, startY, endX, endY, dir, apply, limitWithinBounds));
   }

   public void addCustomApplyAreaEach(int startX, int startY, int endX, int endY, int dir, Preset.CustomApplyFunction apply) {
      this.addCustomApplyAreaEach(startX, startY, endX, endY, dir, apply, true);
   }

   public void addCustomApplyRectEach(int tileX, int tileY, int width, int height, int dir, Preset.CustomApplyFunction apply, boolean limitWithinBounds) {
      this.addCustomApplyAreaEach(tileX, tileY, tileX + width - 1, tileY + height - 1, dir, apply, limitWithinBounds);
   }

   public void addCustomApplyRectEach(int tileX, int tileY, int width, int height, int dir, Preset.CustomApplyFunction apply) {
      this.addCustomApplyAreaEach(tileX, tileY, tileX + width - 1, tileY + height - 1, dir, apply);
   }

   public void addMobs(int tileX, int tileY, Consumer<Mob> onAdded, String... mobIDs) {
      for (String mobID : mobIDs) {
         this.addMob(mobID, tileX, tileY, onAdded, false);
      }
   }

   protected void chanceAddMobs(float chancePercent, int tileX, int tileY, String... mobIDs) {
      if (!(GameRandom.globalRandom.nextFloat() > chancePercent)) {
         this.addMobs(tileX, tileY, false, mobIDs);
      }
   }

   public void addMobs(int tileX, int tileY, boolean canDespawn, String... mobIDs) {
      for (String mobID : mobIDs) {
         this.addMob(mobID, tileX, tileY, canDespawn, false);
      }
   }

   public <T extends Mob> void addMob(Function<Level, T> mobSupplier, int tileX, int tileY, Consumer<T> onAdded, boolean limitWithinBounds) {
      this.addCustomApply(tileX, tileY, 0, (level, levelX, levelY, dir, blackboard) -> {
         if (!level.isClient()) {
            T mob = mobSupplier.apply(level);
            if (mob != null) {
               mob.onSpawned(levelX * 32 + 16, levelY * 32 + 16);
               level.entityManager.addMob(mob, levelX * 32 + 16, levelY * 32 + 16);
               onAdded.accept(mob);
               return (level1, presetX, presetY) -> mob.remove();
            }
         }

         return null;
      }, limitWithinBounds);
   }

   public <T extends Mob> void addMob(Function<Level, T> mobSupplier, int tileX, int tileY, Consumer<T> onAdded) {
      this.addMob(mobSupplier, tileX, tileY, onAdded, false);
   }

   public <T extends Mob> void addMob(String mobStringID, int tileX, int tileY, Class<T> mobClass, Consumer<T> onAdded, boolean limitWithinBounds) {
      this.addMob(level -> mobClass.cast(MobRegistry.getMob(mobStringID, level)), tileX, tileY, onAdded, limitWithinBounds);
   }

   public <T extends Mob> void addMob(String mobStringID, int tileX, int tileY, Class<T> mobClass, Consumer<T> onAdded) {
      this.addMob(level -> mobClass.cast(MobRegistry.getMob(mobStringID, level)), tileX, tileY, onAdded);
   }

   public void addMob(String mobStringID, int tileX, int tileY, Consumer<Mob> onAdded, boolean limitWithinBounds) {
      this.addMob(mobStringID, tileX, tileY, Mob.class, onAdded, limitWithinBounds);
   }

   public void addMob(String mobStringID, int tileX, int tileY, Consumer<Mob> onAdded) {
      this.addMob(mobStringID, tileX, tileY, Mob.class, onAdded);
   }

   public void addMob(String mobStringID, int tileX, int tileY, boolean canDespawn, boolean limitWithinBounds) {
      this.addMob(mobStringID, tileX, tileY, mob -> mob.canDespawn = canDespawn, limitWithinBounds);
   }

   public void addMob(String mobStringID, int tileX, int tileY, boolean canDespawn) {
      this.addMob(mobStringID, tileX, tileY, mob -> mob.canDespawn = canDespawn);
   }

   public String getRandomHostileMobNameForBiomeLevelExcept(Biome biome, LevelIdentifier levelIdentifier, GameRandom seededRandom, String... exceptions) {
      String[] mobStringIDsForBiomeLevel = JournalRegistry.getHostileMobStringIDsForBiomeLevel(biome, levelIdentifier);
      if (mobStringIDsForBiomeLevel != null) {
         String[] streamResult = Arrays.stream(mobStringIDsForBiomeLevel)
            .filter(stringID -> Arrays.stream(exceptions).noneMatch(stringID::contains))
            .toArray(String[]::new);
         if (streamResult.length == 0) {
            return "skeleton";
         }

         String chanceResult = seededRandom.getOneOf(streamResult);
         return chanceResult == null ? "skeleton" : chanceResult;
      } else {
         return "skeleton";
      }
   }

   public String getRandomHostileMobNameForBiomeLevelFromGiven(Biome biome, LevelIdentifier levelIdentifier, GameRandom seededRandom, String... choices) {
      String[] mobStringIDsForBiomeLevel = JournalRegistry.getHostileMobStringIDsForBiomeLevel(biome, levelIdentifier);
      if (mobStringIDsForBiomeLevel != null) {
         String[] streamResult = Arrays.stream(mobStringIDsForBiomeLevel)
            .filter(stringID -> Arrays.stream(choices).anyMatch(stringID::contains))
            .toArray(String[]::new);
         if (streamResult.length == 0) {
            return "skeleton";
         }

         String chanceResult = seededRandom.getOneOf(streamResult);
         return chanceResult == null ? "skeleton" : chanceResult;
      } else {
         return "skeleton";
      }
   }

   public void addRandomHostileMobForBiomeLevel(
      Biome biome, LevelIdentifier levelIdentifier, int tileX, int tileY, boolean canDespawn, GameRandom seededRandom, String[] exceptions
   ) {
      String mobStringIDsForBiomeLevel = this.getRandomHostileMobNameForBiomeLevelExcept(biome, levelIdentifier, seededRandom, exceptions);
      this.addMob(mobStringIDsForBiomeLevel, tileX, tileY, canDespawn);
   }

   public void addHuman(String mobStringID, int homeX, int homeY, Consumer<HumanMob> onAdded, GameRandom random) {
      this.addCustomApply(homeX, homeY, 0, (level, levelX, levelY, dir, blackboard) -> {
         if (!level.isClient()) {
            Mob mob = MobRegistry.getMob(mobStringID, level);
            if (!(mob instanceof HumanMob)) {
               GameLog.err.println("Tried spawning non human or null mob with addHuman() in preset. MobStringID: " + mobStringID);
               return null;
            } else {
               HumanMob humanMob = (HumanMob)mob;
               Point spawnLocation = GameUtils.getValidMobLocationAroundObject(random, level, mob, levelX, levelY, true);
               mob.onSpawned(spawnLocation.x, spawnLocation.y);
               level.entityManager.addMob(humanMob, spawnLocation.x, spawnLocation.y);
               humanMob.setHome(levelX, levelY);
               humanMob.canDespawn = false;
               onAdded.accept(humanMob);
               return (level1, presetX, presetY) -> humanMob.remove();
            }
         } else {
            return null;
         }
      });
   }

   public void addInventory(LootTable lootTable, GameRandom random, int tileX, int tileY, Object... extra) {
      this.addCustomApply(
         tileX,
         tileY,
         0,
         (level, levelX, levelY, dir, blackboard) -> {
            LevelObject masterLevelObject = level.getLevelObject(levelX, levelY).getMasterLevelObject().orElse(null);
            if (masterLevelObject != null) {
               levelX = masterLevelObject.tileX;
               levelY = masterLevelObject.tileY;
            }

            try {
               ObjectEntity objEnt = level.entityManager.getObjectEntity(levelX, levelY);
               if (objEnt == null || !objEnt.implementsOEInventory()) {
                  throw new NullPointerException(
                     "Could not find an objectEntity ("
                        + objEnt
                        + ") with inventory for loot table at [X"
                        + levelX
                        + ", Y"
                        + levelY
                        + "] in preset "
                        + this.getClass()
                  );
               }
            } catch (Exception e) {
               System.err.println(e.getMessage());
               return null;
            }

            lootTable.applyToLevel(
               random, level.buffManager.getModifier(LevelModifiers.LOOT), level, levelX, levelY, GameUtils.concat(new Object[]{level}, extra)
            );
            return null;
         },
         true
      );
   }

   public void addSign(Supplier<String> text, int x, int y, int object, int objectRotation) {
      this.setObject(x, y, object, objectRotation);
      this.addCustomApply(x, y, 0, (level, levelX, levelY, dir, blackboard) -> {
         try {
            ObjectEntity objEnt = level.entityManager.getObjectEntity(levelX, levelY);
            if (objEnt instanceof SignObjectEntity) {
               ((SignObjectEntity)objEnt).setMessage(text.get());
            } else if (level.isServer()) {
               throw new NullPointerException("Could not find a sign objectEntity for preset " + this.getClass() + " at " + levelX + ", " + levelY);
            }
         } catch (Exception e) {
            System.err.println(e.getMessage());
         }

         return null;
      }, true);
   }

   public void addSign(Supplier<String> text, int x, int y, int objectRotation) {
      this.setObject(x, y, ObjectRegistry.getObjectID("sign"), objectRotation);
      this.addCustomApply(x, y, 0, (level, levelX, levelY, dir, blackboard) -> {
         try {
            ObjectEntity objEnt = level.entityManager.getObjectEntity(levelX, levelY);
            if (objEnt instanceof SignObjectEntity) {
               ((SignObjectEntity)objEnt).setMessage(text.get());
            } else if (level.isServer()) {
               throw new NullPointerException("Could not find a sign objectEntity for preset " + this.getClass() + " at " + levelX + ", " + levelY);
            }
         } catch (Exception e) {
            System.err.println(e.getMessage());
         }

         return null;
      }, true);
   }

   public void addSign(String text, int x, int y) {
      this.addSign(() -> text, x, y, this.getObjectRotation(x, y));
   }

   public void addSign(Supplier<String> text, int x, int y) {
      this.addSign(text, x, y, this.getObjectRotation(x, y));
   }

   public Point getMirroredPoint(int x, int y, boolean xMirror, boolean yMirror) {
      return PresetUtils.getMirroredPoint(x, y, xMirror, yMirror, this.width, this.height);
   }

   public int getMirroredX(int x) {
      return PresetUtils.getMirroredValue(x, this.width);
   }

   public int getMirroredY(int y) {
      return PresetUtils.getMirroredValue(y, this.height);
   }

   public static class ApplyAreaPredicate implements Preset.ApplyPredicate {
      public final int startX;
      public final int startY;
      public final int endX;
      public final int endY;
      public final int dir;
      public final Preset.ApplyAreaPredicateFunction applyFunction;
      public final boolean limitWithinBounds;

      public ApplyAreaPredicate(int startX, int startY, int endX, int endY, int dir, Preset.ApplyAreaPredicateFunction applyFunction, boolean limitWithinBounds) {
         Objects.requireNonNull(applyFunction);
         this.startX = startX;
         this.startY = startY;
         this.endX = endX;
         this.endY = endY;
         this.dir = dir;
         this.applyFunction = applyFunction;
         this.limitWithinBounds = limitWithinBounds;
      }

      public ApplyAreaPredicate(int startX, int startY, int endX, int endY, int dir, Preset.ApplyPredicateFunction applyFunction, boolean limitWithinBounds) {
         this(startX, startY, endX, endY, dir, (level, levelStartX, levelStartY, levelEndX, levelEndY, levelDir) -> {
            for (int x = levelStartX; x <= levelEndX; x++) {
               for (int y = levelStartY; y <= levelEndY; y++) {
                  if (!applyFunction.canApplyToLevel(level, x, y, levelDir)) {
                     return false;
                  }
               }
            }

            return true;
         }, limitWithinBounds);
         Objects.requireNonNull(applyFunction);
      }

      @Override
      public boolean canApplyToLevel(Level level, int presetX, int presetY) {
         int startX = presetX + Math.min(this.startX, this.endX);
         int endX = presetX + Math.max(this.startX, this.endX);
         int startY = presetY + Math.min(this.startY, this.endY);
         int endY = presetY + Math.max(this.startY, this.endY);
         return this.applyFunction.canApplyToLevel(level, startX, startY, endX, endY, this.dir);
      }

      @Override
      public Preset.ApplyPredicate copy(int xOffset, int yOffset, int maxWidth, int maxHeight) throws PresetCopyException {
         int newStartX = this.startX + xOffset;
         int newStartY = this.startY + yOffset;
         int newEndX = this.endX + xOffset;
         int newEndY = this.endY + yOffset;
         if (this.limitWithinBounds) {
            newStartX = Math.max(newStartX, 0);
            newStartY = Math.max(newStartY, 0);
            newEndX = Math.min(newEndX, maxWidth - 1);
            newEndY = Math.min(newEndY, maxHeight - 1);
            if (newStartX == newEndX) {
               return null;
            }

            if (newStartY == newEndY) {
               return null;
            }
         }

         return new Preset.ApplyAreaPredicate(newStartX, newStartY, newEndX, newEndY, this.dir, this.applyFunction, this.limitWithinBounds);
      }

      @Override
      public Preset.ApplyPredicate mirrorX(int width) {
         int newDir = this.dir != 1 && this.dir != 3 ? this.dir : (this.dir + 2) % 4;
         return new Preset.ApplyAreaPredicate(
            PresetUtils.getMirroredValue(this.startX, width),
            this.startY,
            PresetUtils.getMirroredValue(this.endX, width),
            this.endY,
            newDir,
            this.applyFunction,
            this.limitWithinBounds
         );
      }

      @Override
      public Preset.ApplyPredicate mirrorY(int height) {
         int newDir = this.dir != 0 && this.dir != 2 ? this.dir : (this.dir + 2) % 4;
         return new Preset.ApplyAreaPredicate(
            this.startX,
            PresetUtils.getMirroredValue(this.startY, height),
            this.endX,
            PresetUtils.getMirroredValue(this.endY, height),
            newDir,
            this.applyFunction,
            this.limitWithinBounds
         );
      }

      @Override
      public Preset.ApplyPredicate rotate(PresetRotation angle, int width, int height) {
         int newDir = (this.dir + (angle == null ? 0 : angle.dirOffset)) % 4;
         Point pos1 = PresetUtils.getRotatedPointInSpace(this.startX, this.startY, width, height, angle);
         Point pos2 = PresetUtils.getRotatedPointInSpace(this.endX, this.endY, width, height, angle);
         return new Preset.ApplyAreaPredicate(pos1.x, pos1.y, pos2.x, pos2.y, newDir, this.applyFunction, this.limitWithinBounds);
      }
   }

   @FunctionalInterface
   public interface ApplyAreaPredicateFunction {
      boolean canApplyToLevel(Level var1, int var2, int var3, int var4, int var5, int var6);
   }

   public interface ApplyPredicate {
      boolean canApplyToLevel(Level var1, int var2, int var3);

      Preset.ApplyPredicate copy(int var1, int var2, int var3, int var4) throws PresetCopyException;

      Preset.ApplyPredicate mirrorX(int var1) throws PresetMirrorException;

      Preset.ApplyPredicate mirrorY(int var1) throws PresetMirrorException;

      Preset.ApplyPredicate rotate(PresetRotation var1, int var2, int var3) throws PresetRotateException;
   }

   @FunctionalInterface
   public interface ApplyPredicateFunction {
      boolean canApplyToLevel(Level var1, int var2, int var3, int var4);
   }

   public static class ApplyTilePredicate implements Preset.ApplyPredicate {
      public final int tileX;
      public final int tileY;
      public final int dir;
      public final Preset.ApplyPredicateFunction applyFunction;
      public final boolean limitWithinBounds;

      public ApplyTilePredicate(int tileX, int tileY, int dir, Preset.ApplyPredicateFunction applyFunction, boolean limitWithinBounds) {
         Objects.requireNonNull(applyFunction);
         this.tileX = tileX;
         this.tileY = tileY;
         this.dir = dir;
         this.applyFunction = applyFunction;
         this.limitWithinBounds = limitWithinBounds;
      }

      @Override
      public boolean canApplyToLevel(Level level, int presetX, int presetY) {
         return this.applyFunction.canApplyToLevel(level, presetX + this.tileX, presetY + this.tileY, this.dir);
      }

      @Override
      public Preset.ApplyPredicate copy(int xOffset, int yOffset, int maxWidth, int maxHeight) throws PresetCopyException {
         int newX = this.tileX + xOffset;
         if (!this.limitWithinBounds || newX >= 0 && newX < maxWidth) {
            int newY = this.tileY + yOffset;
            return !this.limitWithinBounds || newY >= 0 && newY < maxHeight
               ? new Preset.ApplyTilePredicate(newX, newY, this.dir, this.applyFunction, this.limitWithinBounds)
               : null;
         } else {
            return null;
         }
      }

      @Override
      public Preset.ApplyPredicate mirrorX(int width) {
         int newDir = this.dir != 1 && this.dir != 3 ? this.dir : (this.dir + 2) % 4;
         return new Preset.ApplyTilePredicate(PresetUtils.getMirroredValue(this.tileX, width), this.tileY, newDir, this.applyFunction, this.limitWithinBounds);
      }

      @Override
      public Preset.ApplyPredicate mirrorY(int height) {
         int newDir = this.dir != 0 && this.dir != 2 ? this.dir : (this.dir + 2) % 4;
         return new Preset.ApplyTilePredicate(this.tileX, PresetUtils.getMirroredValue(this.tileY, height), newDir, this.applyFunction, this.limitWithinBounds);
      }

      @Override
      public Preset.ApplyPredicate rotate(PresetRotation angle, int width, int height) {
         int newDir = (this.dir + (angle == null ? 0 : angle.dirOffset)) % 4;
         Point pos = PresetUtils.getRotatedPointInSpace(this.tileX, this.tileY, width, height, angle);
         return new Preset.ApplyTilePredicate(pos.x, pos.y, newDir, this.applyFunction, this.limitWithinBounds);
      }
   }

   public interface CustomApply {
      Preset.UndoLogic applyToLevel(Level var1, int var2, int var3, GameBlackboard var4);

      Preset.CustomApply copy(int var1, int var2, int var3, int var4) throws PresetCopyException;

      Preset.CustomApply mirrorX(int var1) throws PresetMirrorException;

      Preset.CustomApply mirrorY(int var1) throws PresetMirrorException;

      Preset.CustomApply rotate(PresetRotation var1, int var2, int var3) throws PresetRotateException;
   }

   public static class CustomApplyArea implements Preset.CustomApply {
      public final int startX;
      public final int startY;
      public final int endX;
      public final int endY;
      public final int dir;
      public final Preset.CustomApplyAreaFunction applyFunction;
      public final boolean limitWithinBounds;

      public CustomApplyArea(int startX, int startY, int endX, int endY, int dir, Preset.CustomApplyAreaFunction applyFunction, boolean limitWithinBounds) {
         Objects.requireNonNull(applyFunction);
         this.startX = startX;
         this.startY = startY;
         this.endX = endX;
         this.endY = endY;
         this.dir = dir;
         this.applyFunction = applyFunction;
         this.limitWithinBounds = limitWithinBounds;
      }

      public CustomApplyArea(int startX, int startY, int endX, int endY, int dir, Preset.CustomApplyFunction applyFunction, boolean limitWithinBounds) {
         this(startX, startY, endX, endY, dir, (level, levelStartX, levelStartY, levelEndX, levelEndY, levelDir, blackboard) -> {
            LinkedList<Preset.UndoLogic> undoLogics = new LinkedList<>();

            for (int x = levelStartX; x <= levelEndX; x++) {
               for (int y = levelStartY; y <= levelEndY; y++) {
                  Preset.UndoLogic undoLogic = applyFunction.applyToLevel(level, x, y, levelDir, blackboard);
                  if (undoLogic != null) {
                     undoLogics.add(undoLogic);
                  }
               }
            }

            return (level1, presetX, presetY) -> undoLogics.forEach(undoLogicx -> undoLogicx.applyUndo(level, presetX, presetY));
         }, limitWithinBounds);
         Objects.requireNonNull(applyFunction);
      }

      @Override
      public Preset.UndoLogic applyToLevel(Level level, int presetX, int presetY, GameBlackboard blackboard) {
         int startX = presetX + Math.min(this.startX, this.endX);
         int endX = presetX + Math.max(this.startX, this.endX);
         int startY = presetY + Math.min(this.startY, this.endY);
         int endY = presetY + Math.max(this.startY, this.endY);
         return this.applyFunction.applyToLevel(level, startX, startY, endX, endY, this.dir, blackboard);
      }

      @Override
      public Preset.CustomApply copy(int xOffset, int yOffset, int maxWidth, int maxHeight) throws PresetCopyException {
         int newStartX = this.startX + xOffset;
         int newStartY = this.startY + yOffset;
         int newEndX = this.endX + xOffset;
         int newEndY = this.endY + yOffset;
         if (this.limitWithinBounds) {
            newStartX = Math.max(newStartX, 0);
            newStartY = Math.max(newStartY, 0);
            newEndX = Math.min(newEndX, maxWidth - 1);
            newEndY = Math.min(newEndY, maxHeight - 1);
            if (newStartX == newEndX) {
               return null;
            }

            if (newStartY == newEndY) {
               return null;
            }
         }

         return new Preset.CustomApplyArea(newStartX, newStartY, newEndX, newEndY, this.dir, this.applyFunction, this.limitWithinBounds);
      }

      @Override
      public Preset.CustomApply mirrorX(int width) throws PresetMirrorException {
         int newDir = this.dir != 1 && this.dir != 3 ? this.dir : (this.dir + 2) % 4;
         return new Preset.CustomApplyArea(
            PresetUtils.getMirroredValue(this.startX, width),
            this.startY,
            PresetUtils.getMirroredValue(this.endX, width),
            this.endY,
            newDir,
            this.applyFunction,
            this.limitWithinBounds
         );
      }

      @Override
      public Preset.CustomApply mirrorY(int height) throws PresetMirrorException {
         int newDir = this.dir != 0 && this.dir != 2 ? this.dir : (this.dir + 2) % 4;
         return new Preset.CustomApplyArea(
            this.startX,
            PresetUtils.getMirroredValue(this.startY, height),
            this.endX,
            PresetUtils.getMirroredValue(this.endY, height),
            newDir,
            this.applyFunction,
            this.limitWithinBounds
         );
      }

      @Override
      public Preset.CustomApply rotate(PresetRotation angle, int width, int height) throws PresetRotateException {
         int newDir = (this.dir + (angle == null ? 0 : angle.dirOffset)) % 4;
         Point pos1 = PresetUtils.getRotatedPointInSpace(this.startX, this.startY, width, height, angle);
         Point pos2 = PresetUtils.getRotatedPointInSpace(this.endX, this.endY, width, height, angle);
         return new Preset.CustomApplyArea(pos1.x, pos1.y, pos2.x, pos2.y, newDir, this.applyFunction, this.limitWithinBounds);
      }
   }

   @FunctionalInterface
   public interface CustomApplyAreaFunction {
      Preset.UndoLogic applyToLevel(Level var1, int var2, int var3, int var4, int var5, int var6, GameBlackboard var7);
   }

   @FunctionalInterface
   public interface CustomApplyFunction {
      Preset.UndoLogic applyToLevel(Level var1, int var2, int var3, int var4, GameBlackboard var5);
   }

   public static class CustomApplyTile implements Preset.CustomApply {
      public final int tileX;
      public final int tileY;
      public final int dir;
      public final Preset.CustomApplyFunction applyFunction;
      public final boolean limitWithinBounds;

      public CustomApplyTile(int tileX, int tileY, int dir, Preset.CustomApplyFunction applyFunction, boolean limitWithinBounds) {
         Objects.requireNonNull(applyFunction);
         this.tileX = tileX;
         this.tileY = tileY;
         this.dir = dir;
         this.applyFunction = applyFunction;
         this.limitWithinBounds = limitWithinBounds;
      }

      @Override
      public Preset.UndoLogic applyToLevel(Level level, int presetX, int presetY, GameBlackboard blackboard) {
         return this.applyFunction.applyToLevel(level, presetX + this.tileX, presetY + this.tileY, this.dir, blackboard);
      }

      @Override
      public Preset.CustomApply copy(int xOffset, int yOffset, int maxWidth, int maxHeight) throws PresetCopyException {
         int newX = this.tileX + xOffset;
         if (!this.limitWithinBounds || newX >= 0 && newX < maxWidth) {
            int newY = this.tileY + yOffset;
            return !this.limitWithinBounds || newY >= 0 && newY < maxHeight
               ? new Preset.CustomApplyTile(newX, newY, this.dir, this.applyFunction, this.limitWithinBounds)
               : null;
         } else {
            return null;
         }
      }

      @Override
      public Preset.CustomApply mirrorX(int width) throws PresetMirrorException {
         int newDir = this.dir != 1 && this.dir != 3 ? this.dir : (this.dir + 2) % 4;
         return new Preset.CustomApplyTile(PresetUtils.getMirroredValue(this.tileX, width), this.tileY, newDir, this.applyFunction, this.limitWithinBounds);
      }

      @Override
      public Preset.CustomApply mirrorY(int height) throws PresetMirrorException {
         int newDir = this.dir != 0 && this.dir != 2 ? this.dir : (this.dir + 2) % 4;
         return new Preset.CustomApplyTile(this.tileX, PresetUtils.getMirroredValue(this.tileY, height), newDir, this.applyFunction, this.limitWithinBounds);
      }

      @Override
      public Preset.CustomApply rotate(PresetRotation angle, int width, int height) throws PresetRotateException {
         int newDir = (this.dir + (angle == null ? 0 : angle.dirOffset)) % 4;
         Point pos = PresetUtils.getRotatedPointInSpace(this.tileX, this.tileY, width, height, angle);
         return new Preset.CustomApplyTile(pos.x, pos.y, newDir, this.applyFunction, this.limitWithinBounds);
      }
   }

   public static class ORApplyPredicate implements Preset.ApplyPredicate {
      public final ArrayList<Preset.ApplyPredicate> predicates;

      public ORApplyPredicate(List<Preset.ApplyPredicate> applyPredicates) {
         this.predicates = new ArrayList<>(applyPredicates);
      }

      public ORApplyPredicate(Preset.ApplyPredicate... predicates) {
         this(Arrays.asList(predicates));
      }

      @Override
      public boolean canApplyToLevel(Level level, int presetX, int presetY) {
         for (Preset.ApplyPredicate predicate : this.predicates) {
            if (predicate.canApplyToLevel(level, presetX, presetY)) {
               return true;
            }
         }

         return false;
      }

      @Override
      public Preset.ApplyPredicate copy(int xOffset, int yOffset, int maxWidth, int maxHeight) throws PresetCopyException {
         ArrayList<Preset.ApplyPredicate> copyList = new ArrayList<>(this.predicates.size());

         for (Preset.ApplyPredicate predicate : this.predicates) {
            Preset.ApplyPredicate copy = predicate.copy(xOffset, yOffset, maxWidth, maxHeight);
            if (copy != null) {
               copyList.add(copy);
            }
         }

         return copyList.isEmpty() ? null : new Preset.ORApplyPredicate(copyList);
      }

      @Override
      public Preset.ApplyPredicate mirrorX(int width) throws PresetMirrorException {
         ArrayList<Preset.ApplyPredicate> nextPredicates = new ArrayList<>(this.predicates.size());

         for (Preset.ApplyPredicate predicate : this.predicates) {
            nextPredicates.add(predicate.mirrorX(width));
         }

         return new Preset.ORApplyPredicate(nextPredicates);
      }

      @Override
      public Preset.ApplyPredicate mirrorY(int height) throws PresetMirrorException {
         ArrayList<Preset.ApplyPredicate> nextPredicates = new ArrayList<>(this.predicates.size());

         for (Preset.ApplyPredicate predicate : this.predicates) {
            nextPredicates.add(predicate.mirrorY(height));
         }

         return new Preset.ORApplyPredicate(nextPredicates);
      }

      @Override
      public Preset.ApplyPredicate rotate(PresetRotation angle, int width, int height) throws PresetRotateException {
         ArrayList<Preset.ApplyPredicate> nextPredicates = new ArrayList<>(this.predicates.size());

         for (Preset.ApplyPredicate predicate : this.predicates) {
            nextPredicates.add(predicate.rotate(angle, width, height));
         }

         return new Preset.ORApplyPredicate(nextPredicates);
      }
   }

   @FunctionalInterface
   public interface ObjectApplyListener {
      void onObjectApply(Level var1, int var2, int var3, int var4, int var5, int var6, GameBlackboard var7);
   }

   public static class PresetLogicGate {
      public final int logicGateID;
      public final SaveData data;
      public final boolean mirrorX;
      public final boolean mirrorY;
      public final PresetRotation rotation;

      public PresetLogicGate(int logicGateID, SaveData data, boolean mirrorX, boolean mirrorY, PresetRotation rotation) {
         if (data != null && !data.toLoadData().getName().equals("data")) {
            throw new IllegalArgumentException("Data name must be \"data\"");
         }

         this.logicGateID = logicGateID;
         this.data = data;
         this.mirrorX = mirrorX;
         this.mirrorY = mirrorY;
         this.rotation = rotation;
      }

      public Preset.PresetLogicGate copy() {
         return new Preset.PresetLogicGate(this.logicGateID, this.data, this.mirrorX, this.mirrorY, this.rotation);
      }

      public void applyToLevel(Level level, int tileX, int tileY, GameBlackboard blackboard) {
         level.logicLayer.setLogicGate(tileX, tileY, this.logicGateID, null);
         LogicGateEntity entity = level.logicLayer.getEntity(tileX, tileY);
         if (entity != null) {
            entity.applyPresetLoadData(this.data.toLoadData(), this.mirrorX, this.mirrorY, this.rotation);
         }
      }
   }

   public static class PresetObjectEntity {
      public final SaveData data;
      public final boolean mirrorX;
      public final boolean mirrorY;
      public final PresetRotation rotation;

      public PresetObjectEntity(SaveData data, boolean mirrorX, boolean mirrorY, PresetRotation rotation) {
         if (data != null && !data.toLoadData().getName().equals("data")) {
            throw new IllegalArgumentException("Data name must be \"data\"");
         }

         this.data = data;
         this.mirrorX = mirrorX;
         this.mirrorY = mirrorY;
         this.rotation = rotation;
      }

      public void applyToLevel(Level level, int tileX, int tileY, GameBlackboard blackboard) {
         ObjectEntity oe = level.entityManager.getObjectEntity(tileX, tileY);
         if (oe != null) {
            oe.applyPresetLoadData(this.data.toLoadData(), this.mirrorX, this.mirrorY, this.rotation);
         }
      }
   }

   @FunctionalInterface
   public interface TileApplyListener {
      void onTileApply(Level var1, int var2, int var3, int var4, GameBlackboard var5);
   }

   @FunctionalInterface
   public interface UndoLogic {
      void applyUndo(Level var1, int var2, int var3);
   }

   @FunctionalInterface
   public interface WireApplyListener {
      void onWireApply(Level var1, int var2, int var3, int var4, boolean var5, GameBlackboard var6);
   }
}
