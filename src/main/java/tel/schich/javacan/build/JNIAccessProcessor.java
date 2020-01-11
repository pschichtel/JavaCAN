package tel.schich.javacan.build;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Collections;
import java.util.Set;

public class JNIAccessProcessor extends AbstractProcessor {
    private static final Set<String> SUPPORTED_ANNOTATIONS = Collections.singleton(JNIAccess.class.getCanonicalName());

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return SUPPORTED_ANNOTATIONS;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(JNIAccess.class);
        for (Element annotatedElement : annotatedElements) {
            boolean performanceCritical = annotatedElement.getAnnotation(JNIAccess.class).performanceCritical();
            switch (annotatedElement.getKind()) {
                case CONSTRUCTOR:
                    processConstructor(roundEnv, annotatedElement, performanceCritical);
                    break;
                case METHOD:
                    processMethod(roundEnv, annotatedElement, performanceCritical);
                    break;
                case FIELD:
                    processField(roundEnv, annotatedElement, performanceCritical);
                    break;
                default:
            }
        }
        return false;
    }

    private void processConstructor(RoundEnvironment env, Element element, boolean performanceCritical) {
    }

    private void processMethod(RoundEnvironment env, Element element, boolean performanceCritical) {
    }

    private void processField(RoundEnvironment env, Element element, boolean performanceCritical) {
    }
}
