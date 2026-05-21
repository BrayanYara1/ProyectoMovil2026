package com.example.gestionturnosapp.ui.especialidades

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gestionturnosapp.data.Especialidad
import com.example.gestionturnosapp.databinding.ItemEspecialidadBinding

class EspecialidadesAdapter(
    private val onItemClick: (Especialidad) -> Unit
) : ListAdapter<Especialidad, EspecialidadesAdapter.ViewHolder>(EspecialidadDiffCallback()) {

    class ViewHolder(val binding: ItemEspecialidadBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEspecialidadBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            tvEspecialidadNombre.text = root.context.getString(item.nombreRes)
            tvEspecialidadDesc.text = root.context.getString(item.descripcionRes)
            ivEspecialidadIcon.setImageResource(item.iconoResId)
            root.setOnClickListener { onItemClick(item) }
        }
    }

    class EspecialidadDiffCallback : DiffUtil.ItemCallback<Especialidad>() {
        override fun areItemsTheSame(oldItem: Especialidad, newItem: Especialidad): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Especialidad, newItem: Especialidad): Boolean {
            return oldItem == newItem
        }
    }
}
