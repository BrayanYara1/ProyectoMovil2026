package com.example.gestionturnosapp.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val isoDateFormat get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun getDisplayTimeFormat() = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val inputFormats = listOf("hh:mm a", "h:mm a", "HH:mm", "H:mm", "hh:mm a", "h:mm a")

    fun parseTime(time: String?): Date? {
        if (time.isNullOrBlank()) return null
        
        // Limpieza de formatos comunes en español
        val cleanTime = time.uppercase()
            .replace("A. M.", "AM")
            .replace("P. M.", "PM")
            .replace("A.M.", "AM")
            .replace("P.M.", "PM")
            .trim()

        for (fmt in inputFormats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(cleanTime)
                if (date != null) return date
            } catch (e: Exception) {}
        }

        for (fmt in inputFormats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                sdf.isLenient = false
                val date = sdf.parse(cleanTime)
                if (date != null) return date
            } catch (e: Exception) {}
        }
        
        return null
    }

    fun formatDisplayTime(time: String?): String {
        val dateObj = parseTime(time)
        return if (dateObj != null) getDisplayTimeFormat().format(dateObj) else time ?: ""
    }

    fun formatDisplayDate(context: android.content.Context, dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        
        return try {
            val date = isoDateFormat.parse(dateStr)
            if (date != null) {
                val calendar = Calendar.getInstance()
                val today = calendar.time
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val tomorrow = calendar.time
                
                val sdfDay = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                when (sdfDay.format(date)) {
                    sdfDay.format(today) -> context.getString(com.example.gestionturnosapp.R.string.today)
                    sdfDay.format(tomorrow) -> context.getString(com.example.gestionturnosapp.R.string.tomorrow)
                    else -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(date).replaceFirstChar { it.uppercase() }
                }
            } else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun isPastDateTime(fecha: String, hora: String): Boolean {
        return try {
            val dateObj = isoDateFormat.parse(fecha) ?: return true
            val timeStr = formatTo24h(hora)
            val timeParts = timeStr.split(":")
            
            val calendar = Calendar.getInstance()
            calendar.time = dateObj
            calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            calendar.set(Calendar.MINUTE, timeParts[1].toInt())
            calendar.set(Calendar.SECOND, 0)
            
            calendar.time.before(Date())
        } catch (e: Exception) {
            true
        }
    }

    fun formatTo24h(time: String): String {
        val date = parseTime(time)
        
        return if (date != null) {
            SimpleDateFormat("HH:mm", Locale.US).format(date)
        } else {
            // Manual fallback for cases like "3:00 PM"
            val cleanTime = time.uppercase().replace("A. M.", "AM").replace("P. M.", "PM")
            val isPM = cleanTime.contains("PM")
            val isAM = cleanTime.contains("AM")
            val digits = cleanTime.filter { it.isDigit() || it == ':' }.split(":")
            
            if (digits.size >= 2) {
                var h = digits[0].toIntOrNull() ?: 0
                val m = digits[1].take(2).padStart(2, '0')
                if (isPM && h < 12) h += 12
                if (isAM && h == 12) h = 0
                String.format(Locale.US, "%02d:%02d", h, m)
            } else time
        }
    }
}
