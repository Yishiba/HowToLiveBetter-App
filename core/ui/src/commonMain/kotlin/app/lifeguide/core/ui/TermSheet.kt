package app.lifeguide.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lifeguide.core.corpus.GlossaryTerm

/** 点了正文里的术语：一句解释，加一个去整张术语表的入口 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermSheet(term: GlossaryTerm, onDismiss: () -> Unit, onOpenGlossary: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(term.term, style = MaterialTheme.typography.titleLarge)
            Text(term.meaning, style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = onOpenGlossary, modifier = Modifier.padding(top = 4.dp)) { Text("看全部术语") }
        }
    }
}
