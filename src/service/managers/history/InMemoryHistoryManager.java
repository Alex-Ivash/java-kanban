package service.managers.history;

import model.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryHistoryManager implements HistoryManager {
    private Node head;
    private Node tail;
    final Map<Integer, Node> history;

    public InMemoryHistoryManager() {
        this.history = new HashMap<>();
    }

    @Override
    public void add(Task task) {
        if (task == null) {
            return;
        }

        Node node = history.get(task.getId());
        linkNeighboringNodes(node);
        linkLast(task);
    }

    @Override
    public void remove(int id) {
        Node taskNode = history.get(id);
        removeNode(taskNode);
    }

    @Override
    public List<Task> getHistory() {
        return getTasks();
    }

    private ArrayList<Task> getTasks() {
        return new ArrayList<>(history.size()) {{
            for (Node curr = head; curr != null; curr = curr.next) {
                add(curr.task);
            }
        }};
    }

    private void removeNode(Node node) {
        if (node == null) {
            return;
        }

        linkNeighboringNodes(node);
        history.remove(node.task.getId());
    }

    private void linkNeighboringNodes(Node node) {
        if (node == null) {
            return;
        }

        Node next = node.next;
        Node prev = node.prev;

        if (next != null) {
            next.prev = prev;
        } else {
            tail = prev;
        }

        if (prev != null) {
            prev.next = next;
        } else {
            head = next;
        }
    }

    private void linkLast(Task task) {
        Node oldLast = tail;
        Node newTaskNode = new Node(oldLast, task, null);
        tail = newTaskNode;

        if (oldLast == null) {
            head = newTaskNode;
        } else {
            oldLast.next = newTaskNode;
        }

        history.put(task.getId(), newTaskNode);
    }

    private static class Node {
        private Node next;
        private Node prev;
        private Task task;

        public Node(Node prev, Task task, Node next) {
            this.prev = prev;
            this.task = task;
            this.next = next;
        }

        @Override
        public boolean equals(Object otherNode) {
            if (this == otherNode) return true;
            if (otherNode.getClass() != getClass()) return false;
            Node node = (Node) otherNode;

            return task.equals(node.task);
        }

        @Override
        public int hashCode() {
            return task.hashCode();
        }
    }
}
