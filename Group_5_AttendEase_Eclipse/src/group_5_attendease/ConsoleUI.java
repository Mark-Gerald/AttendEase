package group_5_attendease;

import java.nio.charset.Charset;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reusable console helpers shared by every class in AttendEase: centering,
 * headings, menus, tables, prompts with re-asking, 12-hour time handling and
 * time-complexity labels.
 *
 * Display text is built with StringBuilder and String arrays only. No
 * java.util collection is used anywhere in the project.
 */
final class ConsoleUI {

    /** Display width. Reads the COLUMNS environment variable if present, otherwise 100. */
    static final int WIDTH = detectWidth();

    /** Width of a standard "form" block (headings, prompts). */
    static final int FORM_WIDTH = 60;

    /** Printed form of O(n squared); falls back to O(n^2) if the console cannot show the superscript. */
    static final String N_SQUARED = canPrintSuperscript() ? "O(n\u00B2)" : "O(n^2)";

    // One shared Scanner for the whole program (several Scanners on System.in can lose input).
    private static final Scanner INPUT = new Scanner(System.in);

    // 12-hour clock: hour 1-12 (leading zero optional), minutes 00-59, then AM or PM.
    private static final Pattern TIME_12H
            = Pattern.compile("^(0?[1-9]|1[0-2]):([0-5][0-9])\\s*(AM|PM)$");
    private static final DateTimeFormatter TIME_24H
            = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    // Left margin of the most recently printed block; prompts line up with it.
    private static int blockLeft = Math.max(0, (WIDTH - FORM_WIDTH) / 2);

    private ConsoleUI() {
    }

    private static boolean canPrintSuperscript() {
        String encoding = System.getProperty("stdout.encoding");
        if (encoding == null) {
            encoding = System.getProperty("sun.stdout.encoding");
        }
        if (encoding == null) {
            encoding = Charset.defaultCharset().name();
        }
        try {
            return Charset.forName(encoding).newEncoder().canEncode('\u00B2');
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static int detectWidth() {
        int width = 100;
        try {
            String columns = System.getenv("COLUMNS");
            if (columns != null) {
                width = Integer.parseInt(columns.trim());
            }
        } catch (NumberFormatException | SecurityException ex) {
            width = 100;
        }
        return Math.max(40, Math.min(width, 140));
    }

    // ------------------------------------------------------------------
    // Basic text helpers
    // ------------------------------------------------------------------
    static String pad(int count) {
        return " ".repeat(Math.max(0, count));
    }

    static String padRight(String text, int width) {
        return text.length() >= width ? text : text + pad(width - text.length());
    }

    /** Shortens text with "..." when it is longer than width. */
    static String fit(String value, int width) {
        if (value == null) {
            return "-";
        }
        if (value.length() <= width) {
            return value;
        }
        if (width <= 3) {
            return value.substring(0, Math.max(0, width));
        }
        return value.substring(0, width - 3) + "...";
    }

    /** Appends one line to a text block (every line ends with a newline). */
    static void addLine(StringBuilder block, String line) {
        block.append(line).append('\n');
    }

    /** Appends a line, word-wrapped to fit the display. */
    static void addWrapped(StringBuilder block, String line) {
        for (String piece : wrap(line, WIDTH - 4)) {
            addLine(block, piece);
        }
    }

    /** Word-wraps text to the given width; words longer than width are cut. */
    static String[] wrap(String text, int width) {
        int max = Math.max(1, width);
        StringBuilder out = new StringBuilder();
        StringBuilder line = new StringBuilder();
        for (String word : (text == null ? "" : text).split(" ", -1)) {
            while (word.length() > max) {
                if (line.length() > 0) {
                    out.append(line).append('\n');
                    line.setLength(0);
                }
                out.append(word, 0, max).append('\n');
                word = word.substring(max);
            }
            if (line.length() == 0) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= max) {
                line.append(' ').append(word);
            } else {
                out.append(line).append('\n');
                line.setLength(0);
                line.append(word);
            }
        }
        out.append(line);
        return out.toString().split("\n", -1);
    }

    static void blank() {
        System.out.println();
    }

    static String minutes(int value) {
        return value + (value == 1 ? " minute" : " minutes");
    }

    // ------------------------------------------------------------------
    // Centered output
    // ------------------------------------------------------------------
    /** Centers one piece of text (wrapping it if it is wider than the display). */
    static void center(String text) {
        for (String line : wrap(text, WIDTH)) {
            System.out.println(line.isEmpty() ? "" : pad((WIDTH - line.length()) / 2) + line);
        }
    }

    /**
     * Centers a text block (lines ended by '\n') as ONE block, so every line
     * keeps the same left edge (menus, tables, diagrams).
     */
    static void centerBlock(String block) {
        int longest = 0;
        int start = 0;
        while (start < block.length()) {
            int end = block.indexOf('\n', start);
            if (end < 0) {
                end = block.length();
            }
            longest = Math.max(longest, end - start);
            start = end + 1;
        }
        blockLeft = Math.max(0, (WIDTH - longest) / 2);
        start = 0;
        while (start < block.length()) {
            int end = block.indexOf('\n', start);
            if (end < 0) {
                end = block.length();
            }
            String line = block.substring(start, end);
            System.out.println(line.isEmpty() ? "" : pad(blockLeft) + line);
            start = end + 1;
        }
    }

    /** Boxed, centered section title. */
    static void heading(String title) {
        int width = Math.min(WIDTH, Math.max(FORM_WIDTH, title.length() + 6));
        int inner = width - 2;
        String text = fit(title, width - 4);
        int left = (inner - text.length()) / 2;

        StringBuilder box = new StringBuilder();
        addLine(box, "+" + "=".repeat(inner) + "+");
        addLine(box, "|" + pad(left) + text + pad(inner - left - text.length()) + "|");
        addLine(box, "+" + "=".repeat(inner) + "+");
        blank();
        centerBlock(box.toString());
        blank();
    }

    // ------------------------------------------------------------------
    // Status and complexity messages
    // ------------------------------------------------------------------
    static void status(boolean success, String message) {
        center((success ? "[OK] " : "[ERROR] ") + message);
    }

    static void ok(String message) {
        status(true, message);
    }

    static void error(String message) {
        status(false, message);
    }

    /** Message for a rejected input value; the user is asked again. */
    static void invalid(String message) {
        center("[INVALID] " + message);
    }

    /** Message that needs the user's attention before continuing. */
    static void warning(String message) {
        center("[WARNING] " + message);
    }

    static void cancelled(String message) {
        center("[CANCELLED] " + message);
    }

    /** Prints the theoretical time complexity (never elapsed time). */
    static void complexity(String bigO, String note) {
        center("Time Complexity: " + bigO);
        if (note != null && !note.isEmpty()) {
            center("(" + note + ")");
        }
    }

    // ------------------------------------------------------------------
    // Menus
    // ------------------------------------------------------------------
    /**
     * Prints menu items in two aligned columns (items 1-2 on the first row,
     * 3-4 on the second, and so on; an odd last item gets its own row).
     * If the two columns do not fit the display, items are listed vertically.
     */
    static void menu(String[] items) {
        int leftWidth = 0;
        int rightWidth = 0;
        for (int i = 0; i < items.length; i++) {
            if (i % 2 == 0) {
                leftWidth = Math.max(leftWidth, items[i].length());
            } else {
                rightWidth = Math.max(rightWidth, items[i].length());
            }
        }
        int gap = 4;
        boolean twoColumns = leftWidth + gap + rightWidth <= WIDTH - 2;

        StringBuilder lines = new StringBuilder();
        if (twoColumns) {
            for (int i = 0; i < items.length; i += 2) {
                if (i + 1 < items.length) {
                    addLine(lines, padRight(items[i], leftWidth + gap) + items[i + 1]);
                } else {
                    addLine(lines, items[i]);
                }
            }
        } else {
            for (String item : items) {
                addLine(lines, item);
            }
        }
        centerBlock(lines.toString());
        blank();
    }

    // ------------------------------------------------------------------
    // Tables
    // ------------------------------------------------------------------
    /**
     * Prints a bordered table centered on screen. Column widths follow the
     * content. If the table is wider than the display, only the flexible
     * column shrinks and its text wraps onto extra lines; if it still does
     * not fit, each row is printed as a small card instead.
     * (rows is only a display buffer filled from the linked list.)
     */
    static void table(String[] headers, String[][] rows, int flexColumn, int flexMinWidth) {
        table(headers, rows, flexColumn, flexMinWidth, new int[0]);
    }

    /**
     * Same as above, with "compact" columns (time ranges such as 08:00 AM-10:00 AM).
     * When the flexible column would become too narrow, these columns are
     * narrowed first and wrap at the dash, so long names stay readable.
     */
    static void table(String[] headers, String[][] rows, int flexColumn, int flexMinWidth,
            int[] compactColumns) {
        int columns = headers.length;
        int[] widths = new int[columns];
        for (int c = 0; c < columns; c++) {
            widths[c] = headers[c].length();
            for (String[] row : rows) {
                widths[c] = Math.max(widths[c], cell(row[c]).length());
            }
        }

        int natural = widths[flexColumn];
        int total = tableWidth(widths);
        if (total > WIDTH) {
            int comfortable = Math.min(natural, Math.max(flexMinWidth, 22));
            widths[flexColumn] = Math.max(flexMinWidth, natural - (total - WIDTH));
            if (widths[flexColumn] < comfortable && compactColumns.length > 0) {
                widths[flexColumn] = natural;
                for (int c : compactColumns) {
                    widths[c] = Math.min(widths[c], Math.max(9, headers[c].length()));
                }
                widths[flexColumn] = Math.max(flexMinWidth,
                        natural - (tableWidth(widths) - WIDTH));
            }
            total = tableWidth(widths);
        }
        if (total > WIDTH) {
            cards(headers, rows);
            return;
        }

        boolean wrappedRows = false;
        for (String[] row : rows) {
            for (int c = 0; c < columns; c++) {
                if (cell(row[c]).length() > widths[c]) {
                    wrappedRows = true;
                }
            }
        }

        String border = border(widths);
        StringBuilder out = new StringBuilder();
        addLine(out, border);
        addLine(out, tableLine(headers, widths));
        addLine(out, border);
        for (String[] row : rows) {
            String[][] parts = new String[columns][];
            int height = 1;
            for (int c = 0; c < columns; c++) {
                String text = cell(row[c]);
                if (text.length() > widths[c] && isCompact(compactColumns, c)) {
                    text = text.replace("-", "- ");   // lets a time range break after the dash
                }
                parts[c] = wrap(text, widths[c]);
                height = Math.max(height, parts[c].length);
            }
            for (int k = 0; k < height; k++) {
                String[] line = new String[columns];
                for (int c = 0; c < columns; c++) {
                    line[c] = k < parts[c].length ? parts[c][k] : "";
                }
                addLine(out, tableLine(line, widths));
            }
            if (wrappedRows) {
                addLine(out, border);   // separate rows when some of them wrap
            }
        }
        if (!wrappedRows) {
            addLine(out, border);
        }
        centerBlock(out.toString());
    }

    private static boolean isCompact(int[] compactColumns, int column) {
        for (int c : compactColumns) {
            if (c == column) {
                return true;
            }
        }
        return false;
    }

    private static String cell(String value) {
        return value == null ? "-" : value;
    }

    private static int tableWidth(int[] widths) {
        int total = 1;
        for (int w : widths) {
            total += w + 3;
        }
        return total;
    }

    private static String border(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int w : widths) {
            sb.append("-".repeat(w + 2)).append('+');
        }
        return sb.toString();
    }

    private static String tableLine(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int c = 0; c < widths.length; c++) {
            sb.append(' ').append(padRight(cells[c], widths[c])).append(" |");
        }
        return sb.toString();
    }

    private static void cards(String[] headers, String[][] rows) {
        int labelWidth = 0;
        for (String h : headers) {
            labelWidth = Math.max(labelWidth, h.length());
        }
        int valueWidth = Math.max(10, WIDTH - labelWidth - 5);
        StringBuilder out = new StringBuilder();
        int number = 1;
        for (String[] row : rows) {
            addLine(out, "--- Record " + number++ + " ---");
            for (int c = 0; c < headers.length; c++) {
                String[] pieces = wrap(cell(row[c]), valueWidth);
                for (int k = 0; k < pieces.length; k++) {
                    String label = k == 0 ? padRight(headers[c], labelWidth) : pad(labelWidth);
                    addLine(out, label + " : " + pieces[k]);
                }
            }
            addLine(out, "");
        }
        centerBlock(out.toString());
    }

    // ------------------------------------------------------------------
    // Input (every askXxx re-asks until the value is valid; typing CANCEL returns null)
    // ------------------------------------------------------------------
    /** Prints "label >> " lined up with the current block and returns the trimmed answer. */
    static String ask(String label) {
        System.out.print(pad(blockLeft) + label + " >> ");
        if (!INPUT.hasNextLine()) {
            blank();
            ok("No more input. Exiting AttendEase.");
            System.exit(0);
        }
        return INPUT.nextLine().trim();
    }

    static boolean isCancel(String text) {
        return text.equalsIgnoreCase("cancel");
    }

    /** Returns the whole number typed, or null if the answer is not a whole number. */
    static Integer askInt(String label) {
        try {
            return Integer.parseInt(ask(label));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Re-asks until something is typed. Returns null if the user cancels. */
    static String askRequired(String label, String fieldName) {
        while (true) {
            String text = ask(label);
            if (isCancel(text)) {
                return null;
            }
            if (!text.isEmpty()) {
                return text;
            }
            invalid(fieldName + " cannot be empty. Please try again.");
        }
    }

    /** Re-asks until a valid hh:mm AM/PM time is typed. Returns null if the user cancels. */
    static LocalTime askTime12(String label) {
        while (true) {
            String text = ask(label);
            if (isCancel(text)) {
                return null;
            }
            if (text.isEmpty()) {
                invalid("Time cannot be empty.");
                center("Please enter the time using hh:mm AM/PM.");
                continue;
            }
            LocalTime time = parse12(text);
            if (time != null) {
                return time;
            }
            invalid("Invalid time format.");
            center("Please enter the time using hh:mm AM/PM.");
        }
    }

    /** Re-asks until a whole number >= 0 is typed. Returns null if the user cancels. */
    static Integer askNonNegativeInt(String label) {
        while (true) {
            String text = ask(label);
            if (isCancel(text)) {
                return null;
            }
            if (text.matches("\\d+")) {
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException ex) {
                    // too large for an int: fall through to the message below
                }
            }
            invalid("Enter a whole number of minutes (0 or more), such as 0, 15, 30 or 60.");
        }
    }

    /** Re-asks until Y or N is typed. Returns null if the user cancels. */
    static Boolean askYesNo(String label) {
        while (true) {
            String text = ask(label);
            if (isCancel(text)) {
                return null;
            }
            if (text.equalsIgnoreCase("Y")) {
                return Boolean.TRUE;
            }
            if (text.equalsIgnoreCase("N")) {
                return Boolean.FALSE;
            }
            invalid("Please enter Y or N.");
        }
    }

    /** Re-asks until a number from min to max is typed. Returns null if the user cancels. */
    static Integer askChoice(String label, int min, int max) {
        while (true) {
            String text = ask(label);
            if (isCancel(text)) {
                return null;
            }
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ex) {
                // handled by the message below
            }
            invalid((text.isEmpty() ? "Nothing was entered." : "'" + text + "' is not a valid choice.")
                    + " Enter a number from " + min + " to " + max + ".");
        }
    }

    // ------------------------------------------------------------------
    // 12-hour (AM/PM) time handling - no time zones involved
    // ------------------------------------------------------------------
    /** Parses "hh:mm AM/PM" (any capitalization). Returns null when invalid. */
    static LocalTime parse12(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = TIME_12H.matcher(text.trim().toUpperCase(Locale.ROOT));
        if (!m.matches()) {
            return null;
        }
        int hour = Integer.parseInt(m.group(1)) % 12;   // 12 AM -> 0, 12 PM -> 12 after +12
        if (m.group(3).equals("PM")) {
            hour += 12;
        }
        return LocalTime.of(hour, Integer.parseInt(m.group(2)));
    }

    /** Formats as hh:mm AM/PM, e.g. 01:45 PM. */
    static String format12(LocalTime time) {
        int hour = time.getHour() % 12;
        if (hour == 0) {
            hour = 12;
        }
        return String.format("%02d:%02d %s", hour, time.getMinute(),
                time.getHour() < 12 ? "AM" : "PM");
    }

    /** Converts to the "HH:mm" text the subject schedule stores internally. */
    static String to24(LocalTime time) {
        return time.format(TIME_24H);
    }

    /** Displays a stored "HH:mm" schedule value as 12-hour time. */
    static String scheduleTime(String hhmm) {
        try {
            return format12(LocalTime.parse(hhmm));
        } catch (DateTimeParseException | NullPointerException ex) {
            return hhmm == null ? "-" : hhmm;
        }
    }

    static String scheduleRange(String start, String end) {
        return scheduleTime(start) + "-" + scheduleTime(end);
    }
}