package necesse.level.gameObject.happinessObject;

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
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.HappinessObject;
import necesse.level.gameObject.furniture.FurnitureObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.StaticMultiTile;

public class FishianBathtubObject extends FurnitureObject implements HappinessObject {
   protected String texturePath;
   public GameTexture texture;
   protected int counterID;

   protected FishianBathtubObject(String texturePath, ToolType toolType, Color mapColor, String... category) {
      super(new Rectangle(8, 10, 24, 2));
      this.texturePath = texturePath;
      this.toolType = toolType;
      this.mapColor = mapColor;
      this.objectHealth = 50;
      this.isLightTransparent = true;
      this.furnitureType = "bathtub";
      this.rarity = Item.Rarity.RARE;
      if (category.length > 0) {
         this.setItemCategory(category);
         this.setCraftingCategory(category);
      } else {
         this.setItemCategory("objects", "furniture");
         this.setCraftingCategory("objects", "furniture");
      }
   }

   @Override
   public MultiTile getMultiTile(int rotation) {
      return new StaticMultiTile(0, 0, 2, 1, true, this.getID(), this.counterID);
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile(this.texturePath);
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
      final TextureDrawOptions top = this.texture.initDraw().sprite(0, 0, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY);
      final TextureDrawOptions bot = this.texture.initDraw().sprite(0, 1, 32).addObjectDamageOverlay(this, level, tileX, tileY).light(light).pos(drawX, drawY);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 5;
         }

         @Override
         public void draw(TickManager tickManager) {
            top.draw();
         }
      });
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 28;
         }

         @Override
         public void draw(TickManager tickManager) {
            bot.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 1, 32).alpha(alpha).draw(drawX, drawY);
      this.texture.initDraw().sprite(0, 0, 32).alpha(alpha).draw(drawX, drawY);
      this.texture.initDraw().sprite(1, 1, 32).alpha(alpha).draw(drawX + 32, drawY);
      this.texture.initDraw().sprite(1, 0, 32).alpha(alpha).draw(drawX + 32, drawY);
   }

   public static int[] registerBathtub(String stringID, String textureName, ToolType toolType, Color mapColor, float brokerValue, String... category) {
      String texturePath = "objects/" + textureName;
      FishianBathtubObject obj1 = new FishianBathtubObject(texturePath, toolType, mapColor, category);
      FishianBathtub2Object obj2 = new FishianBathtub2Object(texturePath, toolType, mapColor, category);
      int id1 = ObjectRegistry.registerObject(stringID, obj1, brokerValue, true, false);
      int id2 = ObjectRegistry.registerObject(stringID + "2", obj2, 0.0F, false);
      obj1.counterID = id2;
      obj2.counterID = id1;
      return new int[]{id1, id2};
   }

   public static int[] registerBathtub(String stringID, String textureName, Color mapColor, float brokerValue, String... category) {
      return registerBathtub(stringID, textureName, ToolType.ALL, mapColor, brokerValue, category);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
