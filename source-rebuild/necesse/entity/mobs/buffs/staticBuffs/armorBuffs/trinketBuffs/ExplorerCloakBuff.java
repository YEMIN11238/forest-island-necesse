package necesse.entity.mobs.buffs.staticBuffs.armorBuffs.trinketBuffs;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.trinketItem.TrinketItem;
import necesse.level.gameTile.GameTile;
import necesse.level.gameTile.LiquidTile;

public class ExplorerCloakBuff extends OutOfCombatBuff {
   @Override
   public void tickEffect(ActiveBuff buff, Mob owner) {
      if (this.isActive(buff) && (owner.dx != 0.0F || owner.dy != 0.0F)) {
         Color color;
         if (owner.inLiquid()) {
            GameTile tile = owner.getLevel().getTile(owner.getTileX(), owner.getTileY());
            if (tile.isLiquid) {
               color = ((LiquidTile)tile).getLiquidColor(owner.getLevel(), owner.getTileX(), owner.getTileY()).brighter();
            } else {
               color = new Color(89, 139, 224);
            }
         } else {
            color = new Color(65, 30, 109);
         }

         boolean next = buff.getGndData().getBoolean("pAlt");
         buff.getGndData().setBoolean("pAlt", !next);
         int dir = owner.getDir();
         Float pos;
         if (dir != 0 && dir != 2) {
            pos = new Float(owner.x, owner.y + (next ? -4 : 4));
         } else {
            pos = new Float(owner.x + (next ? -4 : 4), owner.y);
         }

         owner.getLevel()
            .entityManager
            .addParticle(
               pos.x + (float)(GameRandom.globalRandom.nextGaussian() * 2.0),
               pos.y + (float)(GameRandom.globalRandom.nextGaussian() * 2.0),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .color(color)
            .sizeFades(10, 12)
            .movesConstant(owner.dx / 10.0F, owner.dy / 10.0F)
            .lifeTime(300)
            .height(0.0F);
      }
   }

   @Override
   protected void updateActive(ActiveBuff buff, boolean active) {
      buff.setModifier(BuffModifiers.EXTENDED_MAP_DISCOVER_RANGE, true);
      if (active) {
         buff.setModifier(BuffModifiers.SPEED_FLAT, 5.0F);
         buff.setModifier(BuffModifiers.SPEED, 0.15F);
         buff.setModifier(BuffModifiers.SWIM_SPEED, 1.0F);
      }
   }

   @Override
   public ListGameTooltips getTrinketTooltip(TrinketItem trinketItem, InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getTrinketTooltip(trinketItem, item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "travelercloak"));
      tooltips.add(Localization.translate("itemtooltip", "piratetelescopemap"));
      return tooltips;
   }
}
