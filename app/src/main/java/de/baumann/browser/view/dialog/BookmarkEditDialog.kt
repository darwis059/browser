package de.baumann.browser.view.dialog

import android.R
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.NonNull
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import de.baumann.browser.Ninja.databinding.DialogEditBookmarkBinding
import de.baumann.browser.database.Bookmark
import de.baumann.browser.database.BookmarkManager
import de.baumann.browser.view.NinjaToast
import kotlinx.coroutines.launch

class BookmarkEditDialog(
        private val activity: Activity,
        private val bookmarkManager: BookmarkManager,
        private val bookmark: Bookmark,
        private val okAction: () -> Unit,
        private val cancelAction: () -> Unit,
) {
    fun show() {
        val lifecycleScope = (activity as LifecycleOwner).lifecycleScope

        val binding = DialogEditBookmarkBinding.inflate(LayoutInflater.from(activity))
        binding.passTitle.setText(bookmark.title)
        if (bookmark.isDirectory) {
            binding.urlContainer.visibility = View.GONE
        } else {
            binding.passUrl.setText(bookmark.url)
        }

        binding.buttonAddFolder.setOnClickListener { addFolder(lifecycleScope, binding) }

        updateFolderSpinner(binding)

        DialogManager(activity).showOkCancelDialog(
                title = activity.getString(de.baumann.browser.Ninja.R.string.menu_save_bookmark),
                view = binding.root,
                okAction = { upsertBookmark(binding, lifecycleScope) },
                cancelAction = { cancelAction.invoke() }
        )
    }

    private fun addFolder(lifecycleScope: LifecycleCoroutineScope, binding: DialogEditBookmarkBinding) {
        lifecycleScope.launch {
            val folderName = getFolderName()
            bookmarkManager.insertDirectory(folderName)
            updateFolderSpinner(binding, folderName)
        }
    }

    private fun updateFolderSpinner(binding: DialogEditBookmarkBinding, selectedFolderName: String? = null) {
        val lifecycleScope = (activity as LifecycleOwner).lifecycleScope
        lifecycleScope.launch {
            val folders = bookmarkManager.getBookmarkFolders().toMutableList().apply { add(0, Bookmark("Top", "", true)) }
            if (bookmark.isDirectory) folders.remove(bookmark)

            binding.folderSpinner.adapter = ArrayAdapter(activity, R.layout.simple_spinner_dropdown_item, folders)
            val selectedIndex = if (selectedFolderName == null) {
                folders.indexOfFirst { it.id == bookmark.parent }
            } else {
                folders.indexOfFirst { it.title == selectedFolderName }
            }
            binding.folderSpinner.setSelection(selectedIndex)

            binding.folderSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    bookmark.parent = folders[position].id
                }

                override fun onNothingSelected(parent: AdapterView<*>?) { }
            }
        }
    }

    private fun upsertBookmark(binding: DialogEditBookmarkBinding, lifecycleScope: LifecycleCoroutineScope) {
        try {
            bookmark.title = binding.passTitle.text.toString().trim { it <= ' ' }
            bookmark.url = binding.passUrl.text.toString().trim { it <= ' ' }
            lifecycleScope.launch {
                bookmarkManager.insert(bookmark)
                okAction.invoke()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NinjaToast.show(activity, de.baumann.browser.Ninja.R.string.toast_error)
        }
    }

    private suspend fun getFolderName(): String {
        val context: Context = activity
        return TextInputDialog(
                context,
                context.getString(de.baumann.browser.Ninja.R.string.folder_name),
                context.getString(de.baumann.browser.Ninja.R.string.folder_name_description),
                ""
        ).show() ?: "New Folder"
    }

}