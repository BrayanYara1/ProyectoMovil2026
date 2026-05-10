package com.example.gestionturnosapp.ui.medicamentos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestionturnosapp.data.Medicamento
import com.example.gestionturnosapp.databinding.ItemMedicationHomeBinding

class MedicamentosAdapter(
    private val onDeleteClick: (Medicamento) -> Unit
) : ListAdapter<Medicamento, MedicamentosAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicationHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemMedicationHomeBinding,
        private val onDeleteClick: (Medicamento) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(med: Medicamento) {
            binding.tvMedName.text = "${med.nombre} ${med.dosis}"
            binding.tvMedSchedule.text = "${med.frecuencia} - Próxima: ${med.proximaToma}"
            
            // Añadir soporte para clic largo para borrar
            binding.root.setOnLongClickListener {
                onDeleteClick(med)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Medicamento>() {
        override fun areItemsTheSame(oldItem: Medicamento, newItem: Medicamento): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Medicamento, newItem: Medicamento): Boolean = oldItem == newItem
    }
}
