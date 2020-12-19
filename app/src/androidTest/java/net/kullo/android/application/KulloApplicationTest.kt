/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.android.application

import android.content.Context
import android.content.SharedPreferences
import android.test.AndroidTestCase

import net.kullo.libkullo.LibKullo

/*

Version 0 preferences file:

<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
  <string name="kullo_address">smith#example.com</string>
  <string name="block_a">123456</string>
  <string name="user_avatar_smith#example.com">XXXXXXXXXX==</string>
  <string name="user_name_smith#example.com">Mr. Smith</string>
  <string name="block_b">123456</string>
  <string name="block_g">123456</string>
  <string name="block_c">123456</string>
  <string name="block_j">123456</string>
  <string name="block_n">123456</string>
  <string name="block_f">123456</string>
  <string name="block_k">123456</string>
  <string name="block_h">123456</string>
  <string name="block_o">123456</string>
  <string name="user_footer_smith#example.com">MyAddress 233
777777 MyCity</string>
  <string name="block_p">123456</string>
  <string name="block_i">123456</string>
  <string name="block_l">123456</string>
  <string name="block_d">123456</string>
  <string name="block_e">123456</string>
  <string name="user_avatar_mime_type_smith#example.com">image/png</string>
  <string name="block_m">123456</string>
  <string name="user_organization_smith#example.com">Company Inc.</string>
</map>

*/
class KulloApplicationTest : AndroidTestCase() {

    private var mSp: SharedPreferences? = null

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        LibKullo.init()

        mSp = context.getSharedPreferences(TEST_PREFS, Context.MODE_PRIVATE)
        val editor = mSp!!.edit()
        editor.clear()
        editor.putString("kullo_address", "smith#example.com")
        editor.putString("block_a", "123456")
        editor.putString("block_b", "123456")
        editor.putString("block_c", "123456")
        editor.putString("block_d", "123456")
        editor.putString("block_e", "123456")
        editor.putString("block_f", "123456")
        editor.putString("block_g", "123456")
        editor.putString("block_h", "123456")
        editor.putString("block_i", "123456")
        editor.putString("block_j", "123456")
        editor.putString("block_k", "123456")
        editor.putString("block_l", "123456")
        editor.putString("block_m", "123456")
        editor.putString("block_n", "123456")
        editor.putString("block_o", "123456")
        editor.putString("block_p", "123456")
        editor.putString("user_name_smith#example.com", "Mr. Smith")
        editor.putString("user_organization_smith#example.com", "Company Inc.")
        editor.putString("user_footer_smith#example.com", "MyAddress 233\n777 MyCity")
        editor.putString("user_avatar_smith#example.com", "XXXXXXXXXX==")
        editor.putString("user_avatar_mime_type_smith#example.com", "image/png")
        editor.apply()
    }

    @Throws(Exception::class)
    fun testNoMigration() {
        assertEquals(22, mSp!!.all.size)
        assertEquals("smith#example.com", mSp!!.getString("kullo_address", ""))
        assertEquals("123456", mSp!!.getString("block_a", ""))
        assertEquals("123456", mSp!!.getString("block_p", ""))
        assertEquals("Mr. Smith", mSp!!.getString("user_name_smith#example.com", ""))
        assertEquals("Company Inc.", mSp!!.getString("user_organization_smith#example.com", ""))
        assertEquals("MyAddress 233\n777 MyCity", mSp!!.getString("user_footer_smith#example.com", ""))
        assertEquals("image/png", mSp!!.getString("user_avatar_mime_type_smith#example.com", ""))
        assertEquals("XXXXXXXXXX==", mSp!!.getString("user_avatar_smith#example.com", ""))
    }

    @Throws(Exception::class)
    fun testMigration1() {
        KulloApplication.migratePreferences(mSp!!, 1)

        assertEquals(24, mSp!!.all.size)

        // kullo_address -> active_user/latest_active_user
        assertEquals(false, mSp!!.contains("kullo_address"))
        assertEquals("smith#example.com", mSp!!.getString("active_user", ""))
        assertEquals("smith#example.com", mSp!!.getString("last_active_user", ""))

        // prefix with address
        assertEquals(false, mSp!!.contains("block_a"))
        assertEquals(false, mSp!!.contains("block_p"))
        assertEquals("123456", mSp!!.getString("smith#example.com_block_a", ""))
        assertEquals("123456", mSp!!.getString("smith#example.com_block_p", ""))

        // unchanged
        assertEquals("Mr. Smith", mSp!!.getString("user_name_smith#example.com", ""))
        assertEquals("Company Inc.", mSp!!.getString("user_organization_smith#example.com", ""))
        assertEquals("MyAddress 233\n777 MyCity", mSp!!.getString("user_footer_smith#example.com", ""))
        assertEquals("image/png", mSp!!.getString("user_avatar_mime_type_smith#example.com", ""))
        assertEquals("XXXXXXXXXX==", mSp!!.getString("user_avatar_smith#example.com", ""))

        // version set
        assertEquals(1, mSp!!.getInt("net.kullo.android.SETTINGS_VERSION", -1))
    }

    @Throws(Exception::class)
    fun testMigration2() {
        KulloApplication.migratePreferences(mSp!!, 2)

        assertEquals(24, mSp!!.all.size)

        // unchanged
        assertEquals("smith#example.com", mSp!!.getString("active_user", ""))
        assertEquals("smith#example.com", mSp!!.getString("last_active_user", ""))

        // Changed separator
        assertEquals("123456", mSp!!.getString("smith#example.com|block_a", ""))
        assertEquals("123456", mSp!!.getString("smith#example.com|block_p", ""))
        assertEquals("Mr. Smith", mSp!!.getString("user_name|smith#example.com", ""))
        assertEquals("Company Inc.", mSp!!.getString("user_organization|smith#example.com", ""))
        assertEquals("MyAddress 233\n777 MyCity", mSp!!.getString("user_footer|smith#example.com", ""))
        assertEquals("image/png", mSp!!.getString("user_avatar_mime_type|smith#example.com", ""))
        assertEquals("XXXXXXXXXX==", mSp!!.getString("user_avatar|smith#example.com", ""))

        // version set
        assertEquals(2, mSp!!.getInt("net.kullo.android.SETTINGS_VERSION", -1))
    }

    @Throws(Exception::class)
    fun testMigration3() {
        KulloApplication.migratePreferences(mSp!!, 3)

        assertEquals(24, mSp!!.all.size)

        // unchanged
        assertEquals("smith#example.com", mSp!!.getString("active_user", ""))
        assertEquals("smith#example.com", mSp!!.getString("last_active_user", ""))
        assertEquals("123456", mSp!!.getString("smith#example.com|block_a", ""))
        assertEquals("123456", mSp!!.getString("smith#example.com|block_p", ""))

        // Move address from suffix to prefix
        assertEquals("Mr. Smith", mSp!!.getString("smith#example.com|user_name", ""))
        assertEquals("Company Inc.", mSp!!.getString("smith#example.com|user_organization", ""))
        assertEquals("MyAddress 233\n777 MyCity", mSp!!.getString("smith#example.com|user_footer", ""))
        assertEquals("image/png", mSp!!.getString("smith#example.com|user_avatar_mime_type", ""))
        assertEquals("XXXXXXXXXX==", mSp!!.getString("smith#example.com|user_avatar", ""))

        // version set
        assertEquals(3, mSp!!.getInt("net.kullo.android.SETTINGS_VERSION", -1))
    }

    @Throws(Exception::class)
    fun testMigration4() {
        KulloApplication.migratePreferences(mSp!!, 4)

        // 5 deleted
        assertEquals(24-5, mSp!!.all.size)

        // unchanged
        assertEquals("smith#example.com", mSp!!.getString("active_user", ""))
        assertEquals("smith#example.com", mSp!!.getString("last_active_user", ""))
        assertEquals("123456", mSp!!.getString("smith#example.com|block_a", ""))
        assertEquals("123456", mSp!!.getString("smith#example.com|block_p", ""))

        // Remove profile information that are in libkullo database now
        assertEquals(null, mSp!!.getString("smith#example.com|user_name", null))
        assertEquals(null, mSp!!.getString("smith#example.com|user_organization", null))
        assertEquals(null, mSp!!.getString("smith#example.com|user_footer", null))
        assertEquals(null, mSp!!.getString("smith#example.com|user_avatar_mime_type", null))
        assertEquals(null, mSp!!.getString("smith#example.com|user_avatar", null))

        // version set
        assertEquals(4, mSp!!.getInt("net.kullo.android.SETTINGS_VERSION", -1))
    }

    companion object {
        private val TEST_PREFS = "net.kullo.android.tests.SETTINGS"
    }
}
