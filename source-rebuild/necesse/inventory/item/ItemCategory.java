package necesse.inventory.item;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Stream;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.util.CategoryString;
import necesse.engine.util.GameUtils;

public class ItemCategory implements Comparable<ItemCategory> {
   public static final ItemCategory masterCategory = new ItemCategory();
   public static final ItemCategoryManager masterManager = new ItemCategoryManager(masterCategory);
   public static final ItemCategory foodQualityMasterCategory = new ItemCategory();
   public static final ItemCategoryManager foodQualityManager = new ItemCategoryManager(foodQualityMasterCategory);
   public static final ItemCategory equipmentMasterCategory = new ItemCategory();
   public static final ItemCategoryManager equipmentManager = new ItemCategoryManager(equipmentMasterCategory);
   public static final ItemCategory craftingMasterCategory = new ItemCategory();
   public static final ItemCategoryManager craftingManager = new ItemCategoryManager(craftingMasterCategory);
   protected ItemCategoryManager manager;
   public final int id;
   public final int depth;
   public final String stringID;
   public final ItemCategory parent;
   public GameMessage displayName;
   protected String[] sortCategories;
   protected HashMap<String, ItemCategory> children = new HashMap<>();
   protected HashMap<Integer, Item> itemIDs = new HashMap<>();
   protected HashMap<Integer, Item> thisOrChildrenItemIDs = new HashMap<>();

   public static ItemCategory createCategory(String categorySortString, String... categoryTree) {
      return masterManager.createCategory(categorySortString, categoryTree);
   }

   public static ItemCategory createCategory(String categorySortString, GameMessage displayName, String... categoryTree) {
      return masterManager.createCategory(categorySortString, displayName, categoryTree);
   }

   public static ItemCategory getCategory(String... categoryTree) {
      return masterManager.getCategory(categoryTree);
   }

   public static ItemCategory getCategory(int categoryID) {
      return masterManager.getCategory(categoryID);
   }

   public ItemCategory setItemCategory(Item item, ItemCategory category) {
      return masterManager.setItemCategory(item, category);
   }

   public static ItemCategory setItemCategory(Item item, String... categoryTree) {
      return masterManager.setItemCategory(item, categoryTree);
   }

   public static ItemCategory getItemsCategory(Item item) {
      return masterManager.getItemsCategory(item);
   }

   protected ItemCategory(int id, String stringID, ItemCategory parent, String categorySortString, GameMessage displayName) {
      this.id = id;
      this.stringID = stringID;
      this.parent = parent;
      this.depth = parent == null ? 0 : parent.depth + 1;
      this.displayName = displayName;
      this.setSortCategories(categorySortString);
   }

   public ItemCategory() {
      this(0, "master", null, "Z-Z-Z", new StaticMessage("MASTER"));
   }

   public void setSortCategories(String categorySortString) {
      this.sortCategories = CategoryString.getCategories(categorySortString);
   }

   public Iterable<String> getChildrenStringIDs() {
      return this.children.keySet();
   }

   public Iterable<ItemCategory> getChildren() {
      return this.children.values();
   }

   public Stream<ItemCategory> streamChildren() {
      return this.children.values().stream();
   }

   public ItemCategory getChild(String stringID) {
      return this.children.get(stringID);
   }

   public Collection<Item> getItems() {
      return this.itemIDs.values();
   }

   public Stream<Item> streamItems() {
      return this.itemIDs.values().stream();
   }

   public boolean containsItem(Item item) {
      return this.itemIDs.containsKey(item.getID());
   }

   public boolean containsItemOrInChildren(Item item) {
      return this.thisOrChildrenItemIDs.containsKey(item.getID());
   }

   public int getItemCountIncludingChildren() {
      return this.thisOrChildrenItemIDs.size();
   }

   public String[] getStringIDTree(boolean includeMaster) {
      LinkedList<String> tree = new LinkedList<>();
      tree.addFirst(this.stringID);

      for (ItemCategory parent = this.parent; parent != null && (parent.parent != null || includeMaster); parent = parent.parent) {
         tree.addFirst(parent.stringID);
      }

      return tree.toArray(new String[0]);
   }

   public void printTree(PrintStream printStream, String prefix) {
      printStream.println(prefix + this.displayName.translate() + " (" + GameUtils.join(this.sortCategories, "-") + ")");
      this.children.values().stream().sorted().forEach(category -> category.printTree(printStream, prefix + "\t"));
      this.itemIDs
         .values()
         .stream()
         .filter(item -> ItemRegistry.isObtainable(item.getID()))
         .map(i -> ItemRegistry.getDisplayName(i.getID()))
         .filter(Objects::nonNull)
         .sorted()
         .forEach(name -> printStream.println(prefix + "\t" + name));
   }

   public boolean isOrHasParent(String categoryStringID) {
      if (this.stringID.equals(categoryStringID)) {
         return true;
      } else {
         return this.parent != null ? this.parent.isOrHasParent(categoryStringID) : false;
      }
   }

   private static int compare(ItemCategory me, ItemCategory him) {
      return new CategoryString(me.sortCategories, me.displayName.translate()).compareTo(new CategoryString(him.sortCategories, him.displayName.translate()));
   }

   public int compareTo(ItemCategory him) {
      if (him == null) {
         return -1;
      }

      if (him == this) {
         return 0;
      }

      if (this.manager == null || this.manager != him.manager) {
         return compare(this, him);
      }

      if (this.parent == him.parent) {
         return compare(this, him);
      }

      ItemCategory me = this;
      if (me == him.parent) {
         return -1;
      }

      if (me.parent == him) {
         return 1;
      }

      while (him.parent != null && him.parent.depth > this.depth) {
         him = him.parent;
      }

      if (him.parent == null) {
         return 1;
      }

      while (me.parent != null && me.parent.depth > him.depth) {
         me = me.parent;
      }

      return me.parent == null ? -1 : compare(me, him);
   }

   static {
      createCategory("A-A-A", "equipment");
      createCategory("A-A-A", "equipment", "tools");
      createCategory("A-A-A", "equipment", "tools", "creative");
      createCategory("A-A-B", "equipment", "tools", "pickaxes");
      createCategory("A-A-C", "equipment", "tools", "axes");
      createCategory("A-A-D", "equipment", "tools", "shovels");
      createCategory("A-A-E", "equipment", "tools", "fishingrods");
      createCategory("A-A-F", "equipment", "tools", "misc");
      createCategory("A-B-A", "equipment", "weapons");
      createCategory("A-B-A", "equipment", "weapons", "meleeweapons");
      createCategory("A-B-B", "equipment", "weapons", "rangedweapons");
      createCategory("A-B-C", "equipment", "weapons", "magicweapons");
      createCategory("A-B-D", "equipment", "weapons", "summonweapons");
      createCategory("A-B-E", "equipment", "weapons", "throwweapons");
      createCategory("A-C-A", "equipment", "ammo");
      createCategory("A-C-B", "equipment", "bait");
      createCategory("A-D-A", "equipment", "armor");
      createCategory("A-E-A", "equipment", "trinkets");
      createCategory("A-F-A", "equipment", "cosmetics");
      createCategory("A-G-A", "equipment", "banners");
      createCategory("B-A-A", "consumable");
      createCategory("B-A-A", "consumable", "rawfood");
      createCategory("B-B-A", "consumable", "commonfish");
      createCategory("B-C-A", "consumable", "food");
      createCategory("B-D-A", "consumable", "potions");
      createCategory("B-E-A", "consumable", "bossitems");
      createCategory("C-A-A", "materials");
      createCategory("C-A-A", "materials", "ore");
      createCategory("C-B-A", "materials", "minerals");
      createCategory("C-C-A", "materials", "bars");
      createCategory("C-D-A", "materials", "stone");
      createCategory("C-E-A", "materials", "logs");
      createCategory("C-F-A", "materials", "specialfish");
      createCategory("C-G-A", "materials", "flowers");
      createCategory("C-H-A", "materials", "mobdrops");
      createCategory("C-I-A", "materials", "essences");
      createCategory("D-A-A", "tiles");
      createCategory("D-A-A", "tiles", "floors");
      createCategory("D-B-A", "tiles", "liquids");
      createCategory("D-C-A", "tiles", "terrain");
      createCategory("E-A-A", "objects");
      createCategory("E-A-A", "objects", "seeds");
      createCategory("E-B-A", "objects", "saplings");
      createCategory("E-C-A", "objects", "craftingstations");
      createCategory("E-D-A", "objects", "lighting");
      createCategory("E-E-A", "objects", "furniture");
      createCategory("E-E-B", "objects", "furniture", "misc");
      createCategory("E-E-C", "objects", "furniture", "oak");
      createCategory("E-E-D", "objects", "furniture", "spruce");
      createCategory("E-E-E", "objects", "furniture", "pine");
      createCategory("E-E-F", "objects", "furniture", "palm");
      createCategory("E-E-G", "objects", "furniture", "maple");
      createCategory("E-E-H", "objects", "furniture", "birch");
      createCategory("E-E-I", "objects", "furniture", "willow");
      createCategory("E-E-J", "objects", "furniture", "dungeon");
      createCategory("E-E-K", "objects", "furniture", "bone");
      createCategory("E-E-L", "objects", "furniture", "dryad");
      createCategory("E-E-M", "objects", "furniture", "bamboo");
      createCategory("E-E-N", "objects", "furniture", "deadwood");
      createCategory("E-E-O", "objects", "furniture", "spidercastle");
      createCategory("E-F-A", "objects", "decorations");
      createCategory("E-F-B", "objects", "decorations", "paintings");
      createCategory("E-F-C", "objects", "decorations", "carpets");
      createCategory("E-F-D", "objects", "decorations", "pots");
      createCategory("E-F-E", "objects", "decorations", "banners");
      createCategory("E-G-A", "objects", "wallsanddoors");
      createCategory("E-H-A", "objects", "fencesandgates");
      createCategory("E-I-A", "objects", "columns");
      createCategory("E-J-A", "objects", "traps");
      createCategory("E-K-A", "objects", "landscaping");
      createCategory("E-K-A", "objects", "landscaping", "rocksandores");
      createCategory("E-K-B", "objects", "landscaping", "forestrocksandores");
      createCategory("E-K-C", "objects", "landscaping", "snowrocksandores");
      createCategory("E-K-D", "objects", "landscaping", "plainsrocksandores");
      createCategory("E-K-E", "objects", "landscaping", "swamprocksandores");
      createCategory("E-K-F", "objects", "landscaping", "desertrocksandores");
      createCategory("E-K-G", "objects", "landscaping", "incursionrocksandores");
      createCategory("E-K-H", "objects", "landscaping", "crystals");
      createCategory("E-K-I", "objects", "landscaping", "tabledecorations");
      createCategory("E-K-J", "objects", "landscaping", "plants");
      createCategory("E-K-K", "objects", "landscaping", "masonry");
      createCategory("E-K-L", "objects", "landscaping", "misc");
      createCategory("E-L-A", "objects", "misc");
      createCategory("F-A-A", "wiring");
      createCategory("F-A-A", "wiring", "logicgates");
      createCategory("G-A-A", "mobs");
      createCategory("G-B-A", "mobs", "passive");
      createCategory("G-B-B", "mobs", "passive", "critters");
      createCategory("G-B-C", "mobs", "passive", "humans");
      createCategory("G-B-D", "mobs", "passive", "husbandry");
      createCategory("G-C-A", "mobs", "hostile");
      createCategory("G-C-B", "mobs", "hostile", "raiders");
      createCategory("G-C-C", "mobs", "hostile", "bosses");
      createCategory("Z-A-A", "misc");
      createCategory("Z-A-A", "misc", "mounts");
      createCategory("Z-B-A", "misc", "pets");
      createCategory("Z-B-B", "misc", "critters");
      createCategory("Z-C-A", "misc", "pouches");
      createCategory("Z-D-A", "misc", "vinyls");
      createCategory("Z-E-A", "misc", "questitems");
      equipmentManager.createCategory("A-A-A", "weapons");
      equipmentManager.createCategory("A-B-A", "weapons", "meleeweapons");
      equipmentManager.createCategory("A-C-A", "weapons", "rangedweapons");
      equipmentManager.createCategory("A-D-A", "weapons", "magicweapons");
      equipmentManager.createCategory("A-E-A", "weapons", "summonweapons");
      equipmentManager.createCategory("A-F-A", "weapons", "throwweapons");
      equipmentManager.createCategory("B-A-A", "armor");
      craftingManager.createCategory("A-A-A", "craftingstations");
      craftingManager.createCategory("B-A-A", "incursions");
      craftingManager.createCategory("B-A-A", "incursions", "tier1essences");
      craftingManager.createCategory("B-A-B", "incursions", "tier2essences");
      craftingManager.createCategory("B-A-C", "incursions", "tier3essences");
      craftingManager.createCategory("B-A-D", "incursions", "dustandshards");
      craftingManager.createCategory("C-A-A", "consumable");
      craftingManager.createCategory("C-A-A", "consumable", "resourcepotions");
      craftingManager.createCategory("C-A-B", "consumable", "greaterbuffpotions");
      craftingManager.createCategory("C-A-C", "consumable", "buffpotions");
      craftingManager.createCategory("C-A-D", "consumable", "utilitypotions");
      craftingManager.createCategory("D-A-A", "equipment");
      craftingManager.createCategory("D-A-A", "equipment", "tools");
      craftingManager.createCategory("D-B-A", "equipment", "weapons");
      craftingManager.createCategory("D-B-A", "equipment", "weapons", "meleeweapons");
      craftingManager.createCategory("D-B-B", "equipment", "weapons", "rangedweapons");
      craftingManager.createCategory("D-B-C", "equipment", "weapons", "magicweapons");
      craftingManager.createCategory("D-B-D", "equipment", "weapons", "summonweapons");
      craftingManager.createCategory("D-B-E", "equipment", "weapons", "throwweapons");
      craftingManager.createCategory("D-C-A", "equipment", "ammo");
      craftingManager.createCategory("D-D-A", "equipment", "armor");
      craftingManager.createCategory("D-E-A", "equipment", "trinkets");
      craftingManager.createCategory("D-F-A", "equipment", "cosmetics");
      craftingManager.createCategory("E-A-A", "objects");
      craftingManager.createCategory("E-A-A", "objects", "lighting");
      craftingManager.createCategory("E-B-A", "objects", "furniture");
      craftingManager.createCategory("E-E-B", "objects", "furniture", "misc");
      craftingManager.createCategory("E-B-C", "objects", "furniture", "oak");
      craftingManager.createCategory("E-B-D", "objects", "furniture", "spruce");
      craftingManager.createCategory("E-B-E", "objects", "furniture", "pine");
      craftingManager.createCategory("E-B-F", "objects", "furniture", "palm");
      craftingManager.createCategory("E-B-G", "objects", "furniture", "maple");
      craftingManager.createCategory("E-B-H", "objects", "furniture", "birch");
      craftingManager.createCategory("E-B-I", "objects", "furniture", "willow");
      craftingManager.createCategory("E-B-J", "objects", "furniture", "dungeon");
      craftingManager.createCategory("E-B-K", "objects", "furniture", "bone");
      craftingManager.createCategory("E-B-L", "objects", "furniture", "dryad");
      craftingManager.createCategory("E-B-M", "objects", "furniture", "bamboo");
      craftingManager.createCategory("E-B-N", "objects", "furniture", "deadwood");
      craftingManager.createCategory("E-B-O", "objects", "furniture", "spidercastle");
      craftingManager.createCategory("E-C-A", "objects", "decorations");
      craftingManager.createCategory("E-C-B", "objects", "decorations", "paintings");
      craftingManager.createCategory("E-C-C", "objects", "decorations", "carpets");
      craftingManager.createCategory("E-C-D", "objects", "decorations", "pots");
      craftingManager.createCategory("E-C-E", "objects", "decorations", "banners");
      craftingManager.createCategory("E-D-A", "objects", "wallsanddoors");
      craftingManager.createCategory("E-E-A", "objects", "fencesandgates");
      craftingManager.createCategory("E-F-A", "objects", "columns");
      craftingManager.createCategory("E-G-A", "objects", "landscaping");
      craftingManager.createCategory("E-G-B", "objects", "landscaping", "misc");
      craftingManager.createCategory("E-G-C", "objects", "landscaping", "tabledecorations");
      craftingManager.createCategory("E-G-D", "objects", "landscaping", "plants");
      craftingManager.createCategory("E-G-E", "objects", "landscaping", "masonry");
      craftingManager.createCategory("E-G-F", "objects", "landscaping", "rocksandores");
      craftingManager.createCategory("E-G-G", "objects", "landscaping", "forestrocksandores");
      craftingManager.createCategory("E-G-H", "objects", "landscaping", "snowrocksandores");
      craftingManager.createCategory("E-G-I", "objects", "landscaping", "plainsrocksandores");
      craftingManager.createCategory("E-G-J", "objects", "landscaping", "swamprocksandores");
      craftingManager.createCategory("E-G-K", "objects", "landscaping", "desertrocksandores");
      craftingManager.createCategory("E-G-L", "objects", "landscaping", "incursionrocksandores");
      craftingManager.createCategory("E-G-M", "objects", "landscaping", "crystals");
      craftingManager.createCategory("E-H-A", "objects", "misc");
      craftingManager.createCategory("F-A-A", "materials");
      craftingManager.createCategory("G-A-A", "tiles");
      craftingManager.createCategory("H-A-A", "wiring");
      craftingManager.createCategory("I-A-A", "misc");
      craftingManager.createCategory("Z-Z-Z", new LocalMessage("itemcategory", "craftingstations"), "finalcraftingstations");
   }
}
