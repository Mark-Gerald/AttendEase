package group_5_attendease;

import java.time.LocalDateTime;

class Node_attendance {

    String studentId;
    String studentName;
    LocalDateTime attendanceTime;
    String status;

    Node_attendance prev;
    Node_attendance next;

    Node_attendance(String studentId, String studentName,
                    LocalDateTime attendanceTime, String status) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.attendanceTime = attendanceTime;
        this.status = status;
        prev = null;
        next = null;
    }

    /** A new, unlinked node holding the same data (used to build sorted views). */
    Node_attendance copy() {
        return new Node_attendance(studentId, studentName, attendanceTime, status);
    }
}