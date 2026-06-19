import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Quick sanity check: re-read a structure .nbt and print its shape. */
public final class VerifyNbt {
    public static void main(String[] args) throws Exception {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new GZIPInputStream(new FileInputStream(args[0]))))) {
            SchemToNbt.Named root = SchemToNbt.readNamedTag(dis);
            Map<String, Object> r = SchemToNbt.asCompound(root.value());
            System.out.println("root keys: " + r.keySet());
            Object size = r.get("size");
            Object pal = r.get("palette");
            Object blocks = r.get("blocks");
            System.out.println("DataVersion: " + r.get("DataVersion"));
            if (size instanceof SchemToNbt.TList tl) System.out.println("size: " + tl.items());
            if (pal instanceof SchemToNbt.TList tl) System.out.println("palette entries: " + tl.items().size());
            if (blocks instanceof SchemToNbt.TList tl) {
                System.out.println("blocks: " + tl.items().size());
                @SuppressWarnings("unchecked")
                Map<String, Object> first = (Map<String, Object>) tl.items().get(0);
                System.out.println("first block keys: " + first.keySet());
            }
            System.out.println("OK - parsed cleanly to EOF");
        }
    }
}
