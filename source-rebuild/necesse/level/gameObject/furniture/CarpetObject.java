package necesse.level.gameObject.furniture;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.StaticMultiTile;

public class CarpetObject extends FurnitureObject {
   protected String textureName;
   public GameTexture texture;
   private int counterIDTopRight;
   private int counterIDBotLeft;
   private int counterIDBotRight;

   protected CarpetObject(String textureName, ToolType toolType, Color mapColor) {
      super(new Rectangle(0, 0));
      this.textureName = textureName;
      this.toolType = toolType;
      this.mapColor = mapColor;
      this.objectHealth = 50;
      this.isLightTransparent = true;
      this.furnitureType = "carpet";
      this.setItemCategory("objects", "decorations", "carpets");
      this.setCraftingCategory("objects", "decorations", "carpets");
   }

   @Override
   public MultiTile getMultiTile(int rotation) {
      return new StaticMultiTile(0, 0, 2, 2, true, this.getID(), this.counterIDTopRight, this.counterIDBotLeft, this.counterIDBotRight);
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
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
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      TextureDrawOptions options = this.texture.initDraw().sprite(0, 0, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY);
      tileList.add(tm -> options.draw());
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 0, 32).alpha(alpha).draw(drawX, drawY);
      this.texture.initDraw().sprite(1, 0, 32).alpha(alpha).draw(drawX + 32, drawY);
      this.texture.initDraw().sprite(0, 1, 32).alpha(alpha).draw(drawX, drawY + 32);
      this.texture.initDraw().sprite(1, 1, 32).alpha(alpha).draw(drawX + 32, drawY + 32);
   }

   protected void setCounterIDs(int topRight, int botLeft, int botRight) {
      this.counterIDTopRight = topRight;
      this.counterIDBotLeft = botLeft;
      this.counterIDBotRight = botRight;
   }

   public static int[] registerCarpet(String stringID, String textureName, ToolType toolType, Color mapColor, float brokerValue) {
      CarpetObject obj1 = new CarpetObject(textureName, toolType, mapColor);
      CarpetRObject obj2 = new CarpetRObject(textureName, toolType, mapColor);
      CarpetDObject obj3 = new CarpetDObject(textureName, toolType, mapColor);
      CarpetDRObject obj4 = new CarpetDRObject(textureName, toolType, mapColor);
      int id1 = ObjectRegistry.registerObject(stringID, obj1, brokerValue, true);
      int id2 = ObjectRegistry.registerObject(stringID + "r", obj2, 0.0F, false);
      int id3 = ObjectRegistry.registerObject(stringID + "d", obj3, 0.0F, false);
      int id4 = ObjectRegistry.registerObject(stringID + "dr", obj4, 0.0F, false);
      obj1.setCounterIDs(id2, id3, id4);
      obj2.setCounterIDs(id1, id3, id4);
      obj3.setCounterIDs(id1, id2, id4);
      obj4.setCounterIDs(id1, id2, id3);
      return new int[]{id1, id2, id3, id4};
   }

   public static int[] registerCarpet(String stringID, String textureName, Color mapColor, float brokerValue) {
      return registerCarpet(stringID, textureName, ToolType.ALL, mapColor, brokerValue);
   }
}
