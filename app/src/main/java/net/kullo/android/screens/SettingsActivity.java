/* Copyright 2015-2016 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import net.kullo.android.R;
import net.kullo.android.kulloapi.CreateSessionResult;
import net.kullo.android.kulloapi.CreateSessionState;
import net.kullo.android.kulloapi.SessionConnector;
import net.kullo.android.littlehelpers.AvatarUtils;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.notifications.GcmConnector;
import net.kullo.javautils.RuntimeAssertion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    public static final String TAG = "SettingsActivity";

    private static final int REQUEST_CODE_SELECT_AVATAR_IMAGE = 1;
    private static final int REQUEST_CODE_CROP_AVATAR_IMAGE = 2;
    private static final String ACTION_CLEAR_AVATAR = "ClearAvatar";

    private TextInputLayout mName;
    private TextInputLayout mOrganization;
    private TextInputLayout mFooter;
    private EditText mNameEditText;
    private EditText mOrganizationEditText;
    private EditText mFooterEditText;
    private ImageView mAvatarView;

    private Uri mCameraOutputFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (detectClearAvatarIntent()) return;

        final CreateSessionResult result = SessionConnector.get().createActivityWithSession(this);
        if (result.state == CreateSessionState.NO_CREDENTIALS) return;

        setContentView(R.layout.activity_settings);

        Ui.prepareActivityForTaskManager(this);
        Ui.setupActionbar(this);
        Ui.setColorStatusBarArrangeHeader(this);

        setupLayout();

        if (result.state == CreateSessionState.CREATING) {
            RuntimeAssertion.require(result.task != null);
            result.task.waitUntilDone();
        }

        GcmConnector.get().fetchAndRegisterToken(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateTextInputViewsFromSession();
        updateAvatarViewFromSession();
        showErrorUsernameIfEmpty();
    }

    @Override
    protected void onResume() {
        super.onResume();
        GcmConnector.get().removeAllNotifications(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Remove Bitmap to save memory
        mAvatarView.setImageBitmap(null);
    }

    @Override
    protected void onPause() {
        super.onPause();

        SessionConnector.get().setClientName(mNameEditText.getText().toString().trim());
        SessionConnector.get().setClientOrganization(mOrganizationEditText.getText().toString().trim());
        SessionConnector.get().setClientFooter(mFooterEditText.getText().toString().trim());

        // Upload potential changes to UserSettings
        SessionConnector.get().syncKullo();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: // back button
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setupLayout() {
        mName = (TextInputLayout) findViewById(R.id.edit_kullo_name);
        mOrganization = (TextInputLayout) findViewById(R.id.edit_kullo_organization);
        mFooter = (TextInputLayout) findViewById(R.id.edit_kullo_footer);
        mNameEditText = mName.getEditText();
        mOrganizationEditText = mOrganization.getEditText();
        mFooterEditText = mFooter.getEditText();
        RuntimeAssertion.require(mNameEditText != null);
        RuntimeAssertion.require(mOrganizationEditText != null);
        RuntimeAssertion.require(mFooterEditText != null);

        mNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                showErrorUsernameIfEmpty();
            }
        });

        mAvatarView = (ImageView) findViewById(R.id.settings_avatar_view);
        mAvatarView.setOnClickListener(new View.OnClickListener() {
            //@Override
            public void onClick(View v) {
                openSelectAvatarSourcesMenu();
            }
        });
    }

    private void showErrorUsernameIfEmpty() {
        if (mNameEditText.getText().toString().isEmpty()) {
            mName.setError(this.getResources().getString(R.string.settings_name_empty_error));
            mName.setErrorEnabled(true);
        } else {
            mName.setError(null);
            mName.setErrorEnabled(false);
        }
    }

    private void openSelectAvatarSourcesMenu() {
        // Determine Uri of camera image to save
        final File cameraOutputDir = getExternalFilesDir(null);
        final File cameraOutputFile = new File(cameraOutputDir, "cameraimg_" + System.currentTimeMillis() + ".jpg");
        mCameraOutputFileUri = Uri.fromFile(cameraOutputFile);
        Log.d(TAG, "Camera output file: " + mCameraOutputFileUri);

        // Option list
        final List<Intent> extraIntents = new ArrayList<>();

        // Clear
        final Intent clearIntent = new Intent();
        clearIntent.setClass(this, SettingsActivity.class);
        clearIntent.setAction(ACTION_CLEAR_AVATAR);
        final LabeledIntent clearIntentOption = new LabeledIntent(clearIntent, getPackageName(),
            getString(R.string.settings_remove_avatar), R.drawable.kullo_settings_avatar);
        extraIntents.add(clearIntentOption);

        // Camera
        final Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = this.getPackageManager();
        final List<ResolveInfo> cameras = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo camera : cameras) {
            final String packageName = camera.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(camera.activityInfo.packageName, camera.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraOutputFileUri);
            extraIntents.add(intent);
        }

        // FileSystem
        final Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/");

        // Chooser of filesystem options
        final Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.select_image_source));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toArray(new Parcelable[]{}));

        // Launch the option menu
        startActivityForResult(chooserIntent, REQUEST_CODE_SELECT_AVATAR_IMAGE);
    }

    private boolean detectClearAvatarIntent() {
        Intent inputIntent = getIntent();
        if (inputIntent == null)
            return false;

        String action = inputIntent.getAction();
        if (action != null && action.equals(ACTION_CLEAR_AVATAR)) {
            setResult(Activity.RESULT_OK, inputIntent);
            finish();
            return true;
        }

        return false;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SELECT_AVATAR_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                // identify action
                final boolean isCamera;
                boolean isDelete = false;
                if (data == null) {
                    isCamera = true;
                } else {
                    final String action = data.getAction();
                    if (action == null) {
                        isCamera = false;
                    } else {
                        isCamera = action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
                        isDelete = action.equals(ACTION_CLEAR_AVATAR);
                    }
                }

                if (isDelete) {
                    // remove avatar
                    clearStoredSessionAvatar();
                    updateAvatarViewFromSession();
                } else {
                    // launch crop activity
                    final Intent cropIntent = new Intent(this, CropImageActivity.class);
                    if (isCamera) {
                        cropIntent.putExtra(CropImageActivity.INPUT_METHOD, CropImageActivity.CAMERA_INPUT);
                        cropIntent.putExtra(CropImageActivity.BITMAP_INPUT_URI, mCameraOutputFileUri.toString());
                    } else {
                        Uri selectedImageUri = data.getData();
                        cropIntent.putExtra(CropImageActivity.INPUT_METHOD, CropImageActivity.FILE_INPUT);
                        cropIntent.putExtra(CropImageActivity.BITMAP_INPUT_URI, selectedImageUri.toString());
                    }

                    startActivityForResult(cropIntent, REQUEST_CODE_CROP_AVATAR_IMAGE);
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, R.string.capture_image_canceled, Toast.LENGTH_SHORT).show();
            } else {
                RuntimeAssertion.fail("Unhandled result code");
            }
        } else if (requestCode == REQUEST_CODE_CROP_AVATAR_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                updateAvatarViewFromSession();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, R.string.capture_image_canceled, Toast.LENGTH_SHORT).show();
            } else {
                RuntimeAssertion.fail("Unhandled result code");
            }
        } else {
            RuntimeAssertion.fail("Unhandled request code");
        }
    }

    private void updateTextInputViewsFromSession() {
        mNameEditText.setText(SessionConnector.get().getClientName());
        mOrganizationEditText.setText(SessionConnector.get().getClientOrganization());
        mFooterEditText.setText(SessionConnector.get().getClientFooter());
    }

    private void clearStoredSessionAvatar() {
        SessionConnector.get().setClientAvatar(new byte[0]);
        SessionConnector.get().setClientAvatarMimeType("");
    }

    private void updateAvatarViewFromSession() {
        byte[] avatar = SessionConnector.get().getClientAvatar();
        if (avatar != null && avatar.length != 0) {
            mAvatarView.setImageBitmap(AvatarUtils.avatarToBitmap(avatar));
        } else {
            mAvatarView.setImageResource(R.drawable.kullo_settings_avatar);
        }
    }
}
