package localforest.visual;

/** Protect the accepted dusk/night palette while adding daytime material grading. */
public final class DaylightMaterialTest {
    public static void main(String[] args) {
        for (int minute = 0; minute < 24 * 60; minute++) {
            float hour = minute / 60f;
            float amount = DaylightMaterials.daylightAtHour(hour);
            check(amount >= 0 && amount <= 1, "Day blend left its valid range");
            if (hour <= 7 || hour >= 18) check(amount == 0, "Night source colors changed at " + hour);
            if (hour >= 8 && hour <= 17) check(amount == 1, "Full daytime palette missing at " + hour);
        }
        check(DaylightMaterials.amount(null) == 0, "Unbound sprites must not inherit a daytime tint");
        check(DaylightMaterials.daylightAtHour(Float.NaN) == 0, "Invalid world time must keep native color");
        check(Math.abs(DaylightMaterials.daylightAtHour(7.99f) - DaylightMaterials.daylightAtHour(8f)) < .001f,
            "Morning material transition stepped");
        check(Math.abs(DaylightMaterials.daylightAtHour(17.99f) - DaylightMaterials.daylightAtHour(18f)) < .001f,
            "Evening material transition stepped");
        System.out.println("Daylight material night isolation and continuous day transitions passed");
    }
    private static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); }
}
