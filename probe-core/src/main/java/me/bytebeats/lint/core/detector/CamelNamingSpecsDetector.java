package me.bytebeats.lint.core.detector;

import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.util.Arrays;
import java.util.List;

public class CamelNamingSpecsDetector extends Detector implements Detector.UastScanner {
    private static final String ISSUE_ID = "probe-camel-naming-specs";
    private static final String ISSUE_MESSAGE = "Java should follow camel naming specs";
    private static final String ISSUE_EXPLANATION = "Java should follow camel naming specs. Class name should start with upper case, method name should start with low case, and then appending words with first Capital character";
    private static final String ISSUE_CLASS_EXPLANATION = "Java should follow camel naming specs. Class %s should start with upper case, and then appending words with first Capital character";
    private static final String ISSUE_METHOD_EXPLANATION = "Java should follow camel naming specs. Method %s should start with low case, and then appending words with first Capital character";

    @Nullable
    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return Arrays.<Class<? extends UElement>>asList(UClass.class);
    }

    @Nullable
    @Override
    public UElementHandler createUastHandler(@NotNull final JavaContext context) {
        return new UElementHandler() {
            @Override
            public void visitClass(@NotNull UClass node) {
                node.accept(new CNSUastVisitor(context));
            }
        };
    }

    private static final class CNSUastVisitor extends AbstractUastVisitor {
        private final JavaContext context;

        public CNSUastVisitor(JavaContext context) {
            this.context = context;
        }

        @Override
        public boolean visitClass(@NotNull UClass node) {
            char className1stChar = node.getName().toCharArray()[0];
            if (className1stChar < 'a' || className1stChar > 'z') {
                context.report(CNSIssue, context.getNameLocation(node), String.format(ISSUE_CLASS_EXPLANATION, node.getName()));
                return true;//true表示触碰 Issue
            }
            return super.visitClass(node);
        }

        @Override
        public boolean visitMethod(@NotNull UMethod node) {
            if (!node.isConstructor()) {
                char methodName1stChar = node.getName().toCharArray()[0];
                if (methodName1stChar >= 'A' && methodName1stChar <= 'Z') {
                    context.report(CNSIssue, context.getLocation(node), String.format(ISSUE_METHOD_EXPLANATION, node.getName()));
                    return true;//true表示触碰 Issue
                }
            }
            return super.visitMethod(node);
        }
    }

    public static final Issue CNSIssue = Issue.create(ISSUE_ID,
            ISSUE_MESSAGE,
            ISSUE_EXPLANATION,
            Category.USABILITY,
            5,
            Severity.WARNING,
            new Implementation(CamelNamingSpecsDetector.class, Scope.JAVA_FILE_SCOPE));
}