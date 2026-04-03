package com.oneira.propofree

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sin

val NeonGreen = Color(0xFF39FF14)
val NeonOrange = Color(0xFFFF9500)
val NeonRed = Color(0xFFFF2D55)
val NeonBlue = Color(0xFF00F0FF)
val TrueBlack = Color(0xFF000000)
val Glass = Color(0xFF1E1E1E).copy(alpha = 0.85f)

@Composable
fun LiveMultiMonitor(modifier: Modifier = Modifier) {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (isActive) { time.floatValue += 0.08f; delay(16) }
    }
    Canvas(modifier = modifier.height(140.dp)) {
        val w = size.width; val h = size.height
        drawWave(time.floatValue, w, h * 0.33f, NeonGreen, 4f, 0f)
        drawWave(time.floatValue * 0.7f, w, h * 0.33f, NeonBlue, 4f, h * 0.33f)
        drawWave(time.floatValue * 0.4f, w, h * 0.33f, NeonOrange, 4f, h * 0.66f)
    }
}

private fun DrawScope.drawWave(t: Float, w: Float, h: Float, color: Color, width: Float, offset: Float) {
    val path = Path(); var first = true
    for (x in 0..w.toInt() step 3) {
        val y = offset + h/2 + (sin((x + t*80)/25f) * h * 0.35f)
        if (first) { path.moveTo(x.toFloat(), y); first = false } else path.lineTo(x.toFloat(), y)
    }
    drawPath(path, color, style = Stroke(width))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropofreeDashboard(navController: androidx.navigation.NavHostController, patientVM: PatientProfileViewModel) {
    var showWipe by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Propofree", color = NeonGreen, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TrueBlack),
                actions = { IconButton(onClick = { showWipe = true }) { Icon(Icons.Default.CleaningServices, "", tint = NeonRed) } })
        },
        containerColor = TrueBlack
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LiveMultiMonitor(Modifier.fillMaxWidth().padding(16.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                item { DashboardCard("Emergency Dosing", NeonRed) { navController.navigate("emergency") } }
                item { DashboardCard("TIVA Wizard", NeonOrange) { navController.navigate("tiva") } }
                item { DashboardCard("Locum Intel", Color(0xFFFFB74D)) { navController.navigate("intel") } }
                item { DashboardCard("Guidelines", Color.Gray) { navController.navigate("library") } }
                item { DashboardCard("Airway QuickRef", NeonBlue) { navController.navigate("airway") } }
                item { DashboardCard("Handoff HUD", NeonGreen) { navController.navigate("handoff") } }
            }
        }
        if (showWipe) {
            AlertDialog(onDismissRequest = { showWipe = false }, title = { Text("Turnover Room") },
                text = { Text("Wipe all patient data?") },
                confirmButton = { Button(onClick = { patientVM.endCase(); showWipe = false }, colors = ButtonDefaults.buttonColors(NeonRed)) { Text("WIPE DATA") } },
                dismissButton = { TextButton(onClick = { showWipe = false }) { Text("Cancel") } })
        }
    }
}

@Composable
fun DashboardCard(title: String, accent: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(128.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Glass), shape = RoundedCornerShape(16.dp)) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomStart) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
fun PatientHeader(profile: PatientProfile, vm: PatientProfileViewModel) {
    OutlinedTextField(value = profile.weightKg.toString(), onValueChange = { vm.updateProfile(profile.copy(weightKg = it.toFloatOrNull() ?: 70f)) },
        label = { Text("WEIGHT (kg)", color = Color.Gray) },
        textStyle = MaterialTheme.typography.displaySmall.copy(color = Color.White),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth())
}

@Composable
fun EmergencyScreen(patientVM: PatientProfileViewModel, calcVM: ClinicalCalculatorViewModel) {
    val profile by patientVM.profile.collectAsState()
    val doses by calcVM.emergencyDoses.collectAsState()
    Column(Modifier.padding(16.dp).fillMaxSize()) {
        PatientHeader(profile, patientVM)
        Text("ZERO-MATH CRITICALS", style = MaterialTheme.typography.labelLarge, color = NeonRed)
        DoseCard("Epinephrine (ACLS)", "${doses.epinephrine} mg", "10 mcg/kg")
        DoseCard("Intralipid 20%", "${doses.intralipidBolus} mL", "1.5 mL/kg bolus")
        DoseCard("Dantrolene (MH)", "${doses.dantrolene} mg", "2.5 mg/kg rapid IV")
    }
}

@Composable
fun TivaScreen(patientVM: PatientProfileViewModel, calcVM: ClinicalCalculatorViewModel) {
    val profile by patientVM.profile.collectAsState()
    val tiva by calcVM.tivaSuggestions.collectAsState()
    Column(Modifier.padding(16.dp).fillMaxSize()) {
        PatientHeader(profile, patientVM)
        Text("TIVA SUGGESTIONS", style = MaterialTheme.typography.labelLarge, color = NeonOrange)
        DoseCard("Propofol Induction", "${tiva.propInduction} mg", "2 mg/kg")
        DoseCard("Propofol Maintenance", "${"%.1f".format(tiva.propMaintenance)} mcg/kg/min", "≈100 mcg/kg/min")
        DoseCard("Remifentanil", "${"%.2f".format(tiva.remiMaintenance)} mcg/kg/min", "0.1 mcg/kg/min")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocumIntelScreen(viewModel: LocumIntelViewModel, onBack: () -> Unit) {
    val feed by viewModel.feed.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Locum Intel") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "") } }) },
        containerColor = TrueBlack) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item { Text("⚠️ NO PHI – VIBES & WORKFLOW ONLY", color = NeonRed, modifier = Modifier.padding(bottom = 16.dp)) }
            items(feed) { tip ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Glass)) {
                    Row(Modifier.padding(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 16.dp)) {
                            IconButton(onClick = { viewModel.upvote(tip.id) }) { Icon(Icons.Default.KeyboardArrowUp, "", tint = NeonOrange) }
                            Text("${tip.upvotes}", color = Color.White)
                        }
                        Column {
                            Text(tip.hospital, color = Color.Gray)
                            Text(tip.surgeon, color = NeonOrange, fontWeight = FontWeight.Bold)
                            Text(tip.tip, color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
                            Row { tip.tags.forEach { Text(it, color = Color.LightGray, modifier = Modifier.padding(end = 8.dp)) } }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseScreen(viewModel: LibraryViewModel, onBack: () -> Unit) {
    val query by viewModel.searchQuery.collectAsState()
    val articles by viewModel.articles.collectAsState()
    Scaffold(topBar = { TopAppBar(title = { Text("Guidelines") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "") } }) },
        containerColor = TrueBlack) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(value = query, onValueChange = { viewModel.updateSearch(it) },
                placeholder = { Text("Search ASA, ACLS...") }, modifier = Modifier.fillMaxWidth().padding(16.dp))
            LazyColumn {
                items(articles) { article ->
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = Glass)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(article.title, fontWeight = FontWeight.Bold, color = NeonGreen)
                            Text(article.content, modifier = Modifier.padding(top = 8.dp), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AirwayScreen(patientVM: PatientProfileViewModel) {
    val profile by patientVM.profile.collectAsState()
    Column(Modifier.padding(16.dp).fillMaxSize()) {
        PatientHeader(profile, patientVM)
        Text("AIRWAY QUICKREF", style = MaterialTheme.typography.headlineSmall, color = NeonBlue)
    }
}

@Composable
fun HandoffScreen(patientVM: PatientProfileViewModel) {
    val profile by patientVM.profile.collectAsState()
    Column(Modifier.padding(16.dp).fillMaxSize()) {
        PatientHeader(profile, patientVM)
        Text("HANDOFF HUD (SBAR)", style = MaterialTheme.typography.headlineSmall, color = NeonGreen)
    }
}

@Composable
fun DoseCard(label: String, dose: String, formula: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Glass), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text(label, color = Color.White, fontWeight = FontWeight.Bold); Text(formula, color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
            Text(dose, style = MaterialTheme.typography.headlineMedium, color = NeonRed)
        }
    }
}
