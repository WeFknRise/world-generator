package backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */

@SpringBootApplication
class WorldGeneratorApplication

fun main(args: Array<String>) {
    runApplication<WorldGeneratorApplication>(*args)
}