package com.oneira.propofree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlin.math.pow

data class PatientProfile(
    val weightKg: Float = 70f,
    val heightCm: Float = 170f,
    val ageYears: Int = 40,
    val isMale: Boolean = true
) {
    val ibw: Float = if (isMale) 50f + 0.91f * (heightCm - 152.4f) else 45.5f + 0.91f * (heightCm - 152.4f)
    val adjBw: Float = ibw + 0.4f * (weightKg - ibw)
    val bsa: Float = 0.007184f * (weightKg.pow(0.425f)) * (heightCm.pow(0.725f))
}

class PatientProfileViewModel : ViewModel() {
    private val _profile = MutableStateFlow(PatientProfile())
    val profile = _profile.asStateFlow()

    fun updateProfile(newProfile: PatientProfile) { _profile.value = newProfile }
    fun endCase() { _profile.value = PatientProfile() }
}

data class EmergencyDoses(val epinephrine: Float = 0f, val intralipidBolus: Float = 0f, val dantrolene: Float = 0f)
data class TivaSuggestions(val propInduction: Float = 0f, val propMaintenance: Float = 0f, val remiMaintenance: Float = 0f)

class ClinicalCalculatorViewModel(private val profileVM: PatientProfileViewModel) : ViewModel() {
    val emergencyDoses = profileVM.profile.map { p ->
        EmergencyDoses(
            epinephrine = p.weightKg * 0.01f,
            intralipidBolus = p.weightKg * 1.5f,
            dantrolene = p.weightKg * 2.5f
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), EmergencyDoses())

    val tivaSuggestions = profileVM.profile.map { p ->
        TivaSuggestions(
            propInduction = p.weightKg * 2.0f,
            propMaintenance = p.weightKg * 100f / 60f,
            remiMaintenance = p.weightKg * 0.1f
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), TivaSuggestions())
}

data class ClinicalArticle(val id: String, val title: String, val category: String, val content: String)

val authoritativeLibrary = listOf(
    ClinicalArticle("last", "Local Anesthetic Systemic Toxicity (LAST)", "ASRA", "• 100% O₂, avoid hyperventilation\n• Intralipid 20% 1.5 mL/kg bolus\n• Repeat q3-5 min up to 3 mL/kg total"),
    ClinicalArticle("mh", "Malignant Hyperthermia", "MHAUS", "• Call MH cart\n• Stop volatiles & sux\n• Dantrolene 2.5 mg/kg rapid IV\n• Cool aggressively")
)

class LibraryViewModel : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _articles = MutableStateFlow(authoritativeLibrary)
    val articles = _articles.asStateFlow()

    fun updateSearch(query: String) {
        _searchQuery.value = query
        _articles.value = if (query.isBlank()) authoritativeLibrary else authoritativeLibrary.filter {
            it.title.contains(query, true) || it.content.contains(query, true)
        }
    }
}

data class SurgeonTip(val id: String, val hospital: String, val surgeon: String, val tip: String, val tags: List<String>, var upvotes: Int)

val mockIntelFeed = listOf(
    SurgeonTip("1", "UW Health", "Ortho - Dr. S", "Room at meat-locker temps. 80s rock playlist.", listOf("#RunsHot", "#GoodVibes"), 142),
    SurgeonTip("2", "University Hospital", "ENT - Dr. R", "Will rush emergence. Do NOT extubate deep.", listOf("#AirwayRisk", "#Rushed"), 89)
)

class LocumIntelViewModel : ViewModel() {
    private val _feed = MutableStateFlow(mockIntelFeed.sortedByDescending { it.upvotes })
    val feed = _feed.asStateFlow()

    fun upvote(id: String) {
        _feed.value = _feed.value.map { if (it.id == id) it.copy(upvotes = it.upvotes + 1) else it }
            .sortedByDescending { it.upvotes }
    }
}
