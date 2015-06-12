package com.odonataworkshop.files.renamer.file;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileManipulation {
	protected static final Logger log = LogManager.getLogger(FileManipulation.class);
    private static final String PHOTOSHOP_XMP_FILE_EXTENSION = ".xmp";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private long mProcessedFiles;
    private long mTotalFiles;
    private long mStartTime;
    private ProcessingHandler mHandler;
    private List<String> mNotRenamedFiles = new LinkedList<String>();



    public void renameFiles(File aStartDir){
        mProcessedFiles = 0;
        mTotalFiles = 0;
        try {
            setTotalFiles(aStartDir);
        } catch (IOException e) {
            log.error("Cannot view all files", e);
        }
        setStartTime(System.currentTimeMillis());
        if(mHandler != null){
            mHandler.handleBeforeStartProcess(aStartDir);
        }
        renameFiles(aStartDir,mProcessedFiles);
        if(mHandler != null){
            mHandler.handleAfterEndProcess(aStartDir);
        }
    }

    public void renameFiles(File aStartDir, long aProcessedFiles) {
        File[] files = aStartDir.listFiles();
        String[] fileNames = aStartDir.list();
        List<File> children = new LinkedList<File>();
        Map<String, String> childrenNames = new LinkedHashMap<String, String>();
        if (aStartDir.isDirectory() && files != null) {
            children = Arrays.asList(files);
            for (String name : fileNames) {
                childrenNames.put(name, getFileCreateDateStr(name));
            }
        }
        for (File child : children) {
            if (child.isDirectory()) {
                renameFiles(child, aProcessedFiles);
            } else {
                if (mHandler != null) {
                    mHandler.handleProcessStart(child);
                }
                String pwd = child.getAbsolutePath().replace(child.getName(), "");
                String fileName = child.getName();
                String extension = getFileExtension(fileName);
                try {
                    String renameTo = renameImageFile(child, pwd, extension);
                    childrenNames.put(fileName, renameTo);
                } catch (ImageProcessingException ipe) {
                    if (ipe.getMessage().equals("File format is not supported")) {
                        if (extension.equals(PHOTOSHOP_XMP_FILE_EXTENSION)) {
                            String renameTo = findImageRenameTo(childrenNames, fileName);
                            if (renameTo != null) {
                                renameFile(child, pwd, renameTo, getFileExtension(fileName));
                            }
                        } else {
                            renameSimpleFile(child, pwd, getFileExtension(fileName));
                        }
                    } else {
                       mNotRenamedFiles.add(child.getAbsolutePath() + " - " + ipe.getMessage());
                       log.error("Unknown error occurred",ipe);
                    }
                }finally {
                    if (mHandler != null) {
                        mProcessedFiles++;
                        mHandler.handleProcessEnd(child);
                    }
                }
            }
        }
    }

    private void setTotalFiles(File aStartDir) throws IOException {
        Files.walkFileTree(aStartDir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                                                     BasicFileAttributes attrs) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                mTotalFiles++;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException e) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void addHandler(ProcessingHandler aHandler) {
        mHandler = aHandler;
    }


    public long getProcessedFiles() {
        return mProcessedFiles;
    }

    public long getTotalFiles() {
        return mTotalFiles;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public void setStartTime(long aStartTime) {
        mStartTime = aStartTime;
    }

    public List<String> getNotRenamedFiles() {
        return mNotRenamedFiles;
    }
    private String getSimpleName(String aFileName) {
        if (aFileName != null) {
            String extension = getFileExtension(aFileName);
            return aFileName.replace(extension, "");
        }
        return "";
    }

    private String getFileExtension(String aFileName) {

        String extension = "";
        int indx = aFileName != null ? aFileName.lastIndexOf(".") : -1;
        if (indx > 0) {
            extension = aFileName.substring(indx);
        }
        return extension;
    }

    private String getFileCreateDateStr(String aFile) {
        Date date = null;
        File file = new File(aFile);
        try {
            if (file.exists()) {
                Metadata childInfo = ImageMetadataReader.readMetadata(new File(aFile));
                ExifSubIFDDirectory directory = childInfo.getDirectory(ExifSubIFDDirectory.class);
                date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            }
        } catch (ImageProcessingException e) {
                if (e.getMessage().equals("File format is not supported")) {
                    BasicFileAttributes attrs = null;
                    try {
                        attrs = Files.readAttributes(new File(aFile).toPath(), BasicFileAttributes.class);
                        long createTime = attrs.creationTime().toMillis();
                        date = new Date(createTime);
                    } catch (IOException e1) {
                        log.error("Cannot read create time attribute",e1);
                    }

                } else{
                    log.error("Unknown error occurred",e);
                }
        } catch (IOException e) {
            log.error("Unknown error occurred",e);
        }
        return date != null ? SDF.format(date) : null;

    }

    private String findImageRenameTo(Map<String, String> aNames, String aFileName) {
        String found = null;
        for (Map.Entry<String, String> entry : aNames.entrySet()) {
            if (entry.getKey().startsWith(getSimpleName(aFileName)) && entry.getValue() != null) {
                found = getSimpleName(entry.getValue());
                break;
            }
        }
        return found;

    }

    private String renameFile(File aFile, String aPwd, String renameTo, String aExtension) {
        int i = 1;
        while (!aFile.renameTo(new File(aPwd + renameTo + aExtension))) {
            renameTo = renameTo + "_" + i;
            i++;
        }
        return renameTo;
    }

    private String renameSimpleFile(File aFile, String aPwd, String aExtension) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(aFile.toPath(), BasicFileAttributes.class);
            long createTime = attrs.creationTime().toMillis();
            Date date = new Date(createTime);
            return renameFile(aFile, aPwd, SDF.format(date), aExtension);
        } catch (IOException e) {
            mNotRenamedFiles.add(aFile.getAbsolutePath() + " - Cannot rename simple file" );
            log.error("Cannot rename simple file", e);
        }
        return null;
    }

    private String renameImageFile(File aFile, String aPwd, String aExtension) throws ImageProcessingException {
        try {
            Metadata childInfo = ImageMetadataReader.readMetadata(aFile);
            ExifSubIFDDirectory directory = childInfo.getDirectory(ExifSubIFDDirectory.class);
            Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            return renameFile(aFile, aPwd, SDF.format(date), aExtension);
        } catch (IOException e) {
            mNotRenamedFiles.add(aFile.getAbsolutePath() + " - Cannot rename image file");
           log.error("Cannot rename image file", e);
        }  catch (NullPointerException e){
            mNotRenamedFiles.add(aFile.getAbsolutePath() + " - No exif data found for rename.");
            log.error("No exif data found for rename!",e );
        }catch (Exception e){
            mNotRenamedFiles.add(aFile.getAbsolutePath());
            log.error("Unknown error occurred!",e );
        }
        return null;
    }
}
