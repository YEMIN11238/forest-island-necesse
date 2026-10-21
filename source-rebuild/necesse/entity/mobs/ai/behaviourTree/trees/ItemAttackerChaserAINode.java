package necesse.entity.mobs.ai.behaviourTree.trees;

import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.StaticItemAttackSlot;
import necesse.entity.mobs.ai.behaviourTree.AINode;
import necesse.entity.mobs.ai.behaviourTree.AINodeResult;
import necesse.entity.mobs.ai.behaviourTree.Blackboard;
import necesse.entity.mobs.ai.behaviourTree.DecoratorAINode;
import necesse.entity.mobs.ai.behaviourTree.event.AIEvent;
import necesse.entity.mobs.ai.behaviourTree.event.ItemAttackAIEvent;
import necesse.entity.mobs.ai.behaviourTree.leaves.ChaserAINode;
import necesse.entity.mobs.attackHandler.AttackHandler;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemAttackerWeaponItem;

public class ItemAttackerChaserAINode<T extends ItemAttackerMob> extends DecoratorAINode<T> {
   public InventoryItem lastWeaponItem;
   public InventoryItem defaultWeaponItem;
   public String currentTargetKey = "currentTarget";

   public ItemAttackerChaserAINode(InventoryItem defaultWeaponItem) {
      super(null);
      this.defaultWeaponItem = defaultWeaponItem;
   }

   public ItemAttackerChaserAINode() {
      this(new InventoryItem("woodsword"));
   }

   public void onRootSet(AINode<T> root, T mob, Blackboard<T> blackboard) {
      super.onRootSet(root, mob, blackboard);
      blackboard.onGlobalTick(event -> {
         AttackHandler attackHandler = mob.getAttackHandler();
         if (attackHandler != null) {
            Mob currentTarget = blackboard.getObject(Mob.class, this.currentTargetKey);
            attackHandler.setItemAttackerTarget(currentTarget);
         }
      });
   }

   public ItemAttackSlot getAttackSlot(T mob) {
      return mob.getCurrentSelectedAttackSlot();
   }

   public AINodeResult tickChild(AINode<T> child, T mob, Blackboard<T> blackboard) {
      boolean shouldUpdate = false;
      ItemAttackSlot slot = this.getAttackSlot(mob);
      ItemAttackSlot nextSlot;
      InventoryItem nextWeaponItem;
      if (slot != null) {
         InventoryItem item = slot.getItem();
         if (item != null && item.item instanceof ItemAttackerWeaponItem) {
            nextSlot = slot;
            nextWeaponItem = item;
         } else {
            nextSlot = new StaticItemAttackSlot(this.defaultWeaponItem);
            nextWeaponItem = this.defaultWeaponItem;
         }
      } else {
         nextSlot = new StaticItemAttackSlot(this.defaultWeaponItem);
         nextWeaponItem = this.defaultWeaponItem;
      }

      if (nextWeaponItem == null && this.lastWeaponItem != null) {
         shouldUpdate = true;
      } else if (this.lastWeaponItem == null
         || nextWeaponItem.item != this.lastWeaponItem.item
         || ((ItemAttackerWeaponItem)nextWeaponItem.item).shouldUpdateItemAttackerChaserAI(mob, this.lastWeaponItem, nextWeaponItem)) {
         shouldUpdate = true;
      }

      if (shouldUpdate) {
         this.lastWeaponItem = nextWeaponItem;
         child = (AINode<T>)this.getWeaponAI(mob, nextSlot, this.lastWeaponItem);
         this.setChild(child);
         this.runOnChaserUpdatedEvent();
      }

      return child == null ? AINodeResult.FAILURE : child.tick(mob, blackboard);
   }

   public void runOnChaserUpdatedEvent() {
      this.getBlackboard().submitEvent("itemAttackerUpdated", new AIEvent());
   }

   public void runOnAttackedEvent(ItemAttackerMob mob, InventoryItem item, int attackX, int attackY, int animAttack, GNDItemMap attackMap) {
      this.getBlackboard().submitEvent("itemAttack", new ItemAttackAIEvent(mob, item, attackX, attackY, animAttack, attackMap));
   }

   public ChaserAINode<T> getChaserAIIfExists() {
      AINode<T> child = this.getChild();
      return child instanceof ChaserAINode ? (ChaserAINode)child : null;
   }

   public AINode<? super T> getWeaponAI(T mob, ItemAttackSlot slot, InventoryItem weapon) {
      return weapon != null && weapon.item instanceof ItemAttackerWeaponItem
         ? ((ItemAttackerWeaponItem)weapon.item).getItemAttackerWeaponChaserAI(this, mob, weapon, slot)
         : null;
   }
}
