package tel.schich.javacan.build;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents constructors, methods and fields that are being accessed from native code.
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface JNIAccess {
    /**
     * If this access is from a performance critical path.
     *
     * @return true if the access must be efficient
     */
    boolean performanceCritical();
}
