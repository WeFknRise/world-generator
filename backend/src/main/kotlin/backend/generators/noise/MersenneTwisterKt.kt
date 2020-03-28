/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class MersenneTwisterKt(seed: Long) {
    private var mt: LongArray = LongArray(N)
    private var mti: Int = N + 1 //mti == n + 1 means mt[n] is not initialized

    init {
        initGenRand(seed)
    }

    fun initGenRand(seed: Long) {
        // we use a long masked by 0xffffffffL as a poor man unsigned int
        // NB: unlike original C code, we are working with java longs, the cast below makes masking unnecessary
        mt[0] = seed and 0xffffffffL

        mti = 1
        while (mti < N) {
            // See Knuth TAOCP Vol2. 3rd Ed. P.106 for multiplier.
            // initializer from the 2002-01-09 C version by Makoto Matsumoto
            mt[mti] = (1812433253L * (mt[mti - 1] xor (mt[mti - 1] shr 30)) + mti)
            mt[mti] = mt[mti] and 0xffffffffL
            ++mti
        }
    }

    fun nextInt(lower: Int, upper: Int): Int {
        val minRange = 0
        val maxRange = 0xffffffff
        val range = upper - lower
        val eRange = range + 1
        val scaling = maxRange / eRange
        val past = eRange * scaling
        var ret: Long

        do {
            ret = next() - minRange
        } while (ret >= past)
        ret /= scaling

        return (ret + lower).toInt()
    }

    fun next(): Long {
        var y: Long

        if (mti >= N) { // generate N words at one time

            if (mti == N + 1) initGenRand(5489L)

            for (k in 0 until (N - M)) {
                y = (mt[k] and UPPER_MASK) or (mt[k + 1] and LOWER_MASK)

                val i = (y and 0x1L).toInt()
                mt[k] = mt[k + M] xor (y ushr 1) xor MAG01[i]
            }

            for (k in (N - M) until (N - 1)) {
                y = (mt[k] and UPPER_MASK) or (mt[k + 1] and LOWER_MASK)

                val i = (y and 0x1L).toInt()
                mt[k] = mt[k + (M - N)] xor (y ushr 1) xor MAG01[i]
            }

            y = (mt[N - 1] and UPPER_MASK) or (mt[0] and LOWER_MASK)

            val i = (y and 0x1L).toInt()
            mt[N - 1] = mt[M - 1] xor (y ushr 1) xor MAG01[i]

            mti = 0
        }

        y = mt[mti++]

        // tempering
        y = y xor (y ushr 11)
        y = y xor ((y shl 7) and B)
        y = y xor ((y shl 15) and C)
        y = y xor (y ushr 18)

        return y
    }

    companion object {
        private const val N = 624
        private const val M = 397

        private val B = 0x9d2c5680L
        private val C = 0xefc60000L

        private const val MATRIX_A = 0x9908b0dfL
        private const val UPPER_MASK = 0x80000000L
        private const val LOWER_MASK = 0x7fffffffL

        private val MAG01 = longArrayOf(0x0, MATRIX_A)
    }
}