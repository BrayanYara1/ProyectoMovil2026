package com.example.gestionturnosapp.data

import androidx.annotation.StringRes
import androidx.annotation.DrawableRes

data class Especialidad(
    val id: Int,
    @StringRes val nombreRes: Int,
    @StringRes val descripcionRes: Int,
    @DrawableRes val iconoResId: Int
)
