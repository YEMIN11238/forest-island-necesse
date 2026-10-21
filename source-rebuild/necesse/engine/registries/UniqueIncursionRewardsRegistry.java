package necesse.engine.registries;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import necesse.engine.GameLog;
import necesse.engine.incursionPerkTree.IncursionPerk;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.TicketSystemList;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.miscItem.GatewayTabletItem;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootList;
import necesse.inventory.lootTable.LootTablePresets;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.inventory.lootTable.lootItem.LootItemList;
import necesse.inventory.lootTable.lootItem.OneOfLootItems;
import necesse.inventory.lootTable.lootItem.UpgradeTierLootItem;
import necesse.inventory.lootTable.presets.IncursionLootLists;
import necesse.inventory.lootTable.presets.PetsLootTable;
import necesse.level.maps.incursion.IncursionData;
import necesse.level.maps.incursion.UniqueIncursionReward;

public class UniqueIncursionRewardsRegistry extends GameRegistry<UniqueIncursionReward> {
   public static final UniqueIncursionRewardsRegistry instance = new UniqueIncursionRewardsRegistry();
   public static int closeRangeWeapons;
   public static int greatswordWeapons;
   public static int spearWeapons;
   public static int glaiveWeapons;
   public static int bowWeapons;
   public static int greatBowWeapons;
   public static int gunWeapons;
   public static int magicWeapons;
   public static int throwWeapons;
   public static int summonWeapons;
   public static int incursionCloseRangeWeapons;
   public static int incursionGreatswordWeapons;
   public static int incursionSpearWeapons;
   public static int incursionGlaiveWeapons;
   public static int incursionBowWeapons;
   public static int incursionGreatBowWeapons;
   public static int incursionGunWeapons;
   public static int incursionMagicWeapons;
   public static int incursionThrowWeapons;
   public static int incursionSummonWeapons;
   public static int tools;
   public static int headArmors;
   public static int bodyArmors;
   public static int feetArmors;
   public static int trinkets;
   public static int armorSets;
   public static int incursionHeadArmors;
   public static int incursionBodyArmors;
   public static int incursionFeetArmors;
   public static int incursionTrinkets;
   public static int incursionArmorSets;
   public static int rareIncursionWeapons;
   public static int rareIncursionTrinkets;
   public static int rareIncursionArmorSets;
   public static int cosmeticArmorSets;
   public static int pets;

   private UniqueIncursionRewardsRegistry() {
      super("UniqueIncursionReward", 32766);
   }

   @Override
   public void registerCore() {
      registerCloseRangeWeapon("closerangeweapon", new UniqueIncursionReward(LootTablePresets.closeRangeWeapons));
      registerIncursionCloseRangeWeapon("incursioncloserangeweapon", new UniqueIncursionReward(LootTablePresets.incursionCloseRangeWeapons));
      registerGreatswordWeapon("greatsword", new UniqueIncursionReward(LootTablePresets.greatswordWeapons));
      registerIncursionGreatswordWeapon("incursiongreatsword", new UniqueIncursionReward(LootTablePresets.incursionGreatswordWeapons));
      registerSpearWeapon("spear", new UniqueIncursionReward(LootTablePresets.spearWeapons));
      registerIncursionSpearWeapon("incursionspear", new UniqueIncursionReward(LootTablePresets.incursionSpearWeapons));
      registerGlaiveWeapon("glaive", new UniqueIncursionReward(LootTablePresets.glaiveWeapons));
      registerIncursionGlaiveWeapon("incursionglaive", new UniqueIncursionReward(LootTablePresets.incursionGlaiveWeapons));
      registerBowWeapon("bow", new UniqueIncursionReward(LootTablePresets.bowWeapons));
      registerIncursionBowWeapon("incursionbow", new UniqueIncursionReward(LootTablePresets.incursionBowWeapons));
      registerGreatBowWeapon("greatbow", new UniqueIncursionReward(LootTablePresets.greatBowWeapons));
      registerIncursionGreatBowWeapon("incursiongreatbow", new UniqueIncursionReward(LootTablePresets.incursionGreatBowWeapons));
      registerGunWeapon("gun", new UniqueIncursionReward(LootTablePresets.gunWeapons));
      registerIncursionGunWeapon("incursiongun", new UniqueIncursionReward(LootTablePresets.incursionGunWeapons));
      registerMagicWeapon("magicweapon", new UniqueIncursionReward(LootTablePresets.magicWeapons));
      registerIncursionMagicWeapon("incursionmagicweapon", new UniqueIncursionReward(LootTablePresets.incursionMagicWeapons));
      registerThrowWeapon("throwingweapon", new UniqueIncursionReward(LootTablePresets.throwWeapons));
      registerIncursionThrowWeapon("incursionthrowingweapon", new UniqueIncursionReward(LootTablePresets.incursionThrowWeapons));
      registerSummonWeapon("summonweapon", new UniqueIncursionReward(LootTablePresets.summonWeapons));
      registerIncursionSummonWeapon("incursionsummonweapon", new UniqueIncursionReward(LootTablePresets.incursionSummonWeapons));
      registerTools("tools", new UniqueIncursionReward(LootTablePresets.tools));
      registerHeadArmors("headarmor", new UniqueIncursionReward(LootTablePresets.headArmor));
      registerBodyArmors("bodyarmor", new UniqueIncursionReward(LootTablePresets.bodyArmor));
      registerFeetArmors("feetarmor", new UniqueIncursionReward(LootTablePresets.feetArmor));
      registerTrinkets("trinkets", new UniqueIncursionReward(LootTablePresets.trinkets));
      registerArmorSets("armorsets", new UniqueIncursionReward(LootTablePresets.armorSets));
      registerIncursionHeadArmors("incursionheadarmor", new UniqueIncursionReward(LootTablePresets.incursionHeadArmor));
      registerIncursionBodyArmors("incursionbodyarmor", new UniqueIncursionReward(LootTablePresets.incursionBodyArmor));
      registerIncursionFeetArmors("incursionfeetarmor", new UniqueIncursionReward(LootTablePresets.incursionFeetArmor));
      registerIncursionTrinkets("incursiontrinkets", new UniqueIncursionReward(LootTablePresets.incursionTrinkets));
      registerIncursionArmorSets("incursionarmorsets", new UniqueIncursionReward(LootTablePresets.incursionArmorSets));
      registerRareIncursionWeapons("rareincursionweapons", new UniqueIncursionReward(LootTablePresets.rareIncursionWeapons));
      registerRareIncursionTrinkets("rareincursiontrinkets", new UniqueIncursionReward(LootTablePresets.rareIncursionTrinkets));
      registerRareIncursionArmor("rareincursionarmorsets", new UniqueIncursionReward(LootTablePresets.rareIncursionArmorSets));
      registerCosmeticArmorSets("cosmeticarmorsets", new UniqueIncursionReward(LootTablePresets.cosmeticArmorSets));
      registerPets("pets", new UniqueIncursionReward(PetsLootTable.petsLootTable));
   }

   protected void onRegister(UniqueIncursionReward object, int id, String stringID, boolean isReplace) {
   }

   @Override
   protected void onRegistryClose() {
   }

   public static List<UniqueIncursionReward> getUniqueIncursionRewards() {
      return instance.streamElements().collect(Collectors.toList());
   }

   public static int registerReward(String stringID, UniqueIncursionReward modifier) {
      return instance.register(stringID, modifier);
   }

   public static int registerCloseRangeWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      closeRangeWeapons = id;
      return id;
   }

   public static int registerIncursionCloseRangeWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionCloseRangeWeapons = id;
      return id;
   }

   public static int registerGreatswordWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      greatswordWeapons = id;
      return id;
   }

   public static int registerIncursionGreatswordWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionGreatswordWeapons = id;
      return id;
   }

   public static int registerSpearWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      spearWeapons = id;
      return id;
   }

   public static int registerIncursionSpearWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionSpearWeapons = id;
      return id;
   }

   public static int registerGlaiveWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      glaiveWeapons = id;
      return id;
   }

   public static int registerIncursionGlaiveWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionGlaiveWeapons = id;
      return id;
   }

   public static int registerBowWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      bowWeapons = id;
      return id;
   }

   public static int registerIncursionBowWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionBowWeapons = id;
      return id;
   }

   public static int registerGreatBowWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      greatBowWeapons = id;
      return id;
   }

   public static int registerIncursionGreatBowWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionGreatBowWeapons = id;
      return id;
   }

   public static int registerGunWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      gunWeapons = id;
      return id;
   }

   public static int registerIncursionGunWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionGunWeapons = id;
      return id;
   }

   public static int registerMagicWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      magicWeapons = id;
      return id;
   }

   public static int registerIncursionMagicWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionMagicWeapons = id;
      return id;
   }

   public static int registerThrowWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      throwWeapons = id;
      return id;
   }

   public static int registerIncursionThrowWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionThrowWeapons = id;
      return id;
   }

   public static int registerSummonWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      summonWeapons = id;
      return id;
   }

   public static int registerIncursionSummonWeapon(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionSummonWeapons = id;
      return id;
   }

   public static int registerTools(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      tools = id;
      return id;
   }

   public static int registerHeadArmors(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      headArmors = id;
      return id;
   }

   public static int registerBodyArmors(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      bodyArmors = id;
      return id;
   }

   public static int registerFeetArmors(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      feetArmors = id;
      return id;
   }

   public static int registerTrinkets(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      trinkets = id;
      return id;
   }

   public static int registerArmorSets(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      armorSets = id;
      return id;
   }

   public static int registerIncursionHeadArmors(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionHeadArmors = id;
      return id;
   }

   public static int registerIncursionBodyArmors(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionBodyArmors = id;
      return id;
   }

   public static int registerIncursionFeetArmors(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionFeetArmors = id;
      return id;
   }

   public static int registerIncursionTrinkets(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionTrinkets = id;
      return id;
   }

   public static int registerIncursionArmorSets(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      incursionArmorSets = id;
      return id;
   }

   public static int registerRareIncursionWeapons(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      rareIncursionWeapons = id;
      return id;
   }

   public static int registerRareIncursionTrinkets(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      rareIncursionTrinkets = id;
      return id;
   }

   public static int registerRareIncursionArmor(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      rareIncursionArmorSets = id;
      return id;
   }

   public static int registerCosmeticArmorSets(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      cosmeticArmorSets = id;
      return id;
   }

   public static int registerPets(String stringID, UniqueIncursionReward modifier) {
      int id = registerReward(stringID, modifier);
      pets = id;
      return id;
   }

   public static LootItemInterface getRandomTierXBiomeWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(
         seededRandom.getOneOf(
            closeRangeWeapons,
            greatswordWeapons,
            spearWeapons,
            glaiveWeapons,
            bowWeapons,
            greatBowWeapons,
            gunWeapons,
            magicWeapons,
            throwWeapons,
            summonWeapons
         )
      );
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXIncursionWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(
         seededRandom.getOneOf(
            incursionCloseRangeWeapons,
            incursionGreatswordWeapons,
            incursionGlaiveWeapons,
            incursionBowWeapons,
            incursionSpearWeapons,
            incursionGreatBowWeapons,
            incursionGunWeapons,
            incursionMagicWeapons,
            incursionThrowWeapons,
            incursionSummonWeapons
         )
      );
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXBiomeAndIncursionWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(
         seededRandom.getOneOf(
            closeRangeWeapons,
            greatswordWeapons,
            spearWeapons,
            glaiveWeapons,
            bowWeapons,
            greatBowWeapons,
            gunWeapons,
            magicWeapons,
            throwWeapons,
            summonWeapons,
            incursionCloseRangeWeapons,
            incursionGreatswordWeapons,
            incursionGlaiveWeapons,
            incursionBowWeapons,
            incursionSpearWeapons,
            incursionGreatBowWeapons,
            incursionGunWeapons,
            incursionMagicWeapons,
            incursionThrowWeapons,
            incursionSummonWeapons
         )
      );
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXBiomeMeleeWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(
         seededRandom.getOneOf(closeRangeWeapons, greatswordWeapons, glaiveWeapons, throwWeapons, spearWeapons)
      );
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXIncursionMeleeWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(
         seededRandom.getOneOf(incursionCloseRangeWeapons, incursionGreatswordWeapons, incursionGlaiveWeapons, incursionThrowWeapons, incursionSpearWeapons)
      );
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXBiomeAndIncursionMeleeWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(
         seededRandom.getOneOf(
            closeRangeWeapons,
            greatswordWeapons,
            glaiveWeapons,
            throwWeapons,
            spearWeapons,
            incursionCloseRangeWeapons,
            incursionGreatswordWeapons,
            incursionGlaiveWeapons,
            incursionThrowWeapons,
            incursionSpearWeapons
         )
      );
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXBiomeRangeWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(seededRandom.getOneOf(bowWeapons, gunWeapons, greatBowWeapons));
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXIncursionRangeWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(seededRandom.getOneOf(incursionBowWeapons, incursionGunWeapons, incursionGreatBowWeapons));
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXBiomeAndIncursionRangeWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(
         seededRandom.getOneOf(bowWeapons, gunWeapons, greatBowWeapons, incursionBowWeapons, incursionGunWeapons, incursionGreatBowWeapons)
      );
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXBiomeSummonWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(seededRandom.getOneOf(summonWeapons));
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXIncursionSummonWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(seededRandom.getOneOf(incursionSummonWeapons));
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXBiomeAndIncursionSummonWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(seededRandom.getOneOf(summonWeapons, incursionSummonWeapons));
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXBiomeMagicWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(seededRandom.getOneOf(magicWeapons));
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXIncursionMagicWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(seededRandom.getOneOf(magicWeapons, incursionMagicWeapons));
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXBiomeAndIncursionMagicWeaponReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(seededRandom.getOneOf(magicWeapons, incursionMagicWeapons));
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXBiomeArmorSetReward() {
      UniqueIncursionReward armorSet = instance.getElement(armorSets);
      return armorSet.lootItemList;
   }

   public static LootItemInterface getRandomTierXIncursionArmorSetReward() {
      UniqueIncursionReward incursionArmorSet = instance.getElement(incursionArmorSets);
      return incursionArmorSet.lootItemList;
   }

   public static LootItemInterface getRandomTierXBiomeAndIncursionArmorSetReward(GameRandom seededRandom) {
      UniqueIncursionReward incursionArmorSet = instance.getElement(seededRandom.getOneOf(armorSets, incursionArmorSets));
      return incursionArmorSet.lootItemList;
   }

   public static LootItemInterface getRandomCosmeticArmorSetReward() {
      UniqueIncursionReward cosmeticArmorSet = instance.getElement(cosmeticArmorSets);
      return cosmeticArmorSet.lootItemList;
   }

   public static LootItemInterface getRandomPetReward() {
      UniqueIncursionReward petsReward = instance.getElement(pets);
      return petsReward.lootItemList;
   }

   public static LootItemInterface getSeededRandomTierXGatewayTablets(int tabletDropAmount, final int tier, final IncursionData incursionData) {
      LootItemList lootList = new LootItemList();

      for (int i = 0; i < tabletDropAmount; i++) {
         lootList.add(new LootItemInterface() {
            @Override
            public void addPossibleLoot(LootList list, Object... extra) {
               list.add("gatewaytablet");
            }

            @Override
            public void addItems(List<InventoryItem> list, GameRandom random, float lootMultiplier, Object... extra) {
               InventoryItem tablet = new InventoryItem("gatewaytablet");
               GatewayTabletItem.initializeGatewayTablet(tablet, random, tier, incursionData);
               list.add(tablet);
            }
         });
      }

      return lootList;
   }

   public static LootItemInterface getCoinsReward(int multiplier, int tier) {
      LootItemList lootList = new LootItemList();
      int baseAmount = 2000;
      int minPerTier = 400;
      int maxPerTier = 600;
      lootList.add(LootItem.between("coin", (baseAmount + minPerTier * tier) * multiplier, (baseAmount + maxPerTier * tier) * multiplier));
      return lootList;
   }

   public static LootItemInterface getUpgradeShardsReward(int tier) {
      LootItemList lootList = new LootItemList();
      int baseAmount = 100;
      int minPerTier = 25;
      int maxPerTier = 50;
      lootList.add(LootItem.between("upgradeshard", baseAmount + minPerTier * tier, baseAmount + maxPerTier * tier));
      return lootList;
   }

   public static LootItemInterface getAlchemyShardsReward(int tier) {
      LootItemList lootList = new LootItemList();
      int baseAmount = 100;
      int minPerTier = 25;
      int maxPerTier = 50;
      lootList.add(LootItem.between("alchemyshard", baseAmount + minPerTier * tier, baseAmount + maxPerTier * tier));
      return lootList;
   }

   public static LootItemInterface getAltarDustReward(int tier) {
      LootItemList lootList = new LootItemList();
      int baseAmount = 50;
      int minPerTier = 15;
      int maxPerTier = 30;
      lootList.add(LootItem.between("altardust", baseAmount + minPerTier * tier, baseAmount + maxPerTier * tier));
      return lootList;
   }

   public static LootItemInterface getTierOneEssencesReward(GameRandom seededRandom, int tier) {
      LootItemList lootList = new LootItemList();
      int baseAmount = 14;
      int minPerTier = 4;
      int maxPerTier = 8;
      LootItem randomEssence = (LootItem)seededRandom.getOneOf(IncursionLootLists.tierOneEssences);
      lootList.add(LootItem.between(randomEssence.itemStringID, baseAmount + minPerTier * tier, baseAmount + maxPerTier * tier));
      return lootList;
   }

   public static LootItemInterface getTierTwoEssencesReward(GameRandom seededRandom, int tier) {
      LootItemList lootList = new LootItemList();
      int baseAmount = 10;
      int minPerTier = 3;
      int maxPerTier = 6;
      OneOfLootItems essencesBasedOnTiers = new OneOfLootItems();
      essencesBasedOnTiers.add(new LootItem("slimeessence"));
      if (tier >= IncursionPerksRegistry.GRAVEYARD_TABLET_CAN_DROP.tier) {
         essencesBasedOnTiers.add(new LootItem("bloodessence"));
         if (tier >= IncursionPerksRegistry.SPIDER_CASTLE_TABLET_CAN_DROP.tier) {
            essencesBasedOnTiers.add(new LootItem("spideressence"));
         }
      }

      LootItem randomEssence = (LootItem)seededRandom.getOneOf(essencesBasedOnTiers);
      lootList.add(LootItem.between(randomEssence.itemStringID, baseAmount + minPerTier * tier, baseAmount + maxPerTier * tier));
      return lootList;
   }

   public static LootItemInterface getTierThreeEssencesReward(GameRandom seededRandom, int tier) {
      LootItemList lootList = new LootItemList();
      int baseAmount = 8;
      int minPerTier = 2;
      int maxPerTier = 4;
      OneOfLootItems essencesBasedOnTiers = new OneOfLootItems();
      essencesBasedOnTiers.add(new LootItem("crystalessence"));
      if (tier >= IncursionPerksRegistry.SCRAPYARD_TABLET_CAN_DROP.tier) {
         essencesBasedOnTiers.add(new LootItem("radiatedessence"));
      }

      LootItem randomEssence = (LootItem)seededRandom.getOneOf(essencesBasedOnTiers);
      lootList.add(LootItem.between(randomEssence.itemStringID, baseAmount + minPerTier * tier, baseAmount + maxPerTier * tier));
      return lootList;
   }

   public static LootItemInterface getIncursionOresReward(GameRandom seededRandom, int tier) {
      LootItemList lootList = new LootItemList();
      int baseAmount = 14;
      int minPerTier = 4;
      int maxPerTier = 8;
      OneOfLootItems oresBasedOnTier = new OneOfLootItems();
      oresBasedOnTier.add(new LootItem("slimeum"));
      oresBasedOnTier.add(new LootItem("slimematter"));
      if (tier >= IncursionPerksRegistry.GRAVEYARD_TABLET_CAN_DROP.tier) {
         oresBasedOnTier.add(new LootItem("nightsteelore"));
         oresBasedOnTier.add(new LootItem("phantomdust"));
         if (tier >= IncursionPerksRegistry.SPIDER_CASTLE_TABLET_CAN_DROP.tier) {
            oresBasedOnTier.add(new LootItem("spideriteore"));
            oresBasedOnTier.add(new LootItem("spidervenom"));
            if (tier >= IncursionPerksRegistry.CRYSTAL_HOLLOW_TABLET_CAN_DROP.tier) {
               oresBasedOnTier.add(new LootItem("pearlescentdiamond"));
               oresBasedOnTier.add(new LootItem("omnicrystal"));
               if (tier >= IncursionPerksRegistry.SCRAPYARD_TABLET_CAN_DROP.tier) {
                  oresBasedOnTier.add(new LootItem("pristinegear"));
                  oresBasedOnTier.add(new LootItem("fuelskull"));
               }
            }
         }
      }

      LootItem randomOre = (LootItem)seededRandom.getOneOf(oresBasedOnTier);
      lootList.add(LootItem.between(randomOre.itemStringID, baseAmount + minPerTier * tier, baseAmount + maxPerTier * tier));
      return lootList;
   }

   private static int getRandomTierForEquipment(GameRandom seededRandom, int maxTier) {
      TicketSystemList<Integer> ticketedTier = new TicketSystemList<>();
      int ticketReductionPerTier = 5;

      for (int i = 1; i <= maxTier; i++) {
         ticketedTier.addObject(100 - ticketReductionPerTier * i, i);
      }

      return ticketedTier.getRandomObject(seededRandom);
   }

   public static int getMaxTierAvailableForTabletDrop(HashSet<Integer> perkIDsToCheck) {
      int altarTier = 1;

      for (IncursionPerk perk : IncursionPerksRegistry.fromPerkIDs(perkIDsToCheck)) {
         altarTier = GameMath.max(altarTier, perk.tier);
      }

      return GameMath.min(altarTier, IncursionData.TABLET_TIER_UPGRADE_CAP);
   }

   private static int getRandomTierForTablets(GameRandom seededRandom, int currentTabletTier) {
      TicketSystemList<Integer> ticketedTier = new TicketSystemList<>();
      ticketedTier.addObject(50, currentTabletTier);
      ticketedTier.addObject(25, currentTabletTier - 1);
      ticketedTier.addObject(10, currentTabletTier - 2);
      return GameMath.max(ticketedTier.getRandomObject(seededRandom), IncursionData.MINIMUM_TIER);
   }

   public static LootItemInterface getSeededRandomReward(IncursionData incursionData, GameRandom seededRandom, int tabletTier) {
      TicketSystemList<LootItemInterface> ticketedRewards = new TicketSystemList<>();
      int tabletTierWeight = 10 * tabletTier;
      int limitedTabletTier = GameMath.limit(tabletTier, IncursionData.MINIMUM_TIER, IncursionData.DROPS_TIER_CAP);
      int tabletRewardTier = 1;
      int equipmentRewardTier = 1;
      int maxEquipmentRewardTier = Math.min(tabletTier, 3);
      if (incursionData != null) {
         for (IncursionPerk perk : IncursionPerksRegistry.fromPerkIDs(incursionData.currentIncursionPerkIDs)) {
            if (perk.setMaxEquipmentRewardTier() > maxEquipmentRewardTier) {
               maxEquipmentRewardTier = perk.setMaxEquipmentRewardTier();
            }
         }

         equipmentRewardTier = getRandomTierForEquipment(
            seededRandom, GameMath.min(Math.min(maxEquipmentRewardTier, tabletTier), IncursionData.ITEM_TIER_UPGRADE_CAP)
         );

         for (IncursionPerk perk : IncursionPerksRegistry.fromPerkIDsSorted(incursionData.currentIncursionPerkIDs)) {
            if (!perk.affectsCurrentIncursion) {
               ticketedRewards = perk.onGenerateTabletRewards(ticketedRewards, seededRandom, equipmentRewardTier, incursionData);
            }
         }

         tabletRewardTier = getRandomTierForTablets(seededRandom, limitedTabletTier);
      }

      ticketedRewards.addObject(100, getSeededRandomTierXGatewayTablets(1, tabletRewardTier, incursionData));
      ticketedRewards.addObject((int)(40.0 + tabletTierWeight * 0.75), getSeededRandomTierXGatewayTablets(2, tabletRewardTier, incursionData));
      if (incursionData != null && incursionData.currentIncursionPerkIDs.contains(IncursionPerksRegistry.INCURSION_LEVEL_GEAR_REWARD.getID())) {
         ticketedRewards.addObject(20 + tabletTierWeight, new UpgradeTierLootItem(getRandomTierXBiomeWeaponReward(seededRandom), equipmentRewardTier));
      } else {
         ticketedRewards.addObject(70 + tabletTierWeight, new UpgradeTierLootItem(getRandomTierXBiomeWeaponReward(seededRandom), equipmentRewardTier));
      }

      if (incursionData != null && incursionData.currentIncursionPerkIDs.contains(IncursionPerksRegistry.INCURSION_LEVEL_GEAR_REWARD.getID())) {
         ticketedRewards.addObject(tabletTierWeight, new UpgradeTierLootItem(getRandomTierXBiomeArmorSetReward(), equipmentRewardTier));
      } else {
         ticketedRewards.addObject(40 + tabletTierWeight, new UpgradeTierLootItem(getRandomTierXBiomeArmorSetReward(), equipmentRewardTier));
      }

      int coinMultiplier = 1;
      if (incursionData != null && incursionData.currentIncursionPerkIDs.contains(IncursionPerksRegistry.MORE_COINS_REWARD.getID())) {
         if (incursionData.currentIncursionPerkIDs.contains(IncursionPerksRegistry.FIVE_TIMES_MORE_COINS_CHANCE.getID()) && seededRandom.getChance(0.1F)) {
            coinMultiplier = 5;
         }

         ticketedRewards.addObject(150 + tabletTierWeight / 2, getCoinsReward(coinMultiplier, tabletTier));
      } else {
         ticketedRewards.addObject(75 + tabletTierWeight / 2, getCoinsReward(coinMultiplier, tabletTier));
      }

      ticketedRewards.addObject(100 + tabletTierWeight / 2, getUpgradeShardsReward(tabletTier));
      ticketedRewards.addObject(75 + tabletTierWeight / 2, getAlchemyShardsReward(tabletTier));
      ticketedRewards.addObject(100 + tabletTierWeight / 2, getAltarDustReward(tabletTier));
      ticketedRewards.addObject(50 + tabletTierWeight / 2, getTierOneEssencesReward(seededRandom, tabletTier));
      ticketedRewards.addObject(GameMath.max(0, -30 + (int)(tabletTierWeight * 1.5)), getTierTwoEssencesReward(seededRandom, tabletTier));
      ticketedRewards.addObject(GameMath.max(0, -180 + tabletTierWeight * 3), getTierThreeEssencesReward(seededRandom, tabletTier));
      ticketedRewards.addObject(GameMath.max(0, -30 + (int)(tabletTierWeight * 1.5)), getIncursionOresReward(seededRandom, tabletTier));
      if (ticketedRewards.isEmpty()) {
         GameLog.warn.println("No rewards found.");
         return null;
      } else {
         return ticketedRewards.getRandomObject(seededRandom);
      }
   }

   public static LootItemInterface getRandomTrinket() {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(trinkets);
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomIncursionTrinket() {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(incursionTrinkets);
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXArmorReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(seededRandom.getOneOf(headArmors, bodyArmors, feetArmors));
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getRandomTierXIncursionArmorReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(seededRandom.getOneOf(incursionHeadArmors, incursionBodyArmors, incursionFeetArmors));
      return rewardTypeChosen.lootItemList;
   }

   public static LootItemInterface getSeededRandomTierXRareReward(GameRandom seededRandom) {
      UniqueIncursionReward rewardTypeChosen = instance.getElement(seededRandom.getOneOf(rareIncursionWeapons, rareIncursionArmorSets, rareIncursionTrinkets));
      return rewardTypeChosen.lootItemList;
   }
}
