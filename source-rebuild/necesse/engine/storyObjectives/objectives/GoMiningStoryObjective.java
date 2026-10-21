package necesse.engine.storyObjectives.objectives;

import java.util.HashSet;
import java.util.stream.Collectors;
import necesse.engine.input.Control;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.registries.GlobalIngredientRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.storyObjectives.StoryObjective;
import necesse.engine.storyObjectives.StoryObjectiveManager;
import necesse.engine.storyObjectives.listeners.ObjectInteractedObjectiveListener;
import necesse.engine.storyObjectives.utils.TileMarkerObjectiveManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormStoryObjectiveComponent;
import necesse.inventory.InventoryItem;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;

public class GoMiningStoryObjective extends StoryObjective implements ObjectInteractedObjectiveListener {
   public static HashSet<String> ORE_OBJECTS = new HashSet<>();
   public static HashSet<String> ORE_ITEMS = new HashSet<>();
   public static HashSet<String> BAR_ITEMS = new HashSet<>();
   public static HashSet<String> LOG_ITEMS = new HashSet<>();
   public static int ORE_COUNT = 12;
   public static int STONE_COUNT = 20;
   public static LootTable reward = new LootTable(new LootItem("ironpickaxe"));
   private final TileMarkerObjectiveManager oreFinder = new TileMarkerObjectiveManager(1, 20) {
      @Override
      public boolean isValidTile(Level level, int tileX, int tileY) {
         GameObject object = level.getObject(tileX, tileY);
         return GoMiningStoryObjective.ORE_OBJECTS.stream().anyMatch(stringID -> object.getStringID().contains(stringID));
      }
   };
   public boolean hasVisitedCave;
   public boolean hasObtainedOre;
   public boolean hasObtainedStone;
   public boolean hasOpenedForge;

   public GoMiningStoryObjective(StoryObjectiveManager manager) {
      super(manager);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      if (this.hasVisitedCave) {
         save.addBoolean("hasVisitedCave", this.hasVisitedCave);
      }

      if (this.hasObtainedOre) {
         save.addBoolean("hasObtainedOre", this.hasObtainedOre);
      }

      if (this.hasObtainedStone) {
         save.addBoolean("hasObtainedStone", this.hasObtainedStone);
      }

      if (this.hasOpenedForge) {
         save.addBoolean("hasOpenedForge", this.hasOpenedForge);
      }
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.hasVisitedCave = save.getBoolean("hasVisitedCave", this.hasVisitedCave, false);
      this.hasObtainedOre = save.getBoolean("hasObtainedOre", this.hasObtainedOre, false);
      this.hasObtainedStone = save.getBoolean("hasObtainedStone", this.hasObtainedStone, false);
      this.hasOpenedForge = save.getBoolean("hasOpenedForge", this.hasOpenedForge, false);
   }

   @Override
   public void writePacket(PacketWriter writer) {
      super.writePacket(writer);
      writer.putNextBoolean(this.hasVisitedCave);
      writer.putNextBoolean(this.hasObtainedOre);
      writer.putNextBoolean(this.hasObtainedStone);
      writer.putNextBoolean(this.hasOpenedForge);
   }

   @Override
   public void readPacket(PacketReader reader) {
      super.readPacket(reader);
      this.hasVisitedCave = reader.getNextBoolean();
      this.hasObtainedOre = reader.getNextBoolean();
      this.hasObtainedStone = reader.getNextBoolean();
      this.hasOpenedForge = reader.getNextBoolean();
   }

   @Override
   public void serverTickCurrentObjective() {
      super.serverTickCurrentObjective();
      if (!this.hasVisitedCave) {
         if (this.getPlayer().getLevel().isBasicCaveLevel()) {
            this.hasVisitedCave = true;
            this.markDirty();
         }
      } else if (!this.hasObtainedOre) {
         boolean hasOre = this.manager
            .getServerClient()
            .playerMob
            .getInv()
            .hasCountOfItem(false, false, false, false, "tutorial", ORE_COUNT, item -> ORE_ITEMS.contains(item.item.getStringID()));
         if (hasOre) {
            this.hasObtainedOre = true;
            this.markDirty();
         }
      } else if (!this.hasObtainedStone) {
         int anyStoneID = GlobalIngredientRegistry.getGlobalIngredientID("anystone");
         boolean hasStone = this.manager
            .getServerClient()
            .playerMob
            .getInv()
            .hasCountOfItem(false, false, false, false, "tutorial", STONE_COUNT, item -> item.item.isGlobalIngredient(anyStoneID));
         if (hasStone) {
            this.hasObtainedStone = true;
            this.markDirty();
         }
      } else if (this.hasOpenedForge) {
         boolean hasBars = this.manager
            .getServerClient()
            .playerMob
            .getInv()
            .hasAnyItem(false, false, false, false, "tutorial", item -> BAR_ITEMS.contains(item.item.getStringID()));
         if (hasBars) {
            this.markCompleted();
         }
      }
   }

   @Override
   public void clientTickCurrentObjective() {
      super.clientTickCurrentObjective();
      if (!this.isCompleted() && !this.hasObtainedOre && this.getPlayer().getLevel().isBasicCaveLevel()) {
         this.oreFinder.clientTick(this.getPlayer());
      }
   }

   @Override
   public void onObjectInteracted(GameObject object, Level level, int tileX, int tileY) {
      if (this.hasObtainedStone && object.getStringID().equals("forge")) {
         this.hasOpenedForge = true;
         this.markDirty();
      }
   }

   @Override
   public DrawOptions getCurrentObjectiveLevelHudDrawOptions(GameCamera camera, PlayerMob perspective) {
      if (!this.isCompleted() && !this.hasObtainedOre && perspective.getLevel().isCave) {
         String woodPickAxeIcon = TypeParsers.getItemParseString(new InventoryItem("woodpickaxe"));
         String oreIcon = TypeParsers.getItemsParseString(ORE_ITEMS.stream().map(InventoryItem::new).collect(Collectors.toList()));
         LocalMessage hitTreeMessage = new LocalMessage(
            "objectives", "mineoremarker", "pickaxeicon", woodPickAxeIcon, "oreicon", oreIcon, "key", TypeParsers.getInputParseString(Control.MOUSE1)
         );
         return this.oreFinder.getArrowHudDrawOptions(hitTreeMessage, null, camera, perspective);
      } else {
         return super.getCurrentObjectiveLevelHudDrawOptions(camera, perspective);
      }
   }

   @Override
   public LootTable getRewards() {
      return reward;
   }

   @Override
   public void setupFormComponent(FormStoryObjectiveComponent form, FormFlow flow) {
      String torchIcon = TypeParsers.getItemParseString(new InventoryItem("torch"));
      String oreIcon = TypeParsers.getItemsParseString(ORE_ITEMS.stream().map(InventoryItem::new).collect(Collectors.toList()));
      String barIcon = TypeParsers.getItemsParseString(BAR_ITEMS.stream().map(InventoryItem::new).collect(Collectors.toList()));
      String logIcon = TypeParsers.getItemsParseString(LOG_ITEMS.stream().map(InventoryItem::new).collect(Collectors.toList()));
      boolean completed = this.isCompleted();
      form.addTitle(flow, new LocalMessage("objectives", "gominingtitle"));
      form.addObjective(
         flow,
         1,
         new LocalMessage("objectives", "craftanduseladder", "caveladder", TypeParsers.getItemAndNameParseString("ladderdown")),
         completed || this.hasVisitedCave
      );
      if (completed || this.hasVisitedCave) {
         LocalMessage mineOreMessage = new LocalMessage(
            "objectives",
            "mineoreobj",
            "count",
            ORE_COUNT,
            "oreicon",
            oreIcon,
            "torchicon",
            torchIcon,
            "key",
            TypeParsers.getInputParseString(Control.PLACE_TORCH)
         );
         form.addObjective(flow, 2, mineOreMessage, completed || this.hasObtainedOre);
         if (completed || this.hasObtainedOre) {
            LocalMessage stoneMessage = new LocalMessage(
               "objectives",
               "minestone",
               "count",
               STONE_COUNT,
               "stone",
               TypeParsers.getItemAndNameParseString("stone"),
               "forge",
               TypeParsers.getItemAndNameParseString("forge")
            );
            form.addObjective(flow, 3, stoneMessage, completed || this.hasObtainedStone);
            if (completed || this.hasObtainedStone) {
               form.addObjective(
                  flow,
                  4,
                  new LocalMessage(
                     "objectives",
                     "craftforge",
                     "forge",
                     TypeParsers.getItemAndNameParseString("forge"),
                     "workstation",
                     TypeParsers.getItemAndNameParseString("workstationduo"),
                     "oreicon",
                     oreIcon,
                     "baricon",
                     barIcon
                  ),
                  completed || this.hasOpenedForge
               );
               if (completed || this.hasOpenedForge) {
                  form.addObjective(
                     flow,
                     5,
                     new LocalMessage(
                        "objectives", "waitforge", "oreicon", oreIcon, "forge", TypeParsers.getItemAndNameParseString("forge"), "baricon", barIcon
                     ),
                     completed
                  );
                  if (!completed) {
                     form.addObjectiveNote(
                        flow,
                        new LocalMessage("objectives", "forgenote", "forge", TypeParsers.getItemAndNameParseString("forge"), "logicon", logIcon),
                        completed
                     );
                  }

                  if (completed) {
                     form.addObjective(
                        flow, 6, new LocalMessage("objectives", "elderclaimreward", "elder", TypeParsers.getMobAndNameParseString("elderhuman")), false
                     );
                  }
               }
            }
         }
      }

      form.addRewardsText(flow);
   }

   static {
      ORE_OBJECTS.add("copperore");
      ORE_OBJECTS.add("ironore");
      ORE_OBJECTS.add("goldore");
      ORE_ITEMS.add("copperore");
      ORE_ITEMS.add("ironore");
      ORE_ITEMS.add("goldore");
      BAR_ITEMS.add("copperbar");
      BAR_ITEMS.add("ironbar");
      BAR_ITEMS.add("goldbar");
      LOG_ITEMS.add("oaklog");
      LOG_ITEMS.add("sprucelog");
      LOG_ITEMS.add("pinelog");
   }
}
