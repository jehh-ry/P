package common;
import java.io.Serializable;

public class FilePacket implements Serializable {
    private String fileName;
    private byte[] fileData;

    public FilePacket(String fileName, byte[] fileData) {
        this.fileName = fileName;
        this.fileData = fileData;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public String toString() {
    return "FilePacket{" +
           "fileName='" + fileName + '\'' +
           ", fileSize=" + (fileData != null ? fileData.length : 0) + " bytes" +
           '}';
}

}
