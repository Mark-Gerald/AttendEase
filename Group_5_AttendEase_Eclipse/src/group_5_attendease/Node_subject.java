package group_5_attendease;

public class Node_subject {

    String subject;
    String subjectCode;

    //Class day
    String day;

    // Lecture schedule
    String lectureStart;
    String lectureEnd;

    // Optional laboratory schedule
    boolean hasLab;
    String labStart;
    String labEnd;

    // Minutes after the class start that still count as on time (0 = strict)
    int gracePeriod;

    Node_subject prev;
    Node_subject next;

    // Each subject owns two attendance lists: one for lecture, one for laboratory
    final AttendanceList lectureAttendance = new AttendanceList();
    final AttendanceList labAttendance = new AttendanceList();

    // Subjects with complete schedule information
    Node_subject(String code, String subject, String day,
            String lectureStart, String lectureEnd,
            boolean hasLab, String labStart, String labEnd,
            int gracePeriod) {

        this.subjectCode = code;
        this.subject = subject;
        this.day = day;
        this.lectureStart = lectureStart;
        this.lectureEnd = lectureEnd;
        this.hasLab = hasLab;
        this.labStart = labStart;
        this.labEnd = labEnd;
        this.gracePeriod = gracePeriod;

        prev = null;
        next = null;
    }
}