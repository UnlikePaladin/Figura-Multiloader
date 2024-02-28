package org.figuramc.figura.parsers;

import com.google.gson.*;
import net.minecraft.nbt.*;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.lua.api.action_wheel.Action;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.model.ParentType;
import org.figuramc.figura.utils.IOUtils;
import org.figuramc.figura.FiguraMod;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

//main class to convert a blockbench model (json) into nbt
//default fields are omitted from the nbt to save up space
//note: use the same instance for parsing multiple models for the same avatar
public class BlockbenchModelParser {

    private final static Gson GSON = new GsonBuilder().create();

    //offsets for usage of diverse models
    private int textureOffset = 0;
    private int animationOffset = 0;

    //used during the parser
    private final HashMap<String, NBTTagCompound> elementMap = new HashMap<>();
    private final HashMap<String, NBTTagList> animationMap = new HashMap<>();
    private final HashMap<String, TextureData> textureMap = new HashMap<>();
    private final HashMap<Integer, String> textureIdMap = new HashMap<>();

    //parser
    public ModelData parseModel(Path avatarFolder, Path sourceFile, String json, String modelName, String folders) throws Exception {
        // parse json -> object
        BlockbenchModel model = GSON.fromJson(json, BlockbenchModel.class);

        //meta check
        if (!model.meta.model_format.equals("free") && !model.meta.model_format.contains(FiguraMod.MOD_ID))
            throw new Exception("Model \"" + modelName + "\" have an incompatible model format. Compatibility is limited to \"Generic Model\" format and third-party " + FiguraMod.MOD_NAME + " specific formats");
        if (Integer.parseInt(model.meta.format_version.split("\\.")[0]) < 4)
            throw new Exception("Model \"" + modelName + "\" was created using a version too old (" + model.meta.format_version + ") of Blockbench. Minimum compatible version is 4.0");

        //return lists
        NBTTagCompound textures = new NBTTagCompound();
        List<NBTTagCompound> animationList = new ArrayList<>();

        //object -> nbt
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("name", modelName);
        parseParent(modelName, nbt);

        //parse textures first
        //we want to save the textures in a separated list
        //we also want to fix the UV mismatch from the resolution and the texture
        //emissive textures are not put into the texture map, so we need to fix parts texture ids
        parseTextures(avatarFolder, sourceFile, folders, modelName, textures, model.textures, model.resolution);

        //parse elements into a map of UUID (String) -> NbtCompound (the element)
        //later when parsing the outliner, we fetch the elements from this map
        parseElements(model.elements);

        //parse animations
        //add the animation metadata to the animation list
        //but return a map with the group animation, as we will store it on the groups themselves
        parseAnimations(animationList, model.animations, modelName, folders);

        //add and parse the outliner
        nbt.setTag("chld", parseOutliner(model.outliner, true));

        //clear variables used by the parser
        elementMap.clear();
        animationMap.clear();
        textureMap.clear();
        textureIdMap.clear();

        //return the parsed data
        return new ModelData(textures, animationList, nbt);
    }

    public static void parseParent(String name, NBTTagCompound nbt) {
        ParentType parentType = ParentType.get(name);
        if (parentType != ParentType.None)
            nbt.setString("pt", parentType.name());
    }

    // -- internal functions -- //

    private void parseTextures(Path avatar, Path sourceFile, String folders, String modelName, NBTTagCompound texturesNbt, BlockbenchModel.Texture[] textures, BlockbenchModel.Resolution resolution) throws Exception {
        if (textures == null)
            return;

        //temp lists

        //used for retrieving texture data by name, so we can expand the same data
        LinkedHashMap<String, NBTTagCompound> texturesTemp = new LinkedHashMap<>();

        //used for storing the index of the specific texture name
        List<String> textureIndex = new ArrayList<>();

        //nbt stuff
        NBTTagCompound src = new NBTTagCompound();
        NBTTagList data = new NBTTagList();

        //read textures
        for (int i = 0; i < textures.length; i++) {
            BlockbenchModel.Texture texture = textures[i];

            //name
            String name = texture.name;
            if (name.endsWith(".png"))
                name = name.substring(0, name.length() - 4);

            //texture type
            String textureType;

            if (name.endsWith("_e")) {
                textureType = "e";
            } else if (name.endsWith("_n")) {
                textureType = "n";
            } else if (name.endsWith("_s")) {
                textureType = "s";
            } else {
                textureType = "d";
            }

            //parse the texture data
            String path;
            byte[] source;
            try {
                //check the file to load
                Path p = sourceFile.resolve(texture.relative_path);
                if (p.getFileSystem() == FileSystems.getDefault()) {
                    File f = p.toFile().getCanonicalFile();
                    p = f.toPath();
                    if (!f.exists()) throw new IllegalStateException("File do not exists!");
                } else {
                    p = p.normalize();
                    if (p.getFileSystem() != avatar.getFileSystem())
                        throw new IllegalStateException("File from outside the avatar folder!");
                }
                if (avatar.getNameCount() > 1) if (!p.startsWith(avatar)) throw new IllegalStateException("File from outside the avatar folder!");
                FiguraMod.debug("path is {}", p.toString());
                //load texture
                source = IOUtils.readFileBytes(p);
                path = avatar.relativize(p)
                        .toString()
                        .replace(p.getFileSystem().getSeparator(), ".");
                path = path.substring(0, path.length() - 4);

                //fix name
                name = folders + name;

                //feedback
                FiguraMod.debug("Loaded {} Texture \"{}\" from {}", textureType.toUpperCase(), name, p);
            } catch (Exception e) {
                if (e instanceof IOException)
                    FiguraMod.LOGGER.error("", e);

                //otherwise, load from the source stored in the model
                source = Base64.getDecoder().decode(texture.source.substring("data:image/png;base64,".length()));
                path = folders + modelName + "." + name;
                FiguraMod.debug("Loaded {} Texture \"{}\" from {}", textureType.toUpperCase(), name, path);
            }

            //add source nbt
            src.setByteArray(path, source);

            //fix texture name
            if (!textureType.equals("d"))
                name = name.substring(0, name.length() - 2);

            //add textures nbt
            if (texturesTemp.containsKey(name)) {
                NBTTagCompound textureContainer = texturesTemp.get(name);
                if (textureContainer.hasKey(textureType))
                    throw new Exception("Model \"" + modelName + "\" contains texture with duplicate name \"" + name + "\"");
                textureContainer.setString(textureType, path);
            } else {
                //create nbt
                NBTTagCompound compound = new NBTTagCompound();
                compound.setString(textureType, path);

                //add to temp lists
                texturesTemp.put(name, compound);
                textureIndex.add(name);
            }

            //used on the model conversion, so save the id as is
            textureIdMap.put(i, name);

            //generate the texture data
            if (!textureMap.containsKey(name)) {
                //id is generated by the position of the name in the list
                int id = textureIndex.indexOf(name) + textureOffset;

                //fix texture size for more speed
                float[] fixedSize;
                if (texture.width != null) {
                    fixedSize = new float[]{(float) texture.width / texture.uv_width, (float) texture.height / texture.uv_height};
                }
                else {
                    int[] imageSize = getTextureSize(source);
                    fixedSize = new float[]{(float) imageSize[0] / resolution.width, (float) imageSize[1] / resolution.height};
                }

                //add the texture on the map
                textureMap.put(name, new TextureData(id, fixedSize));
            }
        }

        for (Map.Entry<String, NBTTagCompound> entry : texturesTemp.entrySet())
            data.appendTag(entry.getValue());

        textureOffset += data.tagCount();
        texturesNbt.setTag("src", src);
        texturesNbt.setTag("data", data);
    }

    private void parseElements(BlockbenchModel.Element[] elements) {
        for (BlockbenchModel.Element element : elements) {
            if (element.type == null)
                element.type = "cube";
            if (!element.type.equalsIgnoreCase("cube") && !element.type.equalsIgnoreCase("mesh"))
                continue;
            if (element.export != null && !element.export)
                continue;

            //temp variables
            String id = element.uuid;
            NBTTagCompound nbt = new NBTTagCompound();

            //parse fields
            nbt.setString("name", element.name);

            //parse transform data
            if (notZero(element.from))
                nbt.setTag("f", toNbtList(element.from));
            if (notZero(element.to))
                nbt.setTag("t", toNbtList(element.to));
            if (notZero(element.rotation))
                nbt.setTag("rot", toNbtList(element.rotation));
            if (notZero(element.origin))
                nbt.setTag("piv", toNbtList(element.origin));
            if (element.inflate != 0f)
                nbt.setFloat("inf", element.inflate);

            nbt.setBoolean("vsb", element.visibility == null || element.visibility);

            //parse faces
            NBTTagCompound data;
            if (element.type.equalsIgnoreCase("cube")) {
                data = parseCubeFaces(element.faces);
                nbt.setTag("cube_data", data);
            } else {
                data = parseMesh(element.faces, element.vertices, element.origin);
                nbt.setTag("mesh_data", data);
            }


            elementMap.put(id, nbt);
        }
    }

    private NBTTagCompound parseCubeFaces(JsonObject faces) {
        NBTTagCompound nbt = new NBTTagCompound();

        for (String cubeFace : BlockbenchModel.CubeFace.FACES) {
            if (!faces.has(cubeFace))
                continue;

            //convert face json to java object
            BlockbenchModel.CubeFace face = GSON.fromJson(faces.getAsJsonObject(cubeFace), BlockbenchModel.CubeFace.class);

            //dont add null faces
            if (face.texture == null)
                continue;

            //parse texture
            TextureData texture = textureMap.get(textureIdMap.get(face.texture));
            if (texture == null)
                continue;

            NBTTagCompound faceNbt = new NBTTagCompound();
            faceNbt.setInteger("tex", texture.id);

            //parse face
            if (face.rotation != 0f)
                faceNbt.setFloat("rot", face.rotation);

            //parse uv
            if (notZero(face.uv)) {
                float[] uv = {face.uv[0] * texture.fixedSize[0], face.uv[1] * texture.fixedSize[1], face.uv[2] * texture.fixedSize[0], face.uv[3] * texture.fixedSize[1]};
                faceNbt.setTag("uv", toNbtList(uv));
            }

            nbt.setTag(String.valueOf(cubeFace.charAt(0)), faceNbt);
        }

        return nbt;
    }

    private NBTTagCompound parseMesh(JsonObject faces, JsonObject vertices, float[] offset) {
        NBTTagCompound nbt = new NBTTagCompound();

        //parse vertices first, as the faces will reference it later
        //we are going to save them in a String -> Integer map
        //the map will be preserved since it is very common to meshes share the same vertices,
        //so we can reduce even more file size
        HashMap<String, Integer> verticesMap = new HashMap<>();
        NBTTagList verticesList = new NBTTagList();

        int index = 0;
        for (Map.Entry<String, JsonElement> entry : vertices.entrySet()) {
            verticesMap.put(entry.getKey(), index);
            float[] arr = jsonToFloat(entry.getValue().getAsJsonArray());
            verticesList.appendTag(new NBTTagFloat(arr[0] + offset[0]));
            verticesList.appendTag(new NBTTagFloat(arr[1] + offset[1]));
            verticesList.appendTag(new NBTTagFloat(arr[2] + offset[2]));
            index++;
        }

        //parse faces
        NBTTagList texesList = new NBTTagList();
        NBTTagList uvsList = new NBTTagList();
        NBTTagList facesList = new NBTTagList();

        int bestType = 0; //byte
        if (index > 255) bestType = 1; //short
        if (index > 32767) bestType = 2; //int
        for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
            //convert json to java object
            BlockbenchModel.MeshFace face = GSON.fromJson(entry.getValue(), BlockbenchModel.MeshFace.class);

            //dont parse empty faces
            //Also skip faces that have less than 3 or more than 4 vertices, since blockbench is jank as hell
            if (face.texture == null || face.vertices == null || face.uv == null || face.vertices.length < 3 || face.vertices.length > 4)
                continue;

            //parse texture
            TextureData texture = textureMap.get(textureIdMap.get(face.texture));
            if (texture == null)
                continue;

            //To get the texture id, shift right 4, to get the vertex count, bitmask with 0xf
            //This just stores both pieces of info in one number, to hopefully save some file size
            short k = (short) ((texture.id << 4) + face.vertices.length);
            texesList.appendTag(new NBTTagShort(k));

            if (face.vertices.length > 3)
                reorderVertices(face.vertices, verticesMap, verticesList);

            for (String vertex : face.vertices) {
                //Face indices
                NBTBase bestVal;
                switch (bestType) {
                    case 0:
                        bestVal = new NBTTagByte(verticesMap.get(vertex).byteValue());
                        break;
                    case 1:
                        bestVal = new NBTTagShort(verticesMap.get(vertex).shortValue());
                        break;
                    case 2:
                        bestVal = new NBTTagInt(verticesMap.get(vertex));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + bestType);
                }
                facesList.appendTag(bestVal);

                //UVs
                float[] uv = jsonToFloat(face.uv.getAsJsonArray(vertex));
                float u = uv[0] * texture.fixedSize[0];
                float v = uv[1] * texture.fixedSize[1];
                uvsList.appendTag(new NBTTagFloat(u));
                uvsList.appendTag(new NBTTagFloat(v));
            }
        }

        nbt.setTag("vtx", verticesList);
        nbt.setTag("tex", texesList);
        nbt.setTag("fac", facesList);
        nbt.setTag("uvs", uvsList);
        return nbt;
    }

    private static final FiguraVec3
            v1 = FiguraVec3.of(),
            v2 = FiguraVec3.of(),
            v3 = FiguraVec3.of(),
            v4 = FiguraVec3.of();

    private static void reorderVertices(String[] vertexNames, Map<String, Integer> nameToIndex, NBTTagList vertices) {
        //Fill in v1, v2, v3, v4 from the given vertices
        readVectors(vertexNames, nameToIndex, vertices);

        if (testOppositeSides(v2, v3, v1, v4)) {
            String temp = vertexNames[2];
            vertexNames[2] = vertexNames[1];
            vertexNames[1] = vertexNames[0];
            vertexNames[0] = temp;
        } else if (testOppositeSides(v1, v2, v3, v4)) {
            String temp = vertexNames[2];
            vertexNames[2] = vertexNames[1];
            vertexNames[1] = temp;
        }

    }

    private static void readVectors(String[] vertexNames, Map<String, Integer> nameToIndex, NBTTagList vertices) {
        int i = nameToIndex.get(vertexNames[0]);
        v1.set(vertices.getFloatAt(3*i), vertices.getFloatAt(3*i+1), vertices.getFloatAt(3*i+2));
        i = nameToIndex.get(vertexNames[1]);
        v2.set(vertices.getFloatAt(3*i), vertices.getFloatAt(3*i+1), vertices.getFloatAt(3*i+2));
        i = nameToIndex.get(vertexNames[2]);
        v3.set(vertices.getFloatAt(3*i), vertices.getFloatAt(3*i+1), vertices.getFloatAt(3*i+2));
        i = nameToIndex.get(vertexNames[3]);
        v4.set(vertices.getFloatAt(3 * i), vertices.getFloatAt(3 * i + 1), vertices.getFloatAt(3 * i + 2));
    }

    private static final FiguraVec3
            t1 = FiguraVec3.of(),
            t2 = FiguraVec3.of(),
            t3 = FiguraVec3.of(),
            t4 = FiguraVec3.of();

    /**
     * Checks whether the two points given are on opposite sides of the line given.
     */
    private static boolean testOppositeSides(FiguraVec3 linePoint1, FiguraVec3 linePoint2, FiguraVec3 point1, FiguraVec3 point2) {
        t1.set(linePoint1);
        t2.set(linePoint2);
        t3.set(point1);
        t4.set(point2);

        t2.subtract(t1);
        t3.subtract(t1);
        t4.subtract(t1);

        t1.set(t2);
        t1.cross(t3);
        t2.cross(t4);
        return t1.dot(t2) < 0;
    }

    private void parseAnimations(List<NBTTagCompound> list, BlockbenchModel.Animation[] animations, String modelName, String folders) {
        if (animations == null)
            return;

        int i = 0;
        for (BlockbenchModel.Animation animation : animations) {
            NBTTagCompound animNbt = new NBTTagCompound();

            //animation metadata
            animNbt.setString("mdl", folders.trim().isEmpty() ? modelName : folders + modelName);
            animNbt.setString("name", animation.name);
            if (!animation.loop.equals("once"))
                animNbt.setString("loop", animation.loop);
            if (animation.override != null && animation.override)
                animNbt.setBoolean("ovr", true);
            if (animation.length != 0f)
                animNbt.setFloat("len", animation.length);

            float offset = toFloat(animation.anim_time_update, 0f);
            if (offset != 0f)
                animNbt.setFloat("off", offset);

            float blend = toFloat(animation.blend_weight, 1f);
            if (blend != 1f)
                animNbt.setFloat("bld", blend);

            float startDelay = toFloat(animation.start_delay, 0f);
            if (startDelay != 0f)
                animNbt.setFloat("sdel", startDelay);

            float loopDelay = toFloat(animation.loop_delay, 0f);
            if (loopDelay != 0f)
                animNbt.setFloat("ldel", loopDelay);

            //animation group data

            //hacky solution to skip the for loop
            if (animation.animators == null)
                animation.animators = new JsonObject();

            for (Map.Entry<String, JsonElement> entry : animation.animators.entrySet()) {
                String id = entry.getKey();
                boolean effect = id.equalsIgnoreCase("effects");

                NBTTagList effectData = new NBTTagList();
                NBTTagList rotData = new NBTTagList();
                NBTTagList posData = new NBTTagList();
                NBTTagList scaleData = new NBTTagList();

                //parse keyframes
                JsonObject animationData = entry.getValue().getAsJsonObject();
                for (JsonElement keyframeJson : animationData.get("keyframes").getAsJsonArray()) {
                    BlockbenchModel.KeyFrame keyFrame = GSON.fromJson(keyframeJson, BlockbenchModel.KeyFrame.class);

                    NBTTagCompound keyframeNbt = new NBTTagCompound();
                    keyframeNbt.setFloat("time", keyFrame.time);

                    if (effect) {
                        if (!keyFrame.channel.equalsIgnoreCase("timeline"))
                            continue;

                        keyframeNbt.setString("src", keyFrame.data_points.get(0).getAsJsonObject().get("script").getAsString());
                        effectData.appendTag(keyframeNbt);
                    } else {
                        keyframeNbt.setString("int", keyFrame.interpolation);

                        //pre
                        JsonObject dataPoints = keyFrame.data_points.get(0).getAsJsonObject();
                        keyframeNbt.setTag("pre", parseKeyFrameData(dataPoints, keyFrame.channel));

                        //end
                        if (keyFrame.data_points.size() > 1) {
                            JsonObject endDataPoints = keyFrame.data_points.get(1).getAsJsonObject();
                            keyframeNbt.setTag("end", parseKeyFrameData(endDataPoints, keyFrame.channel));
                        }

                        //bezier stuff
                        if (notZero(keyFrame.bezier_left_value))
                            keyframeNbt.setTag("bl", toNbtList(keyFrame.bezier_left_value));
                        if (notZero(keyFrame.bezier_right_value))
                            keyframeNbt.setTag("br", toNbtList(keyFrame.bezier_right_value));
                        if (isDifferent(keyFrame.bezier_left_time, -0.1f))
                            keyframeNbt.setTag("blt", toNbtList(keyFrame.bezier_left_time));
                        if (isDifferent(keyFrame.bezier_right_time, 0.1f))
                            keyframeNbt.setTag("brt", toNbtList(keyFrame.bezier_right_time));

                        switch (keyFrame.channel) {
                            case "position":
                                posData.appendTag(keyframeNbt);
                                break;
                            case "rotation":
                                rotData.appendTag(keyframeNbt);
                                break;
                            case "scale":
                                scaleData.appendTag(keyframeNbt);
                                break;
                        }
                    }
                }

                //add to nbt
                if (effect) {
                    animNbt.setTag("code", effectData);
                } else {
                    NBTTagList partAnimations = animationMap.containsKey(id) ? animationMap.get(id) : new NBTTagList();
                    NBTTagCompound nbt = new NBTTagCompound();
                    NBTTagCompound channels = new NBTTagCompound();

                    if (!rotData.hasNoTags()) {
                        JsonElement globalRotJson = animationData.get("rotation_global");
                        if (globalRotJson != null && globalRotJson.getAsBoolean())
                            channels.setTag("grot", rotData);
                        else
                            channels.setTag("rot", rotData);
                    }
                    if (!posData.hasNoTags())
                        channels.setTag("pos", posData);
                    if (!scaleData.hasNoTags())
                        channels.setTag("scl", scaleData);

                    if (!channels.hasNoTags()) {
                        nbt.setInteger("id", i + animationOffset);
                        nbt.setTag("data", channels);
                    }
                    if (!nbt.hasNoTags())
                        partAnimations.appendTag(nbt);

                    if (!partAnimations.hasNoTags())
                        animationMap.put(id, partAnimations);
                }
            }

            list.add(animNbt);
            i++;
        }

        animationOffset += list.size();
    }

    private NBTTagList parseKeyFrameData(JsonObject object, String channel) {
        BlockbenchModel.KeyFrameData frameData = GSON.fromJson(object, BlockbenchModel.KeyFrameData.class);

        float fallback = channel.equals("scale") ? 1f : 0f;
        Object x = keyFrameData(frameData.x, fallback);
        Object y = keyFrameData(frameData.y, fallback);
        Object z = keyFrameData(frameData.z, fallback);

        NBTTagList nbt = new NBTTagList();
        if (x instanceof Float && y instanceof Float && z instanceof Float) {
            Float zz = (Float) z;
            Float yy = (Float) y;
            Float xx = (Float) x;
            nbt.appendTag(new NBTTagFloat(xx));
            nbt.appendTag(new NBTTagFloat(yy));
            nbt.appendTag(new NBTTagFloat(zz));
        } else {
            nbt.appendTag(new NBTTagString(String.valueOf(x)));
            nbt.appendTag(new NBTTagString(String.valueOf(y)));
            nbt.appendTag(new NBTTagString(String.valueOf(z)));
        }

        return nbt;
    }

    private NBTTagList parseOutliner(JsonArray outliner, boolean parentVsb) {
        NBTTagList children = new NBTTagList();

        if (outliner == null)
            return children;

        for (JsonElement element : outliner) {
            //check if it is an ID first
            if (element instanceof JsonPrimitive) {
                String key = element.getAsString();
                if (elementMap.containsKey(key)) {
                    NBTTagCompound elementNbt = elementMap.get(key);
                    //fix children visibility (very jank)
                    if (elementNbt.hasKey("vsb") && elementNbt.getBoolean("vsb") == parentVsb)
                        elementNbt.removeTag("vsb");
                    children.appendTag(elementNbt);
                }

                continue;
            }

            //then parse as GroupElement (outliner)
            NBTTagCompound groupNbt = new NBTTagCompound();
            BlockbenchModel.GroupElement group = GSON.fromJson(element, BlockbenchModel.GroupElement.class);

            //skip not exported groups
            if (group.export != null && !group.export)
                continue;

            //parse fields
            groupNbt.setString("name", group.name);

            //visibility
            boolean thisVisibility = group.visibility == null || group.visibility;
            if (thisVisibility != parentVsb)
                groupNbt.setBoolean("vsb", thisVisibility);

            //parse transforms
            if (notZero(group.origin))
                groupNbt.setTag("piv", toNbtList(group.origin));
            if (notZero(group.rotation))
                groupNbt.setTag("rot", toNbtList(group.rotation));

            //parent type
            parseParent(group.name, groupNbt);

            //parse children
            if (!(group.children == null || group.children.size() == 0))
                groupNbt.setTag("chld", parseOutliner(group.children, thisVisibility));

            //add animations
            if (animationMap.containsKey(group.uuid))
                groupNbt.setTag("anim", animationMap.get(group.uuid));

            children.appendTag(groupNbt);
        }

        return children;
    }

    // -- helper functions -- //

    //converts a float array into a nbt list
    public static NBTTagList toNbtList(float[] floats) {
        NBTTagList list = new NBTTagList();

        int bestType = 0; //byte
        for (float f : floats) {
            if (Math.rint(f) - f == 0) {
                if (f < -127 || f >= 128)
                    bestType = 1; //short
                if (f < -16383 || f >= 16384) {
                    bestType = 2;
                    break;
                }
            } else {
                bestType = 2; //float
                break;
            }
        }

        for (float f : floats) {
            switch (bestType) {
                case 0:
                    list.appendTag(new NBTTagByte((byte) f));
                    break;
                case 1:
                    list.appendTag(new NBTTagShort((short) f));
                    break;
                case 2:
                    list.appendTag(new NBTTagFloat(f));
                    break;
            }
        }

        return list;
    }

    //check if a float array is not composed of only zeros
    public static boolean notZero(float[] floats) {
        return isDifferent(floats, 0f);
    }

    public static boolean isDifferent(float[] floats, float value) {
        if (floats == null)
            return false;

        for (float f : floats) {
            if (f != value) {
                return true;
            }
        }

        return false;
    }

    //try converting a String to float, with a fallback
    public static float toFloat(String input, float fallback) {
        try {
            return Float.parseFloat(input);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public static Object keyFrameData(String input, float fallback) {
        try {
            return Float.parseFloat(input);
        } catch (Exception ignored) {
            return input == null || input.trim().isEmpty() ? fallback : input;
        }
    }

    //get texture size
    public static int[] getTextureSize(byte[] texture) {
        int w = (int) texture[16] & 0xFF;
        w = (w << 8) + ((int) texture[17] & 0xFF);
        w = (w << 8) + ((int) texture[18] & 0xFF);
        w = (w << 8) + ((int) texture[19] & 0xFF);

        int h = (int) texture[20] & 0xFF;
        h = (h << 8) + ((int) texture[21] & 0xFF);
        h = (h << 8) + ((int) texture[22] & 0xFF);
        h = (h << 8) + ((int) texture[23] & 0xFF);

        return new int[]{w, h};
    }

    public static float[] jsonToFloat(JsonArray array) {
        float[] f = new float[array.size()];

        int i = 0;
        for (JsonElement element : array) {
            f[i] = element.isJsonNull() ? 0f : element.getAsFloat();
            i++;
        }

        return f;
    }

    //dummy texture data
    public class TextureData {
        private final int id;
        private final float[] fixedSize;

        public TextureData(int id, float[] fixedSize) {
            this.id = id;
            this.fixedSize = fixedSize.clone(); // Clone the array to ensure immutability
        }

        public int getId() {
            return id;
        }

        public float[] getFixedSize() {
            return fixedSize.clone(); // Return a copy of the array to ensure immutability
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TextureData)) return false;
            TextureData that = (TextureData) o;
            return id == that.id && Arrays.equals(fixedSize, that.fixedSize);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(id);
            result = 31 * result + Arrays.hashCode(fixedSize);
            return result;
        }

        // You can override toString(), equals(), and hashCode() if needed

        // Other methods and members as needed
    }

    //dummy class containing the return object of the parser

    public class ModelData {
        private final NBTTagCompound textures;
        private final List<NBTTagCompound> animationList;
        private final NBTTagCompound modelNbt;

        public ModelData(NBTTagCompound textures, List<NBTTagCompound> animationList, NBTTagCompound modelNbt) {
            this.textures = textures;
            this.animationList = new ArrayList<>(animationList);
            this.modelNbt = modelNbt;
        }

        public NBTTagCompound textures() {
            return textures;
        }

        public List<NBTTagCompound> animationList() {
            return animationList;
        }

        public NBTTagCompound modelNbt() {
            return modelNbt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ModelData)) return false;
            ModelData modelData = (ModelData) o;
            return Objects.equals(textures, modelData.textures) && Objects.equals(animationList, modelData.animationList) && Objects.equals(modelNbt, modelData.modelNbt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(textures, animationList, modelNbt);
        }
    }

}
