package necesse.reports;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.DocumentFilter.FilterBypass;
import necesse.engine.input.controller.ControllerInput;
import necesse.engine.localization.Localization;

public class RecentCrashJFrame extends JFrame {
   private JTextArea console;
   private JButton dontSendReport;
   private JButton sendReport;

   public static void main(String[] args) {
      new RecentCrashJFrame(new CrashReportData(Collections.singletonList(new Exception()), null, null, "TestCrash"));
   }

   public static void showAndBlock(CrashReportData crashData) {
      RecentCrashJFrame crashFrame = new RecentCrashJFrame(crashData);
      crashFrame.setVisible(true);
      crashFrame.requestFocus();

      while (Arrays.stream(JFrame.getFrames()).anyMatch(Component::isVisible)) {
         try {
            Thread.sleep(500L);
         } catch (InterruptedException e) {
            throw new RuntimeException(e);
         }
      }
   }

   private RecentCrashJFrame(CrashReportData crashData) {
      super("Necesse recent crash report");
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
      String infoText = getLocale(
         "ui", "recentcrashsorry", "Sorry! It looks like your game recently crashed :(\nPlease help the development by sending the crash report!"
      );
      infoLabel.setText("<html><div>" + infoText + "</div></html>");
      JPanel consolePanel;
      panel.add(consolePanel = new JPanel());
      consolePanel.setLayout(new BoxLayout(consolePanel, 3));
      this.console = new JTextArea();
      this.console.setEditable(false);
      consolePanel.add(new JScrollPane(this.console));
      consolePanel.setPreferredSize(new Dimension(400, 200));
      this.console.setText(crashData.getFullReport(null));
      JPanel inputPanel = new JPanel();
      inputPanel.setLayout(new BoxLayout(inputPanel, 2));
      inputPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
      inputPanel.setMaximumSize(new Dimension(32767, 0));
      inputPanel.add(this.sendReport = new JButton(getLocale("ui", "sendreport", "Send report")));
      inputPanel.add(Box.createRigidArea(new Dimension(5, 0)));
      inputPanel.add(this.dontSendReport = new JButton(getLocale("ui", "dontsendreport", "Don't send report")));
      consolePanel.add(inputPanel);
      this.sendReport
         .addActionListener(
            e -> {
               RecentCrashJFrame.DetailsFrame sendDialog = new RecentCrashJFrame.DetailsFrame(
                  this, crashData, getLocale("ui", "crashdetailshere", "Type details here...")
               );
               sendDialog.setVisible(true);
               sendDialog.requestFocus();
               this.dispose();
            }
         );
      this.dontSendReport.addActionListener(e -> this.dispose());
      this.setContentPane(panel);
      this.setPreferredSize(new Dimension(600, 450));
      this.setDefaultCloseOperation(2);
      ControllerInput.setActiveActionSet(ControllerInput.DESKTOP_CONTROLS);
      this.pack();
      this.setLocationRelativeTo(null);
      this.setVisible(true);
      this.requestFocus();
   }

   private static String getLocale(String category, String key, String errorDefault) {
      try {
         return Localization.translate(category, key).replace("\n", "<br>");
      } catch (Exception e) {
         return errorDefault.replace("\n", "<br>");
      }
   }

   private static class DetailsFrame extends JFrame {
      private JTextArea console;
      private JButton sendReport;

      public DetailsFrame(Frame previous, CrashReportData data, String userDetails) {
         super("Necesse crash report");
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
         String infoText = RecentCrashJFrame.getLocale(
            "ui", "crashgivedetails", "Please give details about what you were doing when the crash happened or any other details you think might be helpful!"
         );
         infoLabel.setText("<html><span>" + infoText + "</span>");
         JPanel consolePanel;
         panel.add(consolePanel = new JPanel());
         consolePanel.setLayout(new BoxLayout(consolePanel, 3));
         this.console = new JTextArea();
         this.console.setEditable(true);
         consolePanel.add(new JScrollPane(this.console));
         consolePanel.setPreferredSize(new Dimension(400, 200));
         DefaultStyledDocument doc = new DefaultStyledDocument();
         doc.setDocumentFilter(new RecentCrashJFrame.DocumentSizeFilter(500));
         this.console.setDocument(doc);
         this.console.setText(userDetails);
         JPanel inputPanel = new JPanel();
         inputPanel.setLayout(new BoxLayout(inputPanel, 2));
         inputPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
         inputPanel.setMaximumSize(new Dimension(32767, 0));
         inputPanel.add(this.sendReport = new JButton(RecentCrashJFrame.getLocale("ui", "sendreport", "Send report")));
         consolePanel.add(inputPanel);
         this.sendReport.addActionListener(e -> {
            RecentCrashJFrame.SendFrame sendFrame = new RecentCrashJFrame.SendFrame(this, data, this.console.getText());
            sendFrame.setVisible(true);
            sendFrame.requestFocus();
            this.dispose();
         });
         this.setContentPane(panel);
         this.setPreferredSize(new Dimension(400, 300));
         this.setDefaultCloseOperation(2);
         this.pack();
         this.setLocationRelativeTo(previous);
      }
   }

   private static class DocumentSizeFilter extends DocumentFilter {
      public int maxChars;

      public DocumentSizeFilter(int maxChars) {
         this.maxChars = maxChars;
      }

      @Override
      public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
         int docLength = fb.getDocument().getLength();
         if (docLength + string.length() <= this.maxChars) {
            super.insertString(fb, offset, string, attr);
         } else {
            int remainingChars = this.maxChars - docLength;
            super.insertString(fb, offset, string.substring(0, remainingChars), attr);
            Toolkit.getDefaultToolkit().beep();
         }
      }

      @Override
      public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
         int docLength = fb.getDocument().getLength();
         if (docLength + text.length() - length <= this.maxChars) {
            super.replace(fb, offset, length, text, attrs);
         } else {
            int remainingChars = this.maxChars - docLength;
            String substring = text.substring(0, remainingChars);
            super.replace(fb, offset, length, substring, attrs);
            Toolkit.getDefaultToolkit().beep();
         }
      }
   }

   private static class RetryFrame extends JFrame {
      public RetryFrame(Frame previous, String error, CrashReportData data, String userDetails) {
         super("Necesse crash report");
         JPanel infoPanel = new JPanel();
         infoPanel.setLayout(new BoxLayout(infoPanel, 2));
         infoPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
         JLabel infoLabel;
         infoPanel.add(infoLabel = new JLabel());
         infoLabel.setHorizontalAlignment(0);
         infoLabel.setText("<html><span>" + error + "</span>");
         JPanel inputPanel = new JPanel();
         inputPanel.setLayout(new BoxLayout(inputPanel, 2));
         inputPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
         inputPanel.setMaximumSize(new Dimension(32767, 0));
         JButton retryButton;
         inputPanel.add(retryButton = new JButton(RecentCrashJFrame.getLocale("ui", "sendreportretry", "Retry")));
         retryButton.addActionListener(e -> {
            RecentCrashJFrame.SendFrame sendFrame = new RecentCrashJFrame.SendFrame(this, data, userDetails);
            sendFrame.setVisible(true);
            sendFrame.requestFocus();
            this.dispose();
         });
         inputPanel.add(Box.createRigidArea(new Dimension(10, 0)));
         JButton closeButton;
         inputPanel.add(closeButton = new JButton(RecentCrashJFrame.getLocale("ui", "closebutton", "Close")));
         closeButton.addActionListener(e -> this.dispose());
         this.getContentPane().add(infoPanel, "Center");
         this.getContentPane().add(inputPanel, "Last");
         this.setPreferredSize(new Dimension(300, 200));
         this.setDefaultCloseOperation(2);
         this.pack();
         this.setLocationRelativeTo(previous);
      }
   }

   private static class SendFrame extends JFrame {
      public SendFrame(Frame previous, CrashReportData data, String userDetails) {
         super("Necesse crash report");
         JLabel infoLabel = new JLabel();
         infoLabel.setHorizontalAlignment(0);
         infoLabel.setVerticalAlignment(0);
         infoLabel.setText("<html><span>Sending report...</span>");
         this.getContentPane().add(infoLabel, "Center");
         this.setPreferredSize(new Dimension(300, 100));
         final AtomicBoolean interrupted = new AtomicBoolean(false);
         new Thread(() -> {
            String error = ReportUtils.sendCrashReport(data, userDetails);
            if (!interrupted.get()) {
               if (error != null) {
                  error = error.replace("\n", "</br>");
                  RecentCrashJFrame.RetryFrame retryFrame = new RecentCrashJFrame.RetryFrame(this, error, data, userDetails);
                  retryFrame.setVisible(true);
                  retryFrame.requestFocus();
               } else {
                  RecentCrashJFrame.ThankYouFrame thankYouFrame = new RecentCrashJFrame.ThankYouFrame(this);
                  thankYouFrame.setVisible(true);
                  thankYouFrame.requestFocus();
               }
            }

            this.dispose();
         }).start();
         this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
               interrupted.set(true);
            }
         });
         this.setDefaultCloseOperation(2);
         this.pack();
         this.setLocationRelativeTo(previous);
      }
   }

   private static class ThankYouFrame extends JFrame {
      public ThankYouFrame(Frame previous) {
         super("Necesse crash report");
         JPanel infoPanel = new JPanel();
         infoPanel.setLayout(new BoxLayout(infoPanel, 2));
         infoPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
         JLabel infoLabel;
         infoPanel.add(infoLabel = new JLabel());
         infoLabel.setHorizontalAlignment(0);
         String infoText = RecentCrashJFrame.getLocale("ui", "sendreportthanks", "Thank you!");
         infoLabel.setText("<html><span>" + infoText + "</span>");
         JPanel inputPanel = new JPanel();
         inputPanel.setLayout(new BoxLayout(inputPanel, 2));
         inputPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
         inputPanel.setMaximumSize(new Dimension(32767, 0));
         JButton closeButton;
         inputPanel.add(closeButton = new JButton(RecentCrashJFrame.getLocale("ui", "closebutton", "Close")));
         closeButton.addActionListener(e -> this.dispose());
         closeButton.setHorizontalAlignment(0);
         this.getContentPane().add(infoPanel, "Center");
         this.getContentPane().add(inputPanel, "Last");
         this.setPreferredSize(new Dimension(300, 150));
         this.setDefaultCloseOperation(2);
         this.pack();
         this.setLocationRelativeTo(previous);
      }
   }
}
