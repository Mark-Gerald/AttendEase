package group_5_attendease;

public class AttendanceMenu {

    private static final String[] ITEMS = {
        "(1) Add Attendance Record", "(2) Update Attendance Record",
        "(3) Delete Attendance Record", "(4) Search Attendance Record",
        "(5) Display Records Alphabetically (A-Z)", "(6) Display Records Alphabetically (Z-A)",
        "(7) Sort by Attendance Time", "(8) Count Attendance Records",
        "(9) Change Attendance Schedule", "(10) Return to Main Menu",
        "(11) Exit"
    };

    private final AttendanceMethod attendance;

    public AttendanceMenu(AttendanceMethod attendance) {
        this.attendance = attendance;
    }

    public void showMenu() {
        boolean running = true;
        while (running) {
            ConsoleUI.heading("ATTENDANCE MANAGEMENT SYSTEM");
            ConsoleUI.center(attendance.contextLabel());   // which subject / list is active
            ConsoleUI.blank();
            ConsoleUI.menu(ITEMS);

            String line = ConsoleUI.ask("Enter a number");
            int choice;
            try {
                choice = Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                invalidChoice(line);
                continue;
            }

            // Each operation prints its own result and "Time Complexity" line.
            switch (choice) {
                case 1 -> attendance.addAttendance();
                case 2 -> attendance.updateAttendance();
                case 3 -> attendance.deleteAttendance();
                case 4 -> attendance.searchAttendance();
                case 5 -> attendance.displayAlphabeticalAscending();
                case 6 -> attendance.displayAlphabeticalDescending();
                case 7 -> attendance.sortByAttendanceTime();
                case 8 -> attendance.countAttendance();
                case 9 -> attendance.changeAttendanceSchedule();
                case 10 -> {
                    ConsoleUI.ok("Returning to the main menu.");
                    running = false;
                }
                case 11 -> {
                    ConsoleUI.ok("Exiting AttendEase.");
                    System.exit(0);
                }
                default -> invalidChoice(line);
            }
        }
    }

    private void invalidChoice(String line) {
        String shown = line.isEmpty() ? "Nothing was entered." : "'" + line + "' is not a menu option.";
        ConsoleUI.error(shown + " Enter a number from 1 to 11.");
    }
}