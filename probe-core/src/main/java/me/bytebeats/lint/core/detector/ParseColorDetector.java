package me.bytebeats.lint.core.detector;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.impl.source.tree.java.MethodElement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UTryExpression;
import org.jetbrains.uast.UastUtils;
import org.jetbrains.uast.kotlin.KotlinUFile;

import java.util.Arrays;
import java.util.List;

public class ParseColorDetector extends Detector implements Detector.UastScanner {
    private static final String ISSUE_ID = "probe-colorparse";
    private static final String ISSUE_CLASS_NAME = "android.graphics.Color";
    private static final String ISSUE_METHOD_NAME = "parseColor";
    private static final String ISSUE_MESSAGE = "Color#parseColor may throw IllegalArgumentException";
    private static final String ISSUE_EXPLANATION = "For color in String format, Color#parseColor may throw IllegalArgumentException and should work with try-catch block";

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList("parseColor");
    }

    @Override
    public void visitMethodCall(@NotNull JavaContext context, @NotNull UCallExpression node, @NotNull PsiMethod method) {
        if (!ISSUE_METHOD_NAME.equals(method.getName()) || !context.getEvaluator().isMemberInClass(method, ISSUE_CLASS_NAME)) {
            return;
        }
        if (isLegalColorConstant(node)) {
            return;
        }
        if (isWorkingWithTryCatchBlock(context, node)) {
            return;
        }

        context.report(ColorParseIssue, node, context.getLocation(node), ISSUE_MESSAGE);
    }

    private boolean isLegalColorConstant(UCallExpression node) {
        return node.getValueArguments().get(0).evaluate().toString().startsWith("#");
    }

    private boolean isWorkingWithTryCatchBlock(JavaContext context, UCallExpression node) {
        if (context.getUastFile() instanceof KotlinUFile) {//kotlin try-catch
            return UastUtils.getParentOfType(node.getUastParent(), UTryExpression.class) != null;
        }
        for (PsiElement parent = node.getSourcePsi().getParent(); parent != null && !(parent instanceof MethodElement); parent = parent.getParent()) {
            if (parent instanceof PsiTryStatement) {
                return true;
            }
        }
        return false;
    }

    public static final Issue ColorParseIssue = Issue.create(ISSUE_ID,
            ISSUE_MESSAGE,
            ISSUE_EXPLANATION,
            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            new Implementation(ParseColorDetector.class, Scope.JAVA_FILE_SCOPE)).setAndroidSpecific(true);
}