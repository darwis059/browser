/*
    This file is part of the browser WebApp.

    browser WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    browser WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */
package de.baumann.browser.unit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.text.Html
import android.text.SpannableString
import android.text.util.Linkify
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.baumann.browser.Ninja.R
import de.baumann.browser.view.dialog.DialogManager
import java.text.SimpleDateFormat
import java.util.*


object HelperUnit {
    private const val REQUEST_CODE_ASK_PERMISSIONS = 123
    private const val REQUEST_CODE_ASK_PERMISSIONS_1 = 1234

    @JvmStatic
    // return true if need permissions
    fun needGrantStoragePermission(activity: Activity): Boolean {
        @SuppressLint("NewApi")
        if (Build.VERSION.SDK_INT in 23..28) {
            val hasWriteExternalStoragePermission = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions( arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_ASK_PERMISSIONS )
                return true
            }
        }
        return false
    }

    fun openUri(activity: Activity, uri: Uri?) {
        uri ?: return
        val browseIntent = Intent(Intent.ACTION_VIEW).setData(uri)
        if (browseIntent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(browseIntent)
        }
    }

    @JvmStatic
    fun grantPermissionsLoc(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 23) {
            val hasAccessFineLocation = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            if (hasAccessFineLocation != PackageManager.PERMISSION_GRANTED) {
                DialogManager(activity).showOkCancelDialog(
                    messageResId = R.string.setting_summary_location,
                    okAction = {
                        activity.requestPermissions(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_CODE_ASK_PERMISSIONS_1
                        )
                    }
                )
            }
        }
    }

    @JvmStatic
    fun applyTheme(context: Context) = context.setTheme(R.style.AppTheme)

    @JvmStatic
    fun setBottomSheetBehavior(dialog: BottomSheetDialog, view: View, beh: Int) {
        val mBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(view.parent as View)
        mBehavior.state = beh
        mBehavior.setBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dialog.cancel()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    @JvmStatic
    fun createShortcut(context: Context, title: String?, url: String?, bitmap: Bitmap?) {
        val url = url ?: return
        val uri = convertUrlToAppScheme(url)
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                action = Intent.ACTION_VIEW
                data = uri
            } ?: return

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) { // code for adding shortcut on pre oreo device
                val installer = Intent().apply {
                    action = "com.android.launcher.action.INSTALL_SHORTCUT"
                    putExtra("android.intent.extra.shortcut.INTENT", intent)
                    putExtra("android.intent.extra.shortcut.NAME", title)
                    if (bitmap != null) {
                        putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap)
                    } else {
                        putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context.applicationContext, R.drawable.qc_bookmarks))
                    }
                }

                context.sendBroadcast(installer)
            } else {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
                var icon: Icon = if (bitmap != null) {
                    Icon.createWithBitmap(bitmap)
                } else {
                    Icon.createWithResource(context, R.drawable.qc_bookmarks)
                }

                if (shortcutManager.isRequestPinShortcutSupported) {
                    val pinShortcutInfo = ShortcutInfo.Builder(context, uri.toString())
                            .setShortLabel(title!!)
                            .setLongLabel(title)
                            .setIcon(icon)
                            .setIntent(intent)
                            .build()
                    shortcutManager.requestPinShortcut(pinShortcutInfo, null)
                } else {
                    println("failed_to_add")
                }
            }
        } catch (e: Exception) {
            println("failed_to_add")
        }
    }

    private fun convertUrlToAppScheme(url: String): Uri {
        val originalUri = Uri.parse(url)
        val scheme = when (originalUri.scheme) {
            "https" -> "einkbros"
            "https" -> "einkbro"
            else -> originalUri.scheme
        }
        return originalUri.buildUpon().scheme(scheme).build()
    }

    fun Uri.toNormalScheme(): Uri {
        val scheme = when (scheme) {
            "einkbros" -> "https"
            "einkbro" -> "https"
            else -> scheme
        }
        return buildUpon().scheme(scheme).build()
    }

    @JvmStatic
    fun fileName(url: String?): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val currentTime = sdf.format(Date())
        val domain = Uri.parse(url).host?.replace("www.", "")?.trim { it <= ' ' } ?: ""
        return domain.replace(".", "_").trim { it <= ' ' } + "_" + currentTime.trim { it <= ' ' }
    }

    @JvmStatic
    fun secString(string: String?): String = string?.replace("'".toRegex(), "\'\'") ?: "No title"

    @JvmStatic
    fun domain(url: String?): String {
        return if (url == null) {
            ""
        } else {
            try {
                Uri.parse(url).host?.replace("www.", "")?.trim { it <= ' ' } ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }

    @JvmStatic
    fun textSpannable(text: String?): SpannableString {
        val s: SpannableString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SpannableString(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY))
        } else {
            SpannableString(Html.fromHtml(text))
        }
        Linkify.addLinks(s, Linkify.WEB_URLS)
        return s
    }

    private val NEGATIVE_COLOR = floatArrayOf(
            -1.0f, 0f, 0f, 0f, 255f, 0f, -1.0f, 0f, 0f, 255f, 0f, 0f, -1.0f, 0f, 255f, 0f, 0f, 0f, 1.0f, 0f)

    fun openFile(activity: Activity, uri: Uri, mimeType: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = uri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        activity.startActivity(Intent.createChooser(intent, "Open file with"))
    }

    fun showBrowserChooser(activity: Activity, url: String?, title: String) {
        val nonNullUrl = url ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(nonNullUrl))
        activity.startActivity(Intent.createChooser(intent, title))
    }

}