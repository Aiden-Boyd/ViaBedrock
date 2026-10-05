import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;

public class ExportBlockConnections {
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String value(Property property, Comparable value) {
        return property.getName(value);
    }

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var result = new TreeMap<String, Object>();
        for (var block : BuiltInRegistries.BLOCK) {
            var states = new TreeMap<String, Integer>();
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                int flags = state.isRedstoneConductor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) ? 1 : 0;
                if (state.isSignalSource()) flags |= 2;
                Direction[] faces = {Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
                for (int i = 0; i < faces.length; i++) {
                    if (state.isFaceSturdy(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, faces[i])) flags |= 4 << i;
                }
                var properties = new TreeMap<String, String>();
                state.getValues().forEach(entry -> properties.put(entry.property().getName(), value(entry.property(), entry.value())));
                String name = BuiltInRegistries.BLOCK.getKey(block).toString();
                if (!properties.isEmpty()) name += "[" + properties.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(java.util.stream.Collectors.joining(",")) + "]";
                states.put(name, flags);
            }
            result.put(BuiltInRegistries.BLOCK.getKey(block).toString(), states.values().stream().distinct().count() == 1 ? states.firstEntry().getValue() : states);
        }
        Files.writeString(Path.of("block_connection_properties.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result));
    }
}
