object Constants {
    const val ARCH_DETECT_CONFIG = "archDetectConfiguration"

    const val SNAPSHOTS_REPO = "mavenCentralSnapshots"
    const val RELEASES_REPO = "mavenLocal"

    val CI = System.getenv("CI") != null
}
