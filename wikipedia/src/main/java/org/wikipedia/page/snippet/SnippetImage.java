package org.wikipedia.page.snippet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import org.wikipedia.R;

/**
 * Creator and holder of a Bitmap which is comprised of an optional lead image, a title,
 * optional description, text, the Wikipedia wordmark, and some license icons.
 */
public final class SnippetImage {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int HORIZONTAL_PADDING = 30;
    private static final int TEXT_WIDTH = WIDTH - 2 * HORIZONTAL_PADDING;
    private static final int DESCRIPTION_WIDTH = 360;
    private static final float SPACING_MULTIPLIER = 1.0f;
    private static final Typeface SERIF = Typeface.create("serif", Typeface.NORMAL);
    private static final int QUARTER = 4;

    private final Context context;
    private final Bitmap leadImageBitmap;
    private final float leadImageFocusY;
    private final String title;
    private final String description;
    private final CharSequence textSnippet;
    private boolean isArticleRTL;

    public SnippetImage(Context context, Bitmap leadImageBitmap,
                        float leadImageFocusY, String title, String description,
                        CharSequence textSnippet) {
        this.context = context;
        this.leadImageBitmap = leadImageBitmap;
        this.leadImageFocusY = leadImageFocusY;
        this.title = title;
        this.description = description;
        this.textSnippet = textSnippet;
    }

    /**
     * Creates a card image usable for sharing and the preview of the same.
     * If we have a leadImageBitmap the use that as the background. If not then
     * just use a black background.
     */
    public Bitmap createImage() {
        Bitmap resultBitmap = drawBackground(leadImageBitmap, leadImageFocusY);
        Canvas canvas = new Canvas(resultBitmap);
        if (leadImageBitmap != null) {
            drawGradient(canvas);
        }

        Layout textLayout = drawTextSnippet(canvas, textSnippet);
        isArticleRTL = textLayout.getParagraphDirection(0) == Layout.DIR_RIGHT_TO_LEFT;

        int top = drawLicenseIcons(canvas, context);
        top = drawDescription(canvas, description, top);
        drawTitle(canvas, title, top);
        drawWordmark(canvas, context);

        return resultBitmap;
    }

    private Bitmap drawBackground(Bitmap leadImageBitmap, float leadImageFocusY) {
        Bitmap resultBitmap;
        if (leadImageBitmap != null) {
            // use lead image
            resultBitmap = scaleCropToFitFace(leadImageBitmap, WIDTH, HEIGHT, leadImageFocusY);
        } else {
            resultBitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
            final int backgroundColor = 0xff242438;
            resultBitmap.eraseColor(backgroundColor);
        }
        return resultBitmap;
    }

    private void drawGradient(Canvas canvas) {
        // draw a dark gradient over the image, so that the white text
        // will stand out better against it.
        final int gradientStartColor = 0x60000000;
        final int gradientStopColor = 0xA0000000;
        Shader shader = new LinearGradient(0, 0, 0, canvas.getHeight(), gradientStartColor,
                gradientStopColor, Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setShader(shader);
        canvas.drawRect(new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), paint);
    }

    private Layout drawTextSnippet(Canvas canvas, CharSequence textSnippet) {
        final int top = 8;
        final int maxHeight = 225;
        final int maxLines = 5;
        final float maxFontSize = 195.0f;
        final float minFontSize = 38.0f;

        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(maxFontSize);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setShadowLayer(1.0f, 1.0f, 1.0f, Color.GRAY);

        StaticLayout textLayout = optimizeTextSize(
                new TextLayoutParams(textSnippet, textPaint, TEXT_WIDTH, SPACING_MULTIPLIER),
                maxHeight, maxLines, maxFontSize, minFontSize);

        canvas.save();
        int horizontalCenterOffset = top + (maxHeight - textLayout.getHeight()) / QUARTER;
        canvas.translate(HORIZONTAL_PADDING, horizontalCenterOffset);
        textLayout.draw(canvas);
        canvas.restore();

        return textLayout;
    }

    private int drawDescription(Canvas canvas, String description, int top) {
        final int marginBottom = 5;
        final int maxHeight = 23;
        final int maxLines = 2;
        final float maxFontSize = 15.0f;
        final float minFontSize = 10.0f;

        if (TextUtils.isEmpty(description)) {
            return top - marginBottom;
        }
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(maxFontSize);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setShadowLayer(1.0f, 0.0f, 0.0f, Color.GRAY);

        StaticLayout textLayout = optimizeTextSize(
                new TextLayoutParams(description, textPaint, DESCRIPTION_WIDTH, SPACING_MULTIPLIER),
                maxHeight, maxLines, maxFontSize, minFontSize);
        int left = HORIZONTAL_PADDING;
        if (isArticleRTL) {
            left = WIDTH - HORIZONTAL_PADDING - textLayout.getWidth();
        }

        top = top - marginBottom - textLayout.getHeight();
        canvas.save();
        canvas.translate(left, top);
        textLayout.draw(canvas);
        canvas.restore();

        return top;
    }

    private void drawTitle(Canvas canvas, String title, int top) {
        final int marginBottom = 0;
        final int maxHeight = 70;
        final int maxLines = 2;
        final float maxFontSize = 30.0f;
        final float spacingMultiplier = 0.7f;

        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(maxFontSize);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTypeface(SERIF);
        textPaint.setShadowLayer(1.0f, 0.0f, 1.0f, Color.GRAY);

        StaticLayout textLayout = optimizeTextSize(
                new TextLayoutParams(title, textPaint, DESCRIPTION_WIDTH, spacingMultiplier),
                maxHeight, maxLines, maxFontSize, maxFontSize);
        int left = HORIZONTAL_PADDING;
        if (isArticleRTL) {
            left = WIDTH - HORIZONTAL_PADDING - textLayout.getWidth();
        }
        int marginBottomTotal = marginBottom;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // versions < 5.0 don't compensate for bottom margin correctly when line
            // spacing is less than 1.0, so we'll compensate ourselves
            final int marginBoost = 10;
            marginBottomTotal += marginBoost;
        }

        top = top - marginBottomTotal - textLayout.getHeight();
        canvas.save();
        canvas.translate(left, top);
        textLayout.draw(canvas);
        canvas.restore();
    }

    private int drawLicenseIcons(Canvas canvas, Context context) {
        final int iconsWidth = 52;
        final int iconsHeight = 16;
        final int top = 319;
        final int bottom = top + iconsHeight;

        int left = HORIZONTAL_PADDING;
        int right = left + iconsWidth;
        if (isArticleRTL) {
            right = WIDTH - HORIZONTAL_PADDING;
            left = right - iconsWidth;
        }

        Drawable d = context.getResources().getDrawable(R.drawable.cc_by_sa_white);
        d.setBounds(left, top, right, bottom);
        d.draw(canvas);

        return top;
    }

    private void drawWordmark(Canvas canvas, Context context) {
        final int top = 293;
        final float fontSize = 24.0f;
        final int maxWidth = WIDTH - DESCRIPTION_WIDTH - 2 * HORIZONTAL_PADDING;

        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.LTGRAY);
        textPaint.setTextSize(fontSize);
        textPaint.setTypeface(SERIF);

        Spanned wikipedia = Html.fromHtml(context.getString(R.string.wp_stylized));
        StaticLayout wordmarkLayout = buildLayout(
                new TextLayoutParams(wikipedia, textPaint, maxWidth, 1.0f));
        final int wordMarkWidth = (int) wordmarkLayout.getLineWidth(0);

        int left = WIDTH - HORIZONTAL_PADDING - wordMarkWidth;
        if (isArticleRTL) {
            left = HORIZONTAL_PADDING;
        }

        canvas.save(); // --
        canvas.translate(left, top);
        wordmarkLayout.draw(canvas);
        canvas.restore(); // --
    }

    /**
     * If the title or text is too long we first reduce the font size.
     * If that is not enough it gets ellipsized.
     */
    private StaticLayout optimizeTextSize(TextLayoutParams params, int maxHeight, int maxLines,
                                          float maxFontSize, float minFontSize) {
        final float threshold1 = 60.0f;
        final float threshold2 = 40.0f;
        final float extraStep1 = 3.0f;
        final float extraStep2 = 1.0f;
        boolean fits = false;
        StaticLayout textLayout = null;

        // Try decreasing font size first
        for (float fontSize = maxFontSize; fontSize >= minFontSize; fontSize -= 1.0f) {
            params.textPaint.setTextSize(fontSize);
            textLayout = buildLayout(params);
            if (textLayout.getHeight() <= maxHeight) {
                fits = true;
                break;
            }

            // make it go faster at the beginning...
            if (fontSize > threshold1) {
                fontSize -= extraStep1;
            } else if (fontSize > threshold2) {
                fontSize -= extraStep2;
            }
        }

        // Then do own ellipsize: cut text off after last fitting space and add "..."
        // Didn't want to cut off randomly in the middle of a line or word.
        if (!fits) {
            final String textStr = params.text.toString();
            final int ellipsisLength = 3;
            final int ellipsisStart = textLayout != null
                    ? textLayout.getLineStart(maxLines) - ellipsisLength
                    : textStr.length();
            final int end = textStr.lastIndexOf(' ', ellipsisStart) + 1;
            if (end > 0) {
                textLayout = buildLayout(
                        new TextLayoutParams(params, textStr.substring(0, end) + "..."));
                if (textLayout.getLineCount() <= maxLines) {
                    fits = true;
                }
            }
        }

        // last resort: use TextUtils.ellipsize()
        if (!fits) {
            final float textRatio = .87f;
            final float maxWidth = textRatio * maxLines * params.lineWidth;
            textLayout = buildLayout(new TextLayoutParams(params,
                    TextUtils.ellipsize(params.text, params.textPaint, maxWidth,
                            TextUtils.TruncateAt.END)));
        }

        return textLayout;
    }

    private StaticLayout buildLayout(TextLayoutParams params) {
        return new StaticLayout(
                params.text,
                params.textPaint,
                params.lineWidth,
                Layout.Alignment.ALIGN_NORMAL,
                params.spacingMultiplier,
                0.0f,
                false);
    }

    // Borrowed from http://stackoverflow.com/questions/5226922/crop-to-fit-image-in-android
    // Modified to allow for face detection adjustment, startY
    private Bitmap scaleCropToFitFace(Bitmap original, int targetWidth, int targetHeight,
                                      float normalizedYOffset) {
        // Need to scale the image, keeping the aspect ratio first
        int width = original.getWidth();
        int height = original.getHeight();

        float widthScale = (float) targetWidth / (float) width;
        float heightScale = (float) targetHeight / (float) height;
        float scaledWidth;
        float scaledHeight;

        int startX = 0;
        int startY = 0;

        if (widthScale > heightScale) {
            scaledWidth = targetWidth;
            scaledHeight = height * widthScale;
            // crop height by...
            startY = (int) (normalizedYOffset * scaledHeight - targetHeight / 2);
            // Adjust the face position by a slight amount.
            // The face recognizer gives the location of the *eyes*, whereas we actually
            // want to center on the *nose*...
            final int faceBoost = 32;
            startY += faceBoost;
            if (startY < 0) {
                startY = 0;
            } else if (startY + targetHeight > scaledHeight) {
                startY = (int)(scaledHeight - targetHeight);
            }
        } else {
            scaledHeight = targetHeight;
            scaledWidth = width * heightScale;
            // crop width by..
            // startX = (int) ((scaledWidth - targetWidth) / 2);
        }

        Bitmap scaledBitmap
                = Bitmap.createScaledBitmap(original, (int) scaledWidth, (int) scaledHeight, true);
        Bitmap bitmap = Bitmap.createBitmap(scaledBitmap, startX, startY, targetWidth, targetHeight);
        scaledBitmap.recycle();
        return bitmap;
    }

    /**
     * Parameter object for #buildLayout and #optimizeTextSize.
     */
    private static class TextLayoutParams {
        private final CharSequence text;
        private final TextPaint textPaint;
        private final int lineWidth;
        private final float spacingMultiplier;

        private TextLayoutParams(CharSequence text, TextPaint textPaint, int lineWidth,
                                 float spacingMultiplier) {
            this.text = text;
            this.textPaint = textPaint;
            this.lineWidth = lineWidth;
            this.spacingMultiplier = spacingMultiplier;
        }

        public TextLayoutParams(TextLayoutParams other, CharSequence text) {
            this.text = text;
            this.textPaint = other.textPaint;
            this.lineWidth = other.lineWidth;
            this.spacingMultiplier = other.spacingMultiplier;
        }
    }
}
