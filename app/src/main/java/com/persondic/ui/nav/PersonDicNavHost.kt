package com.persondic.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.persondic.ui.briefing.BriefingScreen
import com.persondic.ui.factedit.FactEditScreen
import com.persondic.ui.interactionlog.InteractionLogScreen
import com.persondic.ui.persondetail.PersonDetailScreen
import com.persondic.ui.personlist.PersonListScreen
import java.util.UUID

@Composable
fun PersonDicNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.PERSON_LIST) {
        composable(Routes.PERSON_LIST) {
            PersonListScreen(
                onPersonClick = { personId -> navController.navigate(Routes.personDetail(personId)) },
            )
        }
        composable(
            route = Routes.PERSON_DETAIL,
            arguments = listOf(navArgument("personId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val personId = UUID.fromString(backStackEntry.arguments?.getString("personId"))
            PersonDetailScreen(
                personId = personId,
                onBack = { navController.popBackStack() },
                onBriefingClick = { navController.navigate(Routes.briefing(it)) },
                onAddFact = { navController.navigate(Routes.addFact(it)) },
                onEditFact = { pid, factId -> navController.navigate(Routes.editFact(pid, factId)) },
            )
        }
        composable(
            route = Routes.FACT_EDIT,
            arguments = listOf(
                navArgument("personId") { type = NavType.StringType },
                navArgument("factId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val personId = UUID.fromString(backStackEntry.arguments?.getString("personId"))
            val factId = backStackEntry.arguments?.getString("factId")?.let(UUID::fromString)
            FactEditScreen(
                personId = personId,
                factId = factId,
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.BRIEFING,
            arguments = listOf(navArgument("personId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val personId = UUID.fromString(backStackEntry.arguments?.getString("personId"))
            BriefingScreen(
                personId = personId,
                onBack = { navController.popBackStack() },
                onRecordInteraction = { navController.navigate(Routes.interactionLog(it)) },
                onEditFact = { pid, factId -> navController.navigate(Routes.editFact(pid, factId)) },
            )
        }
        composable(
            route = Routes.INTERACTION_LOG,
            arguments = listOf(navArgument("personId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val personId = UUID.fromString(backStackEntry.arguments?.getString("personId"))
            InteractionLogScreen(
                personId = personId,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
