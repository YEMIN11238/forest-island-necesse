package necesse.entity.objectEntity;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import necesse.engine.GameTileRange;
import necesse.engine.localization.Localization;
import necesse.engine.sound.SoundCooldown;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameRandom;
import necesse.entity.events.LongEntityEvent;
import necesse.entity.levelEvent.WaitForSecondsEvent;
import necesse.entity.particle.Particle;
import necesse.gfx.GameResources;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.Level;
import necesse.level.maps.hudManager.floatText.UniqueFloatText;
import necesse.level.maps.multiTile.MultiTile;

public class HuginStatueObjectEntity extends ObjectEntity {
   public static GameTileRange fogTileRange = new GameTileRange(10, true, new Rectangle(2, 1));
   public static GameTileRange lightTileRange = new GameTileRange(2, new Rectangle(2, 1));
   public static int line1Time = 3000;
   public static int line2Time = 4000;
   public static int line3Time = 4000;
   public long lastInteractionTime;
   public final LongEntityEvent activatedEvent = this.registerEvent(new LongEntityEvent() {
      @Override
      protected void run(long value) {
         HuginStatueObjectEntity.this.lastInteractionTime = value;
         HuginStatueObjectEntity.this.showActivatedText();
      }
   });

   public HuginStatueObjectEntity(Level level, int tileX, int tileY) {
      super(level, "huginstatue", tileX, tileY);
   }

   @Override
   public boolean shouldSave() {
      return false;
   }

   public void activateStatue() {
      if (this.isServer() && this.canActivate()) {
         this.lastInteractionTime = this.getTime();
         this.activatedEvent.runAndSend(this.lastInteractionTime);
      }
   }

   public boolean canActivate() {
      return this.lastInteractionTime == 0L || this.getTime() > this.lastInteractionTime + line1Time + line2Time + line3Time;
   }

   @Override
   public void clientTick() {
      super.clientTick();
      int tileX = this.tileX;
      int tileY = this.tileY + 4;

      for (Point tile : fogTileRange.getValidTiles(tileX, tileY)) {
         this.spawnFog(tile.x, tile.y);
      }

      for (Point tile : lightTileRange.getValidTiles(tileX, tileY)) {
         this.getLevel().lightManager.refreshParticleLight(tile.x, tile.y, 220.0F, 0.5F, 150);
      }
   }

   public void spawnFog(int tileX, int tileY) {
      if (GameRandom.globalRandom.getChance(0.025F) && !this.getLevel().getObject(tileX, tileY).drawsFullTile()) {
         int posX = tileX * 32 + GameRandom.globalRandom.nextInt(32);
         int posY = tileY * 32 + GameRandom.globalRandom.nextInt(32);
         boolean mirror = GameRandom.globalRandom.nextBoolean();
         this.getLevel()
            .entityManager
            .addParticle(posX, posY + 30, Particle.GType.COSMETIC)
            .sprite(GameResources.fogParticles.sprite(GameRandom.globalRandom.nextInt(4), 0, 32, 16))
            .fadesAlpha(0.4F, 0.4F)
            .size((options, lifeTime, timeAlive, lifePercent) -> {})
            .height(30.0F)
            .dontRotate()
            .movesConstant(GameRandom.globalRandom.getFloatBetween(2.0F, 5.0F) * GameRandom.globalRandom.getOneOf(1.0F, -1.0F), 0.0F)
            .modify((options, lifeTime, timeAlive, lifePercent) -> options.mirror(mirror, false))
            .lifeTime(3000);
      }
   }

   public void showActivatedText() {
      if (!this.isServer()) {
         MultiTile multiTile = this.getObject().getMultiTile(this.getLevel(), 0, this.tileX, this.tileY);
         final Point centerPos = multiTile.getCenterLevelPos(this.tileX, this.tileY);
         SoundManager.playSound(GameResources.crow, SoundEffect.effect(centerPos.x, centerPos.y).volume(0.8F).pitch(1.0F), (SoundCooldown)null);
         this.getLevel().entityManager.events.addHidden(new WaitForSecondsEvent(0.4F) {
            @Override
            public void onWaitOver() {
               SoundManager.playSound(GameResources.crow, SoundEffect.effect(centerPos.x, centerPos.y).volume(0.8F).pitch(0.9F), (SoundCooldown)null);
            }
         });
         centerPos.y -= 20;
         final int line2TypeWriterExpand = 1500;
         final int line3TypeWriterExpand = 1500;
         FontOptions fontOptions = new FontOptions(16).outline().color(new Color(243, 134, 25));
         UniqueFloatText text = new UniqueFloatText(centerPos.x, centerPos.y, Localization.translate("ui", "valheimhuginspeak1"), fontOptions, "huginthought") {
            @Override
            public int getAnchorX() {
               return centerPos.x;
            }

            @Override
            public int getAnchorY() {
               return centerPos.y;
            }
         };
         text.hoverTime = line1Time + 1000;
         text.riseTime = 500;
         this.getLevel().hudManager.addElement(text);
         WaitForSecondsEvent line1Wait = new WaitForSecondsEvent(line1Time / 1000.0F) {
            @Override
            public void onWaitOver() {
               FontOptions fontOptions = new FontOptions(16).outline();
               UniqueFloatText text = new UniqueFloatText(
                  centerPos.x, centerPos.y, Localization.translate("ui", "valheimhuginspeak2"), fontOptions, "huginthought"
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
               text.hoverTime = HuginStatueObjectEntity.line2Time + 1000;
               text.riseTime = 0;
               text.expandTime = 0;
               text.typeWriterExpandTime = line2TypeWriterExpand;
               this.level.hudManager.addElement(text);
            }
         };
         this.getLevel().entityManager.events.addHidden(line1Wait);
         WaitForSecondsEvent line2Wait = new WaitForSecondsEvent((line1Time + line2Time) / 1000.0F) {
            @Override
            public void onWaitOver() {
               FontOptions fontOptions = new FontOptions(16).outline();
               UniqueFloatText text = new UniqueFloatText(
                  centerPos.x, centerPos.y, Localization.translate("ui", "valheimhuginspeak3"), fontOptions, "huginthought"
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
               text.hoverTime = HuginStatueObjectEntity.line3Time;
               text.riseTime = 0;
               text.expandTime = 0;
               text.typeWriterExpandTime = line3TypeWriterExpand;
               this.level.hudManager.addElement(text);
            }
         };
         this.getLevel().entityManager.events.addHidden(line2Wait);
      }
   }
}
