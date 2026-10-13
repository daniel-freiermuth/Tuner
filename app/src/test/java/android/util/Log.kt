package android.util

/** Test-only replacement for the android.jar stub, which throws "not mocked" on every call.
 * Lets JVM unit tests run code that allocates through MemoryPool (which logs via Log.v).
 */
@Suppress("unused")
object Log {
    @JvmStatic fun v(tag: String?, msg: String?): Int = 0

    @JvmStatic fun d(tag: String?, msg: String?): Int = 0

    @JvmStatic fun i(tag: String?, msg: String?): Int = 0

    @JvmStatic fun w(tag: String?, msg: String?): Int = 0

    @JvmStatic fun e(tag: String?, msg: String?): Int = 0
}
