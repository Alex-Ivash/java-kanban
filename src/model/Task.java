package model;

import java.util.Objects;

public class Task {
    protected int id;
    protected Status status;
    protected String name;
    protected String description;


    public Task(Status status, String name, String description) {
        this.status = status;
        this.name = name;
        this.description = description;
    }

    public Task(String name, String description) {
        this.name = name;
        this.description = description;
    } // конструктор для эпика, с целью расчета статуса самим эпиком, без возможности его задать на этапе конструирования(по ТЗ)

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName() + "{");
        sb.append("id=").append(id);
        sb.append(", status=").append(status);
        sb.append(", name='").append(name);
        sb.append(", description='").append(description);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object otherTask) {
        if (this == otherTask) return true;
        if (otherTask.getClass() != getClass()) return false;
        Task task = (Task) otherTask;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
