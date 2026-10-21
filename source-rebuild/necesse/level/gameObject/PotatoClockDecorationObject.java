package necesse.level.gameObject;

import java.awt.Color;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameUtils;
import necesse.engine.world.WorldEntity;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.gameTooltips.GameTooltipManager;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.gfx.gameTooltips.TooltipLocation;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;

public class PotatoClockDecorationObject extends TableDecorationObject {
   public PotatoClockDecorationObject(String textureName, Color mapColor, int decorationWidth, int decorationHeight) {
      super(textureName, ToolType.ALL, mapColor, decorationWidth, decorationHeight, 0, 0);
      this.rarity = Item.Rarity.UNIQUE;
   }

   @Override
   public void onMouseHover(Level level, int x, int y, GameCamera camera, PlayerMob perspective, boolean debug) {
      super.onMouseHover(level, x, y, camera, perspective, debug);
      if (perspective != null) {
         WorldEntity worldEntity = perspective.getWorldEntity();
         StringTooltips tooltips = new StringTooltips();
         tooltips.add(Localization.translate("ui", "currenttime", "time", worldEntity.getDayTimeReadable()));
         tooltips.add(worldEntity.getTimeOfDay().displayName.translate());
         GameTooltipManager.addTooltip(tooltips, TooltipLocation.INTERACT_FOCUS);
      }
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      if (perspective != null) {
         WorldEntity worldEntity = perspective.getWorldEntity();
         tooltips.add(Localization.translate("ui", "currenttime", "time", worldEntity.getDayTimeReadable()));
         tooltips.add(worldEntity.getTimeOfDay().displayName.translate());
         tooltips.add(GameUtils.getDevQuoteTooltip("itemtooltip", "potatoclockquote", "Jonathan"));
      }

      return tooltips;
   }
}
