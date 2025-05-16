import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.awt.Color;
import java.io.Serializable;

public class Task implements Comparable<Task>, Serializable {
    private static final long serialVersionUID = 1L;
    private String description;
    private String priority;
    private String category;
    private LocalDateTime dueDate;
    private boolean completed;
    private String notes;
    private transient Color categoryColor; // transient because Color might not be serializable
    private LocalDateTime createdAt;

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
        // Save color RGB values
        out.writeInt(categoryColor.getRGB());
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Restore color from RGB values
        categoryColor = new Color(in.readInt(), true);
    }

    public Task(String description, String priority, String category) {
        this.description = description.trim();
        this.priority = priority.toUpperCase();
        this.category = category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase();
        this.completed = false;
        this.notes = "";
        this.createdAt = LocalDateTime.now();
        this.categoryColor = new Color(200, 200, 200); // Default gray
    }

    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public String getCategory() { return category; }
    public LocalDateTime getDueDate() { return dueDate; }
    public boolean isCompleted() { return completed; }
    public String getNotes() { return notes; }
    public Color getCategoryColor() { return categoryColor; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setDescription(String description) { this.description = description.trim(); }
    public void setPriority(String priority) { this.priority = priority.toUpperCase(); }
    public void setCategory(String category) {
        this.category = category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase();
    }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setNotes(String notes) { this.notes = notes.trim(); }
    public void setCategoryColor(Color color) { this.categoryColor = color; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (completed) sb.append("✓ ");
        sb.append("[").append(priority).append("] ")
          .append(description)
          .append(" (").append(category).append(")");
        if (dueDate != null) {
            sb.append(" Due: ").append(dueDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        }
        return sb.toString();
    }

    @Override
    public int compareTo(Task other) {
        if (this.completed != other.completed) return this.completed ? 1 : -1;
        if (this.dueDate != null && other.dueDate != null) {
            int dateCompare = this.dueDate.compareTo(other.dueDate);
            if (dateCompare != 0) return dateCompare;
        }
        int priorityCompare = this.priority.compareTo(other.priority);
        if (priorityCompare != 0) return priorityCompare;
        int categoryCompare = this.category.compareTo(other.category);
        if (categoryCompare != 0) return categoryCompare;
        return this.description.compareTo(other.description);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Task task = (Task) obj;
        return description.equals(task.description) &&
               priority.equals(task.priority) &&
               category.equals(task.category);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + description.hashCode();
        result = 31 * result + priority.hashCode();
        result = 31 * result + category.hashCode();
        return result;
    }
} 