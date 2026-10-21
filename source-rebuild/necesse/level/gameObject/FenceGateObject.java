package necesse.level.gameObject;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.texture.SharedTextureDrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptions;
import necesse.gfx.drawables.LevelSortedDrawable;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.item.toolItem.ToolType;
import necesse.inventory.lootTable.LootTable;
import necesse.level.maps.Level;
import necesse.level.maps.LevelObject;
import necesse.level.maps.light.GameLight;
import necesse.level.maps.regionSystem.RegionType;

public class FenceGateObject extends DoorObject implements FenceObjectInterface {
   protected String textureName;
   protected GameTexture texture;
   protected int collisionWidth;
   protected int collisionHeight;
   protected HashSet<Integer> connections = new HashSet<>();

   protected FenceGateObject(int counterID, boolean isOpen, String textureName, Color mapColor, int collisionWidth, int collisionHeight) {
      super(new Rectangle(32, 32), counterID, isOpen);
      this.setItemCategory("objects", "fencesandgates");
      this.setCraftingCategory("objects", "fencesandgates");
      this.textureName = textureName;
      this.mapColor = mapColor;
      this.collisionWidth = collisionWidth;
      this.collisionHeight = collisionHeight;
      this.isFence = true;
      this.toolType = ToolType.ALL;
      this.isLightTransparent = true;
      this.canPlaceOnShore = true;
      this.regionType = RegionType.FENCE_GATE;
      this.hoverHitbox = new Rectangle(0, -10, 32, 42);
      this.replaceCategories.add("fencegate");
      this.canReplaceCategories.add("fencegate");
      this.canReplaceCategories.add("fence");
      this.canReplaceCategories.add("wall");
      this.canReplaceCategories.add("door");
      this.replaceRotations = false;
   }

   @Override
   public void loadTextures() {
      super.loadTextures();
      this.texture = GameTexture.fromFile("objects/" + this.textureName);
   }

   @Override
   public boolean attachesToObject(GameObject me, Level level, int tileX, int tileY, LevelObject other) {
      return (other.object.isWall || other.object.isFence || other.object.isRock) && !other.object.isDoor;
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
      int drawY = camera.getTileDrawY(tileY) - this.texture.getHeight() + 32;
      int rotation = this.rotateTowardsAttachment(level, tileX, tileY);
      if (rotation != 1 && rotation != 3) {
         final TextureDrawOptions draw = this.texture.initDraw().sprite(1, 0, 32, this.texture.getHeight()).light(light).pos(drawX, drawY);
         list.add(new LevelSortedDrawable(this, tileX, tileY) {
            @Override
            public int getSortY() {
               return 14;
            }

            @Override
            public void draw(TickManager tickManager) {
               draw.draw();
            }
         });
      } else {
         final SharedTextureDrawOptions botDraws = new SharedTextureDrawOptions(this.texture).addObjectDamageOverlay(this, level, tileX, tileY);
         final SharedTextureDrawOptions topDraws = new SharedTextureDrawOptions(this.texture).addObjectDamageOverlay(this, level, tileX, tileY);
         botDraws.addSprite(2, 0, 32, this.texture.getHeight()).light(light).pos(drawX, drawY + 14);
         if (rotation == 3) {
            botDraws.addSprite(3, 0, 32, this.texture.getHeight()).light(light).pos(drawX, drawY + 14);
         }

         topDraws.addSprite(2, 0, 32, this.texture.getHeight()).light(light).pos(drawX, drawY - 14);
         topDraws.addSprite(4, 0, 32, this.texture.getHeight()).light(light).pos(drawX, drawY - 14);
         list.add(new LevelSortedDrawable(this, tileX, tileY) {
            @Override
            public int getSortY() {
               return 26;
            }

            @Override
            public void draw(TickManager tickManager) {
               botDraws.draw();
            }
         });
         list.add(new LevelSortedDrawable(this, tileX, tileY) {
            @Override
            public int getSortY() {
               return 6;
            }

            @Override
            public void draw(TickManager tickManager) {
               topDraws.draw();
            }
         });
      }
   }

   @Override
   public void drawPreview(Level level, int tileX, int tileY, int rotation, float alpha, PlayerMob player, GameCamera camera) {
      int drawX = camera.getTileDrawX(tileX);
      int drawY = camera.getTileDrawY(tileY) - this.texture.getHeight() + 32;
      rotation = this.rotateTowardsAttachment(level, tileX, tileY, rotation);
      if (rotation != 1 && rotation != 3) {
         this.texture.initDraw().sprite(1, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY);
      } else {
         this.texture.initDraw().sprite(2, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - 14);
         this.texture.initDraw().sprite(4, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY - 14);
         this.texture.initDraw().sprite(2, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY + 14);
         if (rotation == 3) {
            this.texture.initDraw().sprite(3, 0, 32, this.texture.getHeight()).alpha(alpha).draw(drawX, drawY + 14);
         }
      }
   }

   @Override
   public Rectangle getCollision(Level level, int x, int y, int rotation) {
      rotation = this.rotateTowardsAttachment(level, x, y, rotation);
      return rotation != 1 && rotation != 3
         ? new Rectangle(x * 32, y * 32 + (32 - this.collisionHeight) / 2, 32, this.collisionHeight)
         : new Rectangle(x * 32 + (32 - this.collisionWidth) / 2, y * 32, this.collisionWidth, 32);
   }

   protected int rotateTowardsAttachment(Level level, int tileX, int tileY) {
      return this.rotateTowardsAttachment(level, tileX, tileY, level.getObjectRotation(tileX, tileY));
   }

   protected int rotateTowardsAttachment(Level level, int tileX, int tileY, int objectRotation) {
      boolean attachesTop = this.attachesToObject(this, level, tileX, tileY, level.getLevelObject(tileX, tileY - 1));
      boolean attachesBot = this.attachesToObject(this, level, tileX, tileY, level.getLevelObject(tileX, tileY + 1));
      boolean attachesLeft = this.attachesToObject(this, level, tileX, tileY, level.getLevelObject(tileX - 1, tileY));
      boolean attachesRight = this.attachesToObject(this, level, tileX, tileY, level.getLevelObject(tileX + 1, tileY));
      int rotation = objectRotation;
      if (attachesTop || attachesBot) {
         switch (objectRotation) {
            case 0:
            case 1:
               rotation = 1;
               break;
            case 2:
            case 3:
               rotation = 3;
         }
      } else if (attachesLeft || attachesRight) {
         switch (objectRotation) {
            case 0:
            case 1:
               rotation = 0;
               break;
            case 2:
            case 3:
               rotation = 2;
         }
      }

      return rotation;
   }

   @Override
   public void playSwitchSound(Level level, int x, int y) {
      if (level.isClient()) {
         SoundManager.playSound(this.isSwitched ? GameResources.doorclose : GameResources.dooropen, SoundEffect.effect(x * 32 + 16, y * 32 + 16));
      }
   }

   public static int[] registerGatePair(
      int fenceID, String stringIDPrefix, String textureName, Color mapColor, int collisionWidth, int collisionHeight, float brokerValue
   ) {
      FenceGateObject closed = new FenceGateObject(0, false, textureName, mapColor, collisionWidth, collisionHeight);
      int closedID = ObjectRegistry.registerObject(stringIDPrefix, closed, brokerValue, true);
      FenceGateObject.FenceGateOpenObject open = new FenceGateObject.FenceGateOpenObject(closedID, true, textureName, mapColor, collisionWidth, collisionHeight);
      int openID = ObjectRegistry.registerObject(stringIDPrefix + "open", open, 0.0F, false);
      closed.counterID = openID;
      closed.connections.add(openID);
      closed.connections.add(fenceID);
      open.connections.add(closedID);
      open.connections.add(fenceID);
      GameObject fenceObject = ObjectRegistry.getObject(fenceID);
      if (fenceObject instanceof FenceObject) {
         ((FenceObject)fenceObject).connections.add(closedID);
         ((FenceObject)fenceObject).connections.add(openID);
      }

      return new int[]{closedID, openID};
   }

   private static class FenceGateOpenObject extends FenceGateObject {
      protected FenceGateOpenObject(int counterID, boolean isOpen, String textureName, Color mapColor, int collisionWidth, int collisionHeight) {
         super(counterID, isOpen, textureName, mapColor, collisionWidth, collisionHeight);
      }

      @Override
      public LootTable getLootTable(Level level, int layerID, int tileX, int tileY) {
         return ObjectRegistry.getObject(this.counterID).getLootTable(level, layerID, tileX, tileY);
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
         int drawY = camera.getTileDrawY(tileY) - this.texture.getHeight() + 32;
         int rotation = this.rotateTowardsAttachment(level, tileX, tileY);
         if (rotation != 1 && rotation != 3) {
            final ArrayList<TextureDrawOptions> draws = new ArrayList<>();
            draws.add(this.texture.initDraw().sprite(0, 0, 32, this.texture.getHeight()).light(light).pos(drawX, drawY));
            list.add(new LevelSortedDrawable(this, tileX, tileY) {
               @Override
               public int getSortY() {
                  return 14;
               }

               @Override
               public void draw(TickManager tickManager) {
                  draws.forEach(TextureDrawOptions::draw);
               }
            });
         } else {
            final ArrayList<TextureDrawOptions> botDraws = new ArrayList<>();
            final ArrayList<TextureDrawOptions> topDraws = new ArrayList<>();
            botDraws.add(
               this.texture
                  .initDraw()
                  .sprite(2, 0, 32, this.texture.getHeight())
                  .addObjectDamageOverlay(this, level, tileX, tileY)
                  .light(light)
                  .pos(drawX, drawY + 14)
            );
            if (rotation == 3) {
               botDraws.add(
                  this.texture
                     .initDraw()
                     .sprite(3, 0, 32, this.texture.getHeight())
                     .addObjectDamageOverlay(this, level, tileX, tileY)
                     .light(light)
                     .pos(drawX, drawY + 14)
               );
            }

            topDraws.add(
               this.texture
                  .initDraw()
                  .sprite(2, 0, 32, this.texture.getHeight())
                  .addObjectDamageOverlay(this, level, tileX, tileY)
                  .light(light)
                  .pos(drawX, drawY - 14)
            );
            botDraws.add(
               this.texture
                  .initDraw()
                  .sprite(5, 0, 32, this.texture.getHeight())
                  .addObjectDamageOverlay(this, level, tileX, tileY)
                  .light(light)
                  .pos(drawX - 16, drawY + 14)
            );
            list.add(new LevelSortedDrawable(this, tileX, tileY) {
               @Override
               public int getSortY() {
                  return 26;
               }

               @Override
               public void draw(TickManager tickManager) {
                  botDraws.forEach(TextureDrawOptions::draw);
               }
            });
            list.add(new LevelSortedDrawable(this, tileX, tileY) {
               @Override
               public int getSortY() {
                  return 6;
               }

               @Override
               public void draw(TickManager tickManager) {
                  topDraws.forEach(TextureDrawOptions::draw);
               }
            });
         }
      }

      @Override
      public Rectangle getCollision(Level level, int x, int y, int rotation) {
         return new Rectangle();
      }

      @Override
      public boolean shouldSnapSmartMining(Level level, int x, int y) {
         return true;
      }

      @Override
      public boolean isSolid(Level level, int x, int y) {
         return false;
      }
   }
}
