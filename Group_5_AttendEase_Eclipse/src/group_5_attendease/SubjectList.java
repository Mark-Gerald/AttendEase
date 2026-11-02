package group_5_attendease;

/**
 * User-defined doubly linked list of subjects. Same pointer rules as
 * AttendanceList; no java.util collection is used.
 */
class SubjectList {

    Node_subject head;
    Node_subject tail;

    /** Insert at the beginning. O(1). */
    void insertAtHead(Node_subject node) {
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
    void insertAtTail(Node_subject node) {
        node.next = null;
        node.prev = tail;
        if (tail == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
    }

    /** Insert at a 1-based position (1 = head, count+1 = tail). O(n). */
    boolean insertAtPosition(int position, Node_subject node) {
        if (position < 1) {
            return false;
        }
        if (position == 1) {
            insertAtHead(node);
            return true;
        }
        Node_subject before = nodeAt(position - 1);
        if (before == null) {
            return false;
        }
        if (before == tail) {
            insertAtTail(node);
        } else {
            node.prev = before;
            node.next = before.next;
            before.next.prev = node;
            before.next = node;
        }
        return true;
    }

    /** Node at a 1-based position, or null. O(n). */
    Node_subject nodeAt(int position) {
        if (position < 1) {
            return null;
        }
        Node_subject cur = head;
        int index = 1;
        while (cur != null && index < position) {
            cur = cur.next;
            index++;
        }
        return cur;
    }

    /** Search by subject code (case-insensitive). O(n). */
    Node_subject findByCode(String code) {
        for (Node_subject cur = head; cur != null; cur = cur.next) {
            if (cur.subjectCode != null && cur.subjectCode.equalsIgnoreCase(code)) {
                return cur;
            }
        }
        return null;
    }

    /** Count by traversal. O(n). */
    int count() {
        int total = 0;
        for (Node_subject cur = head; cur != null; cur = cur.next) {
            total++;
        }
        return total;
    }

    /** Unlink a node that is in this list, reconnecting its neighbours. O(1). */
    void remove(Node_subject node) {
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
}