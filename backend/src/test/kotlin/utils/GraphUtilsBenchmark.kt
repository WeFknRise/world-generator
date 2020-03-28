package utils

import backend.extensions.StopwatchExtensions.averageTimeMS
import backend.extensions.StopwatchExtensions.prettyPrintMS
import backend.utils.GraphUtils.boundaryVertices
import backend.utils.GraphUtils.generateVoronoiGraphs
import backend.utils.GraphUtils.jitteredGraph
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.util.StopWatch
import utils.GraphUtilsBenchmarkConstants.boundaryVertices
import utils.GraphUtilsBenchmarkConstants.boundaryVerticesLarge
import utils.GraphUtilsBenchmarkConstants.boundaryVerticesMedium
import utils.GraphUtilsBenchmarkConstants.cellsDesired
import utils.GraphUtilsBenchmarkConstants.cellsDesiredLarge
import utils.GraphUtilsBenchmarkConstants.cellsDesiredMedium
import utils.GraphUtilsBenchmarkConstants.density
import utils.GraphUtilsBenchmarkConstants.densityLarge
import utils.GraphUtilsBenchmarkConstants.densityMedium
import utils.GraphUtilsBenchmarkConstants.graphVertices
import utils.GraphUtilsBenchmarkConstants.graphVerticesLarge
import utils.GraphUtilsBenchmarkConstants.graphVerticesMedium
import utils.GraphUtilsBenchmarkConstants.height
import utils.GraphUtilsBenchmarkConstants.heightLarge
import utils.GraphUtilsBenchmarkConstants.heightMedium
import utils.GraphUtilsBenchmarkConstants.spacing
import utils.GraphUtilsBenchmarkConstants.spacingLarge
import utils.GraphUtilsBenchmarkConstants.spacingMedium
import utils.GraphUtilsBenchmarkConstants.width
import utils.GraphUtilsBenchmarkConstants.widthLarge
import utils.GraphUtilsBenchmarkConstants.widthMedium
import utils.MathUtils.rn
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
@TestInstance(PER_CLASS)
class GraphUtilsBenchmark {
    private lateinit var stopWatch: StopWatch

    @BeforeAll()
    fun beforeAll() {
        cellsDesired = 10_000.0
        width = 1_920.0
        height = 937.0
        spacing = rn(sqrt(width * height / cellsDesired), 2.0)

        cellsDesiredMedium = 1_000_000.0
        widthMedium = 19_200.0
        heightMedium = 9_370.0
        spacingMedium = rn(sqrt(width * height / cellsDesired), 2.0)

        cellsDesiredLarge = 10_000.0 * (densityLarge.pow(2))
        widthLarge = 48_000.0
        heightLarge = 23_425.0
        spacingLarge = rn(sqrt(width * height / cellsDesired), 2.0)

        boundaryVertices = boundaryVertices(width, height, density.toInt(), spacing)
        graphVertices = jitteredGraph(width, height, density.toInt(), spacing) { 0.0 }

        boundaryVerticesMedium = boundaryVertices(widthMedium, heightMedium, densityMedium.toInt(), spacingMedium)
        graphVerticesMedium = jitteredGraph(widthMedium, heightMedium, densityMedium.toInt(), spacingMedium) { 0.0 }

        boundaryVerticesLarge = boundaryVertices(widthLarge, heightLarge, densityLarge.toInt(), spacingLarge)
        graphVerticesLarge = jitteredGraph(widthLarge, heightLarge, densityLarge.toInt(), spacingLarge) { 0.0 }
    }

    @BeforeEach()
    fun beforeEach(info: TestInfo, ri: RepetitionInfo) {
        if (ri.currentRepetition == 1) {
            stopWatch = StopWatch(info.displayName)
        }
    }

    @RepeatedTest(25)
    fun jitteredGraphSmallBenchmark(repetitionInfo: RepetitionInfo) {
        stopWatch.start("Repetition Small #${repetitionInfo.currentRepetition}")
        jitteredGraph(width, height, density.toInt(), spacing) { 0.0 }
        stopWatch.stop()
    }

    @RepeatedTest(25)
    fun jitteredGraphMediumBenchmark(repetitionInfo: RepetitionInfo) {
        stopWatch.start("Repetition Medium #${repetitionInfo.currentRepetition}")
        jitteredGraph(widthMedium, heightMedium, densityMedium.toInt(), spacingMedium) { 0.0 }
        stopWatch.stop()
    }

    @RepeatedTest(25)
    fun jitteredGraphLargeBenchmark(repetitionInfo: RepetitionInfo) {
        stopWatch.start("Repetition Large #${repetitionInfo.currentRepetition}")
        jitteredGraph(widthLarge, heightLarge, densityLarge.toInt(), spacingLarge) { 0.0 }
        stopWatch.stop()
    }

    @RepeatedTest(25)
    fun generateVoronoiGraphsSmallBenchmark(repetitionInfo: RepetitionInfo) {
        stopWatch.start("Repetition Small #${repetitionInfo.currentRepetition}")
        generateVoronoiGraphs(boundaryVertices, graphVertices, density.toInt())
        stopWatch.stop()
    }

    @RepeatedTest(25)
    fun generateVoronoiGraphsMediumBenchmark(repetitionInfo: RepetitionInfo) {
        stopWatch.start("Repetition Medium #${repetitionInfo.currentRepetition}")
        generateVoronoiGraphs(boundaryVerticesMedium, graphVerticesMedium, densityMedium.toInt())
        stopWatch.stop()
    }

    @AfterEach
    fun afterEach(ri: RepetitionInfo) {
        if (ri.currentRepetition == ri.totalRepetitions && ri.totalRepetitions > 5) {
            println(stopWatch.prettyPrintMS())
            println(stopWatch.averageTimeMS(5))
        }
    }
}