/*
 * Copyright 2015–2018 Kullo GmbH
 *
 * This source code is licensed under the 3-clause BSD license. See LICENSE.txt
 * in the root directory of this source tree for details.
 */
package net.kullo.android.screens.registration;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import net.kullo.android.R;
import net.kullo.android.application.KulloApplication;
import net.kullo.android.screens.RegistrationActivity;
import net.kullo.android.storage.AppPreferences;

public class EnterAccountInfoFragment extends Fragment {

    @SuppressWarnings("unused")
    private static final String TAG = EnterAccountInfoFragment.class.getSimpleName();
    private EditText mNameEdit;
    private EditText mOrganizationEdit;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_registration_enter_account_info, container, false);

        mNameEdit =  (EditText) view.findViewById(R.id.edit_kullo_name_text);
        mOrganizationEdit = (EditText) view.findViewById(R.id.edit_kullo_organization_text);

        Button finishButton = (Button)view.findViewById(R.id.button_finish);
        if (finishButton != null)
            finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // always accept
                storeData();
                openNextView();
            }
        });

        return view;
    }

    public void storeData() {
        String name = mNameEdit.getText().toString().trim();
        String organization = mOrganizationEdit.getText().toString().trim();

        if (!name.isEmpty()) {
            KulloApplication.sharedInstance.preferences.set(AppPreferences.AppScopeKey.NEW_USER_NAME, name);
        }
        if (!organization.isEmpty()) {
            KulloApplication.sharedInstance.preferences.set(AppPreferences.AppScopeKey.NEW_USER_ORGANIZATION, organization);
        }
    }

    public void openNextView() {
        ((RegistrationActivity)getActivity()).nextFragment();
    }


}
