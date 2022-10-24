package moe.plushie.armourers_workshop.init.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import moe.plushie.armourers_workshop.api.skin.ISkinPaintType;
import moe.plushie.armourers_workshop.core.data.color.ColorScheme;
import moe.plushie.armourers_workshop.core.data.color.PaintColor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ColorSchemeArgument implements ArgumentType<ColorScheme> {

    private static final Collection<String> EXAMPLES = Arrays.asList("<dyeIndex=[paintType:]#RRGGBB>", "<dyeIndex=[paintType:]R,G,B>");

    public ColorSchemeArgument() {
        super();
    }

    public static ColorScheme getColorScheme(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, ColorScheme.class);
    }

    @Override
    public ColorScheme parse(final StringReader reader) throws CommandSyntaxException {
        ColorSchemeParser parser = new ColorSchemeParser(reader).parse();
        ColorScheme colorScheme = new ColorScheme();
        for (Map.Entry<ISkinPaintType, PaintColor> entry : parser.getProperties().entrySet()) {
            colorScheme.setColor(entry.getKey(), entry.getValue());
        }
        return colorScheme;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        StringReader stringReader = new StringReader(builder.getInput());
        stringReader.setCursor(builder.getStart());
        ColorSchemeParser parser = new ColorSchemeParser(stringReader);
        try {
            parser.parse();
        } catch (CommandSyntaxException commandSyntaxException) {
            // empty catch block
        }
        return parser.fillSuggestions(builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Serializer implements ArgumentSerializer<ColorSchemeArgument> {

        public void serializeToNetwork(ColorSchemeArgument argument, FriendlyByteBuf buffer) {
        }

        public ColorSchemeArgument deserializeFromNetwork(FriendlyByteBuf buffer) {
            return new ColorSchemeArgument();
        }

        public void serializeToJson(ColorSchemeArgument argument, JsonObject json) {
        }
    }
}