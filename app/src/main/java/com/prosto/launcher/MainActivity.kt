package com.prosto.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.LocationManager
import android.location.Location
import android.location.LocationListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.vector.ImageVector

private data class WeatherCity(val name: String, val latitude: Double, val longitude: Double)
private data class WeatherInfo(val temperature: Int, val description: String, val emoji: String)
private val russianWeatherCities = listOf(
 WeatherCity("Москва",55.7558,37.6173), WeatherCity("Санкт-Петербург",59.9343,30.3351), WeatherCity("Казань",55.7961,49.1064), WeatherCity("Екатеринбург",56.8389,60.6057), WeatherCity("Новосибирск",55.0084,82.9357), WeatherCity("Краснодар",45.0355,38.9753), WeatherCity("Нижний Новгород",56.2965,43.9361), WeatherCity("Самара",53.1959,50.1002), WeatherCity("Сочи",43.5855,39.7231), WeatherCity("Ростов-на-Дону",47.2357,39.7015), WeatherCity("Воронеж",51.6608,39.2003), WeatherCity("Уфа",54.7388,55.9721), WeatherCity("Челябинск",55.1644,61.4368), WeatherCity("Пермь",58.0105,56.2502), WeatherCity("Омск",54.9885,73.3242), WeatherCity("Волгоград",48.7080,44.5133), WeatherCity("Красноярск",56.0153,92.8932), WeatherCity("Тюмень",57.1522,65.5272), WeatherCity("Иркутск",52.2864,104.2807), WeatherCity("Владивосток",43.1155,131.8855), WeatherCity("Калининград",54.7104,20.4522), WeatherCity("Саратов",51.5336,46.0343)
)
private suspend fun loadWeather(city: WeatherCity, source: String, apiKey: String): Pair<WeatherInfo?, String?> = withContext(Dispatchers.IO) {
 runCatching {
  fun openWeatherConnection(): java.net.HttpURLConnection {
   val connection = URL("https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}&current=temperature_2m,weather_code&timezone=auto").openConnection() as java.net.HttpURLConnection
   connection.connectTimeout = 20000
   connection.readTimeout = 20000
   connection.setRequestProperty("User-Agent", "ProstoLauncher/1.0 (https://github.com/eldar2013/Prosto-launcher)")
   return connection
  }
  fun parseOpenMeteo(): WeatherInfo {
   val json = JSONObject(openWeatherConnection().inputStream.bufferedReader().use { it.readText() })
   val current = json.getJSONObject("current")
   val code = current.getInt("weather_code")
   val (description, emoji) = when(code) { 0->"Ясно" to "☀️"; 1,2->"Переменная облачность" to "🌤️"; 3->"Облачно" to "☁️"; 45,48->"Туман" to "🌫️"; in 51..67->"Дождь" to "🌧️"; in 71..77->"Снег" to "❄️"; in 80..82->"Ливень" to "🌦️"; in 85..86->"Снегопад" to "🌨️"; in 95..99->"Гроза" to "⛈️"; else->"Погода" to "🌡️" }
   return WeatherInfo(kotlin.math.round(current.getDouble("temperature_2m")).toInt(), description, emoji)
  }
  if (source == "yandex" || source == "google") {
   val connection = when (source) {
    "yandex" -> URL("https://api.weather.yandex.ru/v2/informers?lat=${city.latitude}&lon=${city.longitude}&lang=ru_RU").openConnection().apply { setRequestProperty("X-Yandex-Weather-Key", apiKey) }
    else -> URL("https://weather.googleapis.com/v1/currentConditions:lookup?key=$apiKey&location.latitude=${city.latitude}&location.longitude=${city.longitude}").openConnection()
   }
   connection.connectTimeout = 20000
   connection.readTimeout = 20000
   connection.setRequestProperty("User-Agent", "ProstoLauncher/1.0 (https://github.com/eldar2013/Prosto-launcher)")
   val json = JSONObject(connection.getInputStream().bufferedReader().use { it.readText() })
   if (source == "yandex") {
    val fact = json.getJSONObject("fact")
    val condition = fact.optString("condition", "cloudy")
    val (description, emoji) = when {
     condition.contains("clear") -> "Ясно" to "☀️"
     condition.contains("partly") -> "Переменная облачность" to "🌤️"
     condition.contains("cloud") -> "Облачно" to "☁️"
     condition.contains("rain") || condition.contains("drizzle") -> "Дождь" to "🌧️"
     condition.contains("snow") -> "Снег" to "❄️"
     condition.contains("thunder") -> "Гроза" to "⛈️"
     condition.contains("fog") -> "Туман" to "🌫️"
     else -> condition to "🌡️"
    }
    WeatherInfo(fact.getInt("temp"), description, emoji)
   } else {
    val current = json.getJSONObject("temperature").getDouble("degrees")
    val description = json.optJSONObject("weatherCondition")?.optJSONObject("description")?.optString("text") ?: "Погода"
    val lower = description.lowercase()
    val emoji = when { "rain" in lower || "дожд" in lower -> "🌧️"; "snow" in lower || "снег" in lower -> "❄️"; "thunder" in lower || "гроз" in lower -> "⛈️"; "cloud" in lower || "облач" in lower -> "☁️"; else -> "☀️" }
    WeatherInfo(kotlin.math.round(current).toInt(), description, emoji)
   }
  } else {
   // MET Norway: бесплатный глобальный прогноз, без API-ключа.
   val metConnection = URL("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=${city.latitude}&lon=${city.longitude}").openConnection() as java.net.HttpURLConnection
   metConnection.connectTimeout = 20000
   metConnection.readTimeout = 20000
   metConnection.setRequestProperty("User-Agent", "ProstoLauncher/1.0 (https://github.com/eldar2013/Prosto-launcher)")
   val metJson = JSONObject(metConnection.inputStream.bufferedReader().use { it.readText() })
   val first = metJson.getJSONObject("properties").getJSONArray("timeseries").getJSONObject(0).getJSONObject("data")
   val temperature = first.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature")
   val next = first.optJSONObject("next_1_hours") ?: first.optJSONObject("next_6_hours")
   val symbol = next?.optJSONObject("summary")?.optString("symbol_code").orEmpty().substringBefore("_")
   val (description, emoji) = when {
    symbol.startsWith("clearsky") -> "Ясно" to "☀️"
    symbol.startsWith("fair") || symbol.startsWith("partlycloudy") -> "Переменная облачность" to "🌤️"
    symbol.startsWith("cloudy") -> "Облачно" to "☁️"
    symbol.startsWith("fog") -> "Туман" to "🌫️"
    symbol.startsWith("rain") || symbol.startsWith("lightrain") || symbol.startsWith("heavyrain") || symbol.startsWith("sleet") -> "Дождь" to "🌧️"
    symbol.startsWith("snow") -> "Снег" to "❄️"
    symbol.startsWith("thunder") -> "Гроза" to "⛈️"
    else -> "Погода" to "🌡️"
   }
   WeatherInfo(kotlin.math.round(temperature).toInt(), description, emoji)
  }
 }.recoverCatching {
  // Резервный источник на случай недоступности MET Norway.
  val connection = URL("https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}&current=temperature_2m,weather_code&timezone=auto").openConnection() as java.net.HttpURLConnection
  connection.connectTimeout = 20000
  connection.readTimeout = 20000
  connection.setRequestProperty("User-Agent", "ProstoLauncher/1.0 (https://github.com/eldar2013/Prosto-launcher)")
  val current = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getJSONObject("current")
  val code = current.getInt("weather_code")
  val (description, emoji) = when(code) { 0->"Ясно" to "☀️"; 1,2->"Переменная облачность" to "🌤️"; 3->"Облачно" to "☁️"; 45,48->"Туман" to "🌫️"; in 51..67->"Дождь" to "🌧️"; in 71..77->"Снег" to "❄️"; in 80..82->"Ливень" to "🌦️"; in 85..86->"Снегопад" to "🌨️"; in 95..99->"Гроза" to "⛈️"; else->"Погода" to "🌡️" }
  WeatherInfo(kotlin.math.round(current.getDouble("temperature_2m")).toInt(), description, emoji)
 }.fold(onSuccess = { it to null }, onFailure = { error -> null to (error.message ?: error.javaClass.simpleName) })
}

private data class WorkspaceWidget(
    val id: Int,
    val type: String,
    val x: Float = 8f,
    val y: Float = 8f,
    val width: Float = 160f,
    val height: Float = 112f,
    val page: Int = 0
)

private data class LauncherApp(
    val label: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable
)

private enum class LauncherTheme { SYSTEM, LIGHT, DARK }
private enum class IconShapeChoice { CIRCLE, SQUARE, VERY_SUNNY, COOKIE_9, SOFT_BURST, CLOVER }

private val Purple = Color(0xFF9A78F2)
private val Lavender = Color(0xFFC5A9FF)
private val DarkInk = Color(0xFF171326)

class MainActivity : ComponentActivity() {
    companion object {
        const val REQ_CONFIGURE_WIDGET = 4711
        // Вызывается, когда закрылся экран настройки виджета (true = пользователь подтвердил).
        var onWidgetConfigured: ((Boolean) -> Unit)? = null
    }

    @Deprecated("Нужен для экрана настройки виджетов (AppWidgetHost)")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CONFIGURE_WIDGET) {
            onWidgetConfigured?.invoke(resultCode == android.app.Activity.RESULT_OK)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Не включаем системное размытие окна при показе папки.
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)

        // Если лаунчер упал при запуске, вместо белого экрана покажем текст ошибки.
        val crashPrefs = getSharedPreferences("prosto_crash", MODE_PRIVATE)
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                crashPrefs.edit().putString("last_crash", android.util.Log.getStackTraceString(error)).commit()
            }
            previousHandler?.uncaughtException(thread, error)
        }
        val lastCrash = crashPrefs.getString("last_crash", null)
        if (lastCrash != null) {
            crashPrefs.edit().remove("last_crash").commit()
            showCrashScreen(lastCrash)
        } else {
            setContent { ProstoHome() }
        }
    }

    private fun showCrashScreen(trace: String) {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val title = android.widget.TextView(this).apply {
            text = "Prosto Launcher упал при запуске. Сделай скриншот этого экрана и пришли его разработчику."
            textSize = 16f
            setTextColor(android.graphics.Color.BLACK)
            setPadding(pad, pad * 2, pad, pad / 2)
        }
        val startButton = android.widget.Button(this).apply {
            text = "Попробовать запустить лаунчер"
            setOnClickListener { setContent { ProstoHome() } }
        }
        val details = android.widget.TextView(this).apply {
            text = trace.take(6000)
            textSize = 11f
            setTextColor(android.graphics.Color.DKGRAY)
            setTextIsSelectable(true)
            setPadding(pad, pad / 2, pad, pad)
        }
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
            addView(title)
            addView(startButton)
            addView(android.widget.ScrollView(this@MainActivity).apply { addView(details) })
        }
        setContentView(root)
    }
}

@Composable
private fun ProstoHome() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prosto_launcher", 0) }
    var themeChoice by remember {
        mutableStateOf(
            when (prefs.getString("theme_choice", "SYSTEM")) {
                "LIGHT" -> LauncherTheme.LIGHT
                "DARK" -> LauncherTheme.DARK
                else -> LauncherTheme.SYSTEM
            }
        )
    }
    val dark = when (themeChoice) {
        LauncherTheme.LIGHT -> false
        LauncherTheme.DARK -> true
        LauncherTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val colors = if (dark) darkColorScheme(
        primary = Lavender, secondary = Purple, background = DarkInk,
        surface = Color(0xFF211B35), onPrimary = DarkInk,
        onBackground = Color.White, onSurface = Color(0xFFF5F0FF),
        surfaceVariant = Color(0xFF39304F), onSurfaceVariant = Color(0xFFD4C9E8)
    ) else lightColorScheme(
        primary = Purple, secondary = Lavender, background = Color(0xFFF5F0FF),
        surface = Color(0xFFF8F4FF), onPrimary = Color.White,
        onBackground = DarkInk, onSurface = DarkInk,
        surfaceVariant = Color(0xFFE8DFFF), onSurfaceVariant = Color(0xFF514767)
    )
    MaterialTheme(colorScheme = colors) {
        ProstoContent(
            prefs = prefs,
            themeChoice = themeChoice,
            dark = dark,
            onThemeChange = {
                themeChoice = it
                prefs.edit().putString("theme_choice", it.name).apply()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ProstoContent(
    prefs: android.content.SharedPreferences,
    themeChoice: LauncherTheme,
    dark: Boolean,
    onThemeChange: (LauncherTheme) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val gridCellPitch = ((configuration.screenWidthDp - 32 - 12) / 4f) + 4f
    val pm = context.packageManager
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember { AppWidgetHost(context, 2048) }
    var hostedWidgetIds by remember {
        mutableStateOf(prefs.getString("hosted_widget_ids", "").orEmpty()
            .split(",").mapNotNull { it.toIntOrNull() })
    }
    fun readWorkspaceWidgets(): List<WorkspaceWidget> = prefs.getStringSet("workspace_widgets", emptySet())
        ?.mapNotNull { entry ->
            val parts = entry.split(",")
            if (parts.size >= 6) runCatching {
                WorkspaceWidget(parts[0].toInt(), parts[1], parts[2].toFloat(), parts[3].toFloat(), parts[4].toFloat(), parts[5].toFloat(),
                    parts.getOrNull(6)?.toIntOrNull() ?: 0)
            }.getOrNull() else null
        }.orEmpty()
    var workspaceWidgets by remember { mutableStateOf(readWorkspaceWidgets()) }
    var workspacePage by remember { mutableIntStateOf(prefs.getInt("workspace_current_page", 0).coerceIn(0, prefs.getInt("workspace_page_count", 1).coerceAtLeast(1) - 1)) }
    var workspacePageCount by remember { mutableIntStateOf(prefs.getInt("workspace_page_count", 1).coerceAtLeast(1)) }
    fun saveWorkspaceWidgets(items: List<WorkspaceWidget>) {
        workspaceWidgets = items
        prefs.edit().putStringSet("workspace_widgets", items.map {
            "${it.id},${it.type},${it.x},${it.y},${it.width},${it.height},${it.page}"
        }.toSet()).apply()
    }
    val screenDensity = context.resources.displayMetrics.density
    // Размеры виджетов в AppWidgetProviderInfo задаются в ПИКСЕЛЯХ, а раскладка у нас в dp.
    // Переводим пиксели в dp и считаем, сколько ячеек сетки занимает виджет.
    fun widgetSizeFor(info: AppWidgetProviderInfo?): Pair<Float, Float> {
        val minWidthDp = (info?.minWidth ?: 0) / screenDensity
        val minHeightDp = (info?.minHeight ?: 0) / screenDensity
        val cols = kotlin.math.ceil((minWidthDp + 30f) / 70f).toInt().coerceIn(1, 4)
        val rows = kotlin.math.ceil((minHeightDp + 30f) / 70f).toInt().coerceIn(1, 5)
        return (cols * gridCellPitch - 4f) to (rows * 98f - 12f)
    }
    // Заполняется ниже, когда известен список приложений на столе. Нужен, чтобы виджет
    // не ставился поверх иконок и не прятал их.
    val appCellOccupied = remember { arrayOf<(Int) -> Boolean>({ false }) }
    fun nextWidgetPosition(page: Int, width: Float, height: Float): Pair<Float, Float>? {
        val maxWidth = gridCellPitch * 4f - 4f
        val maxHeight = 574f
        val safeWidth = width.coerceAtMost(maxWidth)
        val safeHeight = height.coerceAtMost(maxHeight)
        for (row in 0..5) {
            for (col in 0..3) {
                val x = col * gridCellPitch
                val y = row * 98f
                if (x + safeWidth > maxWidth + 4f || y + safeHeight > maxHeight + 12f) continue
                val overlaps = workspaceWidgets.any { other ->
                    other.page == page &&
                        x < other.x + other.width && x + safeWidth > other.x &&
                        y < other.y + other.height && y + safeHeight > other.y
                }
                val coversApp = page == workspacePage && (0 until 24).any { idx ->
                    val cellLeft = (idx % 4) * gridCellPitch
                    val cellTop = (idx / 4) * 98f
                    appCellOccupied[0](idx) &&
                        x < cellLeft + gridCellPitch && x + safeWidth + 4f > cellLeft &&
                        y < cellTop + 98f && y + safeHeight + 12f > cellTop
                }
                if (!overlaps && !coversApp) return x to y
            }
        }
        return null
    }
    fun widgetsOverlap(a: WorkspaceWidget, b: WorkspaceWidget): Boolean =
        a.page == b.page &&
            a.x < b.x + b.width && a.x + a.width > b.x &&
            a.y < b.y + b.height && a.y + a.height > b.y
    fun addOwnWidget(type: String) {
        val own = workspaceWidgets.filter { it.type != "android" }
        val id = (own.minOfOrNull { it.id } ?: 0) - 1
        val width = gridCellPitch * 4f - 4f
        val height = 120f
        val position = nextWidgetPosition(workspacePage, width, height)
        if (position == null) {
            Toast.makeText(context, "Нет свободного места для виджета", Toast.LENGTH_SHORT).show()
            return
        }
        saveWorkspaceWidgets(workspaceWidgets + WorkspaceWidget(
            id = id, type = type,
            x = position.first, y = position.second,
            width = width, height = height,
            page = workspacePage
        ))
    }
    var widgetSourceOpen by remember { mutableStateOf(false) }
    var selectedWorkspaceWidget by remember { mutableStateOf<Int?>(null) }
    var resizingWidgetId by remember { mutableStateOf<Int?>(null) }
    var draggingWidget by remember { mutableStateOf(false) }
    var weatherCityName by remember { mutableStateOf(prefs.getString("weather_city_name", "") ?: "") }
    var weatherLatitude by remember { mutableStateOf(prefs.getString("weather_latitude", "")?.toDoubleOrNull()) }
    var weatherLongitude by remember { mutableStateOf(prefs.getString("weather_longitude", "")?.toDoubleOrNull()) }
    var weatherCityDialog by remember { mutableStateOf(false) }
    var pendingWeatherWidget by remember { mutableStateOf(false) }
    var weatherInfo by remember { mutableStateOf<WeatherInfo?>(null) }
    var weatherLoading by remember { mutableStateOf(false) }
    var weatherError by remember { mutableStateOf<String?>(null) }
    var weatherRefreshToken by remember { mutableIntStateOf(0) }
    var activeWeatherLocationListener by remember { mutableStateOf<LocationListener?>(null) }
    val weatherLocationHandler = remember { Handler(Looper.getMainLooper()) }
    fun stopWeatherLocationUpdates() {
        activeWeatherLocationListener?.let { listener ->
            runCatching {
                (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager).removeUpdates(listener)
            }
        }
        activeWeatherLocationListener = null
        weatherLocationHandler.removeCallbacksAndMessages("weather_location_timeout")
    }
    fun applyWeatherLocation(latitude: Double, longitude: Double, name: String) {
        weatherLatitude = latitude
        weatherLongitude = longitude
        weatherCityName = name
        prefs.edit().putString("weather_city_name", name)
            .putString("weather_latitude", latitude.toString())
            .putString("weather_longitude", longitude.toString()).apply()
        if (pendingWeatherWidget) { pendingWeatherWidget = false; addOwnWidget("weather") }
    }
    fun saveDetectedWeatherLocation(location: Location) {
        val name = runCatching {
            if (Geocoder.isPresent()) Geocoder(context, Locale.getDefault())
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()?.locality
            else null
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: if ((prefs.getString("app_language", "ru") ?: "ru") == "en") "Current location" else "Текущее местоположение"
        applyWeatherLocation(location.latitude, location.longitude, name)
    }
    fun detectWeatherLocation(): Boolean {
        return runCatching {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = manager.getProviders(true).asSequence()
                .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
                .maxByOrNull { it.time } ?: return@runCatching false
            saveDetectedWeatherLocation(location)
            true
        }.getOrDefault(false)
    }
    fun requestFreshWeatherLocation() {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        stopWeatherLocationUpdates()
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                stopWeatherLocationUpdates()
                saveDetectedWeatherLocation(location)
            }
        }
        activeWeatherLocationListener = listener
        val providers = manager.getProviders(true).filter { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        var requested = false
        providers.forEach { provider ->
            requested = runCatching {
                manager.requestLocationUpdates(provider, 1000L, 0f, listener, Looper.getMainLooper())
                true
            }.getOrDefault(requested)
        }
        if (!requested) {
            stopWeatherLocationUpdates()
            weatherCityDialog = true
        } else {
            weatherLocationHandler.postAtTime({
                if (activeWeatherLocationListener != null) {
                    stopWeatherLocationUpdates()
                    weatherCityDialog = true
                }
            }, "weather_location_timeout", android.os.SystemClock.uptimeMillis() + 15000L)
        }
    }
    val weatherLocationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted && !detectWeatherLocation()) weatherCityDialog = true
        else if (!granted) weatherCityDialog = true
    }
    fun requestWeatherWidget() {
        if (weatherCityName.isNotBlank() && weatherLatitude != null && weatherLongitude != null) {
            addOwnWidget("weather")
        } else {
            pendingWeatherWidget = true
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                if (!detectWeatherLocation()) weatherCityDialog = true
            } else weatherLocationPermission.launch(arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ))
        }
    }
    LaunchedEffect(weatherCityName, weatherLatitude, weatherLongitude, weatherRefreshToken) {
        weatherInfo = null
        weatherError = null
        val lat = weatherLatitude
        val lon = weatherLongitude
        if (lat == null || lon == null) return@LaunchedEffect
        val city = WeatherCity(weatherCityName, lat, lon)
        while (true) {
            weatherLoading = true
            val result = loadWeather(city, "openmeteo", "")
            weatherInfo = result.first
            weatherError = result.second?.let { "Ошибка погоды: $it" }
            weatherLoading = false
            delay(3 * 60 * 60 * 1000L)
        }
    }
    DisposableEffect(appWidgetHost) {
        runCatching { appWidgetHost.startListening() }
        onDispose { runCatching { appWidgetHost.stopListening() } }
    }
    var pendingConfigureId by remember { mutableStateOf<Int?>(null) }
    fun placeAndroidWidget(id: Int) {
        if (workspaceWidgets.any { it.id == id }) return
        val provider = appWidgetManager.getAppWidgetInfo(id)
        if (provider == null) {
            runCatching { appWidgetHost.deleteAppWidgetId(id) }
            Toast.makeText(context, "Не удалось добавить виджет: система его не привязала", Toast.LENGTH_LONG).show()
            return
        }
        val (widgetWidth, widgetHeight) = widgetSizeFor(provider)
        val position = nextWidgetPosition(workspacePage, widgetWidth, widgetHeight)
        if (position == null) {
            runCatching { appWidgetHost.deleteAppWidgetId(id) }
            Toast.makeText(
                context,
                "Нет свободного места для виджета. Освободи ячейки на этом рабочем столе или открой другой",
                Toast.LENGTH_LONG
            ).show()
        } else {
            if (id !in hostedWidgetIds) {
                hostedWidgetIds = hostedWidgetIds + id
                prefs.edit().putString("hosted_widget_ids", hostedWidgetIds.joinToString(",")).apply()
            }
            saveWorkspaceWidgets(workspaceWidgets + WorkspaceWidget(
                id, "android",
                x = position.first, y = position.second,
                width = widgetWidth, height = widgetHeight,
                page = workspacePage
            ))
        }
    }
    var pendingPickId by remember { mutableStateOf<Int?>(null) }
    // Виджет уже привязан к нашему хосту: если у него есть экран настройки — показываем его,
    // иначе сразу кладём на рабочий стол.
    fun continueWidgetAdd(id: Int) {
        val info = appWidgetManager.getAppWidgetInfo(id)
        val activity = context.findActivity()
        if (info?.configure != null && activity != null) {
            pendingConfigureId = id
            val started = runCatching {
                appWidgetHost.startAppWidgetConfigureActivityForResult(
                    activity, id, 0, MainActivity.REQ_CONFIGURE_WIDGET, null
                )
            }.isSuccess
            if (!started) {
                pendingConfigureId = null
                placeAndroidWidget(id)
            }
        } else {
            placeAndroidWidget(id)
        }
    }
    // Запрос системного разрешения на привязку виджета (нужен на некоторых прошивках, в т.ч. MIUI).
    val widgetBindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingPickId ?: -1
        pendingPickId = null
        if (result.resultCode == android.app.Activity.RESULT_OK && id > 0) {
            continueWidgetAdd(id)
        } else if (id > 0) {
            runCatching { appWidgetHost.deleteAppWidgetId(id) }
        }
    }
    val widgetPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Некоторые прошивки не возвращают id виджета в ответе — тогда берём тот, что мы выделили сами.
        val extraId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        val id = if (extraId > 0) extraId else (pendingPickId ?: -1)
        pendingPickId = null
        if (result.resultCode == android.app.Activity.RESULT_OK && id > 0) {
            if (appWidgetManager.getAppWidgetInfo(id) != null) {
                continueWidgetAdd(id)
            } else {
                // Виджет выбран, но не привязан. Пробуем привязать сами.
                val provider: android.content.ComponentName? = if (Build.VERSION.SDK_INT >= 33) {
                    result.data?.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, android.content.ComponentName::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra<android.content.ComponentName>(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER)
                }
                if (provider == null) {
                    placeAndroidWidget(id) // покажет сообщение об ошибке и освободит id
                } else {
                    val bound = runCatching { appWidgetManager.bindAppWidgetIdIfAllowed(id, provider) }.getOrDefault(false)
                    if (bound) {
                        continueWidgetAdd(id)
                    } else {
                        pendingPickId = id
                        val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
                        }
                        val launched = runCatching { widgetBindLauncher.launch(bindIntent) }.isSuccess
                        if (!launched) {
                            pendingPickId = null
                            placeAndroidWidget(id)
                        }
                    }
                }
            }
        } else if (id > 0) {
            runCatching { appWidgetHost.deleteAppWidgetId(id) }
        }
    }
    val onWidgetConfigured = rememberUpdatedState<(Boolean) -> Unit> { ok ->
        val id = pendingConfigureId
        pendingConfigureId = null
        if (id != null) {
            if (ok) {
                placeAndroidWidget(id)
            } else {
                runCatching { appWidgetHost.deleteAppWidgetId(id) }
            }
        }
    }
    DisposableEffect(Unit) {
        MainActivity.onWidgetConfigured = { ok -> onWidgetConfigured.value(ok) }
        onDispose { MainActivity.onWidgetConfigured = null }
    }
    var apps by remember { mutableStateOf(emptyList<LauncherApp>()) }
    var search by remember { mutableStateOf("") }
    var drawerOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var settingsPage by remember { mutableStateOf("") }
    var settingsQuery by remember { mutableStateOf("") }
    var isDefaultLauncher by remember { mutableStateOf(false) }
    var expandedWidgetGroup by remember { mutableStateOf<String?>(null) }
    var appLanguage by remember { mutableStateOf(prefs.getString("app_language", "ru") ?: "ru") }
    var widgetsOpen by remember { mutableStateOf(false) }
    var quickMenuOpen by remember { mutableStateOf(false) }
    var workspaceContextApp by remember { mutableStateOf<LauncherApp?>(null) }
    var workspaceContextAnchor by remember { mutableStateOf(Offset.Zero) }
    var workspaceEditMode by remember { mutableStateOf(false) }
    var confirmDeleteWorkspacePage by remember { mutableStateOf(false) }

    val oldHome = remember {
        prefs.getString("home_apps_ordered", null)?.split("|")?.filter { it.isNotBlank() }.orEmpty()
    }
    var dockPackages by remember {
        mutableStateOf(
            prefs.getString("dock_apps_ordered", null)?.split("|")?.filter { it.isNotBlank() }
                ?: oldHome.take(4)
        )
    }
    var workspacePackages by remember {
        mutableStateOf(
            run {
                val pageKey = if (workspacePage == 0) "workspace_slots" else "workspace_slots_$workspacePage"
                val storedSlots = prefs.getString(pageKey, null)?.split("|")?.take(24).orEmpty()
                // Старые версии могли оставить ключ workspace_slots пустым, хотя список
                // workspace_apps_ordered ещё содержал приложения. Восстанавливаем их.
                val fallback = if (workspacePage == 0) {
                    prefs.getString("workspace_apps_ordered", null)?.split("|")?.filter { it.isNotBlank() }
                        ?.take(24) ?: oldHome.drop(4).take(24)
                } else emptyList()
                val saved = if (storedSlots.any { it.isNotBlank() }) storedSlots else fallback
                saved + List((24 - saved.size).coerceAtLeast(0)) { "" }
            }
        )
    }
    appCellOccupied[0] = { idx -> workspacePackages.getOrNull(idx).orEmpty().isNotBlank() }
    fun widgetOccupiesCell(index: Int, page: Int = workspacePage): Boolean {
        val row = index / 4
        val col = index % 4
        val cellLeft = col * gridCellPitch
        val cellTop = row * 98f
        val cellRight = cellLeft + gridCellPitch
        val cellBottom = cellTop + 98f
        return workspaceWidgets.any { widget ->
            if (widget.page != page ||
                !widget.x.isFinite() || !widget.y.isFinite() ||
                !widget.width.isFinite() || !widget.height.isFinite() ||
                widget.width <= 0f || widget.height <= 0f
            ) {
                false
            } else {
                // Сравниваем реальные границы виджета с ячейкой.
                // Не прижимаем координаты за пределами экрана к крайнему ряду/столбцу:
                // старая запись виджета за экраном не должна скрывать иконки.
                val widgetLeft = widget.x
                val widgetTop = widget.y
                val widgetRight = widget.x + widget.width + 4f
                val widgetBottom = widget.y + widget.height + 12f
                widgetLeft < cellRight && widgetRight > cellLeft &&
                    widgetTop < cellBottom && widgetBottom > cellTop
            }
        }
    }
    var selectedApp by remember { mutableStateOf<LauncherApp?>(null) }
    var openedFolder by remember { mutableStateOf<String?>(null) }
    // Отдельный флаг видимости нужен, чтобы карточка папки успевала плавно закрыться.
    var folderVisible by remember { mutableStateOf(false) }
    var folderOverlayAlive by remember { mutableStateOf(false) }
    var folderOrigin by remember { mutableStateOf(Offset.Unspecified) }
    var overlayCenter by remember { mutableStateOf(Offset.Zero) }
    val folderProgress = remember { Animatable(0f) }
    var folderMenuKey by remember { mutableStateOf<String?>(null) }
    var renamingFolder by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var memberMenu by remember { mutableStateOf<Pair<String, String>?>(null) }
    var addToFolderKey by remember { mutableStateOf<String?>(null) }
    var iconShapeChoice by remember {
        mutableStateOf(
            when (prefs.getString("icon_shape", "CIRCLE")) {
                "SQUARE" -> IconShapeChoice.SQUARE
                "VERY_SUNNY", "TRIANGLE" -> IconShapeChoice.VERY_SUNNY
                "COOKIE_9", "COOKIE_12" -> IconShapeChoice.COOKIE_9
                "SOFT_BURST" -> IconShapeChoice.SOFT_BURST
                "CLOVER", "CLOVER_4" -> IconShapeChoice.CLOVER
                else -> IconShapeChoice.CIRCLE
            }
        )
    }
    var folderOpacity by remember { mutableFloatStateOf(prefs.getFloat("folder_opacity", 0.88f).coerceIn(0.15f, 1f)) }
    var folderContents by remember {
        mutableStateOf(prefs.getStringSet("folder_contents", emptySet())?.mapNotNull { entry ->
            val split = entry.split(":", limit = 2)
            if (split.size == 2) split[0] to split[1].split(",").filter { it.isNotBlank() } else null
        }?.toMap().orEmpty())
    }
    var folderNames by remember {
        mutableStateOf(prefs.getStringSet("folder_names", emptySet())?.mapNotNull { entry ->
            val split = entry.split(":", limit = 2)
            if (split.size == 2 && split[1].isNotBlank()) split[0] to split[1] else null
        }?.toMap().orEmpty())
    }
    var wallpaperUri by remember { mutableStateOf(prefs.getString("wallpaper_uri", null)) }

    val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            wallpaperUri = uri.toString()
            prefs.edit().putString("wallpaper_uri", wallpaperUri).apply()
        }
    }
    // Не читаем и не декодируем изображения обоев в главном потоке:
    // на больших картинках это блокирует Compose и оставляет белый экран.
    var defaultWallpaper by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var customWallpaper by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(Unit) {
        defaultWallpaper = withContext(Dispatchers.IO) {
            runCatching {
                val encoded = context.resources.openRawResource(R.raw.prosto_wallpaper).bufferedReader().use { it.readText() }
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }

    LaunchedEffect(wallpaperUri) {
        customWallpaper = withContext(Dispatchers.IO) {
            wallpaperUri?.let { raw ->
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(raw)).use { stream ->
                        if (stream != null) BitmapFactory.decodeStream(stream)?.asImageBitmap() else null
                    }
                }.getOrNull()
            }
        }
    }

    val slotRects = remember { mutableStateMapOf<Int, Rect>() }
    val slotCoordinates = remember { mutableStateMapOf<Int, LayoutCoordinates>() }
    val blockedRects = remember { mutableStateMapOf<String, Rect>() }
    var rootInputOrigin by remember { mutableStateOf(Offset.Zero) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dropTargetIndex by remember { mutableStateOf<Int?>(null) }
    var dragPoint by remember { mutableStateOf(Offset.Zero) }

    fun saveDock(updated: List<String>) {
        dockPackages = updated.distinct().take(4)
        prefs.edit().putString("dock_apps_ordered", dockPackages.joinToString("|")).apply()
    }
    fun saveWorkspace(updated: List<String>) {
        workspacePackages = updated.take(24).let { slots ->
            slots + List((24 - slots.size).coerceAtLeast(0)) { "" }
        }
        prefs.edit()
            .putString(if (workspacePage == 0) "workspace_slots" else "workspace_slots_$workspacePage",
                workspacePackages.joinToString("|"))
            .putString("workspace_apps_ordered", workspacePackages.filter { it.isNotBlank() }.joinToString("|"))
            .putInt("workspace_current_page", workspacePage)
            .putInt("workspace_page_count", workspacePageCount)
            .apply()
    }
    fun switchWorkspacePage(page: Int) {
        val target = page.coerceIn(0, workspacePageCount - 1)
        if (target == workspacePage) return
        workspacePage = target
        val storedSlots = prefs.getString(if (target == 0) "workspace_slots" else "workspace_slots_$target", null)
            ?.split("|").orEmpty().take(24)
        val fallback = if (target == 0) {
            prefs.getString("workspace_apps_ordered", null)?.split("|")?.filter { it.isNotBlank() }
                ?.take(24) ?: oldHome.drop(4).take(24)
        } else emptyList()
        val saved = if (storedSlots.any { it.isNotBlank() }) storedSlots else fallback
        workspacePackages = saved + List((24 - saved.size).coerceAtLeast(0)) { "" }
        prefs.edit().putInt("workspace_current_page", target).apply()
    }
    fun addWorkspacePage() {
        workspacePageCount += 1
        prefs.edit().putInt("workspace_page_count", workspacePageCount).apply()
        switchWorkspacePage(workspacePageCount - 1)
    }
    fun deleteCurrentWorkspacePage() {
        if (workspacePageCount <= 1 || workspacePage == 0) return
        val removedPage = workspacePage
        saveWorkspaceWidgets(workspaceWidgets.filterNot { it.page == removedPage })
        prefs.edit().remove("workspace_slots_$removedPage").apply()
        val shiftedWidgets = workspaceWidgets.filterNot { it.page == removedPage }.map {
            if (it.page > removedPage) it.copy(page = it.page - 1) else it
        }
        saveWorkspaceWidgets(shiftedWidgets)
        for (page in removedPage + 1 until workspacePageCount) {
            val slots = prefs.getString("workspace_slots_$page", null)
            if (slots != null) prefs.edit().putString("workspace_slots_${page - 1}", slots).apply()
            prefs.edit().remove("workspace_slots_$page").apply()
        }
        workspacePageCount = (workspacePageCount - 1).coerceAtLeast(1)
        prefs.edit().putInt("workspace_page_count", workspacePageCount).apply()
        workspaceEditMode = false
        workspacePage = (removedPage - 1).coerceAtLeast(0)
        val saved = prefs.getString(if (workspacePage == 0) "workspace_slots" else "workspace_slots_$workspacePage", null)
            ?.split("|").orEmpty().take(24)
        workspacePackages = saved + List((24 - saved.size).coerceAtLeast(0)) { "" }
        prefs.edit().putInt("workspace_current_page", workspacePage).apply()
    }
    fun addToWorkspace(pkg: String) {
        if (pkg in workspacePackages) {
            Toast.makeText(context, "Уже на рабочем столе", Toast.LENGTH_SHORT).show()
            return
        }
        val slots = workspacePackages.toMutableList()
        val emptyIndex = slots.indices.firstOrNull { slots[it].isBlank() && !widgetOccupiesCell(it) } ?: -1
        if (emptyIndex < 0) {
            Toast.makeText(context, "Рабочий стол заполнен", Toast.LENGTH_SHORT).show()
            return
        }
        saveDock(dockPackages.filterNot { it == pkg })
        slots[emptyIndex] = pkg
        saveWorkspace(slots)
    }
    fun addToDock(pkg: String) {
        if (pkg in dockPackages) {
            Toast.makeText(context, "Уже в нижней панели", Toast.LENGTH_SHORT).show()
            return
        }
        if (dockPackages.size >= 4) {
            Toast.makeText(context, "Нижняя панель заполнена", Toast.LENGTH_SHORT).show()
            return
        }
        saveWorkspace(workspacePackages.filterNot { it == pkg })
        saveDock(dockPackages + pkg)
    }
    fun removeFromLauncher(pkg: String) {
        saveWorkspace(workspacePackages.map { if (it == pkg) "" else it })
        saveDock(dockPackages.filterNot { it == pkg })
    }
    fun persistFolders(updated: Map<String, List<String>>) {
        folderContents = updated
        prefs.edit().putStringSet("folder_contents", updated.map { (key, values) ->
            "$key:${values.joinToString(",")}"
        }.toSet()).apply()
    }
    fun persistFolderNames(updated: Map<String, String>) {
        folderNames = updated
        prefs.edit().putStringSet("folder_names", updated.map { (key, value) -> "$key:$value" }.toSet()).apply()
    }
    fun renameFolder(key: String, newName: String) {
        val clean = newName.trim().take(24)
        persistFolderNames(if (clean.isBlank()) folderNames - key else folderNames + (key to clean))
    }
    fun moveApp(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in workspacePackages.indices || toIndex !in workspacePackages.indices ||
            fromIndex == toIndex || widgetOccupiesCell(toIndex)) return
        val changed = workspacePackages.toMutableList()
        val moved = changed[fromIndex]
        val target = changed[toIndex]
        if (moved.isBlank()) return
        if (target.isNotBlank() && moved != target && !moved.startsWith("@folder@") && !target.startsWith("@folder@")) {
            val folderKey = "@folder@${System.currentTimeMillis()}"
            persistFolders(folderContents + (folderKey to listOf(moved, target)))
            changed[fromIndex] = ""
            changed[toIndex] = folderKey
        } else if (target.startsWith("@folder@") && !moved.startsWith("@folder@")) {
            val contents = folderContents[target].orEmpty()
            if (moved !in contents) persistFolders(folderContents + (target to (contents + moved)))
            changed[fromIndex] = ""
        } else {
            changed[fromIndex] = target
            changed[toIndex] = moved
        }
        saveWorkspace(changed)
    }

    // Получение списка приложений и загрузка их значков выполняются в фоне:
    // на устройствах с большим числом приложений PackageManager может надолго
    // блокировать главный поток и оставлять лаунчер на пустом экране.
    val appLoadScope = rememberCoroutineScope()
    val uninstallLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        appLoadScope.launch {
            val loadedApps = withContext(Dispatchers.IO) { queryLauncherApps(context) }
            apps = loadedApps
        }
    }
    LaunchedEffect(Unit) {
        val loadedApps = withContext(Dispatchers.IO) { queryLauncherApps(context) }
        apps = loadedApps
    }

    val appMap = remember(apps) { apps.associateBy { it.packageName } }
    val dockApps = dockPackages.mapNotNull { appMap[it] }
    val filteredApps = apps.filter { it.label.contains(search, ignoreCase = true) }
        .sortedBy { it.label.lowercase() }
    val drawerGridState = rememberLazyGridState()
    val drawerScope = rememberCoroutineScope()

    // ---- Шторка приложений: выезжает снизу и следует за пальцем ----
    // sheetOffset: 0 = шторка полностью открыта, drawerClosedOffset = убрана за нижний край экрана.
    val drawerClosedOffset = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    var drawerShown by remember { mutableStateOf(false) }
    var sheetOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(drawerOpen) {
        if (drawerOpen) {
            if (!drawerShown) {
                sheetOffset = drawerClosedOffset
                drawerShown = true
            }
            animate(sheetOffset, 0f, animationSpec = tween(380, easing = FastOutSlowInEasing)) { value, _ -> sheetOffset = value }
        } else if (drawerShown) {
            animate(sheetOffset, drawerClosedOffset, animationSpec = tween(320, easing = FastOutSlowInEasing)) { value, _ -> sheetOffset = value }
            drawerShown = false
        }
    }
    // Палец отпущен: закрыть шторку или вернуть её на место.
    fun settleDrawer(velocityY: Float) {
        val shouldClose = velocityY > 1400f || (sheetOffset > drawerClosedOffset * 0.28f && velocityY > -1400f)
        if (shouldClose) {
            drawerOpen = false
        } else {
            drawerScope.launch {
                animate(sheetOffset, 0f, animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f)) { value, _ -> sheetOffset = value }
            }
        }
    }
    // Если список приложений уже долистан до верха, то потянуть вниз можно в любом месте списка:
    // шторка поедет за пальцем и закроется.
    val drawerNestedScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && sheetOffset > 0f && source == NestedScrollSource.UserInput) {
                    val used = maxOf(available.y, -sheetOffset)
                    sheetOffset += used
                    return Offset(0f, used)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && source == NestedScrollSource.UserInput) {
                    sheetOffset += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (sheetOffset > 0f) {
                    settleDrawer(available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }
    var alphabetHeight by remember { mutableIntStateOf(0) }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    var bubbleLetter by remember { mutableStateOf("") }
    val alphabet = remember(filteredApps) { filteredApps.map { it.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#" }.distinct().sorted() }
    val alphabetTargets = remember(filteredApps) {
        filteredApps.mapIndexed { index, app -> (app.label.firstOrNull()?.uppercaseChar()?.toString() ?: "#") to index }
            .distinctBy { it.first }.toMap()
    }

    fun folderTitle(key: String): String = folderNames[key] ?: "Папка"

    // Достаёт приложение из папки. Если в папке остаётся меньше двух приложений, папка исчезает,
    // а оставшееся приложение встаёт на её место. Возвращает true, если папка исчезла.
    fun takeOutOfFolder(folderKey: String, pkg: String, placeOnDesktop: Boolean): Boolean {
        val members = folderContents[folderKey].orEmpty()
        val rest = members - pkg
        val slots = workspacePackages.toMutableList()
        val dissolve = rest.size < 2
        if (dissolve) {
            persistFolders(folderContents - folderKey)
            persistFolderNames(folderNames - folderKey)
            val remaining = rest.firstOrNull().orEmpty()
            val slot = slots.indexOf(folderKey)
            if (slot >= 0) {
                slots[slot] = remaining
            } else if (remaining.isNotBlank()) {
                val empty = slots.indices.firstOrNull { slots[it].isBlank() && !widgetOccupiesCell(it) } ?: -1
                if (empty >= 0) slots[empty] = remaining
            }
        } else {
            persistFolders(folderContents + (folderKey to rest))
        }
        if (placeOnDesktop) {
            val empty = slots.indices.firstOrNull { slots[it].isBlank() && !widgetOccupiesCell(it) } ?: -1
            if (empty >= 0) {
                slots[empty] = pkg
            } else {
                Toast.makeText(context, "На рабочем столе нет места — приложение осталось в списке всех приложений", Toast.LENGTH_LONG).show()
            }
        }
        saveWorkspace(slots)
        return dissolve
    }

    fun addAppToFolder(folderKey: String, pkg: String) {
        val members = folderContents[folderKey].orEmpty()
        if (pkg in members) return
        saveWorkspace(workspacePackages.map { if (it == pkg) "" else it })
        saveDock(dockPackages.filterNot { it == pkg })
        persistFolders(folderContents + (folderKey to (members + pkg)))
    }

    fun moveAppBetweenFolders(fromKey: String, pkg: String, toKey: String) {
        if (fromKey == toKey) return
        takeOutOfFolder(fromKey, pkg, placeOnDesktop = false)
        persistFolders(folderContents + (toKey to (folderContents[toKey].orEmpty() + pkg)))
    }

    // Удаляет папку целиком. Приложения из неё возвращаются на рабочий стол (если есть место).
    fun deleteFolder(key: String) {
        val members = folderContents[key].orEmpty().filter { it in appMap }
        val slots = workspacePackages.toMutableList()
        val slot = slots.indexOf(key)
        if (slot >= 0) slots[slot] = ""
        var notPlaced = 0
        members.forEach { member ->
            val empty = slots.indices.firstOrNull { slots[it].isBlank() && !widgetOccupiesCell(it) } ?: -1
            if (empty >= 0) slots[empty] = member else notPlaced++
        }
        saveWorkspace(slots)
        persistFolders(folderContents - key)
        persistFolderNames(folderNames - key)
        if (notPlaced > 0) {
            Toast.makeText(context, "Не все приложения поместились на рабочий стол — остальные есть в списке всех приложений", Toast.LENGTH_LONG).show()
        }
    }

    // Запуск приложения с анимацией «из иконки» (как на iPhone): система увеличивает окно приложения из точки, где лежала иконка.
    val rootView = LocalView.current
    fun launch(pkg: String, bounds: Rect?) {
        launchApp(context, pkg, rootView, bounds?.let { iconRectFor(it, rootView.resources.displayMetrics.density) })
    }

    // Открывает папку «из иконки»: запоминаем, где иконка папки лежит на экране.
    fun openFolder(key: String) {
        val slot = workspacePackages.indexOf(key)
        folderOrigin = slotRects[slot]?.center ?: Offset.Unspecified
        openedFolder = key
        folderVisible = true
    }

    // Анимация как на iPhone: карточка пружинисто вырастает из иконки, закрытие — плавно сворачивается обратно.
    LaunchedEffect(folderVisible) {
        if (folderVisible) {
            folderOverlayAlive = true
            folderProgress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 300f))
        } else {
            folderProgress.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            folderOverlayAlive = false
        }
    }

    BackHandler(enabled = folderVisible || drawerOpen) {
        if (folderVisible) folderVisible = false else drawerOpen = false
    }


    @Composable
    fun MainWorkspaceLayer() {
        Box(Modifier.fillMaxSize()) {
        val activeWallpaper = customWallpaper ?: defaultWallpaper
        if (activeWallpaper != null) {
            Image(
                bitmap = activeWallpaper,
                contentDescription = "Обои Prosto Launcher",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else Box(Modifier.fillMaxSize().background(DarkInk))
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (dark) 0.12f else 0.03f)))

        // Рабочий стол теперь лежит в обычном Box на весь экран — так же, как нижняя панель,
        // которая рисуется нормально. Раньше здесь был Column с weight-блоком внутри.
        Box(
            Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 24.dp)
                .pointerInput(workspacePage, workspacePageCount, draggingIndex, draggingWidget) {
                    var horizontalTravel = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            horizontalTravel += amount
                        },
                        onDragEnd = {
                            if (draggingIndex == null && !draggingWidget && horizontalTravel < -70f) {
                                if (workspacePage < workspacePageCount - 1) switchWorkspacePage(workspacePage + 1)
                            } else if (draggingIndex == null && !draggingWidget && horizontalTravel > 70f && workspacePage > 0) {
                                switchWorkspacePage(workspacePage - 1)
                            }
                            horizontalTravel = 0f
                        },
                        onDragCancel = { horizontalTravel = 0f }
                    )
                }
        ) {
            // Не скрываем ячейки рабочего стола из-за сохранённых координат виджетов.
            // Виджеты рисуются отдельным слоем поверх сетки; даже повреждённая запись виджета
            // не должна делать приложения невидимыми или удалять их из сохранённых слотов.
            Box(Modifier.fillMaxSize()) {
            // Обычная (не ленивая) сетка 4x6: все 24 ячейки всегда составляются и рисуются,
            // поэтому добавленные приложения не могут «пропасть» из-за LazyVerticalGrid.
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              for (gridRow in 0 until 6) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  for (gridCol in 0 until 4) {
                    val index = gridRow * 4 + gridCol
                    val pkg = workspacePackages.getOrNull(index).orEmpty()
                    val app = appMap[pkg]
                    val isFolder = pkg.startsWith("@folder@") && pkg in folderContents
                    Box(
                        Modifier.weight(1f)
                    ) {
                    Box(
                        Modifier.fillMaxWidth().height(86.dp)
                            .graphicsLayer {
                                val shrinking = if (workspaceEditMode) 0.82f else 1f
                                scaleX = shrinking
                                scaleY = shrinking
                                if (workspaceEditMode) {
                                    val density = this.density
                                    val cellCenterX = ((index % 4) + 0.5f) * gridCellPitch * density
                                    val cellCenterY = ((index / 4) + 0.5f) * 98f * density
                                    translationX = (configuration.screenWidthDp * density / 2f - cellCenterX) * 0.10f
                                    translationY = (configuration.screenHeightDp * density / 2f - cellCenterY) * 0.08f
                                } else {
                                    translationX = 0f
                                    translationY = 0f
                                }
                                alpha = if (workspaceEditMode) 0.92f else 1f
                            }
                            .onGloballyPositioned { coordinates ->
                                val rect = coordinates.boundsInRoot()
                                slotRects[index] = rect
                                slotCoordinates[index] = coordinates
                                blockedRects["workspace-$index"] = rect
                            }
                            .pointerInput(index, workspacePackages) {
                                var pressStart = Offset.Zero
                                var movedFar = false
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { start ->
                                        pressStart = start
                                        movedFar = false
                                        if (app != null || isFolder) {
                                            // Show the context menu immediately; keep the drag gesture active.
                                            if (app != null) {
                                                workspaceContextApp = app
                                                workspaceContextAnchor = slotRects[index]?.let { Offset(it.left, it.bottom + 6f) } ?: Offset.Zero
                                            }
                                            draggingIndex = index
                                            dropTargetIndex = null
                                            val rect = slotRects[index]
                                            dragPoint = slotCoordinates[index]?.localToRoot(start)
                                                ?: rect?.let { Offset(it.left + start.x, it.top + start.y) }
                                                ?: Offset.Zero
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        if (draggingIndex != null) {
                                            change.consume()
                                            if ((change.position - pressStart).getDistance() > 24f) {
                                                movedFar = true
                                                if (app != null) workspaceContextApp = null
                                            }
                                            val rect = slotRects[index]
                                            dragPoint = slotCoordinates[index]?.localToRoot(change.position)
                                                ?: rect?.let { Offset(it.left + change.position.x, it.top + change.position.y) }
                                                ?: dragPoint
                                            dropTargetIndex = slotRects.entries
                                                .filter { (_, bounds) -> bounds.contains(dragPoint) }
                                                .minByOrNull { (_, bounds) ->
                                                    val dx = bounds.center.x - dragPoint.x
                                                    val dy = bounds.center.y - dragPoint.y
                                                    dx * dx + dy * dy
                                                }?.key
                                        }
                                    },
                                    onDragEnd = {
                                        val from = draggingIndex
                                        val target = dropTargetIndex
                                        val density = context.resources.displayMetrics.density
                                        val edgeLimit = 24f * density
                                        val screenWidthPx = configuration.screenWidthDp * density
                                        if (from != null && movedFar &&
                                            (dragPoint.x > screenWidthPx - edgeLimit || (dragPoint.x < edgeLimit && workspacePage > 0))) {
                                            val movedPackage = workspacePackages.getOrNull(from).orEmpty()
                                            if (movedPackage.isNotBlank()) {
                                                saveWorkspace(workspacePackages.mapIndexed { idx, value -> if (idx == from) "" else value })
                                                if (dragPoint.x > screenWidthPx - edgeLimit) {
                                                    if (workspacePage == workspacePageCount - 1) addWorkspacePage()
                                                    else switchWorkspacePage(workspacePage + 1)
                                                } else if (workspacePage > 0) switchWorkspacePage(workspacePage - 1)
                                                val destination = workspacePackages.indices.firstOrNull {
                                                    workspacePackages[it].isBlank() && !widgetOccupiesCell(it)
                                                } ?: -1
                                                if (destination >= 0) {
                                                    val changed = workspacePackages.toMutableList()
                                                    changed[destination] = movedPackage
                                                    saveWorkspace(changed)
                                                }
                                            }
                                        } else if (from != null && target != null) moveApp(from, target)
                                        if (from != null && !movedFar && isFolder) folderMenuKey = pkg
                                        draggingIndex = null
                                        dropTargetIndex = null
                                    },
                                    onDragCancel = {
                                        val from = draggingIndex
                                        if (from != null && !movedFar && isFolder) folderMenuKey = pkg
                                        draggingIndex = null
                                        dropTargetIndex = null
                                    }
                                )
                            }
                            .clickable(enabled = (app != null || isFolder) && draggingIndex == null) {
                                if (isFolder) openFolder(pkg) else app?.let { launch(it.packageName, slotRects[index]) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFolder) {
                            Column(
                                Modifier.fillMaxWidth().graphicsLayer {
                                    alpha = if (draggingIndex == index) 0.45f else 1f
                                },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    Modifier.size(54.dp).clip(RoundedCornerShape(15.dp))
                                        .background(Purple.copy(alpha = folderOpacity))
                                        .border(1.dp, Lavender.copy(alpha = 0.75f), RoundedCornerShape(15.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        val members = folderContents[pkg].orEmpty().take(4).mapNotNull { appMap[it] }
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            members.take(2).forEach { AppImage(it, 20, iconShapeChoice) }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            members.drop(2).take(2).forEach { AppImage(it, 20, iconShapeChoice) }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    folderNames[pkg] ?: "Папка", fontSize = 11.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, color = Color.White
                                )
                            }
                        } else if (app != null) {
                            Column(
                                Modifier.fillMaxWidth().graphicsLayer {
                                    alpha = if (draggingIndex == index) 0.45f else 1f
                                    scaleX = if (draggingIndex == index) 1.06f else 1f
                                    scaleY = if (draggingIndex == index) 1.06f else 1f
                                },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AppImage(app, 54, iconShapeChoice)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    app.label, fontSize = 11.sp, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, color = Color.White
                                )
                            }
                        } else if (pkg.isNotBlank() && !pkg.startsWith("@folder@")) {
                            // В слоте лежит приложение, которого нет в списке установленных (удалено/скрыто).
                            Text(pkg.substringAfterLast('.'), fontSize = 10.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, color = Color.White.copy(alpha = 0.6f))
                        }
                        if (dropTargetIndex == index) {
                            Box(
                                Modifier.matchParentSize().padding(2.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Lavender.copy(alpha = 0.28f))
                                    .border(2.dp, Lavender, RoundedCornerShape(14.dp))
                            )
                        }
                    }
                    }
                  }
                }
              }
            }

                Box(Modifier.fillMaxSize()) {
                    workspaceWidgets.filter { it.page == workspacePage }.forEach { widget ->
                        val latestWorkspaceWidgets = rememberUpdatedState(workspaceWidgets)
                        val widgetProvider = if (widget.type == "android") appWidgetManager.getAppWidgetInfo(widget.id) else null
                        val widgetModifier = Modifier
                            .offset(x = widget.x.dp, y = (widget.y + 10f).dp)
                            .width(widget.width.dp).height(widget.height.dp)
                        val resizeBy: (String, Float, Float) -> Unit = { edge, dx, dy ->
                            val current = latestWorkspaceWidgets.value.firstOrNull { it.id == widget.id }
                            if (current != null) {
                                val mode = widgetProvider?.resizeMode ?: AppWidgetProviderInfo.RESIZE_BOTH
                                val canW = widget.type != "android" || (mode and AppWidgetProviderInfo.RESIZE_HORIZONTAL) != 0
                                val canH = widget.type != "android" || (mode and AppWidgetProviderInfo.RESIZE_VERTICAL) != 0
                                val minW = maxOf(gridCellPitch - 4f,
                                    ((widgetProvider?.minResizeWidth?.takeIf { it > 0 } ?: widgetProvider?.minWidth)
                                        ?.let { it / screenDensity } ?: (gridCellPitch - 4f)))
                                val minH = maxOf(86f,
                                    ((widgetProvider?.minResizeHeight?.takeIf { it > 0 } ?: widgetProvider?.minHeight)
                                        ?.let { it / screenDensity } ?: 86f))
                                var x = current.x
                                var y = current.y
                                var w = current.width
                                var h = current.height
                                when (edge) {
                                    "left" -> if (canW) {
                                        val wanted = (w - dx).coerceAtLeast(minW)
                                        val snapped = (((kotlin.math.round((wanted + 4f) / gridCellPitch).coerceIn(1f, 4f) * gridCellPitch) - 4f).coerceAtLeast(minW))
                                        x = (current.x + current.width - snapped).coerceAtLeast(0f)
                                        w = current.x + current.width - x
                                    }
                                    "right" -> if (canW) {
                                        val wanted = (w + dx).coerceAtLeast(minW)
                                        w = (((kotlin.math.round((wanted + 4f) / gridCellPitch).coerceIn(1f, 4f) * gridCellPitch) - 4f).coerceAtLeast(minW))
                                        w = w.coerceAtMost(gridCellPitch * 4f - 4f - x)
                                    }
                                    "top" -> if (canH) {
                                        val wanted = (h - dy).coerceAtLeast(minH)
                                        val snapped = ((kotlin.math.round((wanted + 12f) / 98f).coerceIn(1f, 6f) * 98f) - 12f).coerceAtLeast(minH)
                                        y = (current.y + current.height - snapped).coerceAtLeast(0f)
                                        h = current.y + current.height - y
                                    }
                                    "bottom" -> if (canH) {
                                        val wanted = (h + dy).coerceAtLeast(minH)
                                        h = (((kotlin.math.round((wanted + 12f) / 98f).coerceIn(1f, 6f) * 98f) - 12f).coerceAtLeast(minH))
                                        h = h.coerceAtMost(586f - y)
                                    }
                                }
                                val candidate = current.copy(x = x, y = y, width = w, height = h)
                                val others = latestWorkspaceWidgets.value.filterNot { it.id == widget.id }
                                if (others.none { widgetsOverlap(candidate, it) }) {
                                    saveWorkspaceWidgets(latestWorkspaceWidgets.value.map { if (it.id == widget.id) candidate else it })
                                }
                            }
                        }
                        Box(
                            modifier = widgetModifier.pointerInput(widget.id, workspacePackages) {
                                var originX = 0f
                                var originY = 0f
                                var totalX = 0f
                                var totalY = 0f
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        latestWorkspaceWidgets.value.firstOrNull { it.id == widget.id }?.let {
                                            originX = it.x; originY = it.y
                                        }
                                        totalX = 0f; totalY = 0f
                                        draggingWidget = true
                                    },
                                    onDragEnd = { draggingWidget = false },
                                    onDragCancel = { draggingWidget = false },
                                    onDrag = { change, drag ->
                                        change.consume()
                                        val density = this.density
                                        totalX += drag.x / density
                                        totalY += drag.y / density
                                        val current = latestWorkspaceWidgets.value.firstOrNull { it.id == widget.id }
                                            ?: return@detectDragGesturesAfterLongPress
                                        val snappedX = (kotlin.math.round((originX + totalX) / gridCellPitch) * gridCellPitch)
                                            .coerceIn(0f, (gridCellPitch * 4f - 4f - current.width).coerceAtLeast(0f))
                                        val snappedY = (kotlin.math.round((originY + totalY) / 98f) * 98f)
                                            .coerceIn(0f, (586f - current.height).coerceAtLeast(0f))
                                        val candidate = current.copy(x = snappedX, y = snappedY)
                                        val others = latestWorkspaceWidgets.value.filterNot { it.id == widget.id }
                                        val coversApp = workspacePackages.indices.any { index ->
                                            val pkg = workspacePackages.getOrNull(index).orEmpty()
                                            if (pkg.isBlank()) false else {
                                                val cellX = (index % 4) * gridCellPitch
                                                val cellY = (index / 4) * 98f
                                                candidate.x < cellX + gridCellPitch &&
                                                    candidate.x + candidate.width > cellX &&
                                                    candidate.y < cellY + 98f &&
                                                    candidate.y + candidate.height > cellY
                                            }
                                        }
                                        if (others.none { widgetsOverlap(candidate, it) } && !coversApp) {
                                            saveWorkspaceWidgets(latestWorkspaceWidgets.value.map {
                                                if (it.id == widget.id) candidate else it
                                            })
                                        }
                                    }
                                )
                            }.graphicsLayer {
                                if (workspaceEditMode) {
                                    scaleX = 0.94f
                                    scaleY = 0.94f
                                    val density = this.density
                                    val centerX = (widget.x + widget.width / 2f) * density
                                    val centerY = (widget.y + widget.height / 2f + 10f) * density
                                    translationX = (configuration.screenWidthDp * density / 2f - centerX) * 0.08f
                                    translationY = (configuration.screenHeightDp * density / 2f - centerY) * 0.06f
                                } else {
                                    scaleX = 1f
                                    scaleY = 1f
                                    translationX = 0f
                                    translationY = 0f
                                }
                            }
                        ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.Transparent,
                            border = if (widget.type == "android" && resizingWidgetId == widget.id) BorderStroke(2.dp, Lavender) else null
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                Row(
                                    Modifier.fillMaxWidth().height(26.dp).background(Color.Transparent),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(Modifier.weight(1f))
                                    if (resizingWidgetId == widget.id) TextButton(onClick = { resizingWidgetId = null }) {
                                        Text(tr("Готово", appLanguage), color = Color.White, fontSize = 10.sp)
                                    }

                                }
                                Box(Modifier.weight(1f).fillMaxWidth().padding(5.dp), contentAlignment = Alignment.Center) {
                                    when (widget.type) {
                                        "digital" -> DigitalClockWidget(appLanguage, Modifier.fillMaxSize())
                                        "weather" -> WeatherWidget(weatherCityName, weatherInfo, weatherLoading, weatherError, appLanguage, Modifier.fillMaxSize())
                                        "analog" -> AnalogClockWidget(Modifier.fillMaxSize())
                                        "date" -> DateWidget(appLanguage, Modifier.fillMaxSize())
                                        "android" -> {
                                            val provider = widgetProvider
                                            if (provider != null) AndroidView(
                                                factory = { ctx ->
                                                    runCatching {
                                                        appWidgetHost.createView(ctx, widget.id, provider).apply {
                                                            setAppWidget(widget.id, provider)
                                                        }
                                                    }.getOrElse {
                                                        TextView(ctx).apply {
                                                            text = "Виджет временно недоступен"
                                                            setTextColor(android.graphics.Color.WHITE)
                                                            gravity = android.view.Gravity.CENTER
                                                        }
                                                    }
                                                },
                                                update = { /* View is initialized once in factory; avoid re-binding provider on every recomposition. */ },
                                                modifier = Modifier.fillMaxSize()
                                            ) else Text(tr("Виджет недоступен", appLanguage), color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        // Widget controls float above the widget itself.
                        Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp)
                            .clip(CircleShape).background(Color(0xAA171326))
                            .clickable { selectedWorkspaceWidget = widget.id }, contentAlignment = Alignment.Center) {
                            Text(tr("⋮", appLanguage), color = Color.White, fontSize = 18.sp)
                        }
                        if (resizingWidgetId == widget.id) {
                            WidgetResizeKnob(Modifier.align(Alignment.CenterStart), "left", gridCellPitch, resizeBy)
                            WidgetResizeKnob(Modifier.align(Alignment.CenterEnd), "right", gridCellPitch, resizeBy)
                            WidgetResizeKnob(Modifier.align(Alignment.TopCenter), "top", gridCellPitch, resizeBy)
                            WidgetResizeKnob(Modifier.align(Alignment.BottomCenter), "bottom", gridCellPitch, resizeBy)
                        }
                    }
                }
            

            }


        }

        // Нижняя панель и точки страниц размещены в одном полноэкранном слое
        // поверх рабочего стола и привязаны к нижнему краю экрана.
        Box(Modifier.fillMaxSize()) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 18.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DockSlot(dockApps.getOrNull(0), "dock-left-1", blockedRects, iconShapeChoice, onLongClick = { a -> workspaceContextApp = a; workspaceContextAnchor = blockedRects["dock-left-1"]?.let { Offset(it.left, it.top - 4f) } ?: Offset.Zero }) { a, r -> launch(a.packageName, r) }
                DockSlot(dockApps.getOrNull(1), "dock-left-2", blockedRects, iconShapeChoice, onLongClick = { a -> workspaceContextApp = a; workspaceContextAnchor = blockedRects["dock-left-2"]?.let { Offset(it.left, it.top - 4f) } ?: Offset.Zero }) { a, r -> launch(a.packageName, r) }
                Surface(
                    modifier = Modifier.size(54.dp)
                        .onGloballyPositioned { blockedRects["drawer-button"] = it.boundsInRoot() }
                        .combinedClickable(
                            onClick = { drawerOpen = true },
                            onLongClick = {
                                if (!drawerOpen && !folderVisible) {
                                    drawerOpen = false
                                    quickMenuOpen = false
                                    workspaceEditMode = true
                                }
                            }
                        ),
                    shape = iconShape(IconShapeChoice.SOFT_BURST), color = Color(0xFFEDE5FF).copy(alpha = 0.92f), shadowElevation = 5.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Все приложения", tint = DarkInk, modifier = Modifier.size(34.dp))
                    }
                }
                DockSlot(dockApps.getOrNull(2), "dock-right-1", blockedRects, iconShapeChoice, onLongClick = { a -> workspaceContextApp = a; workspaceContextAnchor = blockedRects["dock-right-1"]?.let { Offset(it.left, it.top - 4f) } ?: Offset.Zero }) { a, r -> launch(a.packageName, r) }
                DockSlot(dockApps.getOrNull(3), "dock-right-2", blockedRects, iconShapeChoice, onLongClick = { a -> workspaceContextApp = a; workspaceContextAnchor = blockedRects["dock-right-2"]?.let { Offset(it.left, it.top - 4f) } ?: Offset.Zero }) { a, r -> launch(a.packageName, r) }
            }
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 88.dp, top = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(workspacePageCount) { page ->
                    Box(
                        Modifier.padding(horizontal = 5.dp).size(if (page == workspacePage) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (page == workspacePage) Lavender else Lavender.copy(alpha = 0.45f))
                            .clickable { switchWorkspacePage(page) }
                    )
                }
            }
        }
        }

    }

    Box(
        Modifier.fillMaxSize()
            .onGloballyPositioned { rootInputOrigin = it.positionInRoot() }
            .pointerInput(drawerOpen, settingsOpen, widgetsOpen, quickMenuOpen, folderVisible, blockedRects.size, workspacePackages, dockPackages, rootInputOrigin) {
            detectTapGestures(
                onTap = { point ->
                    val blocked = blockedRects.any { (key, rect) ->
                        val occupiedWorkspace = key.startsWith("workspace-") &&
                            workspacePackages.getOrNull(key.substringAfter("-").toIntOrNull() ?: -1).orEmpty().isNotBlank()
                        val occupiedDock = key.startsWith("dock-") &&
                            dockPackages.getOrNull(key.substringAfterLast("-").toIntOrNull()?.minus(1) ?: -1) != null
                        (occupiedWorkspace || occupiedDock || key == "drawer-button") && rect.contains(point)
                    }
                    if (!blocked) {
                        workspaceEditMode = false
                        quickMenuOpen = false
                        selectedWorkspaceWidget = null
                        resizingWidgetId = null
                    }
                }
            )
        }
    ) {
        MainWorkspaceLayer()
        if (drawerShown) {
            // Затемнение рабочего стола: чем выше шторка, тем темнее. Нажатие по нему закрывает шторку.
            // sheetOffset читается только при рисовании, поэтому анимация не перекомпонует весь экран.
            Box(
                Modifier.fillMaxSize()
                    .drawBehind {
                        val progress = (1f - sheetOffset / drawerClosedOffset).coerceIn(0f, 1f)
                        drawRect(Color.Black.copy(alpha = 0.45f * progress))
                    }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { drawerOpen = false }
            )
            Column(
                Modifier.fillMaxSize()
                    .offset { IntOffset(0, sheetOffset.roundToInt().coerceAtLeast(0)) }
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color(0xFF171326).copy(alpha = 0.97f))
                    // Шторка забирает нажатия, чтобы они не доходили до рабочего стола под ней.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .nestedScroll(drawerNestedScroll)
                    .padding(horizontal = 18.dp)
            ) {
                // Шапка: за неё (и за ручку) тоже можно тянуть шторку вниз.
                Column(
                    Modifier.fillMaxWidth().draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> sheetOffset = (sheetOffset + delta).coerceAtLeast(0f) },
                        onDragStopped = { velocity -> settleDrawer(velocity) }
                    )
                ) {
                    Box(
                        Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 6.dp)
                            .width(36.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f))
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Apps", color = Color.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                        Surface(color = Lavender.copy(alpha = 0.18f), shape = RoundedCornerShape(18.dp)) {
                            Text("${filteredApps.size}", color = Lavender, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)) {
                Row(Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        state = drawerGridState,
                        columns = GridCells.Adaptive(minSize = 76.dp),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val cellBounds = remember { arrayOfNulls<Rect>(1) }
                            Column(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .onGloballyPositioned { cellBounds[0] = it.boundsInRoot() }
                                    .combinedClickable(
                                        onClick = { launch(app.packageName, cellBounds[0]) },
                                        onLongClick = { selectedApp = app }
                                    ).padding(vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AppImage(app, 52, iconShapeChoice)
                                Spacer(Modifier.height(4.dp))
                                Text(app.label, color = Color.White, fontSize = 11.sp, maxLines = 2,
                                    textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (alphabet.isNotEmpty()) {
                        // Алфавит на всю высоту списка: все буквы видны сразу, без прокрутки.
                        // Можно нажать на букву или провести пальцем по столбцу.
                        fun letterAt(y: Float): String? {
                            if (alphabetHeight <= 0 || alphabet.isEmpty()) return null
                            val index = (y / alphabetHeight * alphabet.size).toInt().coerceIn(0, alphabet.size - 1)
                            return alphabet[index]
                        }
                        fun jumpToLetter(letter: String) {
                            activeLetter = letter
                            bubbleLetter = letter
                            val target = alphabetTargets[letter] ?: 0
                            drawerScope.launch { drawerGridState.scrollToItem(target) }
                        }
                        val slotDp = with(LocalDensity.current) { alphabetHeight.toDp() } / alphabet.size
                        val letterSize = (slotDp.value * 0.78f).coerceIn(7f, 12f)
                        Column(
                            modifier = Modifier.width(26.dp).fillMaxHeight()
                                .onSizeChanged { alphabetHeight = it.height }
                                .pointerInput(alphabet, alphabetHeight) {
                                    detectTapGestures(
                                        onPress = { offset ->
                                            letterAt(offset.y)?.let { jumpToLetter(it) }
                                            tryAwaitRelease()
                                            activeLetter = null
                                        }
                                    )
                                }
                                .pointerInput(alphabet, alphabetHeight) {
                                    detectVerticalDragGestures(
                                        onDragStart = { offset -> letterAt(offset.y)?.let { jumpToLetter(it) } },
                                        onDragEnd = { activeLetter = null },
                                        onDragCancel = { activeLetter = null }
                                    ) { change, _ ->
                                        change.consume()
                                        letterAt(change.position.y)?.let { jumpToLetter(it) }
                                    }
                                },
                            verticalArrangement = Arrangement.SpaceEvenly,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            alphabet.forEach { letter ->
                                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(
                                        letter,
                                        color = if (activeLetter == letter) Color.White else Lavender,
                                        fontSize = letterSize.sp,
                                        lineHeight = letterSize.sp,
                                        maxLines = 1,
                                        fontWeight = if (activeLetter == letter) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
                    // Кружок с выбранной буквой: появляется рядом с буквой в алфавите и крутится.
                    val bubbleIndex = alphabet.indexOf(bubbleLetter).coerceAtLeast(0)
                    val bubbleTargetY = (bubbleIndex + 0.5f) * alphabetHeight / alphabet.size.coerceAtLeast(1)
                    val bubbleY = androidx.compose.animation.core.animateFloatAsState(
                        targetValue = bubbleTargetY, animationSpec = tween(70), label = "letter-bubble-y"
                    )
                    // Полное имя нужно, чтобы компилятор не выбрал версию AnimatedVisibility для внешнего Column.
                    androidx.compose.animation.AnimatedVisibility(
                        visible = activeLetter != null && alphabet.isNotEmpty(),
                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 40.dp)
                            .offset { IntOffset(0, (bubbleY.value - 30.dp.toPx()).roundToInt()) },
                        enter = fadeIn(tween(90)) + scaleIn(initialScale = 0.6f),
                        exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.6f)
                    ) {
                        LetterBubble(bubbleLetter)
                    }
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    placeholder = { Text(tr("Поиск приложений", appLanguage)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Lavender, unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        focusedLeadingIconColor = Lavender, unfocusedLeadingIconColor = Color.White
                    )
                )
            }
        }

        selectedWorkspaceWidget?.let { widgetId ->
            AlertDialog(
                onDismissRequest = { selectedWorkspaceWidget = null },
                title = { Text(tr("Виджет", appLanguage)) },
                text = { Text(tr("Что сделать с виджетом?", appLanguage)) },
                confirmButton = {
                    if (workspaceWidgets.firstOrNull { it.id == widgetId }?.type == "weather") {
                        TextButton(onClick = {
                            weatherRefreshToken++
                            selectedWorkspaceWidget = null
                        }) { Text(tr("Обновить погоду", appLanguage)) }
                    }
                    TextButton(onClick = {
                        resizingWidgetId = widgetId
                        selectedWorkspaceWidget = null
                    }) { Text(tr("Изменить размер", appLanguage)) }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            val target = workspaceWidgets.firstOrNull { it.id == widgetId }
                            if (target?.type == "android") {
                                runCatching { appWidgetHost.deleteAppWidgetId(widgetId) }
                                hostedWidgetIds = hostedWidgetIds - widgetId
                                prefs.edit().putString("hosted_widget_ids", hostedWidgetIds.joinToString(",")).apply()
                            }
                            saveWorkspaceWidgets(workspaceWidgets.filterNot { it.id == widgetId })
                            selectedWorkspaceWidget = null
                        }) { Text(tr("Убрать виджет", appLanguage), color = MaterialTheme.colorScheme.error) }
                        TextButton(onClick = { selectedWorkspaceWidget = null }) { Text(tr("Отмена", appLanguage)) }
                    }
                }
            )
        }

        workspaceContextApp?.let { app ->
            val ru = appLanguage == "ru"
            Dialog(onDismissRequest = { workspaceContextApp = null }) {
                Column(Modifier.width(280.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Surface(color = Color(0xFF171B20), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp), shadowElevation = 14.dp) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            TextButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                workspaceContextApp = null
                                runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.packageName))) }
                            }) {
                                Text("ⓘ", color = Color(0xFFE5E7EB), fontSize = 21.sp)
                                Spacer(Modifier.width(14.dp))
                                Text(if (ru) "О приложении" else "App info", color = Color(0xFFF3F4F6), modifier = Modifier.fillMaxWidth())
                            }
                            TextButton(modifier = Modifier.fillMaxWidth(), onClick = { addToDock(app.packageName); workspaceContextApp = null }) {
                                Text("⌂", color = Color(0xFFE5E7EB), fontSize = 23.sp)
                                Spacer(Modifier.width(14.dp))
                                Text(if (ru) "В док" else "Add to dock", color = Color(0xFFF3F4F6), modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    Surface(color = Color(0xFF171B20), shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp, topStart = 0.dp, topEnd = 0.dp), shadowElevation = 14.dp) {
                        TextButton(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), onClick = {
                            removeFromLauncher(app.packageName)
                            workspaceContextApp = null
                        }) {
                            Text("✕", color = Color(0xFFF2A7B5), fontSize = 22.sp)
                            Spacer(Modifier.width(14.dp))
                            Text(if (ru) "Убрать с экрана" else "Remove from home screen", color = Color(0xFFF2A7B5), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        selectedApp?.let { app ->
            val alreadyOnHome = app.packageName in workspacePackages
            AlertDialog(
                onDismissRequest = { selectedApp = null },
                title = { Text(app.label) },
                text = {
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !alreadyOnHome,
                        onClick = { addToWorkspace(app.packageName); selectedApp = null; drawerOpen = false }
                    ) { Text(if (appLanguage == "ru") "Добавить на рабочий стол" else "Add to Home screen", modifier = Modifier.fillMaxWidth()) }
                },
                confirmButton = { TextButton(onClick = { selectedApp = null }) { Text(if (appLanguage == "ru") "Закрыть" else "Close") } }
            )
        }

        AnimatedVisibility(
            visible = workspaceEditMode,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 174.dp),
            enter = fadeIn(tween(180)) + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut(tween(120)) + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(Color(0xDD242333)).padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable {
                    workspaceEditMode = false
                    widgetSourceOpen = true
                }) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f), modifier = Modifier.size(58.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(tr("▦", appLanguage), color = Color.White, fontSize = 28.sp) }
                    }
                    Text(tr("Виджеты", appLanguage), color = Color.White, fontSize = 12.sp, maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable {
                    addWorkspacePage()
                    workspaceEditMode = false
                }) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f), modifier = Modifier.size(58.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(tr("+", appLanguage), color = Color.White, fontSize = 34.sp) }
                    }
                    Text(tr("Новый рабочий стол", appLanguage), color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable {
                    workspaceEditMode = false
                    settingsOpen = true
                }) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.16f), modifier = Modifier.size(58.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(tr("⚙", appLanguage), color = Color.White, fontSize = 28.sp) }
                    }
                    Text(tr("Настройки лаунчера", appLanguage), color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (workspaceEditMode && workspacePage > 0) {
            TextButton(
                onClick = { confirmDeleteWorkspacePage = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 34.dp, end = 18.dp)
            ) { Text(tr("Удалить этот экран", appLanguage), color = Color.White) }
        }
        if (confirmDeleteWorkspacePage) {
            AlertDialog(
                onDismissRequest = { confirmDeleteWorkspacePage = false },
                title = { Text(tr("Удалить рабочий стол?", appLanguage)) },
                text = { Text(tr("Приложения и виджеты с этого рабочего стола будут удалены с него.", appLanguage)) },
                confirmButton = { TextButton(onClick = { confirmDeleteWorkspacePage = false; deleteCurrentWorkspacePage() }) { Text(tr("Удалить", appLanguage)) } },
                dismissButton = { TextButton(onClick = { confirmDeleteWorkspacePage = false }) { Text(tr("Отмена", appLanguage)) } }
            )
        }


        if (weatherCityDialog) {
            AlertDialog(onDismissRequest = { weatherCityDialog = false; pendingWeatherWidget = false }, title = { Text(tr("Выбери город", appLanguage)) },
                text = { LazyColumn(Modifier.heightIn(max = 420.dp)) { lazyItems(russianWeatherCities) { city ->
                    Text(city.name, modifier = Modifier.fillMaxWidth().clickable {
                        weatherCityName = city.name; weatherLatitude = city.latitude; weatherLongitude = city.longitude; prefs.edit().putString("weather_city_name", city.name).putString("weather_latitude", city.latitude.toString()).putString("weather_longitude", city.longitude.toString()).apply(); weatherCityDialog = false
                        if (pendingWeatherWidget) { pendingWeatherWidget = false; addOwnWidget("weather") }
                    }.padding(vertical = 12.dp, horizontal = 8.dp))
                } } }, confirmButton = { TextButton(onClick = { weatherCityDialog = false; pendingWeatherWidget = false }) { Text(tr("Отмена", appLanguage)) } })
        }
        if (widgetSourceOpen) {
            AlertDialog(
                onDismissRequest = { widgetSourceOpen = false },
                title = { Text(tr("Добавить виджет", appLanguage)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr("Часы и дата", appLanguage), color = Purple, fontWeight = FontWeight.Bold)
                        listOf("digital" to "Цифровые часы", "analog" to "Аналоговые часы", "date" to "Дата", "weather" to "Погода").forEach { (type, label) ->
                            Text(tr(label, appLanguage), modifier = Modifier.fillMaxWidth().clickable {
                                if (type == "weather") requestWeatherWidget() else addOwnWidget(type)
                                widgetSourceOpen = false
                            }.padding(10.dp))
                        }
                        Divider()
                        Text(tr("Другие", appLanguage), modifier = Modifier.fillMaxWidth().clickable {
                            widgetSourceOpen = false
                            val id = appWidgetHost.allocateAppWidgetId()
                            pendingPickId = id
                            val pick = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                            runCatching { widgetPicker.launch(pick) }.onFailure { appWidgetHost.deleteAppWidgetId(id) }
                        }.padding(10.dp))
                    }
                },
                confirmButton = { TextButton(onClick = { widgetSourceOpen = false }) { Text(tr("Закрыть", appLanguage)) } }
            )
        }

        // Добавление виджета из списка установленных приложений (экран «Виджеты»).
        fun startAddProviderWidget(info: AppWidgetProviderInfo) {
            val id = appWidgetHost.allocateAppWidgetId()
            val bound = runCatching { appWidgetManager.bindAppWidgetIdIfAllowed(id, info.provider) }.getOrDefault(false)
            if (bound) {
                widgetsOpen = false
                continueWidgetAdd(id)
            } else {
                pendingPickId = id
                val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                }
                val launched = runCatching { widgetBindLauncher.launch(bindIntent) }.isSuccess
                if (launched) {
                    widgetsOpen = false
                } else {
                    pendingPickId = null
                    runCatching { appWidgetHost.deleteAppWidgetId(id) }
                }
            }
        }

        if (widgetsOpen) {
            val allProviders = remember { runCatching { appWidgetManager.installedProviders.toList() }.getOrDefault(emptyList()) }
            val providerGroups = remember(allProviders, apps) {
                allProviders.groupBy { it.provider.packageName }.map { (pkg, list) ->
                    val label = appMap[pkg]?.label
                        ?: runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrNull()
                        ?: pkg
                    Triple(pkg, label, list.sortedBy { runCatching { it.loadLabel(pm) }.getOrDefault("") })
                }.sortedBy { it.second.lowercase() }
            }
            val ownWidgets = listOf(
                "digital" to tr("Часы", appLanguage),
                "analog" to tr("Аналоговые", appLanguage),
                "date" to tr("Дата", appLanguage),
                "weather" to tr("Погода", appLanguage)
            )
            val validHosted = hostedWidgetIds.filter { appWidgetManager.getAppWidgetInfo(it) != null }
            Dialog(
                onDismissRequest = { widgetsOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            ScreenBackButton { widgetsOpen = false }
                            Spacer(Modifier.width(16.dp))
                            Text(tr("Виджеты", appLanguage), fontSize = 34.sp, color = MaterialTheme.colorScheme.onBackground)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Встроенные виджеты Prosto
                            item(key = "group-own") {
                                ExpandableCard(
                                    title = "Prosto",
                                    subtitle = widgetCountText(ownWidgets.size, appLanguage),
                                    expanded = expandedWidgetGroup == "own",
                                    onToggle = { expandedWidgetGroup = if (expandedWidgetGroup == "own") null else "own" }
                                ) {
                                    ownWidgets.forEach { (type, label) ->
                                        WidgetEntryRow(label = label, details = null, preview = null) {
                                            if (type == "weather") requestWeatherWidget() else addOwnWidget(type)
                                            widgetsOpen = false
                                        }
                                    }
                                }
                            }
                            // Виджеты установленных приложений, сгруппированные по приложению
                            lazyItems(providerGroups, key = { "group-" + it.first }) { (pkg, label, list) ->
                                ExpandableCard(
                                    title = label,
                                    subtitle = widgetCountText(list.size, appLanguage),
                                    expanded = expandedWidgetGroup == pkg,
                                    onToggle = { expandedWidgetGroup = if (expandedWidgetGroup == pkg) null else pkg }
                                ) {
                                    list.forEach { info ->
                                        val size = widgetSizeFor(info)
                                        val cols = ((size.first + 4f) / gridCellPitch).roundToInt().coerceIn(1, 4)
                                        val rows = ((size.second + 12f) / 98f).roundToInt().coerceIn(1, 5)
                                        WidgetEntryRow(
                                            label = runCatching { info.loadLabel(pm) }.getOrDefault(label),
                                            details = "$cols×$rows",
                                            preview = { ctx ->
                                                runCatching { info.loadPreviewImage(ctx, 0) }.getOrNull()
                                                    ?: runCatching { info.loadIcon(ctx, 0) }.getOrNull()
                                            }
                                        ) { startAddProviderWidget(info) }
                                    }
                                }
                            }
                            // Уже добавленные виджеты (можно удалить)
                            if (validHosted.isNotEmpty()) {
                                item(key = "group-added") {
                                    ExpandableCard(
                                        title = tr("Добавленные виджеты", appLanguage),
                                        subtitle = widgetCountText(validHosted.size, appLanguage),
                                        expanded = expandedWidgetGroup == "added",
                                        onToggle = { expandedWidgetGroup = if (expandedWidgetGroup == "added") null else "added" }
                                    ) {
                                        validHosted.forEach { widgetId ->
                                            val provider = appWidgetManager.getAppWidgetInfo(widgetId)
                                            Row(
                                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    runCatching { provider?.loadLabel(pm) }.getOrNull() ?: "Widget",
                                                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                IconButton(onClick = {
                                                    appWidgetHost.deleteAppWidgetId(widgetId)
                                                    hostedWidgetIds = hostedWidgetIds - widgetId
                                                    prefs.edit().putString("hosted_widget_ids", hostedWidgetIds.joinToString(",")).apply()
                                                    saveWorkspaceWidgets(workspaceWidgets.filterNot { it.id == widgetId })
                                                }) {
                                                    Icon(Icons.Default.Delete, contentDescription = tr("Удалить виджет", appLanguage),
                                                        tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(settingsOpen) {
            if (settingsOpen) {
                settingsPage = ""
                settingsQuery = ""
                isDefaultLauncher = runCatching {
                    val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == context.packageName
                }.getOrDefault(false)
            }
        }
        if (settingsOpen) {
            val shapeLabels = listOf(
                IconShapeChoice.CIRCLE to "Circle",
                IconShapeChoice.SQUARE to "Square",
                IconShapeChoice.VERY_SUNNY to "Very sunny",
                IconShapeChoice.COOKIE_9 to "9-sided cookie",
                IconShapeChoice.SOFT_BURST to "Soft burst",
                IconShapeChoice.CLOVER to "Clover"
            )
            val themeName = when (themeChoice) {
                LauncherTheme.LIGHT -> tr("Светлая", appLanguage)
                LauncherTheme.DARK -> tr("Тёмная", appLanguage)
                else -> tr("Системная", appLanguage)
            }
            val languageName = if (appLanguage == "en") "English" else "Русский"
            val cityName = if (weatherCityName.isBlank()) tr("Город не выбран", appLanguage) else weatherCityName
            // Пункты главного экрана настроек: id, раздел, иконка, название, описание.
            val settingsEntries = listOf(
                SettingsEntry("theme", "look", Icons.Default.Palette, tr("Тема", appLanguage), themeName),
                SettingsEntry("icons", "look", Icons.Default.Apps, tr("Настройка иконок", appLanguage),
                    shapeLabels.firstOrNull { it.first == iconShapeChoice }?.second ?: ""),
                SettingsEntry("wallpaper", "look", Icons.Default.Image, tr("Обои", appLanguage),
                    tr("Выбери изображение из галереи или верни стандартные обои.", appLanguage)),
                SettingsEntry("folders", "look", Icons.Default.Folder, tr("Прозрачность папок", appLanguage),
                    "${(folderOpacity * 100).toInt()}%"),
                SettingsEntry("language", "general", Icons.Default.Language, tr("Язык", appLanguage), languageName),
                SettingsEntry("weather", "general", Icons.Default.WbSunny, tr("Погода", appLanguage), cityName),
                SettingsEntry("about", "general", Icons.Default.Info, "Prosto Launcher", "0.2.0")
            )
            Dialog(
                onDismissRequest = { if (settingsPage.isNotEmpty()) settingsPage = "" else settingsOpen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
            ) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(12.dp))
                        ScreenBackButton { if (settingsPage.isNotEmpty()) settingsPage = "" else settingsOpen = false }
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 30.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (settingsPage.isEmpty()) {
                                item {
                                    Text(tr("Настройки", appLanguage), fontSize = 38.sp, color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp))
                                }
                                item {
                                    TextField(
                                        value = settingsQuery,
                                        onValueChange = { settingsQuery = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(28.dp),
                                        placeholder = { Text(tr("Найти настройку…", appLanguage)) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        colors = TextFieldDefaults.colors(
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            disabledIndicatorColor = Color.Transparent,
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                                val query = settingsQuery.trim()
                                val visibleEntries = if (query.isEmpty()) settingsEntries else settingsEntries.filter {
                                    it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true)
                                }
                                if (query.isNotEmpty() && visibleEntries.isEmpty()) {
                                    item {
                                        Text(tr("Ничего не найдено", appLanguage), color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(16.dp))
                                    }
                                }
                                listOf("look" to tr("Внешний вид", appLanguage), "general" to tr("Общие", appLanguage)).forEach { (section, sectionTitle) ->
                                    val group = visibleEntries.filter { it.section == section }
                                    if (group.isNotEmpty()) {
                                        item(key = "section-$section") {
                                            if (query.isEmpty()) {
                                                Text(sectionTitle, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 20.dp, top = 12.dp))
                                            }
                                        }
                                        item(key = "group-$section") {
                                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                group.forEachIndexed { index, entry ->
                                                    val big = 28.dp
                                                    val small = 6.dp
                                                    val shape = RoundedCornerShape(
                                                        topStart = if (index == 0) big else small, topEnd = if (index == 0) big else small,
                                                        bottomStart = if (index == group.lastIndex) big else small,
                                                        bottomEnd = if (index == group.lastIndex) big else small
                                                    )
                                                    SettingsRow(entry.icon, entry.title, entry.subtitle, shape) { settingsPage = entry.id }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                val pageTitle = settingsEntries.firstOrNull { it.id == settingsPage }?.title ?: ""
                                item {
                                    Text(pageTitle, fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp))
                                }
                                when (settingsPage) {
                                    "theme" -> item {
                                        Column {
                                            ThemeRow(tr("Системная", appLanguage), themeChoice == LauncherTheme.SYSTEM) { onThemeChange(LauncherTheme.SYSTEM) }
                                            ThemeRow(tr("Светлая", appLanguage), themeChoice == LauncherTheme.LIGHT) { onThemeChange(LauncherTheme.LIGHT) }
                                            ThemeRow(tr("Тёмная", appLanguage), themeChoice == LauncherTheme.DARK) { onThemeChange(LauncherTheme.DARK) }
                                        }
                                    }
                                    "icons" -> item {
                                        Column {
                                            shapeLabels.forEach { (choice, label) ->
                                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        iconShapeChoice = choice
                                                        prefs.edit().putString("icon_shape", choice.name).apply()
                                                    }.padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically) {
                                                    RadioButton(selected = iconShapeChoice == choice, onClick = {
                                                        iconShapeChoice = choice
                                                        prefs.edit().putString("icon_shape", choice.name).apply()
                                                    })
                                                    Spacer(Modifier.width(8.dp))
                                                    apps.firstOrNull()?.let { AppImage(it, 34, choice) }
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(label)
                                                }
                                            }
                                        }
                                    }
                                    "wallpaper" -> item {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(tr("Выбери изображение из галереи или верни стандартные обои.", appLanguage),
                                                style = MaterialTheme.typography.bodyMedium)
                                            OutlinedButton(onClick = { wallpaperPicker.launch(arrayOf("image/*")) }) {
                                                Text(tr("Выбрать обои из галереи", appLanguage))
                                            }
                                            if (wallpaperUri != null) {
                                                TextButton(onClick = {
                                                    wallpaperUri = null
                                                    prefs.edit().remove("wallpaper_uri").apply()
                                                }) { Text(tr("Вернуть стандартные обои", appLanguage)) }
                                            }
                                        }
                                    }
                                    "folders" -> item {
                                        Column {
                                            Text(tr("${(folderOpacity * 100).toInt()}%", appLanguage), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            WavySlider(
                                                value = folderOpacity,
                                                onValueChange = {
                                                    folderOpacity = it
                                                    prefs.edit().putFloat("folder_opacity", it).apply()
                                                },
                                                valueRange = 0.15f..1f,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    "language" -> item {
                                        Column {
                                            listOf("ru" to "Русский", "en" to "English").forEach { (code, label) ->
                                                Row(Modifier.fillMaxWidth().clickable {
                                                    appLanguage = code
                                                    prefs.edit().putString("app_language", code).apply()
                                                }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    RadioButton(selected = appLanguage == code, onClick = {
                                                        appLanguage = code
                                                        prefs.edit().putString("app_language", code).apply()
                                                    })
                                                    Text(label)
                                                }
                                            }
                                        }
                                    }
                                    "weather" -> item {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(if (weatherCityName.isBlank()) tr("Город не выбран", appLanguage) else if (appLanguage == "en") "City: $weatherCityName" else "Город: $weatherCityName",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            OutlinedButton(onClick = { weatherCityDialog = true }) { Text(tr("Выбрать город", appLanguage)) }
                                            Text(tr("Погода обновляется автоматически. Ключ API не нужен.", appLanguage), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    "about" -> item {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Prosto Launcher 0.2.0", style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            OutlinedButton(onClick = {
                                                runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                                                    .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) } }
                                            }) { Text(tr("Сделать лаунчером по умолчанию", appLanguage)) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---------- Открытая папка: светлая полупрозрачная карточка по центру ----------
        if (folderOverlayAlive) {
            val screenHeightDp = LocalConfiguration.current.screenHeightDp
            val folderKey = openedFolder
            val memberApps = folderKey?.let { folderContents[it] }.orEmpty().mapNotNull { appMap[it] }
            val cardAlpha = (0.45f + folderOpacity * 0.5f).coerceIn(0.5f, 0.95f)
            Box(
                Modifier.fillMaxSize()
                    .onGloballyPositioned { overlayCenter = Offset(it.size.width / 2f, it.size.height / 2f) }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        folderVisible = false
                    },
                contentAlignment = Alignment.Center
            ) {
                // Фон рабочего стола остаётся чётким при открытии папки.
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            val raw = folderProgress.value
                            val scale = 0.14f + 0.86f * raw
                            scaleX = scale
                            scaleY = scale
                            if (folderOrigin.isSpecified) {
                                translationX = (folderOrigin.x - overlayCenter.x) * (1f - raw)
                                translationY = (folderOrigin.y - overlayCenter.y) * (1f - raw)
                            }
                            alpha = (raw.coerceIn(0f, 1f) * 2.2f).coerceIn(0f, 1f)
                        }
                        // Нажатие внутри карточки не должно закрывать папку.
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
                    shape = RoundedCornerShape(32.dp),
                    color = Color(0xFFF4EEFF).copy(alpha = cardAlpha),
                    shadowElevation = 20.dp,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
                ) {
                    Column(
                        Modifier.padding(start = 18.dp, end = 18.dp, top = 24.dp, bottom = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            Modifier.fillMaxWidth()
                                .heightIn(max = (screenHeightDp * 0.5f).dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Сетка по 3 иконки в ряд; последней ячейкой идёт «Добавить».
                            val cells = memberApps.map<LauncherApp, LauncherApp?> { it } + listOf<LauncherApp?>(null)
                            cells.chunked(3).forEach { rowCells ->
                                Row(Modifier.fillMaxWidth()) {
                                    rowCells.forEach { cell ->
                                        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                                            if (cell != null && folderKey != null) {
                                                FolderAppCell(
                                                    app = cell,
                                                    iconShapeChoice = iconShapeChoice,
                                                    onClick = { bounds ->
                                                        folderVisible = false
                                                        launch(cell.packageName, bounds)
                                                    },
                                                    onLongClick = { memberMenu = folderKey to cell.packageName }
                                                )
                                            } else if (folderKey != null) {
                                                FolderAddCell { addToFolderKey = folderKey }
                                            }
                                        }
                                    }
                                    repeat(3 - rowCells.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = folderKey?.let { folderTitle(it) } ?: "Папка",
                            color = DarkInk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(enabled = folderKey != null) {
                                folderKey?.let {
                                    renameText = folderNames[it].orEmpty()
                                    renamingFolder = it
                                }
                            }.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // ---------- Меню долгого нажатия на папку на рабочем столе ----------
        folderMenuKey?.let { key ->
            AlertDialog(
                onDismissRequest = { folderMenuKey = null },
                title = { Text(folderTitle(key)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                folderMenuKey = null
                                openFolder(key)
                            }
                        ) { Text(tr("Открыть", appLanguage), modifier = Modifier.fillMaxWidth()) }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                folderMenuKey = null
                                renameText = folderNames[key].orEmpty()
                                renamingFolder = key
                            }
                        ) { Text(tr("Переименовать", appLanguage), modifier = Modifier.fillMaxWidth()) }
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                folderMenuKey = null
                                deleteFolder(key)
                                Toast.makeText(context, "Папка удалена", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(tr("Удалить папку", appLanguage), color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { folderMenuKey = null }) { Text(tr("Закрыть", appLanguage)) } }
            )
        }

        // ---------- Переименование папки ----------
        renamingFolder?.let { key ->
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(key) { runCatching { focusRequester.requestFocus() } }
            AlertDialog(
                onDismissRequest = { renamingFolder = null },
                title = { Text(tr("Переименовать папку", appLanguage)) },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it.take(24) },
                        singleLine = true,
                        placeholder = { Text(tr("Папка", appLanguage)) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        renameFolder(key, renameText)
                        renamingFolder = null
                    }) { Text(tr("Сохранить", appLanguage)) }
                },
                dismissButton = { TextButton(onClick = { renamingFolder = null }) { Text(tr("Отмена", appLanguage)) } }
            )
        }

        // ---------- Меню приложения внутри папки ----------
        memberMenu?.let { (folderKey, pkg) ->
            val memberApp = appMap[pkg]
            val otherFolders = folderContents.keys.filter { it != folderKey }
            val otherTitles = otherFolders.map { folderTitle(it) }
            AlertDialog(
                onDismissRequest = { memberMenu = null },
                title = { Text(memberApp?.label ?: pkg) },
                text = {
                    Column(
                        Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                memberMenu = null
                                val dissolved = takeOutOfFolder(folderKey, pkg, placeOnDesktop = true)
                                if (dissolved) folderVisible = false
                            }
                        ) { Text(tr("Убрать из папки", appLanguage), modifier = Modifier.fillMaxWidth()) }
                        otherFolders.forEachIndexed { i, otherKey ->
                            val title = otherTitles[i]
                            val label = if (otherTitles.count { it == title } > 1) {
                                "$title (${otherTitles.take(i + 1).count { it == title }})"
                            } else title
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    memberMenu = null
                                    val willDissolve = (folderContents[folderKey]?.size ?: 0) < 3
                                    moveAppBetweenFolders(folderKey, pkg, otherKey)
                                    if (willDissolve) folderVisible = false
                                    Toast.makeText(context, "Перемещено в «$title»", Toast.LENGTH_SHORT).show()
                                }
                            ) { Text(tr("Переместить в «$label»", appLanguage), modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { memberMenu = null }) { Text(tr("Закрыть", appLanguage)) } }
            )
        }

        // ---------- Добавление приложений в папку ----------
        addToFolderKey?.let { folderKey ->
            val inFolders = folderContents.values.flatten().toSet()
            val candidates = apps.filter { it.packageName !in inFolders }
            AlertDialog(
                onDismissRequest = { addToFolderKey = null },
                title = { Text(tr("Добавить в папку", appLanguage)) },
                text = {
                    if (candidates.isEmpty()) {
                        Text(tr("Все приложения уже лежат в папках", appLanguage))
                    } else {
                        LazyColumn(
                            Modifier.heightIn(max = 380.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            lazyItems(candidates, key = { it.packageName }) { candidate ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                        .clickable { addAppToFolder(folderKey, candidate.packageName) }
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppImage(candidate, 38, iconShapeChoice)
                                    Spacer(Modifier.width(12.dp))
                                    Text(candidate.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { addToFolderKey = null }) { Text(tr("Готово", appLanguage)) } }
            )
        }
    }
}
}

private fun tr(value: String, language: String): String {
    if (language != "en") return value
    return when (value) {
        "Найти настройку…" -> "Search settings…"
        "Почти готово" -> "Almost done"
        "Сделайте его основным лаунчером" -> "Make it your default launcher"
        "Сделать основным" -> "Set as default"
        "Внешний вид" -> "Appearance"
        "Общие" -> "General"
        "Ничего не найдено" -> "Nothing found"
        "Добавленные виджеты" -> "Added widgets"
        "Настройки лаунчера" -> "Launcher settings"
        "Сделать лаунчером по умолчанию" -> "Set as default launcher"
        "Готово" -> "Done"
        "Новый рабочий стол" -> "New home screen"
        "В нижн. панель" -> "Add to dock"
        "Убрать с главного экрана" -> "Remove from home screen"
        "Зажми приложение, чтобы открыть меню действий" -> "Long-press an app to open its action menu"
        "Системная" -> "System"
        "Светлая" -> "Light"
        "Тёмная" -> "Dark"
        "Тема" -> "Theme"
        "Настройка иконок" -> "Icon settings"
        "Погода" -> "Weather"
        "Город не выбран" -> "No city selected"
        "Выбрать город" -> "Choose city"
        "Источник погоды" -> "Weather source"
        "Яндекс Погода" -> "Yandex Weather"
        "Google Weather" -> "Google Weather"
        "без ключа" -> "no key required"
        "Ключ API Яндекс Погоды" -> "Yandex Weather API key"
        "Ключ выдаётся в кабинете разработчика Яндекса." -> "Get a key from the Yandex developer dashboard."
        "Нужен ключ Google Maps Platform с включённым Weather API." -> "A Google Maps Platform key with Weather API enabled is required."
        "Прозрачность папок" -> "Folder transparency"
        "Обои" -> "Wallpaper"
        "Выбери изображение из галереи или верни стандартные обои." -> "Choose an image from the gallery or restore the default wallpaper."
        "Выбрать обои из галереи" -> "Choose wallpaper from gallery"
        "Вернуть стандартные обои" -> "Restore default wallpaper"
        "Виджет" -> "Widget"
        "Что сделать с виджетом?" -> "What would you like to do with this widget?"
        "Обновить погоду" -> "Refresh weather"
        "Изменить размер" -> "Resize"
        "Убрать виджет" -> "Remove widget"
        "Отмена" -> "Cancel"
        "На главный экран" -> "Add to home screen"
        "О приложении" -> "App info"
        "Все приложения" -> "All apps"
        "Поиск приложений" -> "Search apps"
        "Часы и дата" -> "Clock & date"
        "Цифровые часы" -> "Digital clock"
        "Аналоговые часы" -> "Analog clock"
        "Дата" -> "Date"
        "Другие" -> "Other"
        "Другие виджеты" -> "Other widgets"
        "Добавить" -> "Add"
        "Добавить виджет" -> "Add widget"
        "Закрыть" -> "Close"
        "Выбери виджет из приложений, установленных на устройстве." -> "Choose a widget from apps installed on your device."
        "Виджеты" -> "Widgets"
        "Часы" -> "Clock"
        "Аналоговые" -> "Analog"
        "Удалить виджет" -> "Remove widget"
        "Настройки иконок" -> "Icon settings"
        "Язык" -> "Language"
        "Удалить этот экран" -> "Delete this screen"
        "Удалить рабочий стол?" -> "Delete this home screen?"
        "Приложения и виджеты с этого рабочего стола будут удалены с него." -> "Apps and widgets on this home screen will be removed from it."
        "Выбрать" -> "Choose"
        "Папка" -> "Folder"
        "Переименовать" -> "Rename"
        "Удалить" -> "Delete"
        "Сохранить" -> "Save"
        "Переместить" -> "Move"
        "Настройки" -> "Settings"
        "Добавлено" -> "Added"
        "Нет данных" -> "No data"
        "Загрузка..." -> "Loading..."
        "Ясно" -> "Clear"
        "Облачно" -> "Cloudy"
        "Переменная облачность" -> "Partly cloudy"
        "Дождь" -> "Rain"
        "Снег" -> "Snow"
        "Гроза" -> "Thunderstorm"
        "Туман" -> "Fog"
        "Не удалось получить погоду. Проверь интернет." -> "Could not load weather. Check your connection."
        "Ливень" -> "Heavy rain"
        "Снегопад" -> "Snowfall"
        "Текущее местоположение" -> "Current location"
        "Погода обновляется автоматически. Ключ API не нужен." -> "Weather updates automatically. No API key required."
        else -> value
    }
}

@Composable
private fun WidgetResizeKnob(
    modifier: Modifier,
    edge: String,
    horizontalStep: Float,
    onResize: (String, Float, Float) -> Unit
) {
    Box(
        modifier = modifier.padding(2.dp).size(20.dp).clip(CircleShape)
            .background(Color.White).border(1.dp, Purple, CircleShape)
            .pointerInput(edge) {
                var accumulatedX = 0f
                var accumulatedY = 0f
                detectDragGestures(
                    onDragStart = {
                        accumulatedX = 0f
                        accumulatedY = 0f
                    },
                    onDragEnd = {
                        accumulatedX = 0f
                        accumulatedY = 0f
                    },
                    onDragCancel = {
                        accumulatedX = 0f
                        accumulatedY = 0f
                    }
                ) { change, drag ->
                    change.consume()
                    val density = this.density
                    accumulatedX += drag.x / density
                    accumulatedY += drag.y / density
                    val verticalStep = 98f
                    val dx = if (edge == "left" || edge == "right") {
                        if (kotlin.math.abs(accumulatedX) >= horizontalStep) {
                            val step = kotlin.math.sign(accumulatedX) * horizontalStep
                            accumulatedX -= step
                            step
                        } else 0f
                    } else 0f
                    val dy = if (edge == "top" || edge == "bottom") {
                        if (kotlin.math.abs(accumulatedY) >= verticalStep) {
                            val step = kotlin.math.sign(accumulatedY) * verticalStep
                            accumulatedY -= step
                            step
                        } else 0f
                    } else 0f
                    if (dx != 0f || dy != 0f) onResize(edge, dx, dy)
                }
            }
    )
}

@Composable
private fun WaveformLine(modifier: Modifier = Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "launcher-wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2.0).toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(2400, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "wave-phase"
    )
    androidx.compose.foundation.Canvas(modifier) {
        val mid = size.height / 2f
        val amplitude = size.height * 0.28f
        val path = androidx.compose.ui.graphics.Path()
        val steps = 160
        for (i in 0..steps) {
            val x = size.width * i / steps
            val y = mid + kotlin.math.sin((x / size.width * Math.PI * 10.0) + phase).toFloat() * amplitude
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Color.White.copy(alpha = 0.95f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        val endX = size.width - 2.dp.toPx()
        drawLine(Color.White, Offset(endX, mid - size.height * 0.48f), Offset(endX, mid + size.height * 0.48f), strokeWidth = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
private fun DigitalClockWidget(language: String, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(15_000)
        }
    }
    Column(modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(now.format(DateTimeFormatter.ofPattern("HH:mm")), fontSize = 48.sp, fontWeight = FontWeight.Light, color = Color.White)
        Text(now.format(DateTimeFormatter.ofPattern("EEE, d MMMM", if (language == "en") Locale.ENGLISH else Locale("ru"))), color = Color.White.copy(alpha = 0.92f), fontSize = 17.sp)
    }
}

@Composable
private fun DateWidget(language: String, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000)
        }
    }
    Surface(modifier.fillMaxSize(), color = Color.Transparent) {
        Box(contentAlignment = Alignment.Center) {
            Text(now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", if (language == "en") Locale.ENGLISH else Locale("ru"))),
                color = Lavender, fontSize = 17.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun WeatherWidget(cityName: String, weather: WeatherInfo?, loading: Boolean, error: String?, language: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(weather?.emoji ?: "🌤️", fontSize = 48.sp); Spacer(Modifier.width(12.dp))
        Column {
            Text(weather?.let { "${if (it.temperature > 0) "+" else ""}${it.temperature}°" } ?: "—°", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Light)
            Text(weather?.description?.let { tr(it, language) } ?: if (loading) tr("Загрузка...", language) else tr("Нет данных", language), color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp)
            Text(error?.let { tr(it, language) } ?: cityName, color = Color.White.copy(alpha = 0.75f), fontSize = if (error == null) 12.sp else 10.sp, maxLines = 2)
        }
    }
}

@Composable
private fun AnalogClockWidget(modifier: Modifier = Modifier) {
    var clockNow by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            clockNow = LocalDateTime.now()
            delay(1000)
        }
    }
    androidx.compose.foundation.Canvas(modifier.fillMaxSize().padding(8.dp)) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.44f
            drawCircle(color = Lavender, radius = radius, center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
            for (tick in 0 until 12) {
                val angle = Math.toRadians(tick * 30.0 - 90.0)
                val inner = radius * 0.86f
                val outer = radius * 0.96f
                drawLine(
                    color = Lavender,
                    start = androidx.compose.ui.geometry.Offset(center.x + kotlin.math.cos(angle).toFloat() * inner, center.y + kotlin.math.sin(angle).toFloat() * inner),
                    end = androidx.compose.ui.geometry.Offset(center.x + kotlin.math.cos(angle).toFloat() * outer, center.y + kotlin.math.sin(angle).toFloat() * outer),
                    strokeWidth = if (tick % 3 == 0) 3.dp.toPx() else 1.5.dp.toPx()
                )
            }
            val seconds = clockNow.second + clockNow.nano / 1_000_000_000f
            val minute = clockNow.minute + seconds / 60f
            val hour = (clockNow.hour % 12) + minute / 60f
            fun hand(value: Float, divisions: Float, length: Float, width: Float, color: Color) {
                val angle = Math.toRadians(value / divisions * 360.0 - 90.0)
                drawLine(
                    color = color,
                    start = center,
                    end = androidx.compose.ui.geometry.Offset(center.x + kotlin.math.cos(angle).toFloat() * radius * length,
                        center.y + kotlin.math.sin(angle).toFloat() * radius * length),
                    strokeWidth = width.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
            hand(hour, 12f, 0.52f, 4f, Lavender)
            hand(minute, 60f, 0.76f, 2.8f, Lavender)
            hand(seconds, 60f, 0.84f, 1.2f, Purple)
            drawCircle(Purple, radius = 4.dp.toPx(), center = center)
    }
}

private fun queryLauncherApps(context: android.content.Context): List<LauncherApp> {
    val pm = context.packageManager
    return runCatching {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val entries = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        entries.mapNotNull { info ->
            runCatching {
                val pkg = info.activityInfo?.packageName ?: return@runCatching null
                if (pkg == context.packageName) return@runCatching null
                LauncherApp(info.loadLabel(pm)?.toString().orEmpty().ifBlank { pkg }, pkg, info.loadIcon(pm))
            }.getOrNull()
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.getDefault()) }
    }.getOrElse { emptyList() }
}


private fun scallopedShape(lobes: Int, innerRadius: Float): Shape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = minOf(size.width, size.height) / 2f
    val points = (0 until lobes * 2).map { i ->
        val angle = Math.PI * 2.0 * i / (lobes * 2.0) - Math.PI / 2.0
        val r = radius * if (i % 2 == 0) 1f else innerRadius
        Offset(cx + r * kotlin.math.cos(angle).toFloat(), cy + r * kotlin.math.sin(angle).toFloat())
    }
    val firstMid = Offset((points.last().x + points.first().x) / 2f, (points.last().y + points.first().y) / 2f)
    moveTo(firstMid.x, firstMid.y)
    points.forEachIndexed { i, point ->
        val next = points[(i + 1) % points.size]
        val mid = Offset((point.x + next.x) / 2f, (point.y + next.y) / 2f)
        quadraticTo(point.x, point.y, mid.x, mid.y)
    }
    close()
}

private fun iconShape(choice: IconShapeChoice): Shape {
    return when (choice) {
        IconShapeChoice.CIRCLE -> CircleShape
        IconShapeChoice.SQUARE -> RectangleShape
        IconShapeChoice.VERY_SUNNY -> scallopedShape(8, 0.78f)
        IconShapeChoice.COOKIE_9 -> GenericShape { size, _ ->
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = minOf(size.width, size.height) / 2f
            for (i in 0 until 9) {
                val angle = (Math.PI * 2.0 * i / 9.0) - Math.PI / 2.0
                val x = cx + radius * kotlin.math.cos(angle).toFloat()
                val y = cy + radius * kotlin.math.sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        IconShapeChoice.SOFT_BURST -> scallopedShape(12, 0.82f)
        IconShapeChoice.CLOVER -> scallopedShape(4, 0.70f)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DockSlot(
    app: LauncherApp?,
    key: String,
    blockedRects: MutableMap<String, Rect>,
    iconShapeChoice: IconShapeChoice,
    onLongClick: (LauncherApp) -> Unit = {},
    onClick: (LauncherApp, Rect?) -> Unit
) {
    Box(Modifier.size(48.dp).onGloballyPositioned { blockedRects[key] = it.boundsInRoot() }, contentAlignment = Alignment.Center) {
        if (app != null) {
            Box(Modifier.fillMaxSize().combinedClickable(onClick = { onClick(app, blockedRects[key]) }, onLongClick = { onLongClick(app) }), contentAlignment = Alignment.Center) {
                AppImage(app, 46, iconShapeChoice)
            }
        }
    }
}

@Composable
private fun AppImage(app: LauncherApp, size: Int, iconShapeChoice: IconShapeChoice = IconShapeChoice.CIRCLE) {
    AndroidView(
        factory = { ctx -> ImageView(ctx).apply { setImageDrawable(app.icon); scaleType = ImageView.ScaleType.FIT_CENTER } },
        update = { it.setImageDrawable(app.icon) },
        modifier = Modifier.size(size.dp).padding(2.dp).clip(iconShape(iconShapeChoice))
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderAppCell(
    app: LauncherApp,
    iconShapeChoice: IconShapeChoice,
    onClick: (Rect?) -> Unit,
    onLongClick: () -> Unit
) {
    val bounds = remember { arrayOfNulls<Rect>(1) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .onGloballyPositioned { bounds[0] = it.boundsInRoot() }
            .combinedClickable(onClick = { onClick(bounds[0]) }, onLongClick = onLongClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppImage(app, 58, iconShapeChoice)
        Spacer(Modifier.height(4.dp))
        Text(
            app.label, fontSize = 12.sp, maxLines = 2, color = DarkInk,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FolderAddCell(onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.padding(2.dp).size(54.dp).clip(CircleShape).background(Purple.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить приложение", tint = Purple, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text("Добавить", fontSize = 12.sp, maxLines = 1, color = DarkInk, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ThemeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

// Квадрат вокруг иконки: от него система «разворачивает» окно запускаемого приложения.
private fun iconRectFor(bounds: Rect, density: Float): Rect {
    val side = minOf(bounds.width, 56f * density)
    val topShift = if (bounds.height > side * 1.3f) 6f * density else 0f
    return Rect(Offset(bounds.center.x, bounds.top + topShift + side / 2f), side / 2f)
}

private fun launchApp(
    context: android.content.Context,
    packageName: String,
    view: android.view.View? = null,
    from: Rect? = null
) {
    runCatching {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return@runCatching
        val options = if (view != null && from != null) {
            android.app.ActivityOptions.makeScaleUpAnimation(
                view,
                from.left.toInt(),
                from.top.toInt(),
                from.width.toInt().coerceAtLeast(1),
                from.height.toInt().coerceAtLeast(1)
            ).toBundle()
        } else null
        context.startActivity(intent, options)
    }
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var ctx: android.content.Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

// ---------------- Общие элементы экранов «Настройки» и «Виджеты» ----------------

private data class SettingsEntry(
    val id: String,
    val section: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenBackButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, shape: Shape, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExpandableCard(
    title: String,
    subtitle: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    Icons.Default.KeyboardArrowDown, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) { content() }
            }
        }
    }
}

@Composable
private fun WidgetEntryRow(
    label: String,
    details: String?,
    preview: ((android.content.Context) -> android.graphics.drawable.Drawable?)?,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (preview != null) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageDrawable(preview(ctx))
                    }
                },
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            if (details != null) {
                Text(details, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

private fun widgetCountText(count: Int, language: String): String {
    if (language == "en") return if (count == 1) "1 widget" else "$count widgets"
    val mod100 = count % 100
    val mod10 = count % 10
    val word = when {
        mod100 in 11..14 -> "виджетов"
        mod10 == 1 -> "виджет"
        mod10 in 2..4 -> "виджета"
        else -> "виджетов"
    }
    return "$count $word"
}

// Волнистая «печенька» (12 мягких волн), как в круглом значке выбранной буквы.
private fun wavyCookieShape(bumps: Int = 12, depth: Float = 0.1f): Shape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = minOf(size.width, size.height) / 2f
    val steps = 240
    for (i in 0..steps) {
        val angle = Math.PI * 2.0 * i / steps
        val r = radius * (1f - depth * (1f - kotlin.math.cos(angle * bumps).toFloat()) / 2f)
        val x = cx + r * kotlin.math.cos(angle).toFloat()
        val y = cy + r * kotlin.math.sin(angle).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

// Кружок с буквой при быстрой прокрутке по алфавиту: фигура крутится, буква стоит на месте.
@Composable
private fun LetterBubble(letter: String, modifier: Modifier = Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "letter-bubble")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(7000, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "letter-bubble-angle"
    )
    val shape = remember { wavyCookieShape() }
    Box(modifier.size(60.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { rotationZ = angle }
                .background(Color(0xFFC4ABF0), shape)
                .border(3.dp, Color(0xFF5B4694), shape)
        )
        Text(letter, color = DarkInk, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

// Ползунок с волнистой линией: волна всё время бежит, а при перетягивании становится выше.
@Composable
private fun WavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    color: Color = Lavender
) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "wavy-slider")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2.0).toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1500, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "wavy-phase"
    )
    var dragging by remember { mutableStateOf(false) }
    val emphasis by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (dragging) 1f else 0f, animationSpec = tween(180), label = "wavy-emphasis"
    )
    val currentOnChange by rememberUpdatedState(onValueChange)
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    fun valueAt(x: Float, width: Float, inset: Float): Float {
        val f = ((x - inset) / (width - 2f * inset)).coerceIn(0f, 1f)
        return valueRange.start + f * (valueRange.endInclusive - valueRange.start)
    }

    androidx.compose.foundation.Canvas(
        modifier.fillMaxWidth().height(48.dp)
            .pointerInput(valueRange) {
                val inset = 12.dp.toPx()
                val width = size.width.toFloat()
                detectTapGestures(onPress = { offset ->
                    dragging = true
                    currentOnChange(valueAt(offset.x, width, inset))
                    tryAwaitRelease()
                    dragging = false
                })
            }
            .pointerInput(valueRange) {
                val inset = 12.dp.toPx()
                val width = size.width.toFloat()
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        currentOnChange(valueAt(offset.x, width, inset))
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false }
                ) { change, _ ->
                    change.consume()
                    currentOnChange(valueAt(change.position.x, width, inset))
                }
            }
    ) {
        val inset = 12.dp.toPx()
        val cy = size.height / 2f
        val start = inset
        val end = size.width - inset
        val thumbX = start + fraction * (end - start)
        val strokeWidth = 4.dp.toPx()
        val gap = 8.dp.toPx() + 2.dp.toPx() * emphasis
        val amplitude = (3.5f + 1.5f * emphasis).dp.toPx()
        val waveLength = 26.dp.toPx()
        val waveEnd = (thumbX - gap).coerceAtLeast(start)

        // Волна слева от ползунка
        val path = androidx.compose.ui.graphics.Path()
        val step = 2.dp.toPx()
        var x = start
        var first = true
        while (x < waveEnd) {
            val y = cy + kotlin.math.sin(((x - start) / waveLength) * (Math.PI * 2.0).toFloat() - phase) * amplitude
            if (first) path.moveTo(x, y) else path.lineTo(x, y)
            first = false
            x += step
        }
        val lastY = cy + kotlin.math.sin(((waveEnd - start) / waveLength) * (Math.PI * 2.0).toFloat() - phase) * amplitude
        if (first) path.moveTo(waveEnd, lastY) else path.lineTo(waveEnd, lastY)
        drawPath(
            path, color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )

        // Прямая линия справа от ползунка
        val lineStart = (thumbX + gap).coerceAtMost(end)
        if (lineStart < end) {
            drawLine(
                color.copy(alpha = 0.28f), Offset(lineStart, cy), Offset(end, cy),
                strokeWidth = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // Ползунок: тонкая вертикальная палочка
        val thumbWidth = 4.dp.toPx()
        val thumbHeight = (28f + 4f * emphasis).dp.toPx()
        drawRoundRect(
            color,
            topLeft = Offset(thumbX - thumbWidth / 2f, cy - thumbHeight / 2f),
            size = androidx.compose.ui.geometry.Size(thumbWidth, thumbHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(thumbWidth / 2f)
        )
    }
}
