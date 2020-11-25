package elasticsearch;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import gg.jte.resolve.ResourceCodeResolver;
import model.SearchHit;
import model.SearchResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class Renderer {

    private final TemplateEngine templateEngine;
    private final JsonFactory factory;

    public Renderer(ObjectMapper mapper) {
        this.factory = mapper.getFactory();
        final ResourceCodeResolver resolver = new ResourceCodeResolver("templates");
        this.templateEngine = TemplateEngine.create(resolver, ContentType.Plain);
    }

    // JSON escape each user content, but make sure no escaping happens when reading input from the template
    private static final class JsonStringOutput extends StringOutput {
        @Override
        public void writeUserContent(String value) {
            super.writeUserContent(escape(value));
        }
    }

    String render(final String templateName, final Map<String, Object> params) {
        try (StringOutput output = new JsonStringOutput()) {
            templateEngine.render(templateName + ".jte", params, output);
            return output.toString();
        }
    }

    public byte[] searchResponse(SearchResponse searchResponse) throws IOException {
        // we can solve this via templates as well once JTE supports preview features or records aren't preview anymore
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             JsonGenerator generator = factory.createGenerator(bos)) {
            generator.writeStartArray();
            for (SearchHit hit : searchResponse.hits()) {
                generator.writeStartObject();
                generator.writeObjectFieldStart("name");
                generator.writeStringField("first", hit.person().firstName());
                generator.writeStringField("last", hit.person().lastName());
                generator.writeEndObject();
                generator.writeStringField("employer", hit.person().employer());
                generator.writeEndObject();
            }
            generator.flush();
            return bos.toByteArray();
        }
    }



    // based on https://github.com/ralfstx/minimal-json/blob/master/com.eclipsesource.json/src/main/java/com/eclipsesource/json/JsonWriter.java
    private static final int CONTROL_CHARACTERS_END = 0x001f;

    private static final char[] QUOT_CHARS = {'\\', '"'};
    private static final char[] BS_CHARS = {'\\', '\\'};
    private static final char[] LF_CHARS = {'\\', 'n'};
    private static final char[] CR_CHARS = {'\\', 'r'};
    private static final char[] TAB_CHARS = {'\\', 't'};
    // In JavaScript, U+2028 and U+2029 characters count as line endings and must be encoded.
    // http://stackoverflow.com/questions/2965293/javascript-parse-error-on-u2028-unicode-character
    private static final char[] UNICODE_2028_CHARS = {'\\', 'u', '2', '0', '2', '8'};
    private static final char[] UNICODE_2029_CHARS = {'\\', 'u', '2', '0', '2', '9'};
    private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f'};

    static String escape(String input) {
        StringBuilder builder = new StringBuilder();
        int length = input.length();
        for (int index = 0; index < length; index++) {
            final char ch = input.charAt(index);
            char[] replacement = getReplacementChars(ch);
            if (replacement != null) {
                builder.append(replacement);
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static char[] getReplacementChars(char ch) {
        if (ch > '\\') {
            if (ch < '\u2028' || ch > '\u2029') {
                // The lower range contains 'a' .. 'z'. Only 2 checks required.
                return null;
            }
            return ch == '\u2028' ? UNICODE_2028_CHARS : UNICODE_2029_CHARS;
        }
        if (ch == '\\') {
            return BS_CHARS;
        }
        if (ch > '"') {
            // This range contains '0' .. '9' and 'A' .. 'Z'. Need 3 checks to get here.
            return null;
        }
        if (ch == '"') {
            return QUOT_CHARS;
        }
        if (ch > CONTROL_CHARACTERS_END) {
            return null;
        }
        if (ch == '\n') {
            return LF_CHARS;
        }
        if (ch == '\r') {
            return CR_CHARS;
        }
        if (ch == '\t') {
            return TAB_CHARS;
        }
        return new char[] {'\\', 'u', '0', '0', HEX_DIGITS[ch >> 4 & 0x000f], HEX_DIGITS[ch & 0x000f]};
    }
}
