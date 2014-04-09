package com.roadwatch.server.utils;

import java.nio.charset.Charset;

import org.apache.commons.codec.binary.Base64;

public class EncryptionUtils
{
	private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");
	
	public static String encrypt(String password)
	{
		return Base64.encodeBase64String(password.getBytes(UTF_8_CHARSET));
	}
	
	public static String decrypt(String encryptedPassword)
	{
		return new String(Base64.decodeBase64(encryptedPassword), UTF_8_CHARSET);
	}	
}