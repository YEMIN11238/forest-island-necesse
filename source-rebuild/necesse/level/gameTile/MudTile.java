package necesse.level.gameTile;

import java.awt.Color;
import java.awt.Point;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;

public class MudTile extends TerrainSplatterTile {
   private final GameRandom drawRandom;

   public MudTile() {
      super(false, "mud");
      this.mapColor = new Color(121, 73, 42);
      this.canBeMined = true;
      this.drawRandom = new GameRandom();
      this.isOrganic = true;
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
      return 0;
   }

   @Override
   public ModifierValue<Float> getSlowModifier(Mob mob) {
      return mob.isFlying() ? super.getSpeedModifier(mob) : new ModifierValue<>(BuffModifiers.SLOW, 0.25F);
   }
}
