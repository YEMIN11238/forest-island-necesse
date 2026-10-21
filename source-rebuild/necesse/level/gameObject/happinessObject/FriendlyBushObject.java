package necesse.level.gameObject.happinessObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Double;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.packet.PacketHitObject;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundSettingsRegistry;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.window.WindowManager;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.mobs.Attacker;
import necesse.entity.mobs.GameDamage;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsPositionMod;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTexture.GameTextureSection;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;
import necesse.inventory.item.toolItem.ToolType;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.HappinessObject;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

public class FriendlyBushObject extends GameObject implements HappinessObject {
   private GameTexture texture;
   private final String leavesTextureName;
   private GameTextureSection leavesTextureSection;
   public int weaveTime = 250;
   public float weaveAmount = 0.08F;
   protected String textureName = "leafpile";
   protected GameTexture[][] textures;
   protected final GameRandom drawRandom;

   public FriendlyBushObject(String leavesTextureName) {
      this.mapColor = new Color(51, 155, 43);
      this.leavesTextureName = leavesTextureName;
      this.rarity = Item.Rarity.EPIC;
      this.displayMapTooltip = true;
      this.drawDamage = false;
      this.objectHealth = 1;
      this.toolType = ToolType.ALL;
      this.drawRandom = new GameRandom();
      this.attackThrough = true;
      this.replaceRotations = false;
      this.setItemCategory("objects", "landscaping", "plants");
      this.setCraftingCategory("objects", "landscaping", "plants");
      this.hoverHitbox = new Rectangle(0, -16, 32, 48);
      this.roomProperties.add("plant");
   }

   @Override
   public void tick(Mob mob, Level level, int x, int y) {
      super.tick(mob, level, x, y);
      if (Settings.wavyGrass && mob.getFlyingHeight() < 10 && (mob.dx != 0.0F || mob.dy != 0.0F)) {
         level.makeGrassWeave(x, y, this.weaveTime, false);
      }
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.getStringID());

      try {
         GameTexture leavesTexture = GameTexture.fromFileRaw("particles/" + this.leavesTextureName);
         this.leavesTextureSection = GameResources.particlesTextureGenerator.addTexture(leavesTexture);
      } catch (FileNotFoundException e) {
         this.leavesTextureSection = null;
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
      float alpha = 1.0F;
      if (perspective != null && !Settings.hideUI && !Settings.hideCursor) {
         Rectangle alphaRec = new Rectangle(tileX * 32 - 12, tileY * 32 - 24, 56, 32);
         if (perspective.getCollision().intersects(alphaRec)) {
            alpha = 0.5F;
         } else if (alphaRec.contains(camera.getX() + WindowManager.getWindow().mousePos().sceneX, camera.getY() + WindowManager.getWindow().mousePos().sceneY)
            )
          {
            alpha = 0.5F;
         }
      }

      Consumer<TextureDrawOptionsPositionMod> waveChange = GameResources.waveShader
         .setupGrassWaveMod(level, tileX, tileY, 400L, this.weaveAmount, 2, this.drawRandom, getTileSeed(tileX, tileY, 0), false, 2.0F);
      final TextureDrawOptions options = this.texture.initDraw().sprite(0, 0, 32).alpha(alpha).light(light).addPositionMod(waveChange).pos(drawX, drawY - 8);
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

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      GameLight light = level.getLightLevel(tileX, tileY);
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY);
      this.texture.initDraw().sprite(0, 0, 32).alpha(alpha).light(light).draw(drawX, drawY - 8);
   }

   @Override
   public void attackThrough(Level level, int x, int y, GameDamage damage) {
      super.attackThrough(level, x, y, damage);
      level.makeGrassWeave(x, y, 300, false);
      if (damage.damage > 0.0F) {
         this.spawnDestroyedParticles(level, x, y);
      }
   }

   @Override
   public void attackThrough(Level level, int x, int y, GameDamage damage, Attacker attacker) {
      level.getServer().network.sendToClientsWithTile(new PacketHitObject(level, x, y, this, damage), level, x, y);
   }

   @Override
   public void spawnDestroyedParticles(Level level, int tileX, int tileY) {
      GameRandom random = GameRandom.globalRandom;
      Double windDir = level.weatherLayer.getWindDirNormalized();
      float windSpeed = level.weatherLayer.getWindAmount(tileX * 32, tileY * 32) * 3.0F * level.weatherLayer.getWindSpeed();
      int particleCount = random.getIntBetween(2, 3);
      ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC);
      boolean alternate = GameRandom.globalRandom.nextBoolean();

      for (int i = 0; i < particleCount; i++) {
         float posX = tileX * 32 + 16
            + (alternate ? GameRandom.globalRandom.getFloatBetween(-1.0F, 0.0F) : GameRandom.globalRandom.getFloatBetween(0.0F, 1.0F)) * 16.0F;
         alternate = !alternate;
         float posY = tileY * 32 + 16;
         float startHeight = GameRandom.globalRandom.getFloatBetween(0.0F, 32.0F);
         float startHeightSpeed = GameRandom.globalRandom.getFloatBetween(0.0F, 60.0F);
         float endHeight = GameRandom.globalRandom.getFloatBetween(-10.0F, -5.0F);
         float gravity = GameRandom.globalRandom.getFloatBetween(8.0F, 20.0F);
         boolean mirror = GameRandom.globalRandom.nextBoolean();
         float rotation = GameRandom.globalRandom.getFloatBetween(-100.0F, 100.0F);
         float moveX = GameRandom.globalRandom.floatGaussian() * 5.0F + (float)windDir.x * windSpeed * 10.0F;
         float moveY = GameRandom.globalRandom.floatGaussian() * 2.0F + (float)windDir.y * windSpeed * 10.0F;
         ParticleOption.FrictionMover frictionMover = new ParticleOption.FrictionMover(moveX, moveY, 0.0F);
         ParticleOption.CollisionMover mover = new ParticleOption.CollisionMover(
            level, frictionMover, new CollisionFilter().mobCollision().addFilter(tp -> tp.object().object.isWall)
         );
         int timeToLive = GameRandom.globalRandom.getIntBetween(3000, 8000);
         int timeToFadeOut = GameRandom.globalRandom.getIntBetween(1000, 2000);
         int totalTime = timeToLive + timeToFadeOut;
         ParticleOption.HeightMover heightMover = new ParticleOption.HeightMover(startHeight, startHeightSpeed, gravity, 2.0F, endHeight, 0.0F);
         AtomicReference<Float> floatingTime = new AtomicReference<>(0.0F);
         ParticleOption particle = level.entityManager
            .addParticle(posX, posY, typeSwitcher.next())
            .sprite(
               this.leavesTextureSection == null
                  ? GameResources.puffParticles.sprite(random.nextInt(5), 0, 12)
                  : this.leavesTextureSection.sprite(random.nextInt(4), 0, 20)
            )
            .fadesAlphaTime(0, timeToFadeOut)
            .sizeFadesInAndOut(15, 20, 100, 0)
            .height(heightMover)
            .onMoveTick((delta, lifeTime, timeAlive, lifePercent) -> {
               if (heightMover.currentHeight > endHeight) {
                  floatingTime.set(floatingTime.get() + delta);
               }
            })
            .modify((options, lifeTime, timeAlive, lifePercent) -> {
               float angle = GameMath.sin(floatingTime.get() / 5.0F) * rotation;
               options.rotate(angle, 10, -4);
            })
            .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
               if (heightMover.currentHeight > endHeight) {
                  mover.tick(pos, delta, lifeTime, timeAlive, lifePercent);
               }
            })
            .modify((options, lifeTime, timeAlive, lifePercent) -> options.mirror(mirror, false))
            .lifeTime(totalTime);
         if (!particle.isRemoved()) {
            SoundManager.playSound(SoundSettingsRegistry.leavesBreakAction, SoundEffect.effect(posX, posY));
         }
      }
   }

   @Override
   public void playDamageSound(Level level, int x, int y, boolean damageDone) {
      SoundManager.playSound(GameResources.leafpiledestroy, SoundEffect.effect(x * 32 + 16, y * 32 + 16));
   }

   @Override
   public ListGameTooltips getItemTooltips(InventoryItem item, PlayerMob perspective) {
      ListGameTooltips tooltips = super.getItemTooltips(item, perspective);
      tooltips.add(this.getHappinessObjectTooltip());
      return tooltips;
   }
}
