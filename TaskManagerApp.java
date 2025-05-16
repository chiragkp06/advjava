import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

public class TaskManagerApp extends JFrame {
    private static final long serialVersionUID = 1L;
    private ArrayList<Task> tasks;
    private DefaultListModel<Task> listModel;
    private JList<Task> taskList;
    private JTextField taskInput;
    private JComboBox<String> priorityCombo;
    private JTextField categoryInput;
    private JTextField searchField;
    private JSpinner dueDateSpinner;
    private JTextArea notesArea;
    private JCheckBox darkModeCheckBox;
    private JLabel statsLabel;
    private boolean isDarkMode = false;
    private static final String SAVE_FILE = "tasks.dat";
    private JComboBox<String> filterCategory;
    private JComboBox<String> filterPriority;

    public TaskManagerApp() {
        this.tasks = new ArrayList<>();
        this.listModel = new DefaultListModel<>();
        setTitle("Enhanced Task Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 700);
        setLocationRelativeTo(null);
        initializeUI();
    }

    private void initializeUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel topPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        topPanel.add(createInputPanel());
        topPanel.add(createFilterPanel());
        topPanel.add(createSearchPanel());
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(createSplitPane(), BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        mainPanel.add(createStatsPanel(), BorderLayout.EAST);
        add(mainPanel);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { saveTasks(); }
        });
        updateTheme();
        loadTasks();
        updateStatistics();
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 2, 2, 2);

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Task:"), gbc);
        taskInput = new JTextField();
        gbc.gridx = 1; gbc.weightx = 1.0;
        panel.add(taskInput, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        panel.add(new JLabel("Priority:"), gbc);
        priorityCombo = new JComboBox<>(new String[]{"HIGH", "MEDIUM", "LOW"});
        gbc.gridx = 3;
        panel.add(priorityCombo, gbc);

        gbc.gridx = 4;
        panel.add(new JLabel("Category:"), gbc);
        categoryInput = new JTextField(10);
        gbc.gridx = 5;
        panel.add(categoryInput, gbc);

        gbc.gridx = 6;
        panel.add(new JLabel("Due Date:"), gbc);
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.SECOND, 0);
        SpinnerDateModel dateModel = new SpinnerDateModel(calendar.getTime(), null, null, java.util.Calendar.MINUTE);
        dueDateSpinner = new JSpinner(dateModel);
        dueDateSpinner.setEditor(new JSpinner.DateEditor(dueDateSpinner, "yyyy-MM-dd HH:mm"));
        gbc.gridx = 7;
        panel.add(dueDateSpinner, gbc);
        return panel;
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Filter Category:"));
        filterCategory = new JComboBox<>(new String[]{"All"});
        panel.add(filterCategory);
        panel.add(new JLabel("Filter Priority:"));
        filterPriority = new JComboBox<>(new String[]{"All", "HIGH", "MEDIUM", "LOW"});
        panel.add(filterPriority);
        darkModeCheckBox = new JCheckBox("Dark Mode");
        darkModeCheckBox.addActionListener(e -> toggleDarkMode());
        panel.add(darkModeCheckBox);
        return panel;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Search:"));
        searchField = new JTextField(30);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { filterTasks(); }
            public void removeUpdate(DocumentEvent e) { filterTasks(); }
            public void insertUpdate(DocumentEvent e) { filterTasks(); }
        });
        panel.add(searchField);
        return panel;
    }

    private JSplitPane createSplitPane() {
        taskList = new JList<>(listModel);
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.addListSelectionListener(e -> updateNotesArea());
        JScrollPane listScroller = new JScrollPane(taskList);
        notesArea = new JTextArea(5, 30);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { saveNotes(); }
            public void removeUpdate(DocumentEvent e) { saveNotes(); }
            public void insertUpdate(DocumentEvent e) { saveNotes(); }
        });
        JScrollPane notesScroller = new JScrollPane(notesArea);
        notesScroller.setBorder(BorderFactory.createTitledBorder("Task Notes"));
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listScroller, notesScroller);
        splitPane.setResizeWeight(0.7);
        return splitPane;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Task");
        JButton removeButton = new JButton("Remove Selected");
        JButton completeButton = new JButton("Toggle Complete");
        JButton sortButton = new JButton("Sort Tasks");
        addButton.addActionListener(e -> addTask());
        removeButton.addActionListener(e -> removeTask());
        completeButton.addActionListener(e -> toggleTaskComplete());
        sortButton.addActionListener(e -> sortTasks());
        panel.add(addButton);
        panel.add(removeButton);
        panel.add(completeButton);
        panel.add(sortButton);
        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Statistics"));
        statsLabel = new JLabel();
        statsLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.add(statsLabel, BorderLayout.CENTER);
        return panel;
    }

    private void addTask() {
        String description = taskInput.getText().trim();
        String priority = (String) priorityCombo.getSelectedItem();
        String category = categoryInput.getText().trim();
        if (!description.isEmpty() && !category.isEmpty()) {
            Task task = new Task(description, priority, category);
            task.setDueDate(((java.util.Date) dueDateSpinner.getValue()).toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            tasks.add(task);
            updateListModel();
            updateCategories();
            updateStatistics();
            taskInput.setText("");
            categoryInput.setText("");
        }
    }

    private void toggleTaskComplete() {
        int selectedIndex = taskList.getSelectedIndex();
        if (selectedIndex != -1) {
            Task task = tasks.get(selectedIndex);
            task.setCompleted(!task.isCompleted());
            updateListModel();
            updateStatistics();
        }
    }

    private void updateNotesArea() {
        int selectedIndex = taskList.getSelectedIndex();
        if (selectedIndex != -1) {
            Task task = tasks.get(selectedIndex);
            notesArea.setText(task.getNotes());
            notesArea.setEnabled(true);
        } else {
            notesArea.setText("");
            notesArea.setEnabled(false);
        }
    }

    private void saveNotes() {
        int selectedIndex = taskList.getSelectedIndex();
        if (selectedIndex != -1) {
            tasks.get(selectedIndex).setNotes(notesArea.getText());
        }
    }

    private void updateCategories() {
        String currentSelection = (String) filterCategory.getSelectedItem();
        filterCategory.removeAllItems();
        filterCategory.addItem("All");
        tasks.stream().map(Task::getCategory).distinct().sorted()
                .forEach(filterCategory::addItem);
        if (currentSelection != null) {
            filterCategory.setSelectedItem(currentSelection);
        }
    }

    private void filterTasks() {
        String searchText = searchField.getText().toLowerCase();
        String categoryFilter = (String) filterCategory.getSelectedItem();
        String priorityFilter = (String) filterPriority.getSelectedItem();
        listModel.clear();
        tasks.stream()
                .filter(task -> searchText.isEmpty() || 
                        task.getDescription().toLowerCase().contains(searchText) ||
                        task.getNotes().toLowerCase().contains(searchText))
                .filter(task -> categoryFilter.equals("All") || 
                        task.getCategory().equals(categoryFilter))
                .filter(task -> priorityFilter.equals("All") || 
                        task.getPriority().equals(priorityFilter))
                .forEach(listModel::addElement);
    }

    private void updateStatistics() {
        long totalTasks = tasks.size();
        long completedTasks = tasks.stream().filter(Task::isCompleted).count();
        long highPriorityTasks = tasks.stream()
                .filter(t -> t.getPriority().equals("HIGH")).count();
        StringBuilder stats = new StringBuilder("<html>");
        stats.append("Total Tasks: ").append(totalTasks).append("<br>")
             .append("Completed: ").append(completedTasks).append("<br>")
             .append("Pending: ").append(totalTasks - completedTasks).append("<br>")
             .append("High Priority: ").append(highPriorityTasks).append("<br>")
             .append("</html>");
        statsLabel.setText(stats.toString());
    }

    private void toggleDarkMode() {
        isDarkMode = darkModeCheckBox.isSelected();
        updateTheme();
    }

    private void updateTheme() {
        Color bgColor = isDarkMode ? new Color(43, 43, 43) : Color.WHITE;
        Color fgColor = isDarkMode ? Color.WHITE : Color.BLACK;
        Color inputBgColor = isDarkMode ? new Color(60, 60, 60) : Color.WHITE;
        Color buttonBgColor = isDarkMode ? new Color(70, 70, 70) : null;
        
        UIManager.put("Panel.background", bgColor);
        UIManager.put("TextField.background", inputBgColor);
        UIManager.put("TextField.foreground", fgColor);
        UIManager.put("TextArea.background", inputBgColor);
        UIManager.put("TextArea.foreground", fgColor);
        UIManager.put("List.background", inputBgColor);
        UIManager.put("List.foreground", fgColor);
        UIManager.put("Label.foreground", fgColor);
        UIManager.put("ComboBox.background", inputBgColor);
        UIManager.put("ComboBox.foreground", fgColor);
        UIManager.put("Button.background", buttonBgColor);
        UIManager.put("Button.foreground", fgColor);
        UIManager.put("TitledBorder.titleColor", fgColor);
        UIManager.put("ScrollPane.background", bgColor);
        UIManager.put("Spinner.background", inputBgColor);
        UIManager.put("Spinner.foreground", fgColor);
        UIManager.put("CheckBox.foreground", fgColor);
        UIManager.put("CheckBox.background", bgColor);

        taskList.setBackground(inputBgColor);
        taskList.setForeground(fgColor);
        notesArea.setBackground(inputBgColor);
        notesArea.setForeground(fgColor);
        taskInput.setBackground(inputBgColor);
        taskInput.setForeground(fgColor);
        categoryInput.setBackground(inputBgColor);
        categoryInput.setForeground(fgColor);
        searchField.setBackground(inputBgColor);
        searchField.setForeground(fgColor);
        dueDateSpinner.getEditor().getComponent(0).setBackground(inputBgColor);
        dueDateSpinner.getEditor().getComponent(0).setForeground(fgColor);
        
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void saveTasks() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {
            oos.writeObject(tasks);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving tasks: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadTasks() {
        if (new File(SAVE_FILE).exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_FILE))) {
                tasks = (ArrayList<Task>) ois.readObject();
                updateListModel();
                updateCategories();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error loading tasks: " + e.getMessage());
            }
        }
    }

    private void removeTask() {
        int selectedIndex = taskList.getSelectedIndex();
        if (selectedIndex != -1) {
            tasks.remove(selectedIndex);
            updateListModel();
            updateCategories();
            updateStatistics();
        }
    }

    private void sortTasks() {
        Collections.sort(tasks);
        updateListModel();
        updateStatistics();
    }

    private void updateListModel() {
        listModel.clear();
        tasks.forEach(listModel::addElement);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new TaskManagerApp().setVisible(true);
        });
    }
} 