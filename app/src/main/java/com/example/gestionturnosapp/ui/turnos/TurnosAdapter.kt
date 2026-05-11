package com.example.gestionturnosapp.ui.turnos

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestionturnosapp.R
import com.example.gestionturnosapp.data.Turno
import com.example.gestionturnosapp.databinding.ItemTurnoBinding
import java.text.SimpleDateFormat
import java.util.*

class TurnosAdapter(
    private val onTurnoClick: (Turno, ItemTurnoBinding) -> Unit,
    private val onDeleteClick: (Turno) -> Unit
) : ListAdapter<Turno, TurnosAdapter.TurnoViewHolder>(TurnoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TurnoViewHolder {
        val binding = ItemTurnoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TurnoViewHolder(binding, onTurnoClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: TurnoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TurnoViewHolder(
        private val binding: ItemTurnoBinding,
        private val onTurnoClick: (Turno, ItemTurnoBinding) -> Unit,
        private val onDeleteClick: (Turno) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.US)
        private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        private val inputFormats = listOf("hh:mm a", "h:mm a", "HH:mm")

        fun bind(turno: Turno) {
            binding.apply {
                tvItemNombre.text = turno.pacienteNombre
                tvItemMotivo.text = turno.motivo

                // Formatear Hora para mostrar AM/PM siempre
                try {
                    var dateHora: Date? = null
                    for (format in inputFormats) {
                        try {
                            val sdf = SimpleDateFormat(format, Locale.US)
                            dateHora = sdf.parse(turno.hora)
                            if (dateHora != null) break
                        } catch (e: Exception) {}
                    }
                    
                    if (dateHora != null) {
                        tvItemHora.text = displayTimeFormat.format(dateHora)
                    } else {
                        tvItemHora.text = turno.hora
                    }
                } catch (e: Exception) {
                    tvItemHora.text = turno.hora
                }

                // Formatear Fecha para el Icono de Calendario
                try {
                    val date = isoDateFormat.parse(turno.fecha)
                    if (date != null) {
                        val cal = Calendar.getInstance()
                        cal.time = date
                        tvItemDiaMes.text = cal.get(Calendar.DAY_OF_MONTH).toString()
                        tvItemMesCorta.text = monthFormat.format(date).uppercase()
                    }
                } catch (e: Exception) {
                    tvItemDiaMes.text = "??"
                    tvItemMesCorta.text = "ERR"
                }
                
                // Configurar estado visualmente con Material 3 Premium
                val (statusLabel, bgColor, textColor) = when(turno.estado.lowercase()) {
                    "completado", "completed" -> Triple(
                        root.context.getString(R.string.status_completed).uppercase(),
                        ContextCompat.getColor(root.context, R.color.status_completed_bg),
                        ContextCompat.getColor(root.context, R.color.status_completed_text)
                    )
                    "cancelado", "cancelled" -> Triple(
                        root.context.getString(R.string.status_cancelled).uppercase(),
                        ContextCompat.getColor(root.context, R.color.status_cancelled_bg),
                        ContextCompat.getColor(root.context, R.color.status_cancelled_text)
                    )
                    else -> Triple(
                        root.context.getString(R.string.status_pending).uppercase(),
                        ContextCompat.getColor(root.context, R.color.status_pending_bg),
                        ContextCompat.getColor(root.context, R.color.status_pending_text)
                    )
                }
                
                tvItemStatus.text = statusLabel
                tvItemStatus.chipBackgroundColor = ColorStateList.valueOf(bgColor)
                tvItemStatus.setTextColor(textColor)
                
                // Icono de estado dinámico
                val statusIcon = when(turno.estado.lowercase()) {
                    "completado", "completed" -> android.R.drawable.checkbox_on_background
                    "cancelado", "cancelled" -> android.R.drawable.ic_delete
                    else -> android.R.drawable.presence_online
                }
                tvItemStatus.setChipIconResource(statusIcon)
                tvItemStatus.chipIconTint = ColorStateList.valueOf(textColor)

                root.transitionName = "card_${turno.id}"
                tvItemNombre.transitionName = "name_${turno.id}"
                dateIconCard.transitionName = "date_${turno.id}"

                root.setOnClickListener { 
                    it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                    onTurnoClick(turno, binding) 
                }
                btnDeleteItem.setOnClickListener { 
                    it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    onDeleteClick(turno) 
                }
            }
        }
    }

    class TurnoDiffCallback : DiffUtil.ItemCallback<Turno>() {
        override fun areItemsTheSame(oldItem: Turno, newItem: Turno): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Turno, newItem: Turno): Boolean = oldItem == newItem
    }
}
