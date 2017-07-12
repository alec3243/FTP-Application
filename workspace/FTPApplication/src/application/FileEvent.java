package application;

import java.io.Serializable;

/**
 * This class is used to contain all of the necessary data within a file.
 * @author Alec J Strickland
 *
 */
public class FileEvent implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String filename;
	private long fileSize;
	private byte[] fileData;
	private FileStatus status;

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public FileStatus getStatus() {
		return status;
	}

	public void setStatus(FileStatus status) {
		this.status = status;
	}

	public byte[] getFileData() {
		return fileData;
	}

	public void setFileData(byte[] data) {
		fileData = data;
	}
}
