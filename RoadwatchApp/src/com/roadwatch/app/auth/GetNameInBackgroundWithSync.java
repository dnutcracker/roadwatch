/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.roadwatch.app.auth;

import java.io.IOException;

import android.app.Activity;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableNotifiedException;

/**
 * This example shows how to fetch tokens if you are creating a sync adapter.
 */
public class GetNameInBackgroundWithSync extends AbstractGetNameTask
{

	public static final String CONTACTS_AUTHORITY = "com.android.contacts";

	public GetNameInBackgroundWithSync(Activity activity, String email, String scope, int requestCode)
	{
		super(activity, email);
	}

	@Override
	protected String fetchToken() throws IOException
	{
		try
		{
			return GoogleAuthUtil.getTokenWithNotification(context, mEmail, SCOPE, null, CONTACTS_AUTHORITY, null);
		}
		catch (UserRecoverableNotifiedException userRecoverableException)
		{
			// Unable to authenticate, but the user can fix this.
			onError("Could not fetch token.", null);
		}
		catch (GoogleAuthException fatalException)
		{
			onError("Unrecoverable error " + fatalException.getMessage(), fatalException);
		}
		return null;
	}
}
