package necesse.inventory.item.placeableItem.objectItem;

import java.awt.Color;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDAltarDataItem;
import necesse.engine.network.gameNetworkData.GNDItem;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.IncursionPerksRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.FallenAltarObjectEntity;
import necesse.gfx.GameColor;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.inventory.InventoryItem;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.incursion.AltarData;

public class FallenAltarObjectItem extends ObjectItem {
   public FallenAltarObjectItem(GameObject object) {
      super(object);
   }

   @Override
   public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
      ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
      AltarData altarData = getAltarData(item.getGndData());
      if (altarData != null && altarData.obtainedPerkIDs != null) {
         Color purple = new Color(184, 69, 227);
         String tierText = Localization.translate("incursion", "altartierlevel", "value", altarData.getAltarTier());
         tooltips.add(new StringTooltips(tierText, GameColor.ITEM_EPIC, 350));
         tooltips.add(Localization.translate("incursion", "containsthefollowingperks"));

         for (Integer obtainedPerkID : altarData.obtainedPerkIDs) {
            String perkText = Localization.translate("incursion", IncursionPerksRegistry.getPerkStringID(obtainedPerkID));
            tooltips.add(new StringTooltips(perkText, purple, 350));
         }
      }

      return tooltips;
   }

   @Override
   public boolean canCombineItem(Level level, PlayerMob player, InventoryItem me, InventoryItem them, String purpose) {
      return !super.canCombineItem(level, player, me, them, purpose) ? false : this.isSameGNDData(level, me, them, purpose);
   }

   @Override
   public boolean isSameGNDData(Level level, InventoryItem me, InventoryItem them, String purpose) {
      return me.getGndData().sameKeys(them.getGndData(), "altarData");
   }

   @Override
   public boolean onPlaceObject(GameObject object, Level level, int layerID, int tileX, int tileY, int rotation, ServerClient client, InventoryItem item) {
      boolean success = super.onPlaceObject(object, level, layerID, tileX, tileY, rotation, client, item);
      if (success) {
         FallenAltarObjectEntity objectEntity = level.entityManager.getObjectEntity(tileX, tileY, FallenAltarObjectEntity.class);
         if (objectEntity != null) {
            AltarData altarData = getAltarData(item.getGndData());
            if (altarData != null) {
               objectEntity.altarData = altarData.makeCopy();
            }
         }
      }

      return success;
   }

   public static InventoryItem setupAltarItem(InventoryItem item, AltarData altarData) {
      GNDItemMap gndData = item.getGndData();
      setupAltarGNDData(gndData, altarData);
      return item;
   }

   public static GNDItemMap altarGNDData(AltarData altarData) {
      GNDItemMap out = new GNDItemMap();
      setupAltarGNDData(out, altarData);
      return out;
   }

   public static void setupAltarGNDData(GNDItemMap gndData, AltarData altarData) {
      gndData.setItem("altarData", new GNDAltarDataItem(altarData.makeCopy()));
   }

   public static AltarData getAltarData(GNDItemMap gndData) {
      GNDItem altarData = gndData.getItem("altarData");
      return altarData instanceof GNDAltarDataItem ? ((GNDAltarDataItem)altarData).altarData : null;
   }
}
