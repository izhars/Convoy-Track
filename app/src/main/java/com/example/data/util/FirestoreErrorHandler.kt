package com.example.data.util

import android.util.Log

enum class OperationType {
    CREATE, READ, UPDATE, DELETE, LIST
}

fun handleFirestoreError(error: Exception, operation: OperationType, path: String) {
    val logPayload = """
        {
            "event": "firestore_error",
            "operation": "${operation.name}",
            "path": "$path",
            "message": "${error.message?.replace("\"", "\\\"") ?: "Unknown error"}",
            "type": "${error.javaClass.simpleName}"
        }
    """.trimIndent()
    Log.e("FirestoreError", logPayload, error)
}
