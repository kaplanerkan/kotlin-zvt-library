package com.panda.zvt_library

import com.panda.zvt_library.model.*

/**
 * ZVT event listener interface.
 *
 * Implement this interface to receive callbacks from the [ZvtClient] for
 * connection state changes, intermediate statuses, receipt print lines,
 * debug logs, and errors.
 *
 * All methods have default no-op implementations, so you only need to
 * override the ones you are interested in.
 *
 * Usage:
 * ```kotlin
 * zvtClient.setCallback(object : ZvtCallback {
 *     override fun onConnectionStateChanged(state: ConnectionState) { ... }
 *     override fun onIntermediateStatus(status: IntermediateStatus) { ... }
 *     override fun onPrintLine(line: String) { ... }
 * })
 * ```
 *
 * @author Erkan Kaplan
 * @since 2026-02-10
 */
interface ZvtCallback {

    /**
     * Called when the connection state changes.
     *
     * @param state The new [ConnectionState].
     */
    fun onConnectionStateChanged(state: ConnectionState) {}

    /**
     * Called when an intermediate status message is received from the terminal (04 FF).
     *
     * Examples: "Insert card", "Enter PIN", "Please wait"
     *
     * @param status The [IntermediateStatus] containing the status code and message.
     */
    fun onIntermediateStatus(status: IntermediateStatus) {}

    /**
     * Called when a print line is received from the terminal (06 D1).
     *
     * @param line The text line to be printed on the receipt.
     */
    fun onPrintLine(line: String) {}

    /**
     * Called for debug log messages from the ZVT client.
     *
     * @param tag Log tag (e.g. "ZvtClient").
     * @param message Log message content.
     */
    fun onDebugLog(tag: String, message: String) {}

    /**
     * Called when a ZVT error occurs.
     *
     * @param error The [ZvtError] describing the error.
     */
    fun onError(error: ZvtError) {}
}
