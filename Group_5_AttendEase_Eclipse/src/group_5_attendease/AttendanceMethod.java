package group_5_attendease;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Attendance operations for ONE attendance list (the lecture list or the
 * laboratory list of a subject). All records live in the user-defined
 * AttendanceList (doubly linked list).
 */
public class AttendanceMethod {

    // Philippine Standard Time: used only to pick today's date for new records.
    private static final ZoneId PH_ZONE = ZoneId.of("Asia/Manila");
    // Source of "now". Package-private and non-final only so tests can supply a fixed clock.
    static Clock clock = Clock.system(PH_ZONE);

    private static final DateTimeFormatter DISPLAY_DATE
            = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    private static final String[] TABLE_HEADERS
            = {"Student ID", "Student Name", "Date", "Attendance Time", "Status"};
    static final String ON_TIME = "On Time";
    static final String LATE = "Late";
    private static final String LOOKUP_NOTE
            = "Finding a student by ID traverses the list: O(n)";
    private static final String CANCEL_NOTE
            = "Duplicate-ID checks traverse the list; no node was inserted";

    private final Node_subject subject;
    // The ACTIVE attendance list. Only these two references change when the schedule is switched.
    private boolean laboratory;
    private AttendanceList list;

    public AttendanceMethod(Node_subject subject) {
        this(subject, false);
    }

    public AttendanceMethod(Node_subject subject, boolean laboratory) {
        this.subject = subject;
        this.laboratory = laboratory;
        this.list = laboratory ? subject.labAttendance : subject.lectureAttendance;
    }

    // ==================================================================
    // 1. ADD  (collect -> validate -> summary -> confirm -> insert at tail)
    // ==================================================================
    public void addAttendance() {
        // The attendance time is captured ONCE, when the user starts this transaction
        // (Philippine time, whole minutes). It is never refreshed or typed in.
        LocalDateTime stamp = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);

        ConsoleUI.heading("ADD ATTENDANCE RECORD");

        // Class already over: the user (teacher) decides whether to continue. Approval is an
        // exception to the rule; the record is then always LATE (see determineStatus).
        boolean afterClass = classEnded(stamp);
        if (afterClass && !confirmAfterClass(stamp)) {
            ConsoleUI.blank();
            ConsoleUI.cancelled("No attendance record was created.");
            ConsoleUI.complexity("O(1)", "One date-time comparison; the list is not traversed");
            return;
        }
        ConsoleUI.center("Type CANCEL at any prompt to cancel.");
        ConsoleUI.blank();

        String id = askNewStudentId();
        if (id == null) {
            cancelDone("No attendance record was added.");
            return;
        }
        String name = ConsoleUI.askRequired("Student name", "Student name");
        if (name == null) {
            cancelDone("No attendance record was added.");
            return;
        }
        String status = determineStatus(stamp);

        // Nothing has been inserted yet: show the summary and wait for confirmation.
        printSummary(id, name, stamp, status);
        ConsoleUI.menu(new String[]{"(1) Confirm and Save", "(2) Cancel"});
        Integer choice = ConsoleUI.askChoice("Enter a number", 1, 2);
        if (choice == null || choice == 2) {
            cancelDone("The attendance record was not saved.");
            return;
        }

        // Re-check the critical conditions at the moment of saving.
        if (list.findById(id) != null) {
            done(false, "This student already has an attendance record. Nothing was saved.",
                    "O(n)", "The duplicate-ID check traverses the list");
            return;
        }
        if (!afterClass && classEnded(LocalDateTime.now(clock))) {   // class ended during review
            // (an approved after-class record has already been confirmed by the user)
            classEndedMessage(false);
            done(false, "The record was not saved.", "O(n)",
                    "The duplicate-ID check traverses the list");
            return;
        }

        Node_attendance newNode = new Node_attendance(id, name, stamp, status);
        list.insertAtTail(newNode);

        ConsoleUI.blank();
        printRecord(newNode);
        done(true, "Attendance record added successfully.", "O(n)",
                "Duplicate-ID check O(n) + insertion at the tail O(1), "
                + "because a tail pointer is kept");
    }

    /** Warning screen for a class that has already ended. True only if the user continues. */
    private boolean confirmAfterClass(LocalDateTime stamp) {
        ConsoleUI.heading("CLASS ALREADY ENDED");
        ConsoleUI.warning("The scheduled class has already ended.");
        ConsoleUI.blank();
        StringBuilder sb = new StringBuilder();
        ConsoleUI.addLine(sb, "Class Schedule   : "
                + ConsoleUI.scheduleRange(sessionStart(), sessionEnd()));
        ConsoleUI.addLine(sb, "Current Time     : " + ConsoleUI.format12(stamp.toLocalTime()));
        ConsoleUI.addLine(sb, "Attendance Time  : " + ConsoleUI.format12(stamp.toLocalTime()));
        ConsoleUI.centerBlock(sb.toString());
        ConsoleUI.blank();
        ConsoleUI.center("This attendance record will automatically be marked as LATE.");
        ConsoleUI.center("Do you want to continue?");
        ConsoleUI.blank();
        ConsoleUI.menu(new String[]{"(1) Continue and Record as LATE", "(2) Cancel Attendance"});
        Integer choice = ConsoleUI.askChoice("Enter a number", 1, 2);
        return choice != null && choice == 1;
    }

    /** "Networking (NET201) - Lecture": which subject and list are active. */
    String contextLabel() {
        return subject.subject + " (" + subject.subjectCode + ") - "
                + (laboratory ? "Laboratory" : "Lecture");
    }

    /** Selected-subject box shown before choosing (or changing) an attendance list. */
    static void printSubjectInfo(Node_subject subject) {
        ConsoleUI.heading("SELECTED SUBJECT");
        StringBuilder sb = new StringBuilder();
        ConsoleUI.addWrapped(sb, "Subject          : " + subject.subject
                + " (" + subject.subjectCode + ")");
        ConsoleUI.addWrapped(sb, "Day              : " + (subject.day == null ? "-" : subject.day));
        ConsoleUI.addWrapped(sb, "Lecture Schedule : "
                + ConsoleUI.scheduleRange(subject.lectureStart, subject.lectureEnd));
        ConsoleUI.addWrapped(sb, "Laboratory       : " + (subject.hasLab
                ? ConsoleUI.scheduleRange(subject.labStart, subject.labEnd) : "None"));
        ConsoleUI.addWrapped(sb, "Grace Period     : " + ConsoleUI.minutes(subject.gracePeriod));
        ConsoleUI.centerBlock(sb.toString());
    }

    /**
     * Choose Attendance List menu with a cancel entry. Re-asks on an invalid
     * number. Returns 1 (Lecture), 2 (Laboratory) or 0 (cancelled).
     */
    static int chooseAttendanceList(Node_subject subject, String cancelText) {
        ConsoleUI.center("CHOOSE ATTENDANCE LIST");
        ConsoleUI.menu(new String[]{
            "(1) Lecture (" + ConsoleUI.scheduleRange(subject.lectureStart, subject.lectureEnd) + ")",
            "(2) Laboratory (" + ConsoleUI.scheduleRange(subject.labStart, subject.labEnd) + ")",
            "(0) " + cancelText
        });
        Integer pick = ConsoleUI.askChoice("Enter a number", 0, 2);
        return pick == null ? 0 : pick;
    }

    // ==================================================================
    // 9. CHANGE ATTENDANCE SCHEDULE (switch Lecture <-> Laboratory)
    // ==================================================================
    /**
     * Switches only the ACTIVE attendance list of the same subject. The subject's
     * stored schedule is never touched, and each list keeps its own records.
     */
    public void changeAttendanceSchedule() {
        printSubjectInfo(subject);
        ConsoleUI.center("Active attendance list: " + (laboratory ? "Laboratory" : "Lecture"));
        ConsoleUI.blank();

        if (!subject.hasLab) {
            ConsoleUI.error("This subject has no laboratory. Lecture is its only attendance list.");
            ConsoleUI.complexity("O(1)", "One field check; the list is not traversed");
            return;
        }
        int pick = chooseAttendanceList(subject, "Cancel / Keep Current Schedule");
        ConsoleUI.blank();
        if (pick == 0) {
            ConsoleUI.cancelled("The attendance schedule was not changed.");
        } else {
            laboratory = pick == 2;
            list = laboratory ? subject.labAttendance : subject.lectureAttendance;
            ConsoleUI.ok("Attendance schedule changed to " + contextLabel() + ".");
        }
        ConsoleUI.complexity("O(1)", "Only the active list reference changes");
    }

    private String askNewStudentId() {
        while (true) {
            String id = ConsoleUI.askRequired("Student ID", "Student ID");
            if (id == null) {
                return null;
            }
            if (list.findById(id) == null) {
                return id;
            }
            ConsoleUI.invalid("This student already has an attendance record.");
            ConsoleUI.center("Please enter a different Student ID.");
        }
    }

    private void printSummary(String id, String name, LocalDateTime stamp, String status) {
        ConsoleUI.heading("ATTENDANCE RECORD SUMMARY");
        LocalDateTime start = scheduledAt(stamp.toLocalDate(), sessionStart());
        StringBuilder sb = new StringBuilder();
        ConsoleUI.addWrapped(sb, "Student ID         : " + id);
        ConsoleUI.addWrapped(sb, "Student Name       : " + name);
        ConsoleUI.addWrapped(sb, "Subject            : " + subject.subject
                + " (" + subject.subjectCode + ")");
        ConsoleUI.addWrapped(sb, "Attendance List    : " + (laboratory ? "Laboratory" : "Lecture"));
        ConsoleUI.addWrapped(sb, "Class Schedule     : "
                + ConsoleUI.scheduleRange(sessionStart(), sessionEnd()));
        ConsoleUI.addWrapped(sb, "Attendance Time    : "
                + ConsoleUI.format12(stamp.toLocalTime()));
        ConsoleUI.addWrapped(sb, "Grace Period       : " + ConsoleUI.minutes(subject.gracePeriod));
        ConsoleUI.addWrapped(sb, "Latest On-Time     : " + (start == null ? "-"
                : ConsoleUI.format12(start.plusMinutes(subject.gracePeriod).toLocalTime())));
        ConsoleUI.addWrapped(sb, "Attendance Status  : " + status.toUpperCase(Locale.ROOT));
        ConsoleUI.centerBlock(sb.toString());
        ConsoleUI.blank();
    }

    // ==================================================================
    // 2. UPDATE
    // ==================================================================
    public void updateAttendance() {
        ConsoleUI.heading("UPDATE ATTENDANCE RECORD");

        Node_attendance current = findRecordByPrompt();
        if (current == null) {
            done(false, "Attendance record not found.", "O(n)", LOOKUP_NOTE);
            return;
        }

        ConsoleUI.blank();
        printRecord(current);
        ConsoleUI.center("Press Enter to keep the current value. Type CANCEL to cancel.");
        ConsoleUI.blank();

        String name = ConsoleUI.ask("New student name (" + current.studentName + ")");
        if (ConsoleUI.isCancel(name)) {
            cancelUpdate();
            return;
        }

        // Re-ask until the time is valid, before the class ends, or left blank.
        LocalDateTime newStamp = null;
        while (true) {
            String text = ConsoleUI.ask("New attendance time ("
                    + ConsoleUI.format12(current.attendanceTime.toLocalTime()) + ")");
            if (text.isEmpty()) {
                break;
            }
            if (ConsoleUI.isCancel(text)) {
                cancelUpdate();
                return;
            }
            LocalTime time = ConsoleUI.parse12(text);
            if (time == null) {
                ConsoleUI.invalid("Invalid time format.");
                ConsoleUI.center("Please enter the time using hh:mm AM/PM.");
                continue;
            }
            LocalDateTime stamp = LocalDateTime.of(current.attendanceTime.toLocalDate(), time);
            if (classEnded(stamp)) {
                classEndedMessage(true);
                continue;
            }
            newStamp = stamp;
            break;
        }

        if (name.isEmpty() && newStamp == null) {
            done(true, "No changes were made.", "O(n)", LOOKUP_NOTE);
            return;
        }
        list.updateRecord(current, name.isEmpty() ? null : name, newStamp,
                newStamp == null ? null : determineStatus(newStamp));

        ConsoleUI.blank();
        printRecord(current);
        done(true, "Attendance record updated successfully.", "O(n)",
                "Lookup by ID O(n) + field updates O(1)");
    }

    private void cancelUpdate() {
        ConsoleUI.blank();
        ConsoleUI.cancelled("Nothing was updated.");
        ConsoleUI.complexity("O(n)", LOOKUP_NOTE);
    }

    // ==================================================================
    // 3. DELETE
    // ==================================================================
    public void deleteAttendance() {
        ConsoleUI.heading("DELETE ATTENDANCE RECORD");

        Node_attendance current = findRecordByPrompt();
        if (current == null) {
            done(false, "Attendance record not found.", "O(n)", LOOKUP_NOTE);
            return;
        }

        ConsoleUI.blank();
        ConsoleUI.center("Record being deleted:");
        printRecord(current);
        list.remove(current);   // reconnects both neighbours, fixes head/tail

        done(true, "Attendance record deleted successfully.", "O(n)",
                "Lookup by ID O(n) + unlinking the node O(1)");
    }

    // ==================================================================
    // 4. SEARCH
    // ==================================================================
    public void searchAttendance() {
        ConsoleUI.heading("SEARCH ATTENDANCE RECORD");

        String query = ConsoleUI.ask("Enter student ID or name to search");
        if (query.isEmpty()) {
            done(false, "Search cannot be empty.", "O(1)",
                    "Input validation only; the list is not traversed");
            return;
        }

        String lowered = query.toLowerCase(Locale.ROOT);
        int found = 0;
        for (Node_attendance cur = list.head; cur != null; cur = cur.next) {
            if (matches(cur, query, lowered)) {
                found++;
            }
        }
        if (found == 0) {
            done(false, "No matching attendance record found.", "O(n)",
                    "Every node is compared once");
            return;
        }

        String[][] rows = new String[found][];
        int i = 0;
        for (Node_attendance cur = list.head; cur != null; cur = cur.next) {
            if (matches(cur, query, lowered)) {
                rows[i++] = rowOf(cur);
            }
        }
        ConsoleUI.blank();
        printRows(rows);
        done(true, found + " matching record(s) found.", "O(n)", "Every node is compared once");
    }

    private boolean matches(Node_attendance node, String query, String lowered) {
        return node.studentId.equalsIgnoreCase(query)
                || node.studentName.toLowerCase(Locale.ROOT).contains(lowered);
    }

    // ==================================================================
    // 5 / 6. DISPLAY ALPHABETICALLY (A-Z and Z-A)
    // ==================================================================
    public void displayAlphabeticalAscending() {
        displayAlphabetical(true);
    }

    public void displayAlphabeticalDescending() {
        displayAlphabetical(false);
    }

    private void displayAlphabetical(boolean ascending) {
        ConsoleUI.heading(ascending
                ? "DISPLAY RECORDS ALPHABETICALLY (A-Z)"
                : "DISPLAY RECORDS ALPHABETICALLY (Z-A)");

        if (list.head == null) {
            printEmptyList(ascending ? "HEAD" : "TAIL");
            done(true, "Attendance list displayed (it is empty).", "O(1)",
                    "head is null, so no node is visited");
            return;
        }

        // Insertion-sort a COPY of the list by name. A-Z walks it head -> tail
        // (next links); Z-A walks it tail -> head (prev links).
        AttendanceList sorted = list.sortedCopyByName();
        printTable(sorted, ascending);
        printListVisualization(sorted, ascending, true);
        done(true, ascending
                ? "Records displayed alphabetically (A-Z)."
                : "Records displayed alphabetically (Z-A).",
                ConsoleUI.N_SQUARED,
                "Insertion sort into a sorted copy: " + ConsoleUI.N_SQUARED
                + " worst case, O(n) if already in order; then one O(n) traversal");
    }

    // ==================================================================
    // 7. SORT BY ATTENDANCE TIME (reorders the stored list)
    // ==================================================================
    public void sortByAttendanceTime() {
        ConsoleUI.heading("SORT BY ATTENDANCE TIME");

        if (list.head == null) {
            printEmptyList("HEAD");
            done(false, "At least two records are needed to sort.", "O(1)",
                    "Fewer than two nodes: nothing to compare");
            return;
        }
        if (list.head.next == null) {
            printTable(list, true);
            printListVisualization(list, true, false);
            done(false, "At least two records are needed to sort.", "O(1)",
                    "Fewer than two nodes: nothing to compare");
            return;
        }

        list.sortByAttendanceTime();   // real date-time comparison, not text
        printTable(list, true);
        printListVisualization(list, true, false);
        done(true, "Records sorted chronologically by attendance time.", ConsoleUI.N_SQUARED,
                "Bubble sort: " + ConsoleUI.N_SQUARED
                + " worst/average case, O(n) if already sorted");
    }

    // ==================================================================
    // 8. COUNT
    // ==================================================================
    public void countAttendance() {
        ConsoleUI.heading("COUNT ATTENDANCE RECORDS");
        ConsoleUI.center("Total attendance records: " + list.count());
        done(true, "Attendance records counted successfully.", "O(n)",
                "No counter is stored, so every node is visited once");
    }

    // ==================================================================
    // Attendance rules: status (grace period) and class-ended check
    // ==================================================================
    private String sessionStart() {
        return laboratory ? subject.labStart : subject.lectureStart;
    }

    private String sessionEnd() {
        return laboratory ? subject.labEnd : subject.lectureEnd;
    }

    /** Combines a date with a stored "HH:mm" time; null if the schedule is missing/invalid. */
    private LocalDateTime scheduledAt(LocalDate date, String hhmm) {
        try {
            return LocalDateTime.of(date, LocalTime.parse(hhmm));
        } catch (DateTimeParseException | NullPointerException ex) {
            return null;
        }
    }

    /**
     * Latest on-time arrival = class start + grace period (inclusive).
     * Arriving at that exact minute is On Time; one minute later is Late.
     */
    private String determineStatus(LocalDateTime arrival) {
        LocalDateTime start = scheduledAt(arrival.toLocalDate(), sessionStart());
        if (start == null) {
            return "Unclassified";   // schedule missing: do not invent a status
        }
        LocalDateTime latestOnTime = start.plusMinutes(subject.gracePeriod);
        // After the class has ended it is always LATE, even if a long grace period would not be over.
        return classEnded(arrival) || arrival.isAfter(latestOnTime) ? LATE : ON_TIME;
    }

    /**
     * The class has ended when the arrival is at or after the scheduled end.
     * The grace period only affects Late/On Time; it never extends the end.
     */
    private boolean classEnded(LocalDateTime arrival) {
        LocalDateTime end = scheduledAt(arrival.toLocalDate(), sessionEnd());
        return end != null && !arrival.isBefore(end);
    }

    /** canRetry = true only where the user types a time (Update), so a hint is shown. */
    private void classEndedMessage(boolean canRetry) {
        ConsoleUI.invalid("This class has already ended.");
        ConsoleUI.center("Attendance cannot be recorded.");
        ConsoleUI.center("The class ended at " + ConsoleUI.scheduleTime(sessionEnd())
                + (canRetry ? ". Enter an earlier time or type CANCEL." : "."));
    }

    // ==================================================================
    // Helpers
    // ==================================================================
    private Node_attendance findRecordByPrompt() {
        String id = ConsoleUI.ask("Enter student ID");
        return id.isEmpty() ? null : list.findById(id);
    }

    // ==================================================================
    // Output: result line, table, visualization
    // ==================================================================
    /** Prints "[OK]/[ERROR] message" followed by "Time Complexity: ...". */
    private void done(boolean success, String message, String bigO, String note) {
        ConsoleUI.blank();
        ConsoleUI.status(success, message);
        ConsoleUI.complexity(bigO, note);
    }

    private void cancelDone(String message) {
        ConsoleUI.blank();
        ConsoleUI.cancelled(message);
        ConsoleUI.complexity("O(n)", CANCEL_NOTE);
    }

    private String[] rowOf(Node_attendance r) {
        return new String[]{
            r.studentId,
            r.studentName,
            r.attendanceTime.toLocalDate().format(DISPLAY_DATE),
            ConsoleUI.format12(r.attendanceTime.toLocalTime()),
            r.status
        };
    }

    private void printRows(String[][] rows) {
        ConsoleUI.center("ATTENDANCE RECORDS");
        ConsoleUI.table(TABLE_HEADERS, rows, 1, 12);   // name column wraps if needed
    }

    /** Table of a whole list, walked forward (next links) or backward (prev links). */
    private void printTable(AttendanceList source, boolean forward) {
        String[][] rows = new String[source.count()][];
        int i = 0;
        for (Node_attendance cur = forward ? source.head : source.tail; cur != null;
                cur = forward ? cur.next : cur.prev) {
            rows[i++] = rowOf(cur);
        }
        printRows(rows);
    }

    private void printRecord(Node_attendance record) {
        printRows(new String[][]{rowOf(record)});
    }

    private void printEmptyList(String startLabel) {
        ConsoleUI.center("ATTENDANCE LIST IS EMPTY");
        ConsoleUI.blank();
        StringBuilder sb = new StringBuilder();
        ConsoleUI.addLine(sb, centerAt(startLabel, 12));
        ConsoleUI.addLine(sb, centerAt("|", 12));
        ConsoleUI.addLine(sb, centerAt("v", 12));
        ConsoleUI.addLine(sb, centerAt("NULL", 12));
        ConsoleUI.centerBlock(sb.toString());
        ConsoleUI.blank();
        ConsoleUI.center(startLabel.toLowerCase(Locale.ROOT)
                + " == null, so no attendance nodes are stored.");
    }

    private static String centerAt(String text, int width) {
        return ConsoleUI.pad((width - text.length()) / 2) + text;
    }

    /**
     * Draws the list that was just traversed. Forward: HEAD at the top, nodes
     * follow the next links, NULL at the bottom. Backward: TAIL at the top,
     * nodes follow the prev links. Every [n] label is checked against the real
     * references; a reference that does not match the traversal shows [?].
     */
    private void printListVisualization(AttendanceList shown, boolean forward, boolean sortedCopy) {
        ConsoleUI.blank();
        ConsoleUI.center("DOUBLY LINKED LIST VISUALIZATION");
        ConsoleUI.blank();

        int inner = Math.max(30, Math.min(44, ConsoleUI.WIDTH - 4));
        int width = inner + 2;

        StringBuilder sb = new StringBuilder();
        ConsoleUI.addLine(sb, centerAt(forward ? "HEAD" : "TAIL", width));
        ConsoleUI.addLine(sb, centerAt("|", width));
        ConsoleUI.addLine(sb, centerAt("v", width));

        int index = forward ? 1 : shown.count();
        Node_attendance before = null;   // node drawn just above the current one
        Node_attendance cur = forward ? shown.head : shown.tail;
        while (cur != null) {
            String prevRef;
            String nextRef;
            if (forward) {
                prevRef = cur.prev == null ? "NULL"
                        : cur.prev == before ? "[" + (index - 1) + "]" : "[?]";
                nextRef = cur.next == null ? "NULL" : "[" + (index + 1) + "]";
            } else {
                nextRef = cur.next == null ? "NULL"
                        : cur.next == before ? "[" + (index + 1) + "]" : "[?]";
                prevRef = cur.prev == null ? "NULL" : "[" + (index - 1) + "]";
            }
            appendNodeBox(sb, shown, cur, index, prevRef, nextRef, inner);

            Node_attendance following = forward ? cur.next : cur.prev;
            if (following != null) {
                appendLinkRows(sb, forward, width);
            }
            before = cur;
            cur = following;
            index += forward ? 1 : -1;
        }
        ConsoleUI.addLine(sb, centerAt("|", width));
        ConsoleUI.addLine(sb, centerAt("v", width));
        ConsoleUI.addLine(sb, centerAt("NULL", width));
        ConsoleUI.centerBlock(sb.toString());

        ConsoleUI.blank();
        if (forward) {
            ConsoleUI.center("Traversal: HEAD to TAIL, following each node's next link (v).");
            ConsoleUI.center("The ^ arrows are the prev links. [n] is the position counted from HEAD.");
        } else {
            ConsoleUI.center("Traversal: TAIL to HEAD, following each node's prev link (v).");
            ConsoleUI.center("The ^ arrows are the next links. [n] is the position counted from HEAD.");
        }
        if (sortedCopy) {
            ConsoleUI.center("This is a sorted copy; the stored attendance list keeps its own order.");
        }
    }

    private void appendNodeBox(StringBuilder sb, AttendanceList shown, Node_attendance node,
            int index, String prevRef, String nextRef, int inner) {
        boolean isHead = node == shown.head;
        boolean isTail = node == shown.tail;
        String tag = isHead && isTail ? "  <HEAD, TAIL>"
                : isHead ? "  <HEAD>"
                : isTail ? "  <TAIL>" : "";

        String border = "+" + "-".repeat(inner) + "+";
        ConsoleUI.addLine(sb, border);
        boxText(sb, "[" + index + "] " + node.studentId + " | " + node.studentName, inner);
        boxText(sb, node.attendanceTime.toLocalDate().format(DISPLAY_DATE) + " "
                + ConsoleUI.format12(node.attendanceTime.toLocalTime())
                + " | " + node.status, inner);
        boxText(sb, "prev: " + prevRef + "   next: " + nextRef + tag, inner);
        ConsoleUI.addLine(sb, border);
    }

    private void boxText(StringBuilder sb, String text, int inner) {
        for (String piece : ConsoleUI.wrap(text, inner - 2)) {
            ConsoleUI.addLine(sb, "| " + ConsoleUI.padRight(piece, inner - 2) + " |");
        }
    }

    /** Two connector rows between neighbouring nodes: one arrow down, one arrow up. */
    private void appendLinkRows(StringBuilder sb, boolean forward, int width) {
        String down = forward ? "next" : "prev";
        String up = forward ? "prev" : "next";
        int left = width / 2 - 5;
        int right = width / 2 + 5;

        ConsoleUI.addLine(sb, ConsoleUI.pad(left) + "|"
                + ConsoleUI.pad(right - left - 1) + "^");
        ConsoleUI.addLine(sb, ConsoleUI.pad(left - down.length() - 1) + down + " v"
                + ConsoleUI.pad(right - left - 1) + "| " + up);
    }
}