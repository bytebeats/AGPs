package me.bytebeats.agp.chronus

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState

/**
 * Created by bytebeats on 2021/6/26 : 20:03
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
class TaskTimeKeeper implements TaskExecutionListener, BuildListener {
    private HansClock clock
    private taskTimes = []

    private boolean enabled = true
    private boolean verbose = true

    TaskTimeKeeper(boolean enabled, boolean verbose) {
        this.enabled = enabled
        this.verbose = verbose
    }

    @Override
    void buildStarted(Gradle gradle) {

    }

    @Override
    void beforeExecute(Task task) {
        if (enabled) {
            clock = new HansClock()
        }
    }

    @Override
    void afterExecute(Task task, TaskState taskState) {
        if (enabled) {
            long ms = clock.timeInMS()
            taskTimes.add([ms, task.path])
            if (verbose) {
                task.project.logger.warn "${task.path} spends ${ms}ms"
            }
        }
    }

    @Override
    void settingsEvaluated(Settings settings) {

    }

    @Override
    void projectsLoaded(Gradle gradle) {

    }

    @Override
    void projectsEvaluated(Gradle gradle) {

    }

    @Override
    void buildFinished(BuildResult buildResult) {
        if (enabled) {
            println("Tasks spend time:")
            for (pair in taskTimes) {
                if (pair[0] > 50) {
                    printf "%10s   %s\n", HansClock.format(pair[0]), pair[1]
                }
            }
        }
    }
}
