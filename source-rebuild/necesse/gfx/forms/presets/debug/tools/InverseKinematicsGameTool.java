package necesse.gfx.forms.presets.debug.tools;

import java.awt.Color;
import java.awt.geom.Point2D.Float;
import java.util.List;
import necesse.engine.GlobalData;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.client.Client;
import necesse.engine.state.MainGame;
import necesse.engine.util.GameLinkedList;
import necesse.engine.util.GameMath;
import necesse.engine.util.InverseKinematics;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.Renderer;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.SortedDrawable;
import necesse.gfx.forms.presets.debug.DebugForm;
import necesse.gfx.forms.presets.sidebar.SidebarForm;
import necesse.level.maps.hudManager.HudDrawElement;

public class InverseKinematicsGameTool extends MouseDebugGameTool {
   public InverseKinematicsGameTool.ControlForm controlForm;
   public HudDrawElement hudElement;
   public InverseKinematics current;
   private Float startPos;
   private Float currentPos;
   private Float targetPos;
   GameLinkedList<InverseKinematics.Limb>.Element selectedLimb;
   public boolean selectedLimbInbound = false;
   private boolean createMode = true;

   public InverseKinematicsGameTool(DebugForm parent) {
      super(parent, "InverseKinematics");
   }

   @Override
   public void init() {
      this.getLevel()
         .hudManager
         .addElement(
            this.hudElement = new HudDrawElement() {
               @Override
               public void addDrawables(List<SortedDrawable> list, final GameCamera camera, PlayerMob perspective) {
                  list.add(
                     new SortedDrawable() {
                        @Override
                        public int getPriority() {
                           return -10000;
                        }

                        @Override
                        public void draw(TickManager tickManager) {
                           if (InverseKinematicsGameTool.this.current != null) {
                              InverseKinematicsGameTool.this.current.drawDebug(camera, new Color(255, 0, 0), new Color(0, 255, 0));
                           } else if (InverseKinematicsGameTool.this.startPos != null) {
                              Renderer.drawCircle(
                                 camera.getDrawX(InverseKinematicsGameTool.this.startPos.x),
                                 camera.getDrawY(InverseKinematicsGameTool.this.startPos.y),
                                 4,
                                 12,
                                 1.0F,
                                 0.0F,
                                 0.0F,
                                 1.0F,
                                 false
                              );
                           }

                           if (InverseKinematicsGameTool.this.targetPos != null) {
                              Renderer.drawCircle(
                                 camera.getDrawX(InverseKinematicsGameTool.this.targetPos.x),
                                 camera.getDrawY(InverseKinematicsGameTool.this.targetPos.y),
                                 4,
                                 12,
                                 0.0F,
                                 1.0F,
                                 0.0F,
                                 1.0F,
                                 false
                              );
                           }

                           if (InverseKinematicsGameTool.this.currentPos != null) {
                              Renderer.drawCircle(
                                 camera.getDrawX(InverseKinematicsGameTool.this.currentPos.x),
                                 camera.getDrawY(InverseKinematicsGameTool.this.currentPos.y),
                                 4,
                                 12,
                                 1.0F,
                                 1.0F,
                                 0.0F,
                                 1.0F,
                                 false
                              );
                           }

                           if (InverseKinematicsGameTool.this.selectedLimb != null && !InverseKinematicsGameTool.this.selectedLimb.isRemoved()) {
                              Float p;
                              if (InverseKinematicsGameTool.this.selectedLimbInbound) {
                                 p = new Float(
                                    ((InverseKinematics.Limb)InverseKinematicsGameTool.this.selectedLimb.object).inboundX,
                                    ((InverseKinematics.Limb)InverseKinematicsGameTool.this.selectedLimb.object).inboundY
                                 );
                              } else {
                                 p = new Float(
                                    ((InverseKinematics.Limb)InverseKinematicsGameTool.this.selectedLimb.object).outboundX,
                                    ((InverseKinematics.Limb)InverseKinematicsGameTool.this.selectedLimb.object).outboundY
                                 );
                              }

                              Renderer.drawCircle(camera.getDrawX(p.x), camera.getDrawY(p.y), 4, 12, 0.0F, 0.0F, 1.0F, 1.0F, false);
                           }
                        }
                     }
                  );
               }
            }
         );
      this.updateInput();
      if (this.controlForm != null) {
         this.controlForm.invalidate();
      }

      if (GlobalData.getCurrentState() instanceof MainGame) {
         this.controlForm = new InverseKinematicsGameTool.ControlForm(100, 100);
         ((MainGame)GlobalData.getCurrentState()).formManager.addSidebar(this.controlForm);
      }
   }

   public void updateInput() {
      if (this.createMode) {
         this.onLeftClick(e -> {
            if (this.current == null) {
               if (this.startPos == null) {
                  this.startPos = new Float(this.getMouseX(), this.getMouseY());
               } else {
                  this.current = InverseKinematics.startFromPoints(this.startPos.x, this.startPos.y, this.getMouseX(), this.getMouseY());
                  this.startPos = null;
               }
            } else {
               this.current.addJointPoint(this.getMouseX(), this.getMouseY());
            }

            return true;
         }, "Add joint");
         this.onRightClick(e -> {
            if (this.current != null) {
               if (this.current.getTotalJoints() > 1) {
                  this.current.removeLastLimb();
               } else {
                  InverseKinematics.Limb first = this.current.limbs.getFirst();
                  this.startPos = new Float(first.inboundX, first.inboundY);
                  this.current = null;
               }
            } else {
               this.startPos = null;
            }

            return true;
         }, "Remove joint");
      } else {
         this.onLeftClick(
            e -> {
               if (this.current != null) {
                  this.targetPos = null;
                  this.currentPos = null;
                  double bestDist = Double.MAX_VALUE;
                  GameLinkedList<InverseKinematics.Limb>.Element bestLimb = null;

                  for (GameLinkedList<InverseKinematics.Limb>.Element element : this.current.limbs.elements()) {
                     InverseKinematics.Limb limb = (InverseKinematics.Limb)element.object;
                     double inDist = new Float(limb.inboundX, limb.inboundY).distance(this.getMouseX(), this.getMouseY());
                     if (inDist < bestDist) {
                        bestDist = inDist;
                        bestLimb = element;
                        this.selectedLimbInbound = true;
                     }

                     double outDist = new Float(limb.outboundX, limb.outboundY).distance(this.getMouseX(), this.getMouseY());
                     if (outDist < bestDist) {
                        bestDist = outDist;
                        bestLimb = element;
                        this.selectedLimbInbound = false;
                     }
                  }

                  if (bestDist < 32.0) {
                     this.selectedLimb = bestLimb;
                     if (this.selectedLimb != null) {
                        if (this.selectedLimbInbound) {
                           this.currentPos = new Float(
                              ((InverseKinematics.Limb)this.selectedLimb.object).inboundX, ((InverseKinematics.Limb)this.selectedLimb.object).inboundY
                           );
                        } else {
                           this.currentPos = new Float(
                              ((InverseKinematics.Limb)this.selectedLimb.object).outboundX, ((InverseKinematics.Limb)this.selectedLimb.object).outboundY
                           );
                        }
                     }
                  } else {
                     this.selectedLimb = null;
                  }
               } else {
                  this.selectedLimb = null;
               }

               return true;
            },
            "Select joint"
         );
         this.onRightClick(e -> {
            this.targetPos = new Float(this.getMouseX(), this.getMouseY());
            if (this.selectedLimb == null && this.current != null) {
               System.out.println(this.current.apply(this.targetPos.x, this.targetPos.y, 0.1F, 0.5F, 100000) + " FABRIK iterations");
            }

            return true;
         }, "Set target");
      }

      this.onScroll(e -> {
         this.createMode = !this.createMode;
         this.updateInput();
         return true;
      }, "Change mode");
   }

   @Override
   public void tick() {
      float speed = 50.0F;
      float delta = 50.0F;
      if (this.selectedLimb != null && this.current != null && this.currentPos != null && this.targetPos != null) {
         double dist = this.currentPos.distance(this.targetPos);
         if (dist != 0.0) {
            float distToMove = speed * delta / 250.0F;
            if (dist <= distToMove) {
               this.currentPos = new Float(this.targetPos.x, this.targetPos.y);
               InverseKinematics.apply(this.selectedLimb, this.selectedLimbInbound, this.currentPos.x, this.currentPos.y, true);
            } else {
               Float dir = GameMath.normalize(this.targetPos.x - this.currentPos.x, this.targetPos.y - this.currentPos.y);
               this.currentPos.x = this.currentPos.x + dir.x * distToMove;
               this.currentPos.y = this.currentPos.y + dir.y * distToMove;
               InverseKinematics.apply(this.selectedLimb, this.selectedLimbInbound, this.currentPos.x, this.currentPos.y, true);
            }
         }
      }
   }

   @Override
   public void isCancelled() {
      super.isCancelled();
      if (this.hudElement != null) {
         this.hudElement.remove();
      }

      if (this.controlForm != null) {
         this.controlForm.invalidate();
      }
   }

   @Override
   public void isCleared() {
      super.isCleared();
      if (this.hudElement != null) {
         this.hudElement.remove();
      }

      if (this.controlForm != null) {
         this.controlForm.invalidate();
      }
   }

   protected static class ControlForm extends SidebarForm {
      private boolean isValid = true;

      public ControlForm(int width, int height) {
         super("IKControl", width, height);
      }

      @Override
      public boolean isValid(Client client) {
         return this.isValid;
      }

      public void invalidate() {
         this.isValid = false;
      }
   }
}
