package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.theme.TelebirrBlue
import com.example.ui.theme.TelebirrContainer
import com.example.ui.theme.UnlockedGreenBg
import com.example.ui.theme.UnlockedGreenText
import com.example.ui.viewmodel.UnlockModalState

@Composable
fun TelebirrPaymentDialog(
  state: UnlockModalState,
  onConfirmPayment: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        val unlocked = state.unlockedContact
        if (unlocked == null) {
          Box(
            modifier =
              Modifier.size(60.dp)
                .clip(CircleShape)
                .background(TelebirrContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.Lock,
              contentDescription = null,
              tint = TelebirrBlue,
              modifier = Modifier.size(30.dp),
            )
          }
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = stringResource(R.string.telebirr_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(R.string.telebirr_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(16.dp))

          // Property & Fee Summary Card
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = state.house.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
              )
              Text(
                text = state.house.location,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "የደላላ ኮሚሽን (Broker Fee)",
                  style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                  text = "0 ብር",
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "ቀጥታ ስልክ መክፈቻ",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = "50 ብር",
                  style = MaterialTheme.typography.titleMedium,
                  color = TelebirrBlue,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }

          if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = state.errorMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
              textAlign = TextAlign.Center,
            )
          }

          Spacer(modifier = Modifier.height(20.dp))
          Button(
            onClick = onConfirmPayment,
            enabled = !state.isProcessing,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = TelebirrBlue,
                contentColor = Color.White,
              ),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .height(52.dp)
                .testTag("confirm_telebirr_payment_button"),
          ) {
            if (state.isProcessing) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text("በመክፈት ላይ...")
            } else {
              Icon(
                imageVector = Icons.Filled.PhoneAndroid,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(stringResource(R.string.telebirr_pay_cta))
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          TextButton(
            onClick = onDismiss,
            enabled = !state.isProcessing,
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("cancel_payment_button"),
          ) {
            Text(stringResource(R.string.cancel))
          }
        } else {
          // Unlocked Contact Success View
          Box(
            modifier =
              Modifier.size(64.dp)
                .clip(CircleShape)
                .background(UnlockedGreenBg),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = UnlockedGreenText,
              modifier = Modifier.size(34.dp),
            )
          }
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "ስልክ ቁጥሩ ተከፍቷል!",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "አሁን ከቤቱ ባለቤት ጋር ያለ ደላላ በቀጥታ መነጋገር ይችላሉ።",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.height(16.dp))

          Surface(
            color = UnlockedGreenBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              if (unlocked.ownerName.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = UnlockedGreenText,
                    modifier = Modifier.size(16.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = unlocked.ownerName,
                    style = MaterialTheme.typography.labelLarge,
                    color = UnlockedGreenText,
                  )
                }
                Spacer(modifier = Modifier.height(6.dp))
              }
              Text(
                text = unlocked.phone,
                style = MaterialTheme.typography.headlineSmall,
                color = UnlockedGreenText,
                fontWeight = FontWeight.Bold,
              )
              if (!state.transactionRef.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Telebirr Ref: ${state.transactionRef}",
                  style = MaterialTheme.typography.labelMedium,
                  color = UnlockedGreenText.copy(alpha = 0.8f),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))
          Button(
            onClick = { launchPhoneDialer(context, unlocked.phone) },
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .height(52.dp)
                .testTag("call_owner_now_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Call,
              contentDescription = null,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("አሁን ይደውሉ (${unlocked.phone})")
          }
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
          ) {
            Text("ዝጋ (Close)")
          }
        }
      }
    }
  }
}
