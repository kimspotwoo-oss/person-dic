package com.persondic.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.persondic.ui.backup.BackupScreen
import com.persondic.ui.briefing.BriefingScreen
import com.persondic.ui.factedit.FactEditScreen
import com.persondic.ui.groupmap.GroupMapScreen
import com.persondic.ui.interactionlog.InteractionLogScreen
import com.persondic.ui.persondetail.PersonDetailScreen
import com.persondic.ui.personadd.PersonAddScreen
import com.persondic.ui.personlist.PersonListScreen
import com.persondic.ui.relationmap.RelationMapScreen
import com.persondic.ui.quickadd.QuickAddScreen
import java.util.UUID

@Composable
fun PersonDicNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.PERSON_LIST) {
        composable(Routes.PERSON_LIST) {
            PersonListScreen(
                onPersonClick = { personId -> navController.navigate(Routes.personDetail(personId)) },
                onGroupMapClick = { navController.navigate(Routes.GROUP_MAP) },
                onRelationMapClick = { navController.navigate(Routes.RELATION_MAP) },
                onQuickAddClick = { navController.navigate(Routes.QUICK_ADD) },
                onBackupClick = { navController.navigate(Routes.BACKUP) },
                onAddPersonClick = { navController.navigate(Routes.PERSON_ADD) },
            )
        }
        composable(Routes.PERSON_ADD) {
            PersonAddScreen(
                onDone = { navController.popBackStack() },
                onSaved = { personId ->
                    // Straight into the new person, and the add screen is not left on the back
                    // stack for the back button to land on.
                    navController.popBackStack()
                    navController.navigate(Routes.personDetail(personId))
                },
            )
        }
        composable(Routes.GROUP_MAP) {
            GroupMapScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.RELATION_MAP) {
            RelationMapScreen(
                onBack = { navController.popBackStack() },
                onPersonClick = { personId -> navController.navigate(Routes.personDetail(personId)) },
            )
        }
        composable(Routes.QUICK_ADD) {
            QuickAddScreen(onDone = { navController.popBackStack() })
        }
        composable(Routes.BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
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
                onOpenInteraction = { pid, id -> navController.navigate(Routes.interactionDetail(pid, id)) },
                onOpenPerson = { other -> navController.navigate(Routes.personDetail(other)) },
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
            arguments = listOf(
                navArgument("personId") { type = NavType.StringType },
                navArgument("interactionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val personId = UUID.fromString(backStackEntry.arguments?.getString("personId"))
            val interactionId = backStackEntry.arguments?.getString("interactionId")?.let(UUID::fromString)
            InteractionLogScreen(
                personId = personId,
                onDone = { navController.popBackStack() },
                interactionId = interactionId,
            )
        }
    }
}
