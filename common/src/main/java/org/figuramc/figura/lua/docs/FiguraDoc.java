package org.figuramc.figura.lua.docs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.commands.FiguraCommands;
import org.figuramc.figura.utils.ColorUtils;
import org.figuramc.figura.utils.FiguraText;
import org.figuramc.figura.utils.TextUtils;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public abstract class FiguraDoc {

    public static final ITextComponent HEADER = new TextComponentString("").setStyle(ColorUtils.Colors.AWESOME_BLUE.style)
                .appendSibling(new TextComponentString("\n•*+•* ").appendSibling(new FiguraText()).appendText(" Docs *•+*•")
                        .setStyle(new Style().setUnderlined(true)));

    public final String name;
    public final String description;

    public FiguraDoc(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // -- Methods -- //

    public abstract int print();

    public FiguraDocSubCommand getCommand() {
        return new FiguraDocSubCommand(name);
    }

    public class FiguraDocSubCommand extends FiguraCommands.FiguraSubCommand {
        public FiguraDocSubCommand(String name) {
            super(name);
        }

        @Override
        public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
            print();
        }
    }

    public JsonObject toJson(boolean translate) {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("description", translate ? new FiguraText("docs." + description).getFormattedText() : FiguraMod.MOD_ID + "." + "docs." + description);
        return json;
    }

    // -- Special prints :p -- //

    public static int printRoot() {
        FiguraMod.sendChatMessage(HEADER.createCopy()
                .appendText("\n\n")
                .appendSibling(new FiguraText("docs").setStyle(ColorUtils.Colors.BLUE.style)));

        return 1;
    }

    // -- Subtypes -- //

    public static class ClassDoc extends FiguraDoc {

        public final ArrayList<MethodDoc> documentedMethods;
        public final ArrayList<FieldDoc> documentedFields;
        public final Class<?> thisClass, superclass;

        public ClassDoc(Class<?> clazz, LuaTypeDoc typeDoc) {
            this(clazz, typeDoc, null);
        }

        public ClassDoc(Class<?> clazz, LuaTypeDoc typeDoc, Map<String, List<FiguraDoc>> children) {
            super(typeDoc.name(), typeDoc.value());

            thisClass = clazz;

            if (clazz.getSuperclass().isAnnotationPresent(LuaTypeDoc.class))
                superclass = clazz.getSuperclass();
            else
                superclass = null;

            // Find methods
            documentedMethods = new ArrayList<>();
            Set<String> foundIndices = new HashSet<>();
            for (Method method : clazz.getDeclaredMethods())
                parseMethodIfNeeded(foundIndices, children, typeDoc, method);
            for (Method method : clazz.getMethods())
                parseMethodIfNeeded(foundIndices, children, typeDoc, method);

            // Find fields
            documentedFields = new ArrayList<>();
            for (Field field : clazz.getDeclaredFields())
                parseFieldIfNeeded(children, foundIndices, field);
            for (Field field : clazz.getFields())
                parseFieldIfNeeded(children, foundIndices, field);
        }

        // Parse docs for this method if none were already found and stored in "foundIndices".
        private void parseMethodIfNeeded(Set<String> foundIndices, Map<String, List<FiguraDoc>> children, LuaTypeDoc typeDoc, Method method) {
            String name = method.getName();
            if (foundIndices.contains(name) || !method.isAnnotationPresent(LuaMethodDoc.class))
                return;

            foundIndices.add(name);
            LuaMethodDoc doc = method.getAnnotation(LuaMethodDoc.class);
            List<FiguraDoc> childList = children == null ? null : children.get(name);
            documentedMethods.add(new MethodDoc(method, doc, childList, typeDoc.name()));
        }

        // Parse docs for this field if none were already found and stored in "foundIndices".
        private void parseFieldIfNeeded(Map<String, List<FiguraDoc>> children, Set<String> foundIndices, Field field) {
            String name = field.getName();
            if (foundIndices.contains(name) || !field.isAnnotationPresent(LuaFieldDoc.class))
                return;

            foundIndices.add(name);
            List<FiguraDoc> childList = children == null ? null : children.get(name);
            documentedFields.add(new FieldDoc(field, field.getAnnotation(LuaFieldDoc.class), childList));
        }


        @Override
        public int print() {
            // header
            ITextComponent message = HEADER.createCopy()
                    .appendText("\n\n")
                    .appendSibling(new TextComponentString("• ")
                            .appendSibling(new FiguraText("docs.text.type"))
                            .appendText(":")
                            .setStyle(ColorUtils.Colors.PURPLE.style));

            // type
            message.appendText("\n\t")
                    .appendSibling(new TextComponentString("• " + name).setStyle(ColorUtils.Colors.BLUE.style));

            if (superclass != null) {
                message.appendText(" (")
                        .appendSibling(new FiguraText("docs.text.extends"))
                        .appendText(" ")
                        .appendSibling(FiguraDocsManager.getClassText(superclass).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                        .appendText(")");
            }

            // description
            message.appendText("\n\n")
                    .appendSibling(new TextComponentString("• ")
                            .appendSibling(new FiguraText("docs.text.description"))
                            .appendText(":")
                            .setStyle(ColorUtils.Colors.PURPLE.style));

            ITextComponent descText = new TextComponentString("").createCopy().setStyle(ColorUtils.Colors.BLUE.style);
            for (ITextComponent component : TextUtils.splitText(new FiguraText("docs." + description), "\n"))
                descText.appendText("\n\t").appendText("• ").appendSibling(component);
            message.appendSibling(descText);

            FiguraMod.sendChatMessage(message);
            return 1;
        }

        @Override
        public FiguraDocSubCommand getCommand() {
            return new FiguraClassDocSubCommand(name);
        }

        public class FiguraClassDocSubCommand extends FiguraDocSubCommand {

            public FiguraClassDocSubCommand(String name) {
                // this
                super(name);

                // methods
                for (FiguraDoc.MethodDoc methodDoc : documentedMethods)
                    children.put(methodDoc.name, methodDoc.getCommand());

                // fields
                for (FiguraDoc.FieldDoc fieldDoc : documentedFields)
                    children.put(fieldDoc.name, fieldDoc.getCommand());
            }

            Map<String, FiguraDocSubCommand> children = new HashMap<>();
            @Override
            public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
                if (args.length > 0) {
                    if (children.containsKey(args[0])) {
                        children.get(args[0]).execute(minecraftServer, iCommandSender, args);
                        return;
                    }
                }
                super.execute(minecraftServer, iCommandSender, args);
            }

            @Override
            public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings, @Nullable BlockPos targetPos) {
                if (strings.length > 0 && children.containsKey(strings[0]))
                    return children.get(strings[0]).getTabCompletions(minecraftServer, iCommandSender, Arrays.copyOfRange(strings, 1, strings.length), targetPos);

                return CommandBase.getListOfStringsMatchingLastWord(strings, children.keySet());
            }
        }

        @Override
        public JsonObject toJson(boolean translate) {
            JsonObject json = super.toJson(translate);

            if (superclass != null)
                json.addProperty("parent", FiguraDocsManager.getNameFor(superclass));

            JsonArray methods = new JsonArray();
            for (FiguraDoc.MethodDoc methodDoc : documentedMethods)
                methods.add(methodDoc.toJson(translate));
            json.add("methods", methods);

            JsonArray fields = new JsonArray();
            for (FiguraDoc.FieldDoc fieldDoc : documentedFields)
                fields.add(fieldDoc.toJson(translate));
            json.add("fields", fields);

            return json;
        }
    }

    public static class MethodDoc extends FiguraDoc {

        public final Class<?>[][] parameterTypes;
        public final String[][] parameterNames;
        public final Class<?>[] returnTypes;
        public final String typeName;
        public final String[] aliases;
        public final boolean isStatic;
        public final List<FiguraDoc> children;

        public MethodDoc(Method method, LuaMethodDoc methodDoc, List<FiguraDoc> children, String typeName) {
            super(method.getName(), methodDoc.value());

            LuaMethodOverload[] overloads = methodDoc.overloads();
            parameterTypes = new Class[overloads.length][];
            parameterNames = new String[overloads.length][];
            returnTypes = new Class[overloads.length];
            isStatic = Modifier.isStatic(method.getModifiers());
            aliases = methodDoc.aliases();
            this.typeName = typeName;
            this.children = children;

            for (int i = 0; i < overloads.length; i++) {
                parameterTypes[i] = overloads[i].argumentTypes();
                parameterNames[i] = overloads[i].argumentNames();

                if (overloads[i].returnType() == LuaMethodOverload.DEFAULT.class)
                    returnTypes[i] = method.getReturnType();
                else
                    returnTypes[i] = overloads[i].returnType();
            }
        }

        @Override
        public int print() {
            // header
            ITextComponent message = HEADER.createCopy();

            // type
            message.appendText("\n\n")
                    .appendSibling(new TextComponentString("• ")
                            .appendSibling(new FiguraText("docs.text.function"))
                            .appendText(":")
                            .setStyle(ColorUtils.Colors.PURPLE.style))
                    .appendText("\n\t")
                    .appendSibling(new TextComponentString("• " + name).setStyle(ColorUtils.Colors.BLUE.style));

            // aliases
            if (aliases.length > 0) {
                message.appendText("\n\n")
                        .appendSibling(new TextComponentString("• ")
                                .appendSibling(new FiguraText("docs.text.aliases"))
                                .appendText(":")
                                .setStyle(ColorUtils.Colors.PURPLE.style));

                for (String alias : aliases) {
                    message.appendText("\n\t")
                            .appendSibling(new TextComponentString("• ")
                                    .appendText(alias)
                                    .setStyle(ColorUtils.Colors.BLUE.style));
                }
            }

            // syntax
            message.appendText("\n\n")
                    .appendSibling(new TextComponentString("• ")
                            .appendSibling(new FiguraText("docs.text.syntax"))
                            .appendText(":")
                            .setStyle(ColorUtils.Colors.PURPLE.style));

            for (int i = 0; i < parameterTypes.length; i++) {

                // name
                message.appendText("\n\t")
                        .appendSibling(new TextComponentString("• ").setStyle(ColorUtils.Colors.BLUE.style))
                        .appendSibling(new TextComponentString("<" + typeName + ">").setStyle(new Style().setColor(TextFormatting.YELLOW)))
                        .appendSibling(new TextComponentString(isStatic ? "." : ":").setStyle(new Style().setBold(true)))
                        .appendSibling(new TextComponentString(name).setStyle(ColorUtils.Colors.BLUE.style))
                        .appendText("(");

                for (int j = 0; j < parameterTypes[i].length; j++) {
                    // type and arg
                    message.appendSibling(FiguraDocsManager.getClassText(parameterTypes[i][j]).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                            .appendText(" ")
                            .appendSibling(new TextComponentString(parameterNames[i][j]).setStyle(new Style().setColor(TextFormatting.WHITE)));

                    if (j != parameterTypes[i].length - 1)
                        message.appendText(", ");
                }

                // return
                message.appendText(") → ")
                        .appendSibling(new FiguraText("docs.text.returns").appendText(" ").setStyle(ColorUtils.Colors.BLUE.style))
                        .appendSibling(FiguraDocsManager.getClassText(returnTypes[i]).setStyle(new Style().setColor(TextFormatting.YELLOW)));
            }

            // description
            message.appendText("\n\n")
                    .appendSibling(new TextComponentString("• ")
                            .appendSibling(new FiguraText("docs.text.description"))
                            .appendText(":")
                            .setStyle(ColorUtils.Colors.PURPLE.style));

            ITextComponent descText = new TextComponentString("").createCopy().setStyle(ColorUtils.Colors.BLUE.style);
            for (ITextComponent component : TextUtils.splitText(new FiguraText("docs." + description), "\n"))
                descText.appendText("\n\t").appendText("• ").appendSibling(component);
            message.appendSibling(descText);

            FiguraMod.sendChatMessage(message);
            return 1;
        }

        @Override
        public FiguraDocSubCommand getCommand() {
            return new FiguraMethodDocSubCommand(name);
        }

        public class FiguraMethodDocSubCommand extends FiguraDocSubCommand {

            public Map<String, FiguraDocSubCommand> fieldCommands = new HashMap<>();
            public FiguraMethodDocSubCommand(String name) {
                super(name);
                if (children != null) {
                    for (FiguraDoc child : children)
                        fieldCommands.put(child.name, child.getCommand());
                }
            }

            @Override
            public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
                if (args.length > 0) {
                    if (fieldCommands.containsKey(args[0])) {
                        fieldCommands.get(args[0]).execute(minecraftServer, iCommandSender, args);
                        return;
                    }
                }
                super.execute(minecraftServer, iCommandSender, args);
            }

            @Override
            public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings, @Nullable BlockPos targetPos) {
                if (strings.length > 0 && fieldCommands.containsKey(strings[0]))
                    return fieldCommands.get(strings[0]).getTabCompletions(minecraftServer, iCommandSender, Arrays.copyOfRange(strings, 1, strings.length), targetPos);

                return CommandBase.getListOfStringsMatchingLastWord(strings, fieldCommands.keySet());
            }
        }

        @Override
        public JsonObject toJson(boolean translate) {
            JsonObject json = super.toJson(translate);

            JsonArray params = new JsonArray();
            for (int i = 0; i < parameterNames.length; i++) {
                JsonArray param = new JsonArray();
                for (int j = 0; j < parameterNames[i].length; j++) {
                    JsonObject paramObj = new JsonObject();
                    paramObj.addProperty("name", parameterNames[i][j]);
                    paramObj.addProperty("type", FiguraDocsManager.getNameFor(parameterTypes[i][j]));
                    param.add(paramObj);
                }

                params.add(param);
            }
            json.add("parameters", params);

            JsonArray returns = new JsonArray();
            for (Class<?> returnType : returnTypes)
                returns.add(FiguraDocsManager.getNameFor(returnType));
            json.add("returns", returns);

            JsonArray aliases = new JsonArray();
            for (String alias : this.aliases)
                aliases.add(alias);
            json.add("aliases", aliases);

            JsonArray children = new JsonArray();
            if (this.children != null) {
                for (FiguraDoc child : this.children)
                    children.add(child.toJson(translate));
            }
            json.add("children", children);

            json.addProperty("static", isStatic);

            return json;
        }
    }

    public static class FieldDoc extends FiguraDoc {

        public final Class<?> type;
        public final boolean editable;
        public final List<FiguraDoc> children;

        public FieldDoc(Field field, LuaFieldDoc luaFieldDoc, List<FiguraDoc> children) {
            super(field.getName(), luaFieldDoc.value());
            type = field.getType();
            editable = !Modifier.isFinal(field.getModifiers());
            this.children = children;
        }

        @Override
        public int print() {
            // header
            ITextComponent message = HEADER.createCopy()

                    // type
                    .appendText("\n\n")
                    .appendSibling(new TextComponentString("• ")
                            .appendSibling(new FiguraText("docs.text.field"))
                            .appendText(":")
                            .setStyle(ColorUtils.Colors.PURPLE.style))
                    .appendText("\n\t")
                    .appendSibling(new TextComponentString("• ").setStyle(ColorUtils.Colors.BLUE.style))
                    .appendSibling(FiguraDocsManager.getClassText(type).setStyle(new Style().setColor(TextFormatting.YELLOW)))
                    .appendSibling(new TextComponentString(" " + name).setStyle(ColorUtils.Colors.BLUE.style))
                    .appendSibling(new TextComponentString(" (")
                            .appendSibling(new FiguraText(editable ? "docs.text.editable" : "docs.text.not_editable"))
                            .appendText(")")
                            .setStyle(new Style().setColor(editable ? TextFormatting.GREEN : TextFormatting.DARK_RED)));

            // description
            message.appendText("\n\n")
                    .appendSibling(new TextComponentString("• ")
                            .appendSibling(new FiguraText("docs.text.description"))
                            .appendText(":")
                            .setStyle(ColorUtils.Colors.PURPLE.style));

            ITextComponent descText = new TextComponentString("").createCopy().setStyle(ColorUtils.Colors.BLUE.style);
            for (ITextComponent component : TextUtils.splitText(new FiguraText("docs." + description), "\n"))
                descText.appendText("\n\t").appendText("• ").appendSibling(component);
            message.appendSibling(descText);

            FiguraMod.sendChatMessage(message);
            return 1;
        }

        @Override
        public FiguraDocSubCommand getCommand() {
            return new FiguraFieldDocSubCommand(name);
        }

        public class FiguraFieldDocSubCommand extends FiguraDocSubCommand {

            public Map<String, FiguraDocSubCommand> childCommands = new HashMap<>();
            public FiguraFieldDocSubCommand(String name) {
                super(name);
                if (children != null) {
                    for (FiguraDoc child : children)
                        childCommands.put(child.name, child.getCommand());
                }
            }

            @Override
            public void execute(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] args) throws CommandException {
                if (args.length > 0) {
                    if (childCommands.containsKey(args[0])) {
                        childCommands.get(args[0]).execute(minecraftServer, iCommandSender, args);
                        return;
                    }
                }
                super.execute(minecraftServer, iCommandSender, args);
            }

            @Override
            public List<String> getTabCompletions(MinecraftServer minecraftServer, ICommandSender iCommandSender, String[] strings, @Nullable BlockPos targetPos) {
                if (strings.length > 0 && childCommands.containsKey(strings[0]))
                    return childCommands.get(strings[0]).getTabCompletions(minecraftServer, iCommandSender, Arrays.copyOfRange(strings, 1, strings.length), targetPos);

                return CommandBase.getListOfStringsMatchingLastWord(strings, childCommands.keySet());
            }
        }

        @Override
        public JsonObject toJson(boolean translate) {
            JsonObject json = super.toJson(translate);
            json.addProperty("type", FiguraDocsManager.getNameFor(this.type));
            json.addProperty("editable", this.editable);

            JsonArray children = new JsonArray();
            if (this.children != null) {
                for (FiguraDoc child : this.children)
                    children.add(child.toJson(translate));
            }
            json.add("children", children);

            return json;
        }
    }
}
