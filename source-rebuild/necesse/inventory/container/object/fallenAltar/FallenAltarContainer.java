package necesse.inventory.container.object.fallenAltar;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.LinkedHashSet;
import necesse.engine.GameTileRange;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.commands.PermissionLevel;
import necesse.engine.incursionPerkTree.IncursionPerk;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.IncursionBiomeRegistry;
import necesse.engine.registries.IncursionPerksRegistry;
import necesse.engine.util.GameRandom;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.engine.world.ReturnedObjects;
import necesse.engine.world.worldData.incursions.OpenIncursion;
import necesse.entity.objectEntity.FallenAltarObjectEntity;
import necesse.entity.objectEntity.interfaces.OEInventory;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.InventoryRange;
import necesse.inventory.container.Container;
import necesse.inventory.container.customAction.BooleanCustomAction;
import necesse.inventory.container.customAction.EmptyCustomAction;
import necesse.inventory.container.customAction.IntCustomAction;
import necesse.inventory.container.object.CraftingStationContainer;
import necesse.inventory.container.slots.GatewayTabletContainerSlot;
import necesse.inventory.item.miscItem.GatewayTabletItem;
import necesse.inventory.recipe.CanCraft;
import necesse.inventory.recipe.Ingredient;
import necesse.inventory.recipe.Recipe;
import necesse.level.maps.incursion.AltarData;
import necesse.level.maps.incursion.IncursionData;
import necesse.level.maps.multiTile.MultiTile;

public class FallenAltarContainer extends Container {
   public FallenAltarObjectEntity altarEntity;
   public int TABLET_SLOT;
   public IntCustomAction openIncursion;
   public EmptyCustomAction enterIncursion;
   public BooleanCustomAction closeIncursion;
   public IntCustomAction obtainPerk;
   public IntCustomAction respecPerk;
   public EmptyCustomAction resetPerkTree;
   public final GameTileRange ingredientRange;
   private final LinkedHashSet<Inventory> nearbyInventories = new LinkedHashSet<>();

   public FallenAltarContainer(final NetworkClient client, int uniqueSeed, final FallenAltarObjectEntity altarEntity, Packet content) {
      super(client, uniqueSeed);
      this.altarEntity = altarEntity;
      this.TABLET_SLOT = this.addSlot(new GatewayTabletContainerSlot(altarEntity.inventory, 0));
      this.addInventoryQuickTransfer(slot -> !altarEntity.hasOpenIncursion(), this.TABLET_SLOT, this.TABLET_SLOT);
      MultiTile multiTile = altarEntity.getLevelObject().getMultiTile();
      Rectangle tileRectangle = multiTile.getTileRectangle(0, 0);
      this.ingredientRange = new GameTileRange(CraftingStationContainer.nearbyCraftTileRange, tileRectangle);
      this.nearbyInventories.addAll(this.craftInventories);

      for (InventoryRange inventoryRange : this.getNearbyInventories(
         altarEntity.getLevel(), altarEntity.tileX, altarEntity.tileY, this.ingredientRange, OEInventory::canUseForNearbyCrafting
      )) {
         this.nearbyInventories.add(inventoryRange.inventory);
      }

      this.checkAchievements();
      new PacketReader(content);
      this.openIncursion = this.registerAction(
         new IntCustomAction() {
            @Override
            protected void run(int value) {
               if (client.isServer()) {
                  if (FallenAltarContainer.this.getSlot(FallenAltarContainer.this.TABLET_SLOT).getItem() != null) {
                     IncursionData incursionData = GatewayTabletItem.getIncursionData(
                        FallenAltarContainer.this.getSlot(FallenAltarContainer.this.TABLET_SLOT).getItem()
                     );
                     if (incursionData != null) {
                        FallenAltarContainer.this.open(incursionData);
                     }
                  }
               }
            }
         }
      );
      this.enterIncursion = this.registerAction(new EmptyCustomAction() {
         @Override
         protected void run() {
            if (client.isServer()) {
               OpenIncursion openIncursion = altarEntity.getOpenIncursion();
               if (openIncursion != null) {
                  altarEntity.enterIncursion(client.getServerClient());
               }
            }
         }
      });
      this.closeIncursion = this.registerAction(new BooleanCustomAction() {
         @Override
         protected void run(boolean isComplete) {
            if (client.isServer()) {
               OpenIncursion openIncursion = altarEntity.getOpenIncursion();
               if (openIncursion != null) {
                  ServerClient serverClient = client.getServerClient();
                  if (openIncursion.canComplete == isComplete) {
                     ReturnedObjects returnedObjects = new ReturnedObjects();
                     if (openIncursion.canComplete) {
                        altarEntity.completeOpenIncursion(FallenAltarContainer.this, serverClient, returnedObjects);
                        altarEntity.altarData.addCompletedIncursion(openIncursion.incursionData);
                     } else {
                        altarEntity.closeOpenIncursion(FallenAltarContainer.this, serverClient, returnedObjects);
                     }

                     for (InventoryItem returnedItem : returnedObjects.items) {
                        serverClient.playerMob.getInv().addItemsDropRemaining(returnedItem, "addback", serverClient.playerMob, true, true);
                     }

                     returnedObjects.items.clear();
                     returnedObjects.returnObjectsToTile(altarEntity.getLevel(), altarEntity.tileX, altarEntity.tileY);
                  }
               }
            }
         }
      });
      final FallenAltarContainer altarContainer = this;
      this.obtainPerk = this.registerAction(
         new IntCustomAction() {
            @Override
            protected void run(int value) {
               IncursionPerk perk = IncursionPerksRegistry.getPerk(value);
               AltarData altarData = altarEntity.altarData;
               boolean canCheat = FallenAltarContainer.this.canCheat();
               if (perk.canObtainPerk(altarData, canCheat)) {
                  boolean canAfford = perk.canAffordToBuyPerk(altarContainer);
                  boolean shouldCheat = canCheat && !canAfford;
                  if (canAfford || shouldCheat) {
                     if (!shouldCheat) {
                        Recipe.craft(
                           perk.buyPerkIngredientCost, client.playerMob.getLevel(), client.playerMob, FallenAltarContainer.this.getCraftInventories(), null
                        );
                     }

                     altarData.obtainPerk(perk);
                     if (perk.getTabletIDForTabletDropPerks() != -1 && !altarData.tabletHasBeenRewardedAlready(perk)) {
                        GameRandom random = new GameRandom(altarContainer.uniqueSeed);
                        InventoryItem tablet = new InventoryItem("gatewaytablet");
                        GatewayTabletItem.initializeCustomGateTablet(
                           tablet, random, perk.tier, IncursionBiomeRegistry.getBiome(perk.getTabletIDForTabletDropPerks()), altarData.obtainedPerkIDs
                        );
                        tablet.setNew(true);
                        client.playerMob.getInv().addItemsDropRemaining(tablet, "addback", client.playerMob, !client.isServer(), true, true);
                        altarData.addPerkIDToTabletsRewardedList(perk);
                        if (client.isServer() && !shouldCheat) {
                           FallenAltarContainer.this.close();
                        }
                     }

                     altarEntity.markDirty();
                     FallenAltarContainer.this.checkAchievements();
                  }
               }
            }
         }
      );
      this.respecPerk = this.registerAction(
         new IntCustomAction() {
            @Override
            protected void run(int value) {
               IncursionPerk perk = IncursionPerksRegistry.getPerk(value);
               AltarData altarData = altarEntity.altarData;
               if (perk.canRespecPerk(altarData)) {
                  boolean canAfford = perk.canAffordToRespecPerk(altarContainer);
                  boolean shouldCheat = FallenAltarContainer.this.canCheat() && !canAfford;
                  if (canAfford || shouldCheat) {
                     if (!shouldCheat) {
                        Recipe.craft(
                           perk.respecIngredientCost, client.playerMob.getLevel(), client.playerMob, FallenAltarContainer.this.getCraftInventories(), null
                        );
                        InventoryItem altarDust = new InventoryItem("altardust", perk.perkCost);
                        client.playerMob.getInv().addItemsDropRemaining(altarDust, "addback", client.playerMob, !client.isServer(), true, true);
                     }

                     altarData.removePerk(perk);
                     altarEntity.markDirty();
                  }
               }
            }
         }
      );
      this.resetPerkTree = this.registerAction(
         new EmptyCustomAction() {
            @Override
            protected void run() {
               AltarData altarData = altarEntity.altarData;
               boolean canAfford = FallenAltarContainer.this.canAffordFullPerkTreeRespec(altarData).canCraft();
               boolean shouldCheat = FallenAltarContainer.this.canCheat() && !canAfford;
               if (canAfford || shouldCheat) {
                  if (!shouldCheat) {
                     Recipe.craft(
                        FallenAltarContainer.this.getFullAltarRespecIngredientCost(altarData),
                        client.playerMob.getLevel(),
                        client.playerMob,
                        FallenAltarContainer.this.getCraftInventories(),
                        null
                     );
                     InventoryItem altarDust = new InventoryItem("altardust", FallenAltarContainer.this.getAltarDustAmountFromFullPerkTreeRespec(altarData));
                     client.playerMob.getInv().addItemsDropRemaining(altarDust, "addback", client.playerMob, !client.isServer(), true, true);
                  }

                  altarEntity.altarData.obtainedPerkIDs.clear();
                  altarEntity.markDirty();
               }
            }
         }
      );
   }

   public boolean canCheat() {
      if (GlobalData.isDevMode() && this.client.playerMob.getLevel().getWorldSettings().cheatsAllowedOrHidden()) {
         if (this.client.isServer()) {
            return this.client.getServerClient().getServer().world.settings.cheatsAllowedOrHidden()
               && this.client.getServerClient().getPermissionLevel().getLevel() >= PermissionLevel.OWNER.getLevel();
         }

         if (this.client.isClient()) {
            return this.client.getClientClient().getClient().worldSettings.cheatsAllowedOrHidden()
               && this.client.getClientClient().getClient().getPermissionLevel().getLevel() >= PermissionLevel.OWNER.getLevel();
         }
      }

      return false;
   }

   public boolean canCheatAndShiftIsDown() {
      if (!this.canCheat()) {
         return false;
      }

      if (this.client.isServer()) {
         throw new IllegalStateException("This method should only be called on the client!");
      }

      GameWindow window = WindowManager.getWindow();
      return window.isKeyDown(340) || window.isKeyDown(344);
   }

   @Override
   public boolean isValid(ServerClient client) {
      return !super.isValid(client) ? false : !this.altarEntity.removed() && this.altarEntity.getLevelObject().isInInteractRange(client.playerMob);
   }

   public boolean open(IncursionData incursion) {
      if (incursion.getCanOpenError(this) == null) {
         if (this.client.isServer()) {
            this.altarEntity.openIncursion(this, incursion, this.client.getServerClient());
         }

         return true;
      } else {
         return false;
      }
   }

   public static Packet getContainerContent(Server server, FallenAltarObjectEntity altar) {
      Packet packet = new Packet();
      new PacketWriter(packet);
      return packet;
   }

   public CanCraft canAffordPerk(IncursionPerk perk) {
      return this.canCraftRecipe(perk.buyPerkIngredientCost, this.getCraftInventories(), true, null);
   }

   public CanCraft canAffordPerkRespec(IncursionPerk perk) {
      return this.canCraftRecipe(perk.respecIngredientCost, this.getCraftInventories(), true, null);
   }

   public CanCraft canAffordFullPerkTreeRespec(AltarData altarData) {
      return this.canCraftRecipe(this.getFullAltarRespecIngredientCost(altarData), this.getCraftInventories(), true, null);
   }

   public Ingredient[] getFullAltarRespecIngredientCost(AltarData altarData) {
      int respecCostInCoins = altarData.obtainedPerkIDs.size() * 1000;
      return new Ingredient[]{new Ingredient("coin", respecCostInCoins)};
   }

   public int getAltarDustAmountFromFullPerkTreeRespec(AltarData altarData) {
      int dust = 0;

      for (Integer obtainedPerkID : altarData.obtainedPerkIDs) {
         dust += IncursionPerksRegistry.getPerk(obtainedPerkID).perkCost;
      }

      return dust;
   }

   private boolean useNearbyInventories() {
      return this.client.isServer() ? this.client.craftingUsesNearbyInventories : Settings.craftingUseNearby.get();
   }

   @Override
   public Collection<Inventory> getCraftInventories() {
      return this.useNearbyInventories() ? this.nearbyInventories : super.getCraftInventories();
   }

   public void checkAchievements() {
      if (this.client.isServer()) {
         ServerClient serverClient = this.client.getServerClient();
         if (serverClient.achievementsLoaded()) {
            if (this.altarEntity.altarData.obtainedPerkIDs.size() >= 40) {
               serverClient.achievements().HAVE_40_PERKS.markCompleted(serverClient);
            }
         }
      }
   }
}
