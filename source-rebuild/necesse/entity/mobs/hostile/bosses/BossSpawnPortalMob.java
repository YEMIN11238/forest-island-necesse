package necesse.entity.mobs.hostile.bosses;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.Localization;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.client.Client;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.engine.util.GameUtils;
import necesse.entity.levelEvent.IncursionLevelEvent;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.MobDrawable;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.gfx.GameColor;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.gfx.gameTooltips.StringTooltips;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.mapData.ClientDiscoveredMap;

public class BossSpawnPortalMob extends Mob {
   protected Color color1 = new Color(150, 13, 38);
   protected Color color2 = new Color(255, 3, 62);
   protected Color color3 = new Color(255, 3, 49);
   protected Color color4 = new Color(244, 152, 152);
   protected Color color5 = new Color(253, 236, 236);
   public int remainingAttemptsTip = 0;

   public BossSpawnPortalMob() {
      super(100);
      this.setFriction(1.0F);
      this.isSummoned = true;
      this.shouldSave = true;
      this.isStatic = true;
      this.collision = new Rectangle(-10, -7, 20, 14);
      this.hitBox = new Rectangle(-14, -12, 28, 24);
      this.selectBox = new Rectangle(-18, -45, 36, 48);
      this.setKnockbackModifier(0.0F);
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addInt("remainingAttemptsTip", this.remainingAttemptsTip);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.remainingAttemptsTip = save.getInt("remainingAttemptsTip", this.remainingAttemptsTip, false);
   }

   @Override
   public void setupSpawnPacket(PacketWriter writer) {
      super.setupSpawnPacket(writer);
      writer.putNextInt(this.remainingAttemptsTip);
   }

   @Override
   public void applySpawnPacket(PacketReader reader) {
      super.applySpawnPacket(reader);
      this.remainingAttemptsTip = reader.getNextInt();
   }

   @Override
   public void init() {
      super.init();
      this.setDir(0);
   }

   @Override
   public boolean canPushMob(Mob other) {
      return false;
   }

   @Override
   public boolean canBePushed(Mob other) {
      return false;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      this.getLevel().lightManager.refreshParticleLightFloat(this.x, this.y, 0.0F, 0.7F);

      for (int i = 0; i < 2; i++) {
         int angle = GameRandom.globalRandom.nextInt(360);
         Float dir = GameMath.getAngleDir(angle);
         float range = GameRandom.globalRandom.getFloatBetween(25.0F, 40.0F);
         float startX = this.x + dir.x * range;
         float startY = this.y + 4.0F;
         float endHeight = 29.0F;
         float startHeight = endHeight + dir.y * range;
         int lifeTime = GameRandom.globalRandom.getIntBetween(200, 500);
         float speed = dir.x * range * 250.0F / lifeTime;
         Color color = GameRandom.globalRandom.getOneOf(this.color1, this.color2, this.color3);
         this.getLevel()
            .entityManager
            .addParticle(startX, startY, Particle.GType.IMPORTANT_COSMETIC)
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
            .sizeFades(10, 16)
            .rotates()
            .heightMoves(startHeight, endHeight)
            .movesConstant(-speed, 0.0F)
            .color(color)
            .fadesAlphaTime(100, 50)
            .lifeTime(lifeTime);
      }

      for (int i = 0; i < 2; i++) {
         int lifeTime = GameRandom.globalRandom.getIntBetween(500, 1000);
         float lifePerc = lifeTime / 1000.0F;
         float startHeight = 5 + GameRandom.globalRandom.nextInt(5);
         float height = startHeight + GameRandom.globalRandom.getIntBetween(20, 50) * lifePerc;
         Color color = GameRandom.globalRandom.getOneOf(this.color1, this.color2, this.color3);
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
               this.y + GameRandom.globalRandom.getFloatBetween(-5.0F, 5.0F),
               Particle.GType.COSMETIC
            )
            .sprite(GameResources.puffParticles.sprite(GameRandom.globalRandom.nextInt(5), 0, 12))
            .sizeFadesInAndOut(6, 12, 100, lifeTime - 100)
            .movesFriction(GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F), GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F), 0.7F)
            .heightMoves(startHeight, height)
            .color(color)
            .lifeTime(lifeTime);
      }
   }

   @Override
   public void interact(PlayerMob player) {
      super.interact(player);
      if (player != null && player.isServerClient()) {
         Level level = this.getLevel();
         if (level instanceof IncursionLevel) {
            IncursionLevelEvent event = ((IncursionLevel)level).getIncursionLevelEvent();
            if (event != null) {
               event.onBossSpawnTriggered(this);
            }
         } else {
            this.remove(0.0F, 0.0F, null, true);
         }
      }
   }

   @Override
   public boolean canInteract(Mob mob) {
      return true;
   }

   @Override
   protected void addHoverTooltips(ListGameTooltips tooltips, boolean debug) {
      super.addHoverTooltips(tooltips, debug);
      if (this.remainingAttemptsTip > 0) {
         GameColor color = GameColor.NO_COLOR;
         if (this.remainingAttemptsTip <= 2) {
            color = GameColor.RED;
         } else if (this.remainingAttemptsTip <= 4) {
            color = GameColor.ITEM_QUEST;
         }

         tooltips.add(new StringTooltips(Localization.translate("misc", "bossattempts", "number", this.remainingAttemptsTip), color));
      }
   }

   @Override
   protected String getInteractTip(PlayerMob perspective, boolean debug) {
      return Localization.translate("controls", "usetip");
   }

   @Override
   protected SoundSettings getHitDeathSound() {
      return new SoundSettings(GameResources.fadedeath2);
   }

   @Override
   public void spawnDeathParticles(float knockbackX, float knockbackY) {
      for (int i = 0; i < 30; i++) {
         int lifeTime = GameRandom.globalRandom.getIntBetween(500, 5000);
         float lifePerc = lifeTime / 5000.0F;
         float startHeight = 20.0F;
         float height = startHeight + GameRandom.globalRandom.getIntBetween(70, 150) * lifePerc;
         this.getLevel()
            .entityManager
            .addParticle(
               this.x + GameRandom.globalRandom.getFloatBetween(-10.0F, 10.0F),
               this.y + GameRandom.globalRandom.getFloatBetween(-5.0F, 5.0F),
               Particle.GType.IMPORTANT_COSMETIC
            )
            .sizeFades(8, 12)
            .movesFriction(GameRandom.globalRandom.getFloatBetween(-40.0F, 40.0F), GameRandom.globalRandom.getFloatBetween(-20.0F, 20.0F), 0.5F)
            .heightMoves(startHeight, height)
            .colorRandom(GameRandom.globalRandom.getOneOf(this.color1, this.color2, this.color3), 0.1F, 0.2F, 0.2F)
            .lifeTime(lifeTime);
      }
   }

   @Override
   public void addDrawables(
      List<MobDrawable> list,
      OrderableDrawables tileList,
      OrderableDrawables topList,
      Level level,
      int x,
      int y,
      TickManager tickManager,
      GameCamera camera,
      PlayerMob perspective
   ) {
      super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
      GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
      int drawX = camera.getDrawX(x) - 1;
      int drawY = camera.getDrawY(y) - 25;
      int offset = (int)(this.getTime() % 1600L) / 200;
      if (offset > 4) {
         offset = 4 - offset % 4;
      }

      int maxSize = 50;
      final SharedTextureDrawOptions options = new SharedTextureDrawOptions(MobRegistry.Textures.bossPortal);
      this.addSphere(drawX, drawY + offset, 0L, 1000, maxSize * 0.68F, maxSize, this.color1, light, options);
      this.addSphere(drawX, drawY + offset, 0L, 900, maxSize * 0.68F, maxSize, this.color1, light, options);
      this.addSphere(drawX, drawY + offset, 0L, 800, maxSize * 0.64F, maxSize * 0.88F, this.color2, light, options);
      this.addSphere(drawX, drawY + offset, 0L, 760, maxSize * 0.64F, maxSize * 0.88F, this.color2, light, options);
      this.addSphere(drawX, drawY + offset, 0L, 700, maxSize * 0.64F, maxSize * 0.88F, this.color2, light, options);
      this.addSphere(drawX, drawY + offset, 0L, 740, maxSize * 0.48F, maxSize * 0.68F, this.color3, light, options);
      this.addSphere(drawX, drawY + offset, 0L, 680, maxSize * 0.48F, maxSize * 0.68F, this.color3, light, options);
      this.addSphere(drawX, drawY + offset, 0L, 650, maxSize * 0.48F, maxSize * 0.68F, this.color3, light, options);
      this.addSphere(drawX, drawY + offset, 0L, 630, maxSize * 0.28F, maxSize * 0.52F, this.color4, light, options);
      this.addSphere(drawX, drawY + offset, 0L, 580, maxSize * 0.28F, maxSize * 0.52F, this.color4, light, options);
      this.addSphere(drawX, drawY + offset, 0L, 550, maxSize * 0.28F, maxSize * 0.52F, this.color4, light, options);
      this.addSphere(drawX, drawY + offset, 0L, 540, maxSize * 0.16F, maxSize * 0.36F, this.color5, light, options);
      this.addSphere(drawX, drawY + offset, 0L, 520, maxSize * 0.16F, maxSize * 0.36F, this.color5, light, options);
      this.addSphere(drawX, drawY + offset, 0L, 490, maxSize * 0.16F, maxSize * 0.36F, this.color5, light, options);
      list.add(new MobDrawable() {
         @Override
         public void draw(TickManager tickManager) {
            options.draw();
         }
      });
      this.addShadowDrawables(tileList, level, x, y, light, camera);
   }

   protected void addSphere(
      int drawX, int drawY, long timeOffset, int animTime, float minSize, float maxSize, Color color, GameLight light, SharedTextureDrawOptions options
   ) {
      float animFloat = GameUtils.getAnimFloat(this.getLocalTime() + timeOffset, animTime);
      float sizeDelta = maxSize - minSize;
      int size = (int)(minSize + sizeDelta * animFloat);
      options.addFull().color(color).alpha(0.5F).light(light).size(size).posMiddle(drawX, drawY, true);
   }

   @Override
   public boolean shouldDrawOnMap() {
      return true;
   }

   @Override
   public boolean isVisibleOnMap(Client client, ClientDiscoveredMap map) {
      return true;
   }

   @Override
   public void drawOnMap(TickManager tickManager, Client client, int x, int y, double tileScale, Rectangle drawBounds, boolean isMinimap) {
      GameLight light = this.getLevel().lightManager.newLight(150.0F);
      int drawX = x;
      int drawY = y - 10;
      int maxSize = 32;
      SharedTextureDrawOptions options = new SharedTextureDrawOptions(MobRegistry.Textures.bossPortal);
      this.addSphere(drawX, drawY, 0L, 1000, maxSize * 0.68F, maxSize, this.color1, light, options);
      this.addSphere(drawX, drawY, 0L, 900, maxSize * 0.68F, maxSize, this.color1, light, options);
      this.addSphere(drawX, drawY, 0L, 800, maxSize * 0.64F, maxSize * 0.88F, this.color2, light, options);
      this.addSphere(drawX, drawY, 0L, 760, maxSize * 0.64F, maxSize * 0.88F, this.color2, light, options);
      this.addSphere(drawX, drawY, 0L, 700, maxSize * 0.64F, maxSize * 0.88F, this.color2, light, options);
      this.addSphere(drawX, drawY, 0L, 740, maxSize * 0.48F, maxSize * 0.68F, this.color3, light, options);
      this.addSphere(drawX, drawY, 0L, 680, maxSize * 0.48F, maxSize * 0.68F, this.color3, light, options);
      this.addSphere(drawX, drawY, 0L, 650, maxSize * 0.48F, maxSize * 0.68F, this.color3, light, options);
      this.addSphere(drawX, drawY, 0L, 630, maxSize * 0.28F, maxSize * 0.52F, this.color4, light, options);
      this.addSphere(drawX, drawY, 0L, 580, maxSize * 0.28F, maxSize * 0.52F, this.color4, light, options);
      this.addSphere(drawX, drawY, 0L, 550, maxSize * 0.28F, maxSize * 0.52F, this.color4, light, options);
      this.addSphere(drawX, drawY, 0L, 540, maxSize * 0.16F, maxSize * 0.36F, this.color5, light, options);
      this.addSphere(drawX, drawY, 0L, 520, maxSize * 0.16F, maxSize * 0.36F, this.color5, light, options);
      this.addSphere(drawX, drawY, 0L, 490, maxSize * 0.16F, maxSize * 0.36F, this.color5, light, options);
      options.draw();
   }

   @Override
   public Rectangle drawOnMapBox(double tileScale, boolean isMinimap) {
      return new Rectangle(-12, -24, 24, 28);
   }

   @Override
   public GameTooltips getMapTooltips() {
      return new StringTooltips(this.getDisplayName());
   }
}
