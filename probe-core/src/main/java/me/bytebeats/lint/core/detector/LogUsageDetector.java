package me.bytebeats.lint.core.detector;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiMethod;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;

import java.util.Arrays;
import java.util.List;

public class LogUsageDetector extends Detector implements Detector.UastScanner {
    private static final String ISSUE_ID = "probe-log";
    private static final String ISSUE_CLASS_NAME = "android.util.Log";
    private static final String ISSUE_MESSAGE = "android.util.Log should not be in release version";
    private static final String ISSUE_EXPLANATION = "android.util.Log is forbidden to push\nUse your own log utilities";

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList("v", "d", "i", "w", "e", "wtf");
    }

    @Override
    public void visitMethodCall(@NotNull JavaContext context, @NotNull UCallExpression node, @NotNull PsiMethod method) {
        if (context.getEvaluator().isMemberInClass(method, ISSUE_CLASS_NAME)) {
            context.report(LogUsage, node, context.getLocation(node), ISSUE_MESSAGE);
        }
    }

    public static final Issue LogUsage = Issue.create(ISSUE_ID,
            ISSUE_MESSAGE,
            ISSUE_EXPLANATION,
            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            new Implementation(LogUsageDetector.class, Scope.JAVA_FILE_SCOPE));
}