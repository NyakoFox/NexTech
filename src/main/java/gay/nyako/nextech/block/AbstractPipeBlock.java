package gay.nyako.nextech.block;

import com.mojang.serialization.MapCodec;
import gay.nyako.nextech.network.Connections;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public abstract class AbstractPipeBlock extends BlockWithEntity {
    ArrayList<BlockPos> network;

    public AbstractPipeBlock(Settings settings) {
        super(settings);
        network = new ArrayList<>();
    }

    @Override
    protected boolean isShapeFullCube(BlockState state, BlockView world, BlockPos pos) {
        return false;
    }

    @Override
    protected boolean isCullingShapeFullCube(BlockState state, BlockView world, BlockPos pos) {
        return false;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    public void remakeNetwork(World world, BlockPos pos)
    {
        ArrayList<BlockPos> network = new ArrayList<>();
        ArrayList<BlockPos> blocksChecked = new ArrayList<>();

        network.add(pos);

        Queue<BlockPos> blocksToCheck = new LinkedList<>();
        blocksToCheck.add(pos);

        while (!blocksToCheck.isEmpty()) {
            BlockPos blockPos = blocksToCheck.poll();
            if (world.getBlockEntity(blockPos) instanceof AbstractPipeBlockEntity pipeBlockEntity)
            {
                for (Direction direction : DIRECTIONS) {
                    boolean connected = pipeBlockEntity.isSideConnected(direction);
                    if (connected)
                    {
                        BlockPos newPos = blockPos.offset(direction);
                        if (blocksChecked.contains(newPos)) continue;
                        blocksChecked.add(newPos);

                        if (connectsTo(world, newPos))
                        {
                            network.add(newPos);
                            if (world.getBlockEntity(newPos) instanceof AbstractPipeBlockEntity) {
                                blocksToCheck.add(newPos);
                            }
                        }

                    }
                }
            }
        }

        this.network = network;
    }

    public abstract boolean connectsTo(World world, BlockPos pos);

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        updateConnections(world, pos);
        remakeNetwork(world, pos);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (!world.isClient) {
            updateConnections(world, pos);
            remakeNetwork(world, pos);
        }
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (!world.isClient) {
            updateConnections(world, pos);
            remakeNetwork(world, pos);
        }
    }

    public void updateConnections(World world, BlockPos pos)
    {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof AbstractPipeBlockEntity pipeBlockEntity)
        {
            Connections connections = new Connections();

            for (Direction direction : DIRECTIONS)
            {
                BlockPos offsetPos = pos.add(direction.getVector());
                if (connectsTo(world, offsetPos))
                {
                    connections.addConnection(direction, new Connections.Connection(offsetPos, false));
                }
                else if (world.getBlockState(offsetPos).getBlock() == this)
                {
                    connections.addConnection(direction, new Connections.Connection(offsetPos, true));
                }
            }

            pipeBlockEntity.setConnections(connections);
            pipeBlockEntity.markDirty();
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        ArrayList<VoxelShape> voxelShapes = new ArrayList<>();

        // Core, always there
        voxelShapes.add(Block.createCuboidShape(5, 5, 5, 11, 11, 11));

        // Connections

        BlockEntity blockEntity = view.getBlockEntity(pos);
        if (blockEntity instanceof AbstractPipeBlockEntity pipeBlockEntity) {
            for (Direction direction : DIRECTIONS) {
                if (pipeBlockEntity.isSideConnected(direction)) {
                    switch (direction) {
                        case NORTH:
                            voxelShapes.add(Block.createCuboidShape(6, 6, 0, 10, 10, 5));
                            break;
                        case SOUTH:
                            voxelShapes.add(Block.createCuboidShape(6, 6, 11, 10, 10, 16));
                            break;
                        case EAST:
                            voxelShapes.add(Block.createCuboidShape(10, 6, 6, 16, 10, 10));
                            break;
                        case WEST:
                            voxelShapes.add(Block.createCuboidShape(0, 6, 6, 11, 10, 10));
                            break;
                        case UP:
                            voxelShapes.add(Block.createCuboidShape(6, 11, 6, 10, 16, 10));
                            break;
                        case DOWN:
                            voxelShapes.add(Block.createCuboidShape(6, 0, 6, 10, 5, 10));
                            break;
                    }
                }
            }
        }

        return voxelShapes.stream().reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();
    }
}
