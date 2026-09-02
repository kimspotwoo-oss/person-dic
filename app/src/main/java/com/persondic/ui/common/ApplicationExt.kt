package com.persondic.ui.common

import android.content.Context
import com.persondic.PersonDicApplication

fun Context.requirePersonDicApplication(): PersonDicApplication = applicationContext as PersonDicApplication
