@file:Suppress("PropertyName", "EnumEntryName")

package de.peekandpoke.funktor.messaging.senders.sendgrid

//@Serializable
//data class SendgridEmail(val email: String, val name: String? = null)
//
//@Serializable
//data class SendgridContent(val type: String, val value: String)
//
//@Serializable
//data class Personalization(
//    val to: List<SendgridEmail>,
//    val cc: List<SendgridEmail>? = null,
//    val bcc: List<SendgridEmail>? = null,
//    val subject: String? = null,
//    val dynamic_template_data: Map<String, String>? = null,
//)
//
//@Serializable
//data class SendgridAttachment(
//    /** base64 of the bytes */
//    val content: String,
//    /** file name with attachment */
//    val filename: String,
//    /** MIME type, e.g. "application/pdf" */
//    val type: String? = null,
//    /** Disposition of the attachment */
//    val disposition: Disposition? = Disposition.attachment,
//    /** needed for inline (CID) only */
//    val content_id: String? = null,
//) {
//    companion object {
//        fun from(attachment: EmailAttachment) = SendgridAttachment(
//            content = attachment.dataBase64,
//            filename = attachment.filename,
//            type = attachment.mimeType,
//            disposition = Disposition.attachment,
//        )
//    }
//
//    enum class Disposition { attachment, inline }
//}
//
///**
// * See https://github.com/sendgrid/sendgrid-java/blob/main/src/main/java/com/sendgrid/helpers/mail/Mail.java
// */
//@Serializable
//data class SendgridMail(
//    val from: SendgridEmail,
//    val personalizations: List<Personalization>,
//    val subject: String? = null,
//    val content: List<SendgridContent>? = null,
//    val template_id: String? = null,
//    val attachments: List<SendgridAttachment>? = null,
//)
