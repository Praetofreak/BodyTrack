package com.project.myscale.data.model

enum class BackupInterval(val days: Long) {
    OFF(0),
    DAILY(1),
    WEEKLY(7),
    MONTHLY(30)
}
