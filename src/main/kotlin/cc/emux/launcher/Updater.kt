package cc.emux.launcher

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*

class Updater(val site: URI) {
	/** Get the hash for the latest version */
	fun getLatestVersion() = site.resolve("/versions/latest").toURL().openStream().reader().readText().trim()

	/** Download the given version to the given location */
	fun downloadVersion(version: EmuVersion, destination: Path): Boolean {
		destination.parent.toFile().mkdirs()

		return try {
			Files.newOutputStream(destination, CREATE, TRUNCATE_EXISTING).use { output ->
				val uri = site.resolve("/versions/").resolve(version.path)
				println("Downloading emulator from $uri")
				uri.toURL().openStream().buffered().use { input ->
					input.copyTo(output)
				}
			}

			true
		} catch (e: Exception) {
			e.printStackTrace()

			false
		}
	}
}