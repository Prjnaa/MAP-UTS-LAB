package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.db.models

data class Attendance(val userId: String = "", val attendanceList: List<AttendanceItem> = listOf())

data class AttendanceItem(
    val date: String = "",
    val checkInTime: String = "",
    var checkOutTime: String = "",
    var checkInPhotoUrl: String = "",
    val checkOutPhotoUrl: String = ""
)
