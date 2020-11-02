package com.rdc.fittracker.model

/**
 * Model with the basic data needed to display the info for the recyclerview.
 */
data class BloodPressureFitReadingData(
    var systolic: Int = 0,
    var diastolic: Int = 0,
    var date: String = "",
    var time: String = "",
    var isHigh: Boolean = false
)