package com.cdevworks.filescryptpro;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

//Important Note: rapidly and simultaneously(multithreading) using a same cipher instance with any single particular secretKey from android Keystore will produce errors.

public class PasswordManager {
    static File sps = new File(MainActivity.internalFilesDir + File.separator + "sps.bks");
    static KeyStore androidKeyStore, ks;
    static MessageDigest digest;
    static char[] defaultExportPassword = "filescrypt".toCharArray();

    static {
        try {
            androidKeyStore = KeyStore.getInstance("AndroidKeyStore");
            androidKeyStore.load(null);
            ks = KeyStore.getInstance("BKS");
            if (sps.exists())
                loadSPS();
            else {
                Log.i("TAG", "instance initializer: created new");
                createNew();
            }
            digest = MessageDigest.getInstance("SHA-256");
        } catch (CertificateException | KeyStoreException | IOException |
                 NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createNew() {
        Log.i("TAG", "CREATED");
        try {
            final char[] masterKey;
            if (!androidKeyStore.containsAlias("filescryptmaster")) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                keyGenerator.init(
                        new KeyGenParameterSpec.Builder("filescryptmaster", KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setKeySize(256)
                                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                .setRandomizedEncryptionRequired(false)
                                //.setUserAuthenticationRequired(true)
                                .build());
                SecretKey secretKey = keyGenerator.generateKey();
                masterKey = Objects.requireNonNull(CryptorEngine.cipherAndCodeText("filescryptmasterbaseencryptiontext", 1, null, secretKey)).toCharArray();
            } else {
                final KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) androidKeyStore
                        .getEntry("filescryptmaster", null);
                masterKey = Objects.requireNonNull(CryptorEngine.cipherAndCodeText("filescryptmasterbaseencryptiontext", 1, null, secretKeyEntry.getSecretKey())).toCharArray();
            }
            Log.i("TAG", "createNew: key1:" + Arrays.toString(masterKey));
            ks.load(null, masterKey);
            try (FileOutputStream fos = new FileOutputStream(sps)) {
                ks.store(fos, masterKey);
            }
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException |
                 NoSuchProviderException | UnrecoverableEntryException | CertificateException |
                 KeyStoreException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String aliasCache = null;

    static synchronized void insert(String alias, SecretKey secretKey) {
        if (aliasCache != null && aliasCache.equals(alias))
            return;
        char[] master = new char[0];
        try {
            if (ks.containsAlias(alias))
                return;
            else aliasCache = alias;
            master = getMaster();
            KeyStore.SecretKeyEntry secret = new KeyStore.SecretKeyEntry(secretKey);
            KeyStore.ProtectionParameter password = new KeyStore.PasswordProtection(master);
            ks.setEntry(alias, secret, password);
            try (FileOutputStream fos = new FileOutputStream(sps)) {
                ks.store(fos, master);
            }
            if (master != null) {
                Arrays.fill(master, '.');
            }
        } catch (KeyStoreException | CertificateException | IOException |
                 NoSuchAlgorithmException e) {
            if (master != null) {
                Arrays.fill(master, '.');
            }
            throw new RuntimeException(e);
        }
    }

    static ConcurrentHashMap<String, SecretKey> keys = new ConcurrentHashMap<>();

    static synchronized SecretKey getKey(String alias) {
        if (keys.containsKey(alias)) {
            return keys.get(alias);
        } else {
            char[] master = getMaster();
            try {
                SecretKey secretKey = (SecretKey) ks.getKey(alias, master);
                if (secretKey != null)
                    keys.put(alias, secretKey);
                if (master != null) {
                    Arrays.fill(master, '.');
                }
                return secretKey;
            } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
                if (master != null) {
                    Arrays.fill(master, '.');
                }
                Log.i("tag1", "getKey: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
    }

    private static char[] masterKey = null;

    synchronized private static char[] getMaster() {
        SecretKey secretKey;
        try {
            if (masterKey == null)
                if (androidKeyStore.containsAlias("filescryptmaster")) {
                    final KeyStore.SecretKeyEntry secretKeyEntry = (KeyStore.SecretKeyEntry) androidKeyStore.getEntry("filescryptmaster", null);
                    secretKey = secretKeyEntry.getSecretKey();
                    masterKey = Objects.requireNonNull(CryptorEngine.cipherAndCodeText("filescryptmasterbaseencryptiontext", 1, null, secretKey)).toCharArray();
                    return Arrays.copyOf(masterKey, masterKey.length);
                } else return null;
            else
                return Arrays.copyOf(masterKey, masterKey.length);
        } catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    static char[] masterHash;

    static char[] getAuthArray() {
        if (masterHash == null) {
            char[] master = getMaster();
            assert master != null;
            byte[] masterBytes = new byte[master.length];
            for (int i = 0; i < master.length; i++) {
                masterBytes[i] = (byte) master[i];
            }
            masterHash = new String(digest.digest(masterBytes)).toCharArray();
            Arrays.fill(master, '.');
            Arrays.fill(masterBytes, (byte) '.');
        }
        return masterHash;
    }

    static boolean authorize(char[] input) {
        return Arrays.equals(input, getAuthArray());
    }

    private static void loadSPS() {
        char[] master = getMaster();
        try (FileInputStream fis = new FileInputStream(sps)) {
            ks.load(fis, master);
            if (master != null) {
                Arrays.fill(master, '.');
            }
        } catch (CertificateException | IOException | NoSuchAlgorithmException e) {
            if (master != null) {
                Arrays.fill(master, '.');
            }
            throw new RuntimeException(e);
        }
    }

    static boolean importPM(String pathOfPM, char[] pwd) {
        try (FileInputStream fis = new FileInputStream(pathOfPM)) {
            KeyStore ks1 = KeyStore.getInstance("BKS");
            ks1.load(fis, pwd);
            Enumeration<String> aliases = ks1.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                SecretKey secretKey = (SecretKey) ks1.getKey(alias, pwd);
                insert(alias, secretKey);
            }
            return true;
        } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException |
                 UnrecoverableKeyException e) {
            return false;
        }
    }

    static ExportResult exportPM(char[] newPwd) {
        try {
            String exportFileName = "PMKeys" + com.cdevworks.filescryptpro.Utils.getDateAndTime() + ".bks";
            String newKeystorePath = MainActivity.externalAppDir.getPath() + File.separator + "Password Manager" + File.separator + exportFileName;
            Files.createDirectories(Objects.requireNonNull(new File(newKeystorePath).getParentFile()).toPath());
            String KeystorePath = sps.getPath();
            char[] KeystorePassword = getMaster();
            char[] newKeystorePassword;
            if (newPwd == null)
                newKeystorePassword = defaultExportPassword;
            else newKeystorePassword = newPwd;

            // Load the old keystore
            KeyStore oldKeystore = KeyStore.getInstance("BKS");
            FileInputStream fis = new FileInputStream(KeystorePath);
            oldKeystore.load(fis, KeystorePassword);
            fis.close();

            // Create a new keystore
            KeyStore newKeystore = KeyStore.getInstance("BKS");
            newKeystore.load(null, newKeystorePassword);

            // Iterate through the entries in the old keystore
            Enumeration<String> aliases = oldKeystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                SecretKey secretKey = (SecretKey) oldKeystore.getKey(alias, KeystorePassword);

                KeyStore.SecretKeyEntry secret = new KeyStore.SecretKeyEntry(secretKey);
                KeyStore.ProtectionParameter password = new KeyStore.PasswordProtection(newKeystorePassword);
                newKeystore.setEntry(alias, secret, password);
            }

            // Save the new keystore to a new file
            FileOutputStream fos = new FileOutputStream(newKeystorePath);
            newKeystore.store(fos, newKeystorePassword);
            fos.close();
            return new ExportResult(true, newKeystorePath);
        } catch (Exception e) {
            e.printStackTrace();
            return new ExportResult(false, null);
        }
    }
}

class ExportResult {
    boolean result;
    String filePath;

    ExportResult(boolean result, String filePath) {
        this.result = result;
        this.filePath = filePath;
    }
}
