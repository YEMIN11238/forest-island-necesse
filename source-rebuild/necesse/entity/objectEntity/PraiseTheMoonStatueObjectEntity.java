package necesse.entity.objectEntity;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;
import necesse.engine.GameTileRange;
import necesse.engine.localization.Localization;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.BuffRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameMath;
import necesse.engine.util.GameRandom;
import necesse.entity.ParticleTypeSwitcher;
import necesse.entity.events.EmptyEntityEvent;
import necesse.entity.events.IntEntityEvent;
import necesse.entity.events.LongEntityEvent;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.buffs.ActiveBuff;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.floatText.UniqueFloatText;
import necesse.level.maps.multiTile.MultiTile;

public class PraiseTheMoonStatueObjectEntity extends ObjectEntity {
   public static GameTileRange particleTileRange = new GameTileRange(5, true, new Rectangle(2, 1));
   public static GameTileRange lightRange = new GameTileRange(2, new Rectangle(2, 1));
   private static final int millisecondsShowingFullLight = 20000;
   private static final int millisecondsUntilBuff = 10000;
   private static final int millisecondsFadingOut = 2500;
   public long lastInteractionTime;
   protected HashSet<Long> interactedAuths = new HashSet<>();
   public final LongEntityEvent activatedEvent = this.registerEvent(new LongEntityEvent() {
      @Override
      protected void run(long value) {
         PraiseTheMoonStatueObjectEntity.this.lastInteractionTime = value;
         PraiseTheMoonStatueObjectEntity.this.showActivatedText();
      }
   });
   public final EmptyEntityEvent activatedFailedEvent = this.registerEvent(new EmptyEntityEvent() {
      @Override
      protected void run() {
         PraiseTheMoonStatueObjectEntity.this.showNothingHappensText();
      }
   });
   public final IntEntityEvent activatePlayerSlotEvent = this.registerEvent(
      new IntEntityEvent() {
         @Override
         protected void run(int value) {
            if (PraiseTheMoonStatueObjectEntity.this.isClient()) {
               PlayerMob player = PraiseTheMoonStatueObjectEntity.this.getClient().getPlayer(value);
               if (player != null && player.isSamePlace(PraiseTheMoonStatueObjectEntity.this.getClient().getLevel())) {
                  int particles = 25;
                  ParticleTypeSwitcher typeSwitcher = new ParticleTypeSwitcher(
                     Particle.GType.CRITICAL, Particle.GType.IMPORTANT_COSMETIC, Particle.GType.COSMETIC
                  );

                  for (int i = 0; i < particles; i++) {
                     AtomicReference<Float> currentAngle = new AtomicReference<>(GameRandom.globalRandom.nextFloat() * 360.0F);
                     int startHeight = GameRandom.globalRandom.getIntBetween(20, 60);
                     int endHeight = GameRandom.globalRandom.getIntBetween(0, 10);
                     int startDistance = GameRandom.globalRandom.getIntBetween(20, 25);
                     int endDistance = GameRandom.globalRandom.getIntBetween(10, 15);
                     float circleSpeed = GameRandom.globalRandom.getFloatBetween(50.0F, 100.0F);
                     int lifeTime = GameRandom.globalRandom.getIntBetween(1000, 2000);
                     player.getLevel()
                        .entityManager
                        .addParticle(
                           player.x + GameMath.sin(currentAngle.get()) * startDistance,
                           player.y + GameMath.cos(currentAngle.get()) * startDistance * 0.75F,
                           typeSwitcher.next()
                        )
                        .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 22))
                        .color(new Color(200, 200, 255))
                        .givesLight(240.0F, 0.35F)
                        .sizeFadesInAndOut(16, 24, lifeTime / 4, lifeTime / 4)
                        .height((delta, lifeTime1, timeAlive, lifePercent) -> GameMath.lerp(lifePercent, startHeight, endHeight))
                        .moves((pos, delta, lifeTime1, timeAlive, lifePercent) -> {
                           float angle = currentAngle.accumulateAndGet(delta * circleSpeed / 250.0F, Float::sum);
                           float currentDistance = GameMath.lerp(lifePercent, startDistance, endDistance);
                           pos.x = player.x + GameMath.sin(angle) * currentDistance;
                           pos.y = player.y + GameMath.cos(angle) * currentDistance * 0.75F;
                        })
                        .lifeTime(lifeTime)
                        .sizeFades(16, 24);
                  }

                  SoundManager.playSound(GameResources.crystalGlyph, SoundEffect.effect(player));
               }
            }
         }
      }
   );

   public PraiseTheMoonStatueObjectEntity(Level level, int tileX, int tileY) {
      super(level, "praisethemoonstatue", tileX, tileY);
   }

   @Override
   public void setupContentPacket(PacketWriter writer) {
      super.setupContentPacket(writer);
      writer.putNextLong(this.lastInteractionTime);
   }

   @Override
   public void applyContentPacket(PacketReader reader) {
      super.applyContentPacket(reader);
      this.lastInteractionTime = reader.getNextLong();
   }

   public void activateStatue(ServerClient client) {
      if (!this.getWorldEntity().isNight() && !this.getLevel().isCave) {
         this.activatedFailedEvent.runAndSend();
      } else {
         this.interactedAuths.add(client.authentication);
         if (this.canActivate()) {
            this.lastInteractionTime = this.getTime();
            this.activatedEvent.runAndSend(this.lastInteractionTime);
         }
      }
   }

   public boolean canActivate() {
      return !this.isCurrentlyRunning();
   }

   public boolean isCurrentlyRunning() {
      return this.lastInteractionTime != 0L && this.getTime() < this.lastInteractionTime + 20000L;
   }

   public boolean shouldActivateBuff() {
      return this.isCurrentlyRunning() && this.getTime() > this.lastInteractionTime + 10000L;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      if (this.isCurrentlyRunning()) {
         int tileX = this.tileX + 1;
         int tileY = this.tileY + 3;
         float lerpedValue = Math.min(1.0F, (float)(this.getTime() - this.lastInteractionTime) / 10000.0F);

         for (Point tile : particleTileRange.getValidTiles(tileX, tileY)) {
            if (GameRandom.globalRandom.getChance(0.02F * lerpedValue)) {
               this.spawnParticle(tile.x, tile.y, lerpedValue);
            }
         }

         for (Point tile : lightRange.getValidTiles(tileX, tileY)) {
            this.getLevel().lightManager.refreshParticleLight(tile.x, tile.y, 220.0F, 0.5F, (int)(lerpedValue * 150.0F));
         }
      } else {
         this.lastInteractionTime = 0L;
      }
   }

   @Override
   public void serverTick() {
      super.serverTick();
      if (this.isServer()) {
         if (this.isCurrentlyRunning()) {
            if (this.shouldActivateBuff() && !this.interactedAuths.isEmpty()) {
               for (long auth : this.interactedAuths) {
                  ServerClient client = this.getServer().getClientByAuth(auth);
                  if (client != null && client.hasSubmittedCharacter()) {
                     client.playerMob.buffManager.addBuff(new ActiveBuff(BuffRegistry.MOON_SHRINE_BUFF, client.playerMob, 1200.0F, null), true);
                     this.activatePlayerSlotEvent.runAndSend(client.slot);
                  }
               }

               this.interactedAuths.clear();
            }
         } else {
            this.lastInteractionTime = 0L;
         }
      }
   }

   public void spawnParticle(int tileX, int tileY, float lerpedValue) {
      if (!this.getLevel().getObject(tileX, tileY).drawsFullTile()) {
         int distanceValue = (int)(16.0F * lerpedValue);
         int posX = tileX * 32 + GameRandom.globalRandom.getIntBetween(-distanceValue, distanceValue);
         int posY = tileY * 32 + GameRandom.globalRandom.getIntBetween(-distanceValue, distanceValue);
         boolean mirror = GameRandom.globalRandom.nextBoolean();
         this.getLevel()
            .entityManager
            .addParticle(posX, posY, Particle.GType.COSMETIC)
            .sprite(GameResources.magicSparkParticles.sprite(GameRandom.globalRandom.getIntBetween(0, 3), 0, 22))
            .fadesAlpha(0.4F, 0.4F)
            .sizeFades(12, 18)
            .height(30.0F)
            .color(new Color(200, 200, 255))
            .movesConstant(GameRandom.globalRandom.getFloatBetween(1.0F, 2.0F) * GameRandom.globalRandom.getOneOf(1.0F, -1.0F), -1.0F)
            .modify((options, lifeTime, timeAlive, lifePercent) -> options.mirror(mirror, false))
            .lifeTime(3000);
      }
   }

   public float getGlowAlpha(long currentTime) {
      if (this.lastInteractionTime == 0L) {
         return 0.0F;
      } else {
         long fadeStartTime = this.lastInteractionTime + 20000L - 2500L;
         if (currentTime >= fadeStartTime) {
            float elapsedFadeTime = (float)(currentTime - fadeStartTime);
            return Math.max(0.0F, 1.0F - elapsedFadeTime / 2500.0F);
         } else {
            return currentTime - this.lastInteractionTime <= 10000L ? Math.min(1.0F, (float)(currentTime - this.lastInteractionTime) / 10000.0F) : 1.0F;
         }
      }
   }

   public void showNothingHappensText() {
      if (!this.isServer()) {
         MultiTile multiTile = this.getObject().getMultiTile(this.getLevel(), 0, this.tileX, this.tileY);
         final Point centerPos = multiTile.getCenterLevelPos(this.tileX, this.tileY);
         centerPos.y -= 65;
         FontOptions fontOptions = new FontOptions(16).outline().color(new Color(255, 255, 255));
         UniqueFloatText nothingHappensText = new UniqueFloatText(
            centerPos.x,
            centerPos.y,
            Localization.translate("ui", "praisethemoonspeakfailed"),
            fontOptions,
            "praisethemoonthought" + this.tileX + "x" + this.tileY
         ) {
            @Override
            public int getAnchorX() {
               return centerPos.x;
            }

            @Override
            public int getAnchorY() {
               return centerPos.y;
            }
         };
         nothingHappensText.fadeInTime = 250;
         nothingHappensText.hoverTime = 2500;
         nothingHappensText.riseTime = 500;
         nothingHappensText.heightIncrease = 25;
         nothingHappensText.fadeOutTime = 250;
         nothingHappensText.expandTime = 0;
         this.getLevel().hudManager.addElement(nothingHappensText);
      }
   }

   public void showActivatedText() {
      if (!this.isServer()) {
         MultiTile multiTile = this.getObject().getMultiTile(this.getLevel(), 0, this.tileX, this.tileY);
         final Point centerPos = multiTile.getCenterLevelPos(this.tileX, this.tileY);
         centerPos.y -= 40;
         final FontOptions fontOptions = new FontOptions(16).outline().color(new Color(136, 229, 245));
         UniqueFloatText text1 = new UniqueFloatText(
            centerPos.x, centerPos.y, Localization.translate("ui", "praisethemoonspeak1"), fontOptions, "praisethemoonthought" + this.tileX + "x" + this.tileY
         ) {
            @Override
            public int getAnchorX() {
               return centerPos.x;
            }

            @Override
            public int getAnchorY() {
               return centerPos.y;
            }
         };
         text1.fadeInTime = 750;
         text1.hoverTime = 4000;
         text1.riseTime = 0;
         text1.fadeOutTime = 750;
         text1.expandTime = 0;
         this.getLevel().hudManager.addElement(text1);
         WaitForSecondsEvent line1Wait = new WaitForSecondsEvent((text1.getFullLifeTime() + 500.0F) / 1000.0F) {
            @Override
            public void onWaitOver() {
               UniqueFloatText text2 = new UniqueFloatText(
                  centerPos.x,
                  centerPos.y,
                  Localization.translate("ui", "praisethemoonspeak2"),
                  fontOptions,
                  "praisethemoonthought" + PraiseTheMoonStatueObjectEntity.this.tileX + "x" + PraiseTheMoonStatueObjectEntity.this.tileY
               ) {
                  @Override
                  public int getAnchorX() {
                     return centerPos.x;
                  }

                  @Override
                  public int getAnchorY() {
                     return centerPos.y;
                  }
               };
               text2.fadeInTime = 750;
               text2.hoverTime = 4000;
               text2.riseTime = 0;
               text2.fadeOutTime = 750;
               text2.expandTime = 0;
               this.level.hudManager.addElement(text2);
               SoundManager.playSound(GameResources.glyphChargeUp, SoundEffect.effect(centerPos.x, centerPos.y));
            }
         };
         this.getLevel().entityManager.events.addHidden(line1Wait);
      }
   }
}
