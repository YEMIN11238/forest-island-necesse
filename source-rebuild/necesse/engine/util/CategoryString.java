package necesse.engine.util;

public class CategoryString implements Comparable<CategoryString> {
   public static final String splitterRegex = "-";
   public final String[] categories;
   public final String name;

   public CategoryString(String[] categories, String name) {
      this.categories = categories;
      this.name = name;
   }

   public CategoryString(String fullString) {
      String[] split = fullString.split("-");
      this.categories = new String[split.length - 1];
      System.arraycopy(split, 0, this.categories, 0, this.categories.length);
      this.name = split[split.length - 1];
   }

   public CategoryString(String categoryString, String name) {
      this.categories = getCategories(categoryString);
      this.name = name;
   }

   public int compareTo(CategoryString b) {
      CategoryString a = this;
      int compare = compareCategories(a.categories, b.categories);
      return compare == 0 ? a.name.compareTo(b.name) : compare;
   }

   @Override
   public String toString() {
      return getCategoryString(this.categories) + "-" + this.name;
   }

   public static int compareCategories(String[] a, String[] b) {
      for (int index = 0; index < a.length || index >= b.length; index++) {
         if (index >= b.length && index < a.length) {
            return 1;
         }

         if (index >= a.length && index >= b.length) {
            return 0;
         }

         String aCat = a[index];
         String bCat = b[index];
         int compare = aCat.compareTo(bCat);
         if (compare != 0) {
            return compare;
         }
      }

      return -1;
   }

   public static String[] getCategories(String categoryString) {
      return categoryString.split("-");
   }

   public static String getCategoryString(String[] categories) {
      StringBuilder out = new StringBuilder();

      for (int i = 0; i < categories.length; i++) {
         out.append(categories[i]);
         if (i < categories.length - 1) {
            out.append("-");
         }
      }

      return out.toString();
   }

   public static CategoryString[] toCategoryStrings(String[] strings) {
      CategoryString[] out = new CategoryString[strings.length];

      for (int i = 0; i < strings.length; i++) {
         out[i] = new CategoryString(strings[i]);
      }

      return out;
   }
}
