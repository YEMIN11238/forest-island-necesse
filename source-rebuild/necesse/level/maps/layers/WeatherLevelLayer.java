package necesse.level.maps.layers;

import java.awt.Color;
import java.awt.geom.Point2D.Double;
import java.awt.geom.Point2D.Float;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import necesse.engine.GameRandomNoise;
import necesse.engine.Settings;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.packet.PacketLevelLayerData;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.sound.SoundPlayer;
import necesse.engine.sound.SoundSettings;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.particle.Particle;
import necesse.entity.particle.ParticleOption;
import necesse.entity.trails.Trail;
import necesse.entity.trails.TrailVector;
import necesse.gfx.GameResources;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawables.LevelDrawUtils;
import necesse.gfx.drawables.LinesDrawOptionsList;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.level.maps.CollisionFilter;
import necesse.level.maps.Level;
import necesse.level.maps.biomes.Biome;

public class WeatherLevelLayer extends LevelLayer {
   public static final long rainFadeTime = 10000L;
   private boolean isRaining;
   private long rainTimer;
   private long rainSet;
   private final GameRandomNoise windNoise = new GameRandomNoise(0);
   private long lastWindWorldTime = Long.MIN_VALUE;
   private float windSpeedFull;
   private float windSpeedLimited;
   private double windOffsetX;
   private double windOffsetY;
   private double windDirX;
   private double windDirY;
   private Double windDirNormalized = new Double();
   private double windAngle;

   public WeatherLevelLayer(Level level) {
      super(level);
   }

   @Override
   public void init() {
   }

   @Override
   public void onLoadingComplete() {
      if (this.level.isServer()) {
         this.tickRainTimer();
      }

      this.rainSet = this.level.getWorldEntity().getWorldTime() - 20000L;
      this.windNoise.seed((int)this.level.getSeed());
   }

   @Override
   public void frameTick(TickManager tickManager) {
      super.frameTick(tickManager);
      this.checkWindUpdate();
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.level.isServer()) {
         this.tickRainTimer();
      }
   }

   @Override
   public void tickTileEffects(GameCamera camera, PlayerMob perspective, LevelDrawUtils.DrawArea drawArea) {
      if (Settings.windEffects) {
         for (int tileX = drawArea.startTileX; tileX < drawArea.endTileX; tileX++) {
            for (int tileY = drawArea.startTileY; tileY < drawArea.endTileY; tileY++) {
               Biome biome = this.level.getBiome(tileX, tileY);
               float modifier = biome.getWindModifier(this.level, tileX, tileY);
               if (modifier <= 0.0F) {
                  return;
               }

               float windSpeed = this.getWindSpeed();
               if (windSpeed > biome.getWindSpeedParticleLimit(this.level)) {
                  float windAmount = this.getWindAmount(tileX, tileY);
                  if (windAmount > biome.getWindAmountParticleLimit(this.level)) {
                     float buffer = 1.0F / (20.0F * biome.getWindParticleBufferModifier(this.level)) * windAmount * windSpeed;

                     while (buffer >= 1.0F || GameRandom.globalRandom.getChance(buffer)) {
                        buffer--;
                        this.spawnWindParticle(
                           tileX * 32 + GameRandom.globalRandom.getFloatBetween(0.0F, 32.0F),
                           tileY * 32 + GameRandom.globalRandom.getFloatBetween(0.0F, 32.0F),
                           modifier
                        );
                     }
                  }
               }
            }
         }
      }
   }

   public void spawnWindParticle(float x, float y, float modifier) {
      int tileX = GameMath.getTileCoordinate(x);
      int tileY = GameMath.getTileCoordinate(y);
      float speed = this.getWindSpeedFull() * modifier;
      Supplier<Float> moveGetter = () -> {
         Double dir = this.getWindDirNormalized();
         return new Float((float)dir.x * speed * 45.0F, (float)dir.y * speed * 45.0F);
      };
      Float initialMove = moveGetter.get();
      float randomMoveX = GameRandom.globalRandom.floatGaussian() * 2.0F;
      float randomMoveY = GameRandom.globalRandom.floatGaussian() * 2.0F;
      float moveX = randomMoveX + initialMove.x;
      float moveY = randomMoveY + initialMove.y;
      float height = 16.0F;
      float curveFreq = GameRandom.globalRandom.getFloatBetween(10.0F, 20.0F);
      float curveAmp = GameRandom.globalRandom.getFloatBetween(6.0F, 20.0F);
      ParticleOption.FrictionMover frictionMover = new ParticleOption.FrictionMover(moveX, moveY, 0.0F);
      ParticleOption.CollisionMover mover = new ParticleOption.CollisionMover(
         this.level, frictionMover, new CollisionFilter().mobCollision().addFilter(tp -> tp.object().object.isWall)
      );
      int timeToLive = GameRandom.globalRandom.getIntBetween(1000, 5000);
      int timeToFadeOut = GameRandom.globalRandom.getIntBetween(1000, 2000);
      int totalTime = timeToLive + timeToFadeOut;
      float trailSize = 20.0F;
      Trail trail = new Trail(new TrailVector(x, y, moveX, moveY, trailSize, height), this.level, new Color(1.0F, 1.0F, 1.0F, 0.0F), 200, 1000);
      trail.sprite = new GameSprite(GameResources.chains, 7, 0, 32);
      AtomicReference<SoundPlayer> windSound = new AtomicReference<>();
      ParticleOption particle = this.level
         .entityManager
         .addTopParticle(x, y, GameRandom.globalRandom.getOneOf(Particle.GType.COSMETIC, Particle.GType.IMPORTANT_COSMETIC))
         .color(new Color(255, 255, 255, 0))
         .height(height)
         .moves((pos, delta, lifeTime, timeAlive, lifePercent) -> {
            Float move = moveGetter.get();
            frictionMover.dx = randomMoveX + move.x;
            frictionMover.dy = randomMoveY + move.y;
            Float perpMoveDir = GameMath.getPerpendicularDir(GameMath.normalize(frictionMover.dx, frictionMover.dy));
            mover.tick(pos, delta, lifeTime, timeAlive, lifePercent);
            float sin = GameMath.sin(timeAlive / curveFreq);
            pos.x = pos.x + sin * perpMoveDir.x * curveAmp * delta / 250.0F;
            pos.y = pos.y + sin * perpMoveDir.y * curveAmp * delta / 250.0F;
         })
         .removeIf(() -> mover.stopped)
         .lifeTime(totalTime);
      particle.onMoveTick((delta, lifeTime, timeAlive, lifePercent) -> {
         Biome biome = this.level.getBiome(tileX, tileY);
         SoundPlayer sound = windSound.get();
         if (sound != null) {
            sound.refreshLooping(2.0F);
         } else {
            SoundSettings windSoundSettings = biome.getWindSound(this.level);
            if (windSoundSettings == null) {
               return;
            }

            windSoundSettings.volume(windSoundSettings.getVolume() * modifier);
            SoundPlayer player = SoundManager.playSound(windSoundSettings, SoundEffect.weather(particle), sp -> sp.fadeIn(2.0F));
            if (player != null) {
               windSound.set(player);
            }
         }

         Float particlePos = particle.getLevelPos();
         Float perpMoveDir = GameMath.getPerpendicularDir(GameMath.normalize(frictionMover.dx, frictionMover.dy));
         float cos = GameMath.sin(timeAlive / curveFreq);
         float dx = moveX + cos * perpMoveDir.x;
         float dy = moveY + cos * perpMoveDir.y;
         trail.addPoint(new TrailVector(particlePos.x, particlePos.y, dx, dy, trailSize * Math.abs(lifePercent - 1.0F), particle.getCurrentHeight()), 0);
         Color windColor = biome.getWindColor(this.level);
         float r = GameMath.getPercentageBetweenTwoNumbers(windColor.getRed(), 0.0F, 255.0F);
         float g = GameMath.getPercentageBetweenTwoNumbers(windColor.getGreen(), 0.0F, 255.0F);
         float b = GameMath.getPercentageBetweenTwoNumbers(windColor.getBlue(), 0.0F, 255.0F);
         trail.col = new Color(r, g, b, 1.0F);
      });
      if (!particle.isRemoved()) {
         this.level.entityManager.addTrail(trail);
      }
   }

   protected void tickRainTimer() {
      if (!this.level.canRain()) {
         this.isRaining = false;
      } else {
         if (this.rainTimer == 0L) {
            this.resetRainTimer();
         }

         long time = this.level.getWorldEntity().getWorldTime();
         boolean lastRaining = this.isRaining;
         int i = 0;

         while (true) {
            if (i >= 10) {
               int rainTime = this.level.getRainTimeInSeconds(this.level, GameRandom.globalRandom);
               int dryTime = this.level.getDryTimeInSeconds(this.level, GameRandom.globalRandom);
               int randomTime = GameRandom.globalRandom.nextInt(rainTime + dryTime);
               if (randomTime < rainTime) {
                  this.setRaining(true, true, false);
                  this.resetRainTimer(time - randomTime * 1000L);
               } else {
                  this.setRaining(false, true, false);
                  this.resetRainTimer(time - (randomTime - rainTime) * 1000L);
               }
               break;
            }

            if (this.rainTimer > time) {
               break;
            }

            this.setRaining(!this.isRaining, true, false);
            this.resetRainTimer(this.rainTimer);
            i++;
         }

         if (this.level.isServer() && this.isRaining != lastRaining) {
            this.level.getServer().network.sendToClientsAtEntireLevel(new PacketLevelLayerData(this), this.level);
         }
      }
   }

   public void resetRainTimer() {
      this.resetRainTimer(this.level.getWorldEntity().getWorldTime());
   }

   protected void resetRainTimer(long worldTime) {
      if (this.isRaining) {
         this.rainTimer = worldTime + this.level.getRainTimeInSeconds(this.level, GameRandom.globalRandom) * 1000L;
      } else {
         this.rainTimer = worldTime + this.level.getDryTimeInSeconds(this.level, GameRandom.globalRandom) * 1000L;
      }
   }

   public void setRaining(boolean isRaining) {
      this.setRaining(isRaining, true, true);
   }

   private void setRaining(boolean isRaining, boolean startTransition, boolean sendPacket) {
      boolean lastRaining = this.isRaining;
      this.isRaining = isRaining;
      this.rainSet = this.level.getWorldEntity().getWorldTime() - (startTransition ? 0L : 10000L);
      if (sendPacket && lastRaining != isRaining && this.level.isServer()) {
         this.level.getServer().network.sendToClientsAtEntireLevel(new PacketLevelLayerData(this), this.level);
      }
   }

   public boolean isRaining() {
      return this.isRaining;
   }

   public long getRemainingRainTime() {
      return this.rainTimer - this.level.getWorldEntity().getWorldTime();
   }

   public float getRainAlpha() {
      if (Settings.alwaysRain) {
         return 1.0F;
      }

      if (!this.level.canRain()) {
         return 0.0F;
      }

      long rainSetLast = this.level.getWorldEntity().getWorldTime() - this.rainSet;
      if (rainSetLast >= 0L && rainSetLast < 10000L) {
         float alpha = (float)rainSetLast / 10000.0F;
         if (!this.isRaining()) {
            alpha = Math.abs(alpha - 1.0F);
         }

         return alpha;
      } else {
         return this.isRaining() ? 1.0F : 0.0F;
      }
   }

   @Override
   public void writeLevelDataPacket(PacketWriter writer) {
      super.writeLevelDataPacket(writer);
      writer.putNextBoolean(this.isRaining);
   }

   @Override
   public void readLevelDataPacket(PacketReader reader) {
      super.readLevelDataPacket(reader);
      this.setRaining(reader.getNextBoolean(), true, false);
   }

   @Override
   public void addSaveData(SaveData save) {
      save.addBoolean("isRaining", this.isRaining);
      save.addLong("rainTimer", this.rainTimer);
   }

   @Override
   public void loadSaveData(LoadData save) {
      this.isRaining = save.getBoolean("isRaining", false, false);
      this.rainTimer = save.getLong("rainTimer", 0L, false);
   }

   private double getWindOffsetXCoord(long worldTime) {
      return (worldTime / 1000.0 + 64462.1792) / this.level.baseBiome.getWindProgressDivider(this.level);
   }

   private double getWindOffsetYCoord(long worldTime) {
      return (worldTime / 1000.0 + 15162.6329) / this.level.baseBiome.getWindProgressDivider(this.level);
   }

   protected void checkWindUpdate() {
      long worldTime = this.level.getWorldTime();
      if (this.lastWindWorldTime != worldTime) {
         synchronized (this.windNoise) {
            double windOffsetXCoord = this.getWindOffsetXCoord(worldTime);
            double windOffsetYCoord = this.getWindOffsetYCoord(worldTime);
            double offsetXPerlin = this.windNoise.perlin1(windOffsetXCoord);
            double offsetYPerlin = this.windNoise.perlin1(windOffsetYCoord);
            this.windOffsetX = offsetXPerlin * this.level.baseBiome.getWindProgressDivider(this.level);
            this.windOffsetY = offsetYPerlin * this.level.baseBiome.getWindProgressDivider(this.level);
            this.windDirX = this.windNoise.perlin1Derivative(windOffsetXCoord);
            this.windDirY = this.windNoise.perlin1Derivative(windOffsetYCoord);
            this.windDirNormalized = GameMath.normalize(this.windDirX, this.windDirY);
            this.windAngle = GameMath.getAngle(this.windDirNormalized);
            this.windSpeedFull = GameMath.preciseDistance(0.0F, 0.0F, (float)this.windDirX, (float)this.windDirY);
            this.windSpeedLimited = Math.min(this.windSpeedFull, 1.0F);
            this.lastWindWorldTime = worldTime;
         }
      }
   }

   public Double getWindDirFull() {
      return new Double(this.windDirX, this.windDirY);
   }

   public Double getWindDirNormalized() {
      return this.windDirNormalized;
   }

   public double getWindAngle() {
      return this.windAngle;
   }

   public float getWindSpeed() {
      return this.windSpeedLimited;
   }

   public float getWindSpeedFull() {
      return this.windSpeedFull;
   }

   public float getWindAmount(float tileX, float tileY) {
      synchronized (this.windNoise) {
         double perlin = this.windNoise.perlin2(tileX / 10.0 + this.windOffsetX / 2.0, tileY / 10.0 + this.windOffsetY / 2.0);
         if (perlin < 0.0) {
            perlin = 0.0;
         }

         if (perlin != 0.0) {
            double speed = this.getWindSpeed();
            double perlin2 = this.windNoise
               .perlin2((tileX + 2415823.1834) / 30.0 + this.windOffsetX / 1.5, (tileY + 7433923.5062) / 30.0 + this.windOffsetY / 1.5);
            perlin2 = Math.abs(perlin2);
            double limit = GameMath.limit(perlin * 2.0 * speed - perlin2 * speed, 0.0, 1.0);
            double cutOff = 0.2F;
            if (limit > cutOff) {
               limit = GameMath.map(limit, cutOff, 1.0, 0.0, 1.0);
            } else {
               limit = 0.0;
            }

            return (float)limit;
         } else {
            return 0.0F;
         }
      }
   }

   public DrawOptions getOffsetXDebugDrawOptions(int drawX, int drawY, int width, int height, long offset) {
      long worldTime = this.level.getWorldTime();
      double start = this.level.weatherLayer.getWindOffsetXCoord(worldTime - offset);
      double end = this.level.weatherLayer.getWindOffsetXCoord(worldTime + offset);
      double current = this.level.weatherLayer.getWindOffsetXCoord(worldTime);
      LinesDrawOptionsList graph = GameRandomNoise.get1DebugDraw(
         drawX, drawY, width, height, start, end, current, new GameRandomNoise.Debug1Draw(1.0F, 0.0F, 0.0F, 1.0F) {
            @Override
            public double get(double x) {
               return WeatherLevelLayer.this.windNoise.perlin1(x);
            }
         }, new GameRandomNoise.Debug1Draw(0.0F, 1.0F, 0.0F, 1.0F) {
            @Override
            public double get(double x) {
               return WeatherLevelLayer.this.windNoise.perlin1Derivative(x);
            }
         }
      );
      return () -> {
         graph.draw();
         this.drawLegend(drawX + width + 5, drawY, "X ");
      };
   }

   public DrawOptions getOffsetYDebugDrawOptions(int drawX, int drawY, int width, int height, long offset) {
      long worldTime = this.level.getWorldTime();
      double start = this.level.weatherLayer.getWindOffsetYCoord(worldTime - offset);
      double end = this.level.weatherLayer.getWindOffsetYCoord(worldTime + offset);
      double current = this.level.weatherLayer.getWindOffsetYCoord(worldTime);
      LinesDrawOptionsList graph = GameRandomNoise.get1DebugDraw(
         drawX, drawY, width, height, start, end, current, new GameRandomNoise.Debug1Draw(1.0F, 0.0F, 0.0F, 1.0F) {
            @Override
            public double get(double x) {
               return WeatherLevelLayer.this.windNoise.perlin1(x);
            }
         }, new GameRandomNoise.Debug1Draw(0.0F, 1.0F, 0.0F, 1.0F) {
            @Override
            public double get(double x) {
               return WeatherLevelLayer.this.windNoise.perlin1Derivative(x);
            }
         }
      );
      return () -> {
         graph.draw();
         this.drawLegend(drawX + width + 5, drawY, "Y ");
      };
   }

   private void drawLegend(int drawX, int drawY, String prefix) {
      FontOptions fontOptions = new FontOptions(16).outline();
      Renderer.initQuadDraw(16, 16).color(1.0F, 0.0F, 0.0F, 1.0F).draw(drawX, drawY);
      FontManager.bit.drawString(drawX + 20, drawY, prefix + "Offset", fontOptions);
      Renderer.initQuadDraw(16, 16).color(0.0F, 1.0F, 0.0F, 1.0F).draw(drawX, drawY + 20);
      FontManager.bit.drawString(drawX + 20, drawY + 20, prefix + "Derivative", fontOptions);
   }
}
