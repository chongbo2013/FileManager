package com.lewa.filemanager;

import com.lewa.filemanager.CustomMenu.DropDownMenu;
import com.lewa.filemanager.FileViewInteractionHub.Mode;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
//LEWA ADD BEGIN
// import android.view.ActionMode;
import lewa.support.v7.view.ActionMode;
//LEWA ADD BEGIN
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lewa.filemanager.R;

public class FileListItem {
    protected static final String SEPARATOR = "/";
    public static void setupFileListItemInfo(Context context, View view,
            FileInfo fileInfo, FileIconHelper fileIcon,
            FileViewInteractionHub fileViewInteractionHub) {
        // if in moving mode, show selected file always
        if (fileInfo == null)
            return;

        if (fileViewInteractionHub.isMoveState()) {
            fileInfo.Selected = fileViewInteractionHub.isFileChecked(fileInfo.filePath);
        }
        if (fileViewInteractionHub.getSelectedFileList().size() <= 0
                && fileViewInteractionHub.getMode() != Mode.Pick) {
            if (fileViewInteractionHub.getMode() == Mode.Edit) {
            } else {
                fileViewInteractionHub.setMode(Mode.View);
            }
        } else if (fileViewInteractionHub.getSelectedFileList().size() > 0
                && fileViewInteractionHub.getMode() != Mode.Pick) {
            fileViewInteractionHub.setMode(Mode.Edit);
        }

        CheckBox checkbox = (CheckBox) view.findViewById(R.id.file_checkbox);
        checkbox.setTag(fileInfo);
        if (fileViewInteractionHub.getMode() == Mode.Edit) {
            checkbox.setVisibility(View.VISIBLE);
            checkbox.setChecked(fileInfo.Selected ? true : false);
            view.setSelected(fileInfo.Selected);
        } else {
            checkbox.setVisibility(View.GONE);
            checkbox.setClickable(false);
            view.setSelected(fileInfo.Selected);
        }

        Util.setText(view, R.id.file_name, fileInfo.fileName);
        Util.setText(view, R.id.file_size,
                (fileInfo.IsDir ? "" : Util.convertStorage(fileInfo.fileSize)));

        TextView time = (TextView) view.findViewById(R.id.modified_time);
        TextView version = (TextView) view.findViewById(R.id.apk_version);
        TextView installInfo = (TextView) view.findViewById(R.id.install_info);
        // Show Apk version and install info for apk category items.
        if ((SEPARATOR + context.getString(R.string.category_apk)).equals(fileViewInteractionHub
                .getCurrentPath())) {
            ApkItem apkItem = new ApkItem(context, fileInfo.filePath); // second_line_apk

            time.setVisibility(View.GONE);
            version.setVisibility(View.VISIBLE);
            installInfo.setVisibility(View.VISIBLE);
            version.setText(context.getString(R.string.apk_version) + " "
                    + apkItem.versionName);
            installInfo.setText(apkItem.installInfo);
        } else {
            time.setVisibility(View.VISIBLE);
            if (version != null) {
                version.setText("");
            }
            if (installInfo != null) {
                installInfo.setText("");
            }

            Util.setText(view, R.id.file_count, fileInfo.IsDir ? "("
                    + fileInfo.Count + ")" : "");
            Util.setText(view, R.id.modified_time,
                    Util.formatDateString(context, fileInfo.ModifiedDate));
        }

        ImageView lFileImage = (ImageView) view.findViewById(R.id.file_image);
        ImageView lFileImageFrame = (ImageView) view
                .findViewById(R.id.file_image_frame);
        lFileImage.setTag(fileInfo.filePath);

        if (fileInfo.IsDir) {
            lFileImageFrame.setVisibility(View.GONE);
            Util.setImageResource(lFileImage, R.drawable.folder,
                    fileInfo.filePath);
            // lFileImage.setImageResource(R.drawable.folder);
        } else {
            fileIcon.setIcon(fileInfo, lFileImage, lFileImageFrame);
        }
    }

    public static class FileItemOnClickListener implements OnClickListener {
        private Context mContext;
        private FileViewInteractionHub mFileViewInteractionHub;
        private DropDownMenu mSelectionMenu;

        public FileItemOnClickListener(Context context,
                FileViewInteractionHub fileViewInteractionHub) {
            mContext = context;
            mFileViewInteractionHub = fileViewInteractionHub;
            // mFileViewInteractionHub.hideBottomBar();
        }

        @Override
        public void onClick(View v) {
            // CheckBox img = (CheckBox) v.findViewById(R.id.file_checkbox);

            CheckBox img = (CheckBox) v;
            if (img == null || img.getTag() == null)
                return;

            FileInfo tag = (FileInfo) img.getTag();
            tag.Selected = !tag.Selected;

            ActionMode actionMode = ((FileManagerMainActivity) mContext)
                    .getActionMode();

            if (mFileViewInteractionHub.onCheckItem(tag, v)) {
                img.setChecked(tag.Selected ? true : false);
            } else {
                tag.Selected = !tag.Selected;
            }

            if (actionMode != null) {
                actionMode.invalidate();
                Util.updateActionModeMenuTitle(actionMode, mContext,
                        mFileViewInteractionHub.getSelectedFileList().size());
                Util.updateActionModeMenuState(actionMode, mContext);
                Menu menu = actionMode.getMenu();
//                MenuItem selectAll = menu.findItem(R.id.action_select_all);
                if (mFileViewInteractionHub.isSelectedAll()) {
//                    selectAll.setIcon(lewa.R.drawable.ic_menu_clear_select);
                    // Delete for standalone by Fan.Yang
                    actionMode.setRightActionButtonResource(lewa.R.drawable.ic_menu_clear_select);
                } else {
//                    selectAll.setIcon(lewa.R.drawable.ic_menu_select_all);
                    // Delete for standalone by Fan.Yang
                   actionMode.setRightActionButtonResource(lewa.R.drawable.ic_menu_select_all);
                }
            }

            mFileViewInteractionHub.updateMenuItem();
        }
    }
}
