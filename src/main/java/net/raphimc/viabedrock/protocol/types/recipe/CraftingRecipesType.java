package net.raphimc.viabedrock.protocol.types.recipe;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;
import net.raphimc.viabedrock.protocol.model.BedrockItem;
import net.raphimc.viabedrock.protocol.model.recipe.*;
import net.raphimc.viabedrock.protocol.storage.CraftingDataStorage;
import net.raphimc.viabedrock.protocol.types.BedrockTypes;
import net.raphimc.viabedrock.protocol.types.InventoryTypes;
import net.raphimc.viabedrock.protocol.types.array.ArrayType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Reads the recipe arrays introduced in Bedrock protocol 2168 and retained by 2193. */
public final class CraftingRecipesType extends Type<CraftingDataStorage[]> {

    private final Type<BedrockItem> itemType;
    private final Type<BedrockItem[]> resultType;

    public CraftingRecipesType(final Type<BedrockItem> itemType) {
        super(CraftingDataStorage[].class);
        this.itemType = itemType;
        this.resultType = new ArrayType<>(itemType, BedrockTypes.UNSIGNED_VAR_INT);
    }

    @Override
    public CraftingDataStorage[] read(final ByteBuf buffer) {
        final List<CraftingDataStorage> recipes = new ArrayList<>();
        this.readArray(buffer, recipes, RecipeType.SHAPED);
        this.readArray(buffer, recipes, RecipeType.SHAPELESS);
        final int multiCount = BedrockTypes.UNSIGNED_VAR_INT.read(buffer);
        for (int i = 0; i < multiCount; i++) {
            BedrockTypes.UUID.read(buffer);
            BedrockTypes.UNSIGNED_VAR_INT.read(buffer);
        }
        this.readArray(buffer, recipes, RecipeType.USER_DATA_SHAPELESS);
        this.readArray(buffer, recipes, RecipeType.SHAPELESS_CHEMISTRY);
        this.readArray(buffer, recipes, RecipeType.SHAPED_CHEMISTRY);
        this.readArray(buffer, recipes, RecipeType.SMITHING_TRANSFORM);
        this.readArray(buffer, recipes, RecipeType.SMITHING_TRIM);
        // Potion mixes, container mixes, and material reducers are not used by manual crafting.
        return recipes.toArray(new CraftingDataStorage[0]);
    }

    private void readArray(final ByteBuf buffer, final List<CraftingDataStorage> recipes, final RecipeType type) {
        final int count = BedrockTypes.UNSIGNED_VAR_INT.read(buffer);
        for (int i = 0; i < count; i++) {
            final String id = BedrockTypes.STRING.read(buffer);
            if (type == RecipeType.SMITHING_TRANSFORM || type == RecipeType.SMITHING_TRIM) {
                final ItemDescriptor template = InventoryTypes.ITEM_DESCRIPTOR_TYPE.read(buffer);
                final ItemDescriptor base = InventoryTypes.ITEM_DESCRIPTOR_TYPE.read(buffer);
                final ItemDescriptor addition = InventoryTypes.ITEM_DESCRIPTOR_TYPE.read(buffer);
                final BedrockItem result = type == RecipeType.SMITHING_TRANSFORM ? this.itemType.read(buffer) : BedrockItem.empty();
                final String tag = BedrockTypes.STRING.read(buffer);
                final int networkId = BedrockTypes.UNSIGNED_VAR_INT.read(buffer);
                recipes.add(new CraftingDataStorage(type, networkId,
                        new SmithingRecipe(id, UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)), tag, 0, template, base, addition, result)));
                continue;
            }

            final boolean shaped = type == RecipeType.SHAPED || type == RecipeType.SHAPED_CHEMISTRY;
            final int width = shaped ? BedrockTypes.VAR_INT.read(buffer) : 0;
            final int height = shaped ? BedrockTypes.VAR_INT.read(buffer) : 0;
            if (shaped && (width <= 0 || height <= 0 || width > 3 || height > 3)) {
                throw new IllegalArgumentException("Invalid recipe dimensions: " + width + "x" + height);
            }
            final ItemDescriptor[] ingredients = InventoryTypes.ITEM_DESCRIPTORS.read(buffer);
            final List<BedrockItem> results = List.of(this.resultType.read(buffer));
            final UUID uuid = BedrockTypes.UUID.read(buffer);
            final String tag = BedrockTypes.STRING.read(buffer);
            final int priority = BedrockTypes.VAR_INT.read(buffer);
            final boolean symmetry = shaped && buffer.readBoolean();
            if (buffer.readBoolean()) {
                BedrockTypes.VAR_INT.read(buffer); // Unlocking context
                if (buffer.readBoolean()) {
                    InventoryTypes.ITEM_DESCRIPTORS.read(buffer);
                }
            }
            final int networkId = BedrockTypes.UNSIGNED_VAR_INT.read(buffer);
            if (shaped) {
                if (ingredients.length != width * height) {
                    throw new IllegalArgumentException("Recipe ingredient count does not match dimensions");
                }
                final ItemDescriptor[][] pattern = new ItemDescriptor[height][width];
                for (int y = 0; y < height; y++) {
                    System.arraycopy(ingredients, y * width, pattern[y], 0, width);
                }
                recipes.add(new CraftingDataStorage(type, networkId, new ShapedRecipe(id, uuid, tag, priority, pattern, results, symmetry)));
            } else {
                recipes.add(new CraftingDataStorage(type, networkId, new ShapelessRecipe(id, uuid, tag, priority, List.of(ingredients), results)));
            }
        }
    }

    @Override
    public void write(final ByteBuf buffer, final CraftingDataStorage[] value) {
        throw new UnsupportedOperationException("Clientbound recipe data");
    }

}
