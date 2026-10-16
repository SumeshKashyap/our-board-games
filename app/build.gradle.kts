import java.net.URI
import java.security.MessageDigest
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

// Version comes from the release tag in CI, e.g. -PappVersion=1.2.3 (tag v1.2.3).
val appVersion = (findProperty("appVersion") as String?) ?: "1.0.0-dev"
val appVersionCode = Regex("""^(\d+)\.(\d+)\.(\d+)""").find(appVersion)
    ?.destructured?.let { (major, minor, patch) -> major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt() } ?: 1

// Release signing: environment variables (CI) or keystore.properties (local). Neither is committed.
val signingProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}
fun signingValue(env: String, prop: String): String? = System.getenv(env) ?: signingProps.getProperty(prop)
val releaseKeystore = signingValue("OURCHESS_KEYSTORE_FILE", "storeFile")?.let(rootProject::file)?.takeIf { it.exists() }

// --- Stockfish -------------------------------------------------------------------------------
// The official Stockfish release is downloaded at build time (not committed: it is ~100 MB)
// and verified against a pinned SHA-256 before use.

val stockfishVersion = "sf_19"
val stockfishCache = rootProject.layout.projectDirectory.dir(".cache/stockfish")
val stockfishDesktopBinary = stockfishCache.file("desktop/stockfish")
val isLinuxX64 = System.getProperty("os.name") == "Linux" && System.getProperty("os.arch") in setOf("amd64", "x86_64")

fun downloadVerified(asset: String, sha256: String) = tasks.registering {
    val target = stockfishCache.file("$stockfishVersion-$asset")
    outputs.file(target)
    doLast {
        val file = target.asFile
        fun digest() = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
        if (file.exists() && digest() == sha256) return@doLast
        file.parentFile.mkdirs()
        logger.lifecycle("Downloading Stockfish $asset ...")
        URI("https://github.com/official-stockfish/Stockfish/releases/download/$stockfishVersion/$asset")
            .toURL().openStream().use { input -> file.outputStream().use(input::copyTo) }
        check(digest() == sha256) { "Checksum mismatch for $asset; refusing to use it" }
    }
}

val downloadStockfishAndroid by downloadVerified(
    "stockfish-android-arm64-universal.tar.gz", "ebb24051aa4a222b4daaf049b882ecf1163d370c128fe02316602643f4d5e426",
)
val downloadStockfishDesktop by downloadVerified(
    "stockfish-linux-x86-64-universal.tar.gz", "9defc0d4e55d49c65a6d042f3e571a39fcea499ade6dbe741b53b8c65e03611f",
)

/** Packs Stockfish as libstockfish.so so Android installs it as an executable native library. */
val extractStockfishAndroid by tasks.registering(Sync::class) {
    from(downloadStockfishAndroid.map { tarTree(resources.gzip(it.outputs.files.singleFile)) }) {
        include("**/stockfish-android-arm64-universal")
        eachFile { path = "arm64-v8a/libstockfish.so" }
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir("generated/stockfish/jniLibs"))
}

/** Desktop build used only by the engine unit tests. */
val extractStockfishDesktop by tasks.registering(Sync::class) {
    from(downloadStockfishDesktop.map { tarTree(resources.gzip(it.outputs.files.singleFile)) }) {
        include("**/stockfish-linux-x86-64-universal")
        eachFile { path = "stockfish"; permissions { unix("rwxr-xr-x") } }
        includeEmptyDirs = false
    }
    into(stockfishCache.dir("desktop"))
}

android {
    namespace = "com.sumesh.ourchess"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sumesh.ourchess"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersion
        // Stockfish ships for 64-bit ARM phones/tablets; x86_64 emulators fall back to the simple engine.
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = signingValue("OURCHESS_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("OURCHESS_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("OURCHESS_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Without a release key (e.g. a fresh checkout), fall back to the debug key so builds still work.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    sourceSets["main"].jniLibs.srcDir(layout.buildDirectory.dir("generated/stockfish/jniLibs"))
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/familyPhotos"))

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    testOptions {
        // Lets unit tests render real screens (Robolectric) for screenshot checks without a device.
        unitTests.isIncludeAndroidResources = true
        unitTests.all { test ->
            test.systemProperty("stockfish.desktop", stockfishDesktopBinary.asFile.absolutePath)
            if (isLinuxX64) test.dependsOn(extractStockfishDesktop)
        }
    }

    // Stockfish is packaged as libstockfish.so and executed from nativeLibraryDir,
    // which requires the .so files to be extracted on install.
    packaging { jniLibs { useLegacyPackaging = true } }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.chesslib)
    implementation(libs.androidx.exifinterface)
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// --- Family photos for the loading screen ------------------------------------------------------
// Private pictures live in app/images/ (git-ignored, never uploaded). Builds on this computer include
// them; builds elsewhere (e.g. GitHub Actions) simply show a plain loading screen.

val familyPhotos by tasks.registering(Sync::class) {
    from("images") { include("*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG") }
    into(layout.buildDirectory.dir("generated/familyPhotos/loading"))
}

tasks.named("preBuild") { dependsOn(extractStockfishAndroid, familyPhotos) }
