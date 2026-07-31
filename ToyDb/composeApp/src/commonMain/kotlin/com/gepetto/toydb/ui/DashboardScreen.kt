package com.gepetto.toydb.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import club.gepetto.composeutils.GcCard
import club.gepetto.composeutils.GcSpacing
import club.gepetto.composeutils.sysBackgroundColor
import club.gepetto.composeutils.sysForegroundColor
import club.gepetto.composeutils.sysTextColor
import com.gepetto.toydb.database.ToyRepository
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import toydb.composeapp.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: ToyRepository,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = remember { repository.getDashboardStats() }
    val categoriesSettings = remember { repository.getCategorySettings() }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(GcSpacing.Standard)
    ) {
        Box(
            modifier = Modifier
                .background(sysBackgroundColor(), shape = RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(Res.string.dashboard_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = sysTextColor()
            )
        }
        Spacer(modifier = Modifier.height(GcSpacing.Standard))

        // Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = GcSpacing.Standard),
            colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(GcSpacing.Standard)) {
                Text(
                    text = stringResource(Res.string.total_summary),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = sysTextColor()
                )
                Spacer(modifier = Modifier.height(GcSpacing.Small))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = stringResource(Res.string.total_toys), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${stats.totalToys}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
                    }
                    Column {
                        Text(text = stringResource(Res.string.total_spent), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "$${String.format("%.2f", stats.totalSpent)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
                    }
                    Column {
                        Text(text = stringResource(Res.string.estimated_value), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "$${String.format("%.2f", stats.totalValue)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = sysTextColor())
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(vertical = GcSpacing.Small)
                .background(
                    color = sysBackgroundColor(),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(Res.string.categories),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = sysTextColor()
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 250.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = GcSpacing.Standard)
        ) {
            items(stats.categories) { catStat ->
                val categoryName = categoriesSettings.find { it.category == catStat.category }?.label
                    ?: catStat.category.replaceFirstChar { it.uppercase() }

                Card(
                    modifier = Modifier
                        .padding(GcSpacing.Small)
                        .clickable { onNavigate(Destination.CategoryExplorer(catStat.category)) },
                    colors = CardDefaults.cardColors(containerColor = sysBackgroundColor()),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(GcSpacing.Standard)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = categoryName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = sysTextColor()
                        )
                        Spacer(modifier = Modifier.height(GcSpacing.Small))
                        Text(
                            text = stringResource(Res.string.items_count, catStat.count),
                            fontSize = 14.sp,
                            color = sysTextColor()
                        )
                        Spacer(modifier = Modifier.height(GcSpacing.XSmall))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stringResource(Res.string.spent_value_label, String.format("%.2f", catStat.totalSpent)), fontSize = 12.sp, color = sysTextColor())
                            Text(text = stringResource(Res.string.est_value_label, String.format("%.2f", catStat.totalValue)), fontSize = 12.sp, color = sysTextColor())
                        }
                    }
                }
            }
        }
    }
}
// Custom format fallback helper to prevent multi-platform string formatting compilation issues
private fun String.Companion.format(format: String, value: Double): String {
    val rounded = (value * 100 + 0.5).toLong() / 100.0
    val parts = rounded.toString().split(".")
    val integerPart = parts[0]
    val decimalPart = parts.getOrNull(1) ?: "00"
    val paddedDecimal = decimalPart.padEnd(2, '0').take(2)
    return "$integerPart.$paddedDecimal"
}
