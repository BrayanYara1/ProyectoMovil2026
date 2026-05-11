package com.example.gestionturnosapp.ui.especialidades

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.gestionturnosapp.data.Especialidad
import com.example.gestionturnosapp.databinding.ItemEspecialidadBinding
import java.util.Locale

class EspecialidadesAdapter(
    private val fullList: List<Especialidad>,
    private val onItemClick: (Especialidad) -> Unit
) : RecyclerView.Adapter<EspecialidadesAdapter.ViewHolder>() {

    private var filteredList: List<Especialidad> = fullList

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

    fun filter(query: String, context: Context) {
        val lowercaseQuery = query.lowercase(Locale.getDefault())
        filteredList = if (lowercaseQuery.isEmpty()) {
            fullList
        } else {
            fullList.filter { especialidad ->
                val name = try {
                    context.getString(especialidad.nombreRes).lowercase(Locale.getDefault())
                } catch (e: Exception) { "" }
                name.contains(lowercaseQuery)
            }
        }
        notifyDataSetChanged()
    }
}
