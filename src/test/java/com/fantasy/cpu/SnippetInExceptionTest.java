package com.fantasy.cpu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

public class SnippetInExceptionTest {

    private Path writeTempAsm(String content) throws IOException {
        Path p = Files.createTempFile("asmtest_snip", ".asm");
        Files.writeString(p, content);
        return p;
    }

    @Test
    public void exceptionContainsSnippetForInvalidImmediate() throws Exception {
        String asm = "start:\n  MOV DEAD, R1\n.end\n";
        Path p = writeTempAsm(asm);
        Assembler a = new Assembler();
        a.parse(p);
        AssembleException ex = assertThrows(AssembleException.class, () -> a.assemble(Path.of("target/test-snip1.bin")));
        // snippet should include the offending text "MOV DEAD, R1"
        String snippet = ex.getSourceSnippet();
        assertNotNull(snippet, "Expected non-null snippet in AssembleException");
        assertTrue(snippet.contains("MOV") && snippet.contains("DEAD"), "Snippet did not contain the offending immediate: " + snippet);
        // also ensure the message includes the snippet
        assertTrue(ex.getMessage().contains("DEAD") || ex.getMessage().contains("MOV"));
    }

    @Test
    public void exceptionContainsSnippetForByteOverflow() throws Exception {
        String asm = "start:\n  MOV.B 0x1FF, R1\n.end\n";
        Path p = writeTempAsm(asm);
        Assembler a = new Assembler();
        a.parse(p);
        AssembleException ex = assertThrows(AssembleException.class, () -> a.assemble(Path.of("target/test-snip2.bin")));
        String snippet = ex.getSourceSnippet();
        assertNotNull(snippet, "Expected non-null snippet in AssembleException");
    // snippet should show the offending immediate (assembler no longer uses '#')
    assertTrue(snippet.contains("MOV.B") && snippet.contains("0x1FF"), "Snippet did not contain the offending immediate overflow: " + snippet);
    assertTrue(ex.getMessage().contains("0x1FF") || ex.getMessage().contains("MOV.B"));
    }
}
