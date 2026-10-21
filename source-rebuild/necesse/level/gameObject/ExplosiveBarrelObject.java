package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.util.GameMath;
import necesse.entity.levelEvent.explosionEvent.DynamiteExplosionEvent;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.maps.Level;
import necesse.level.maps.levelBuffManager.LevelModifiers;
import necesse.level.maps.light.GameLight;

public class ExplosiveBarrelObject extends GameObject {
   public GameTexture texture;

   public ExplosiveBarrelObject() {
      super(new Rectangle(5, 12, 22, 12));
      this.drawDamage = false;
      this.objectHealth = 1;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.attackThrough = true;
      this.mapColor = new Color(179, 44, 30);
      this.displayMapTooltip = true;
      this.setItemCategory("objects", "misc");
      this.setCraftingCategory("objects", "misc");
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.getStringID());
   }

   @Override
   public void attackThrough(Level level, int x, int y, GameDamage damage, Attacker attacker) {
      super.attackThrough(level, x, y, damage, attacker);
      if (damage.damage > 0.0F) {
         this.explode(level, x, y);
      }
   }

   @Override
   public void doExplosionDamage(Level level, int layerID, int tileX, int tileY, int damage, float toolTier, Attacker attacker, ServerClient client) {
      if (damage > 0) {
         this.explode(level, tileX, tileY);
      }
   }

   public void explode(Level level, int tileX, int tileY) {
      level.setObject(tileX, tileY, 0);
      if (!level.isClient()) {
         if (level.isServer()) {
            level.sendObjectChangePacket(level.getServer(), tileX, tileY, 0);
         }

         GameDamage explosiveDamage = new GameDamage(250.0F).modDamage(level.buffManager.getModifier(LevelModifiers.ENEMY_DAMAGE));
         int levelX = GameMath.getLevelCoordinate(tileX) + 16;
         int levelY = GameMath.getLevelCoordinate(tileY) + 16;
         DynamiteExplosionEvent e = new DynamiteExplosionEvent(levelX, levelY, 230, explosiveDamage, true, false, 10.0F, null);
         level.entityManager.events.add(e);
      }
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
      final TextureDrawOptions options = this.texture.initDraw().sprite(0, 0, 32, 44).light(light).pos(drawX, drawY - this.texture.getHeight() / 2 + 12);
      list.add(new LevelSortedDrawable(this, tileX, tileY) {
         @Override
         public int getSortY() {
            return 16;
         }

         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
   }
}
