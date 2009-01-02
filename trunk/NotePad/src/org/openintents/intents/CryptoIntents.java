/* 
 * Copyright 2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openintents.intents;

// Keeping version information in this file is important, because it has to be
// copied between various projects and repositories manually.
//
// Version: 2009 Jan 2: added EXTRA_TEXT_ARRAY

/**
 * @author Isaac Potoczny-Jones
 *
 */
public class CryptoIntents {

	/**
	 * Activity Action: Encrypt all strings given in the extra(s) EXTRA_TEXT or
	 * EXTRA_TEXT_ARRAY.
	 * Returns all encrypted string in the same extra(s).
	 * 
	 * <p>Constant Value: "org.openintents.action.ENCRYPT"</p>
	 */
	public static final String ACTION_ENCRYPT = "org.openintents.action.ENCRYPT";

	/**
	 * Activity Action: Decrypt all strings given in the extra TEXT or
	 * EXTRA_TEXT_ARRAY.
	 * Returns all decrypted string in the same extra(s).
	 * 
	 * <p>Constant Value: "org.openintents.action.DECRYPT"</p>
	 */
	public static final String ACTION_DECRYPT = "org.openintents.action.DECRYPT";
	
	/**
	 * Activity Action: Get the password corresponding to the category of the
	 * calling application, and the EXTRA_DESCRIPTION, as provided.
	 * Returns the decrypted username & password in the extras EXTRA_USERNAME and
	 * EXTRA_PASSWORD. CATEGORY is an optional parameter.
	 * 
	 * <p>Constant Value: "org.openintents.action.GET_PASSWORD"</p>
	 */
	public static final String ACTION_GET_PASSWORD = "org.openintents.action.GET_PASSWORD";
	
	/**
	 * Activity Action: Set the password corresponding to the category of the
	 * calling application, and the EXTRA_DESCRIPTION, EXTRA_USERNAME and
	 * EXTRA_PASSWORD as provided. CATEGORY is an optional parameter.
	 * 
	 * If both username and password are the non-null empty string, delete this
	 * password entry.
	 * <p>Constant Value: "org.openintents.action.SET_PASSWORD"</p>
	 */
	public static final String ACTION_SET_PASSWORD = "org.openintents.action.SET_PASSWORD";
	
	/**
	 * The text to encrypt or decrypt, or the location for the return result.
	 * 
	 * <p>Constant Value: "org.openintents.extra.TEXT"</p>
	 */
	public static final String EXTRA_TEXT = "org.openintents.extra.TEXT";
	
	/**
	 * An array of text to encrypt or decrypt, or the location for the return result.
	 * Use this to encrypt several strings at once.
	 * 
	 * <p>Constant Value: "org.openintents.extra.TEXT_ARRAY"</p>
	 */
	public static final String EXTRA_TEXT_ARRAY = "org.openintents.extra.TEXT_ARRAY";
	
	/**
	 * Required input parameter to GET_PASSWORD and SET_PASSWORD. Corresponds to the "description"
	 * field in passwordsafe. Should be a descriptive name for the password you're using,
	 * and will already be specific to your application.
	 * 
	 * <p>Constant Value: "org.openintents.extra.DESCRIPTION"</p>
	 */
	public static final String EXTRA_DESCRIPTION = "org.openintents.extra.DESCRIPTION";
	
	/**
	 * Optional input parameter to GET_PASSWORD and SET_PASSWORD. Corresponds to the "category"
	 * field in passwordsafe. If null, will be assumed to be the fully-qualified package
	 * name of your application. If non-null (and not == the package name) the user should
	 * be asked whether it's permissable to get or set this password.
	 * 
	 * <p>Constant Value: "org.openintents.extra.CATEGORY"</p>
	 */
	public static final String EXTRA_CATEGORY = "org.openintents.extra.CATEGORY";

	/**
	 * Output parameter from GET_PASSWORD and optional input parameter to SET_PASSWORD.
	 * Corresponds to the decrypted "username" field in passwordsafe.
	 * 
	 * <p>Constant Value: "org.openintents.extra.USERNAME"</p>
	 */
	public static final String EXTRA_USERNAME = "org.openintents.extra.USERNAME";
	
	/**
	 * Output parameter from GET_PASSWORD and _required_ input parameter to SET_PASSWORD.
	 * Corresponds to the decrypted "password" field in passwordsafe.
	 * 
	 * <p>Constant Value: "org.openintents.extra.PASSWORD"</p>
	 */
	public static final String EXTRA_PASSWORD = "org.openintents.extra.PASSWORD";
}
