package mts.build.firebasedatabase;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.apache.commons.ssl.OpenSSL;

import java.io.IOException;
import java.security.GeneralSecurityException;

import mts.build.firebasedatabase.R;

public class Prefs {

    private static final String AES_ALGORITHM = "aes256";
    private static final char[] SECRET_KEY = "mtscrypt".toCharArray();

    private final Context context;
    private final SharedPreferences sp;
    private final SharedPreferences.Editor editor;

    public Prefs(Context context) {
        this.context = context;
        sp = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        editor = sp.edit();
    }

    /**
     * @param cipherText encrypted text
     * @return decrypted text
     */
    public static String decrypt(String cipherText) {
        String decryptedText;
        try {
            byte[] encryptedData = Base64.decode(cipherText, Base64.DEFAULT);
            byte[] _decryptedData = OpenSSL.decrypt(AES_ALGORITHM, SECRET_KEY, encryptedData);
            decryptedText = new String(_decryptedData);
        } catch (IOException e) {
            decryptedText = cipherText;
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            decryptedText = cipherText;
            e.printStackTrace();
        } catch (RuntimeException e) {
            decryptedText = cipherText;
            e.printStackTrace();
        }
        return decryptedText;
    }

    public static String encrypt(String text) {
        String encrypted;
        try {
            byte[] encryptedData = OpenSSL.encrypt(AES_ALGORITHM, SECRET_KEY, text.getBytes("UTF8"), false);
            encrypted = Base64.encodeToString(encryptedData, Base64.DEFAULT);
        } catch (IOException e) {
            encrypted = text;
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            encrypted = text;
            e.printStackTrace();
        } catch (RuntimeException e) {
            encrypted = text;
            e.printStackTrace();
        }
        return encrypted;
    }

}
