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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MatrixUtil;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;

public abstract class AbstractPipeBlock extends BlockWithEntity {
    public static final VoxelShape CORE_SHAPE = Block.createCuboidShape(5, 5, 5, 11, 11, 11);
    protected static final VoxelShape CONNECTOR_SHAPE = Block.createCuboidShape(6, 6, 0, 10, 10, 5);
    protected static final VoxelShape END_SHAPE = Block.createCuboidShape( 5, 5, 0, 11, 11, 2);

    public static final HashMap<Direction, VoxelShape> CONNECTOR_SHAPES = new HashMap<>();
    public static final HashMap<Direction, VoxelShape> END_SHAPES = new HashMap<>();

    static {
        for (var direction : DIRECTIONS) {
            CONNECTOR_SHAPES.put(direction, getRotatedShape(CONNECTOR_SHAPE, direction));
            END_SHAPES.put(direction, getRotatedShape(END_SHAPE, direction));
        }
    }

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

    public VoxelShape getConnectionShape(BlockView view, BlockPos pos, Direction direction) {
        var blockEntity = view.getBlockEntity(pos);

        if (blockEntity instanceof AbstractPipeBlockEntity pipeBlockEntity) {
            return getConnectionShape(pipeBlockEntity, direction);
        }

        return null;
    }

    public VoxelShape getConnectionShape(AbstractPipeBlockEntity blockEntity, Direction direction) {
        var connection = blockEntity.getConnection(direction);

        if (connection == null) {
            return null;
        }

        if (connection.isPipe) {
            return CONNECTOR_SHAPES.get(direction);
        } else {
            return VoxelShapes.combineAndSimplify(CONNECTOR_SHAPES.get(direction), END_SHAPES.get(direction), BooleanBiFunction.OR);
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        ArrayList<VoxelShape> voxelShapes = new ArrayList<>();

        // Core, always there
        voxelShapes.add(CORE_SHAPE);

        // Connections
        BlockEntity blockEntity = view.getBlockEntity(pos);
        if (blockEntity instanceof AbstractPipeBlockEntity pipeBlockEntity) {
            for (Direction direction : DIRECTIONS) {
                var shape = getConnectionShape(pipeBlockEntity, direction);
                if (shape != null) {
                    voxelShapes.add(shape);
                }
            }
        }

        return voxelShapes.stream().reduce((v1, v2) -> VoxelShapes.combineAndSimplify(v1, v2, BooleanBiFunction.OR)).get();
    }

    protected static VoxelShape getRotatedShape(VoxelShape shape, Direction direction)
    {
        return switch (direction) {
            case NORTH -> shape;
            case WEST -> getRotatedShape(shape, ShapeRotationAxis.POSITIVE_Y, 1);
            case SOUTH -> getRotatedShape(shape, ShapeRotationAxis.POSITIVE_Y, 2);
            case EAST -> getRotatedShape(shape, ShapeRotationAxis.POSITIVE_Y, 3);
            case UP -> getRotatedShape(shape, ShapeRotationAxis.POSITIVE_X, 1);
            case DOWN -> getRotatedShape(shape, ShapeRotationAxis.POSITIVE_X, 3);
        };
    }

    protected static VoxelShape getRotatedShape(VoxelShape shape, ShapeRotationAxis axis, int steps)
    {
        int stepsMod = steps % 4;

        VoxelShape[] rotatedShape = {VoxelShapes.empty()};

        shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            for (var i = 0; i < stepsMod; i++) {
                var _minX = minX;
                var _minY = minY;
                var _minZ = minZ;
                var _maxX = maxX;
                var _maxY = maxY;
                var _maxZ = maxZ;
                switch (axis) {
                    case POSITIVE_X -> {
                        minY = 1 - _maxZ;
                        maxY = 1 - _minZ;
                        minZ = _minY;
                        maxZ = _maxY;
                    }
                    case NEGATIVE_X -> {
                        minY = _minZ;
                        maxY = _maxZ;
                        minZ = 1 - _maxY;
                        maxZ = 1 - _minY;
                    }
                    case POSITIVE_Y -> {
                        minX = _minZ;
                        maxX = _maxZ;
                        minZ = 1 - _maxX;
                        maxZ = 1 - _minX;
                    }
                    case NEGATIVE_Y -> {
                        minX = 1 - _maxZ;
                        maxX = 1 - _minZ;
                        minZ = _minX;
                        maxZ = _maxX;
                    }
                    case POSITIVE_Z -> {
                        minX = 1 - _maxY;
                        maxX = 1 - _minY;
                        minY = _minX;
                        maxY = _maxX;
                    }
                    case NEGATIVE_Z -> {
                        minX = _minY;
                        maxX = _maxY;
                        minY = 1 - _maxX;
                        maxY = 1 - _minX;
                    }
                }
            }

            rotatedShape[0] = VoxelShapes.combine(rotatedShape[0], VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ), BooleanBiFunction.OR);
        });

        return rotatedShape[0];
    }

    protected enum ShapeRotationAxis {
        POSITIVE_X,
        NEGATIVE_X,
        POSITIVE_Y,
        NEGATIVE_Y,
        POSITIVE_Z,
        NEGATIVE_Z
    }
}
