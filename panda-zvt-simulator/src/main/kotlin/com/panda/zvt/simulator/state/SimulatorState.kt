package com.panda.zvt.simulator.state

import com.panda.zvt.simulator.config.SimulatorConfig
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class SimulatorState(initialConfig: SimulatorConfig) {

    private val _config = AtomicReference(initialConfig)
    private val _traceCounter = AtomicInteger(1)
    private val _receiptCounter = AtomicInteger(1)
    private val _turnoverCounter = AtomicInteger(1)
    private val _registered = AtomicBoolean(false)
    private val _busy = AtomicBoolean(false)

    var config: SimulatorConfig
        get() = _config.get()
        set(value) { _config.set(value) }

    fun nextTraceNumber(): Int = _traceCounter.getAndIncrement()
    fun nextReceiptNumber(): Int = _receiptCounter.getAndIncrement()
    fun nextTurnoverNumber(): Int = _turnoverCounter.getAndIncrement()

    fun isRegistered(): Boolean = _registered.get()
    fun setRegistered(v: Boolean) { _registered.set(v) }

    fun currentTrace(): Int = _traceCounter.get()
    fun currentReceipt(): Int = _receiptCounter.get()

    fun isBusy(): Boolean = _busy.get()
    fun setBusy(v: Boolean) { _busy.set(v) }

    fun updateConfig(newConfig: SimulatorConfig) { _config.set(newConfig) }

    fun reset() {
        _traceCounter.set(1)
        _receiptCounter.set(1)
        _turnoverCounter.set(1)
        _registered.set(false)
        _busy.set(false)
    }
}
