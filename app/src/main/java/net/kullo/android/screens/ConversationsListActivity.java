/* Copyright 2015 Kullo GmbH. All rights reserved. */
package net.kullo.android.screens;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;

import net.kullo.android.R;
import net.kullo.android.application.LogoutIntent;
import net.kullo.android.kulloapi.KulloConnector;
import net.kullo.android.littlehelpers.AvatarUtils;
import net.kullo.android.littlehelpers.KulloConstants;
import net.kullo.android.littlehelpers.Ui;
import net.kullo.android.screens.conversationslist.ConversationsFragment;
import net.kullo.javautils.RuntimeAssertion;
import net.kullo.libkullo.api.AsyncTask;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConversationsListActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "ConversationsListActivity";
    private Toolbar mToolbar;
    private NavigationView mNavigationView;
    private View mNavigationHeaderView;
    private CircleImageView mNavigationHeaderAvatarView;
    private TextView mNavigationHeaderNameView;
    private TextView mNavigationHeaderAddressView;
    private DrawerLayout mDrawerLayout;
    private Boolean mUserLearnedDrawer = false;
    private MaterialDialog mConfirmLogoutDialog;
    private MenuItem mPreviousMenuItemNavigationView;

    //LIFECYCLE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AsyncTask task = KulloConnector.get().createActivityWithSession(this);

        setContentView(R.layout.activity_conversations_list);
        mToolbar = Ui.setupActionbar(this, false);
        setupLayout();

        if (savedInstanceState == null) {
            // Activity is created the first time (rotate to create multiple times)
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.contentframe, new ConversationsFragment());
            fragmentTransaction.commit();
        }

        if (task != null) task.waitUntilDone();
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateAccountInfoInNavigationHeader();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Throw away avatar bitmap in order to save memory. This is reloaded
        // in onStart anyway.
        mNavigationHeaderAvatarView.setImageBitmap(null);
    }

    public void startConversationButtonClicked(View view) {
        startActivity(new Intent(this, StartConversationActivity.class));
    }

    private void setupLayout() {
        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        RuntimeAssertion.require(mNavigationView != null);
        mNavigationView.setNavigationItemSelectedListener(this);

        mNavigationHeaderView = mNavigationView.inflateHeaderView(R.layout.navigation_view_header);
        RuntimeAssertion.require(mNavigationHeaderView != null);

        mNavigationHeaderAvatarView = (CircleImageView) mNavigationHeaderView.findViewById(R.id.avatar);
        mNavigationHeaderNameView = (TextView) mNavigationHeaderView.findViewById(R.id.username);
        mNavigationHeaderAddressView = (TextView) mNavigationHeaderView.findViewById(R.id.address);
        RuntimeAssertion.require(mNavigationHeaderAvatarView != null);
        RuntimeAssertion.require(mNavigationHeaderNameView != null);
        RuntimeAssertion.require(mNavigationHeaderAddressView != null);

        // Initializing Drawer Layout and ActionBarToggle
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.open_drawer, R.string.close_drawer) {
            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open
                updateAccountInfoInNavigationHeader();
                super.onDrawerOpened(drawerView);
            }
        };

        // Setting the actionbarToggle to drawer layout
        mDrawerLayout.setDrawerListener(actionBarDrawerToggle);

        // calling sync state to show menu icon
        actionBarDrawerToggle.syncState();

        if (!mUserLearnedDrawer) {
            mDrawerLayout.openDrawer(GravityCompat.START);
            mUserLearnedDrawer = true;
        }

        // Select conversations menu item
        mNavigationView.getMenu().getItem(0).setCheckable(true);
        mNavigationView.getMenu().getItem(0).setChecked(true);
        mPreviousMenuItemNavigationView = mNavigationView.getMenu().getItem(0);
    }

    private void updateAccountInfoInNavigationHeader() {
        mNavigationHeaderNameView.setText(KulloConnector.get().getClientName());
        mNavigationHeaderAddressView.setText(KulloConnector.get().getClientAddressAsString());

        byte[] avatar = KulloConnector.get().getClientAvatar();
        if (avatar != null && avatar.length > 0) {
            mNavigationHeaderAvatarView.setImageBitmap(AvatarUtils.avatarToBitmap(avatar));
            mNavigationHeaderAvatarView.setVisibility(View.VISIBLE);
        } else {
            mNavigationHeaderAvatarView.setVisibility(View.INVISIBLE);
        }
    }

    // This method will get triggered on item click in navigation menu
    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        menuItem.setCheckable(true);
        menuItem.setChecked(true);

        // Check to see which item was being clicked and perform appropriate action
        Fragment fragment = null;
        switch (menuItem.getItemId()) {
            case R.id.menuitem_conversations:
                fragment = getSupportFragmentManager().findFragmentByTag(ConversationsFragment.TAG);
                if (fragment == null || !(fragment instanceof ConversationsFragment)) {
                    fragment = new ConversationsFragment();
                }
                break;
            case R.id.menuitem_settings:
                Intent intentSettings = new Intent(this, SettingsActivity.class);
                startActivity(intentSettings);
                break;
            case R.id.menuitem_account:
                Intent intentAccount = new Intent(this, AccountActivity.class);
                startActivity(intentAccount);
                break;
            case R.id.menuitem_feedback:
                Intent intent = new Intent(this, ComposeActivity.class);
                intent.putExtra(KulloConstants.CONVERSATION_RECIPIENT, "hi#kullo.net");
                startActivity(intent);
                break;
            case R.id.menuitem_about:
                Intent intentAbout = new Intent(this, AboutActivity.class);
                startActivity(intentAbout);
                break;
            case R.id.menuitem_logout:
                mConfirmLogoutDialog = new MaterialDialog.Builder(this)
                        .title(R.string.logout_warning_header)
                        .content(R.string.logout_warning)
                        .positiveText(R.string.logout_warning_button_positive)
                        .negativeText(R.string.logout_warning_button_negative)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                forceLogout();
                            }

                            @Override
                            public void onNegative(MaterialDialog dialog) {
                                mConfirmLogoutDialog.dismiss();
                            }
                        })
                        .show();
                return true;
            default:
                RuntimeAssertion.require(false);
        }

        // deselect previous menu item
        if (mPreviousMenuItemNavigationView != null) {
            mPreviousMenuItemNavigationView.setChecked(false);
            mNavigationView.invalidate();
        }

        // remember currently selected menu item to deselect it later - workaround design library issue
        mPreviousMenuItemNavigationView = menuItem;

        // Closing drawer on item click
        mDrawerLayout.closeDrawers();

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (fragment != null) {
            fragmentTransaction.replace(R.id.contentframe, fragment);
            fragmentTransaction.commit();
        }
        return true;
    }

    public void forceLogout() {
        Intent intent = new LogoutIntent(this);
        startActivity(intent);
        finish();
    }
}
