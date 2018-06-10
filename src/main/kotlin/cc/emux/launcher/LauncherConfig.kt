package cc.emux.launcher

import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Launcher configuration - which build to use, etc */
object LauncherConfig {
	private val userConfig by lazy { dataDir.resolve("launcher.properties") }

	private val props by lazy {
		val defaults = Properties()
		LauncherConfig::class.java.getResourceAsStream("/launcher.properties")?.also { defaults.load(it) }

		return@lazy Properties(defaults).apply {
			if (Files.isRegularFile(userConfig)) load(Files.newBufferedReader(userConfig))
		}
	}

	/** Saves properties to user config file */
	fun save() {
		if (!Files.exists(userConfig.parent)) {
			userConfig.parent.toFile().mkdirs()
		}

		props.store(Files.newBufferedWriter(userConfig, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), "CCEmuX Launcher Config")
	}

	// Probably overkill
	private fun delegate(name: String) = object : ReadWriteProperty<Any?, String?> {
		override fun getValue(thisRef: Any?, property: KProperty<*>) = props.getProperty(name)

		override fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
			when (value) {
				null -> props.remove(name)
				else -> props.setProperty(name, value)
			}

			save()
		}
	}

	/** The selected build - e.g. cc or cct */
	var build by delegate("build")

	/** The version hash for the currently-used version */
	var version by delegate("version")

	/** If set, the site to use for automatic updates */
	var updateSite by delegate("updateSite")
}