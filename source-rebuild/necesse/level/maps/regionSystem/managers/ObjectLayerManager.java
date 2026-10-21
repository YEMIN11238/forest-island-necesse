package necesse.level.maps.regionSystem.managers;

import java.util.ArrayList;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.level.gameObject.GameObject;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.regionSystem.Region;

public class ObjectLayerManager {
   protected final Level level;

   public ObjectLayerManager(Level level) {
      this.level = level;
   }

   public int getObjectID(int layerID, int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null ? 0 : region.objectLayer.getObjectIDByRegion(layerID, tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public GameObject getObject(int layerID, int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null
         ? ObjectRegistry.getObject(0)
         : region.objectLayer.getObjectByRegion(layerID, tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public void setObject(int layerID, int tileX, int tileY, int objectID) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, true);
      if (region != null) {
         region.objectLayer.setObjectByRegion(layerID, tileX - region.tileXOffset, tileY - region.tileYOffset, objectID);
      }
   }

   public byte getObjectRotation(int layerID, int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null ? 0 : region.objectLayer.getObjectRotationByRegion(layerID, tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public void setObjectRotation(int layerID, int tileX, int tileY, int rotation) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, true);
      if (region != null) {
         region.objectLayer.setObjectRotationByRegion(layerID, tileX - region.tileXOffset, tileY - region.tileYOffset, rotation);
      }
   }

   public boolean isPlayerPlaced(int layerID, int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null ? false : region.objectLayer.isPlayerPlacedByRegion(layerID, tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public boolean isPlayerPlaced(int tileX, int tileY) {
      return this.isPlayerPlaced(0, tileX, tileY);
   }

   public void setIsPlayerPlaced(int layerID, int tileX, int tileY, boolean isPlayerPlaced) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, true);
      if (region != null) {
         region.objectLayer.setIsPlayerPlacedByRegion(layerID, tileX - region.tileXOffset, tileY - region.tileYOffset, isPlayerPlaced);
      }
   }

   public void setIsPlayerPlaced(int tileX, int tileY, boolean isPlayerPlaced) {
      this.setIsPlayerPlaced(0, tileX, tileY, isPlayerPlaced);
   }

   public ArrayList<LevelObject> getHitboxPriorityList(int tileX, int tileY, boolean ignoreAir) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null
         ? new ArrayList<>()
         : region.objectLayer.getHitboxPriorityListByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset, ignoreAir);
   }

   public GameLight getCombinedLight(int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null
         ? this.level.lightManager.newLight(0.0F, 0.0F, 0.0F)
         : region.objectLayer.getCombinedLightByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }

   public GameObject addObjectDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      Region region = level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null
         ? ObjectRegistry.getObject(0)
         : region.objectLayer
            .addObjectDrawablesByRegion(list, tileList, level, tileX - region.tileXOffset, tileY - region.tileYOffset, tickManager, camera, perspective);
   }

   public void addObjectsDebugTooltip(StringTooltips tooltips, int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      if (region != null) {
         region.objectLayer.addObjectsDebugTooltipByRegion(tooltips, tileX - region.tileXOffset, tileY - region.tileYOffset);
      }
   }

   public int getTileObjectsHash(int tileX, int tileY) {
      Region region = this.level.regionManager.getRegionByTile(tileX, tileY, false);
      return region == null ? 0 : region.objectLayer.getTileObjectsHashByRegion(tileX - region.tileXOffset, tileY - region.tileYOffset);
   }
}
