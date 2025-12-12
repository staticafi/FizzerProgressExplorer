package fizzer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Console extends JPanel {

    private JTextArea outputArea;
    private JTextField inputField;
    private ExecutionTree executionTree;

    public Console(ExecutionTree executionTree_) {
        executionTree = executionTree_;

        setLayout(new BorderLayout());

        final Font font = new Font("Monospaced", Font.PLAIN, ProgressExplorer.textFontSize);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        //outputArea.setFocusable(false);
        outputArea.setFont(font);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        inputField = new JTextField();
        inputField.setFont(font);
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String command = inputField.getText();
                inputField.setText("");

                print(">>> " + command);
                String result = handleCommand(command);
                print(result);
            }
        });

        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);

        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    inputField.requestFocus();
                }
            }
        });
    }

    private void print(String text) {
        outputArea.append(text + "\n");
    }

    public void clear() {
        outputArea.setText("");
        inputField.setText("");
    }

    public void onAnalysisChanged() {
        
    }

    private String handleCommand(String cmd) {
        switch (cmd.trim()) {
            default:
                return "Unknown command: " + cmd;
        }
    }
}
