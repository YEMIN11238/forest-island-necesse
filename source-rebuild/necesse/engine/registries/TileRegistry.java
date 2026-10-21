package necesse.engine.registries;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import necesse.engine.GameLoadingScreen;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.LoadedMod;
import necesse.inventory.item.placeableItem.tileItem.TileItem;
import necesse.level.gameTile.AncientRootsTile;
import necesse.level.gameTile.ArcanicFloorTile;
import necesse.level.gameTile.AscendedCorruptionTile;
import necesse.level.gameTile.AscendedGrowthTile;
import necesse.level.gameTile.AscendedVoidTile;
import necesse.level.gameTile.AshTile;
import necesse.level.gameTile.BasaltRockTile;
import necesse.level.gameTile.ChromaKeyTile;
import necesse.level.gameTile.CryptAshTile;
import necesse.level.gameTile.CrystalGravelTile;
import necesse.level.gameTile.CrystalTile;
import necesse.level.gameTile.DeepIceTile;
import necesse.level.gameTile.DeepRockTile;
import necesse.level.gameTile.DeepSandstoneTile;
import necesse.level.gameTile.DeepSnowRockTile;
import necesse.level.gameTile.DeepSwampRockTile;
import necesse.level.gameTile.DirtTile;
import necesse.level.gameTile.DungeonFloorTile;
import necesse.level.gameTile.EmptyTile;
import necesse.level.gameTile.FactoryFloorTile;
import necesse.level.gameTile.FarmlandTile;
import necesse.level.gameTile.GameTile;
import necesse.level.gameTile.GraniteTile;
import necesse.level.gameTile.GrassTile;
import necesse.level.gameTile.GravelTile;
import necesse.level.gameTile.IceTile;
import necesse.level.gameTile.LavaPathTile;
import necesse.level.gameTile.LavaTile;
import necesse.level.gameTile.MoonPath;
import necesse.level.gameTile.MudTile;
import necesse.level.gameTile.OozeLiquidTile;
import necesse.level.gameTile.OvergrownGrassTile;
import necesse.level.gameTile.OvergrownPlainsGrassTile;
import necesse.level.gameTile.OvergrownSwampGrassTile;
import necesse.level.gameTile.PathTiledTile;
import necesse.level.gameTile.PlainsGrassTile;
import necesse.level.gameTile.PuddleCobbleTile;
import necesse.level.gameTile.QuicksandTile;
import necesse.level.gameTile.RavenFloorTile;
import necesse.level.gameTile.RockTile;
import necesse.level.gameTile.RustGravelTile;
import necesse.level.gameTile.SandBrickTile;
import necesse.level.gameTile.SandGravelTile;
import necesse.level.gameTile.SandTile;
import necesse.level.gameTile.SandstoneTile;
import necesse.level.gameTile.SimpleFloorTile;
import necesse.level.gameTile.SimpleTerrainTile;
import necesse.level.gameTile.SimpleTiledFloorTile;
import necesse.level.gameTile.SlimeLiquidTile;
import necesse.level.gameTile.SlimeRockTile;
import necesse.level.gameTile.SnowRockTile;
import necesse.level.gameTile.SnowTile;
import necesse.level.gameTile.SpiderNestTile;
import necesse.level.gameTile.SpiritWaterTile;
import necesse.level.gameTile.SwampGrassTile;
import necesse.level.gameTile.SwampRockTile;
import necesse.level.gameTile.WaterTile;

public class TileRegistry extends GameRegistry<TileRegistry.TileRegistryElement> {
   public static final TileRegistry instance = new TileRegistry();
   private static String[] stringIDs = null;
   public static int waterID;
   public static int emptyID;
   public static int dirtID;
   public static int sandID;
   public static int gravelID;
   public static int lavaID;
   public static int quicksandID;
   public static int snowID;
   public static int iceID;
   public static int deepIceID;
   public static int mudID;
   public static int spiderNestID;
   public static int cryptAshID;
   public static int farmlandID;
   public static int strawID;
   public static int puddleCobble;
   public static int spiritWaterID;
   public static int ancientRootsID;
   public static int grassID;
   public static int overgrownGrassID;
   public static int plainsGrassID;
   public static int overgrownPlainsGrassID;
   public static int swampGrassID;
   public static int overgrownSwampGrassID;
   public static int ashID;
   public static int woodFloorID;
   public static int pineFloorID;
   public static int palmFloorID;
   public static int willowFloorID;
   public static int dryadFloorID;
   public static int woodPathID;
   public static int stoneFloorID;
   public static int stoneBrickFloorID;
   public static int stoneTiledFloorID;
   public static int stonePathID;
   public static int rockID;
   public static int snowStoneFloorID;
   public static int snowStoneBrickFloorID;
   public static int snowStonePathID;
   public static int snowRockID;
   public static int swampStoneFloorID;
   public static int swampStoneBrickFloorID;
   public static int swampStonePathID;
   public static int swampRockID;
   public static int sandstoneFloorID;
   public static int sandBrickID;
   public static int sandstoneBrickFloorID;
   public static int sandstonePathID;
   public static int sandstoneID;
   public static int graniteFloorID;
   public static int graniteBrickFloorID;
   public static int granitePathID;
   public static int graniteRockID;
   public static int deepStoneFloorID;
   public static int deepStoneBrickFloorID;
   public static int deepStoneTiledFloorID;
   public static int deepRockID;
   public static int deepSnowStoneFloorID;
   public static int deepSnowStoneBrickFloorID;
   public static int deepSnowRockID;
   public static int deepSwampStoneFloorID;
   public static int deepSwampStoneBrickFloorID;
   public static int deepSwampRockID;
   public static int basaltFloorID;
   public static int basaltPathID;
   public static int basaltRockID;
   public static int dryadPathID;
   public static int deepSandstoneID;
   public static int dungeonFloorID;
   public static int bambooFloorID;
   public static int deadWoodFloorID;
   public static int ravenFloorID;
   public static int arcanicFloorID;
   public static int arcanicPathID;
   public static int rustGravelID;
   public static int ascendedCorruptionID;
   public static int ascendedGrowthID;
   public static int ascendedVoidID;

   private TileRegistry() {
      super("Tile", 32762);
   }

   @Override
   public void registerCore() {
      GameLoadingScreen.drawLoadingString(Localization.translate("loading", "tiles"));
      emptyID = registerTile("emptytile", new EmptyTile(), 0.0F, false);
      dirtID = registerTile("dirttile", new DirtTile(), 0.0F, false, false, true);
      waterID = registerTile("watertile", new WaterTile(), 20.0F, true);
      grassID = registerTile("grasstile", new GrassTile(), 0.0F, false, false, true);
      overgrownGrassID = registerTile("overgrowngrasstile", new OvergrownGrassTile(), 0.0F, false, false, true);
      woodFloorID = registerTile("woodfloor", new SimpleFloorTile("woodfloor", new Color(118, 71, 20)), -1.0F, true);
      woodPathID = registerTile("woodpathtile", new PathTiledTile("woodpath", new Color(103, 61, 18)), -1.0F, true);
      rockID = registerTile("rocktile", new RockTile(), -1.0F, true);
      stoneFloorID = registerTile("stonefloor", new SimpleFloorTile("stonefloor", new Color(125, 136, 146)), -1.0F, true);
      stoneBrickFloorID = registerTile("stonebrickfloor", new SimpleFloorTile("stonebrickfloor", new Color(125, 136, 146)), -1.0F, true);
      stoneTiledFloorID = registerTile("stonetiledfloor", new SimpleFloorTile("stonetiledfloor", new Color(125, 136, 146)), -1.0F, true);
      stonePathID = registerTile("stonepathtile", new PathTiledTile("stonepath", new Color(110, 117, 127)), -1.0F, true);
      deepRockID = registerTile("deeprocktile", new DeepRockTile(), -1.0F, true);
      deepStoneFloorID = registerTile("deepstonefloor", new SimpleFloorTile("deepstonefloor", new Color(52, 67, 81)), -1.0F, true);
      deepStoneBrickFloorID = registerTile("deepstonebrickfloor", new SimpleFloorTile("deepstonebrickfloor", new Color(57, 59, 60)), -1.0F, true);
      deepStoneTiledFloorID = registerTile("deepstonetiledfloor", new SimpleFloorTile("deepstonetiledfloor", new Color(66, 73, 75)), -1.0F, true);
      snowID = registerTile("snowtile", new SnowTile(), 5.0F, true);
      pineFloorID = registerTile("pinefloor", new SimpleFloorTile("pinefloor", new Color(125, 83, 27)), -1.0F, true);
      snowRockID = registerTile("snowrocktile", new SnowRockTile(), -1.0F, true);
      iceID = registerTile("icetile", new IceTile(), 5.0F, true);
      snowStoneFloorID = registerTile("snowstonefloor", new SimpleFloorTile("snowstonefloor", new Color(66, 102, 133)), -1.0F, true);
      snowStoneBrickFloorID = registerTile("snowstonebrickfloor", new SimpleFloorTile("snowstonebrickfloor", new Color(74, 112, 156)), -1.0F, true);
      snowStonePathID = registerTile("snowstonepathtile", new PathTiledTile("snowstonepath", new Color(150, 178, 217)), -1.0F, true);
      deepSnowRockID = registerTile("deepsnowrocktile", new DeepSnowRockTile(), -1.0F, true);
      deepIceID = registerTile("deepicetile", new DeepIceTile(), 5.0F, true);
      deepSnowStoneFloorID = registerTile("deepsnowstonefloor", new SimpleFloorTile("deepsnowstonefloor", new Color(52, 72, 80)), -1.0F, true);
      deepSnowStoneBrickFloorID = registerTile("deepsnowstonebrickfloor", new SimpleFloorTile("deepsnowstonebrickfloor", new Color(75, 78, 80)), -1.0F, true);
      dungeonFloorID = registerTile("dungeonfloor", new DungeonFloorTile(), 4.0F, true);
      plainsGrassID = registerTile("plainsgrasstile", new PlainsGrassTile(), 0.0F, false, false, true);
      overgrownPlainsGrassID = registerTile("overgrownplainsgrasstile", new OvergrownPlainsGrassTile(), 0.0F, false, false, true);
      graniteRockID = registerTile("graniterocktile", new GraniteTile(), -1.0F, true);
      graniteFloorID = registerTile("granitefloor", new SimpleFloorTile("granitefloor", new Color(107, 53, 51)), -1.0F, true);
      graniteBrickFloorID = registerTile("granitebrickfloor", new SimpleFloorTile("granitebrickfloor", new Color(107, 53, 51)), -1.0F, true);
      granitePathID = registerTile("granitepathtile", new PathTiledTile("granitepath", new Color(135, 70, 67)), -1.0F, true);
      basaltRockID = registerTile("basaltrocktile", new BasaltRockTile(), -1.0F, true);
      spiritWaterID = registerTile("spiritwatertile", new SpiritWaterTile(), 20.0F, true);
      basaltFloorID = registerTile("basaltfloor", new SimpleFloorTile("basaltfloor", new Color(60, 49, 81)), -1.0F, true);
      basaltPathID = registerTile("basaltpathtile", new PathTiledTile("basaltpath", new Color(52, 42, 62)), -1.0F, true);
      dryadFloorID = registerTile("dryadfloor", new SimpleTiledFloorTile("dryadfloor", new Color(70, 53, 43)), -1.0F, true);
      dryadPathID = registerTile("dryadpath", new PathTiledTile("dryadpath", new Color(31, 35, 45)), -1.0F, true);
      swampGrassID = registerTile("swampgrasstile", new SwampGrassTile(), 0.0F, false, false, true);
      overgrownSwampGrassID = registerTile("overgrownswampgrasstile", new OvergrownSwampGrassTile(), 0.0F, false, false, true);
      willowFloorID = registerTile("willowfloor", new SimpleTiledFloorTile("willowfloor", new Color(94, 55, 47)), -1.0F, true);
      swampRockID = registerTile("swamprocktile", new SwampRockTile(), -1.0F, true);
      swampStoneFloorID = registerTile("swampstonefloor", new SimpleFloorTile("swampstonefloor", new Color(63, 96, 80)), -1.0F, true);
      swampStoneBrickFloorID = registerTile("swampstonebrickfloor", new SimpleFloorTile("swampstonebrickfloor", new Color(63, 96, 80)), -1.0F, true);
      swampStonePathID = registerTile("swampstonepathtile", new PathTiledTile("swampstonepath", new Color(57, 77, 60)), -1.0F, true);
      deepSwampRockID = registerTile("deepswamprocktile", new DeepSwampRockTile(), -1.0F, true);
      puddleCobble = registerTile("puddlecobble", new PuddleCobbleTile(), 5.0F, true);
      deepSwampStoneFloorID = registerTile("deepswampstonefloor", new SimpleFloorTile("deepswampstonefloor", new Color(53, 80, 74)), -1.0F, true);
      deepSwampStoneBrickFloorID = registerTile("deepswampstonebrickfloor", new SimpleFloorTile("deepswampstonebrickfloor", new Color(43, 63, 60)), -1.0F, true);
      bambooFloorID = registerTile("bamboofloor", new SimpleFloorTile("bamboofloor", new Color(116, 74, 71)), -1.0F, true);
      sandID = registerTile("sandtile", new SandTile(), 0.0F, true);
      palmFloorID = registerTile("palmfloor", new SimpleTiledFloorTile("palmfloor", new Color(119, 83, 26)), -1.0F, true);
      sandstoneID = registerTile("sandstonetile", new SandstoneTile(), -1.0F, true);
      sandBrickID = registerTile("sandbrick", new SandBrickTile(), -1.0F, true);
      quicksandID = registerTile("quicksandtile", new QuicksandTile(), 20.0F, true);
      sandstoneFloorID = registerTile("sandstonefloor", new SimpleFloorTile("sandstonefloor", new Color(160, 136, 94)), -1.0F, true);
      sandstoneBrickFloorID = registerTile("sandstonebrickfloor", new SimpleFloorTile("sandstonebrickfloor", new Color(160, 136, 94)), -1.0F, true);
      sandstonePathID = registerTile("sandstonepathtile", new PathTiledTile("sandstonepath", new Color(146, 121, 87)), -1.0F, true);
      deepSandstoneID = registerTile("deepsandstonetile", new DeepSandstoneTile(), -1.0F, true);
      registerTile("slimerocktile", new SlimeRockTile(), -1.0F, true);
      registerTile("liquidslimetile", new SlimeLiquidTile(), 20.0F, true);
      deadWoodFloorID = registerTile("deadwoodfloor", new SimpleFloorTile("deadwoodfloor", new Color(59, 43, 40)), -1.0F, true);
      registerTile("cryptpath", new PathTiledTile("cryptpath", new Color(66, 66, 78)), 5.0F, true);
      registerTile("spidercastlefloor", new SimpleFloorTile("spidercastlefloor", new Color(47, 44, 71)), 5.0F, true);
      registerTile("spidercobbletile", new SimpleFloorTile("spidercobbletile", new Color(50, 90, 104)), 5.0F, true);
      registerTile("spidercastlecarpet", new PathTiledTile("spidercastlecarpet", new Color(151, 84, 117)), 5.0F, true);
      ravenFloorID = registerTile("ravenfloor", new RavenFloorTile(), 5.0F, true);
      arcanicFloorID = registerTile("arcanicfloor", new ArcanicFloorTile(), 5.0F, true);
      arcanicPathID = registerTile("arcanicpath", new PathTiledTile("arcanicpath", new Color(105, 138, 130)), 5.0F, true);
      registerTile("moonpath", new MoonPath(), 5.0F, true);
      registerTile("darkmoonpath", new PathTiledTile("darkmoonpath", new Color(40, 103, 108)), 5.0F, true);
      registerTile("darkfullmoonpath", new PathTiledTile("darkfullmoonpath", new Color(86, 80, 111)), 5.0F, true);
      registerTile("dawnpath", new PathTiledTile("dawnpath", new Color(236, 197, 129)), 5.0F, true);
      registerTile("lavapath", new LavaPathTile(), 5.0F, true);
      registerTile("crystaltile", new CrystalTile(), 5.0F, true);
      registerTile("ancientruinfloor", new SimpleFloorTile("ancientruinfloor", new Color(114, 90, 81)), 5.0F, true);
      rustGravelID = registerTile("rustgravel", new RustGravelTile(), 2.0F, true);
      registerTile("scrapfloor", new SimpleFloorTile("scrapfloor", new Color(67, 69, 73)), 5.0F, true);
      registerTile("junkfloor", new SimpleFloorTile("junkfloor", new Color(67, 69, 73)), 5.0F, true);
      registerTile("factoryfloor", new FactoryFloorTile(), 5.0F, true);
      registerTile("liquidoozetile", new OozeLiquidTile(), 20.0F, true);
      ascendedCorruptionID = registerTile("ascendedcorruption", new AscendedCorruptionTile(), 0.0F, false, false, true);
      ascendedGrowthID = registerTile("ascendedgrowth", new AscendedGrowthTile(), 0.0F, false, false, true);
      ascendedVoidID = registerTile("ascendedvoid", new AscendedVoidTile(), 0.0F, true, true, true);
      mudID = registerTile("mudtile", new MudTile(), 5.0F, true);
      ancientRootsID = registerTile("ancientrootstile", new AncientRootsTile(), -1.0F, true);
      gravelID = registerTile("graveltile", new GravelTile(), -1.0F, true);
      registerTile("sandgraveltile", new SandGravelTile(), 2.0F, true);
      registerTile("amethystgravel", new CrystalGravelTile("amethystgravel", new Color(88, 74, 127)), 10.0F, true);
      registerTile("sapphiregravel", new CrystalGravelTile("sapphiregravel", new Color(11, 102, 127)), 10.0F, true);
      registerTile("emeraldgravel", new CrystalGravelTile("emeraldgravel", new Color(0, 98, 81)), 10.0F, true);
      registerTile("rubygravel", new CrystalGravelTile("rubygravel", new Color(113, 43, 59)), 10.0F, true);
      registerTile("topazgravel", new CrystalGravelTile("topazgravel", new Color(113, 99, 43)), 10.0F, true);
      lavaID = registerTile("lavatile", new LavaTile(), 20.0F, true);
      spiderNestID = registerTile("spidernesttile", new SpiderNestTile(), 0.0F, true);
      cryptAshID = registerTile("cryptash", new CryptAshTile(), 0.0F, true);
      farmlandID = registerTile("farmland", new FarmlandTile(), 5.0F, true);
      strawID = registerTile("strawtile", new SimpleTerrainTile("strawtile", new Color(255, 207, 5)), 2.0F, true);
      ashID = registerTile("ashtile", new AshTile(), 0.0F, false, false, true);
      registerTile("chromakeygreen", new ChromaKeyTile(new Color(0, 255, 0)), 0.0F, false, false, true);
      registerTile("chromakeyblue", new ChromaKeyTile(new Color(0, 0, 255)), 0.0F, false, false, true);
      registerTile("chromakeycyan", new ChromaKeyTile(new Color(0, 255, 255)), 0.0F, false, false, true);
      registerTile("chromakeypink", new ChromaKeyTile(new Color(255, 0, 255)), 0.0F, false, false, true);
   }

   protected void onRegister(TileRegistry.TileRegistryElement object, int id, String stringID, boolean isReplace) {
      TileItem tItem = object.tile.generateNewTileItem();
      if (tItem != null) {
         ItemRegistry.replaceItem(
            object.tile.getStringID(),
            tItem,
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
      this.streamElements().map(e -> e.tile).forEach(GameTile::updateLocalDisplayName);

      for (TileRegistry.TileRegistryElement element : this.getElements()) {
         element.tile.onTileRegistryClosed();
      }

      stringIDs = instance.streamElements().map(e -> e.tile.getStringID()).toArray(String[]::new);
   }

   public static int registerTile(String stringID, GameTile tile, float itemBrokerValue, boolean itemObtainable) {
      return registerTile(stringID, tile, itemBrokerValue, itemObtainable, itemObtainable);
   }

   public static int registerTile(
      String stringID, GameTile tile, float itemBrokerValue, boolean itemObtainable, boolean itemCountInStats, String... isObtainedByOtherItemStringIDs
   ) {
      return registerTile(stringID, tile, itemBrokerValue, itemObtainable, itemCountInStats, itemObtainable, isObtainedByOtherItemStringIDs);
   }

   public static int registerTile(
      String stringID,
      GameTile tile,
      float itemBrokerValue,
      boolean itemObtainable,
      boolean itemCountInStats,
      boolean itemObtainableInCreative,
      String... isObtainedByOtherItemStringIDs
   ) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register tiles");
      } else {
         return instance.register(
            stringID,
            new TileRegistry.TileRegistryElement(
               tile, itemBrokerValue, itemObtainable, itemObtainableInCreative, itemCountInStats, Arrays.asList(isObtainedByOtherItemStringIDs)
            )
         );
      }
   }

   public static int replaceTile(String stringID, GameTile tile, float itemBrokerValue, boolean itemObtainable) {
      return replaceTile(stringID, tile, itemBrokerValue, itemObtainable, itemObtainable);
   }

   public static int replaceTile(
      String stringID, GameTile tile, float itemBrokerValue, boolean itemObtainable, boolean itemCountInStats, String... isObtainedByOtherItemStringIDs
   ) {
      return replaceTile(stringID, tile, itemBrokerValue, itemObtainable, itemCountInStats, itemObtainable, isObtainedByOtherItemStringIDs);
   }

   public static int replaceTile(
      String stringID,
      GameTile tile,
      float itemBrokerValue,
      boolean itemObtainable,
      boolean itemCountInStats,
      boolean itemObtainableInCreative,
      String... isObtainedByOtherItemStringIDs
   ) {
      return instance.replace(
         stringID,
         new TileRegistry.TileRegistryElement(
            tile, itemBrokerValue, itemObtainable, itemObtainableInCreative, itemCountInStats, Arrays.asList(isObtainedByOtherItemStringIDs)
         )
      );
   }

   public static Stream<GameTile> streamTiles() {
      return instance.streamElements().map(e -> e.tile);
   }

   public static Iterable<GameTile> getTiles() {
      return streamTiles().collect(Collectors.toList());
   }

   public static GameTile getTile(int id) {
      if (id >= instance.size()) {
         id = 0;
      }

      TileRegistry.TileRegistryElement element = instance.getElement(id);
      if (element == null) {
         throw new NullPointerException(instance.objectCallName + " ID " + id + " seems to be missing or corrupted");
      } else {
         return element.tile;
      }
   }

   public static int getTileID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static GameTile getTile(String stringID) {
      TileRegistry.TileRegistryElement element = instance.getElement(stringID);
      if (element == null) {
         throw new NullPointerException(instance.objectCallName + " stringID " + stringID + " seems to be missing or corrupted");
      } else {
         return element.tile;
      }
   }

   public static String getTileStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static String[] getTileStringIDs() {
      if (stringIDs == null) {
         throw new IllegalStateException("TileRegistry not yet closed");
      } else {
         return stringIDs;
      }
   }

   protected static class TileRegistryElement implements IDDataContainer {
      public final GameTile tile;
      public final float itemBrokerValue;
      public final boolean itemObtainable;
      public final boolean itemObtainableInCreative;
      public final boolean itemCountInStats;
      public final List<String> isObtainedByOtherItemStringIDs;

      public TileRegistryElement(
         GameTile tile,
         float itemBrokerValue,
         boolean itemObtainable,
         boolean itemObtainableInCreative,
         boolean itemCountInStats,
         List<String> isObtainedByOtherItemStringIDs
      ) {
         this.tile = tile;
         this.itemBrokerValue = itemBrokerValue;
         this.itemObtainable = itemObtainable;
         this.itemObtainableInCreative = itemObtainableInCreative;
         this.itemCountInStats = itemCountInStats;
         this.isObtainedByOtherItemStringIDs = isObtainedByOtherItemStringIDs;
      }

      @Override
      public IDData getIDData() {
         return this.tile.idData;
      }
   }
}
