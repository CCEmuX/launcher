@file:JvmName("Launcher")

package cc.emux.launcher

import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarFile
import javax.imageio.ImageIO
import javax.swing.JOptionPane
import javax.swing.UIManager
import kotlin.concurrent.thread

/** CCEmuX data dir */
val dataDir by lazy {
	val os = System.getProperty("os.name")

	when {
		os.startsWith("Windows") -> Paths.get(System.getenv("appdata") ?: System.getProperty("user.home"))
		os.startsWith("Mac") -> Paths.get(System.getProperty("user.home"), "Library", "Application Support")
		os.startsWith("Linux") -> Paths.get(System.getProperty("user.home"), ".local", "share")
		else -> Paths.get(System.getProperty("user.home"))
	}.resolve("ccemux")
}

/** Prompt the user to select the CCEmuX build they want (CC vs CCT) */
fun promptBuild(): String {
	val builds = mapOf(
			"cc" to "ComputerCraft",
			"cct" to "CC-Tweaked"
	).entries.toList()

	val result = JOptionPane.showOptionDialog(
			null,
			"Which CCEmuX build would you like?",
			"CCEmuX build",
			JOptionPane.DEFAULT_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null,
			builds.map { it.value }.toTypedArray(),
			0
	)

	return builds[result].key
}

fun getVersionJar(version: EmuVersion) = dataDir.resolve("versions").resolve(version.path)

fun main(args: Array<String>) {
	println("Starting CCEmuX launcher")

	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

	val updater = LauncherConfig.updateSite?.let(::URI)?.let(::Updater)

	if (updater == null) println("No update site specified, automatic downloads disabled")

	val build = LauncherConfig.build ?: promptBuild().also { LauncherConfig.build = it }
	val hash = LauncherConfig.version ?: updater?.getLatestVersion()?.also { LauncherConfig.version = it }

	if (hash == null) {
		JOptionPane.showMessageDialog(
				null,
				"CCEmuX version could not be determined. Ensure you have an internet connection and an update site set.",
				"Fatal error",
				JOptionPane.ERROR_MESSAGE)

		System.exit(1)
		return
	}

	val version = EmuVersion(hash, build)
	val emuJar = getVersionJar(version)

	println("Emulator version: $version")

	if (!Files.isRegularFile(emuJar)) {
		if (updater == null || !updater.downloadVersion(version, emuJar)) {
			JOptionPane.showMessageDialog(
					null,
					"CCEmuX could not be downloaded. Ensure you have an internet connection and an update site set.",
					"Fatal error",
					JOptionPane.ERROR_MESSAGE
			)

			System.exit(1)
			return
		}
	}

	// setup emulator thread
	val emu = JarFile(emuJar.toFile())
	val mainClassName = checkNotNull(emu.manifest?.mainAttributes?.getValue("Main-Class")) { "No main class found" }
	println("Emulator main class: $mainClassName")

	val classLoader = URLClassLoader(arrayOf(emuJar.toUri().toURL()))
	val mainMethod = classLoader.loadClass(mainClassName).getDeclaredMethod("main", arrayOf<String>()::class.java)

	val emuThread = thread(false, contextClassLoader = classLoader) { mainMethod.invoke(null, arrayOf<String>()) }
	emuThread.setUncaughtExceptionHandler { t, e ->
		e.printStackTrace()
		JOptionPane.showMessageDialog(
				null,
				"CCEmuX has crashed with an unexpected error: $e",
				"Fatal error",
				JOptionPane.ERROR_MESSAGE
		)
	}

	// setup update checker thread
	val updateThread = thread(false) {
		if (updater == null) return@thread

		val trayIcon = if (SystemTray.isSupported()) {
			TrayIcon(ImageIO.read(Updater::class.java.getResource("/tray.png")), "CCEmuX Updater").also {
				SystemTray.getSystemTray().add(it)
			}
		} else null

		trayIcon?.isImageAutoSize = true
		trayIcon?.toolTip = "Checking for CCEmuX updates..."

		val latestHash = updater.getLatestVersion()
		val latestVersion = EmuVersion(latestHash, build)

		if (version != latestVersion) {
			println("Update available: $version -> $latestVersion")
			trayIcon?.toolTip = "Applying CCEmuX update..."

			// download and apply update
			if (updater.downloadVersion(latestVersion, getVersionJar(latestVersion))) {
				LauncherConfig.version = latestHash
				println("Update applied")
				trayIcon?.toolTip = "CCEmuX update complete - restart to apply"

				trayIcon?.displayMessage(
						"CCEmuX updated",
						"CCEmuX has been updated successfully. Restart to apply changes.",
						TrayIcon.MessageType.INFO
				)
			}
		} else {
			trayIcon?.toolTip = "No CCEmuX update available"
		}
	}

	updateThread.isDaemon = true
	updateThread.priority = Thread.MIN_PRIORITY

	emuThread.start()
	updateThread.start()
	emuThread.join()
}
