package com.fantasy.cpu;

/**
 * Exception thrown for assembly errors. Extends IllegalArgumentException so existing
 * tests that expect IllegalArgumentException continue to work.
 */
public class AssembleException extends IllegalArgumentException {
    private final String sourceFile;
    private final int lineNumber;
    private final int address;
    private final String sourceSnippet;

    public AssembleException(String message, String sourceFile, int lineNumber, int address) {
        this(message, sourceFile, lineNumber, address, null);
    }

    public AssembleException(String message, String sourceFile, int lineNumber, int address, String sourceSnippet) {
        super(buildMessage(message, sourceFile, lineNumber, sourceSnippet));
        this.sourceFile = sourceFile;
        this.lineNumber = lineNumber;
        this.address = address;
        this.sourceSnippet = (sourceSnippet == null) ? "" : sourceSnippet;
    }

    private static String buildMessage(String message, String sourceFile, int lineNumber, String snippet) {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        if (sourceFile != null) sb.append(String.format(" (%s:%d)", sourceFile, Math.max(lineNumber, -1)));
        if (snippet != null && !snippet.isEmpty()) sb.append(" -> ").append(snippet.trim());
        return sb.toString();
    }

    public String getSourceFile() { return sourceFile; }
    public int getLineNumber() { return lineNumber; }
    public int getAddress() { return address; }
    public String getSourceSnippet() { return sourceSnippet; }
}
