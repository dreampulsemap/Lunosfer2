import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SendMessageRequest(
    val recipientId: String,
    val content: String? = null,
    val lang: String? = null,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null,
    val attachmentName: String? = null
)

fun main() {
    val req = SendMessageRequest(recipientId = "123", content = "hello")
    println(Json.encodeToString(req))
}
