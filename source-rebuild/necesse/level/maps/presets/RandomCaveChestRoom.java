package necesse.level.maps.presets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.hostile.MimicMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.TrialEntranceObjectEntity;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.presets.set.ChestRoomSet;

public class RandomCaveChestRoom extends Preset {
   public RandomCaveChestRoom(GameRandom random, LootTable lootTable, AtomicInteger lootRotation, ChestRoomSet... chestRoomSets) {
      super(7, 7);
      this.applyScript(
         "PRESET = {\n\twidth = 7,\n\theight = 7,\n\ttileIDs = [17, stonefloor],\n\ttiles = [-1, 17, 17, 17, 17, 17, -1, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, 17, -1, 17, 17, 17, 17, 17, -1],\n\tobjectIDs = [0, air, 78, stonewall],\n\tobjects = [-1, 78, 78, 78, 78, 78, -1, 78, 78, 0, 0, 0, 78, 78, 78, 0, 0, 0, 0, 0, 78, 78, 0, 0, 0, 0, 0, 78, 78, 0, 0, 0, 0, 0, 78, 78, 78, 0, 0, 0, 78, 78, -1, 78, 78, 78, 78, 78, -1],\n\trotations = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],\n\ttileObjectsClear = true,\n\twallDecorObjectsClear = true,\n\ttableDecorObjectsClear = true\n}"
      );
      ChestRoomSet defaultSet = ChestRoomSet.stone;
      ChestRoomSet set = chestRoomSets.length == 0 ? ChestRoomSet.stone : random.getOneOf(chestRoomSets);
      defaultSet.replaceWith(set, this);
      this.addCustomApply(this.openingApply(random, lootTable, lootRotation, set));
   }

   private Preset.CustomApply openingApply(final GameRandom random, final LootTable lootTable, final AtomicInteger lootRotation, final ChestRoomSet set) {
      return new Preset.CustomApply() {
         @Override
         public Preset.UndoLogic applyToLevel(Level level, int presetX, int presetY, GameBlackboard blackboard) {
            int trapID = set.traps.length == 0 ? -1 : random.getOneOf(Arrays.stream(set.traps).boxed().toArray(Integer[]::new));
            boolean spawnTrap = trapID != -1 && random.getChance(0.9F);
            boolean trapDir = spawnTrap && random.nextBoolean();
            ArrayList<Runnable> validOpenings = new ArrayList<>();
            Runnable topOpening = () -> {
               level.setObject(presetX + 3, presetY + 3, set.inventoryObject, 0);
               level.setObject(presetX + 3, presetY, set.wallSet.doorClosed, 0);
               if (spawnTrap) {
                  RandomCaveChestRoom.this.placeTrap(level, set.pressureplate, trapID, presetX + 3, presetY + 1, trapDir ? 1 : 3, 2);
               }
            };
            Runnable rightOpening = () -> {
               level.setObject(presetX + 3, presetY + 3, set.inventoryObject, 1);
               level.setObject(presetX + 6, presetY + 3, set.wallSet.doorClosed, 1);
               if (spawnTrap) {
                  RandomCaveChestRoom.this.placeTrap(level, set.pressureplate, trapID, presetX + 5, presetY + 3, trapDir ? 0 : 2, 2);
               }
            };
            Runnable botOpening = () -> {
               level.setObject(presetX + 3, presetY + 3, set.inventoryObject, 2);
               level.setObject(presetX + 3, presetY + 6, set.wallSet.doorClosed, 2);
               if (spawnTrap) {
                  RandomCaveChestRoom.this.placeTrap(level, set.pressureplate, trapID, presetX + 3, presetY + 5, trapDir ? 1 : 3, 2);
               }
            };
            Runnable leftOpening = () -> {
               level.setObject(presetX + 3, presetY + 3, set.inventoryObject, 3);
               level.setObject(presetX, presetY + 3, set.wallSet.doorClosed, 3);
               if (spawnTrap) {
                  RandomCaveChestRoom.this.placeTrap(level, set.pressureplate, trapID, presetX + 1, presetY + 3, trapDir ? 0 : 2, 2);
               }
            };
            if (!level.getObject(presetX + 3, presetY - 1).isSolid) {
               validOpenings.add(topOpening);
            }

            if (!level.getObject(presetX + 7, presetY + 3).isSolid) {
               validOpenings.add(rightOpening);
            }

            if (!level.getObject(presetX + 3, presetY + 7).isSolid) {
               validOpenings.add(botOpening);
            }

            if (!level.getObject(presetX - 1, presetY + 3).isSolid) {
               validOpenings.add(leftOpening);
            }

            if (validOpenings.isEmpty()) {
               random.runOneOf(topOpening, rightOpening, botOpening, leftOpening);
            } else {
               random.getOneOf(validOpenings).run();
            }

            if (random.getChance(0.1F)) {
               int trialEntranceID = ObjectRegistry.getObjectID("trialentrance");
               level.setObject(presetX + 3, presetY + 3, trialEntranceID);
               ObjectEntity objectEntity = level.entityManager.getObjectEntity(presetX + 3, presetY + 3);
               if (objectEntity instanceof TrialEntranceObjectEntity) {
                  for (int i = 0; i < 2; i++) {
                     ArrayList<InventoryItem> itemList = lootTable.getNewList(random, level.buffManager.getModifier(LevelModifiers.LOOT), level, lootRotation);
                     ((TrialEntranceObjectEntity)objectEntity).addLootList(itemList);
                  }
               }
            } else if (level.isDeepCaveLevel() && random.getChance(0.1F)) {
               byte rotation = level.getObjectRotation(presetX + 3, presetY + 3);
               level.setObject(presetX + 3, presetY + 3, 0);
               if (!level.isClient()) {
                  MimicMob mimic = (MimicMob)MobRegistry.getMob("mimic", level);
                  lootTable.addItems(mimic.loot, random, level.buffManager.getModifier(LevelModifiers.LOOT), level, lootRotation);
                  mimic.canDespawn = false;
                  mimic.setDir(rotation);
                  mimic.onSpawned((presetX + 3) * 32 + 16, (presetY + 3) * 32 + 16);
                  level.entityManager.addMob(mimic, (presetX + 3) * 32 + 16, (presetY + 3) * 32 + 16);
                  return (level1, presetX1, presetY1) -> mimic.remove();
               }
            } else {
               lootTable.applyToLevel(random, level.buffManager.getModifier(LevelModifiers.LOOT), level, presetX + 3, presetY + 3, level, lootRotation);
            }

            return (level1, presetX1, presetY1) -> {};
         }

         @Override
         public Preset.CustomApply copy(int xOffset, int yOffset, int maxWidth, int maxHeight) throws PresetCopyException {
            throw new PresetCopyException("Cannot copy random cave chest rooms");
         }

         @Override
         public Preset.CustomApply mirrorX(int width) throws PresetMirrorException {
            return RandomCaveChestRoom.this.openingApply(random, lootTable, lootRotation, set);
         }

         @Override
         public Preset.CustomApply mirrorY(int height) throws PresetMirrorException {
            return RandomCaveChestRoom.this.openingApply(random, lootTable, lootRotation, set);
         }

         @Override
         public Preset.CustomApply rotate(PresetRotation angle, int width, int height) throws PresetRotateException {
            return RandomCaveChestRoom.this.openingApply(random, lootTable, lootRotation, set);
         }
      };
   }

   private void placeTrap(Level level, int plateID, int trapID, int plateX, int plateY, int dir, int trapRange) {
      if (plateID >= 0 && trapID >= 0) {
         level.setObject(plateX, plateY, plateID);
         dir %= 4;
         if (dir == 0) {
            level.setObject(plateX, plateY - trapRange, trapID, 2);

            for (int i = 0; i <= trapRange; i++) {
               level.wireManager.setWire(plateX, plateY - i, 0, true);
            }
         } else if (dir == 1) {
            level.setObject(plateX + trapRange, plateY, trapID, 3);

            for (int i = 0; i <= trapRange; i++) {
               level.wireManager.setWire(plateX + i, plateY, 0, true);
            }
         } else if (dir == 2) {
            level.setObject(plateX, plateY + trapRange, trapID, 0);

            for (int i = 0; i <= trapRange; i++) {
               level.wireManager.setWire(plateX, plateY + i, 0, true);
            }
         } else {
            level.setObject(plateX - trapRange, plateY, trapID, 1);

            for (int i = 0; i <= trapRange; i++) {
               level.wireManager.setWire(plateX - i, plateY, 0, true);
            }
         }
      }
   }
}
