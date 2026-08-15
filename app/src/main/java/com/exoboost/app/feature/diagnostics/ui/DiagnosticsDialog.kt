package com.exoboost.app.feature.diagnostics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.exoboost.app.core.designsystem.ElectricBlue
import com.exoboost.app.core.designsystem.EmeraldGreen
import com.exoboost.app.core.designsystem.GlassBorder
import com.exoboost.app.core.designsystem.Slate100
import com.exoboost.app.core.designsystem.Slate200
import com.exoboost.app.core.designsystem.Slate400
import com.exoboost.app.core.designsystem.Slate800
import com.exoboost.app.core.designsystem.Slate900
import com.exoboost.app.feature.diagnostics.model.DeviceDiagnosticReport
import com.exoboost.app.feature.diagnostics.model.DiagnosticItem

@Composable
fun DiagnosticsDialog(
    report: DeviceDiagnosticReport,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "System Diagnostics",
                            color = Slate100,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${report.manufacturer} ${report.model} • ${report.androidVersion} (API ${report.apiLevel})",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                report.items.forEach { item ->
                    DiagnosticItemCard(item = item)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Honest capability disclosure: ExoBoost strictly utilizes public Android APIs without root. Any limitations (such as DRM blackout on protected video or vendor HAL session 0 restrictions) are enforced at the hardware/OS level.",
                    color = Slate400,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done")
            }
        },
        containerColor = Slate900,
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun DiagnosticItemCard(item: DiagnosticItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, GlassBorder, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    color = Slate100,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (item.isAvailable) EmeraldGreen.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.statusText,
                        color = if (item.isAvailable) EmeraldGreen else Color(0xFFF59E0B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = item.detailText,
                color = Slate400,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
