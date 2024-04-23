package org.figuramc.figura.model;

import com.google.common.collect.ImmutableMap;
import net.minecraft.nbt.*;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.animation.Animation;
import org.figuramc.figura.animation.Interpolation;
import org.figuramc.figura.animation.Keyframe;
import org.figuramc.figura.animation.TransformType;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.math.vector.FiguraVec2;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.math.vector.FiguraVec4;
import org.figuramc.figura.model.rendering.Vertex;
import org.figuramc.figura.model.rendering.texture.FiguraTextureSet;
import org.figuramc.figura.model.rendering.texture.RenderTypes;
import org.figuramc.figura.utils.MathUtils;
import org.figuramc.figura.utils.NbtType;
import org.figuramc.figura.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Take the reading code out of FiguraModelPart itself, since that class
 * was becoming really massive. Reduces bloat slightly
 */
public class FiguraModelPartReader {

    public static FiguraModelPart read(Avatar owner, NBTTagCompound partCompound, List<FiguraTextureSet> textureSets, boolean smoothNormals) {
        // Read name
        String name = partCompound.getString("name");

        // Read transformation
        PartCustomization customization = new PartCustomization();
        customization.needsMatrixRecalculation = true;

        FiguraVec3 rot = FiguraVec3.of();
        readVec3(rot, partCompound, "rot");
        customization.setRot(rot);

        FiguraVec3 piv = FiguraVec3.of();
        readVec3(piv, partCompound, "piv");
        customization.setPivot(piv);

        if (partCompound.hasKey("primary")) {
            try {
                customization.setPrimaryRenderType(RenderTypes.valueOf(partCompound.getString("primary")));
            } catch (Exception ignored) {}
        }
        if (partCompound.hasKey("secondary")) {
            try {
                customization.setSecondaryRenderType(RenderTypes.valueOf(partCompound.getString("secondary")));
            } catch (Exception ignored) {}
        }

        if (partCompound.hasKey("vsb"))
            customization.visible = partCompound.getBoolean("vsb");

        // textures
        List<Integer> facesByTexture = new ArrayList<>(0);
        while (textureSets.size() > facesByTexture.size())
            facesByTexture.add(0);

        // Read vertex data
        Map<Integer, List<Vertex>> vertices = new HashMap<>();
        if (hasCubeData(partCompound)) {
            readCuboid(facesByTexture, partCompound, vertices);
            customization.partType = PartCustomization.PartType.CUBE;
        } else if (hasMeshData(partCompound)) {
            readMesh(facesByTexture, partCompound, vertices);
            customization.partType = PartCustomization.PartType.MESH;
        }

        // smooth normals
        if (partCompound.hasKey("smo"))
            smoothNormals = partCompound.getBoolean("smo");

        if (Configs.FORCE_SMOOTH_AVATAR.value || (smoothNormals && !vertices.isEmpty()))
            smoothfy(vertices);

        // Read children
        ArrayList<FiguraModelPart> children = new ArrayList<>(0);
        if (partCompound.hasKey("chld")) {
            NBTTagList listTag = partCompound.getTagList("chld", NbtType.COMPOUND.getValue());
            for (int currTag = 0; currTag < listTag.tagCount(); currTag++){
                children.add(read(owner, listTag.getCompoundTagAt(currTag), textureSets, smoothNormals));
            }
        }

        FiguraModelPart result = new FiguraModelPart(owner, name, customization, vertices, children);

        for (FiguraModelPart child : children)
            child.parent = result;

        result.facesByTexture = facesByTexture;
        storeTextures(result, textureSets);
        if (partCompound.hasKey("pt")) {
            try {
                result.parentType = ParentType.valueOf(partCompound.getString("pt"));
            } catch (Exception ignored) {}
        }

        // Read animations :D
        if (partCompound.hasKey("anim")) {
            NBTTagList nbt = partCompound.getTagList("anim", NbtType.COMPOUND.getValue());
            for (int currTag = 0; currTag < nbt.tagCount(); currTag++){

                NBTTagCompound compound = nbt.getCompoundTagAt(currTag);
                Animation animation;

                if (!compound.hasKey("id") || !compound.hasKey("data") || (animation = owner.animations.get(compound.getInteger("id"))) == null)
                    continue;

                NBTTagCompound animNbt = compound.getCompoundTag("data");
                for (String channelString : animNbt.getKeySet()) {
                    TransformType type;
                    switch (channelString) {
                        case "pos":
                            type = TransformType.POSITION;
                            break;
                        case "rot":
                            type = TransformType.ROTATION;
                            break;
                        case "grot":
                            type = TransformType.GLOBAL_ROT;
                            break;
                        case "scl":
                            type = TransformType.SCALE;
                            break;
                        default:
                            type = null;
                            break;
                    }

                    if (type == null)
                        continue;

                    List<Keyframe> keyframes = new ArrayList<>();
                    NBTTagList keyframeList = animNbt.getTagList(channelString, NbtType.COMPOUND.getValue());

                    for (int keyframeTag = 0; keyframeTag < nbt.tagCount(); keyframeTag++){
                        NBTTagCompound keyframeNbt = keyframeList.getCompoundTagAt(keyframeTag);
                        float time = keyframeNbt.getFloat("time");
                        Interpolation interpolation;
                        try {
                            interpolation = Interpolation.valueOf(keyframeNbt.getString("int").toUpperCase());
                        } catch (Exception e) {
                            FiguraMod.LOGGER.error("Invalid interpolation type in the model {}, something is wrong with this model!", keyframeNbt.getString("int"));
                            FiguraMod.LOGGER.error("", e);
                            continue;
                        }

                        Pair<FiguraVec3, String[]> pre = parseKeyframeData(keyframeNbt, "pre");
                        if (pre == null) pre = Pair.of(FiguraVec3.of(), null);
                        Pair<FiguraVec3, String[]> end = parseKeyframeData(keyframeNbt, "end");
                        if (end == null) end = pre;

                        FiguraVec3 bezierLeft = FiguraVec3.of();
                        FiguraVec3 bezierRight = FiguraVec3.of();
                        readVec3(bezierLeft, keyframeNbt, "bl");
                        readVec3(bezierRight, keyframeNbt, "br");

                        FiguraVec3 bezierLeftTime = FiguraVec3.of(-0.1, -0.1, -0.1);
                        FiguraVec3 bezierRightTime = FiguraVec3.of(0.1, 0.1, 0.1);
                        readVec3(bezierLeftTime, keyframeNbt, "blt");
                        readVec3(bezierRightTime, keyframeNbt, "brt");
                        bezierLeftTime.add(1, 1, 1);
                        bezierLeftTime = MathUtils.clamp(bezierLeftTime, 0, 1);
                        bezierRightTime = MathUtils.clamp(bezierRightTime, 0, 1);

                        keyframes.add(new Keyframe(owner, animation, time, interpolation, pre, end, bezierLeft, bezierRight, bezierLeftTime, bezierRightTime));
                    }

                    keyframes.sort(Keyframe::compareTo);
                    animation.addAnimation(result, new Animation.AnimationChannel(type, keyframes.toArray(new Keyframe[0])));
                }
            }
        }

        return result;
    }

    private static Pair<FiguraVec3, String[]> parseKeyframeData(NBTTagCompound keyframeNbt, String tag) {
        if (!keyframeNbt.hasKey(tag))
            return null;

        NBTTagList floatList = keyframeNbt.getTagList(tag, NbtType.FLOAT.getValue());
        if (!floatList.hasNoTags()) {
            FiguraVec3 ret = FiguraVec3.of();
            readVec3(ret, floatList);
            return Pair.of(ret, null);
        } else {
            NBTTagList stringList = keyframeNbt.getTagList(tag, NbtType.STRING.getValue());
            return Pair.of(null, new String[]{stringList.getStringTagAt(0), stringList.getStringTagAt(1), stringList.getStringTagAt(2)});
        }
    }

    /**
     * There's a lot of obscure cases to test this on, so... something might go wrong with it, and I can't test everything.
     * Obviously I *think* it should work, and it has so far, but I still might be missing something.
     */
    private static void storeTextures(FiguraModelPart modelPart, List<FiguraTextureSet> textureSets) {
        // textures
        List<FiguraTextureSet> list = new ArrayList<>(0);
        for (int j = 0; j < modelPart.facesByTexture.size(); j++)
            list.add(textureSets.get(j));
        modelPart.textures = list;

        // size
        int w = -1, h = -1;
        for (FiguraModelPart child : modelPart.children) {
            // If any child has multiple textures, then we know this parent must as well.
            if (child.textureWidth == -1) {
                modelPart.textureWidth = -1;
                modelPart.textureHeight = -1;
                return;
            }
            // If any child has a texture different than one we've already seen, this parent must have multiple textures.
            if (child.textureWidth != w || child.textureHeight != h) {
                if (w != -1) {
                    modelPart.textureWidth = -1;
                    modelPart.textureHeight = -1;
                    return;
                }
                w = child.textureWidth;
                h = child.textureHeight;
            }
        }
        if (modelPart.customization.partType != PartCustomization.PartType.GROUP) {
            int i = -1;
            for (int j = 0; j < modelPart.facesByTexture.size(); j++) {
                if (modelPart.facesByTexture.get(j) > 0) {
                    int realTexWidth = textureSets.get(j).getWidth();
                    int realTexHeight = textureSets.get(j).getHeight();
                    if ((w != -1 && w != realTexWidth) || (h != -1 && h != realTexHeight)) {
                        modelPart.textureWidth = -1;
                        modelPart.textureHeight = -1;
                        return;
                    }
                    if (i != -1) {
                        modelPart.textureWidth = -1;
                        modelPart.textureHeight = -1;
                        return;
                    }
                    i = j;
                    w = realTexWidth;
                    h = realTexHeight;
                }
            }
        }
        modelPart.textureWidth = w;
        modelPart.textureHeight = h;
    }

    private static void readVec3(FiguraVec3 target, NBTTagCompound tag, String name) {
        if (tag.hasKey(name))
            readVec3(target, (NBTTagList) tag.getTag(name));
    }

    private static void readVec3(FiguraVec3 target, NBTTagList list) {
        byte elementType = (byte) list.getTagType();
        if (elementType == NbtType.FLOAT.getValue()) {
            target.set(list.getFloatAt(0), list.getFloatAt(1), list.getFloatAt(2));
        } else if (elementType == NbtType.INT.getValue()) {
            target.set(list.getIntAt(0), list.getIntAt(1), list.getIntAt(2));
        } else if (elementType == NbtType.SHORT.getValue()) {
            target.set(
                    ((NBTTagShort) list.get(0)).getShort(),
                    ((NBTTagShort) list.get(1)).getShort(),
                    ((NBTTagShort) list.get(2)).getShort()
            );
        } else if (elementType == NbtType.BYTE.getValue()) {
            target.set(
                    ((NBTTagByte) list.get(0)).getByte(),
                    ((NBTTagByte) list.get(1)).getByte(),
                    ((NBTTagByte) list.get(2)).getByte()
            );
        }
    }

    private static void readVec4(FiguraVec4 target, NBTTagCompound tag, String name) {
        if (tag.hasKey(name)) {
            NBTTagList list = (NBTTagList) tag.getTag(name);
            byte elementType = (byte) list.getTagType();
            if (elementType == NbtType.FLOAT.getValue()) {
                target.set(list.getFloatAt(0), list.getFloatAt(1), list.getFloatAt(2), list.getFloatAt(3));
            } else if (elementType == NbtType.INT.getValue()) {
                target.set(list.getIntAt(0), list.getIntAt(1), list.getIntAt(2), list.getIntAt(3));
            } else if (elementType == NbtType.SHORT.getValue()) {
                target.set(
                        ((NBTTagShort) list.get(0)).getShort(),
                        ((NBTTagShort) list.get(1)).getShort(),
                        ((NBTTagShort) list.get(2)).getShort(),
                        ((NBTTagShort) list.get(3)).getShort()
                );
            } else if (elementType == NbtType.BYTE.getValue()) {
                target.set(
                        ((NBTTagByte) list.get(0)).getByte(),
                        ((NBTTagByte) list.get(1)).getByte(),
                        ((NBTTagByte) list.get(2)).getByte(),
                        ((NBTTagByte) list.get(3)).getByte()
                );
            }
        } else {
            target.set(0, 0, 0, 0);
        }
    }

    private static boolean hasCubeData(NBTTagCompound partCompound) {
        if (partCompound.hasKey("cube_data", NbtType.COMPOUND.getValue()))
            return !partCompound.getCompoundTag("cube_data").hasNoTags();
        return false;
    }

    private static boolean hasMeshData(NBTTagCompound partCompound) {
        if (partCompound.hasKey("mesh_data", NbtType.COMPOUND.getValue()))
            return !partCompound.getCompoundTag("mesh_data").hasNoTags();
        return false;
    }
    private static final Map<String, FiguraVec3[]> faceData = ImmutableMap.<String, FiguraVec3[]>builder() // booze 🥴
            .put("n", new FiguraVec3[] {
                    FiguraVec3.of(1, 0, 0),
                    FiguraVec3.of(0, 0, 0),
                    FiguraVec3.of(0, 1, 0),
                    FiguraVec3.of(1, 1, 0),
                    FiguraVec3.of(0, 0, -1)
            })
            .put("s", new FiguraVec3[] {
                    FiguraVec3.of(0, 0, 1),
                    FiguraVec3.of(1, 0, 1),
                    FiguraVec3.of(1, 1, 1),
                    FiguraVec3.of(0, 1, 1),
                    FiguraVec3.of(0, 0, 1)
            })
            .put("e", new FiguraVec3[] {
                    FiguraVec3.of(1, 0, 1),
                    FiguraVec3.of(1, 0, 0),
                    FiguraVec3.of(1, 1, 0),
                    FiguraVec3.of(1, 1, 1),
                    FiguraVec3.of(1, 0, 0)
            })
            .put("w", new FiguraVec3[] {
                    FiguraVec3.of(0, 0, 0),
                    FiguraVec3.of(0, 0, 1),
                    FiguraVec3.of(0, 1, 1),
                    FiguraVec3.of(0, 1, 0),
                    FiguraVec3.of(-1, 0, 0)
            })
            .put("u", new FiguraVec3[] {
                    FiguraVec3.of(0, 1, 1),
                    FiguraVec3.of(1, 1, 1),
                    FiguraVec3.of(1, 1, 0),
                    FiguraVec3.of(0, 1, 0),
                    FiguraVec3.of(0, 1, 0)
            })
            .put("d", new FiguraVec3[] {
                    FiguraVec3.of(0, 0, 0),
                    FiguraVec3.of(1, 0, 0),
                    FiguraVec3.of(1, 0, 1),
                    FiguraVec3.of(0, 0, 1),
                    FiguraVec3.of(0, -1, 0)
            })
            .build();

    private static final FiguraVec2[] uvValues = {
            FiguraVec2.of(0, 1),
            FiguraVec2.of(1, 1),
            FiguraVec2.of(1, 0),
            FiguraVec2.of(0, 0)
    };


    private static void readCuboid(List<Integer> facesByTexture, NBTTagCompound data, Map<Integer, List<Vertex>> vertices) {
        // Read from and to
        FiguraVec3 from = FiguraVec3.of();
        readVec3(from, data, "f");
        FiguraVec3 to = FiguraVec3.of();
        readVec3(to, data, "t");

        // Read inflate
        double inflate = 0;
        if (data.hasKey("inf"))
            inflate = data.getFloat("inf");
        from.add(-inflate, -inflate, -inflate);
        to.add(inflate, inflate, inflate);

        // Cache difference between from and to
        FiguraVec3 ftDiff = to.copy();
        ftDiff.subtract(from);

        // Iterate over faces, add them
        for (String direction : faceData.keySet())
            readFace(data.getCompoundTag("cube_data"), facesByTexture, direction, vertices, from, ftDiff);
    }

    private static void readFace(NBTTagCompound faces, List<Integer> facesByTexture, String direction, Map<Integer, List<Vertex>> vertices, FiguraVec3 from, FiguraVec3 ftDiff) {
        if (faces.hasKey(direction)) {
            NBTTagCompound face = faces.getCompoundTag(direction);
            short texId = face.getShort("tex");
            facesByTexture.set(texId, facesByTexture.get(texId) + 1);

            FiguraVec3 normal = faceData.get(direction)[4];
            int rotation = (int) (face.getFloat("rot") / 90f);
            FiguraVec4 uv = FiguraVec4.of();
            readVec4(uv, face, "uv");
            for (int i = 0; i < 4; i++) {
                FiguraVec3 tempPos = ftDiff.copy();
                tempPos.multiply(faceData.get(direction)[i]);
                tempPos.add(from);

                FiguraVec2 normalizedUv = uvValues[(i + rotation) % 4];

                List<Vertex> list = vertices.getOrDefault((int) texId, new ArrayList<>());
                list.add(new Vertex(
                        (float) tempPos.x, (float) tempPos.y, (float) tempPos.z,
                        (float) MathUtils.lerp(normalizedUv.x, uv.x, uv.z),
                        (float) MathUtils.lerp(normalizedUv.y, uv.y, uv.w),
                        (float) normal.x, (float) normal.y, (float) normal.z
                ));
                vertices.put((int) texId, list);
            }
        }
    }

    private static void readMesh(List<Integer> facesByTexture, NBTTagCompound data, Map<Integer, List<Vertex>> vertices) {
        NBTTagCompound meshData = data.getCompoundTag("mesh_data");
        // mesh_data:
        // "vtx": List<Float>, xyz
        // "tex": List<Short>, (texID << 4) + numVerticesInFace
        // "fac": List<Byte, Short, or Int>, just the indices of various vertices
        // "uvs": List<Float>, uv for each vertex

        // Get the vertex, UV, and texture lists from the mesh data
        NBTTagList verts = meshData.getTagList("vtx", NbtType.FLOAT.getValue());
        NBTTagList uvs = meshData.getTagList("uvs", NbtType.FLOAT.getValue());
        NBTTagList tex = meshData.getTagList("tex", NbtType.SHORT.getValue());

        // Determine the best data type to use for the face list based on the size of the vertex list
        int bestType = 0; // byte
        if (verts.tagCount() > 255 * 3) bestType = 1; // short
        if (verts.tagCount() > 32767 * 3) bestType = 2; // int

        // Get the face list using the determined data type
        NBTTagList fac;
        switch (bestType) {
            case 0:
                fac = meshData.getTagList("fac", NbtType.BYTE.getValue());
                break;
            case 1:
                fac = meshData.getTagList("fac", NbtType.SHORT.getValue());
                break;
            default:
                fac = meshData.getTagList("fac", NbtType.INT.getValue());
                break;
        }

        // Initialize counters for the vertex and UV lists
        int vi = 0, uvi = 0;

        // Create arrays to store temporary vertex and UV data
        float[] posArr = new float[12];
        float[] uvArr = new float[8];

        // Iterate through the texture list
        for (int ti = 0; ti < tex.tagCount(); ti++) {
            // Get the packed texture data for this iteration
            short packed = ((NBTTagShort)tex.get(ti)).getShort();
            // Extract the texture ID and number of vertices from the packed data
            int texId = packed >> 4;
            int numVerts = packed & 0xf;
            // Increment the number of faces for the current texture ID
            facesByTexture.set(texId, facesByTexture.get(texId) + 1);

            // Extract the vertex and UV data for the current texture
            for (int j = 0; j < numVerts; j++) {
                // Get the vertex ID based on the determined data type
                int vid;
                switch (bestType) {
                    case 0:
                        vid = ((NBTTagByte) fac.get(vi + j)).getByte() & 0xff;
                        break;
                    case 1:
                        vid = ((NBTTagShort) fac.get(vi + j)).getShort() & 0xffff;
                        break;
                    default:
                        vid = fac.getIntAt(vi + j);
                        break;
                }
                // Get the vertex position and UV data from the lists
                posArr[3 * j] = verts.getFloatAt(3 * vid);
                posArr[3 * j + 1] = verts.getFloatAt(3 * vid + 1);
                posArr[3 * j + 2] = verts.getFloatAt(3 * vid + 2);

                uvArr[2 * j] = uvs.getFloatAt(uvi + 2 * j);
                uvArr[2 * j + 1] = uvs.getFloatAt(uvi + 2 * j + 1);
            }

            // Calculate the normal vector for the current texture
            FiguraVec3 p1 = FiguraVec3.of(posArr[0], posArr[1], posArr[2]);
            FiguraVec3 p2 = FiguraVec3.of(posArr[3], posArr[4], posArr[5]);
            FiguraVec3 p3 = FiguraVec3.of(posArr[6], posArr[7], posArr[8]);
            p3.subtract(p2);
            p1.subtract(p2);
            p3.cross(p1);
            p3.normalize();
            // p3 now contains the normal vector

            // Add the vertex data to the appropriate builder
            for (int j = 0; j < numVerts; j++) {
                List<Vertex> list = vertices.getOrDefault(texId, new ArrayList<>());
                list.add(new Vertex(
                        posArr[3 * j], posArr[3 * j + 1], posArr[3 * j + 2],
                        uvArr[2 * j], uvArr[2 * j + 1],
                        (float) p3.x, (float) p3.y, (float) p3.z
                ));
                vertices.put(texId, list);
            }
            // Add a vertex if necessary
            if (numVerts == 3) {
                List<Vertex> list = vertices.getOrDefault(texId, new ArrayList<>());
                list.add(new Vertex(
                        posArr[6], posArr[7], posArr[8],
                        uvArr[4], uvArr[5],
                        (float) p3.x, (float) p3.y, (float) p3.z
                ));
                vertices.put(texId, list);
            }

            // Increment the counters for the vertex and UV lists
            vi += numVerts;
            uvi += 2 * numVerts;
        }
    }

    // thanks to Scarlet Light#7611
    private static void smoothfy(Map<Integer, List<Vertex>> verticesByTextuers) {
        // separate vertices
        Map<String, List<Vertex>> verticesByPos = new HashMap<>();
        for (List<Vertex> vertices : verticesByTextuers.values()) {
            for (Vertex vertex : vertices) {
                String id = String.valueOf(vertex.getPos());
                List<Vertex> list = verticesByPos.computeIfAbsent(id, str -> new ArrayList<>(4));
                list.add(vertex);
            }
        }

        // for all separated vertices
        for (List<Vertex> vertices : verticesByPos.values()) {
            // sum their normals
            FiguraVec3 result = FiguraVec3.of();
            for (Vertex vertex : vertices)
                result.add(vertex.getNormal());
            // normalize the normal
            result.normalize();
            // apply new normal
            for (Vertex vertex : vertices)
                vertex.setNormal(result);
        }
    }
}
