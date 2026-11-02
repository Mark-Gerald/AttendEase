package group_5_attendease;

import java.time.LocalTime;

public final class SubjectMethod {

    private static final String[] DAYS = {
        "Monday", "Tuesday", "Wednesday", "Thursday",
        "Friday", "Saturday", "Sunday"
    };
    private static final String[] TABLE_HEADERS
            = {"No.", "Code", "Subject", "Day", "Lecture", "Laboratory", "Grace Period"};

    // All subjects live in our own doubly linked list.
    private final SubjectList subjects = new SubjectList();

    // Sample subjects (schedules are stored internally as 24-hour HH:mm; last value = grace minutes)
    public SubjectMethod() {
        addSubject("IT204", "Data Structures and Algorithms",
                "Monday", "08:00", "10:00",
                true, "13:00", "16:00", 15);

        addSubject("ENT101", "Technoprenuership",
                "Tuesday", "09:00", "11:00",
                false, "", "", 30);

        addSubject("IM201", "Information Management",
                "Wednesday", "08:00", "10:00",
                true, "13:00", "15:00", 15);

        addSubject("MATH201", "Discrete Mathematics",
                "Thursday", "10:00", "12:00",
                false, "", "", 0);

        addSubject("NET201", "Networking",
                "Friday", "08:00", "10:00",
                true, "13:00", "16:00", 60);
    }

    // ------------------------------------------------------------------
    // Add a subject
    // ------------------------------------------------------------------
    public void addSubject(String code, String subject, String day,
            String lectureStart, String lectureEnd,
            boolean hasLab, String labStart, String labEnd, int gracePeriod) {
        subjects.insertAtTail(new Node_subject(code, subject, day,
                lectureStart, lectureEnd, hasLab, labStart, labEnd, gracePeriod));
    }

    public void addSubject() {
        ConsoleUI.heading("ADD SUBJECT");
        ConsoleUI.center("Type CANCEL at any prompt to cancel.");
        ConsoleUI.blank();

        String code = askNewCode();
        if (code == null) {
            cancelAdd();
            return;
        }
        String name = ConsoleUI.askRequired("Enter subject name", "Subject name");
        if (name == null) {
            cancelAdd();
            return;
        }
        String day = chooseDay("Choose lecture day");
        if (day == null) {
            cancelAdd();
            return;
        }
        ConsoleUI.blank();
        String[] lecture = askTimeRange("Lecture start time (hh:mm AM/PM)",
                "Lecture end time (hh:mm AM/PM)");
        if (lecture == null) {
            cancelAdd();
            return;
        }
        Integer grace = ConsoleUI.askNonNegativeInt("Grace period in minutes (0 for none)");
        if (grace == null) {
            cancelAdd();
            return;
        }
        Boolean hasLab = ConsoleUI.askYesNo("Does this subject have a laboratory? (Y/N)");
        if (hasLab == null) {
            cancelAdd();
            return;
        }

        String labStart = "";
        String labEnd = "";
        if (hasLab) {
            ConsoleUI.center("Laboratory day: " + day);
            String[] lab = askTimeRange("Laboratory start time (hh:mm AM/PM)",
                    "Laboratory end time (hh:mm AM/PM)");
            if (lab == null) {
                cancelAdd();
                return;
            }
            labStart = lab[0];
            labEnd = lab[1];
        }

        subjects.insertAtTail(new Node_subject(code, name, day,
                lecture[0], lecture[1], hasLab, labStart, labEnd, grace));

        StringBuilder details = new StringBuilder();
        ConsoleUI.addWrapped(details, "Code         : " + code);
        ConsoleUI.addWrapped(details, "Subject      : " + name);
        ConsoleUI.addWrapped(details, "Lecture      : " + day + " "
                + ConsoleUI.scheduleRange(lecture[0], lecture[1]));
        ConsoleUI.addWrapped(details, "Laboratory   : " + (hasLab
                ? day + " " + ConsoleUI.scheduleRange(labStart, labEnd) : "None"));
        ConsoleUI.addWrapped(details, "Grace Period : " + ConsoleUI.minutes(grace));
        ConsoleUI.blank();
        ConsoleUI.centerBlock(details.toString());

        ConsoleUI.blank();
        ConsoleUI.ok("Subject added successfully!");
        ConsoleUI.complexity("O(n)", "Duplicate-code check O(n) + insertion at the tail O(1)");
    }

    private String askNewCode() {
        while (true) {
            String code = ConsoleUI.askRequired("Enter subject code", "Subject code");
            if (code == null) {
                return null;
            }
            if (subjects.findByCode(code) == null) {
                return code;
            }
            ConsoleUI.invalid("Subject code already exists. Please enter a different code.");
        }
    }

    private void cancelAdd() {
        ConsoleUI.blank();
        ConsoleUI.cancelled("No subject was added.");
        ConsoleUI.complexity("O(n)", "Any duplicate-code check traverses the list");
    }

    // ------------------------------------------------------------------
    // View all subjects
    // ------------------------------------------------------------------
    public void viewAllSubjects() {
        ConsoleUI.heading("SUBJECT LIST");
        if (subjects.head == null) {
            ConsoleUI.error("No subjects available.");
            ConsoleUI.complexity("O(1)", "head is null, so no node is visited");
            return;
        }
        printSubjectTable();
        ConsoleUI.blank();
        ConsoleUI.ok("Subjects displayed successfully.");
        ConsoleUI.complexity("O(n)", "Every subject node is visited once");
    }

    private void printSubjectTable() {
        String[][] rows = new String[subjects.count()][];
        int i = 0;
        for (Node_subject cur = subjects.head; cur != null; cur = cur.next) {
            String lecture = cur.lectureStart == null
                    ? "-" : ConsoleUI.scheduleRange(cur.lectureStart, cur.lectureEnd);
            String lab = cur.hasLab
                    ? ConsoleUI.scheduleRange(cur.labStart, cur.labEnd) : "None";
            rows[i] = new String[]{
                String.valueOf(i + 1),
                cur.subjectCode == null ? "-" : cur.subjectCode,
                cur.subject == null ? "-" : cur.subject,
                cur.day == null ? "-" : cur.day,
                lecture,
                lab,
                ConsoleUI.minutes(cur.gracePeriod)
            };
            i++;
        }
        ConsoleUI.table(TABLE_HEADERS, rows, 2, 12, new int[]{4, 5});   // time columns narrow first, then the name wraps
    }

    // ------------------------------------------------------------------
    // Delete a subject (selected by list number)
    // ------------------------------------------------------------------
    public void deleteSubject() {
        ConsoleUI.heading("DELETE SUBJECT");
        if (subjects.head == null) {
            fail("No subjects available to delete.", "O(1)", "head is null");
            return;
        }
        printSubjectTable();
        ConsoleUI.blank();

        Integer choice = ConsoleUI.askInt("Enter the subject number (not the code) to delete");
        Node_subject current = choice == null ? null : subjects.nodeAt(choice);
        if (current == null) {
            fail("Invalid subject number.", "O(n)", "Locating a position traverses the list");
            return;
        }

        subjects.remove(current);   // reconnects both neighbours, fixes head/tail

        ConsoleUI.blank();
        ConsoleUI.ok("Subject \"" + current.subject + "\" deleted successfully.");
        ConsoleUI.complexity("O(n)", "Locating the position O(n) + unlinking the node O(1)");
    }

    // ------------------------------------------------------------------
    // Select a subject and open its attendance menu
    // ------------------------------------------------------------------
    public void selectSubject() {
        // Level 1: Subject List. Level 2: Choose Attendance List. Cancelling level 2 comes
        // back here; entering 0 at level 1 returns to the AttendEase main menu.
        while (true) {
            ConsoleUI.heading("SELECT SUBJECT");
            if (subjects.head == null) {
                fail("No subjects available.", "O(1)", "head is null");
                return;
            }
            printSubjectTable();
            ConsoleUI.blank();

            Node_subject current = askSubject();
            if (current == null) {
                return;   // cancelled: back to the main menu, nothing selected
            }

            AttendanceMethod.printSubjectInfo(current);
            boolean laboratory = false;
            ConsoleUI.blank();
            if (current.hasLab) {
                int pick = AttendanceMethod.chooseAttendanceList(current,
                        "Cancel / Return to Subject List");
                if (pick == 0) {
                    ConsoleUI.blank();
                    ConsoleUI.cancelled("Returning to the Subject List...");
                    continue;   // show the Subject List again
                }
                laboratory = pick == 2;
            } else {
                ConsoleUI.center("This subject has no laboratory. Opening Lecture attendance.");
            }

            new AttendanceMenu(new AttendanceMethod(current, laboratory)).showMenu();
            return;
        }
    }

    /**
     * Keeps asking for a subject number until it is valid (the node is looked up in the
     * linked list, no hardcoded count) or the user enters 0. Returns null if cancelled.
     */
    private Node_subject askSubject() {
        int lookups = 0;   // how many times the list was traversed
        while (true) {
            String text = ConsoleUI.ask("Enter the subject number to select (0 to Cancel)");
            int number = -1;
            boolean numeric = text.matches("-?\\d+");
            if (numeric) {
                try {
                    number = Integer.parseInt(text);
                } catch (NumberFormatException ex) {
                    numeric = false;   // too many digits for an int
                }
            }
            if (!numeric) {
                ConsoleUI.blank();
                ConsoleUI.error("Invalid input. Please enter a numeric subject number.");
                ConsoleUI.blank();
                continue;
            }
            if (number == 0) {
                ConsoleUI.blank();
                ConsoleUI.cancelled("Returning to AttendEase Main Menu...");
                ConsoleUI.complexity(lookups == 0 ? "O(1)" : "O(n)", lookups == 0
                        ? "Cancelled before any lookup"
                        : "Each attempt located a position by traversing the list");
                return null;
            }
            lookups++;
            Node_subject found = subjects.nodeAt(number);
            if (found != null) {
                ConsoleUI.blank();
                ConsoleUI.ok("Subject selected.");
                ConsoleUI.complexity("O(n)", "Locating the subject by position traverses the list");
                return found;
            }
            ConsoleUI.blank();
            ConsoleUI.error("Invalid subject number.");
            ConsoleUI.center("Please enter a valid subject number or 0 to cancel.");
            ConsoleUI.blank();
        }
    }

    // ------------------------------------------------------------------
    // Update a subject and its schedule
    // ------------------------------------------------------------------
    public void updateSubject() {
        ConsoleUI.heading("UPDATE SUBJECT");
        if (subjects.head == null) {
            fail("No subjects available to update.", "O(1)", "head is null");
            return;
        }
        printSubjectTable();
        ConsoleUI.blank();

        Integer choice = ConsoleUI.askInt("Enter the subject number to update");
        Node_subject current = choice == null ? null : subjects.nodeAt(choice);
        if (current == null) {
            fail("Invalid subject number.", "O(n)", "Locating a position traverses the list");
            return;
        }

        ConsoleUI.blank();
        ConsoleUI.center("Current subject: " + current.subject);
        ConsoleUI.center("Press Enter to keep the name. Type CANCEL at any prompt to cancel.");
        ConsoleUI.center("Nothing is saved until every answer is valid.");
        ConsoleUI.blank();

        // Collect and validate every answer first, then apply them together.
        String newName = ConsoleUI.ask("Enter new subject name");
        if (ConsoleUI.isCancel(newName)) {
            cancelUpdate();
            return;
        }

        String newDay = current.day;
        ConsoleUI.center("Current day: " + (current.day == null ? "Not assigned" : current.day));
        Boolean changeDay = ConsoleUI.askYesNo("Change lecture day? (Y/N)");
        if (changeDay == null) {
            cancelUpdate();
            return;
        }
        if (changeDay) {
            newDay = chooseDay("Choose new day");
            if (newDay == null) {
                cancelUpdate();
                return;
            }
        }

        String lecStart = current.lectureStart;
        String lecEnd = current.lectureEnd;
        Boolean changeLecture = ConsoleUI.askYesNo("Change lecture time? (Y/N)");
        if (changeLecture == null) {
            cancelUpdate();
            return;
        }
        if (changeLecture) {
            String[] range = askTimeRange("New lecture start (hh:mm AM/PM)",
                    "New lecture end (hh:mm AM/PM)");
            if (range == null) {
                cancelUpdate();
                return;
            }
            lecStart = range[0];
            lecEnd = range[1];
        }

        int newGrace = current.gracePeriod;
        ConsoleUI.center("Current grace period: " + ConsoleUI.minutes(current.gracePeriod));
        Boolean changeGrace = ConsoleUI.askYesNo("Change grace period? (Y/N)");
        if (changeGrace == null) {
            cancelUpdate();
            return;
        }
        if (changeGrace) {
            Integer grace = ConsoleUI.askNonNegativeInt("New grace period in minutes (0 for none)");
            if (grace == null) {
                cancelUpdate();
                return;
            }
            newGrace = grace;
        }

        boolean hasLab = current.hasLab;
        String labStart = current.labStart;
        String labEnd = current.labEnd;
        Boolean changeLab = ConsoleUI.askYesNo("Change laboratory details? (Y/N)");
        if (changeLab == null) {
            cancelUpdate();
            return;
        }
        if (changeLab) {
            Boolean labAnswer = ConsoleUI.askYesNo("Does this subject have a lab? (Y/N)");
            if (labAnswer == null) {
                cancelUpdate();
                return;
            }
            if (labAnswer) {
                String[] range = askTimeRange("Laboratory start (hh:mm AM/PM)",
                        "Laboratory end (hh:mm AM/PM)");
                if (range == null) {
                    cancelUpdate();
                    return;
                }
                hasLab = true;
                labStart = range[0];
                labEnd = range[1];
            } else {
                hasLab = false;
                labStart = "";
                labEnd = "";
            }
        }

        if (!newName.isEmpty()) {
            current.subject = newName;
        }
        current.day = newDay;
        current.lectureStart = lecStart;
        current.lectureEnd = lecEnd;
        current.gracePeriod = newGrace;
        current.hasLab = hasLab;
        current.labStart = labStart;
        current.labEnd = labEnd;

        ConsoleUI.blank();
        ConsoleUI.ok("Subject updated successfully!");
        ConsoleUI.complexity("O(n)", "Locating the position O(n) + field updates O(1)");
    }

    private void cancelUpdate() {
        ConsoleUI.blank();
        ConsoleUI.cancelled("Nothing was updated.");
        ConsoleUI.complexity("O(n)", "Locating the position traverses the list");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    /** Shows the day menu and re-asks until a valid day number is typed; null if cancelled. */
    private String chooseDay(String label) {
        String[] items = new String[DAYS.length];
        for (int i = 0; i < DAYS.length; i++) {
            items[i] = "(" + (i + 1) + ") " + DAYS[i];
        }
        ConsoleUI.blank();
        ConsoleUI.menu(items);
        Integer pick = ConsoleUI.askChoice(label + " - enter a number", 1, DAYS.length);
        return pick == null ? null : DAYS[pick - 1];
    }

    /**
     * Asks a start and an end time (hh:mm AM/PM), re-asking the end time until
     * it is later than the start. Returns {start, end} as "HH:mm", or null if cancelled.
     */
    private String[] askTimeRange(String startLabel, String endLabel) {
        LocalTime start = ConsoleUI.askTime12(startLabel);
        if (start == null) {
            return null;
        }
        while (true) {
            LocalTime end = ConsoleUI.askTime12(endLabel);
            if (end == null) {
                return null;
            }
            if (end.isAfter(start)) {
                return new String[]{ConsoleUI.to24(start), ConsoleUI.to24(end)};
            }
            ConsoleUI.invalid("The end time must be later than the start time ("
                    + ConsoleUI.format12(start) + ").");
        }
    }

    private void fail(String message, String bigO, String note) {
        ConsoleUI.blank();
        ConsoleUI.error(message);
        ConsoleUI.complexity(bigO, note);
    }
}