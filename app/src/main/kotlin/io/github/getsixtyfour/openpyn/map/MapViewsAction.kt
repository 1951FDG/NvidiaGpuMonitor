package io.github.getsixtyfour.openpyn.map

import android.view.View
import com.naver.android.svc.core.views.ViewsAction

interface MapViewsAction : ViewsAction {

    fun showCountryFilterDialog()
    fun toggleCommand(v: View)
    fun toggleFavoriteMarker()
    fun toggleJuiceSSH()
    fun toggleSettings()
    fun updateMasterMarkerWithDelay(timeMillis: Long = 0)
}
