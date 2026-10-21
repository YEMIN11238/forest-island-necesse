package necesse.level.maps.presets;

import java.util.concurrent.atomic.AtomicBoolean;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.friendly.human.HumanMob;
import necesse.gfx.HumanGender;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.maps.presets.set.CarpetSet;
import necesse.level.maps.presets.set.FloorSet;
import necesse.level.maps.presets.set.FurnitureSet;
import necesse.level.maps.presets.set.LargelPaintingSet;
import necesse.level.maps.presets.set.SmallPaintingSet;
import necesse.level.maps.presets.set.WallSet;

public class RetirementHomePreset extends Preset {
   public RetirementHomePreset(
      GameRandom random,
      WallSet wall,
      WallSet door,
      CarpetSet carpet,
      FloorSet mainFloor,
      FloorSet bathroomFloor,
      FurnitureSet furniture,
      SmallPaintingSet kitchenPainting,
      SmallPaintingSet entrancePainting,
      SmallPaintingSet deskPainting,
      SmallPaintingSet chestPainting,
      LargelPaintingSet largePainting
   ) {
      super(
         "PRESET = {\n\twidth = 18,\n\theight = 12,\n\ttileIDs = [64, cryptpath, 65, spidercastlefloor, 67, spidercastlecarpet, 36, basaltfloor, 27, dungeonfloor],\n\ttiles = [-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, -1, -1, -1, -1, 65, 65, 65, 65, 65, 65, 67, 67, 65, 65, 65, 65, 65, 65, -1, -1, -1, -1, 65, 65, 65, 65, 65, 65, 67, 67, 65, 65, 65, 65, 65, 65, -1, -1, -1, -1, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 27, 65, 65, -1, -1, -1, -1, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 36, 36, 36, 36, -1, -1, -1, -1, 65, 65, 65, 65, 65, 65, 65, 65, 65, 36, 36, 36, 36, 36, -1, -1, -1, -1, 65, 65, 65, 65, 65, 65, 65, 65, 65, 36, 36, 36, 36, 36, -1, -1, -1, 65, 65, 65, 65, 65, 65, 65, 65, 65, 65, 36, 36, 36, 36, 36, 36, -1, -1, -1, -1, -1, 64, 64, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1],\n\tobjectIDs = [0, air, 707, dungeonbathtub, 835, decorativepot1, 324, oillantern, 708, dungeonbathtub2, 836, decorativepot2, 1093, firemone, 709, dungeontoilet, 645, maplemodulartable, 326, walllantern, 328, candle, 531, oakchair, 668, birchmodulartable, 348, paintingrainsun, 352, paintingmouse, 545, oakdisplay, 803, spidercastlecandelabra, 1123, cookingpot, 932, leathercarpet, 356, paintingstonecaveling, 358, paintingswampcaveling, 169, deepstonewall, 873, dirtyplate, 938, purplecarpet, 362, paintinglargeship, 363, paintinglargeship2, 876, cuttingboard, 174, deepstonewindow, 687, dungeonchest, 690, dungeondesk, 691, dungeonmodulartable, 692, dungeonchair, 693, dungeonbench, 694, dungeonbench2, 695, dungeonbookshelf, 696, dungeoncabinet, 1464, swampcrate, 699, dungeondoublebed, 700, dungeondoublebedfoot1, 188, basaltdoor, 701, dungeondoublebed2, 189, basaltdooropen, 702, dungeondoublebedfoot2, 703, dungeondresser],\n\tobjects = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 169, 169, 169, 169, 169, 169, 169, 169, 169, 169, 169, 169, 169, 169, 169, 169, 0, 0, 169, 703, 695, 695, 690, 169, 169, 668, 668, 169, 169, 696, 699, 701, 703, 169, 0, 0, 174, 938, 938, 938, 531, 803, 545, 0, 0, 687, 803, 0, 700, 702, 0, 174, 0, 0, 169, 938, 938, 938, 938, 0, 692, 0, 0, 692, 0, 0, 932, 932, 0, 169, 0, 0, 169, 169, 0, 0, 169, 0, 0, 694, 693, 0, 0, 169, 169, 188, 169, 169, 0, 0, 169, 1093, 0, 0, 169, 169, 0, 0, 0, 0, 169, 169, 169, 0, 709, 169, 0, 0, 174, 694, 0, 0, 0, 356, 0, 0, 0, 0, 358, 1123, 169, 0, 0, 174, 0, 0, 169, 693, 326, 0, 0, 326, 692, 691, 691, 692, 324, 645, 169, 707, 708, 169, 0, 0, 169, 174, 169, 188, 189, 169, 174, 169, 169, 174, 169, 169, 169, 169, 169, 169, 0, 0, 1464, 0, 326, 0, 0, 326, 0, 836, 835, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],\n\trotations = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 1, 3, 3, 3, 1, 0, 2, 2, 2, 2, 0, 0, 0, 2, 2, 2, 2, 0, 0, 1, 0, 0, 0, 2, 2, 2, 2, 0, 1, 0, 0, 2, 3, 1, 2, 0, 0, 1, 3, 3, 3, 0, 2, 2, 2, 3, 0, 0, 0, 2, 3, 0, 0, 2, 2, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 2, 2, 1, 3, 3, 3, 2, 2, 3, 1, 3, 1, 0, 0, 3, 0, 0, 0, 2, 2, 3, 0, 3, 3, 2, 2, 3, 2, 2, 0, 0, 0, 2, 0, 0, 2, 0, 0, 1, 1, 1, 3, 3, 3, 3, 0, 0, 1, 0, 0, 3, 3, 3, 2, 2, 3, 3, 0, 0, 1, 2, 3, 3, 2, 2, 1, 0, 0, 3, 0, 2, 2, 2, 2, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],\n\ttileObjects = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 938, 938, 938, 938, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 938, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],\n\ttileRotations = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],\n\twallDecorObjects = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 348, 0, 0, 363, 362, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 352, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 326, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],\n\twallDecorRotations = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 2, 0, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],\n\ttableDecorObjects = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 328, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 328, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 873, 0, 0, 876, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],\n\ttableDecorRotations = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],\n\tclearOtherWires = false\n}"
      );
      WallSet.deepStone.replaceWith(wall, this);
      WallSet.basalt.replaceWith(door, this);
      CarpetSet.purple.replaceWith(carpet, this);
      FloorSet.spiderCastle.replaceWith(mainFloor, this);
      FloorSet.basalt.replaceWith(bathroomFloor, this);
      FurnitureSet.dungeon.replaceWith(furniture, this);
      SmallPaintingSet.rareSwampcaveling.replaceWith(kitchenPainting, this);
      SmallPaintingSet.rareStonecaveling.replaceWith(entrancePainting, this);
      SmallPaintingSet.commonRainsun.replaceWith(deskPainting, this);
      SmallPaintingSet.uncommonMouse.replaceWith(chestPainting, this);
      LargelPaintingSet.rareShip.replaceWith(largePainting, this);
      AtomicBoolean firstIsFemale = new AtomicBoolean(true);
      this.addHuman("human", 13, 2, human -> firstIsFemale.set(human.gender == HumanGender.FEMALE), random);
      this.addHuman("human", 14, 2, human -> {
         if (random.getChance(0.75)) {
            human.gender = firstIsFemale.get() ? HumanGender.MALE : HumanGender.FEMALE;
            human.customLook = true;
            human.setSettlerName(HumanMob.getRandomName(random, human.gender));
            human.randomizeLook(human.look, human.gender, random);
         }
      }, random);
      this.addInventory(new LootTable(random.getOneOf(LootItem.between("sapphire", 2, 5), LootItem.between("amethyst", 2, 5))), random, 7, 3);
      this.addInventory(LootTablePresets.surfaceRuinsChest, random, 10, 3);
      PresetUtils.addShoreTiles(this, 0, 0, this.width, this.height);
   }
}
