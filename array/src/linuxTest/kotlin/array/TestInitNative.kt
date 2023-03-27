package array

actual fun nativeTestInit() {
}

actual fun tryGc() {
    kotlin.native.internal.GC.collect()
}
