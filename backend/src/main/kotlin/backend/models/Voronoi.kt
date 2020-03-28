package backend.models

import enums.TerrainGroup
import enums.TerrainType
import models.geometry.Vertex

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class Voronoi(val nPoints: Int) {
    val v = mutableMapOf<Int, IntArray>() // cell vertices (every index (key) can be used to get the corresponding list of keys from 'p' in VoronoiVertices which make up the shape)
    val c = mutableMapOf<Int, IntArray>() // adjacent cells (key corresponds with the key from 'v' and contains which cells are adjacent to it (these are also the keys in 'v'))
    val b = mutableMapOf<Int, Boolean>() // near-border cell (key corresponds with the key from 'v' and contains whether a cell is near the border)

    val p = mutableMapOf<Int, Vertex>() // vertex coordinates (every index (key) can be used to get the corresponding vertex)
    val n = mutableMapOf<Int, IntArray>() // neighboring vertices (the key is the same key for 'p' and 'a' and contains which other vertices are it's neighbour)
    val a = mutableMapOf<Int, IntArray>() // adjacent cells (the key is the same key for 'p' and 'n' and contains which cell from 'v' in VoronoiCells corresponds)

    val l = mutableMapOf<Int, Boolean>() // is cell land key is same key as v
    val t = mutableMapOf<Int, TerrainType>() // is cell land key is same key as v
    val g = mutableMapOf<Int, TerrainGroup>() // is cell land key is same key as v
}