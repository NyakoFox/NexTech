package gay.nyako.nextech;

import gay.nyako.nextech.model.PipeModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class NexTechModelLoadingPlugin implements ModelLoadingPlugin {
    public static final ModelIdentifier PIPE_MODEL = new ModelIdentifier(Identifier.of("nextech", "pipe"), "");
    public static final ModelIdentifier PIPE_MODEL_ITEM = new ModelIdentifier(Identifier.of("nextech", "pipe"), "inventory");

    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        pluginContext.addModels(PipeModel.MODEL_DEPENDENCIES);
        pluginContext.modifyModelOnLoad().register((original, context) -> {
            // This is called for every model that is loaded, so make sure we only target ours
            final ModelIdentifier id = context.topLevelId();
            if (id != null && (id.equals(PIPE_MODEL) || id.equals(PIPE_MODEL_ITEM))) {
                return new PipeModel();
            } else {
                // If we don't modify the model we just return the original as-is
                return original;
            }
        });
    }
}
