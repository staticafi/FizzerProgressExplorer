package fizzer;

import javax.swing.*;

import fizzer.nav.GenPaths;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Console extends JPanel {

    private JTextArea outputArea;
    private JTextField inputField;
    private ArrayList<String> history;
    private int historyCursor;
    private ExecutionTree executionTree;

    public Console(ExecutionTree executionTree_) {
        history = new ArrayList<>();
        historyCursor = 0;
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

                if (!command.isEmpty()) {
                    history.remove(command);
                    history.add(command);
                    historyCursor = history.size();
                }

                print(">>> " + command);
                handleCommand(command);
            }
        });
        InputMap im = inputField.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = inputField.getActionMap();
        im.put(KeyStroke.getKeyStroke("UP"), "arrowUpPressed");
        im.put(KeyStroke.getKeyStroke("DOWN"), "arrowDownPressed");
        am.put("arrowUpPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onFetchPreviousCommand();
            }
        });
        am.put("arrowDownPressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onFetchNextCommand();
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

    private void onFetchPreviousCommand() {
        if (historyCursor > 0)
            --historyCursor;
        if (historyCursor < history.size())
            inputField.setText(history.get(historyCursor));
    }

    private void onFetchNextCommand() {
        if (historyCursor < history.size())
            ++historyCursor;
        if (historyCursor < history.size())
            inputField.setText(history.get(historyCursor));
        else
            inputField.setText("");
    }

    private void handleCommand(String cmd) {
        switch (cmd.trim()) {
            case "": break;
            case "leaves":
                for (Node leaf : executionTree.getLeaves())
                    print(Long.toString(leaf.guid));
                break;
            case "clear":
                outputArea.setText("");
                break;
            case "clear history":
                history.clear();
                historyCursor = 0;
                print("History cleared.");
                break;
            case "paths":
                print(new GenPaths(executionTree).run());
                break;
            default:
                print("Unknown command.");
                break;
        }
    }
}
