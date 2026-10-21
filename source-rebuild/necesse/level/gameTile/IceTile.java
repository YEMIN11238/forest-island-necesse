package necesse.level.gameTile;

import java.awt.Color;
import java.awt.Point;
import necesse.engine.localization.Localization;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.TileRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.level.maps.Level;

public class IceTile extends TerrainSplatterTile {
   private final GameRandom drawRandom;

   public IceTile() {
      super(false, "ice");
      this.mapColor = new Color(68, 186, 226);
      this.canBeMined = true;
      this.drawRandom = new GameRandom();
   }

   @Override
   public Point getTerrainSprite(GameTextureSection terrainTexture, Level level, int tileX, int tileY) {
      int tile;
      synchronized (this.drawRandom) {
         tile = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(terrainTexture.getHeight() / 32);
      }

      return new Point(0, tile);
   }

   @Override
   public int getTerrainPriority() {
      return 200;
   }

   @Override
   public ModifierValue<Float> getSpeedModifier(Mob mob) {
      return mob.isFlying() ? super.getSpeedModifier(mob) : new ModifierValue<>(BuffModifiers.SPEED, 0.25F);
   }

   @Override
   public ModifierValue<Float> getFrictionModifier(Mob mob) {
      return mob.isFlying() ? super.getFrictionModifier(mob) : new ModifierValue<>(BuffModifiers.FRICTION, -0.75F);
   }

   @Override
   public String canPlace(Level level, int x, int y, boolean byPlayer) {
      if (level.getTileID(x, y) != TileRegistry.waterID) {
         return "notwater";
      }

      boolean hasShore = !level.getTile(x - 1, y).isLiquid;
      hasShore = hasShore || !level.getTile(x + 1, y).isLiquid;
      hasShore = hasShore || !level.getTile(x, y - 1).isLiquid;
      hasShore = hasShore || !level.getTile(x, y + 1).isLiquid;
      return !hasShore ? "notshore" : null;
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(Localization.translate("itemtooltip", "icetip"));
      return tooltips;
   }

   @Override
   public int getDestroyedTile() {
      return TileRegistry.waterID;
   }

   @Override
   public boolean canBePlacedOn(Level level, int tileX, int tileY, GameTile placing) {
      return false;
   }
}
