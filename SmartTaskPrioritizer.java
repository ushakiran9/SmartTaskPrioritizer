import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

class ProductivityTask {
    String title;
    String deadline;
    String category;
    int importance;
    int estimatedMinutes;
    boolean completed;

    ProductivityTask(String title, String deadline, String category, int importance, int estimatedMinutes) {
        this.title = title;
        this.deadline = deadline;
        this.category = category;
        this.importance = importance;
        this.estimatedMinutes = estimatedMinutes;
        this.completed = false;
    }

    int priorityScore() {
        int score = importance * 8;
        int effortAdj = Math.max(1, estimatedMinutes / 10);

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date today = sdf.parse(sdf.format(new Date()));
            Date due = sdf.parse(deadline);
            long daysRemaining = (due.getTime() - today.getTime()) / (1000 * 3600 * 24);
            if (daysRemaining < 0) score += 25;
            else if (daysRemaining == 0) score += 15;
            else if (daysRemaining == 1) score += 7;
        } catch (Exception ignored) {}

        return score - effortAdj;
    }
}

public class SmartTaskPrioritizer extends JFrame {
    private List<ProductivityTask> taskList = new ArrayList<>();
    private DefaultTableModel tableModel;
    private final String saveFile = "smart_tasks.csv";

    public SmartTaskPrioritizer() {
        setTitle("SmartTaskPrioritizer: Productivity Manager");
        setSize(1150, 670); // Much larger window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Table font and enhanced row color coding
        tableModel = new DefaultTableModel(new String[]{
            "Title", "Deadline", "Category", "Importance", "Effort (min)", "Priority Score", "Completed"
        }, 0);

        JTable table = new JTable(tableModel) {
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int col) {
                Component comp = super.prepareRenderer(renderer, row, col);
                int score = Integer.parseInt(getModel().getValueAt(row, 5).toString());
                if (score >= 40) comp.setBackground(new Color(255, 235, 238));
                else if (score >= 30) comp.setBackground(new Color(255, 249, 196));
                else comp.setBackground(Color.white);
                comp.setFont(new Font("SansSerif", Font.PLAIN, 16)); // Larger table font
                return comp;
            }
        };
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 18));
        JScrollPane pane = new JScrollPane(table);

        // Big control panel: Layout as 2 rows
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Controls row 1
        JPanel controlsRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        JButton btnAdd = styledButton("Add Task");
        JButton btnEdit = styledButton("Edit Task");
        JButton btnDelete = styledButton("Delete Task");
        JButton btnComplete = styledButton("Mark as Done");
        JButton btnExport = styledButton("Export CSV");
        controlsRow1.add(btnAdd); controlsRow1.add(btnEdit);
        controlsRow1.add(btnDelete); controlsRow1.add(btnComplete);
        controlsRow1.add(btnExport);

        // Controls row 2 (search, refresh, summary)
        JPanel controlsRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        JTextField searchField = new JTextField(18);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 18));
        JButton btnSearch = styledButton("Search");
        JButton btnShowAll = styledButton("Show All");
        JButton btnReload = styledButton("Refresh");
        JButton btnSummary = styledButton("Show Summary");

        controlsRow2.add(new JLabel("<html><b>Filter:</b></html>"));
        controlsRow2.add(searchField);
        controlsRow2.add(btnSearch); controlsRow2.add(btnShowAll);
        controlsRow2.add(btnReload); controlsRow2.add(btnSummary);

        // Panel to hold both control rows vertically
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.add(controlsRow1);
        controlPanel.add(Box.createVerticalStrut(8));
        controlPanel.add(controlsRow2);

        // Add to main panel
        mainPanel.add(pane, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        add(mainPanel);

        // Button actions
        btnAdd.addActionListener(e -> showAddTaskDialog());
        btnEdit.addActionListener(e -> editSelectedTask(table));
        btnDelete.addActionListener(e -> deleteSelectedTask(table));
        btnComplete.addActionListener(e -> markTaskComplete(table));
        btnReload.addActionListener(e -> updateTable());
        btnSummary.addActionListener(e -> showStats());
        btnExport.addActionListener(e -> exportTasksCSV());
        btnSearch.addActionListener(e -> filterTasks(searchField.getText()));
        btnShowAll.addActionListener(e -> updateTable());

        readTasksFromFile();
        updateTable();
        notifyTodaysTasks();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                saveTasksToFile();
            }
        });
    }

    // Style button appearance (large, bold)
    private JButton styledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 18));
        btn.setBackground(new Color(245, 245, 255));
        btn.setFocusPainted(false);
        return btn;
    }

    private void showAddTaskDialog() {
        JTextField titleField = new JTextField();
        JTextField deadlineField = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        JTextField categoryField = new JTextField();
        JTextField importanceField = new JTextField("3");
        JTextField effortField = new JTextField("30");

        titleField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        deadlineField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        categoryField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        importanceField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        effortField.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JPanel fieldsPanel = new JPanel(new GridLayout(0, 1, 8, 8));
        fieldsPanel.add(new JLabel("Task Title:")); fieldsPanel.add(titleField);
        fieldsPanel.add(new JLabel("Deadline (yyyy-MM-dd):")); fieldsPanel.add(deadlineField);
        fieldsPanel.add(new JLabel("Category:")); fieldsPanel.add(categoryField);
        fieldsPanel.add(new JLabel("Importance (1-5):")); fieldsPanel.add(importanceField);
        fieldsPanel.add(new JLabel("Effort (minutes):")); fieldsPanel.add(effortField);

        int result = JOptionPane.showConfirmDialog(this, fieldsPanel, "Add Task", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                taskList.add(new ProductivityTask(
                    titleField.getText(), deadlineField.getText(), categoryField.getText(),
                    Integer.parseInt(importanceField.getText()), Integer.parseInt(effortField.getText()))
                );
                updateTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input! Please check all fields.");
            }
        }
    }

    private void editSelectedTask(JTable table) {
        int row = table.getSelectedRow();
        if (row >= 0 && row < taskList.size()) {
            ProductivityTask t = taskList.get(row);

            JTextField titleField = new JTextField(t.title);
            JTextField deadlineField = new JTextField(t.deadline);
            JTextField categoryField = new JTextField(t.category);
            JTextField importanceField = new JTextField(String.valueOf(t.importance));
            JTextField effortField = new JTextField(String.valueOf(t.estimatedMinutes));

            titleField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            deadlineField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            categoryField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            importanceField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            effortField.setFont(new Font("SansSerif", Font.PLAIN, 16));

            JPanel fieldsPanel = new JPanel(new GridLayout(0, 1, 8, 8));
            fieldsPanel.add(new JLabel("Task Title:")); fieldsPanel.add(titleField);
            fieldsPanel.add(new JLabel("Deadline (yyyy-MM-dd):")); fieldsPanel.add(deadlineField);
            fieldsPanel.add(new JLabel("Category:")); fieldsPanel.add(categoryField);
            fieldsPanel.add(new JLabel("Importance (1-5):")); fieldsPanel.add(importanceField);
            fieldsPanel.add(new JLabel("Effort (minutes):")); fieldsPanel.add(effortField);

            int result = JOptionPane.showConfirmDialog(this, fieldsPanel, "Edit Task", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    t.title = titleField.getText();
                    t.deadline = deadlineField.getText();
                    t.category = categoryField.getText();
                    t.importance = Integer.parseInt(importanceField.getText());
                    t.estimatedMinutes = Integer.parseInt(effortField.getText());
                    updateTable();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Invalid input! Please check all fields.");
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a task row to edit.");
        }
    }

    private void deleteSelectedTask(JTable table) {
        int row = table.getSelectedRow();
        if (row >= 0 && row < taskList.size()) {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this task?",
                "Delete Task", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                taskList.remove(row);
                updateTable();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a task row to delete.");
        }
    }

    private void markTaskComplete(JTable table) {
        int row = table.getSelectedRow();
        if (row >= 0 && row < taskList.size()) {
            taskList.get(row).completed = true;
            updateTable();
        } else {
            JOptionPane.showMessageDialog(this, "Select a task row to mark as done.");
        }
    }

    private void updateTable() {
        tableModel.setRowCount(0);
        taskList.stream()
            .sorted((t1, t2) -> t2.priorityScore() - t1.priorityScore())
            .forEach(t -> tableModel.addRow(new Object[]{
                t.title, t.deadline, t.category, t.importance, t.estimatedMinutes,
                t.priorityScore(), t.completed ? "Yes" : ""
            }));
    }

    private void showStats() {
        long doneCount = taskList.stream().filter(t -> t.completed).count();
        long totalCount = taskList.size();
        StringBuilder stats = new StringBuilder();
        stats.append("Completed tasks: ").append(doneCount).append(" / ").append(totalCount)
            .append(String.format(" (%.1f%%)%n", totalCount > 0 ? 100.0 * doneCount / totalCount : 0));
        stats.append("\nOverdue Tasks:\n");
        String todayStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        taskList.stream()
            .filter(t -> !t.completed && t.deadline.compareTo(todayStr) < 0)
            .forEach(t -> stats.append("- ").append(t.title).append(" [due: ").append(t.deadline).append("]\n"));

        JOptionPane.showMessageDialog(this, stats.toString(), "Task Summary", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportTasksCSV() {
        try (PrintWriter out = new PrintWriter(new FileWriter("tasks_exported.csv"))) {
            for (ProductivityTask t : taskList)
                out.println(String.join(",", t.title, t.deadline, t.category,
                    String.valueOf(t.importance), String.valueOf(t.estimatedMinutes), String.valueOf(t.completed)));
            JOptionPane.showMessageDialog(this, "Tasks exported to tasks_exported.csv");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error exporting tasks.");
        }
    }

    private void filterTasks(String keyword) {
        tableModel.setRowCount(0);
        taskList.stream()
            .filter(t -> t.title.toLowerCase().contains(keyword.toLowerCase())
                      || t.category.toLowerCase().contains(keyword.toLowerCase()))
            .sorted((t1, t2) -> t2.priorityScore() - t1.priorityScore())
            .forEach(t -> tableModel.addRow(new Object[]{
                t.title, t.deadline, t.category, t.importance, t.estimatedMinutes,
                t.priorityScore(), t.completed ? "Yes" : ""
            }));
    }

    private void saveTasksToFile() {
        try (PrintWriter out = new PrintWriter(new FileWriter(saveFile))) {
            for (ProductivityTask t : taskList)
                out.println(String.join(",", t.title, t.deadline, t.category,
                        String.valueOf(t.importance), String.valueOf(t.estimatedMinutes), String.valueOf(t.completed)));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving tasks.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void readTasksFromFile() {
        taskList.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(saveFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] vals = line.split(",", -1);
                if (vals.length == 6) {
                    ProductivityTask t = new ProductivityTask(vals[0], vals[1], vals[2],
                        Integer.parseInt(vals[3]), Integer.parseInt(vals[4]));
                    t.completed = Boolean.parseBoolean(vals[5]);
                    taskList.add(t);
                }
            }
        } catch (IOException ignored) {}
    }

    private void notifyTodaysTasks() {
        String todayStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        List<String> dueToday = new ArrayList<>();
        for (ProductivityTask t : taskList) {
            if (!t.completed && t.deadline.equals(todayStr)) dueToday.add(t.title);
        }
        if (!dueToday.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Tasks due today:\n" + String.join("\n", dueToday), 
                "Reminder: Due Today", JOptionPane.WARNING_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SmartTaskPrioritizer().setVisible(true));
    }
}
