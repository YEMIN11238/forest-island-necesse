package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.objectEntity.ObjectEntity;
import necesse.entity.objectEntity.TrainingDummyObjectEntity;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsPositionMod;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.regionSystem.RegionType;

public class TrainingDummyObject extends GameObject {
   public static final int weaveTime = 200;
   public GameTexture base;
   public GameTexture body;
   protected GameRandom drawRandom;

   public TrainingDummyObject() {
      super(new Rectangle(6, 18, 20, 10));
      this.mapColor = new Color(129, 79, 30);
      this.displayMapTooltip = true;
      this.objectHealth = 50;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.drawRandom = new GameRandom();
      this.regionType = RegionType.SUMMON_IGNORED;
      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "misc");
   }

   @Override
   public List<Rectangle> getProjectileCollisions(Level level, int x, int y, int rotation) {
      return Collections.emptyList();
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.base = GameTexture.fromFile("objects/trainingdummy_base");
      this.body = GameTexture.fromFile("objects/trainingdummy_body");
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
      int drawX = camera.getTileDrawX(tileX) - 16;
      int drawY = camera.getTileDrawY(tileY) - 32;
      int rotation = level.getObjectRotation(tileX, tileY) % 4;
      Consumer<TextureDrawOptionsPositionMod> waveChange = GameResources.waveShader
         .setupGrassWaveMod(level, tileX, tileY, 200L, 0.1F, 2, this.drawRandom, level.grassWeaveStart(tileX, tileY) * GameRandom.prime(5), false, 0.0F);
      final TextureDrawOptions options = this.body
         .initDraw()
         .sprite(rotation, 0, 64)
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .addPositionMod(waveChange)
         .pos(drawX, drawY - 10);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 19;
         }

         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
      TextureDrawOptions tileOptions = this.base
         .initDraw()
         .sprite(rotation, 0, 64)
         .addObjectDamageOverlay(this, level, tileX, tileY)
         .light(light)
         .pos(drawX, drawY);
      tileList.add(tm -> tileOptions.draw());
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX) - 16;
      int drawY = camera.getTileDrawY(tileY) - 32;
      this.base.initDraw().sprite(rotation, 0, 64).alpha(alpha).draw(drawX, drawY);
      this.body.initDraw().sprite(rotation, 0, 64).alpha(alpha).draw(drawX, drawY - 10);
   }

   @Override
   public ObjectEntity getNewObjectEntity(Level level, int x, int y) {
      return new TrainingDummyObjectEntity(level, x, y, false);
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(Localization.translate("itemtooltip", "trainingdummytip"));
      return tooltips;
   }
}
