package services

import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.w3c.xhr.XMLHttpRequest
import visuals.CellLayer
import kotlin.js.Promise

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
object GeneratorService {
    val json = Json(JsonConfiguration.Stable)

    suspend fun retrieveCellLayer(): CellLayer {
        val xhr = retrievePromise("/layer/cell").await()
        return json.parse(CellLayer.serializer(), xhr.responseText)
    }

    private fun retrievePromise(url: String): Promise<XMLHttpRequest> {
        return Promise {
            resolve, _ -> retrieveRequest(url, resolve)
        }
    }

    private fun retrieveRequest(url: String, resolve: (XMLHttpRequest) -> Unit) {
        val xhr = XMLHttpRequest()
        xhr.onreadystatechange = {
            if (xhr.readyState == XMLHttpRequest.DONE) resolve(xhr)
        }
        xhr.open("GET", url)
        xhr.send()
    }
}