package group_5_attendease;

public class Group_5_AttendEase {

    private static final String[] ITEMS = {
        "(1) View all subjects", "(2) Create a subject attendance list",
        "(3) Delete a subject attendance list", "(4) Update a subject attendance list",
        "(5) Select a subject attendance list", "(6) Exit"
    };

    public static void main(String[] args) {
        SubjectMethod subjects = new SubjectMethod();
        boolean running = true;

        while (running) {
            ConsoleUI.heading("AttendEase");
            ConsoleUI.menu(ITEMS);

            String line = ConsoleUI.ask("Enter a number");
            int choice;
            try {
                choice = Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                invalidChoice(line);
                continue;
            }

            switch (choice) {
                case 1 -> subjects.viewAllSubjects();
                case 2 -> subjects.addSubject();
                case 3 -> subjects.deleteSubject();
                case 4 -> subjects.updateSubject();
                case 5 -> subjects.selectSubject();
                case 6 -> {
                    ConsoleUI.ok("Exiting AttendEase...");
                    running = false;
                }
                default -> invalidChoice(line);
            }
        }
    }

    private static void invalidChoice(String line) {
        String shown = line.isEmpty() ? "Nothing was entered." : "'" + line + "' is not a menu option.";
        ConsoleUI.error(shown + " Enter a number from 1 to 6.");
    }
}