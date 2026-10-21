package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.regionSystem.SimulatePriorityList;

public class FlowerPatchObject extends GameObject {
   public static HashSet<Integer> flowerPatchObjectIDs = new HashSet<>();
   public static double spreadChance = GameMath.getAverageSuccessRuns(900.0);
   protected String textureName;
   protected GameTexture texture;
   protected final GameRandom drawRandom;

   public FlowerPatchObject(String textureName, Color mapColor) {
      super(new Rectangle(0, 0, 0, 0));
      this.textureName = textureName;
      this.mapColor = mapColor;
      this.toolType = ToolType.ALL;
      this.objectHealth = 10;
      this.displayMapTooltip = true;
      this.drawDamage = false;
      this.drawRandom = new GameRandom();
      this.validObjectLayers.add(ObjectLayerRegistry.TILE_LAYER);
      this.hoverHitboxSortY = -16;
      this.setItemCategory("objects", "landscaping", "plants");
      this.roomProperties.add("flower");
      this.roomProperties.add("plant");
   }

   @Override
   public void onObjectRegistryClosed() {
      super.onObjectRegistryClosed();
      flowerPatchObjectIDs.add(this.getID());
   }

   public GrassSpreadOptions getSpreadOptions(Level level) {
      return GrassSpreadOptions.init(this, level)
         .maxSpread(3, 6, 2, p -> flowerPatchObjectIDs.contains(level.getObjectID(p.x, p.y)))
         .placeMethod(p -> this.placeObject(level, p.x, p.y, 0, true));
   }

   @Override
   public void tick(Level level, int x, int y) {
      super.tick(level, x, y);
      if (level.isServer() && level.objectLayer.isPlayerPlaced(x, y) && GameRandom.globalRandom.getChance(spreadChance)) {
         this.getSpreadOptions(level).tickSpread(x, y, true);
      }
   }

   @Override
   public void addSimulateLogic(Level level, int x, int y, long ticks, SimulatePriorityList list, boolean sendChanges) {
      super.addSimulateLogic(level, x, y, ticks, list, sendChanges);
      if (level.objectLayer.isPlayerPlaced(x, y)) {
         this.getSpreadOptions(level).addSimulateSpread(x, y, spreadChance, ticks, list, sendChanges);
      }
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
   }

   @Override
   public void addLayerDrawables(
      List<LevelSortedDrawable> list,
      OrderableDrawables tileList,
      Level level,
      int layerID,
      int tileX,
      int tileY,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int randomYOffset = this.getRandomYOffset(tileX, tileY);
      int sprite;
      synchronized (this.drawRandom) {
         sprite = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(this.texture.getWidth() / 32);
      }

      drawY += randomYOffset;
      TextureDrawOptions options = this.texture
         .initDraw()
         .sprite(sprite, 0, 32, this.texture.getHeight())
         .addObjectDamageOverlay(this, level, layerID, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - this.texture.getHeight() + 32);
      tileList.add(tm -> options.draw());
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      int randomYOffset = this.getRandomYOffset(tileX, tileY);
      int sprite;
      synchronized (this.drawRandom) {
         sprite = this.drawRandom.seeded(getTileSeed(tileX, tileY)).nextInt(this.texture.getWidth() / 32);
      }

      drawY += randomYOffset;
      this.texture.initDraw().sprite(sprite, 0, 32, this.texture.getHeight()).light(light).alpha(alpha).draw(drawX, drawY - this.texture.getHeight() + 32);
   }

   public int getRandomYOffset(int tileX, int tileY) {
      synchronized (this.drawRandom) {
         return (int)((this.drawRandom.seeded(getTileSeed(tileX, tileY, 1)).nextFloat() * 2.0F - 1.0F) * 8.0F) - 4;
      }
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      String superError = super.canPlace(level, layerID, x, y, rotation, byPlayer, ignoreOtherLayers);
      if (superError != null) {
         return superError;
      }

      if (!level.getTile(x, y).isOrganic && !level.getTile(x, y).getStringID().equals("snowtile")) {
         return "notorganic";
      }

      if (layerID != 0) {
         GameObject object = level.getObject(0, x, y);
         if (object.isWall && !object.isDoor || object.isRock) {
            return "tilecovered";
         }
      }

      return null;
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      if (!super.isValid(level, layerID, x, y)) {
         return false;
      }

      if (!level.getTile(x, y).isOrganic && !level.getTile(x, y).getStringID().equals("snowtile")) {
         return false;
      }

      if (layerID == 0) {
         return true;
      }

      GameObject object = level.getObject(0, x, y);
      return (!object.isWall || object.isDoor) && !object.isRock;
   }

   @Override
   public void playDamageSound(Level level, int x, int y, boolean damageDone) {
      SoundManager.playSound(GameResources.grass, SoundEffect.effect(x * 32 + 16, y * 32 + 16));
   }
}
