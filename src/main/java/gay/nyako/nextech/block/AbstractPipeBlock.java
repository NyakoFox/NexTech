package gay.nyako.nextech.block;

import gay.nyako.nextech.NexTech;
import gay.nyako.nextech.network.ConnectionMode;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.ArrayList;

public abstract class AbstractPipeBlock extends BlockWithEntity {
    public AbstractPipeBlock(Settings settings) {
        super(settings);
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

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        updateConnections(world, pos);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        super.onStateReplaced(state, world, pos, newState, moved);
        if (!world.isClient) {
            updateConnections(world, pos);
        }
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (!world.isClient) {
            updateConnections(world, pos);
        }
    }

    public void updateConnections(World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof AbstractPipeBlockEntity pipeBlockEntity) {
            pipeBlockEntity.updateConnections();
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player != null && !player.getStackInHand(Hand.MAIN_HAND).isEmpty() && player.getStackInHand(Hand.MAIN_HAND).getItem() == Items.STICK) {
            if (world.isClient) {
                return ActionResult.SUCCESS;
            }

            var blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof AbstractPipeBlockEntity pipeBlockEntity) {
                NexTech.LOGGER.info("==== DEBUGGING PIPE! ====");

                var network = pipeBlockEntity.getPipeNetwork();

                if (!network.containsNode(pipeBlockEntity)) {
                    NexTech.LOGGER.info("Not in a network!");
                }

                var endpoints = network.getEndpointsFrom(pipeBlockEntity.getPipeNetworkNode());

                for (var endpoint : endpoints) {
                    var node = endpoint.node();

                    NexTech.LOGGER.info("[{}] | {} | Direction: {} | Distance: {}", node.getPipePos().toShortString(), node.getConnectionMode().toString(), node.getPipeSide().toString(), endpoint.distance());
                }
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
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
