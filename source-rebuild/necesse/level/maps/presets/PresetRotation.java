package necesse.level.maps.presets;

public enum PresetRotation {
   CLOCKWISE(1),
   ANTI_CLOCKWISE(3),
   HALF_180(2);

   public final int dirOffset;

   PresetRotation(int dirOffset) {
      this.dirOffset = dirOffset;
   }

   public static PresetRotation toRotationAngle(int rightAngles) {
      rightAngles %= 4;
      if (rightAngles == 0) {
         return null;
      } else if (rightAngles == 3 || rightAngles == -1) {
         return ANTI_CLOCKWISE;
      } else {
         return rightAngles != -3 && rightAngles != 1 ? HALF_180 : CLOCKWISE;
      }
   }

   public static PresetRotation addRotations(PresetRotation last, PresetRotation rotation) {
      if (last == null) {
         return rotation;
      } else {
         return rotation == null ? last : toRotationAngle(last.dirOffset + rotation.dirOffset);
      }
   }
}
