package necesse.level.gameTile;

import java.awt.Color;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.registries.DamageTypeRegistry;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.mobs.buffs.BuffModifiers;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.LevelTileTerrainDrawOptions;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;

public class LavaTile extends LiquidTile {
   public GameTextureSection texture;
   protected final GameRandom drawRandom;

   public LavaTile() {
      super(new Color(250, 132, 12), "lava");
      this.lightLevel = 100;
      this.lightHue = 0.0F;
      this.lightSat = 0.6F;
      this.drawRandom = new GameRandom();
   }

   @Override
   protected void loadTextures() {
      super.loadTextures();
      this.texture = tileTextures.addTexture(GameTexture.fromFile("tiles/lava"));
   }

   @Override
   public float getMinLiquidAlpha(Level level) {
      return 0.9F;
   }

   @Override
   public float getMaxLiquidAlpha(Level level) {
      return 0.9F;
   }

   @Override
   public LiquidTile.TextureIndexes getTextureIndexes(Level level, int tileX, int tileY, Biome biome) {
      return new LiquidTile.TextureIndexes(0, 0, 0, 0, 250, 250);
   }

   @Override
   public double getPathCost(Level level, int tileX, int tileY, Mob mob) {
      return !mob.isLavaImmune() ? 1000.0 : super.getPathCost(level, tileX, tileY, mob);
   }

   @Override
   public float getItemSinkingRate(float currentSinking) {
      return TickManager.getTickDelta(60.0F);
   }

   @Override
   public float getItemMaxSinking() {
      return 1.0F;
   }

   @Override
   public void tick(Mob mob, Level level, int x, int y) {
      if (mob.canLevelInteract()
         && !mob.isFlying()
         && !mob.isWaterWalking()
         && level.inLiquid(mob.getX(), mob.getY())
         && level.isServer()
         && mob.canTakeDamage()
         && !mob.isLavaImmune()
         && !mob.isOnGenericCooldown("lavadamage")) {
         int maxHealth = mob.getMaxHealth();
         float damage = Math.max((float)Math.pow(maxHealth, 0.5) + maxHealth / 20.0F, 20.0F);
         damage *= mob.buffManager.getModifier(BuffModifiers.FIRE_DAMAGE);
         if (damage != 0.0F) {
            mob.isServerHit(new GameDamage(DamageTypeRegistry.TRUE, damage), 0.0F, 0.0F, 0.0F, LAVA_ATTACKER);
         } else if (mob.isPlayer) {
            PlayerMob player = (PlayerMob)mob;
            if (player.isServerClient()) {
               ServerClient client = player.getServerClient();
               if (client.achievementsLoaded()) {
                  client.achievements().HOT_TUB.markCompleted(client);
               }
            }
         }

         mob.startGenericCooldown("lavadamage", 500L);
         mob.addBuff(new ActiveBuff(BuffRegistry.Debuffs.ON_FIRE, mob, 10.0F, null), true);
      }
   }

   @Override
   public Color getLiquidColor(Level level, int tileX, int tileY, Biome biome) {
      return this.getLiquidColor(1);
   }

   @Override
   public void tickEffect(Level level, int x, int y) {
      if (GameRandom.globalRandom.getEveryXthChance(200) && level.getObjectID(x, y) == 0) {
         int spriteRes = 12;
         Color particleColor = this.getLiquidColor(level, x, y);
         level.entityManager
            .addParticle(
               ParticleOption.base(x * 32 + GameRandom.globalRandom.nextInt(32 - spriteRes), y * 32 + GameRandom.globalRandom.nextInt(32 - spriteRes)),
               Particle.GType.COSMETIC
            )
            .lifeTime(1000)
            .sprite((options, lifeTime, timeAlive, lifePercent) -> {
               int frames = GameResources.liquidBlobParticle.getWidth() / spriteRes;
               return options.add(GameResources.liquidBlobParticle.sprite(Math.min((int)(lifePercent * frames), frames - 1), 0, spriteRes));
            })
            .color(particleColor);
      }
   }

   @Override
   protected void addLiquidTopDrawables(
      LevelTileTerrainDrawOptions list, List<LevelSortedDrawable> sortedList, Level level, int tileX, int tileY, GameCamera camera, TickManager tickManager
   ) {
   }

   @Override
   public int getLiquidBobbing(Level level, int tileX, int tileY) {
      return super.getLiquidBobbing(level, tileX, tileY) / 2;
   }
}
