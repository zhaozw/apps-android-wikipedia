package org.wikipedia.page.snippet;

import android.annotation.TargetApi;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageViewFragmentInternal;
import org.wikipedia.util.ShareUtils;

/**
 * Deals with selected text in the WebView.
 */
public class SnippetShareAdapter {
    private final PageActivity activity;
    private final WikipediaApp app;
    private ActionMode webViewActionMode;
    private ClipboardManager.OnPrimaryClipChangedListener clipListener;
    private MenuItem copyMenuItem;
    private boolean snippetShared;

    public SnippetShareAdapter(PageActivity activity) {
        this.activity = activity;
        app = (WikipediaApp) activity.getApplicationContext();
    }

    /**
     * Since API <11 doesn't provide a long-press context for the WebView anyway, and we're
     * using clipboard features that are only supported in API 11+, we'll mark this whole
     * method as TargetApi(11), so that the IDE doesn't get upset.
     * @param mode ActionMode under which this context is starting.
     */
    @TargetApi(11)
    public void onTextSelected(final ActionMode mode) {
        webViewActionMode = mode;
        Menu menu = mode.getMenu();

        // Find the context menu item for copying text to the clipboard...
        // The most practical way to do this seems to be to get the resource name of the
        // menu item, and see if it resembles "action_menu_copy", which appears to remain
        // consistent throughout the various APIs.
        for (int i = 0; i < menu.size(); i++) {
            String resourceName
                    = activity.getResources().getResourceName(menu.getItem(i).getItemId());
            if (resourceName.contains("action_menu_copy")) {
                copyMenuItem = menu.getItem(i);
                break;
            }
        }

        // add our clipboard listener, so that we'll get an event when the text
        // is copied onto it...
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(
                Context.CLIPBOARD_SERVICE);
        if (clipListener == null) {
            clipListener = new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {
                    // get the text from the clipboard!
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(
                            Context.CLIPBOARD_SERVICE);
                    if (clipboard.hasPrimaryClip()
                            && clipboard.getPrimaryClip().getItemCount() > 0) {

                        CharSequence selectedText = clipboard.getPrimaryClip().getItemAt(0)
                                .coerceToText(activity);
                        Log.d("Share", ">>> Clipboard text: " + selectedText);

                        String textSnippet = sanitizeText(selectedText.toString());

                        // Pass the clipboard text to the Share handler!
                        shareSnippet(textSnippet);
                    }
                    clipboard.removePrimaryClipChangedListener(clipListener);
                }
            };
        }
        // remove it first, just in case it was added from the last context, and
        // ended up not being used.
        clipboard.removePrimaryClipChangedListener(clipListener);
        // and add it again.
        clipboard.addPrimaryClipChangedListener(clipListener);

        // inject our Share item into the menu...
        MenuItem shareItem = menu.add(R.string.share_via);
        shareItem.setIcon(app.getCurrentTheme() == WikipediaApp.THEME_DARK
                ? R.drawable.ic_message_dark
                : R.drawable.ic_message);
        MenuItemCompat.setShowAsAction(shareItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

        shareItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (copyMenuItem != null) {
                    // programmatically invoke the copy-to-clipboard action...
                    webViewActionMode.getMenu()
                            .performIdentifierAction(copyMenuItem.getItemId(), 0);
                    // this will trigger a state-change event in the Clipboard, which we'll
                    // catch with our listener above.
                }
                // leave context mode...
                if (webViewActionMode != null) {
                    webViewActionMode.finish();
                }
                return true;
            }
        });
    }

    private String sanitizeText(String selectedText) {
        return selectedText.replaceAll("\\[\\d+\\]", "") // [1]
                .replaceAll("\\(\\s*;\\s*", "\\(") // (; -> (    hacky way for IPA remnants
                .replaceAll("\\s{2,}", " ");
    }

    private void shareSnippet(CharSequence selectedText) {
        final int minTextSnippetLength = 6;
        selectedText = selectedText.toString().trim();
        if (selectedText.length() < minTextSnippetLength) {
            return;
        }
        final PageViewFragmentInternal curPageFragment = activity.getCurPageFragment();
        if (curPageFragment == null) {
            return;
        }

        PageTitle title = curPageFragment.getTitle();
        String introText = activity.getString(R.string.snippet_share_intro,
                title.getDisplayText(),
                title.getCanonicalUri() + "?source=app");
        Bitmap resultBitmap = SnippetImage.createImage(activity,
                curPageFragment.getLeadImageBitmap(),
                curPageFragment.getImageBaseYOffset(),
                title.getDisplayText(), selectedText);
        ShareUtils.shareImage(activity, resultBitmap, "*/*",
                title.getDisplayText(), title.getDisplayText(), introText, false);
    }
}
