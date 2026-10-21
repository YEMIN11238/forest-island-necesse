package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.FileNotFoundException;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.registries.ObjectLayerRegistry;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.multiTile.MultiTile;
import necesse.level.maps.multiTile.StaticMultiTile;

class LargeTableDecorationObject2 extends GameObject {
   protected String textureName;
   public GameTexture texture;
   public GameTexture texture_decor;
   public int decorationWidth;
   public int decorationHeight;
   public int holderDrawXOffset;
   public int holderDrawYOffset;
   protected int counterID;
   public GameMessage customLocalization;

   public LargeTableDecorationObject2(
      String textureName, ToolType toolType, Color mapColor, int decorationWidth, int decorationHeight, int holderDrawXOffset, int holderDrawYOffset
   ) {
      this.textureName = textureName;
      this.toolType = toolType;
      this.mapColor = mapColor;
      this.decorationWidth = decorationWidth;
      this.decorationHeight = decorationHeight;
      this.holderDrawXOffset = holderDrawXOffset;
      this.holderDrawYOffset = holderDrawYOffset;
      this.setItemCategory("objects", "landscaping", "tabledecorations");
      this.setCraftingCategory("objects", "landscaping", "tabledecorations");
      this.objectHealth = 50;
      this.stackSize = 100;
      this.displayMapTooltip = true;
      this.isLightTransparent = true;
      this.canPlaceOnShore = true;
      this.validObjectLayers.add(ObjectLayerRegistry.FENCE_AND_TABLE_DECOR);
   }

   public LargeTableDecorationObject2 setCustomLocalization(GameMessage name) {
      this.customLocalization = name;
      return this;
   }

   public LargeTableDecorationObject2 setCustomLocalization(String category, String key) {
      return this.setCustomLocalization(new LocalMessage(category, key));
   }

   public LargeTableDecorationObject2 setCustomLocalization(String key) {
      return this.setCustomLocalization("object", key);
   }

   @Override
   public GameMessage getNewLocalization() {
      return this.customLocalization != null ? this.customLocalization : super.getNewLocalization();
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = new ListGameTooltips();
      tooltips.add(Localization.translate("itemtooltip", "tabledecorationtip"));
      tooltips.add(Localization.translate("itemtooltip", "placetip"));
      return tooltips;
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);

      try {
         this.texture_decor = GameTexture.fromFileRaw("objects/" + this.textureName + "_decor");
      } catch (FileNotFoundException e) {
         this.texture_decor = this.texture;
      }
   }

   @Override
   public MultiTile getMultiTile(int rotation) {
      return new StaticMultiTile(1, 0, 2, 1, false, this.counterID, this.getID());
   }

   @Override
   protected ObjectHoverHitbox getHoverHitbox(Level level, int layerID, int tileX, int tileY) {
      ObjectHoverHitbox hitbox = super.getHoverHitbox(level, layerID, tileX, tileY);
      if (layerID == ObjectLayerRegistry.FENCE_AND_TABLE_DECOR) {
         DecorationHolderInterface holder = this.getDecorationHolder(level, tileX, tileY);
         if (holder != null) {
            int offset = holder.getDecorationDrawOffset(level, tileX, tileY, this).yOffset + this.holderDrawYOffset;
            hitbox.y += offset;
            hitbox.height -= offset;
         }
      }

      return hitbox;
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
      GameTexture texture = this.texture;
      GameTexture textureDecor = this.texture_decor;
      int sortY = 16;
      if (layerID == ObjectLayerRegistry.FENCE_AND_TABLE_DECOR) {
         LevelObject masterObject = this.getMultiTile(level, layerID, tileX, tileY).getMasterLevelObject(level, layerID, tileX, tileY).orElse(null);
         int holderTileX;
         int holderTileY;
         if (masterObject != null) {
            holderTileX = masterObject.tileX;
            holderTileY = masterObject.tileY;
         } else {
            holderTileX = tileX;
            holderTileY = tileY;
         }

         DecorationHolderInterface holder = this.getDecorationHolder(level, holderTileX, holderTileY);
         if (holder != null) {
            DecorDrawOffset drawOffset = holder.getDecorationDrawOffset(level, holderTileX, holderTileY, this);
            drawX += drawOffset.xOffset + this.holderDrawXOffset;
            drawY += drawOffset.yOffset + this.holderDrawYOffset;
            sortY = drawOffset.sortY;
            if (!drawOffset.useShadowTexture) {
               texture = textureDecor;
            }
         }
      }

      final TextureDrawOptions options = texture.initDraw()
         .sprite(1, 0, 32, texture.getHeight())
         .addObjectDamageOverlay(this, level, layerID, tileX, tileY)
         .light(light)
         .pos(drawX, drawY - texture.getHeight() + 32);
      final int finalSortY = sortY;
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return finalSortY;
         }

         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      GameTexture texture = this.texture;
      GameTexture textureDecor = this.texture_decor;
      Point holderTile = this.getMultiTile(rotation).getMasterTilePos(tileX, tileY).orElse(null);
      if (holderTile == null) {
         holderTile = new Point(tileX, tileY);
      }

      DecorationHolderInterface holder = this.getDecorationHolder(level, holderTile.x, holderTile.y);
      if (holder != null) {
         DecorDrawOffset drawOffset = holder.getDecorationDrawOffset(level, holderTile.x, holderTile.y, this);
         drawX += drawOffset.xOffset + this.holderDrawXOffset;
         drawY += drawOffset.yOffset + this.holderDrawYOffset;
         if (!drawOffset.useShadowTexture) {
            texture = textureDecor;
         }
      }

      texture.initDraw().sprite(1, 0, 32, texture.getHeight()).alpha(alpha).draw(drawX, drawY - texture.getHeight() + 32);
   }

   public DecorationHolderInterface getDecorationHolder(Level level, int tileX, int tileY) {
      GameObject object = level.getObject(tileX, tileY);
      return object instanceof DecorationHolderInterface ? (DecorationHolderInterface)object : null;
   }

   @Override
   public boolean canBePlacedOn(Level level, int layerID, int x, int y, GameObject newObject, boolean ignoreOtherLayers) {
      return false;
   }

   public boolean attachesToObject(Level level, int tileX, int tileY) {
      DecorationHolderInterface holder = this.getDecorationHolder(level, tileX, tileY);
      return holder == null ? false : holder.canPlaceDecoration(level, tileX, tileY);
   }

   @Override
   public String canPlace(Level level, int layerID, int x, int y, int rotation, boolean byPlayer, boolean ignoreOtherLayers) {
      MultiTile multiTile = this.getMultiTile(rotation);
      if (multiTile.isMaster) {
         String childError = multiTile.streamOtherObjects(x, y)
            .map(e -> e.value.canPlace(level, layerID, e.tileX, e.tileY, rotation, byPlayer, ignoreOtherLayers))
            .reduce(null, (prev, e) -> prev != null ? prev : e);
         if (childError != null) {
            return childError;
         }
      }

      if (layerID == ObjectLayerRegistry.FENCE_AND_TABLE_DECOR) {
         DecorationHolderInterface holder = this.getDecorationHolder(level, x, y);
         if (holder == null) {
            return "nodecorholder";
         }

         if (!holder.canPlaceDecoration(level, x, y)) {
            return "occupied";
         }

         Dimension maxSize = holder.getMaxDecorationSize(level, x, y);
         if (this.decorationWidth > maxSize.width || this.decorationHeight > maxSize.height) {
            return "decortoobig";
         }

         if (this.attachesToObject(level, x - 1, y)) {
            return null;
         }
      }

      return super.canPlace(level, layerID, x, y, rotation, byPlayer, ignoreOtherLayers);
   }

   @Override
   public boolean isValid(Level level, int layerID, int x, int y) {
      int rotation = level.getObjectRotation(layerID, x, y);
      if (layerID == ObjectLayerRegistry.FENCE_AND_TABLE_DECOR) {
         DecorationHolderInterface holder = this.getDecorationHolder(level, x, y);
         if (holder == null) {
            return false;
         }

         Dimension maxSize = holder.getMaxDecorationSize(level, x, y);
         if (this.decorationWidth > maxSize.width || this.decorationHeight > maxSize.height) {
            return false;
         }
      }

      return this.getMultiTile(rotation).streamOtherIDs(x, y).allMatch(e -> level.getObjectID(layerID, e.tileX, e.tileY) == e.value);
   }
}
