package com.example.kspissue

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.generated.allExampleSets

@Composable
fun ComposeList() {
    Surface {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(allExampleSets.toList()) {
                Text(text = it.java.simpleName)
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    ComposeList()
}
