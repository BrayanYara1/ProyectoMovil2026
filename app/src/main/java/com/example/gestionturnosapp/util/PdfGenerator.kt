package com.example.gestionturnosapp.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.Medicamento
import com.example.gestionturnosapp.data.Turno
import com.example.gestionturnosapp.data.Usuario
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    fun generateHealthReport(
        context: Context,
        usuario: Usuario?,
        turnos: List<Turno>,
        meds: List<Medicamento>,
    ): Uri? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        
        var y = 50f
        val margin = 40f
        val contentWidth = 595f - (margin * 2)

        // HEADER
        paint.color = context.getColor(R.color.primary)
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText(context.getString(R.string.app_name), margin, y, paint)
        
        y += 30f
        paint.color = Color.BLACK
        paint.textSize = 18f
        canvas.drawText(context.getString(R.string.pdf_title), margin, y, paint)
        
        y += 40f
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText(context.getString(R.string.pdf_generated_at, dateFormat.format(Date())), margin, y, paint)

        // USUARIO
        y += 30f
        paint.isFakeBoldText = true
        canvas.drawText(context.getString(R.string.pdf_patient), margin, y, paint)
        paint.isFakeBoldText = false
        canvas.drawText(usuario?.nombre ?: context.getString(R.string.label_anonymous), margin + 70f, y, paint)
        
        y += 20f
        canvas.drawText("${context.getString(R.string.hint_email)}: ${usuario?.email ?: "N/A"}", margin, y, paint)

        // SECCIÓN TURNOS
        y += 50f
        paint.color = context.getColor(R.color.primary)
        paint.isFakeBoldText = true
        paint.textSize = 16f
        canvas.drawText(context.getString(R.string.pdf_appointments_title), margin, y, paint)
        
        y += 10f
        canvas.drawLine(margin, y, margin + contentWidth, y, paint)
        
        y += 25f
        paint.color = Color.BLACK
        paint.textSize = 12f
        if (turnos.isEmpty()) {
            canvas.drawText(context.getString(R.string.pdf_no_appointments), margin, y, paint)
            y += 20f
        } else {
            turnos.take(10).forEach { turno ->
                paint.isFakeBoldText = true
                canvas.drawText("${turno.especialidad} - ${turno.doctor}", margin, y, paint)
                y += 18f
                paint.isFakeBoldText = false
                val displayDate = DateUtils.formatDisplayDate(context, turno.fecha)
                canvas.drawText("$displayDate a las ${turno.hora}", margin + 10f, y, paint)
                y += 25f
            }
        }

        // SECCIÓN MEDICAMENTOS
        y += 30f
        paint.color = context.getColor(R.color.primary)
        paint.isFakeBoldText = true
        paint.textSize = 16f
        canvas.drawText(context.getString(R.string.pdf_meds_title), margin, y, paint)
        
        y += 10f
        canvas.drawLine(margin, y, margin + contentWidth, y, paint)
        
        y += 25f
        paint.color = Color.BLACK
        paint.textSize = 12f
        if (meds.isEmpty()) {
            canvas.drawText(context.getString(R.string.pdf_no_meds), margin, y, paint)
            y += 20f
        } else {
            meds.take(15).forEach { med ->
                paint.isFakeBoldText = true
                canvas.drawText("${med.nombre} (${med.dosis})", margin, y, paint)
                y += 18f
                paint.isFakeBoldText = false
                canvas.drawText("${context.getString(R.string.hint_med_freq)}: ${med.frecuencia}", margin + 10f, y, paint)
                y += 25f
            }
        }

        // FOOTER
        paint.color = Color.GRAY
        paint.textSize = 10f
        canvas.drawText(context.getString(R.string.pdf_footer), margin, 820f, paint)

        pdfDocument.finishPage(page)

        // SAVE FILE
        val fileName = "Resumen_Salud_${System.currentTimeMillis()}.pdf"
        val file = File(context.cacheDir, fileName)
        
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            FileProvider.getUriForFile(context, "com.example.gestionturnosapp.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
