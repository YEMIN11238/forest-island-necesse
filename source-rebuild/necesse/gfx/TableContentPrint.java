package necesse.gfx;

import java.util.Arrays;
import java.util.LinkedList;

public class TableContentPrint {
   public final TableContentPrint.ColumnWidths colWidths;
   private final LinkedList<TableContentPrint.TableRow> rows = new LinkedList<>();
   protected int defaultPadding;

   public TableContentPrint(TableContentPrint.ColumnWidths colWidths, int defaultPadding) {
      this.colWidths = colWidths;
      this.defaultPadding = defaultPadding;
   }

   public TableContentPrint(int defaultPadding) {
      this(new TableContentPrint.ColumnWidths(), defaultPadding);
   }

   public void setMinimumColumnWidth(int column, int width) {
      this.colWidths.setMinimumWidth(column, width);
   }

   public TableContentPrint.TableRow newRow() {
      TableContentPrint.TableRow row = new TableContentPrint.TableRow();
      this.rows.add(row);
      return row;
   }

   public int getColumnWidth(int column) {
      return this.colWidths.getWidth(column);
   }

   public int getWidth() {
      return this.colWidths.getTotalWidth();
   }

   public static void printWhiteSpaces(int count) {
      for (int i = 0; i < count; i++) {
         System.out.print(" ");
      }
   }

   public static String prefixWhiteSpaces(String text, int whiteSpaces) {
      if (whiteSpaces <= 0) {
         return text;
      }

      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < whiteSpaces; i++) {
         sb.append(" ");
      }

      sb.append(text);
      return sb.toString();
   }

   public void print(String prefix) {
      for (TableContentPrint.TableRow row : this.rows) {
         row.print(prefix);
      }
   }

   public void print() {
      this.print("");
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
      private final String text;
      private final boolean centered;

      public Field(String text, boolean centered) {
         this.text = text;
         this.centered = centered;
      }

      public void print(int columnWidth) {
         int offsetX = this.centered ? columnWidth / 2 - this.text.length() / 2 : 0;
         TableContentPrint.printWhiteSpaces(offsetX);
         System.out.print(this.text);
         TableContentPrint.printWhiteSpaces(columnWidth - offsetX - this.text.length());
      }
   }

   public class TableRow {
      private final LinkedList<TableContentPrint.Field> fields = new LinkedList<>();

      private TableRow() {
      }

      protected TableContentPrint.TableRow addColumn(Object text, boolean centered) {
         int column = this.fields.size();
         String string = String.valueOf(text);
         TableContentPrint.this.setMinimumColumnWidth(column, string.length() + TableContentPrint.this.defaultPadding);
         this.fields.add(new TableContentPrint.Field(string, centered));
         return this;
      }

      public TableContentPrint.TableRow addColumn(Object text, boolean centered, int padding) {
         return this.addColumn(TableContentPrint.prefixWhiteSpaces(String.valueOf(text), padding), centered);
      }

      public TableContentPrint.TableRow addColumn(Object text, int padding) {
         return this.addColumn(text, false, padding);
      }

      public TableContentPrint.TableRow addColumn(Object text) {
         return this.addColumn(text, false, 0);
      }

      public TableContentPrint.TableRow addEmptyColumn() {
         return this.addColumn("", false);
      }

      private void print(String prefix) {
         System.out.print(prefix);
         int column = 0;

         for (TableContentPrint.Field field : this.fields) {
            field.print(TableContentPrint.this.getColumnWidth(column));
            column++;
         }

         System.out.println();
      }
   }
}
