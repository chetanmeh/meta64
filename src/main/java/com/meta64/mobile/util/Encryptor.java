package com.meta64.mobile.util;

import java.security.Key;


import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Symmetric Encryption using AES
 * 
 * It's highly recommended that you put --aeskey=XXXXXXXXXXXXXXXX not in a text file but as a
 * command line parameter when the application is started so that a hacker has to gain access to
 * your actual launch script to see the password.
 * 
 */
@Component
@Scope("singleton")
public class Encryptor {

	@Value("${aeskey}")
	private String keyStr;

	private Key aesKey = null;
	private Cipher cipher = null;

	synchronized private void init() throws Exception {
		if (keyStr == null || keyStr.length() != 16) {
			throw new Exception("bad aes key configured");
		}
		if (aesKey == null) {
			aesKey = new SecretKeySpec(keyStr.getBytes(), "AES");
			cipher = Cipher.getInstance("AES");
		}
	}

	synchronized public String encrypt(String text) throws Exception {
		init();
		cipher.init(Cipher.ENCRYPT_MODE, aesKey);
		return DatatypeConverter.printBase64Binary(cipher.doFinal(text.getBytes()));
	}

	synchronized public String decrypt(String text) throws Exception {
		init();
		cipher.init(Cipher.DECRYPT_MODE, aesKey);
		return new String(cipher.doFinal(DatatypeConverter.parseBase64Binary(text)));
	}
}