package com.persondic.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.persondic.ui.factedit.FactEditScreen
import com.persondic.ui.personlist.PersonListScreen
import java.util.UUID

@Composable
fun PersonDicNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.PERSON_LIST) {
        composable(Routes.PERSON_LIST) {
            PersonListScreen(
                onPersonClick = { personId -> navController.navigate(Routes.factEdit(personId)) },
            )
        }
        composable(
            route = Routes.FACT_EDIT,
            arguments = listOf(navArgument("personId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val personId = UUID.fromString(backStackEntry.arguments?.getString("personId"))
            FactEditScreen(
                personId = personId,
                onDone = { navController.popBackStack() },
            )
        }
    }
}
