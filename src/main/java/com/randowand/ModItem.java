package com.randowand;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

public class ModItem extends Item {
    public ModItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        ItemStack wandStack = context.getItemInHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        ItemStack offhandStack = player.getOffhandItem();

        // Shift + Right-Click logic
        if (player.isShiftKeyDown()) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            CompoundTag tag = wandStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            List<ItemStack> storedItems = loadInventoryFromNbt(tag, level);

            if (!offhandStack.isEmpty() && offhandStack.getItem() instanceof BlockItem) {
                ItemStack toStore = offhandStack.copy();

                boolean merged = false;
                for (ItemStack existing : storedItems) {
                    if (ItemStack.isSameItemSameComponents(existing, toStore)) {
                        existing.grow(toStore.getCount());
                        merged = true;
                        break;
                    }
                }
                if (!merged) {
                    storedItems.add(toStore);
                }

                int storedCount = toStore.getCount();
                String itemName = toStore.getHoverName().getString();

                offhandStack.setCount(0);
                saveInventoryToNbt(tag, storedItems, level);
                wandStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                player.displayClientMessage(Component.literal("§aStored " + storedCount + "x " + itemName + " into wand."), true);
                return InteractionResult.SUCCESS;
            } else if (offhandStack.isEmpty()) {
                if (!storedItems.isEmpty()) {
                    ItemStack removed = storedItems.remove(storedItems.size() - 1);
                    int removedCount = removed.getCount();
                    String itemName = removed.getHoverName().getString();

                    if (!player.getInventory().add(removed)) {
                        player.drop(removed, false);
                    }
                    saveInventoryToNbt(tag, storedItems, level);
                    wandStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
                    player.displayClientMessage(Component.literal("§eReturned " + removedCount + "x " + itemName + " from wand."), true);
                    return InteractionResult.SUCCESS;
                } else {
                    player.displayClientMessage(Component.literal("§cWand inventory is empty!"), true);
                    return InteractionResult.FAIL;
                }
            }
        }

        // Normal Right-Click: Place random block from internal wand storage
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        CompoundTag tag = wandStack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        List<ItemStack> storedItems = loadInventoryFromNbt(tag, level);

        if (storedItems.isEmpty()) {
            player.displayClientMessage(Component.literal("§c[RandoWand] No blocks stored inside the wand! Shift+right-click with a block in your offhand to store blocks."), true);
            return InteractionResult.FAIL;
        }

        BlockPos targetPos = clickedPos.relative(face);

        if (level.getBlockState(targetPos).canBeReplaced()) {
            RandomSource random = level.getRandom();
            int randomIndex = random.nextInt(storedItems.size());
            ItemStack chosenStack = storedItems.get(randomIndex);

            if (chosenStack.getItem() instanceof BlockItem blockItem) {
                BlockState stateToPlace = blockItem.getBlock().defaultBlockState();

                if (stateToPlace.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    stateToPlace = stateToPlace.setValue(BlockStateProperties.HORIZONTAL_FACING, player.getDirection().getOpposite());
                }

                boolean placed = level.setBlock(targetPos, stateToPlace, 3);

                if (placed) {
                    chosenStack.shrink(1);
                    if (chosenStack.isEmpty()) {
                        storedItems.remove(randomIndex);
                    }

                    saveInventoryToNbt(tag, storedItems, level);
                    wandStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));

                    if (!player.getAbilities().instabuild && Config.wandDurability > 0) {
                        int oldDamage = wandStack.getDamageValue();
                        wandStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);

                        // Check if the wand just broke from this usage
                        if (wandStack.isEmpty() || wandStack.getDamageValue() >= wandStack.getMaxDamage()) {
                            dropStoredContents(level, player.blockPosition(), storedItems, player);
                        }
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity) {
        super.onDestroyed(itemEntity);
        Level level = itemEntity.level();
        if (!level.isClientSide()) {
            ItemStack stack = itemEntity.getItem();
            CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
            List<ItemStack> storedItems = loadInventoryFromNbt(tag, level);
            dropStoredContents(level, itemEntity.blockPosition(), storedItems, null);
        }
    }

    private void dropStoredContents(Level level, BlockPos pos, List<ItemStack> storedItems, Player player) {
        for (ItemStack stored : storedItems) {
            if (!stored.isEmpty()) {
                if (player != null && player.getInventory().add(stored)) {
                    // Successfully added to player inventory
                } else {
                    // Drop remainder on the floor
                    ItemEntity droppedEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stored);
                    level.addFreshEntity(droppedEntity);
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.randowand.title").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        tooltipComponents.add(Component.translatable("tooltip.randowand.control1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.randowand.control2").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.randowand.control3").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.randowand.control4").withStyle(ChatFormatting.GRAY));

        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        List<ItemStack> storedItems = loadInventoryFromNbt(tag, null);

        if (storedItems.isEmpty()) {
            tooltipComponents.add(Component.literal("Stored Blocks: Empty").withStyle(ChatFormatting.RED));
        } else {
            tooltipComponents.add(Component.literal("Stored Blocks:").withStyle(ChatFormatting.GREEN));
            for (ItemStack stored : storedItems) {
                tooltipComponents.add(Component.literal(" - " + stored.getCount() + "x " + stored.getHoverName().getString()).withStyle(ChatFormatting.WHITE));
            }
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    private List<ItemStack> loadInventoryFromNbt(CompoundTag tag, Level level) {
        List<ItemStack> items = new ArrayList<>();
        if (tag.contains("StoredItems")) {
            ListTag listTag = tag.getList("StoredItems", 10);
            net.minecraft.core.HolderLookup.Provider lookup = level != null ? level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                ItemStack.parse(lookup, itemTag).ifPresent(items::add);
            }
        }
        return items;
    }

    private void saveInventoryToNbt(CompoundTag tag, List<ItemStack> items, Level level) {
        ListTag listTag = new ListTag();
        net.minecraft.core.HolderLookup.Provider lookup = level != null ? level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                listTag.add(stack.save(lookup, itemTag));
            }
        }
        tag.put("StoredItems", listTag);
    }
}