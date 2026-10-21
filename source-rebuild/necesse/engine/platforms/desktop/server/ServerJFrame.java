package necesse.engine.platforms.desktop.server;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import necesse.engine.commands.AutoComplete;
import necesse.engine.commands.ParsedCommand;
import necesse.engine.loading.ServerLoader;
import necesse.engine.network.server.Server;

public class ServerJFrame extends JFrame {
   private final JTextArea console;
   private final JScrollPane consoleScroll;
   private final JTextField input;
   private final JButton enter;
   private final JLabel infoLabel;
   private int lastMessage;
   private final ArrayList<String> commands;
   private final ServerLoader serverLoader;
   private Server server;
   private List<AutoComplete> autoCompleteList;
   private int autoCompleteIndex;
   private String autocompleteStart;

   public ServerJFrame(ServerLoader serverLoader) throws HeadlessException {
      super("Necessenecesse/engine/server");
      this.serverLoader = serverLoader;
      JPanel panel;
      this.add(panel = new JPanel());
      panel.setLayout(new BoxLayout(panel, 2));
      panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
      JPanel infoPanel;
      panel.add(infoPanel = new JPanel());
      infoPanel.setLayout(new BoxLayout(infoPanel, 3));
      infoPanel.setPreferredSize(new Dimension(250, 32767));
      infoPanel.setMinimumSize(new Dimension(200, -32768));
      infoPanel.setMaximumSize(new Dimension(250, 32767));
      infoPanel.add(this.infoLabel = new JLabel());
      JPanel consolePanel;
      panel.add(consolePanel = new JPanel());
      consolePanel.setLayout(new BoxLayout(consolePanel, 3));
      this.console = new JTextArea();
      this.console.setEditable(false);
      consolePanel.add(this.consoleScroll = new JScrollPane(this.console));
      consolePanel.setPreferredSize(new Dimension(400, 200));
      JPanel inputPanel = new JPanel();
      inputPanel.setLayout(new BoxLayout(inputPanel, 2));
      inputPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
      inputPanel.setMaximumSize(new Dimension(32767, 0));
      inputPanel.add(Box.createHorizontalGlue());
      inputPanel.add(this.input = new JTextField());
      inputPanel.add(Box.createRigidArea(new Dimension(10, 0)));
      inputPanel.add(this.enter = new JButton("Enter"));
      consolePanel.add(inputPanel);
      this.setContentPane(panel);
      this.setPreferredSize(new Dimension(600, 450));
      this.setDefaultCloseOperation(0);
      this.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            if (ServerJFrame.this.server != null) {
               ServerJFrame.this.server.stop(s -> {
                  try {
                     Thread.sleep(2000L);
                  } catch (InterruptedException e1) {
                     e1.printStackTrace();
                  }

                  System.exit(0);
               });
            } else {
               System.exit(0);
            }
         }
      });
      this.enter.addActionListener(e -> this.submitCommand(this.input.getText()));
      this.input
         .addKeyListener(
            new KeyAdapter() {
               @Override
               public void keyPressed(KeyEvent e) {
                  if (e.getKeyCode() == 10) {
                     ServerJFrame.this.submitCommand(ServerJFrame.this.input.getText());
                     ServerJFrame.this.autoCompleteIndex = -1;
                  } else if (e.getKeyCode() == 38) {
                     if (ServerJFrame.this.lastMessage < ServerJFrame.this.commands.size() - 1) {
                        ServerJFrame.this.lastMessage++;
                        ServerJFrame.this.input.setText(ServerJFrame.this.commands.get(ServerJFrame.this.lastMessage));
                        ServerJFrame.this.input.moveCaretPosition(ServerJFrame.this.input.getText().length());
                        e.consume();
                        ServerJFrame.this.autoCompleteIndex = -1;
                     }
                  } else if (e.getKeyCode() == 40) {
                     if (ServerJFrame.this.lastMessage >= 0) {
                        ServerJFrame.this.lastMessage--;
                        if (ServerJFrame.this.lastMessage >= 0) {
                           ServerJFrame.this.input.setText(ServerJFrame.this.commands.get(ServerJFrame.this.lastMessage));
                        } else {
                           ServerJFrame.this.input.setText("");
                        }

                        ServerJFrame.this.input.moveCaretPosition(ServerJFrame.this.input.getText().length());
                        e.consume();
                        ServerJFrame.this.autoCompleteIndex = -1;
                     }
                  } else if (e.getKeyCode() == 9) {
                     if (ServerJFrame.this.server != null) {
                        String text = ServerJFrame.this.input.getText();
                        boolean startWithSlash = text.startsWith("/");
                        if (startWithSlash) {
                           text = text.substring(1);
                        }

                        int nextSpace = text.indexOf(" ", ServerJFrame.this.input.getCaretPosition() - (startWithSlash ? 1 : 0));
                        if (ServerJFrame.this.autoCompleteIndex < 0) {
                           ServerJFrame.this.autocompleteStart = text.substring(0, nextSpace == -1 ? text.length() : nextSpace);
                           ServerJFrame.this.autoCompleteList = ServerJFrame.this.server
                              .commandsManager
                              .autocomplete(new ParsedCommand(ServerJFrame.this.autocompleteStart), null);
                           ServerJFrame.this.autoCompleteIndex = 0;
                        } else {
                           ServerJFrame.this.autoCompleteIndex++;
                        }

                        if (!ServerJFrame.this.autoCompleteList.isEmpty()) {
                           String autoText = ServerJFrame.this.autoCompleteList
                              .get(ServerJFrame.this.autoCompleteIndex % ServerJFrame.this.autoCompleteList.size())
                              .getFullCommand(ServerJFrame.this.autocompleteStart);
                           String end = text.substring(nextSpace == -1 ? text.length() : nextSpace);
                           ServerJFrame.this.input.setText((startWithSlash ? "/" : "") + autoText + end);
                           ServerJFrame.this.input.setCaretPosition(autoText.length() + (startWithSlash ? 1 : 0));
                        }

                        e.consume();
                     }
                  } else {
                     ServerJFrame.this.autoCompleteIndex = -1;
                  }
               }
            }
         );
      this.pack();
      this.setVisible(true);
      this.setInfoLines(new String[0]);
      this.input.requestFocus();
      this.input.setFocusTraversalKeysEnabled(false);
      this.lastMessage = -1;
      this.commands = new ArrayList<>();
   }

   public void submitCommand(String command) {
      if (!command.equals("")) {
         this.commands.add(0, command);
      }

      this.input.setText("");
      this.input.requestFocus();
      this.lastMessage = -1;
      if (command.equals("clear")) {
         this.clearConsole();
      } else {
         this.serverLoader.handleCommand(command);
      }
   }

   public void setInfoLines(String[] lines) {
      this.infoLabel.setText("<html><span>---------- Server info ---------<br>" + String.join("<br>", lines) + "</span>");
   }

   public void clearConsole() {
      this.console.setText(null);
      this.console.setCaretPosition(this.console.getText().length());
   }

   public void setServer(Server server) {
      this.server = server;
   }

   public void setInfoLines(List<String> lines) {
      this.setInfoLines(lines.toArray(new String[0]));
   }

   public void writeConsole(String message) {
      this.console.append(message + "\n");
      this.console.setCaretPosition(this.console.getText().length());
   }
}
