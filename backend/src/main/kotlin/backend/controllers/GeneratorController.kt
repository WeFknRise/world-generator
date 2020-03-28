package backend.controllers

import backend.constants.Constants.defaultWorldHeight
import backend.constants.Constants.defaultWorldWidth
import backend.generators.CellLayerGenerator
import backend.generators.WorldGenerator
import backend.globals.WorldGlobals.cellsMultiplier
import backend.globals.WorldGlobals.density
import backend.globals.WorldGlobals.worldHeight
import backend.globals.WorldGlobals.worldMultiplier
import backend.globals.WorldGlobals.worldWidth
import constants.RESTConstants.densityParam
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import visuals.CellLayer

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
@RestController
class GeneratorController {
    private val json = Json(JsonConfiguration.Stable)
    private val generator = WorldGenerator()

    @GetMapping("/generate")
    fun generateWorld(@RequestParam width: Double, @RequestParam height: Double, @RequestParam(required = false) multiplier: Int = 1, @RequestParam(
            name = densityParam, required = false) cellDensity: Int = 1): Boolean {
        density = cellDensity
        worldMultiplier = multiplier
        worldWidth = width * worldMultiplier
        worldHeight = height * worldMultiplier
        cellsMultiplier = (width / defaultWorldWidth) * (height / defaultWorldHeight) // Calculate default cellsMultiplier

        generator.generate()
        return true
    }

    @GetMapping("/layer/cell")
    fun retrieveCellLayer(): String {
        val clg = CellLayerGenerator(generator.voronoiGraphs)
        val cellLayer = clg.generate()

        return json.stringify(CellLayer.serializer(), cellLayer)
    }
}