package pl.pw.geogame

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.geometry.GeometryEngine
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReference
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.layers.ArcGISMapImageLayer
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.BasemapStyle
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener
import com.esri.arcgisruntime.mapping.view.Graphic
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol
import com.google.gson.Gson
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Region
import pl.pw.geogame.data.model.BeaconFileWrapper
import pl.pw.geogame.data.model.QuizData
import pl.pw.geogame.data.model.ReferenceBeacon
import pl.pw.geogame.data.model.quizQuestions
import java.io.BufferedReader
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var startGameButton: Button
    private lateinit var scoreTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var floorButtons: List<Button>

    // Beacon
    private var beaconManager: BeaconManager? = null
    private val referenceBeacons = mutableListOf<ReferenceBeacon>()
    private lateinit var connectionStateReceiver: ConnectionStateChangeReceiver
    private val region = Region("all-beacons-region", null, null, null)
    private var wasConnectionHintShown = false
    private val beaconJsonFileNames = listOf(
        "beacons_gg0.txt",
        "beacons_gg1.txt",
        "beacons_gg2b1.txt",
        "beacons_gg3b2.txt",
        "beacons_gg3b3.txt",
        "beacons_gg4.txt",
        "beacons_gg_out.txt"
    )

    // Map
    private lateinit var mapView: MapView
    private lateinit var graphicsOverlay: GraphicsOverlay
    private val floorGraphicsOverlays = mutableMapOf<Int, GraphicsOverlay>()
    private var deviceGraphic: Graphic? = null
    private var quizGraphic: Graphic? = null
    private var floorLayer: ArcGISMapImageLayer? = null
    private var currentFloor = 0
    private val defaultSpatialRef = SpatialReference.create(2180)
    private val floorLayers = mutableMapOf<Int, ArcGISMapImageLayer>()
    private val floorMapLayers = mapOf(
        0 to "https://arcgis.cenagis.edu.pl/server/rest/services/SION2_Topo_MV/sion2_wms_topo_GG_f19/MapServer",
        1 to "https://arcgis.cenagis.edu.pl/server/rest/services/SION2_Topo_MV/sion2_wms_topo_GG_f2/MapServer",
        2 to "https://arcgis.cenagis.edu.pl/server/rest/services/SION2_Topo_MV/sion2_wms_topo_GG_f36/MapServer",
        3 to "https://arcgis.cenagis.edu.pl/server/rest/services/SION2_Topo_MV/sion2_wms_topo_GG_f48/MapServer",
        4 to "https://arcgis.cenagis.edu.pl/server/rest/services/SION2_Topo_MV/sion2_wms_topo_GG_f5/MapServer"
    )

    // Quiz
    private val quizPoints = quizQuestions
    private var userScore = 0
    private var userTime = 0
    private var startTimeMillis: Long = 0
    private var totalElapsedTimeMillis: Long = 0
    private var currentQuizIndex = 0
    private val timeHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var timeRunnable: Runnable? = null

    companion object {
        private const val TAG="pw.MainActivity" // dostęp statyczny
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.entries.any { !it.value }) {
                Toast.makeText(
                    this,
                    "Without granting permissions, the app will not work properly.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                listenForConnectionChanges()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ArcGISRuntimeEnvironment.setApiKey(getString(R.string.maps_api_key))

        mapView = findViewById(R.id.map_view)
        graphicsOverlay = GraphicsOverlay()
        mapView.graphicsOverlays.add(graphicsOverlay)
// ------------------------------------- do testów -------------------------------------
        mapView.setOnTouchListener(object : DefaultMapViewOnTouchListener(this, mapView) {
            @SuppressLint("ClickableViewAccessibility")
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val screenPoint = android.graphics.Point(e.x.toInt(), e.y.toInt())

                floorGraphicsOverlays[currentFloor]?.let { overlay ->
                    val identifyFuture = mapView.identifyGraphicsOverlayAsync(overlay, screenPoint, 10.0, false)

                    identifyFuture.addDoneListener {
                        val result = identifyFuture.get()
                        val graphics = result.graphics

                        if (quizGraphic != null && graphics.contains(quizGraphic)) {
                            showQuizDialog(quizPoints[currentQuizIndex])
                        }
                    }
                }

                mapView.performClick()
                return super.onSingleTapConfirmed(e)
            }

        })
// ------------------------------------------------------------------------------------------


        startGameButton = findViewById(R.id.position_button)
        scoreTextView = findViewById(R.id.points_text)
        timeTextView = findViewById(R.id.time_text)

        buttonListener()

        setUpUI()
        setupFloorButtons()
        setUpMap()
        requestRequiredPermissions()
        setUpBeaconManager()
        loadBeaconsFromAssets()
    }

    private fun buttonListener() {

        startGameButton.setOnClickListener {
            checkAndStartGame()
        }
    }

    private fun stopScanning() {
        beaconManager?.stopRangingBeacons(region)
        Log.d(TAG, "Positioning stopped.")
    }

    private fun requestRequiredPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }
        if (allPermissionsGranted(permissions)) {
            listenForConnectionChanges()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun allPermissionsGranted(
        permissions: Array<String>,
    ): Boolean {
        permissions.forEach { permissionName ->
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    permissionName
                ) == PackageManager.PERMISSION_DENIED
            ) {
                return false
            }
        }
        return true
    }

    private fun listenForConnectionChanges(): Boolean {
        if (!wasConnectionHintShown) {
            Toast.makeText(
                this,
                "Upewnij się, że lokalizacja i bluetooth są włączone",
                Toast.LENGTH_SHORT
            ).show()
            wasConnectionHintShown = true
        }

        connectionStateReceiver = ConnectionStateChangeReceiver()
        val filter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }

        registerReceiver(connectionStateReceiver, filter)

        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        // set to True because of emulator
//        val bluetoothEnabled = bluetoothManager.adapter.isEnabled
        val bluetoothEnabled = true

        return gpsEnabled && bluetoothEnabled
    }

    private fun setUpUI() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun addBaseLayers(map: ArcGISMap) {
        for ((floor, url) in floorMapLayers) {
            val layer = ArcGISMapImageLayer(url)
            layer.minScale = 7000.0
            layer.maxScale = 0.0
            layer.opacity = 0.9F
            floorLayers[floor] = layer
        }
        showFloor(currentFloor)

    }

    private fun showFloor(floor: Int) {
        floorLayer?.let {
            mapView.map.operationalLayers.remove(it)
        }

        floorLayers[floor]?.let {
            floorLayer = it
            mapView.map.operationalLayers.add(it)
        }

        currentFloor = floor
        updateFloorButtonUI(floor)

        floorGraphicsOverlays.forEach { (floorKey, overlay) ->
            overlay.isVisible = floorKey == floor
        }
        mapView.invalidate()
    }

    private fun setupFloorButtons() {
        floorButtons = listOf(
            findViewById<Button>(R.id.floor_label_0),
            findViewById<Button>(R.id.floor_label_1),
            findViewById<Button>(R.id.floor_label_2),
            findViewById<Button>(R.id.floor_label_3),
            findViewById<Button>(R.id.floor_label_4)
        )

        floorButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                showFloor(index)
                updateFloorButtonUI(index)            }
        }

        updateFloorButtonUI(currentFloor)
    }
    private fun updateFloorButtonUI(activeIndex: Int) {
        if (!::floorButtons.isInitialized) return
        floorButtons.forEachIndexed { index, button ->
            val bgColor = if (index == activeIndex) R.color.colorPrimary else R.color.colorSecondary
            val textColor = if (index == activeIndex) R.color.white else R.color.colorPrimary

            button.setBackgroundColor(ContextCompat.getColor(this, bgColor))
            button.setTextColor(ContextCompat.getColor(this, textColor))
        }
    }

    private fun setUpMap() {
        val arcGISMap = ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC)
        mapView.map = arcGISMap

        floorMapLayers.keys.forEach { floor ->
            val overlay = GraphicsOverlay()
            overlay.isVisible = floor == currentFloor
            floorGraphicsOverlays[floor] = overlay
            mapView.graphicsOverlays.add(overlay)
        }

        val startPoint = Point(21.0103, 52.2206, SpatialReferences.getWgs84())
        val startPoint2180 = GeometryEngine.project(startPoint, defaultSpatialRef) as Point
        mapView.setViewpointCenterAsync(startPoint2180, 2000.0)
        addBaseLayers(arcGISMap)
    }


    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume")

    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("MainActivity", "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(connectionStateReceiver)
        beaconManager?.stopRangingBeacons(region)
        Log.d("MainActivity", "onDestroy")

    }

    private fun setUpBeaconManager () {
        beaconManager =  BeaconManager.getInstanceForApplication(this)
        listOf(
            BeaconParser.EDDYSTONE_UID_LAYOUT,
            BeaconParser.EDDYSTONE_TLM_LAYOUT,
            BeaconParser.EDDYSTONE_URL_LAYOUT
        ).forEach {
            beaconManager?.beaconParsers?.add(BeaconParser().setBeaconLayout(it))
        }
    }

    private fun loadBeaconsFromAssets() {
        val gson = Gson()

        for (fileName in beaconJsonFileNames) {
            try {
                val inputStream = assets.open(fileName)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonText = reader.readText()
                reader.close()

                val beaconWrapper = gson.fromJson(jsonText, BeaconFileWrapper::class.java)
                beaconWrapper.items.forEach { item ->
                    item.beaconUid?.let {
                        referenceBeacons.add(
                            ReferenceBeacon(
                                beaconUid = item.beaconUid,
                                latitude = item.latitude,
                                longitude = item.longitude
                            )
                        )
                    }
                }

                Log.d("BeaconLoader", "Loaded ${beaconWrapper.items.size} valid beacons from $fileName")
            } catch (e: Exception) {
                Log.e("BeaconLoader", "Error loading $fileName: ${e.message}")
            }
        }

        Log.d("BeaconLoader", "Total reference beacons loaded: ${referenceBeacons.size}")
    }

    private fun startScanning() {

        try {
            beaconManager?.startRangingBeacons(region)

            beaconManager?.addRangeNotifier { beacons, _ ->
                Log.d(TAG, "${beacons.size} beacons found.")

                beacons.forEach { beacon ->
                    Log.d(TAG, "Beacon detected: ID=${beacon.id1}, Distance=${beacon.distance} RSSI=${beacon.rssi}")
                }

                if (beacons.isNotEmpty()) {
                    calculatePosition(beacons)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error occurred while scanning beacons: ", e)
        }
    }

    private fun calculatePosition(beacons: MutableCollection<Beacon>) {
        if (beacons.isEmpty()) {
            Log.d(TAG, "No beacons were detected.")
            return
        }

        var weightedLatitudeSum = 0.0
        var weightedLongitudeSum = 0.0
        var weightSum = 0.0

        beacons.forEach { beacon ->
            val realBeacon = referenceBeacons.find { it.beaconUid == beacon.bluetoothAddress }
            if (realBeacon != null && beacon.distance > 0) {
                val weight = 1.0 / beacon.distance
                weightedLatitudeSum += realBeacon.latitude * weight
                weightedLongitudeSum += realBeacon.longitude * weight
                weightSum += weight
            }
        }

        if (weightSum == 0.0) {
            Log.e(TAG, "Weight sum is zero, cannot calculate position.")
            return
        }

        val estimatedLatitude = weightedLatitudeSum / weightSum
        val estimatedLongitude = weightedLongitudeSum / weightSum

        Log.d("PositionCalculation", "Estimated position: Lat=$estimatedLatitude, Lon=$estimatedLongitude")
        updateUserPosition(estimatedLatitude, estimatedLongitude)
    }

    private fun checkAndStartGame() {
        resetGame()

        startGameButton.visibility = Button.INVISIBLE

        val gpsAndBluetoothEnabled = listenForConnectionChanges()
        if (!gpsAndBluetoothEnabled) {
            return
        }
        startScanning()

        // ------------------------------ do testów -----------------------------------------
        val userLon = 21.010318215414
        val userLat = 52.220730290226065
        updateUserPosition(userLat, userLon)
        // ----------------------------------------------------------------------------------

        addQuizPointAsMarker()
        startTimeMillis = System.currentTimeMillis()

        timeRunnable = object : Runnable {
            override fun run() {
                val sessionTime = System.currentTimeMillis() - startTimeMillis + totalElapsedTimeMillis
                userTime = (sessionTime / 1000).toInt()
                timeTextView.text = "Czas: ${formatTime(userTime)}"
                timeHandler.postDelayed(this, 1000)
            }
        }
        timeHandler.post(timeRunnable!!)

    }

    private fun resetGame() {
        userScore = 0
        scoreTextView.text = "Punkty: 0"
        timeRunnable?.let { timeHandler.removeCallbacks(it) }
        timeTextView.text = "Czas: 0"

        userTime = 0
        totalElapsedTimeMillis = 0
        startTimeMillis = 0

        currentQuizIndex = 0
        floorGraphicsOverlays.values.forEach { it.graphics.clear() }

    }

    private fun endGame () {
        startGameButton.text = "Zagraj jeszcze raz"
        startGameButton.visibility = Button.VISIBLE
        timeRunnable?.let { timeHandler.removeCallbacks(it) }
        AlertDialog.Builder(this)
            .setTitle("Gra ukończona!")
            .setMessage("Wynik: $userScore / 140 pkt\nCzas: ${formatTime(userTime)}")
            .setPositiveButton("Zamknij") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun drawMarker(point: Point, icon: Drawable, graphicsOverlay: GraphicsOverlay): Graphic {
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            icon.setBounds(0, 0, 50, 50)
            icon.draw(canvas)
        }

        val symbol = PictureMarkerSymbol.createAsync(BitmapDrawable(resources, bitmap)).get().apply {
            width = 40f
            height = 40f
            leaderOffsetY = 10f
            offsetY = 10f
        }

        val graphic = Graphic(point, symbol)
        graphicsOverlay.graphics.add(graphic)

        mapView.invalidate()  // refresh map
        return graphic
    }


    private fun updateUserPosition(latitude: Double, longitude: Double) {
        val userIcon = ContextCompat.getDrawable(this, R.drawable.man_avatar)!!
        val point = Point(longitude, latitude, SpatialReferences.getWgs84())
        val point2180 = GeometryEngine.project(point, defaultSpatialRef) as Point

        if (deviceGraphic == null) {
            deviceGraphic = drawMarker(point2180, userIcon, graphicsOverlay)

        } else {
            deviceGraphic!!.geometry = point2180
        }
        checkIfReachedQuizPoint(latitude, longitude)
        mapView.invalidate()
    }


    private fun addQuizPointAsMarker() {
        quizGraphic?.let { oldGraphic ->
            floorGraphicsOverlays.values.forEach { overlay ->
                overlay.graphics.remove(oldGraphic)
            }
            quizGraphic = null
        }

        if (currentQuizIndex >= quizPoints.size) {
            endGame()
            return
        }

        val currentPoint = quizPoints[currentQuizIndex]
        val quizIcon = ContextCompat.getDrawable(this, R.drawable.locationpin) ?: return

        val point = Point(currentPoint.longitude, currentPoint.latitude, SpatialReferences.getWgs84())
        val point2180 = GeometryEngine.project(point, defaultSpatialRef) as Point

        val overlay = floorGraphicsOverlays[currentPoint.floor.ordinal] ?: return

        quizGraphic = drawMarker(point2180, quizIcon, overlay)

        if (currentFloor == currentPoint.floor.ordinal) {
            mapView.setViewpointCenterAsync(point2180, 2000.0)
        }
    }

    private fun checkIfReachedQuizPoint(userLat: Double, userLon: Double) {
        if (currentQuizIndex >= quizPoints.size) return

        val point = quizPoints[currentQuizIndex]
        val distance = measureDistance(userLat, userLon, point.latitude, point.longitude)

        if (distance < 5.0 && !point.isVisited) {  // 5 metrów tolerancji
            quizPoints[currentQuizIndex] = point.copy(isVisited = true)
            Log.d("MainActiviy", "Place ${quizPoints[currentQuizIndex].id} reached")
            showQuizDialog(point)
        }
    }

    private fun measureDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val loc1 = android.location.Location("").apply {
            latitude = lat1
            longitude = lon1
        }
        val loc2 = android.location.Location("").apply {
            latitude = lat2
            longitude = lon2
        }
        return loc1.distanceTo(loc2).toDouble()
    }

    private fun formatTime(secondsTotal: Int): String {
        val minutes = secondsTotal / 60
        val seconds = secondsTotal % 60

        return if (minutes > 0) {
            "$minutes min $seconds sek"
        } else {
            "$seconds sek"
        }
    }

    private fun showQuizDialog(quizPoint: QuizData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_quiz, null)
        val questionTextView = dialogView.findViewById<TextView>(R.id.questionTextView)
        val answersRadioGroup = dialogView.findViewById<RadioGroup>(R.id.answersRadioGroup)

        questionTextView.text = quizPoint.question

        quizPoint.answers.forEachIndexed { index, answer ->
            val radioButton = RadioButton(this)
            radioButton.text = answer
            radioButton.id = index
            radioButton.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
            radioButton.buttonTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorSecondary))

            answersRadioGroup.addView(radioButton)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Zatwierdź", null)
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))

            button.setOnClickListener {
                val selectedId = answersRadioGroup.checkedRadioButtonId
                if (selectedId == -1) {
                    Toast.makeText(this, "Wybierz odpowiedź!", Toast.LENGTH_SHORT).show()
                } else {
                    val correct = selectedId == quizPoint.correctAnswerIndex
                    if (correct) {
                        userScore += quizPoint.difficulty.points
                        Toast.makeText(
                            this,
                            "Poprawna odpowiedź! +${quizPoint.difficulty.points} pkt.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(this, "Niepoprawna odpowiedź!", Toast.LENGTH_SHORT).show()
                    }
                    scoreTextView.text = "Punkty: $userScore"
                    currentQuizIndex++
                    addQuizPointAsMarker()
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }


}