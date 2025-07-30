package com.informatlux.test

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
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
import org.osmdroid.views.overlay.Polyline
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt
import kotlin.random.Random

class MapsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var mapView: MapView
    private lateinit var bottomSheet: BottomSheetBehavior<*>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesAdapter: PlacesAutoCompleteAdapter

    private var currentlySelectedMarker: Marker? = null

    private val photon by lazy {
        Retrofit.Builder()
            .baseUrl("https://photon.komoot.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PhotonService::class.java)
    }
    private val routingService by lazy {
        Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RoutingService::class.java)
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

        bottomSheet = BottomSheetBehavior.from(binding.bottomSheet).apply {
            peekHeight = 0
            state = BottomSheetBehavior.STATE_COLLAPSED
        }

        setupInsets()
        setupSearch()
        setupChips()
        setupDirectionButton()
        setupBottomNavigation()

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
                    addMarker(userPt, "You Are Here", R.drawable.user_current_location, 30f)
                    fetchAllCategoriesAround(userPt)
                }
            }
    }

    private fun setupSearch() {
        placesAdapter = PlacesAutoCompleteAdapter(this)
        val searchInput = (binding.searchInputLayout.editText as? AutoCompleteTextView)
        searchInput?.setAdapter(placesAdapter)

        searchInput?.setOnItemClickListener { parent, view, position, id ->
            val selectedPlace = parent.getItemAtPosition(position) as? PlaceSuggestion
            if (selectedPlace != null) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.applicationWindowToken, 0)
                searchInput.clearFocus()

                val newCenter = GeoPoint(selectedPlace.lat, selectedPlace.lon)
                mapView.controller.animateTo(newCenter)
                mapView.controller.setZoom(14.5)
                fetchAllCategoriesAround(newCenter)
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

    private fun fetchPhoton(q: String, lat: Double, lon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = photon.searchNearby(q, lat, lon, 50)
                if (!resp.isSuccessful) throw Exception("Photon API error: ${resp.code()}")

                val feats = resp.body()!!.features
                withContext(Dispatchers.Main) {
                    feats.forEach { feat ->
                        val coords = feat.geometry.coordinates
                        val name = feat.properties["name"] as? String ?: "Facility"
                        val pt = GeoPoint(coords[1], coords[0])
                        val randomId = generateRandomId()
                        val randomCapacity = "${Random.nextInt(15, 90)}% Capacity"

                        val facilityMarker = Marker(mapView).apply {
                            position = pt
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = name
                            relatedObject = FacilityData(name, randomId, randomCapacity, pt)
                            icon = getResized(drawableId = R.drawable.map_marker_green_icon, w = 80, h = 80)
                        }

                        facilityMarker.setOnMarkerClickListener { m, _ ->
                            handleMarkerClickWithAnimation(m)
                            true
                        }
                        mapView.overlays.add(facilityMarker)
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

    data class FacilityData(val name: String, val id: String, val capacity: String, val geoPoint: GeoPoint)

    private fun generateRandomId(): String {
        val letters = ('A'..'Z').toList()
        val numbers = ('0'..'9').toList()
        return (1..4).map { letters.random() }.joinToString("") +
                (1..6).map { numbers.random() }.joinToString("")
    }

    private fun updateBottomSheetInfo(data: FacilityData) {
        binding.locationName.text = data.name
        binding.locationId.text = data.id
        binding.locationCapacity.text = data.capacity
    }

    private fun handleMarkerClickWithAnimation(clickedMarker: Marker) {
        // Animate previously selected marker's icon down
        currentlySelectedMarker?.let {
            if (it != clickedMarker) {
                animateMarkerIconScale(it, fromSize = 100, toSize = 80, isSelected = false)
            }
        }

        // Animate clicked marker's icon up
        if (currentlySelectedMarker != clickedMarker) {
            animateMarkerIconScale(clickedMarker, fromSize = 80, toSize = 100, isSelected = true)
            currentlySelectedMarker = clickedMarker
        }

        val data = clickedMarker.relatedObject as? FacilityData
        data?.let {
            fetchRouteDuration(it)
            updateBottomSheetInfo(it)
        }

        bottomSheet.state = BottomSheetBehavior.STATE_EXPANDED
        mapView.invalidate()
    }

    private fun animateMarkerIconScale(marker: Marker, fromSize: Int, toSize: Int, isSelected: Boolean) {
        val animator = ValueAnimator.ofInt(fromSize, toSize)
        animator.duration = 220
        animator.addUpdateListener { animation ->
            val size = animation.animatedValue as Int
            marker.icon = getResized(
                drawableId = if (isSelected) R.drawable.map_marker_dark_icon else R.drawable.map_marker_green_icon,
                w = size,
                h = size
            )
            mapView.invalidate()
        }
        animator.start()
    }

    @SuppressLint("MissingPermission")
    private fun fetchRouteDuration(destinationData: FacilityData) {
        binding.locationDuration.visibility = View.VISIBLE
        binding.locationDuration.text = "..."

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            binding.locationDuration.text = ""
            binding.locationDuration.visibility = View.GONE
            return
        }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { currentLocation ->
                if (currentLocation != null) {
                    val start = currentLocation
                    val end = destinationData.geoPoint
                    val coordinates = "${start.longitude},${start.latitude};${end.longitude},${end.latitude}"

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = routingService.getRoute(coordinates)
                            if (response.isSuccessful && response.body()?.routes?.isNotEmpty() == true) {
                                val durationInSeconds = response.body()!!.routes[0].duration
                                val durationInMinutes = (durationInSeconds / 60).roundToInt()
                                withContext(Dispatchers.Main) {
                                    binding.locationDuration.text = "$durationInMinutes min"
                                    binding.locationDuration.visibility = View.VISIBLE
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    binding.locationDuration.visibility = View.GONE
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                binding.locationDuration.visibility = View.GONE
                            }
                        }
                    }
                } else {
                    binding.locationDuration.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                binding.locationDuration.visibility = View.GONE
            }
    }

    private fun setupDirectionButton() {
        binding.btnDirection.setOnClickListener {
            if (currentlySelectedMarker == null) {
                Toast.makeText(this, "Please select a facility pin on the map first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            getCurrentLocationAndLaunchDirections()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndLaunchDirections() {
        val dest = currentlySelectedMarker?.position ?: return

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { currentLocation ->
                if (currentLocation != null) {
                    val start = GeoPoint(currentLocation.latitude, currentLocation.longitude)
                    fetchAndDrawRoute(start, dest)
                } else {
                    Toast.makeText(this, "Could not get current location. Please enable GPS.", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun decodePolyline(encoded: String): List<GeoPoint> {
        val poly = ArrayList<GeoPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = GeoPoint(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }
        return poly
    }

    private fun fetchAndDrawRoute(start: GeoPoint, end: GeoPoint) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val coordParam = "${start.longitude},${start.latitude};${end.longitude},${end.latitude}"
                val resp = routingService.getRoute(coordParam)
                if (resp.isSuccessful && resp.body() != null && resp.body()!!.routes.isNotEmpty()) {
                    val route = resp.body()!!.routes[0]
                    val points = decodePolyline(route.geometry)

                    withContext(Dispatchers.Main) {
                        mapView.overlays.removeAll { it is Polyline }
                        val line = Polyline().apply {
                            setPoints(points)
                            outlinePaint.color = ContextCompat.getColor(this@MapsActivity, R.color.my_blue_primary)
                            outlinePaint.strokeWidth = 10f
                        }
                        mapView.overlays.add(line)
                        mapView.invalidate()
                        Toast.makeText(this@MapsActivity, "Route found: ${(route.duration / 60).toInt()} min", Toast.LENGTH_LONG).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MapsActivity, "No route found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MapsActivity, "Routing error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun addMarker(pt: GeoPoint, title: String, drawableId: Int, size: Float) {
        if (!::mapView.isInitialized || mapView == null) {
            // MapView is not ready, do not add marker
            return
        }
        Marker(mapView).apply {
            position = pt
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
            icon = getResized(drawableId = drawableId, w = size.toInt(), h = size.toInt())
        }.also { mapView.overlays.add(it) }
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

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_maps
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == binding.bottomNavigation.selectedItemId) {
                false
            } else {
                when (item.itemId) {
                    R.id.nav_home -> {
                        startActivity(Intent(this, MainActivity::class.java)
                            .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP })
                        finish()
                        true
                    }
                    R.id.nav_maps -> true
                    R.id.nav_ai -> {
                        startActivity(Intent(this, AIActivity::class.java)
                            .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP })
                        finish()
                        true
                    }
                    R.id.nav_article -> {
                        startActivity(Intent(this, ArticleActivity::class.java)
                            .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP })
                        finish()
                        true
                    }
                    R.id.nav_more -> {
                        startActivity(Intent(this, MoreActivity::class.java)
                            .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP })
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
    }

    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
}
