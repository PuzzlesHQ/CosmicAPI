package io.github.puzzle.cosmic.impl.client.item;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectMap;
import com.github.puzzle.core.loader.util.Reflection;
import com.github.puzzle.game.engine.items.ItemModelBuilder;
import com.github.puzzle.game.engine.items.model.IPuzzleItemModel;
import com.github.puzzle.game.engine.shaders.ItemShader;
import com.github.puzzle.game.mixins.client.accessors.ItemRenderAccessor;
import com.github.puzzle.game.resources.PuzzleGameAssetLoader;
import com.github.puzzle.game.util.DataTagUtil;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.ImmutablePair;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;
import finalforeach.cosmicreach.blocks.BlockPosition;
import finalforeach.cosmicreach.entities.Entity;
import finalforeach.cosmicreach.gamestates.InGame;
import finalforeach.cosmicreach.items.Item;
import finalforeach.cosmicreach.items.ItemStack;
import finalforeach.cosmicreach.rendering.items.ItemRenderer;
import finalforeach.cosmicreach.rendering.meshes.MeshData;
import finalforeach.cosmicreach.rendering.RenderOrder;
import finalforeach.cosmicreach.rendering.shaders.GameShader;
import finalforeach.cosmicreach.ui.UI;
import finalforeach.cosmicreach.util.Identifier;
import finalforeach.cosmicreach.world.Sky;
import finalforeach.cosmicreach.world.Zone;
import io.github.puzzle.cosmic.api.data.point.IDataPointManifest;
import io.github.puzzle.cosmic.api.item.IPuzzleItem;
import io.github.puzzle.cosmic.api.item.IPuzzleItemStack;
import io.github.puzzle.cosmic.impl.data.point.DataPointManifest;
import io.github.puzzle.cosmic.impl.data.point.array.ListDataPoint;
import io.github.puzzle.cosmic.impl.data.point.single.EnumDataPoint;
import io.github.puzzle.cosmic.impl.data.point.single.IdentifierDataPoint;
import io.github.puzzle.cosmic.impl.data.point.single.PairDataPoint;
import io.github.puzzle.cosmic.item.AbstractCosmicItem;
import io.github.puzzle.cosmic.item.ItemDataPointSpecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.function.Function;

import static finalforeach.cosmicreach.rendering.items.ItemRenderer.registerItemModelCreator;

public class CosmicItemModel implements IPuzzleItemModel {

    private static final Logger log = LoggerFactory.getLogger(CosmicItemModel.class);
    static HashMap<String, Texture> ITEM_TEXTURE_CACHE = new HashMap<>();
    static HashMap<String, Mesh> ITEM_MESH_CACHE = new HashMap<>();
    static HashMap<String, Pair<String, String>> INDEX_CACHE = new HashMap<>();
    static Mesh _2D_MESH;

    GameShader program = new MeshData(ItemShader.DEFAULT_ITEM_SHADER, RenderOrder.FULLY_TRANSPARENT).getShader();

    static Matrix4 noRotMtrx = new Matrix4();
    static Camera itemCam2 = new OrthographicCamera(100F, 100F);

    static {
        noRotMtrx.setTranslation(0, -1f, 0);

        itemCam2.position.set(0, 0, 2);
        itemCam2.lookAt(0, 0, 0);
        ((OrthographicCamera) itemCam2).zoom = 0.027F;
        itemCam2.update();
    }

    AbstractCosmicItem item;

    public CosmicItemModel(AbstractCosmicItem item) {
        DataPointManifest manifest = item.pGetPointManifest();
        this.item = item;

        boolean isOld = false;
        if (manifest.has(ItemDataPointSpecs.TEXTURE_LOCATION) && manifest.has(ItemDataPointSpecs.MODEL_TYPE)) {
            Identifier location = manifest.get(ItemDataPointSpecs.TEXTURE_LOCATION).cast(Identifier.class).getValue();
            location = location.getName().startsWith("textures/items/") ? location : Identifier.of(location.getNamespace(), "textures/items/" + location.getName());
            IPuzzleItem.ItemModelType modelType = manifest.get(ItemDataPointSpecs.MODEL_TYPE).getValue();


            if (!ITEM_MESH_CACHE.containsKey(item.getID() + "_" + location + "_" + modelType + "_model")){
                Texture localTex;

                if (ITEM_TEXTURE_CACHE.containsKey(item.getID() + "_" + location + "_texture")) {
                    localTex = ITEM_TEXTURE_CACHE.get(item.getID() + "_" + location + "_texture");
                } else localTex = ItemModelBuilder.flip(PuzzleGameAssetLoader.LOADER.getResource(location, Texture.class));

                Mesh m = null;
                switch (modelType) {
                    case ITEM_MODEL_2D -> {
                        if (_2D_MESH != null) m = _2D_MESH;
                        else _2D_MESH = ItemModelBuilder.build2DMesh();
                    }
                    case ITEM_MODEL_3D -> m = ItemModelBuilder.build2_5DMesh(localTex);
                }
                ITEM_MESH_CACHE.put(item.getID() + "_" + location + "_" + modelType + "_model", m);
                ITEM_TEXTURE_CACHE.put(item.getID() + "_" + location + "_texture", localTex);
                INDEX_CACHE.put(item.getID() + "_0", new ImmutablePair<>(
                        item.getID() + "_" + location + "_" + modelType + "_model",
                        item.getID() + "_" + location + "_texture"
                ));
                isOld = true;
            }
        }

        if (item.pGetPointManifest().has(ItemDataPointSpecs.TEXTURE_DICT)) {
            int index = isOld ? 1 : 0;
            for (PairDataPoint<EnumDataPoint<IPuzzleItem.ItemModelType>, IdentifierDataPoint> pairAttribute : item.pGetPointManifest().get(ItemDataPointSpecs.TEXTURE_DICT).getValue()) {
                Pair<EnumDataPoint<IPuzzleItem.ItemModelType>, IdentifierDataPoint> pair = pairAttribute.getValue();
                Identifier location = pair.getRight().getValue();
                location = location.getName().startsWith("textures/items/") ? location : Identifier.of(location.getNamespace(), "textures/items/" + location.getName());
                IPuzzleItem.ItemModelType modelType = pair.getLeft().getValue();

                if (!ITEM_MESH_CACHE.containsKey(item.getID() + "_" + location + "_" + modelType + "_model")) {
                    Texture localTex;

                    if (ITEM_TEXTURE_CACHE.containsKey(item.getID() + "_" + location + "_texture")) {
                        localTex = ITEM_TEXTURE_CACHE.get(item.getID() + "_" + location + "_texture");
                    } else
                        localTex = ItemModelBuilder.flip(PuzzleGameAssetLoader.LOADER.getResource(location, Texture.class));

                    Mesh m = null;
                    switch (modelType) {
                        case ITEM_MODEL_2D -> {
                            if (_2D_MESH != null) m = _2D_MESH;
                            else _2D_MESH = ItemModelBuilder.build2DMesh();
                        }
                        case ITEM_MODEL_3D -> m = ItemModelBuilder.build2_5DMesh(localTex);
                    }

                    ITEM_MESH_CACHE.put(item.getID() + "_" + location + "_" + modelType + "_model", m);
                    ITEM_TEXTURE_CACHE.put(item.getID() + "_" + location + "_texture", localTex);
                    INDEX_CACHE.put(item.getID() + "_" + index, new ImmutablePair<>(
                            item.getID() + "_" + location + "_" + modelType + "_model",
                            item.getID() + "_" + location + "_texture"
                    ));
                    index++;
                }
            }
        }
    }

    static final Color tintColor = new Color();
    static final BlockPosition tmpBlockPos = new BlockPosition(null, 0, 0, 0);

    public Mesh getMeshFromIndex(int i) {
        String meshId = INDEX_CACHE.get(item.getID() + "_" + i).getLeft();
        return ITEM_MESH_CACHE.get(meshId);
    }

    public Texture getTextureFromIndex(int i) {
        String meshId = INDEX_CACHE.get(item.getID() + "_" + i).getRight();
        return ITEM_TEXTURE_CACHE.get(meshId);
    }

    public void renderGeneric(Vector3 pos, ItemStack stack, Camera cam, Matrix4 tmpMatrix, boolean isSlot) {
        IDataPointManifest stackManifest;
        try {
            stackManifest = IPuzzleItemStack.as(stack).pGetPointManifest();
        } catch (Exception ignore) {
            stackManifest = null;
        }

        int currentEntry;
        if (stackManifest != null) {
            currentEntry = stackManifest.has(ItemDataPointSpecs.TEXTURE_INDEX) ? stackManifest.get(ItemDataPointSpecs.TEXTURE_INDEX).getValue() : 0;
            currentEntry = currentEntry >= item.getTextures().size() ? 0 : currentEntry;
        } else currentEntry = 0;
        if (isSlot) {
            tintColor.set(Color.WHITE);
        } else {
            Zone zone = InGame.getLocalPlayer().getZone();
            try {
                Entity.setLightingColor(zone, pos, Sky.currentSky.currentAmbientColor, tintColor, tmpBlockPos, tmpBlockPos);
            } catch (Exception e) {
                tintColor.set(Color.WHITE);
            }
        }

        program.bind(cam);
        program.bindOptionalMatrix4("u_projViewTrans", cam.combined);
        program.bindOptionalMatrix4("u_modelMat", tmpMatrix);
        program.bindOptionalUniform4f("tintColor", tintColor);
        program.bindOptionalInt("isInSlot", isSlot ? 1 : 0);
        program.bindOptionalTexture("texDiffuse", getTextureFromIndex(currentEntry), 0);
        if (getMeshFromIndex(currentEntry) != null)
            getMeshFromIndex(currentEntry).render(program.shader, GL20.GL_TRIANGLES);
        program.unbind();
    }

    @Override
    public void renderInSlot(Vector3 pos, ItemStack stack, Camera slotCamera, Matrix4 tmpMatrix, boolean useAmbientLighting) {
        renderGeneric(new Vector3(0, 0, 0), stack, slotCamera, noRotMtrx, true);
    }

    @Override
    public void dispose(WeakReference<Item> itemRef) {
        for (int i = 0; i < item.getTextures().size(); i++) {
            getMeshFromIndex(i).dispose();
            getTextureFromIndex(i).dispose();
        }
    }

    @Override
    public Camera getItemSlotCamera() {
        return itemCam2;
    }

    static final PerspectiveCamera heldItemCamera = new PerspectiveCamera();

    @Override
    public void renderAsHeldItem(Vector3 pos, ItemStack stack2, Camera handCam, float popUpTimer, float maxPopUpTimer, float swingTimer, float maxSwingTimer) {
        Matrix4 tmpHeldMat4 = new Matrix4();
        ItemStack stack = UI.hotbar.getSelectedItemStack();

        heldItemCamera.fieldOfView = 50.0F;
        heldItemCamera.viewportHeight = handCam.viewportHeight;
        heldItemCamera.viewportWidth = handCam.viewportWidth;
        heldItemCamera.near = handCam.near;
        heldItemCamera.far = handCam.far;
        heldItemCamera.update();
        tmpHeldMat4.idt();
        float swing;
        if (popUpTimer > 0.0F) {
            swing = (float)Math.pow(popUpTimer / maxPopUpTimer, 2.0);
            tmpHeldMat4.translate(0.0F, -1.0F * swing, 0.0F);
        }

        tmpHeldMat4.translate(1.65F, -1.25F, -2.5F);
        tmpHeldMat4.rotate(Vector3.Y, -75.0F);
        tmpHeldMat4.translate(-0.25F, -0.25F, -0.25F);
        if (swingTimer > 0.0F) {
            swing = swingTimer / maxSwingTimer;
            swing = 1.0F - (float)Math.pow(swing - 0.5F, 2.0) / 0.25F;
            tmpHeldMat4.rotate(Vector3.Z, 90.0F * swing);
            float st = -swing;
            tmpHeldMat4.translate(st * 2.0F, st, 0.0F);
        }

        if (item.pIsTool()) {
            tmpHeldMat4.translate(.6f,0, 0);
            tmpHeldMat4.translate(0,-.2f, 0);
            tmpHeldMat4.rotate(new Vector3(0, 0, 1), 20);
            tmpHeldMat4.rotate(new Vector3(1, 0, 0), 15);
        }

        Gdx.gl.glDepthFunc(GL20.GL_ALWAYS);
        renderGeneric(pos, stack, heldItemCamera, tmpHeldMat4, false);
        Gdx.gl.glDepthFunc(GL20.GL_LESS);
    }

    @Override
    public void renderAsEntity(Vector3 pos, ItemStack stack, Camera entityCam, Matrix4 tmpMatrix) {
        tmpMatrix.translate(0.5F, 0.2F, 0.5F);
        tmpMatrix.scale(0.7f, 0.7f, 0.7f);
        renderGeneric(pos, stack, entityCam, tmpMatrix, false);
    }

    public static <T extends AbstractCosmicItem> T registerItemModel(T item) {
        ItemRenderAccessor.getRefMap().put(item, new WeakReference<>(item));
        ObjectMap<Class<? extends Item>, Function<?, Item>> modelCreators = Reflection.getFieldContents(ItemRenderer.class, "modelCreators");

        if (!modelCreators.containsKey(item.getClass())) {
            registerItemModelCreator(item.getClass(), (item0) -> new CosmicItemModel(item0.get()).wrap());
        }
        return item;
    }
}
