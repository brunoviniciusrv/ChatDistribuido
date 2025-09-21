package br.com.chat.distribuido.model;

import java.io.Serializable;

public class FileMessage implements Serializable {
    private static final long serialVersionUID = 2L;
    private final String sender;
    private final String filename;
    private final byte[] fileContent;

    public FileMessage(String sender, String filename, byte[] fileContent) {
        this.sender = sender;
        this.filename = filename;
        this.fileContent = fileContent;
    }

    public String getSender() { return sender; }
    public String getFilename() { return filename; }
    public byte[] getFileContent() { return fileContent; }
}