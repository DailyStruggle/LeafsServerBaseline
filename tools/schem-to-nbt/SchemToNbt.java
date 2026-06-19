import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Dependency-free converter: WorldEdit Sponge schematic (v2, gzipped NBT) ->
 * vanilla Minecraft structure template (.nbt, gzipped NBT).
 *
 * Usage: java SchemToNbt <input.schem> <output.nbt> [dataVersion]
 *
 * Reads the Sponge v2 fields (Width/Height/Length, Palette, BlockData,
 * BlockEntities, optional DataVersion) and emits the structure-block format
 * (DataVersion, size, palette, blocks[], entities[]). Block-entity NBT is
 * carried across (Id -> id, Pos stripped). Entities are dropped.
 */
public final class SchemToNbt {

    // ---- NBT tag ids ----
    static final int TAG_END = 0, TAG_BYTE = 1, TAG_SHORT = 2, TAG_INT = 3, TAG_LONG = 4,
            TAG_FLOAT = 5, TAG_DOUBLE = 6, TAG_BYTE_ARRAY = 7, TAG_STRING = 8, TAG_LIST = 9,
            TAG_COMPOUND = 10, TAG_INT_ARRAY = 11, TAG_LONG_ARRAY = 12;

    // ---- typed wrappers so the writer knows the intended tag id ----
    record Named(int type, String name, Object value) {}
    record TList(int elemType, List<Object> items) {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java SchemToNbt <input.schem> <output.nbt> [dataVersion]");
            System.exit(2);
        }
        File in = new File(args[0]);
        File out = new File(args[1]);
        Integer overrideDv = args.length >= 3 ? Integer.parseInt(args[2]) : null;

        Named root;
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new GZIPInputStream(new FileInputStream(in))))) {
            root = readNamedTag(dis);
        }
        Map<String, Object> r = asCompound(root.value());
        // Sponge v3 nests block data under a "Schematic"/"Blocks" compound; v2 is flat.
        Map<String, Object> schem = r;
        if (r.containsKey("Schematic")) {
            schem = asCompound(r.get("Schematic"));
        }
        Map<String, Object> blocksContainer = schem;
        if (schem.containsKey("Blocks")) { // v3
            blocksContainer = asCompound(schem.get("Blocks"));
        }

        int width = ((Number) schem.get("Width")).intValue() & 0xFFFF;
        int height = ((Number) schem.get("Height")).intValue() & 0xFFFF;
        int length = ((Number) schem.get("Length")).intValue() & 0xFFFF;

        int dataVersion = overrideDv != null ? overrideDv
                : (schem.containsKey("DataVersion") ? ((Number) schem.get("DataVersion")).intValue()
                : (r.containsKey("DataVersion") ? ((Number) r.get("DataVersion")).intValue() : 3955));

        // Palette: compound { "blockstate string": int id }
        Map<String, Object> paletteRaw = asCompound(blocksContainer.containsKey("Palette")
                ? blocksContainer.get("Palette") : schem.get("Palette"));
        // id -> blockstate string
        String[] idToState = new String[paletteRaw.size()];
        for (Map.Entry<String, Object> e : paletteRaw.entrySet()) {
            int id = ((Number) e.getValue()).intValue();
            if (id >= idToState.length) idToState = Arrays.copyOf(idToState, id + 1);
            idToState[id] = e.getKey();
        }

        byte[] blockData = (byte[]) (blocksContainer.containsKey("BlockData")
                ? blocksContainer.get("BlockData") : schem.get("BlockData"));

        // Build the structure palette (list of {Name, Properties}) deduped by raw state string.
        // We keep the same index mapping as the schem palette ids for simplicity.
        List<Object> structPalette = new ArrayList<>();
        for (String state : idToState) {
            structPalette.add(stateToPaletteEntry(state == null ? "minecraft:air" : state));
        }

        // Block entities, keyed by packed position.
        Map<Long, Map<String, Object>> beByPos = new HashMap<>();
        Object beList = blocksContainer.containsKey("BlockEntities") ? blocksContainer.get("BlockEntities")
                : schem.get("BlockEntities");
        if (beList instanceof TList tl) {
            for (Object o : tl.items()) {
                Map<String, Object> be = asCompound(o);
                int[] pos = toIntArray(be.get("Pos"));
                if (pos == null || pos.length < 3) continue;
                Map<String, Object> nbt = new LinkedHashMap<>();
                Object id = be.get("Id");
                if (id != null) nbt.put("id", id);
                for (Map.Entry<String, Object> en : be.entrySet()) {
                    String k = en.getKey();
                    if (k.equals("Pos") || k.equals("Id")) continue;
                    nbt.put(k, en.getValue());
                }
                beByPos.put(pack(pos[0], pos[1], pos[2]), nbt);
            }
        }

        // Decode the varint block data (YZX order) into the structure "blocks" list.
        List<Object> blocks = new ArrayList<>();
        int index = 0; // running cell index
        int i = 0;     // byte cursor
        int total = width * height * length;
        while (i < blockData.length && index < total) {
            int value = 0, varintLen = 0;
            while (true) {
                int b = blockData[i] & 0xFF;
                value |= (b & 0x7F) << (7 * varintLen);
                varintLen++;
                i++;
                if ((b & 0x80) == 0) break;
                if (varintLen > 5) throw new IOException("VarInt too long");
            }
            int x = index % width;
            int z = (index / width) % length;
            int y = index / (width * length);

            Map<String, Object> block = new LinkedHashMap<>();
            block.put("state", new Named(TAG_INT, "state", value));
            block.put("pos", new Named(TAG_LIST, "pos", new TList(TAG_INT,
                    new ArrayList<>(List.of(x, y, z)))));
            Map<String, Object> be = beByPos.get(pack(x, y, z));
            if (be != null) {
                block.put("nbt", new Named(TAG_COMPOUND, "nbt", be));
            }
            blocks.add(block);
            index++;
        }

        // Assemble the root structure compound.
        Map<String, Object> structRoot = new LinkedHashMap<>();
        structRoot.put("DataVersion", new Named(TAG_INT, "DataVersion", dataVersion));
        structRoot.put("size", new Named(TAG_LIST, "size", new TList(TAG_INT,
                new ArrayList<>(List.of(width, height, length)))));
        structRoot.put("palette", new Named(TAG_LIST, "palette", new TList(TAG_COMPOUND, structPalette)));
        structRoot.put("blocks", new Named(TAG_LIST, "blocks", new TList(TAG_COMPOUND, blocks)));
        structRoot.put("entities", new Named(TAG_LIST, "entities", new TList(TAG_COMPOUND, new ArrayList<>())));

        File parent = out.getParentFile();
        if (parent != null) parent.mkdirs();
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(out))))) {
            writeNamedTag(dos, new Named(TAG_COMPOUND, "", structRoot));
        }

        System.out.printf("Converted %s (%dx%dx%d, %d palette, %d blocks, %d block-entities, DataVersion %d) -> %s%n",
                in.getName(), width, height, length, idToState.length, blocks.size(), beByPos.size(), dataVersion,
                out.getPath());
    }

    // ---- blockstate string -> palette compound {Name, Properties?} ----
    static Map<String, Object> stateToPaletteEntry(String state) {
        Map<String, Object> entry = new LinkedHashMap<>();
        int br = state.indexOf('[');
        if (br < 0) {
            entry.put("Name", new Named(TAG_STRING, "Name", state));
            return entry;
        }
        String name = state.substring(0, br);
        String props = state.substring(br + 1, state.lastIndexOf(']'));
        entry.put("Name", new Named(TAG_STRING, "Name", name));
        Map<String, Object> p = new LinkedHashMap<>();
        for (String kv : props.split(",")) {
            int eq = kv.indexOf('=');
            if (eq < 0) continue;
            String k = kv.substring(0, eq).trim();
            String v = kv.substring(eq + 1).trim();
            p.put(k, new Named(TAG_STRING, k, v));
        }
        entry.put("Properties", new Named(TAG_COMPOUND, "Properties", p));
        return entry;
    }

    static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFF) << 28) | ((long) (y & 0x3FFF) << 14) | (z & 0x3FFF);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asCompound(Object o) {
        if (o instanceof Named n) return (Map<String, Object>) n.value();
        return (Map<String, Object>) o;
    }

    static int[] toIntArray(Object o) {
        Object v = (o instanceof Named n) ? n.value() : o;
        if (v instanceof int[] ia) return ia;
        if (v instanceof TList tl) {
            int[] out = new int[tl.items().size()];
            for (int i = 0; i < out.length; i++) out[i] = ((Number) tl.items().get(i)).intValue();
            return out;
        }
        return null;
    }

    // ================= NBT reader =================
    static Named readNamedTag(DataInputStream in) throws IOException {
        int type = in.readUnsignedByte();
        if (type == TAG_END) return new Named(TAG_END, "", null);
        String name = in.readUTF();
        Object value = readPayload(in, type);
        return new Named(type, name, value);
    }

    static Object readPayload(DataInputStream in, int type) throws IOException {
        switch (type) {
            case TAG_BYTE: return in.readByte();
            case TAG_SHORT: return in.readShort();
            case TAG_INT: return in.readInt();
            case TAG_LONG: return in.readLong();
            case TAG_FLOAT: return in.readFloat();
            case TAG_DOUBLE: return in.readDouble();
            case TAG_BYTE_ARRAY: {
                int len = in.readInt();
                byte[] b = new byte[len];
                in.readFully(b);
                return b;
            }
            case TAG_STRING: return in.readUTF();
            case TAG_LIST: {
                int elem = in.readUnsignedByte();
                int len = in.readInt();
                List<Object> list = new ArrayList<>(Math.max(0, len));
                for (int i = 0; i < len; i++) list.add(readPayload(in, elem));
                return new TList(elem, list);
            }
            case TAG_COMPOUND: {
                Map<String, Object> map = new LinkedHashMap<>();
                while (true) {
                    int t = in.readUnsignedByte();
                    if (t == TAG_END) break;
                    String nm = in.readUTF();
                    map.put(nm, readPayload(in, t));
                }
                return map;
            }
            case TAG_INT_ARRAY: {
                int len = in.readInt();
                int[] a = new int[len];
                for (int i = 0; i < len; i++) a[i] = in.readInt();
                return a;
            }
            case TAG_LONG_ARRAY: {
                int len = in.readInt();
                long[] a = new long[len];
                for (int i = 0; i < len; i++) a[i] = in.readLong();
                return a;
            }
            default: throw new IOException("Unknown tag type " + type);
        }
    }

    // ================= NBT writer =================
    static void writeNamedTag(DataOutputStream out, Named tag) throws IOException {
        out.writeByte(tag.type());
        if (tag.type() == TAG_END) return;
        out.writeUTF(tag.name());
        writePayload(out, tag.type(), tag.value());
    }

    @SuppressWarnings("unchecked")
    static void writePayload(DataOutputStream out, int type, Object value) throws IOException {
        switch (type) {
            case TAG_BYTE: out.writeByte(((Number) value).intValue()); break;
            case TAG_SHORT: out.writeShort(((Number) value).intValue()); break;
            case TAG_INT: out.writeInt(((Number) value).intValue()); break;
            case TAG_LONG: out.writeLong(((Number) value).longValue()); break;
            case TAG_FLOAT: out.writeFloat(((Number) value).floatValue()); break;
            case TAG_DOUBLE: out.writeDouble(((Number) value).doubleValue()); break;
            case TAG_BYTE_ARRAY: {
                byte[] b = (byte[]) value;
                out.writeInt(b.length);
                out.write(b);
                break;
            }
            case TAG_STRING: out.writeUTF((String) value); break;
            case TAG_LIST: {
                TList tl = (TList) value;
                int elem = tl.items().isEmpty() ? TAG_END : tl.elemType();
                out.writeByte(elem);
                out.writeInt(tl.items().size());
                for (Object o : tl.items()) {
                    Object payload = (o instanceof Named n) ? n.value() : o;
                    writePayload(out, elem, payload);
                }
                break;
            }
            case TAG_COMPOUND: {
                Map<String, Object> map = (Map<String, Object>) value;
                for (Map.Entry<String, Object> e : map.entrySet()) {
                    Object v = e.getValue();
                    Named n = (v instanceof Named) ? (Named) v : new Named(inferType(v), e.getKey(), v);
                    out.writeByte(n.type());
                    out.writeUTF(e.getKey());
                    writePayload(out, n.type(), n.value());
                }
                out.writeByte(TAG_END);
                break;
            }
            case TAG_INT_ARRAY: {
                int[] a = (int[]) value;
                out.writeInt(a.length);
                for (int v : a) out.writeInt(v);
                break;
            }
            case TAG_LONG_ARRAY: {
                long[] a = (long[]) value;
                out.writeInt(a.length);
                for (long v : a) out.writeLong(v);
                break;
            }
            default: throw new IOException("Cannot write tag type " + type);
        }
    }

    static int inferType(Object v) {
        if (v instanceof Byte) return TAG_BYTE;
        if (v instanceof Short) return TAG_SHORT;
        if (v instanceof Integer) return TAG_INT;
        if (v instanceof Long) return TAG_LONG;
        if (v instanceof Float) return TAG_FLOAT;
        if (v instanceof Double) return TAG_DOUBLE;
        if (v instanceof byte[]) return TAG_BYTE_ARRAY;
        if (v instanceof String) return TAG_STRING;
        if (v instanceof TList) return TAG_LIST;
        if (v instanceof Map) return TAG_COMPOUND;
        if (v instanceof int[]) return TAG_INT_ARRAY;
        if (v instanceof long[]) return TAG_LONG_ARRAY;
        throw new IllegalArgumentException("Cannot infer NBT type for " + v);
    }
}
