package necesse.level.maps.incursion;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;
import necesse.engine.GlobalData;
import necesse.engine.Settings;
import necesse.engine.expeditions.SettlerExpedition;
import necesse.engine.localization.Localization;
import necesse.engine.localization.message.GameMessageBuilder;
import necesse.engine.modifiers.Modifier;
import necesse.engine.modifiers.ModifierValue;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.IncursionBiomeRegistry;
import necesse.engine.registries.UniqueIncursionModifierRegistry;
import necesse.engine.registries.UniqueIncursionRewardsRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.LoadDataException;
import necesse.engine.save.SaveData;
import necesse.engine.storyObjectives.listeners.IncursionCompletedObjectiveListener;
import necesse.engine.util.GameBlackboard;
import necesse.engine.util.GameLootUtils;
import necesse.engine.util.GameRandom;
import necesse.engine.util.LevelIdentifier;
import necesse.engine.world.WorldEntity;
import necesse.entity.objectEntity.FallenAltarObjectEntity;
import necesse.gfx.fairType.FairItemGlyph;
import necesse.gfx.fairType.FairType;
import necesse.gfx.fairType.TypeParsers;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormFairTypeLabel;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormLabel;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.presets.containerComponent.object.FallenAltarContainerForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTooltips.GameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.object.fallenAltar.FallenAltarContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.armorItem.ArmorItem;
import necesse.inventory.item.miscItem.CoinItem;
import necesse.inventory.item.miscItem.GatewayTabletItem;
import necesse.inventory.item.toolItem.ToolItem;
import necesse.inventory.item.trinketItem.TrinketItem;
import necesse.inventory.lootTable.lootItem.LootItemList;
import necesse.level.maps.IncursionLevel;
import necesse.level.maps.levelBuffManager.LevelModifiers;

public abstract class BiomeMissionIncursionData extends IncursionData {
   public float difficulty;
   public IncursionBiome biome;
   protected int tabletTier;
   public float[] damageScalingPerTier = new float[]{0.0F, 0.15F, 0.14F, 0.13F, 0.12F, 0.11F, 0.1F, 0.12F, 0.13F, 0.15F};
   public float[] healthScalingPerTier = new float[]{0.0F, 0.25F, 0.27F, 0.29F, 0.31F, 0.33F, 0.35F, 0.38F, 0.4F, 0.42F};
   public float undefinedDamageScalingPerTier = 0.04F;
   public float undefinedHealthScalingPerTier = 0.45F;
   protected boolean modifiersInitialized = false;
   protected ArrayList<UniqueIncursionModifier> uniqueIncursionModifiers = new ArrayList<>();
   protected LootItemList playerPersonalIncursionCompleteRewards;
   protected LootItemList playerSharedIncursionCompleteRewards;
   protected ArrayList<ModifierValue<?>> levelModifiers = new ArrayList<>();
   private static final GameLootUtils.LootValueMap<BiomeMissionIncursionData.IncursionModifier> modifierMap = new GameLootUtils.LootValueMap<BiomeMissionIncursionData.IncursionModifier>() {
      public float getValuePerCount(BiomeMissionIncursionData.IncursionModifier object) {
         return object.value;
      }

      public int getRemainingCount(BiomeMissionIncursionData.IncursionModifier object) {
         return object.percent;
      }

      public void setRemainingCount(BiomeMissionIncursionData.IncursionModifier object, int count) {
         object.percent = count;
      }

      public boolean canCombine(BiomeMissionIncursionData.IncursionModifier object, BiomeMissionIncursionData.IncursionModifier other) {
         return object.modifier == other.modifier;
      }

      public void onCombine(BiomeMissionIncursionData.IncursionModifier object, BiomeMissionIncursionData.IncursionModifier other) {
         object.percent = object.percent + other.percent;
      }

      public BiomeMissionIncursionData.IncursionModifier copy(BiomeMissionIncursionData.IncursionModifier object, int amount) {
         return new BiomeMissionIncursionData.IncursionModifier(object.modifier, object.isLevelModifier, object.invert, object.value, amount);
      }
   };

   public BiomeMissionIncursionData() {
   }

   public BiomeMissionIncursionData(float difficulty, IncursionBiome biome, int tabletTier) {
      this.difficulty = difficulty;
      this.biome = biome;
      this.tabletTier = tabletTier;
   }

   protected void initModifiers() {
      this.levelModifiers = new ArrayList<>();
      this.uniqueIncursionModifiers = new ArrayList<>();
      this.playerPersonalIncursionCompleteRewards = new LootItemList();
      this.playerSharedIncursionCompleteRewards = new LootItemList();
      float lootPercentIncreasePerTier = 15.0F;
      BiomeMissionIncursionData.IncursionModifier tierHealthIncrease = new BiomeMissionIncursionData.IncursionModifier(
         LevelModifiers.ENEMY_MAX_HEALTH, false, false, this.getHealthIncrease(), 100
      );
      BiomeMissionIncursionData.IncursionModifier tierDamageIncrease = new BiomeMissionIncursionData.IncursionModifier(
         LevelModifiers.ENEMY_DAMAGE, false, false, this.getDamageIncrease(), 100
      );
      BiomeMissionIncursionData.IncursionModifier lootDropIncrease = new BiomeMissionIncursionData.IncursionModifier(
         LevelModifiers.LOOT, false, false, lootPercentIncreasePerTier * this.tabletTier, 100
      );
      this.levelModifiers.add(new ModifierValue<>(tierHealthIncrease.modifier, tierHealthIncrease.value / tierHealthIncrease.percent));
      this.levelModifiers.add(new ModifierValue<>(tierDamageIncrease.modifier, tierDamageIncrease.value / tierDamageIncrease.percent));
      this.levelModifiers.add(new ModifierValue<>(lootDropIncrease.modifier, lootDropIncrease.value / tierDamageIncrease.percent));
      int modifiersToAdd = MODIFIERS_TO_ADD_FROM_TIER_1;
      if (this.tabletTier >= 4) {
         modifiersToAdd = MODIFIERS_TO_ADD_FROM_TIER_4;
      }

      if (this.tabletTier >= 8) {
         modifiersToAdd = MODIFIERS_TO_ADD_FROM_TIER_8;
      }

      ArrayList<UniqueIncursionModifier> modifiersAlreadyUsed = new ArrayList<>();

      for (int i = 0; i < modifiersToAdd; i++) {
         GameRandom uniqueModifierRandom = new GameRandom(this.getUniqueID()).nextSeeded(20 * (i + 1) * this.getTabletTier());
         UniqueIncursionModifier newModifier = UniqueIncursionModifierRegistry.getRandomIncursionModifier(uniqueModifierRandom, modifiersAlreadyUsed);
         this.uniqueIncursionModifiers.add(newModifier);
         modifiersAlreadyUsed.add(newModifier);
         this.playerSharedIncursionCompleteRewards.add(UniqueIncursionRewardsRegistry.getSeededRandomReward(this, uniqueModifierRandom, this.getTabletTier()));
      }

      this.playerSharedIncursionCompleteRewards.add(SettlerExpedition.legendaryHappinessObjects);
      this.modifiersInitialized = true;
   }

   private float getDamageIncrease() {
      float damageIncrease = 0.0F;

      for (int i = 0; i < this.tabletTier && i < this.damageScalingPerTier.length; i++) {
         damageIncrease += this.damageScalingPerTier[i];
      }

      if (this.tabletTier > this.damageScalingPerTier.length) {
         damageIncrease += (this.tabletTier - this.damageScalingPerTier.length) * this.undefinedDamageScalingPerTier;
      }

      return damageIncrease * 100.0F;
   }

   private float getHealthIncrease() {
      float healthIncrease = 0.0F;

      for (int i = 0; i < this.tabletTier && i < this.healthScalingPerTier.length; i++) {
         healthIncrease += this.healthScalingPerTier[i];
      }

      if (this.tabletTier > this.healthScalingPerTier.length) {
         healthIncrease += (this.tabletTier - this.healthScalingPerTier.length) * this.undefinedHealthScalingPerTier;
      }

      return healthIncrease * 100.0F;
   }

   @Override
   public void addSaveData(SaveData save) {
      super.addSaveData(save);
      save.addFloat("difficulty", this.difficulty);
      save.addUnsafeString("biome", this.biome.getStringID());
      save.addInt("tabletTier", this.tabletTier);
   }

   @Override
   public void applyLoadData(LoadData save) {
      super.applyLoadData(save);
      this.difficulty = save.getFloat("difficulty");
      String biomeStringID = save.getUnsafeString("biome");
      this.biome = IncursionBiomeRegistry.getBiome(biomeStringID);
      if (this.biome == null) {
         throw new LoadDataException("Could not find incursion biome with stringID " + biomeStringID);
      }

      this.tabletTier = save.getInt("tabletTier");
   }

   @Override
   public void writePacket(PacketWriter writer) {
      super.writePacket(writer);
      writer.putNextFloat(this.difficulty);
      writer.putNextShortUnsigned(this.biome.getID());
      writer.putNextInt(this.tabletTier);
   }

   @Override
   public void applyPacket(PacketReader reader) {
      super.applyPacket(reader);
      this.difficulty = reader.getNextFloat();
      int biomeID = reader.getNextShortUnsigned();
      this.biome = IncursionBiomeRegistry.getBiome(biomeID);
      if (this.biome == null) {
         new IllegalArgumentException("Could not find incursion biome with id " + biomeID).printStackTrace();
      }

      this.tabletTier = reader.getNextInt();
   }

   @Override
   public IncursionBiome getIncursionBiome() {
      return this.biome;
   }

   @Override
   public GameSprite getTabletSprite() {
      return this.biome.getTabletSprite();
   }

   @Override
   public void setTabletTier(int tier) {
      this.tabletTier = tier;
      this.modifiersInitialized = false;
   }

   @Override
   public int getTabletTier() {
      return this.tabletTier;
   }

   @Override
   public boolean isSameIncursion(IncursionData otherData) {
      if (otherData.getID() != this.getID()) {
         return false;
      } else {
         BiomeMissionIncursionData otherBiomeData = (BiomeMissionIncursionData)otherData;
         if (otherBiomeData.getUniqueID() != this.getUniqueID()) {
            return false;
         } else {
            return otherBiomeData.difficulty != this.difficulty ? false : otherBiomeData.biome.getID() == this.biome.getID();
         }
      }
   }

   @Override
   public void setUpDetails(FallenAltarContainer container, FallenAltarContainerForm form, FormContentBox content, boolean isOpen) {
      if (!this.modifiersInitialized) {
         this.initModifiers();
      }

      FormFlow flow = new FormFlow(12);
      int fontSize = 16;
      int headlineFontSize = 20;
      content.addComponent(
         flow.nextY(
            new FormLocalLabel(
               this.biome.getLocalization(), new FontOptions(headlineFontSize), 0, content.getMinContentWidth() / 2, 0, content.getMinContentWidth() - 10
            ),
            4
         )
      );
      this.biome.setupTypeAndTierLabels(this, fontSize, content, flow);
      flow.next(14);
      FormFlow lootFlow = new FormFlow();
      Form lootContent = new Form(content.getMinContentWidth(), 0);
      lootContent.drawBase = false;
      lootContent.shouldLimitDrawArea = false;

      for (FairType loot : this.getLoot(this, new FontOptions(fontSize))) {
         FormFairTypeLabel label = new FormFairTypeLabel("", lootContent.getWidth() / 2, 0);
         label.setMaxWidth(lootContent.getWidth() - 10);
         label.setCustomFairType(loot);
         label.setTextAlign(FairType.TextAlign.CENTER);
         lootContent.addComponent(lootFlow.nextY(label, 2));
      }

      ArrayList<FairType> privateDrops = this.biome.getPrivateDropsDisplay(new FontOptions(fontSize));
      if (privateDrops != null) {
         for (FairType privateDrop : privateDrops) {
            FormFairTypeLabel label = new FormFairTypeLabel("", lootContent.getWidth() / 2, 0);
            label.setCustomFairType(privateDrop);
            label.setMaxWidth(lootContent.getWidth() - 10);
            label.setTextAlign(FairType.TextAlign.CENTER);
            lootContent.addComponent(lootFlow.nextY(label, 2));
         }
      }

      this.getAndSetupRewardLabels(this.getPlayerPersonalIncursionCompleteRewards(), fontSize, lootContent, lootFlow);
      this.getAndSetupRewardLabels(this.getPlayerSharedIncursionCompleteRewards(), fontSize, lootContent, lootFlow);
      lootContent.setHeight(lootFlow.next());
      if (!lootContent.isEmpty()) {
         content.addComponent(
            flow.nextY(
               new FormLocalLabel(
                  "ui", "incursionloot", new FontOptions(headlineFontSize), 0, content.getMinContentWidth() / 2, 0, content.getMinContentWidth() - 10
               ),
               2
            )
         );
         content.addComponent(flow.nextY(lootContent, 14));
      }

      lootFlow = new FormFlow();
      lootContent = new Form(content.getMinContentWidth(), 0);
      lootContent.drawBase = false;
      lootContent.shouldLimitDrawArea = false;
      final Form modifierLootContent = lootContent;
      final FormFlow modifierLootFlow = lootFlow;
      if (!this.levelModifiers.isEmpty()) {
         this.levelModifiers.stream().flatMap(m -> m.getAllTooltips().stream()).sequential().sorted(Comparator.comparingInt(t -> -t.sign)).forEach(tooltip -> {
            FormFairTypeLabel modifierLabel = new FormFairTypeLabel("", modifierLootContent.getWidth() / 2, 0);
            modifierLabel.setFontOptions(new FontOptions(fontSize));
            modifierLabel.setMaxWidth(modifierLootContent.getWidth() - 10);
            modifierLabel.setCustomFairType(tooltip.toFairType(modifierLabel.getFontOptions(), false));
            modifierLabel.setTextAlign(FairType.TextAlign.CENTER);
            modifierLabel.setColor(tooltip.getTextColor());
            modifierLootContent.addComponent(modifierLootFlow.nextY(modifierLabel, 2));
         });
      }

      for (UniqueIncursionModifier modifier : this.uniqueIncursionModifiers) {
         GameMessageBuilder modifiersBuilder = new GameMessageBuilder();
         FormFairTypeLabel uniqueModifierLabel = new FormFairTypeLabel(modifiersBuilder, lootContent.getWidth() / 2, 0);
         uniqueModifierLabel.setFontOptions(new FontOptions(fontSize));
         uniqueModifierLabel.setMaxWidth(lootContent.getWidth() - 10);
         uniqueModifierLabel.setParsers(
            TypeParsers.GAME_COLOR, TypeParsers.InputIcon(uniqueModifierLabel.getFontOptions()), TypeParsers.ItemIcon(fontSize), TypeParsers.MobIcon(fontSize)
         );
         uniqueModifierLabel.setTextAlign(FairType.TextAlign.CENTER);
         uniqueModifierLabel.setText(modifier.getModifierDescription());
         uniqueModifierLabel.setColor(Settings.UI.incursionModifierOrange);
         lootContent.addComponent(lootFlow.nextY(uniqueModifierLabel, 4));
      }

      lootContent.setHeight(lootFlow.next());
      if (!lootContent.isEmpty()) {
         content.addComponent(
            flow.nextY(
               new FormLocalLabel(
                  "ui", "incursionmodifiers", new FontOptions(headlineFontSize), 0, content.getMinContentWidth() / 2, 0, content.getMinContentWidth() - 10
               ),
               2
            )
         );
         content.addComponent(flow.nextY(lootContent, 14));
      }

      if (GlobalData.isDevMode() && GlobalData.debugCheatActive()) {
         flow.next(14);
         content.addComponent(flow.nextY(new FormLabel("Debug info:", new FontOptions(16), 0, content.getMinContentWidth() / 2, 0), 4));
         content.addComponent(flow.nextY(new FormLabel("UniqueID: " + this.getUniqueID(), new FontOptions(12), 0, content.getMinContentWidth() / 2, 0), 4));
         content.addComponent(flow.nextY(new FormLabel("Difficulty: " + this.difficulty, new FontOptions(12), 0, content.getMinContentWidth() / 2, 0), 4));
      }

      flow.next(2);
      content.setContentBox(new Rectangle(content.getWidth(), flow.next()));
      content.setScroll(0, 0);
   }

   protected abstract Iterable<FairType> getLoot(IncursionData var1, FontOptions var2);

   public void getAndSetupRewardLabels(IncursionRewardGetter rewardsChosen, int fontSize, Form content, FormFlow flow) {
      for (InventoryItem rewardItem : rewardsChosen.getRewards(true)) {
         FormFairTypeLabel label = new FormFairTypeLabel("", content.getWidth() / 2, 0);
         GameBlackboard tooltipBlackboard = new GameBlackboard().set("hideModifierAndRewards", true);
         label.setParsers(
            TypeParsers.GAME_COLOR,
            TypeParsers.ItemIcon(fontSize, true, fairItemGlyph -> fairItemGlyph.setTooltipBlackboard(tooltipBlackboard)),
            TypeParsers.MobIcon(fontSize)
         );
         label.setFontOptions(new FontOptions(fontSize));
         label.setMaxWidth(content.getWidth() - 10);
         int displayMinAmount = rewardItem.getGndData().getInt("incursionDisplayMinAmount", -1);
         int displayMaxAmount = rewardItem.getGndData().getInt("incursionDisplayMaxAmount", -1);
         String prefix;
         if (displayMinAmount >= 0) {
            prefix = "" + displayMinAmount;
            if (displayMaxAmount > displayMinAmount) {
               prefix = prefix + "-" + displayMaxAmount + " ";
            } else {
               prefix = prefix + "+ ";
            }
         } else {
            prefix = "";
         }

         if (rewardItem.item instanceof GatewayTabletItem) {
            String tier = Localization.translate("item", "tier", "tiernumber", GatewayTabletItem.getIncursionData(rewardItem).getTabletTier());
            label.setText(
               TypeParsers.getItemParseString(rewardItem)
                  + " "
                  + prefix
                  + tier
                  + " "
                  + GatewayTabletItem.getIncursionData(rewardItem).getIncursionBiome().displayName.translate()
            );
         } else if (rewardItem.item instanceof TrinketItem) {
            label.setText(TypeParsers.getItemParseString(rewardItem) + " " + prefix + Localization.translate("item", rewardItem.item.getStringID()));
         } else if (!(rewardItem.item instanceof ToolItem) && !(rewardItem.item instanceof ArmorItem)) {
            if (rewardItem.item instanceof CoinItem) {
               rewardItem.getGndData().setBoolean("showCoinStackIcon", true);
               label.setText(prefix + TypeParsers.getItemParseString(rewardItem) + " " + rewardItem.getItemDisplayName());
            }

            label.setText(TypeParsers.getItemParseString(rewardItem) + " " + prefix + rewardItem.getItemDisplayName());
         } else {
            int itemTier = (int)rewardItem.item.getUpgradeTier(rewardItem);
            if (itemTier > 0) {
               String tier = Localization.translate("item", "tier", "tiernumber", itemTier);
               label.setText(
                  TypeParsers.getItemParseString(rewardItem) + " " + prefix + tier + " " + Localization.translate("item", rewardItem.item.getStringID())
               );
            } else {
               label.setText(TypeParsers.getItemParseString(rewardItem) + " " + prefix + Localization.translate("item", rewardItem.item.getStringID()));
            }
         }

         label.setTextAlign(FairType.TextAlign.CENTER);
         content.addComponent(flow.nextY(label, 2));
         label.setColor(Settings.UI.incursionModifierOrange);
      }
   }

   public int getLootCount() {
      int counter = 0;

      for (FairType loot : this.getLoot(this, new FontOptions(0))) {
         counter++;
      }

      ArrayList<FairType> privateDropsDisplay = this.biome.getPrivateDropsDisplay(new FontOptions(0));
      if (privateDropsDisplay != null) {
         counter += privateDropsDisplay.size();
      }

      return counter;
   }

   protected FairType getItemMessage(InventoryItem item, FontOptions fontOptions) {
      FairType fairType = new FairType();
      fairType.append(new FairItemGlyph(fontOptions.getSize(), item));
      fairType.append(fontOptions, item.getItemDisplayName());
      return fairType;
   }

   protected FairType getItemMessage(Item item, FontOptions fontOptions) {
      return this.getItemMessage(new InventoryItem(item), fontOptions);
   }

   @Override
   public GameTooltips getOpenButtonTooltips(FallenAltarContainer container) {
      return null;
   }

   @Override
   public String getCanOpenError(FallenAltarContainer container) {
      return this.biome.getCanOpenError(this, container);
   }

   @Override
   public void onOpened(FallenAltarContainer container, ServerClient client) {
      if (client != null) {
         client.newStats.opened_incursions.add(this);
      }
   }

   @Override
   public void onCompleted(ServerClient client) {
      if (client != null) {
         client.newStats.completed_incursions.add(this);
         client.storyManager.streamObjectives(IncursionCompletedObjectiveListener.class).forEach(objective -> objective.onIncursionCompleted(this));
      }
   }

   @Override
   public void onClosed(FallenAltarObjectEntity altar, ServerClient client) {
   }

   @Override
   public IncursionLevel getNewIncursionLevel(
      FallenAltarObjectEntity altar, LevelIdentifier identifier, Server server, WorldEntity worldEntity, AltarData altarData
   ) {
      return this.biome.getNewIncursionLevel(altar, identifier, this, server, worldEntity, altarData);
   }

   @Override
   public Stream<ModifierValue<?>> getDefaultLevelModifiers() {
      if (!this.modifiersInitialized) {
         this.initModifiers();
      }

      return this.levelModifiers.stream();
   }

   @Override
   public ArrayList<UniqueIncursionModifier> getUniqueIncursionModifiers() {
      if (!this.modifiersInitialized) {
         this.initModifiers();
      }

      return this.uniqueIncursionModifiers;
   }

   @Override
   public IncursionRewardGetter getPlayerPersonalIncursionCompleteRewards() {
      if (!this.modifiersInitialized) {
         this.initModifiers();
      }

      return isForDisplayOnly -> {
         GameRandom lootRandom = new GameRandom(this.getUniqueID()).nextSeeded(77 * this.getTabletTier());
         ArrayList<InventoryItem> items = new ArrayList<>();
         this.playerPersonalIncursionCompleteRewards.addItems(items, lootRandom, 1.0F, isForDisplayOnly ? "displayOnly" : null);
         return items;
      };
   }

   @Override
   public IncursionRewardGetter getPlayerSharedIncursionCompleteRewards() {
      if (!this.modifiersInitialized) {
         this.initModifiers();
      }

      return isForDisplayOnly -> {
         GameRandom lootRandom = new GameRandom(this.getUniqueID()).nextSeeded(155 * this.getTabletTier());
         ArrayList<InventoryItem> items = new ArrayList<>();
         this.playerSharedIncursionCompleteRewards.addItems(items, lootRandom, 1.0F, isForDisplayOnly ? "displayOnly" : null);
         return items;
      };
   }

   private static class IncursionModifier {
      public final Modifier<Float> modifier;
      public boolean isLevelModifier;
      public boolean invert;
      public float value;
      public int percent;

      public IncursionModifier(Modifier<Float> modifier, boolean isLevelModifier, boolean invert, float value, int percent) {
         this.modifier = modifier;
         this.isLevelModifier = isLevelModifier;
         this.invert = invert;
         this.value = value;
         this.percent = percent;
      }

      public void apply(Collection<ModifierValue<?>> levelModifiers, Collection<ModifierValue<?>> playerModifiers) {
         float modifierValue = this.percent / 100.0F;
         if (this.invert) {
            modifierValue = -modifierValue;
         }

         if (this.isLevelModifier) {
            levelModifiers.add(new ModifierValue<>(this.modifier, modifierValue));
         } else {
            playerModifiers.add(new ModifierValue<>(this.modifier, modifierValue));
         }
      }
   }
}
