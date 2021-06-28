package me.bytebeats.lint.core.registry;

import com.android.tools.lint.checks.LogDetector;
import com.android.tools.lint.checks.ThreadDetector;
import com.android.tools.lint.checks.ToastDetector;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.ApiKt;
import com.android.tools.lint.detector.api.Issue;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import me.bytebeats.lint.core.detector.ParseColorDetector;
import me.bytebeats.lint.core.detector.LogUsageDetector;

/**
 * Created by bytebeats on 2021/6/28 : 14:27
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */

/**
 * Registry issues.
 */
public class ProbeIssueRegistry extends IssueRegistry {
    @NotNull
    @Override
    public List<Issue> getIssues() {
        return Arrays.asList(LogUsageDetector.LogIssue, ParseColorDetector.ColorParseIssue, ToastDetector.ISSUE, LogDetector.CONDITIONAL, ThreadDetector.THREAD);
    }

    @Override
    public int getMinApi() {
        return 1;
    }

    @Override
    public int getApi() {
        return ApiKt.CURRENT_API;
    }
}
