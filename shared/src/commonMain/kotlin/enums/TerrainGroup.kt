package enums

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
enum class TerrainGroup {
    VOID,
    OCEAN,

    /**
     * Lake groups
     */
    FRESH,
    SALT,
    FROZEN,
    SINKHOLE,
    LAVA,

    /**
     * Land groups
     */
    ISLE,
    ISLAND,
    CONTINENT,
    LAKE_ISLAND,
    SEA_ISLAND
}