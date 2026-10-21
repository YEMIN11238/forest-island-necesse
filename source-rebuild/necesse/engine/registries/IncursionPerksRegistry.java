package necesse.engine.registries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;
import necesse.engine.incursionPerkTree.AlchemyShardsDropPotionsPerk;
import necesse.engine.incursionPerkTree.AllBannerBuffsPerk;
import necesse.engine.incursionPerkTree.ArcanicInvadersPerk;
import necesse.engine.incursionPerkTree.AscendedShardsCanDropPerk;
import necesse.engine.incursionPerkTree.BiggerArenaPerk;
import necesse.engine.incursionPerkTree.BossBringsInvadersPerk;
import necesse.engine.incursionPerkTree.CavelingPortalCanSpawnPerk;
import necesse.engine.incursionPerkTree.CavelingsCanSpawnPerk;
import necesse.engine.incursionPerkTree.CavelingsDropBetterLootPerk;
import necesse.engine.incursionPerkTree.ChanceToDropDoubleTabletsPerk;
import necesse.engine.incursionPerkTree.ChanceToMineFullClusterPerk;
import necesse.engine.incursionPerkTree.ChefInvadersPerk;
import necesse.engine.incursionPerkTree.CosmeticArmorRewardPerk;
import necesse.engine.incursionPerkTree.CratesDropAltarDustPerk;
import necesse.engine.incursionPerkTree.CrystalHollowTabletCanDropPerk;
import necesse.engine.incursionPerkTree.DoubleBossChancePerk;
import necesse.engine.incursionPerkTree.EmpowermentBuffsPerk;
import necesse.engine.incursionPerkTree.EnableBannerOfWarPerk;
import necesse.engine.incursionPerkTree.EnchantScrollsRewardPerk;
import necesse.engine.incursionPerkTree.EssenceVeinCanSpawnPerk;
import necesse.engine.incursionPerkTree.FiveTimesMoreCoinsChancePerk;
import necesse.engine.incursionPerkTree.FlamelingsCanDiePerk;
import necesse.engine.incursionPerkTree.GraveyardTabletCanDropPerk;
import necesse.engine.incursionPerkTree.ImprovedEmpowermentBuffsPerk;
import necesse.engine.incursionPerkTree.ImprovedEquipmentTiersOnePerk;
import necesse.engine.incursionPerkTree.ImprovedEquipmentTiersThreePerk;
import necesse.engine.incursionPerkTree.ImprovedEquipmentTiersTwoPerk;
import necesse.engine.incursionPerkTree.IncreasedBossLootPerk;
import necesse.engine.incursionPerkTree.IncursionLevelGearRewardPerk;
import necesse.engine.incursionPerkTree.IncursionPerk;
import necesse.engine.incursionPerkTree.MegaUpgradeShardVeinPerk;
import necesse.engine.incursionPerkTree.MobsDropAlchemyShardsPerk;
import necesse.engine.incursionPerkTree.MobsDropAltarDustPerk;
import necesse.engine.incursionPerkTree.MobsDropMoreAltarDustPerk;
import necesse.engine.incursionPerkTree.MobsDropUpgradeShardsPerk;
import necesse.engine.incursionPerkTree.MobsExplodeUpgradeShardsPerk;
import necesse.engine.incursionPerkTree.ModifiersAffectEnemiesPerk;
import necesse.engine.incursionPerkTree.MoonArenaTabletCanDropPerk;
import necesse.engine.incursionPerkTree.MoreCoinsRewardPerk;
import necesse.engine.incursionPerkTree.MoreExtractionsPerk;
import necesse.engine.incursionPerkTree.MoreHuntsPerk;
import necesse.engine.incursionPerkTree.MoreMagicWeaponsRewardPerk;
import necesse.engine.incursionPerkTree.MoreMeleeWeaponsRewardPerk;
import necesse.engine.incursionPerkTree.MoreRangedWeaponsRewardPerk;
import necesse.engine.incursionPerkTree.MoreSummonWeaponsRewardPerk;
import necesse.engine.incursionPerkTree.PetsRewardPerk;
import necesse.engine.incursionPerkTree.RavenInvadersPerk;
import necesse.engine.incursionPerkTree.RescueSettlerRewardPerk;
import necesse.engine.incursionPerkTree.RescuedSettlerMorePersonalitiesPerk;
import necesse.engine.incursionPerkTree.ScrapyardTabletCanDropPerk;
import necesse.engine.incursionPerkTree.SecondLifeBuffPerk;
import necesse.engine.incursionPerkTree.SlimeTabletCanDropPerk;
import necesse.engine.incursionPerkTree.SpiderCastleTabletCanDropPerk;
import necesse.engine.incursionPerkTree.SunArenaTabletCanDropPerk;
import necesse.engine.incursionPerkTree.ThirdLifeBuffPerk;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.util.GameUtils;

public class IncursionPerksRegistry extends GameRegistry<IncursionPerk> {
   public static final IncursionPerksRegistry instance = new IncursionPerksRegistry();
   public static IncursionPerk MOBS_DROP_ALTAR_DUST;
   public static IncursionPerk MOBS_DROP_MORE_ALTAR_DUST;
   public static IncursionPerk FLAMELINGS_CAN_DIE;
   public static IncursionPerk ALTAR_DUST_DROPS_ALCHEMY_SHARDS;
   public static IncursionPerk ALTAR_DUST_DROPS_UPGRADE_SHARDS;
   public static IncursionPerk MORE_EXTRACTIONS;
   public static IncursionPerk MORE_HUNTS;
   public static IncursionPerk RAVEN_INVADERS;
   public static IncursionPerk ARCANIC_INVADERS;
   public static IncursionPerk CHEF_INVADER;
   public static IncursionPerk MEGA_UPGRADE_SHARD_VEIN;
   public static IncursionPerk MORE_MELEE_WEAPONS_REWARD;
   public static IncursionPerk MORE_RANGED_WEAPONS_REWARD;
   public static IncursionPerk MORE_MAGIC_WEAPONS_REWARD;
   public static IncursionPerk MORE_SUMMON_WEAPONS_REWARD;
   public static IncursionPerk COSMETIC_ARMOR_REWARD;
   public static IncursionPerk SLIME_CAVE_TABLET_CAN_DROP;
   public static IncursionPerk GRAVEYARD_TABLET_CAN_DROP;
   public static IncursionPerk SPIDER_CASTLE_TABLET_CAN_DROP;
   public static IncursionPerk SUN_ARENA_TABLET_CAN_DROP;
   public static IncursionPerk MOON_ARENA_TABLET_CAN_DROP;
   public static IncursionPerk CRYSTAL_HOLLOW_TABLET_CAN_DROP;
   public static IncursionPerk SCRAPYARD_TABLET_CAN_DROP;
   public static IncursionPerk ASCENDED_SHARDS_CAN_DROP;
   public static IncursionPerk CAVELINGS_CAN_SPAWN;
   public static IncursionPerk CAVELING_PORTAL_CAN_SPAWN;
   public static IncursionPerk CAVELINGS_DROP_BETTER_LOOT;
   public static IncursionPerk ALCHEMY_SHARDS_DROP_POTIONS;
   public static IncursionPerk CHANCE_TO_MINE_FULL_CLUSTER;
   public static IncursionPerk ENCHANT_SCROLLS_REWARD;
   public static IncursionPerk MORE_COINS_REWARD;
   public static IncursionPerk PETS_REWARD;
   public static IncursionPerk CHANCE_TO_DROP_DOUBLE_TABLETS;
   public static IncursionPerk FIVE_TIMES_MORE_COINS_CHANCE;
   public static IncursionPerk RESCUE_SETTLER_REWARD;
   public static IncursionPerk RESCUED_SETTLER_MORE_PERSONALITIES;
   public static IncursionPerk BOSS_BRINGS_INVADERS;
   public static IncursionPerk DOUBLE_BOSS_CHANCE;
   public static IncursionPerk MODIFIERS_AFFECT_ENEMIES;
   public static IncursionPerk IMPROVED_EQUIPMENT_TIERS_ONE;
   public static IncursionPerk IMPROVED_EQUIPMENT_TIERS_TWO;
   public static IncursionPerk IMPROVED_EQUIPMENT_TIERS_THREE;
   public static IncursionPerk INCURSION_LEVEL_GEAR_REWARD;
   public static IncursionPerk EMPOWERMENT_BUFFS;
   public static IncursionPerk SECOND_LIFE_BUFF;
   public static IncursionPerk INCREASED_BOSS_LOOT;
   public static IncursionPerk IMPROVED_EMPOWERMENT_BUFFS;
   public static IncursionPerk THIRD_LIFE_BUFF;
   public static IncursionPerk ENABLE_BANNER_OF_WAR;
   public static IncursionPerk CRATES_DROP_ALTAR_DUST;
   public static IncursionPerk MOBS_EXPLODE_UPGRADE_SHARDS;
   public static IncursionPerk ESSENCE_VEINS_CAN_SPAWN;
   public static IncursionPerk BIGGER_ARENA;
   public static IncursionPerk ALL_BANNERS_BUFFS;
   private final int tier1Cost = 30;
   private final int tier2Cost = 50;
   private final int tier3Cost = 75;
   private final int tier4Cost = 125;
   private final int tier5Cost = 200;
   private final int tier6Cost = 320;
   private final int tier7Cost = 500;
   private final int tier8Cost = 800;
   private final int tier9Cost = 1300;
   private final int tier10Cost = 2000;

   private IncursionPerksRegistry() {
      super("Incursion Perks", 32762);
   }

   @Override
   public void registerCore() {
      MORE_MELEE_WEAPONS_REWARD = registerPerk("moremeleeweaponsreward", new MoreMeleeWeaponsRewardPerk(1, 30, 1));
      MORE_RANGED_WEAPONS_REWARD = registerPerk("morerangedweaponsreward", new MoreRangedWeaponsRewardPerk(1, 30, 2));
      MOBS_DROP_ALTAR_DUST = registerPerk("mobsdropaltardust", new MobsDropAltarDustPerk(1, 30, 3));
      MORE_MAGIC_WEAPONS_REWARD = registerPerk("moremagicweaponsreward", new MoreMagicWeaponsRewardPerk(1, 30, 4));
      MORE_COINS_REWARD = registerPerk("morecoinsreward", new MoreCoinsRewardPerk(1, 30, 5));
      MORE_SUMMON_WEAPONS_REWARD = registerPerk("moresummonweaponsreward", new MoreSummonWeaponsRewardPerk(1, 30, 6));
      ALTAR_DUST_DROPS_UPGRADE_SHARDS = registerPerk("mobsdropupgradeshards", new MobsDropUpgradeShardsPerk(2, 50, 1, MOBS_DROP_ALTAR_DUST));
      ALTAR_DUST_DROPS_ALCHEMY_SHARDS = registerPerk("mobsdropalchemyshards", new MobsDropAlchemyShardsPerk(2, 50, 2, MOBS_DROP_ALTAR_DUST));
      ALTAR_DUST_DROPS_UPGRADE_SHARDS.otherPerksThatLockThisPerk.add(ALTAR_DUST_DROPS_ALCHEMY_SHARDS);
      ALTAR_DUST_DROPS_ALCHEMY_SHARDS.otherPerksThatLockThisPerk.add(ALTAR_DUST_DROPS_UPGRADE_SHARDS);
      COSMETIC_ARMOR_REWARD = registerPerk("cosmeticarmorreward", new CosmeticArmorRewardPerk(2, 50, 3));
      SLIME_CAVE_TABLET_CAN_DROP = registerPerk("slimecavetabletcandrop", new SlimeTabletCanDropPerk(2, 100, 4, MOBS_DROP_ALTAR_DUST));
      FIVE_TIMES_MORE_COINS_CHANCE = registerPerk("fivetimesmorecoinschance", new FiveTimesMoreCoinsChancePerk(2, 50, 5, MORE_COINS_REWARD));
      CAVELINGS_CAN_SPAWN = registerPerk("cavelingscanspawn", new CavelingsCanSpawnPerk(2, 50, 6));
      GRAVEYARD_TABLET_CAN_DROP = registerPerk("graveyardtabletcandrop", new GraveyardTabletCanDropPerk(3, 150, 4, SLIME_CAVE_TABLET_CAN_DROP));
      MORE_EXTRACTIONS = registerPerk("moreextractions", new MoreExtractionsPerk(3, 75, 3));
      MORE_HUNTS = registerPerk("morehunts", new MoreHuntsPerk(3, 75, 5));
      MORE_EXTRACTIONS.otherPerksThatLockThisPerk.add(MORE_HUNTS);
      MORE_HUNTS.otherPerksThatLockThisPerk.add(MORE_EXTRACTIONS);
      ALCHEMY_SHARDS_DROP_POTIONS = registerPerk("alchemyshardsdroppotions", new AlchemyShardsDropPotionsPerk(3, 75, 2, ALTAR_DUST_DROPS_ALCHEMY_SHARDS));
      ALCHEMY_SHARDS_DROP_POTIONS.otherPerksThatLockThisPerk.add(ALTAR_DUST_DROPS_UPGRADE_SHARDS);
      MEGA_UPGRADE_SHARD_VEIN = registerPerk("megaupgradeshardveinperk", new MegaUpgradeShardVeinPerk(3, 75, 1, ALTAR_DUST_DROPS_UPGRADE_SHARDS));
      MEGA_UPGRADE_SHARD_VEIN.otherPerksThatLockThisPerk.add(ALTAR_DUST_DROPS_ALCHEMY_SHARDS);
      MOBS_DROP_MORE_ALTAR_DUST = registerPerk(
         "mobsdropmorealtardust", new MobsDropMoreAltarDustPerk(4, 125, 1, ALCHEMY_SHARDS_DROP_POTIONS, MEGA_UPGRADE_SHARD_VEIN)
      );
      RAVEN_INVADERS = registerPerk("raveninvaders", new RavenInvadersPerk(4, 125, 2));
      ARCANIC_INVADERS = registerPerk("arcanicinvaders", new ArcanicInvadersPerk(4, 125, 3));
      CHEF_INVADER = registerPerk("chefinvader", new ChefInvadersPerk(4, 125, 4));
      SPIDER_CASTLE_TABLET_CAN_DROP = registerPerk("spidercastletabletcandrop", new SpiderCastleTabletCanDropPerk(4, 225, 5, GRAVEYARD_TABLET_CAN_DROP));
      CAVELING_PORTAL_CAN_SPAWN = registerPerk("cavelingportalcanspawn", new CavelingPortalCanSpawnPerk(4, 125, 6, CAVELINGS_CAN_SPAWN));
      CHANCE_TO_MINE_FULL_CLUSTER = registerPerk("chancetominefullcluster", new ChanceToMineFullClusterPerk(5, 200, 1, MOBS_DROP_MORE_ALTAR_DUST));
      ENCHANT_SCROLLS_REWARD = registerPerk("enchantsreward", new EnchantScrollsRewardPerk(5, 200, 2));
      RESCUE_SETTLER_REWARD = registerPerk("rescuesettlerreward", new RescueSettlerRewardPerk(5, 200, 4));
      MOON_ARENA_TABLET_CAN_DROP = registerPerk("moonarenatabletcandrop", new MoonArenaTabletCanDropPerk(5, 320, 5, SPIDER_CASTLE_TABLET_CAN_DROP));
      CAVELINGS_DROP_BETTER_LOOT = registerPerk("cavelingsbetterloot", new CavelingsDropBetterLootPerk(5, 200, 6, CAVELING_PORTAL_CAN_SPAWN));
      CHANCE_TO_DROP_DOUBLE_TABLETS = registerPerk("doubletabletchancedrop", new ChanceToDropDoubleTabletsPerk(6, 320, 1));
      BIGGER_ARENA = registerPerk("biggerarena", new BiggerArenaPerk(6, 320, 2));
      BOSS_BRINGS_INVADERS = registerPerk("bossbringsinvaders", new BossBringsInvadersPerk(6, 320, 3, RAVEN_INVADERS, ARCANIC_INVADERS, CHEF_INVADER));
      RESCUED_SETTLER_MORE_PERSONALITIES = registerPerk(
         "rescuedsettlermorepersonalities", new RescuedSettlerMorePersonalitiesPerk(6, 320, 4, RESCUE_SETTLER_REWARD)
      );
      SUN_ARENA_TABLET_CAN_DROP = registerPerk("sunarenatabletcandrop", new SunArenaTabletCanDropPerk(6, 470, 5, MOON_ARENA_TABLET_CAN_DROP));
      IMPROVED_EQUIPMENT_TIERS_ONE = registerPerk("improvedtiersone", new ImprovedEquipmentTiersOnePerk(6, 320, 6));
      CRATES_DROP_ALTAR_DUST = registerPerk("cratesdropaltardust", new CratesDropAltarDustPerk(7, 500, 1));
      FLAMELINGS_CAN_DIE = registerPerk("flamelingscandie", new FlamelingsCanDiePerk(7, 500, 2));
      ALL_BANNERS_BUFFS = registerPerk("allbannerbuffs", new AllBannerBuffsPerk(7, 500, 4));
      CRYSTAL_HOLLOW_TABLET_CAN_DROP = registerPerk("crystalhollowtabletcandrop", new CrystalHollowTabletCanDropPerk(7, 680, 5, SUN_ARENA_TABLET_CAN_DROP));
      MOBS_EXPLODE_UPGRADE_SHARDS = registerPerk("mobsexplodeupgradeshards", new MobsExplodeUpgradeShardsPerk(8, 800, 1, CRATES_DROP_ALTAR_DUST));
      DOUBLE_BOSS_CHANCE = registerPerk("doublebosschance", new DoubleBossChancePerk(8, 800, 3, BOSS_BRINGS_INVADERS));
      SECOND_LIFE_BUFF = registerPerk("secondlifebuff", new SecondLifeBuffPerk(8, 800, 2, DOUBLE_BOSS_CHANCE));
      EMPOWERMENT_BUFFS = registerPerk("empowermentbuffs", new EmpowermentBuffsPerk(8, 800, 4, DOUBLE_BOSS_CHANCE));
      EMPOWERMENT_BUFFS.otherPerksThatLockThisPerk.add(SECOND_LIFE_BUFF);
      SECOND_LIFE_BUFF.otherPerksThatLockThisPerk.add(EMPOWERMENT_BUFFS);
      IMPROVED_EQUIPMENT_TIERS_TWO = registerPerk("improvedtierstwo", new ImprovedEquipmentTiersTwoPerk(8, 800, 6, IMPROVED_EQUIPMENT_TIERS_ONE));
      SCRAPYARD_TABLET_CAN_DROP = registerPerk("scrapyardtabletcandrop", new ScrapyardTabletCanDropPerk(8, 1010, 5, CRYSTAL_HOLLOW_TABLET_CAN_DROP));
      ESSENCE_VEINS_CAN_SPAWN = registerPerk("essenceveinscanspawn", new EssenceVeinCanSpawnPerk(9, 1300, 1, MOBS_EXPLODE_UPGRADE_SHARDS));
      THIRD_LIFE_BUFF = registerPerk("thirdlifebuff", new ThirdLifeBuffPerk(9, 1300, 2, SECOND_LIFE_BUFF));
      IMPROVED_EMPOWERMENT_BUFFS = registerPerk("improvedempowermentbuffs", new ImprovedEmpowermentBuffsPerk(9, 1300, 4, EMPOWERMENT_BUFFS));
      IMPROVED_EMPOWERMENT_BUFFS.otherPerksThatLockThisPerk.add(SECOND_LIFE_BUFF);
      THIRD_LIFE_BUFF.otherPerksThatLockThisPerk.add(EMPOWERMENT_BUFFS);
      MODIFIERS_AFFECT_ENEMIES = registerPerk("modifiersaffectenemiesperk", new ModifiersAffectEnemiesPerk(9, 1300, 5));
      INCURSION_LEVEL_GEAR_REWARD = registerPerk("incursionlevelgearreward", new IncursionLevelGearRewardPerk(9, 1300, 6, IMPROVED_EQUIPMENT_TIERS_TWO));
      ENABLE_BANNER_OF_WAR = registerPerk("enablebannerofwar", new EnableBannerOfWarPerk(10, 2000, 1));
      PETS_REWARD = registerPerk("petsreward", new PetsRewardPerk(10, 2000, 2));
      INCREASED_BOSS_LOOT = registerPerk("increasedbossloot", new IncreasedBossLootPerk(10, 2000, 3, DOUBLE_BOSS_CHANCE));
      ASCENDED_SHARDS_CAN_DROP = registerPerk("ascendedshardsdrop", new AscendedShardsCanDropPerk(10, 2300, 4));
      IMPROVED_EQUIPMENT_TIERS_THREE = registerPerk("improvedtiersthree", new ImprovedEquipmentTiersThreePerk(10, 2000, 6, INCURSION_LEVEL_GEAR_REWARD));
   }

   protected void onRegister(IncursionPerk object, int id, String stringID, boolean isReplace) {
      for (IncursionPerk element : this.getElements()) {
         if (element != object && element.tier == object.tier && element.xPositionOnPerkTree == object.xPositionOnPerkTree) {
            System.err
               .println(
                  "Error registering incursion perk with stringID "
                     + stringID
                     + ": "
                     + element.getStringID()
                     + " is already registered at tier "
                     + object.tier
                     + " and x position "
                     + object.xPositionOnPerkTree
               );
            break;
         }
      }
   }

   @Override
   protected void onRegistryClose() {
      for (IncursionPerk element : this.getElements()) {
         element.onChallengeRegistryClosed();
      }
   }

   public static IncursionPerk registerPerk(String stringID, IncursionPerk incursionPerk) {
      if (LoadedMod.isRunningModClientSide()) {
         throw new IllegalStateException("Client/server only mods cannot register perks");
      }

      instance.register(stringID, incursionPerk);
      return incursionPerk;
   }

   public static Iterable<IncursionPerk> getPerks() {
      return instance.getElements();
   }

   public static ArrayList<IncursionPerk> getPerksAtTier(int tier) {
      ArrayList<IncursionPerk> perksAtTier = new ArrayList<>();

      for (IncursionPerk perk : instance.getElements()) {
         if (perk.tier == tier) {
            perksAtTier.add(perk);
         }
      }

      return perksAtTier;
   }

   public static boolean doesPerkExists(String stringID) {
      try {
         return instance.getElementIDRaw(stringID) >= 0;
      } catch (NoSuchElementException e) {
         return false;
      }
   }

   public static int getTotalPerkCount() {
      return instance.size();
   }

   public static IncursionPerk getPerk(String id) {
      return instance.getElement(id);
   }

   public static IncursionPerk getPerk(int id) {
      return instance.getElement(id);
   }

   public static int getPerkID(String stringID) {
      return instance.getElementID(stringID);
   }

   public static String getPerkStringID(int id) {
      return instance.getElementStringID(id);
   }

   public static Iterable<IncursionPerk> fromPerkIDs(Iterable<Integer> perkIDs) {
      return GameUtils.mapIterable(perkIDs.iterator(), IncursionPerksRegistry::getPerk);
   }

   public static Iterable<IncursionPerk> fromPerkIDsSorted(Collection<Integer> perkIDs) {
      return () -> perkIDs.stream().sorted().map(IncursionPerksRegistry::getPerk).iterator();
   }
}
