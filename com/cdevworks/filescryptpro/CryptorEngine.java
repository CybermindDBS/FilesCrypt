package com.cdevworks.filescryptpro;

import android.os.StatFs;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

class CryptionConfigs {
    int cipherMode;
    byte[] pwd, iv;
    char[] usePasswordManagerToggle;
    Boolean encryptFileName = false, encryptFolderName = false, deleteSourceFileAfterOperation;
    String savePath;

    CryptionConfigs(int cipherMode, byte[] pwd, byte[] iv, String savePath, Boolean encryptFileName, Boolean encryptFolderName, Boolean deleteSourceFileAfterOperation, char[] usePasswordManagerToggle) {
        this.cipherMode = cipherMode;
        this.pwd = pwd;
        this.iv = iv;
        this.usePasswordManagerToggle = usePasswordManagerToggle;
        this.savePath = savePath;
        if (cipherMode == 1) {
            this.encryptFileName = encryptFileName;
            this.encryptFolderName = encryptFolderName;
        }
        this.deleteSourceFileAfterOperation = deleteSourceFileAfterOperation;
    }
}

class CryptorEngineInput implements Serializable {
    byte[] pwd, iv;
    char[] usePasswordManagerToggle;
    String sourceFilePath, fileSavePath, rootFolder, fileSavePathRevert;
    int cipherMode;
    boolean fileNameEncryptionStatus, deleteSourceFileAfterOperation, isFile, isFEPNCrypt;
    String origSourcePath;
    long fileSizeInBytes;

    CryptorEngineInput(String filePath, int cipherMode, byte[] pwd, byte[] iv, boolean fileNameEncryptionStatus, boolean deleteSourceFileAfterOperation, boolean isFile, String fileSavePath, String origSourcePath, String rootFolder, long fileSizeInBytes, char[] usePasswordManagerToggle, boolean isFEPNCrypt) {
        this.pwd = pwd;
        this.iv = iv;
        this.sourceFilePath = filePath;
        this.fileSavePath = fileSavePath;
        this.fileSavePathRevert = fileSavePath;
        this.rootFolder = rootFolder;
        this.cipherMode = cipherMode;
        this.isFile = isFile;
        //if isFEPNCrypt is true for a folder, then it supports getting decrypted with just password manager.
        this.isFEPNCrypt = isFEPNCrypt;
        //if usePasswordManagerToggle = new char[0] -> password manager mode off, if usePasswordManagerToggle = getMaster() and pwd = null -> password manager mode on.
        this.usePasswordManagerToggle = usePasswordManagerToggle;
        this.fileNameEncryptionStatus = fileNameEncryptionStatus;
        this.deleteSourceFileAfterOperation = deleteSourceFileAfterOperation;
        this.origSourcePath = origSourcePath;
        this.fileSizeInBytes = fileSizeInBytes;
    }
}

class CryptorEngineInputHandler {
    static boolean isBadInput(@NonNull String name, int cipherMode) {
        if (name.contains(CryptorEngine.fileExtension)) {
            return cipherMode == 1;
        } else {
            return cipherMode != 1;
        }
    }

    static boolean isBadDirInput(String name, int cipherMode) {
        if (cipherMode == 1) {
            return name.contains(CryptorEngine.dirExtension);
        } else {
            return false;
        }
    }

    static long getFileSize(Path filePath) {
        long fileSizeInBytes;
        try {
            // dividing long with 1048576L for getting it in MB representation.
            fileSizeInBytes = Files.size(filePath);
        } catch (IOException e) {
            fileSizeInBytes = 0L;
        }
        return fileSizeInBytes;
    }

    static void makeNewInputs(@NonNull ArrayList<String> inputFiles, CryptionConfigs configs) {
        Log.i("tag1", "makeNewInputs: " + inputFiles);
        Log.i("tag1", "makeNewInputs: " + configs.usePasswordManagerToggle.length);
        FileOperationLog.makingCryptorEngineInputs = true;
        ArrayList<CryptorEngineInput> cryptorEngineInputs = new ArrayList<>();
        //In folderCryptorEngineInputs, the sub folders will be added, and these sub-folder's names will be encrypted/decrypted when all cryptorEngines are free and available after processing files, (so that no problem occurs in file paths).
        ArrayList<CryptorEngineInput> folderCryptorEngineInputs = new ArrayList<>();
        ArrayList<String> files = new ArrayList<>(inputFiles);
        for (String filePath : files) {
            File sourceFile = new File(filePath);
            if (sourceFile.isFile()) {
                if (isBadInput(filePath, configs.cipherMode)) continue;
                String fileSavePath;
                if (configs.savePath.equals("SameAsSource")) fileSavePath = sourceFile.getPath();
                else fileSavePath = configs.savePath + File.separator + sourceFile.getName();
                cryptorEngineInputs.add(new CryptorEngineInput(filePath, configs.cipherMode, configs.pwd, configs.iv, configs.encryptFileName, configs.deleteSourceFileAfterOperation, true, fileSavePath, null, null, getFileSize(sourceFile.toPath()), configs.usePasswordManagerToggle, false));
            } else {
                /*
                DESC: Mechanism of Folder Cipher.
                Source location: /storage/emulated/0/MyFolder
                Save location: /storage/emulated/0/HERE
                step 1: generate modified source folder name in HERE, lets say /storage/emulated/0/HERE/"MyFolder Encrypted-FEP" or /storage/emulated/0/HERE/"MyFolder Decrypted-FEP" for decryption.
                step 2: now, for example to encrypt this file /storage/emulated/0/MyFolder/File1
                process:-
                to generate the final savePath:
                1. Replace source file's source location path "/storage/emulated/0/MyFolder"/Folder2/File1 with save location path + modified source folder name
                i.e /storage/emulated/0/HERE/MyFolder_Encrypted-FilesCrypt/Folder2/File1
                step 4: after creating and finishing the cryptorEngineInputs for all the files, proceed with folder name encryption/decryption if needed.
                FINISH
                 */
                File sourceFolder = new File(filePath);
                File originalSourceFolder = new File(filePath);
                String folderSavePath;
                if (configs.savePath.equals("SameAsSource"))
                    folderSavePath = Objects.requireNonNull(sourceFolder.getParentFile()).getPath();
                else folderSavePath = configs.savePath;
                String modifiedSourceFolderName;
                String actionName = " - Encrypt";
                String actionName2 = " - Decrypt";
                StringBuilder sb;

                //Code for generating the folder's distinct name.
                if (configs.cipherMode == 1) {
                    if (sourceFolder.getName().contains(CryptorEngine.dirExtension)) continue;
                    sb = new StringBuilder(sourceFolder.getName());
                    if (sourceFolder.getName().contains(actionName)) {
                        int indexOfActionName = sb.lastIndexOf(actionName);
                        modifiedSourceFolderName = sb.replace(indexOfActionName, sb.lastIndexOf(actionName) + actionName.length(), actionName).toString();
                        modifiedSourceFolderName = CEFileHandler.getNewDistinctFolderName(folderSavePath + File.separator + modifiedSourceFolderName);
                    } else if (sourceFolder.getName().contains(actionName2)) {
                        int indexOfActionName2 = sb.lastIndexOf(actionName2);
                        modifiedSourceFolderName = sb.replace(indexOfActionName2, indexOfActionName2 + actionName2.length(), actionName).toString();
                        modifiedSourceFolderName = CEFileHandler.getNewDistinctFolderName(folderSavePath + File.separator + modifiedSourceFolderName);
                    } else {
                        modifiedSourceFolderName = CEFileHandler.getNewDistinctFolderName(folderSavePath + File.separator + sourceFolder.getName() + actionName);
                    }
                } else {
                    if (sourceFolder.getName().contains(CryptorEngine.dirExtension)) {
                        StringBuilder builder = new StringBuilder(sourceFolder.getName());
                        sourceFolder = new File(Objects.requireNonNull(sourceFolder.getParentFile()).getPath() + File.separator + CryptorEngine.cipherAndCodeText(builder.substring(0, builder.lastIndexOf("PMHASH_")), configs.cipherMode, null, com.cdevworks.filescryptpro.PasswordManager.getKey(builder.substring(builder.lastIndexOf("PMHASH_") + 7, builder.lastIndexOf(CryptorEngine.dirExtension)))));
                    }
                    sb = new StringBuilder(sourceFolder.getName());
                    if (sourceFolder.getName().contains(actionName)) {
                        int indexOfActionName = sb.lastIndexOf(actionName);
                        modifiedSourceFolderName = sb.replace(indexOfActionName, indexOfActionName + actionName.length(), actionName2).toString();
                        modifiedSourceFolderName = CEFileHandler.getNewDistinctFolderName(folderSavePath + File.separator + modifiedSourceFolderName);
                    } else if (sourceFolder.getName().contains(actionName2)) {
                        int indexOfActionName2 = sb.lastIndexOf(actionName2);
                        modifiedSourceFolderName = sb.replace(indexOfActionName2, indexOfActionName2 + actionName2.length(), actionName2).toString();
                        modifiedSourceFolderName = CEFileHandler.getNewDistinctFolderName(folderSavePath + File.separator + modifiedSourceFolderName);
                    } else {
                        modifiedSourceFolderName = CEFileHandler.getNewDistinctFolderName(folderSavePath + File.separator + sourceFolder.getName() + actionName2);
                    }
                }
                String rootFolder = folderSavePath + File.separator + modifiedSourceFolderName;

                try {
                    String finalModifiedSourceFolderName = modifiedSourceFolderName;
                    Files.walkFileTree(originalSourceFolder.toPath(), new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path sourceFilePath, BasicFileAttributes attrs) {
                            StringBuilder sourceFileSavePath = new StringBuilder(sourceFilePath.toString());
                            sourceFileSavePath.replace(0, filePath.length(), folderSavePath + File.separator + finalModifiedSourceFolderName);
                            if (isBadInput(sourceFilePath.toString(), configs.cipherMode)) {
                                return FileVisitResult.CONTINUE;
                            }
                            cryptorEngineInputs.add(new CryptorEngineInput(sourceFilePath.toString(), configs.cipherMode, configs.pwd, configs.iv, configs.encryptFileName, configs.deleteSourceFileAfterOperation, true, sourceFileSavePath.toString(), null, rootFolder, getFileSize(sourceFilePath), configs.usePasswordManagerToggle, false));
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });

                    if (configs.cipherMode == 2 || configs.encryptFolderName || configs.deleteSourceFileAfterOperation)
                        Files.walkFileTree(originalSourceFolder.toPath(), new SimpleFileVisitor<>() {
                            boolean isFEPNCrypt = false;

                            @Override
                            public FileVisitResult postVisitDirectory(Path directoryPath, IOException exc) {
                                if (directoryPath.toString().equals(sourceFile.getPath())) {
                                    return FileVisitResult.CONTINUE;
                                }
                                if (isBadDirInput(directoryPath.toString(), configs.cipherMode))
                                    return FileVisitResult.CONTINUE;
                                if (configs.cipherMode == 2) {
                                    if (directoryPath.toFile().getName().contains(CryptorEngine.fileExtension)) {
                                        configs.encryptFolderName = true;
                                        isFEPNCrypt = false;
                                    } else if (directoryPath.toFile().getName().contains(CryptorEngine.dirExtension)) {
                                        configs.encryptFolderName = isFEPNCrypt = true;
                                    } else {
                                        configs.encryptFolderName = isFEPNCrypt = false;
                                    }
                                } else if (configs.usePasswordManagerToggle.length > 0)
                                    isFEPNCrypt = true;
                                StringBuilder sourceFolderSavePath = new StringBuilder(directoryPath.toString());
                                sourceFolderSavePath.replace(0, filePath.length(), folderSavePath + File.separator + finalModifiedSourceFolderName);
                                CEFileHandler.createFolder(Paths.get(sourceFolderSavePath.toString()));
                                if (configs.encryptFolderName || configs.deleteSourceFileAfterOperation)
                                    folderCryptorEngineInputs.add(new CryptorEngineInput(sourceFolderSavePath.toString(), configs.cipherMode, configs.pwd, configs.iv, configs.encryptFolderName, configs.deleteSourceFileAfterOperation, false, null, directoryPath.toString(), rootFolder, 0L, configs.usePasswordManagerToggle, isFEPNCrypt));
                                return FileVisitResult.CONTINUE;
                            }

                            @Override
                            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                                return FileVisitResult.CONTINUE;
                            }
                        });

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (configs.deleteSourceFileAfterOperation)
                    CryptorEngineHandler.rootFolders.add(originalSourceFolder.toPath());
            }
        }

        FileOperationLog.totalFiles += cryptorEngineInputs.size();
        if (configs.cipherMode == 1) FileOperationLog.filesToEncrypt += cryptorEngineInputs.size();
        else FileOperationLog.filesToDecrypt += cryptorEngineInputs.size();
        long totalBytes = 0L;
        for (CryptorEngineInput cryptorEngineInput : cryptorEngineInputs) {
            totalBytes += cryptorEngineInput.fileSizeInBytes;
        }

        String storageLocationSpaceCheck = configs.savePath;
        if (storageLocationSpaceCheck.equals("SameAsSource"))
            storageLocationSpaceCheck = MainActivity.externalFilesDir.getPath();
        if (CEUtils.availableStorageSpace(storageLocationSpaceCheck) > ((FileOperationLog.totalSizeInMB - (FileOperationLog.processedBytes / 1048576L)) + (totalBytes / 1048576L))) {
            FileOperationLog.totalSizeInMB += totalBytes / 1048576L;
            CryptorEngineHandler.processQueue.addAll(cryptorEngineInputs);
            CryptorEngineHandler.folderNameCryptionQueue.addAll(folderCryptorEngineInputs);
            Log.i("", ">>>>>>>>>>>>>>>>>>>>>>>>> ADDED NEW Inputs to CryptorEngineHandler, Files: " + CryptorEngineHandler.processQueue.size() + ", Folders: " + CryptorEngineHandler.folderNameCryptionQueue.size());
            CryptorEngine.cryptorEngineHandler.notifyAddingInputs();
        } else {
            if (!FileOperationLog.cryptorEnginesProcessing) {
                FileOperationLog.refresh();
                FileOperationLog.newReport();
                CryptorEngineHandler.processQueue.clear();
                CryptorEngineHandler.folderNameCryptionQueue.clear();
                CryptorEngineHandler.rootFolders.clear();
            }
            //noinspection ResultOfMethodCallIgnored
            MainActivity.reportFile.delete();
            CEUtils.showMessage("Insufficient available storage space. Please ensure an adequate amount of free space is available and try again.", 2);
        }
        FileOperationLog.makingCryptorEngineInputs = false;
    }
}

class Buffer {
    int bytesRead = 0;
    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(9830400);
}

class BufferPool {
    final static Queue<Buffer> bufferPool = new ConcurrentLinkedQueue<>();

    static void createBuffers(int amount) {
        Log.i("", "_>>> Buffers Created: " + amount);
        while (--amount >= 0) {
            BufferPool.bufferPool.add(new Buffer());
        }
    }

    static Buffer getBuffer() {
        Buffer buffer;
        while ((buffer = BufferPool.bufferPool.poll()) == null) {
            try {
                //noinspection BusyWait
                Thread.sleep(10);
                Log.i("", "WAITING FOR BUFFER, BUFFER COUNT: " + BufferPool.bufferPool.size());
            } catch (InterruptedException e) {
                CEUtils.showMessage("Please Restart the App.", 1);
                try {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                throw new RuntimeException(e);
            }
        }
        return buffer;
    }
}

public class CryptorEngine implements Runnable {
    static String version = MainActivity.version;
    boolean engineAvailable;
    boolean runThreadCE, runThreadFR, runThreadBC, runThreadFW, ongoingProcessStatus;
    static String fileExtension = ".cryptf", dirExtension = ".cryptd";
    Cipher cipher, cipher2;
    MessageDigest digest;
    int cipherMode;
    SecretKey secretKey, secretKey1;
    SecretKeyFactory factory;
    byte[] pwd, iv, salt;
    IvParameterSpec parameterSpec;
    SecureRandom random;
    Queue<Buffer> readBatchBufferPool;
    Queue<Buffer> processedBatchBufferPool;
    File sourceFile, processedFile;
    CryptorEngineInput cryptorEngineInput;
    ExecutorService pool, generalService;
    Thread cryptorEngine;
    Runnable fileReaderRunnable, byteCrypterRunnable, fileWriterRunnable, engineAvail;
    byte[] bytesHolder;
    char[] usePasswordManagerToggle;
    String alias = "";
    Buffer FRBuffer, BCBuffer1, BCBuffer2, FWBuffer;
    static Cipher cipher3;
    static SecretKeyFactory factory2;
    static LinkedHashSet<String> cleanupFolders = new LinkedHashSet<>();
    static CryptorEngineHandler cryptorEngineHandler = new CryptorEngineHandler();
    static byte[] constantVal = {48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48, 48};

    //denotes string "EmptyFile"
    static byte[] constantVal2 = {69, 109, 112, 116, 121, 70, 105, 108, 101};

    //represents the value of constantVal2 after hashing it with SHA-256.
    static byte[] constantVal3 = {-69, -65, 32, 106, -100, -12, -39, -42, -46, 110, -78, 60, -12, -81, 35, -113, -54, -74, -21, -7, -41, 10, 18, -46, -116, -5, 113, -99, 69, 87, -15, 98};

    //constant IV for encrypting/decrypting folder-names.
    static IvParameterSpec constantParameterSpec = new IvParameterSpec(constantVal);

    static {
        try {
            cipher3 = Cipher.getInstance("AES/CBC/PKCS7Padding");
            factory2 = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    CryptorEngine() {
        runThreadCE = false;
        cryptorEngine = new Thread(this);
        cryptorEngine.setName("FEP_CryptorEngine");
        cryptorEngine.start();

        runThreadCE = runThreadFR = runThreadBC = runThreadFW = false;

        pool = Executors.newFixedThreadPool(3);
        generalService = Executors.newFixedThreadPool(1);

        fileReaderRunnable = this::fileReader;
        byteCrypterRunnable = this::byteCrypter;
        fileWriterRunnable = this::fileWriter;
        engineAvail = () -> {
            while (!(cryptorEngine.getState() == Thread.State.WAITING) && CryptorEngineHandler.runEnginesAndHandler) {
                try {
                    //noinspection BusyWait
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            engineAvailable = true;
        };

        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher2 = Cipher.getInstance("AES/CBC/NoPadding");
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            digest = MessageDigest.getInstance("SHA-256");
            random = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }

        engineAvailable = true;
    }

    void setCommonConfigs() {
        Log.i("tag1", " <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<SET COMMON CONFIGS>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        pwd = cryptorEngineInput.pwd;
        salt = constantVal;
        if (pwd == null) return;
        try {
            //check whether this iteration count needs to be reduced, to be tested in low end device.
            KeySpec spec = new PBEKeySpec(new String(pwd, StandardCharsets.UTF_8).toCharArray(), salt, 6300, 256);
            secretKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
            if (cipherMode == 1) {
                //Generates alias for password manager purpose.
                byte[] mergedArray = new byte[constantVal2.length + pwd.length + constantVal3.length];
                ByteBuffer buff = ByteBuffer.wrap(mergedArray);
                buff.put(constantVal2);
                buff.put(pwd);
                buff.put(constantVal3);
                mergedArray = buff.array();
                alias = Base64.getUrlEncoder().encodeToString(generateHash(mergedArray));
                if (usePasswordManagerToggle.length > 1)
                    com.cdevworks.filescryptpro.PasswordManager.insert(alias, secretKey);
            }

        } catch (InvalidKeySpecException e) {
            CEUtils.showMessage("Invalid Key Exception", 1);
            throw new RuntimeException();
        }
    }

    @Override
    public void run() {
        while (CryptorEngineHandler.runEnginesAndHandler) {
            if (runThreadCE) {
                runThreadCE = false;
                ongoingProcessStatus = true;
                int cryptionResult;
                try {
                    if (cryptorEngineInput.cipherMode == 1) {
                        if (cryptorEngineInput.iv == null) {
                            iv = new byte[cipher.getBlockSize()];
                            random.nextBytes(iv);
                        } else iv = cryptorEngineInput.iv;
                        parameterSpec = new IvParameterSpec(iv);
                    }
                    cipherMode = cryptorEngineInput.cipherMode;
                    sourceFile = new File(cryptorEngineInput.sourceFilePath);
                    usePasswordManagerToggle = cryptorEngineInput.usePasswordManagerToggle;
                    readBatchBufferPool = new ConcurrentLinkedQueue<>();
                    processedBatchBufferPool = new ConcurrentLinkedQueue<>();
                    runThreadFR = runThreadBC = runThreadFW = true;
                    if (!Arrays.equals(cryptorEngineInput.pwd, pwd)) setCommonConfigs();
                    if (cryptorEngineInput.isFile) {
                        processedFile = CEFileHandler.getNewTempFile(cryptorEngineInput.fileSavePath);
                        cryptionResult = doFileCipher();
                    } else {
                        cryptionResult = doFolderCipher();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    cryptionResult = -4;
                }
                String operation, object;
                if (cipherMode == 1) operation = "encrypt";
                else operation = "decrypt";

                if (cryptorEngineInput.isFile) object = "File";
                else object = "Folder";

                switch (cryptionResult) {
                    case 0:
                        //Status Code: Wrong Password used for <operation> the <object>
                        Log.i("res", "Wrong Password used for " + operation + "ing the " + object);
                        OperationReport.logger(FileOperationLog.report.log, "Wrong Password used for " + operation + "ing the " + object, cryptorEngineInput);
                        cryptorEngineInput.fileSavePath = cryptorEngineInput.fileSavePathRevert;
                        //noinspection ResultOfMethodCallIgnored
                        processedFile.delete();
                        cleanupFolders.add(cryptorEngineInput.rootFolder);
                        break;
                    case 1:
                        if (!cryptorEngineInput.isFile) object = "Folder's name";
                        //Code: File/Folder's name has been successfully <operation>.
                        Log.i("tag1", object + " has been successfully " + operation + "ed");
                        if (cryptorEngineInput.isFile) {
                            FileOperationLog.incrementFilesProcessed();
                            if (cryptorEngineInput.deleteSourceFileAfterOperation) {
                                //noinspection ResultOfMethodCallIgnored
                                sourceFile.delete();
                            }
                            OperationReport.logger(FileOperationLog.report.log, object + " has been successfully " + operation + "ed", cryptorEngineInput);
                        } else {
                            if (cryptorEngineInput.deleteSourceFileAfterOperation) try {
                                Files.delete(Paths.get(cryptorEngineInput.origSourcePath));
                            } catch (IOException ignored) {
                            }
                        }
                        break;
                    case 2:
                        //This case takes place when a folder only needs to be deleted and not needing to be renamed.
                        if (cryptorEngineInput.deleteSourceFileAfterOperation) try {
                            Files.delete(Paths.get(cryptorEngineInput.origSourcePath));
                        } catch (IOException ignored) {
                        }
                        break;
                    case -1:
                        //Status Code: Error while <operation> the <object> name
                        Log.i("res", "Error while " + operation + "ing the " + object + " name");
                        OperationReport.logger(FileOperationLog.report.log, "Error while " + operation + "ing the " + object + " name", cryptorEngineInput);
                        cryptorEngineInput.fileSavePath = cryptorEngineInput.fileSavePathRevert;
                        if (!cryptorEngineInput.isFile) //noinspection ResultOfMethodCallIgnored
                            sourceFile.delete();
                        break;
                    case -2:
                        //Status Code: Error while <operation> the <object> content
                        Log.i("res", "Error while " + operation + "ing the " + object + "'s content");
                        OperationReport.logger(FileOperationLog.report.log, "Error while " + operation + "ing the " + object + "'s content", cryptorEngineInput);
                        cryptorEngineInput.fileSavePath = cryptorEngineInput.fileSavePathRevert;
                        //noinspection ResultOfMethodCallIgnored
                        processedFile.delete();
                        break;
                    case -3:
                        //Status Code: Error while <operation> the <object>
                        Log.i("res", "Error while " + operation + "ing the " + object);
                        OperationReport.logger(FileOperationLog.report.log, "Error while " + operation + "ing the " + object, cryptorEngineInput);
                        cryptorEngineInput.fileSavePath = cryptorEngineInput.fileSavePathRevert;
                        //noinspection ResultOfMethodCallIgnored
                        processedFile.delete();
                        break;
                    case -4:
                        //Status Code: Unknown Error.
                        Log.i("res", "Unknown Error");
                        OperationReport.logger(FileOperationLog.report.log, "Unknown Error", cryptorEngineInput);
                        cryptorEngineInput.fileSavePath = cryptorEngineInput.fileSavePathRevert;
                        if (processedFile != null) //noinspection ResultOfMethodCallIgnored
                            processedFile.delete();
                        break;
                    case -6:
                        //Status Code: Password not found in password manager.
                        Log.i("res", "Password not found in password manager");
                        OperationReport.logger(FileOperationLog.report.log, "Password not found in password manager", cryptorEngineInput);
                        cryptorEngineInput.fileSavePath = cryptorEngineInput.fileSavePathRevert;
                        if (processedFile != null) //noinspection ResultOfMethodCallIgnored
                            processedFile.delete();
                        break;
                }
            } else {
                if (engineAvail != null) generalService.execute(engineAvail);
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        generalService.shutdown();
        pool.shutdown();
    }

    void configureAndRun(CryptorEngineInput cryptorEngineInput) {
        this.cryptorEngineInput = cryptorEngineInput;
        runThreadCE = true;
        synchronized (this) {
            notify();
        }
    }

    int doFolderCipher() {
        boolean res;
        if (cryptorEngineInput.fileNameEncryptionStatus) {
            res = cipherFileName();
        } else return 2;
        if (res) return 1;
        else return -1;
    }

    int doFileCipher() {
        boolean fileContentCipherResult, fileNameCipherResult = true;
        int returnCode, parseTagResult;
        if (cipherMode == 1) {
            GenerateFileTag();
        } else {
            parseTagResult = ParseFileTag();
            if (parseTagResult != 1 && parseTagResult != -5) {
                return parseTagResult;
            }
        }
        fileContentCipherResult = cipherFileContent();
        if (cryptorEngineInput.fileNameEncryptionStatus) fileNameCipherResult = cipherFileName();
        if (fileContentCipherResult && fileNameCipherResult) returnCode = 1;
        else if (fileContentCipherResult) returnCode = -1;
        else if (fileNameCipherResult) returnCode = -2;
        else returnCode = -3;

        return returnCode;
    }

    void GenerateFileTag() {
        try (FileInputStream fileInputStream = new FileInputStream(sourceFile); FileWriter fileWriter = new FileWriter(processedFile)) {
            byte[] bytesContent = new byte[16];
            int bytesRead = fileInputStream.read(bytesContent);
            if (bytesRead == -1) {
                //if file size == 0 bytes, the following array translates to 'EmptyFile'.
                bytesContent = constantVal2;
            } else if (bytesRead != 16) bytesContent = Arrays.copyOf(bytesContent, bytesRead);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            bytesHolder = digest.digest(bytesContent);
            JSONObject encryptedFileProperties = new JSONObject();
            encryptedFileProperties.put("Algorithm", "AES/CBC/PKCS7Padding");
            encryptedFileProperties.put("InitializationVector", new JSONArray(iv));
            encryptedFileProperties.put("FilenameEncryption", cryptorEngineInput.fileNameEncryptionStatus);
            encryptedFileProperties.put("PartialContentHash", new JSONArray(bytesHolder));
            if (usePasswordManagerToggle.length > 0) encryptedFileProperties.put("PMHash", alias);
            fileWriter.write("<ENCRYPTION PROPERTIES>\nThis file is encrypted with FilesCrypt Pro V" + version + "\n" + encryptedFileProperties + "\n</ENCRYPTION PROPERTIES>\n");
        } catch (IOException | JSONException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    int ParseFileTag() {
        try (FileInputStream fileInputStream = new FileInputStream(sourceFile); InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream); BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            bufferedReader.readLine();
            bufferedReader.readLine();
            JSONObject jsonObject = new JSONObject(bufferedReader.readLine());
            cryptorEngineInput.fileNameEncryptionStatus = jsonObject.getBoolean("FilenameEncryption");
            JSONArray jsonArray = jsonObject.getJSONArray("InitializationVector");
            iv = new byte[cipher.getBlockSize()];
            for (int i = 0; i < jsonArray.length(); i++) {
                iv[i] = (byte) jsonArray.getInt(i);
            }
            parameterSpec = new IvParameterSpec(iv);
            JSONArray jsonArray2 = jsonObject.getJSONArray("PartialContentHash");
            byte[] partialContentHash = new byte[32];
            for (int i = 0; i < jsonArray2.length(); i++) {
                partialContentHash[i] = (byte) jsonArray2.getInt(i);
            }
            if (usePasswordManagerToggle.length > 0) {
                if (com.cdevworks.filescryptpro.PasswordManager.authorize(usePasswordManagerToggle)) {
                    try {
                        String PMHash = jsonObject.getString("PMHash");
                        if (!PMHash.equals(alias) || secretKey1 == null) {
                            alias = PMHash;
                            secretKey1 = com.cdevworks.filescryptpro.PasswordManager.getKey(alias);
                        }
                        if (secretKey1 == null) {
                            Log.i("tag1", "ParseFileTag: getKey() returned null");
                            Log.i("tag1", "Password not found in Password Manager!");
                            //return -6 if no key found in password manager.
                            return -6;
                        } else secretKey = secretKey1;
                    } catch (JSONException e) {
                        // return -6 if PMHash not entry present in FEPTag.
                        Log.i("tag1", "No PMHash in FEPTag!");
                        return -6;
                    }
                }
            }
            if (checkPW(partialContentHash)) return 1;
            else {
                Log.i("tag1", "Wrong Password!");
                return 0;
            }
        } catch (JSONException e) {
            cryptorEngineInput.fileNameEncryptionStatus = false;
            //return -5 if FEPTag not present or is corrupted.
            return -5;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    boolean checkPW(byte[] bytesContentHash) {
        try (FileInputStream fileInputStream = new FileInputStream(sourceFile)) {
            byte[] bytesContent = new byte[17];
            //The following code is for skipping to read the FileTag in the file.
            int character, counter = 0;
            while (counter != 4) {
                character = fileInputStream.read();
                if ((char) character == '\n') {
                    counter++;
                }
            }

            Cipher cipherInstance;
            int bytesRead = fileInputStream.read(bytesContent);
            if (bytesRead > 16) {
                cipherInstance = cipher2;
                bytesContent = Arrays.copyOf(bytesContent, 16);
            } else {
                if (Arrays.equals(bytesContentHash, constantVal3)) return true;
                cipherInstance = cipher;
                bytesContent = Arrays.copyOf(bytesContent, bytesRead);
            }
            cipherInstance.init(2, secretKey, parameterSpec);
            bytesContent = cipherInstance.doFinal(bytesContent);
            bytesHolder = generateHash(bytesContent);
            return Arrays.equals(bytesHolder, bytesContentHash);
        } catch (InvalidAlgorithmParameterException | IOException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            return false;
        }
    }

    byte[] generateHash(byte[] input) {
        bytesHolder = digest.digest(input);
        digest.reset();
        return bytesHolder;
    }

    void fileReader() {
        FRBuffer = null;
        try (FileInputStream fis = new FileInputStream(sourceFile); FileChannel fileChannel = fis.getChannel()) {
            //The following code is for skipping to read the FileTag in the file.
            if (cipherMode == 2) {
                int character, counter = 0;
                while (counter != 4) {
                    character = fis.read();
                    if ((char) character == '\n') {
                        counter++;
                    }
                }
            }
            for (FRBuffer = BufferPool.getBuffer(), FRBuffer.byteBuffer.limit(FRBuffer.byteBuffer.capacity() - 16), FRBuffer.bytesRead = fileChannel.read(FRBuffer.byteBuffer); FRBuffer.bytesRead > 0 && runThreadFR; FRBuffer = BufferPool.getBuffer(), FRBuffer.byteBuffer.limit(FRBuffer.byteBuffer.capacity() - 16), FRBuffer.bytesRead = fileChannel.read(FRBuffer.byteBuffer)) {
                readBatchBufferPool.add(FRBuffer);
            }
            FRBuffer.byteBuffer.clear();
            if (!BufferPool.bufferPool.contains(FRBuffer)) BufferPool.bufferPool.add(FRBuffer);
        } catch (Exception e) {
            if (ongoingProcessStatus) terminateThreadsAndClearBuffers();
        }
        runThreadFR = false;
    }

    void byteCrypter() {
        BCBuffer1 = null;
        BCBuffer2 = null;
        try {
            while ((runThreadFR || !readBatchBufferPool.isEmpty()) && runThreadBC) {
                while (readBatchBufferPool.isEmpty() && runThreadFR) {
                    //noinspection BusyWait
                    Thread.sleep(10);
                }
                if ((BCBuffer1 = readBatchBufferPool.poll()) != null) {
                    BCBuffer2 = BufferPool.getBuffer();
                    BCBuffer1.byteBuffer.flip();
                    cipher.update(BCBuffer1.byteBuffer, BCBuffer2.byteBuffer);
                    BCBuffer2.byteBuffer.flip();
                    processedBatchBufferPool.add(BCBuffer2);
                    BCBuffer1.byteBuffer.clear();
                    BufferPool.bufferPool.add(BCBuffer1);
                }
            }
        } catch (ShortBufferException | InterruptedException e) {
            if (ongoingProcessStatus) terminateThreadsAndClearBuffers();
        }
        runThreadBC = false;
    }

    void fileWriter() {
        FWBuffer = null;
        try (FileOutputStream fos = new FileOutputStream(processedFile, true); FileChannel fileChannel = fos.getChannel()) {
            while ((runThreadBC || !processedBatchBufferPool.isEmpty()) && runThreadFW) {
                while (processedBatchBufferPool.isEmpty() && runThreadBC) {
                    try {
                        //noinspection BusyWait
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if ((FWBuffer = processedBatchBufferPool.poll()) != null) {
                    FWBuffer.bytesRead = fileChannel.write(FWBuffer.byteBuffer);
                    FileOperationLog.updateBytesProcessed(FWBuffer.bytesRead);
                    FWBuffer.byteBuffer.clear();
                    BufferPool.bufferPool.add(FWBuffer);
                }
            }
            fos.write(cipher.doFinal());
            runThreadFW = false;
            if (ongoingProcessStatus) synchronized (this) {
                notifyAll();
            }
        } catch (Exception e) {
            if (ongoingProcessStatus) terminateThreadsAndClearBuffers();
        }
    }

    synchronized void terminateThreadsAndClearBuffers() {
        Log.i("tag1", ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  terminateThreadsAndClearBuffersX");
        if (!ongoingProcessStatus) return;
        Log.i("tag1", ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  terminateThreadsAndClearBuffers");
        ongoingProcessStatus = false;
        runThreadFR = runThreadBC = runThreadFW = false;
        pool.shutdownNow();
        for (Buffer buffer : readBatchBufferPool) {
            buffer.byteBuffer.clear();
        }
        for (Buffer buffer : processedBatchBufferPool) {
            buffer.byteBuffer.clear();
        }
        pool = Executors.newFixedThreadPool(3);
        synchronized (this) {
            notifyAll();
        }
    }

    boolean cipherFileName() {
        try {
            if (cryptorEngineInput.isFile) cipher.init(cipherMode, secretKey, parameterSpec);
            else cipher.init(cipherMode, secretKey, constantParameterSpec);
        } catch (InvalidKeyException e) {
            return false;
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        String processedFileName;
        File renamedFile;
        try {
            if (cipherMode == 1) {
                if (cryptorEngineInput.isFile) {
                    processedFileName = Base64.getUrlEncoder().encodeToString(cipher.doFinal(sourceFile.getName().getBytes(StandardCharsets.UTF_8))) + CryptorEngine.fileExtension;
                } else {
                    if (cryptorEngineInput.isFEPNCrypt)
                        processedFileName = Base64.getUrlEncoder().encodeToString(cipher.doFinal(sourceFile.getName().getBytes(StandardCharsets.UTF_8))) + "PMHASH_" + alias + CryptorEngine.dirExtension;
                    else
                        processedFileName = Base64.getUrlEncoder().encodeToString(cipher.doFinal(sourceFile.getName().getBytes(StandardCharsets.UTF_8))) + CryptorEngine.fileExtension;
                }
            } else {
                StringBuilder sb = new StringBuilder(sourceFile.getName());
                sb.delete(sb.lastIndexOf(".crypt"), sb.length());
                if (sb.charAt(0) == '(') {
                    while (sb.charAt(0) != ' ') {
                        sb.deleteCharAt(0);
                    }
                    sb.deleteCharAt(0);
                }

                if (cryptorEngineInput.isFEPNCrypt) {
                    alias = sb.substring(sb.lastIndexOf("PMHASH_") + 7);
                    secretKey1 = com.cdevworks.filescryptpro.PasswordManager.getKey(alias);
                    processedFileName = sb.substring(0, sb.lastIndexOf("PMHASH_"));
                    if (usePasswordManagerToggle.length > 0) {
                        if (secretKey1 == null) {
                            try {
                                cipher.init(cipherMode, secretKey, constantParameterSpec);
                                processedFileName = new String(cipher.doFinal(Base64.getUrlDecoder().decode(processedFileName.getBytes(StandardCharsets.UTF_8))));
                            } catch (Exception e) {
                                return false;
                            }
                        } else {
                            secretKey = secretKey1;
                            cipher.init(cipherMode, secretKey, constantParameterSpec);
                            processedFileName = new String(cipher.doFinal(Base64.getUrlDecoder().decode(processedFileName.getBytes(StandardCharsets.UTF_8))));
                        }
                    } else
                        processedFileName = new String(cipher.doFinal(Base64.getUrlDecoder().decode(processedFileName.getBytes(StandardCharsets.UTF_8))));
                } else
                    processedFileName = new String(cipher.doFinal(Base64.getUrlDecoder().decode(sb.toString().getBytes(StandardCharsets.UTF_8))));

            }
            // Renaming .tmpf file to .cryptf file.
            if (cryptorEngineInput.isFile)
                renamedFile = CEFileHandler.renameFile(processedFile, processedFileName);
            else renamedFile = CEFileHandler.renameFile(sourceFile, processedFileName);
            if (renamedFile == null) {
                return false;
            }
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException e) {
            return false;
        }
        processedFile = renamedFile;
        return true;
    }

    boolean cipherFileContent() {
        /* DESC: File cipher architecture :-
        All the three threads run in parallel,
        one thread reads bytes from file to a queue (continuously batch by batch).
        one thread fetches the bytes from queue, processes(encrypt/decrypt) the bytes and stores it in another queue one by one.
        one thread fetches the processed bytes from the other queue and writes it to the file one by one.
         */
        try {
            cipher.init(cipherMode, secretKey, parameterSpec);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        pool.execute(fileReaderRunnable);
        pool.execute(byteCrypterRunnable);
        pool.execute(fileWriterRunnable);

        //waiting until all these three threads have completed their work.
        while (runThreadFW) {
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        if (!ongoingProcessStatus) {
            return false;
        }

//         Renaming .tmpf file to cryptf file if file name encryption is false, (if it was true then it will be taken care in cipherFileName() method).
        if (!cryptorEngineInput.fileNameEncryptionStatus) {
            if (cipherMode == 1) {
                return CEFileHandler.renameFile(processedFile, sourceFile.getName() + CryptorEngine.fileExtension) != null;
            } else {
                StringBuilder sb = new StringBuilder(sourceFile.getName());
                sb.delete(sb.lastIndexOf(".crypt"), sb.length());
                return CEFileHandler.renameFile(processedFile, sb.toString()) != null;
            }
        }
        return true;
    }

    /**
     * @noinspection SameParameterValue
     */
    static String cipherAndCodeText(String name, int cipherMode, byte[] pwd, SecretKey... secretKeyVal) {
        String encryptedAndEncodedText = null, decryptedText = null;
        byte[] salt;
        SecretKey secretKey;
        IvParameterSpec parameterSpec;
        try {
            salt = constantVal;
            parameterSpec = new IvParameterSpec(constantVal);
            if (secretKeyVal.length == 0) try {
                //check whether this iteration count needs to be reduced, to be tested in low end device.
                KeySpec spec = new PBEKeySpec(new String(pwd, StandardCharsets.UTF_8).toCharArray(), salt, 6300, 256);
                secretKey = new SecretKeySpec(factory2.generateSecret(spec).getEncoded(), "AES");
            } catch (InvalidKeySpecException e) {
                CEUtils.showMessage("Invalid Key Exception", 1);
                throw new RuntimeException();
            }
            else secretKey = secretKeyVal[0];
            cipher3.init(cipherMode, secretKey, parameterSpec);
            if (cipherMode == 1) {
                encryptedAndEncodedText = Base64.getUrlEncoder().encodeToString(cipher3.doFinal(name.getBytes(StandardCharsets.UTF_8)));
//                Log.i("tag1", "cipherAndCodeText: " + encryptedAndEncodedText);
            } else {
                decryptedText = new String(cipher3.doFinal(Base64.getUrlDecoder().decode(name)));
            }
        } catch (InvalidAlgorithmParameterException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            Log.i("tag1", "cipherAndCodeTextERR: ");
            e.printStackTrace();
            return null;
        }
        return cipherMode == 1 ? encryptedAndEncodedText : decryptedText;
    }
}

class CryptorEngineHandler {
    static Queue<CryptorEngineInput> processQueue;
    static Queue<CryptorEngineInput> folderNameCryptionQueue;
    static Queue<Path> rootFolders;
    static CryptorEngine[] cryptorEngines;
    static boolean runEnginesAndHandler = false;
    static int totalProcessorCores, bufferCount;

    void initiate() {
        CryptorEngineHandler.runEnginesAndHandler = true;
        processQueue = new ConcurrentLinkedQueue<>();
        folderNameCryptionQueue = new ConcurrentLinkedQueue<>();
        rootFolders = new ConcurrentLinkedQueue<>();
        if (cryptorEngines == null) initiateCryptorEngines();
        if (BufferPool.bufferPool.isEmpty()) allocateBuffers();
        startQueueProcessor();
    }

    void initiateCryptorEngines() {
        totalProcessorCores = Runtime.getRuntime().availableProcessors();
        cryptorEngines = new CryptorEngine[totalProcessorCores];
        for (int i = 0; i < cryptorEngines.length; i++) {
            cryptorEngines[i] = new CryptorEngine();
        }
    }

    void notifyAddingInputs() {
        if (!FileOperationLog.cryptorEnginesProcessing) FileOperationLog.newReport();
        FileOperationLog.cryptorEnginesProcessing = true;
        synchronized (this) {
            notify();
        }
    }

    void allocateBuffers() {
        int bufferSizeInMB = 10;
        double availableMemInHeap = CEUtils.availableHeapMem();
        double formattedVal = Math.floor(availableMemInHeap / 10) * 10d;
        //Reducing MB for other purposes (formattedVal/5)...
        formattedVal = formattedVal - (formattedVal / 5);
        int bufferAmount = (int) (formattedVal) / bufferSizeInMB;
        int maxBuffersAllowed = totalProcessorCores * 10;
        bufferCount = Math.min(bufferAmount, maxBuffersAllowed);
        BufferPool.createBuffers(bufferCount);
    }

    void startQueueProcessor() {
        boolean reAddedInputs = false;
        while (runEnginesAndHandler) {
            if (!processQueue.isEmpty()) {
                CryptorEngineInput cryptorEngineInput = processQueue.poll();
                while (true) {
                    int availableEngine = getAvailableEngine();
                    if (availableEngine != -1) {
                        CryptorEngine cryptorEngine = cryptorEngines[availableEngine];
                        cryptorEngine.configureAndRun(cryptorEngineInput);
                        break;
                    } else {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            } else {
                while (!allEnginesAvailable()) {
                    if (!processQueue.isEmpty()) {
                        reAddedInputs = true;
                        break;
                    }
                }
                if (reAddedInputs) {
                    reAddedInputs = false;
                    continue;
                }
                if (!folderNameCryptionQueue.isEmpty() || !rootFolders.isEmpty()) {
                    FileOperationLog.folderCryptionOnProgress = true;
                    doFolderNameCryption();
                    deleteRootDirectories();
                } else {
                    System.gc();
                    FileOperationLog.refresh();
                    if (!CryptorEngine.cleanupFolders.isEmpty()) cleanupErrors();
                    if (!runEnginesAndHandler) break;
                    Log.i("tag1", "_>>> <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< CRYPTOR-ENGINE ASSIGNING ON-HOLD >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                    synchronized (this) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    Log.i("tag1", "_>>> <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< CRYPTOR-ENGINE ASSIGNING STARTED >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                }
            }
        }
    }

    void doFolderNameCryption() {
        while (!folderNameCryptionQueue.isEmpty()) {
            CryptorEngineInput cryptorEngineInput = folderNameCryptionQueue.poll();
            while (true) {
                int availableEngine = getAvailableEngine();
                if (availableEngine != -1) {
                    CryptorEngine cryptorEngine = cryptorEngines[availableEngine];
                    cryptorEngine.configureAndRun(cryptorEngineInput);
                    while (!cryptorEngine.engineAvailable) {
                        try {
                            //noinspection BusyWait
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    break;
                } else {
                    try {
                        //noinspection BusyWait
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    static void deleteRootDirectories() {
        while (!rootFolders.isEmpty()) {
            Path path;
            if ((path = rootFolders.poll()) != null) {
                //noinspection ResultOfMethodCallIgnored
                path.toFile().delete();
            }
        }
    }

    int getAvailableEngine() {
        for (int i = 0; i < cryptorEngines.length; i++) {
            if (cryptorEngines[i].engineAvailable) {
                cryptorEngines[i].engineAvailable = false;
                return i;
            }
        }
        return -1;
    }

    boolean allEnginesAvailable() {
        for (CryptorEngine cryptorEngine : cryptorEngines) {
            if (!cryptorEngine.engineAvailable) {
                return false;
            }
        }
        return true;
    }

    static void stop() {
        runEnginesAndHandler = false;
        for (CryptorEngine cryptorEngine : cryptorEngines) {
            cryptorEngine.configureAndRun(null);
        }
        cryptorEngines = null;
    }

    static void cancelOperation() {
        for (CryptorEngine cryptorEngine : cryptorEngines) {
            cryptorEngine.terminateThreadsAndClearBuffers();
        }
    }

    static void cleanupErrors() {
        try {
            for (String folderPath : CryptorEngine.cleanupFolders) {
                if (folderPath != null)
                    Files.walkFileTree(Paths.get(folderPath), new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult postVisitDirectory(Path directoryPath, IOException exc) throws IOException {
                            Files.delete(directoryPath);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });
            }
        } catch (IOException ignored) {
        }
    }
}

class FileOperationLog implements Serializable {
    static int totalFiles = 0;
    static int filesProcessed = 0, filesToEncrypt = 0, filesToDecrypt = 0;
    static long totalSizeInMB = 0, processedBytes = 0;
    static boolean cryptorEnginesProcessing = false, folderCryptionOnProgress = false, makingCryptorEngineInputs = false;
    static OperationReport report = new OperationReport();

    public static void newReport() {
        report = new OperationReport();
    }

    public static void saveReport() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(MainActivity.internalFilesDir + File.separator + "FileCipherReport"); ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            objectOutputStream.writeObject(report);
        } catch (IOException e) {
            if (MainActivity.reportFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                MainActivity.reportFile.delete();
                Log.i("tag1", "saveReport: java.io.NotSerializableException");
            }
        }
    }

    public static void refresh() {
        if (totalFiles != 0) {
            report.totalFiles = totalFiles;
            report.filesProcessed = filesProcessed;
            report.filesToEncrypt = filesToEncrypt;
            report.filesToDecrypt = filesToDecrypt;
            report.status = filesProcessed == totalFiles;
            saveReport();
            MainActivity.statusUpdateRunning = false;
        }

        totalFiles = 0;
        filesProcessed = 0;
        totalSizeInMB = 0;
        processedBytes = 0;
        filesToEncrypt = 0;
        filesToDecrypt = 0;
        cryptorEnginesProcessing = false;
        folderCryptionOnProgress = false;
    }

    protected static synchronized void updateBytesProcessed(int amount) {
        processedBytes += amount;
    }

    protected static synchronized void incrementFilesProcessed() {
        filesProcessed += 1;
    }
}

class OperationReport implements Serializable {
    int totalFiles, filesProcessed, filesToEncrypt, filesToDecrypt;
    boolean status = true;
    HashMap<String, List<CryptorEngineInput>> log = new HashMap<>();

    static void logger(@NonNull HashMap<String, List<CryptorEngineInput>> log, String code, CryptorEngineInput fileInput) {
        if (log.containsKey(code)) {
            List<CryptorEngineInput> reference = log.get(code);
            if (reference != null) {
                reference.add(fileInput);
            }
        } else {
            log.put(code, Collections.synchronizedList(new ArrayList<>(Collections.singleton(fileInput))));
            //log.put(code, new ArrayList<>(Collections.singleton(fileInput)));
        }
    }

    static void loadReport() {
        try (FileInputStream fis = new FileInputStream(MainActivity.reportFile); ObjectInputStream ois = new ObjectInputStream(fis)) {
            FileOperationLog.report = (OperationReport) ois.readObject();
        } catch (Exception e) {
            Log.i("tag1", ">>>>>>>>>>>>>>>> ERROR WHILE LOADING REPORT <<<<<<<<<<<<<<<<<<<<<");
        }
    }
}

class CEFileHandler {
    //Making this method synchronized is very important.
    synchronized static File renameFile(File sourceFile, String newName) {
        File renamedFile = new File(sourceFile.getParentFile() + File.separator + newName);
        int counter = 0;
        while (true) {
            if (renamedFile.exists()) {
                renamedFile = new File(sourceFile.getParentFile() + File.separator + "(" + ++counter + ") " + newName);
            } else return sourceFile.renameTo(renamedFile) ? renamedFile : null;
        }
    }

    @Nullable
    static File getNewTempFile(String fileSavePath) {
        File tmpFile;
        int counter = 0;
        tmpFile = new File(fileSavePath + ".tmpf");
        try {
            //This method creates the folders from a given path if they are not present..
            createFolder(Objects.requireNonNull(tmpFile.getAbsoluteFile().getParentFile()).toPath());
            if (!tmpFile.createNewFile()) {
                while (true) {
                    if (tmpFile.exists()) {
                        tmpFile = new File(tmpFile.getParentFile() + File.separator + "(" + ++counter + ") " + tmpFile.getName());
                    } else return tmpFile.createNewFile() ? tmpFile : null;
                }
            }
        } catch (IOException e) {
            Log.i("tag1", "getNewTempFile: " + fileSavePath);
            throw new RuntimeException();
        }
        return tmpFile;
    }

    static void createFolder(Path folderPath) {
        try {
            Files.createDirectories(folderPath);
        } catch (IOException e) {
            CEUtils.showMessage(e.getMessage(), 1);
            throw new RuntimeException(e);
        }
    }

    @NonNull
    static String getNewDistinctFolderName(String sourceFolder) {
        File check = new File(sourceFolder);
        int counter = 0;
        while (check.exists()) {
            check = new File(sourceFolder + " (" + ++counter + ")");
        }
        return check.getName();
    }
}

class CEUtils {
    static Runtime runtime = Runtime.getRuntime();

    static long availableHeapMem() {
        //Returns available space in Heap in MB.
        return ((runtime.maxMemory() / 1048576L) - ((runtime.totalMemory() - runtime.freeMemory()) / 1048576L));
    }

    static long availableStorageSpace(String storagePath) {
        //Returns available storage space in MB.
        File file = new File(storagePath);
        long availableSizeInBytes = new StatFs(file.getPath()).getAvailableBytes();
        return (availableSizeInBytes / 1048576L);
    }

    protected static void showMessage(String msg, int mode) {
        MainActivity.toastMessage = msg;
        if (mode == 1) MainActivity.handler.post(MainActivity.runnable);
        else MainActivity.handler.post(MainActivity.runnable2);
    }
}