/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens.registration;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.view.ViewGroup;

import net.kullo.android.R;
import net.kullo.android.screens.RegistrationActivity;
import net.kullo.android.littlehelpers.KulloConstants;

public class EnterAccountInfoFragment extends Fragment {

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
        String nameString = mNameEdit.getText().toString();
        String organizationString = mOrganizationEdit.getText().toString();

        SharedPreferences sharedPref = getActivity().getApplicationContext().getSharedPreferences(
            KulloConstants.ACCOUNT_PREFS_PLAIN, Context.MODE_PRIVATE);

        String address = sharedPref.getString(KulloConstants.KULLO_ADDRESS, "");
        if (!address.isEmpty()) {
            SharedPreferences.Editor editor = sharedPref.edit();
            final String KEY_NAME = "user_name_" + address;
            final String KEY_ORGANIZATION     = "user_organization_" + address;

            if (!nameString.isEmpty()) editor.putString(KEY_NAME, nameString);
            if (!organizationString.isEmpty()) editor.putString(KEY_ORGANIZATION, organizationString);
            editor.commit();
        }
    }


    public void openNextView() {
        ((RegistrationActivity)getActivity()).nextFragment();
    }


}
