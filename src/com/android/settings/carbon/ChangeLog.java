/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.carbon;

import com.android.settings.R;

import android.os.Bundle;
import android.app.Fragment;
import android.content.Intent;
import android.content.ComponentName;
import android.content.Context;

public class ChangeLog extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    Intent intent = new Intent(Intent.ACTION_MAIN);
	    intent.setComponent(new ComponentName("com.helicopter88.changelog","com.helicopter88.changelog.MainActivity"));
	    startActivity(intent);
	}
}
