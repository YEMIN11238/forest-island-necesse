package necesse.level.gameObject;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.Performance;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.registries.TileRegistry;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class CobwebObject extends GrassObject {
   public CobwebObject() {
      super("cobweb", -1);
      this.mapColor = new Color(120, 120, 120);
      this.displayMapTooltip = true;
      this.weaveAmount = 0.05F;
      this.extraWeaveSpace = 32;
      this.randomYOffset = 4.0F;
      this.objectTileLayerIgnored = true;
      this.playerCanPlaceOnOrganic = false;
      this.grassValidTileIDs.add(TileRegistry.spiderNestID);
   }

   @Override
   public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
      return level.objectLayer.isPlayerPlaced(tileX, tileY) ? super.getLootTable(level, layerID, tileX, tileY) : new LootTable();
   }

   @Override
   protected String runGrassCanPlace(Level level, int x, int y, boolean byPlayer) {
      return byPlayer && !level.isLiquidTile(x, y) ? null : super.runGrassCanPlace(level, x, y, byPlayer);
   }

   @Override
   public int getLightLevelMod(Level level, int x, int y) {
      return 30;
   }

   @Override
   public void addDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      Performance.record(tickManager, "cobwebSetup", () -> {
         Integer[] adj = level.getAdjacentObjectsInt(tileX, tileY);
         int objs = 0;

         for (Integer id : adj) {
            if (id == this.getID()) {
               objs++;
            }
         }

         int minTextureIndex;
         int maxTextureIndex;
         if (objs < 5) {
            minTextureIndex = 4;
            maxTextureIndex = 7;
         } else {
            minTextureIndex = 0;
            maxTextureIndex = 3;
         }

         int drawX = camera.getTileDrawX(tileX);
         int drawY = camera.getTileDrawY(tileY);
         GameLight light = level.getLightLevel(tileX, tileY);
         this.addGrassDrawable(list, tileList, level, tileX, tileY, drawX, drawY, light, 6, -5, 0, minTextureIndex, maxTextureIndex);
         this.addGrassDrawable(list, tileList, level, tileX, tileY, drawX, drawY, light, 26, -5, 1, minTextureIndex, maxTextureIndex);
      });
   }

   @Override
   public ModifierValue<Float> getSlowModifier(Mob mob) {
      return !mob.isHostile && !mob.isFlying() ? new ModifierValue<>(BuffModifiers.SLOW, 0.5F) : super.getSpeedModifier(mob);
   }
}
