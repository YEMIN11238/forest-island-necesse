package necesse.engine.registries;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.GameLoadingScreen;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.util.GameUtils;
import necesse.engine.util.LevelIdentifier;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.friendly.critters.flyingbugs.FireflyMob;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.lootItem.ChanceLootItem;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.AirObject;
import necesse.level.gameObject.AncientMummyStatueObject;
import necesse.level.gameObject.AncientPillarObject;
import necesse.level.gameObject.AncientTotemObject;
import necesse.level.gameObject.ArcanicMachinery;
import necesse.level.gameObject.AscendedPylonObject;
import necesse.level.gameObject.AscendedWallObject;
import necesse.level.gameObject.BambooDebrisObject;
import necesse.level.gameObject.BannerObject;
import necesse.level.gameObject.BannerOfPeaceObject;
import necesse.level.gameObject.BannerOfWarObject;
import necesse.level.gameObject.BeeHiveObject;
import necesse.level.gameObject.BigTentObject;
import necesse.level.gameObject.BlacksmithStatueObject;
import necesse.level.gameObject.BloodStainsObject;
import necesse.level.gameObject.BoneSpikeWallObject;
import necesse.level.gameObject.BounceGlyphTrapObject;
import necesse.level.gameObject.BrazierObject;
import necesse.level.gameObject.BrokenPaintingObject;
import necesse.level.gameObject.BurnedBushObject;
import necesse.level.gameObject.BurnedLeafPileObject;
import necesse.level.gameObject.BurnedTreeObject;
import necesse.level.gameObject.CactusObject;
import necesse.level.gameObject.CandlePedestalObject;
import necesse.level.gameObject.CattailObject;
import necesse.level.gameObject.CaveTreeObject;
import necesse.level.gameObject.CaveWaterPlantObject;
import necesse.level.gameObject.ChallengersBannerObject;
import necesse.level.gameObject.CheesePressObject;
import necesse.level.gameObject.ChickenGlyphTrapObject;
import necesse.level.gameObject.ChieftainBoneSpikeWallObject;
import necesse.level.gameObject.ChieftainsPedestalObject;
import necesse.level.gameObject.ChristmasTreeObject;
import necesse.level.gameObject.ChristmasWreathObject;
import necesse.level.gameObject.CobwebObject;
import necesse.level.gameObject.CoinPileObject;
import necesse.level.gameObject.ColumnObject;
import necesse.level.gameObject.CoolingTowerObject;
import necesse.level.gameObject.CowSkeletonObject;
import necesse.level.gameObject.CryptGrassObject;
import necesse.level.gameObject.CrystalClusterObject;
import necesse.level.gameObject.CrystalClusterSmallObject;
import necesse.level.gameObject.CustomWildFlowerObject;
import necesse.level.gameObject.DeadwoodTreeObject;
import necesse.level.gameObject.DeepSwampGrassObject;
import necesse.level.gameObject.DeepSwampTallGrassObject;
import necesse.level.gameObject.DiningSetTableDecorationObject;
import necesse.level.gameObject.DoubleStreetlampObject;
import necesse.level.gameObject.DryadTreeObject;
import necesse.level.gameObject.DungeonEntranceObject;
import necesse.level.gameObject.DungeonExitObject;
import necesse.level.gameObject.EggNestObject;
import necesse.level.gameObject.ElectricTowerObject;
import necesse.level.gameObject.EmptyRavenNestObject;
import necesse.level.gameObject.ExplosiveBarrelObject;
import necesse.level.gameObject.FallenAltarObject;
import necesse.level.gameObject.FallenIcicleObject;
import necesse.level.gameObject.FallingIcicleTriggerObject;
import necesse.level.gameObject.FenceGateObject;
import necesse.level.gameObject.FenceObject;
import necesse.level.gameObject.FireChaliceObject;
import necesse.level.gameObject.FireflyJarDecorationObject;
import necesse.level.gameObject.FlowerPatchObject;
import necesse.level.gameObject.FlowerPotObject;
import necesse.level.gameObject.FountainObject;
import necesse.level.gameObject.FreyaAndZithaStatueObject;
import necesse.level.gameObject.FreyaStatueObject;
import necesse.level.gameObject.FruitBushObject;
import necesse.level.gameObject.FruitTreeObject;
import necesse.level.gameObject.FuelSkullEncasingObject;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.GlowCoralObject;
import necesse.level.gameObject.GravestoneObject;
import necesse.level.gameObject.GroundSlimeObject;
import necesse.level.gameObject.HangingDecorationObject;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.HomestoneObject;
import necesse.level.gameObject.HuginStatueObject;
import necesse.level.gameObject.KatanaStoneObject;
import necesse.level.gameObject.KegObject;
import necesse.level.gameObject.LEDPanelObject;
import necesse.level.gameObject.LadderDownObject;
import necesse.level.gameObject.LargePaintingObject;
import necesse.level.gameObject.LargeTableDecorationObject;
import necesse.level.gameObject.LeafPileObject;
import necesse.level.gameObject.LeverObject;
import necesse.level.gameObject.MaskedPressurePlateObject;
import necesse.level.gameObject.MinecartTrackObject;
import necesse.level.gameObject.MissionBoardObject;
import necesse.level.gameObject.ModularCarpetObject;
import necesse.level.gameObject.OvergrownEnchantedFountainObject;
import necesse.level.gameObject.PaintingObject;
import necesse.level.gameObject.PearlescentShardObject;
import necesse.level.gameObject.PicnicBlanketObject;
import necesse.level.gameObject.PigStatueObject;
import necesse.level.gameObject.PlainsGrassObject;
import necesse.level.gameObject.PotTableDecorationObject;
import necesse.level.gameObject.PotatoClockDecorationObject;
import necesse.level.gameObject.PraiseTheMoonStatueObject;
import necesse.level.gameObject.ProcessingForgeObject;
import necesse.level.gameObject.RandomCoinStackObject;
import necesse.level.gameObject.RandomCrateObject;
import necesse.level.gameObject.RandomVaseObject;
import necesse.level.gameObject.RavenEffigyObject;
import necesse.level.gameObject.RavenNestWithEggObject;
import necesse.level.gameObject.RavenSkullObject;
import necesse.level.gameObject.ReedsObject;
import necesse.level.gameObject.ReverseDamageGlyphTrapObject;
import necesse.level.gameObject.RockObject;
import necesse.level.gameObject.RockOreObject;
import necesse.level.gameObject.RoyalEggObject;
import necesse.level.gameObject.RunicBoulderObject;
import necesse.level.gameObject.SaplingObject;
import necesse.level.gameObject.ScrapheapBigObject;
import necesse.level.gameObject.SeedObject;
import necesse.level.gameObject.SettlementFlagObject;
import necesse.level.gameObject.SignObject;
import necesse.level.gameObject.SingleOreRockSmall;
import necesse.level.gameObject.SingleRockObject;
import necesse.level.gameObject.SingleRockSmall;
import necesse.level.gameObject.SinkObject;
import necesse.level.gameObject.SmallRuneStoneObject;
import necesse.level.gameObject.SnowManTrainingDummyObject;
import necesse.level.gameObject.SnowPileObject;
import necesse.level.gameObject.SpiderCastleWallCandleObject;
import necesse.level.gameObject.SpiderEggObject;
import necesse.level.gameObject.SpiderThroneObject;
import necesse.level.gameObject.SpideriteArmorStandObject;
import necesse.level.gameObject.SpikeTrapObject;
import necesse.level.gameObject.StatueObject;
import necesse.level.gameObject.SteppingStonesObject;
import necesse.level.gameObject.StreetlampObject;
import necesse.level.gameObject.SurfaceGrassObject;
import necesse.level.gameObject.SwampGrassObject;
import necesse.level.gameObject.SwampRootObject;
import necesse.level.gameObject.SwampSporeObject;
import necesse.level.gameObject.TNTObject;
import necesse.level.gameObject.TableDecorationObject;
import necesse.level.gameObject.TeleportationStoneObject;
import necesse.level.gameObject.TempleEntranceObject;
import necesse.level.gameObject.TempleExitObject;
import necesse.level.gameObject.TemplePedestalObject;
import necesse.level.gameObject.TheCoreStatueObject;
import necesse.level.gameObject.ThornsObject;
import necesse.level.gameObject.TicTacToeGameObject;
import necesse.level.gameObject.TikiTorchObject;
import necesse.level.gameObject.TileClutterObject;
import necesse.level.gameObject.TorchObject;
import necesse.level.gameObject.TrainingDummyObject;
import necesse.level.gameObject.TrapTrackObject;
import necesse.level.gameObject.TreeObject;
import necesse.level.gameObject.TreeSaplingObject;
import necesse.level.gameObject.TreeStumpObject;
import necesse.level.gameObject.TrialEntranceObject;
import necesse.level.gameObject.TrialExitObject;
import necesse.level.gameObject.VendingMachineObject;
import necesse.level.gameObject.VoidVesselObject;
import necesse.level.gameObject.WallArrowTrapObject;
import necesse.level.gameObject.WallCandleObject;
import necesse.level.gameObject.WallExplosiveBarrelTrapObject;
import necesse.level.gameObject.WallFlameTrapObject;
import necesse.level.gameObject.WallObject;
import necesse.level.gameObject.WallSawTrapObject;
import necesse.level.gameObject.WallTorchObject;
import necesse.level.gameObject.WallVoidTrapObject;
import necesse.level.gameObject.WallWindowPlaceObject;
import necesse.level.gameObject.WaterLanternObject;
import necesse.level.gameObject.WaterPlantObject;
import necesse.level.gameObject.WaystoneObject;
import necesse.level.gameObject.WitchStatueObject;
import necesse.level.gameObject.WitheredGrassObject;
import necesse.level.gameObject.ZithaStatueObject;
import necesse.level.gameObject.container.AlchemyTableObject;
import necesse.level.gameObject.container.ApiaryObject;
import necesse.level.gameObject.container.ArmorStandObject;
import necesse.level.gameObject.container.BannerStandObject;
import necesse.level.gameObject.container.BookshelfObject;
import necesse.level.gameObject.container.CabinetObject;
import necesse.level.gameObject.container.CampfireObject;
import necesse.level.gameObject.container.CarpentersBenchObject;
import necesse.level.gameObject.container.CartographerTableObject;
import necesse.level.gameObject.container.CaveglowAlchemyTableObject;
import necesse.level.gameObject.container.CoffinObject;
import necesse.level.gameObject.container.CompostBinObject;
import necesse.level.gameObject.container.CookingPotObject;
import necesse.level.gameObject.container.CookingStationObject;
import necesse.level.gameObject.container.CoolingBoxInventoryObject;
import necesse.level.gameObject.container.DemonicAnvilObject;
import necesse.level.gameObject.container.DemonicWorkstationDuoObject;
import necesse.level.gameObject.container.DemonicWorkstationLegacyObject;
import necesse.level.gameObject.container.DisplayStandObject;
import necesse.level.gameObject.container.FallenAlchemyTableObject;
import necesse.level.gameObject.container.FallenAnvilObject;
import necesse.level.gameObject.container.FallenCarpentersBenchObject;
import necesse.level.gameObject.container.FallenLandscapingStationObject;
import necesse.level.gameObject.container.FallenWorkstationObject;
import necesse.level.gameObject.container.FeedingTroughObject;
import necesse.level.gameObject.container.FireworkDispenserObject;
import necesse.level.gameObject.container.GrainMillBaseObject;
import necesse.level.gameObject.container.IncineratorInventoryObject;
import necesse.level.gameObject.container.InventoryObject;
import necesse.level.gameObject.container.IronAnvilObject;
import necesse.level.gameObject.container.LandscapingStationObject;
import necesse.level.gameObject.container.MimicStorageBoxInventoryObject;
import necesse.level.gameObject.container.MusicPlayerObject;
import necesse.level.gameObject.container.RoastingStationObject;
import necesse.level.gameObject.container.SalvageStationObject;
import necesse.level.gameObject.container.ShippingChestObject;
import necesse.level.gameObject.container.StorageBoxInventoryObject;
import necesse.level.gameObject.container.TransmutationStationObject;
import necesse.level.gameObject.container.TungstenAnvilObject;
import necesse.level.gameObject.container.TungstenCarpentersBenchObject;
import necesse.level.gameObject.container.TungstenLandscapingStationObject;
import necesse.level.gameObject.container.TungstenWorkstationObject;
import necesse.level.gameObject.container.UpgradeStationObject;
import necesse.level.gameObject.container.VoidAlchemyTableObject;
import necesse.level.gameObject.container.WaterproofStorageBoxInventoryObject;
import necesse.level.gameObject.container.WorkstationDuoObject;
import necesse.level.gameObject.container.WorkstationLegacyObject;
import necesse.level.gameObject.furniture.BathtubObject;
import necesse.level.gameObject.furniture.BedObject;
import necesse.level.gameObject.furniture.BenchObject;
import necesse.level.gameObject.furniture.CandelabraObject;
import necesse.level.gameObject.furniture.CandlesObject;
import necesse.level.gameObject.furniture.ChairObject;
import necesse.level.gameObject.furniture.ChieftainsThroneChairObject;
import necesse.level.gameObject.furniture.ClockObject;
import necesse.level.gameObject.furniture.DeadWoodChairObject;
import necesse.level.gameObject.furniture.DeskObject;
import necesse.level.gameObject.furniture.DinnerTableObject;
import necesse.level.gameObject.furniture.DresserObject;
import necesse.level.gameObject.furniture.DungeonChairObject;
import necesse.level.gameObject.furniture.FlowerObject;
import necesse.level.gameObject.furniture.LampObject;
import necesse.level.gameObject.furniture.LogBenchObject;
import necesse.level.gameObject.furniture.ModularTableObject;
import necesse.level.gameObject.furniture.SpiderCastleChairObject;
import necesse.level.gameObject.furniture.ToiletObject;
import necesse.level.gameObject.furniture.doubleBed.DoubleBedBaseObject;
import necesse.level.gameObject.happinessObject.BearRugObject;
import necesse.level.gameObject.happinessObject.CavelingDisplayStandObject;
import necesse.level.gameObject.happinessObject.DryadPlantObject;
import necesse.level.gameObject.happinessObject.FishianBathtubObject;
import necesse.level.gameObject.happinessObject.FriendlyBushObject;
import necesse.level.gameObject.happinessObject.GoblinFriendsLargePaintingObject;
import necesse.level.gameObject.happinessObject.LuckyRabbitsFootObject;
import necesse.level.gameObject.happinessObject.MossyCarpetObject;
import necesse.level.gameObject.happinessObject.ParrotPerchObject;
import necesse.level.gameObject.happinessObject.PestWardenLampObject;
import necesse.level.gameObject.happinessObject.PirateCarpetObject;
import necesse.level.gameObject.happinessObject.QueenEggNestObject;
import necesse.level.gameObject.happinessObject.ReaperScytheStandObject;
import necesse.level.gameObject.happinessObject.RuneboundDrumObject;
import necesse.level.gameObject.happinessObject.SageAndGritPedestalObject;
import necesse.level.gameObject.happinessObject.SarcophagusObject;
import necesse.level.gameObject.happinessObject.SheepChairObject;
import necesse.level.gameObject.happinessObject.SlimeBenchObject;
import necesse.level.gameObject.happinessObject.SpiritBasinObject;
import necesse.level.gameObject.happinessObject.SwampGuardianLargePaintingObject;
import necesse.level.gameObject.happinessObject.TropicalIslandPaintingObject;
import necesse.level.gameObject.happinessObject.UnmeltableIceObject;
import necesse.level.gameObject.happinessObject.VoidFlameObject;
import necesse.level.gameObject.happinessObject.VultureDisplayStandObject;
import necesse.level.gameObject.happinessObject.WallChampionBeltObject;
import necesse.level.gameObject.happinessObject.WallFangOfTheProtectorObject;
import necesse.level.gameObject.happinessObject.WitchCauldronObject;
import necesse.level.gameObject.happinessObject.WizardBookshelfObject;
import necesse.level.gameObject.happinessObject.WoodenDuckObject;
import necesse.level.maps.levelData.settlementData.settler.thoughts.HappinessObjectSettlerThought;

public class ObjectRegistry extends GameRegistry<ObjectRegistry.ObjectRegistryElement> {
   public static int rockID;
   public static int snowRockID;
   public static int sandstoneRockID;
   public static int swampRockID;
   public static int graniteRockID;
   public static int deepRockID;
   public static int deepSnowRockID;
   public static int basaltRockID;
   public static int deepSwampRockID;
   public static int deepSandstoneRockID;
   public static int charredRockID;
   public static int cobWebID;
   public static int thornsID;
   public static final ObjectRegistry instance = new ObjectRegistry();
   private static String[] stringIDs = null;

   private ObjectRegistry() {
      super("Object", 32762);
   }

   @Override
   public void registerCore() {
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "objects"));
      Color woodFurniture = new Color(117, 59, 9);
      Color goldFurniture = new Color(255, 233, 73);
      registerObject("air", new AirObject(), 0.0F, false);
      registerObject("coin", new CoinPileObject().addGlobalIngredient("coin"), 1.0F, true);
      registerObject("oaktree", new TreeObject("oaktree", "oaklog", "oaksapling", new Color(116, 69, 43), 45, 60, 110, "oakleaves"), 0.0F, false, false, true);
      registerObject("oaktreestump", new TreeStumpObject("oaktreestump", "oaklog", new Color(116, 69, 43)), 8.0F, true);
      registerObject("oaksapling", new TreeSaplingObject("oaksapling", new Color(116, 69, 43), "oaktree", 1800, 2700, true), 5.0F, true);
      registerObject(
         "sprucetree",
         new TreeObject("sprucetree", "sprucelog", "sprucesapling", new Color(121, 74, 23), 45, 60, 110, "spruceleaves"),
         0.0F,
         false,
         false,
         true
      );
      registerObject("sprucetreestump", new TreeStumpObject("sprucetreestump", "sprucelog", new Color(121, 74, 23)), 8.0F, true);
      LogBenchObject.registerLogBench("sprucelogbench", "sprucelogbench", "sprucelog", ToolType.AXE, new Color(121, 74, 23), 8.0F);
      registerObject("sprucesapling", new TreeSaplingObject("sprucesapling", new Color(121, 74, 23), "sprucetree", 1800, 2700, true), 5.0F, true);
      registerObject(
         "pinetree", new TreeObject("pinetree", "pinelog", "pinesapling", new Color(75, 65, 36), 32, 60, 120, "pineleaves"), 0.0F, false, false, true
      );
      registerObject("pinesapling", new TreeSaplingObject("pinesapling", new Color(75, 65, 36), "pinetree", 1800, 2700, true), 5.0F, true);
      registerObject(
         "palmtree", new TreeObject("palmtree", "palmlog", "palmsapling", new Color(133, 79, 18), 40, 80, 120, "palmleaves"), 0.0F, false, false, true
      );
      registerObject("palmsapling", new TreeSaplingObject("palmsapling", new Color(133, 79, 18), "palmtree", 1800, 2700, true, "sandtile"), 5.0F, true);
      registerObject(
         "willowtree", new TreeObject("willowtree", "willowlog", "willowsapling", new Color(99, 52, 50), 42, 70, 110, "willowleaves"), 0.0F, false, false, true
      );
      registerObject("willowtreestump", new TreeStumpObject("willowtreestump", "willowlog", new Color(99, 52, 50)), 8.0F, true);
      LogBenchObject.registerLogBench("willowlogbench", "willowlogbench", "willowlog", ToolType.AXE, new Color(99, 52, 50), 8.0F);
      registerObject("willowsapling", new TreeSaplingObject("willowsapling", new Color(99, 52, 50), "willowtree", 1800, 2700, true), 5.0F, true);
      TreeObject mapleTree = new TreeObject("mapletree", "maplelog", "maplesapling", new Color(133, 38, 12), 42, 70, 110, "mapleleaves");
      mapleTree.setTreeYIndexToLeafYIndexTable(1, 0, 1, 0);
      registerObject("mapletree", mapleTree, 0.0F, false, false, true);
      registerObject("maplesapling", new TreeSaplingObject("maplesapling", new Color(133, 38, 12), "mapletree", 1800, 2700, true), 5.0F, true);
      TreeObject birchTree = new TreeObject("birchtree", "birchlog", "birchsapling", new Color(176, 59, 25), 42, 70, 110, "birchleaves");
      birchTree.setTreeYIndexToLeafYIndexTable(0, 0, 1, 1);
      registerObject("birchtree", birchTree, 0.0F, false, false, true);
      registerObject("birchsapling", new TreeSaplingObject("birchsapling", new Color(176, 59, 25), "birchtree", 1800, 2700, true), 5.0F, true);
      registerObject("cactus", new CactusObject("cactussapling"), 0.0F, false, false, true);
      registerObject(
         "cactussapling",
         new TreeSaplingObject("cactussapling", new Color(47, 79, 8), "cactus", 1800, 2700, true).overrideValidTiles("sandtile", "farmland"),
         5.0F,
         true
      );
      registerObject(
         "dryadtree",
         new DryadTreeObject("dryadtree", "dryadlog", "dryadsapling", new Color(121, 74, 23), 45, 60, 110, "dryadleaves"),
         0.0F,
         false,
         false,
         true
      );
      TreeSaplingObject dryadSapling = new TreeSaplingObject(
         "dryadsapling", new Color(121, 74, 23), "dryadtree", 1800, 2700, false, "sandtile", "basaltrocktile", "granite", "watertile"
      );
      dryadSapling.canPlaceOnLiquid = true;
      registerObject("dryadsapling", dryadSapling, 5.0F, true);
      LogBenchObject.registerLogBench("dryadlogbench", "dryadlogbench", "dryadlog", ToolType.AXE, new Color(157, 107, 62), 8.0F);
      registerObject(
         "cavemapletree",
         new CaveTreeObject("cavemapletree", "maplelog", "cavemaplesapling", new Color(156, 44, 15), 42, 70, 110, "mapleleaves"),
         0.0F,
         false,
         false,
         true
      );
      registerObject(
         "cavemaplesapling",
         new TreeSaplingObject("cavemaplesapling", new Color(156, 44, 15), "cavemapletree", 1800, 2700, false, "basaltrocktile", "granite"),
         5.0F,
         true
      );
      registerObject(
         "cavebirchtree",
         new CaveTreeObject("cavebirchtree", "birchlog", "cavebirchsapling", new Color(156, 44, 15), 42, 70, 110, "birchleaves"),
         0.0F,
         false,
         false,
         true
      );
      registerObject(
         "cavebirchsapling",
         new TreeSaplingObject("cavebirchsapling", new Color(156, 44, 15), "cavebirchtree", 1800, 2700, false, "basaltrocktile", "granite"),
         5.0F,
         true
      );
      registerObject(
         "bambootree", new TreeObject("bambootree", "bamboo", "bamboosapling", new Color(106, 131, 63), 42, 70, 110, "bambooleaves"), 0.0F, false, false, true
      );
      registerObject("bamboosapling", new TreeSaplingObject("bamboosapling", new Color(106, 131, 63), "bambootree", 1200, 2100, true), 5.0F, true);
      LogBenchObject.registerLogBench("bamboologbench", "bamboologbench", "dryadlog", ToolType.AXE, new Color(155, 83, 73), 8.0F);
      registerObject(
         "deadwoodtree",
         new DeadwoodTreeObject("deadwood", "deadwoodlog", "deadwoodsapling", new Color(74, 53, 60), 42, 42, 70, "deadwoodleaves"),
         0.0F,
         false,
         false,
         true
      );
      registerObject("deadwoodsapling", new TreeSaplingObject("deadwoodsapling", new Color(74, 53, 60), "deadwoodtree", 2700, 3600, true), 5.0F, true);
      registerObject(
         "appletree",
         new FruitTreeObject("appletree", "sprucelog", "applesapling", 900.0F, 1800.0F, "apple", 1.5F, 4, new Color(182, 60, 53), 30, 60, 100, "appleleaves"),
         0.0F,
         false,
         false,
         true
      );
      registerObject("applesapling", new TreeSaplingObject("applesapling", new Color(182, 60, 53), "appletree", 1800, 2700, false), 50.0F, true);
      registerObject(
         "lemontree",
         new FruitTreeObject("lemontree", "sprucelog", "lemonsapling", 900.0F, 1800.0F, "lemon", 1.5F, 4, new Color(211, 185, 6), 30, 60, 100, "lemonleaves"),
         0.0F,
         false,
         false,
         true
      );
      registerObject("lemonsapling", new TreeSaplingObject("lemonsapling", new Color(211, 185, 6), "lemontree", 1800, 2700, false), 50.0F, true);
      registerObject(
         "coconuttree",
         new FruitTreeObject(
            "coconuttree", "palmlog", "coconutsapling", 900.0F, 1800.0F, "coconut", 1.5F, 4, new Color(109, 68, 29), 42, 70, 110, "fruitpalmleaves"
         ),
         0.0F,
         false,
         false,
         true
      );
      registerObject(
         "coconutsapling", new TreeSaplingObject("coconutsapling", new Color(109, 68, 29), "coconuttree", 1800, 2700, false, "sandtile"), 50.0F, true
      );
      registerObject(
         "bananatree",
         new FruitTreeObject(
            "bananatree", "palmlog", "bananasapling", 900.0F, 1800.0F, "banana", 1.5F, 4, new Color(232, 210, 75), 42, 60, 80, "fruitpalmleaves"
         ),
         0.0F,
         false,
         false,
         true
      );
      registerObject("bananasapling", new TreeSaplingObject("bananasapling", new Color(232, 210, 75), "bananatree", 1800, 2700, false, "sandtile"), 50.0F, true);
      registerObject(
         "blueberrybush",
         new FruitBushObject("blueberrybush", "blueberrysapling", 900.0F, 1800.0F, "blueberry", 1.0F, 2, new Color(16, 93, 162))
            .setDebrisColor(new Color(46, 99, 39)),
         0.0F,
         false,
         false,
         true
      );
      registerObject("blueberrysapling", new SaplingObject("blueberrysapling", null, "blueberrybush", 1200, 2100, false), 30.0F, true);
      registerObject(
         "raspberrybush",
         new FruitBushObject("raspberrybush", "raspberrysapling", 900.0F, 1800.0F, "raspberry", 1.0F, 2, new Color(182, 60, 53))
            .setDebrisColor(new Color(178, 126, 39)),
         0.0F,
         false,
         false,
         true
      );
      registerObject("raspberrysapling", new SaplingObject("raspberrysapling", new Color(203, 139, 15), "raspberrybush", 1200, 2100, false), 30.0F, true);
      registerObject(
         "blackberrybush",
         new FruitBushObject("blackberrybush", "blackberrysapling", 900.0F, 1800.0F, "blackberry", 1.0F, 2, new Color(60, 49, 81))
            .setDebrisColor(new Color(50, 115, 44)),
         0.0F,
         false,
         false,
         true
      );
      registerObject("blackberrysapling", new SaplingObject("blackberrysapling", null, "blackberrybush", 1200, 2100, false), 30.0F, true);
      registerObject("workstation", new WorkstationLegacyObject(), 10.0F, false);
      WorkstationDuoObject.registerWorkstation();
      registerObject("forge", new ProcessingForgeObject(), 2.0F, true);
      CarpentersBenchObject.registerCarpentersBench();
      TungstenCarpentersBenchObject.registerTungstenCarpentersBench();
      FallenCarpentersBenchObject.registerFallenCarpentersBench();
      LandscapingStationObject.registerLandscapingStation();
      TungstenLandscapingStationObject.registerTungstenLandscapingStation();
      FallenLandscapingStationObject.registerFallenLandscapingStation();
      registerObject("ironanvil", new IronAnvilObject(), 20.0F, true);
      registerObject("alchemytable", new AlchemyTableObject(), 10.0F, true);
      registerObject("demonicworkstation", new DemonicWorkstationLegacyObject(), 40.0F, false);
      DemonicWorkstationDuoObject.registerWorkstation();
      registerObject("demonicanvil", new DemonicAnvilObject(), 40.0F, true);
      registerObject("voidalchemytable", new VoidAlchemyTableObject(), 65.0F, true);
      TungstenWorkstationObject.registerWorkstation();
      registerObject("tungstenanvil", new TungstenAnvilObject(), 80.0F, true);
      registerObject("caveglowalchemytable", new CaveglowAlchemyTableObject(), 85.0F, true);
      FallenWorkstationObject.registerWorkstation();
      registerObject("fallenanvil", new FallenAnvilObject(), 140.0F, true);
      registerObject("fallenalchemytable", new FallenAlchemyTableObject(), 50.0F, true);
      registerObject("transmutationstation", new TransmutationStationObject(), 50.0F, true);
      registerObject("cartographertable", new CartographerTableObject(), 10.0F, true);
      int[] woodWallIDs = WallObject.registerWallObjects("wood", "woodwall", 0.0F, new Color(93, 67, 24), ToolType.ALL, -1.0F, -1.0F);
      WallObject woodWall = (WallObject)getObject(woodWallIDs[0]);
      int[] pineWallIDs = WallObject.registerWallObjects("pine", "pinewall", 0.0F, new Color(134, 97, 44), ToolType.ALL, -1.0F, -1.0F);
      WallObject pineWall = (WallObject)getObject(pineWallIDs[0]);
      int[] palmWallIDs = WallObject.registerWallObjects("palm", "palmwall", 0.0F, new Color(128, 86, 27), ToolType.ALL, -1.0F, -1.0F);
      WallObject palmWall = (WallObject)getObject(palmWallIDs[0]);
      int[] willowWallIDs = WallObject.registerWallObjects("willow", "willowwall", 0.0F, new Color(121, 71, 60), ToolType.ALL, -1.0F, -1.0F);
      WallObject willowWall = (WallObject)getObject(willowWallIDs[0]);
      int[] dryadWallIDs = WallObject.registerWallObjects("dryad", "dryadwall", 0.0F, new Color(97, 74, 60), ToolType.ALL, -1.0F, -1.0F);
      WallObject dryadWall = (WallObject)getObject(dryadWallIDs[0]);
      int[] bambooWallIDs = WallObject.registerWallObjects("bamboo", "bamboowall", 0.0F, new Color(104, 57, 51), ToolType.ALL, -1.0F, -1.0F);
      WallObject bambooWall = (WallObject)getObject(bambooWallIDs[0]);
      int[] stoneWallIDs = WallObject.registerWallObjects("stone", "stonewall", 0.0F, new Color(130, 139, 152), -1.0F, -1.0F);
      WallObject stoneWall = (WallObject)getObject(stoneWallIDs[0]);
      int[] sandstoneWallIDs = WallObject.registerWallObjects("sandstone", "sandstonewall", 0.0F, new Color(204, 155, 110), -1.0F, -1.0F);
      WallObject sandstoneWall = (WallObject)getObject(sandstoneWallIDs[0]);
      int[] swampstoneWallIDs = WallObject.registerWallObjects("swampstone", "swampstonewall", 0.0F, new Color(124, 131, 30), -1.0F, -1.0F);
      WallObject swampstoneWall = (WallObject)getObject(swampstoneWallIDs[0]);
      int[] snowstoneWallIDs = WallObject.registerWallObjects("snowstone", "snowstonewall", 0.0F, new Color(182, 200, 214), -1.0F, -1.0F);
      WallObject snowstoneWall = (WallObject)getObject(snowstoneWallIDs[0]);
      int[] graniteWallIDs = WallObject.registerWallObjects("granite", "granitewall", 0.0F, new Color(88, 49, 38), -1.0F, -1.0F);
      WallObject graniteWall = (WallObject)getObject(graniteWallIDs[0]);
      int[] iceWallIDs = WallObject.registerWallObjects("ice", "icewall", 0.0F, new Color(63, 185, 238), -1.0F, -1.0F);
      WallObject iceWall = (WallObject)getObject(iceWallIDs[0]);
      WallObject.registerWallObjects("brick", "brickwall", 0.0F, new Color(166, 91, 62), -1.0F, -1.0F);
      int[] dungeonWallIDs = WallObject.registerWallObjects("dungeon", "dungeonwall", 1.0F, new Color(98, 104, 113), 5.0F, 10.0F);
      WallObject dungeonWall = (WallObject)getObject(dungeonWallIDs[0]);
      int[] deepstoneWallIDs = WallObject.registerWallObjects("deepstone", "deepstonewall", 2.0F, new Color(90, 82, 63), -1.0F, -1.0F);
      WallObject deepstoneWall = (WallObject)getObject(deepstoneWallIDs[0]);
      int[] obsidianWallIDs = WallObject.registerWallObjects("obsidian", "obsidianwall", 6.0F, new Color(21, 21, 60), -1.0F, -1.0F);
      WallObject obsidianWall = (WallObject)getObject(obsidianWallIDs[0]);
      int[] deepSnowstoneWallIDs = WallObject.registerWallObjects("deepsnowstone", "deepsnowstonewall", 6.0F, new Color(32, 90, 107), -1.0F, -1.0F);
      WallObject deepSnowstoneWall = (WallObject)getObject(deepSnowstoneWallIDs[0]);
      int[] basaltWallIDs = WallObject.registerWallObjects("basalt", "basaltwall", 7.0F, new Color(58, 69, 104), -1.0F, -1.0F);
      WallObject basaltWall = (WallObject)getObject(basaltWallIDs[0]);
      int[] deepSwampstoneWallIDs = WallObject.registerWallObjects("deepswampstone", "deepswampstonewall", 8.0F, new Color(123, 141, 70), -1.0F, -1.0F);
      WallObject deepSwampstoneWall = (WallObject)getObject(deepSwampstoneWallIDs[0]);
      int[] deepSandstoneWallIDs = WallObject.registerWallObjects("deepsandstone", "deepsandstonewall", 9.0F, new Color(124, 104, 70), -1.0F, -1.0F);
      WallObject deepSandstoneWall = (WallObject)getObject(deepSandstoneWallIDs[0]);
      int[] cryptWallIDs = WallObject.registerWallObjects(
         "crypt", "cryptwall", "cryptwalloutlines", 10.0F, new Color(83, 89, 115), ToolType.PICKAXE, -1.0F, -1.0F
      );
      WallObject cryptWall = (WallObject)getObject(cryptWallIDs[0]);
      int[] spiderCastleWallIDs = WallObject.registerWallObjects(
         "spidercastle", "spidercastlewall", 10.0F, new Color(86, 80, 111), ToolType.PICKAXE, -1.0F, -1.0F
      );
      WallObject spiderCastleWall = (WallObject)getObject(spiderCastleWallIDs[0]);
      int[] dawnWallIDs = WallObject.registerWallObjects("dawn", "dawnwall", 10.0F, new Color(216, 159, 117), ToolType.PICKAXE, 5.0F, 10.0F);
      WallObject dawnWall = (WallObject)getObject(dawnWallIDs[0]);
      int[] duskWallIDs = WallObject.registerWallObjects("dusk", "duskwall", 10.0F, new Color(50, 53, 88), ToolType.PICKAXE, 5.0F, 10.0F);
      WallObject duskWall = (WallObject)getObject(duskWallIDs[0]);
      int[] ancientRuinWallIDs = WallObject.registerWallObjects("ancientruin", "ancientruinwall", 10.0F, new Color(102, 83, 76), ToolType.PICKAXE, -1.0F, -1.0F);
      WallObject ancientRuinWall = (WallObject)getObject(ancientRuinWallIDs[0]);
      int[] ravenWallIDs = WallObject.registerWallObjects("raven", "ravenwall", 10.0F, new Color(109, 53, 150), ToolType.PICKAXE, -1.0F, -1.0F);
      WallObject ravenWall = (WallObject)getObject(ravenWallIDs[0]);
      int[] arcanicWallIDs = WallObject.registerWallObjects("arcanic", "arcanicwall", 10.0F, new Color(163, 199, 196), ToolType.PICKAXE, -1.0F, -1.0F);
      WallObject arcanicWall = (WallObject)getObject(arcanicWallIDs[0]);
      int[] factoryWallIDs = WallObject.registerWallObjects("factory", "factorywall", 10.0F, new Color(105, 103, 134), ToolType.PICKAXE, -1.0F, -1.0F);
      WallObject factoryWall = (WallObject)getObject(factoryWallIDs[0]);
      registerObject("wallwindow", new WallWindowPlaceObject(), -1.0F, true);
      int woodFenceID = registerObject("woodfence", new FenceObject("woodfence", new Color(93, 67, 24), 12, 10, -24), 2.0F, true);
      FenceGateObject.registerGatePair(woodFenceID, "woodfencegate", "woodfencegate", new Color(93, 67, 24), 12, 10, 4.0F);
      int stoneFenceID = registerObject("stonefence", new FenceObject("stonefence", new Color(115, 115, 127), 16, 14, -24), 2.0F, true);
      FenceGateObject.registerGatePair(stoneFenceID, "stonefencegate", "stonefencegate", new Color(115, 115, 127), 16, 14, 4.0F);
      int ironFenceID = registerObject("ironfence", new FenceObject("ironfence", new Color(130, 139, 152), 12, 10, -26), 2.0F, true);
      FenceGateObject.registerGatePair(ironFenceID, "ironfencegate", "ironfencegate", new Color(130, 139, 152), 12, 10, 4.0F);
      int cryptFenceID = registerObject("cryptfence", new FenceObject("cryptfence", new Color(83, 89, 115), 12, 10, -26), 2.0F, true);
      FenceGateObject.registerGatePair(cryptFenceID, "cryptfencegate", "cryptfencegate", new Color(83, 89, 115), 12, 10, 4.0F);
      int forestHedgeFenceID = registerObject("foresthedge", new FenceObject("foresthedge", new Color(125, 164, 45), 12, 10, -26), 2.0F, true);
      FenceGateObject.registerGatePair(forestHedgeFenceID, "foresthedgegate", "foresthedgegate", new Color(125, 164, 45), 12, 10, 4.0F);
      int snowHedgeFenceID = registerObject("snowhedge", new FenceObject("snowhedge", new Color(143, 192, 185), 12, 10, -26), 2.0F, true);
      FenceGateObject.registerGatePair(snowHedgeFenceID, "snowhedgegate", "snowhedgegate", new Color(143, 192, 185), 12, 10, 4.0F);
      int plainsHedgeFenceID = registerObject("plainshedge", new FenceObject("plainshedge", new Color(210, 161, 42), 12, 10, -26), 2.0F, true);
      FenceGateObject.registerGatePair(plainsHedgeFenceID, "plainshedgegate", "plainshedgegate", new Color(210, 161, 42), 12, 10, 4.0F);
      int swampHedgeFenceID = registerObject("swamphedge", new FenceObject("swamphedge", new Color(88, 110, 46), 12, 10, -26), 2.0F, true);
      FenceGateObject.registerGatePair(swampHedgeFenceID, "swamphedgegate", "swamphedgegate", new Color(88, 110, 46), 12, 10, 4.0F);
      int banditFenceID = registerObject("banditfence", new FenceObject("banditfence", new Color(93, 67, 24), 12, 10, -26), 2.0F, true);
      FenceGateObject.registerGatePair(banditFenceID, "banditfencegate", "banditfencegate", new Color(93, 67, 24), 12, 10, 4.0F);
      int steelFenceID = registerObject("jailfence", new FenceObject("jailfence", new Color(67, 69, 73), 12, 10, -26), 2.0F, true);
      FenceGateObject.registerGatePair(steelFenceID, "jailfencegate", "jailfencegate", new Color(67, 69, 73), 12, 10, 4.0F);
      registerObject("woodcolumn", new ColumnObject("woodcolumn", woodWall.mapColor, ToolType.ALL), 10.0F, true);
      registerObject("stonecolumn", new ColumnObject("stonecolumn", stoneWall.mapColor, ToolType.PICKAXE), 2.0F, true);
      registerObject("snowstonecolumn", new ColumnObject("snowstonecolumn", snowstoneWall.mapColor, ToolType.PICKAXE), 2.0F, true);
      registerObject("granitecolumn", new ColumnObject("granitecolumn", snowstoneWall.mapColor, ToolType.PICKAXE), 2.0F, true);
      registerObject("sandstonecolumn", new ColumnObject("sandstonecolumn", sandstoneWall.mapColor, ToolType.PICKAXE), 2.0F, true);
      registerObject("swampstonecolumn", new ColumnObject("swampstonecolumn", swampstoneWall.mapColor, ToolType.PICKAXE), 2.0F, true);
      registerObject("deepstonecolumn", new ColumnObject("deepstonecolumn", deepstoneWall.mapColor, ToolType.PICKAXE), 2.0F, true);
      registerObject("obsidiancolumn", new ColumnObject("obsidiancolumn", obsidianWall.mapColor, ToolType.PICKAXE), 2.0F, true);
      registerObject("deepsnowstonecolumn", new ColumnObject("deepsnowstonecolumn", deepSnowstoneWall.mapColor, ToolType.PICKAXE), 2.0F, true);
      registerObject("basaltcolumn", new ColumnObject("basaltcolumn", basaltWall.mapColor, ToolType.PICKAXE), 2.0F, true);
      registerObject("deepswampstonecolumn", new ColumnObject("deepswampstonecolumn", deepSwampstoneWall.mapColor, ToolType.PICKAXE), 2.0F, true);
      registerObject("deepsandstonecolumn", new ColumnObject("deepsandstonecolumn", deepSandstoneWall.mapColor, ToolType.PICKAXE), 2.0F, true);
      registerObject("cryptcolumn", new ColumnObject("cryptcolumn", cryptWall.mapColor, ToolType.PICKAXE), 2.0F, true);
      registerObject("spidercastlecolumn", new ColumnObject("spidercastlecolumn", cryptWall.mapColor, ToolType.PICKAXE), 2.0F, true);
      registerObject("arcaniccolumn", new ColumnObject("arcaniccolumn", new Color(120, 217, 202), ToolType.PICKAXE), -1.0F, true);
      registerObject("storagebox", new StorageBoxInventoryObject("storagebox", 40, woodFurniture).setItemCategory("objects", "furniture", "misc"), 10.0F, true);
      registerObject(
         "barrel",
         new InventoryObject("barrel", 40, new Rectangle(4, 4, 24, 24), woodFurniture)
            .setItemCategory("objects", "misc")
            .setCraftingCategory("objects", "misc"),
         10.0F,
         true
      );
      registerObject(
         "demonchest",
         new StorageBoxInventoryObject("demonchest", 40, new Color(97, 95, 132)).setItemCategory("objects", "misc").setCraftingCategory("objects", "misc"),
         20.0F,
         true
      );
      registerObject("coolingbox", new CoolingBoxInventoryObject("coolingbox", 40, new Color(153, 121, 81)), 10.0F, true);
      registerObject("vendingmachine", new VendingMachineObject("vendingmachine", 40, new Color(153, 121, 81)), 10.0F, true);
      registerObject("incinerator", new IncineratorInventoryObject(), 50.0F, true);
      registerObject(
         "merchantsbackpack", new StorageBoxInventoryObject("merchantsbackpack", 10, new Color(190, 86, 136)).setItemCategory("objects", "misc"), 20.0F, true
      );
      registerObject("sack", new StorageBoxInventoryObject("sack", 20, new Color(124, 71, 35)).setItemCategory("objects", "misc"), -1.0F, true);
      registerObject("torch", new TorchObject("torch", new Color(255, 255, 152), 50.0F, 0.2F).setWallPlaceObjectStringID("walltorch"), 0.1F, true);
      registerObject("walltorch", new WallTorchObject().setItemDroppedStringID("torch"), 0.0F, false);
      registerObject("ironlamp", new LampObject("ironlamp", new Rectangle(), new Color(255, 255, 152), 0.0F, 0.0F), 10.0F, true);
      int goldLamp = registerObject("goldlamp", new LampObject("goldlamp", new Rectangle(), new Color(255, 255, 152), 0.0F, 0.0F), 20.0F, true);
      getObject(goldLamp).roomProperties.add("goldfurniture");
      FireChaliceObject.registerFireChalice("firechalice", "firechalice", new Color(67, 69, 73), true, true, true);
      FireChaliceObject.registerFireChalice("templefirechalice", "templefirechalice", new Color(58, 49, 29), true, true, true);
      registerObject("copperstreetlamp", new StreetlampObject(), 20.0F, true);
      registerObject("ironstreetlamp", new StreetlampObject(), 30.0F, true);
      registerObject("goldstreetlamp", new StreetlampObject(), 50.0F, true);
      registerObject("tungstenstreetlamp", new StreetlampObject(), 100.0F, true);
      registerObject("scraplamp", new StreetlampObject(), 100.0F, true);
      DoubleStreetlampObject.registerDoubleStreetlamp("copperdoublestreetlamp", "copperdoublestreetlamp", ToolType.ALL, new Color(255, 255, 152), 28.0F);
      DoubleStreetlampObject.registerDoubleStreetlamp("irondoublestreetlamp", "irondoublestreetlamp", ToolType.ALL, new Color(255, 255, 152), 42.0F);
      DoubleStreetlampObject.registerDoubleStreetlamp("golddoublestreetlamp", "golddoublestreetlamp", ToolType.ALL, new Color(255, 255, 152), 70.0F);
      DoubleStreetlampObject.registerDoubleStreetlamp("tungstendoublestreetlamp", "tungstendoublestreetlamp", ToolType.ALL, new Color(255, 255, 152), 140.0F);
      registerObject("oillantern", new TorchObject("oillantern", ToolType.ALL, new Color(255, 255, 152), 50.0F, 0.2F, false, 0), 4.0F, true);
      registerObject(
         "lantern",
         new TorchObject("lantern", ToolType.ALL, new Color(255, 255, 152), 50.0F, 0.2F, false, 0).setWallPlaceObjectStringID("walllantern"),
         20.0F,
         true
      );
      registerObject("walllantern", new WallCandleObject().setItemDroppedStringID("lantern"), 0.0F, false);
      registerObject("waterlantern", new WaterLanternObject(), 20.0F, true);
      registerObject(
         "candle", new TorchObject("candle", ToolType.ALL, new Color(255, 255, 152), 50.0F, 0.2F, true, 4).setWallPlaceObjectStringID("wallcandle"), 5.0F, true
      );
      registerObject("wallcandle", new WallCandleObject().setItemDroppedStringID("candle"), 0.0F, false);
      registerObject("stonecandlepedestal", new CandlePedestalObject(), 7.0F, true);
      registerObject("snowcandlepedestal", new CandlePedestalObject(), 7.0F, true);
      registerObject("swampcandlepedestal", new CandlePedestalObject(), 7.0F, true);
      registerObject("desertcandlepedestal", new CandlePedestalObject(), 7.0F, true);
      registerObject("tikitorch", new TikiTorchObject(), 2.0F, true);
      float basaltTorchFlameHue = 170.0F;
      TorchObject basaltTorch = new TorchObject("basalttorch", new Color(169, 209, 193), 156.0F, 0.2F);
      basaltTorch.flameHue = basaltTorchFlameHue;
      registerObject("basalttorch", basaltTorch.setWallPlaceObjectStringID("wallbasalttorch"), 0.1F, true);
      WallCandleObject basaltWallTorch = new WallCandleObject();
      basaltWallTorch.lightHue = basaltTorchFlameHue;
      registerObject("wallbasalttorch", basaltWallTorch.setItemDroppedStringID("basalttorch"), 0.0F, false);
      TorchObject arcanicLamp = new TorchObject("arcaniclamp", new Color(59, 208, 229), 156.0F, 0.45F);
      arcanicLamp.disableParticles = true;
      registerObject("arcaniclamp", arcanicLamp.setWallPlaceObjectStringID("wallarcaniclamp"), 0.1F, true);
      WallCandleObject arcanicWallLamp = new WallCandleObject();
      arcanicWallLamp.lightHue = 156.0F;
      arcanicWallLamp.lightSat = 0.45F;
      arcanicWallLamp.disableParticles = true;
      registerObject("wallarcaniclamp", arcanicWallLamp.setItemDroppedStringID("arcaniclamp"), 0.0F, false);
      registerObject("woodencandleset", new TorchObject("woodencandleset", ToolType.ALL, new Color(225, 179, 89), 50.0F, 0.2F, true, 4), -1.0F, true);
      registerObject("swampcandlestand", new TorchObject("swampcandlestand", ToolType.ALL, new Color(225, 179, 89), 50.0F, 0.2F, true, 4), -1.0F, true);
      registerObject("swampcandlestandduo", new TorchObject("swampcandlestandduo", ToolType.ALL, new Color(225, 179, 89), 50.0F, 0.2F, true, 4), -1.0F, true);
      registerObject("brazier", new BrazierObject(), -1.0F, true);
      registerObject("paintingbroken", new BrokenPaintingObject(Item.Rarity.NORMAL), 10.0F, true);
      registerObject("paintingapple", new PaintingObject(Item.Rarity.COMMON), 50.0F, true, false);
      registerObject("paintingavocado", new PaintingObject(Item.Rarity.COMMON), 50.0F, true, false);
      registerObject("paintingbanana", new PaintingObject(Item.Rarity.COMMON), 50.0F, true, false);
      registerObject("paintingabstract", new PaintingObject(Item.Rarity.COMMON), 50.0F, true, false);
      registerObject("paintingrainsun", new PaintingObject(Item.Rarity.COMMON), 50.0F, true, false);
      registerObject("paintingheart", new PaintingObject(Item.Rarity.UNCOMMON), 80.0F, true, false);
      registerObject("paintingdagger", new PaintingObject(Item.Rarity.UNCOMMON), 80.0F, true, false);
      registerObject("paintingeye", new PaintingObject(Item.Rarity.UNCOMMON), 80.0F, true, false);
      registerObject("paintingmouse", new PaintingObject(Item.Rarity.UNCOMMON), 80.0F, true, false);
      registerObject("paintingparrot", new PaintingObject(Item.Rarity.UNCOMMON), 80.0F, true, false);
      registerObject("paintingduck", new PaintingObject(Item.Rarity.UNCOMMON), 80.0F, true, false);
      registerObject("paintingcastle", new PaintingObject(Item.Rarity.UNCOMMON), 80.0F, true, false);
      registerObject("paintingstonecaveling", new PaintingObject(Item.Rarity.RARE), 155.0F, true, false);
      registerObject("paintingsnowcaveling", new PaintingObject(Item.Rarity.RARE), 155.0F, true, false);
      registerObject("paintingswampcaveling", new PaintingObject(Item.Rarity.RARE), 155.0F, true, false);
      registerObject("paintingsandstonecaveling", new PaintingObject(Item.Rarity.RARE), 155.0F, true, false);
      LargePaintingObject.registerLargePainting("paintinglargeworldmap", Item.Rarity.RARE, 235.0F, true, false);
      LargePaintingObject.registerLargePainting("paintinglargeship", Item.Rarity.RARE, 235.0F, true, false);
      LargePaintingObject.registerLargePainting("paintinglargecastle", Item.Rarity.RARE, 235.0F, true, false);
      LargePaintingObject.registerLargePainting("paintinglargeabstract", Item.Rarity.RARE, 235.0F, true, false);
      registerObject("paintingelder", new PaintingObject(Item.Rarity.EPIC), 675.0F, true, false);
      registerObject("paintingcooljonas", new PaintingObject(Item.Rarity.EPIC), 675.0F, true, false);
      registerObject("paintingposter", new PaintingObject(Item.Rarity.COMMON), 60.0F, true, false);
      int[] flatgrassPaintingIDs = LargePaintingObject.registerLargePainting("paintinglargeflatgrass", Item.Rarity.RARE, 800.0F, true, false);
      LargePaintingObject flatgrassPainting = (LargePaintingObject)getObject(flatgrassPaintingIDs[0]);
      flatgrassPainting.tooltipSupplier = () -> GameUtils.getDevQuoteTooltip("itemtooltip", "flatgrasspaintingquote", "Fair");
      registerObject("mossymonkstatue", new StatueObject("mossymonkstatue", 16, 4), -1.0F, true);
      registerObject("angelicstatue", new StatueObject("angelicstatue"), -1.0F, true);
      registerObject("hoodedknightstatue", new StatueObject("hoodedknightstatue"), -1.0F, true);
      registerObject("ravenstatue", new StatueObject("ravenstatue", 16, 1), -1.0F, true);
      AncientPillarObject.registerAncientPillar("ancientpillar", true);
      AncientMummyStatueObject.registerAncientMummyStatue("ancientmummystatue", true);
      RavenEffigyObject.registerRavenEffigy("raveneffigy", true);
      PigStatueObject.registerPigStatue("pigstatue", true);
      BlacksmithStatueObject.registerBlacksmithStatue("blacksmithstatue", true);
      WitchStatueObject.registerWitchStatue("witchstatue", true);
      FreyaStatueObject.registerFreyaStatue("freyastatue", true);
      ZithaStatueObject.registerZithaStatue("zithastatue", true);
      FreyaAndZithaStatueObject.registerFreyaAndZithaStatue("freyaandzithastatue", true);
      HuginStatueObject.registerHuginStatue("huginstatue", false);
      TheCoreStatueObject.registerTheCoreStatue("thecore", false);
      PraiseTheMoonStatueObject.registerPraiseTheMoonStatue("praisethemoonstatue", true, true);
      registerObject("sign", new SignObject(), 10.0F, true);
      registerObject("flowerpot", new FlowerPotObject(), 10.0F, true);
      registerObject("armorstand", new ArmorStandObject(), 10.0F, true);
      registerObject("trainingdummy", new TrainingDummyObject(), 60.0F, true);
      registerObject("snowmantrainingdummy", new SnowManTrainingDummyObject(), 60.0F, true);
      CoffinObject.registerCoffinObject("stonecoffin", "stonecoffin", "stone", ToolType.PICKAXE, new Color(130, 139, 152), -1.0F, true);
      CoffinObject.registerCoffinObject("basaltcoffin", "basaltcoffin", "basalt", ToolType.PICKAXE, new Color(130, 139, 152), -1.0F, true);
      CoffinObject.registerCoffinObject("cryptcoffin", "cryptcoffin", "cryptstone", ToolType.PICKAXE, new Color(91, 92, 105), -1.0F, true);
      registerObject("gravestone1", new GravestoneObject("gravestone1", ToolType.PICKAXE, new Color(130, 139, 152)), -1.0F, true);
      registerObject("gravestone2", new GravestoneObject("gravestone2", ToolType.PICKAXE, new Color(130, 139, 152)), -1.0F, true);
      registerObject("cryptgravestone1", new GravestoneObject("cryptgravestone1", ToolType.PICKAXE, new Color(91, 92, 105)), -1.0F, true);
      registerObject("cryptgravestone2", new GravestoneObject("cryptgravestone2", ToolType.PICKAXE, new Color(91, 92, 105)), -1.0F, true);
      registerObject("minecarttrack", new MinecartTrackObject(), 0.5F, true);
      registerObject("traptrack", new TrapTrackObject(), 0.5F, false, false, true);
      registerObject("spideregg", new SpiderEggObject(), -1.0F, true);
      FountainObject.registerFountain("fountain", true, true);
      FountainObject.registerFountain("bloodfountain", true, true);
      OvergrownEnchantedFountainObject.registerFountain("overgrownfountain", true);
      registerObject("chieftainsthrone", new ChieftainsThroneChairObject("chieftainsthrone", new Color(211, 78, 39)), 300.0F, false);
      registerObject("bloodstains", new BloodStainsObject("bloodstains", new Color(103, 9, 9), ToolType.ALL), 5.0F, true);
      BigTentObject.registerTent("bigtent", true);
      PicnicBlanketObject.registerPicnicBlanketObject("picnicblanket", "picnicblanket", ToolType.ALL, new Color(229, 122, 234), -1.0F, true);
      registerObject("steppingstones", new SteppingStonesObject("steppingstones", new Color(119, 115, 115), ToolType.ALL), -1.0F, true);
      registerObject("largesteppingstone", new SteppingStonesObject("largesteppingstone", new Color(119, 115, 115), ToolType.ALL), -1.0F, true);
      registerObject("mossysteppingstones", new SteppingStonesObject("mossysteppingstones", new Color(76, 112, 76), ToolType.ALL), -1.0F, true);
      registerObject("largemossysteppingstone", new SteppingStonesObject("largemossysteppingstone", new Color(76, 112, 76), ToolType.ALL), -1.0F, true);
      Color oakMapColor = new Color(113, 67, 17);
      String[] oakCategory = new String[]{"objects", "furniture", "oak"};
      registerObject("oakchest", new StorageBoxInventoryObject("oakchest", 40, oakMapColor, oakCategory), 10.0F, true);
      DinnerTableObject.registerDinnerTable("oakdinnertable", "oakdinnertable", oakMapColor, 20.0F, oakCategory);
      registerObject("oakdesk", new DeskObject("oakdesk", oakMapColor, oakCategory), 10.0F, true);
      registerObject("oakmodulartable", new ModularTableObject("oakmodulartable", oakMapColor, oakCategory), 10.0F, true);
      registerObject("oakchair", new ChairObject("oakchair", oakMapColor, oakCategory), 5.0F, true);
      BenchObject.registerBench("oakbench", "oakbench", oakMapColor, 10.0F, oakCategory);
      registerObject("oakbookshelf", new BookshelfObject("oakbookshelf", oakMapColor, oakCategory), 10.0F, true);
      registerObject("oakcabinet", new CabinetObject("oakcabinet", oakMapColor, oakCategory), 10.0F, true);
      BedObject.registerBed("oakbed", "oakbed", oakMapColor, 100.0F, oakCategory);
      DoubleBedBaseObject.registerDoubleBed("oakdoublebed", "oakdoublebed", oakMapColor, 150.0F, oakCategory);
      registerObject("oakdresser", new DresserObject("oakdresser", oakMapColor, oakCategory), 10.0F, true);
      registerObject("oakclock", new ClockObject("oakclock", oakMapColor, oakCategory), 10.0F, true);
      registerObject("oakcandelabra", new CandelabraObject("oakcandelabra", oakMapColor, 50.0F, 0.1F, oakCategory), 10.0F, true);
      registerObject("oakdisplay", new DisplayStandObject("oakdisplay", oakMapColor, 20, oakCategory), 10.0F, true);
      BathtubObject.registerBathtub("oakbathtub", "oakbathtub", oakMapColor, 10.0F, oakCategory);
      registerObject("oaktoilet", new ToiletObject("oaktoilet", oakMapColor, oakCategory), 5.0F, true);
      oakMapColor = new Color(148, 112, 53);
      oakCategory = new String[]{"objects", "furniture", "spruce"};
      registerObject("sprucechest", new StorageBoxInventoryObject("sprucechest", 40, oakMapColor, oakCategory), 10.0F, true);
      DinnerTableObject.registerDinnerTable("sprucedinnertable", "sprucedinnertable", oakMapColor, 20.0F, oakCategory);
      registerObject("sprucedesk", new DeskObject("sprucedesk", oakMapColor, oakCategory), 10.0F, true);
      registerObject("sprucemodulartable", new ModularTableObject("sprucemodulartable", oakMapColor, oakCategory), 10.0F, true);
      registerObject("sprucechair", new ChairObject("sprucechair", oakMapColor, oakCategory), 5.0F, true);
      BenchObject.registerBench("sprucebench", "sprucebench", oakMapColor, 10.0F, oakCategory);
      registerObject("sprucebookshelf", new BookshelfObject("sprucebookshelf", oakMapColor, oakCategory), 10.0F, true);
      registerObject("sprucecabinet", new CabinetObject("sprucecabinet", oakMapColor, oakCategory), 10.0F, true);
      BedObject.registerBed("sprucebed", "sprucebed", oakMapColor, 100.0F, oakCategory);
      DoubleBedBaseObject.registerDoubleBed("sprucedoublebed", "sprucedoublebed", oakMapColor, 200.0F, oakCategory);
      registerObject("sprucedresser", new DresserObject("sprucedresser", oakMapColor, oakCategory), 10.0F, true);
      registerObject("spruceclock", new ClockObject("spruceclock", oakMapColor, oakCategory), 10.0F, true);
      registerObject("sprucecandelabra", new CandelabraObject("sprucecandelabra", oakMapColor, 50.0F, 0.1F, oakCategory), 10.0F, true);
      registerObject("sprucedisplay", new DisplayStandObject("sprucedisplay", oakMapColor, 20, oakCategory), 10.0F, true);
      BathtubObject.registerBathtub("sprucebathtub", "sprucebathtub", oakMapColor, 10.0F, oakCategory);
      registerObject("sprucetoilet", new ToiletObject("sprucetoilet", oakMapColor, oakCategory), 5.0F, true);
      oakMapColor = new Color(137, 96, 23);
      oakCategory = new String[]{"objects", "furniture", "pine"};
      registerObject("pinechest", new StorageBoxInventoryObject("pinechest", 40, oakMapColor, oakCategory), 10.0F, true);
      DinnerTableObject.registerDinnerTable("pinedinnertable", "pinedinnertable", oakMapColor, 20.0F, oakCategory);
      registerObject("pinedesk", new DeskObject("pinedesk", oakMapColor, oakCategory), 10.0F, true);
      registerObject("pinemodulartable", new ModularTableObject("pinemodulartable", oakMapColor, oakCategory), 10.0F, true);
      registerObject("pinechair", new ChairObject("pinechair", oakMapColor, oakCategory), 5.0F, true);
      BenchObject.registerBench("pinebench", "pinebench", oakMapColor, 10.0F, oakCategory);
      registerObject("pinebookshelf", new BookshelfObject("pinebookshelf", oakMapColor, oakCategory), 10.0F, true);
      registerObject("pinecabinet", new CabinetObject("pinecabinet", oakMapColor, oakCategory), 10.0F, true);
      BedObject.registerBed("pinebed", "pinebed", oakMapColor, 100.0F, oakCategory);
      DoubleBedBaseObject.registerDoubleBed("pinedoublebed", "pinedoublebed", oakMapColor, 150.0F, oakCategory);
      registerObject("pinedresser", new DresserObject("pinedresser", oakMapColor, oakCategory), 10.0F, true);
      registerObject("pineclock", new ClockObject("pineclock", oakMapColor, oakCategory), 10.0F, true);
      registerObject("pinecandelabra", new CandelabraObject("pinecandelabra", oakMapColor, 50.0F, 0.1F, oakCategory), 10.0F, true);
      registerObject("pinedisplay", new DisplayStandObject("pinedisplay", oakMapColor, 20, oakCategory), 10.0F, true);
      BathtubObject.registerBathtub("pinebathtub", "pinebathtub", oakMapColor, 10.0F, oakCategory);
      registerObject("pinetoilet", new ToiletObject("pinetoilet", oakMapColor, oakCategory), 5.0F, true);
      oakMapColor = new Color(136, 71, 60);
      oakCategory = new String[]{"objects", "furniture", "willow"};
      registerObject("willowchest", new StorageBoxInventoryObject("willowchest", 40, oakMapColor, oakCategory), 10.0F, true);
      DinnerTableObject.registerDinnerTable("willowdinnertable", "willowdinnertable", oakMapColor, 20.0F, oakCategory);
      registerObject("willowdesk", new DeskObject("willowdesk", oakMapColor, oakCategory), 10.0F, true);
      registerObject("willowmodulartable", new ModularTableObject("willowmodulartable", oakMapColor, oakCategory), 10.0F, true);
      registerObject("willowchair", new ChairObject("willowchair", oakMapColor, oakCategory), 5.0F, true);
      BenchObject.registerBench("willowbench", "willowbench", oakMapColor, 10.0F, oakCategory);
      registerObject("willowbookshelf", new BookshelfObject("willowbookshelf", oakMapColor, oakCategory), 10.0F, true);
      registerObject("willowcabinet", new CabinetObject("willowcabinet", oakMapColor, oakCategory), 10.0F, true);
      BedObject.registerBed("willowbed", "willowbed", oakMapColor, 100.0F, oakCategory);
      DoubleBedBaseObject.registerDoubleBed("willowdoublebed", "willowdoublebed", oakMapColor, 150.0F, oakCategory);
      registerObject("willowdresser", new DresserObject("willowdresser", oakMapColor, oakCategory), 10.0F, true);
      registerObject("willowclock", new ClockObject("willowclock", oakMapColor, oakCategory), 10.0F, true);
      registerObject("willowcandelabra", new CandelabraObject("willowcandelabra", oakMapColor, 50.0F, 0.1F, oakCategory), 10.0F, true);
      registerObject("willowdisplay", new DisplayStandObject("willowdisplay", oakMapColor, 20, oakCategory), 10.0F, true);
      BathtubObject.registerBathtub("willowbathtub", "willowbathtub", oakMapColor, 10.0F, oakCategory);
      registerObject("willowtoilet", new ToiletObject("willowtoilet", oakMapColor, oakCategory), 5.0F, true);
      oakMapColor = new Color(121, 90, 37);
      oakCategory = new String[]{"objects", "furniture", "palm"};
      registerObject("palmchest", new StorageBoxInventoryObject("palmchest", 40, oakMapColor, oakCategory), 10.0F, true);
      DinnerTableObject.registerDinnerTable("palmdinnertable", "palmdinnertable", oakMapColor, 20.0F, oakCategory);
      registerObject("palmdesk", new DeskObject("palmdesk", oakMapColor, oakCategory), 10.0F, true);
      registerObject("palmmodulartable", new ModularTableObject("palmmodulartable", oakMapColor, oakCategory), 10.0F, true);
      registerObject("palmchair", new ChairObject("palmchair", oakMapColor, oakCategory), 5.0F, true);
      BenchObject.registerBench("palmbench", "palmbench", oakMapColor, 10.0F, oakCategory);
      registerObject("palmbookshelf", new BookshelfObject("palmbookshelf", oakMapColor, oakCategory), 10.0F, true);
      registerObject("palmcabinet", new CabinetObject("palmcabinet", oakMapColor, oakCategory), 10.0F, true);
      BedObject.registerBed("palmbed", "palmbed", oakMapColor, 100.0F, oakCategory);
      DoubleBedBaseObject.registerDoubleBed("palmdoublebed", "palmdoublebed", oakMapColor, 150.0F, oakCategory);
      registerObject("palmdresser", new DresserObject("palmdresser", oakMapColor, oakCategory), 10.0F, true);
      registerObject("palmclock", new ClockObject("palmclock", oakMapColor, oakCategory), 10.0F, true);
      registerObject("palmcandelabra", new CandelabraObject("palmcandelabra", oakMapColor, 50.0F, 0.1F, oakCategory), 10.0F, true);
      registerObject("palmdisplay", new DisplayStandObject("palmdisplay", oakMapColor, 20, oakCategory), 10.0F, true);
      BathtubObject.registerBathtub("palmbathtub", "palmbathtub", oakMapColor, 10.0F, oakCategory);
      registerObject("palmtoilet", new ToiletObject("palmtoilet", oakMapColor, oakCategory), 5.0F, true);
      oakMapColor = new Color(96, 45, 8);
      oakCategory = new String[]{"objects", "furniture", "maple"};
      registerObject("maplechest", new StorageBoxInventoryObject("maplechest", 40, oakMapColor, oakCategory), 10.0F, true);
      DinnerTableObject.registerDinnerTable("mapledinnertable", "mapledinnertable", oakMapColor, 20.0F, oakCategory);
      registerObject("mapledesk", new DeskObject("mapledesk", oakMapColor, oakCategory), 10.0F, true);
      registerObject("maplemodulartable", new ModularTableObject("maplemodulartable", oakMapColor, oakCategory), 10.0F, true);
      registerObject("maplechair", new ChairObject("maplechair", oakMapColor, oakCategory), 5.0F, true);
      BenchObject.registerBench("maplebench", "maplebench", oakMapColor, 10.0F, oakCategory);
      registerObject("maplebookshelf", new BookshelfObject("maplebookshelf", oakMapColor, oakCategory), 10.0F, true);
      registerObject("maplecabinet", new CabinetObject("maplecabinet", oakMapColor, oakCategory), 10.0F, true);
      BedObject.registerBed("maplebed", "maplebed", oakMapColor, 100.0F, oakCategory);
      DoubleBedBaseObject.registerDoubleBed("mapledoublebed", "mapledoublebed", oakMapColor, 150.0F, oakCategory);
      registerObject("mapledresser", new DresserObject("mapledresser", oakMapColor, oakCategory), 10.0F, true);
      registerObject("mapleclock", new ClockObject("mapleclock", oakMapColor, oakCategory), 10.0F, true);
      registerObject("maplecandelabra", new CandelabraObject("maplecandelabra", oakMapColor, 50.0F, 0.1F, oakCategory), 10.0F, true);
      registerObject("mapledisplay", new DisplayStandObject("mapledisplay", oakMapColor, 20, oakCategory), 10.0F, true);
      BathtubObject.registerBathtub("maplebathtub", "maplebathtub", oakMapColor, 10.0F, oakCategory);
      registerObject("mapletoilet", new ToiletObject("mapletoilet", oakMapColor, oakCategory), 5.0F, true);
      oakMapColor = new Color(174, 161, 137);
      oakCategory = new String[]{"objects", "furniture", "birch"};
      registerObject("birchchest", new StorageBoxInventoryObject("birchchest", 40, oakMapColor, oakCategory), 10.0F, true);
      DinnerTableObject.registerDinnerTable("birchdinnertable", "birchdinnertable", oakMapColor, 20.0F, oakCategory);
      registerObject("birchdesk", new DeskObject("birchdesk", oakMapColor, oakCategory), 10.0F, true);
      registerObject("birchmodulartable", new ModularTableObject("birchmodulartable", oakMapColor, oakCategory), 10.0F, true);
      registerObject("birchchair", new ChairObject("birchchair", oakMapColor, oakCategory), 5.0F, true);
      BenchObject.registerBench("birchbench", "birchbench", oakMapColor, 10.0F, oakCategory);
      registerObject("birchbookshelf", new BookshelfObject("birchbookshelf", oakMapColor, oakCategory), 10.0F, true);
      registerObject("birchcabinet", new CabinetObject("birchcabinet", oakMapColor, oakCategory), 10.0F, true);
      BedObject.registerBed("birchbed", "birchbed", oakMapColor, 100.0F, oakCategory);
      DoubleBedBaseObject.registerDoubleBed("birchdoublebed", "birchdoublebed", oakMapColor, 150.0F, oakCategory);
      registerObject("birchdresser", new DresserObject("birchdresser", oakMapColor, oakCategory), 10.0F, true);
      registerObject("birchclock", new ClockObject("birchclock", oakMapColor, oakCategory), 10.0F, true);
      registerObject("birchcandelabra", new CandelabraObject("birchcandelabra", oakMapColor, 50.0F, 0.1F, oakCategory), 10.0F, true);
      registerObject("birchdisplay", new DisplayStandObject("birchdisplay", oakMapColor, 20, oakCategory), 10.0F, true);
      BathtubObject.registerBathtub("birchbathtub", "birchbathtub", oakMapColor, 10.0F, oakCategory);
      registerObject("birchtoilet", new ToiletObject("birchtoilet", oakMapColor, oakCategory), 5.0F, true);
      oakMapColor = new Color(109, 68, 29);
      oakCategory = new String[]{"objects", "furniture", "dungeon"};
      registerObject("dungeonchest", new StorageBoxInventoryObject("dungeonchest", 40, oakMapColor, oakCategory), 10.0F, true);
      DinnerTableObject.registerDinnerTable("dungeondinnertable", "dungeondinnertable", oakMapColor, 20.0F, oakCategory);
      registerObject("dungeondesk", new DeskObject("dungeondesk", oakMapColor, oakCategory), 10.0F, true);
      registerObject("dungeonmodulartable", new ModularTableObject("dungeonmodulartable", oakMapColor, oakCategory), 10.0F, true);
      registerObject("dungeonchair", new DungeonChairObject("dungeonchair", oakMapColor, oakCategory), 5.0F, true);
      BenchObject.registerBench("dungeonbench", "dungeonbench", oakMapColor, 10.0F, oakCategory);
      registerObject("dungeonbookshelf", new BookshelfObject("dungeonbookshelf", oakMapColor, oakCategory), 10.0F, true);
      registerObject("dungeoncabinet", new CabinetObject("dungeoncabinet", oakMapColor, oakCategory), 10.0F, true);
      BedObject.registerBed("dungeonbed", "dungeonbed", oakMapColor, 100.0F, oakCategory);
      DoubleBedBaseObject.registerDoubleBed("dungeondoublebed", "dungeondoublebed", oakMapColor, 150.0F, oakCategory);
      registerObject("dungeondresser", new DresserObject("dungeondresser", oakMapColor, oakCategory), 10.0F, true);
      registerObject("dungeonclock", new ClockObject("dungeonclock", oakMapColor, oakCategory), 10.0F, true);
      CandelabraObject dungeonCandelabra = new CandelabraObject("dungeoncandelabra", oakMapColor, 270.0F, 0.4F, oakCategory);
      dungeonCandelabra.flameHue = dungeonCandelabra.lightHue;
      dungeonCandelabra.smokeHue = dungeonCandelabra.lightHue;
      registerObject("dungeoncandelabra", dungeonCandelabra, 10.0F, true);
      registerObject("dungeondisplay", new DisplayStandObject("dungeondisplay", oakMapColor, 20, oakCategory), 10.0F, true);
      BathtubObject.registerBathtub("dungeonbathtub", "dungeonbathtub", oakMapColor, 10.0F, oakCategory);
      registerObject("dungeontoilet", new ToiletObject("dungeontoilet", oakMapColor, oakCategory), 5.0F, true);
      oakMapColor = new Color(191, 192, 166);
      oakCategory = new String[]{"objects", "furniture", "bone"};
      registerObject("bonechest", new StorageBoxInventoryObject("bonechest", 40, oakMapColor, oakCategory), 10.0F, true);
      DinnerTableObject.registerDinnerTable("bonedinnertable", "bonedinnertable", oakMapColor, 20.0F, oakCategory);
      registerObject("bonedesk", new DeskObject("bonedesk", oakMapColor, oakCategory), 10.0F, true);
      registerObject("bonemodulartable", new ModularTableObject("bonemodulartable", oakMapColor, oakCategory), 10.0F, true);
      registerObject("bonechair", new ChairObject("bonechair", oakMapColor, oakCategory), 5.0F, true);
      BenchObject.registerBench("bonebench", "bonebench", oakMapColor, 10.0F, oakCategory);
      registerObject("bonebookshelf", new BookshelfObject("bonebookshelf", oakMapColor, oakCategory), 10.0F, true);
      registerObject("bonecabinet", new CabinetObject("bonecabinet", oakMapColor, oakCategory), 10.0F, true);
      BedObject.registerBed("bonebed", "bonebed", oakMapColor, 100.0F, oakCategory);
      DoubleBedBaseObject.registerDoubleBed("bonedoublebed", "bonedoublebed", oakMapColor, 150.0F, oakCategory);
      registerObject("bonedresser", new DresserObject("bonedresser", oakMapColor, oakCategory), 10.0F, true);
      registerObject("boneclock", new ClockObject("boneclock", oakMapColor, oakCategory), 10.0F, true);
      dungeonCandelabra = new CandelabraObject("bonecandelabra", oakMapColor, 50.0F, 0.1F, oakCategory);
      registerObject("bonecandelabra", dungeonCandelabra, 10.0F, true);
      registerObject("bonedisplay", new DisplayStandObject("bonedisplay", oakMapColor, 20, oakCategory), 10.0F, true);
      BathtubObject.registerBathtub("bonebathtub", "bonebathtub", oakMapColor, 10.0F, oakCategory);
      registerObject("bonetoilet", new ToiletObject("bonetoilet", oakMapColor, oakCategory), 5.0F, true);
      oakMapColor = new Color(157, 107, 62);
      oakCategory = new String[]{"objects", "furniture", "dryad"};
      registerObject("dryadchest", new StorageBoxInventoryObject("dryadchest", 40, oakMapColor, oakCategory), -1.0F, true);
      DinnerTableObject.registerDinnerTable("dryaddinnertable", "dryaddinnertable", oakMapColor, -1.0F, oakCategory);
      registerObject("dryaddesk", new DeskObject("dryaddesk", oakMapColor, oakCategory), -1.0F, true);
      registerObject("dryadmodulartable", new ModularTableObject("dryadmodulartable", oakMapColor, oakCategory), -1.0F, true);
      registerObject("dryadchair", new ChairObject("dryadchair", oakMapColor, oakCategory), -1.0F, true);
      BenchObject.registerBench("dryadbench", "dryadbench", oakMapColor, -1.0F, oakCategory);
      registerObject("dryadbookshelf", new BookshelfObject("dryadbookshelf", oakMapColor, oakCategory), -1.0F, true);
      registerObject("dryadcabinet", new CabinetObject("dryadcabinet", oakMapColor, oakCategory), -1.0F, true);
      BedObject.registerBed("dryadbed", "dryadbed", oakMapColor, -1.0F, oakCategory);
      DoubleBedBaseObject.registerDoubleBed("dryaddoublebed", "dryaddoublebed", oakMapColor, -1.0F, oakCategory);
      registerObject("dryaddresser", new DresserObject("dryaddresser", oakMapColor, oakCategory), -1.0F, true);
      registerObject("dryadclock", new ClockObject("dryadclock", oakMapColor, oakCategory), -1.0F, true);
      dungeonCandelabra = new CandelabraObject("dryadcandelabra", oakMapColor, 50.0F, 0.1F, oakCategory);
      registerObject("dryadcandelabra", dungeonCandelabra, -1.0F, true);
      registerObject("dryaddisplay", new DisplayStandObject("dryaddisplay", oakMapColor, 20, oakCategory), -1.0F, true);
      BathtubObject.registerBathtub("dryadbathtub", "dryadbathtub", oakMapColor, -1.0F, oakCategory);
      registerObject("dryadtoilet", new ToiletObject("dryadtoilet", oakMapColor, oakCategory), -1.0F, true);
      oakMapColor = new Color(131, 71, 63);
      oakCategory = new String[]{"objects", "furniture", "bamboo"};
      registerObject("bamboochest", new StorageBoxInventoryObject("bamboochest", 40, oakMapColor, oakCategory), -1.0F, true);
      DinnerTableObject.registerDinnerTable("bamboodinnertable", "bamboodinnertable", oakMapColor, -1.0F, oakCategory);
      registerObject("bamboodesk", new DeskObject("bamboodesk", oakMapColor, oakCategory), -1.0F, true);
      registerObject("bamboomodulartable", new ModularTableObject("bamboomodulartable", oakMapColor, oakCategory), -1.0F, true);
      registerObject("bamboochair", new ChairObject("bamboochair", oakMapColor, oakCategory), -1.0F, true);
      BenchObject.registerBench("bamboobench", "bamboobench", oakMapColor, -1.0F, oakCategory);
      registerObject("bamboobookshelf", new BookshelfObject("bamboobookshelf", oakMapColor, oakCategory), -1.0F, true);
      registerObject("bamboocabinet", new CabinetObject("bamboocabinet", oakMapColor, oakCategory), -1.0F, true);
      BedObject.registerBed("bamboobed", "bamboobed", oakMapColor, -1.0F, oakCategory);
      DoubleBedBaseObject.registerDoubleBed("bamboodoublebed", "bamboodoublebed", oakMapColor, -1.0F, oakCategory);
      registerObject("bamboodresser", new DresserObject("bamboodresser", oakMapColor, oakCategory), -1.0F, true);
      registerObject("bambooclock", new ClockObject("bambooclock", oakMapColor, oakCategory), -1.0F, true);
      dungeonCandelabra = new CandelabraObject("bamboocandelabra", oakMapColor, 50.0F, 0.1F, oakCategory);
      registerObject("bamboocandelabra", dungeonCandelabra, -1.0F, true);
      registerObject("bamboodisplay", new DisplayStandObject("bamboodisplay", oakMapColor, 20, oakCategory), -1.0F, true);
      BathtubObject.registerBathtub("bamboobathtub", "bamboobathtub", oakMapColor, -1.0F, oakCategory);
      registerObject("bambootoilet", new ToiletObject("bambootoilet", oakMapColor, oakCategory), -1.0F, true);
      oakMapColor = new Color(111, 79, 97);
      oakCategory = new String[]{"objects", "furniture", "deadwood"};
      registerObject("deadwoodchest", new StorageBoxInventoryObject("deadwoodchest", 40, oakMapColor, oakCategory), 10.0F, true);
      DinnerTableObject.registerDinnerTable("deadwooddinnertable", "deadwooddinnertable", oakMapColor, 20.0F, oakCategory);
      registerObject("deadwooddesk", new DeskObject("deadwooddesk", oakMapColor, oakCategory), 10.0F, true);
      registerObject("deadwoodmodulartable", new ModularTableObject("deadwoodmodulartable", oakMapColor, oakCategory), 10.0F, true);
      registerObject("deadwoodchair", new DeadWoodChairObject("deadwoodchair", oakMapColor, oakCategory), 5.0F, true);
      BenchObject.registerBench("deadwoodbench", "deadwoodbench", oakMapColor, 10.0F, oakCategory);
      registerObject("deadwoodbookshelf", new BookshelfObject("deadwoodbookshelf", oakMapColor, oakCategory), 10.0F, true);
      registerObject("deadwoodcabinet", new CabinetObject("deadwoodcabinet", oakMapColor, oakCategory), 10.0F, true);
      BedObject.registerBed("deadwoodbed", "deadwoodbed", oakMapColor, 100.0F, oakCategory);
      DoubleBedBaseObject.registerDoubleBed("deadwooddoublebed", "deadwooddoublebed", oakMapColor, 150.0F, oakCategory);
      registerObject("deadwooddresser", new DresserObject("deadwooddresser", oakMapColor, oakCategory), 10.0F, true);
      registerObject("deadwoodclock", new ClockObject("deadwoodclock", oakMapColor, oakCategory), 10.0F, true);
      dungeonCandelabra = new CandelabraObject("deadwoodcandelabra", oakMapColor, 20.0F, 0.4F, oakCategory);
      dungeonCandelabra.flameHue = dungeonCandelabra.lightHue;
      dungeonCandelabra.smokeHue = dungeonCandelabra.lightHue;
      registerObject("deadwoodcandelabra", dungeonCandelabra, 10.0F, true);
      CandlesObject deadwoodCandles = new CandlesObject("deadwoodcandles", oakMapColor, 20.0F, 0.4F);
      deadwoodCandles.flameHue = deadwoodCandles.lightHue;
      deadwoodCandles.smokeHue = deadwoodCandles.lightHue;
      registerObject("deadwoodcandles", deadwoodCandles, 10.0F, true);
      registerObject("deadwooddisplay", new DisplayStandObject("deadwooddisplay", oakMapColor, 20, oakCategory), 10.0F, true);
      BathtubObject.registerBathtub("deadwoodbathtub", "deadwoodbathtub", oakMapColor, 10.0F, oakCategory);
      registerObject("deadwoodtoilet", new ToiletObject("deadwoodtoilet", oakMapColor, oakCategory), 5.0F, true);
      String[] miscCategory = new String[]{"objects", "furniture", "misc"};
      Color spiderCastleFurnitureMapColor = new Color(151, 84, 117);
      dungeonCandelabra = new CandelabraObject("spidercastlecandelabra", spiderCastleFurnitureMapColor, 75.0F, 0.4F);
      dungeonCandelabra.flameHue = dungeonCandelabra.lightHue;
      dungeonCandelabra.smokeHue = dungeonCandelabra.lightHue;
      registerObject("spidercastlecandelabra", dungeonCandelabra, 10.0F, true);
      registerObject("spidercastlewallcandle", new SpiderCastleWallCandleObject(), 2.0F, true);
      registerObject("spideritearmorstand", new SpideriteArmorStandObject(spiderCastleFurnitureMapColor), -1.0F, true);
      registerObject("spidercastlemodulartable", new ModularTableObject("spidercastlemodulartable", spiderCastleFurnitureMapColor, miscCategory), 10.0F, true);
      registerObject(
         "spidercastlechair", new SpiderCastleChairObject("spidercastlechair", spiderCastleFurnitureMapColor).setItemCategory(miscCategory), 5.0F, true
      );
      BenchObject.registerBench("spidercastlebench", "spidercastlebench", spiderCastleFurnitureMapColor, 10.0F, miscCategory);
      registerObject("spidercastlebookshelf", new BookshelfObject("spidercastlebookshelf", spiderCastleFurnitureMapColor, miscCategory), 10.0F, true);
      EmptyRavenNestObject.registerEmptyRavenNest("emptyravennest");
      RavenNestWithEggObject.registerRavenNestWithEgg();
      RavenSkullObject.registerRavenSkull();
      ArcanicMachinery.registerArcanicMachinery("arcanicmachinery", true, true);
      registerObject("barshelf", new BookshelfObject("barshelf", new Color(134, 75, 32), miscCategory), -1.0F, true);
      registerObject(
         "stool", new SpiderCastleChairObject("stool", new Color(206, 46, 32)).setItemCategory(miscCategory).setCraftingCategory(miscCategory), -1.0F, true
      );
      registerObject("sink", new SinkObject("sink"), -1.0F, true);
      registerObject("tungstensink", new SinkObject("tungstensink"), -1.0F, true);
      registerObject("largekeg", new KegObject("largekeg"), -1.0F, true);
      registerObject("burnedleafpile", new BurnedLeafPileObject("burnedleafpile", new Color(34, 20, 12)), -1.0F, true);
      registerObject("burnedtree", new BurnedTreeObject("burnedtree", "charredbark", new Color(34, 20, 12), 42, 70, 110, null), -1.0F, true, true);
      registerObject("burnedtreestump", new TreeStumpObject("burnedtreestump", "charredbark", new Color(34, 20, 12)), -1.0F, true);
      registerObject("burnedbush", new BurnedBushObject("burnedbush", new Color(34, 20, 12)), -1.0F, true);
      LogBenchObject.registerLogBench("burnedlogbench", "burnedlogbench", "charredbark", ToolType.AXE, new Color(34, 20, 12), -1.0F);
      oakCategory = new String[]{"objects", "landscaping", "misc"};
      RockObject charredRock;
      charredRockID = registerObject("charredforestrock", charredRock = new RockObject("rock", new Color(72, 71, 77), "stone", oakCategory), -1.0F, false);
      SingleRockObject.registerSurfaceRock(charredRock, "charredrock", new Color(130, 139, 152), -1.0F, true, oakCategory);
      registerObject("charredrocksmall", new SingleRockSmall(charredRock, "charredrocksmall", new Color(130, 139, 152), oakCategory), -1.0F, true);
      int[] goldTables = DinnerTableObject.registerDinnerTable("golddinnertable", "golddinnertable", goldFurniture, 50.0F, miscCategory);
      getObject(goldTables[0]).roomProperties.add("goldfurniture");
      getObject(goldTables[1]).roomProperties.add("goldfurniture");
      getObject(registerObject("goldchair", new ChairObject("goldchair", goldFurniture, miscCategory), 25.0F, true)).roomProperties.add("goldfurniture");
      registerObject("piratechest", new WaterproofStorageBoxInventoryObject("piratechest", 40, new Color(81, 56, 40), miscCategory), 10.0F, true);
      registerObject("mimicchest", new MimicStorageBoxInventoryObject("mimicchest", 40, woodFurniture, miscCategory), 10.0F, true);
      String[] potsCategory = new String[]{"objects", "decorations", "pots"};
      registerObject("decorativepot1", new PotTableDecorationObject("decorativepot1", new Color(66, 47, 54), 14, 12, "decorativepot", false), 20.0F, true);
      registerObject("decorativepot2", new PotTableDecorationObject("decorativepot2", new Color(66, 47, 54), 14, 12, "decorativepot", false), 20.0F, true);
      registerObject("decorativepot3", new PotTableDecorationObject("decorativepot3", new Color(140, 164, 212), 14, 12, "decorativepot", false), 20.0F, true);
      registerObject("decorativepot4", new PotTableDecorationObject("decorativepot4", new Color(140, 164, 212), 12, 12, "decorativepot", false), 20.0F, true);
      registerObject("pottedcactus1", new PotTableDecorationObject("pottedcactus1", new Color(24, 88, 68), 14, 12, "pottedcactus", true), 20.0F, true);
      registerObject("pottedcactus2", new PotTableDecorationObject("pottedcactus2", new Color(91, 146, 53), 14, 12, "pottedcactus", true), 20.0F, true);
      registerObject("pottedcactus3", new PotTableDecorationObject("pottedcactus3", new Color(91, 146, 53), 16, 12, "pottedcactus", true), 20.0F, true);
      registerObject("pottedcactus4", new PotTableDecorationObject("pottedcactus4", new Color(91, 146, 53), 16, 12, 3, 0, "pottedcactus", true), 20.0F, true);
      registerObject("pottedflower1", new PotTableDecorationObject("pottedflower1", new Color(118, 66, 138), 14, 12, "pottedflower", true), 20.0F, true);
      registerObject("pottedflower2", new PotTableDecorationObject("pottedflower2", new Color(156, 48, 121), 14, 12, "pottedflower", true), 20.0F, true);
      registerObject("pottedflower3", new PotTableDecorationObject("pottedflower3", new Color(118, 66, 138), 14, 12, "pottedflower", true), 20.0F, true);
      registerObject("pottedflower4", new PotTableDecorationObject("pottedflower4", new Color(52, 110, 152), 14, 12, "pottedflower", true), 20.0F, true);
      registerObject("pottedflower5", new PotTableDecorationObject("pottedflower5", new Color(196, 57, 72), 14, 12, "pottedflower", true), 20.0F, true);
      registerObject("pottedflower6", new PotTableDecorationObject("pottedflower6", new Color(201, 77, 59), 14, 12, "pottedflower", true), 20.0F, true);
      registerObject("pottedplant1", new PotTableDecorationObject("pottedplant1", new Color(28, 99, 77), 14, 12, "pottedplant", true), 20.0F, true);
      registerObject("pottedplant2", new PotTableDecorationObject("pottedplant2", new Color(28, 99, 77), 14, 12, "pottedplant", true), 20.0F, true);
      registerObject("pottedplant3", new PotTableDecorationObject("pottedplant3", new Color(28, 99, 77), 14, 12, "pottedplant", true), 20.0F, true);
      registerObject("pottedplant4", new PotTableDecorationObject("pottedplant4", new Color(28, 99, 77), 14, 12, "pottedplant", true), 20.0F, true);
      registerObject("pottedplant5", new PotTableDecorationObject("pottedplant5", new Color(40, 100, 79), 14, 12, "pottedplant", true), 20.0F, true);
      registerObject("pottedplant6", new PotTableDecorationObject("pottedplant6", new Color(40, 100, 79), 14, 12, "pottedplant", true), 20.0F, true);
      registerObject("pottedplant7", new PotTableDecorationObject("pottedplant7", new Color(50, 121, 92), 14, 12, "pottedplant", true), 20.0F, true);
      registerObject("bonsaitree1", new PotTableDecorationObject("bonsaitree1", new Color(131, 28, 22), 28, 12, "bonsaitree", true), 20.0F, true);
      registerObject("bonsaitree2", new PotTableDecorationObject("bonsaitree2", new Color(131, 28, 22), 28, 12, "bonsaitree", true), 20.0F, true);
      registerObject("skull", new TableDecorationObject("skull", new Color(136, 136, 136), 20, 16), 20.0F, true);
      registerObject("mosscoveredskull", new TableDecorationObject("mosscoveredskull", new Color(115, 140, 110), 20, 16), 20.0F, true);
      registerObject("bloodgoblet", new TableDecorationObject("bloodgoblet", new Color(184, 106, 31), 16, 18), -1.0F, true);
      registerObject("bloodgobletspilled", new TableDecorationObject("bloodgobletspilled", new Color(184, 106, 31), 20, 16), -1.0F, true);
      registerObject("swampstonecase", new TableDecorationObject("swampstonecase", new Color(50, 94, 37), 16, 14), -1.0F, true);
      registerObject("tableclock", new TableDecorationObject("tableclock", new Color(143, 82, 37), 16, 18), -1.0F, true);
      registerObject("dogplush", new TableDecorationObject("dogplush", new Color(238, 223, 220), 14, 12, 3, 2), -1.0F, true);
      registerObject("dryadspiritcage", new TableDecorationObject("dryadspiritcage", new Color(96, 81, 46), 16, 18, 0, 2), -1.0F, true);
      registerObject("dinoplush", new TableDecorationObject("dinoplush", new Color(165, 185, 114), 14, 14, 2, 0), -1.0F, true);
      registerObject("plate", new TableDecorationObject("plate", new Color(213, 213, 213), 14, 10, 0, 2), -1.0F, true);
      registerObject("mug", new TableDecorationObject("mug", new Color(213, 213, 213), 14, 10, 0, 2), -1.0F, true);
      registerObject("teapot", new TableDecorationObject("teapot", new Color(232, 204, 204), 14, 16), -1.0F, true);
      registerObject("diningset", new DiningSetTableDecorationObject("diningset", new Color(232, 204, 204), 14, 16, 0, -2), -1.0F, true);
      registerObject("bluediningset", new DiningSetTableDecorationObject("bluediningset", new Color(152, 184, 229), 14, 16, 0, -2), -1.0F, true);
      registerObject("reddiningset", new DiningSetTableDecorationObject("reddiningset", new Color(232, 141, 141), 14, 16, 0, -2), -1.0F, true);
      registerObject("dirtyplate", new TableDecorationObject("dirtyplate", new Color(212, 232, 204), 14, 10, 0, 2), -1.0F, true);
      registerObject("dirtydishes", new TableDecorationObject("dirtydishes", new Color(212, 232, 204), 14, 16, 0, 4), -1.0F, true);
      registerObject("stewpot", new TableDecorationObject("stewpot", new Color(128, 131, 131), 14, 18, 0, 4), -1.0F, true);
      registerObject("cuttingboard", new TableDecorationObject("cuttingboard", new Color(169, 104, 59), 14, 16, 0, 4), -1.0F, true);
      registerObject("utensilrack1", new HangingDecorationObject("utensilrack1", new Color(128, 131, 131)).setCustomLocalization("utensilrack"), -1.0F, true);
      registerObject("utensilrack2", new HangingDecorationObject("utensilrack2", new Color(128, 131, 131)).setCustomLocalization("utensilrack"), -1.0F, true);
      registerObject("papertowel", new TableDecorationObject("papertowel", new Color(169, 104, 59), 14, 14, 0, 2), -1.0F, true);
      registerObject("stackofpaper", new TableDecorationObject("stackofpaper", new Color(222, 222, 222), 14, 14, 0, 2), -1.0F, true);
      registerObject("greenandpinkbooks", new TableDecorationObject("greenandpinkbooks", new Color(95, 173, 68), 14, 14, 0, 2), -1.0F, true);
      registerObject("redbooks", new TableDecorationObject("redbooks", new Color(215, 69, 36), 14, 16), -1.0F, true);
      registerObject("blueandyellowbooks", new TableDecorationObject("blueandyellowbooks", new Color(36, 173, 215), 14, 14, 0, 2), -1.0F, true);
      registerObject("yellowbook", new TableDecorationObject("yellowbook", new Color(215, 185, 36), 14, 12, 0, 2), -1.0F, true);
      registerObject("greenbook", new TableDecorationObject("greenbook", new Color(142, 213, 71), 14, 14, 0, 2), -1.0F, true);
      registerObject("orangebook", new TableDecorationObject("orangebook", new Color(222, 117, 53), 14, 14), -1.0F, true);
      registerObject("stackedbooks", new TableDecorationObject("stackedbooks", new Color(185, 53, 222), 14, 16), -1.0F, true);
      registerObject("quillandparchment", new TableDecorationObject("quillandparchment", new Color(210, 210, 210), 14, 18, 0, 2), -1.0F, true);
      registerObject("observantmask", new TableDecorationObject("observantmask", new Color(187, 33, 33), 14, 16, 0, 2), -1.0F, true);
      registerObject("unamusedmask", new TableDecorationObject("unamusedmask", new Color(187, 33, 33), 14, 16, 0, 2), -1.0F, true);
      registerObject("farseersorb", new TableDecorationObject("farseersorb", new Color(33, 110, 187), 14, 16, 0, 2), -1.0F, true);
      registerObject("fishonastick", new TableDecorationObject("fishonastick", new Color(55, 94, 39), 14, 18, 1, 3), -1.0F, true);
      registerObject("voidcube", new TableDecorationObject("voidcube", new Color(103, 47, 122), 14, 16, 0, 2), -1.0F, true);
      registerObject("relicofthedepths", new TableDecorationObject("relicofthedepths", new Color(59, 108, 95), 14, 18, 0, 2), -1.0F, true);
      registerObject("largeglobe", new TableDecorationObject("largeglobe", new Color(38, 126, 213), 16, 18, 0, 4), -1.0F, true);
      registerObject("emptyglassdisplay", new TableDecorationObject("emptyglassdisplay", new Color(199, 252, 255), 16, 14, 0, 2), -1.0F, true);
      registerObject("brokenglassdisplay", new TableDecorationObject("brokenglassdisplay", new Color(199, 252, 255), 16, 14, 0, 2), -1.0F, true);
      registerObject("beetglassdisplay", new TableDecorationObject("beetglassdisplay", new Color(253, 199, 255), 16, 14, 0, 2), -1.0F, true);
      registerObject("brokenherbglassdisplay", new TableDecorationObject("brokenherbglassdisplay", new Color(226, 255, 199), 16, 14, 0, 2), -1.0F, true);
      registerObject("caveglowglassdisplay", new TableDecorationObject("caveglowglassdisplay", new Color(155, 255, 152), 16, 18, 0, 2), -1.0F, true);
      registerObject("experimentalroot", new TableDecorationObject("experimentalroot", new Color(95, 107, 95), 16, 18), -1.0F, true);
      registerObject("testtubes", new TableDecorationObject("testtubes", new Color(255, 233, 199), 16, 14, 0, 2), -1.0F, true);
      registerObject("voidflaskandtesttube", new TableDecorationObject("voidflaskandtesttube", new Color(220, 199, 255), 16, 18, 0, 2), -1.0F, true);
      registerObject("voidflasks", new TableDecorationObject("voidflasks", new Color(220, 199, 255), 14, 16, 0, 2), -1.0F, true);
      registerObject("blueberryandcornextract", new TableDecorationObject("blueberryandcornextract", new Color(255, 252, 199), 16, 22, 0, 2), -1.0F, true);
      registerObject("caveglowflask", new TableDecorationObject("caveglowflask", new Color(199, 255, 232), 12, 16, 0, 4), -1.0F, true);
      LargeTableDecorationObject.registerLargeTableDecoration("stuffedpig", new Color(211, 134, 220), 28, 16, 0, 4, -1.0F, true, true);
      registerObject("spilledgoldchalice", new TableDecorationObject("spilledgoldchalice", new Color(229, 208, 43), 12, 10, 0, 2), -1.0F, true);
      registerObject("goldchalice", new TableDecorationObject("goldchalice", new Color(229, 208, 43), 8, 10), -1.0F, true);
      registerObject("oldplate", new TableDecorationObject("oldplate", new Color(155, 153, 116), 12, 10, 0, -2), -1.0F, true);
      registerObject("rottenpigdish", new TableDecorationObject("rottenpigdish", new Color(151, 116, 155), 14, 16, 0, -2), -1.0F, true);
      registerObject("rottenfishstew", new TableDecorationObject("rottenfishstew", new Color(119, 155, 116), 14, 12, 0, -2), -1.0F, true);
      registerObject("oldsoup", new TableDecorationObject("oldsoup", new Color(147, 155, 116), 14, 12, 0, -2), -1.0F, true);
      registerObject("brokenplate", new TableDecorationObject("brokenplate", new Color(147, 155, 116), 14, 12), -1.0F, true);
      registerObject("simplesculpture", new TableDecorationObject("simplesculpture", new Color(213, 213, 213), 11, 14, 0, 4), -1.0F, true);
      registerObject("oldchalices", new TableDecorationObject("oldchalices", new Color(155, 153, 116), 14, 12, 0, 6), -1.0F, true);
      registerObject("forgottenblade", new TableDecorationObject("forgottenblade", new Color(155, 153, 116), 8, 16, 0, 2), -1.0F, true);
      LargeTableDecorationObject.registerLargeTableDecoration("duelistclock", new Color(104, 147, 99), 28, 16, 0, 4, -1.0F, true, true);
      LargeTableDecorationObject.registerLargeTableDecoration("halfeatenduck", new Color(147, 74, 47), 28, 16, 0, 2, -1.0F, true, true);
      LargeTableDecorationObject.registerLargeTableDecoration("roastedduck", new Color(147, 74, 47), 28, 16, 0, 2, -1.0F, true, true);
      LargeTableDecorationObject.registerLargeTableDecoration("leftovertray", new Color(147, 74, 47), 28, 16, 0, 2, -1.0F, true, true);
      registerObject("fireflyjaryellow", new FireflyJarDecorationObject(FireflyMob.FireflyType.YELLOW), -1.0F, true);
      registerObject("fireflyjargreen", new FireflyJarDecorationObject(FireflyMob.FireflyType.GREEN), -1.0F, true);
      registerObject("fireflyjarblue", new FireflyJarDecorationObject(FireflyMob.FireflyType.BLUE), -1.0F, true);
      registerObject("potatoclock", new PotatoClockDecorationObject("potatoclock", new Color(184, 106, 31), 16, 18), 25.0F, true, false);
      registerObject("woolcarpet", new ModularCarpetObject("woolcarpet", new Color(230, 230, 230)), 25.0F, true);
      registerObject("leathercarpet", new ModularCarpetObject("leathercarpet", new Color(96, 65, 53)), 25.0F, true);
      registerObject("brownbearcarpet", new ModularCarpetObject("brownbearcarpet", new Color(96, 65, 53)), 25.0F, true);
      registerObject("bluecarpet", new ModularCarpetObject("bluecarpet", new Color(96, 65, 53)), 25.0F, true);
      registerObject("goldgridcarpet", new ModularCarpetObject("goldgridcarpet", new Color(96, 65, 53)), 25.0F, true);
      registerObject("greencarpet", new ModularCarpetObject("greencarpet", new Color(96, 65, 53)), 25.0F, true);
      registerObject("heartcarpet", new ModularCarpetObject("heartcarpet", new Color(96, 65, 53)), 25.0F, true);
      registerObject("purplecarpet", new ModularCarpetObject("purplecarpet", new Color(96, 65, 53)), 25.0F, true);
      registerObject("redyarncarpet", new ModularCarpetObject("redyarncarpet", new Color(96, 65, 53)), 25.0F, true);
      registerObject("steelgreycarpet", new ModularCarpetObject("steelgreycarpet", new Color(96, 65, 53)), 25.0F, true);
      registerObject("velourcarpet", new ModularCarpetObject("velourcarpet", new Color(96, 65, 53)), 25.0F, true);
      registerObject("groundslime", new GroundSlimeObject(), 5.0F, false);
      registerObject("spiritbasin", new SpiritBasinObject("spiritbasin", "objects", "furniture"), 1000.0F, true, false);
      registerObject("dryadplant", new DryadPlantObject("dryadplant", "objects", "decorations", "pots"), 500.0F, true, false);
      registerObject("pestwardenlamp", new PestWardenLampObject("pestwardenlamp", "objects", "lighting"), 1000.0F, true, false);
      registerObject("luckyrabbitsfoot", new LuckyRabbitsFootObject("luckyrabbitsfoot", 0, 2), 500.0F, true, false);
      registerObject("wizardbookshelf", new WizardBookshelfObject("wizardbookshelf", new Color(182, 60, 53), "objects", "furniture"), 500.0F, true, false);
      registerObject("friendlybush", new FriendlyBushObject("oakleaves"), 750.0F, true, false);
      registerObject(
         "cavelingdisplay", new CavelingDisplayStandObject("cavelingdisplay", new Color(95, 95, 95), 20, "objects", "furniture"), 1000.0F, true, false
      );
      registerObject("sheepchair", new SheepChairObject("sheepchair", new Color(200, 200, 200), "objects", "furniture"), 500.0F, true, false);
      registerObject("parrotperch", new ParrotPerchObject("parrotperch", new Color(116, 69, 43), "objects", "decorations"), 750.0F, true, false);
      registerObject("unmeltableice", new UnmeltableIceObject("unmeltableice", new Color(36, 174, 214), "objects", "decorations"), 1000.0F, true, false);
      registerObject("championbelt", new WallChampionBeltObject("championbelt"), 500.0F, true, false);
      registerObject("fangoftheprotector", new WallFangOfTheProtectorObject("fangoftheprotector"), 750.0F, true, false);
      registerObject(
         "sagepedestal", new SageAndGritPedestalObject("sagepedestal", true, new Color(34, 146, 230), "objects", "decorations"), 1000.0F, true, false
      );
      registerObject(
         "gritpedestal", new SageAndGritPedestalObject("gritpedestal", false, new Color(250, 120, 22), "objects", "decorations"), 1000.0F, true, false
      );
      registerObject("tropicalislandpainting", new TropicalIslandPaintingObject("tropicalislandpainting"), 750.0F, true, false);
      registerObject("sarcophagus", new SarcophagusObject("sarcophagus", new Color(136, 80, 65), "objects", "decorations"), 750.0F, true, false);
      registerObject(
         "vulturedisplay", new VultureDisplayStandObject("vulturedisplay", new Color(160, 149, 51), 20, "objects", "furniture"), 750.0F, true, false
      );
      registerObject("woodenduck", new WoodenDuckObject("woodenduck", 0, 2), 500.0F, true, false);
      registerObject("voidflame", new VoidFlameObject(), 750.0F, true, false);
      GoblinFriendsLargePaintingObject.registerLargePainting("goblinfriendspainting", Item.Rarity.RARE, 500.0F, true, false);
      SwampGuardianLargePaintingObject.registerLargePainting("swampguardianpainting", Item.Rarity.EPIC, 750.0F, true, false);
      ReaperScytheStandObject.registerReaperScytheStand("reapersscythestand");
      SlimeBenchObject.registerBench("slimebench", "slimebench", new Color(0, 191, 163), 500.0F, "objects", "furniture");
      WitchCauldronObject.registerWitchCauldron("witchcauldron");
      FishianBathtubObject.registerBathtub("fishianbathtub", "fishianbathtub", new Color(151, 102, 175), 500.0F, "objects", "furniture");
      BearRugObject.registerBearRug();
      PirateCarpetObject.registerPirateCarpet();
      MossyCarpetObject.registerMossyCarpet();
      RuneboundDrumObject.registerRuneboundDrum();
      QueenEggNestObject.registerQueenEggNest();
      registerObject("woodpressureplate", new MaskedPressurePlateObject("pressureplatemask", "woodfloor", new Color(93, 67, 24)), 5.0F, true);
      registerObject("stonepressureplate", new MaskedPressurePlateObject("pressureplatemask", "stonefloor", new Color(130, 139, 152)), 1.0F, true);
      registerObject("snowstonepressureplate", new MaskedPressurePlateObject("pressureplatemasklight", "snowstonefloor", new Color(182, 200, 214)), 1.0F, true);
      registerObject("granitepressureplate", new MaskedPressurePlateObject("pressureplatemasklight", "granitefloor", new Color(107, 53, 51)), 1.0F, true);
      registerObject("swampstonepressureplate", new MaskedPressurePlateObject("pressureplatemask", "swampstonefloor", new Color(124, 131, 30)), 1.0F, true);
      registerObject("sandstonepressureplate", new MaskedPressurePlateObject("pressureplatemasklight", "sandstonefloor", new Color(204, 155, 110)), 1.0F, true);
      registerObject("dungeonpressureplate", new MaskedPressurePlateObject("pressureplatemaskdark", "dungeonfloor", new Color(98, 104, 113)), 10.0F, true);
      registerObject("deepstonepressureplate", new MaskedPressurePlateObject("pressureplatemaskdark", "deepstonefloor", new Color(90, 82, 63)), 15.0F, true);
      registerObject(
         "deepsnowstonepressureplate", new MaskedPressurePlateObject("pressureplatemask", "deepsnowstonefloor", new Color(32, 90, 107)), 15.0F, true
      );
      registerObject("basaltpressureplate", new MaskedPressurePlateObject("pressureplatemask", "basaltfloor", new Color(32, 90, 107)), 15.0F, true);
      registerObject(
         "deepswampstonepressureplate", new MaskedPressurePlateObject("pressureplatemask", "deepswampstonefloor", new Color(123, 141, 70)), 15.0F, true
      );
      LeverObject.registerLeverPair("rocklever", "rocklever", 1.0F);
      registerObject("ledpanel", new LEDPanelObject(), 10.0F, true);
      registerObject("woodarrowtrap", new WallArrowTrapObject(woodWall), 50.0F, true);
      registerObject("woodsawtrap", new WallSawTrapObject(woodWall), 50.0F, false, false, true);
      registerObject("dryadarrowtrap", new WallArrowTrapObject(dryadWall), 50.0F, true);
      registerObject("stoneflametrap", new WallFlameTrapObject(stoneWall), 50.0F, true);
      registerObject("stonearrowtrap", new WallArrowTrapObject(stoneWall), 50.0F, true);
      registerObject("stonesawtrap", new WallSawTrapObject(stoneWall), 50.0F, false, false, true);
      registerObject("sandstoneflametrap", new WallFlameTrapObject(sandstoneWall), 50.0F, true);
      registerObject("sandstonearrowtrap", new WallArrowTrapObject(sandstoneWall), 50.0F, true);
      registerObject("sandstonesawtrap", new WallSawTrapObject(sandstoneWall), 50.0F, false, false, true);
      registerObject("swampstoneflametrap", new WallFlameTrapObject(swampstoneWall), 50.0F, true);
      registerObject("swampstonearrowtrap", new WallArrowTrapObject(swampstoneWall), 50.0F, true);
      registerObject("swampstonesawtrap", new WallSawTrapObject(swampstoneWall), 50.0F, false, false, true);
      registerObject("snowstonearrowtrap", new WallArrowTrapObject(snowstoneWall), 50.0F, true);
      registerObject("snowstonesawtrap", new WallSawTrapObject(snowstoneWall), 50.0F, false, false, true);
      registerObject("icearrowtrap", new WallArrowTrapObject(iceWall), 50.0F, true);
      registerObject("icesawtrap", new WallSawTrapObject(iceWall), 50.0F, false, false, true);
      registerObject("graniteflametrap", new WallFlameTrapObject(graniteWall), 50.0F, true);
      registerObject("granitearrowtrap", new WallArrowTrapObject(graniteWall), 50.0F, true);
      registerObject("granitesawtrap", new WallSawTrapObject(graniteWall), 50.0F, false, false, true);
      registerObject("dungeonflametrap", new WallFlameTrapObject(dungeonWall), 50.0F, true);
      registerObject("dungeonarrowtrap", new WallArrowTrapObject(dungeonWall), 50.0F, true);
      registerObject("dungeonvoidtrap", new WallVoidTrapObject(dungeonWall), 50.0F, true);
      registerObject("dungeonsawtrap", new WallSawTrapObject(dungeonWall), 50.0F, false, false, true);
      registerObject("deepstoneflametrap", new WallFlameTrapObject(deepstoneWall), 50.0F, true);
      registerObject("deepstonearrowtrap", new WallArrowTrapObject(deepstoneWall), 50.0F, true);
      registerObject("deepstonesawtrap", new WallSawTrapObject(deepstoneWall), 50.0F, false, false, true);
      registerObject("obsidianflametrap", new WallFlameTrapObject(obsidianWall), 50.0F, true);
      registerObject("obsidianarrowtrap", new WallArrowTrapObject(obsidianWall), 50.0F, true);
      registerObject("obsidiansawtrap", new WallSawTrapObject(obsidianWall), 50.0F, false, false, true);
      registerObject("deepsnowstoneflametrap", new WallFlameTrapObject(deepSnowstoneWall), 50.0F, true);
      registerObject("deepsnowstonearrowtrap", new WallArrowTrapObject(deepSnowstoneWall), 50.0F, true);
      registerObject("deepsnowstonesawtrap", new WallSawTrapObject(deepSnowstoneWall), 50.0F, false, false, true);
      registerObject("basaltflametrap", new WallFlameTrapObject(basaltWall), 50.0F, true);
      registerObject("basaltarrowtrap", new WallArrowTrapObject(basaltWall), 50.0F, true);
      registerObject("basaltsawtrap", new WallSawTrapObject(basaltWall), 50.0F, false, false, true);
      registerObject("deepswampstoneflametrap", new WallFlameTrapObject(deepSwampstoneWall), 50.0F, true);
      registerObject("deepswampstonearrowtrap", new WallArrowTrapObject(deepSwampstoneWall), 50.0F, true);
      registerObject("deepswampstonesawtrap", new WallSawTrapObject(deepSwampstoneWall), 50.0F, false, false, true);
      registerObject("deepsandstoneflametrap", new WallFlameTrapObject(deepSandstoneWall), 50.0F, true);
      registerObject("deepsandstonearrowtrap", new WallArrowTrapObject(deepSandstoneWall), 50.0F, true);
      registerObject("deepsandstonesawtrap", new WallSawTrapObject(deepSandstoneWall), 50.0F, false, false, true);
      registerObject("spidercastlearrowtrap", new WallArrowTrapObject(spiderCastleWall), 50.0F, true);
      registerObject("tnt", new TNTObject(), 150.0F, true);
      registerObject("fireworkdispenser", new FireworkDispenserObject(), 30.0F, true);
      registerObject("spiketrap", new SpikeTrapObject(), 50.0F, false, false, true);
      registerObject("glyphtrapchicken", new ChickenGlyphTrapObject(), 50.0F, true);
      registerObject("glyphtrapbounce", new BounceGlyphTrapObject(), 50.0F, true);
      registerObject("glyphtrapreversedamage", new ReverseDamageGlyphTrapObject(), 50.0F, true);
      WallFlameTrapObject factoryFlameTrap = new WallFlameTrapObject(factoryWall);
      factoryFlameTrap.range = 12;
      factoryFlameTrap.perpendicularWidth = 2;
      factoryFlameTrap.damage = new GameDamage(DamageTypeRegistry.TRUE, 100.0F);
      registerObject("factoryflametrap", factoryFlameTrap, 50.0F, false);
      registerObject("factoryexplosivebarreltrap", new WallExplosiveBarrelTrapObject(factoryWall), 50.0F, false);
      SeedObject.registerSeedObjects("wheatseed", "wheat", "wheat", 4, 0, 5, 1200.0F, 2100.0F, new Color(255, 177, 8), 1.0F);
      SeedObject.registerSeedObjects("cornseed", "corn", "corn", 3, 0, 5, 2100.0F, 3000.0F, new Color(248, 188, 6), 1.0F);
      SeedObject.registerSeedObjects("tomatoseed", "tomato", "tomato", 3, 0, 5, 2100.0F, 3000.0F, new Color(232, 17, 17), 1.0F);
      SeedObject.registerSeedObjects("cabbageseed", "cabbage", "cabbage", 2, 0, 5, 2100.0F, 3000.0F, new Color(56, 161, 50), 1.0F);
      SeedObject.registerSeedObjects("chilipepperseed", "chilipepper", "chilipepper", 3, 0, 5, 2100.0F, 3000.0F, new Color(205, 15, 15), 1.0F);
      SeedObject.registerSeedObjects("sugarbeetseed", "sugarbeet", "sugarbeet", 2, 0, 5, 2100.0F, 3000.0F, new Color(202, 181, 146), 1.0F);
      SeedObject.registerSeedObjects("eggplantseed", "eggplant", "eggplant", 2, 0, 5, 2100.0F, 3000.0F, new Color(130, 36, 161), 1.0F);
      SeedObject.registerSeedObjects("potatoseed", "potato", "potato", 3, 0, 5, 2100.0F, 3000.0F, new Color(90, 61, 48), 1.0F);
      SeedObject.registerSeedObjects("beetseed", "beet", "beet", 3, 0, 5, 2100.0F, 3000.0F, new Color(151, 84, 117), 1.0F);
      SeedObject[] riceSeeds = SeedObject.registerSeedObjects(
         "riceseed", "rice", "riceseed", 3, 0, 5, 2100.0F, 3000.0F, new Color(239, 233, 202), Item.Rarity.NORMAL, 960, 1.0F
      );
      Arrays.stream(riceSeeds).map(GameObject::getObjectItem).forEach(obj -> obj.setItemCategory("consumable", "rawfood"));
      SeedObject.registerSeedObjects("carrotseed", "carrot", "carrot", 2, 0, 5, 2100.0F, 3000.0F, new Color(186, 91, 14), 1.0F);
      SeedObject.registerSeedObjects("onionseed", "onion", "onion", 2, 0, 5, 2100.0F, 3000.0F, new Color(161, 18, 90), 1.0F);
      SeedObject.registerSeedObjects("pumpkinseed", "pumpkin", "pumpkin", 2, 0, 5, 2100.0F, 3000.0F, new Color(228, 137, 25), 1.0F);
      SeedObject.registerSeedObjects("strawberryseed", "strawberry", "strawberry", 4, 0, 5, 2100.0F, 3000.0F, new Color(182, 60, 53), 1.0F);
      SeedObject[] coffeeBeans = SeedObject.registerSeedObjects(
         "coffeebeans", "coffee", "coffeebeans", 5, 0, 5, 2100.0F, 3000.0F, new Color(120, 49, 33), Item.Rarity.NORMAL, 960, 3.0F
      );
      Arrays.stream(coffeeBeans).map(GameObject::getObjectItem).forEach(obj -> obj.setItemCategory("consumable", "rawfood"));
      SeedObject.registerSeedObjects("sunflowerseed", "sunflower", "sunflower", 1, 0, 5, 1500.0F, 2100.0F, new Color(255, 207, 5), 1.0F);
      registerObject(
         "wildsunflower",
         new CustomWildFlowerObject("sunflower", 5, "sunflowerseed", 2, "sunflower", 1, new Color(255, 207, 5), "plainsgrasstile", "overgrownplainsgrasstile"),
         0.0F,
         false
      );
      registerObject(
         "sunflower", new FlowerObject("sunflower", 6, 250, 960, "wildsunflower", new Color(255, 207, 5)).addGlobalIngredient("anyflower"), 3.0F, true
      );
      SeedObject.registerSeedObjects("firemoneseed", "firemone", "firemone", 3, 0, 5, 1500.0F, 2100.0F, new Color(225, 58, 1), 1.0F);
      registerObject("wildfiremone", new CustomWildFlowerObject("firemone", 5, "firemoneseed", 2, "firemone", 3, new Color(225, 58, 1)), 0.0F, false);
      registerObject("firemone", new FlowerObject("firemone", 6, 250, 960, "wildfiremone", new Color(225, 58, 1)).addGlobalIngredient("anyflower"), 3.0F, true);
      SeedObject.registerSeedObjects("iceblossomseed", "iceblossom", "iceblossom", 3, 0, 5, 1500.0F, 2100.0F, new Color(36, 174, 214), 1.0F);
      registerObject(
         "wildiceblossom", new CustomWildFlowerObject("iceblossom", 5, "iceblossomseed", 2, "iceblossom", 3, new Color(36, 174, 214), "snowtile"), 0.0F, false
      );
      registerObject(
         "iceblossom",
         new FlowerObject("iceblossom", 6, 250, 960, "wildiceblossom", new Color(36, 174, 214))
            .addGlobalIngredient("anyflower")
            .addGlobalIngredient("anycoolingfuel"),
         3.0F,
         true
      );
      int mushroomFlower = registerObject(
         "mushroomflower", new FlowerObject("mushroom", 6, 250, 960, "mushroom", "wildmushroom", new Color(126, 97, 68)), 0.0F, false
      );
      SeedObject.registerSeedObjects(
         "mushroom", "mushroom", "mushroom", 4, 0, 5, 1500.0F, 2100.0F, true, mushroomFlower, new Color(126, 97, 68), Item.Rarity.NORMAL, 600, 1.0F
      );
      registerObject(
         "wildmushroom",
         new CustomWildFlowerObject("mushroom", 5, "mushroom", 2, "mushroom", 1, new Color(126, 97, 68), "swampgrasstile", "overgrownswampgrasstile", "mudtile"),
         0.0F,
         false
      );
      SeedObject[] caveGlowSeeds = SeedObject.registerSeedObjects(
         "caveglowsprout", "caveglow", "caveglow", 2, 0, 5, 1500.0F, 2100.0F, false, 0, new Color(85, 182, 91), Item.Rarity.UNCOMMON, 0, 1.0F
      );
      caveGlowSeeds[caveGlowSeeds.length - 1].lightLevel = 50;
      CustomWildFlowerObject wildCaveGlow = new CustomWildFlowerObject(
         "wildcaveglow", 0, "caveglowsprout", 2, "caveglow", 4, new Color(85, 182, 91), "deeprocktile"
      );
      wildCaveGlow.lightLevel = 50;
      registerObject("wildcaveglow", wildCaveGlow, 0.0F, false);
      FlowerObject caveGlow = new FlowerObject("caveglow", 6, 250, 960, "wildcaveglow", new Color(85, 182, 91));
      caveGlow.rarity = Item.Rarity.UNCOMMON;
      caveGlow.lightLevel = 50;
      registerObject("caveglow", caveGlow, 2.0F, true);
      CustomWildFlowerObject wildAncientRoots = new CustomWildFlowerObject(
         "ancientroots",
         5,
         "ancientrootssprout",
         2,
         "ancientroots",
         1,
         new Color(126, 97, 68),
         "overgrowngrasstile",
         "overgrownplainsgrasstile",
         "overgrownswampgrasstile",
         "ancientrootstile"
      );
      wildAncientRoots.customSeedLootItem = new ChanceLootItem(0.1F, "ancientrootssprout", 1);
      wildAncientRoots.customProductLootItem = new ChanceLootItem(0.1F, "ancientroots", 1);
      registerObject("wildancientroots", wildAncientRoots, 0.0F, false);
      FlowerObject ancientRoots = new FlowerObject("ancientroots", 6, 250, 1920, "wildancientroots", new Color(126, 97, 68));
      ancientRoots.rarity = Item.Rarity.RARE;
      registerObject("ancientroots", ancientRoots, 15.0F, true);
      SeedObject[] ancientRootSeeds = SeedObject.registerSeedObjects(
         "ancientrootssprout", "ancientroots", "ancientroots", 1, 0, 5, 3000.0F, 4200.0F, new Color(126, 97, 68), Item.Rarity.RARE, 0, 40.0F
      );

      for (SeedObject ancientRootSeed : ancientRootSeeds) {
         ancientRootSeed.customSeedLootItem = new LootItem("ancientrootssprout", 1);
      }

      registerObject("campfire", new CampfireObject(), 10.0F, true);
      registerObject("cookingpot", new CookingPotObject(), 10.0F, true);
      registerObject("roastingstation", new RoastingStationObject(), 10.0F, true);
      CookingStationObject.registerCookingStation();
      registerObject("compostbin", new CompostBinObject(), 30.0F, true);
      GrainMillBaseObject.registerGrainMill();
      registerObject("cheesepress", new CheesePressObject(), 50.0F, true);
      registerObject("beehive", new BeeHiveObject(), 10.0F, false, false, true);
      registerObject("apiary", new ApiaryObject(), 80.0F, true);
      FeedingTroughObject.registerFeedingTrough();
      registerObject("eggnest", new EggNestObject(), 20.0F, true);
      String[] forestRocksCategory = new String[]{"objects", "landscaping", "forestrocksandores"};
      RockObject rock;
      rockID = registerObject("rock", rock = new RockObject("rock", new Color(72, 71, 77), "stone", forestRocksCategory), -1.0F, true);
      SingleRockObject.registerSurfaceRock(rock, "surfacerock", new Color(130, 139, 152), -1.0F, true, forestRocksCategory);
      registerObject("surfacerocksmall", new SingleRockSmall(rock, "surfacerocksmall", new Color(130, 139, 152), forestRocksCategory), -1.0F, true);
      SingleRockObject.registerSurfaceRock(rock, "caverock", new Color(130, 139, 152), -1.0F, true, forestRocksCategory);
      registerObject("caverocksmall", new SingleRockSmall(rock, "caverocksmall", new Color(130, 139, 152), forestRocksCategory), -1.0F, true);
      registerObject("ironorerock", new RockOreObject(rock, "oremask", "ironore", new Color(169, 128, 106), "ironore", forestRocksCategory), -1.0F, true);
      registerObject("copperorerock", new RockOreObject(rock, "oremask", "copperore", new Color(161, 83, 42), "copperore", forestRocksCategory), -1.0F, true);
      registerObject("goldorerock", new RockOreObject(rock, "oremask", "goldore", new Color(198, 145, 47), "goldore", forestRocksCategory), -1.0F, true);
      String[] snowRocksCategory = new String[]{"objects", "landscaping", "snowrocksandores"};
      RockObject snowRock;
      snowRockID = registerObject("snowrock", snowRock = new RockObject("snowrock", new Color(186, 237, 255), "snowstone", snowRocksCategory), -1.0F, true);
      SingleRockObject.registerSurfaceRock(snowRock, "snowsurfacerock", new Color(49, 142, 184), -1.0F, true, snowRocksCategory);
      registerObject("snowsurfacerocksmall", new SingleRockSmall(snowRock, "snowsurfacerocksmall", new Color(49, 142, 184), snowRocksCategory), -1.0F, true);
      SingleRockObject.registerSurfaceRock(snowRock, "snowcaverock", new Color(49, 142, 184), -1.0F, true, snowRocksCategory);
      registerObject("snowcaverocksmall", new SingleRockSmall(snowRock, "snowcaverocksmall", new Color(49, 142, 184), snowRocksCategory), -1.0F, true);
      registerObject("ironoresnow", new RockOreObject(snowRock, "oremask", "ironore", new Color(169, 128, 106), "ironore", snowRocksCategory), -1.0F, true);
      registerObject("copperoresnow", new RockOreObject(snowRock, "oremask", "copperore", new Color(161, 83, 42), "copperore", snowRocksCategory), -1.0F, true);
      registerObject("goldoresnow", new RockOreObject(snowRock, "oremask", "goldore", new Color(198, 145, 47), "goldore", snowRocksCategory), -1.0F, true);
      registerObject(
         "frostshardsnow",
         new RockOreObject(snowRock, "oremask", "frostshardore", new Color(121, 201, 224), "frostshard", 1, 1, 1, snowRocksCategory),
         -1.0F,
         true
      );
      String[] desertRocksCategory = new String[]{"objects", "landscaping", "desertrocksandores"};
      RockObject sandstoneRock;
      sandstoneRockID = registerObject(
         "sandstonerock", sandstoneRock = new RockObject("sandstonerock", new Color(186, 145, 95), "sandstone", desertRocksCategory), -1.0F, true
      );
      sandstoneRock.toolTier = 4.0F;
      SingleRockObject.registerSurfaceRock(sandstoneRock, "sandsurfacerock", new Color(114, 90, 81), -1.0F, true, desertRocksCategory);
      registerObject(
         "sandsurfacerocksmall", new SingleRockSmall(sandstoneRock, "sandsurfacerocksmall", new Color(114, 90, 81), desertRocksCategory), -1.0F, true
      );
      SingleRockObject.registerSurfaceRock(sandstoneRock, "sandcaverock", new Color(204, 169, 110), -1.0F, true, desertRocksCategory);
      registerObject("sandcaverocksmall", new SingleRockSmall(sandstoneRock, "sandcaverocksmall", new Color(204, 169, 110), desertRocksCategory), -1.0F, true);
      registerObject(
         "ironoresandstone", new RockOreObject(sandstoneRock, "oremask", "ironore", new Color(169, 128, 106), "ironore", desertRocksCategory), -1.0F, true
      );
      registerObject(
         "copperoresandstone", new RockOreObject(sandstoneRock, "oremask", "copperore", new Color(161, 83, 42), "copperore", desertRocksCategory), -1.0F, true
      );
      registerObject(
         "goldoresandstone", new RockOreObject(sandstoneRock, "oremask", "goldore", new Color(198, 145, 47), "goldore", desertRocksCategory), -1.0F, true
      );
      registerObject(
         "quartzsandstone",
         new RockOreObject(sandstoneRock, "oremask", "quartzore", new Color(182, 188, 158), "quartz", 1, 1, 1, desertRocksCategory),
         -1.0F,
         true
      );
      String[] swampRocksCategory = new String[]{"objects", "landscaping", "swamprocksandores"};
      RockObject swampRock;
      swampRockID = registerObject("swamprock", swampRock = new RockObject("swamprock", new Color(47, 79, 8), "swampstone", swampRocksCategory), -1.0F, true);
      swampRock.toolTier = 3.0F;
      SingleRockObject.registerSurfaceRock(swampRock, "swampsurfacerock", new Color(67, 69, 73), -1.0F, true, swampRocksCategory);
      registerObject("swampsurfacerocksmall", new SingleRockSmall(swampRock, "swampsurfacerocksmall", new Color(67, 69, 73), swampRocksCategory), -1.0F, true);
      SingleRockObject.registerSurfaceRock(swampRock, "swampcaverock", new Color(48, 71, 74), -1.0F, true, swampRocksCategory);
      registerObject("swampcaverocksmall", new SingleRockSmall(swampRock, "swampcaverocksmall", new Color(48, 71, 74), swampRocksCategory), -1.0F, true);
      registerObject("ironoreswamp", new RockOreObject(swampRock, "oremask", "ironore", new Color(169, 128, 106), "ironore", swampRocksCategory), -1.0F, true);
      registerObject(
         "copperoreswamp", new RockOreObject(swampRock, "oremask", "copperore", new Color(161, 83, 42), "copperore", swampRocksCategory), -1.0F, true
      );
      registerObject("goldoreswamp", new RockOreObject(swampRock, "oremask", "goldore", new Color(198, 145, 47), "goldore", swampRocksCategory), -1.0F, true);
      registerObject("ivyoreswamp", new RockOreObject(swampRock, "oremask", "ivyore", new Color(91, 130, 36), "ivyore", swampRocksCategory), -1.0F, true);
      String[] plainsRocksCategory = new String[]{"objects", "landscaping", "plainsrocksandores"};
      RockObject graniteRock;
      graniteRockID = registerObject(
         "graniterock", graniteRock = new RockObject("graniterock", new Color(98, 46, 44), "granite", plainsRocksCategory), -1.0F, true
      );
      graniteRock.toolTier = 2.0F;
      SingleRockObject.registerSurfaceRock(graniteRock, "granitecaverock", new Color(115, 61, 59), -1.0F, true, plainsRocksCategory);
      registerObject("granitecaverocksmall", new SingleRockSmall(graniteRock, "granitecaverocksmall", new Color(115, 61, 59), plainsRocksCategory), -1.0F, true);
      registerObject(
         "ironoregraniterock", new RockOreObject(graniteRock, "oremask", "ironore", new Color(169, 128, 106), "ironore", plainsRocksCategory), -1.0F, true
      );
      registerObject(
         "copperoregraniterock", new RockOreObject(graniteRock, "oremask", "copperore", new Color(161, 83, 42), "copperore", plainsRocksCategory), -1.0F, true
      );
      registerObject(
         "goldoregraniterock", new RockOreObject(graniteRock, "oremask", "goldore", new Color(198, 145, 47), "goldore", plainsRocksCategory), -1.0F, true
      );
      RockObject clayRock = new RockObject("clayrock", new Color(87, 52, 22), "clay", forestRocksCategory);
      clayRock.displayMapTooltip = true;
      registerObject("clayrock", clayRock, -1.0F, true);
      RockObject deepRock;
      deepRockID = registerObject("deeprock", deepRock = new RockObject("deeprock", new Color(92, 37, 23), "deepstone", forestRocksCategory), -1.0F, true);
      deepRock.toolTier = 5.0F;
      SingleRockObject.registerSurfaceRock(deepRock, "deepcaverock", new Color(82, 83, 91), -1.0F, true, forestRocksCategory);
      registerObject("deepcaverocksmall", new SingleRockSmall(deepRock, "deepcaverocksmall", new Color(82, 83, 91), forestRocksCategory), -1.0F, true);
      registerObject(
         "ironoredeeprock", new RockOreObject(deepRock, "oremask", "ironore", new Color(169, 128, 106), "ironore", forestRocksCategory), -1.0F, true
      );
      registerObject(
         "copperoredeeprock", new RockOreObject(deepRock, "oremask", "copperore", new Color(161, 83, 42), "copperore", forestRocksCategory), -1.0F, true
      );
      registerObject("goldoredeeprock", new RockOreObject(deepRock, "oremask", "goldore", new Color(198, 145, 47), "goldore", forestRocksCategory), -1.0F, true);
      registerObject(
         "tungstenoredeeprock", new RockOreObject(deepRock, "oremask", "tungstenore", new Color(40, 49, 57), "tungstenore", forestRocksCategory), -1.0F, true
      );
      registerObject(
         "lifequartzdeeprock",
         new RockOreObject(deepRock, "oremask", "lifequartzore", new Color(180, 50, 61), "lifequartz", 1, 1, 1, forestRocksCategory),
         -1.0F,
         true
      );
      RockObject obsidian;
      registerObject("obsidianrock", obsidian = new RockObject("obsidianrock", new Color(21, 21, 60), "obsidian", 2, 3, 2, forestRocksCategory), -1.0F, true);
      obsidian.displayMapTooltip = true;
      obsidian.toolTier = 6.0F;
      registerObject(
         "upgradesharddeeprock",
         new RockOreObject(deepRock, "oremask", "upgradeshardore", new Color(0, 27, 107), "upgradeshard", 1, 1, 1, false, forestRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "alchemysharddeeprock",
         new RockOreObject(deepRock, "oremask", "alchemyshardore", new Color(102, 0, 61), "alchemyshard", 1, 1, 1, false, forestRocksCategory),
         -1.0F,
         true
      );
      RockOreObject shadowEssenceOre = new RockOreObject(
         deepRock, "oremask", "shadowessenceore", new Color(47, 28, 94), "shadowessence", 1, 1, 1, false, forestRocksCategory
      );
      shadowEssenceOre.lightLevel = 150;
      shadowEssenceOre.lightHue = 254.0F;
      shadowEssenceOre.lightSat = 38.0F;
      registerObject("shadowessencedeeprock", shadowEssenceOre, -1.0F, true);
      RockObject snowDeepRock;
      deepSnowRockID = registerObject(
         "deepsnowrock", snowDeepRock = new RockObject("deepsnowrock", new Color(0, 81, 98), "deepsnowstone", snowRocksCategory), -1.0F, true
      );
      snowDeepRock.toolTier = 6.0F;
      SingleRockObject.registerSurfaceRock(snowDeepRock, "deepsnowcaverock", new Color(0, 137, 131), -1.0F, true, snowRocksCategory);
      registerObject(
         "deepsnowcaverocksmall", new SingleRockSmall(snowDeepRock, "deepsnowcaverocksmall", new Color(0, 137, 131), snowRocksCategory), -1.0F, true
      );
      registerObject(
         "ironoredeepsnowrock", new RockOreObject(snowDeepRock, "oremask", "ironore", new Color(169, 128, 106), "ironore", snowRocksCategory), -1.0F, true
      );
      registerObject(
         "copperoredeepsnowrock", new RockOreObject(snowDeepRock, "oremask", "copperore", new Color(161, 83, 42), "copperore", snowRocksCategory), -1.0F, true
      );
      registerObject(
         "goldoredeepsnowrock", new RockOreObject(snowDeepRock, "oremask", "goldore", new Color(198, 145, 47), "goldore", snowRocksCategory), -1.0F, true
      );
      registerObject(
         "tungstenoredeepsnowrock",
         new RockOreObject(snowDeepRock, "oremask", "tungstenore", new Color(40, 49, 57), "tungstenore", snowRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "lifequartzdeepsnowrock",
         new RockOreObject(snowDeepRock, "oremask", "lifequartzore", new Color(180, 50, 61), "lifequartz", 1, 1, 1, snowRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "glacialoredeepsnowrock",
         new RockOreObject(snowDeepRock, "oremask", "glacialore", new Color(88, 105, 218), "glacialore", snowRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "upgradesharddeepsnowrock",
         new RockOreObject(snowDeepRock, "oremask", "upgradeshardore", new Color(0, 27, 107), "upgradeshard", 1, 1, 1, false, snowRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "alchemysharddeepsnowrock",
         new RockOreObject(snowDeepRock, "oremask", "alchemyshardore", new Color(102, 0, 61), "alchemyshard", 1, 1, 1, false, snowRocksCategory),
         -1.0F,
         true
      );
      RockOreObject cryoEssenceOre = new RockOreObject(
         snowDeepRock, "oremask", "cryoessenceore", new Color(64, 147, 253), "cryoessence", 1, 1, 1, false, snowRocksCategory
      );
      cryoEssenceOre.lightLevel = 150;
      cryoEssenceOre.lightHue = 200.0F;
      cryoEssenceOre.lightSat = 100.0F;
      registerObject("cryoessencedeepsnowrock", cryoEssenceOre, -1.0F, true);
      RockObject basaltRock;
      basaltRockID = registerObject("basaltrock", basaltRock = new RockObject("basaltrock", new Color(58, 69, 104), "basalt", plainsRocksCategory), -1.0F, true);
      basaltRock.toolTier = 7.0F;
      SingleRockObject.registerSurfaceRock(basaltRock, "basaltcaverock", new Color(58, 69, 104), -1.0F, true, plainsRocksCategory);
      registerObject("basaltcaverocksmall", new SingleRockSmall(basaltRock, "basaltcaverocksmall", new Color(58, 69, 104), plainsRocksCategory), -1.0F, true);
      registerObject(
         "ironorebasaltrock", new RockOreObject(basaltRock, "oremask", "ironore", new Color(169, 128, 106), "ironore", plainsRocksCategory), -1.0F, true
      );
      registerObject(
         "copperorebasaltrock", new RockOreObject(basaltRock, "oremask", "copperore", new Color(161, 83, 42), "copperore", plainsRocksCategory), -1.0F, true
      );
      registerObject(
         "goldorebasaltrock", new RockOreObject(basaltRock, "oremask", "goldore", new Color(198, 145, 47), "goldore", plainsRocksCategory), -1.0F, true
      );
      registerObject(
         "tungstenorebasaltrock",
         new RockOreObject(basaltRock, "oremask", "tungstenore", new Color(40, 49, 57), "tungstenore", plainsRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "lifequartzbasaltrock",
         new RockOreObject(basaltRock, "oremask", "lifequartzore", new Color(180, 50, 61), "lifequartz", 1, 1, 1, plainsRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "amberbasaltrock", new RockOreObject(basaltRock, "oremask", "amberore", new Color(227, 120, 64), "amber", 1, 1, 1, plainsRocksCategory), -1.0F, true
      );
      RockObject deepSwampRock;
      deepSwampRockID = registerObject(
         "deepswamprock", deepSwampRock = new RockObject("deepswamprock", new Color(64, 101, 70), "deepswampstone", swampRocksCategory), -1.0F, true
      );
      deepSwampRock.toolTier = 8.0F;
      SingleRockObject.registerSurfaceRock(deepSwampRock, "deepswampcaverock", new Color(105, 119, 65), -1.0F, true, swampRocksCategory);
      registerObject(
         "deepswampcaverocksmall", new SingleRockSmall(deepSwampRock, "deepswampcaverocksmall", new Color(105, 119, 65), swampRocksCategory), -1.0F, true
      );
      registerObject(
         "ironoredeepswamprock", new RockOreObject(deepSwampRock, "oremask", "ironore", new Color(169, 128, 106), "ironore", swampRocksCategory), -1.0F, true
      );
      registerObject(
         "copperoredeepswamprock",
         new RockOreObject(deepSwampRock, "oremask", "copperore", new Color(161, 83, 42), "copperore", swampRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "goldoredeepswamprock", new RockOreObject(deepSwampRock, "oremask", "goldore", new Color(198, 145, 47), "goldore", swampRocksCategory), -1.0F, true
      );
      registerObject(
         "tungstenoredeepswamprock",
         new RockOreObject(deepSwampRock, "oremask", "tungstenore", new Color(40, 49, 57), "tungstenore", swampRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "lifequartzdeepswamprock",
         new RockOreObject(deepSwampRock, "oremask", "lifequartzore", new Color(180, 50, 61), "lifequartz", 1, 1, 1, swampRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "myceliumoredeepswamprock",
         new RockOreObject(deepSwampRock, "oremask", "myceliumore", new Color(170, 87, 24), "myceliumore", swampRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "upgradesharddeepswamprock",
         new RockOreObject(deepSwampRock, "oremask", "upgradeshardore", new Color(0, 27, 107), "upgradeshard", 1, 1, 1, false, swampRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "alchemysharddeepswamprock",
         new RockOreObject(deepSwampRock, "oremask", "alchemyshardore", new Color(102, 0, 61), "alchemyshard", 1, 1, 1, false, swampRocksCategory),
         -1.0F,
         true
      );
      RockOreObject bioEssenceOre = new RockOreObject(
         deepSwampRock, "oremask", "bioessenceore", new Color(156, 238, 69), "bioessence", 1, 1, 1, false, swampRocksCategory
      );
      bioEssenceOre.lightLevel = 150;
      bioEssenceOre.lightHue = 75.0F;
      bioEssenceOre.lightSat = 59.0F;
      registerObject("bioessencedeepswamprock", bioEssenceOre, -1.0F, true);
      RockObject deepSandstoneRock;
      deepSandstoneRockID = registerObject(
         "deepsandstonerock",
         deepSandstoneRock = new RockObject("deepsandstonerock", new Color(133, 79, 18), "deepsandstone", desertRocksCategory),
         -1.0F,
         true
      );
      deepSandstoneRock.toolTier = 9.0F;
      SingleRockObject.registerSurfaceRock(deepSandstoneRock, "deepsandcaverock", new Color(156, 102, 30), -1.0F, true, desertRocksCategory);
      registerObject(
         "deepsandcaverocksmall", new SingleRockSmall(deepSandstoneRock, "deepsandcaverocksmall", new Color(156, 102, 30), desertRocksCategory), -1.0F, true
      );
      registerObject(
         "ironoredeepsandstonerock",
         new RockOreObject(deepSandstoneRock, "oremask", "ironore", new Color(169, 128, 106), "ironore", desertRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "copperoredeepsandstonerock",
         new RockOreObject(deepSandstoneRock, "oremask", "copperore", new Color(161, 83, 42), "copperore", desertRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "goldoredeepsandstonerock",
         new RockOreObject(deepSandstoneRock, "oremask", "goldore", new Color(198, 145, 47), "goldore", desertRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "ancientfossiloredeepsnowrock",
         new RockOreObject(deepSandstoneRock, "oremask", "ancientfossilore", new Color(63, 56, 34), "ancientfossilore", desertRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "lifequartzdeepsandstonerock",
         new RockOreObject(deepSandstoneRock, "oremask", "lifequartzore", new Color(180, 50, 61), "lifequartz", 1, 1, 1, desertRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "upgradesharddeepsandstonerock",
         new RockOreObject(deepSandstoneRock, "oremask", "upgradeshardore", new Color(0, 27, 107), "upgradeshard", 1, 1, 1, false, desertRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "alchemysharddeepsandstonerock",
         new RockOreObject(deepSandstoneRock, "oremask", "alchemyshardore", new Color(102, 0, 61), "alchemyshard", 1, 1, 1, false, desertRocksCategory),
         -1.0F,
         true
      );
      RockOreObject primordialEssenceOre = new RockOreObject(
         deepSandstoneRock, "oremask", "primordialessenceore", new Color(44, 243, 236), "primordialessence", 1, 1, 1, false, desertRocksCategory
      );
      primordialEssenceOre.lightLevel = 150;
      primordialEssenceOre.lightHue = 193.0F;
      primordialEssenceOre.lightSat = 71.0F;
      registerObject("primordialessencedeepsandstonerock", primordialEssenceOre, -1.0F, true);
      String[] incursionRocksCategory = new String[]{"objects", "landscaping", "incursionrocksandores"};
      RockObject slimeRock;
      registerObject("slimerock", slimeRock = new RockObject("slimerock", new Color(47, 110, 30), "slimestone", incursionRocksCategory), -1.0F, true);
      slimeRock.toolTier = 10.0F;
      SingleRockObject.registerSurfaceRock(slimeRock, "slimecaverock", new Color(73, 147, 50), -1.0F, true, incursionRocksCategory);
      registerObject(
         "slimecaverocksmall", new SingleRockSmall(slimeRock, "slimecaverocksmall", new Color(73, 147, 50), 0, 0, 1, incursionRocksCategory), -1.0F, true
      );
      registerObject(
         "slimeumslimerock",
         new RockOreObject(slimeRock, "oremask", "slimeum", new Color(231, 220, 6), "slimeum", 1, 2, 1, incursionRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "upgradeshardslimerock",
         new RockOreObject(slimeRock, "oremask", "upgradeshardore", new Color(0, 27, 107), "upgradeshard", 1, 1, 1, false, incursionRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "alchemyshardslimerock",
         new RockOreObject(slimeRock, "oremask", "alchemyshardore", new Color(102, 0, 61), "alchemyshard", 1, 1, 1, false, incursionRocksCategory),
         -1.0F,
         true
      );
      RockOreObject slimeEssenceOre = new RockOreObject(
         slimeRock, "oremask", "slimeessenceore", new Color(1, 199, 161), "slimeessence", 1, 1, 1, false, incursionRocksCategory
      );
      slimeEssenceOre.lightLevel = 150;
      slimeEssenceOre.lightHue = 171.0F;
      slimeEssenceOre.lightSat = 100.0F;
      registerObject("slimeessenceslimerock", slimeEssenceOre, -1.0F, true);
      registerObject(
         "cryptnightsteelorerocksmall",
         new SingleOreRockSmall(
            "cryptstone",
            4.0F,
            "cryptrock",
            "nightsteelore",
            "cryptorerock",
            "cryptorerock_nightsteelore",
            new Color(63, 7, 156),
            "nightsteelore",
            3,
            7,
            5,
            incursionRocksCategory
         ),
         -1.0F,
         true
      );
      registerObject(
         "cryptupgradeshardorerocksmall",
         new SingleOreRockSmall(
            "cryptstone",
            4.0F,
            "cryptrock",
            "upgradeshardore",
            "cryptorerock",
            "cryptorerock_upgradeshard",
            new Color(0, 27, 107),
            "upgradeshard",
            3,
            4,
            4,
            false,
            incursionRocksCategory
         ),
         -1.0F,
         true
      );
      registerObject(
         "cryptalchemyshardorerocksmall",
         new SingleOreRockSmall(
            "cryptstone",
            4.0F,
            "cryptrock",
            "alchemyshardore",
            "cryptorerock",
            "cryptorerock_alchemyshard",
            new Color(102, 0, 61),
            "alchemyshard",
            3,
            4,
            4,
            false,
            incursionRocksCategory
         ),
         -1.0F,
         true
      );
      SingleOreRockSmall bloodEssenceOre = new SingleOreRockSmall(
         "cryptstone",
         4.0F,
         "cryptrock",
         "bloodessenceore",
         "cryptorerock",
         "cryptorerock_bloodessence",
         new Color(211, 0, 18),
         "bloodessence",
         2,
         3,
         3,
         false,
         incursionRocksCategory
      );
      bloodEssenceOre.lightLevel = 150;
      bloodEssenceOre.lightHue = 358.0F;
      bloodEssenceOre.lightSat = 71.0F;
      registerObject("bloodessenceoreorerocksmall", bloodEssenceOre, -1.0F, true);
      RockObject spiderRock;
      registerObject("spiderrock", spiderRock = new RockObject("spiderrock", new Color(0, 77, 94), "spiderstone", incursionRocksCategory), -1.0F, true);
      spiderRock.toolTier = 10.0F;
      SingleRockObject.registerSurfaceRock(spiderRock, "spidercaverock", new Color(0, 77, 94), -1.0F, true, incursionRocksCategory);
      registerObject("spidercaverocksmall", new SingleRockSmall(spiderRock, "spidercaverocksmall", new Color(0, 77, 94), incursionRocksCategory), -1.0F, true);
      registerObject(
         "spideritespiderrock",
         new RockOreObject(spiderRock, "oremask", "spideriteore", new Color(166, 204, 52), "spideriteore", 1, 2, 1, incursionRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "upgradeshardspiderrock",
         new RockOreObject(spiderRock, "oremask", "upgradeshardore", new Color(0, 27, 107), "upgradeshard", 1, 1, 1, false, incursionRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "alchemyshardspiderrock",
         new RockOreObject(spiderRock, "oremask", "alchemyshardore", new Color(102, 0, 61), "alchemyshard", 1, 1, 1, false, incursionRocksCategory),
         -1.0F,
         true
      );
      RockOreObject spiderEssenceOre = new RockOreObject(
         spiderRock, "oremask", "spideressenceore", new Color(65, 74, 253), "spideressence", 1, 1, 1, false, incursionRocksCategory
      );
      spiderEssenceOre.lightLevel = 150;
      spiderEssenceOre.lightHue = 191.0F;
      spiderEssenceOre.lightSat = 100.0F;
      registerObject("spideressencespiderrock", spiderEssenceOre, -1.0F, true);
      RockObject crystalRock;
      registerObject("crystalrock", crystalRock = new RockObject("crystalwall", new Color(99, 15, 143), "crystalstone", incursionRocksCategory), -1.0F, true);
      crystalRock.toolTier = 10.0F;
      registerObject(
         "upgradeshardcrystalrock",
         new RockOreObject(crystalRock, "oremask", "upgradeshardore", new Color(0, 27, 107), "upgradeshard", 1, 1, 1, false, incursionRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "alchemyshardcrystalrock",
         new RockOreObject(crystalRock, "oremask", "alchemyshardore", new Color(102, 0, 61), "alchemyshard", 1, 1, 1, false, incursionRocksCategory),
         -1.0F,
         true
      );
      RockOreObject crystalEssenceOre = new RockOreObject(
         crystalRock, "oremask", "crystalessenceore", new Color(228, 156, 255), "crystalessence", 1, 1, 1, false, incursionRocksCategory
      );
      crystalEssenceOre.lightLevel = 150;
      crystalEssenceOre.lightHue = 251.0F;
      crystalEssenceOre.lightSat = 99.0F;
      registerObject("crystalessencecrystalrock", crystalEssenceOre, -1.0F, true);
      RockObject scrapRock;
      registerObject("scraprock", scrapRock = new RockObject("scraprock", new Color(91, 92, 105), "junk", incursionRocksCategory), -1.0F, true);
      scrapRock.toolTier = 10.0F;
      ScrapheapBigObject.registerScrapHeapBig();
      SingleRockObject.registerSurfaceRock(scrapRock, "scrapheap", new Color(91, 92, 105), -1.0F, true, incursionRocksCategory);
      registerObject(
         "upgradeshardscraprock",
         new RockOreObject(scrapRock, "oremask", "upgradeshardore", new Color(0, 27, 107), "upgradeshard", 1, 1, 1, false, incursionRocksCategory),
         -1.0F,
         true
      );
      registerObject(
         "alchemyshardscraprock",
         new RockOreObject(scrapRock, "oremask", "alchemyshardore", new Color(102, 0, 61), "alchemyshard", 1, 1, 1, false, incursionRocksCategory),
         -1.0F,
         true
      );
      RockOreObject radiatedEssenceOre = new RockOreObject(
         scrapRock, "oremask", "radiatedessenceore", new Color(109, 225, 156), "radiatedessence", 1, 1, 1, false, incursionRocksCategory
      );
      radiatedEssenceOre.lightLevel = 150;
      radiatedEssenceOre.lightHue = 75.0F;
      radiatedEssenceOre.lightSat = 74.0F;
      registerObject("radiatedessencescraprock", radiatedEssenceOre, -1.0F, true);
      RockObject ascendedWall;
      registerObject("ascendedwall", ascendedWall = new AscendedWallObject(incursionRocksCategory), 0.0F, false);
      ascendedWall.toolTier = 0.0F;
      registerObject("ascendedpylon", new AscendedPylonObject(), 0.0F, false);
      String[] crystalsCategory = new String[]{"objects", "landscaping", "crystals"};
      CrystalClusterObject.registerCrystalCluster("amethystcluster", new Color(184, 174, 255), 247.0F, "amethyst", -1.0F, true, crystalsCategory);
      CrystalClusterObject.registerCrystalCluster("amethystclusterpure", new Color(184, 174, 255), 247.0F, "amethyst", -1.0F, true, crystalsCategory);
      CrystalClusterObject.registerCrystalCluster("sapphirecluster", new Color(82, 210, 255), 198.0F, "sapphire", -1.0F, true, crystalsCategory);
      CrystalClusterObject.registerCrystalCluster("sapphireclusterpure", new Color(82, 210, 255), 198.0F, "sapphire", -1.0F, true, crystalsCategory);
      CrystalClusterObject.registerCrystalCluster("emeraldcluster", new Color(0, 191, 163), 171.0F, "emerald", -1.0F, true, crystalsCategory);
      CrystalClusterObject.registerCrystalCluster("emeraldclusterpure", new Color(0, 191, 163), 171.0F, "emerald", -1.0F, true, crystalsCategory);
      CrystalClusterObject.registerCrystalCluster("rubycluster", new Color(255, 125, 175), 337.0F, "ruby", -1.0F, true, crystalsCategory);
      CrystalClusterObject.registerCrystalCluster("rubyclusterpure", new Color(255, 125, 175), 337.0F, "ruby", -1.0F, true, crystalsCategory);
      CrystalClusterObject.registerCrystalCluster("topazcluster", new Color(255, 210, 125), 60.0F, "topaz", -1.0F, true, crystalsCategory);
      CrystalClusterObject.registerCrystalCluster("topazclusterpure", new Color(255, 210, 125), 60.0F, "topaz", -1.0F, true, crystalsCategory);
      registerObject(
         "amethystclustersmall",
         new CrystalClusterSmallObject("amethystcluster_small", new Color(184, 174, 255), 247.0F, "amethyst", 0, 1, 1, crystalsCategory),
         -1.0F,
         true
      );
      registerObject(
         "sapphireclustersmall",
         new CrystalClusterSmallObject("sapphirecluster_small", new Color(82, 210, 255), 198.0F, "sapphire", 0, 1, 1, crystalsCategory),
         -1.0F,
         true
      );
      registerObject(
         "emeraldclustersmall",
         new CrystalClusterSmallObject("emeraldcluster_small", new Color(0, 191, 163), 171.0F, "emerald", 0, 1, 1, crystalsCategory),
         -1.0F,
         true
      );
      registerObject(
         "rubyclustersmall",
         new CrystalClusterSmallObject("rubycluster_small", new Color(255, 125, 175), 337.0F, "ruby", 0, 1, 1, crystalsCategory),
         -1.0F,
         true
      );
      registerObject(
         "topazclustersmall",
         new CrystalClusterSmallObject("topazcluster_small", new Color(255, 210, 125), 60.0F, "topaz", 0, 1, 1, crystalsCategory),
         -1.0F,
         true
      );
      registerObject("pearlescentshard", new PearlescentShardObject("pearlescentshard", new Color(245, 247, 250)), -1.0F, true);
      registerObject("fuelskullencasing", new FuelSkullEncasingObject("fuelskullencasing", new Color(172, 209, 64)), -1.0F, true);
      registerObject("glowcoral", new GlowCoralObject(), 5.0F, true);
      registerObject("seashell", new TileClutterObject("seashell", new Color(220, 212, 255)).setItemCategory("objects", "misc"), 5.0F, true);
      registerObject("seasnail", new TileClutterObject("seasnail", new Color(158, 101, 32)).setItemCategory("objects", "misc"), 5.0F, true);
      registerObject("seastar", new TileClutterObject("seastar", new Color(113, 149, 125)).setItemCategory("objects", "misc"), 5.0F, true);
      registerObject("bamboodebris", new BambooDebrisObject(), -1.0F, true);
      registerObject(
         "incursioncavelingcaverocks",
         new SingleRockSmall(deepRock, "incursioncavelingcaverocks", new Color(98, 154, 119), incursionRocksCategory),
         0.0F,
         false
      );
      registerObject("grass", new SurfaceGrassObject(), -1.0F, true);
      registerObject("swampgrass", new SwampGrassObject(), -1.0F, true);
      registerObject("plainsgrass", new PlainsGrassObject(), -1.0F, true);
      registerObject("cryptgrass", new CryptGrassObject(), -1.0F, true);
      registerObject("witheredgrass", new WitheredGrassObject(), 0.0F, false);
      SwampRootObject.registerSwampRoot();
      CowSkeletonObject.registerCowSkeleton();
      registerObject("yellowflowerpatch", new FlowerPatchObject("yellowflowerpatch", new Color(249, 155, 78)).addGlobalIngredient("anyflower"), 2.0F, true);
      registerObject("redflowerpatch", new FlowerPatchObject("redflowerpatch", new Color(182, 60, 53)).addGlobalIngredient("anyflower"), 2.0F, true);
      registerObject("purpleflowerpatch", new FlowerPatchObject("purpleflowerpatch", new Color(128, 68, 159)).addGlobalIngredient("anyflower"), 2.0F, true);
      registerObject("blueflowerpatch", new FlowerPatchObject("blueflowerpatch", new Color(82, 116, 197)).addGlobalIngredient("anyflower"), 2.0F, true);
      registerObject("whiteflowerpatch", new FlowerPatchObject("whiteflowerpatch", new Color(234, 219, 201)).addGlobalIngredient("anyflower"), 2.0F, true);
      registerObject("cattail", new CattailObject("cattail", 32, new Color(99, 52, 50)), 1.0F, true);
      registerObject("reeds", new ReedsObject("reeds", 32, new Color(76, 95, 51)), 1.0F, true);
      registerObject("watergrass", new WaterPlantObject("watergrass", 32, new Color(63, 143, 59)), 1.0F, true);
      registerObject("cavewatergrass", new CaveWaterPlantObject("cavewatergrass", 32, new Color(83, 143, 89)), 1.0F, true);
      registerObject("deepswampgrass", new DeepSwampGrassObject(), -1.0F, true);
      registerObject("deepswamptallgrass", new DeepSwampTallGrassObject(), -1.0F, true);
      int nextPile = registerObject("snowpile3", new SnowPileObject(3, -1), -1.0F, true);
      int var159 = registerObject("snowpile2", new SnowPileObject(2, nextPile), -1.0F, true);
      int var160 = registerObject("snowpile1", new SnowPileObject(1, var159), -1.0F, true);
      registerObject("snowpile0", new SnowPileObject(0, var160), -1.0F, true);
      registerObject("leafpile", new LeafPileObject("leafpile", new Color(184, 75, 26)), -1.0F, true);
      cobWebID = registerObject("cobweb", new CobwebObject(), -1.0F, true);
      thornsID = registerObject("thorns", new ThornsObject(), 5.0F, true);
      registerObject("katanastone", new KatanaStoneObject(), -1.0F, true);
      LadderDownObject.registerLadderPair("ladder", "ladder", "caveladder", LevelIdentifier.CAVE_IDENTIFIER, new Color(138, 109, 37), Item.Rarity.NORMAL, 10);
      registerObject("trialentrance", new TrialEntranceObject(), 0.0F, false);
      registerObject("trialexit", new TrialExitObject(), 0.0F, false);
      registerObject("dungeonentrance", new DungeonEntranceObject(), 0.0F, false);
      registerObject("dungeonexit", new DungeonExitObject(), 0.0F, false);
      LadderDownObject.registerLadderPair(
         "deepladder", "deepladder", "deepcaveladder", LevelIdentifier.DEEP_CAVE_IDENTIFIER, new Color(11, 102, 127), Item.Rarity.UNCOMMON, 20
      );
      TempleEntranceObject.registerTempleEntrance();
      TempleExitObject.registerTempleExit();
      FallenAltarObject.registerFallenAltar();
      SpiderThroneObject.registerSpiderThrone();
      RunicBoulderObject.registerRunicBoulder();
      registerObject("smallrunestone", new SmallRuneStoneObject(), 10.0F, true);
      TicTacToeGameObject.registerTicTacToeObjects();
      registerObject("upgradestation", new UpgradeStationObject(), 100.0F, true);
      registerObject("salvagestation", new SalvageStationObject(), 100.0F, true);
      registerObject("coinstack", new RandomCoinStackObject(), 0.0F, false);
      registerObject("crate", new RandomCrateObject("crates"), -1.0F, true);
      registerObject("snowcrate", new RandomCrateObject("snowcrates"), -1.0F, true);
      registerObject("swampcrate", new RandomCrateObject("swampcrates"), -1.0F, true);
      registerObject("christmascrate", new RandomCrateObject("christmascrates"), -1.0F, true, false);
      registerObject("arcaniccrate", new RandomCrateObject("arcaniccrates"), -1.0F, true);
      registerObject("vase", new RandomVaseObject("vases"), -1.0F, true);
      registerObject("royaleggobject", new RoyalEggObject(), 0.0F, false);
      registerObject("swampspore", new SwampSporeObject(), 0.0F, false);
      registerObject("ancienttotem", new AncientTotemObject(), 0.0F, false);
      registerObject("templepedestal", new TemplePedestalObject(), 0.0F, false);
      registerObject("chieftainbonespikewall", new ChieftainBoneSpikeWallObject(new Color(191, 192, 166), ToolType.NONE), 0.0F, false);
      registerObject("bonespikewall", new BoneSpikeWallObject(new Color(191, 192, 166), ToolType.PICKAXE), 20.0F, true);
      registerObject("chieftainspedestal", new ChieftainsPedestalObject(), 0.0F, false);
      registerObject("settlementflag", new SettlementFlagObject(), 500.0F, true);
      registerObject("homestone", new HomestoneObject(), 400.0F, true);
      registerObject("waystone", new WaystoneObject(), 100.0F, true);
      registerObject("bannerstand", new BannerStandObject(), 200.0F, true);
      registerObject("bannerofpeace", new BannerOfPeaceObject(), 200.0F, true);
      registerObject("bannerofwar", new BannerOfWarObject(), 1000.0F, true);
      registerObject("challengersbanner", new ChallengersBannerObject(), 1000.0F, true);
      registerObject("teleportationstone", new TeleportationStoneObject(), 1000.0F, true);
      MissionBoardObject.registerMissionBoard();
      ShippingChestObject.registerShippingChest();
      registerObject("musicplayer", new MusicPlayerObject(), 800.0F, true);
      registerObject("christmastree", new ChristmasTreeObject(), 500.0F, true, false);
      registerObject("christmaswreath", new ChristmasWreathObject(), 50.0F, true, false);
      registerObject("fallingicicletrigger", new FallingIcicleTriggerObject(), 0.0F, false);
      registerObject("fallenicicle", new FallenIcicleObject(), -1.0F, true);
      registerObject("frostbanner", new BannerObject(), -1.0F, true);
      registerObject("largefrostbanner", new BannerObject(), -1.0F, true);
      registerObject("dryadbanner", new BannerObject(), -1.0F, true);
      registerObject("largedryadbanner", new BannerObject(), -1.0F, true);
      registerObject("eggcellentbanner", new BannerObject(), -1.0F, true);
      registerObject("largeeggcellentbanner", new BannerObject(-8), -1.0F, true);
      registerObject("fishianbanner", new BannerObject(), -1.0F, true);
      registerObject("largefishianbanner", new BannerObject(-8), -1.0F, true);
      registerObject("piratebanner", new BannerObject(), -1.0F, true);
      registerObject("largepiratebanner", new BannerObject(), -1.0F, true);
      registerObject("vampiricbanner", new BannerObject(), -1.0F, true);
      registerObject("largevampiricbanner", new BannerObject(-8), -1.0F, true);
      registerObject("voidvessel", new VoidVesselObject().setItemCategory("objects", "misc").setCraftingCategory("objects", "misc"), 0.0F, true);
      ElectricTowerObject.registerElectricTower();
      CoolingTowerObject.registerCoolingTower();
      registerObject("explosivebarrel", new ExplosiveBarrelObject(), 0.0F, false);
   }

   protected void onRegister(ObjectRegistry.ObjectRegistryElement object, int id, String stringID, boolean isReplace) {
      if (object.object instanceof HappinessObject && object.object.isMultiTileMaster()) {
         object.object.roomProperties.add("happinessobject");
         if (isReplace) {
            SettlerThoughtRegistry.replaceSettlerThought("happinessobject_" + stringID, new HappinessObjectSettlerThought((HappinessObject)object.object));
         } else {
            SettlerThoughtRegistry.registerSettlerThought("happinessobject_" + stringID, new HappinessObjectSettlerThought((HappinessObject)object.object));
         }
      }

      Item item = object.object.generateNewObjectItem();
      if (item != null) {
         ItemRegistry.replaceItem(
            object.object.getStringID(),
            item,
            object.itemBrokerValue,
            object.itemObtainable,
            object.itemCountInStats,
            object.itemObtainableInCreative,
            object.isObtainedByOtherItemStringIDs
         );
      }
   }

   @Override
   protected void onRegistryClose() {
      this.streamElements().map(e -> e.object).forEach(obj -> {
         String valid = obj.getMultiTile(0).checkValid();
         if (valid != null) {
            throw new IllegalStateException(obj.getStringID() + " has invalid multi tile setup: " + valid);
         }
      });
      this.streamElements().map(e -> e.object).forEach(GameObject::updateLocalDisplayName);

      for (ObjectRegistry.ObjectRegistryElement element : this.getElements()) {
         element.object.onObjectRegistryClosed();
      }

      stringIDs = instance.streamElements().map(e -> e.object.getStringID()).toArray(String[]::new);
   }

   public static int registerObject(String stringID, GameObject object, float itemBrokerValue, boolean itemObtainable) {
      return registerObject(stringID, object, itemBrokerValue, itemObtainable, itemObtainable);
   }

   public static int registerObject(
      String stringID, GameObject object, float itemBrokerValue, boolean itemObtainable, boolean itemCountInStats, String... isObtainedByOtherItemStringIDs
   ) {
      return registerObject(stringID, object, itemBrokerValue, itemObtainable, itemCountInStats, itemObtainable, isObtainedByOtherItemStringIDs);
   }

   public static int registerObject(
      String stringID,
      GameObject object,
      float itemBrokerValue,
      boolean itemObtainable,
      boolean itemCountInStats,
      boolean itemObtainableInCreative,
      String... isObtainedByOtherItemStringIDs
   ) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register objects");
      } else {
         return instance.register(
            stringID,
            new ObjectRegistry.ObjectRegistryElement(
               object, itemBrokerValue, itemObtainable, itemObtainableInCreative, itemCountInStats, Arrays.asList(isObtainedByOtherItemStringIDs)
            )
         );
      }
   }

   public static int replaceObject(String stringID, GameObject object, float itemBrokerValue, boolean itemObtainable) {
      return replaceObject(stringID, object, itemBrokerValue, itemObtainable, itemObtainable);
   }

   public static int replaceObject(
      String stringID, GameObject object, float itemBrokerValue, boolean itemObtainable, boolean itemCountInStats, String... isObtainedByOtherItemStringIDs
   ) {
      return replaceObject(stringID, object, itemBrokerValue, itemObtainable, itemCountInStats, itemObtainable, isObtainedByOtherItemStringIDs);
   }

   public static int replaceObject(
      String stringID,
      GameObject object,
      float itemBrokerValue,
      boolean itemObtainable,
      boolean itemCountInStats,
      boolean itemObtainableInCreative,
      String... isObtainedByOtherItemStringIDs
   ) {
      return instance.replace(
         stringID,
         new ObjectRegistry.ObjectRegistryElement(
            object, itemBrokerValue, itemObtainable, itemObtainableInCreative, itemCountInStats, Arrays.asList(isObtainedByOtherItemStringIDs)
         )
      );
   }

   public static GameObject getObject(int id) {
      if (id >= instance.size()) {
         id = 0;
      }

      ObjectRegistry.ObjectRegistryElement element = instance.getElement(id);
      if (element == null) {
         throw new NullPointerException(instance.objectCallName + " ID " + id + " seems to be missing or corrupted");
      } else {
         return element.object;
      }
   }

   public static int getObjectID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static String getObjectStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static GameObject getObject(String stringID) {
      ObjectRegistry.ObjectRegistryElement element = instance.getElement(stringID);
      if (element == null) {
         throw new NullPointerException(instance.objectCallName + " stringID " + stringID + " seems to be missing or corrupted");
      } else {
         return element.object;
      }
   }

   public static int getObjectIDRaw(String stringID) throws NoSuchElementException {
      return instance.getElementIDRaw(stringID);
   }

   public static Stream<GameObject> streamObjects() {
      return instance.streamElements().map(e -> e.object);
   }

   public static Iterable<GameObject> getObjects() {
      return streamObjects().collect(Collectors.toList());
   }

   public static int getObjectsCount() {
      return instance.size();
   }

   public static String[] getObjectStringIDs() {
      if (stringIDs == null) {
         throw new IllegalStateException("ObjectRegistry not yet closed");
      } else {
         return stringIDs;
      }
   }

   protected static class ObjectRegistryElement implements IDDataContainer {
      public final GameObject object;
      public final float itemBrokerValue;
      public final boolean itemObtainable;
      public final boolean itemObtainableInCreative;
      public final boolean itemCountInStats;
      public final List<String> isObtainedByOtherItemStringIDs;

      public ObjectRegistryElement(
         GameObject object,
         float itemBrokerValue,
         boolean itemObtainable,
         boolean itemObtainableInCreative,
         boolean itemCountInStats,
         List<String> isObtainedByOtherItemStringIDs
      ) {
         this.object = object;
         this.itemBrokerValue = itemBrokerValue;
         this.itemObtainable = itemObtainable;
         this.itemObtainableInCreative = itemObtainableInCreative;
         this.itemCountInStats = itemCountInStats;
         this.isObtainedByOtherItemStringIDs = isObtainedByOtherItemStringIDs;
      }

      @Override
      public IDData getIDData() {
         return this.object.idData;
      }
   }
}
