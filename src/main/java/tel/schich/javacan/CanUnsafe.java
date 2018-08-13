package tel.schich.javacan;

/**
 * Make unsafe operations very explicit!
 */
public class CanUnsafe {
    public static int getRawFileDescriptor(HasFileDescriptor obj) {
        return obj.getFileDescriptor();
    }
}
