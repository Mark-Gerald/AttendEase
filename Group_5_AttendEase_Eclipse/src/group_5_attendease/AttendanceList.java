package group_5_attendease;

import java.time.LocalDateTime;

/**
 * User-defined doubly linked list of attendance records.
 * The nodes ARE the storage: head, tail and every prev/next reference are
 * maintained here by hand. No java.util collection is used.
 */
class AttendanceList {

    Node_attendance head;   // first node; head.prev is always null
    Node_attendance tail;   // last node;  tail.next is always null

    boolean isEmpty() {
        return head == null;
    }

    // ---------------- insertion ----------------
    /** Insert at the beginning. O(1). */
    void insertAtHead(Node_attendance node) {
        node.prev = null;
        node.next = head;
        if (head == null) {
            tail = node;
        } else {
            head.prev = node;
        }
        head = node;
    }

    /** Insert at the end. O(1) because a tail reference is kept. */
    void insertAtTail(Node_attendance node) {
        node.next = null;
        node.prev = tail;
        if (tail == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
    }

    /** Insert directly after a known node (null means "before everything"). O(1). */
    void insertAfter(Node_attendance after, Node_attendance node) {
        if (after == null) {
            insertAtHead(node);
        } else if (after == tail) {
            insertAtTail(node);
        } else {
            node.prev = after;
            node.next = after.next;
            after.next.prev = node;
            after.next = node;
        }
    }

    /** Insert at a 1-based position (1 = head, count+1 = tail). O(n). */
    boolean insertAtPosition(int position, Node_attendance node) {
        if (position < 1) {
            return false;
        }
        if (position == 1) {
            insertAtHead(node);
            return true;
        }
        Node_attendance before = nodeAt(position - 1);
        if (before == null) {
            return false;
        }
        insertAfter(before, node);
        return true;
    }

    // ---------------- search / traversal ----------------
    /** Node at a 1-based position, or null. O(n). */
    Node_attendance nodeAt(int position) {
        if (position < 1) {
            return null;
        }
        Node_attendance cur = head;
        int index = 1;
        while (cur != null && index < position) {
            cur = cur.next;
            index++;
        }
        return cur;
    }

    /** Search by student ID (case-insensitive). O(n). */
    Node_attendance findById(String id) {
        for (Node_attendance cur = head; cur != null; cur = cur.next) {
            if (cur.studentId.equalsIgnoreCase(id)) {
                return cur;
            }
        }
        return null;
    }

    /** Count by traversal (no stored counter). O(n). */
    int count() {
        int total = 0;
        for (Node_attendance cur = head; cur != null; cur = cur.next) {
            total++;
        }
        return total;
    }

    // ---------------- update / delete ----------------
    /** Change a node's data in place; links are untouched. O(1) once the node is known. */
    void updateRecord(Node_attendance node, String name, LocalDateTime time, String status) {
        if (name != null) {
            node.studentName = name;
        }
        if (time != null) {
            node.attendanceTime = time;
            node.status = status;
        }
    }

    /** Unlink a node that is in this list, reconnecting its neighbours. O(1). */
    void remove(Node_attendance node) {
        if (node.prev == null) {
            head = node.next;
        } else {
            node.prev.next = node.next;
        }
        if (node.next == null) {
            tail = node.prev;
        } else {
            node.next.prev = node.prev;
        }
        node.prev = null;
        node.next = null;
    }

    // ---------------- sorting ----------------
    /**
     * Sort this list by attendance date-time with bubble sort. Node data is
     * swapped, so every prev/next link stays exactly as it was.
     * O(n^2) worst/average, O(n) when already sorted (early exit).
     */
    void sortByAttendanceTime() {
        if (head == null || head.next == null) {
            return;
        }
        boolean swapped;
        do {
            swapped = false;
            for (Node_attendance cur = head; cur.next != null; cur = cur.next) {
                if (cur.attendanceTime.compareTo(cur.next.attendanceTime) > 0) {
                    swapData(cur, cur.next);
                    swapped = true;
                }
            }
        } while (swapped);
    }

    /**
     * Build a NEW list holding copies of every record in A-Z order of student
     * name (case-insensitive) using insertion sort on the linked list. The
     * stored list is not modified. Walking the new list from tail to head gives
     * Z-A order. O(n^2) worst case; O(n) if the records are already in order,
     * because each insertion starts searching from the tail.
     */
    AttendanceList sortedCopyByName() {
        AttendanceList sorted = new AttendanceList();
        for (Node_attendance src = head; src != null; src = src.next) {
            Node_attendance copy = src.copy();
            Node_attendance after = sorted.tail;
            while (after != null
                    && after.studentName.compareToIgnoreCase(copy.studentName) > 0) {
                after = after.prev;
            }
            sorted.insertAfter(after, copy);   // after equal names => stable order
        }
        return sorted;
    }

    private void swapData(Node_attendance a, Node_attendance b) {
        String id = a.studentId;
        String name = a.studentName;
        LocalDateTime time = a.attendanceTime;
        String status = a.status;

        a.studentId = b.studentId;
        a.studentName = b.studentName;
        a.attendanceTime = b.attendanceTime;
        a.status = b.status;

        b.studentId = id;
        b.studentName = name;
        b.attendanceTime = time;
        b.status = status;
    }
}