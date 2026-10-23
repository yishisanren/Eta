package io.github.mangi.eta.agent.runtime

import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/** A run that starts driving the shared Android screen owns it until the run ends. */
internal class AgentForegroundRunGate(private val controller: AgentRunController) : AutoCloseable {
    private var acquired = false

    @Synchronized fun enter() {
        if (acquired) return
        while (true) {
            controller.throwIfCancelled()
            try {
                if (screen.tryAcquire(100, TimeUnit.MILLISECONDS)) break
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                throw AgentRunCancelledException()
            }
        }
        acquired = true
        if (controller.isCancelled) {
            close()
            throw AgentRunCancelledException()
        }
    }

    @Synchronized override fun close() {
        if (!acquired) return
        acquired = false
        screen.release()
    }

    private companion object {
        val screen = Semaphore(1, true)
    }
}
