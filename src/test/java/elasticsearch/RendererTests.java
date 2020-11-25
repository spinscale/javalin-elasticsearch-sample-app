package elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RendererTests {

    private static final Renderer renderer = new Renderer(new ObjectMapper());

    @Test
    public void testEscape() {
        assertThat(Renderer.escape("a")).isEqualTo("a");
        assertThat(Renderer.escape("ab")).isEqualTo("ab");
        assertThat(Renderer.escape("test")).isEqualTo("test");
        assertThat(Renderer.escape(" test")).isEqualTo(" test");
        assertThat(Renderer.escape(" test ")).isEqualTo(" test ");
        assertThat(Renderer.escape("\"")).isEqualTo("\\\"");
        assertThat(Renderer.escape("\"a")).isEqualTo("\\\"a");
        assertThat(Renderer.escape("\"a\"")).isEqualTo("\\\"a\\\"");
        assertThat(Renderer.escape("a\"b")).isEqualTo("a\\\"b");
        assertThat(Renderer.escape("{\"spam\":\"eggs\"}")).isEqualTo("{\\\"spam\\\":\\\"eggs\\\"}");
    }

    @Test
    public void testRendering() {
        String data = renderer.render("test", Collections.emptyMap());
        assertThat(data).isEqualTo("{\"hello\":\"world\"}");
    }

    @Test
    public void testRenderingWithDifferentArguments() {
        String data = renderer.render("test", Map.of("name", "test"));
        assertThat(data).isEqualTo("{\"hello\":\"test\"}");
    }

    @Test
    public void testRenderingEscaping() {
        String data = renderer.render("test", Map.of("name", "world\",\"foo\":\"bar"));
        assertThat(data).isNotEqualTo("{\"hello\":\"world\",\"foo\":\"bar\"}");
    }
}
