package dev.jovanni0.itec19.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jovanni0.itec19.data.Team


@Composable
fun TeamPeakScreen(
    onTeamSelected: (Team) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Choose Your Team",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Draw on posters to claim them for your team.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Team.entries.forEach { team ->
                Button(
                    onClick = { onTeamSelected(team) },
                    modifier = Modifier.width(220.dp).height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = team.color.copy(alpha = 0.2f),
                        contentColor = team.color
                    ),
                    border = BorderStroke(2.dp, team.color),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Team ${team.displayName}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}