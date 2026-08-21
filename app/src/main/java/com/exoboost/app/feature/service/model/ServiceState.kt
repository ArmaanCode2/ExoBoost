package com.exoboost.app.feature.service.model

enum class ServiceState {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING;

    val isRunning: Boolean get() = this == RUNNING
    val isActive: Boolean get() = this == RUNNING || this == STARTING
}
