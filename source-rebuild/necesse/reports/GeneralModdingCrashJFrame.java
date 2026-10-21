package necesse.reports;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import necesse.engine.localization.Localization;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.ModListData;
import necesse.engine.modLoader.ModLoader;

public class GeneralModdingCrashJFrame extends JFrame {
   public static void main(String[] args) {
      new GeneralModdingCrashJFrame(new CrashReportData(Collections.singletonList(new RuntimeException()), null, null, "Test"));
   }

   public GeneralModdingCrashJFrame(CrashReportData crashData) {
      super("Necesse modded crash report");
      JPanel panel;
      this.add(panel = new JPanel());
      panel.setLayout(new BoxLayout(panel, 1));
      panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      JPanel infoPanel;
      panel.add(infoPanel = new JPanel());
      infoPanel.setLayout(new BoxLayout(infoPanel, 2));
      infoPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
      JLabel infoLabel;
      infoPanel.add(infoLabel = new JLabel());
      String infoText = getLocale("ui", "moddederror", "An error occurred while running mods.");
      infoLabel.setText("<html><div>" + infoText + "</div></html>");
      JPanel consolePanel;
      panel.add(consolePanel = new JPanel());
      consolePanel.setLayout(new BoxLayout(consolePanel, 3));
      JTextArea console = new JTextArea();
      console.setEditable(false);
      consolePanel.add(new JScrollPane(console));
      consolePanel.setPreferredSize(new Dimension(400, 200));
      console.setText(crashData.getFullReport(null));
      JPanel inputPanel = new JPanel();
      inputPanel.setLayout(new BoxLayout(inputPanel, 2));
      inputPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
      inputPanel.setMaximumSize(new Dimension(32767, 0));
      JButton disableAll;
      inputPanel.add(disableAll = new JButton(getLocale("ui", "moderrordisableall", "Disable all mods")));
      inputPanel.add(Box.createRigidArea(new Dimension(5, 0)));
      disableAll.addActionListener(e -> {
         ArrayList<ModListData> list = new ArrayList<>();

         for (LoadedMod m : ModLoader.getAllMods()) {
            ModListData data = new ModListData(m);
            data.enabled = false;
            list.add(data);
         }

         ModLoader.saveModListSettings(list);
         this.dispose();
      });
      JButton quit;
      inputPanel.add(quit = new JButton(getLocale("ui", "closebutton", "Close")));
      consolePanel.add(inputPanel);
      quit.addActionListener(e -> this.dispose());
      this.setContentPane(panel);
      this.setPreferredSize(new Dimension(600, 450));
      this.setDefaultCloseOperation(2);
      this.pack();
      this.setLocationRelativeTo(null);
      this.setVisible(true);
      this.requestFocus();
   }

   private static String getLocale(Supplier<String> stringSupplier, String errorDefault) {
      try {
         return stringSupplier.get().replace("\n", "<br>");
      } catch (Exception e) {
         return errorDefault.replace("\n", "<br>");
      }
   }

   private static String getLocale(String localeCategory, String localeKey, String errorDefault) {
      return getLocale(() -> Localization.translate(localeCategory, localeKey), errorDefault);
   }
}
