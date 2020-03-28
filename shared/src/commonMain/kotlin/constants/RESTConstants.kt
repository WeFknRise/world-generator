package constants

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
object RESTConstants {
    //URL prefixes
    const val generatorUrl = "/generate"
    const val getCellLayerUrl = "/layer/cell"

    //URL parameter keys
    const val widthParam = "width"
    const val heightParam = "height"
    const val densityParam = "density"
    const val multiplierParam = "multiplier"

    fun urlBuilder(urlPrefix: String, vararg params: Pair<String, Any>) : String {
        var paramString = if (params.isEmpty()) "" else "?"

        params.forEachIndexed { i, param ->
            paramString += "${param.first}=${param.second}"
            if (i < (params.size - 1)) paramString += "&"
        }

        return "$urlPrefix$paramString"
    }
}