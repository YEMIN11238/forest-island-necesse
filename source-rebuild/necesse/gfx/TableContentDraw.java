package necesse.gfx;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.BiConsumer;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;

public class TableContentDraw {
   public final TableContentDraw.ColumnWidths colWidths;
   private int totalHeight;
   private final LinkedList<TableContentDraw.TableRow> rows = new LinkedList<>();

   public TableContentDraw(TableContentDraw.ColumnWidths colWidths) {
      this.colWidths = colWidths;
   }

   public TableContentDraw() {
      this(new TableContentDraw.ColumnWidths());
   }

   public void setMinimumColumnWidth(int column, int width) {
      this.colWidths.setMinimumWidth(column, width);
   }

   public TableContentDraw.TableRow newRow() {
      TableContentDraw.TableRow row = new TableContentDraw.TableRow();
      this.rows.add(row);
      return row;
   }

   public int getColumnWidth(int column) {
      return this.colWidths.getWidth(column);
   }

   public int getWidth() {
      return this.colWidths.getTotalWidth();
   }

   public int getHeight() {
      return this.totalHeight;
   }

   public void draw(int x, int y) {
      for (TableContentDraw.TableRow row : this.rows) {
         y += row.draw(x, y);
      }
   }

   public static void drawSeries(int x, int y, Iterable<TableContentDraw> tables) {
      for (TableContentDraw table : tables) {
         table.draw(x, y);
         y += table.getHeight();
      }
   }

   public static void drawSeries(int x, int y, TableContentDraw... tables) {
      drawSeries(x, y, Arrays.asList(tables));
   }

   public static class ColumnWidths {
      private int totalWidth;
      private int[] widths = new int[0];

      public void setMinimumWidth(int column, int width) {
         if (column >= this.widths.length) {
            this.widths = Arrays.copyOf(this.widths, column + 1);
         }

         if (width > this.widths[column]) {
            this.totalWidth = this.totalWidth + (width - this.widths[column]);
            this.widths[column] = width;
         }
      }

      public int getWidth(int column) {
         return column >= this.widths.length ? 0 : this.widths[column];
      }

      public int getTotalWidth() {
         return this.totalWidth;
      }
   }

   private static class Field {
      private final BiConsumer<Integer, Integer> drawLogic;
      private final int drawWidth;
      private final int drawHeight;
      private final boolean centeredX;
      private final boolean centeredY;

      public Field(BiConsumer<Integer, Integer> drawLogic, int drawWidth, int drawHeight, boolean centeredX, boolean centeredY) {
         this.drawLogic = drawLogic;
         this.drawWidth = drawWidth;
         this.drawHeight = drawHeight;
         this.centeredX = centeredX;
         this.centeredY = centeredY;
      }

      public void draw(int boxX, int boxY, int boxWidth, int boxHeight) {
         int offsetX = this.centeredX ? boxWidth / 2 - this.drawWidth / 2 : 0;
         int offsetY = this.centeredY ? boxHeight / 2 - this.drawHeight / 2 : 0;
         this.drawLogic.accept(boxX + offsetX, boxY + offsetY);
      }
   }

   public class TableRow {
      private final LinkedList<TableContentDraw.Field> fields = new LinkedList<>();
      private int maxHeight;

      private TableRow() {
      }

      public TableContentDraw.TableRow setMinimumHeight(int height) {
         if (height > this.maxHeight) {
            TableContentDraw.this.totalHeight += height - this.maxHeight;
            this.maxHeight = height;
         }

         return this;
      }

      public TableContentDraw.TableRow addColumn(BiConsumer<Integer, Integer> drawLogic, int drawWidth, int drawHeight, boolean centeredX, boolean centeredY) {
         int column = this.fields.size();
         TableContentDraw.this.setMinimumColumnWidth(column, drawWidth);
         this.fields.add(new TableContentDraw.Field(drawLogic, drawWidth, drawHeight, centeredX, centeredY));
         return this.setMinimumHeight(drawHeight);
      }

      public TableContentDraw.TableRow addColumn(BiConsumer<Integer, Integer> drawLogic, int drawWidth, int drawHeight) {
         return this.addColumn(drawLogic, drawWidth, drawHeight, false, false);
      }

      public TableContentDraw.TableRow addTextColumn(String text, FontOptions options, boolean centeredX, boolean centeredY, int paddingX, int paddingY) {
         return this.addColumn(
            (x, y) -> FontManager.bit.drawString(x.intValue(), y.intValue(), text, options),
            FontManager.bit.getWidthCeil(text, options) + paddingX,
            FontManager.bit.getHeightCeil(text, options) + paddingY,
            centeredX,
            centeredY
         );
      }

      public TableContentDraw.TableRow addTextColumn(String text, FontOptions options, boolean centeredX, boolean centeredY) {
         return this.addTextColumn(text, options, centeredX, centeredY, 0, 0);
      }

      public TableContentDraw.TableRow addTextColumn(String text, FontOptions options, int paddingX, int paddingY) {
         return this.addTextColumn(text, options, false, false, paddingX, paddingY);
      }

      public TableContentDraw.TableRow addTextColumn(String text, FontOptions options) {
         return this.addTextColumn(text, options, false, false);
      }

      public TableContentDraw.TableRow addEmptyColumn() {
         return this.addColumn((x, y) -> {}, 0, 0);
      }

      private int draw(int x, int y) {
         int column = 0;

         for (TableContentDraw.Field field : this.fields) {
            int boxWidth = TableContentDraw.this.getColumnWidth(column);
            field.draw(x, y, boxWidth, this.maxHeight);
            x += boxWidth;
            column++;
         }

         return this.maxHeight;
      }
   }
}
