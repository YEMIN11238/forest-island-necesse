package necesse.engine;

import java.util.Arrays;
import java.util.HashSet;

public class GameVersion {
   private static final HashSet<Character> ints = new HashSet<>(Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'));
   public final String versionString;
   private final int[] versions;

   public GameVersion(String versionString) {
      this.versionString = versionString;
      String[] split = versionString.split("\\.");
      this.versions = new int[split.length];

      for (int i = 0; i < split.length; i++) {
         try {
            int j = 0;

            while (j < split[i].length() && ints.contains(split[i].charAt(j))) {
               j++;
            }

            this.versions[i] = Integer.parseInt(split[i].substring(0, j));
         } catch (NumberFormatException e) {
            this.versions[i] = Integer.MIN_VALUE;
         }
      }
   }

   public boolean isLaterThan(GameVersion other) {
      int max = Math.max(this.versions.length, other.versions.length);

      for (int i = 0; i < max; i++) {
         if (i >= this.versions.length) {
            return false;
         }

         if (i >= other.versions.length) {
            return true;
         }

         if (this.versions[i] < other.versions[i]) {
            return false;
         }

         if (this.versions[i] > other.versions[i]) {
            return true;
         }
      }

      return false;
   }

   public boolean isLaterThan(String versionString) {
      return this.isLaterThan(new GameVersion(versionString));
   }

   public boolean isEarlierThan(GameVersion other) {
      int max = Math.max(this.versions.length, other.versions.length);

      for (int i = 0; i < max; i++) {
         if (i >= this.versions.length) {
            return true;
         }

         if (i >= other.versions.length) {
            return false;
         }

         if (this.versions[i] < other.versions[i]) {
            return true;
         }

         if (this.versions[i] > other.versions[i]) {
            return false;
         }
      }

      return false;
   }

   public boolean isEarlierThan(String versionString) {
      return this.isEarlierThan(new GameVersion(versionString));
   }

   @Override
   public String toString() {
      return this.versionString;
   }
}
