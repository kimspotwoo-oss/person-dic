package com.persondic.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.persondic.ui.personlist.PersonListScreen

@Composable
fun PersonDicNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.PERSON_LIST) {
        composable(Routes.PERSON_LIST) {
            PersonListScreen(onPersonClick = {})
        }
    }
}
