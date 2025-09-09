package com.zaneschepke.wireguardautotunnel.ui.screens.support.donate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zaneschepke.wireguardautotunnel.BuildConfig
import com.zaneschepke.wireguardautotunnel.ui.common.SectionDivider
import com.zaneschepke.wireguardautotunnel.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.ui.screens.support.donate.components.DonationHeroSection
import com.zaneschepke.wireguardautotunnel.ui.screens.support.donate.components.DonationOptions
import com.zaneschepke.wireguardautotunnel.ui.screens.support.donate.components.GoogleDonationMessage
import com.zaneschepke.wireguardautotunnel.util.Constants

@Composable
fun DonateScreen(navController: NavController) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
                .padding(horizontal = 12.dp),
    ) {
        DonationHeroSection()
        SectionDivider()
        if (BuildConfig.FLAVOR != Constants.GOOGLE_PLAY_FLAVOR) {
            DonationOptions { navController.navigate(Route.Addresses) }
        } else {
            GoogleDonationMessage()
        }
    }
}
