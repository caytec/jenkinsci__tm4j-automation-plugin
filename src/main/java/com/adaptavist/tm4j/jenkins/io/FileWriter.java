package com.adaptavist.tm4j.jenkins.io;

import hudson.FilePath;
import hudson.util.DirScanner;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileWriter {

    private InputStream zipFile;
    private List<String> fileNames = new ArrayList<>();

    public FileWriter(InputStream zipFile) {
        this.zipFile = zipFile;
    }

    public void extractFeatureFilesFromZipAndSave(File rootDir, FilePath workspace, String targetPath) throws IOException, InterruptedException {
        if (workspace.isRemote()) {
            extractToRemoteNode(rootDir, workspace, targetPath);
        } else {
            extractToLocalNode(getTargetFeatureFilesPath(targetPath, workspace.getRemote()));
        }
    }

    private void extractToRemoteNode(File rootDir, FilePath workspace, String targetPath) throws IOException, InterruptedException {
        String targetFeatureFilesPath = rootDir.getPath() + "/" + targetPath;
        extractToLocalNode(targetFeatureFilesPath);
        FilePath localNodeFilePath = new FilePath(new File(targetFeatureFilesPath));
        FilePath remoteNodeFilePath = new FilePath(workspace.getChannel(), workspace.getRemote() + "/" + targetPath);
        remoteNodeFilePath.deleteContents();
        localNodeFilePath.copyRecursiveTo("**/*.feature", remoteNodeFilePath);
    }

    private String getTargetFeatureFilesPath(String targetPath, String remote) {
        return remote + "/" + targetPath;
    }

    private void extractToLocalNode(String targetFeatureFilesPath) throws IOException {
        createFolderIfItDoesNotExist(targetFeatureFilesPath);
        cleanUpFolder(targetFeatureFilesPath);
        ZipInputStream zipInputStream = new ZipInputStream(zipFile);
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            fileNames.add(entry.getName());
            saveFeatureFile(zipInputStream, targetFeatureFilesPath, entry);
            zipInputStream.closeEntry();
        }
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    private void createFolderIfItDoesNotExist(String targetFeatureFilesPath) throws IOException {
        Path path = Paths.get(targetFeatureFilesPath);
        if (Files.notExists(path)) {
            Files.createDirectories(path);
        }
    }

    private void cleanUpFolder(String targetFeatureFilesPath) throws IOException {
        FileUtils.cleanDirectory(Paths.get(targetFeatureFilesPath).toFile());
    }

    private void saveFeatureFile(InputStream zipInputStream, String targetFeatureFilesPath, ZipEntry zipEntry) throws IOException {
        byte[] buffer = new byte[2048];
        final File zipEntryFile = new File(targetFeatureFilesPath, zipEntry.getName());
        if (!zipEntryFile.toPath().normalize().startsWith(targetFeatureFilesPath)) {
            throw new RuntimeException("Bad zip entry");
        }
        FileOutputStream fileOutputStream = new FileOutputStream(zipEntryFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream, buffer.length);
        int length;
        while ((length = zipInputStream.read(buffer, 0, buffer.length)) >= 0) {
            bufferedOutputStream.write(buffer, 0, length);
        }
        bufferedOutputStream.close();
        fileOutputStream.close();
    }
}
