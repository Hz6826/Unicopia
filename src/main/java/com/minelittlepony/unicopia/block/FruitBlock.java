package com.minelittlepony.unicopia.block;

import java.util.List;

import com.minelittlepony.unicopia.ability.EarthPonyKickAbility.Buckable;
import com.mojang.datafixers.util.Function5;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.*;

public class FruitBlock extends Block implements Buckable {
    public static final int DEFAULT_FRUIT_SIZE = 5;
    public static final double DEFAULT_STEM_OFFSET = 2.6F;
    public static final VoxelShape DEFAULT_SHAPE = createFruitShape(DEFAULT_STEM_OFFSET, DEFAULT_FRUIT_SIZE);
    private static final MapCodec<FruitBlock> CODEC = createCodec(FruitBlock::new);

    protected final Direction attachmentFace;
    protected final Block stem;
    protected final VoxelShape shape;

    public static <T extends FruitBlock> MapCodec<T> createCodec(Function5<Direction, Block, VoxelShape, Boolean, Settings, T> constructor) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                Direction.CODEC.fieldOf("attachment_face").forGetter(b -> b.attachmentFace),
                Registries.BLOCK.getCodec().fieldOf("stem").forGetter(b -> b.stem),
                RecordCodecBuilder.<VoxelShape>create(i -> i.group(
                        Codec.DOUBLE.fieldOf("stem_offset").forGetter(b -> (double)0),
                        Codec.DOUBLE.fieldOf("fruit_offset").forGetter(b -> (double)0)
                ).apply(i, FruitBlock::createFruitShape)).fieldOf("shape").forGetter(b -> b.shape),
                Codec.BOOL.fieldOf("flammable").forGetter(b -> false),
                BedBlock.createSettingsCodec()
        ).apply(instance, constructor));
    }

    public static VoxelShape createFruitShape(double stemOffset, double fruitSize) {
        final double min = (16 - fruitSize) * 0.5;
        final double max = 16 - min;
        final double top = 16 - stemOffset;
        return createCuboidShape(min, top - fruitSize, min, max, top, max);
    }

    @Override
    public MapCodec<? extends FruitBlock> getCodec() {
        return CODEC;
    }

    public FruitBlock(Direction attachmentFace, Block stem, VoxelShape shape, Settings settings) {
        this(attachmentFace, stem, shape, true, settings.sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY));
    }

    public FruitBlock(Direction attachmentFace, Block stem, VoxelShape shape, boolean flammable, Settings settings) {
        super(settings.nonOpaque().suffocates(BlockConstructionUtils::never).blockVision(BlockConstructionUtils::never));
        this.attachmentFace = attachmentFace;
        this.stem = stem;
        this.shape = shape;

        if (flammable) {
            FlammableBlockRegistry.getDefaultInstance().add(this, 20, 50);
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shape;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos attachedPos = pos.offset(attachmentFace.getOpposite());
        BlockState attachedState = world.getBlockState(attachedPos);
        return canAttachTo(attachedState);
    }

    @Deprecated
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (!state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.canPlaceAt(world, pos)) {
            world.breakBlock(pos, true);
        }
    }

    @Deprecated
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (!newState.isOf(state.getBlock())) {
            BlockState leaves = world.getBlockState(pos.up());
            if (leaves.contains(FruitBearingBlock.STAGE)) {
                world.setBlockState(pos.up(), leaves.withIfExists(FruitBearingBlock.AGE, 0).with(FruitBearingBlock.STAGE, FruitBearingBlock.Stage.IDLE));
            }
        }
    }

    protected boolean canAttachTo(BlockState state) {
        return state.isOf(stem);
    }

    @Override
    public List<ItemStack> onBucked(ServerWorld world, BlockState state, BlockPos pos) {
        List<ItemStack> stacks = Block.getDroppedStacks(state, world, pos, null);
        world.breakBlock(pos, false);
        return stacks;
    }
}
