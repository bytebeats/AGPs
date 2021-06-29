package me.bytebeats.lint.core.detector;

import com.android.SdkConstants;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bytebeats on 2021/6/29 : 11:05
 * E-mail: happychinapc@gmail.com
 * Quote: Peasant. Educated. Worker
 */
public class TranslucentOrientatedActivityDetector extends Detector implements Detector.XmlScanner {
    private static final String ISSUE_ID = "probe-translucent-orientated-activity";
    private static final String ISSUE_MESSAGE = "Translucent Theme and android:screenOrientation would not appear in AndroidManifest.xml";
    private static final String ISSUE_EXPLANATION = "Translucent Theme can't work with android:screenOrientation in Android 8.0, or Activities will crash";
    private final Map<ElementEntity, String> mThemeMap = new HashMap<>();

    @Nullable
    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(SdkConstants.TAG_ACTIVITY, SdkConstants.TAG_STYLE);
    }

    @Override
    public void visitElement(@NotNull XmlContext context, @NotNull Element element) {
        switch (element.getTagName()) {
            case SdkConstants.TAG_ACTIVITY:
                if (isOrientatedScreen(element)) {
                    String theme = element.getAttributeNS(SdkConstants.ANDROID_URI, SdkConstants.ATTR_THEME);
                    if ("@style/Theme.AppTheme.Transparent".equals(theme)) {
                        reportIssue(context, element);
                    } else {
                        mThemeMap.put(new ElementEntity(context, element), theme.substring(theme.indexOf('/') + 1));
                    }
                }
                break;
            case SdkConstants.TAG_STYLE:
                String style = element.getAttribute(SdkConstants.ATTR_NAME);
                for (Map.Entry<ElementEntity, String> entry : mThemeMap.entrySet()) {
                    if (entry.getValue().equals(style)) {
                        if (isTranslucentOrFloatingTheme(element)) {
                            reportIssue(entry.getKey().getContext(), entry.getKey().getElement());
                        } else {
                            mThemeMap.put(entry.getKey(), element.getAttribute(SdkConstants.ATTR_PARENT));
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    private void reportIssue(XmlContext context, Element element) {
        context.report(TranslucentOrientatedActivity, element, context.getLocation(element), ISSUE_MESSAGE);
    }

    private boolean isOrientatedScreen(Element element) {
        String value = element.getAttributeNS(SdkConstants.ANDROID_URI, "screenOrientation");
        return Arrays.asList("landscape", "sensorLandscape", "reverseLandscape",
                "userLandscape", "portrait", "sensorPortrait", "reversePortrait",
                "userPortrait", "locked")
                .contains(value);
    }

    private boolean isTranslucentOrFloatingTheme(Element element) {
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element
                    && SdkConstants.TAG_ITEM.equals(((Element) child).getTagName())
                    && child.getFirstChild() != null
                    && SdkConstants.VALUE_TRUE.equals(child.getFirstChild().getNodeValue())) {
                return Arrays.asList("android:windowIsTranslucent", "android:windowSwipeToDismiss", "android:windowIsFloating").contains(((Element) child).getAttribute(SdkConstants.ATTR_NAME));
            }
        }
        return element.getAttribute(SdkConstants.ATTR_PARENT).contains("Transparent");
    }

    private static final class ElementEntity {
        private final XmlContext mContext;
        private final Element mElement;

        public ElementEntity(XmlContext mContext, Element mElement) {
            this.mContext = mContext;
            this.mElement = mElement;
        }

        public XmlContext getContext() {
            return mContext;
        }

        public Element getElement() {
            return mElement;
        }
    }

    public static final Issue TranslucentOrientatedActivity = Issue.create(ISSUE_ID,
            ISSUE_MESSAGE,
            ISSUE_EXPLANATION,
            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            new Implementation(TranslucentOrientatedActivityDetector.class, Scope.MANIFEST_AND_RESOURCE_SCOPE)).setAndroidSpecific(true);
}
