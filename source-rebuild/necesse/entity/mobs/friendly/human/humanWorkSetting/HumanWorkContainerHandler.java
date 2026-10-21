package necesse.entity.mobs.friendly.human.humanWorkSetting;

import necesse.gfx.forms.presets.containerComponent.mob.ShopContainerForm;
import necesse.inventory.container.mob.ShopContainer;

public abstract class HumanWorkContainerHandler<T> {
   public final ShopContainer container;
   public final ShopContainer.ContainerWorkSetting<T> setting;

   public HumanWorkContainerHandler(ShopContainer container, ShopContainer.ContainerWorkSetting<T> setting) {
      this.container = container;
      this.setting = setting;
   }

   public abstract void initForm(ShopContainerForm<?> var1);

   public abstract void updateDialogue(ShopContainerForm<?> var1);
}
