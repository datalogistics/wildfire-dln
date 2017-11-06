
package com.bbn.roger.encryption;

// This may need to be changed to another Base64 library to work outside of Android
import android.util.Base64;

import com.atakmap.coremap.log.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.SimpleTimeZone;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A utility class that performs encryption and decryption.
 * Its methods assume that the symmetric key has already been agreed upon. When I say "symmetric key"
 * in this context, I am referring to some plaintext value, like "1234", that will be converted into
 * a proper AES key via the encodeKey() method.
 * Encryption algorithm: AES
 * Block cipher mode: CBC (Cipher block chaining)
 * Padding mode: PKCS5
 * Initial vector (IV): produced by a SHA-1 pseudo-random number generator.
 *  I've read that this essentially uses the SHA-1 hash, a seed, and a counter. It's considered
 *  secure and fast, but may not be as secure as using the dev/urandom.
 *  One advantage of using an initial vector is that the same message will be encrypted differently each time,
 *  assuming the initial vectors are unique.
 */
// Source: http://stackoverflow.com/questions/4551263/how-can-i-convert-a-string-to-a-secretkey/8828196#8828196
public class EncryptionUtils {

    public static final String TAG = EncryptionUtils.class.getSimpleName();
    public static final String cipherOptions = "AES/CBC/PKCS5Padding";
    public static final String symKeyAlgorithm = "AES";
    public static final String hashAlgorithm = "SHA-1";
    public static final String prng = "SHA1PRNG";
    public static final int KEY_SIZE = 256;

    public static byte[] encrypt(byte[] symKeyData, byte[] encodedMessage)
            throws Exception {
        final Cipher cipher = Cipher.getInstance(cipherOptions);
        final int blockSize = cipher.getBlockSize();

        // create the key
        SecretKeySpec symKey = new SecretKeySpec(symKeyData, symKeyAlgorithm);

        // generate random IV
        final byte[] ivData = setInitialVector(blockSize);
        final IvParameterSpec iv = new IvParameterSpec(ivData);

        cipher.init(Cipher.ENCRYPT_MODE, symKey, iv);

        final byte[] encryptedMessage = cipher.doFinal(encodedMessage);

        // concatenate IV and encrypted message
        final byte[] ivAndEncryptedMessage = new byte[blockSize
                + encryptedMessage.length];
        System.arraycopy(ivData, 0, ivAndEncryptedMessage, 0, blockSize);
        System.arraycopy(encryptedMessage, 0, ivAndEncryptedMessage,
                blockSize, encryptedMessage.length);

        return ivAndEncryptedMessage;
    }

    public static byte[] decrypt(byte[] symKeyData, byte[] ivAndEncryptedMessage) throws Exception {
        final Cipher cipher = Cipher.getInstance(cipherOptions);
        final int blockSize = cipher.getBlockSize();

        // create the key
        SecretKeySpec symKey = symKey = new SecretKeySpec(symKeyData,
                symKeyAlgorithm);

        // retrieve random IV from start of the received message
        final byte[] ivData = new byte[blockSize];
        System.arraycopy(ivAndEncryptedMessage, 0, ivData, 0, blockSize);
        final IvParameterSpec iv = new IvParameterSpec(ivData);

        // retrieve the encrypted message itself
        final byte[] encryptedMessage = new byte[ivAndEncryptedMessage.length
                - blockSize];
        System.arraycopy(ivAndEncryptedMessage, blockSize,
                encryptedMessage, 0, encryptedMessage.length);

        cipher.init(Cipher.DECRYPT_MODE, symKey, iv);

        byte[] output = cipher.doFinal(encryptedMessage);
        return output;
    }

    private static byte[] setInitialVector(int blockSize)
            throws NoSuchAlgorithmException {
        final byte[] ivData = new byte[blockSize];
        final SecureRandom rnd = SecureRandom.getInstance(prng);
        rnd.nextBytes(ivData);
        return ivData;
    }


    public static void generateKey(String filename) throws IOException {
        FileOutputStream atakFos = null;
        try {
            System.out.println("*** Generating new AES key ***");

            // Generate new AES key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(KEY_SIZE);
            SecretKey key = keyGen.generateKey();
            byte[] keyAsByteArray = key.getEncoded();

            // Write key to shared file
            String atakFilename = filename == null ? generateNewKeyFilename() : filename;
            atakFos = new FileOutputStream(atakFilename);
            writeKeyToFile(keyAsByteArray, atakFos);
            System.out.println("Creating new shared key file: " + atakFilename);

            // Display success message
            System.out.println("New key saved");
        } catch (Exception e) {
            System.err.println("Key generation failed");
        } finally {
            if (atakFos != null) {
                atakFos.close();
            }
        }
    }

    /**
     * Makes a file with the base64-encoded key
     * @param keyAsBytes
     * @param atakFos
     * @throws Exception
     */
    public static void writeKeyToFile(byte[] keyAsBytes, FileOutputStream atakFos) throws Exception {
        // Get the base64-encoded symmetric key
        String key = new String(
                Base64.encode(keyAsBytes, Base64.DEFAULT));

        // Generate content
        String content = key;

        // Write everything to file intended for atak device
        atakFos.write(content.getBytes());
        atakFos.flush();
        atakFos.close();
    }

    public static String generateNewKeyFilename() {
        long timeInMillis = System.currentTimeMillis();
        Date datetime = new Date(timeInMillis);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
        dateFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
        return "gotenna" + dateFormat.format(datetime) + ".key";
    }

    /**
     * Reads a key file written with @see writeKeyToFile
     * @param keyFileName
     * @return base64 decoded key
     * @throws IOException
     */
    public static byte[] readKeyFromFile(String keyFileName) throws IOException {
        FileInputStream fis = new FileInputStream(AndroidEncryptionUtils.KEY_LOCATION_ON_SDCARD + "/" + keyFileName);
        byte[] buffer = new byte[1024];
        int bytesRead = fis.read(buffer);
        Log.d(TAG, "read " + bytesRead + " bytes from " + keyFileName);
        byte[] rightSize = Arrays.copyOf(buffer, bytesRead);
        return Base64.decode(rightSize, Base64.DEFAULT);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        String command = args[0];
        if(command.equals("generate")) {
            try {
                generateKey(args.length > 1 ? args[1] : null);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else if (command.equals("encrypt")) {
            try {
                FileOutputStream fos = new FileOutputStream("encryptedMessage");
                writeKeyToFile(
                        encrypt(
                                Base64.decode(args[1], Base64.DEFAULT), args[2].getBytes()), fos);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (command.equals("decrypt")) {
            try {
                System.out.println(
                        new String(decrypt(
                                Base64.decode(args[1], Base64.DEFAULT),
                                Base64.decode(args[2], Base64.DEFAULT))));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Unknown command");
        }
    }

}
