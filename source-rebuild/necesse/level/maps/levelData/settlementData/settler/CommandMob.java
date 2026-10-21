package necesse.level.maps.levelData.settlementData.settler;

import necesse.engine.localization.message.GameMessage;
import necesse.engine.network.client.Client;
import necesse.engine.network.server.ServerClient;
import necesse.entity.mobs.Mob;
import necesse.gfx.drawOptions.human.HumanDrawOptions;
import necesse.gfx.forms.ComponentListContainer;
import necesse.gfx.forms.components.FormComponent;
import necesse.gfx.forms.components.FormFlow;

public interface CommandMob {
   boolean hasCommandOrders();

   boolean canBeCommandPartySelected(Client var1);

   boolean canBeCommanded(Client var1);

   boolean canBeCommanded(ServerClient var1, boolean var2);

   void clearCommandsOrders(ServerClient var1);

   void commandFollow(ServerClient var1, Mob var2);

   void commandGuard(ServerClient var1, int var2, int var3);

   void commandAttack(ServerClient var1, Mob var2);

   void setHideOnLowHealth(boolean var1);

   boolean getHideOnLowHealth();

   Mob getMob();

   GameMessage getCommandMobSingleName();

   GameMessage getCommandMobTypeName();

   HumanDrawOptions getCommandFaceDrawOptions();

   void addCommandFormHeader(ComponentListContainer<? super FormComponent> var1, FormFlow var2, int var3);
}
