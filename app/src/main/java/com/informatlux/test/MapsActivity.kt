package com.informatlux.test

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.informatlux.test.databinding.ActivityMapsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.math.roundToInt


class MapsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mapView: MapView
    private lateinit var bottomSheet: BottomSheetBehavior<*>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesAdapter: PlacesAutoCompleteAdapter

    private val photon by lazy {
        Retrofit.Builder()
            .baseUrl("https://photon.komoot.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PhotonService::class.java)
    }

    private val locPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) locateAndPlotAll()
        else Toast.makeText(this, "Location permission is required to use this feature.", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(applicationContext, getSharedPreferences("osm_prefs", MODE_PRIVATE))
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mapView = binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }
        bottomSheet = BottomSheetBehavior.from(binding.bottomSheet)

        setupInsets()
        setupSearch()
        setupChips()
        setupDirectionButton()
        setupBottomNavigation() // FIX: Call the new function

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locateAndPlotAll()
        } else {
            locPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomSheet) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val pad = (16f * resources.displayMetrics.density).roundToInt()
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, pad + bars.bottom)
            insets
        }
    }

    @SuppressLint("MissingPermission")
    private fun locateAndPlotAll() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                loc?.let {
                    val userPt = GeoPoint(it.latitude, it.longitude)
                    mapView.controller.setZoom(14.0)
                    mapView.controller.setCenter(userPt)
                    addMarker(userPt, "You Are Here", R.drawable.user_current_location, 30)
                    fetchAllCategoriesAround(userPt)
                }
            }
    }

    private fun setupSearch() {
        placesAdapter = PlacesAutoCompleteAdapter(this)
        (binding.searchInputLayout.editText as? AutoCompleteTextView)?.apply {
            setAdapter(placesAdapter)
            setOnItemClickListener { parent, _, position, _ ->
                val selectedPlace = parent.getItemAtPosition(position) as? PlaceSuggestion
                if (selectedPlace != null) {
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(this.windowToken, 0)
                    this.clearFocus()

                    val newCenter = GeoPoint(selectedPlace.lat, selectedPlace.lon)
                    mapView.controller.animateTo(newCenter)
                    mapView.controller.setZoom(16.0)

                    fetchAllCategoriesAround(newCenter)
                }
            }
        }
    }

    private fun fetchAllCategoriesAround(center: GeoPoint) {
        mapView.overlays.removeAll { it is Marker && it.title != "You Are Here" }
        mapView.invalidate()
        fetchPhoton("recycling", center.latitude, center.longitude)
        fetchPhoton("reuse centre", center.latitude, center.longitude)
        fetchPhoton("electronic waste", center.latitude, center.longitude)
    }

    private fun setupChips() {
        binding.chipEcoBins.setOnClickListener {
            currentCenter()?.let { fetchPhoton("recycling", it.latitude, it.longitude) }
        }
        binding.chipRecycleCenters.setOnClickListener {
            currentCenter()?.let { fetchPhoton("reuse centre", it.latitude, it.longitude) }
        }
        binding.chipEWaste.setOnClickListener {
            currentCenter()?.let { fetchPhoton("electronic waste", it.latitude, it.longitude) }
        }
    }

    private fun currentCenter(): GeoPoint? = mapView.mapCenter as? GeoPoint

    private fun fetchPhoton(q: String, lat: Double, lon: Double) { // FIX: Removed radius parameter
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // FIX: Call the corrected 'searchNearby' function without the radius.
                val resp = photon.searchNearby(q, lat, lon, 50)
                if (!resp.isSuccessful) throw Exception("Photon API error: ${resp.code()}")

                val feats = resp.body()!!.features
                withContext(Dispatchers.Main) {
                    feats.forEach { feat ->
                        val coords = feat.geometry.coordinates
                        val name = feat.properties["name"] as? String ?: "Facility"
                        val pt = GeoPoint(coords[1], coords[0]) // [1] is latitude, [0] is longitude
                        Marker(mapView).apply {
                            position = pt
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = name
                            icon = getResized(drawableId = R.drawable.map_marker_green_icon, w = 80, h = 80)
                            setOnMarkerClickListener { m, _ ->
                                binding.locationName.text = m.title
                                bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
                                true
                            }
                        }.also { mapView.overlays.add(it) }
                    }
                    mapView.invalidate()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MapsActivity, "Load failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun addMarker(pt: GeoPoint, title: String, drawableId: Int, size: Int) {
        Marker(mapView).apply {
            position = pt
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
            icon = getResized(drawableId = drawableId, w = size, h = size)
        }.also { mapView.overlays.add(it) }
    }

    private fun setupDirectionButton() {
        binding.btnDirection.setOnClickListener {
            Toast.makeText(this, "Select a pin first to get directions", Toast.LENGTH_SHORT).show()
        }
    }

    // FIX: This function now handles navigation for the bottom bar.
    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_maps // Ensure Maps is selected
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            // Prevent re-selecting the current item to avoid unnecessary actions
            if (item.itemId == binding.bottomNavigation.selectedItemId) {
                return@setOnItemSelectedListener false
            }

            when (item.itemId) {
                R.id.nav_home -> {
                    // Navigate back to MainActivity and close this one
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_maps -> {
                    // We are already on this screen
                    true
                }
                R.id.nav_scan -> {
                    // Navigate back to MainActivity and close this one
                    val intent = Intent(this, AIActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_article -> {
                    // Navigate back to MainActivity and close this one
                    val intent = Intent(this, ArticleActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    true
                }

                else -> {
                    Toast.makeText(this, "${item.title} feature coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
    }

    private fun getResized(drawableId: Int, w: Int, h: Int): Drawable {
        val dr = ContextCompat.getDrawable(this, drawableId)!!
        val bmp = when (dr) {
            is BitmapDrawable -> Bitmap.createScaledBitmap(dr.bitmap, w, h, true)
            else -> {
                val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                Canvas(b).apply { dr.setBounds(0, 0, w, h); dr.draw(this) }
                b
            }
        }
        return BitmapDrawable(resources, bmp)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}