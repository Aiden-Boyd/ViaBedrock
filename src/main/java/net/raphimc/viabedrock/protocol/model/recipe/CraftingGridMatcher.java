package net.raphimc.viabedrock.protocol.model.recipe;

import net.raphimc.viabedrock.protocol.model.BedrockItem;

import java.util.List;
import java.util.function.BiPredicate;

/** Matches manual crafting inputs and returns the quantity consumed from each grid slot. */
public final class CraftingGridMatcher {

    public static int[] match(final Recipe recipe, final BedrockItem[] grid, final int width,
                              final BiPredicate<ItemDescriptor, BedrockItem> matches) {
        if (grid.length != width * width) {
            throw new IllegalArgumentException("Invalid crafting grid");
        }
        if (recipe instanceof ShapedRecipe shaped) {
            final ItemDescriptor[][] pattern = shaped.getPattern();
            final int height = pattern.length;
            final int patternWidth = height == 0 ? 0 : pattern[0].length;
            if (height == 0 || patternWidth == 0 || height > width || patternWidth > width) {
                return null;
            }
            for (int y = 0; y <= width - height; y++) {
                for (int x = 0; x <= width - patternWidth; x++) {
                    for (int mirror = 0; mirror < 2; mirror++) {
                        final int[] consumed = new int[grid.length];
                        boolean valid = true;
                        for (int slot = 0; slot < grid.length; slot++) {
                            final int gx = slot % width - x;
                            final int gy = slot / width - y;
                            final ItemDescriptor ingredient = gx >= 0 && gx < patternWidth && gy >= 0 && gy < height
                                    ? pattern[gy][mirror == 0 ? gx : patternWidth - 1 - gx] : new ItemDescriptor.InvalidDescriptor();
                            if (!matches.test(ingredient, grid[slot])
                                    || (!grid[slot].isEmpty() && grid[slot].amount() < Math.max(1, ingredient.amount()))) {
                                valid = false;
                                break;
                            }
                            consumed[slot] = grid[slot].isEmpty() ? 0 : Math.max(1, ingredient.amount());
                        }
                        if (valid) {
                            return consumed;
                        }
                    }
                }
            }
        } else if (recipe instanceof ShapelessRecipe shapeless) {
            final List<ItemDescriptor> ingredients = shapeless.getIngredients().stream()
                    .filter(ingredient -> ingredient.getType() != ItemDescriptorType.INVALID).toList();
            int occupied = 0;
            for (BedrockItem item : grid) {
                if (!item.isEmpty()) {
                    occupied++;
                }
            }
            if (occupied != ingredients.size() || ingredients.isEmpty()) {
                return null;
            }
            final int[] consumed = new int[grid.length];
            if (assign(ingredients, 0, grid, consumed, matches)) {
                return consumed;
            }
        }
        return null;
    }

    private static boolean assign(final List<ItemDescriptor> ingredients, final int index, final BedrockItem[] grid,
                                  final int[] consumed, final BiPredicate<ItemDescriptor, BedrockItem> matches) {
        if (index == ingredients.size()) {
            return true;
        }
        final ItemDescriptor ingredient = ingredients.get(index);
        final int amount = Math.max(1, ingredient.amount());
        for (int slot = 0; slot < grid.length; slot++) {
            if (consumed[slot] != 0 || grid[slot].isEmpty() || grid[slot].amount() < amount || !matches.test(ingredient, grid[slot])) {
                continue;
            }
            consumed[slot] = amount;
            if (assign(ingredients, index + 1, grid, consumed, matches)) {
                return true;
            }
            consumed[slot] = 0;
        }
        return false;
    }

    private CraftingGridMatcher() {
    }

}
