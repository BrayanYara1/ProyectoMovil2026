package com.example.gestionturnosapp.ui.especialidades

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gestionturnosapp.data.Especialidad
import com.example.gestionturnosapp.databinding.ItemEspecialidadBinding

class EspecialidadesAdapter(
    private val filteredList: List<Especialidad>,
    private val onItemClick: (Especialidad) -> Unit
) : RecyclerView.Adapter<EspecialidadesAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemEspecialidadBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEspecialidadBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredList[position]
        holder.binding.apply {
            tvEspecialidadNombre.text = root.context.getString(item.nombreRes)
            tvEspecialidadDesc.text = root.context.getString(item.descripcionRes)
            ivEspecialidadIcon.setImageResource(item.iconoResId)
            root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun getItemCount() = filteredList.size
}
