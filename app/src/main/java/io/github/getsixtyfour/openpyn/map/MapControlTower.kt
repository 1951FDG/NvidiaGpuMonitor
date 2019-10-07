package io.github.getsixtyfour.openpyn.map

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageView
import com.abdeveloper.library.MultiSelectable
import com.androidmapsextensions.lazy.LazyMarker
import com.androidmapsextensions.lazy.LazyMarker.OnLevelChangeCallback
import com.androidmapsextensions.lazy.LazyMarker.OnMarkerCreateListener
import com.antoniocarlon.map.CameraUpdateAnimator
import com.antoniocarlon.map.CameraUpdateAnimator.Animation
import com.antoniocarlon.map.CameraUpdateAnimator.AnimatorListener
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.naver.android.svc.annotation.ControlTower
import com.naver.android.svc.annotation.RequireScreen
import com.naver.android.svc.annotation.RequireViews
import de.jupf.staticlog.Log
import de.westnordost.countryboundaries.CountryBoundaries
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.utilities.LazyMarkerStorage
import io.github.getsixtyfour.openpyn.utilities.PrintArray
import io.github.getsixtyfour.openpyn.utilities.SubmitCallbackListener
import io.github.getsixtyfour.openpyn.utilities.countryList
import io.github.getsixtyfour.openpyn.utilities.createGeoJson
import io.github.getsixtyfour.openpyn.utilities.jsonArray
import io.github.getsixtyfour.openpyn.utilities.logException
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnCommandExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.info
import org.json.JSONArray
import org.json.JSONObject
import java.util.HashSet
import kotlinx.android.synthetic.main.fragment_map.view.map
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * @author 1951FDG
 */
@ControlTower
@RequireViews(MapViews::class)
@RequireScreen(MapFragment::class)
class MapControlTower : SVC_MapControlTower(), AnkoLogger, OnMapReadyCallback, OnMapLoadedCallback,
    OnCameraIdleListener, OnMapClickListener, OnMarkerClickListener, OnInfoWindowClickListener,
    SubmitCallbackListener, MapViewsAction, AnimatorListener, OnCommandExecuteListener,
    CoroutineScope by MainScope() {
    private val map by lazy { views.rootView.map }

    private val mHandler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
        GlobalScope.launch(Main) {
            screen.toolBar?.hideProgress(true)
        }
        exception.printStackTrace()
        Log.error("Caught $exception")
        logException(exception)
    }

    private val applicationContext: Context
        get() = screen.requireContext().applicationContext

    private lateinit var markers: HashMap<LatLng, LazyMarker>
    private lateinit var flags: HashSet<CharSequence>
    private lateinit var mAnimations: ArrayList<Animation>
    private var mMap: GoogleMap? = null
    private var mCameraUpdateAnimator: CameraUpdateAnimator? = null

    private val mMarkerStorage by lazy { LazyMarkerStorage(FAVORITE_KEY) }
    //set by async
    private lateinit var mCountries: List<MultiSelectable>
    private var mCountryBoundaries: CountryBoundaries? = null
    private lateinit var mFavorites: ArrayList<LazyMarker>
    private lateinit var mJsonArray: JSONArray
    private var mTileProvider: MapBoxOfflineTileProvider? = null

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    private val mSendChannel = actor<Context>(coroutineContext, Channel.RENDEZVOUS) {
        channel.map(IO) { createGeoJson(it) }.consumeEach { animateCamera(it) }
    }

    override fun onCreated() {
        loadData()
    }

    override fun onStarted() {
        mMap?.let { map.onStart() }
    }

    override fun onResumed() {
        mMap?.let { map.onResume() }
    }

    override fun onPause() {
        mMap?.let { map.onPause() }
    }

    override fun onStop() {
        mMap?.let { map.onStop() }
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    override fun onDestroy() {
        mMap?.let { map.onDestroy() }
        mCameraUpdateAnimator?.onDestroy()
        mCameraUpdateAnimator?.animatorListener = null
        mSendChannel.close()
        mTileProvider?.close()
        cancel()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap?.let {
            // todo get rid of asReference..., check all this invocations
            onMarkerCreateListener.setMap(it)
            it.addTileOverlay(TileOverlayOptions().tileProvider(mTileProvider).fadeIn(false))
            it.setMaxZoomPreference(mTileProvider!!.maximumZoom)
            it.setMinZoomPreference(mTileProvider!!.minimumZoom)
            it.setOnInfoWindowClickListener(this)
            it.setOnMapClickListener(this)
            it.setOnMarkerClickListener(this)
            it.setOnMapLoadedCallback(this)

            // todo
            //val params = fab1.layoutParams as ConstraintLayout.LayoutParams
            //it.setPadding(0, 0, 0, params.height + params.bottomMargin)

            it.uiSettings?.isScrollGesturesEnabled = true
            it.uiSettings?.isZoomGesturesEnabled = true

            mCameraUpdateAnimator = CameraUpdateAnimator(it, mAnimations, this)
            mCameraUpdateAnimator?.animatorListener = this

            // Load map
            views.showMap()
            map.onResume()
        }
    }

    override fun onMapLoaded() {
        mCameraUpdateAnimator?.execute()
    }

    override fun onCameraIdle() {
        val bounds = mMap!!.projection.visibleRegion.latLngBounds

        markers.forEach { (key, value) ->
            if (bounds.contains(key) && flags.contains(value.tag)) {
                if (!value.isVisible) value.isVisible = true
            } else {
                if (value.isVisible) value.isVisible = false

                if (value.zIndex == 1.0f) {
                    value.setLevel(value.level, onLevelChangeCallback)

                    views.hideFavoriteFab()
                }
            }
        }
    }

    override fun onMapClick(point: LatLng) {
        markers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            it.setLevel(it.level, onLevelChangeCallback)

            views.hideFavoriteFab()
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if (marker.zIndex == 1.0f) {
            views.callConnectFabOnClick()
        } else {
            markers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
                it.setLevel(it.level, onLevelChangeCallback)
            }

            markers[marker.position]?.let {
                it.zIndex = 1.0f
                it.setIcon(mDescriptor10)

                views.toggleFavoriteFab(it.level == 1)
            }
        }

        return false
    }

    override fun onInfoWindowClick(marker: Marker) {
        if (marker.zIndex == 1.0f) {
            views.callConnectFabOnClick()
        }
    }

    override fun onCancel() {
    }

    override fun onSelected(selectedIds: ArrayList<Int>, selectedNames: ArrayList<String>, dataString: String) {
        mCountries.let {
            flags = getCurrentFlags(it, selectedIds)
        }

        onCameraIdle()
    }

    override fun showCountryFilterDialog() {
        mCameraUpdateAnimator?.let {
            if (!it.isAnimating) {
                PrintArray.show("pref_country_values", screen.requireActivity(), this)
            }
        }
    }

    override fun toggleCommand(v: View?) {
        (screen.requireActivity() as? OnClickListener)?.onClick(v)
    }

    override fun toggleFavoriteMarker() {
        markers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            when (it.level) {
                0 -> {
                    it.setLevel(1, null)
                    mMarkerStorage.addFavorite(applicationContext, it)
                }
                1 -> {
                    it.setLevel(0, null)
                    mMarkerStorage.removeFavorite(applicationContext, it)
                }
            }
            views.toggleFavoriteFab((it.level == 1))
        }
    }

    @ObsoleteCoroutinesApi
    @ExperimentalCoroutinesApi
    override fun updateMasterMarker(show: Boolean) {
        mSendChannel.offer(applicationContext)
    }

    fun updateMasterMarkerWithDelay(show: Boolean, delayMillis: Long) {
        launch {
            // ui thread
            delay(delayMillis)
            updateMasterMarker(show)
        }
    }

    override fun onAnimationStart() {
        views.setClickableFabs(false)
    }

    override fun onAnimationEnd() {
        views.setClickableFabs(true)
    }

    override fun onAnimationFinish(animation: Animation) {
        if (animation.isClosest) {

            views.fakeLayoutAllFabs()
            markers[animation.target]?.let {
                if (flags.contains(it.tag)) {
                    it.zIndex = 1.0f
                    it.setIcon(mDescriptor10)

                    if (!it.isVisible) it.isVisible = true
                    if (!it.isInfoWindowShown) it.showInfoWindow()

                    views.toggleFavoriteFab(it.level == 1)
                }
            }

            views.showAllFabs()
        } else {
            (animation.tag as? JSONObject)?.let {
                //todo test threats
                showThreats(activity!!, it)
                views.showMiniBar(createUserMessage(context!!, it).build())
            }
        }
    }

    override fun onAnimationCancel(animation: Animation) {
        markers[animation.target]?.let {
            info("Animation to $it canceled")
        }
    }

    fun onSessionFinished() {
        info("onSessionFinished")
        views.setClickableConnectFab(true)
        views.toggleConnectFab(false)

        views.showListAndLocationFab()
        markers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            if (!it.isInfoWindowShown) it.showInfoWindow()
            views.showFavoriteFab()
        }

        mMap?.let {
            it.setOnInfoWindowClickListener(this)
            it.setOnMapClickListener(this)
            it.setOnMarkerClickListener(this)
            it.uiSettings?.isScrollGesturesEnabled = true
            it.uiSettings?.isZoomGesturesEnabled = true
        }
    }

    fun onSessionStarted() {
        info("onSessionStarted")
        views.setClickableConnectFab(true)
        views.toggleConnectFab(true)

        views.hideListAndLocationFab()
        markers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            if (it.isInfoWindowShown) it.hideInfoWindow()
            views.hideFavoriteFab()
        }
        mMap?.let {
            it.setOnInfoWindowClickListener(null)
            it.setOnMapClickListener(null)
            it.setOnMarkerClickListener { true }
            it.uiSettings?.isScrollGesturesEnabled = false
            it.uiSettings?.isZoomGesturesEnabled = false
        }
    }

    fun onSessionCancelled() {
        info("onSessionCancelled")
        views.setClickableConnectFab(true)
        views.toggleConnectFab(false)
    }

    override fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String> {
        var pair: Pair<Coordinate?, String> = Pair(null, "")

        markers.entries.firstOrNull { it.value.zIndex == 1.0f }?.let {
            val latLng = it.key
            val tag = it.value.tag

            pair = when {
                markers.count { entry -> entry.value.tag == tag } == 1 -> Pair(null, tag.toString())
                else -> Pair(Coordinate(latLng.latitude, latLng.longitude), tag.toString())
            }
        }

        return pair
    }

    override fun onConnect() {
        TODO("not implemented")
    }

    override fun onDisconnect() {
        TODO("not implemented")
    }

    private fun loadData() = launch(mHandler) {
        // ui thread
        screen.toolBar?.showProgress(true)
        val ref = applicationContext.asReference()
        val mapView = views.findViewById<MapView>(R.id.map)

        // background thread
        val countries = async(IO) { countryList(ref(), R.raw.emojis) }
        val countryBoundaries = async(IO) { getCountryBoundaries(ref()) }
        val favorites = async(IO) { LazyMarkerStorage(FAVORITE_KEY).loadFavorites(ref()) }
        val jsonArray = async(IO) { jsonArray(ref(), R.raw.nordvpn, ".json") }
        val tileProvider = async(IO) { fileBackedTileProvider() }
        val jsonObj = async(IO) { createGeoJson(ref()) }
        val map = async(Main) { mapView?.onCreate(null) }

        // ui thread
        mCountries = countries.await()
        mTileProvider = tileProvider.await()
        mCountryBoundaries = countryBoundaries.await()
        mFavorites = favorites.await()
        mJsonArray = jsonArray.await()
        map.await()

        // Show data in UI
        screen.toolBar?.hideProgress(true)
        showData(jsonObj.await())
    }

    private suspend fun showData(jsonObj: JSONObject?) {
        val ref = applicationContext.asReference()
        // todo use current context or not?
        val (hashSet, hashMap) = withContext(Default) {
            createMarkers(ref(), mJsonArray, mCountries, onMarkerCreateListener, mFavorites, onLevelChangeCallback)
        }
        flags = withContext(Default) { showPrintArray(ref(), mCountries, hashSet) }
        markers = hashMap
        mAnimations = createCameraUpdates()
        val latLng = getCurrentPosition(applicationContext, mCountryBoundaries, screen.lastLocation, flags, jsonObj, mJsonArray)
        val animation = Animation(CameraUpdateFactory.newLatLng(latLng)).apply {
            callback = true
            isAnimate = true
            isClosest = true
            tag = jsonObj
            target = latLng
        }
        mAnimations.add(animation)
        val mapView = views.findViewById<MapView>(R.id.map)
        val watermark = mapView?.findViewWithTag<ImageView>("GoogleWatermark")

        if (watermark != null) {
            watermark.visibility = View.INVISIBLE
            /*
            val params = watermark.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0)
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
            params.addRule(RelativeLayout.ALIGN_PARENT_START, 0)
            params.addRule(RelativeLayout.ALIGN_PARENT_END, 0)
            */
        }
        mapView?.getMapAsync(this)
    }

    private fun animateCamera(jsonObj: JSONObject?, closest: Boolean = false, execute: Boolean = true) {
        // check if not already animating
        mCameraUpdateAnimator?.let {
            if (!it.isAnimating) {
                val latLng = getCurrentPosition(applicationContext, mCountryBoundaries, screen.lastLocation, flags, jsonObj, mJsonArray)
                val animation = Animation(CameraUpdateFactory.newLatLng(latLng)).apply {
                    callback = true
                    isAnimate = true
                    isClosest = closest
                    tag = jsonObj
                    target = latLng
                }
                it.add(animation)
                // Execute the animation
                if (execute) it.execute()
            }
        }
    }

    companion object {
        private const val FAVORITE_KEY = "pref_favorites"
        internal val mDescriptor0: BitmapDescriptor by lazy { BitmapDescriptorFactory.fromResource(R.drawable.map1) }
        internal val mDescriptor1: BitmapDescriptor by lazy { BitmapDescriptorFactory.fromResource(R.drawable.map2) }
        internal val mDescriptor10: BitmapDescriptor by lazy { BitmapDescriptorFactory.fromResource(R.drawable.map0) }
        val onLevelChangeCallback: OnLevelChangeCallback = OnLevelChangeCallback { marker, level ->
            when (level) {
                0 -> {
                    marker.zIndex = 0f
                    marker.setIcon(mDescriptor0)
                }
                1 -> {
                    marker.zIndex = level / 10.toFloat()
                    marker.setIcon(mDescriptor1)
                }
            }
        }
        val onMarkerCreateListener: OnMarkerCreateListener1 = OnMarkerCreateListener1()

        class OnMarkerCreateListener1 : OnMarkerCreateListener {
            private lateinit var map: GoogleMap

            fun setMap(googleMap: GoogleMap) {

                this.map = googleMap
            }

            override fun onMarkerCreate(options: MarkerOptions, tag: Any?): Marker {
                val marker = map.addMarker(options)
                if (tag != null) {
                    marker.tag = tag
                }
                return marker
            }
        }
    }
}
