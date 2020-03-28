import backend.constants.FastNoiseConstants.CELL_2D_X
import backend.constants.FastNoiseConstants.CELL_2D_Y
import backend.constants.FastNoiseConstants.CELL_3D_X
import backend.constants.FastNoiseConstants.CELL_3D_Y
import backend.constants.FastNoiseConstants.CELL_3D_Z
import backend.constants.FastNoiseConstants.CUBIC_2D_BOUNDING
import backend.constants.FastNoiseConstants.CUBIC_3D_BOUNDING
import backend.constants.FastNoiseConstants.F2
import backend.constants.FastNoiseConstants.F3
import backend.constants.FastNoiseConstants.F4
import backend.constants.FastNoiseConstants.G2
import backend.constants.FastNoiseConstants.G3
import backend.constants.FastNoiseConstants.G4
import backend.constants.FastNoiseConstants.GRAD_4
import backend.constants.FastNoiseConstants.GRAD_X
import backend.constants.FastNoiseConstants.GRAD_Y
import backend.constants.FastNoiseConstants.GRAD_Z
import backend.constants.FastNoiseConstants.SIMPLEX_4D
import backend.constants.FastNoiseConstants.VAL_LUT
import backend.enums.noise.*
import backend.enums.noise.CellularDistanceFunction.*
import backend.enums.noise.CellularReturnType.*
import backend.enums.noise.FractalType.*
import backend.enums.noise.Interp.*
import backend.enums.noise.NoiseType.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Class: FastNoiseKt
 * Explanation: Kotlin implementation of FastNoise
 *
 * @author Jarno Michiels
 */
class FastNoiseKt(private var seed: Int = 1337) {
    private var fractalType = FBM
    private var cellularDistanceFunction = Euclidean
    private var cellularReturnType = CellValue
    private var cellularNoiseLookup: FastNoiseKt? = null
    private var frequency = 4f//0.01f
    private var interp = Quintic
    private var noiseType = SimplexFractal
    private var octaves = 3
    private var lacunarity = 2.0f
    private var gain = 0.5f
    private var fractalBounding = 0.0f
    private var gradientPerturbAmp = 1.0f
    private var cellularJitter = 0.45f
    private val defaultRandom: (Int, Int) -> Int = { lower, upper -> Random.nextInt(lower, upper) }
    private var random: (Int, Int) -> Int = defaultRandom
    private val perm = IntArray(512)
    private val permMod12 = IntArray(512)
    private val mt = MersenneTwisterKt(seed.toLong())
    private val mtNext: (Int, Int) -> Int = { lower, upper -> mt.nextInt(lower, upper) }

    init {
        setSeed(seed)
        calculateFractalBounding()
    }

    // Returns a 0 double
    fun getDecimalType(): Float {
        return 0.0f
    }

    // Sets seed used for all noise types
    // Default: 1337
    fun setSeed(seed: Int) {
        this.seed = seed
        mt.initGenRand(seed.toLong())
        random = if (seed != 0) mtNext else defaultRandom
        buildPermutationTables(perm, permMod12, random)
    }

    // Sets frequency for all noise types
    // Default: 0.01
    fun setFrequency(frequency: Float) {
        this.frequency = frequency
    }

    // Changes the interpolation method used to smooth between noise values
    // Possible interpolation methods (lowest to highest quality) :
    // - Linear
    // - Hermite
    // - Quintic
    // Used in Value, Gradient Noise and Position Perturbing
    // Default: Quintic
    fun setInterp(interp: Interp) {
        this.interp = interp
    }

    // Sets noise return type of GetNoise(...)
    // Default: Simplex
    fun setNoiseType(noiseType: NoiseType) {
        this.noiseType = noiseType
    }

    // Sets octave count for all fractal noise types
    // Default: 3
    fun setFractalOctaves(octaves: Int, calcBounding: Boolean = true) {
        this.octaves = octaves
        if (calcBounding) calculateFractalBounding()
    }

    // Sets octave lacunarity for all fractal noise types
    // Default: 2.0
    fun setFractalLacunarity(lacunarity: Float) {
        this.lacunarity = lacunarity
    }

    // Sets octave gain for all fractal noise types
    // Default: 0.5
    fun setFractalGain(gain: Float, calcBounding: Boolean = true) {
        this.gain = gain
        if (calcBounding) calculateFractalBounding()
    }

    // Sets the maximum perturb distance from original location when using GradientPerturb{Fractal}(...)
    // Default: 1.0
    fun setGradientPerturbAmp(gradientPerturbAmp: Float) {
        this.gradientPerturbAmp = gradientPerturbAmp
    }

    // Noise used to calculate a cell value if cellular return type is NoiseLookup
    // The lookup value is acquired through GetNoise() so ensure you SetNoiseType() on the noise lookup, value, gradient or simplex is recommended
    fun setCellularNoiseLookup(noise: FastNoiseKt) {
        cellularNoiseLookup = noise
    }

    // Sets method for combining octaves in all fractal noise types
    // Default: FBM
    fun setFractalType(fractalType: FractalType) {
        this.fractalType = fractalType
    }

    // Sets return type from cellular noise calculations
    // Note: NoiseLookup requires another FastNoise object be set with SetCellularNoiseLookup() to function
    // Default: CellValue
    fun setCellularDistanceFunction(cellularDistanceFunction: CellularDistanceFunction) {
        this.cellularDistanceFunction = cellularDistanceFunction
    }

    // Sets distance function used in cellular noise calculations
    // Default: Euclidean
    fun setCellularReturnType(cellularReturnType: CellularReturnType) {
        this.cellularReturnType = cellularReturnType
    }

    // Sets the maximum distance a cellular point can move from it's grid position
    // Setting this high will make artifacts more common
    // Default: 0.45
    fun setCellularJitter(cellularJitter: Float) {
        this.cellularJitter = cellularJitter
    }

    // Sets the maximum distance a cellular point can move from it's grid position
    // Setting this high will make artifacts more common
    // Default: 0.45
    fun setFractalBounding(fractalBounding: Float) {
        this.fractalBounding = fractalBounding
    }

    private fun fastFloor(f: Float): Int = if (f >= 0) f.toInt() else f.toInt() - 1
    private fun fastRound(f: Float): Int = if (f >= 0) (f + 0.5f).toInt() else (f - 0.5f).toInt()
    private fun lerp(a: Float, b: Float, t: Float): Float = a + t * (b - a)
    private fun interpHermiteFunc(t: Float): Float = t * t * (3 - 2 * t)
    private fun interpQuinticFunc(t: Float): Float = t * t * t * (t * (t * 6 - 15) + 10)

    private fun cubicLerp(a: Float, b: Float, c: Float, d: Float, t: Float): Float {
        val p = d - c - (a - b)
        return t * t * t * p + t * t * (a - b - p) + t * (c - a) + b
    }

    private fun calculateFractalBounding() {
        var amp = gain
        var ampFractal = 1.0f
        for (i in 1 until octaves) {
            ampFractal += amp
            amp *= gain
        }
        fractalBounding = 1.0f / ampFractal
    }

    fun buildPermutationTables(perm: IntArray, perm12: IntArray, random: (Int, Int) -> Int) {
        for (i in 0 until 256) perm[i] = i

        for (i in 0 until 256) {
            val r = (i + random(0, (256 - i)))

            val aux = perm[i]
            perm[i] = perm[r]
            perm[i + 256] = perm[r]
            perm[r] = aux

            perm12[i] = perm[i] % 12
            perm12[i + 256] = perm[i] % 12
        }
    }

    private fun index2D_12(offset: Int, x: Int, y: Int): Int = permMod12[(x and 0xff) + perm[(y and 0xff) + offset]]
    private fun index3D_12(offset: Int, x: Int, y: Int, z: Int): Int = permMod12[(x and 0xff) + perm[(y and 0xff) + perm[(z and 0xff) + offset]]]
    private fun index4D_32(offset: Int, x: Int, y: Int, z: Int, w: Int): Int = perm[(x and 0xff) + perm[(y and 0xff) + perm[(z and 0xff) + perm[(w and 0xff) + offset]]]] and 31

    private fun index2D_256(offset: Int, x: Int, y: Int): Int = perm[(x and 0xff) + perm[(y and 0xff) + offset]]
    private fun index3D_256(offset: Int, x: Int, y: Int, z: Int): Int = perm[(x and 0xff) + perm[(y and 0xff) + perm[(z and 0xff) + offset]]]
    private fun index4D_256(offset: Int, x: Int, y: Int, z: Int, w: Int): Int = perm[(x and 0xff) + perm[(y and 0xff) + perm[(z and 0xff) + perm[(w and 0xff) + offset]]]]

    fun getNoise(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        x1 *= frequency
        y1 *= frequency
        z1 *= frequency

        return when (noiseType) {
            Value          -> singleValue(0, x1, y1, z1)
            ValueFractal   -> when (fractalType) {
                FBM        -> singleValueFractalFBM(x1, y1, z1)
                Billow     -> singleValueFractalBillow(x1, y1, z1)
                RigidMulti -> singleValueFractalRigidMulti(x1, y1, z1)
            }
            Perlin         -> singlePerlin(0, x1, y1, z1)
            PerlinFractal  -> when (fractalType) {
                FBM        -> singlePerlinFractalFBM(x1, y1, z1)
                Billow     -> singlePerlinFractalBillow(x1, y1, z1)
                RigidMulti -> singlePerlinFractalRigidMulti(x1, y1, z1)
            }
            Simplex        -> singleSimplex(0, x1, y1, z1)
            SimplexFractal -> when (fractalType) {
                FBM        -> singleSimplexFractalFBM(x1, y1, z1)
                Billow     -> singleSimplexFractalBillow(x1, y1, z1)
                RigidMulti -> singleSimplexFractalRigidMulti(x1, y1, z1)
            }
            Cellular       -> when (cellularReturnType) {
                CellValue, NoiseLookup, Distance -> singleCellular(x1, y1, z1)
                else                             -> singleCellular2Edge(x1, y1, z1)
            }
            WhiteNoise     -> getWhiteNoise(x1, y1, z1)
            Cubic          -> singleCubic(0, x1, y1, z1)
            CubicFractal   -> when (fractalType) {
                FBM        -> singleCubicFractalFBM(x1, y1, z1)
                Billow     -> singleCubicFractalBillow(x1, y1, z1)
                RigidMulti -> singleCubicFractalRigidMulti(x1, y1, z1)
            }
        }
    }

    fun getNoise(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        x1 *= frequency
        y1 *= frequency

        return when (noiseType) {
            Value          -> singleValue(0, x1, y1)
            ValueFractal   -> when (fractalType) {
                FBM        -> singleValueFractalFBM(x1, y1)
                Billow     -> singleValueFractalBillow(x1, y1)
                RigidMulti -> singleValueFractalRigidMulti(x1, y1)
            }
            Perlin         -> singlePerlin(0, x1, y1)
            PerlinFractal  -> when (fractalType) {
                FBM        -> singlePerlinFractalFBM(x1, y1)
                Billow     -> singlePerlinFractalBillow(x1, y1)
                RigidMulti -> singlePerlinFractalRigidMulti(x1, y1)
            }
            Simplex        -> singleSimplex(0, x1, y1)
            SimplexFractal -> when (fractalType) {
                FBM        -> singleSimplexFractalFBM(x1, y1)
                Billow     -> singleSimplexFractalBillow(x1, y1)
                RigidMulti -> singleSimplexFractalRigidMulti(x1, y1)
            }
            Cellular       -> when (cellularReturnType) {
                CellValue, NoiseLookup, Distance -> singleCellular(x1, y1)
                else                             -> singleCellular2Edge(x1, y1)
            }
            WhiteNoise     -> getWhiteNoise(x1, y1)
            Cubic          -> singleCubic(0, x1, y1)
            CubicFractal   -> when (fractalType) {
                FBM        -> singleCubicFractalFBM(x1, y1)
                Billow     -> singleCubicFractalBillow(x1, y1)
                RigidMulti -> singleCubicFractalRigidMulti(x1, y1)
            }
        }
    }

    // White Noise
    private fun floatCast2Int(d: Float): Int {
        val i = d.toRawBits()
        return i xor (i shr 16)
    }

    fun getWhiteNoise(x: Float, y: Float, z: Float, w: Float): Float {
        val xi: Int = floatCast2Int(x)
        val yi: Int = floatCast2Int(y)
        val zi: Int = floatCast2Int(z)
        val wi: Int = floatCast2Int(w)
        return valCoord4D(seed, xi, yi, zi, wi)
    }

    fun getWhiteNoise(x: Float, y: Float, z: Float): Float {
        val xi: Int = floatCast2Int(x)
        val yi: Int = floatCast2Int(y)
        val zi: Int = floatCast2Int(z)
        return valCoord3D(seed, xi, yi, zi)
    }

    fun getWhiteNoise(x: Float, y: Float): Float {
        val xi: Int = floatCast2Int(x)
        val yi: Int = floatCast2Int(y)
        return valCoord2D(seed, xi, yi)
    }

    fun getWhiteNoiseInt(x: Int, y: Int, z: Int, w: Int): Float = valCoord4D(seed, x, y, z, w)
    fun getWhiteNoiseInt(x: Int, y: Int, z: Int): Float = valCoord3D(seed, x, y, z)
    fun getWhiteNoiseInt(x: Int, y: Int): Float = valCoord2D(seed, x, y)

    // Value Noise
    fun getValueFractal(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        x1 *= frequency
        y1 *= frequency
        z1 *= frequency

        return when (fractalType) {
            FBM        -> singleValueFractalFBM(x1, y1, z1)
            Billow     -> singleValueFractalBillow(x1, y1, z1)
            RigidMulti -> singleValueFractalRigidMulti(x1, y1, z1)
        }
    }

    private fun singleValueFractalFBM(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        var sum: Float = singleValue(perm[0], x1, y1, z1)
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            z1 *= lacunarity
            amp *= gain
            sum += singleValue(perm[i], x1, y1, z1) * amp
        }

        return (sum * fractalBounding).toFloat()
    }

    private fun singleValueFractalBillow(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        var sum: Float = abs(singleValue(perm[0], x1, y1, z1)) * 2 - 1
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            z1 *= lacunarity
            amp *= gain
            sum += (abs(singleValue(perm[i], x1, y1, z1)) * 2 - 1) * amp
        }

        return (sum * fractalBounding)
    }

    private fun singleValueFractalRigidMulti(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        var sum: Float = 1 - abs(singleValue(perm[0], x1, y1, z1))
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            z1 *= lacunarity
            amp *= gain
            sum -= (1 - abs(singleValue(perm[i], x1, y1, z1))) * amp
        }

        return sum
    }

    fun getValue(x: Float, y: Float, z: Float): Float {
        return singleValue(0, x * frequency, y * frequency, z * frequency)
    }

    private fun singleValue(offset: Int, x: Float, y: Float, z: Float): Float {
        val x0 = fastFloor(x)
        val y0 = fastFloor(y)
        val z0 = fastFloor(z)
        val x1 = x0 + 1
        val y1 = y0 + 1
        val z1 = z0 + 1
        val xs: Float
        val ys: Float
        val zs: Float

        when (interp) {
            Linear  -> {
                xs = x - x0
                ys = y - y0
                zs = z - z0
            }
            Hermite -> {
                xs = interpHermiteFunc(x - x0)
                ys = interpHermiteFunc(y - y0)
                zs = interpHermiteFunc(z - z0)
            }
            Quintic -> {
                xs = interpQuinticFunc(x - x0)
                ys = interpQuinticFunc(y - y0)
                zs = interpQuinticFunc(z - z0)
            }
        }

        val xf00 = lerp(valCoord3DFast(offset, x0, y0, z0), valCoord3DFast(offset, x1, y0, z0), xs)
        val xf10 = lerp(valCoord3DFast(offset, x0, y1, z0), valCoord3DFast(offset, x1, y1, z0), xs)
        val xf01 = lerp(valCoord3DFast(offset, x0, y0, z1), valCoord3DFast(offset, x1, y0, z1), xs)
        val xf11 = lerp(valCoord3DFast(offset, x0, y1, z1), valCoord3DFast(offset, x1, y1, z1), xs)

        val yf0 = lerp(xf00, xf10, ys)
        val yf1 = lerp(xf01, xf11, ys)

        return lerp(yf0, yf1, zs)
    }

    fun getValueFractal(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        x1 *= frequency
        y1 *= frequency

        return when (fractalType) {
            FBM        -> singleValueFractalFBM(x1, y1)
            Billow     -> singleValueFractalBillow(x1, y1)
            RigidMulti -> singleValueFractalRigidMulti(x1, y1)
        }
    }

    private fun singleValueFractalFBM(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        var sum = singleValue(perm[0], x1, y1)
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            amp *= gain
            sum += singleValue(perm[i], x1, y1) * amp
        }

        return (sum * fractalBounding).toFloat()
    }

    private fun singleValueFractalBillow(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        var sum = abs(singleValue(perm[0], x1, y1)) * 2 - 1
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            amp *= gain
            sum += (abs(singleValue(perm[i], x1, y1)) * 2 - 1) * amp
        }

        return (sum * fractalBounding).toFloat()
    }

    private fun singleValueFractalRigidMulti(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        var sum = 1 - abs(singleValue(perm[0], x1, y1))
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            amp *= gain
            sum -= (1 - abs(singleValue(perm[i], x1, y1))) * amp
        }

        return sum
    }

    fun getValue(x: Float, y: Float): Float {
        return singleValue(0, x * frequency, y * frequency)
    }

    private fun singleValue(offset: Int, x: Float, y: Float): Float {
        val x0 = fastFloor(x)
        val y0 = fastFloor(y)
        val x1 = x0 + 1
        val y1 = y0 + 1
        val xs: Float
        val ys: Float

        when (interp) {
            Linear  -> {
                xs = x - x0
                ys = y - y0
            }
            Hermite -> {
                xs = interpHermiteFunc(x - x0)
                ys = interpHermiteFunc(y - y0)
            }
            Quintic -> {
                xs = interpQuinticFunc(x - x0)
                ys = interpQuinticFunc(y - y0)
            }
        }

        val xf0 = lerp(valCoord2DFast(offset, x0, y0), valCoord2DFast(offset, x1, y0), xs)
        val xf1 = lerp(valCoord2DFast(offset, x0, y1), valCoord2DFast(offset, x1, y1), xs)

        return lerp(xf0, xf1, ys)
    }

    // Gradient Noise
    fun getPerlinFractal(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        x1 *= frequency
        y1 *= frequency
        z1 *= frequency

        return when (fractalType) {
            FBM        -> singlePerlinFractalFBM(x1, y1, z1)
            Billow     -> singlePerlinFractalBillow(x1, y1, z1)
            RigidMulti -> singlePerlinFractalRigidMulti(x1, y1, z1)
        }
    }

    private fun singlePerlinFractalFBM(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        var sum = singlePerlin(perm[0], x1, y1, z1)
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            z1 *= lacunarity
            amp *= gain
            sum += singlePerlin(perm[i], x1, y1, z1) * amp
        }

        return (sum * fractalBounding)
    }

    private fun singlePerlinFractalBillow(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        var sum = abs(singlePerlin(perm[0], x1, y1, z1)) * 2 - 1
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            z1 *= lacunarity
            amp *= gain
            sum += (abs(singlePerlin(perm[i], x1, y1, z1)) * 2 - 1) * amp
        }

        return (sum * fractalBounding)
    }

    private fun singlePerlinFractalRigidMulti(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        var sum = 1 - abs(singlePerlin(perm[0], x1, y1, z1))
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            z1 *= lacunarity
            amp *= gain
            sum -= (1 - abs(singlePerlin(perm[i], x1, y1, z1))) * amp
        }

        return sum
    }

    fun getPerlin(x: Float, y: Float, z: Float): Float {
        return singlePerlin(0, x * frequency, y * frequency, z * frequency)
    }

    private fun singlePerlin(offset: Int, x: Float, y: Float, z: Float): Float {
        val x0 = fastFloor(x)
        val y0 = fastFloor(y)
        val z0 = fastFloor(z)
        val x1 = x0 + 1
        val y1 = y0 + 1
        val z1 = z0 + 1
        val xs: Float
        val ys: Float
        val zs: Float

        when (interp) {
            Linear  -> {
                xs = x - x0
                ys = y - y0
                zs = z - z0
            }
            Hermite -> {
                xs = interpHermiteFunc(x - x0)
                ys = interpHermiteFunc(y - y0)
                zs = interpHermiteFunc(z - z0)
            }
            Quintic -> {
                xs = interpQuinticFunc(x - x0)
                ys = interpQuinticFunc(y - y0)
                zs = interpQuinticFunc(z - z0)
            }
        }

        val xd0 = x - x0
        val yd0 = y - y0
        val zd0 = z - z0
        val xd1 = xd0 - 1
        val yd1 = yd0 - 1
        val zd1 = zd0 - 1
        val xf00 = lerp(gradCoord3D(offset, x0, y0, z0, xd0, yd0, zd0), gradCoord3D(offset, x1, y0, z0, xd1, yd0, zd0), xs)
        val xf10 = lerp(gradCoord3D(offset, x0, y1, z0, xd0, yd1, zd0), gradCoord3D(offset, x1, y1, z0, xd1, yd1, zd0), xs)
        val xf01 = lerp(gradCoord3D(offset, x0, y0, z1, xd0, yd0, zd1), gradCoord3D(offset, x1, y0, z1, xd1, yd0, zd1), xs)
        val xf11 = lerp(gradCoord3D(offset, x0, y1, z1, xd0, yd1, zd1), gradCoord3D(offset, x1, y1, z1, xd1, yd1, zd1), xs)

        val yf0 = lerp(xf00, xf10, ys)
        val yf1 = lerp(xf01, xf11, ys)

        return lerp(yf0, yf1, zs)
    }

    fun getPerlinFractal(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        x1 *= frequency
        y1 *= frequency

        return when (fractalType) {
            FBM        -> singlePerlinFractalFBM(x1, y1)
            Billow     -> singlePerlinFractalBillow(x1, y1)
            RigidMulti -> singlePerlinFractalRigidMulti(x1, y1)
        }
    }

    private fun singlePerlinFractalFBM(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        var sum = singlePerlin(perm[0], x1, y1)
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            amp *= gain
            sum += singlePerlin(perm[i], x1, y1) * amp
        }

        return (sum * fractalBounding)
    }

    private fun singlePerlinFractalBillow(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        var sum = abs(singlePerlin(perm[0], x1, y1)) * 2 - 1
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            amp *= gain
            sum += (abs(singlePerlin(perm[i], x1, y1)) * 2 - 1) * amp
        }

        return (sum * fractalBounding)
    }

    private fun singlePerlinFractalRigidMulti(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        var sum = 1 - abs(singlePerlin(perm[0], x1, y1))
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            amp *= gain
            sum -= (1 - abs(singlePerlin(perm[i], x1, y1))) * amp
        }

        return sum
    }

    fun getPerlin(x: Float, y: Float): Float {
        return singlePerlin(0, x * frequency, y * frequency)
    }

    private fun singlePerlin(offset: Int, x: Float, y: Float): Float {
        val x0 = fastFloor(x)
        val y0 = fastFloor(y)
        val x1 = x0 + 1
        val y1 = y0 + 1
        val xs: Float
        val ys: Float

        when (interp) {
            Linear  -> {
                xs = x - x0
                ys = y - y0
            }
            Hermite -> {
                xs = interpHermiteFunc(x - x0)
                ys = interpHermiteFunc(y - y0)
            }
            Quintic -> {
                xs = interpQuinticFunc(x - x0)
                ys = interpQuinticFunc(y - y0)
            }
        }

        val xd0 = x - x0
        val yd0 = y - y0
        val xd1 = xd0 - 1
        val yd1 = yd0 - 1
        val xf0 = lerp(gradCoord2D(offset, x0, y0, xd0, yd0), gradCoord2D(offset, x1, y0, xd1, yd0), xs)
        val xf1 = lerp(gradCoord2D(offset, x0, y1, xd0, yd1), gradCoord2D(offset, x1, y1, xd1, yd1), xs)

        return lerp(xf0, xf1, ys)
    }

    // Simplex Noise
    fun getSimplexFractal(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        x1 *= frequency
        y1 *= frequency
        z1 *= frequency

        return when (fractalType) {
            FBM        -> singleSimplexFractalFBM(x1, y1, z1)
            Billow     -> singleSimplexFractalBillow(x1, y1, z1)
            RigidMulti -> singleSimplexFractalRigidMulti(x1, y1, z1)
        }
    }

    private fun singleSimplexFractalFBM(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        var sum = singleSimplex(perm[0], x1, y1, z1)
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            z1 *= lacunarity
            amp *= gain
            sum += singleSimplex(perm[i], x1, y1, z1) * amp
        }

        return (sum * fractalBounding)
    }

    private fun singleSimplexFractalBillow(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        var sum = abs(singleSimplex(perm[0], x1, y1, z1)) * 2 - 1
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            z1 *= lacunarity
            amp *= gain
            sum += (abs(singleSimplex(perm[i], x1, y1, z1)) * 2 - 1) * amp
        }

        return (sum * fractalBounding)
    }

    private fun singleSimplexFractalRigidMulti(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        var sum = 1 - abs(singleSimplex(perm[0], x1, y1, z1))
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            z1 *= lacunarity
            amp *= gain
            sum -= (1 - abs(singleSimplex(perm[i], x1, y1, z1))) * amp
        }

        return sum
    }

    fun getSimplex(x: Float, y: Float, z: Float): Float {
        return singleSimplex(0, x * frequency, y * frequency, z * frequency)
    }

    private fun singleSimplex(offset: Int, x: Float, y: Float, z: Float): Float {
        var t = (x + y + z) * F3
        val i = fastFloor(x + t)
        val j = fastFloor(y + t)
        val k = fastFloor(z + t)
        t = (i + j + k) * G3
        val x0 = x - (i - t)
        val y0 = y - (j - t)
        val z0 = z - (k - t)
        val i1: Int
        val j1: Int
        val k1: Int
        val i2: Int
        val j2: Int
        val k2: Int
        if (x0 >= y0) {
            if (y0 >= z0) {
                i1 = 1
                j1 = 0
                k1 = 0
                i2 = 1
                j2 = 1
                k2 = 0
            } else if (x0 >= z0) {
                i1 = 1
                j1 = 0
                k1 = 0
                i2 = 1
                j2 = 0
                k2 = 1
            } else  // x0 < z0
            {
                i1 = 0
                j1 = 0
                k1 = 1
                i2 = 1
                j2 = 0
                k2 = 1
            }
        } else  // x0 < y0
        {
            if (y0 < z0) {
                i1 = 0
                j1 = 0
                k1 = 1
                i2 = 0
                j2 = 1
                k2 = 1
            } else if (x0 < z0) {
                i1 = 0
                j1 = 1
                k1 = 0
                i2 = 0
                j2 = 1
                k2 = 1
            } else  // x0 >= z0
            {
                i1 = 0
                j1 = 1
                k1 = 0
                i2 = 1
                j2 = 1
                k2 = 0
            }
        }
        val x1 = x0 - i1 + G3
        val y1 = y0 - j1 + G3
        val z1 = z0 - k1 + G3
        val x2 = x0 - i2 + F3
        val y2 = y0 - j2 + F3
        val z2 = z0 - k2 + F3
        val x3 = x0 - 1f + 3f * G3
        val y3 = y0 - 1f + 3f * G3
        val z3 = z0 - 1f + 3f * G3
        val n0: Float
        val n1: Float
        val n2: Float
        val n3: Float

        t = 0.6.toFloat() - x0 * x0 - y0 * y0 - z0 * z0
        if (t < 0) n0 = 0f else {
            t *= t
            n0 = t * t * gradCoord3D(offset, i, j, k, x0, y0, z0)
        }
        t = 0.6.toFloat() - x1 * x1 - y1 * y1 - z1 * z1
        if (t < 0) n1 = 0f else {
            t *= t
            n1 = t * t * gradCoord3D(offset, i + i1, j + j1, k + k1, x1, y1, z1)
        }
        t = 0.6.toFloat() - x2 * x2 - y2 * y2 - z2 * z2
        if (t < 0) n2 = 0f else {
            t *= t
            n2 = t * t * gradCoord3D(offset, i + i2, j + j2, k + k2, x2, y2, z2)
        }
        t = 0.6.toFloat() - x3 * x3 - y3 * y3 - z3 * z3
        if (t < 0) n3 = 0f else {
            t *= t
            n3 = t * t * gradCoord3D(offset, i + 1, j + 1, k + 1, x3, y3, z3)
        }

        return 32 * (n0 + n1 + n2 + n3)
    }

    fun getSimplexFractal(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        x1 *= frequency
        y1 *= frequency

        return when (fractalType) {
            FBM        -> singleSimplexFractalFBM(x1, y1)
            Billow     -> singleSimplexFractalBillow(x1, y1)
            RigidMulti -> singleSimplexFractalRigidMulti(x1, y1)
        }
    }

    private fun singleSimplexFractalFBM(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        var sum = singleSimplex(perm[0], x1, y1)
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            amp *= gain

            sum += singleSimplex(perm[i], x1, y1) * amp
        }

        return (sum * fractalBounding)
    }

    private fun singleSimplexFractalBillow(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        var sum = abs(singleSimplex(perm[0], x1, y1)) * 2 - 1
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            amp *= gain
            sum += (abs(singleSimplex(perm[i], x1, y1)) * 2 - 1) * amp
        }

        return (sum * fractalBounding)
    }

    private fun singleSimplexFractalRigidMulti(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        var sum = 1 - abs(singleSimplex(perm[0], x1, y1))
        var amp = 1f

        for (i in 1 until octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            amp *= gain
            sum -= (1 - abs(singleSimplex(perm[i], x1, y1))) * amp
        }

        return sum
    }

    fun getSimplex(x: Float, y: Float): Float {
        return singleSimplex(0, x * frequency, y * frequency)
    }

    private fun singleSimplex(offset: Int, x: Float, y: Float): Float {
        var t = (x + y) * F2
        val i = fastFloor(x + t)
        val j = fastFloor(y + t)
        t = (i + j) * G2
        val X0 = i - t
        val Y0 = j - t
        val x0 = x - X0
        val y0 = y - Y0
        val i1: Int
        val j1: Int

        if (x0 > y0) {
            i1 = 1
            j1 = 0
        } else {
            i1 = 0
            j1 = 1
        }

        val x1 = x0 - i1 + G2
        val y1 = y0 - j1 + G2
        val x2 = x0 - 1 + (2 * G2)
        val y2 = y0 - 1 + (2 * G2)
        val n0: Float
        val n1: Float
        val n2: Float

        t = 0.5.toFloat() - x0 * x0 - y0 * y0
        if (t < 0) n0 = 0f else {
            t *= t
            n0 = t * t * gradCoord2D(offset, i, j, x0, y0)
        }

        t = 0.5.toFloat() - x1 * x1 - y1 * y1
        if (t < 0) n1 = 0f else {
            t *= t
            n1 = t * t * gradCoord2D(offset, i + i1, j + j1, x1, y1)
        }

        t = 0.5.toFloat() - x2 * x2 - y2 * y2
        if (t < 0) n2 = 0f else {
            t *= t
            n2 = t * t * gradCoord2D(offset, i + 1, j + 1, x2, y2)
        }

        return 70 * (n0 + n1 + n2)
    }

    fun getSimplex(x: Float, y: Float, z: Float, w: Float): Float {
        return singleSimplex(0, x * frequency, y * frequency, z * frequency, w * frequency)
    }

    private fun singleSimplex(offset: Int, x: Float, y: Float, z: Float, w: Float): Float {
        val n0: Float
        val n1: Float
        val n2: Float
        val n3: Float
        val n4: Float
        var t = (x + y + z + w) * F4
        val i = fastFloor(x + t)
        val j = fastFloor(y + t)
        val k = fastFloor(z + t)
        val l = fastFloor(w + t)
        t = (i + j + k + l) * G4
        val X0 = i - t
        val Y0 = j - t
        val Z0 = k - t
        val W0 = l - t
        val x0 = x - X0
        val y0 = y - Y0
        val z0 = z - Z0
        val w0 = w - W0
        var c = if (x0 > y0) 32 else 0
        c += if (x0 > z0) 16 else 0
        c += if (y0 > z0) 8 else 0
        c += if (x0 > w0) 4 else 0
        c += if (y0 > w0) 2 else 0
        c += if (z0 > w0) 1 else 0
        c = c shl 2
        val i1 = if (SIMPLEX_4D[c] >= 3) 1 else 0
        val i2 = if (SIMPLEX_4D[c] >= 2) 1 else 0
        val i3 = if (SIMPLEX_4D[c++] >= 1) 1 else 0
        val j1 = if (SIMPLEX_4D[c] >= 3) 1 else 0
        val j2 = if (SIMPLEX_4D[c] >= 2) 1 else 0
        val j3 = if (SIMPLEX_4D[c++] >= 1) 1 else 0
        val k1 = if (SIMPLEX_4D[c] >= 3) 1 else 0
        val k2 = if (SIMPLEX_4D[c] >= 2) 1 else 0
        val k3 = if (SIMPLEX_4D[c++] >= 1) 1 else 0
        val l1 = if (SIMPLEX_4D[c] >= 3) 1 else 0
        val l2 = if (SIMPLEX_4D[c] >= 2) 1 else 0
        val l3 = if (SIMPLEX_4D[c] >= 1) 1 else 0
        val x1 = x0 - i1 + G4
        val y1 = y0 - j1 + G4
        val z1 = z0 - k1 + G4
        val w1 = w0 - l1 + G4
        val x2 = x0 - i2 + 2 * G4
        val y2 = y0 - j2 + 2 * G4
        val z2 = z0 - k2 + 2 * G4
        val w2 = w0 - l2 + 2 * G4
        val x3 = x0 - i3 + 3 * G4
        val y3 = y0 - j3 + 3 * G4
        val z3 = z0 - k3 + 3 * G4
        val w3 = w0 - l3 + 3 * G4
        val x4 = x0 - 1 + 4 * G4
        val y4 = y0 - 1 + 4 * G4
        val z4 = z0 - 1 + 4 * G4
        val w4 = w0 - 1 + 4 * G4

        t = 0.6.toFloat() - x0 * x0 - y0 * y0 - z0 * z0 - w0 * w0
        if (t < 0) n0 = 0f else {
            t *= t
            n0 = t * t * gradCoord4D(offset, i, j, k, l, x0, y0, z0, w0)
        }
        t = 0.6.toFloat() - x1 * x1 - y1 * y1 - z1 * z1 - w1 * w1
        if (t < 0) n1 = 0f else {
            t *= t
            n1 = t * t * gradCoord4D(offset, i + i1, j + j1, k + k1, l + l1, x1, y1, z1, w1)
        }
        t = 0.6.toFloat() - x2 * x2 - y2 * y2 - z2 * z2 - w2 * w2
        if (t < 0) n2 = 0f else {
            t *= t
            n2 = t * t * gradCoord4D(offset, i + i2, j + j2, k + k2, l + l2, x2, y2, z2, w2)
        }
        t = 0.6.toFloat() - x3 * x3 - y3 * y3 - z3 * z3 - w3 * w3
        if (t < 0) n3 = 0f else {
            t *= t
            n3 = t * t * gradCoord4D(offset, i + i3, j + j3, k + k3, l + l3, x3, y3, z3, w3)
        }
        t = 0.6.toFloat() - x4 * x4 - y4 * y4 - z4 * z4 - w4 * w4
        if (t < 0) n4 = 0f else {
            t *= t
            n4 = t * t * gradCoord4D(offset, i + 1, j + 1, k + 1, l + 1, x4, y4, z4, w4)
        }

        return 27 * (n0 + n1 + n2 + n3 + n4)
    }

    // Cubic Noise
    fun getCubicFractal(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        x1 *= frequency
        y1 *= frequency
        z1 *= frequency
        return when (fractalType) {
            FBM        -> singleCubicFractalFBM(x1, y1, z1)
            Billow     -> singleCubicFractalBillow(x1, y1, z1)
            RigidMulti -> singleCubicFractalRigidMulti(x1, y1, z1)
        }
    }

    private fun singleCubicFractalFBM(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        var sum = singleCubic(perm[0], x1, y1, z1)
        var amp = 1f
        var i = 0

        while (++i < octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            z1 *= lacunarity
            amp *= gain
            sum += singleCubic(perm[i], x1, y1, z1) * amp
        }

        return (sum * fractalBounding).toFloat()
    }

    private fun singleCubicFractalBillow(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        var sum = abs(singleCubic(perm[0], x1, y1, z1)) * 2 - 1
        var amp = 1f
        var i = 0

        while (++i < octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            z1 *= lacunarity
            amp *= gain
            sum += (abs(singleCubic(perm[i], x1, y1, z1)) * 2 - 1) * amp
        }

        return (sum * fractalBounding)
    }

    private fun singleCubicFractalRigidMulti(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        var sum = 1 - abs(singleCubic(perm[0], x1, y1, z1))
        var amp = 1f
        var i = 0

        while (++i < octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            z1 *= lacunarity
            amp *= gain
            sum -= (1 - abs(singleCubic(perm[i], x1, y1, z1))) * amp
        }

        return sum
    }

    fun getCubic(x: Float, y: Float, z: Float): Float {
        return singleCubic(0, x * frequency, y * frequency, z * frequency)
    }

    private fun singleCubic(offset: Int, x: Float, y: Float, z: Float): Float {
        val x1 = fastFloor(x)
        val y1 = fastFloor(y)
        val z1 = fastFloor(z)
        val x0 = x1 - 1
        val y0 = y1 - 1
        val z0 = z1 - 1
        val x2 = x1 + 1
        val y2 = y1 + 1
        val z2 = z1 + 1
        val x3 = x1 + 2
        val y3 = y1 + 2
        val z3 = z1 + 2
        val xs = x - x1.toFloat()
        val ys = y - y1.toFloat()
        val zs = z - z1.toFloat()
        return cubicLerp(
                cubicLerp(
                        cubicLerp(valCoord3DFast(offset, x0, y0, z0), valCoord3DFast(offset, x1, y0, z0), valCoord3DFast(offset, x2, y0, z0), valCoord3DFast(offset, x3, y0, z0), xs),
                        cubicLerp(valCoord3DFast(offset, x0, y1, z0), valCoord3DFast(offset, x1, y1, z0), valCoord3DFast(offset, x2, y1, z0), valCoord3DFast(offset, x3, y1, z0), xs),
                        cubicLerp(valCoord3DFast(offset, x0, y2, z0), valCoord3DFast(offset, x1, y2, z0), valCoord3DFast(offset, x2, y2, z0), valCoord3DFast(offset, x3, y2, z0), xs),
                        cubicLerp(valCoord3DFast(offset, x0, y3, z0), valCoord3DFast(offset, x1, y3, z0), valCoord3DFast(offset, x2, y3, z0), valCoord3DFast(offset, x3, y3, z0), xs), ys),
                cubicLerp(
                        cubicLerp(valCoord3DFast(offset, x0, y0, z1), valCoord3DFast(offset, x1, y0, z1), valCoord3DFast(offset, x2, y0, z1), valCoord3DFast(offset, x3, y0, z1), xs),
                        cubicLerp(valCoord3DFast(offset, x0, y1, z1), valCoord3DFast(offset, x1, y1, z1), valCoord3DFast(offset, x2, y1, z1), valCoord3DFast(offset, x3, y1, z1), xs),
                        cubicLerp(valCoord3DFast(offset, x0, y2, z1), valCoord3DFast(offset, x1, y2, z1), valCoord3DFast(offset, x2, y2, z1), valCoord3DFast(offset, x3, y2, z1), xs),
                        cubicLerp(valCoord3DFast(offset, x0, y3, z1), valCoord3DFast(offset, x1, y3, z1), valCoord3DFast(offset, x2, y3, z1), valCoord3DFast(offset, x3, y3, z1), xs), ys),
                cubicLerp(
                        cubicLerp(valCoord3DFast(offset, x0, y0, z2), valCoord3DFast(offset, x1, y0, z2), valCoord3DFast(offset, x2, y0, z2), valCoord3DFast(offset, x3, y0, z2), xs),
                        cubicLerp(valCoord3DFast(offset, x0, y1, z2), valCoord3DFast(offset, x1, y1, z2), valCoord3DFast(offset, x2, y1, z2), valCoord3DFast(offset, x3, y1, z2), xs),
                        cubicLerp(valCoord3DFast(offset, x0, y2, z2), valCoord3DFast(offset, x1, y2, z2), valCoord3DFast(offset, x2, y2, z2), valCoord3DFast(offset, x3, y2, z2), xs),
                        cubicLerp(valCoord3DFast(offset, x0, y3, z2), valCoord3DFast(offset, x1, y3, z2), valCoord3DFast(offset, x2, y3, z2), valCoord3DFast(offset, x3, y3, z2), xs), ys),
                cubicLerp(
                        cubicLerp(valCoord3DFast(offset, x0, y0, z3), valCoord3DFast(offset, x1, y0, z3), valCoord3DFast(offset, x2, y0, z3), valCoord3DFast(offset, x3, y0, z3), xs),
                        cubicLerp(valCoord3DFast(offset, x0, y1, z3), valCoord3DFast(offset, x1, y1, z3), valCoord3DFast(offset, x2, y1, z3), valCoord3DFast(offset, x3, y1, z3), xs),
                        cubicLerp(valCoord3DFast(offset, x0, y2, z3), valCoord3DFast(offset, x1, y2, z3), valCoord3DFast(offset, x2, y2, z3), valCoord3DFast(offset, x3, y2, z3), xs),
                        cubicLerp(valCoord3DFast(offset, x0, y3, z3), valCoord3DFast(offset, x1, y3, z3), valCoord3DFast(offset, x2, y3, z3), valCoord3DFast(offset, x3, y3, z3), xs), ys),
                zs) * CUBIC_3D_BOUNDING
    }

    fun getCubicFractal(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        x1 *= frequency
        y1 *= frequency
        return when (fractalType) {
            FBM        -> singleCubicFractalFBM(x1, y1)
            Billow     -> singleCubicFractalBillow(x1, y1)
            RigidMulti -> singleCubicFractalRigidMulti(x1, y1)
        }
    }

    private fun singleCubicFractalFBM(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        var sum = singleCubic(perm[0], x1, y1)
        var amp = 1f
        var i = 0

        while (++i < octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            amp *= gain
            sum += singleCubic(perm[i], x1, y1) * amp
        }

        return (sum * fractalBounding).toFloat()
    }

    private fun singleCubicFractalBillow(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        var sum = abs(singleCubic(perm[0], x1, y1)) * 2 - 1
        var amp = 1f
        var i = 0

        while (++i < octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            amp *= gain
            sum += (abs(singleCubic(perm[i], x1, y1)) * 2 - 1) * amp
        }

        return (sum * fractalBounding)
    }

    private fun singleCubicFractalRigidMulti(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        var sum = 1 - abs(singleCubic(perm[0], x1, y1))
        var amp = 1f
        var i = 0

        while (++i < octaves) {
            x1 *= lacunarity
            y1 *= lacunarity
            amp *= gain
            sum -= (1 - abs(singleCubic(perm[i], x1, y1))) * amp
        }

        return sum
    }

    fun getCubic(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        x1 *= frequency
        y1 *= frequency

        return singleCubic(0, x1, y1)
    }

    private fun singleCubic(offset: Int, x: Float, y: Float): Float {
        val x1 = fastFloor(x)
        val y1 = fastFloor(y)
        val x0 = x1 - 1
        val y0 = y1 - 1
        val x2 = x1 + 1
        val y2 = y1 + 1
        val x3 = x1 + 2
        val y3 = y1 + 2
        val xs = x - x1.toFloat()
        val ys = y - y1.toFloat()

        return cubicLerp(
                cubicLerp(valCoord2DFast(offset, x0, y0), valCoord2DFast(offset, x1, y0), valCoord2DFast(offset, x2, y0), valCoord2DFast(offset, x3, y0), xs),
                cubicLerp(valCoord2DFast(offset, x0, y1), valCoord2DFast(offset, x1, y1), valCoord2DFast(offset, x2, y1), valCoord2DFast(offset, x3, y1), xs),
                cubicLerp(valCoord2DFast(offset, x0, y2), valCoord2DFast(offset, x1, y2), valCoord2DFast(offset, x2, y2), valCoord2DFast(offset, x3, y2), xs),
                cubicLerp(valCoord2DFast(offset, x0, y3), valCoord2DFast(offset, x1, y3), valCoord2DFast(offset, x2, y3), valCoord2DFast(offset, x3, y3), xs),
                ys) * CUBIC_2D_BOUNDING
    }

    // Cellular Noise
    fun getCellular(x: Float, y: Float, z: Float): Float {
        var x1 = x
        var y1 = y
        var z1 = z
        x1 *= frequency
        y1 *= frequency
        z1 *= frequency

        return when (cellularReturnType) {
            CellValue, NoiseLookup, Distance -> singleCellular(x1, y1, z1)
            else                             -> singleCellular2Edge(x1, y1, z1)
        }
    }

    private fun singleCellular(x: Float, y: Float, z: Float): Float {
        val xr = fastRound(x)
        val yr = fastRound(y)
        val zr = fastRound(z)
        var distance = 999999f
        var xc = 0
        var yc = 0
        var zc = 0

        when (cellularDistanceFunction) {
            Euclidean                          -> {
                var xi = xr - 1
                while (xi <= xr + 1) {
                    var yi = yr - 1
                    while (yi <= yr + 1) {
                        var zi = zr - 1
                        while (zi <= zr + 1) {
                            val lutPos = index3D_256(0, xi, yi, zi)
                            val vecX = xi - x + CELL_3D_X[lutPos] * cellularJitter
                            val vecY = yi - y + CELL_3D_Y[lutPos] * cellularJitter
                            val vecZ = zi - z + CELL_3D_Z[lutPos] * cellularJitter
                            val newDistance = vecX * vecX + vecY * vecY + vecZ * vecZ
                            if (newDistance < distance) {
                                distance = newDistance
                                xc = xi
                                yc = yi
                                zc = zi
                            }
                            zi++
                        }
                        yi++
                    }
                    xi++
                }
            }
            Manhattan                        -> {
                var xi = xr - 1
                while (xi <= xr + 1) {
                    var yi = yr - 1
                    while (yi <= yr + 1) {
                        var zi = zr - 1
                        while (zi <= zr + 1) {
                            val lutPos = index3D_256(0, xi, yi, zi)
                            val vecX = xi - x + CELL_3D_X[lutPos] * cellularJitter
                            val vecY = yi - y + CELL_3D_Y[lutPos] * cellularJitter
                            val vecZ = zi - z + CELL_3D_Z[lutPos] * cellularJitter
                            val newDistance = abs(vecX) + abs(vecY) + abs(vecZ)
                            if (newDistance < distance) {
                                distance = newDistance
                                xc = xi
                                yc = yi
                                zc = zi
                            }
                            zi++
                        }
                        yi++
                    }
                    xi++
                }
            }
            Natural -> {
                var xi = xr - 1
                while (xi <= xr + 1) {
                    var yi = yr - 1
                    while (yi <= yr + 1) {
                        var zi = zr - 1
                        while (zi <= zr + 1) {
                            val lutPos = index3D_256(0, xi, yi, zi)
                            val vecX = xi - x + CELL_3D_X[lutPos] * cellularJitter
                            val vecY = yi - y + CELL_3D_Y[lutPos] * cellularJitter
                            val vecZ = zi - z + CELL_3D_Z[lutPos] * cellularJitter
                            val newDistance = abs(vecX) + abs(vecY) + abs(vecZ) + (vecX * vecX + vecY * vecY + vecZ * vecZ)
                            if (newDistance < distance) {
                                distance = newDistance
                                xc = xi
                                yc = yi
                                zc = zi
                            }
                            zi++
                        }
                        yi++
                    }
                    xi++
                }
            }
        }

        return when (cellularReturnType) {
            CellValue   -> valCoord3D(seed, xc, yc, zc)
            NoiseLookup -> {
                val lutPos = index3D_256(0, xc, yc, zc)
                cellularNoiseLookup!!.getNoise(xc + CELL_3D_X[lutPos] * cellularJitter, yc + CELL_3D_Y[lutPos] * cellularJitter, zc + CELL_3D_Z[lutPos] * cellularJitter)
            }
            Distance    -> distance
            else        -> 0f
        }
    }

    private fun singleCellular2Edge(x: Float, y: Float, z: Float): Float {
        val xr = fastRound(x)
        val yr = fastRound(y)
        val zr = fastRound(z)
        var distance = 999999f
        var distance2 = 999999f

        when (cellularDistanceFunction) {
            Euclidean -> {
                var xi = xr - 1
                while (xi <= xr + 1) {
                    var yi = yr - 1
                    while (yi <= yr + 1) {
                        var zi = zr - 1
                        while (zi <= zr + 1) {
                            val lutPos = index3D_256(0, xi, yi, zi)
                            val vecX = xi - x + CELL_3D_X[lutPos] * cellularJitter
                            val vecY = yi - y + CELL_3D_Y[lutPos] * cellularJitter
                            val vecZ = zi - z + CELL_3D_Z[lutPos] * cellularJitter
                            val newDistance = vecX * vecX + vecY * vecY + vecZ * vecZ
                            distance2 = max(min(distance2, newDistance), distance)
                            distance = min(distance, newDistance)
                            zi++
                        }
                        yi++
                    }
                    xi++
                }
            }
            Manhattan -> {
                var xi = xr - 1
                while (xi <= xr + 1) {
                    var yi = yr - 1
                    while (yi <= yr + 1) {
                        var zi = zr - 1
                        while (zi <= zr + 1) {
                            val lutPos = index3D_256(0, xi, yi, zi)
                            val vecX = xi - x + CELL_3D_X[lutPos] * cellularJitter
                            val vecY = yi - y + CELL_3D_Y[lutPos] * cellularJitter
                            val vecZ = zi - z + CELL_3D_Z[lutPos] * cellularJitter
                            val newDistance = abs(vecX) + abs(vecY) + abs(vecZ)
                            distance2 = max(min(distance2, newDistance), distance)
                            distance = min(distance, newDistance)
                            zi++
                        }
                        yi++
                    }
                    xi++
                }
            }
            Natural   -> {
                var xi = xr - 1
                while (xi <= xr + 1) {
                    var yi = yr - 1
                    while (yi <= yr + 1) {
                        var zi = zr - 1
                        while (zi <= zr + 1) {
                            val lutPos = index3D_256(0, xi, yi, zi)
                            val vecX = xi - x + CELL_3D_X[lutPos] * cellularJitter
                            val vecY = yi - y + CELL_3D_Y[lutPos] * cellularJitter
                            val vecZ = zi - z + CELL_3D_Z[lutPos] * cellularJitter
                            val newDistance = abs(vecX) + abs(vecY) + abs(vecZ) + (vecX * vecX + vecY * vecY + vecZ * vecZ)
                            distance2 = max(min(distance2, newDistance), distance)
                            distance = min(distance, newDistance)
                            zi++
                        }
                        yi++
                    }
                    xi++
                }
            }
        }

        return when (cellularReturnType) {
            Distance2    -> distance2 - 1
            Distance2Add -> distance2 + distance
            Distance2Sub -> distance2 - distance
            Distance2Mul -> distance2 * distance
            Distance2Div -> distance / distance2
            else         -> 0f
        }
    }

    fun getCellular(x: Float, y: Float): Float {
        var x1 = x
        var y1 = y
        x1 *= frequency
        y1 *= frequency

        return when (cellularReturnType) {
            CellValue, NoiseLookup, Distance -> singleCellular(x1, y1)
            else                             -> singleCellular2Edge(x1, y1)
        }
    }

    private fun singleCellular(x: Float, y: Float): Float {
        val xr = fastRound(x)
        val yr = fastRound(y)
        var distance = 999999f
        var xc = 0
        var yc = 0

        when (cellularDistanceFunction) {
            Euclidean -> {
                var xi = xr - 1
                while (xi <= xr + 1) {
                    var yi = yr - 1
                    while (yi <= yr + 1) {
                        val lutPos = index2D_256(0, xi, yi)
                        val vecX = xi - x + CELL_2D_X[lutPos] * cellularJitter
                        val vecY = yi - y + CELL_2D_Y[lutPos] * cellularJitter
                        val newDistance = vecX * vecX + vecY * vecY
                        if (newDistance < distance) {
                            distance = newDistance
                            xc = xi
                            yc = yi
                        }
                        yi++
                    }
                    xi++
                }
            }
            Manhattan -> {
                var xi = xr - 1
                while (xi <= xr + 1) {
                    var yi = yr - 1
                    while (yi <= yr + 1) {
                        val lutPos = index2D_256(0, xi, yi)
                        val vecX = xi - x + CELL_2D_X[lutPos] * cellularJitter
                        val vecY = yi - y + CELL_2D_Y[lutPos] * cellularJitter
                        val newDistance = abs(vecX) + abs(vecY)
                        if (newDistance < distance) {
                            distance = newDistance
                            xc = xi
                            yc = yi
                        }
                        yi++
                    }
                    xi++
                }
            }
            Natural   -> {
                var xi = xr - 1
                while (xi <= xr + 1) {
                    var yi = yr - 1
                    while (yi <= yr + 1) {
                        val lutPos = index2D_256(0, xi, yi)
                        val vecX = xi - x + CELL_2D_X[lutPos] * cellularJitter
                        val vecY = yi - y + CELL_2D_Y[lutPos] * cellularJitter
                        val newDistance = abs(vecX) + abs(vecY) + (vecX * vecX + vecY * vecY)
                        if (newDistance < distance) {
                            distance = newDistance
                            xc = xi
                            yc = yi
                        }
                        yi++
                    }
                    xi++
                }
            }
        }

        return when (cellularReturnType) {
            CellValue   -> valCoord2D(seed, xc, yc)
            NoiseLookup -> {
                val lutPos = index2D_256(0, xc, yc)
                cellularNoiseLookup!!.getNoise(xc + CELL_2D_X[lutPos] * cellularJitter, yc + CELL_2D_Y[lutPos] * cellularJitter)
            }
            Distance    -> distance
            else        -> 0f
        }
    }

    private fun singleCellular2Edge(x: Float, y: Float): Float {
        val xr = fastRound(x)
        val yr = fastRound(y)
        var distance = 999999f
        var distance2 = 999999f

        when (cellularDistanceFunction) {
            Euclidean -> {
                var xi = xr - 1
                while (xi <= xr + 1) {
                    var yi = yr - 1
                    while (yi <= yr + 1) {
                        val lutPos = index2D_256(0, xi, yi)
                        val vecX = xi - x + CELL_2D_X[lutPos] * cellularJitter
                        val vecY = yi - y + CELL_2D_Y[lutPos] * cellularJitter
                        val newDistance = vecX * vecX + vecY * vecY
                        distance2 = max(min(distance2, newDistance), distance)
                        distance = min(distance, newDistance)
                        yi++
                    }
                    xi++
                }
            }
            Manhattan -> {
                var xi = xr - 1
                while (xi <= xr + 1) {
                    var yi = yr - 1
                    while (yi <= yr + 1) {
                        val lutPos = index2D_256(0, xi, yi)
                        val vecX = xi - x + CELL_2D_X[lutPos] * cellularJitter
                        val vecY = yi - y + CELL_2D_Y[lutPos] * cellularJitter
                        val newDistance = abs(vecX) + abs(vecY)
                        distance2 = max(min(distance2, newDistance), distance)
                        distance = min(distance, newDistance)
                        yi++
                    }
                    xi++
                }
            }
            Natural   -> {
                var xi = xr - 1
                while (xi <= xr + 1) {
                    var yi = yr - 1
                    while (yi <= yr + 1) {
                        val lutPos = index2D_256(0, xi, yi)
                        val vecX = xi - x + CELL_2D_X[lutPos] * cellularJitter
                        val vecY = yi - y + CELL_2D_Y[lutPos] * cellularJitter
                        val newDistance = abs(vecX) + abs(vecY) + (vecX * vecX + vecY * vecY)
                        distance2 = max(min(distance2, newDistance), distance)
                        distance = min(distance, newDistance)
                        yi++
                    }
                    xi++
                }
            }
        }

        return when (cellularReturnType) {
            Distance2    -> distance2 - 1
            Distance2Add -> distance2 + distance - 1
            Distance2Sub -> distance2 - distance - 1
            Distance2Mul -> distance2 * distance - 1
            Distance2Div -> distance / distance2 - 1
            else         -> 0f
        }
    }

    fun gradientPerturb(x: Float, y: Float, z: Float) {
        singleGradientPerturb(0, gradientPerturbAmp, frequency, x, y, z)
    }

    fun gradientPerturbFractal(x: Float, y: Float, z: Float) {
        var amp = (gradientPerturbAmp * fractalBounding).toFloat()
        var freq = frequency

        singleGradientPerturb(perm[0], amp, frequency, x, y, z)

        for (i in 1 until octaves) {
            freq *= lacunarity
            amp *= gain
            singleGradientPerturb(perm[i], amp, freq, x, y, z)
        }
    }

    private fun singleGradientPerturb(offset: Int, perturbAmp: Float, freq: Float, x: Float, y: Float, z: Float) {
        val xf: Float = x * freq
        val yf: Float = y * freq
        val zf: Float = z * freq
        val x0 = fastFloor(xf)
        val y0 = fastFloor(yf)
        val z0 = fastFloor(zf)
        val x1 = x0 + 1
        val y1 = y0 + 1
        val z1 = z0 + 1
        val xs: Float
        val ys: Float
        val zs: Float

        when (interp) {
            Linear  -> {
                xs = xf - x0
                ys = yf - y0
                zs = zf - z0
            }
            Hermite -> {
                xs = interpHermiteFunc(xf - x0)
                ys = interpHermiteFunc(yf - y0)
                zs = interpHermiteFunc(zf - z0)
            }
            Quintic -> {
                xs = interpQuinticFunc(xf - x0)
                ys = interpQuinticFunc(yf - y0)
                zs = interpQuinticFunc(zf - z0)
            }
        }

        var lutPos0 = index3D_256(offset, x0, y0, z0)
        var lutPos1 = index3D_256(offset, x1, y0, z0)

        var lx0x = lerp(CELL_3D_X[lutPos0], CELL_3D_X[lutPos1], xs)
        var ly0x = lerp(CELL_3D_Y[lutPos0], CELL_3D_Y[lutPos1], xs)
        var lz0x = lerp(CELL_3D_Z[lutPos0], CELL_3D_Z[lutPos1], xs)

        lutPos0 = index3D_256(offset, x0, y1, z0)
        lutPos1 = index3D_256(offset, x1, y1, z0)

        var lx1x = lerp(CELL_3D_X[lutPos0], CELL_3D_X[lutPos1], xs)
        var ly1x = lerp(CELL_3D_Y[lutPos0], CELL_3D_Y[lutPos1], xs)
        var lz1x = lerp(CELL_3D_Z[lutPos0], CELL_3D_Z[lutPos1], xs)

        val lx0y = lerp(lx0x, lx1x, ys)
        val ly0y = lerp(ly0x, ly1x, ys)
        val lz0y = lerp(lz0x, lz1x, ys)

        lutPos0 = index3D_256(offset, x0, y0, z1)
        lutPos1 = index3D_256(offset, x1, y0, z1)

        lx0x = lerp(CELL_3D_X[lutPos0], CELL_3D_X[lutPos1], xs)
        ly0x = lerp(CELL_3D_Y[lutPos0], CELL_3D_Y[lutPos1], xs)
        lz0x = lerp(CELL_3D_Z[lutPos0], CELL_3D_Z[lutPos1], xs)

        lutPos0 = index3D_256(offset, x0, y1, z1)
        lutPos1 = index3D_256(offset, x1, y1, z1)

        lx1x = lerp(CELL_3D_X[lutPos0], CELL_3D_X[lutPos1], xs)
        ly1x = lerp(CELL_3D_Y[lutPos0], CELL_3D_Y[lutPos1], xs)
        lz1x = lerp(CELL_3D_Z[lutPos0], CELL_3D_Z[lutPos1], xs)

        val xLerp = x + lerp(lx0y, lerp(lx0x, lx1x, ys), zs) * perturbAmp
        val yLerp = y + lerp(ly0y, lerp(ly0x, ly1x, ys), zs) * perturbAmp
        val zLerp = z + lerp(lz0y, lerp(lz0x, lz1x, ys), zs) * perturbAmp
    }

    fun gradientPerturb(x: Float, y: Float) {
        singleGradientPerturb(0, gradientPerturbAmp, frequency, x, y)
    }

    fun gradientPerturbFractal(x: Float, y: Float) {
        var amp = (gradientPerturbAmp * fractalBounding)
        var freq = frequency

        singleGradientPerturb(perm[0], amp, frequency, x, y)

        for (i in 1 until octaves) {
            freq *= lacunarity
            amp *= gain
            singleGradientPerturb(perm[i], amp, freq, x, y)
        }
    }

    private fun singleGradientPerturb(offset: Int, perturbAmp: Float, freq: Float, x: Float, y: Float) {
        val xf: Float = x * freq
        val yf: Float = y * freq
        val x0 = fastFloor(xf)
        val y0 = fastFloor(yf)
        val x1 = x0 + 1
        val y1 = y0 + 1
        val xs: Float
        val ys: Float

        when (interp) {
            Linear  -> {
                xs = xf - x0
                ys = yf - y0
            }
            Hermite -> {
                xs = interpHermiteFunc(xf - x0)
                ys = interpHermiteFunc(yf - y0)
            }
            Quintic -> {
                xs = interpQuinticFunc(xf - x0)
                ys = interpQuinticFunc(yf - y0)
            }
        }

        var lutPos0 = index2D_256(offset, x0, y0)
        var lutPos1 = index2D_256(offset, x1, y0)

        val lx0x = lerp(CELL_2D_X[lutPos0], CELL_2D_X[lutPos1], xs)
        val ly0x = lerp(CELL_2D_Y[lutPos0], CELL_2D_Y[lutPos1], xs)

        lutPos0 = index2D_256(offset, x0, y1)
        lutPos1 = index2D_256(offset, x1, y1)

        val lx1x = lerp(CELL_2D_X[lutPos0], CELL_2D_X[lutPos1], xs)
        val ly1x = lerp(CELL_2D_Y[lutPos0], CELL_2D_Y[lutPos1], xs)

        val xLerp = x + lerp(lx0x, lx1x, ys) * perturbAmp
        val yLerp = y + lerp(ly0x, ly1x, ys) * perturbAmp
    }

    //Hashing
    val X_PRIME = 1619
    val Y_PRIME = 31337
    val Z_PRIME = 6971
    val W_PRIME = 1013

    fun valCoord2D(offset: Int, x: Int, y: Int): Float {
        var n = offset
        n = n xor X_PRIME * x
        n = n xor Y_PRIME * y
        return (n * n * n * 60493) / (2147483648.0.toFloat())
    }

    fun valCoord3D(offset: Int, x: Int, y: Int, z: Int): Float {
        var n = offset
        n = n xor X_PRIME * x
        n = n xor Y_PRIME * y
        n = n xor Z_PRIME * z
        return (n * n * n * 60493) / (2147483648.0.toFloat())
    }

    fun valCoord4D(offset: Int, x: Int, y: Int, z: Int, w: Int): Float {
        var n = offset
        n = n xor X_PRIME * x
        n = n xor Y_PRIME * y
        n = n xor Z_PRIME * z
        n = n xor W_PRIME * w
        return (n * n * n * 60493) / (2147483648.0.toFloat())
    }

    fun valCoord2DFast(offset: Int, x: Int, y: Int): Float = VAL_LUT[index2D_256(offset, x, y)]
    fun valCoord3DFast(offset: Int, x: Int, y: Int, z: Int): Float = VAL_LUT[index3D_256(offset, x, y, z)]

    private fun gradCoord2D(offset: Int, x: Int, y: Int, xd: Float, yd: Float): Float {
        val lutPos = index2D_12(offset, x, y)
        return xd * GRAD_X[lutPos] + yd * GRAD_Y[lutPos]
    }

    private fun gradCoord3D(offset: Int, x: Int, y: Int, z: Int, xd: Float, yd: Float, zd: Float): Float {
        val lutPos = index3D_12(offset, x, y, z)
        return xd * GRAD_X[lutPos] + yd * GRAD_Y[lutPos] + zd * GRAD_Z[lutPos]
    }

    private fun gradCoord4D(offset: Int, x: Int, y: Int, z: Int, w: Int, xd: Float, yd: Float, zd: Float, wd: Float): Float {
        val lutPos = index4D_32(offset, x, y, z, w) shl 2
        return xd * GRAD_4[lutPos] + yd * GRAD_4[lutPos + 1] + zd * GRAD_4[lutPos + 2] + wd * GRAD_4[lutPos + 3]
    }

    fun copy(): FastNoiseKt {
        val copy = FastNoiseKt(seed)

        copy.setNoiseType(this.noiseType)
        copy.setFrequency(this.frequency)
        copy.setInterp(this.interp)
        copy.setFractalType(this.fractalType)
        copy.setFractalOctaves(this.octaves)
        copy.setFractalLacunarity(this.lacunarity)
        copy.setFractalGain(this.gain)
        copy.setCellularDistanceFunction(this.cellularDistanceFunction)
        copy.setCellularReturnType(this.cellularReturnType)
        copy.setCellularJitter(this.cellularJitter)
        copy.setFractalBounding(this.fractalBounding)

        return copy
    }
}