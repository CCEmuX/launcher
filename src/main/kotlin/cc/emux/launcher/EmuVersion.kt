package cc.emux.launcher

data class EmuVersion(
		/** Build hash of this version */
		val hash: String,

		/** Build artifact */
		val build: String
) {
	/** File/download path for this build */
	val path = "$hash/CCEmuX-$build.jar"
}