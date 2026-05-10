package com.example.gestionturnosapp.ui.estudios

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.gestionturnosapp.data.EstudioMedico
import com.example.gestionturnosapp.databinding.ItemEstudioBinding

class EstudiosAdapter : ListAdapter<EstudioMedico, EstudiosAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEstudioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemEstudioBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(estudio: EstudioMedico) {
            binding.apply {
                tvEstudioTitulo.text = estudio.titulo
                tvEstudioTipo.text = estudio.tipo.uppercase()
                tvEstudioResultado.text = "Resultado: ${estudio.resultadoBreve}"
                tvEstudioFecha.text = estudio.fecha
                
                if (!estudio.urlDocumento.isNullOrEmpty()) {
                    ivEstudioAdjunto.visibility = View.VISIBLE
                    ivEstudioAdjunto.load(estudio.urlDocumento)
                } else {
                    ivEstudioAdjunto.visibility = View.GONE
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<EstudioMedico>() {
        override fun areItemsTheSame(oldItem: EstudioMedico, newItem: EstudioMedico): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: EstudioMedico, newItem: EstudioMedico): Boolean = oldItem == newItem
    }
}
