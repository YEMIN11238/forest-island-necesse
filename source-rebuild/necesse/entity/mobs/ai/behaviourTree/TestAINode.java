package necesse.entity.mobs.ai.behaviourTree;

import necesse.entity.mobs.Mob;

public class TestAINode extends AINode<Mob> {
   public String name;
   public int runningTicks;
   private int currentTick;

   public TestAINode(String name, int runningTicks) {
      this.name = name;
      this.runningTicks = runningTicks;
   }

   public TestAINode(String name) {
      this(name, 0);
   }

   @Override
   public void onRootSet(AINode<Mob> root, Mob mob, Blackboard<Mob> blackboard) {
      System.out.println(this.name + " onRootSet " + root + ", " + blackboard);
   }

   @Override
   public void init(Mob mob, Blackboard<Mob> blackboard) {
      System.out.println(this.name + " init " + mob);
      this.currentTick = 0;
   }

   @Override
   public AINodeResult tick(Mob mob, Blackboard<Mob> blackboard) {
      if (this.currentTick < this.runningTicks) {
         this.currentTick++;
         System.out.println(this.name + " tick running " + this.currentTick + "/" + this.runningTicks + " " + mob);
         return AINodeResult.RUNNING;
      } else {
         System.out.println(this.name + " tick success " + mob);
         return AINodeResult.SUCCESS;
      }
   }
}
