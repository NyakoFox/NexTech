package gay.nyako.nextech.model;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.*;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class PipeModel implements FabricBakedModel, BakedModel, UnbakedModel {
    private static final Identifier coreModelId = Identifier.of("nextech", "block/pipe/core");
    private static final Identifier connectorModelId = Identifier.of("nextech", "block/pipe/connector");
    public static final List<Identifier> MODEL_DEPENDENCIES = List.of(coreModelId, connectorModelId);

    public PipeModel()
    {
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        return List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean hasDepth() {
        return false;
    }

    @Override
    public boolean isSideLit() {
        return true;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getParticleSprite() {
        return MinecraftClient.getInstance().getBakedModelManager().getModel(coreModelId).getParticleSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        var coreModel = MinecraftClient.getInstance().getBakedModelManager().getModel(coreModelId);
        return coreModel.getTransformation();
    }

    @Override
    public ModelOverrideList getOverrides() {
        return ModelOverrideList.EMPTY;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        var client = MinecraftClient.getInstance();
        var bakedModelManager = client.getBakedModelManager();

        var coreModel = (FabricBakedModel) bakedModelManager.getModel(coreModelId);
        var connectorModel = (FabricBakedModel) bakedModelManager.getModel(connectorModelId);

        coreModel.emitBlockQuads(blockView, state, pos, randomSupplier, context);

        var data = blockView.getBlockEntityRenderData(pos);
        if (data instanceof boolean[] connections) {
            for (int i = 0; i < connections.length; i++) {
                if (connections[i]) {
                    var direction = Direction.byId(i);
                    pushTransform(direction, context);

                    connectorModel.emitBlockQuads(blockView, state, pos, randomSupplier, context);

                    context.popTransform();
                }
            }
        }
    }

    private void pushTransform(Direction direction, RenderContext context) {
        var angle = switch (direction) {
            case NORTH -> RotationAxis.POSITIVE_Y.rotationDegrees(0.0F);
            case SOUTH -> RotationAxis.POSITIVE_Y.rotationDegrees(180.0F);
            case WEST -> RotationAxis.POSITIVE_Y.rotationDegrees(90.0F);
            case EAST -> RotationAxis.POSITIVE_Y.rotationDegrees(270.0F);
            case UP -> RotationAxis.POSITIVE_X.rotationDegrees(90.0F);
            case DOWN -> RotationAxis.POSITIVE_X.rotationDegrees(270.0F);
        };

        context.pushTransform(new RotationQuadTransform(angle));
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        var coreModel = (FabricBakedModel) MinecraftClient.getInstance().getBakedModelManager().getModel(coreModelId);
        coreModel.emitItemQuads(stack, randomSupplier, context);
    }

    @Override
    public Collection<Identifier> getModelDependencies() {
        return MODEL_DEPENDENCIES;
    }

    @Override
    public void setParents(Function<Identifier, UnbakedModel> modelLoader) {
        modelLoader.apply(coreModelId).setParents(modelLoader);
        modelLoader.apply(connectorModelId).setParents(modelLoader);
    }

    @Nullable
    @Override
    public BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter, ModelBakeSettings rotationContainer) {
        return this;
    }

    private static class RotationQuadTransform implements RenderContext.QuadTransform {
        private final Quaternionf rotation;

        public RotationQuadTransform(Quaternionf rotation) {
            this.rotation = rotation;
        }

        @Override
        public boolean transform(MutableQuadView quad) {
            for (var i = 0; i < 4; ++i) {
                var pos = quad.copyPos(i, null);
                pos.sub(new Vector3f(8.0F / 16.0F, 8.0F / 16.0F, 8.0F / 16.0F));
                pos.rotate(rotation);
                pos.add(new Vector3f(8.0F / 16.0F, 8.0F / 16.0F, 8.0F / 16.0F));

                quad.pos(i, pos);
            }

            return true;
        }
    }
}
