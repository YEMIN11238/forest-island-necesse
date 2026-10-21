package necesse.gfx.fairType.parsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveComponent;
import necesse.engine.save.SaveSyntaxException;
import necesse.gfx.fairType.FairGlyph;
import necesse.gfx.fairType.FairItemGlyph;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.Item;

public class TypeItemParser extends TypeParser<TypeItemParser.ItemParserResult> {
   public static final Pattern ITEM_PATTERN = Pattern.compile("\\[item=(\\w+)](\\{.+})?");
   public static final Pattern ITEMS_PATTERN = Pattern.compile("\\[items=((?:\\w+(?:\\{.+})?|,)+)]");
   public static final Pattern ITEM_IN_ITEMS_PATTERN = Pattern.compile("(\\w+)(\\{.+})?");
   public final int size;
   public final boolean allowGND;
   public final Function<FairItemGlyph, FairItemGlyph> modder;

   public TypeItemParser(int size, boolean allowGND, Function<FairItemGlyph, FairItemGlyph> modder) {
      this.size = size;
      this.allowGND = allowGND;
      this.modder = modder;
   }

   public TypeItemParser.ItemParserResult getMatchResult(FairGlyph[] glyphs, int startIndex) {
      StringBuilder sBuilder = new StringBuilder();

      for (FairGlyph glyph : glyphs) {
         sBuilder.append(glyph.getCharacter());
      }

      Matcher multiMatcher = ITEMS_PATTERN.matcher(sBuilder.toString());
      Matcher singleMatcher = ITEM_PATTERN.matcher(sBuilder.toString());
      boolean multiMatchFound = multiMatcher.find(startIndex);
      boolean singleMatchFound = singleMatcher.find(startIndex);
      if (multiMatchFound && singleMatchFound && singleMatcher.start(0) < multiMatcher.start(0)) {
         multiMatchFound = false;
      }

      if (!multiMatchFound) {
         if (singleMatchFound) {
            String stringID = singleMatcher.group(1);
            int end = singleMatcher.end(1) + 1;

            try {
               String fullMatch = singleMatcher.group(0);
               int sectionStop = SaveComponent.getSectionStop(fullMatch, '[', ']', 0);
               stringID = fullMatch.substring(singleMatcher.start(1) - singleMatcher.start(0), sectionStop);
               end = singleMatcher.start(0) + sectionStop + 1;
            } catch (SaveSyntaxException var23) {
            }

            String gndString = null;
            if (this.allowGND && singleMatcher.groupCount() > 1 && singleMatcher.group(2) != null) {
               String gndGroup = singleMatcher.group(2);

               try {
                  int itemGNDSectionStop = SaveComponent.getSectionStop(gndGroup, '{', '}', 0);
                  gndString = gndGroup.substring(0, itemGNDSectionStop + 1);
                  end = singleMatcher.start(2) + itemGNDSectionStop + 1;
               } catch (SaveSyntaxException var22) {
               }
            }

            InventoryItem inventoryItem = this.constructItem(stringID, gndString);
            return new TypeItemParser.ItemParserResult(singleMatcher.start(), end, inventoryItem);
         } else {
            return null;
         }
      } else {
         String itemsData = multiMatcher.group(1);
         int end = multiMatcher.end(1) + 1;

         try {
            String fullMatch = multiMatcher.group(0);
            int sectionStop = SaveComponent.getSectionStop(fullMatch, '[', ']', 0);
            itemsData = fullMatch.substring(multiMatcher.start(1) - multiMatcher.start(0), sectionStop);
            end = multiMatcher.start(0) + sectionStop + 1;
         } catch (SaveSyntaxException var21) {
         }

         Matcher itemMatcher = ITEM_IN_ITEMS_PATTERN.matcher(itemsData);
         ArrayList<InventoryItem> items = new ArrayList<>();
         int itemStartIndex = 0;

         while (itemMatcher.find(itemStartIndex)) {
            String itemStringID = itemMatcher.group(1);
            int itemEnd = itemMatcher.end(1) + 1;
            String gndString = null;
            if (this.allowGND && itemMatcher.groupCount() > 1 && itemMatcher.group(2) != null) {
               String gndGroup = itemMatcher.group(2);

               try {
                  int itemGndSectionStop = SaveComponent.getSectionStop(gndGroup, '{', '}', 0);
                  gndString = gndGroup.substring(0, itemGndSectionStop + 1);
                  itemEnd = itemMatcher.start(2) + itemGndSectionStop + 1;
               } catch (SaveSyntaxException var20) {
               }
            }

            InventoryItem inventoryItem = this.constructItem(itemStringID, gndString);
            if (inventoryItem != null) {
               items.add(inventoryItem);
            }

            itemStartIndex = itemEnd;
            if (itemStartIndex >= itemsData.length()) {
               break;
            }
         }

         return new TypeItemParser.ItemParserResult(multiMatcher.start(), end, items);
      }
   }

   public FairGlyph[] parse(TypeItemParser.ItemParserResult result, FairGlyph[] oldGlyphs) {
      if (result.items.isEmpty()) {
         return oldGlyphs;
      }

      FairItemGlyph glyph = new FairItemGlyph(this.size, result.items);
      if (this.modder != null) {
         glyph = this.modder.apply(glyph);
      }

      return new FairGlyph[]{glyph};
   }

   public InventoryItem constructItem(String itemStringId, String gndSection) {
      Item item = ItemRegistry.getItem(itemStringId);
      if (item == null) {
         return null;
      }

      InventoryItem inventoryItem = new InventoryItem(item);
      if (gndSection != null) {
         try {
            LoadData gndLoad = new LoadData(gndSection);
            GNDItemMap gndData = new GNDItemMap(gndLoad);
            if (gndData != null) {
               inventoryItem.setGndData(gndData);
            }
         } catch (Exception var7) {
         }
      }

      return inventoryItem;
   }

   public static class ItemParserResult extends TypeParserResult {
      public final List<InventoryItem> items;

      public ItemParserResult(int start, int end, List<InventoryItem> items) {
         super(start, end);
         this.items = items;
      }

      public ItemParserResult(int start, int end, InventoryItem inventoryItem) {
         this(start, end, inventoryItem == null ? Collections.emptyList() : Collections.singletonList(inventoryItem));
      }

      @Override
      public String toString() {
         return super.toString() + "{start=" + this.start + ",end=" + this.end + "}{items=" + Arrays.toString(this.items.toArray()) + "}";
      }
   }
}
