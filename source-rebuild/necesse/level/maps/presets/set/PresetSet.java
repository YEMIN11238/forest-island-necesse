package necesse.level.maps.presets.set;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;
import necesse.engine.registries.BiomeRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.level.maps.biomes.Biome;
import necesse.level.maps.presets.Preset;

public abstract class PresetSet<T extends PresetSet<T>> {
   private static final HashMap<Biome, HashMap<Class<? extends PresetSet<?>>, HashSet<PresetSet<?>>>> incursionSets = new HashMap<>();
   private static final HashMap<Biome, HashMap<Class<? extends PresetSet<?>>, HashSet<PresetSet<?>>>> surfaceBiomeSets = new HashMap<>();
   private static final HashMap<Biome, HashMap<Class<? extends PresetSet<?>>, HashSet<PresetSet<?>>>> caveBiomeSets = new HashMap<>();
   private static final HashMap<Biome, HashMap<Class<? extends PresetSet<?>>, HashSet<PresetSet<?>>>> deepCaveBiomeSets = new HashMap<>();
   private static final HashMap<Class<? extends PresetSet<?>>, HashSet<PresetSet<?>>> alwaysAllowSets = new HashMap<>();
   public int[][] objectArrays = null;
   public int[][] tileArrays = null;
   public PresetSet<?>[][] nestedSetArrays = null;

   public <C extends Preset> C replaceWith(T replacementPresetSet, C preset) {
      if (replacementPresetSet == this) {
         return preset;
      }

      replaceIDs(this.objectArrays, replacementPresetSet.objectArrays, preset::replaceNonEmptyObjects);
      replaceIDs(this.tileArrays, replacementPresetSet.tileArrays, preset::replaceNonEmptyTiles);
      if (this.nestedSetArrays != null && replacementPresetSet.nestedSetArrays != null) {
         for (int arrayIndex = 0; arrayIndex < this.nestedSetArrays.length && arrayIndex < replacementPresetSet.nestedSetArrays.length; arrayIndex++) {
            PresetSet<?>[] myIDArray = this.nestedSetArrays[arrayIndex];
            PresetSet<?>[] otherIDArray = replacementPresetSet.nestedSetArrays[arrayIndex];
            if (otherIDArray != null && myIDArray != null) {
               for (int IDIndex = 0; IDIndex < myIDArray.length && IDIndex < otherIDArray.length; IDIndex++) {
                  PresetSet<?> mySet = myIDArray[IDIndex];
                  PresetSet<?> otherSet = otherIDArray[IDIndex];
                  if (otherSet.getClass().isInstance(mySet)) {
                     ((PresetSet)mySet.getClass().cast(mySet)).replaceWith(otherSet, preset);
                  }
               }
            }
         }
      }

      return preset;
   }

   private static void replaceIDs(int[][] myIDs, int[][] otherIDs, BiConsumer<Integer, Integer> consumer) {
      if (myIDs != null && otherIDs != null) {
         for (int arrayIndex = 0; arrayIndex < myIDs.length && arrayIndex < otherIDs.length; arrayIndex++) {
            int[] myIDArray = myIDs[arrayIndex];
            int[] otherIDArray = otherIDs[arrayIndex];
            if (otherIDArray != null && myIDArray != null) {
               for (int IDIndex = 0; IDIndex < myIDArray.length && IDIndex < otherIDArray.length; IDIndex++) {
                  int myID = myIDArray[IDIndex];
                  int otherID = otherIDArray[IDIndex];
                  consumer.accept(myID, otherID);
               }
            }
         }
      }
   }

   public <C extends Preset> C replaceWithRandomly(GameRandom random, T[] randomSets, C preset) {
      if (randomSets != null && randomSets.length != 0) {
         if (this.objectArrays != null) {
            for (int i = 0; i < this.objectArrays.length; i++) {
               ArrayList<Integer> objects = new ArrayList<>();
               int[] objectIDs = this.objectArrays[i];
               if (objectIDs != null && objectIDs.length != 0) {
                  for (T presetSet : randomSets) {
                     if (i < presetSet.objectArrays.length) {
                        int[] presetSetObjectIDs = presetSet.objectArrays[i];
                        if (presetSetObjectIDs != null) {
                           for (int objectID : presetSetObjectIDs) {
                              objects.add(objectID);
                           }
                        }
                     }
                  }

                  for (int objectID : objectIDs) {
                     if (objectID != -1) {
                        for (int[] presetObjectLayers : preset.objects) {
                           for (int j = 0; j < presetObjectLayers.length; j++) {
                              int presetObjectIds = presetObjectLayers[j];
                              if (presetObjectIds == objectID) {
                                 presetObjectLayers[j] = random.getOneOf(objects);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (this.tileArrays != null) {
            for (int i = 0; i < this.tileArrays.length; i++) {
               ArrayList<Integer> tiles = new ArrayList<>();
               int[] tileIDs = this.tileArrays[i];
               if (tileIDs != null && tileIDs.length != 0) {
                  for (T presetSet : randomSets) {
                     if (i < presetSet.tileArrays.length) {
                        int[] presetSetTileIDs = presetSet.tileArrays[i];
                        if (presetSetTileIDs != null) {
                           for (int tileID : presetSetTileIDs) {
                              tiles.add(tileID);
                           }
                        }
                     }
                  }

                  for (int tileID : tileIDs) {
                     if (tileID != -1) {
                        for (int j = 0; j < preset.tiles.length; j++) {
                           int presetTileIDs = preset.tiles[j];
                           if (presetTileIDs == tileID) {
                              preset.tiles[j] = random.getOneOf(tiles);
                           }
                        }
                     }
                  }
               }
            }
         }

         if (this.nestedSetArrays != null) {
            for (int i = 0; i < this.nestedSetArrays.length; i++) {
               ArrayList<PresetSet<?>> sets = new ArrayList<>();
               PresetSet<?>[] nestedSets = this.nestedSetArrays[i];
               if (nestedSets != null && nestedSets.length != 0) {
                  for (PresetSet<?> presetSet : randomSets) {
                     if (i < presetSet.nestedSetArrays.length) {
                        PresetSet<?>[] presetSetNestedSets = presetSet.nestedSetArrays[i];
                        if (presetSetNestedSets != null) {
                           sets.addAll(Arrays.asList(presetSetNestedSets));
                        }
                     }
                  }

                  for (PresetSet<?> nestedSet : nestedSets) {
                     if (nestedSet != null) {
                        for (PresetSet<?> set : sets) {
                           if (set.getClass().isInstance(nestedSet)) {
                              ((PresetSet)nestedSet.getClass().cast(nestedSet)).replaceWith(set, preset);
                           }
                        }
                     }
                  }
               }
            }
         }

         return preset;
      } else {
         return preset;
      }
   }

   public int replaceSingleObjectRandomly(GameRandom random, T lastSet, int lastObjectID) {
      if (this.objectArrays != null) {
         for (int i = 0; i < this.objectArrays.length; i++) {
            int[] objectIDs = this.objectArrays[i];
            if (objectIDs != null && objectIDs.length != 0 && i < lastSet.objectArrays.length) {
               int[] lastObjectIDs = lastSet.objectArrays[i];
               if (lastObjectIDs != null) {
                  for (int objectID : lastObjectIDs) {
                     if (objectID != -1 && objectID == lastObjectID) {
                        return objectIDs[random.nextInt(objectIDs.length)];
                     }
                  }
               }
            }
         }
      }

      return lastObjectID;
   }

   public boolean canReplaceObject(int objectID) {
      if (objectID <= 0) {
         return false;
      }

      if (this.objectArrays != null) {
         for (int[] objectArray : this.objectArrays) {
            if (objectArray != null) {
               for (int object : objectArray) {
                  if (object == objectID) {
                     return true;
                  }
               }
            }
         }
      }

      if (this.nestedSetArrays != null) {
         for (PresetSet<?>[] nestedSetArray : this.nestedSetArrays) {
            for (PresetSet<?> nestedSet : nestedSetArray) {
               if (nestedSet.canReplaceObject(objectID)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public boolean canReplaceTile(int tileID) {
      if (tileID <= 0) {
         return false;
      }

      if (this.tileArrays != null) {
         for (int[] tileArray : this.tileArrays) {
            if (tileArray != null) {
               for (int tile : tileArray) {
                  if (tile == tileID) {
                     return true;
                  }
               }
            }
         }
      }

      if (this.nestedSetArrays != null) {
         for (PresetSet<?>[] nestedSetArray : this.nestedSetArrays) {
            for (PresetSet<?> nestedSet : nestedSetArray) {
               if (nestedSet.canReplaceTile(tileID)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   protected T incursion(Biome... biomes) {
      for (Biome biome : biomes) {
         incursionSets.computeIfAbsent(biome, b -> new HashMap<>())
            .computeIfAbsent((Class<? extends PresetSet<?>>)this.getClass(), c -> new HashSet<>())
            .add(this);
      }

      return (T)this;
   }

   protected T incursion() {
      return this.incursion(
         BiomeRegistry.PLAINS,
         BiomeRegistry.SWAMP,
         BiomeRegistry.SNOW,
         BiomeRegistry.DESERT,
         BiomeRegistry.FOREST,
         BiomeRegistry.FOREST_DEEP_CAVE_INCURSION,
         BiomeRegistry.SWAMP_DEEP_CAVE_INCURSION,
         BiomeRegistry.SNOW_DEEP_CAVE_INCURSION,
         BiomeRegistry.DESERT_DEEP_CAVE_INCURSION,
         BiomeRegistry.SLIME_CAVE,
         BiomeRegistry.GRAVEYARD,
         BiomeRegistry.SPIDER_CASTLE,
         BiomeRegistry.SUN_ARENA,
         BiomeRegistry.MOON_ARENA,
         BiomeRegistry.CRYSTAL_HOLLOW
      );
   }

   protected T surface(Biome... biomes) {
      for (Biome biome : biomes) {
         surfaceBiomeSets.computeIfAbsent(biome, b -> new HashMap<>())
            .computeIfAbsent((Class<? extends PresetSet<?>>)this.getClass(), c -> new HashSet<>())
            .add(this);
      }

      return (T)this;
   }

   protected T surface() {
      return this.surface(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SWAMP, BiomeRegistry.SNOW, BiomeRegistry.DESERT);
   }

   protected T cave(Biome... biomes) {
      for (Biome biome : biomes) {
         caveBiomeSets.computeIfAbsent(biome, b -> new HashMap<>())
            .computeIfAbsent((Class<? extends PresetSet<?>>)this.getClass(), c -> new HashSet<>())
            .add(this);
      }

      return (T)this;
   }

   protected T cave() {
      return this.cave(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SWAMP, BiomeRegistry.SNOW, BiomeRegistry.DESERT);
   }

   protected T deepCave(Biome... biomes) {
      for (Biome biome : biomes) {
         deepCaveBiomeSets.computeIfAbsent(biome, b -> new HashMap<>())
            .computeIfAbsent((Class<? extends PresetSet<?>>)this.getClass(), c -> new HashSet<>())
            .add(this);
      }

      return (T)this;
   }

   protected T deepCave() {
      return this.deepCave(BiomeRegistry.FOREST, BiomeRegistry.PLAINS, BiomeRegistry.SWAMP, BiomeRegistry.SNOW, BiomeRegistry.DESERT);
   }

   protected T all() {
      alwaysAllowSets.computeIfAbsent((Class<? extends PresetSet<?>>)this.getClass(), c -> new HashSet<>()).add(this);
      return (T)this;
   }

   protected T all(Biome... biomes) {
      return this.surface(biomes).cave(biomes).deepCave(biomes).incursion(biomes);
   }

   public static <C extends PresetSet<?>> C[] getIncursionSets(Class<C> presetType, Biome biome) {
      return getSets(presetType, incursionSets, biome);
   }

   public static <C extends PresetSet<?>> C[] getSurfaceSets(Class<C> presetType, Biome biome) {
      return getSets(presetType, surfaceBiomeSets, biome);
   }

   public static <C extends PresetSet<?>> C[] getCaveSets(Class<C> presetType, Biome biome) {
      return getSets(presetType, caveBiomeSets, biome);
   }

   public static <C extends PresetSet<?>> C[] getDeepCaveSets(Class<C> presetType, Biome biome) {
      return getSets(presetType, deepCaveBiomeSets, biome);
   }

   private static <C extends PresetSet<?>> C[] getSets(
      Class<C> presetType, HashMap<Biome, HashMap<Class<? extends PresetSet<?>>, HashSet<PresetSet<?>>>> presetSetList, Biome biome
   ) {
      ArrayList<PresetSet<?>> output = new ArrayList<>();
      HashMap<Class<? extends PresetSet<?>>, HashSet<PresetSet<?>>> biomeSet = presetSetList.get(biome);
      HashSet<PresetSet<?>> alwaysSets = alwaysAllowSets.get(presetType);
      if (alwaysSets != null) {
         output.addAll(alwaysSets);
      }

      if (biomeSet != null) {
         HashSet<PresetSet<?>> sets = biomeSet.get(presetType);
         if (sets != null) {
            output.addAll(sets);
         }
      }

      return (C[])(!output.isEmpty()
         ? output.toArray((PresetSet[])((PresetSet[])Array.newInstance(presetType, output.size())))
         : (PresetSet[])Array.newInstance(presetType, 0));
   }

   @SafeVarargs
   public static <C extends PresetSet<C>> C[] getReducedSetForIncursionBiome(C[] set, Biome biome, C... alwaysInclude) {
      return (C[])getReducedSetForBiome(set, biome, incursionSets, true, alwaysInclude);
   }

   @SafeVarargs
   public static <C extends PresetSet<C>> C[] getReducedSetForIncursionBiome(C[] set, Biome biome, boolean returnValidSetOnNoMatch, C... alwaysInclude) {
      return (C[])getReducedSetForBiome(set, biome, incursionSets, returnValidSetOnNoMatch, alwaysInclude);
   }

   @SafeVarargs
   public static <C extends PresetSet<C>> C[] getReducedSetForBiome(C[] set, Biome biome, LevelIdentifier identifier, C... alwaysInclude) {
      return (C[])getReducedSetForBiome(set, biome, identifier, true, alwaysInclude);
   }

   @SafeVarargs
   public static <C extends PresetSet<C>> C[] getReducedSetForBiome(
      C[] set, Biome biome, LevelIdentifier identifier, boolean returnRandomValidSetOnNoMatch, C... alwaysInclude
   ) {
      HashMap<Biome, HashMap<Class<? extends PresetSet<?>>, HashSet<PresetSet<?>>>> biomeSets = surfaceBiomeSets;
      if (identifier == LevelIdentifier.CAVE_IDENTIFIER) {
         biomeSets = caveBiomeSets;
      } else if (identifier == LevelIdentifier.DEEP_CAVE_IDENTIFIER) {
         biomeSets = deepCaveBiomeSets;
      }

      return (C[])getReducedSetForBiome(set, biome, biomeSets, returnRandomValidSetOnNoMatch, alwaysInclude);
   }

   @SafeVarargs
   private static <C extends PresetSet<C>> C[] getReducedSetForBiome(
      C[] set,
      Biome biome,
      HashMap<Biome, HashMap<Class<? extends PresetSet<?>>, HashSet<PresetSet<?>>>> presetSetList,
      boolean returnValidSetOnNoMatch,
      C... alwaysInclude
   ) {
      ArrayList<C> reducedSet = new ArrayList<>(set.length);
      Class<C> setClass = (Class<C>)set.getClass().getComponentType();
      HashSet<PresetSet<?>> alwaysSets = alwaysAllowSets.get(setClass);
      HashMap<Class<? extends PresetSet<?>>, HashSet<PresetSet<?>>> biomeSet = presetSetList.get(biome);
      if (biomeSet != null) {
         HashSet<PresetSet<?>> presetSets = biomeSet.get(setClass);
         if (presetSets != null) {
            for (C presetSet : set) {
               if (presetSets.contains(presetSet)) {
                  reducedSet.add(presetSet);
               }
            }
         }

         if (alwaysSets != null) {
            for (C presetSet : set) {
               if (alwaysSets.contains(presetSet)) {
                  reducedSet.add(presetSet);
               }
            }
         }
      }

      Collections.addAll(reducedSet, alwaysInclude);
      if (reducedSet.isEmpty() && returnValidSetOnNoMatch) {
         if (biomeSet != null) {
            HashSet<PresetSet<?>> presetSets = biomeSet.get(setClass);
            if (presetSets != null) {
               return (C[])presetSets.toArray((PresetSet[])((PresetSet[])Array.newInstance(set.getClass().getComponentType(), presetSets.size())));
            }
         }

         return set;
      } else {
         return (C[])reducedSet.toArray((PresetSet[])((PresetSet[])Array.newInstance(set.getClass().getComponentType(), reducedSet.size())));
      }
   }
}
