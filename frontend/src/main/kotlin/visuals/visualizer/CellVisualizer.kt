package visuals.visualizer

import constants.WorldElements.cells
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asPromise
import kotlinx.coroutines.async
import services.GeneratorService.retrieveCellLayer
import kotlin.js.Promise

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class CellVisualizer {
    private suspend fun draw() {
        val cl = retrieveCellLayer()
        cells.append("path").attr("d", cl.path).attr("stroke", "#808080").attr("fill", "none").attr("strokeWidth", "1.0").attr("opacity", "1.0")
    }

    fun unDraw() {
        cells.selectAll("*").remove()
    }

    fun drawPromise(): Promise<Unit> {
        return GlobalScope.async { draw() }.asPromise()
    }
}